package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketTurtle;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector3i;

import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketTurtleImpl extends PacketAnimalImpl<PacketTurtle> implements PacketTurtle {

    public PacketTurtleImpl(UUID uuid) {
        super(uuid, EntityTypes.TURTLE);
    }

    @Override
    public Vector3i homePos() {
        return fromPacketEvents(get(HOME_POS_INDEX, com.github.retrooper.packetevents.util.Vector3i.zero()));
    }

    @Override
    public void homePos(@NotNull Vector3i pos) {
        set(HOME_POS_INDEX, toPacketEvents(checkNotNull(pos, "pos")));
        afterSet();
    }

    @Override
    public boolean hasEgg() {
        return get(HAS_EGG_INDEX, false);
    }

    @Override
    public void hasEgg(boolean hasEgg) {
        set(HAS_EGG_INDEX, hasEgg);
        afterSet();
    }

    @Override
    public boolean layingEgg() {
        return get(LAYING_EGG_INDEX, false);
    }

    @Override
    public void layingEgg(boolean layingEgg) {
        set(LAYING_EGG_INDEX, layingEgg);
        afterSet();
    }

    @Override
    public Vector3i travelPos() {
        return fromPacketEvents(get(TRAVEL_POS_INDEX, com.github.retrooper.packetevents.util.Vector3i.zero()));
    }

    @Override
    public void travelPos(@NotNull Vector3i pos) {
        set(TRAVEL_POS_INDEX, toPacketEvents(checkNotNull(pos, "pos")));
        afterSet();
    }

    @Override
    public boolean goingHome() {
        return get(GOING_HOME_INDEX, false);
    }

    @Override
    public void goingHome(boolean goingHome) {
        set(GOING_HOME_INDEX, goingHome);
        afterSet();
    }

    @Override
    public boolean traveling() {
        return get(TRAVELING_INDEX, false);
    }

    @Override
    public void traveling(boolean traveling) {
        set(TRAVELING_INDEX, traveling);
        afterSet();
    }
}
