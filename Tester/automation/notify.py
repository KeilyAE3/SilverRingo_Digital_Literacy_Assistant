"""Send the completed run's summary; never execute downloaded artifacts."""
import json
import os
from pathlib import Path
import urllib.request


def payload():
    conclusion = os.environ.get('SYNC_CONCLUSION', 'unknown')
    text = f"Team Sync: workflow {conclusion}\n"
    text += f"{os.environ.get('SYNC_ACTOR', '')} / {os.environ.get('SYNC_BRANCH', '')}\n"
    try:
        path = Path(os.environ['SYNC_RESULTS'])
        if path.stat().st_size > 2_000_000:
            raise ValueError('Report too large')
        data = json.loads(path.read_text(encoding='utf-8'))
        if data.get('sha') != os.environ['SYNC_SHA']:
            raise ValueError('Commit mismatch')
        text += f"검사: {data['status']} / 테스트 {data['total']} / 실패 이벤트 {data['failure_events']} / 생략 {data['skipped']}\n"
        for failure in data['failures'][:3]:
            text += f"실패: {failure['test'][:180]}\n"
        if data['status'] == 'not_configured':
            text += '실제 기능 테스트가 아직 등록되지 않았습니다.\n'
    except (OSError, ValueError, KeyError, TypeError):
        text += '검사 결과 파일을 읽을 수 없습니다. 실행 로그를 확인하세요.\n'
    text += '현재 브랜치 검사 결과입니다. 상세 결과: ' + os.environ.get('SYNC_RUN_URL', '')
    return {'content': text[:1900], 'allowed_mentions': {'parse': []}}


if __name__ == '__main__':
    url = os.environ.get('DISCORD_WEBHOOK_URL', '')
    if not url:
        print('DISCORD_WEBHOOK_URL 미설정: 알림 생략')
    else:
        request = urllib.request.Request(url, data=json.dumps(payload()).encode('utf-8'),
                                         headers={'Content-Type': 'application/json', 'User-Agent': 'TeamSync/1.0'})
        with urllib.request.urlopen(request, timeout=20) as response:
            print('Discord notification:', response.status)
