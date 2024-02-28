package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.PacketMob;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.PacketLivingEntityImpl;
import java.util.UUID;

public abstract class PacketMobImpl<Impl extends PacketMob<Impl>> extends
    PacketLivingEntityImpl<Impl> implements PacketMob<Impl> {

  public PacketMobImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  @Override
  public boolean noAI() {
    return getMaskBit(MOB_BIT_MASK_INDEX, NO_AI_BIT);
  }

  @Override
  public void noAI(boolean noAI) {
    setMaskBit(MOB_BIT_MASK_INDEX, NO_AI_BIT, noAI);
    afterSet();
  }

  @Override
  public boolean leftHanded() {
    return getMaskBit(MOB_BIT_MASK_INDEX, LEFT_HANDED_BIT);
  }

  @Override
  public void leftHanded(boolean leftHanded) {
    setMaskBit(MOB_BIT_MASK_INDEX, LEFT_HANDED_BIT, leftHanded);
    afterSet();
  }

  @Override
  public boolean aggressive() {
    return getMaskBit(MOB_BIT_MASK_INDEX, AGGRESSIVE_BIT);
  }

  @Override
  public void aggressive(boolean aggressive) {
    setMaskBit(MOB_BIT_MASK_INDEX, AGGRESSIVE_BIT, aggressive);
    afterSet();
  }
}
