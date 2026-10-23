# SilverRingo collaboration rules

- Discord is the team's place for assignments, design discussion, and weekly decisions. GitHub Issues, Projects, PRs, and human approvals are optional.
- Use task branches such as `feat/<owner>/<topic>`, `fix/<owner>/<topic>`, `integration/<sprint>`, and `release/<demo>`; do not use a person's branch as an indefinitely accumulating work queue.
- `main` represents the team's integrated sprint version. Synchronize early in `integration/<sprint>` or task branches; push the agreed integrated result to main. A partial demonstration can use `release/<demo>`.
- Before editing a module, read its contracts in `docs/contracts/` and relevant change reports. Record Discord decisions only when they change an interface, dependency, or integration prerequisite; transcripts are unnecessary.
- For a logical module change, add or update `docs/reports/<owner>/<task>.json` using `docs/templates/report.json`. Do not write one report per debugging commit. Do not put a commit's own SHA inside that same commit; CI records the tested SHA.
- When changing an external interface, update its contract and relevant consumer/provider tests in the same push. State compatibility and migration requirements. Do not invent modules, owners, APIs, completed tests, or dates.
- New modules must be registered in `automation/config.json` with real source paths and executable check commands; integration checks are configured separately. An unconfigured application check is NOT a passed test.
- Run `python -m unittest discover -s automation/tests -v` and `python automation/check.py` before pushing. The Python checker does not install application dependencies; add real setup steps to the workflow when the stack is chosen.
- Never commit Discord webhook URLs or credentials. Configure secrets through GitHub settings.
- Agents that do not automatically load this file must be configured by their user to read it. Agent output still needs executable validation.
