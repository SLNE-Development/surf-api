package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart.container;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container.PacketAbstractMinecartContainer;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart.PacketAbstractMinecartImpl;

import java.util.UUID;

public abstract class PacketAbstractMinecartContainerImpl<Impl extends PacketAbstractMinecartContainer<Impl>> extends PacketAbstractMinecartImpl<Impl> implements PacketAbstractMinecartContainer<Impl> {

    public PacketAbstractMinecartContainerImpl(UUID uuid, EntityType type, int defaultBlockOffset, WrappedBlockState defaultBlockState) {
        super(uuid, type, defaultBlockOffset, defaultBlockState);
    }
}
