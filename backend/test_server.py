import json
import tempfile
import threading
import unittest
import uuid
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from server import Store, ApiError, make_server


class BackendTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.store = Store(str(Path(self.tmp.name) / 'test.sqlite'))
        self.owner = self.user('Anna')
        self.member = self.user('Bartek')
        self.stranger = self.user('Celina')
        self.trip = str(uuid.uuid4())
        self.trip_data = {'title': 'Tatry', 'destination': 'Zakopane', 'startDate': '2026-10-10', 'endDate': '2026-10-13', 'currency': 'PLN', 'description': ''}
        self.store.apply(self.owner['user']['id'], self.op(self.trip, 'trip', self.trip_data))

    def tearDown(self):
        self.store.close()
        self.tmp.cleanup()

    def user(self, name):
        return self.store.register({'name': name, 'email': name.lower()+'@example.org', 'password': 'Secret123!'})

    def op(self, rid, kind, data, base=0, deleted=False):
        return {'id': str(uuid.uuid4()), 'recordId': rid, 'tripId': self.trip, 'kind': kind, 'payload': data, 'baseVersion': base, 'deleted': deleted}

    def join(self):
        code = self.store.invitation(self.owner['user']['id'], self.trip)['code']
        return self.store.join(self.member['user']['id'], code)

    def event(self, title='Spacer'):
        return {'title': title, 'date': '2026-10-11', 'time': '09:30', 'place': '', 'description': '', 'rank': 0, 'done': False, 'reminder': False}

    def conversion_setup(self):
        self.join()
        idea = {'title': 'Spacer', 'place': '', 'description': ''}
        self.store.apply(self.owner['user']['id'], self.op('idea', 'proposal', idea))
        uid = self.member['user']['id']
        self.store.apply(uid, self.op('idea:'+uid, 'vote', {'proposalId': 'idea', 'userId': uid}))
        return idea

    def test_conversion_is_atomic_and_removes_votes(self):
        self.conversion_setup()
        user = self.owner['user']['id']
        result = self.store.apply(user, self.op('idea', 'convert', self.event(), 1))
        self.assertEqual(('idea', 'event', 2), (result['record']['id'], result['record']['kind'], result['record']['version']))
        records = self.store.snapshot(user)['records']
        self.assertFalse(any(r['kind'] in ('proposal', 'vote') and not r['deleted'] for r in records))
        self.assertTrue(any(r['kind'] == 'activity' and r['payload']['action'] == 'convert' for r in records))

    def test_concurrent_conversion_creates_only_one_event(self):
        from concurrent.futures import ThreadPoolExecutor
        self.conversion_setup()
        operations = [self.op('idea', 'convert', self.event('Anna'), 1), self.op('idea', 'convert', self.event('Bartek'), 1)]
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(self.store.apply, u['user']['id'], op) for u, op in zip([self.owner, self.member], operations)]
            results = [future.result() for future in futures]
        self.assertEqual(['applied', 'conflict'], sorted(r['status'] for r in results))
        self.assertEqual(1, len([r for r in self.store.snapshot(self.owner['user']['id'])['records'] if r['kind'] == 'event' and not r['deleted']]))

    def test_invalid_conversion_keeps_proposal_and_votes(self):
        self.conversion_setup()
        with self.assertRaises(ApiError):
            self.store.apply(self.owner['user']['id'], self.op('idea', 'convert', dict(self.event(), date='2026-11-01'), 1))
        records = self.store.snapshot(self.owner['user']['id'])['records']
        self.assertTrue(any(r['id'] == 'idea' and r['kind'] == 'proposal' and r['version'] == 1 for r in records))
        self.assertTrue(any(r['kind'] == 'vote' and not r['deleted'] for r in records))
        self.assertFalse(any(r['kind'] == 'event' for r in records))

    def test_conversion_rolls_back_on_database_failure(self):
        from unittest.mock import patch
        import sqlite3
        self.conversion_setup()
        with patch.object(self.store, 'activity', side_effect=sqlite3.OperationalError('test rollback')):
            with self.assertRaises(sqlite3.OperationalError):
                self.store.apply(self.owner['user']['id'], self.op('idea', 'convert', self.event(), 1))
        records = self.store.snapshot(self.owner['user']['id'])['records']
        self.assertEqual('proposal', next(r for r in records if r['id'] == 'idea')['kind'])
        self.assertTrue(any(r['kind'] == 'vote' and not r['deleted'] for r in records))

    def test_conversion_retry_is_idempotent(self):
        self.conversion_setup()
        operation = self.op('idea', 'convert', self.event(), 1)
        first = self.store.apply(self.owner['user']['id'], operation)
        self.assertEqual(first, self.store.apply(self.owner['user']['id'], operation))
        history = [r for r in self.store.snapshot(self.owner['user']['id'])['records'] if r['kind'] == 'activity' and r['payload']['action'] == 'convert']
        self.assertEqual(1, len(history))

    def test_conversion_conflict_preserves_newer_proposal(self):
        idea = self.conversion_setup()
        self.store.apply(self.owner['user']['id'], self.op('idea', 'proposal', dict(idea, title='Nowy pomysł'), 1))
        result = self.store.apply(self.member['user']['id'], self.op('idea', 'convert', self.event(), 1))
        self.assertEqual('conflict', result['status'])
        self.assertEqual('proposal', result['record']['kind'])
        self.assertEqual('Nowy pomysł', result['record']['payload']['title'])

    def test_unsynced_conversion_creates_single_event(self):
        result = self.store.apply(self.owner['user']['id'], self.op('local-idea', 'convert', self.event()))
        self.assertEqual(('event', 1), (result['record']['kind'], result['record']['version']))

    def test_conversion_requires_access_and_rejects_deleted_source(self):
        idea = self.conversion_setup()
        with self.assertRaises(ApiError) as error:
            self.store.apply(self.stranger['user']['id'], self.op('idea', 'convert', self.event(), 1))
        self.assertEqual(403, error.exception.status)
        self.store.apply(self.owner['user']['id'], self.op('idea', 'proposal', idea, 1, True))
        with self.assertRaises(ApiError):
            self.store.apply(self.owner['user']['id'], self.op('idea', 'convert', self.event(), 2))

    def test_password_is_hashed_and_session_can_be_revoked(self):
        stored = self.store.db.execute('SELECT password,salt FROM users').fetchone()
        self.assertNotEqual('Secret123!', stored['password'])
        self.assertEqual(self.owner['user']['id'], self.store.authenticate(self.owner['token']))
        self.store.logout(self.owner['token'])
        with self.assertRaises(ApiError):
            self.store.authenticate(self.owner['token'])

    def test_invalid_login_and_duplicate_email(self):
        with self.assertRaises(ApiError):
            self.store.login({'email': 'anna@example.org', 'password': 'Wrong123!'})
        with self.assertRaises(ApiError):
            self.user('Anna')

    def test_invite_join_rotation_and_owner_role(self):
        code = self.store.invitation(self.owner['user']['id'], self.trip)['code']
        self.join()
        self.assertIn(self.trip, self.store.snapshot(self.member['user']['id'])['tripIds'])
        with self.assertRaises(ApiError):
            self.store.invitation(self.member['user']['id'], self.trip)
        self.store.invitation(self.owner['user']['id'], self.trip, True)
        with self.assertRaises(ApiError):
            self.store.join(self.stranger['user']['id'], code)
        self.assertEqual('owner', self.store.role(self.owner['user']['id'], self.trip))

    def test_access_control_is_enforced_on_server(self):
        self.assertEqual([], self.store.snapshot(self.stranger['user']['id'])['records'])
        with self.assertRaises(ApiError):
            self.store.apply(self.stranger['user']['id'], self.op('event', 'event', self.event()))
        self.join()
        with self.assertRaises(ApiError):
            self.store.apply(self.member['user']['id'], self.op(self.trip, 'trip', dict(self.trip_data, title='Zmiana'), 1))

    def test_idempotent_retry_does_not_duplicate_or_increment_version(self):
        op = self.op('event', 'event', self.event())
        first = self.store.apply(self.owner['user']['id'], op)
        second = self.store.apply(self.owner['user']['id'], op)
        self.assertEqual(first, second)
        self.assertEqual(1, second['record']['version'])
        with self.assertRaises(ApiError):
            self.store.apply(self.owner['user']['id'], dict(op, payload=self.event('Inna')))

    def test_conflict_detected_and_can_be_resolved(self):
        self.join()
        self.store.apply(self.owner['user']['id'], self.op('event', 'event', self.event()))
        self.store.apply(self.owner['user']['id'], self.op('event', 'event', self.event('Anna zmienia'), 1))
        result = self.store.apply(self.member['user']['id'], self.op('event', 'event', self.event('Bartek zmienia'), 1))
        self.assertEqual('conflict', result['status'])
        self.assertEqual('Anna zmienia', result['record']['payload']['title'])
        resolved = self.store.apply(self.member['user']['id'], self.op('event', 'event', self.event('Bartek zmienia'), 2))
        self.assertEqual(3, resolved['record']['version'])

    def test_deleted_record_is_tombstone_and_cannot_be_resurrected(self):
        self.store.apply(self.owner['user']['id'], self.op('event', 'event', self.event()))
        self.store.apply(self.owner['user']['id'], self.op('event', 'event', self.event(), 1, True))
        record = next(r for r in self.store.snapshot(self.owner['user']['id'])['records'] if r['id'] == 'event')
        self.assertTrue(record['deleted'])
        with self.assertRaises(ApiError):
            self.store.apply(self.owner['user']['id'], self.op('event', 'event', self.event(), 2))

    def test_votes_are_unique_and_can_be_toggled(self):
        self.store.apply(self.owner['user']['id'], self.op('idea', 'proposal', {'title': 'Kasprowy', 'place': '', 'description': ''}))
        uid = self.owner['user']['id']
        rid = f'idea:{uid}'
        data = {'proposalId': 'idea', 'userId': uid}
        self.store.apply(uid, self.op(rid, 'vote', data))
        self.store.apply(uid, self.op(rid, 'vote', data, 1, True))
        self.store.apply(uid, self.op(rid, 'vote', data, 2))
        votes = [r for r in self.store.snapshot(uid)['records'] if r['kind'] == 'vote' and not r['deleted']]
        self.assertEqual(1, len(votes))

    def test_expense_validation_currency_and_removed_members(self):
        self.join()
        a = self.owner['user']['id']
        b = self.member['user']['id']
        data = {'title': 'Bilety', 'date': '2026-10-10', 'amount': 101, 'payer': a, 'shares': {a: 51, b: 50}}
        self.store.apply(a, self.op('expense', 'expense', data))
        for bad in [dict(data, amount=100), dict(data, amount=1.01), dict(data, shares={a: 51, 'stranger': 50})]:
            with self.assertRaises(ApiError):
                self.store.apply(a, self.op(str(uuid.uuid4()), 'expense', bad))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op(self.trip, 'trip', dict(self.trip_data, currency='EUR'), 1))
        member = next(r for r in self.store.snapshot(a)['records'] if r['kind']=='member' and r['payload']['userId']==b)
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op(member['id'], 'member', member['payload'], 1, True))

    def test_member_removal_revokes_access_and_clears_assignee(self):
        self.join()
        a = self.owner['user']['id']
        b = self.member['user']['id']
        task = {'title': 'Zakupy', 'category': 'task', 'assignee': b, 'done': False, 'reminder': False, 'date': '2026-10-10', 'time': '09:00'}
        self.store.apply(a, self.op('task', 'task', task))
        member = next(r for r in self.store.snapshot(a)['records'] if r['kind']=='member' and r['payload']['userId']==b)
        self.store.apply(a, self.op(member['id'], 'member', member['payload'], 1, True))
        self.assertEqual([], self.store.snapshot(b)['tripIds'])
        snapshot = self.store.snapshot(a)
        self.assertEqual('', next(r for r in snapshot['records'] if r['id']=='task')['payload']['assignee'])
        with self.assertRaises(ApiError):
            self.store.apply(b, self.op('event', 'event', self.event()))

    def test_trip_delete_revokes_invitation_and_visibility(self):
        self.join()
        a = self.owner['user']['id']
        code = self.store.invitation(a, self.trip)['code']
        self.store.apply(a, self.op(self.trip, 'trip', self.trip_data, 1, True))
        self.assertEqual([], self.store.snapshot(a)['tripIds'])
        self.assertEqual([], self.store.snapshot(self.member['user']['id'])['tripIds'])
        with self.assertRaises(ApiError):
            self.store.join(self.stranger['user']['id'], code)

    def test_invalid_event_and_date_range_change(self):
        a = self.owner['user']['id']
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('event', 'event', dict(self.event(), date='2026-10-14')))
        self.store.apply(a, self.op('event', 'event', self.event()))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op(self.trip, 'trip', dict(self.trip_data, endDate='2026-10-10'), 1))

    def test_database_persists_after_reopen(self):
        path = str(Path(self.tmp.name) / 'test.sqlite')
        self.store.close()
        self.store = Store(path)
        self.assertIn(self.trip, self.store.snapshot(self.owner['user']['id'])['tripIds'])
        self.assertEqual(self.owner['user']['id'], self.store.authenticate(self.owner['token']))

    def test_budget_singleton_owner_limits_and_recreation(self):
        self.join()
        a, b = self.owner['user']['id'], self.member['user']['id']
        budget = {'limit': 100000, 'categoryLimits': {'food': 30000, 'transport': 20000}}
        rid = f'budget:{self.trip}'
        self.store.apply(a, self.op(rid, 'budget', budget))
        with self.assertRaises(ApiError):
            self.store.apply(b, self.op(rid, 'budget', budget, 1))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('other-budget', 'budget', budget))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op(rid, 'budget', dict(budget, categoryLimits={'food': 80000, 'transport': 80000}), 1))
        self.store.apply(a, self.op(rid, 'budget', budget, 1, True))
        self.assertEqual('applied', self.store.apply(a, self.op(rid, 'budget', budget, 2))['status'])

    def test_repayment_ownership_edit_delete_and_member_removal(self):
        self.join()
        a, b = self.owner['user']['id'], self.member['user']['id']
        from datetime import date
        payment = {'from': b, 'to': a, 'amount': 10000, 'date': date.today().isoformat(), 'note': 'Oddano gotówkę'}
        self.store.apply(b, self.op('payment', 'settlement', payment))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('payment', 'settlement', dict(payment, **{'from': a, 'to': b}), 1))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('payment', 'settlement', payment, 1, True))
        with self.assertRaises(ApiError):
            self.store.apply(b, self.op('other', 'settlement', dict(payment, to=b)))
        with self.assertRaises(ApiError):
            self.store.apply(b, self.op('other', 'settlement', dict(payment, date='2999-01-01')))
        member = next(r for r in self.store.snapshot(a)['records'] if r['kind']=='member' and r['payload']['userId']==b)
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op(member['id'], 'member', member['payload'], 1, True))
        self.store.apply(b, self.op('payment', 'settlement', dict(payment, amount=5000), 1))
        self.store.apply(b, self.op('payment', 'settlement', payment, 2, True))
        self.store.apply(a, self.op(member['id'], 'member', member['payload'], 1, True))

    def test_activity_server_authority_idempotency_and_conflict(self):
        a = self.owner['user']['id']
        operation = self.op('event', 'event', self.event())
        self.store.apply(a, operation)
        def history():
            return [r for r in self.store.snapshot(a)['records'] if r['kind']=='activity']
        before = history()
        self.store.apply(a, operation)
        self.assertEqual(len(before), len(history()))
        self.store.apply(a, self.op('event', 'event', self.event('Zmiana'), 1))
        self.assertEqual('conflict', self.store.apply(a, self.op('event', 'event', self.event('Konflikt'), 1))['status'])
        self.assertEqual(len(before)+1, len(history()))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('fake', 'activity', {'actorName': 'Ktoś'}))
        self.assertTrue(all(r['payload']['actorId']==a for r in history()))

    def test_place_coordinates_and_category_validation(self):
        a = self.owner['user']['id']
        place = {'title': 'Morskie Oko', 'latitude': 49.1978, 'longitude': 20.0700, 'description': ''}
        self.store.apply(a, self.op('place', 'place', place))
        for bad in [dict(place, latitude=91), dict(place, longitude=float('nan')), dict(place, latitude=True)]:
            with self.assertRaises(ApiError):
                self.store.apply(a, self.op(str(uuid.uuid4()), 'place', bad))
        expense = {'title': 'Hotel', 'date': '2026-10-10', 'amount': 100, 'payer': a, 'shares': {a: 100}, 'category': 'accommodation'}
        self.store.apply(a, self.op('expense', 'expense', expense))
        with self.assertRaises(ApiError):
            self.store.apply(a, self.op('bad-expense', 'expense', dict(expense, category='fake')))

    def test_online_backup_and_nonoverwrite(self):
        from backup import backup
        destination = Path(self.tmp.name) / 'backup.sqlite'
        backup(Path(self.tmp.name) / 'test.sqlite', destination)
        restored = Store(str(destination))
        try:
            self.assertIn(self.trip, restored.snapshot(self.owner['user']['id'])['tripIds'])
            self.assertEqual(self.owner['user']['id'], restored.authenticate(self.owner['token']))
        finally:
            restored.close()
        with self.assertRaises(ValueError):
            backup(Path(self.tmp.name) / 'test.sqlite', destination)

    def test_only_trusted_proxy_can_change_rate_limit_ip(self):
        for trusted in [(), ('127.0.0.1',)]:
            server = make_server(self.store, port=0, trusted_proxies=trusted)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            url = f'http://127.0.0.1:{server.server_port}/auth/login'
            def attempt(ip):
                request = Request(url, data=b'{broken', headers={'X-Forwarded-For': ip})
                try:
                    urlopen(request)
                except HTTPError as error:
                    return error.code
            try:
                for _ in range(30):
                    self.assertEqual(400, attempt('198.51.100.1'))
                self.assertEqual(429, attempt('198.51.100.1'))
                self.assertEqual(400 if trusted else 429, attempt('198.51.100.2'))
                if trusted:
                    self.assertEqual(400, attempt('not-an-ip'))
            finally:
                server.shutdown()
                server.server_close()
                thread.join()

    def test_http_auth_sync_and_error_contract(self):
        server = make_server(self.store, port=0)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f'http://127.0.0.1:{server.server_port}'
        try:
            self.assertEqual('ok', json.load(urlopen(base+'/health'))['status'])
            with self.assertRaises(HTTPError) as error:
                urlopen(base+'/sync')
            self.assertEqual(401, error.exception.code)
            request = Request(base+'/sync', headers={'Authorization': 'Bearer '+self.owner['token']})
            self.assertIn(self.trip, json.load(urlopen(request))['tripIds'])
            malformed = Request(base+'/auth/login', data=b'{broken', headers={'Content-Type': 'application/json'})
            with self.assertRaises(HTTPError) as error:
                urlopen(malformed)
            self.assertEqual(400, error.exception.code)
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == '__main__':
    unittest.main(verbosity=2)
