package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowSnowball;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;
import java.util.UUID;

public final class PacketThrowSnowballImpl extends
    PacketThrowItemProjectileImpl<PacketThrowSnowball> implements PacketThrowSnowball {

  private static final ItemStack SNOWBALL = ItemStackFactory.of(ItemTypes.SNOWBALL);

  public PacketThrowSnowballImpl(UUID uuid) {
    super(uuid, EntityTypes.SNOWBALL, SNOWBALL.copy());
  }
}
