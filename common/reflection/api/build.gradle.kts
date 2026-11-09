// surf-api's reflection surface: the `SurfReflection` contract that turns a `@SurfProxy` interface
// into a dynamic proxy over a target class, and the annotations such an interface is written with --
// `@SurfProxy`, `@Field`, `@Constructor`, `@Static` and `@Name`.
//
// It is the half of the plan's nbt cut that the code refused to merge. `reflection` and `nbt` share
// no import, name no declaration of each other and have disjoint consumers: the four Paper NMS
// bridges and their channel injectors, Velocity's event-manager proxies and the standalone
// bootstrap test reach for this module, none of them for tags. Filing it under
// `dev.slne.surf.api.nbt` would have named it after a domain it has nothing to do with.
//
// Nothing here names a contract still inside `surf-api-core-api`, so core re-exports the module
// with `api(...)` and everything compiling against surf-api-core keeps resolving the proxy
// declarations unchanged. `SurfReflectionImpl` stays in `surf-api-core-impl`: nothing compiles
// against it, so a module of its own would buy no boundary.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `SurfReflection.INSTANCE` resolves the implementation with `surfBean`.
    api(projects.surfApiDiApi)
    // `InternalSurfApi`, which the `surfBean` call opts into, and the Kotlin reflection artifact
    // `@SurfProxy`'s `KClass` parameter arrives with.
    api(projects.surfApiAnnotations)
}

description = "surf-api-reflection-api"
