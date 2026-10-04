package dev.slne.surf.api.minestom.inventory.framework.view

import dev.slne.surf.api.minestom.inventory.framework.modifyConfig
import dev.slne.surf.api.minestom.inventory.framework.view.container.ViewContainer
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.components.ViewContainerGlyphComponent
import dev.slne.surf.api.minestom.inventory.framework.view.container.component.components.ViewContainerTitleComponent
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.addChild
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.removeChildrenOfType
import dev.slne.surf.api.minestom.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.api.minestom.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.api.minestom.inventory.framework.view.settings.align.TextAlignment
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.ViewType
import me.devnatan.inventoryframework.context.*
import net.kyori.adventure.text.Component

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
 * The view's title is composed from a [ViewContainer] that holds the header components: the
 * aligned title text plus whatever the view adds on top of it. The container can be modified at any time via
 * [modifyContainer].
 *
 * A title set on the view's config instead - `onInit { title(...) }`, or `modifyConfig { title(...) }`
 * inside [onViewOpen] - is adopted as the container's header rather than replacing the rendered
 * container, so the header texture, the alignment and every other header element survive it. From
 * any other context use [updateHeader], which does the same.
 *
 * You can directly create a subclass or use the dsl functions ([surfView] or [paginatedSurfView]) instead.
 *
 * ```kotlin
 * val view = surfView("My View") {
 *     settings { rows(ViewRows.FOUR) }
 *     onFirstRender {
 *         slot(4, 1) { withItem(Material.DIAMOND) }
 *     }
 * }
 * view.register() // Called in JavaPlugin#onLoad
 * view.open(player)
 * ```
 *
 * @param defaultHeader the plain-text title string rendered in the inventory's title bar
 * @see surfView
 * @see paginatedSurfView
 * @see SurfViewSettings
 */
