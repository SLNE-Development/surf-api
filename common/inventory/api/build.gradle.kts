// The platform-neutral half of the inventory framework: view settings, header geometry, text
// alignment, the container components and the DSL markers and context extensions that name no
// platform type. Paper and Minestom each kept their own copy of these; they are compiled once here
// and both platforms depend on them.
//
// The dependency on inventory-framework-api is deliberate and is the line this module cannot
// cross: `inventory-framework-platform-bukkit` and `-platform-minestom` both declare
// `me.devnatan.inventoryframework.context.Context`, `RenderContext`, `View` and friends under the
// same fully qualified names with different platform types behind them. Only the `-api` artifact's
// `IF*` interfaces are the same class on both platforms, so a file that names a platform context
// has to stay in its platform module.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCoreApi)
    api(libs.inventory.framework.api)
}

description = "surf-api-inventory-api"
