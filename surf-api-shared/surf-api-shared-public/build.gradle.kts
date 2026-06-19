@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    `core-convention`
}

kotlin {
    abiValidation()
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.adventure.serializer.legacy)
    compileOnlyApi(libs.adventure.serializer.plain)
    compileOnlyApi(libs.adventure.serializer.ansi)

    api(libs.kotlin.reflect)
    api(libs.bundles.kotlin.serialization)
}