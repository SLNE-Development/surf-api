package dev.slne.surf.surfapi.bukkit.test.command.subcommands.display

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.paper.display.color
import dev.slne.surf.api.paper.display.cursor.CursorStyle
import dev.slne.surf.api.paper.display.document.document
import dev.slne.surf.api.paper.display.shape.Shape
import dev.slne.surf.api.paper.display.style.*
import dev.slne.surf.api.paper.server.display.Display
import dev.slne.surf.api.paper.server.display.DisplayManager
import dev.slne.surf.api.paper.server.display.modal.confirmDialog

/**
 * Test command for the Display/UI API framework.
 *
 * Usage:
 * - `/surfapitest display open` — Opens a demo display with styled UI elements
 * - `/surfapitest display close` — Closes the active display
 */
class DisplayTest(name: String) : CommandAPICommand(name) {

    // Catppuccin Mocha palette
    companion object {
        private val BG = color(0x1E1E2E)
        private val SURFACE = color(0x313244)
        private val OVERLAY = color(0x45475A)
        private val TEXT = color(0xCDD6F4)
        private val SUBTEXT = color(0x9399B2)
        private val BLUE = color(0x89B4FA)
        private val GREEN = color(0xA6E3A1)
        private val RED = color(0xF38BA8)
        private val YELLOW = color(0xF9E2AF)
        private val MAUVE = color(0xCBA6F7)
        private val BORDER = color(0x585B70)
    }

    init {
        subcommand("open") {
            playerExecutor { player, _ ->
                // Create a 384x256 pixel document (3x2 map tiles)
                val doc = document(384, 256) {
                    style {
                        backgroundColor = BG
                        padding = Insets.all(8)
                        gap = 6
                    }

                    // --- Title Bar ---
                    div {
                        style {
                            backgroundColor = SURFACE
                            padding = Insets(6, 10, 6, 10)
                            flexDirection = FlexDirection.ROW
                            alignItems = AlignItems.CENTER
                            gap = 8
                        }
                        shape(Shape.circle(4, filled = true)) {
                            style { color = BLUE }
                        }
                        label("Surf Display API Demo") {
                            style { color = TEXT; fontSize = 16 }
                        }
                    }

                    // --- Content Area ---
                    div {
                        style {
                            flexDirection = FlexDirection.ROW
                            gap = 8
                        }

                        // Left column - Shape showcase
                        div {
                            style {
                                width = 120
                                backgroundColor = SURFACE
                                padding = Insets.all(6)
                                gap = 4
                                border = Border(1, BORDER)
                            }
                            label("Shapes") {
                                style { color = BLUE; fontSize = 12 }
                            }
                            div {
                                style {
                                    flexDirection = FlexDirection.ROW
                                    gap = 6
                                    alignItems = AlignItems.CENTER
                                }
                                shape(Shape.circle(8, filled = true)) {
                                    style { color = GREEN }
                                }
                                shape(Shape.rectangle(16, 16, filled = true)) {
                                    style { color = RED }
                                }
                                shape(Shape.triangle(16, 14, filled = true)) {
                                    style { color = YELLOW }
                                }
                            }
                            div {
                                style {
                                    flexDirection = FlexDirection.ROW
                                    gap = 6
                                    alignItems = AlignItems.CENTER
                                }
                                shape(Shape.ellipse(10, 6, filled = true)) {
                                    style { color = MAUVE }
                                }
                                shape(Shape.roundedRectangle(20, 14, 4, filled = true)) {
                                    style { color = BLUE }
                                }
                            }
                        }

                        // Right column - Interactive elements
                        div {
                            style {
                                backgroundColor = SURFACE
                                padding = Insets.all(6)
                                gap = 4
                                border = Border(1, BORDER)
                            }
                            label("Interactive Elements") {
                                style { color = BLUE; fontSize = 12 }
                            }

                            // Clickable button
                            div {
                                style {
                                    backgroundColor = OVERLAY
                                    padding = Insets(3, 8, 3, 8)
                                    border = Border(1, GREEN)
                                    cursor = CursorStyle.POINTER
                                }
                                label("Click Me!") {
                                    style { color = GREEN; fontSize = 11 }
                                }
                                hoverable(
                                    onEnter = { _ ->
                                        style.backgroundColor = color(0x585B70)
                                    },
                                    onExit = { _ ->
                                        style.backgroundColor = OVERLAY
                                    }
                                )
                                onClick { ctx ->
                                    player.sendMessage("Display clicked at (${ctx.pixelX}, ${ctx.pixelY})!")
                                }
                            }

                            // Another button that opens a modal
                            div {
                                style {
                                    backgroundColor = OVERLAY
                                    padding = Insets(3, 8, 3, 8)
                                    border = Border(1, MAUVE)
                                    cursor = CursorStyle.POINTER
                                }
                                label("Show Modal") {
                                    style { color = MAUVE; fontSize = 11 }
                                }
                                hoverable(
                                    onEnter = { _ ->
                                        style.backgroundColor = color(0x585B70)
                                    },
                                    onExit = { _ ->
                                        style.backgroundColor = OVERLAY
                                    }
                                )
                            }

                            label("Text alignment:") {
                                style { color = SUBTEXT; fontSize = 10 }
                            }

                            // Text alignment demo
                            div {
                                style {
                                    backgroundColor = color(0x11111B)
                                    padding = Insets.all(3)
                                    gap = 1
                                }
                                label("Left aligned") {
                                    style { color = TEXT; fontSize = 10; textAlign = TextAlign.LEFT }
                                }
                                label("Center aligned") {
                                    style { color = TEXT; fontSize = 10; textAlign = TextAlign.CENTER }
                                }
                                label("Right aligned") {
                                    style { color = TEXT; fontSize = 10; textAlign = TextAlign.RIGHT }
                                }
                            }
                        }
                    }

                    // --- Footer ---
                    div {
                        style {
                            backgroundColor = SURFACE
                            padding = Insets(4, 10, 4, 10)
                            flexDirection = FlexDirection.ROW
                            justifyContent = JustifyContent.SPACE_BETWEEN
                        }
                        label("Sneak to close") {
                            style { color = SUBTEXT; fontSize = 10 }
                        }
                        label("Surf API v1.0") {
                            style { color = SUBTEXT; fontSize = 10 }
                        }
                    }
                }

                val display = Display(doc)

                // Wire up the "Show Modal" button to actually show a modal
                // The second child of the content area's right column (index 1) is the modal button
                val contentArea = doc.root.children[1] // content row
                val rightColumn = contentArea.children[1] // right column div
                val modalButton = rightColumn.children[1] // "Show Modal" button
                modalButton.onClick { _ ->
                    val modal = display.confirmDialog(
                        title = "Confirm Action",
                        message = "This is a modal dialog demo.\nDo you want to proceed?",
                        onConfirm = {
                            player.sendMessage("Confirmed!")
                            display.dismissModal()
                        },
                        onCancel = {
                            player.sendMessage("Cancelled!")
                            display.dismissModal()
                        }
                    )
                    display.showModal(modal)
                }

                DisplayManager.open(player, display)
                player.sendMessage("Display opened! Look around to move cursor. Sneak to close.")
            }
        }

        subcommand("close") {
            playerExecutor { player, _ ->
                if (DisplayManager.hasDisplay(player.uniqueId)) {
                    DisplayManager.close(player)
                    player.sendMessage("Display closed.")
                } else {
                    player.sendMessage("No active display.")
                }
            }
        }
    }
}
