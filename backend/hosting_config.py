import os
import re
from pathlib import Path


ALLOWED = {
    'TRIPSYNC_DATABASE_ENGINE', 'TRIPSYNC_DB', 'TRIPSYNC_TRUSTED_PROXIES',
    'TRIPSYNC_MYSQL_HOST', 'TRIPSYNC_MYSQL_PORT', 'TRIPSYNC_MYSQL_DATABASE',
    'TRIPSYNC_MYSQL_USER', 'TRIPSYNC_MYSQL_PASSWORD', 'TRIPSYNC_MYSQL_SSL_CA',
    'PORT', 'SERVER_PORT',
}


def load_hosting_config(path=None, environment=None):
    path = Path(path) if path is not None else Path(__file__).resolve().parent.parent / '.env'
    environment = os.environ if environment is None else environment
    mysql_required = ('TRIPSYNC_MYSQL_HOST', 'TRIPSYNC_MYSQL_DATABASE', 'TRIPSYNC_MYSQL_USER', 'TRIPSYNC_MYSQL_PASSWORD')
    if environment.get('TRIPSYNC_DATABASE_ENGINE', '').lower() == 'mysql' and all(environment.get(key) for key in mysql_required):
        return
    if not path.is_file():
        return
    values = {}
    for number, line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(), 1):
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        line = line.lstrip()
        if line.startswith('export '):
            line = line[7:]
        key, separator, value = line.partition('=')
        key = key.strip()
        if not separator or not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', key):
            raise ValueError(f'Nieprawidłowa konfiguracja .env w wierszu {number}.')
        if key not in ALLOWED or key in environment:
            continue
        if key in values:
            raise ValueError(f'Powtórzona zmienna .env w wierszu {number}.')
        values[key] = value
    for key, value in values.items():
        if key not in environment:
            environment[key] = value
