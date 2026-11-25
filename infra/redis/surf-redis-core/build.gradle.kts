plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

surfCoreApi {
    allowInternalApi(
        "dev.slne.surf.api.shared.api.util.InternalSurfApi",
        "dev.slne.surf.redis.util.InternalRedisAPI",
    )
}

@Suppress("RedundantSuppression", "AvoidDuplicateDependencies") // different classifiers
dependencies {
    api(projects.surfApiRedis.surfApiRedisApi)
    api(platform(libs.netty.bom))

    implementation("io.netty:netty-transport-classes-io_uring")
    implementation("io.netty:netty-transport-classes-epoll")
    implementation("io.netty:netty-transport-classes-kqueue")

    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-aarch_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-riscv64" }
    }

    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-aarch_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-riscv64" }
    }

    runtimeOnly("io.netty:netty-transport-native-kqueue") {
        artifact { classifier = "osx-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-kqueue") {
        artifact { classifier = "osx-aarch_64" }
    }

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.lincheck)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("lincheck")
    }
}

val lincheckTest = tasks.register<Test>("lincheckTest") {
    description = "Runs the selected Lincheck concurrency tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    maxHeapSize = "2g"
    useJUnitPlatform {
        includeTags("lincheck")
    }
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(lincheckTest)
}

buildConfig {
    forClass("dev.slne.surf.redis", "RedisConstants") {
        buildConfigField("REDISSON_VERSION", libs.versions.redisson)
    }
}
