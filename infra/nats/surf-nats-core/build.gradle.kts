import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.inject.Inject

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.kotlin.js.plain.objects)
    id("com.github.gmazzo.buildconfig") version "6.1.2"
}

// Kotlin/JS test harness: protobuf-es and protoc-gen-surf-nats code for src/jsTest/proto and NATS servers (Docker) for
// the e2e tests.

val bufVersion = "1.75.0"
val natsTestImage = "nats:2-alpine"
val natsTestContainerPrefix = "surf-nats-core-js-test"

val rootNodeModules = rootProject.layout.buildDirectory.dir("js/node_modules")
val fixtureEsProtoDir = layout.buildDirectory.dir("es-proto")
val generatedEsDir = layout.buildDirectory.dir("generated/es")
val fixtureImageFile = layout.buildDirectory.file("generated/protoc-image/fixtures.binpb")
val protocGoldenDir = layout.projectDirectory.dir("src/jsTest/protoc/golden")
// Below the root build/js, so Node resolves the npm dependencies of the staged package from the root node_modules
val protocPluginDir = rootProject.layout.buildDirectory.dir("js/surf-nats-protoc")

val prepareProtocPlugin = tasks.register<Sync>("prepareProtocPlugin") {
    description = "Stages the npm distribution, whose bin protoc-gen-surf-nats generateFixtureEs runs."
    into(protocPluginDir)
    from(tasks.named("jsNodeProductionLibraryDistribution"))
}
val natsTestUrlFile = layout.buildDirectory.file("nats-js-test/url.txt")
val natsTestAuthFile = layout.buildDirectory.file("nats-js-test/auth.properties")
val natsTestContainersFile = layout.buildDirectory.file("nats-js-test/containers.txt")
val jsE2eEnabled = providers.gradleProperty("surf.nats.js.e2e").map(String::toBoolean).getOrElse(true)

val prepareFixtureEsProtos = tasks.register<Sync>("prepareFixtureEsProtos") {
    description = "Stages the protos and the buf configuration for generateFixtureEs."
    val nodeExecutable = project.the<NodeJsEnvSpec>().executable
    val protocGenEs = rootNodeModules.map { it.file("@bufbuild/protoc-gen-es/bin/protoc-gen-es").asFile }
    val protocGenSurfNats = protocPluginDir.map { it.file("bin/protoc-gen-surf-nats.mjs").asFile }

    into(fixtureEsProtoDir)
    from("src/jsTest/buf") {
        filesMatching("buf.gen.yaml") {
            expand(
                "node" to nodeExecutable.get().replace('\\', '/'),
                "protocGenEs" to protocGenEs.get().invariantSeparatorsPath,
                "protocGenSurfNats" to protocGenSurfNats.get().invariantSeparatorsPath,
            )
        }
    }
    into("proto") {
        from(rootDir.resolve("surf-nats-types/surf-nats-types-common/src/main/proto"))
        from(rootDir.resolve("surf-nats-types/surf-nats-types-paper/src/main/proto"))
        from(rootDir.resolve("surf-nats-test/surf-nats-test-fixtures/src/main/proto"))
        from("src/jsTest/proto")
    }
}

val generateFixtureEs = tasks.register<Exec>("generateFixtureEs") {
    description = "Generates protobuf-es code and surf-nats clients (target=js+dts) for the JS test protos into " +
            "build/generated/es."
    dependsOn(prepareFixtureEsProtos, prepareProtocPlugin, ":kotlinNpmInstall", tasks.named("kotlinNodeJsSetup"))

    val nodeExecutable = project.the<NodeJsEnvSpec>().executable
    val buf = rootNodeModules.map { it.file("@bufbuild/buf/bin/buf").asFile.absolutePath }
    val outputDir = generatedEsDir

    inputs.dir(fixtureEsProtoDir)
    inputs.dir(protocPluginDir)
    outputs.dir(outputDir)
    workingDir(fixtureEsProtoDir)

    doFirst {
        outputDir.get().asFile.deleteRecursively()
        executable = nodeExecutable.get()
        setArgs(listOf(buf.get(), "generate", "--output", outputDir.get().asFile.absolutePath))
    }
}

// FileDescriptorSet of the JS test protos (with source info) for the in-process tests of protoc-gen-surf-nats.
val buildFixtureImage = tasks.register<Exec>("buildFixtureImage") {
    description = "Builds the JS test protos into a FileDescriptorSet for the protoc-gen-surf-nats tests."
    dependsOn(prepareFixtureEsProtos, ":kotlinNpmInstall", tasks.named("kotlinNodeJsSetup"))

    val nodeExecutable = project.the<NodeJsEnvSpec>().executable
    val buf = rootNodeModules.map { it.file("@bufbuild/buf/bin/buf").asFile.absolutePath }
    val imageFile = fixtureImageFile

    inputs.dir(fixtureEsProtoDir.map { it.dir("proto") })
    outputs.file(imageFile)
    workingDir(fixtureEsProtoDir)

    doFirst {
        imageFile.get().asFile.parentFile.mkdirs()
        executable = nodeExecutable.get()
        setArgs(listOf(buf.get(), "build", "--as-file-descriptor-set", "-o", imageFile.get().asFile.absolutePath))
    }
}

// NATS servers (Docker, random host ports) for the Kotlin/JS e2e tests: one with JetStream, one with token and one with
// user/password authentication. jsNodeTest starts them when it executes; the build service removes them when the build
// finishes. Containers of one build directory are listed in containersFile, so concurrent builds never remove each
// other's, and start() removes the leftovers of an aborted run in this build directory.
abstract class NatsTestServers : BuildService<NatsTestServers.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val image: Property<String>
        val containerPrefix: Property<String>
        val urlFile: RegularFileProperty
        val authFile: RegularFileProperty
        val containersFile: RegularFileProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        removeListedContainers()

        val servers = listOf(
            "js" to listOf("-js"),
            "token" to listOf("--auth", TOKEN),
            "user" to listOf("--user", USER, "--pass", PASS),
        ).map { (role, arguments) ->
            "${parameters.containerPrefix.get()}-$role-${UUID.randomUUID().toString().take(8)}" to arguments
        }
        // Listed before the first docker run, so close() also removes a partially started set
        write(parameters.containersFile, servers.joinToString(System.lineSeparator()) { it.first })

        val executor = Executors.newFixedThreadPool(servers.size)
        val (url, tokenUrl, userUrl) = try {
            servers
                .map { (name, arguments) -> CompletableFuture.supplyAsync({ run(name, arguments) }, executor) }
                .map(CompletableFuture<String>::join)
        } finally {
            executor.shutdown()
        }

        write(parameters.urlFile, url)
        write(
            parameters.authFile,
            listOf("tokenUrl=$tokenUrl", "token=$TOKEN", "userUrl=$userUrl", "user=$USER", "pass=$PASS")
                .joinToString(System.lineSeparator()),
        )
    }

    override fun close() = removeListedContainers()

    private fun run(name: String, arguments: List<String>): String {
        execOperations.exec {
            commandLine(
                listOf("docker", "run", "-d", "--rm", "--name", name, "-p", "127.0.0.1::4222", parameters.image.get()) +
                        arguments
            )
            standardOutput = ByteArrayOutputStream()
        }

        val port = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("docker", "port", name, "4222/tcp")
            standardOutput = port
        }
        val url = "nats://" + port.toString().lineSequence().map(String::trim).first(String::isNotEmpty)
        logger.lifecycle("NATS test server $name listens on $url")
        return url
    }

    private fun removeListedContainers() {
        val file = parameters.containersFile.get().asFile
        if (!file.isFile) return
        for (name in file.readLines().map(String::trim).filter(String::isNotEmpty)) {
            execOperations.exec {
                commandLine("docker", "rm", "-f", name)
                isIgnoreExitValue = true
                standardOutput = ByteArrayOutputStream()
                errorOutput = ByteArrayOutputStream()
            }
        }
        file.delete()
    }

    private fun write(target: RegularFileProperty, text: String) {
        val file = target.get().asFile
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    private companion object {
        const val TOKEN = "surf-nats-test-token"
        const val USER = "surf-nats-test"
        const val PASS = "surf-nats-test-pass"

        val logger: Logger = Logging.getLogger(NatsTestServers::class.java)
    }
}

