plugins {
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

gradlePlugin {
    plugins {
        create("tavallArchitectureTests") {
            id = "org.tavall.architecture-tests"
            implementationClass = "org.tavall.architecture.gradle.TavallArchitectureTestsPlugin"
            displayName = "Tavall Architecture Tests"
            description = "Runs selected canonical Tavall architecture-test modules against production targets owned by a repository test suite."
        }
    }
}

tasks.test {
    val privateRepository = providers.gradleProperty("tavallPrivateMavenRepository")
        .orElse(providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY"))
        .orElse("/srv/dev-storage/deps/private")
    dependsOn(
        ":modules:core:publishAllPublicationsToTavallPrivateRepository",
        ":modules:patterns:publishAllPublicationsToTavallPrivateRepository",
    )
    environment("TAVALL_PRIVATE_MAVEN_REPOSITORY", privateRepository.get())
    systemProperty("tavall.architecture.testVersion", project.version.toString())
    testLogging {
        events("failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}
