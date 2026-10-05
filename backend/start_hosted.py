import ipaddress
import os
from pathlib import Path

from server import Store, make_server
from hosting_config import load_hosting_config


def main():
    load_hosting_config()
    port = int(os.environ.get('PORT') or os.environ.get('SERVER_PORT') or '9853')
    if not 1 <= port <= 65535:
        raise ValueError('Port musi być w zakresie 1–65535.')
    proxies = [str(ipaddress.ip_address(value.strip())) for value in os.environ.get('TRIPSYNC_TRUSTED_PROXIES', '').split(',') if value.strip()]
    engine = os.environ.get('TRIPSYNC_DATABASE_ENGINE', 'sqlite').lower()
    if engine == 'mysql':
        from mysql_database import MySQLDatabase
        store = Store(database=MySQLDatabase())
    elif engine == 'sqlite':
        database = Path(os.environ.get('TRIPSYNC_DB', 'data/tripsync.sqlite')).resolve()
        database.parent.mkdir(parents=True, exist_ok=True)
        store = Store(str(database))
    else:
        raise ValueError('TRIPSYNC_DATABASE_ENGINE musi być sqlite lub mysql.')
    try:
        server = make_server(store, '0.0.0.0', port, proxies)
        try:
            print(f'TripSync API na porcie {server.server_port}', flush=True)
            server.serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            server.server_close()
    finally:
        store.close()


if __name__ == '__main__':
    main()
