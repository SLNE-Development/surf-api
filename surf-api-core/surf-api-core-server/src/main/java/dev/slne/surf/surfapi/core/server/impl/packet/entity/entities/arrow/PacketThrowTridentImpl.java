package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.arrow;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow.PacketThrowTrident;
import java.util.UUID;

public class PacketThrowTridentImpl extends PacketAbstractArrowImpl<PacketThrowTrident> implements
    PacketThrowTrident {

  public PacketThrowTridentImpl(UUID uuid) {
    super(uuid, EntityTypes.TRIDENT);
  }

  @Override
  public int loyaltyLevel() {
    return get(LOYALTY_LEVEL_INDEX, 0);
  }

  @Override
  public void loyaltyLevel(int loyaltyLevel) {
    set(LOYALTY_LEVEL_INDEX, loyaltyLevel);
    afterSet();
  }

  @Override
  public boolean enchantmentGlint() {
    return get(ENCHANTMENT_GLINT_INDEX, false);
  }

  @Override
  public void enchantmentGlint(boolean enchantmentGlint) {
    set(ENCHANTMENT_GLINT_INDEX, enchantmentGlint);
    afterSet();
  }
}
