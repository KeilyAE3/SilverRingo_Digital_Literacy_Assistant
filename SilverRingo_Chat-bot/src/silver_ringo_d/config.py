"""Explicit environment loading. Does not read .env or call external APIs."""
from dataclasses import dataclass, field
from pathlib import Path
from collections.abc import Mapping
import os


@dataclass(frozen=True)
class Settings:
    llm_mode: str = 'fake'
    top_k: int = 4
    knowledge_dir: Path = field(default_factory=lambda: Path(__file__).resolve().parents[2] / 'knowledge')
    gemini_model: str = ''
    gemini_api_key: str = field(default='', repr=False)

    def __post_init__(self):
        if self.llm_mode not in ('fake', 'gemini'):
            raise ValueError('SR_D_LLM_MODE must be fake or gemini')
        if type(self.top_k) is not int or self.top_k < 1:
            raise ValueError('SR_D_TOP_K must be a positive integer')
        if not isinstance(self.knowledge_dir, Path):
            raise ValueError('knowledge_dir must be a Path')
        if not isinstance(self.gemini_model, str) or not isinstance(self.gemini_api_key, str):
            raise ValueError('Gemini settings must be strings')
        if self.llm_mode == 'gemini' and (not self.gemini_api_key.strip() or not self.gemini_model.strip()):
            raise ValueError('gemini mode requires GEMINI_API_KEY and GEMINI_MODEL')


def load_settings(environ: Mapping[str, str] | None = None) -> Settings:
    env = os.environ if environ is None else environ
    try:
        top_k = int(env.get('SR_D_TOP_K', '4'))
    except (ValueError, TypeError):
        raise ValueError('SR_D_TOP_K must be a positive integer') from None
    kwargs = {}
    if env.get('SR_D_KNOWLEDGE_DIR', '').strip():
        kwargs['knowledge_dir'] = Path(env['SR_D_KNOWLEDGE_DIR']).expanduser().resolve()
    return Settings(llm_mode=env.get('SR_D_LLM_MODE', 'fake'), top_k=top_k,
                    gemini_model=env.get('GEMINI_MODEL', ''),
                    gemini_api_key=env.get('GEMINI_API_KEY', ''), **kwargs)
