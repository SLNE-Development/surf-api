package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster.PacketSpellcasterIllager;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllagerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public abstract class PacketSpellcasterIllagerImpl<Impl extends PacketSpellcasterIllager<Impl>> extends PacketAbstractIllagerImpl<Impl> implements PacketSpellcasterIllager<Impl> {

    public PacketSpellcasterIllagerImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public @NotNull Spell spell() {
        return Spell.BY_ID.get(get(SPELL_INDEX, Spell.NONE.id()));
    }

    @Override
    public void spell(@NotNull Spell spell) {
        setByte(SPELL_INDEX, checkNotNull(spell, "Spell is null. Consider using Spell.NONE").id());
        afterSet();
    }
}
