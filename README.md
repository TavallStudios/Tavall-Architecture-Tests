# Tavall Architecture Tests

This repository is the canonical executable architecture-test layer for Tavall Studios. `tavall-docs` owns the written architecture policy; this repository turns reusable parts of that policy into checks that consumer repositories actually execute.

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
    modules.set(listOf("core", "patterns", "testing", "di"))
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

The plugin registers `architectureTest`, runs it on JUnit Platform, compiles every configured target, and points the canonical engine at those targets' compiled production classes, Java source roots, runtime classpaths, and test-source roots. Production classes remain the architecture subject; test source is inspected as evidence about how that production behavior is tested.

If `targetProjects` is empty, the plugin preserves the single-project fallback and inspects the project that applies it. This is useful for genuinely single-module repositories, but repository test-suite consumption is the Tavall-wide default.

Selecting a rule module changes executable verification; it is not merely a dependency declaration. When `GITHUB_TOKEN` is available, the plugin also adds the architecture-test package repository for its module artifacts.

Canonical module artifacts are intentionally modular rather than one giant `all` artifact:

- `org.tavall:tavall-architecture-core`
- `org.tavall:tavall-architecture-patterns`
- `org.tavall:tavall-architecture-testing`
- `org.tavall:tavall-architecture-di`
- `org.tavall:tavall-architecture-registry`
- `org.tavall:tavall-architecture-cache`
- `org.tavall:tavall-architecture-database`
- `org.tavall:tavall-architecture-runtime`

`core` is always included. The plugin defaults to `core`, `patterns`, and `testing`; consumers may add the other rule families that apply to their production stack. Other executable modules add rules through the `ArchitectureRule` service-provider contract. Tavall-library compile dependencies remain compile-only in architecture modules so the gate inspects the consumer's checked-in/runtime dependency versions instead of forcing architecture-test copies of those libraries onto the consumer test runtime.

`tavall-docs` remains the human-readable architecture authority. This repository owns only the mechanically enforceable subset it actually implements. Repository-local tests may extend the canonical suite for product behavior, but they must not fork reusable Tavall-wide rules into incompatible copies.

## Per-class architecture assessment

The canonical engine produces an explicit result for every discovered `org.tavall.*` production class, including classes with zero findings.

Each class result contains:

- class identity and source location when available;
- selected rule families;
- `PASS`, `DEBT`, or `FAIL` status;
- a 0-100 compliance score;
- every finding that contributed to the score/status;
- exact source file plus line/column ranges for source-backed findings;
- the human policy reference when the rule declares one.

The score is diagnostic only. Any unbaselined blocking finding fails the class regardless of score. Baselined debt remains visible and reduces the score; it does not become invisible success.

The JUnit `architectureTest` task and the direct analyzer use the same `ArchitectureAssessmentEngine`, so CI, AI, and local developer feedback do not maintain separate interpretations of architecture status.

The machine-readable report is written to:

```text
build/reports/tavall-architecture/architecture-report.json
```

## Continuous authoring loop

For fast feedback while writing Java, run:

```text
./gradlew :<test-suite>:architectureAnalyze
```

The task compiles the configured production targets and runs the same canonical rule engine without wrapping it in the JUnit test task. In an interactive durable environment it can be kept active with Gradle continuous mode:

```text
./gradlew :<test-suite>:architectureAnalyze --continuous
```

AI agents should run `architectureAnalyze` after coherent production or test Java edits. Repository/root `check` remains the authoritative completion gate and still crosses the architecture boundary once through the canonical testing suite.

## Canonical test authoring

The `testing` module enforces the mechanically provable portion of `tavall-docs/docs/quality/code-architecture/TESTING_AND_GIT.md`.

For behavior-bearing concrete production types, canonical analysis expects a direct JUnit 5 test in the matching package/path. Interfaces, annotations, enums, records, abstract types, nested types, and types without a public/protected behavior method are not forced into meaningless one-file-per-type tests.

Direct tests are checked for mechanically reliable rules including:

- matching `<ProductionType>Test` package/path;
- JUnit 5 rather than JUnit 4;
- behavior-oriented method names rather than the `test*` prefix;
- at least one assertion/verification in each behavior test;
- no Mockito mock of the subject under test;
- no reflective `getDeclaredMethod` testing of the subject's private methods;
- production-equivalent DI composition for Tavall-managed `@DelegatesTo` behavior instead of direct construction;
- valid Java test source.

The analyzer deliberately does not pretend static source inspection can prove every human testing requirement. Infrastructure semantics, realistic fakes, failure/cleanup completeness, and whether an assertion is meaningful still require the appropriate repository test/runtime evidence.

A fail-closed scaffold can be generated before the AI writes the behavior:

```text
./gradlew :<test-suite>:generateTavallTestScaffold \
  -PtavallTestClass=org.tavall.example.PlayerService
```

The task resolves the production owner among configured architecture targets and writes the matching `src/test/java` path. Generated scaffolds intentionally call `fail(...)` and contain a canonical scaffold marker. The analyzer rejects that marker until the generated placeholder is replaced with actual behavior-oriented assertions, preventing boilerplate generation from manufacturing false green coverage.

## Migration debt

Consumers may point the plugin at a temporary debt file:

```kotlin
architectureTests {
    debtFile.set(layout.projectDirectory.file("config/architecture-debt.txt"))
}
```

Each non-comment line is `<rule-id>|<subject>`. A matching current violation is tolerated temporarily but remains visible in per-class findings and score. New violations fail. A debt entry that no longer corresponds to a real violation also fails, forcing the baseline to shrink instead of becoming an immortal ignore list.

## Canonical snapshots

The historical `repositories/` tree remains provenance for the original Project Novus architecture tests and `manifest/sources.json` pins imported blobs. Those snapshots are not the consumer execution mechanism.

## Publishing

Every module and the Gradle plugin publish as Gradle-compatible Maven artifacts to the `Tavall-Architecture-Tests` GitHub Packages repository. Supply `GITHUB_TOKEN`/`GITHUB_ACTOR` when resolving or publishing package artifacts.

## Validation boundary

For this repository, root `check` depends on every module/plugin `check`. For consumers, the repository test suite's `check` depends on `architectureTest`, and the repository root `check` must depend on that suite. Local CI/DI and promotion checks therefore cross one canonical suite boundary while still inspecting all declared production modules.
