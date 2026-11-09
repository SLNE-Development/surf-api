// The shared base every other domain of surf-api needs: `logger` and the rest of `util`, the
// transforming set views, the voxel line tracer, the weighted random selector and the small caps
// table, plus the two pieces the final cut of `surf-api-core-api` added -- the `algorithms` the
// convex hull and the topological sort live in, and the `invoker` bridge that calls a suspend
// `MethodHandle` from a JVM hidden class.
//
// The invoker is here rather than with the event system, which the plan offered as the
// alternative, because the event system is not its only user: Velocity's `SuspendingEventHandler`
// uses it on Velocity's own `EventManager`, `InternalInvokerApi` advertises it to surf-* plugins at
// large, and the event *contracts* never name it -- only implementations do. It is a general
// suspend-dispatch bridge, which is what the base is for.
//
// It is cut out so the domains still inside `surf-api-core-api` can be cut out downwards. Both
// earlier cuts needed `core.util` -- `surf-api-config-api` ended up depending on
// `surf-api-core-api` partly because of `logger` -- and every remaining domain needs it too, so
// without this module each further cut would point upwards at the shrinking remains of core.
//
// It takes the shape `surf-api-serialization-api` has: `surf-api-core-api` re-exports it with
// `api(...)`, so everything compiling against surf-api-core keeps resolving these declarations. Its
// one project dependency is `surf-api-annotations`, which the suspend invoker bridge needs for the
// `@InternalInvokerApi` opt-in marker; that module depends on nothing itself, so the base still
// sits at the bottom of the graph.
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
    // `@InternalInvokerApi`, which the invoker bridge is marked with.
    api(projects.surfApiAnnotations)

    // kotlin.stdlib.default.dependency is off, and surf-api-annotations only brings the standard
    // library along with kotlin-reflect, which is not something to rely on.
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
