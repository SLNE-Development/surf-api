plugins {
    java
    `maven-publish`
    id("net.linguica.maven-settings")
}

repositories {
    mavenLocal()
    maven("https://repo.slne.dev/repository/maven-proxy")
    maven("https://repo.slne.dev/repository/maven-public") { name = "maven-public" }
    maven("https://repo.slne.dev/repository/maven-snapshots") { name = "maven-snapshots" }
    maven("https://repo.slne.dev/repository/maven-releases") { name = "maven-releases" }
}
group = "dev.slne.surf"
version = "1.20.4-1.0.0-SNAPSHOT"

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
