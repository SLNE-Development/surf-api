package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketFox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketFoxImpl extends PacketAnimalImpl<PacketFox> implements PacketFox {

    public PacketFoxImpl(UUID uuid) {
        super(uuid, EntityTypes.FOX);
    }

    @Override
    public Type foxType() {
        return Type.values()[get(TYPE_INDEX, 0)];
    }

    @Override
    public void foxType(@NotNull Type type) {
        set(TYPE_INDEX, checkNotNull(type, "type").ordinal());
        afterSet();
    }

    @Override
    public boolean sitting() {
        return getMaskBit(FOX_BIT_MASK_INDEX, SITTING_FLAG);
    }

    @Override
    public void sitting(boolean sitting) {
        setMaskBit(FOX_BIT_MASK_INDEX, SITTING_FLAG, sitting);
        afterSet();
    }

    @Override
    public boolean crouching() {
        return getMaskBit(FOX_BIT_MASK_INDEX, CROUCHING_FLAG);
    }

    @Override
    public void crouching(boolean crouching) {
        setMaskBit(FOX_BIT_MASK_INDEX, CROUCHING_FLAG, crouching);
        afterSet();
    }

    @Override
    public boolean interested() {
        return getMaskBit(FOX_BIT_MASK_INDEX, INTERESTED_FLAG);
    }

    @Override
    public void interested(boolean interested) {
        setMaskBit(FOX_BIT_MASK_INDEX, INTERESTED_FLAG, interested);
        afterSet();
    }

    @Override
    public boolean leaping() {
        return getMaskBit(FOX_BIT_MASK_INDEX, LEAPING_FLAG);
    }

    @Override
    public void leaping(boolean leaping) {
        setMaskBit(FOX_BIT_MASK_INDEX, LEAPING_FLAG, leaping);
        afterSet();
    }

    @Override
    public boolean sleeping() {
        return getMaskBit(FOX_BIT_MASK_INDEX, SLEEPING_FLAG);
    }

    @Override
    public void sleeping(boolean sleeping) {
        setMaskBit(FOX_BIT_MASK_INDEX, SLEEPING_FLAG, sleeping);
        afterSet();
    }

    @Override
    public boolean faceplanted() {
        return getMaskBit(FOX_BIT_MASK_INDEX, FACEPLANTED_FLAG);
    }

    @Override
    public void faceplanted(boolean faceplanted) {
        setMaskBit(FOX_BIT_MASK_INDEX, FACEPLANTED_FLAG, faceplanted);
        afterSet();
    }

    @Override
    public boolean defending() {
        return getMaskBit(FOX_BIT_MASK_INDEX, DEFENDING_FLAG);
    }

    @Override
    public void defending(boolean defending) {
        setMaskBit(FOX_BIT_MASK_INDEX, DEFENDING_FLAG, defending);
        afterSet();
    }

    @Override
    public Optional<UUID> firstTrustedPlayer() {
        return get(FIRST_TRUSTED_PLAYER_INDEX, Optional.empty());
    }

    @Override
    public void firstTrustedPlayer(@Nullable UUID uuid) {
        setOptUuid(FIRST_TRUSTED_PLAYER_INDEX, Optional.ofNullable(uuid));
        afterSet();
    }

    @Override
    public Optional<UUID> secondTrustedPlayer() {
        return get(SECOND_TRUSTED_PLAYER_INDEX, Optional.empty());
    }

    @Override
    public void secondTrustedPlayer(@Nullable UUID uuid) {
        setOptUuid(SECOND_TRUSTED_PLAYER_INDEX, Optional.ofNullable(uuid));
        afterSet();
    }
}
