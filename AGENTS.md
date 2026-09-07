# Tavall Architecture Tests Agent Guidance

This repository is the canonical source for Tavall Studios architecture tests.

Before creating or materially changing architecture behavior:

1. Read the current shared policy in `TavallStudios/tavall-docs`, including `docs/quality/GIT_WORKFLOW.md` and the relevant architecture/quality documents.
2. Read the applicable tests in this repository before proposing class ownership, DI, lifecycle, runtime, module, or API-boundary changes.
3. Treat files under `repositories/` as canonical architecture-test sources. Consumer-repository copies are compatibility mirrors until centralized execution wiring replaces them.
4. Preserve imported test contents and repository-relative paths during migration-only changes. Do not combine relocation with rule rewrites.
5. Record source commit and blob provenance in `manifest/sources.json` for every imported file.
6. Use focused `working/*` branches and pull requests according to the canonical Tavall Git workflow. Owner promotion still requires a visible Owner Self-Review record when independent review is unavailable.

When architecture rules intentionally change, update the canonical test here in the same review graph as the implementation change, and update shared documentation in `tavall-docs` when policy or design guidance changes.
