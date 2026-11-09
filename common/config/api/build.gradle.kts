// surf-api's configuration surface: the Configurate loader presets and `SpongeConfigManager`, the
// `SpongeConfigClass` companions, `SurfConfigApi`, the versioned migrations, the ~25 constraint
// annotations, the three-state helper types and the `TypeSerializer`s that read and write them.
//
// Unlike `surf-api-serialization-api`, this module depends on `surf-api-core-api` rather than being
// re-exported by it: `SpongeConfigSerializers.ComponentSerializer` reads MiniMessage through
// `SurfMiniMessageHolder`, and `SpongeConfigManager` and `ConfigMigrationBuilder` log through
// `core.util.logger`. Both live in surf-api-core-api, so the configuration domain sits downstream of
// it the way `surf-api-inventory-api` does, and the aggregates and the platform api modules export
// it on to consumers.
//
// There is deliberately nothing else here: everything the configuration domain compiles against was
// already on surf-api-core-api's api and compileOnlyApi surface, so inheriting that surface keeps
// the compile classpath of these files exactly what it was before the move.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
}

description = "surf-api-config-api"
