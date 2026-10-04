import argparse
import hashlib
import hmac
import json
import ipaddress
import re
import secrets
import sqlite3
import ssl
import threading
import time
import uuid
from datetime import date, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse


class ApiError(Exception):
    def __init__(self, status, message):
        self.status = status
        self.message = message
        super().__init__(message)


def require(condition, message, status=400):
    if not condition:
        raise ApiError(status, message)


def text(value, label, limit=240, optional=False):
    require(isinstance(value, str), f'{label}: oczekiwano tekstu.')
    require((optional or bool(value.strip())) and len(value) <= limit, f'{label}: nieprawidłowa długość.')
    return value.strip()


def iso_date(value):
    try:
        return date.fromisoformat(value)
    except (ValueError, TypeError):
        raise ApiError(400, 'Nieprawidłowa data. Użyj RRRR-MM-DD.')


CATEGORIES = {'transport', 'accommodation', 'food', 'activities', 'shopping', 'other'}


class Store:
    def __init__(self, path):
        self.db = sqlite3.connect(path, check_same_thread=False)
        self.db.row_factory = sqlite3.Row
        self.lock = threading.RLock()
        self.db.execute('PRAGMA foreign_keys=ON')
        self.db.execute('PRAGMA journal_mode=WAL')
        self.db.executescript('''
CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL, salt TEXT NOT NULL, password TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id), expires INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS records(id TEXT PRIMARY KEY, trip_id TEXT NOT NULL, kind TEXT NOT NULL, payload TEXT NOT NULL, version INTEGER NOT NULL, deleted INTEGER NOT NULL DEFAULT 0);
CREATE INDEX IF NOT EXISTS records_trip ON records(trip_id);
CREATE TABLE IF NOT EXISTS members(trip_id TEXT NOT NULL, user_id TEXT NOT NULL REFERENCES users(id), role TEXT NOT NULL, PRIMARY KEY(trip_id,user_id));
CREATE TABLE IF NOT EXISTS invites(code TEXT PRIMARY KEY, trip_id TEXT UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS operations(user_id TEXT NOT NULL, id TEXT NOT NULL, digest TEXT NOT NULL, result TEXT NOT NULL, PRIMARY KEY(user_id,id));
''')
        self.db.commit()

    def close(self):
        self.db.close()

    def register(self, body):
        name = text(body.get('name'), 'Imię', 80)
        email = text(body.get('email'), 'E-mail', 254).lower()
        password = text(body.get('password'), 'Hasło', 128)
        require(re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+', email), 'Nieprawidłowy e-mail.')
        require(len(password) >= 8, 'Hasło musi mieć co najmniej 8 znaków.')
        salt = secrets.token_hex(16)
        digest = hashlib.pbkdf2_hmac('sha256', password.encode(), bytes.fromhex(salt), 260000).hex()
        with self.lock, self.db:
            try:
                self.db.execute('INSERT INTO users VALUES(?,?,?,?,?)', (str(uuid.uuid4()), name, email, salt, digest))
            except sqlite3.IntegrityError:
                raise ApiError(409, 'Ten adres e-mail jest już zarejestrowany.')
        return self.login(body)

    def login(self, body):
        email = text(body.get('email'), 'E-mail', 254).lower()
        password = text(body.get('password'), 'Hasło', 128)
        with self.lock, self.db:
            user = self.db.execute('SELECT * FROM users WHERE email=?', (email,)).fetchone()
            salt = bytes.fromhex(user['salt']) if user else b'0000000000000000'
            digest = hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 260000).hex()
            require(user is not None and hmac.compare_digest(user['password'], digest), 'Nieprawidłowy e-mail lub hasło.', 401)
            token = secrets.token_urlsafe(48)
            self.db.execute('DELETE FROM sessions WHERE expires < ?', (int(time.time()),))
            self.db.execute('INSERT INTO sessions VALUES(?,?,?)', (hashlib.sha256(token.encode()).hexdigest(), user['id'], int(time.time()) + 604800))
            return {'token': token, 'user': {'id': user['id'], 'name': user['name'], 'email': user['email']}}

    def authenticate(self, token):
        with self.lock:
            row = self.db.execute('SELECT user_id FROM sessions WHERE token_hash=? AND expires>?', (hashlib.sha256(token.encode()).hexdigest(), int(time.time()))).fetchone()
            require(row is not None, 'Sesja wygasła. Zaloguj się ponownie.', 401)
            return row['user_id']

    def logout(self, token):
        with self.lock, self.db:
            self.db.execute('DELETE FROM sessions WHERE token_hash=?', (hashlib.sha256(token.encode()).hexdigest(),))
        return {'ok': True}

    def record(self, row):
        return {'id': row['id'], 'tripId': row['trip_id'], 'kind': row['kind'], 'payload': json.loads(row['payload']), 'version': row['version'], 'deleted': bool(row['deleted'])}

    def role(self, user, trip):
        row = self.db.execute('SELECT role FROM members WHERE trip_id=? AND user_id=?', (trip, user)).fetchone()
        return row['role'] if row else None

    def snapshot(self, user):
        with self.lock:
            trips = [r[0] for r in self.db.execute("SELECT m.trip_id FROM members m JOIN records r ON r.id=m.trip_id WHERE m.user_id=? AND r.deleted=0", (user,))]
            rows = []
            for trip in trips:
                rows.extend(self.record(r) for r in self.db.execute('SELECT * FROM records WHERE trip_id=? ORDER BY kind,id', (trip,)))
            return {'tripIds': trips, 'records': rows}

    def save_record(self, record_id, trip, kind, payload, version, deleted=False):
        self.db.execute('INSERT OR REPLACE INTO records VALUES(?,?,?,?,?,?)', (record_id, trip, kind, json.dumps(payload, ensure_ascii=False), version, int(deleted)))
        return self.record(self.db.execute('SELECT * FROM records WHERE id=?', (record_id,)).fetchone())

    def activity(self, trip, user, action, kind, title):
        actor = self.db.execute('SELECT name FROM users WHERE id=?', (user,)).fetchone()
        self.save_record(str(uuid.uuid4()), trip, 'activity', {'actorId': user, 'actorName': actor['name'], 'action': action, 'recordKind': kind, 'title': title, 'at': int(time.time() * 1000)}, 1)

    def member_record(self, trip, user_id, role):
        user = self.db.execute('SELECT * FROM users WHERE id=?', (user_id,)).fetchone()
        rid = f'{trip}:{user_id}'
        current = self.db.execute('SELECT version FROM records WHERE id=?', (rid,)).fetchone()
        return self.save_record(rid, trip, 'member', {'userId': user_id, 'name': user['name'], 'role': role}, (current['version'] if current else 0) + 1)

    def validate_payload(self, kind, payload, trip, user):
        require(isinstance(payload, dict), 'Nieprawidłowe dane rekordu.')
        if kind == 'trip':
            allowed = {'title', 'destination', 'startDate', 'endDate', 'currency', 'description'}
            text(payload.get('title'), 'Nazwa', 120)
            text(payload.get('destination'), 'Miejsce', 120)
            text(payload.get('description', ''), 'Opis', 2000, True)
            require(iso_date(payload.get('startDate')) <= iso_date(payload.get('endDate')), 'Koniec wyjazdu nie może poprzedzać początku.')
            require(payload.get('currency') in ['PLN', 'EUR', 'USD'], 'Nieobsługiwana waluta.')
            existing = self.db.execute("SELECT payload FROM records WHERE trip_id=? AND kind IN ('expense','settlement','budget') AND deleted=0", (trip,)).fetchone()
            previous = self.db.execute('SELECT payload FROM records WHERE id=?', (trip,)).fetchone()
            if existing and previous:
                require(json.loads(previous['payload'])['currency'] == payload['currency'], 'Nie można zmienić waluty po dodaniu wydatków.')
            events = self.db.execute("SELECT payload FROM records WHERE trip_id=? AND kind='event' AND deleted=0", (trip,)).fetchall()
            for event in events:
                dt = iso_date(json.loads(event['payload'])['date'])
                require(iso_date(payload['startDate']) <= dt <= iso_date(payload['endDate']), 'Nowe daty muszą obejmować punkty harmonogramu.')
        elif kind == 'event':
            allowed = {'title', 'date', 'time', 'place', 'description', 'done', 'rank', 'reminder'}
            text(payload.get('title'), 'Nazwa', 120)
            text(payload.get('place', ''), 'Miejsce', 240, True)
            text(payload.get('description', ''), 'Opis', 2000, True)
            day = iso_date(payload.get('date'))
            require(re.fullmatch(r'([01]\d|2[0-3]):[0-5]\d', str(payload.get('time'))), 'Nieprawidłowa godzina.')
            parent = json.loads(self.db.execute('SELECT payload FROM records WHERE id=?', (trip,)).fetchone()['payload'])
            require(iso_date(parent['startDate']) <= day <= iso_date(parent['endDate']), 'Punkt musi przypadać w terminie wyjazdu.')
            require(type(payload.get('rank')) is int and abs(payload['rank']) < 1000000000, 'Nieprawidłowa kolejność.')
            require(type(payload.get('done')) is bool and type(payload.get('reminder')) is bool, 'Nieprawidłowy status.')
        elif kind == 'task':
            allowed = {'title', 'category', 'assignee', 'done', 'date', 'time', 'reminder'}
            text(payload.get('title'), 'Nazwa', 120)
            require(payload.get('category') in ['task', 'packing'], 'Nieprawidłowa kategoria.')
            require(type(payload.get('done')) is bool and type(payload.get('reminder')) is bool, 'Nieprawidłowy status.')
            assignee = payload.get('assignee', '')
            require(not assignee or self.role(assignee, trip), 'Wybrana osoba nie należy do wyjazdu.')
            if payload.get('reminder'):
                iso_date(payload.get('date'))
                require(re.fullmatch(r'([01]\d|2[0-3]):[0-5]\d', str(payload.get('time'))), 'Nieprawidłowa godzina.')
        elif kind == 'proposal':
            allowed = {'title', 'place', 'description'}
            text(payload.get('title'), 'Nazwa', 120)
            text(payload.get('place', ''), 'Miejsce', 240, True)
            text(payload.get('description', ''), 'Opis', 2000, True)
        elif kind == 'vote':
            allowed = {'proposalId', 'userId'}
            require(payload.get('userId') == user, 'Możesz zarządzać tylko własnym głosem.', 403)
            proposal = self.db.execute("SELECT id FROM records WHERE id=? AND trip_id=? AND kind='proposal' AND deleted=0", (payload.get('proposalId'), trip)).fetchone()
            require(proposal is not None, 'Propozycja nie istnieje.')
        elif kind == 'expense':
            allowed = {'title', 'amount', 'payer', 'shares', 'date', 'category'}
            require(payload.get('category', 'other') in CATEGORIES, 'Nieprawidłowa kategoria wydatku.')
            text(payload.get('title'), 'Nazwa', 120)
            iso_date(payload.get('date'))
            amount = payload.get('amount')
            require(type(amount) is int and 0 < amount <= 100000000, 'Kwota musi być dodatnia i nie większa niż 1 000 000.')
            require(self.role(payload.get('payer'), trip), 'Płatnik nie należy do wyjazdu.')
            shares = payload.get('shares')
            require(isinstance(shares, dict) and 0 < len(shares) <= 100, 'Wybierz uczestników kosztu.')
            require(all(type(v) is int and v >= 0 and self.role(k, trip) for k, v in shares.items()), 'Nieprawidłowy podział kosztu.')
            require(sum(shares.values()) == amount, 'Suma udziałów musi być równa kwocie wydatku.')
        elif kind == 'budget':
            allowed = {'limit', 'categoryLimits'}
            require(type(payload.get('limit')) is int and 0 < payload['limit'] <= 100000000, 'Nieprawidłowy limit budżetu.')
            limits = payload.get('categoryLimits')
            require(isinstance(limits, dict) and set(limits) <= CATEGORIES, 'Nieprawidłowe kategorie budżetu.')
            require(all(type(v) is int and 0 <= v <= payload['limit'] for v in limits.values()), 'Limity kategorii muszą mieścić się w budżecie.')
            require(sum(limits.values()) <= payload['limit'], 'Suma limitów kategorii przekracza budżet.')
        elif kind == 'settlement':
            allowed = {'from', 'to', 'amount', 'date', 'note'}
            require(payload.get('from') == user, 'Możesz zapisać wyłącznie własną spłatę.', 403)
            require(payload.get('from') != payload.get('to') and self.role(payload.get('to'), trip), 'Wybierz innego uczestnika.')
            require(type(payload.get('amount')) is int and 0 < payload['amount'] <= 100000000, 'Nieprawidłowa kwota spłaty.')
            require(iso_date(payload.get('date')) <= date.today(), 'Spłata nie może być z przyszłości.')
            text(payload.get('note', ''), 'Notatka', 500, True)
        elif kind == 'place':
            allowed = {'title', 'latitude', 'longitude', 'description'}
            text(payload.get('title'), 'Nazwa', 120)
            text(payload.get('description', ''), 'Opis', 2000, True)
            require(type(payload.get('latitude')) in (int, float) and -90 <= payload['latitude'] <= 90, 'Nieprawidłowa szerokość geograficzna.')
            require(type(payload.get('longitude')) in (int, float) and -180 <= payload['longitude'] <= 180, 'Nieprawidłowa długość geograficzna.')
        elif kind == 'member':
            allowed = {'userId', 'name', 'role'}
        else:
            raise ApiError(400, 'Nieznany rodzaj rekordu.')
        require(set(payload) <= allowed, 'Dane zawierają nieobsługiwane pola.')

    def apply(self, user, op):
        require(isinstance(op, dict), 'Nieprawidłowa operacja.')
        oid = text(op.get('id'), 'Identyfikator operacji', 100)
        rid = text(op.get('recordId'), 'Identyfikator rekordu', 150)
        trip = text(op.get('tripId'), 'Identyfikator wyjazdu', 100)
        kind = op.get('kind')
        require(kind in ['trip', 'event', 'task', 'proposal', 'vote', 'expense', 'member', 'budget', 'settlement', 'place'], 'Nieznany rodzaj rekordu.')
        base = op.get('baseVersion')
        deleted = op.get('deleted', False)
        require(type(base) is int and base >= 0 and type(deleted) is bool, 'Nieprawidłowa wersja.')
        digest = hashlib.sha256(json.dumps(op, sort_keys=True).encode()).hexdigest()
        with self.lock, self.db:
            known = self.db.execute('SELECT digest,result FROM operations WHERE user_id=? AND id=?', (user, oid)).fetchone()
            if known:
                require(known['digest'] == digest, 'Identyfikator operacji został użyty z innymi danymi.', 409)
                return json.loads(known['result'])
            current = self.db.execute('SELECT * FROM records WHERE id=?', (rid,)).fetchone()
            if current:
                require(current['trip_id'] == trip and current['kind'] == kind, 'Nie można zmienić tożsamości rekordu.')
            role = self.role(user, trip)
            creating = kind == 'trip' and not current and base == 0 and not deleted and rid == trip
            parent = self.db.execute('SELECT deleted FROM records WHERE id=?', (trip,)).fetchone()
            if not creating:
                require(role is not None and parent is not None and not parent['deleted'], 'Brak dostępu do wyjazdu.', 403)
                if kind in ['trip', 'member', 'budget']:
                    require(role == 'owner', 'Ta operacja wymaga roli organizatora.', 403)
            if (current['version'] if current else 0) != base:
                return {'id': oid, 'status': 'conflict', 'record': self.record(current) if current else None}
            if current and current['deleted'] and not deleted and kind not in ('vote', 'budget'):
                raise ApiError(409, 'Usuniętego rekordu nie można przywrócić.')
            payload = op.get('payload')
            require(isinstance(payload, dict), 'Nieprawidłowe dane.')
            if kind == 'budget':
                require(rid == f'budget:{trip}', 'Budżet musi mieć stały identyfikator.')
            if kind == 'settlement':
                original = json.loads(current['payload']) if current else payload
                require(original.get('from') == user and payload.get('from') == user, 'Możesz zarządzać wyłącznie własnymi spłatami.', 403)
            if kind == 'vote':
                require(payload.get('userId') == user and rid == f"{payload.get('proposalId')}:{user}", 'Nieprawidłowy głos.', 403)
            if kind == 'member':
                require(deleted and current, 'Uczestników dodaje się przez zaproszenie.')
                target = json.loads(current['payload'])['userId']
                require(self.role(target, trip) != 'owner', 'Nie można usunąć organizatora.')
                expenses = self.db.execute("SELECT payload FROM records WHERE trip_id=? AND kind='expense' AND deleted=0", (trip,)).fetchall()
                require(not any(target == (e := json.loads(r['payload']))['payer'] or target in e['shares'] for r in expenses), 'Najpierw usuń lub zmień wydatki dotyczące tej osoby.')
                settlements = self.db.execute("SELECT payload FROM records WHERE trip_id=? AND kind='settlement' AND deleted=0", (trip,)).fetchall()
                require(not any(target in (json.loads(r['payload'])['from'], json.loads(r['payload'])['to']) for r in settlements), 'Najpierw usuń spłaty dotyczące tej osoby.')
                self.db.execute('DELETE FROM members WHERE trip_id=? AND user_id=?', (trip, target))
                for row in self.db.execute("SELECT * FROM records WHERE trip_id=? AND kind IN ('task','vote') AND deleted=0", (trip,)).fetchall():
                    data = json.loads(row['payload'])
                    if row['kind'] == 'task' and data.get('assignee') == target:
                        data['assignee'] = ''
                        self.save_record(row['id'], trip, 'task', data, row['version'] + 1)
                    elif row['kind'] == 'vote' and data.get('userId') == target:
                        self.save_record(row['id'], trip, 'vote', data, row['version'] + 1, True)
            if not deleted:
                self.validate_payload(kind, payload, trip, user)
            record = self.save_record(rid, trip, kind, payload, base + 1, deleted)
            if creating:
                self.db.execute('INSERT INTO members VALUES(?,?,?)', (trip, user, 'owner'))
                self.member_record(trip, user, 'owner')
            if deleted and kind == 'proposal':
                for vote in self.db.execute("SELECT * FROM records WHERE trip_id=? AND kind='vote' AND deleted=0", (trip,)).fetchall():
                    if json.loads(vote['payload'])['proposalId'] == rid:
                        self.save_record(vote['id'], trip, 'vote', json.loads(vote['payload']), vote['version'] + 1, True)
            if deleted and kind == 'trip':
                self.db.execute('DELETE FROM invites WHERE trip_id=?', (trip,))
                self.db.execute('UPDATE records SET deleted=1,version=version+1 WHERE trip_id=? AND id<>?', (trip, rid))
            if not (kind == 'trip' and deleted):
                original = json.loads(current['payload']) if current else payload
                self.activity(trip, user, 'delete' if deleted else 'update' if current else 'create', kind, (original if deleted else payload).get('title', '') or kind)
            result = {'id': oid, 'status': 'applied', 'record': record}
            self.db.execute('INSERT INTO operations VALUES(?,?,?,?)', (user, oid, digest, json.dumps(result)))
            return result

    def invitation(self, user, trip, rotate=False):
        with self.lock, self.db:
            require(self.role(user, trip) == 'owner', 'Tylko organizator udostępnia zaproszenia.', 403)
            row = self.db.execute('SELECT deleted FROM records WHERE id=?', (trip,)).fetchone()
            require(row and not row['deleted'], 'Wyjazd nie istnieje.', 404)
            current = self.db.execute('SELECT code FROM invites WHERE trip_id=?', (trip,)).fetchone()
            if rotate or not current:
                self.db.execute('DELETE FROM invites WHERE trip_id=?', (trip,))
                code = secrets.token_hex(6).upper()
                self.db.execute('INSERT INTO invites VALUES(?,?)', (code, trip))
                self.activity(trip, user, 'invite', 'member', 'Utworzenie lub odnowienie zaproszenia')
            else:
                code = current['code']
            return {'code': code, 'tripId': trip}

    def join(self, user, code):
        with self.lock, self.db:
            row = self.db.execute('SELECT i.trip_id FROM invites i JOIN records r ON r.id=i.trip_id WHERE i.code=? AND r.deleted=0', (str(code).strip().upper(),)).fetchone()
            require(row is not None, 'Kod zaproszenia jest nieprawidłowy lub wygasł.', 404)
            trip = row['trip_id']
            count = self.db.execute('SELECT COUNT(*) FROM members WHERE trip_id=?', (trip,)).fetchone()[0]
            require(count < 100 or self.role(user, trip), 'Wyjazd osiągnął limit 100 osób.')
            if not self.role(user, trip):
                self.db.execute('INSERT INTO members VALUES(?,?,?)', (trip, user, 'participant'))
                self.member_record(trip, user, 'participant')
                self.activity(trip, user, 'join', 'member', 'Dołączenie do grupy')
            return {'tripId': trip}


