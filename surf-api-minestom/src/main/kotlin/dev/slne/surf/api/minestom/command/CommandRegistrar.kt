package dev.slne.surf.api.minestom.command

/**
 * Registers commands once the command API is ready to accept them.
 *
 * Implementations build their commands with `CommandAPICommand` or `CommandTree` and register
 * them from [register]. Registering outside of this callback is unsupported, because the backing
 * platform is only installed for the duration of the server's lifecycle.
 */
interface CommandRegistrar {

    fun register()
}
