package dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketThrowTrident extends PacketAbstractArrow<PacketThrowTrident>, Spawnable {

  int LOYALTY_LEVEL_INDEX = 10, ENCHANTMENT_GLINT_INDEX = 11;

  int loyaltyLevel();

  void loyaltyLevel(int loyaltyLevel);

  boolean enchantmentGlint();

  void enchantmentGlint(boolean enchantmentGlint);
}
