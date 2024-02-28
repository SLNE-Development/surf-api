package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster.PacketIllusioner;
import java.util.UUID;

public final class PacketIllusionerImpl extends
    PacketSpellcasterIllagerImpl<PacketIllusioner> implements PacketIllusioner {

  public PacketIllusionerImpl(UUID uuid) {
    super(uuid, EntityTypes.ILLUSIONER);
  }
}
