// surf-api's configuration surface: the Configurate loader presets and `SpongeConfigManager`, the
// `SpongeConfigClass` companions, `SurfConfigApi`, the versioned migrations, the ~25 constraint
// annotations, the three-state helper types and the `TypeSerializer`s that read and write them.
//
// This module used to carry a blanket `api(projects.surfApiCoreApi)` for two declarations:
// `common.util.logger`, which `SpongeConfigManager` and `ConfigMigrationBuilder` log through, and
// `SurfMiniMessageHolder`, which `SpongeConfigSerializers.ComponentSerializer` reads MiniMessage
// through. Both have since been cut out of `surf-api-core-api` into `surf-api-common-api` and
// `surf-api-messages-api`, so nothing the configuration domain compiles against is left in core and
// the dependency is narrowed to those two modules. The configuration domain still sits downstream of
// them -- it names their declarations, so it cannot be re-exported the way
// `surf-api-serialization-api` is -- and the aggregates and the platform api modules keep exporting
// it on to consumers.
//
// The library declarations below are the ones these files name and used to inherit from
// `surf-api-core-api`'s own surface, each in the configuration core declared it in, so the compile
// classpath of the configuration domain is what it was while the blanket dependency stood.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `SpongeConfigManager` and `ConfigMigrationBuilder` log through `common.util.logger`.
    api(projects.surfApiCommonApi)
    // `SpongeConfigSerializers.ComponentSerializer` reads MiniMessage through
    // `SurfMiniMessageHolder`, and the Adventure artifacts the serializers compile against arrive
    // with it.
    api(projects.surfApiMessagesApi)
    // `SurfConfigApi` is resolved with `surfBean`, and its implementation is a `@Service`.
    api(projects.surfApiDiApi)

    api(libs.configurate.yaml)
    api(libs.configurate.jackson)
    api(libs.configurate.kotlin)
    compileOnlyApi(libs.commons.lang3)
}

description = "surf-api-config-api"
