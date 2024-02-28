package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart.container;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.container.PacketChestMinecart;
import dev.slne.surf.surfapi.core.api.util.BlockStateFactory;
import java.util.UUID;

public final class PacketChestMinecartImpl extends
    PacketAbstractMinecartContainerImpl<PacketChestMinecart> implements PacketChestMinecart {

  private static final WrappedBlockState CHEST_BLOCK_STATE = BlockStateFactory.builder(
      StateTypes.CHEST).facing(BlockFace.NORTH).build();

  public PacketChestMinecartImpl(UUID uuid) {
    super(uuid, EntityTypes.CHEST_MINECART, 8, CHEST_BLOCK_STATE);
    StateTypes.CHEST.createBlockState().setFacing(BlockFace.NORTH);
  }

  @Override
  public int getData() {
    return 1;
  }
}
