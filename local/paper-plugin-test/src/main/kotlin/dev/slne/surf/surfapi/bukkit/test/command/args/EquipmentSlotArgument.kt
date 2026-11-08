package dev.slne.surf.surfapi.bukkit.test.command.args

import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import org.bukkit.inventory.EquipmentSlot

class EquipmentSlotArgument(name: String) :
    CustomArgument<EquipmentSlot, String>(StringArgument(name), { info ->
        try {
            EquipmentSlot.valueOf(info.input)
        } catch (e: IllegalArgumentException) {
            throw CustomArgumentException.fromMessageBuilder(
                MessageBuilder()
                    .append("Invalid equipment slot: ")
                    .appendArgInput()
                    .append(" (valid: ")
                    .append(EquipmentSlot.entries.joinToString(", ") { it.name })
                    .append(")")
            )
        }
    }) {
    init {
        replaceSuggestions(ArgumentSuggestions.strings(EquipmentSlot.entries.map { it.name }))
    }
}