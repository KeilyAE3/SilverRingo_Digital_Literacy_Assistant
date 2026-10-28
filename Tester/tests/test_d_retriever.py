import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'SilverRingo_Chat-bot' / 'src'))

from silver_ringo_d.knowledge_store import KnowledgeStore
from silver_ringo_d.retriever import LexicalRetriever


class DRetrieverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        store = KnowledgeStore(ROOT / 'SilverRingo_Chat-bot' / 'knowledge')
        store.load()
        cls.retriever = LexicalRetriever(store)

    def ids(self, query, include_ideas=False):
        return [hit.document.id for hit in self.retriever.search(
            query, top_k=4, include_ideas=include_ideas)]

    def test_app_introduction(self):
        """D-QA-01: 앱 소개 질의가 실버링고 앱 문서를 검색한다."""
        self.assertIn('app:overview', self.ids('실버링고는 어떤 앱이야?'))

    def test_kiosk_payment_learning(self):
        """D-QA-02: 카드 결제 학습 질의가 키오스크 결제 단계를 검색한다."""
        self.assertIn('course:kiosk:level:5',
                      self.ids('키오스크에서 카드 결제하는 법을 배우고 싶어'))

    def test_ddocdoc_guide(self):
        """D-QA-03: 병원 접수 질의가 똑닥 가이드 문서를 검색한다."""
        self.assertTrue(any(identifier.startswith('guide:ddocdoc')
                            for identifier in self.ids('똑닥으로 병원 접수를 하려는데 어려워')))

    def test_retired_service_is_not_current_result(self):
        """D-QA-04: 정부24 현재 사용 질의가 폐기 자료나 실행 후보를 검색하지 않는다."""
        identifiers = self.ids('정부24로 등본을 발급해 줘')
        self.assertNotIn('guide:retired:gov24', identifiers)
        self.assertNotIn('guide:retired:kakao_t', identifiers)

    def test_curated_lexical_variants_match_without_embeddings(self):
        """검색 보완: 계산·무인계산대 같은 검토된 표현도 현재 자료에 연결한다."""
        self.assertIn('course:kiosk:level:5',
                      self.ids('무인계산대에서 카드로 계산하는 법'))
        self.assertTrue(any(identifier.startswith('guide:ddocdoc')
                            for identifier in self.ids('병원 등록을 도와줘')))


if __name__ == '__main__':
    unittest.main()
