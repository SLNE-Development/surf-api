package dev.slne.surf.redis.util

import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf

abstract class LuaScriptRegistry(prefix: String) {
    private val prefix = prefix.removeSuffix(".").removeSuffix("/").replace('.', '/')
    private val scripts = mutableObject2ObjectMapOf<String, String>()

    protected fun load(name: String) {
        val path = "$prefix/$name.lua"
        val stream = this::class.java.getResourceAsStream(path)

        requireNotNull(stream) { "Lua script resource not found on classpath: '$path'. Ensure that '$name.lua' is packaged under '$prefix' and available at initialization." }

        val script = stream.use { it.bufferedReader().readText() }
        scripts[name] = script
    }

    fun get(name: String) = scripts[name] ?: error("Lua script not loaded: $name")
}