plugins {
    // Apply the java Plugin to add support for Java.
    java
    `maven-publish`
    id("net.linguica.maven-settings")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repository.sonatype.org/content/groups/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
}

group = "dev.slne.surf"
version = "1.0-SNAPSHOT"

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
    repositories {
        maven {
            name = "slne-space"
            url = uri(System.getenv("REPOSITORY_URL") ?: "https://packages.slne.dev/maven/p/surf/maven")
            credentials {
                username = System.getenv("JB_SPACE_CLIENT_ID")
                password = System.getenv("JB_SPACE_CLIENT_SECRET")
            }
        }
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.add("-parameters")
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}
