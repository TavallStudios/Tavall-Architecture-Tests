import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.io.File

plugins {
    `java-library`
}

val tavallProductionProjects = listOf(
    ":minecraft-framework:backend-api",
    ":minecraft-framework:kingdom-game-api",
    ":minecraft-framework:minecraft-framework-core",
    ":minecraft-framework:minecraft-framework-game",
    ":minecraft-framework:minecraft-nms-framework",
    ":minecraft-framework:novus-essentials",
    ":minecraft-kingdom-server",
    ":minecraft-kingdom-proxy",
    ":minecraft-cloud",
    ":novus-ffa",
    ":novus-achievements",
    ":novus-lobby",
    ":novus-web",
    ":novus-discord:novus-discord-core",
    ":tavall-discord",
    ":novus-runtime",
)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.tavall:tavall-di:1.0.0")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.66-stable")
    testImplementation("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")

    tavallProductionProjects.forEach { projectPath ->
        testImplementation(project(projectPath))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    jvmArgs("--enable-preview")
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    dependsOn(tavallProductionProjects.map { projectPath ->
        project(projectPath).tasks.named("classes")
    })

    doFirst {
        val classRoots = tavallProductionProjects
            .map { projectPath ->
                project(projectPath)
                    .layout
                    .buildDirectory
                    .dir("classes/java/main")
                    .get()
                    .asFile
            }
            .filter(File::isDirectory)
            .joinToString(File.pathSeparator) { it.absolutePath }

        require(classRoots.isNotBlank()) {
            "No compiled Tavall production class roots were found."
        }
        systemProperty("tavall.architecture.classRoots", classRoots)
    }
}

rootProject.tasks.named("check") {
    dependsOn(project.tasks.named("check"))
}
