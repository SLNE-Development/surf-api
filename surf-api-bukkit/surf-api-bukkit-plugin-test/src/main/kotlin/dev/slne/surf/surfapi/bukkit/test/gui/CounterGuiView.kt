package dev.slne.surf.surfapi.bukkit.test.gui

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.bukkit.test.plugin
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import kotlinx.coroutines.runBlocking
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

/**
 * Example GUI demonstrating MutableProp usage with a counter.
 * The counter is shared across all viewers (global state).
 */
object CounterGuiView : AbstractGuiView() {
    private val counterProp = Prop.Mutable("counter", 0)
    private val counterDisplayRef = Ref<Component>()

    override fun onInit(config: ViewConfig) {
        config.type = InventoryType.CHEST
        config.rows = 3
        config.title = text("Counter Gui")
    }

    override fun onFirstRender(context: RenderContext) {
        // Counter display at top center
        context.renderComponent(
            Slot.at(4, 0), dynamicComponent(
                renderer = { ctx ->
                    val count = runBlocking { counterProp.get() } ?: 0

                    GuiItem.of(ItemStack(Material.PAPER) {
                        displayName {
                            info("$count GUI view")
                        }
                    })
                }
            ) {
                ref = counterDisplayRef
            })

        // Increment +1 button
        context.renderComponent(
            Slot.at(2, 1), component(
                GuiItem.of(ItemStack(Material.LIME_CONCRETE) {
                    displayName {
                        success("+1")
                    }

                    buildLore {
                        line {
                            gray("Click to increment by 1")
                        }
                    }
                })
            ) {
                onClick = {
                    plugin.launch {
                        val current = counterProp.get() ?: 0

                        counterProp.set(current + 1)
                        counterDisplayRef.update()
                    }
                }
            })

        // Increment +10 button
        context.renderComponent(
            Slot.at(3, 1), component(
                GuiItem.of(ItemStack(Material.GREEN_CONCRETE) {
                    displayName {
                        success("+10")
                    }

                    buildLore {
                        line {
                            gray("Click to increment by 10")
                        }
                    }
                })
            ) {
                onClick = {
                    plugin.launch {
                        val current = counterProp.get() ?: 0

                        counterProp.set(current + 10)
                        counterDisplayRef.update()
                    }
                }
            })

        // Decrement -1 button
        context.renderComponent(
            Slot.at(5, 1), component(
                GuiItem.of(ItemStack(Material.PINK_CONCRETE) {
                    displayName {
                        error("-1")
                    }

                    buildLore {
                        line {
                            gray("Click to decrement by 1")
                        }
                    }
                })
            ) {
                onClick = {
                    plugin.launch {
                        val current = counterProp.get() ?: 0

                        counterProp.set(current - 1)
                        counterDisplayRef.update()
                    }
                }
            })

        // Decrement -10 button
        context.renderComponent(
            Slot.at(6, 1), component(
                GuiItem.of(ItemStack(Material.RED_CONCRETE) {
                    displayName {
                        error("-10")
                    }

                    buildLore {
                        line {
                            gray("Click to decrement by 10")
                        }
                    }
                })
            ) {
                onClick = {
                    plugin.launch {
                        val current = counterProp.get() ?: 0

                        counterProp.set(current - 10)
                        counterDisplayRef.update()
                    }
                }
            })

        // Reset button
        context.renderComponent(
            Slot.at(4, 2), component(
                GuiItem.of(ItemStack(Material.BARRIER) {
                    displayName {
                        error("Reset Counter")
                    }

                    buildLore {
                        line {
                            gray("Click to reset the counter to 0")
                        }
                    }
                })
            ) {
                onClick = {
                    counterProp.set(0)
                    counterDisplayRef.update()
                }
            })
    }
}
