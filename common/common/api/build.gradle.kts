// The shared base every other domain of surf-api needs: `logger` and the rest of `util`, the
// transforming set views, the voxel line tracer, the weighted random selector and the small caps
// table.
//
// It is cut out so the domains still inside `surf-api-core-api` can be cut out downwards. Both
// earlier cuts needed `core.util` -- `surf-api-config-api` ended up depending on
// `surf-api-core-api` partly because of `logger` -- and every remaining domain needs it too, so
// without this module each further cut would point upwards at the shrinking remains of core.
//
// It therefore has no project dependency at all, the shape `surf-api-serialization-api` has:
// `surf-api-core-api` re-exports it with `api(...)`, so everything compiling against
// surf-api-core keeps resolving these declarations.
//
// The libraries below are the ones the moved files name, declared in the same configuration
// `surf-api-core-api` declared them in, so a consumer's compile classpath is what it was before
// the move. `core.util.logger` returns a flogger `FluentLogger` and the coroutine helpers log
// through an Adventure `ComponentLogger`, which is why both are here.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // kotlin.stdlib.default.dependency is off, and this module has no project dependency that
    // would bring the standard library in transitively.
    api(libs.kotlin.stdlib)
    api(libs.bundles.kotlin.coroutines)
    // SerializableError is @Serializable.
    api(libs.bundles.kotlin.serialization)

    compileOnlyApi(libs.flogger)
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    // org.slf4j.Logger is a supertype of ComponentLogger.
    compileOnlyApi(libs.slf4j)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.commons.math3)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.spongepowered.math)
}

description = "surf-api-common-api"
