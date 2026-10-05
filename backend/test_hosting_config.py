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
            path.write_text('TRIPSYNC_MYSQL_HOST=localhost\nSECRET_PASSWORD=private-value\n', encoding='utf-8')
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
