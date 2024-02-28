package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.basic.PacketFireworkRocket;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class PacketFireworkRocketImpl extends
    PacketEntityImpl<PacketFireworkRocket> implements PacketFireworkRocket {

  public PacketFireworkRocketImpl(UUID uuid) {
    super(uuid, EntityTypes.FIREWORK_ROCKET);
    shooterEntityId = -1;
  }

  @Override
  public ItemStack fireworkItem() {
    return get(FIREWORK_ITEM_INDEX, ItemStack.EMPTY.copy());
  }

  @Override
  public void fireworkItem(@NotNull ItemStack fireworkItem) {
    set(FIREWORK_ITEM_INDEX, fireworkItem);
    afterSet();
  }

  @Override
  public void shooterEntityId(int shooterEntityId) {
    super.shooterEntityId(shooterEntityId);
    setOptInt(SHOOTER_ENTITY_ID_INDEX,
        Optional.ofNullable(shooterEntityId == -1 ? null : shooterEntityId));
    afterSet();
  }

  @Override
  public boolean shotAtAngle() {
    return get(SHOT_AT_ANGLE_INDEX, false);
  }

  @Override
  public void shotAtAngle(boolean shotAtAngle) {
    set(SHOT_AT_ANGLE_INDEX, shotAtAngle);
    afterSet();
  }

  @Contract(pure = true)
  @Override
  public int getData() {
    return NO_DATA;
  }
}
