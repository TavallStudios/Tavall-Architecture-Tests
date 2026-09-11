plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("org.junit.jupiter:junit-jupiter-api:5.11.4")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}
