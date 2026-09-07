# Tavall Architecture Tests

This repository is the canonical Tavall Studios source for architecture tests and architecture-test reference implementations.

## Canonical layout

Imported tests live under:

```text
repositories/<source-repository>/<original-repository-relative-path>
```

Files under `repositories/` are imported without semantic edits. `manifest/sources.json` pins the exact source commit and Git blob SHA for every imported file so migrations can be verified byte-for-byte.

Shared architecture and engineering policy remains canonical in [Tavall Docs](https://github.com/TavallStudios/tavall-docs). This repository is the executable/test reference layer that agents, skills, reviewers, and engineers should consult alongside those documents.

## Initial migration

The first import is pinned to:

```text
TavallStudios/tavall-project-novus@705a17b7db22a6012f3cdefe99328129842301a0
```

It includes the complete `tavall-architecture-tests` module plus the architecture-specific tests currently colocated with Project Novus production modules.

The copied Gradle module still references Project Novus project paths. This migration establishes canonical source ownership first; standalone multi-repository execution wiring is a separate integration step so the initial move can remain 1:1 instead of quietly rewriting the tests during relocation.

## Consumer rule

Until centralized execution is wired into every consumer, repository-local copies may exist only as compatibility mirrors. Architecture-test changes should originate here first, then be synchronized into consumers. The end state is one maintained source of truth rather than a small civilization of nearly-identical test suites drifting apart.
