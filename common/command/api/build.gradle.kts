// The platform-independent part of surf-api's command surface: `SurfCommandUtil`,
// `CommandExceptionBuilder` -- which renders a CommandAPI syntax error with a caret under the
// offending character -- `WrapperCommandExceptionComponent`, which carries an Adventure component
// through Brigadier's exception types, and the `awaiting`/`awaitingOrNull` helpers that read a
// suspending argument out of `CommandArguments`.
//
// The plan sent these four files to `surf-api-messages-api`, because the messages domain was what
// blocked them: `CommandExceptionBuilder` names `Colors` and `WrapperCommandExceptionComponent`
// names `ComponentMessage`. Reading them says they are their own domain instead. Three of the four
// name CommandAPI or Brigadier types in their own signatures and only borrow text to render an
// error, so `dev.slne.surf.api.messages.command` would have filed the command surface under the
// text one, and `surf-api-messages-api` -- which a plugin pulls in for the `Colors` palette alone
// -- would have had to export `dev.jorel.commandapi` to every consumer of a chat message. The
// arguments, the executors and the registration live in the three platform api modules; this is
// the piece all three share, and it has its own skill in `.claude/skills/surf-api-commands`.
//
// It sits downstream of `surf-api-messages-api` rather than being re-exported by it, the shape
// `surf-api-config-api` has. `surf-api-core-api` re-exports this module, so everything compiling
// against surf-api-core keeps resolving these declarations unchanged.
//
// The libraries below are the ones the moved files name, each in the configuration
// `surf-api-core-api` declared it in, so a consumer's compile classpath is what it was before the
// move.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `CommandExceptionBuilder` paints its caret with `Colors`, and
    // `WrapperCommandExceptionComponent` is built on `ComponentMessage`.
    api(projects.surfApiMessagesApi)

    // `CommandArguments` and `WrapperCommandSyntaxException`.
    compileOnlyApi(libs.commandapi.core)
    // `CommandSyntaxException` and `SimpleCommandExceptionType`, which
    // `WrapperCommandExceptionComponent` extends and wraps.
    compileOnlyApi(libs.brigadier)
    // The components the builder assembles and `SurfCommandUtil` takes.
    compileOnlyApi(libs.adventure.api)

    // `awaiting` and `awaitingOrNull` await a `Deferred`.
    api(libs.bundles.kotlin.coroutines)
}

description = "surf-api-command-api"
