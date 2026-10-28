"""Manual M03 probe. Fake by default; --live explicitly enables paid network use.

Uses fixed non-personal questions. This is not ChatService or a Tester check.
"""
import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))
from silver_ringo_d.config import Settings, load_settings
from silver_ringo_d.local_settings import load_local_settings
from silver_ringo_d.knowledge_store import KnowledgeStore, KnowledgeError
from silver_ringo_d.models import KnowledgeHit
from silver_ringo_d.retriever import LexicalRetriever
from silver_ringo_d.llm import LLMError, serialize_context
from silver_ringo_d.gemini_adapter import create_llm


def main():
    # Stable Korean output for Windows terminals and redirected JSON.
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--probe', choices=('weak', 'rag'), default='rag')
    parser.add_argument('--live', action='store_true', help='Explicitly allow a real Gemini request')
    parser.add_argument('--env-file', type=Path, help='Explicit local settings file, only for --live')
    args = parser.parse_args()
    if args.env_file and not args.live:
        parser.error('--env-file is only read with --live')
    client = None
    try:
        settings = (load_local_settings(args.env_file) if args.env_file else load_settings()) if args.live else Settings()
        if args.live and settings.llm_mode != 'gemini':
            raise ValueError('Live probe requires SR_D_LLM_MODE=gemini')
        store = KnowledgeStore(settings.knowledge_dir)
        store.load()
        if args.probe == 'weak':
            question = '실버링고가 어떤 앱인지 짧게 소개해 주세요.'
            doc = next(doc for doc in store.documents() if doc.id == 'app:overview')
            hits = [KnowledgeHit(doc, 1.0, [])]
        else:
            question = '키오스크에서 카드 결제를 연습하고 싶어요.'
            hits = LexicalRetriever(store).search(question, top_k=settings.top_k)
        if not hits:
            raise ValueError('Probe requires evidence; no request sent')
        client = create_llm(settings)
        raw = client.generate(question, serialize_context(hits), [])
        print(json.dumps({'transport': 'gemini' if args.live else 'fake', 'probe': args.probe,
                          'selected_ids': [hit.document.id for hit in hits],
                          'response': json.loads(raw)}, ensure_ascii=False, indent=2))
        return 0
    except LLMError as exc:
        print(json.dumps({'error': exc.code, 'message': str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1
    except (ValueError, KnowledgeError, OSError, StopIteration):
        print('Probe setup failed; check mode, settings and knowledge resources.', file=sys.stderr)
        return 1
    finally:
        if client is not None and hasattr(client, 'close'):
            client.close()


if __name__ == '__main__':
    raise SystemExit(main())
