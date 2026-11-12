package dev.slne.surf.database.columns

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

fun Table.component(name: String): Column<Component> = json(
    name = name,
    serialize = { component ->
        val compact = component.compact()
        GsonComponentSerializer.gson().serialize(compact)
    },
    deserialize = { json ->
        GsonComponentSerializer.gson().deserialize(json)
    }
)