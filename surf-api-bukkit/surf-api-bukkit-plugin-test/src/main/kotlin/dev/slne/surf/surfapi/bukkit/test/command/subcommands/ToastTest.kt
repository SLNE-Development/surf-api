package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.api.surfBukkitApi
import dev.slne.surf.surfapi.bukkit.api.toast.toast
import dev.slne.surf.surfapi.bukkit.api.util.send
import dev.slne.surf.surfapi.bukkit.api.util.sendToast
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.toast.ToastStyle
import org.bukkit.Material

class ToastTest(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { player, _ ->
            player.sendToast {
                icon(Material.DIAMOND)

                text {
                    info("Dsl-Extension Test")
                }

                style(ToastStyle.TASK)
            }

            player.sendToast(toast {
                icon(Material.GOLD_INGOT)

                text {
                    info("Toast-Builder-Dsl Test")
                }

                style(ToastStyle.CHALLENGE)
            })

            surfBukkitApi.createToast(Material.NETHERITE_AXE, buildText {
                info("Direct API Call Test")
            }, ToastStyle.GOAL).send(player)
        }
    }
}