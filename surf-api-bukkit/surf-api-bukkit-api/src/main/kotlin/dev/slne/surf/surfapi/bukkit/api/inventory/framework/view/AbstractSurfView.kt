package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.modifyConfig
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.ViewContainer
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewContainerGlyphComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewContainerTitleComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.addChild
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.backHint
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.viewFrame
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.ViewType
import me.devnatan.inventoryframework.context.*
import org.bukkit.plugin.java.JavaPlugin

abstract class AbstractSurfView(
    private val header: String,
) : View() {
    open val settings: SurfViewSettings = SimpleViewSettings()
    private val container = ViewContainer()

    protected open fun onViewInit(config: ViewConfigBuilder) = Unit
    protected open fun onViewOpen(open: OpenContext) = Unit
    protected open fun onViewRender(render: RenderContext) = Unit
    protected open fun onViewClick(click: SlotClickContext) = Unit
    protected open fun onViewClose(close: CloseContext) = Unit
    protected open fun onViewUpdate(update: Context) = Unit

    protected fun modifyContainer(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) {
        context(ViewContainerModificationContext(container)) {
            block()
        }

        if (updateContext != null) {
            if (updateContext is OpenContext) {
                updateContext.modifyConfig {
                    title(container.render())
                }
            } else {
                updateContext.updateTitleForEveryone(container.render())
            }
        }
    }

    private fun applyContainerDefaults() {
        modifyContainer {
            addChild(ViewContainerGlyphComponent(settings.rows))
            addChild(
                ViewContainerTitleComponent(
                    title = header,
                    font = settings.font,
                    charSpacing = ViewContainerTitleComponent.CHAR_SPACING,
                    textAlignment = settings.headerTextAlignment
                )
            )

            if (settings.navigateBackOnOutsideClick) {
                backHint()
            }

            containerDefaults()
        }
    }

    context(_: ViewContainerModificationContext)
    protected open fun containerDefaults() {
    }

    final override fun onInit(config: ViewConfigBuilder) {
        applyContainerDefaults()

        with(settings) {
            if (cancelOnPickup) config.cancelOnPickup()
            if (cancelOnDrag) config.cancelOnDrag()
            if (cancelOnClick) config.cancelOnClick()
            if (cancelOnDrop) config.cancelOnDrop()
        }

        onViewInit(config)

        config.title(container.render())
        config.size(settings.rows.rows)
        config.type(ViewType.CHEST)
    }

    final override fun onOpen(open: OpenContext) {
        onViewOpen(open)
    }

    final override fun onFirstRender(render: RenderContext) {
        onViewRender(render)
    }

    final override fun onClick(click: SlotClickContext) {
        if (click.isOutsideClick && settings.navigateBackOnOutsideClick) {
            handleOutsideClick(click)
            return
        }

        onViewClick(click)
    }

    final override fun onClose(close: CloseContext) {
        onViewClose(close)
    }

    final override fun onUpdate(update: Context) {
        onViewUpdate(update)
    }

    private fun handleOutsideClick(click: SlotClickContext) {
        val viewer = click.viewer
        val previousContext = viewer.previousContext

        if (previousContext == null) {
            click.closeForPlayer()
        } else {
            val previousView = previousContext.root
            if (previousView !is View) {
                click.closeForPlayer()
                return
            }

            val player = click.player

            viewer.previousContext = null
            click.closeForPlayer()
            player.scheduler.run(JavaPlugin.getProvidingPlugin(javaClass), {
                viewFrame.open(previousView.javaClass, player)
            }, null)
        }
    }
}