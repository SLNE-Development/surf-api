plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.adventure.serializer.legacy)
    compileOnlyApi(libs.adventure.serializer.plain)
    compileOnlyApi(libs.adventure.serializer.ansi)

    api(libs.kotlin.reflect)
    api(libs.bundles.kotlin.serialization)
}