"""Lazy optional google-genai boundary; never constructs a client at import."""
from copy import deepcopy
import time
from .config import Settings
from .llm import LLMError, prepare_request, validate_response, FakeLLM
from .system_prompt import SYSTEM_INSTRUCTION, RESPONSE_SCHEMA


class GeminiAdapter:
    def __init__(self, settings: Settings, *, client=None, sleep=time.sleep,
                 timeout_ms=15000, max_attempts=3):
        if settings.llm_mode != 'gemini' or not settings.gemini_api_key.strip() or not settings.gemini_model.strip():
            raise LLMError('configuration')
        if type(timeout_ms) is not int or not 1000 <= timeout_ms <= 60000:
            raise ValueError('timeout_ms must be in 1000..60000')
        if type(max_attempts) is not int or not 1 <= max_attempts <= 3:
            raise ValueError('max_attempts must be in 1..3')
        self._settings = settings
        self._client = client
        self._owns_client = client is None
        self._sleep = sleep
        self._timeout_ms = timeout_ms
        self._max_attempts = max_attempts

    def _get_client(self):
        if self._client is None:
            try:
                from google import genai
                self._client = genai.Client(api_key=self._settings.gemini_api_key,
                    http_options={'api_version': 'v1beta', 'timeout': self._timeout_ms,
                                  'retry_options': {'attempts': 1}})
            except Exception:
                raise LLMError('configuration') from None
        return self._client

    def close(self):
        if self._owns_client and self._client is not None:
            try:
                self._client.close()
            except Exception:
                pass  # Never expose transport diagnostics or credentials.
            finally:
                self._client = None

    def generate(self, question, context, history):
        request = prepare_request(question, context, history, secret=self._settings.gemini_api_key)
        if request.fallback:
            return request.fallback
        client = self._get_client()
        config = {'system_instruction': SYSTEM_INSTRUCTION,
                  'response_mime_type': 'application/json',
                  'response_json_schema': deepcopy(RESPONSE_SCHEMA),
                  'max_output_tokens': 2048}
        for attempt in range(self._max_attempts):
            error_code, retryable = None, False
            try:
                response = client.models.generate_content(model=self._settings.gemini_model,
                    contents=deepcopy(request.contents), config=deepcopy(config))
                raw = response.text
            except Exception as exc:
                status = getattr(exc, 'code', None) or getattr(exc, 'status_code', None)
                if isinstance(exc, TimeoutError) or type(exc).__name__ in ('TimeoutException', 'ReadTimeout', 'ConnectTimeout', 'WriteTimeout', 'PoolTimeout'):
                    error_code, retryable = 'unavailable', True
                elif status == 429:
                    error_code, retryable = 'rate_limit', True
                elif isinstance(status, int) and 500 <= status < 600:
                    error_code, retryable = 'unavailable', True
                elif status in (400, 401, 403, 404):
                    error_code = 'configuration'
                else:
                    error_code = 'unavailable'
            # Raise outside except to avoid chaining provider exceptions containing input.
            if error_code:
                if retryable and attempt + 1 < self._max_attempts:
                    self._sleep(0.25 * (2 ** attempt))
                    continue
                raise LLMError(error_code) from None
            return validate_response(raw, request.records, secret=self._settings.gemini_api_key)


def create_llm(settings: Settings):
    return FakeLLM() if settings.llm_mode == 'fake' else GeminiAdapter(settings)
