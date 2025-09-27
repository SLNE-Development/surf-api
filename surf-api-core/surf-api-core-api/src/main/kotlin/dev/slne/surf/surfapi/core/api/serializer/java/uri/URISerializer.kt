@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.uri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.net.URI

typealias SerializableURI = @Serializable(with = URISerializer::class) URI

@Serializer(forClass = URI::class)
object URISerializer : KSerializer<URI>