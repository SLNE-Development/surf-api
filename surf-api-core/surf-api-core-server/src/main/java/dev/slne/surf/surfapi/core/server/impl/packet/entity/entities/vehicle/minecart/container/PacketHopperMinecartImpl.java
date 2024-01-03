package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart.container;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container.PacketHopperMinecart;

import java.util.UUID;

public final class PacketHopperMinecartImpl extends PacketAbstractMinecartContainerImpl<PacketHopperMinecart> implements PacketHopperMinecart {

    public PacketHopperMinecartImpl(UUID uuid) {
        super(uuid, EntityTypes.HOPPER_MINECART, 1, StateTypes.HOPPER.createBlockState());
    }

    @Override
    public int getData() {
        return 5;
    }
}
