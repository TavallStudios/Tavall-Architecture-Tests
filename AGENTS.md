# Tavall Architecture Tests Agent Guidance

This repository is the canonical executable source for Tavall Studios architecture tests.

Before creating or materially changing architecture behavior:

1. Read the current shared policy in `TavallStudios/tavall-docs`, including `docs/quality/GIT_WORKFLOW.md` and the relevant architecture/quality documents.
2. Read the applicable tests and reusable rule modules in this repository before proposing class ownership, DI, lifecycle, runtime, module, API-boundary, or test-authoring changes.
3. Treat `modules/` and the consumer Gradle plugin as the maintained executable architecture-test source. Files under `repositories/` are provenance/migration snapshots, not the consumer execution mechanism.
4. Do not copy reusable canonical rule source into consumers. The default consumer boundary is the repository's canonical `*-test-suite` (or equivalent root verification suite for a genuinely single-module repository). That suite applies `org.tavall.architecture-tests`, selects applicable modules, and declares the real production projects whose `main` classes/source roots/runtime classpaths are inspected. Production subprojects should not each run duplicate canonical gates.
5. Feed the configured production targets' test-source roots through the same suite boundary. Test source is inspected as evidence about how production behavior is tested; it never replaces inspection of the real production classes.
6. For Java production or test edits, run the suite's `architectureAnalyze` task after each coherent edit boundary. In a durable interactive development environment, prefer `architectureAnalyze --continuous` while editing so changed production classes are rebuilt and re-assessed continuously. The repository/root `check` remains the authoritative completion gate.
7. Before creating a direct behavior test for a production type, use the canonical test-authoring contract. `generateTavallTestScaffold -PtavallTestClass=<production FQCN>` may create the initial package/class/JUnit shape, but its generated test intentionally fails and is also rejected by the analyzer until replaced with real behavior assertions.
8. Never treat a numeric architecture score as permission to ignore a blocking finding. Every discovered production class receives a per-class assessment; an unbaselined blocking finding fails that class regardless of score. Architecture debt must remain explicit and shrinking.
9. Findings should carry exact source file and line/column evidence whenever the rule is source-backed. New source-aware rules should use the canonical source index/location model rather than emitting whole-file-only errors when an exact location is mechanically available.
10. Keep repository-specific adapters, runtime simulations, and explicit shrinking migration debt local to the consumer repository testing suite.
11. Preserve imported snapshot contents and repository-relative paths during migration/provenance-only changes. Record source commit and blob provenance in `manifest/sources.json` for imported files.
12. Every reusable-rule or consumer-wiring change must include executable evidence that the repository/root `check` path reaches the testing suite's `architectureTest`, that configured production targets are compiled and inspected, that `architectureAnalyze` reports per-class evidence, and that the intended rule can fail against a real compiled consumer fixture.
13. Use focused `working/*` branches and pull requests according to the canonical Tavall Git workflow. Owner promotion still requires a visible Owner Self-Review record when independent review is unavailable.

`tavall-docs` remains the human-readable Tavall-wide architecture authority. This repository owns the mechanically enforceable subset it actually implements. When architecture rules intentionally change, update the canonical executable rule here in the same review graph as the implementation change, and update shared documentation in `tavall-docs` when policy or design guidance changes.
