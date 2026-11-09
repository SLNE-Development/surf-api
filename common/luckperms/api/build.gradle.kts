// surf-api's LuckPerms access: the `LuckPermsAccess` entry point that fetches or loads a `User`,
// the cached-meta extensions on `User` -- prefix, suffix, weight and the typed `getMeta` family --
// and the two `Audience` extensions that resolve a viewer to its LuckPerms user.
//
// The plan sent this file to `surf-api-messages-api`, because the messages domain was what blocked
// it. Reading it says the dependency is one call wide: `Audience.getLuckPermsUserOrNull` reads
// `messages.adventure.uuidOrNull` to get a viewer's UUID, and nothing else here is text at all.
// Everything else in the file is `net.luckperms.api`, so moving it into the text module would have
// put the LuckPerms api on the surface of the module a plugin pulls in for the `Colors` palette,
// and filed permission lookups under `dev.slne.surf.api.messages`. It is its own domain, one file
// wide for now.
//
// It sits downstream of `surf-api-messages-api` rather than being re-exported by it, the shape
// `surf-api-config-api` has. `surf-api-core-api` re-exports this module, so everything compiling
// against surf-api-core keeps resolving these declarations unchanged.
//
// The libraries below are the ones the moved file names, each in the configuration
// `surf-api-core-api` declared it in, so a consumer's compile classpath is what it was before the
// move.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `Audience.getLuckPermsUserOrNull` resolves the viewer's UUID with `adventure.uuidOrNull`.
    api(projects.surfApiMessagesApi)

    // `LuckPerms`, `LuckPermsProvider` and `User`, which every declaration here names.
    compileOnlyApi(libs.luckperms)
    // `Audience`, which the two extensions are declared on.
    compileOnlyApi(libs.adventure.api)

    // `loadUser` awaits LuckPerms' `CompletableFuture`.
    api(libs.bundles.kotlin.coroutines)
}

description = "surf-api-luckperms-api"
