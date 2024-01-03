package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.animal;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.animal.PacketAxolotl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class PacketAxolotlImpl extends PacketAnimalImpl<PacketAxolotl> implements PacketAxolotl {

    public PacketAxolotlImpl(UUID uuid) {
        super(uuid, EntityTypes.AXOLOTL);
    }

    @Override
    public Variant variant() {
        return Variant.values()[get(VARIANT_INDEX, 0)];
    }

    @Override
    public void variant(@NotNull Variant variant) {
        set(VARIANT_INDEX, variant.ordinal());
        afterSet();
    }

    @Override
    public boolean playingDead() {
        return get(PLAYING_DEAD_INDEX, false);
    }

    @Override
    public void playingDead(boolean playingDead) {
        set(PLAYING_DEAD_INDEX, playingDead);
        afterSet();
    }

    @Override
    public boolean spawnedFromBucket() {
        return get(SPAWNED_FROM_BUCKET_INDEX, false);
    }

    @Override
    public void spawnedFromBucket(boolean spawnedFromBucket) {
        set(SPAWNED_FROM_BUCKET_INDEX, spawnedFromBucket);
        afterSet();
    }
}
