import unittest
from server import ApiError, AuthRateLimiter


class RateLimiterTests(unittest.TestCase):
    def test_rejects_excess_attempts_without_growing_history(self):
        limiter = AuthRateLimiter(limit=2)
        limiter.check('client', now=10)
        limiter.check('client', now=11)
        for _ in range(20):
            with self.assertRaises(ApiError) as error:
                limiter.check('client', now=12)
            self.assertEqual(429, error.exception.status)
        self.assertEqual(2, len(limiter.clients['client']))
        limiter.check('client', now=311)
        self.assertEqual([311], limiter.clients['client'])

    def test_caps_clients_and_keeps_active_rate_limits(self):
        limiter = AuthRateLimiter(limit=1, max_clients=2)
        limiter.check('first', now=10)
        limiter.check('second', now=11)
        with self.assertRaises(ApiError):
            limiter.check('third', now=12)
        with self.assertRaises(ApiError):
            limiter.check('first', now=13)
        self.assertEqual({'first', 'second'}, set(limiter.clients))

    def test_expired_clients_are_removed_and_capacity_is_reused(self):
        limiter = AuthRateLimiter(max_clients=2)
        limiter.check('old', now=0)
        limiter.check('active', now=250)
        limiter.check('new', now=300)
        self.assertEqual({'active', 'new'}, set(limiter.clients))
        self.assertEqual([250], limiter.clients['active'])
        limiter.check('later', now=601)
        self.assertEqual({'later'}, set(limiter.clients))
