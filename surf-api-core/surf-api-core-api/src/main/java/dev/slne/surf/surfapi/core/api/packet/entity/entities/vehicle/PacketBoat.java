package dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketBoat<Impl extends PacketBoat<Impl>> extends PacketEntity<Impl>, Spawnable {

  int TIME_SINCE_LAST_HIT_INDEX = 8, FORWARD_DIRECTION_INDEX = 9, DAMAGE_TAKEN_INDEX = 10, TYPE_ID_INDEX = 11,
      PADDLE_LEFT_INDEX = 12, PADDLE_RIGHT_INDEX = 13, BUBBLE_TIME_INDEX = 14;

  int timeSinceLastHit();

  void timeSinceLastHit(int timeSinceLastHit);

  int forwardDirection();

  void forwardDirection(int forwardDirection);

  float damageTaken();

  void damageTaken(float damageTaken);

  Type type();

  void type(@NotNull Type type);

  boolean paddleLeft();

  void paddleLeft(boolean paddleLeft);

  boolean paddleRight();

  void paddleRight(boolean paddleRight);

  int bubbleTime();

  void bubbleTime(int bubbleTime);

  enum Type {
    OAK(0),
    SPRUCE(1),
    BIRCH(2),
    JUNGLE(3),
    ACACIA(4),
    DARK_OAK(5);

    private static final Map<Integer, Type> BY_ID;

    static {
      BY_ID = Arrays.stream(values())
          .collect(Collectors.toConcurrentMap(type -> type.id, Function.identity()));
    }

    private final int id;

    @Contract(pure = true)
    Type(int id) {
      this.id = id;
    }

    public static Type getById(int id) {
      return BY_ID.get(id);
    }

    public int getId() {
      return id;
    }
  }
}
