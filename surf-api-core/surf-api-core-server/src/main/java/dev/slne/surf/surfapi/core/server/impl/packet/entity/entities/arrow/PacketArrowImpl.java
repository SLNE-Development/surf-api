package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.arrow;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.arrow.PacketArrow;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

public final class PacketArrowImpl extends PacketAbstractArrowImpl<PacketArrow> implements
    PacketArrow {

  public PacketArrowImpl(UUID uuid) {
    super(uuid, EntityTypes.ARROW);
  }

  @Override
  public Optional<TextColor> color() {
    int color = get(COLOR_INDEX, -1);

    if (color <= -1) {
      return Optional.empty();
    }

    return Optional.of(TextColor.color(color));
  }

  @Override
  public void color(@Nullable TextColor color) {
    set(COLOR_INDEX, color != null ? color.value() : -1);
    afterSet();
  }

  @Override
  public int getData() {
    return shooterEntityId <= 0 ? 0 : shooterEntityId + 1;
  }
}
