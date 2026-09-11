# Tavall Architecture Tests

This repository is the canonical executable architecture-test layer for Tavall Studios. `tavall-docs` owns the written architecture policy; this repository turns reusable parts of that policy into tests that consumer repositories actually execute.

## Consumer contract

Do not copy canonical test source into consumers. A repository's canonical testing suite (`*-test-suite`, or the equivalent root verification suite for a single-module repository) consumes this repository's published Gradle plugin/modules and feeds the real production modules it owns through that boundary.

The testing suite is the repository-level architecture verification boundary. Production subprojects do not each need to apply and execute a duplicate architecture gate.

Because the plugin is published through GitHub Packages, consumers resolve its plugin marker from this repository in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.pkg.github.com/TavallStudios/Tavall-Architecture-Tests") {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "github"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        gradlePluginPortal()
    }
}
```

Then enable canonical architecture execution in the repository testing suite and declare the production projects it owns:

```kotlin
plugins {
    id("org.tavall.architecture-tests") version "<version>"
}

architectureTests {
    modules.set(listOf("core", "patterns", "di"))
    targetProjects.set(
        listOf(
            ":backend-api",
            ":runtime",
            ":discord",
        )
    )
}

rootProject.tasks.named("check") {
    dependsOn(tasks.named("check"))
}
```

The plugin registers `architectureTest`, runs it on JUnit Platform, compiles every configured target, and points the canonical engine at those targets' compiled production classes, Java source roots, and runtime classpaths. The suite therefore validates the same `main` types that the repository actually builds and ships.

If `targetProjects` is empty, the plugin preserves the single-project fallback and inspects the project that applies it. This is useful for genuinely single-module repositories, but repository test-suite consumption is the Tavall-wide default.

Selecting a rule module changes executable verification; it is not merely a dependency declaration. When `GITHUB_TOKEN` is available, the plugin also adds the architecture-test package repository for its module artifacts.

Canonical module artifacts are intentionally opt-in rather than one giant `all` artifact:

- `org.tavall:tavall-architecture-core`
- `org.tavall:tavall-architecture-patterns`
- `org.tavall:tavall-architecture-di`
- `org.tavall:tavall-architecture-registry`
- `org.tavall:tavall-architecture-cache`
- `org.tavall:tavall-architecture-database`
- `org.tavall:tavall-architecture-runtime`

`core` is always included by the plugin. Other executable modules add rules through the `ArchitectureRule` service-provider contract. Tavall-library compile dependencies are compile-only in architecture modules so the gate inspects the consumer's checked-in/runtime dependency versions instead of forcing architecture-test copies of those libraries onto the consumer test runtime. `runtime` supplies shared runtime-test support; product/runtime simulations that require Paper, Discord, Redis, PostgreSQL, or another real runtime remain owned by the consumer repository's testing suite.

`tavall-docs` remains the human-readable architecture authority. This repository owns only the mechanically enforceable subset it actually implements. Repository-local tests may extend the canonical suite for product behavior, but they must not fork reusable Tavall-wide rules into incompatible copies.

## Migration debt

Consumers may point the plugin at a temporary debt file:

```kotlin
architectureTests {
    debtFile.set(layout.projectDirectory.file("config/architecture-debt.txt"))
}
```

Each non-comment line is `<rule-id>|<subject>`. A matching current violation is tolerated temporarily. New violations fail. A debt entry that no longer corresponds to a real violation also fails, forcing the baseline to shrink instead of becoming an immortal ignore list.

## Canonical snapshots

The historical `repositories/` tree remains provenance for the original Project Novus architecture tests and `manifest/sources.json` pins imported blobs. Those snapshots are not the consumer execution mechanism.

## Publishing

Every module and the Gradle plugin publish as Gradle-compatible Maven artifacts to the `Tavall-Architecture-Tests` GitHub Packages repository. Supply `GITHUB_TOKEN`/`GITHUB_ACTOR` when resolving or publishing package artifacts.

## Validation boundary

For this repository, root `check` depends on every module/plugin `check`. For consumers, the repository test suite's `check` depends on `architectureTest`, and the repository root `check` must depend on that suite. Local CI/DI and promotion checks therefore cross one canonical suite boundary while still inspecting all declared production modules.
