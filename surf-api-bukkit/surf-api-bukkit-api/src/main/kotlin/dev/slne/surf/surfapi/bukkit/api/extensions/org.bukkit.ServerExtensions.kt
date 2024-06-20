package dev.slne.surf.surfapi.bukkit.api.extensions

import org.bukkit.Bukkit
import org.bukkit.Server

val server: Server
    get() = Bukkit.getServer()