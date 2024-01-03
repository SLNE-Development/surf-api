package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.tameable.PacketTameableAnimal;
import dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAnimalImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public abstract class PacketTameableAnimalImpl<Impl extends PacketTameableAnimal<Impl>> extends PacketAnimalImpl<Impl> implements PacketTameableAnimal<Impl> {

    public PacketTameableAnimalImpl(UUID uuid, EntityType type) {
        super(uuid, type);
    }

    @Override
    public boolean tamed() {
        return getMaskBit(TAMEABLE_FLAGS, TAMED_FLAG);
    }

    @Override
    public void setTamed(boolean tame) {
        setTamedNoUpdate(tame);
        if (!tame) setOwnerUuidNoUpdate(null);
        afterSet();
    }

    @Override
    public Optional<UUID> ownerUuid() {
        return get(OWNER_UUID, Optional.empty());
    }

    @Override
    public void ownerUuid(@Nullable UUID ownerUuid) {
        setOwnerUuidNoUpdate(ownerUuid);
        setTamedNoUpdate(ownerUuid != null);
        afterSet();
    }

    @Override
    public boolean sitting() {
        return getMaskBit(TAMEABLE_FLAGS, SITTING_FLAG);
    }

    @Override
    public void setSitting(boolean sitting) {
        setMaskBit(TAMEABLE_FLAGS, SITTING_FLAG, sitting);
        afterSet();
    }

    private void setTamedNoUpdate(boolean tame) {
        setMaskBit(TAMEABLE_FLAGS, TAMED_FLAG, tame);
    }

    private void setOwnerUuidNoUpdate(@Nullable UUID ownerUuid) {
        setOptUuid(OWNER_UUID, Optional.ofNullable(ownerUuid));
    }
}
