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
            description = "Runs selected canonical Tavall architecture-test modules against a consumer project."
        }
    }
}

tasks.test {
    dependsOn(
        ":modules:core:publishToMavenLocal",
        ":modules:patterns:publishToMavenLocal",
    )
    systemProperty("tavall.architecture.testVersion", project.version.toString())
}
