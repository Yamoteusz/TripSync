import json
import os
import threading
from http import HTTPStatus
from pathlib import Path
from types import SimpleNamespace
from server import Handler, Store


class WsgiHandler(Handler):
    def __init__(self, environment, service):
        self.server = service
        self.path = environment.get('PATH_INFO', '/')
        self.client_address = (environment.get('REMOTE_ADDR', 'unknown'), 0)
        self.rfile = environment['wsgi.input']
        self.headers = {'Content-Length': environment.get('CONTENT_LENGTH') or '0', 'Authorization': environment.get('HTTP_AUTHORIZATION', '')}
        self.response = None

    def respond(self, status, data):
        encoded = json.dumps(data, ensure_ascii=False).encode('utf-8')
        self.response = (f'{status} {HTTPStatus(status).phrase}', [('Content-Type', 'application/json; charset=utf-8'), ('Content-Length', str(len(encoded))), ('Cache-Control', 'no-store'), ('X-Content-Type-Options', 'nosniff')], [encoded])


def create_application(path):
    service = SimpleNamespace(store=Store(path), trusted_proxies=set(), attempts={}, rate_lock=threading.Lock())
    def application(environment, start_response):
        handler = WsgiHandler(environment, service)
        handler.route(environment.get('REQUEST_METHOD', 'GET'))
        status, headers, content = handler.response
        start_response(status, headers)
        return content
    application.store = service.store
    return application


application = create_application(os.environ.get('TRIPSYNC_DB', str(Path(__file__).with_name('tripsync.sqlite'))))
