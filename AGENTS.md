# Tavall Architecture Tests Agent Guidance

This repository is the canonical executable source for Tavall Studios architecture tests.

Before creating or materially changing architecture behavior:

1. Read the current shared policy in `TavallStudios/tavall-docs`, including `docs/quality/GIT_WORKFLOW.md` and the relevant architecture/quality documents.
2. Read the applicable tests and reusable rule modules in this repository before proposing class ownership, DI, lifecycle, runtime, module, or API-boundary changes.
3. Treat `modules/` and the consumer Gradle plugin as the maintained executable architecture-test source. Files under `repositories/` are provenance/migration snapshots, not the consumer execution mechanism.
4. Do not copy reusable canonical rule source into consumers. Consumers apply `org.tavall.architecture-tests`, opt into applicable modules, and keep only repository-specific adapters, runtime simulations, and explicit shrinking migration debt locally.
5. Preserve imported snapshot contents and repository-relative paths during migration/provenance-only changes. Record source commit and blob provenance in `manifest/sources.json` for imported files.
6. Every reusable-rule or consumer-wiring change must include executable evidence that the consumer `check` path reaches `architectureTest` and that the intended rule can fail against a real compiled consumer fixture.
7. Use focused `working/*` branches and pull requests according to the canonical Tavall Git workflow. Owner promotion still requires a visible Owner Self-Review record when independent review is unavailable.

When architecture rules intentionally change, update the canonical executable rule here in the same review graph as the implementation change, and update shared documentation in `tavall-docs` when policy or design guidance changes.
