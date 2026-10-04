import io
import json
import os
import tempfile
import threading
import unittest
from pathlib import Path
from urllib.request import Request, urlopen
from wsgiref.simple_server import make_server


class WsgiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        os.environ['TRIPSYNC_DB'] = str(Path(cls.tmp.name) / 'import.sqlite')
        import wsgi
        cls.module = wsgi
        cls.app = staticmethod(wsgi.create_application(str(Path(cls.tmp.name) / 'test.sqlite')))

    @classmethod
    def tearDownClass(cls):
        cls.app.store.close()
        cls.module.application.store.close()
        os.environ.pop('TRIPSYNC_DB', None)
        cls.tmp.cleanup()

    def test_wsgi_real_http_accounts_sync_and_persistence(self):
        server = make_server('127.0.0.1', 0, self.app)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f'http://127.0.0.1:{server.server_port}'
        try:
            self.assertEqual('1.1.0', json.load(urlopen(base+'/health'))['version'])
            request = Request(base+'/auth/register', data=json.dumps({'name': 'Tester', 'email': 'wsgi@example.org', 'password': 'Secret123!'}).encode(), headers={'Content-Type': 'application/json'})
            account = json.load(urlopen(request))
            snapshot = json.load(urlopen(Request(base+'/sync', headers={'Authorization': 'Bearer '+account['token']})))
            self.assertEqual([], snapshot['tripIds'])
            self.assertEqual(account['user']['id'], self.app.store.authenticate(account['token']))
        finally:
            server.shutdown()
            server.server_close()
            thread.join()

    def test_wsgi_invalid_body_and_unauthorized(self):
        statuses = []
        def response(status, headers):
            statuses.append(status)
        body = self.app({'PATH_INFO': '/sync', 'REQUEST_METHOD': 'GET', 'wsgi.input': io.BytesIO()}, response)
        self.assertTrue(statuses[-1].startswith('401'))
        self.assertIn('message', json.loads(body[0]))
        self.app({'PATH_INFO': '/auth/register', 'REQUEST_METHOD': 'POST', 'CONTENT_LENGTH': '7', 'REMOTE_ADDR': 'test-client', 'wsgi.input': io.BytesIO(b'{broken')}, response)
        self.assertTrue(statuses[-1].startswith('400'))
