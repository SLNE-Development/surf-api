package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.messages.adventure.sendText
import dev.slne.surf.api.paper.inventory.removal.ItemRemovalPlan
import dev.slne.surf.api.paper.inventory.removal.ItemRemovalPolicy
import dev.slne.surf.api.paper.inventory.removal.planRemoval
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Plans an item removal from the executing player's storage slots and optionally commits it.
 *
 * `/surfapitest removal plan <item> <amount> <includeOffhand> <includeArmor>` only reports the
 * plan, and `plan-pair` reports the plan for two items removed together. `commit <item> <amount>
 * <policy> <includeOffhand> <includeArmor>` removes the items under the chosen removal policy and
 * reports the result.
 */
class ItemRemovalTest(name: String) : CommandAPICommand(name) {

    private val policyNames = ItemRemovalPolicy.entries
        .map { it.name }
        .toTypedArray()

    init {
        subcommand("plan") {
            itemStackArgument("item")
            integerArgument("amount", 1)
            booleanArgument("includeOffhand")
            booleanArgument("includeArmor")

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val includeOffhand: Boolean by args
                val includeArmor: Boolean by args

                val plan = player.planRemoval(
                    item,
                    amount,
                    includeOffhand = includeOffhand,
                    includeArmor = includeArmor,
                )

                player.sendPlan(plan)
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

                player.sendPlan(player.planRemoval(items))
            }
        }

        subcommand("commit") {
            itemStackArgument("item")
            integerArgument("amount", 1)
            multiLiteralArgument("policy", *policyNames)
            booleanArgument("includeOffhand")
            booleanArgument("includeArmor")

            playerExecutor { player, args ->
                val item: ItemStack by args
                val amount: Int by args
                val policy: String by args
                val includeOffhand: Boolean by args
                val includeArmor: Boolean by args
                val plan = player.planRemoval(
                    item,
                    amount,
                    includeOffhand = includeOffhand,
                    includeArmor = includeArmor,
                )
                val result = plan.commit(ItemRemovalPolicy.valueOf(policy))

                player.sendText {
                    appendInfoPrefix()
                    info("Removed: ")
                    variableValue(result.removed)
                    info(", missing: ")
                    variableValue(result.missing)
                }
            }
        }
    }

    private fun Player.sendPlan(plan: ItemRemovalPlan) {
        sendText {
            appendInfoPrefix()
            info("Available: ")
            variableValue(plan.isAvailable)
            info(", missing: ")
            variableValue(plan.missing)

            for (entry in plan.entries) {
                appendNewline()
                keyValue(entry.item.type.key.asString(), entry.missing)
            }
        }
    }
}
