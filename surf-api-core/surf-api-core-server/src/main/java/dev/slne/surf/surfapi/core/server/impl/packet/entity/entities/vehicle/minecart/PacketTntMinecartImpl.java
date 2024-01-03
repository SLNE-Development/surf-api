package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketTntMinecart;
import dev.slne.surf.surfapi.core.api.util.BlockStateFactory;

import java.util.UUID;

public final class PacketTntMinecartImpl extends PacketAbstractMinecartImpl<PacketTntMinecart> implements PacketTntMinecart {

    private static final WrappedBlockState TNT_BLOCK_STATE = BlockStateFactory.of(StateTypes.TNT);

    public PacketTntMinecartImpl(UUID uuid) {
        super(uuid, EntityTypes.TNT_MINECART, 6, TNT_BLOCK_STATE);
    }

    @Override
    public int getData() {
        return 3;
    }
}
