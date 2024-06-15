package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.PacketCreeper;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketCreeperImpl extends PacketMonsterImpl<PacketCreeper> implements
    PacketCreeper {

  public PacketCreeperImpl(UUID uuid) {
    super(uuid, EntityTypes.CREEPER);
  }

  @Override
  public State state() {
    return State.BY_ID.get(get(STATE_INDEX, State.IDLE.getId()));
  }

  @Override
  public void state(@NotNull State state) {
    set(STATE_INDEX, state.getId());
    afterSet();
  }

  @Override
  public boolean powered() {
    return get(POWERED_INDEX, false);
  }

  @Override
  public void powered(boolean value) {
    set(POWERED_INDEX, value);
    afterSet();
  }

  @Override
  public boolean ignited() {
    return get(IGNITED_INDEX, false);
  }

  @Override
  public void ignited(boolean ignited) {
    set(IGNITED_INDEX, ignited);
    afterSet();
  }
}
