package dev.slne.surf.surfapi.bukkit.server.reflection

import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

interface HolderSetProxy {

    @Proxies(HolderSet.Named::class)
    interface NamedProxy {

        @MethodName("bind")
        fun <T> bind(instance: HolderSet.Named<T>, contents: List<Holder<T>>)
    }
}