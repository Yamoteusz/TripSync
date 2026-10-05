import sqlite3
import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock

from mysql_database import MySQLDatabase, Result, SCHEMA, translate
from server import Store


class DriverError(Exception):
    pass


class IntegrityError(DriverError):
    pass


class MySQLDatabaseTests(unittest.TestCase):
    def setUp(self):
        self.connection = MagicMock()
        self.cursor = self.connection.cursor.return_value.__enter__.return_value
        self.cursor.description = None
        self.driver = SimpleNamespace(Error=DriverError, IntegrityError=IntegrityError, cursors=SimpleNamespace(DictCursor=object()), connect=MagicMock(return_value=self.connection))
        self.environment = {'TRIPSYNC_MYSQL_HOST': 'localhost', 'TRIPSYNC_MYSQL_DATABASE': 'test', 'TRIPSYNC_MYSQL_USER': 'test', 'TRIPSYNC_MYSQL_PASSWORD': 'private-test-password'}
        self.database = MySQLDatabase(self.environment, self.driver)
        self.connection.reset_mock()

    def test_missing_password_fails_without_connection_or_secret_output(self):
        self.driver.connect.reset_mock()
        environment = dict(self.environment)
        del environment['TRIPSYNC_MYSQL_PASSWORD']
        with self.assertRaisesRegex(ValueError, 'TRIPSYNC_MYSQL_PASSWORD'):
            MySQLDatabase(environment, self.driver)
        self.driver.connect.assert_not_called()

    def test_transaction_commits_success_and_rolls_back_failure(self):
        with self.database:
            self.database.execute('DELETE FROM sessions WHERE token_hash=?', ('secret',))
        self.cursor.execute.assert_called_with('DELETE FROM sessions WHERE token_hash=%s', ('secret',))
        self.connection.commit.assert_called_once()
        self.connection.rollback.assert_not_called()
        with self.assertRaises(ValueError):
            with self.database:
                raise ValueError('invalid')
        self.connection.rollback.assert_called_once()
        self.assertFalse(self.database.in_transaction)

    def test_integrity_error_preserves_existing_registration_handler(self):
        self.cursor.execute.side_effect = IntegrityError('private database details')
        with self.assertRaisesRegex(sqlite3.IntegrityError, '^Konflikt danych MySQL.$'):
            self.database.execute('INSERT INTO users VALUES(?,?,?,?,?)', ('1', 'A', 'a@a.pl', 'salt', 'hash'))

    def test_connection_recovers_only_outside_transaction(self):
        self.connection.ping.side_effect = DriverError('disconnected')
        self.database.execute('SELECT * FROM users')
        self.assertEqual(2, self.driver.connect.call_count)
        self.connection.ping.reset_mock(side_effect=True)
        with self.database:
            self.connection.ping.reset_mock()
            self.database.execute('SELECT * FROM users')
            self.connection.ping.assert_not_called()

    def test_no_retry_of_failed_write_in_transaction(self):
        self.cursor.execute.side_effect = DriverError('lost connection')
        with self.assertRaises(sqlite3.OperationalError):
            with self.database:
                self.database.execute('DELETE FROM sessions')
        self.connection.rollback.assert_called_once()
        self.assertEqual(1, self.driver.connect.call_count)

    def test_snapshot_supports_named_and_positional_row_access(self):
        self.cursor.description = True
        self.cursor.fetchall.return_value = [{'trip_id': 'trip-1'}]
        store = Store(database=self.database)
        result = self.database.execute('SELECT trip_id FROM members WHERE user_id=?', ('user-1',))
        row = result.fetchone()
        self.assertEqual('trip-1', row[0])
        self.assertEqual('trip-1', row['trip_id'])
        self.assertIsNone(result.fetchone())
        self.assertIs(store.db, self.database)

    def test_upsert_updates_record_without_replace_delete_semantics(self):
        query = translate('INSERT OR REPLACE INTO records VALUES(?,?,?,?,?,?)')
        self.assertIn('ON DUPLICATE KEY UPDATE', query)
        self.assertNotIn('REPLACE', query)
        self.assertEqual(6, query.count('%s'))
        self.assertEqual(6, len(SCHEMA))


if __name__ == '__main__':
    unittest.main()
