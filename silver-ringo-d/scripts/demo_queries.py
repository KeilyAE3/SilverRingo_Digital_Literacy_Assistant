"""Run four reproducible M07 questions through the real local RAG pipeline."""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from silver_ringo_d.action_registry import ActionRegistry
from silver_ringo_d.chat_service import ChatService
from silver_ringo_d.config import Settings
from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.llm import FakeLLM
from silver_ringo_d.models import ChatRequest
from silver_ringo_d.retriever import LexicalRetriever


def main():
    store = KnowledgeStore(ROOT / 'knowledge')
    store.load()
    service = ChatService(LexicalRetriever(store), FakeLLM(),
                          ActionRegistry(), Settings())
    questions = [
        '실버링고는 어떤 앱이야?',
        '키오스크에서 카드 결제하는 법을 배우고 싶어',
        '똑닥으로 병원 접수를 하려는데 어려워',
        '정부24로 등본을 발급해 줘',
    ]
    for question in questions:
        response = service.handle(ChatRequest(question))
        print(json.dumps({
            'question': question,
            'reply': response.reply,
            'sources': [source.id for source in response.sources],
            'suggested_actions': [action.id for action in response.suggested_actions],
        }, ensure_ascii=False))


if __name__ == '__main__':
    main()
