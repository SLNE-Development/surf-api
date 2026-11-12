// The generated key tables for blocks, items, sounds and vanilla advancements: 40,848 lines that
// change with `mcVersion` and with nothing else. Keeping them out of surf-api-core-api means a
// change to the core contracts no longer recompiles them, and a version bump no longer touches the
// core module. ABI validation is deliberately not applied: the surface here is generated, so a dump
// would change with every Minecraft version and record nothing worth reviewing.
plugins {
    id("surf.module-api")
}

dependencies {
    // kotlin.stdlib.default.dependency is off, and this module has no project dependency that
    // would bring the standard library in transitively.
    api(libs.kotlin.stdlib)
    compileOnlyApi(libs.adventure.api)
}

description = "surf-api-registry-keys"
