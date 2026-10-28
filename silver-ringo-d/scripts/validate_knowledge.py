"""Deferred manual validation command. No API keys or network required."""
import argparse
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))
from silver_ringo_d.knowledge_store import KnowledgeStore, KnowledgeError


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--knowledge-dir', type=Path, default=ROOT / 'knowledge')
    parser.add_argument('--repository', type=Path, help='Also compare pinned sources against local origin refs')
    args = parser.parse_args()
    try:
        store = KnowledgeStore(args.knowledge_dir)
        store.load()
        counts = store.counts()
        report = dict(total_documents=sum(counts.values()), status_counts=counts,
                      current_documents=len(store.documents()), actions=len(store.actions()))
        if args.repository:
            report['source_changes'] = store.source_changes(args.repository)
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 2 if any(item['changed'] for item in report.get('source_changes', [])) else 0
    except KnowledgeError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    except OSError:
        print('Local Git unavailable; source comparison could not run.', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
