package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowEnderPearl;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;
import java.util.UUID;

public final class PacketThrowEnderPearlImpl extends
    PacketThrowItemProjectileImpl<PacketThrowEnderPearl> implements PacketThrowEnderPearl {

  private static final ItemStack ENDER_PEARL = ItemStackFactory.of(ItemTypes.ENDER_PEARL);

  public PacketThrowEnderPearlImpl(UUID uuid) {
    super(uuid, EntityTypes.ENDER_PEARL, ENDER_PEARL.copy());
  }
}
