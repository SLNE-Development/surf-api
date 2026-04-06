@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.java.datetime.datetime.offset

import dev.slne.surf.api.core.serializer.java.datetime.datetime.UtcInstantDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

typealias SerializableOffsetDateTime = @Serializable(with = OffsetDateTimeSerializer::class) OffsetDateTime

object OffsetDateTimeSerializer : UtcInstantDateTimeSerializer<OffsetDateTime>(
    "surf.api.java.datetime.datetime.OffsetDateTime"
) {
    override fun toInstant(value: OffsetDateTime): Instant = value.toInstant()
    override fun fromInstant(instant: Instant): OffsetDateTime =
        instant.atZone(ZoneId.systemDefault()).toOffsetDateTime()
}