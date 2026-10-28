"""Convert a local C content.json into D knowledge; never contacts Git/network.

Only this converter owns learning_courses.json. D editorial additions below are
marked curated_by_d; copied fields retain the source commit and byte hash.
"""
import argparse
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import re
import tempfile

CURATED = {
    1: (['주문 시작', '매장에서 먹기', '포장'], ['무인 주문 시작']),
    2: (['메뉴', '버거', '카테고리'], ['상품 찾기']),
    3: (['세트', '옵션', '음료', '감자튀김'], ['세트 메뉴 고르기']),
    4: (['장바구니', '수량', '합계'], ['주문 내역 확인']),
    5: (['결제', '카드', '모의 결제'], ['기계로 계산', '카드로 돈 내기']),
    6: (['전체 주문', '실전 연습', '혼자 주문'], ['혼자 해보기']),
}


def convert(raw: bytes, commit: str):
    if not re.fullmatch('[0-9a-f]{40}', commit):
        raise ValueError('A full source commit SHA is required')
    data = json.loads(raw.decode('utf-8-sig'))
    if not isinstance(data, dict) or type(data.get('schemaVersion')) is not int or data['schemaVersion'] != 1 or data.get('topicId') != 'kiosk':
        raise ValueError('Unsupported C schema/topic')
    levels, guides = data.get('levels'), data.get('guides')
    if (not isinstance(levels, list) or len(levels) != 6
            or any(not isinstance(level, dict) or type(level.get('id')) is not int for level in levels)
            or {level['id'] for level in levels} != set(range(1, 7))):
        raise ValueError('Expected C levels 1..6 exactly once')
    expected = {'start', 'dining', 'menu', 'options', 'cart', 'review', 'payment', 'card', 'receipt'}
    if not isinstance(guides, dict) or set(guides) != expected or any(not isinstance(v, str) or not v.strip() for v in guides.values()):
        raise ValueError('Expected nine named C guides')
    source = dict(branch='jib', path='silver-ringo-c/web/content.json', kind='code',
                  commit=commit, sha256=hashlib.sha256(raw).hexdigest())
    def record(id, title, content, keywords=None, aliases=None):
        return dict(id=id, title=title, content=content, status='prototype',
                    source=deepcopy(source), keywords=keywords or [], aliases=aliases or [],
                    alias_origin='curated_by_d', related_action_ids=['open_kiosk_course'])
    course = record('course:kiosk', '키오스크 배우기',
                    '키오스크 사용의 시작, 메뉴 선택, 옵션, 장바구니, 모의 카드 결제, 혼자 주문하기를 6단계로 연습합니다. 실제 주문이나 결제는 이루어지지 않습니다.',
                    ['키오스크', '무인 주문', '햄버거 주문', '식당 주문'], ['기계로 주문하기', '화면 눌러서 주문하기'])
    course.update(topic_id='kiosk', summary='키오스크로 주문하는 방법을 단계적으로 연습하는 학습 코스입니다.',
                  summary_origin='curated_by_d', unlock_policy='앞 단계 통과 시 다음 단계 해제. 세부 판정은 C 소유.',
                  unlock_policy_source=dict(branch='jib', path='silver-ringo-c/android/app/src/main/java/kr/silverringo/learning/LearningEntry.kt', kind='code', commit=commit),
                  levels=[], guides=[])
    for level in sorted(levels, key=lambda value: value['id']):
        for name in ('title', 'description', 'concept', 'task', 'start', 'end'):
            if not isinstance(level.get(name), str) or not level[name].strip():
                raise ValueError('Invalid C level text field: ' + name)
        if not isinstance(level.get('checks'), list) or any(not isinstance(v, str) or not v.strip() for v in level['checks']):
            raise ValueError('Invalid C checks')
        keywords, aliases = CURATED[level['id']]
        item = record(f"course:kiosk:level:{level['id']}", level['title'], level['concept'], keywords, aliases)
        item.update(level_id=level['id'], summary=level['description'],
                    **{name: deepcopy(level[name]) for name in ('task', 'start', 'end', 'checks')})
        course['levels'].append(item)
    for step_id, content in guides.items():
        item = record('course:kiosk:guide:' + step_id, '키오스크 안내: ' + step_id, content)
        item.update(step_id=step_id, title_origin='curated_by_d')
        course['guides'].append(item)
    return dict(schema_version=1, source=source,
                notes='C content.json에서 재생성. 상품/가격 제외. A 통합 및 Android 실기기 검증 미완료. aliases는 D 편집값.',
                courses=[course])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.input.resolve() == args.output.resolve():
        parser.error('Input and output must differ')
    try:
        payload = convert(args.input.read_bytes(), args.commit)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', newline='\n',
                                         dir=args.output.parent, delete=False) as handle:
            temp = Path(handle.name)
            try:
                handle.write(json.dumps(payload, ensure_ascii=False, indent=2) + '\n')
            except BaseException:
                handle.close()
                temp.unlink(missing_ok=True)
                raise
        try:
            temp.replace(args.output)
        finally:
            temp.unlink(missing_ok=True)
    except (OSError, UnicodeError, ValueError):
        parser.exit(1, 'C import failed; check input schema, commit and output path.\n')
    print('Generated learning_courses.json (runtime acceptance not evaluated).')


if __name__ == '__main__':
    main()
