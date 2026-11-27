package dev.slne.surf.api.paper.server.impl.sidebar

import dev.slne.surf.api.paper.sidebar.*
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Service

@Service
class SurfSidebarFactoryImpl(private val registry: SidebarRegistry) : SurfSidebarFactory {

    override fun createSidebar(
        plugin: Plugin,
        block: SidebarBuilder.() -> Unit,
    ): SurfViewerSidebar {
        val builder = SidebarBuilderImpl().apply(block)

        return SurfViewerSidebarImpl(plugin, builder.definition(), registry)
    }

    override fun createAutoUpdatingSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit,
    ): SurfAutoUpdatingSidebar {
        val builder = AutoUpdatingSidebarBuilderImpl().apply(block)

        return SurfAutoUpdatingSidebarImpl(
            plugin,
            builder.definition(),
            registry,
            builder.updateInterval,
        )
    }

    override fun createGlobalSidebar(
        plugin: Plugin,
        block: AutoUpdatingSidebarBuilder.() -> Unit,
    ): SurfGlobalSidebar {
        val builder = AutoUpdatingSidebarBuilderImpl().apply(block)

        return SurfGlobalSidebarImpl(
            plugin,
            builder.definition(),
            registry,
            builder.updateInterval,
        )
    }
}
