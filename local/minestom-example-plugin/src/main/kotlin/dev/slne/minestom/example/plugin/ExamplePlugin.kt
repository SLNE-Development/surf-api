package dev.slne.minestom.example.plugin

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin

/**
 * A plugin that only uses the plugin api, `surf-api-minestom`.
 *
 * The class carries no logic. Its package is not below `dev.slne.surf.api`, which surf-api's own
 * root context scans: a plugin shaded into the server would otherwise have its beans registered in
 * the root context as well.
 *
 * Its logic lives in beans: [Greeting] and [Heartbeat] take part in the plugin's lifecycle,
 * [ExampleEvents] contributes an event node and [ExampleCommands] registers a command. Everything
 * is removed or cancelled when the plugin is disabled.
 */
class ExamplePlugin : SurfMinestomPlugin()
