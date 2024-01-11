package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.other;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnExperienceOrb;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.other.PacketExperienceOrb;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.*;

public final class PacketExperienceOrbImpl implements PacketExperienceOrb {
    private final int entityId;
    private final Vector3i position;
    private final short experience;
    private final Set<UUID> viewers;

    public PacketExperienceOrbImpl(Vector3i position, short experience) {
        this.position = position;
        this.experience = experience;
        this.entityId = SurfCorePacketEntityApi.get().idProvider().nextEntityId();
        this.viewers = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public Vector3i position() {
        return position;
    }

    @Override
    public short experience() {
        return experience;
    }

    @Override
    public boolean addViewer(UUID viewer) {
        checkNotNull(viewer, "Viewer cannot be null");

        boolean success = viewers.add(viewer);

        if (success) {
            SurfCorePacketApi.get().sendPacket(viewer, spawnPacket());
        }

        return success;
    }

    @Override
    public boolean removeViewer(UUID viewer) {
        checkNotNull(viewer, "Viewer cannot be null");

        boolean success = viewers.remove(viewer);

        if (success) {
            SurfCorePacketApi.get().sendPacket(viewer, despawnPacket());
        }

        return success;
    }

    @Override
    public void despawn() {
        PacketWrapper<?> packet = despawnPacket();
        viewers.forEach(uuid -> SurfCorePacketApi.get().sendPacket(uuid, packet));
    }

    private PacketWrapper<?> spawnPacket() {
        return new WrapperPlayServerSpawnExperienceOrb(entityId, position.x(), position.y(), position.z(), experience);
    }

    private PacketWrapper<?> despawnPacket() {
        return new WrapperPlayServerDestroyEntities(entityId);
    }
}
