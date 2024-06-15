package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.throwprojectile;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.throwprojectile.PacketThrowExperienceBottle;
import dev.slne.surf.surfapi.core.api.util.ItemStackFactory;
import java.util.UUID;

public final class PacketThrowExperienceBottleImpl extends
    PacketThrowItemProjectileImpl<PacketThrowExperienceBottle> implements
    PacketThrowExperienceBottle {

  private static final ItemStack EXPERIENCE_BOTTLE = ItemStackFactory.of(
      ItemTypes.EXPERIENCE_BOTTLE);

  public PacketThrowExperienceBottleImpl(UUID uuid) {
    super(uuid, EntityTypes.EXPERIENCE_BOTTLE, EXPERIENCE_BOTTLE.copy());
  }
}
