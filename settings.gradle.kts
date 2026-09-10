rootProject.name = "tavall-architecture-tests"

include(
    ":modules:core",
    ":modules:patterns",
    ":modules:di",
    ":modules:registry",
    ":modules:cache",
    ":modules:database",
    ":modules:runtime",
    ":gradle-plugin",
)

project(":modules:core").name = "tavall-architecture-core"
project(":modules:patterns").name = "tavall-architecture-patterns"
project(":modules:di").name = "tavall-architecture-di"
project(":modules:registry").name = "tavall-architecture-registry"
project(":modules:cache").name = "tavall-architecture-cache"
project(":modules:database").name = "tavall-architecture-database"
project(":modules:runtime").name = "tavall-architecture-runtime"
project(":gradle-plugin").name = "tavall-architecture-gradle-plugin"
