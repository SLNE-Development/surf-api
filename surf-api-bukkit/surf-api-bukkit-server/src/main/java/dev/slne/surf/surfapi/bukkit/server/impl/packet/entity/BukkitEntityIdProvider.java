package dev.slne.surf.surfapi.bukkit.server.impl.packet.entity;

import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import net.minecraft.world.entity.Entity;

public class BukkitEntityIdProvider implements EntityIdProvider {
    @Override
    public int nextEntityId() {
//        return SpigotReflectionUtil.generateEntityId();
//        return Reflection.ENTITY_PROXY.entityCounter().getAndDecrement();
        return Entity.nextEntityId();
    }
}
