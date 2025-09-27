@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.url

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.net.URL

typealias SerializableURL = @Serializable(with = URLSerializer::class) URL

@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL>