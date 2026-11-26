package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.insertion.ItemInsertionPlan
import dev.slne.surf.api.paper.inventory.insertion.ItemInsertionPolicy
import dev.slne.surf.api.paper.inventory.insertion.planInsertion
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Plans an item insertion into the executing player's storage slots and optionally commits it.
 *
 * `/surfapitest insertion plan <item> <amount>` only reports the plan, and `plan-pair` reports the
 * plan for two items inserted together. `commit <item> <amount> <policy> <includeOffhand>` inserts
 * the items under the chosen insertion policy and reports the result.
 */
class ItemInsertionTest(name: String) : CommandAPICommand(name) {

    private val policyNames = ItemInsertionPolicy.entries
        .map { it.name }
        .toTypedArray()

    init {
        subcommand("plan") {
            itemStackArgument("item")
            integerArgument("amount", 1)

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args

                player.sendPlan(player.planInsertion(item, amount))
            }
        }

        subcommand("plan-pair") {
            itemStackArgument("item")
            integerArgument("amount", 1)
            itemStackArgument("otherItem")
            integerArgument("otherAmount", 1)

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val otherItem: ItemStack by args
                val otherAmount: Int by args

                val items = mapOf(item to amount, otherItem to otherAmount)

                player.sendPlan(player.planInsertion(items))
            }
        }

        subcommand("commit") {
            itemStackArgument("item")
            integerArgument("amount", 1)
            multiLiteralArgument("policy", *policyNames)
            booleanArgument("includeOffhand")

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val policy: String by args
                val includeOffhand: Boolean by args
                val result = player.planInsertion(item, amount, includeOffhand)
                    .commit(ItemInsertionPolicy.valueOf(policy))

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

    private fun Player.sendPlan(plan: ItemInsertionPlan) {
        sendText {
            appendInfoPrefix()
            info("Fits: ")
            variableValue(plan.fits)
            info(", leftover: ")
            variableValue(plan.leftover)

            for (entry in plan.entries) {
                appendNewline()
                keyValue(entry.item.type.key.asString(), entry.leftover)
            }
        }
    }
}
