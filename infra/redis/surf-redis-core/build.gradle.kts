plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.0.9"
}

@Suppress("AvoidDuplicateDependencies") // different classifiers
dependencies {
    api(projects.surfRedisApi)
    api(platform(libs.netty.bom))

    // transport classes
    implementation("io.netty:netty-transport-classes-io_uring")
    implementation("io.netty:netty-transport-classes-epoll")
    implementation("io.netty:netty-transport-classes-kqueue")

    // io_uring natives (Linux only)
    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-aarch_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-epoll") {
        artifact { classifier = "linux-riscv64" }
    }

    // epoll natives (Linux only)
    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-aarch_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-io_uring") {
        artifact { classifier = "linux-riscv64" }
    }

    // kqueue natives (macOS only)
    runtimeOnly("io.netty:netty-transport-native-kqueue") {
        artifact { classifier = "osx-x86_64" }
    }
    runtimeOnly("io.netty:netty-transport-native-kqueue") {
        artifact { classifier = "osx-aarch_64" }
    }
}

buildConfig {
    forClass("dev.slne.surf.redis", "RedisConstants") {
        buildConfigField("REDISSON_VERSION", libs.versions.redisson)
    }
}