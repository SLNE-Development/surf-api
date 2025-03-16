package dev.slne.surf.surfapi.velocity.api.command.args

import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.velocitypowered.api.command.VelocityBrigadierMessage
import dev.slne.surf.surfapi.core.api.messages.adventure.text

fun SimpleCommandExceptionType(message: String) =
    SimpleCommandExceptionType(VelocityBrigadierMessage.tooltip(text(message)))