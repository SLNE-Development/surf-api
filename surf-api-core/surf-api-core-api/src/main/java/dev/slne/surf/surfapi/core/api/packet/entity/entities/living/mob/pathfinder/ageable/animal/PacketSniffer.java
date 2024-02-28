package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.sniffer.SnifferState;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketSniffer extends PacketAnimal<PacketSniffer>, Spawnable {

  int STATE_INDEX = 17, DROP_SEED_AT_TICK_INDEX = 18;

  SnifferState state();

  void state(@NotNull SnifferState state);

  int dropSeedAtTick();

  void dropSeedAtTick(int dropSeedAtTick);
}
