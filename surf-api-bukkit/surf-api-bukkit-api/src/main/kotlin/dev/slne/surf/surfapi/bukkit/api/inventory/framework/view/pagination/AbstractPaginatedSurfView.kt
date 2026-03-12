package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.modifyConfig
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.AbstractSurfView
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.addChild
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.blockCell
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.removeChildrenOfType
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.surfapi.core.api.util.prepend
import kotlinx.coroutines.delay
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*
import me.devnatan.inventoryframework.state.State
import me.devnatan.inventoryframework.state.StateValue
import me.devnatan.inventoryframework.state.StateValueHost
import me.devnatan.inventoryframework.state.StateWatcher
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UnstableApiUsage")
abstract class AbstractPaginatedSurfView(header: String) : AbstractSurfView(header) {
    protected abstract val layoutTarget: Char
    override val settings: PaginatedViewSettings = PaginatedViewSettings()

    private val paginationState: State<Pagination> by lazy {
        createPagination()
            .layoutTarget(layoutTarget)
            .apply {
                pageSwitchHandler.prepend { context, _ ->
                    updatePaginationGlyph(context)
                }
            }.build()
    }

    private val paginationRow: String by lazy { " " + layoutTarget.toString().repeat(7) + " " }

    protected abstract fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>

    protected open fun onPaginatedInit(config: ViewConfigBuilder) = Unit
    protected open fun onPaginatedOpen(open: OpenContext) = Unit
    protected open fun onPaginatedRender(render: RenderContext) = Unit
    protected open fun onPaginatedClick(click: SlotClickContext) = Unit
    protected open fun onPaginatedClose(close: CloseContext) = Unit

    private fun updatePaginationGlyph(context: Context) {
        val pagination = paginationState.get(context) ?: return
        val buttonGlyph = PaginationButtonGlyphComponent.getByPaginationState(
            row = settings.paginationButtonRow,
            pagination = pagination
        )

        modifyContainer(context) {
            removeChildrenOfType<PaginationButtonGlyphComponent>()
            addChild(buttonGlyph)
        }
    }

    context(_: ViewContainerModificationContext)
    final override fun containerDefaults() {
        val paginationContentRows = settings.paginationViewRows.paginationContentRows

        for (y in 0 until settings.rows.rows) {
            for (x in 0 until 9) {
                if (y in paginationContentRows && x in 1..7) continue
                blockCell(x, y)
            }
        }

        applyContainerDefaults()
    }

    context(_: ViewContainerModificationContext)
    protected open fun applyContainerDefaults() {
    }

    override fun onViewInit(config: ViewConfigBuilder) {
        paginationState // initialize pagination state
        onPaginatedInit(config)
    }

    override fun onViewOpen(open: OpenContext) {
        val layout = arrayOfNulls<String>(settings.rows.rows)
        layout[0] = EMPTY_ROW
        repeat(settings.rows.rows - 2) { i ->
            layout[i + 1] = paginationRow
        }
        layout[layout.lastIndex] = EMPTY_ROW

        open.modifyConfig {
            layout(*layout)
        }

        onPaginatedOpen(open)
    }

    final override fun onViewRender(render: RenderContext) {
        val pagination = paginationState.get(render) ?: return
        val paginationButtonRow = settings.paginationButtonRow

        render.watchState(pagination.id, InitialPaginationStateWatcher())

        render.slot(PaginationButton.LEFT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.empty())
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canBack)
            .onClick(pagination::back)

        render.slot(PaginationButton.RIGHT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.empty())
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canAdvance)
            .onClick(pagination::advance)

        onPaginatedRender(render)
    }

    final override fun onViewClick(click: SlotClickContext) {
        onPaginatedClick(click)
    }

    final override fun onViewClose(close: CloseContext) {
        onPaginatedClose(close)
    }

    private inner class InitialPaginationStateWatcher : StateWatcher {
        private val initialHandled = AtomicBoolean(false)

        override fun stateRegistered(state: State<*>, caller: Any?) {}
        override fun stateUnregistered(state: State<*>, caller: Any?) {}
        override fun stateValueGet(state: State<*>, host: StateValueHost, internalValue: StateValue, rawValue: Any?) {}

        override fun stateValueSet(
            host: StateValueHost,
            value: StateValue,
            rawOldValue: Any?,
            rawNewValue: Any?
        ) {
            val pagination = value as? Pagination ?: return
            if (pagination.isLoading) return
            if (!initialHandled.compareAndSet(false, true)) return
            if (host !is Context) return

            val plugin = JavaPlugin.getProvidingPlugin(javaClass)
            plugin.launch(plugin.entityDispatcher(host.player)) {
                delay(1.ticks)
                updatePaginationGlyph(host)
            }
        }
    }

    companion object {
        private val EMPTY_ROW = " ".repeat(9)
    }
}