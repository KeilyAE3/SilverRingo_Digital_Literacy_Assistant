# 모듈 간 인터페이스

현재 등록된 앱 모듈과 계약은 없습니다. [계약 템플릿](../templates/contract.json)을 복사해 `<interface-id>.json`으로 추가하세요.
`provider`와 `consumers`에는 `automation/config.json`의 실제 모듈 id를 씁니다.

이 JSON은 협업 명세의 최소 구조입니다. HTTP API의 OpenAPI나 공유 타입을 대체하지 않습니다.
형식 검사는 실제 구현의 호환성을 증명하지 않으므로 제공자·소비자 테스트를 함께 등록해야 합니다.
