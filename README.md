# Tavall Architecture Tests

This repository is the canonical executable architecture-test layer for Tavall Studios. `tavall-docs` owns the written architecture policy; this repository turns reusable parts of that policy into tests that consumer repositories actually execute.

## Consumer contract

Do not copy canonical test source into consumers. Apply the Gradle plugin, select the modules that apply to the repository, and keep only repository-specific adapters, runtime simulations, and temporary migration debt locally.

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

Then enable canonical architecture execution in the Java project:

```kotlin
plugins {
    id("org.tavall.architecture-tests") version "<version>"
}

architectureTests {
    modules.set(listOf("core", "patterns", "di"))
}
```

The plugin registers `architectureTest`, runs it on JUnit Platform, points it at the consumer's compiled production classes and Java source roots, and wires it into `check`. Selecting a module therefore changes executable verification; it is not merely a dependency declaration. When `GITHUB_TOKEN` is available, the plugin also adds the architecture-test package repository for its module artifacts.

Canonical module artifacts are intentionally opt-in rather than one giant `all` artifact:

- `org.tavall:tavall-architecture-core`
- `org.tavall:tavall-architecture-patterns`
- `org.tavall:tavall-architecture-di`
- `org.tavall:tavall-architecture-registry`
- `org.tavall:tavall-architecture-cache`
- `org.tavall:tavall-architecture-database`
- `org.tavall:tavall-architecture-runtime`

`core` is always included by the plugin. Other executable modules add rules through the `ArchitectureRule` service-provider contract. Tavall-library compile dependencies are compile-only in architecture modules so the gate inspects the consumer's checked-in/runtime dependency versions instead of forcing architecture-test copies of those libraries onto the consumer test runtime. `runtime` supplies shared runtime-test support; product/runtime simulations that require Paper, Discord, Redis, PostgreSQL, or another real runtime remain owned by the consumer repository.

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

For this repository, root `check` depends on every module/plugin `check`. For consumers, `check` depends on `architectureTest`, so local CI/DI and promotion checks cannot accidentally skip canonical architecture verification after the plugin is applied.
