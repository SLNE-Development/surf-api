package dev.slne.surf.surfapi.bukkit.test.gui

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.props
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.slot
import dev.slne.surf.surfapi.bukkit.api.gui.props.MutableProp
import dev.slne.surf.surfapi.bukkit.api.gui.props.PropScope
import dev.slne.surf.surfapi.bukkit.api.gui.ref.createRef
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.bukkit.server.gui.view.BukkitGuiView
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemType

/**
 * Test view demonstrating the new GUI framework.
 */
object TestGuiView : BukkitGuiView() {
    
    // Define props using the props DSL
    private val propsMap = props {
        mutable("clickCount", 0, PropScope.VIEWER)
    }
    
    private val clickCountProp = propsMap["clickCount"] as MutableProp<Int>
    
    // Create a ref to the counter component for updates
    private val counterRef = createRef<Component>()
    
    override fun onInit(config: ViewConfig) {
        config.title = text("New GUI Framework Test", NamedTextColor.GOLD)
        config.size = 54 // 6 rows
        config.cancelOnClick = true
    }
    
    override fun onFirstRender(context: dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext) {
        // Counter display component (updates when clicked)
        val counterComponent = dynamicComponent(
            renderer = { ctx ->
                val count = clickCountProp.get(ctx.propContext)
                buildItem(ItemType.GOLD_BLOCK) {
                    displayName {
                        text("Click Counter: $count", NamedTextColor.YELLOW)
                    }
                }
            }
        ) {
            ref = counterRef
        }
        
        context.slot(4, counterComponent)
        
        // Increment button
        val incrementButton = component(
            item = buildItem(ItemType.EMERALD) {
                displayName {
                    text("Click to Increment!", NamedTextColor.GREEN)
                }
            }
        ) {
            onClick = {
                val currentCount = clickCountProp.get(propContext)
                clickCountProp.set(propContext, currentCount + 1)
                
                // Update the counter display using ref
                counterRef.update()
            }
        }
        
        context.slot(13, incrementButton)
        
        // Close button
        val closeButton = component(
            item = buildItem(ItemType.BARRIER) {
                displayName {
                    text("Close", NamedTextColor.RED)
                }
            }
        ) {
            onClick = {
                player.sendMessage(text("Thanks for testing the new GUI framework!", NamedTextColor.AQUA))
                close()
            }
        }
        
        context.slot(49, closeButton)
        
        // Back button (for navigation demo)
        val backButton = component(
            item = buildItem(ItemType.ARROW) {
                displayName {
                    text("Back to Parent", NamedTextColor.GRAY)
                }
            }
        ) {
            onClick = {
                navigateBack()
            }
        }
        
        context.slot(45, backButton)
    }
    
    override fun onOpen(context: dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext) {
        context.player.sendMessage(text("Opening new GUI framework test!", NamedTextColor.GREEN))
    }
    
    override fun onClose(context: dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext) {
        context.player.sendMessage(text("Closing GUI!", NamedTextColor.YELLOW))
    }
    
    override fun onUpdate(context: dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext) {
        // Called when view is updated
    }
}
