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

/**
 * Abstract base class for all Surf inventory views built on top of the inventory framework.
 *
 * [AbstractSurfView] extends [View] and wires the inventory framework's lifecycle callbacks
 * to protected, open hook methods (`onViewInit`, `onViewOpen`, `onViewRender`, etc.) that
 * subclasses can override without having to call `super`. The `final` overrides of the
 * inventory framework's lifecycle methods ensure that the container (title composition)
 * and settings (cancel behaviours, row count, type) are always applied before delegating
 * to the subclass hooks.
 *
 * The view's title is composed from a [ViewContainer] that holds a glyph background component
 * and an aligned title text component. The container can be modified at any time via
 * [modifyContainer].
 *
 * Subclasses are typically not created directly — use [surfView] or [paginatedSurfView] instead.
 *
 * ```kotlin
 * val view = surfView("My View") {
 *     settings { rows(ViewRows.FOUR) }
 *     onFirstRender {
 *         slot(4, 1) { withItem(Material.DIAMOND) }
 *     }
 * }
 * view.register()
 * view.open(player)
 * ```
 *
 * @param header the plain-text title string rendered in the inventory's title bar
 * @see surfView
 * @see paginatedSurfView
 * @see SurfViewSettings
 */
abstract class AbstractSurfView(
    private val header: String,
) : View() {
    /**
     * The [SurfViewSettings] controlling layout, cancel behaviours, font, and alignment.
     * Defaults to [SimpleViewSettings] with all defaults applied.
     */
    open val settings: SurfViewSettings = SimpleViewSettings()
    private val container = ViewContainer()

    /**
     * Called during [onInit] after the container defaults are applied.
     * Override to perform additional configuration on the [ViewConfigBuilder].
     *
     * @param config the [ViewConfigBuilder] passed by the inventory framework
     */
    protected open fun onViewInit(config: ViewConfigBuilder) = Unit

    /**
     * Called during [onOpen].
     * Override to react to the view being opened for a player.
     *
     * @param open the [OpenContext] provided by the inventory framework
     */
    protected open fun onViewOpen(open: OpenContext) = Unit

    /**
     * Called during [onFirstRender].
     * Override to populate the view's slots for the first time.
     *
     * @param render the [RenderContext] provided by the inventory framework
     */
    protected open fun onViewRender(render: RenderContext) = Unit

    /**
     * Called during [onClick] when the click is not an outside-click or
     * when [SurfViewSettings.navigateBackOnOutsideClick] is `false`.
     *
     * @param click the [SlotClickContext] provided by the inventory framework
     */
    protected open fun onViewClick(click: SlotClickContext) = Unit

    /**
     * Called during [onClose].
     * Override to react to the view being closed.
     *
     * @param close the [CloseContext] provided by the inventory framework
     */
    protected open fun onViewClose(close: CloseContext) = Unit

    /**
     * Called during [onUpdate].
     * Override to update the view's contents on state changes.
     *
     * @param update the [Context] provided by the inventory framework
     */
    protected open fun onViewUpdate(update: Context) = Unit

    /**
     * Applies modifications to the [ViewContainer] and optionally updates the inventory title.
     *
     * The [block] is executed within a [ViewContainerModificationContext] that provides
     * component management functions. If [updateContext] is provided:
     * - For an [OpenContext], the title is set via `modifyConfig`.
     * - For any other context, `updateTitleForEveryone` is called to update all viewers.
     *
     * @param updateContext optional context used to propagate the title change; `null` skips
     *   the title update (useful during initial setup)
     * @param block modifications to apply to the [ViewContainer]
     */
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

    /**
     * Hook called within [applyContainerDefaults] to allow subclasses to add their own
     * container components after the standard glyph and title components have been added.
     *
     * This function runs in the scope of a [ViewContainerModificationContext].
     */
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