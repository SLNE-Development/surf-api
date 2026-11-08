@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.java.datetime.datetime.zdt

import dev.slne.surf.api.core.serializer.java.datetime.datetime.UtcInstantDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

typealias SerializableZonedDateTime = @Serializable(with = ZonedDateTimeSerializer::class) ZonedDateTime

object ZonedDateTimeSerializer : UtcInstantDateTimeSerializer<ZonedDateTime>(
    "surf.api.java.datetime.datetime.ZonedDateTime"
) {
    override fun toInstant(value: ZonedDateTime): Instant = value.toInstant()
    override fun fromInstant(instant: Instant): ZonedDateTime =
        instant.atZone(ZoneId.systemDefault())
}