plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    compileOnly("org.tavall:tavall-database-core-contracts:1.0.0")
}