@Suppress("UnstableApiUsage")
abstract class AbstractSurfView(
    private val defaultHeader: String,
) : View() {
    /**
     * The [SurfViewSettings] controlling layout, cancel behaviours, font, and alignment.
     * Defaults to [SimpleViewSettings] with all defaults applied.
     */
    open val settings: SurfViewSettings = SimpleViewSettings()

    private val containerState = lazyState { _ -> ViewContainer() }

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
     * Applies modifications to the [ViewContainer] and updates the inventory title.
     *
     * The [block] is executed within a [ViewContainerModificationContext] that provides
     * component management functions. Title updates are propagated based on [context]:
     * - For an [OpenContext], the title is set via `modifyConfig`.
     * - For any other context, `updateTitleForEveryone` is called to update all viewers.
     *
     * @param context context used to propagate the title change to viewers
     * @param block modifications to apply to the [ViewContainer]
     */
    protected fun modifyContainer(
        context: Context,
        block: context(ViewContainerModificationContext) () -> Unit
    ) {
        val container = containerState.get(context)

        context(ViewContainerModificationContext(container)) {
            block()
        }

        if (context is OpenContext) {
            context.modifyConfig {
                title(container.render())
            }
        } else {
            context.updateTitleForEveryone(container.render())
        }
    }

    /**
     * Replaces the header text of the view, keeping every other container component - the
     * background glyph, row texts, the pagination overlay - in place.
     *
     * This is the API to change a view's title with: setting the title directly on the view's
     * config (`modifyConfig { title(...) }`) would replace the whole rendered container, dropping
     * the header texture along with the alignment shift, which leaves the text unaligned and every
     * other header element gone.
     *
     * ```kotlin
     * override fun onViewClick(click: SlotClickContext) {
     *     updateHeader(click, text("Seite 2", Colors.GOLD))
     * }
     * ```
     *
     * @param context the context used to propagate the title change to viewers
     * @param header the title component to render
     * @param alignment horizontal alignment of the title; defaults to the alignment the current
     *   title is rendered with, or to [SurfViewSettings.headerTextAlignment] if there is none
     */
    protected fun updateHeader(
        context: Context,
        header: Component,
        alignment: TextAlignment? = null,
    ) = modifyContainer(context) { applyHeader(header, alignment) }

    /**
     * Replaces the header text of the view with the plain, white string [header].
     *
     * @param context the context used to propagate the title change to viewers
     * @param header the plain-text title to render
     * @param alignment horizontal alignment of the title; defaults to the alignment the current
     *   title is rendered with, or to [SurfViewSettings.headerTextAlignment] if there is none
     * @see updateHeader
     */
    protected fun updateHeader(
        context: Context,
        header: String,
        alignment: TextAlignment? = null,
    ) = updateHeader(context, Component.text(header), alignment)

    /**
     * Swaps the container's [ViewContainerTitleComponent] for one rendering [header].
     *
     * Only the title component is replaced, so every other component the container holds survives
     * the title change. When [alignment] is `null` the alignment of the title currently rendered is
     * kept, falling back to [SurfViewSettings.headerTextAlignment] if the container has no title
     * yet.
     */
    context(ctx: ViewContainerModificationContext)
    internal fun applyHeader(header: Component, alignment: TextAlignment? = null) {
        val currentAlignment = ctx.container.children
            .filterIsInstance<ViewContainerTitleComponent>()
            .firstOrNull()
            ?.textAlignment

        removeChildrenOfType<ViewContainerTitleComponent>()

        addChild(
            ViewContainerTitleComponent(
                title = header,
                font = settings.font,
                textAlignment = alignment ?: currentAlignment ?: settings.headerTextAlignment,
                geometry = settings.headerGeometry,
                defaultColor = settings.headerTextColor,
                metrics = settings.headerFontMetrics
            )
        )
    }

    private fun applyContainerDefaults(context: OpenContext) {
        // A title set on the config before the container exists - via `onInit { title(...) }` or by
        // whoever opened the view - is the header the view was asked to show, so it wins over the
        // one the view was constructed with.
        val header = context.configuredTitle() ?: Component.text(defaultHeader)

        modifyContainer(context) {
            if (settings.backgroundGlyph) {
                addChild(ViewContainerGlyphComponent(settings.rows))
            }

            applyHeader(header)

            containerDefaults()
        }
    }

    /**
     * Adopts a title that was set on the config from outside the container - `modifyConfig
     * { title(...) }` inside [onViewOpen] being the usual case - as the container's header.
     *
     * Without this, that title would replace the rendered container as a whole: the header texture,
     * the alignment shift and every row text would be gone, and the raw text would sit wherever the
     * client happens to draw an inventory title.
     */
    private fun adoptExternalHeader(open: OpenContext) {
        val container = containerState.get(open)
        val header = open.configuredTitle() ?: return

        // The container itself put its own render on the config, that one is not external.
        if (header == container.lastRenderedTitle) return

        modifyContainer(open) { applyHeader(header) }
    }

    /**
     * Returns the title currently set on this context's config as a [Component], or `null` if no
     * title is set. Non-component titles (e.g. plain strings) are wrapped as-is.
     */
    private fun OpenContext.configuredTitle(): Component? = when (val title = config.title) {
        null -> null
        is Component -> title
        else -> Component.text(title.toString())
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
        with(settings) {
            if (cancelOnPickup) config.cancelOnPickup()
            if (cancelOnDrag) config.cancelOnDrag()
            if (cancelOnClick) config.cancelOnClick()
            if (cancelOnDrop) config.cancelOnDrop()
        }

        onViewInit(config)

        config.size(settings.rows.rows)
        config.type(ViewType.CHEST)
    }

    final override fun onOpen(open: OpenContext) {
        recordHistory(open)
        applyContainerDefaults(open)
        onViewOpen(open)
        adoptExternalHeader(open)
    }

    private fun recordHistory(open: OpenContext) {
        val player = open.player
        val entry = ViewNavigationHistory.NavEntry(javaClass, open.initialData)

        when {
            ViewNavigationHistory.consumeBackNavigation(player) -> Unit
            open.viewer!!.isSwitching -> ViewNavigationHistory.pushForward(player, entry)
            else -> ViewNavigationHistory.reset(player, entry)
        }
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
        val player = close.player
        if (!close.viewer.isSwitching && !ViewNavigationHistory.isPending(player.uuid)) {
            ViewNavigationHistory.clear(player)
        }
        onViewClose(close)
    }

    final override fun onUpdate(update: Context) {
        onViewUpdate(update)
    }

    private fun handleOutsideClick(click: SlotClickContext) {
        val player = click.player
        val target = ViewNavigationHistory.popToPrevious(player)

        if (target == null) {
            ViewNavigationHistory.clear(player)
            click.closeForPlayer()
            return
        }

        ViewNavigationHistory.markBackNavigation(player)

        // Minestom IF's `openForPlayer(Class, Any)` rejects a null `initialData` (Kotlin
        // non-null parameter), unlike the Bukkit platform's Java signature. Fall back to the
        // single-argument overload when the recorded entry carries no data.
        val data = target.data
        if (data == null) {
            click.openForPlayer(target.viewClass)
        } else {
            click.openForPlayer(target.viewClass, data)
        }
    }
}
