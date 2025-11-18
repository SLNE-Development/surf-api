@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.kyori.adventure.text.format.TextDecoration

typealias SerializableTextDecoration = @Serializable(with = AdventureTextDecorationSerializer::class) TextDecoration

object AdventureTextDecorationSerializer : KSerializer<TextDecoration> by serializer()