val natsTestServers = gradle.sharedServices.registerIfAbsent("surfNatsJsTestServers", NatsTestServers::class.java) {
    parameters {
        image = natsTestImage
        containerPrefix = natsTestContainerPrefix
        urlFile = natsTestUrlFile
        authFile = natsTestAuthFile
        containersFile = natsTestContainersFile
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm()

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "60s"
                }
            }
        }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
        outputModuleName = "@slne/surf-nats"

        // The hand-written facade (src/jsMain/resources) adds protobuf-es generics on top of the Kotlin exports;
        // bin/protoc-gen-surf-nats.mjs runs the protoc plugin.
        compilations["main"].packageJson {
            customField("main", "index.mjs")
            customField("types", "index.d.mts")
            customField("bin", mapOf("protoc-gen-surf-nats" to "bin/protoc-gen-surf-nats.mjs"))
            customField(
                "exports", mapOf(
                    "." to mapOf(
                        "types" to "./index.d.mts",
                        "import" to "./index.mjs",
                        "default" to "./index.mjs",
                    )
                )
            )
        }
    }

    sourceSets {
        commonMain {
            // Pure-stdlib protocol rules, also compiled into the surf-nats-gradle-plugin build.
            kotlin.srcDir("src/protocol/kotlin")
            dependencies {
                api(projects.surfNatsApi)
            }
        }
        commonTest {
            kotlin.srcDir("src/protocolTest/kotlin")
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jsMain.dependencies {
            implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
            implementation(libs.kotlin.wrappers.js)
            implementation(libs.kotlin.wrappers.node)
            implementation(libs.kotlin.wrappers.web)

            implementation(npm("@nats-io/transport-node", libs.versions.nats.js.get()))
            implementation(npm("@nats-io/jetstream", libs.versions.nats.js.get()))
            implementation(npm("@nats-io/obj", libs.versions.nats.js.get()))
            implementation(peerNpm("@bufbuild/protobuf", libs.versions.bufbuild.protobuf.get()))
            // protoc-gen-surf-nats; loaded on demand, so the client runtime does not load it
            implementation(npm("@bufbuild/protoplugin", libs.versions.bufbuild.protobuf.get()))
        }
        jsTest {
            resources.srcDir(generateFixtureEs)
            dependencies {
                implementation(kotlin("test"))
                implementation(npm("@bufbuild/protobuf", libs.versions.bufbuild.protobuf.get()))
                implementation(devNpm("@bufbuild/buf", bufVersion))
                implementation(devNpm("@bufbuild/protoc-gen-es", libs.versions.bufbuild.protobuf.get()))
            }
        }
        jvmMain.dependencies {
            api(projects.surfNatsTypes.surfNatsTypesCommon)
            implementation(libs.jnats)

            compileOnly(libs.surf.api.core)
            compileOnly(libs.kotlinx.serialization.protobuf)
        }
        jvmTest.dependencies {
            implementation(projects.surfNatsTest.surfNatsTestFixtures)
            implementation(libs.surf.api.core)
            implementation(libs.testcontainers.nats)
            implementation(libs.junit.jupiter)
            implementation(kotlin("test-junit5"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
            implementation(libs.lincheck)

            runtimeOnly(libs.junit.platform.launcher)
            runtimeOnly(libs.surf.api.standalone)
            runtimeOnly(libs.bundles.log4j)
        }
    }
}

val jsInteropTag = "js-interop"

tasks.withType<Test>().configureEach {
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    systemProperty("slf4j.provider", "org.apache.logging.slf4j.SLF4JServiceProvider")
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform {
        excludeTags(jsInteropTag)
    }
}

tasks.named<KotlinJsTest>("jsNodeTest") {
    // ProtocPluginTest reads the fixture image and compares against the golden files;
    // -Psurf.nats.protoc.updateGolden=true rewrites the golden files instead.
    dependsOn(buildFixtureImage)
    inputs.file(fixtureImageFile)
    inputs.dir(protocGoldenDir)
    environment("SURF_NATS_PROTOC_IMAGE", fixtureImageFile.get().asFile.absolutePath)
    environment("SURF_NATS_PROTOC_GOLDEN_DIR", protocGoldenDir.asFile.absolutePath)
    if (providers.gradleProperty("surf.nats.protoc.updateGolden").map(String::toBoolean).getOrElse(false)) {
        environment("SURF_NATS_PROTOC_UPDATE_GOLDEN", "true")
    }

    if (jsE2eEnabled) {
        val servers = natsTestServers
        usesService(servers)
        environment("SURF_NATS_JS_TEST_URL_FILE", natsTestUrlFile.get().asFile.absolutePath)
        environment("SURF_NATS_JS_TEST_AUTH_FILE", natsTestAuthFile.get().asFile.absolutePath)
        doFirst {
            servers.get().start()
        }
    }
}

// npm distribution and generated fixtures, staged next to the consumer sample and the interop script.
val jsDistributionWithFixtures = copySpec {
    from(tasks.named("jsNodeProductionLibraryDistribution")) {
        into("node_modules/@slne/surf-nats")
    }
    from(generateFixtureEs)
    from("src/jsTest/resources/gen") {
        into("gen")
    }
}

// Type-checks a TypeScript consumer (tsc --strict) against the npm distribution and its facade.

val jsConsumerCheckDir = layout.buildDirectory.dir("js-consumer-check")

val prepareJsConsumerCheck = tasks.register<Sync>("prepareJsConsumerCheck") {
    description = "Stages the TypeScript consumer sample with the npm distribution and the generated fixtures."
    into(jsConsumerCheckDir)
    from("src/jsTest/consumer")
    with(jsDistributionWithFixtures)
    from(rootNodeModules.map { it.dir("@bufbuild/protobuf") }) {
        into("node_modules/@bufbuild/protobuf")
    }
    // The target=ts golden files of protoc-gen-surf-nats, next to the protobuf-es declarations they import
    into("golden-ts") {
        from(protocGoldenDir.dir("ts"))
        from("src/jsTest/resources/gen")
        from(generateFixtureEs.map { it.outputs.files.singleFile.resolve("gen") }) {
            include("**/*_pb.d.ts")
        }
    }
}

val checkJsConsumerTypes = tasks.register<Exec>("checkJsConsumerTypes") {
    group = "verification"
    description = "Runs tsc --strict on a consumer of the @slne/surf-nats npm distribution."
    dependsOn(prepareJsConsumerCheck)

    val nodeExecutable = project.the<NodeJsEnvSpec>().executable
    // The TypeScript version KGP pins for the package; the hoisted root copy may be an older transitive one.
    val jsPackages = rootProject.layout.buildDirectory.dir("js/packages/@slne")
    val tsc = jsPackages.map { packages ->
        listOf("surf-nats-test", "surf-nats")
            .map { packages.file("$it/node_modules/typescript/bin/tsc").asFile }
            .plus(rootNodeModules.get().file("typescript/bin/tsc").asFile)
            .first { it.isFile }
            .absolutePath
    }
    // Written after a successful run: UP-TO-DATE until the staged consumer or the TypeScript version changes
    val passedMarker = layout.buildDirectory.file("js-consumer-check.passed")

    inputs.dir(jsConsumerCheckDir)
    inputs.file(tsc.map { File(it).parentFile.parentFile.resolve("package.json") })
    outputs.file(passedMarker)
    workingDir(jsConsumerCheckDir)

    doFirst {
        executable = nodeExecutable.get()
        setArgs(listOf(tsc.get(), "-p", "tsconfig.json"))
    }
    doLast {
        passedMarker.get().asFile.writeText("")
    }
}

// JVM <-> JS interop: JsInteropTest (jvmTest classes, tag js-interop) runs src/jsTest/interop/interop.mjs on Node against
// the npm distribution and the generated fixtures. jvmTest excludes the tag and stays independent of the JS build.
// The staging directory lies below the root build/js: Node resolves the npm dependencies of the distribution by walking
// up from the importing file to the root node_modules. Node is the binary KGP downloads (NodeJsEnvSpec);
// -Psurf.nats.interop.node=<path> selects another one.

val jsInteropDir = rootProject.layout.buildDirectory.dir("js/surf-nats-interop")

val prepareJsInterop = tasks.register<Sync>("prepareJsInterop") {
    description = "Stages the interop script with the npm distribution and the generated fixtures for jvmInteropTest."
    into(jsInteropDir)
    from("src/jsTest/interop")
    with(jsDistributionWithFixtures)
}

abstract class JsInteropArguments : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val directory: DirectoryProperty

    @get:Input
    abstract val node: Property<String>

    override fun asArguments(): List<String> = listOf(
        "-Dsurf.nats.interop.dir=${directory.get().asFile.absolutePath}",
        "-Dsurf.nats.interop.node=${node.get()}",
    )
}

val jvmInteropTest = tasks.register<Test>("jvmInteropTest") {
    group = "verification"
    description = "Runs JsInteropTest: surf-nats-js on Node against the JVM runtime (Testcontainers NATS)."

    val compilation = kotlin.jvm().compilations.getByName("test")
    testClassesDirs = compilation.output.classesDirs
    classpath = files(compilation.output.allOutputs, compilation.runtimeDependencyFiles)

    useJUnitPlatform {
        includeTags(jsInteropTag)
    }

    dependsOn(prepareJsInterop)
    jvmArgumentProviders += objects.newInstance<JsInteropArguments>().apply {
        directory = jsInteropDir
        node = providers.gradleProperty("surf.nats.interop.node").orElse(project.the<NodeJsEnvSpec>().executable)
    }
    mustRunAfter("jvmTest")
}

tasks.named("check") {
    dependsOn(checkJsConsumerTypes, jvmInteropTest)
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }
    forClass("dev.slne.surf.nats", "NatsConstants") {
        buildConfigField("VERSION", provider { project.version.toString() })
    }
}
