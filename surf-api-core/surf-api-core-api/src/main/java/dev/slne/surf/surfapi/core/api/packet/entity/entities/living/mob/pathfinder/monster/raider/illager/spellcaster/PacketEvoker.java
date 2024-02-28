package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster;

import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;

@CanBeSpawned
public interface PacketEvoker extends PacketSpellcasterIllager<PacketEvoker>, Spawnable {

}
