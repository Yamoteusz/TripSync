import os
import sqlite3
import ssl


SCHEMA = (
    'CREATE TABLE IF NOT EXISTS users(id VARCHAR(100) PRIMARY KEY, name VARCHAR(80) NOT NULL, email VARCHAR(254) UNIQUE NOT NULL, salt VARCHAR(32) NOT NULL, password VARCHAR(64) NOT NULL)',
    'CREATE TABLE IF NOT EXISTS sessions(token_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(100) NOT NULL, expires BIGINT NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id))',
    'CREATE TABLE IF NOT EXISTS records(id VARCHAR(201) PRIMARY KEY, trip_id VARCHAR(100) NOT NULL, kind VARCHAR(32) NOT NULL, payload LONGTEXT NOT NULL, version BIGINT NOT NULL, deleted TINYINT NOT NULL DEFAULT 0, INDEX records_trip(trip_id))',
    'CREATE TABLE IF NOT EXISTS members(trip_id VARCHAR(100) NOT NULL, user_id VARCHAR(100) NOT NULL, role VARCHAR(32) NOT NULL, PRIMARY KEY(trip_id,user_id), FOREIGN KEY(user_id) REFERENCES users(id))',
    'CREATE TABLE IF NOT EXISTS invites(code VARCHAR(100) PRIMARY KEY, trip_id VARCHAR(100) UNIQUE NOT NULL)',
    'CREATE TABLE IF NOT EXISTS operations(user_id VARCHAR(100) NOT NULL, id VARCHAR(100) NOT NULL, digest VARCHAR(64) NOT NULL, result LONGTEXT NOT NULL, PRIMARY KEY(user_id,id))',
)


class Row(dict):
    def __getitem__(self, key):
        return list(self.values())[key] if isinstance(key, int) else super().__getitem__(key)


class Result:
    def __init__(self, rows):
        self.rows = [Row(row) for row in rows]

    def fetchone(self):
        return self.rows.pop(0) if self.rows else None

    def fetchall(self):
        rows, self.rows = self.rows, []
        return rows

    def __iter__(self):
        return iter(self.fetchall())


def translate(query):
    if query.startswith('INSERT OR REPLACE INTO records VALUES'):
        return 'INSERT INTO records VALUES(%s,%s,%s,%s,%s,%s) ON DUPLICATE KEY UPDATE trip_id=VALUES(trip_id), kind=VALUES(kind), payload=VALUES(payload), version=VALUES(version), deleted=VALUES(deleted)'
    return query.replace('?', '%s')


class MySQLDatabase:
    def __init__(self, environment=None, driver=None):
        if driver is None:
            import pymysql
            driver = pymysql
        self.driver = driver
        env = os.environ if environment is None else environment
        required = ['TRIPSYNC_MYSQL_HOST', 'TRIPSYNC_MYSQL_DATABASE', 'TRIPSYNC_MYSQL_USER', 'TRIPSYNC_MYSQL_PASSWORD']
        missing = [name for name in required if not env.get(name)]
        if missing:
            raise ValueError('Brak zmiennych: ' + ', '.join(missing))
        self.options = dict(host=env[required[0]], database=env[required[1]], user=env[required[2]], password=env[required[3]], port=int(env.get('TRIPSYNC_MYSQL_PORT', '3306')), charset='utf8mb4', cursorclass=driver.cursors.DictCursor, autocommit=True, connect_timeout=10, read_timeout=15, write_timeout=15)
        if env.get('TRIPSYNC_MYSQL_SSL_CA'):
            self.options['ssl'] = ssl.create_default_context(cafile=env['TRIPSYNC_MYSQL_SSL_CA'])
        self.in_transaction = False
        self.connection = self._connect()
        try:
            for statement in SCHEMA:
                self.execute(statement + ' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin')
        except Exception:
            self.close()
            raise

    def _connect(self):
        try:
            connection = self.driver.connect(**self.options)
            if 'ssl' in self.options and not isinstance(connection._sock, ssl.SSLSocket):
                connection.close()
                raise sqlite3.OperationalError('Serwer MySQL nie zestawił wymaganego TLS.')
            return connection
        except self.driver.Error:
            raise sqlite3.OperationalError('Nie udało się połączyć z bazą MySQL.') from None

    def _ready(self):
        if not self.in_transaction:
            try:
                self.connection.ping(reconnect=False)
            except self.driver.Error:
                try:
                    self.connection.close()
                except self.driver.Error:
                    pass
                self.connection = self._connect()

    def execute(self, query, parameters=()):
        self._ready()
        try:
            with self.connection.cursor() as cursor:
                cursor.execute(translate(query), parameters)
                return Result(cursor.fetchall()) if cursor.description else Result([])
        except self.driver.IntegrityError:
            raise sqlite3.IntegrityError('Konflikt danych MySQL.') from None
        except self.driver.Error:
            raise sqlite3.OperationalError('Błąd operacji MySQL.') from None

    def __enter__(self):
        self._ready()
        try:
            self.connection.begin()
        except self.driver.Error:
            raise sqlite3.OperationalError('Nie można rozpocząć transakcji MySQL.') from None
        self.in_transaction = True
        return self

    def __exit__(self, exception_type, exception, traceback):
        try:
            if exception_type is None:
                self.connection.commit()
            else:
                self.connection.rollback()
        except self.driver.Error:
            try:
                self.connection.close()
            except self.driver.Error:
                pass
            raise sqlite3.OperationalError('Błąd zakończenia transakcji MySQL.') from None
        finally:
            self.in_transaction = False

    def close(self):
        self.connection.close()
