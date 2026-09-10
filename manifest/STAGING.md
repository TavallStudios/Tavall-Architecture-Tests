# Tavall Architecture Tests Staging

This file records the persistent repository/release staging boundary for the
canonical architecture-test repository.

- Branch: `staging/architecture`
- Parent: `main`
- Role: repository/release staging
- State: `ACTIVE`
- Promotion: manual through the staging pull request

The staging pull request is the integration root for reusable architecture
contracts. Child pull requests target this branch or a dependency branch that
reaches it. GitHub pull requests and their current heads remain authoritative;
this file is only a repository-local topology marker.
