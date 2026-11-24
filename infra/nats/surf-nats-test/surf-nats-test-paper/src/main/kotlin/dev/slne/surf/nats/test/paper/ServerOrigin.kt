package dev.slne.surf.nats.test.paper

import org.bukkit.Bukkit

/**
 * The name this server carries in the events it publishes and the answers it gives, such as
 * `paper:25565`.
 */
internal val serverOrigin: String
    get() = "paper:${Bukkit.getPort()}"
