package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.arrow;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow.PacketSpectralArrow;
import java.util.UUID;

public final class PacketSpectralArrowImpl extends
    PacketAbstractArrowImpl<PacketSpectralArrow> implements PacketSpectralArrow {

  public PacketSpectralArrowImpl(UUID uuid) {
    super(uuid, EntityTypes.SPECTRAL_ARROW);
  }

  @Override
  public int getData() {
    return shooterEntityId <= 0 ? 0 : shooterEntityId + 1;
  }
}
