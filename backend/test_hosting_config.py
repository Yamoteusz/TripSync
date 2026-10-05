import tempfile
import unittest
from pathlib import Path

from hosting_config import load_hosting_config


class HostingConfigTests(unittest.TestCase):
    def test_password_symbols_are_literal_and_environment_wins(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            path.write_text('TRIPSYNC_MYSQL_PASSWORD=a#b=c$"\\z\nPORT=9853\n', encoding='utf-8')
            environment = {'PORT': '8000'}
            load_hosting_config(path, environment)
            self.assertEqual('a#b=c$"\\z', environment['TRIPSYNC_MYSQL_PASSWORD'])
            self.assertEqual('8000', environment['PORT'])

    def test_invalid_line_never_discloses_secret_or_partially_applies(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            path.write_text('TRIPSYNC_MYSQL_HOST=localhost\nINVALID KEY=private-value\n', encoding='utf-8')
            environment = {}
            with self.assertRaises(ValueError) as error:
                load_hosting_config(path, environment)
            self.assertNotIn('private-value', str(error.exception))
            self.assertEqual({}, environment)

    def test_missing_file_leaves_environment_unchanged(self):
        with tempfile.TemporaryDirectory() as directory:
            environment = {'PORT': '8000'}
            load_hosting_config(Path(directory) / '.env', environment)
            self.assertEqual({'PORT': '8000'}, environment)

    def test_hosting_variables_bom_and_export_do_not_block_config(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            path.write_text('\ufeffREQUIREMENTS_FILE=requirements.txt\nexport TRIPSYNC_MYSQL_HOST=database\nPYTHONPATH=/private/hosting\n', encoding='utf-8')
            environment = {}
            load_hosting_config(path, environment)
            self.assertEqual({'TRIPSYNC_MYSQL_HOST': 'database'}, environment)

    def test_complete_mysql_environment_ignores_invalid_file(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / '.env'
            path.write_text('invalid private configuration\n', encoding='utf-8')
            environment = {'TRIPSYNC_DATABASE_ENGINE': 'mysql', 'TRIPSYNC_MYSQL_HOST': 'host', 'TRIPSYNC_MYSQL_DATABASE': 'database', 'TRIPSYNC_MYSQL_USER': 'user', 'TRIPSYNC_MYSQL_PASSWORD': 'private'}
            expected = dict(environment)
            load_hosting_config(path, environment)
            self.assertEqual(expected, environment)
