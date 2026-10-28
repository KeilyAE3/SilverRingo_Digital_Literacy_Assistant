"""Conservative Korean lexical normalization; not a morphological analyzer."""
import re
import unicodedata

# Editorial retrieval rules, not source quotations or learned embeddings.
SUFFIXES = tuple(sorted((
    '에서는', '으로는', '에서', '으로', '에게', '까지', '부터',
    '하는', '하기', '하고', '하면', '해야', '하려면',
    '이야', '인가', '이에요', '입니다', '은', '는', '이', '가', '을', '를', '에', '도', '로',
), key=lambda value: (-len(value), value)))
STOPWORDS = frozenset({
    '어떤', '어디', '어떻게', '무엇', '있어', '있어요', '해요', '해주세요',
    '싶어요', '싶어', '알려줘', '해줘', '지금', '앞으로', '계획', '기능',
    '사용', '안내', '선택', '확인', '방법', '배우기', '배우고', '만들',
})


def normalize(value: str) -> str:
    """NFKC + casefold, punctuation becomes spaces; no path/command handling."""
    value = unicodedata.normalize('NFKC', value).casefold()
    return ' '.join(re.findall(r'[a-z0-9가-힣]+', value))


def tokens(value: str) -> tuple[str, ...]:
    result = []
    for token in normalize(value).split():
        if token in STOPWORDS:
            continue
        for suffix in SUFFIXES:
            if token.endswith(suffix) and len(token) - len(suffix) >= 2:
                token = token[:-len(suffix)]
                break
        if len(token) >= 2 and token not in STOPWORDS:
            result.append(token)
    return tuple(result)


def contains_phrase(query: tuple[str, ...], phrase: tuple[str, ...]) -> bool:
    """Whole normalized tokens only; 병 never matches 병원 or 병원비."""
    return bool(phrase) and any(query[i:i + len(phrase)] == phrase
                                for i in range(len(query) - len(phrase) + 1))
