// surf-api's serialization surface: the kotlinx.serialization `KSerializer` implementations for
// Adventure, the JDK and SpongePowered math types, the `SerializersModule`s that group them, and
// the DataFixerUpper `Codec` extensions.
//
// It has no project dependency at all, which is the point of cutting it out first: nothing here
// names a surf-api contract, so `surf-api-core-api` can re-export it with `api(...)` and no module
// in the repository needs a build-file change.
//
// Configuration deliberately stayed behind. `SpongeConfigSerializers` and the Configurate
// `TypeSerializer`s it registers read the config constraint annotations and the config helper types,
// so they belong to the configuration domain and move with it, not with this cut.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // kotlin.stdlib.default.dependency is off, and this module has no project dependency that
    // would bring the standard library in transitively.
    api(libs.kotlin.stdlib)
    api(libs.bundles.kotlin.serialization)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.spongepowered.math)

    api(libs.datafixerupper) { isTransitive = false }
}

tasks {
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-serialization-api"
