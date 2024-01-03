package dev.slne.surf.surfapi.bukkit.server.reflection;

import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

import java.util.concurrent.atomic.AtomicInteger;

@Proxies(className = "net.minecraft.world.entity.Entity")
public interface EntityProxy {

    @Static
    @FieldGetter("ENTITY_COUNTER")
    AtomicInteger entityCounter();
}