class Handler(BaseHTTPRequestHandler):
    server_version = 'TripSync/1.1'

    def setup(self):
        super().setup()
        self.connection.settimeout(15)

    def log_message(self, fmt, *args):
        return

    def respond(self, status, data):
        encoded = json.dumps(data, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(encoded)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.end_headers()
        self.wfile.write(encoded)

    def body(self):
        try:
            size = int(self.headers.get('Content-Length', '0'))
            require(0 < size <= 262144, 'Nieprawidłowy rozmiar żądania.', 413)
            data = json.loads(self.rfile.read(size))
            require(isinstance(data, dict), 'Oczekiwano obiektu JSON.')
            return data
        except (ValueError, UnicodeDecodeError):
            raise ApiError(400, 'Nieprawidłowy JSON.')

    def route(self, method):
        try:
            path = urlparse(self.path).path.rstrip('/')
            store = self.server.store
            if method == 'GET' and path == '/health':
                return self.respond(200, {'status': 'ok', 'version': '1.1.0'})
            if method == 'POST' and path in ['/auth/register', '/auth/login']:
                with self.server.rate_lock:
                    now = time.time()
                    key = self.client_address[0]
                    if key in self.server.trusted_proxies:
                        forwarded = self.headers.get('X-Forwarded-For', '')
                        try:
                            key = str(ipaddress.ip_address(forwarded))
                        except ValueError:
                            raise ApiError(400, 'Nieprawidłowy adres klienta proxy.')
                    attempts = [t for t in self.server.attempts.get(key, []) if now-t < 300]
                    require(len(attempts) < 30, 'Zbyt wiele prób. Spróbuj za kilka minut.', 429)
                    attempts.append(now)
                    self.server.attempts[key] = attempts
                return self.respond(200, store.register(self.body()) if path.endswith('register') else store.login(self.body()))
            auth = self.headers.get('Authorization', '')
            require(auth.startswith('Bearer '), 'Zaloguj się, aby kontynuować.', 401)
            token = auth[7:]
            user = store.authenticate(token)
            if method == 'POST' and path == '/auth/logout':
                return self.respond(200, store.logout(token))
            if method == 'GET' and path == '/sync':
                return self.respond(200, store.snapshot(user))
            if method == 'POST' and path == '/operations':
                operations = self.body().get('operations')
                require(isinstance(operations, list) and 0 < len(operations) <= 100, 'Oczekiwano od 1 do 100 operacji.')
                results = []
                for op in operations:
                    try:
                        results.append(store.apply(user, op))
                    except ApiError as err:
                        results.append({'id': op.get('id', '') if isinstance(op, dict) else '', 'status': 'forbidden' if err.status == 403 else 'invalid', 'message': err.message})
                return self.respond(200, {'results': results})
            if method == 'POST' and path == '/join':
                return self.respond(200, store.join(user, self.body().get('code')))
            match = re.fullmatch(r'/trips/([a-zA-Z0-9-]+)/invite', path)
            if match and method in ['GET', 'POST']:
                return self.respond(200, store.invitation(user, match.group(1), method == 'POST'))
            raise ApiError(404, 'Nie znaleziono zasobu.')
        except ApiError as err:
            self.respond(err.status, {'message': err.message})
        except (sqlite3.Error, OSError):
            self.respond(503, {'message': 'Serwer chwilowo niedostępny. Spróbuj ponownie.'})
        except Exception:
            self.respond(500, {'message': 'Nie udało się przetworzyć żądania.'})

    def do_GET(self):
        self.route('GET')

    def do_POST(self):
        self.route('POST')


def make_server(store, host='127.0.0.1', port=8080, trusted_proxies=()):
    server = ThreadingHTTPServer((host, port), Handler)
    server.store = store
    server.trusted_proxies = set(trusted_proxies)
    server.attempts = {}
    server.rate_lock = threading.Lock()
    return server


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8080)
    parser.add_argument('--db', default=str(Path(__file__).with_name('tripsync.sqlite')))
    parser.add_argument('--trusted-proxy', action='append', default=[])
    parser.add_argument('--cert')
    parser.add_argument('--key')
    args = parser.parse_args()
    if bool(args.cert) != bool(args.key):
        parser.error('Podaj jednocześnie --cert i --key.')
    store = Store(args.db)
    server = make_server(store, args.host, args.port, args.trusted_proxy)
    if args.cert:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2
        ctx.load_cert_chain(args.cert, args.key)
        server.socket = ctx.wrap_socket(server.socket, server_side=True)
    print(f'TripSync API: {"https" if args.cert else "http"}://{args.host}:{server.server_port}', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        store.close()


if __name__ == '__main__':
    main()
