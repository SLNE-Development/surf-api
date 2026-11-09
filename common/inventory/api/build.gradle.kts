// The platform-neutral half of the inventory framework: view settings, header geometry, text
// alignment, the container components and the DSL markers and context extensions that name no
// platform type, plus the `internal` package the framework measures and renders with -- the default
// font width table, the shift and pagination glyphs, the alignment maths, the slot geometry, the
// text layout, the button state sessions and the container rendering helpers. Paper and Minestom
// each kept their own copy of these; they are compiled once here and both platforms depend on them.
//
// The `internal` package arrived from `surf-api-core-api`, where it was the inventory framework's
// own measurement code sitting in core. Its consumers are this module and the two platform api
// modules, which already depend on this one, and only `ViewContainerRendering.kt` names anything
// outside Adventure and glm: `messages.builder.SurfComponentBuilder`.
//
// The dependency on inventory-framework-api is deliberate and is the line this module cannot
// cross: `inventory-framework-platform-bukkit` and `-platform-minestom` both declare
// `me.devnatan.inventoryframework.context.Context`, `RenderContext`, `View` and friends under the
// same fully qualified names with different platform types behind them. Only the `-api` artifact's
// `IF*` interfaces are the same class on both platforms, so a file that names a platform context
// has to stay in its platform module.
//
// The blanket `api(projects.surfApiCoreApi)` this module carried is gone with the move. The whole
// of it was the `internal` package plus the text declarations the components build their titles and
// row labels from, and both now resolve without core: nothing here names a declaration left in
// `surf-api-core-api`. The two platform api modules declare `api(projects.surfApiCoreApi)`
// themselves, so what a consumer of surf-api-paper or surf-api-minestom resolves is unchanged.
//
// The libraries below are the ones these files name, each in the configuration
// `surf-api-core-api` declared it in, so the compile classpath of the inventory domain is what it
// was while the blanket dependency stood.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `Colors`, the `text` extension and the `SurfComponentBuilder` DSL the titles, the row labels
    // and `ViewContainerRendering` are built with.
    api(projects.surfApiMessagesApi)
    // `InternalSurfApi`, which marks every declaration in the `internal` package.
    api(projects.surfApiAnnotations)

    api(libs.inventory.framework.api)
    // The components these files assemble and measure.
    compileOnlyApi(libs.adventure.api)
    // `ShiftGlyphs` rounds a pixel shift with `glm_.func.common.abs`.
    api(libs.glm)
}

description = "surf-api-inventory-api"
