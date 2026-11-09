// surf-api's player-facing text surface: the `Colors` palette and the prefixes, `CommonComponents`,
// the `SurfComponentBuilder` DSL with its colour helpers, the Adventure extension functions
// (`buildText`, `sendText`, the audience/pointer/key/title/book/boss-bar/sound/resource-pack DSLs),
// `ComponentMessage`, `DefaultFontInfo`, `SurfMessageBundle`, the `Pagination`/`SuspendPagination`
// contracts, the configured MiniMessage instance, the `ActionBarService` contract and `Rarity`.
//
// `Rarity` arrived with the final cut of `surf-api-core-api`. It is a six-value enum whose only
// content is a display name and a colour and whose only behaviour is `asComponent`, so it is text
// and nothing else -- which is also how `.claude/skills/surf-api-messages` already described it.
// `core.command` and `core.luckperms`, which the plan paired it with, became modules of their own
// downstream of this one: they only borrow text to render an error or to read a viewer's UUID, and
// the rest of them is CommandAPI, Brigadier and the LuckPerms api.
//
// It takes the shape `surf-api-common-api` and `surf-api-serialization-api` have: it names no
// contract that is still inside `surf-api-core-api`, so `surf-api-core-api` re-exports it with
// `api(...)` and everything compiling against surf-api-core keeps resolving these declarations.
//
// Cutting it out is what unblocks the rest: `core.command`, `core.rarity` and `core.luckperms` all
// read from this domain and could not move while it was part of core, and `surf-api-config-api`
// kept a blanket dependency on `surf-api-core-api` for `SurfMiniMessageHolder` alone.
//
// The implementations stay in `surf-api-core-impl`. `ActionBarServiceImpl` resolves players through
// `SurfApiCore`/`SurfApiCoreImpl`, which are core's, so the runtime half of this domain genuinely
// belongs to core's runtime; the pagination implementations are only there because they ship on the
// same classpath and splitting them out would buy no boundary.
//
// The libraries below are the ones the moved files name, declared in the configuration
// `surf-api-core-api` declared them in, so a consumer's compile classpath is what it was before the
// move. MiniMessage, the gson serializer and the plain-text serializer arrive through
// `surf-api-annotations`, exactly as they did for these files inside core.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `SurfMiniMessageHolder`'s `<small>` tag runs `common.font.toSmallCaps`.
    api(projects.surfApiCommonApi)
    // `InternalSurfApi`, plus the Adventure artifacts this module compiles against that no build
    // file here declares directly: MiniMessage, the gson serializer and the plain-text serializer.
    api(projects.surfApiAnnotations)
    // `ActionBarService` and `InternalPaginationBridge` resolve their implementation with
    // `surfBean`.
    api(projects.surfApiDiApi)

    // `SuspendPagination`, `ActionBarService` and the click-callback extensions.
    api(libs.bundles.kotlin.coroutines)

    // Exposed transitively by surf-api-annotations as well; declared here so the test convention
    // (testImplementation extendsFrom compileOnlyApi) puts Adventure on the test classpath.
    compileOnlyApi(libs.adventure.api)
    // `ComponentMessage` is a Brigadier `Message`.
    compileOnlyApi(libs.brigadier)
}

description = "surf-api-messages-api"
