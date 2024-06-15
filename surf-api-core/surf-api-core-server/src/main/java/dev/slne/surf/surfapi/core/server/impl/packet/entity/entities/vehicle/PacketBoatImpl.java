package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.PacketBoat;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.PacketEntityImpl;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public sealed class PacketBoatImpl<Impl extends PacketBoat<Impl>> extends
    PacketEntityImpl<Impl> implements PacketBoat<Impl> permits PacketChestBoatImpl {

  protected PacketBoatImpl(UUID uuid, EntityType type) {
    super(uuid, type);
  }

  public PacketBoatImpl(UUID uuid) {
    super(uuid, EntityTypes.BOAT);
  }

  @Override
  public int timeSinceLastHit() {
    return get(TIME_SINCE_LAST_HIT_INDEX, 0);
  }

  @Override
  public void timeSinceLastHit(int timeSinceLastHit) {
    set(TIME_SINCE_LAST_HIT_INDEX, timeSinceLastHit);
    afterSet();
  }

  @Override
  public int forwardDirection() {
    return get(FORWARD_DIRECTION_INDEX, 1);
  }

  @Override
  public void forwardDirection(int forwardDirection) {
    set(FORWARD_DIRECTION_INDEX, forwardDirection);
    afterSet();
  }

  @Override
  public float damageTaken() {
    return get(DAMAGE_TAKEN_INDEX, 0.0f);
  }

  @Override
  public void damageTaken(float damageTaken) {
    set(DAMAGE_TAKEN_INDEX, damageTaken);
    afterSet();
  }

  @Override
  public Type type() {
    return Type.getById(get(TYPE_ID_INDEX, Type.OAK.getId()));
  }

  @Override
  public void type(@NotNull Type type) {
    set(TYPE_ID_INDEX, type.getId());
    afterSet();
  }

  @Override
  public boolean paddleLeft() {
    return get(PADDLE_LEFT_INDEX, false);
  }

  @Override
  public void paddleLeft(boolean paddleLeft) {
    set(PADDLE_LEFT_INDEX, paddleLeft);
    afterSet();
  }

  @Override
  public boolean paddleRight() {
    return get(PADDLE_RIGHT_INDEX, false);
  }

  @Override
  public void paddleRight(boolean paddleRight) {
    set(PADDLE_RIGHT_INDEX, paddleRight);
    afterSet();
  }

  @Override
  public int bubbleTime() {
    return get(BUBBLE_TIME_INDEX, 0);
  }

  @Override
  public void bubbleTime(int bubbleTime) {
    set(BUBBLE_TIME_INDEX, bubbleTime);
    afterSet();
  }
}
