plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)
    api(projects.surfApiMessagesApi)
    api(projects.surfApiEventApi)
    api(projects.surfApiNbtApi)
    api(projects.surfApiReflectionApi)
    api(projects.surfApiCommandApi)
    api(projects.surfApiLuckpermsApi)
    api(projects.surfApiRegistryKeys)
    api(projects.surfApiAnnotations)
    api(projects.surfApiSpringApi)
    api(projects.surfApiSerializationApi)
    compileOnlyApi(libs.adventure.nbt)
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.luckperms)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.bundles.surf.runtime.utils)
    compileOnlyApi(libs.aide.reflection)
    compileOnlyApi(libs.glm)

    compileOnlyApi(libs.caffeine.courotines)
    compileOnlyApi(libs.bundles.kotlin.coroutines)

    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.gson)
}

description = "surf-api-core-api"
