package dev.slne.surf.api.paper.sidebar

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.plugin.Plugin

/**
 * Creates a sidebar with manually managed viewers and updates.
 *
 * Use [SurfViewerSidebar] to add or remove viewers and trigger updates when
 * the displayed content should be refreshed.
 *
 * @param plugin the plugin that owns the sidebar
 * @param block configures the sidebar
 * @return the configured sidebar
 */
@OptIn(InternalSurfApi::class)
fun sidebar(plugin: Plugin, block: SidebarBuilder.() -> Unit): SurfViewerSidebar {
    return SurfSidebarFactory.INSTANCE.createSidebar(plugin, block)
}

/**
 * Creates a sidebar that updates automatically while its viewers are managed manually.
 *
 * @param plugin the plugin that owns the sidebar
 * @param block configures the sidebar and its update behavior
 * @return the configured auto-updating sidebar
 */
@OptIn(InternalSurfApi::class)
fun autoUpdatingSidebar(
    plugin: Plugin,
    block: AutoUpdatingSidebarBuilder.() -> Unit
): SurfAutoUpdatingSidebar = SurfSidebarFactory.INSTANCE.createAutoUpdatingSidebar(plugin, block)

/**
 * Creates an automatically updating sidebar shown to all online players.
 *
 * @param plugin the plugin that owns the sidebar
 * @param block configures the sidebar and its update behavior
 * @return the configured global sidebar
 */
@OptIn(InternalSurfApi::class)
fun globalSidebar(
    plugin: Plugin,
    block: AutoUpdatingSidebarBuilder.() -> Unit
): SurfGlobalSidebar = SurfSidebarFactory.INSTANCE.createGlobalSidebar(plugin, block)

/**
 * Internal factory for creating sidebar implementations.
 */
@InternalSurfApi
interface SurfSidebarFactory {
    fun createSidebar(plugin: Plugin, block: SidebarBuilder.() -> Unit): SurfViewerSidebar

    fun createAutoUpdatingSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit
    ): SurfAutoUpdatingSidebar

    fun createGlobalSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit
    ): SurfGlobalSidebar

    companion object {
        val INSTANCE = requiredService<SurfSidebarFactory>()
    }
}
