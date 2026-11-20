package dev.slne.surf.api.spring.context.fixture

import dev.slne.surf.api.spring.PluginApi
import org.springframework.context.Lifecycle

// Each subpackage below is one test plugin; SurfContextManagerTest scans them one at a time.

/**
 * Records what fixtures did, in order, for the tests to assert on.
 */
object Journal {
    val entries = mutableListOf<String>()

    fun record(entry: String) {
        synchronized(entries) { entries += entry }
    }

    fun clear() = synchronized(entries) { entries.clear() }
}

@PluginApi
interface Greeting {
    fun greet(): String
}

@PluginApi
interface Repository<T : Any> {
    fun describe(): String
}

class Apple
class Pear
class Plum

@PluginApi
interface Clock : Lifecycle

@PluginApi
interface Optional {
    fun answer(): String
}

/**
 * Private to its plugin: implements no `@PluginApi` interface.
 */
interface Helper
