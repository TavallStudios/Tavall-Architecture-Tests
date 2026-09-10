plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    compileOnly("org.tavall:abstract-cache-system:1.0.0")
}
