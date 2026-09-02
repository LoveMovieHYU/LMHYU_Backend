---
description: 변경분을 code-reviewer 에이전트로 리뷰한다 (읽기 전용, 컨벤션·아키텍처·안전성 점검)
---

`code-reviewer` 에이전트를 실행해 현재 변경분을 리뷰한다.

## 대상 결정

- 인자 `$ARGUMENTS` 가 있으면 그것을 리뷰 대상으로 삼는다:
  - 브랜치명/커밋 해시 → 해당 diff (`git diff <base>...HEAD` 등)
  - 파일/디렉터리 경로 → 해당 경로의 변경분
  - PR 번호 → `gh pr diff <번호>` 로 변경 파일 확보
- 인자가 없으면 **working tree + 스테이징된 변경**(`git diff`, `git diff --staged`)을 리뷰한다.
  변경이 없으면 마지막 커밋(`git show`)을 대상으로 한다.

## 실행

1. 위 규칙으로 리뷰 대상 diff/파일 목록을 확정한다.
2. `code-reviewer` 에이전트(Agent tool, subagent_type: "code-reviewer")를 호출하고
   확정한 대상을 프롬프트로 전달한다.
3. 에이전트가 반환한 심각도별 리뷰 결과를 사용자에게 그대로 정리해 보고한다.

에이전트는 읽기 전용이다. 코드를 고치지 않는다 — 수정이 필요하면 `spring-developer` 에이전트로 넘긴다.
