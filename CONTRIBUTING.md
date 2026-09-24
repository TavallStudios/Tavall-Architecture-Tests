# Contributing to Tavall Architecture Tests

This repository is the canonical executable source for Tavall Studios architecture tests.

## Development & Contribution Guidelines

1. **Policy Authority**:
   - Written architecture and documentation policy is governed organization-wide by [`TavallStudios/tavall-docs`](https://github.com/TavallStudios/tavall-docs), including `docs/quality/GIT_WORKFLOW.md` and relevant architecture standards.
   - This repository implements the mechanically enforceable subset of that policy as automated JUnit Platform architecture tests and a reusable Gradle plugin.

2. **Source Layout & Executable Boundary**:
   - `modules/` and `gradle-plugin/` are the maintained executable architecture-test sources.
   - Files under `repositories/` are historical provenance/migration snapshots, pinned via `manifest/sources.json`. Do not edit them as consumer runtime logic.
   - Do not copy reusable canonical rule source into consumer repositories. Consumer repositories consume the published Gradle plugin and declare the production projects inspected by their repository test suite (`*-test-suite`).

3. **Rule Verification Requirements**:
   - Every reusable-rule or consumer-wiring change must include executable evidence that the repository/root `check` path reaches the testing suite's `architectureTest`, that configured production targets are compiled and inspected, and that the intended rule can fail against a real compiled consumer fixture.

4. **Git Workflow**:
   - Create a focused topic branch (`working/<topic>`) targeting `main`.
   - Validate locally with `./gradlew check` (Java 25).
   - Follow standard review flow with visible review or Owner Self-Review records before promotion.
