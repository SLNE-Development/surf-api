@file:OptIn(NmsUseWithCaution::class)
@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.toast.toast
import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.`object`.ObjectContents
import org.bukkit.inventory.ItemType

class ToastTest(name: String) : CommandAPICommand(name) {
    val toast = toast {
        icon(ItemType.DIAMOND)

        title {
            primary("Dsl-Extension Test")
            appendNewline()
            info("With multiple lines!")
        }

        frame(AdvancementDisplay.Frame.TASK)
    }

    init {
        playerExecutor { player, _ ->
            val playerHeadToast = toast {
                icon(ItemType.PLAYER_HEAD) {
                    setData(
                        DataComponentTypes.PROFILE,
                        ResolvableProfile.resolvableProfile(player.playerProfile)
                    )
                }

                title {
                    primary("Player Head Test")
                }
            }

            val playerObjectToast = toast {
                icon(ItemType.BARRIER)

                title {
                    append(Component.`object`(ObjectContents.playerHead(player.uniqueId)))
                    appendSpace()
                    append(Component.text("Player Head Test"))
                }
            }

            toast.createOperation()
                .add(playerHeadToast.createOperation())
                .add(playerObjectToast.createOperation())
                .execute(player)
        }
    }
}