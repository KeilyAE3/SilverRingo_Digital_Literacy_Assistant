"""Regression coverage for the rate limiter already deployed on DHub."""
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'SilverRingo_Chat-bot/src'))
from fastapi.testclient import TestClient
from silver_ringo_d.api import create_app, SlidingWindowRateLimiter
from silver_ringo_d.llm import FakeLLM
from Tester.tests.test_d_chat_service import service


class RateLimitTests(unittest.TestCase):
    def test_per_ip_limit_returns_429_and_health_remains_available(self):
        """VM 기존 정책: 같은 IP의 초과 요청은 429이며 health는 제한하지 않는다."""
        app = create_app(service(FakeLLM()), rate_limit_per_minute=1,
                         rate_limit_window_seconds=60, global_rate_limit_per_minute=10)
        with TestClient(app) as client:
            self.assertEqual(client.post('/v1/chat', json={'message': '안녕하세요'}).status_code, 200)
            response = client.post('/v1/chat', json={'message': '다시 요청'})
            self.assertEqual(response.status_code, 429)
            self.assertEqual(response.json()['error'], 'rate_limited')
            self.assertEqual(client.get('/health').status_code, 200)

    def test_global_limit_blocks_even_below_per_ip_limit(self):
        """전체 한도는 IP별 한도가 남아 있어도 429로 차단한다."""
        app = create_app(service(FakeLLM()), rate_limit_per_minute=10,
                         rate_limit_window_seconds=60, global_rate_limit_per_minute=1)
        with TestClient(app) as client:
            self.assertEqual(client.post('/v1/chat', json={'message': '와이파이 연결 방법'}).status_code, 200)
            response = client.post('/v1/chat', json={'message': '사진 보내는 방법'})
            self.assertEqual(response.status_code, 429)
            self.assertEqual(response.json()['error'], 'rate_limited')

    def test_sliding_window_expiration_and_client_isolation(self):
        """시간 창이 만료되면 재허용하며 서로 다른 IP의 버킷은 분리한다."""
        limiter = SlidingWindowRateLimiter(1, 60, 1)
        with patch('silver_ringo_d.api.time.monotonic', return_value=100):
            self.assertTrue(limiter.allow_ip('client-a'))
            self.assertFalse(limiter.allow_ip('client-a'))
            self.assertTrue(limiter.allow_ip('client-b'))
            self.assertTrue(limiter.allow_global())
            self.assertFalse(limiter.allow_global())
        with patch('silver_ringo_d.api.time.monotonic', return_value=160):
            self.assertTrue(limiter.allow_ip('client-a'))
            self.assertTrue(limiter.allow_global())
