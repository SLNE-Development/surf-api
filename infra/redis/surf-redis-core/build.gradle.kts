plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

dependencies {
    api(project(":surf-redis-api"))

    api(platform("io.netty:netty-bom:4.2.9.Final"))

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