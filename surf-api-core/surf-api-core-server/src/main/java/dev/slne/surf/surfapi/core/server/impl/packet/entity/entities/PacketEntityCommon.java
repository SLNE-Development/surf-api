package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityMetadataProvider;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.base.MoreObjects;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.util.pos.rot.PreciseRotation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.math.vector.Vector3f;
import org.spongepowered.math.vector.Vector4f;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;

import static com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.*;
import static com.google.common.base.Preconditions.*;

public abstract class PacketEntityCommon implements EntityMetadataProvider {

    protected static final ClientVersion LATEST_CLIENT_VERSION = ClientVersion.getLatest();

    private Int2ObjectMap<Int2ObjectMap<EntityData>> protocolToDataVersion;
    protected EntityType type;
    protected boolean deleted = false;

    protected PacketEntityCommon(EntityType type) {
        this.type = type;
        this.protocolToDataVersion = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());
    }

    protected void addSupportedVersion(ClientVersion from, ClientVersion to) {
        for (ClientVersion version : EnumSet.range(from, to)) {
            addSupportedVersion(version);
        }
    }

    protected void addSupportedVersion(ClientVersion version) {
        addSupportedVersion(protocolToDataVersion, version);
    }

    @SuppressWarnings("unused")
    protected <Data> void setForVersions(ClientVersion from, ClientVersion to, int index, EntityDataType<Data> type, Data value) {
        final EnumSet<ClientVersion> versions = EnumSet.range(from, to);

        for (ClientVersion version : versions) {
            setForVersion(version, index, type, value);
        }
    }

    protected <Data> void setForVersion(ClientVersion clientVersion, int index, EntityDataType<Data> type, Data value) {
        checkDeleted();
        protocolToDataVersion.computeIfPresent(clientVersion.getProtocolVersion(), (protocolVersion, indexToEntityDatas) -> {
            final EntityData data = indexToEntityDatas.get(index);

            if (data != null) {
                data.setValue(value);
            } else {
                indexToEntityDatas.put(index, new EntityData((byte) index, type, value));
            }

            return indexToEntityDatas;
        });
    }

    protected <Data> void set(int index, EntityDataType<Data> type, Data value) {
        setForVersion(LATEST_CLIENT_VERSION, index, type, value);
    }

    protected void set(int index, boolean value) {
        set(index, BOOLEAN, value);
    }

    protected void set(int index, int value) {
        set(index, INT, value);
    }

    protected void set(int index, float value) {
        set(index, FLOAT, value);
    }

    protected void setByte(int index, byte value) {
        set(index, BYTE, value);
    }

    protected void set(int index, String value) {
        set(index, STRING, value);
    }

    protected void set(int index, ItemStack value) {
        set(index, ITEMSTACK, value);
    }

    protected void set(int index, @NotNull PreciseRotation value) {
        set(index, ROTATION, value.toPacketEvents());
    }

    protected void set(int index, @NotNull Vector3i value) {
        set(index, BLOCK_POSITION, value);
    }

    protected void set(int index, @NotNull PaintingType value) {
        set(index, PAINTING_VARIANT_TYPE, value.getId());
    }

    protected void set(int index, @NotNull Component value) {
        set(index, ADV_COMPONENT, value);
    }

    protected void set(int index, @NotNull Vector3f value) {
        set(index, VECTOR3F, new com.github.retrooper.packetevents.util.Vector3f(value.x(), value.y(), value.z()));
    }

    protected void set(int index, @NotNull Vector4f value) {
        set(index, QUATERNION, new Quaternion4f(value.x(), value.y(), value.z(), value.w()));
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "SameParameterValue"})
    protected void setOptInt(int index, Optional<Integer> value) {
        set(index, OPTIONAL_INT, value);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "SameParameterValue"})
    protected void setOptBlockPos(int index, @NotNull Optional<Vector3i> value) {
        set(index, OPTIONAL_BLOCK_POSITION, value);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "SameParameterValue"})
    protected void setOptUuid(int index, @NotNull Optional<UUID> value) {
        set(index, OPTIONAL_UUID, value);
    }

    @SuppressWarnings("unchecked")
    protected <Data> Data get(int index, @Nullable Data defaultReturnValue) {
        checkDeleted();
        final Int2ObjectMap<EntityData> map = protocolToDataVersion.get(LATEST_CLIENT_VERSION.getProtocolVersion());
        final EntityData data = map.get(index);

        return data == null ? defaultReturnValue : (Data) data.getValue();
    }

    protected int get(int index, int defaultReturnValue) {
        return get(index, (Integer) defaultReturnValue);
    }

    protected float get(int index, float defaultReturnValue) {
        return get(index, (Float) defaultReturnValue);
    }

    protected byte get(int index, byte defaultReturnValue) {
        return get(index, (Byte) defaultReturnValue);
    }

    protected boolean get(int index, boolean defaultReturnValue) {
        return get(index, (Boolean) defaultReturnValue);
    }

    protected Vector3f get3f(int index, @NotNull Vector3f defaultReturnValue) {
        final com.github.retrooper.packetevents.util.Vector3f pe = get(index, null);
        return pe == null ? defaultReturnValue : new Vector3f(pe.x, pe.y, pe.z);
    }

    protected Vector4f get4f(int index, @NotNull Vector4f defaultReturnValue) {
        final Quaternion4f pe = get(index, null);
        return pe == null ? defaultReturnValue : new Vector4f(pe.getX(), pe.getY(), pe.getZ(), pe.getW());
    }

    @SuppressWarnings("SameParameterValue")
    protected void setMaskBit(int index, byte bit, boolean value) {
        byte mask = this.getMask(index);
        boolean currentValue = (mask & bit) == bit;
        if (currentValue != value) {
            if (value) {
                mask |= bit;
            } else {
                mask &= (byte) (~bit);
            }

            this.setMask(index, mask);
        }
    }

    protected void setMaskBit(int index, byte bit, byte value) {
        byte mask = this.getMask(index);
        byte currentValue = (byte) (mask & bit);
        if (currentValue != value) {
            if (value != 0) {
                mask |= bit;
            } else {
                mask &= (byte) (~bit);
            }

            this.setMask(index, mask);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected boolean getMaskBit(int index, byte bit) {
        return getMaskBitRaw(index, bit) == bit;
    }

    protected byte getMaskBitRaw(int index, byte bit) { // TODO: does this work?
        return (byte) (this.getMask(index) & bit);
    }

    protected byte getMask(int index) {
        return get(index, (byte) 0);
    }

    protected void setMask(int index, byte mask) {
        set(index, BYTE, mask);
    }

    protected boolean isVersionNewerThanOrEquals(ClientVersion version, UUID uuid) {
        return SurfCoreApi.getCore().getPlayer(uuid)
                .map(player -> PacketEvents.getAPI().getPlayerManager().getUser(player)
                        .getClientVersion().isNewerThanOrEquals(version))
                .orElse(true);
    }

    protected void checkDeleted() {
        checkState(!deleted, "Entity has been deleted therefore it cannot be modified");
    }

    @Override
    public List<EntityData> entityData(ClientVersion clientVersion) {
        return new ArrayList<>(protocolToDataVersion.getOrDefault(
                clientVersion.getProtocolVersion(),
                protocolToDataVersion.get(LATEST_CLIENT_VERSION.getProtocolVersion())
        ).values());
    }

    @OverridingMethodsMustInvokeSuper
    public void delete() {
        deleted = true;

        protocolToDataVersion = null;
        type = null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("EntityData", protocolToDataVersion.get(LATEST_CLIENT_VERSION.getProtocolVersion()).toString())
                .add("type", type)
                .toString();
    }

    @SuppressWarnings("SameParameterValue")
    protected static void addSupportedVersion(@NotNull Int2ObjectMap<Int2ObjectMap<EntityData>> versionsMap,
                                              @NotNull ClientVersion version) {
        versionsMap.putIfAbsent(
                version.getProtocolVersion(),
                Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>())
        );
    }

    protected static Vector3i toPacketEvents(org.spongepowered.math.vector.Vector3i sponge) {
        return new Vector3i(sponge.x(), sponge.y(), sponge.z());
    }

    protected static org.spongepowered.math.vector.Vector3i fromPacketEvents(Vector3i packetEvents) {
        return new org.spongepowered.math.vector.Vector3i(packetEvents.x, packetEvents.y, packetEvents.z);
    }
}
