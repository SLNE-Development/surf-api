package dev.slne.surf.database.health

import dev.slne.surf.database.DatabaseApi
import java.util.concurrent.ConcurrentHashMap

internal data class RegisteredDatabase(
    val name: String,
    val api: DatabaseApi
)

internal object DatabaseHealthRegistry {
    private val databases = ConcurrentHashMap<DatabaseApi, String>()

    fun register(databaseApi: DatabaseApi, name: String) {
        databases[databaseApi] = name
    }

    fun unregister(databaseApi: DatabaseApi) {
        databases.remove(databaseApi)
    }

    fun snapshot(): List<RegisteredDatabase> {
        return databases.entries
            .map { (api, name) ->
                RegisteredDatabase(
                    name = name,
                    api = api
                )
            }
            .sortedBy(RegisteredDatabase::name)
    }
}