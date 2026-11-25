package dev.slne.surf.api.paper.serializer.bukkit.location

import dev.slne.surf.api.serialization.java.uuid.JavaUUIDStringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bukkit.Location
import org.bukkit.World
import java.lang.reflect.Proxy
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationSerializerJsonTest {

    private val worldUid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
    private val location = Location(world(worldUid), 1.0, 2.0, 3.0, 4f, 5f)

    @Test
    fun `world uuid falls back to bytes without a contextual serializer`() {
        val json = Json.encodeToString(LocationSerializer, location)

        assertEquals(
            """{"worldUuid":[0,17,34,51,68,85,102,119,-120,-103,-86,-69,-52,-35,-18,-1],""" +
                """"x":1.0,"y":2.0,"z":3.0,"yaw":4.0,"pitch":5.0}""",
            json,
        )
    }

    @Test
    fun `world uuid follows a contextual uuid serializer`() {
        val format = Json {
            serializersModule = SerializersModule { contextual(JavaUUIDStringSerializer) }
        }

        assertEquals(
            """{"worldUuid":"00112233-4455-6677-8899-aabbccddeeff",""" +
                """"x":1.0,"y":2.0,"z":3.0,"yaw":4.0,"pitch":5.0}""",
            format.encodeToString(LocationSerializer, location),
        )
    }

    private fun world(uid: UUID): World {
        return Proxy.newProxyInstance(
            World::class.java.classLoader,
            arrayOf(World::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getUID" -> uid
                else -> throw UnsupportedOperationException(method.name)
            }
        } as World
    }
}
