plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    compileOnly("org.tavall:tavall-di:1.0.0")
    testImplementation("org.tavall:tavall-di:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
