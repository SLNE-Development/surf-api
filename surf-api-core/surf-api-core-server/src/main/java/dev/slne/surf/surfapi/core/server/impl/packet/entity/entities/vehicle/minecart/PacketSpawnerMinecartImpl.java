package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketSpawnerMinecart;
import dev.slne.surf.surfapi.core.api.util.blockstate.BlockStateFactory;
import java.util.UUID;

public final class PacketSpawnerMinecartImpl extends
    PacketAbstractMinecartImpl<PacketSpawnerMinecart> implements PacketSpawnerMinecart {

  private static final WrappedBlockState SPAWNER = BlockStateFactory.of(StateTypes.SPAWNER);

  public PacketSpawnerMinecartImpl(UUID uuid) {
    super(uuid, EntityTypes.SPAWNER_MINECART, 6, SPAWNER);
  }

  @Override
  public int getData() {
    return 4;
  }
}
