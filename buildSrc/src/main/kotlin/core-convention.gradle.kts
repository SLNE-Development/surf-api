import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

val libs = the<LibrariesForLibs>()
val javaVersion = project.findProperty("javaVersion") as String

plugins {
    java
    `java-library`
    id("publish-convention")
    id("java-toolchain-convention")
    id("test-convention")

    kotlin("jvm")
    kotlin("plugin.serialization")
    // Opens classes carrying a Spring stereotype or `@Configuration`, which Spring subclasses for
    // proxies and configuration enhancement.
    kotlin("plugin.spring")

    id("com.gradleup.shadow")
}

val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = findProperty("group") as String
version = findProperty("version") as String + if (snapshot) "-SNAPSHOT" else ""

repositories {
    mavenCentral()
    maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
}

dependencies {
    compileOnlyApi("org.jetbrains:annotations:26.1.0")
}

extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        // Keeps parameter names in the bytecode, which Spring needs to match `@Qualifier`-less
        // dependencies and SpEL expressions to constructor and method parameters.
        javaParameters = true
    }
}

java {
    withSourcesJar()

    if (project.name != "surf-api-paper-plugin-test") {
        withJavadocJar()
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

// The shaded jar bundles Spring unrelocated, so it must not be published under the module's coordinate.
// shadowJar and the shadowRuntimeElements configuration remain for in-build consumers and release assets.
shadow {
    addShadowVariantIntoJavaComponent = false
}

tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    if (this !is com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
        // Shadow defaults to EXCLUDE, which drops every later copy of a path before the transformer runs, so only the
        // first jar's service file would survive. Several bundled jars ship the same one, such as the
        // ContextContributor registrations.
        filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }

        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("net.kyori.adventure.nbt", "$relocationPrefix.kyori.nbt") {
            exclude("net.kyori.adventure.nbt.api.**")
        }
        relocate("org.spongepowered.configurate", "$relocationPrefix.configurate")
    }

    javadoc {
        isFailOnError = false
        val options = options as StandardJavadocDocletOptions
        options.use()
        options.tags("apiNote:a:API Note:")
        options.addStringOption("Xdoclint:none", "-quiet")
    }
}
