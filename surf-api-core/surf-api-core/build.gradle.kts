import org.gradle.api.internal.artifacts.dsl.dependencies.DependenciesExtensionModule.module

plugins {
    `core-convention`
    `api-validation`
    idea
}

val generatedFastutilSources = layout.buildDirectory.dir("generated/sources/fastutil/main/kotlin")
val generateFastutilExtensions by tasks.registering(GenerateFastutilExtensions::class) {
    description = "Generates fastutil extension functions for primitive collections."
    group = "codegen"
    templateDirectory.set(layout.projectDirectory.dir("src/codegen/fastutil"))
    outputDirectory.set(generatedFastutilSources)
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(generateFastutilExtensions)
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateFastutilExtensions)
}

tasks.named("sourcesJar") {
    dependsOn(generateFastutilExtensions)
}

idea {
    module {
        generatedSourceDirs.add(generatedFastutilSources.get().asFile)
    }
}

dependencies {
    api(projects.surfApiShared.surfApiSharedPublic)
    api(libs.adventure.nbt)
    compileOnlyApi(libs.luckperms)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.dazzleconf)
    compileOnlyApi(libs.spongepowered.math)
    compileOnlyApi(libs.okhttp)
    api(libs.okhttp.kotlin)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    api(libs.configurate.yaml)
    api(libs.configurate.jackson)
    api(libs.configurate.kotlin)
    compileOnlyApi(libs.flogger)
    compileOnlyApi(libs.commons.math4.core)
    compileOnlyApi(libs.commons.math3)
    compileOnlyApi(libs.aide.reflection)
    api(libs.glm)

    api(libs.caffeine.courotines)
    api(libs.bundles.kotlin.coroutines)
    api(libs.bundles.reactor.netty)

    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.gson)
    compileOnlyApi(libs.commons.lang3)
    compileOnlyApi(libs.commons.text)
    compileOnlyApi(libs.fastutil)

    api(libs.bundles.ktor.client)

    api(libs.datafixerupper) { isTransitive = false }

    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.fastutil)
    testRuntimeOnly(libs.flogger)
    testRuntimeOnly(libs.flogger.slf4j.backend)
    testRuntimeOnly(libs.commons.lang3)
    testRuntimeOnly(libs.commons.math3)
    testRuntimeOnly(libs.spongepowered.math)
    testRuntimeOnly(libs.adventure.api)
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-core"
