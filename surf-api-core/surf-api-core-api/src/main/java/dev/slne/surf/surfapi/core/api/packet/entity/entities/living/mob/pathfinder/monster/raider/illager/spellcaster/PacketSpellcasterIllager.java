package dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.spellcaster;

import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.raider.illager.PacketAbstractIllager;
import dev.slne.surf.surfapi.core.api.util.ById;
import dev.slne.surf.surfapi.core.api.util.Util;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface PacketSpellcasterIllager<Impl extends PacketSpellcasterIllager<Impl>> extends PacketAbstractIllager<Impl> {

    int SPELL_INDEX = 17;

    /**
     * Gets the {@link Spell} the entity is currently using.
     *
     * @return the current spell
     */
    @NotNull
    Spell spell();

    /**
     * Sets the {@link Spell} the entity is currently using.
     *
     * @param spell the spell the entity should be using
     */
    void spell(@NotNull Spell spell);

    /**
     * Represents the current spell the entity is using.
     */
    enum Spell implements ById.ByByteId {

        /**
         * No spell is being used.
         */
        NONE,
        /**
         * The spell that summons Vexes.
         */
        SUMMON_VEX,
        /**
         * The spell that summons Fangs.
         */
        FANGS,
        /**
         * The "wololo" spell.
         */
        WOLOLO,
        /**
         * The spell that makes the casting entity invisible.
         */
        DISAPPEAR,
        /**
         * The spell that makes the target blind.
         */
        BLINDNESS;

        public static final Byte2ObjectMap<Spell> BY_ID = Util.byByteIdMap(values());

        @Contract(pure = true)
        public byte id() {
            return (byte) ordinal();
        }
    }
}
