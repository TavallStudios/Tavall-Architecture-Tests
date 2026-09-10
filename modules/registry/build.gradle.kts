plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    compileOnly("org.tavall:tavall-registry:1.0.0")
}
