"""Deterministic lexical retrieval over KnowledgeStore snapshots.

Returns evidence records only. Prompt budgets, history, API requests and action
authorization belong to later modules. No network, embeddings or logging.
"""
import math
from collections import Counter
from typing import Protocol

from .knowledge_store import CURRENT_STATUSES
from .models import KnowledgeDocument, KnowledgeHit
from .normalizer import contains_phrase, normalize, tokens

MAX_QUERY_LENGTH = 1000
MAX_TOP_K = 10
DEFAULT_MIN_SCORE = 4.0


class DocumentStore(Protocol):
    def documents(self, statuses=None) -> list[KnowledgeDocument]: ...


class LexicalRetriever:
    def __init__(self, store: DocumentStore, *, min_score: float = DEFAULT_MIN_SCORE):
        if type(min_score) not in (int, float) or not math.isfinite(min_score) or min_score <= 0:
            raise ValueError('min_score must be finite and positive')
        self.store = store
        self.min_score = min_score

    def search(self, query: str, top_k: int = 4, include_ideas: bool = False) -> list[KnowledgeHit]:
        if not isinstance(query, str):
            raise ValueError('query must be a string')
        if len(query) > MAX_QUERY_LENGTH:
            raise ValueError('query exceeds 1000 characters')
        if type(top_k) is not int:
            raise ValueError('top_k must be an integer')
        if type(include_ideas) is not bool:
            raise ValueError('include_ideas must be a boolean')
        limit = max(0, min(MAX_TOP_K, top_k))
        if limit == 0 or not normalize(query):
            return []
        statuses = CURRENT_STATUSES | {'idea'} if include_ideas else CURRENT_STATUSES
        # Refresh from the store each call so successful reloads are visible.
        documents = [doc for doc in self.store.documents(statuses=statuses) if doc.status in statuses]
        prepared = [(doc, self._fields(doc)) for doc in documents]
        frequency = Counter(term for _, fields in prepared
                            for term in set().union(*(set(words) for _, words in fields)))
        query_tokens = tokens(query)
        query_terms = set(query_tokens)
        hits = []
        for doc, fields in prepared:
            exact_id = query.strip().casefold() == doc.id.casefold()
            exact_title = normalize(query) == normalize(doc.title)
            if not query_terms and not exact_id and not exact_title:
                continue
            best, matched, anchored = {}, set(), False
            for weight, words in fields:
                overlap = query_terms.intersection(words)
                if weight > 1 and overlap:
                    anchored = True
                # Field length cannot increase score; repeated tokens count once.
                correction = math.sqrt(1 + len(set(words)) / 12)
                for term in overlap:
                    idf = 1 + math.log((len(documents) + 1) / (frequency[term] + 1))
                    best[term] = max(best.get(term, 0), weight * idf / correction)
                    matched.add(term)
            if not (exact_id or exact_title or anchored or len(matched) >= 2):
                continue
            # Multi-concept questions need more than one incidental shared word.
            if len(query_terms) >= 3 and len(matched) < 2 and not (exact_id or exact_title):
                continue
            # Average by total meaningful query terms to suppress incidental overlap.
            score = sum(best.values()) / max(1, len(query_terms))
            phrase_bonus = 0.0
            for value, weight in [(doc.title, 12.0), *[(k, 8.0) for k in doc.keywords],
                                  *[(a, 5.0) for a in doc.aliases]]:
                phrase = tokens(value)
                if len(phrase) >= 2 and contains_phrase(query_tokens, phrase):
                    phrase_bonus = max(phrase_bonus, weight)
                    matched.add(normalize(value))
            score += phrase_bonus
            if exact_title:
                score += 30
                matched.add(normalize(doc.title))
            if exact_id:
                score += 50
                matched.add(doc.id)
            score = round(score, 6)
            if score >= self.min_score:
                hits.append(KnowledgeHit(document=doc, score=score, matched_terms=sorted(matched)))
        hits.sort(key=lambda hit: (-hit.score, hit.document.id))
        return hits[:limit]

    @staticmethod
    def _fields(doc):
        return [(6, tokens(doc.title)),
                (5, tuple(word for value in doc.keywords for word in tokens(value))),
                (4, tuple(word for value in doc.aliases for word in tokens(value))),
                (1, tokens(doc.content))]


# Public interface name used in the M02 specification.
Retriever = LexicalRetriever
