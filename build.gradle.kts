import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
}

group = "org.tavall"
version = providers.gradleProperty("tavallArchitectureVersion")
    .orElse(providers.gradleProperty("tavallVersion"))
    .orElse("1.1.0")
    .get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        val githubToken = providers.environmentVariable("GITHUB_TOKEN")
            .orElse(providers.environmentVariable("GH_TOKEN"))
            .orNull
        if (!githubToken.isNullOrBlank()) {
            listOf(
                "Tavall-Architecture-Tests",
                "tavall-di",
                "tavall-registry",
                "tavall-cache",
                "tavall-database",
            ).forEach { repository ->
                maven("https://maven.pkg.github.com/TavallStudios/$repository") {
                    name = "github${repository.replace("-", "")}"
                    credentials {
                        username = providers.environmentVariable("GITHUB_ACTOR").orElse("github").get()
                        password = githubToken
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion = JavaLanguageVersion.of(25)
            withSourcesJar()
            withJavadocJar()
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(25)
            options.encoding = "UTF-8"
            options.compilerArgs.add("-parameters")
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
        tasks.withType<Jar>().configureEach {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            manifest.attributes["Implementation-Version"] = project.version.toString()
        }
    }

        pluginManager.withPlugin("maven-publish") {
            extensions.configure<PublishingExtension> {
            if (project.path.startsWith(":modules:")) {
                publications.create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifactId = "tavall-architecture-${project.name}"
                }
                }
                repositories {
                    val privateRepository = providers.gradleProperty("tavallPrivateMavenRepository")
                        .orElse(providers.environmentVariable("TAVALL_PRIVATE_MAVEN_REPOSITORY"))
                        .orElse("/srv/dev-storage/deps/private")
                    maven {
                        name = "TavallPrivate"
                        url = uri(privateRepository.get())
                    }
                    val token = providers.environmentVariable("GITHUB_TOKEN")
                        .orElse(providers.environmentVariable("GH_TOKEN"))
                if (token.isPresent) {
                    maven {
                        name = "GitHubPackages"
                        url = uri("https://maven.pkg.github.com/TavallStudios/Tavall-Architecture-Tests")
                        credentials {
                            username = providers.environmentVariable("GITHUB_ACTOR").orElse("github").get()
                            password = token.get()
                        }
                    }
                }
            }
        }
    }
}

gradle.projectsEvaluated {
    val verificationProjects = subprojects.filter { project ->
        project.plugins.hasPlugin("java") && project.tasks.findByName("check") != null
    }
    tasks.named("check") {
        dependsOn(verificationProjects.map { project -> project.tasks.named("check") })
    }
}
