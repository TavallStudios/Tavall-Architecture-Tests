# Tavall Architecture Tests Progression

> **Document Type:** System Progression  
> **Source of Truth For:** Audited provenance, implementation, publication, and consumer adoption progression for the canonical Tavall Architecture Tests  
> **Must Not Define:** Written architecture policy (owned by `tavall-docs`), product-specific behavior (owned by consumer repositories)  
> **Current Status:** Production Canonical / Reusable Gradle Plugin & Modules Published / Active Across Consumers  
> **Last Audited Producer Commit:** `3e71575`  
> **Canonical Repository:** `TavallStudios/Tavall-Architecture-Tests`

## 1. About

Tracks the evolution and adoption of Tavall Studios' canonical executable architecture-testing layer, from its extraction out of Project Novus to its modular distribution and consumer integration model.

`tavall-docs` owns written architecture policy; this repository implements reusable, mechanically enforceable subsets of that policy as a Gradle plugin and test modules that consumer repositories execute during their own verification cycles.

## 2. Authority & Consumer Model

The division of authority is explicit:

- **Written Policy (`tavall-docs`)**: Canonical written architecture rules, design standards, module boundaries, and quality requirements.
- **Executable Enforcement (`Tavall-Architecture-Tests`)**: Reusable ArchUnit and AST-based rule implementations packaged as Gradle plugin and modular libraries.
- **Consumer Execution (Consumer Repositories)**: Each repository's canonical testing suite (`*-test-suite`, or root verification suite) applies `org.tavall.architecture-tests` and declares its production targets.
- **Historical Provenance (`repositories/`, `manifest/sources.json`)**: Preserved historical snapshots and imported commit hashes from Project Novus. These provide provenance only, not execution authority.

## 3. Progression Timeline

- **2026-09-07 - bootstrap architecture test repository (`2d25857`)**: Created the standalone repository to decouple architecture rule verification from individual product codebases.
- **2026-09-07 - import canonical architecture suite from Project Novus (`e1ed715`, PR #1)**: Imported core architecture tests and established initial standalone suite structure.
- **2026-09-07 - establish architecture test staging root (`f1d4258`)**: Established persistent staging root and quality validation gates.
- **2026-09-07 to 2026-09-08 - reusable CLI, MCP, and staging architecture contracts (`990f13f`, `0345070`, `fc90e8a`, `04e7be4`, `7257a4d`, PR #4)**: Defined reusable CLI/MCP contracts, prohibited new Repository type names, and distinguished release discovery from lease authority.
- **2026-09-10 - executable consumer gates (`dc8b90e`, `df7897e`, `53620bc`, `bef2f47`, PR #6, PR #3)**: Modularized architecture test runner, aligned consumer gates with runtime dependency ownership, defined repository `LOCAL_CI` entrypoint, and published complete consumer artifact graph to GitHub Packages.
- **2026-09-11 - canonical repository test-suite consumer model (`09dda79`, `90b0418`, `31b91cd`, `77aaa4f`, `c88fae9`, `9e29da6`, PR #7)**: Established repository testing suite (`*-test-suite`) as the canonical consumer boundary, replacing per-subproject duplicate gates with suite-owned target inspection.
- **2026-09-14 - update Tavall MC provenance repository name (`b38f994`, PR #9)**: Aligned provenance manifests with `tavall-mc` repository rename.
- **2026-09-19 - Tavall MC adopts canonical architecture suite**: Tavall MC consumer wired to canonical architecture-tests plugin via test suite boundary.
- **2026-09-22 - aggregate inherited DI marker debt and include compile classpaths (`0f6ca4c`, `81ae78d`, `3e71575`, PR #10)**: Aggregated retired DI marker debt handling and included consumer compile classpaths for comprehensive dependency analysis.
- **2026-09-23 - progression and lineage consolidation (`working/architecture-tests-progression-lineage-20260923`)**: Added canonical progression documentation, linked authority model to Tavall Docs, and bound repository verification contract.

## 4. Module Matrix

| Module | Published Artifact | Enforcement Scope |
| --- | --- | --- |
| `gradle-plugin` | `org.tavall.architecture-tests` | Gradle integration, task registration (`architectureTest`), target class and source resolution |
| `modules/core` | `org.tavall:tavall-architecture-core` | Core type hygiene, forbidden imports, package boundary assertions |
| `modules/patterns` | `org.tavall:tavall-architecture-patterns` | General structural patterns, exception handling rules, immutable record conventions |
| `modules/di` | `org.tavall:tavall-architecture-di` | Dependency injection hygiene, constructor injection rules, singleton/lifecycle validation |
| `modules/registry` | `org.tavall:tavall-architecture-registry` | Registry registration patterns and lookup lifecycle enforcement |
| `modules/cache` | `org.tavall:tavall-architecture-cache` | Cache boundaries, TTL declarations, and thread-safety invariants |
| `modules/database` | `org.tavall:tavall-architecture-database` | Persistence isolation, transaction boundary rules, and entity constraints |
| `modules/runtime` | `org.tavall:tavall-architecture-runtime` | Shared runtime verification helpers and test fixture boundaries |

## 5. Validation and Acceptance Gates

1. **Standalone Suite Validation**: Root `./gradlew check` validates all rule modules and executes Gradle plugin functional tests.
2. **Package Publication Gate**: All artifacts publish to `TavallStudios/Tavall-Architecture-Tests` on GitHub Packages with verified POMs and Gradle metadata.
3. **Consumer Verification Gate**: Consumers execute `architectureTest` through their repository test-suite boundary; failure blocks local CI and PR promotion.
4. **Temporary Debt Gate**: Deprecated or legacy patterns permitted temporarily via `config/architecture-debt.txt`; non-matching entries cause immediate build failure to prevent debt creep.
