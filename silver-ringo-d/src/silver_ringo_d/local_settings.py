"""Explicit local KEY=VALUE loading; no shell evaluation or environment mutation."""
import os
from pathlib import Path
from .config import load_settings

ALLOWED = {'GEMINI_API_KEY', 'GEMINI_MODEL', 'SR_D_LLM_MODE', 'SR_D_TOP_K', 'SR_D_KNOWLEDGE_DIR'}


def load_local_settings(path: str | Path, *, environ=None):
    """Environment overrides file. No interpolation, export, or inline comments."""
    values = {}
    try:
        raw = Path(path).read_text(encoding='utf-8-sig')
        if len(raw) > 16384:
            raise ValueError()
        for line in raw.splitlines():
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            key, separator, value = line.partition('=')
            key, value = key.strip(), value.strip()
            if not separator or key not in ALLOWED or key in values:
                raise ValueError()
            if value.startswith(('"', "'")):
                if len(value) < 2 or value[-1] != value[0]:
                    raise ValueError()
                value = value[1:-1]
            values[key] = value
    except (OSError, UnicodeError, ValueError):
        raise ValueError('Cannot load local settings; check file format and permissions') from None
    env = os.environ if environ is None else environ
    values.update({key: env[key] for key in ALLOWED if key in env})
    return load_settings(values)
