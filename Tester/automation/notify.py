"""Team Sync 실행 결과를 Discord 채널에 전달한다."""
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request


def shorten(value, limit):
    """긴 내용을 한 줄로 정리한다."""
    text = " ".join(str(value).split())
    return text if len(text) <= limit else text[:limit - 1] + "…"


def read_report():
    path = Path(os.environ["SYNC_RESULTS"])

    if path.stat().st_size > 2_000_000:
        raise ValueError("결과 파일이 너무 큽니다.")

    data = json.loads(path.read_text(encoding="utf-8"))

    if not isinstance(data, dict):
        raise ValueError("결과 파일 형식이 올바르지 않습니다.")

    if data.get("sha") != os.environ["SYNC_SHA"]:
        raise ValueError("결과 파일의 커밋이 실행 대상과 다릅니다.")

    for key in (
        "status", "total", "passed", "failure_events",
        "skipped", "expected_failures", "tests",
    ):
        if key not in data:
            raise ValueError(f"결과 파일에 {key} 항목이 없습니다.")

    if not isinstance(data["tests"], list):
        raise ValueError("tests는 목록이어야 합니다.")

    if not all(isinstance(item, dict) for item in data["tests"]):
        raise ValueError("개별 테스트 결과 형식이 올바르지 않습니다.")

    return data


def make_payload():
    conclusion = os.environ.get("SYNC_CONCLUSION", "unknown")
    actor = shorten(os.environ.get("SYNC_ACTOR", ""), 80)
    branch = shorten(os.environ.get("SYNC_BRANCH", ""), 100)
    sha = os.environ.get("SYNC_SHA", "")[:8]

    try:
        report = read_report()
        lines = [
            f"**Team Sync — {shorten(report['status'], 40)}**",
            f"GitHub 실행: {conclusion}",
            f"실행자: {actor}",
            f"브랜치: {branch} / 커밋: {sha}",
            "",
            (
                f"테스트 {report['total']} / 성공 {report['passed']} / "
                f"실패 이벤트 {report['failure_events']} / "
                f"생략 {report['skipped']} / "
                f"예상된 실패 {report['expected_failures']}"
            ),
        ]

        unsuccessful = [
            item for item in report["tests"]
            if item.get("status") != "passed"
        ]

        for item in unsuccessful[:2]:
            details = item.get("details", [])
            if not isinstance(details, list):
                details = [details]

            detail = "\n".join(str(value) for value in details)
            # traceback의 마지막 줄에는 일반적으로 예외명과 메시지가 있다.
            reason = next(
                (line for line in reversed(detail.splitlines()) if line.strip()),
                "상세 로그를 확인하세요.",
            )

            test_id = str(item.get("test_id", "unknown"))
            lines.extend([
                "",
                f"**[{shorten(item.get('status', 'unknown'), 30)}]**",
                f"기능 모듈: {shorten(item.get('module', ''), 120)}",
                f"테스트: {shorten(test_id.rsplit('.', 1)[-1], 100)}",
                f"명세: {shorten(item.get('description', ''), 150)}",
                f"원인: {shorten(reason, 250)}",
            ])

        if len(unsuccessful) > 2:
            lines.append(f"\n나머지 미통과 항목 {len(unsuccessful) - 2}개는 전체 결과에서 확인하세요.")

        if report["status"] == "not_configured":
            lines.append("\n등록된 테스트가 없습니다.")

    except (OSError, ValueError, KeyError, TypeError):
        lines = [
            "**Team Sync — 상세 결과 확인 불가**",
            f"GitHub 실행: {conclusion}",
            f"실행자: {actor}",
            f"브랜치: {branch} / 커밋: {sha}",
            "",
            "결과 파일이 없거나 형식·커밋 검증에 실패했습니다.",
            "아래 GitHub 실행 로그를 확인하세요.",
        ]

    # 긴 실패 설명 때문에 전체 결과 링크가 잘리지 않도록 따로 붙인다.
    body = "\n".join(lines)
    if len(body) > 1600:
        body = body[:1599] + "…"

    run_url = os.environ.get("SYNC_RUN_URL", "")
    return {
        "content": body + f"\n\n전체 결과·로그: {run_url}",
        "allowed_mentions": {"parse": []},
    }


def main():
    url = os.environ.get("DISCORD_WEBHOOK_URL", "").strip()

    if not url:
        print("알림 실패: GitHub Secret DISCORD_WEBHOOK_URL이 없습니다.")
        return 1

    parsed = urllib.parse.urlsplit(url)
    if (
        parsed.scheme != "https"
        or parsed.netloc != "discord.com"
        or not parsed.path.startswith("/api/webhooks/")
    ):
        print("알림 실패: Discord에서 복사한 Webhook URL을 등록하세요.")
        return 1

    # Discord가 메시지 생성 결과를 반환하도록 요청한다.
    query = dict(urllib.parse.parse_qsl(parsed.query))
    query["wait"] = "true"
    url = urllib.parse.urlunsplit(
        parsed._replace(query=urllib.parse.urlencode(query))
    )

    request = urllib.request.Request(
        url,
        data=json.dumps(make_payload(), ensure_ascii=False).encode("utf-8"),
        headers={
            "Content-Type": "application/json",
            "User-Agent": "TeamSync/1.0",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            message = json.load(response)

        if not isinstance(message, dict) or not message.get("id"):
            print("알림 실패: Discord 메시지 생성 응답을 확인하지 못했습니다.")
            return 1

        print(f"Discord 전송 성공: message_id={message['id']}")
        return 0

    except urllib.error.HTTPError as exc:
        print(f"Discord 전송 실패: HTTP {exc.code}")
        print("Webhook 유효 여부와 채널 설정을 확인하세요.")
        return 1

    except (urllib.error.URLError, TimeoutError, ValueError, OSError):
        # 예외 원문에는 Webhook URL이 포함될 수 있으므로 출력하지 않는다.
        print("Discord 전송 실패: 네트워크 또는 응답 처리 오류입니다.")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())