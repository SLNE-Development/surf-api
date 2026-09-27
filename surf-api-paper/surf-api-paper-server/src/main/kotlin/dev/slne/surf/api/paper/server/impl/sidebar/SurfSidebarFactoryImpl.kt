package dev.slne.surf.api.paper.server.impl.sidebar

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.sidebar.*
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.plugin.Plugin

@OptIn(InternalSurfApi::class)
@AutoService(SurfSidebarFactory::class)
class SurfSidebarFactoryImpl : SurfSidebarFactory {

    override fun createSidebar(plugin: Plugin, block: SidebarBuilder.() -> Unit): SurfViewerSidebar {
        val builder = SidebarBuilderImpl().apply(block)
        return SurfViewerSidebarImpl(plugin, builder.definition())
    }

    override fun createAutoUpdatingSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit
    ): SurfAutoUpdatingSidebar {
        val builder = AutoUpdatingSidebarBuilderImpl().apply(block)
        return SurfAutoUpdatingSidebarImpl(plugin, builder.definition(), builder.updateInterval)
    }

    override fun createGlobalSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit
    ): SurfGlobalSidebar {
        val builder = AutoUpdatingSidebarBuilderImpl().apply(block)
        return SurfGlobalSidebarImpl(plugin, builder.definition(), builder.updateInterval)
    }
}
