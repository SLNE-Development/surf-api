package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketGlowItemFrame;
import java.util.UUID;

public final class PacketGlowItemFrameImpl extends
    PacketItemFrameImpl<PacketGlowItemFrame> implements PacketGlowItemFrame {

  public PacketGlowItemFrameImpl(UUID uuid) {
    super(uuid, EntityTypes.GLOW_ITEM_FRAME);
  }
}
