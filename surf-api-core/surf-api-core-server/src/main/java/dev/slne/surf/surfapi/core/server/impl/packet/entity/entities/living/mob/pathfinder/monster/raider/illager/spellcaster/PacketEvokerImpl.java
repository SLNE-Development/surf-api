package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster.PacketEvoker;
import java.util.UUID;

public final class PacketEvokerImpl extends PacketSpellcasterIllagerImpl<PacketEvoker> implements
    PacketEvoker {

  public PacketEvokerImpl(UUID uuid) {
    super(uuid, EntityTypes.EVOKER);
  }
}
