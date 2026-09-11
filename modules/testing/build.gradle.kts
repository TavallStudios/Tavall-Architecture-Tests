plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":modules:core"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}
