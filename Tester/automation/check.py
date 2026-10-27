"""등록된 테스트를 실행하고 전체 항목 및 실패 상세를 출력한다."""
import json
import os
from pathlib import Path
import sys
import unittest
import argparse

TESTER = Path(__file__).resolve().parents[1]
ROOT = TESTER.parent


def flatten(suite):
    """중첩된 TestSuite에서 개별 테스트를 꺼낸다."""
    for item in suite:
        if isinstance(item, unittest.TestSuite):
            yield from flatten(item)
        else:
            yield item


def load_suite(mode):
    config = json.loads(
        (TESTER / "config.json").read_text(encoding="utf-8")
    )
    checks = config["checks"]

    if not isinstance(checks, list):
        raise ValueError("config.json의 checks는 목록이어야 합니다.")

    suite = unittest.TestSuite()
    registered = set()

    for check in checks:
        if not isinstance(check, dict):
            raise ValueError("checks의 각 항목은 객체여야 합니다.")

        selection = check.get(mode)

        if not isinstance(selection, dict):
            raise ValueError(
                f"{check.get('name', '(이름 없음)')}: "
                f"{mode} 설정이 필요합니다."
            )

        enabled = selection.get("enabled", True)

        if not isinstance(enabled, bool):
            raise ValueError("enabled는 true 또는 false여야 합니다.")

        # 꺼진 검사 묶음은 파일 확인이나 import도 수행하지 않는다.
        if not enabled:
            continue

        # include 생략 = 전체 실행
        # include: [] = 실행할 항목 없음
        include = (
            selection.get("include")
            if "include" in selection
            else None
        )

        if "include" in selection:
            if not isinstance(include, list):
                raise ValueError("include는 테스트 메서드 이름 목록이어야 합니다.")

            if not all(
                isinstance(name, str) and name.startswith("test_")
                for name in include
            ):
                raise ValueError(
                    "include에는 test_로 시작하는 메서드 이름을 적어야 합니다."
                )

            if len(include) != len(set(include)):
                raise ValueError("include에 같은 이름을 중복 등록할 수 없습니다.")

            if not include:
                continue

        module_path = (ROOT / check["module"]).resolve()
        test_path = (TESTER / check["test"]).resolve()

        if not module_path.is_relative_to(ROOT):
            raise ValueError("기능 파일은 저장소 안에 있어야 합니다.")

        if not test_path.is_relative_to(TESTER):
            raise ValueError("테스트 파일은 Tester 안에 있어야 합니다.")

        if not module_path.is_file():
            raise FileNotFoundError(
                f"기능 파일 없음: {check['module']}"
            )

        if not test_path.is_file():
            raise FileNotFoundError(
                f"테스트 파일 없음: {check['test']}"
            )

        if (
            not test_path.name.startswith("test_")
            or test_path.suffix != ".py"
        ):
            raise ValueError("테스트 파일 이름은 test_*.py여야 합니다.")

        if test_path in registered:
            raise ValueError(
                f"중복 등록된 테스트 파일: {check['test']}"
            )

        registered.add(test_path)

        import_name = ".".join(
            test_path.relative_to(ROOT).with_suffix("").parts
        )

        # 각 파일마다 새 loader를 사용해 로딩 오류를 구분한다.
        loader = unittest.TestLoader()
        loaded = loader.loadTestsFromName(import_name)

        # 선택 과정에서 import 오류가 가려지지 않도록 먼저 확인한다.
        if loader.errors:
            raise ValueError(
                f"테스트 로딩 실패: {check['test']}\n"
                + "\n".join(loader.errors)
            )

        candidates = list(flatten(loaded))

        if not candidates:
            raise ValueError(
                f"테스트 항목이 없는 파일: {check['test']}"
            )

        if include is None:
            selected = candidates
        else:
            by_name = {}

            for test in candidates:
                method_name = test.id().rsplit(".", 1)[-1]
                by_name.setdefault(method_name, []).append(test)

            missing = [
                name for name in include
                if name not in by_name
            ]
            if missing:
                raise ValueError(
                    f"{check['test']}: 존재하지 않는 테스트 이름: "
                    + ", ".join(missing)
                )

            ambiguous = [
                name for name in include
                if len(by_name[name]) > 1
            ]
            if ambiguous:
                raise ValueError(
                    f"{check['test']}: 여러 클래스에서 중복된 테스트 이름: "
                    + ", ".join(ambiguous)
                    + ". 선택할 메서드 이름을 서로 다르게 지정하세요."
                )

            # include에 작성한 순서대로 실행한다.
            selected = [
                by_name[name][0]
                for name in include
            ]

        for test in selected:
            test.sync_info = {
                "group": check["name"],
                "module": check["module"],
                "test_file": check["test"],
            }
            suite.addTest(test)

    return suite


