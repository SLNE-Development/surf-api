package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.insertion.planInsertion
import org.bukkit.inventory.ItemStack

/**
 * Plans an item insertion into the executing player's storage slots and optionally commits it.
 *
 * `/surfapitest insertion plan <item> <amount>` only reports the plan; `commit` inserts the items
 * under the default insertion policy and reports the result.
 */
class ItemInsertionTest(name: String) : CommandAPICommand(name) {
    init {
        subcommand("plan") {
            itemStackArgument("item")
            integerArgument("amount", 1)

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val plan = player.planInsertion(item, amount)

                player.sendText {
                    appendInfoPrefix()
                    info("Fits: ")
                    variableValue(plan.fits)
                    info(", leftover: ")
                    variableValue(plan.leftover)
                }
            }
        }

        subcommand("commit") {
            itemStackArgument("item")
            integerArgument("amount", 1)

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val result = player.planInsertion(item, amount).commit()

                player.sendText {
                    appendInfoPrefix()
                    info("Inserted: ")
                    variableValue(result.inserted)
                    info(", leftover: ")
                    variableValue(result.leftover)
                }
            }
        }
    }
}
