package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import static com.google.common.base.Preconditions.checkArgument;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Orientation;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketPainting;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PacketPaintingImpl extends PacketEntityImpl<PacketPainting> implements PacketPainting {

  private Orientation orientation = Orientation.SOUTH;

  public PacketPaintingImpl(UUID uuid) {
    super(uuid, EntityTypes.PAINTING);
  }

  @Override
  public PaintingType painting() {
    return PaintingType.getById(get(PAINTING_INDEX, PaintingType.KEBAB.getId()));
  }

  @Override
  public void painting(@NotNull PaintingType painting) {
    set(PAINTING_INDEX, painting);
    afterSet();
  }

  @Override
  public Orientation orientation() {
    return orientation;
  }

  @Override
  public void orientation(@NotNull Orientation orientation) {
    checkArgument(orientation != Orientation.UP && orientation != Orientation.DOWN,
        "Orientation cannot be UP nor DOWN!");

    this.orientation = orientation;
  }

  @Override
  public int getData() {
    return orientation.ordinal();
  }
}
