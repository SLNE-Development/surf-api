package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.DyeColor;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketCat;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketCatImpl extends PacketTameableAnimalImpl<PacketCat> implements PacketCat {

  public PacketCatImpl(UUID uuid) {
    super(uuid, EntityTypes.CAT);
  }

  @Override
  public Type catType() {
    return Type.BY_ID.get(get(CAT_TYPE, Type.BLACK.id()));
  }

  @Override
  public void catType(@NotNull Type type) {
    set(CAT_TYPE, type.id());
    afterSet();
  }

  @Override
  public boolean lyingDown() {
    return get(LYING_DOWN, false);
  }

  @Override
  public void lyingDown(boolean lyingDown) {
    set(LYING_DOWN, lyingDown);
    afterSet();
  }

  @Override
  public boolean headUp() {
    return get(HEAD_UP, false);
  }

  @Override
  public void headUp(boolean headUp) {
    set(HEAD_UP, headUp);
    afterSet();
  }

  @Override
  public DyeColor collarColor() {
    return DyeColor.getById(get(COLLAR_COLOR, DyeColor.RED.getWoolData()));
  }

  @Override
  public void collarColor(@NotNull DyeColor color) {
    set(COLLAR_COLOR, color.getWoolData());
    afterSet();
  }
}
