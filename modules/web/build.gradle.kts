plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
