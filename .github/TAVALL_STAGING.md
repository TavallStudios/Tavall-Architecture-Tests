# Tavall Architecture Tests Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
```

This branch is the active platform integration target for exact-source build and architecture-test changes. Its ancestry was refreshed from current `main` before new work was attached. Pull requests remain the work and review records; this file only identifies the repository's integration root.

Active child pull requests target this branch or a dependency branch that reaches it. Source-level checks, staging integration, and promotion remain separate validation steps.
