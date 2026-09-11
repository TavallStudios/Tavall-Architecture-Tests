# Tavall Architecture Tests Agent Guidance

This repository is the canonical executable source for Tavall Studios architecture tests.

Before creating or materially changing architecture behavior:

1. Read the current shared policy in `TavallStudios/tavall-docs`, including `docs/quality/GIT_WORKFLOW.md` and the relevant architecture/quality documents.
2. Read the applicable tests and reusable rule modules in this repository before proposing class ownership, DI, lifecycle, runtime, module, or API-boundary changes.
3. Treat `modules/` and the consumer Gradle plugin as the maintained executable architecture-test source. Files under `repositories/` are provenance/migration snapshots, not the consumer execution mechanism.
4. Do not copy reusable canonical rule source into consumers. The default consumer boundary is the repository's canonical `*-test-suite` (or equivalent root verification suite for a genuinely single-module repository). That suite applies `org.tavall.architecture-tests`, selects applicable modules, and declares the real production projects whose `main` classes/source roots/runtime classpaths are inspected. Production subprojects should not each run duplicate canonical gates.
5. Keep repository-specific adapters, runtime simulations, and explicit shrinking migration debt local to the consumer repository testing suite.
6. Preserve imported snapshot contents and repository-relative paths during migration/provenance-only changes. Record source commit and blob provenance in `manifest/sources.json` for imported files.
7. Every reusable-rule or consumer-wiring change must include executable evidence that the repository/root `check` path reaches the testing suite's `architectureTest`, that configured production targets are compiled and inspected, and that the intended rule can fail against a real compiled consumer fixture.
8. Use focused `working/*` branches and pull requests according to the canonical Tavall Git workflow. Owner promotion still requires a visible Owner Self-Review record when independent review is unavailable.

`tavall-docs` remains the human-readable Tavall-wide architecture authority. This repository owns the mechanically enforceable subset it actually implements. When architecture rules intentionally change, update the canonical executable rule here in the same review graph as the implementation change, and update shared documentation in `tavall-docs` when policy or design guidance changes.
