package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.interaction;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.interaction.PacketInteraction;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;

public final class PacketInteractionImpl extends PacketEntityImpl<PacketInteraction> implements
    PacketInteraction {

  public PacketInteractionImpl(UUID uuid) {
    super(uuid, EntityTypes.INTERACTION);
  }

  @Override
  public float width() {
    return get(WIDTH_INDEX, 1.0f);
  }

  @Override
  public void width(float width) {
    set(WIDTH_INDEX, width);
    afterSet();
  }

  @Override
  public float height() {
    return get(HEIGHT_INDEX, 1.0f);
  }

  @Override
  public void height(float height) {
    set(HEIGHT_INDEX, 1.0f);
    afterSet();
  }

  @Override
  public boolean responsive() {
    return get(RESPONSIVE_INDEX, false);
  }

  @Override
  public void responsive(boolean responsive) {
    set(RESPONSIVE_INDEX, responsive);
    afterSet();
  }
}
