plugins {
    java
    `maven-publish`
    id("net.linguica.maven-settings")
}

repositories {
    maven("https://repo.slne.dev/repository/maven-proxy")
    maven("https://repo.slne.dev/repository/maven-public") { name = "maven-public" }
}

group = "dev.slne.surf"
version = "2.0-SNAPSHOT"

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
//    withJavadocJar() // TODO: 22.01.2024 15:14 - does not work with '@apiNote' for some weird reasons
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "maven-snapshots"
            url = uri("https://repo.slne.dev/repository/maven-snapshots/")
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
