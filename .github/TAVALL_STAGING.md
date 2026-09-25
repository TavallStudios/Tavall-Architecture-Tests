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

This branch is the active platform integration target for exact-source build and architecture-test changes. Pull requests remain the work and review records; this file only identifies the repository's integration root.

The current architecture artifact and Gradle-composite work is tracked in PR #14. Its source-level checks, staging integration, and promotion state remain separate validation steps.