class Result(unittest.TextTestResult):
    """unittest 결과에 모듈, 명세, 오류 원문을 함께 기록한다."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.records = []
        self.current = None

    def startTest(self, test):
        super().startTest(test)
        self.current = {
            **test.sync_info,
            "test_id": test.id(),
            "description": test.shortDescription() or "(명세 없음)",
            "status": "passed",
            "details": [],
        }
        self.records.append(self.current)

        self.stream.writeln(
            f"\n기능 모듈: {self.current['module']}\n"
            f"테스트 항목: {self.current['test_id']}\n"
            f"명세: {self.current['description']}"
        )

    def record_detail(self, status, text):
        # 앞선 실패를 뒤의 skip 등으로 덮어쓰지 않는다.
        if self.current["status"] not in ("failed", "error"):
            self.current["status"] = status
        self.current["details"].append(text)

    def addFailure(self, test, err):
        super().addFailure(test, err)
        self.record_detail("failed", self._exc_info_to_string(err, test))

    def addError(self, test, err):
        super().addError(test, err)
        self.record_detail("error", self._exc_info_to_string(err, test))

    def addSkip(self, test, reason):
        super().addSkip(test, reason)
        self.record_detail("skipped", reason)

    def addExpectedFailure(self, test, err):
        super().addExpectedFailure(test, err)
        self.record_detail(
            "expected_failure",
            self._exc_info_to_string(err, test),
        )

    def addUnexpectedSuccess(self, test):
        super().addUnexpectedSuccess(test)
        self.record_detail(
            "failed",
            "expectedFailure로 지정된 테스트가 예상과 달리 성공했습니다.",
        )

    def addSubTest(self, test, subtest, err):
        super().addSubTest(test, subtest, err)
        if err is not None:
            status = (
                "failed"
                if issubclass(err[0], test.failureException)
                else "error"
            )
            self.record_detail(
                status,
                f"{subtest.id()}\n"
                + self._exc_info_to_string(err, subtest),
            )


def table_cell(value):
    """Markdown 표가 줄바꿈이나 | 문자로 깨지지 않게 한다."""
    return str(value).replace("|", r"\|").replace("\r", " ").replace("\n", " ")


def make_summary(report):
    lines = [
        "# Team Sync",
        "",
        f"Status: **{report['status']}**",
        "",
        (
            f"Tests: {report['total']} / "
            f"Passed: {report['passed']} / "
            f"Failure events: {report['failure_events']} / "
            f"Skipped: {report['skipped']} / "
            f"Expected failures: {report['expected_failures']}"
        ),
        "",
        f"선택 설정: {report['mode']}",
        f"검사 범위: {report['scope']}",
        "",
        "## 이번 실행의 검사 목록",
        "",
        "config.json에서 활성화·선택한 항목만 실행했습니다.",
        "설정으로 제외한 항목은 테스트 수와 Skipped에 포함하지 않습니다.",
        "",
        "| 검사 묶음 | 기능 모듈 식별명 | 테스트 항목 식별명 | 명세 | 결과 |",
        "|---|---|---|---|---|",
    ]

    for item in report["tests"]:
        values = [
            item["group"],
            item["module"],
            item["test_id"],
            item["description"],
            item["status"],
        ]
        lines.append("| " + " | ".join(map(table_cell, values)) + " |")

    if report["status"] == "not_configured":
        lines.extend([
            "",
            "실행 대상 없음: Tester/config.json의 checks, enabled, include를 확인하세요.",
        ])

    lines.extend(["", "## 실패 및 미통과 상세", ""])
    unsuccessful = [
        item for item in report["tests"]
        if item["status"] != "passed"
    ]

    if not unsuccessful:
        lines.append("없음")

    for item in unsuccessful:
        lines.extend([
            f"### {item['test_id']}",
            "",
            f"- 검사 묶음: {item['group']}",
            f"- 기능 모듈 식별명: {item['module']}",
            f"- 테스트 파일: {item['test_file']}",
            f"- 명세: {item['description']}",
            f"- 결과: {item['status']}",
            "",
        ])

        # 들여쓴 코드 블록으로 assertion 메시지와 traceback을 보존한다.
        for detail in item["details"]:
            lines.extend(
                "    " + line for line in detail.splitlines()
            )
            lines.append("")

    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description="Team Sync 테스트 실행")
    parser.add_argument(
        "--mode",
        choices=("local", "workflow"),
        default="local",
    )
    args = parser.parse_args()

    os.chdir(ROOT)
    sys.path.insert(0, str(ROOT))

    output = TESTER / ".team-sync"
    output.mkdir(exist_ok=True)

    for name in ("results.json", "summary.md", "tests.log"):
        (output / name).unlink(missing_ok=True)

    with (output / "tests.log").open("w", encoding="utf-8") as log:
        try:
            suite = load_suite(args.mode)
            result = unittest.TextTestRunner(
                stream=log,
                verbosity=2,
                resultclass=Result,
            ).run(suite)

            failures = [
                {"test": test.id(), "detail": detail}
                for test, detail in result.failures + result.errors
            ]
            failures += [
                {"test": test.id(), "detail": "Unexpected success"}
                for test in result.unexpectedSuccesses
            ]

            if result.testsRun == 0:
                status = "not_configured"
            elif (
                result.wasSuccessful()
                and not result.skipped
                and not result.expectedFailures
            ):
                status = "passed"
            else:
                status = "failed"

            report = {
                "status": status,
                "total": result.testsRun,
                "passed": sum(
                    item["status"] == "passed"
                    for item in result.records
                ),
                "failure_events": len(failures),
                "skipped": len(result.skipped),
                "expected_failures": len(result.expectedFailures),
                "failures": failures,
                "tests": result.records,
            }

        except Exception as exc:
            detail = f"{type(exc).__name__}: {exc}"
            log.write(f"設定 또는 테스트 로딩 실패: {detail}\n")
            report = {
                "status": "failed",
                "total": 0,
                "passed": 0,
                "failure_events": 1,
                "skipped": 0,
                "expected_failures": 0,
                "failures": [
                    {"test": "configuration", "detail": detail}
                ],
                "tests": [{
                    "group": "검사 준비",
                    "module": "(실행 전)",
                    "test_file": "(실행 전)",
                    "test_id": "configuration",
                    "description": "설정 및 테스트 로딩",
                    "status": "error",
                    "details": [detail],
                }],
            }

    report["sha"] = os.environ.get("GITHUB_SHA", "")
    report["mode"] = args.mode
    report["scope"] = (
        "main에 반영된 실행 대상 커밋"
        if (
            os.environ.get("GITHUB_ACTIONS") == "true"
            and os.environ.get("GITHUB_REF") == "refs/heads/main"
        )
        else "현재 로컬 작업 파일: main과의 가상 병합 없음"
    )

    (output / "results.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    summary = make_summary(report)
    (output / "summary.md").write_text(summary, encoding="utf-8")
    print(summary)

    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(
            os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8"
        ) as stream:
            stream.write(summary)

    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())