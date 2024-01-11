package dev.slne.surf.surfapi.bukkit.server.reflection;

import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static;

@Proxies(className = "net.minecraft.world.entity.Entity")
public interface EntityProxy {

    @Static
    @MethodName("nextEntityId")
    int generateEntityId();
}
