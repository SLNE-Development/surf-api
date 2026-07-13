import me.champeau.jmh.JMHTask

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("com.github.gmazzo.buildconfig") version "6.0.9"
    id("me.champeau.jmh") version "0.7.3"
}

@Suppress("RedundantSuppression", "AvoidDuplicateDependencies") // different classifiers
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

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
    testImplementation(libs.lincheck)
    testRuntimeOnly("it.unimi.dsi:fastutil:8.5.18")

    add("jmhImplementation", "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
}

afterEvaluate {
    configurations.named("jmhRuntimeClasspath") {
        setExtendsFrom(extendsFrom.filterNot { it.name == "testRuntimeClasspath" })
    }
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

val benchmarkSmoke = providers.gradleProperty("benchmarkSmoke").isPresent

jmh {
    jmhVersion = "1.37"
    benchmarkMode = listOf("avgt")
    timeUnit = "ns"
    warmupIterations = if (benchmarkSmoke) 1 else 3
    iterations = if (benchmarkSmoke) 1 else 5
    fork = if (benchmarkSmoke) 1 else 2
    warmup = if (benchmarkSmoke) "100ms" else "1s"
    timeOnIteration = if (benchmarkSmoke) "100ms" else "1s"
    profilers = listOf("gc")
    resultFormat = "JSON"
    failOnError = true
}

tasks.register<JavaExec>("eventWireSizeReport") {
    description = "Prints JSON and binary event wire sizes for every benchmark payload."
    group = "benchmark"
    dependsOn(tasks.named("jmhClasses"))
    classpath = sourceSets["jmh"].runtimeClasspath + sourceSets["main"].runtimeClasspath
    mainClass = "dev.slne.surf.redis.event.EventWireSizeReport"
}

val jmhPlainJar = tasks.register<Jar>("jmhPlainJar") {
    description = "Builds the unshaded JMH harness used by forked benchmark JVMs."
    group = "jmh"
    dependsOn(tasks.named("jmhCompileGeneratedClasses"))
    archiveClassifier = "jmh-plain"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets["main"].output)
    from(sourceSets["jmh"].output)
    from(layout.buildDirectory.dir("jmh-generated-classes"))
    from(layout.buildDirectory.dir("jmh-generated-resources"))
}

tasks.named<JMHTask>("jmh") {
    dependsOn(jmhPlainJar)
    jarArchive = jmhPlainJar.flatMap { it.archiveFile }
}

tasks.register("codecBenchmark") {
    description = "Runs the JMH JSON-versus-binary event transport benchmarks."
    group = "benchmark"
    dependsOn(tasks.named("jmh"))
}

buildConfig {
    forClass("dev.slne.surf.redis", "RedisConstants") {
        buildConfigField("REDISSON_VERSION", libs.versions.redisson)
    }
}
