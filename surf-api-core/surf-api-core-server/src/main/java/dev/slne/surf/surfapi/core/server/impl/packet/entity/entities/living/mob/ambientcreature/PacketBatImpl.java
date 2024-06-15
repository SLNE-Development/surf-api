package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.ambientcreature;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.ambientcreature.PacketBat;
import java.util.UUID;

public final class PacketBatImpl extends PacketAmbientCreatureImpl<PacketBat> implements PacketBat {

  public PacketBatImpl(UUID uuid) {
    super(uuid, EntityTypes.BAT);
  }

  @Override
  public boolean hanging() {
    return getMaskBit(BAT_BIT_MASK_INDEX, HANGING_BIT);
  }

  @Override
  public void hanging(boolean hanging) {
    setMaskBit(BAT_BIT_MASK_INDEX, HANGING_BIT, hanging);
    afterSet();
  }
}
