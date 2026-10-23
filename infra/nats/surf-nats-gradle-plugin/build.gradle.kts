import java.io.StringReader
import java.util.Properties

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
}

group = "dev.slne.surf.nats"
version = providers.fileContents(layout.projectDirectory.file("../gradle.properties")).asText
    .map { text -> Properties().apply { load(StringReader(text)) }.getProperty("version") }
    .get()

kotlin {
    jvmToolchain(25)
}

val bundledProtoDirs = listOf(
    layout.projectDirectory.dir("../surf-nats-types/surf-nats-types-common/src/main/proto"),
    layout.projectDirectory.dir("../surf-nats-types/surf-nats-types-paper/src/main/proto"),
)

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }
    forClass("dev.slne.surf.nats.gradle", "SurfNatsPluginVersion") {
        buildConfigField("GROUP", provider { project.group.toString() })
        buildConfigField("VERSION", provider { project.version.toString() })
        buildConfigField("KOTLINX_SERIALIZATION_VERSION", libs.versions.kotlinx.serialization)
    }
    forClass("dev.slne.surf.nats.gradle.schema", "BundledProtoPaths") {
        buildConfigField("PATHS", provider {
            bundledProtoDirs.flatMap { dir ->
                val root = dir.asFile
                fileTree(root) { include("**/*.proto") }.files.map { it.relativeTo(root).invariantSeparatorsPath }
            }.sorted()
        })
    }
}

sourceSets {
    main {
        // Same protocol rules as the surf-nats-core runtime (also compiled there for JVM and JS).
        kotlin.srcDir("../surf-nats-core/src/protocol/kotlin")
        bundledProtoDirs.forEach { resources.srcDir(it) }
    }
    test {
        kotlin.srcDir("../surf-nats-core/src/protocolTest/kotlin")
    }
}

dependencies {
    implementation(libs.wire.schema) {
        exclude(group = "com.google.guava")
        exclude(group = "com.palantir.javapoet")
        exclude(group = "com.squareup", module = "kotlinpoet")
    }
    implementation(libs.kotlinpoet)
    // Applied by the plugin for Kotlin/JVM projects; surf-api projects get it from surf-api as well.
    implementation(libs.kotlin.serialization.plugin)

    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.gradle.api)
    compileOnly(libs.surf.api.gradle.plugin)

    testImplementation(libs.kotlin.gradle.plugin)
    testImplementation(libs.surf.api.gradle.plugin)
    testImplementation(gradleKotlinDsl())
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

testing {
    suites {
        // A consumer classpath without surf-api, whose plugin jar brings the serialization plugin to the other tests.
        register<JvmTestSuite>("plainJvmTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(gradleApi())
                implementation(libs.kotlin.gradle.plugin)
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("plainJvmTest"))
}

gradlePlugin {
    plugins {
        create("surfNats") {
            id = "dev.slne.surf.nats"
            implementationClass = "dev.slne.surf.nats.gradle.SurfNatsPlugin"
            displayName = "surf-nats"
            description = "Generates surf-nats Kotlin contracts from .proto files"
        }
    }
}

publishing {
    repositories {
        maven("https://reposilite.slne.dev/releases/") {
            name = "slne-repository-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }
}
