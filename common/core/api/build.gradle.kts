plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // The shared base -- `logger` and the rest of `util`, the collection views, the voxel line
    // tracer, the weighted random selector and the small caps table -- was cut out of this
    // module so that the domains still here can be cut out downwards. It is re-exported so that
    // everything compiling against surf-api-core keeps resolving the helpers it already used.
    api(projects.surfApiCommonApi)
    // The messages domain -- the palette and the prefixes, the SurfComponentBuilder DSL, the
    // Adventure extensions, ComponentMessage, DefaultFontInfo, SurfMessageBundle, pagination, the
    // configured MiniMessage instance and the ActionBarService contract -- was cut out of this
    // module. It is re-exported so that everything compiling against surf-api-core keeps resolving
    // the text declarations it already used.
    api(projects.surfApiMessagesApi)
    // The event domain -- the SurfEvent hierarchy, the @SurfEventHandler annotation, the
    // priorities and the SurfEventBus contract -- was cut out of this module. It is re-exported so
    // that everything compiling against surf-api-core keeps resolving the event declarations it
    // already used.
    api(projects.surfApiEventApi)
    // The nbt domain -- the binary-tag views, the FastCompoundBinaryTag contract, the
    // InternalNbtBridge, the tag extensions and NbtOps -- and the reflection domain -- the
    // SurfReflection contract and the @SurfProxy annotations -- were cut out of this module. Both
    // are re-exported so that everything compiling against surf-api-core keeps resolving the
    // declarations it already used.
    api(projects.surfApiNbtApi)
    api(projects.surfApiReflectionApi)
    api(projects.surfApiRegistryKeys)
    api(projects.surfApiAnnotations)
    // The DI contracts travel with the api, not with the container: a downstream plugin writes
    // @Service and takes a BeanContainer at its entry point, and it compiles against surf-api-core
    // like everything else. Which module exports these IS the sharing decision — one exporter, so
    // the interface a SHARED bean crosses the application boundary as is loaded exactly once.
    api(projects.surfApiDiApi)
    // The serialization surface was cut out of this module; it is re-exported so that
    // everything compiling against surf-api-core keeps resolving the serializers it already
    // used.
    api(projects.surfApiSerializationApi)
    api(libs.adventure.nbt)
    // Exposed transitively by surf-api-annotations as well; declared here so the test convention
    // (testImplementation extendsFrom compileOnlyApi) puts Adventure on the test classpath.
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.luckperms)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.dazzleconf)
    compileOnlyApi(libs.spongepowered.math)
    compileOnlyApi(libs.okhttp)
    api(libs.okhttp.kotlin)
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

    api(libs.bundles.ktor.client)

    api(libs.datafixerupper) { isTransitive = false }
}

tasks {
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-core-api"
