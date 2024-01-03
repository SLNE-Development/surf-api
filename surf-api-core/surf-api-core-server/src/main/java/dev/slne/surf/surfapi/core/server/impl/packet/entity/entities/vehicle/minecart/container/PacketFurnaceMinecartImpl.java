package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart.container;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container.PacketFurnaceMinecart;
import dev.slne.surf.surfapi.core.api.util.BlockStateFactory;

import java.util.UUID;

public final class PacketFurnaceMinecartImpl extends PacketAbstractMinecartContainerImpl<PacketFurnaceMinecart> implements PacketFurnaceMinecart {

    private static final WrappedBlockState FURNACE_BLOCK_STATE = BlockStateFactory.builder(StateTypes.FURNACE).facing(BlockFace.NORTH).build();
    private static final WrappedBlockState FURNACE_BLOCK_STATE_LIT = BlockStateFactory.builder(FURNACE_BLOCK_STATE).lit(true).build();

    public PacketFurnaceMinecartImpl(UUID uuid) {
        super(uuid, EntityTypes.FURNACE_MINECART, 6, FURNACE_BLOCK_STATE);
    }

    @Override
    public boolean hasFuel() {
        return get(HAS_FUEL_INDEX, false);
    }

    @Override
    public void hasFuel(boolean hasFuel, boolean updateDisplayedBlockState) {
        set(HAS_FUEL_INDEX, hasFuel);

        if (updateDisplayedBlockState) {
            set(DISPLAY_BLOCK_DATA_INDEX, hasFuel ? FURNACE_BLOCK_STATE_LIT.getGlobalId() : FURNACE_BLOCK_STATE.getGlobalId());
        }

        afterSet();
    }

    @Override
    public int getData() {
        return 2;
    }
}
