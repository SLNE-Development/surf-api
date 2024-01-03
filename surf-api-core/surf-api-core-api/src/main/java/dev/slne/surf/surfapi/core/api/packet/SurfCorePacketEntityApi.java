package dev.slne.surf.surfapi.core.api.packet;

import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.packet.entity.interact.SurfInteractHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

@ApiStatus.NonExtendable
public interface SurfCorePacketEntityApi {

    void idProvider(@NotNull EntityIdProvider provider);

    @NotNull
    EntityIdProvider idProvider();

    <T extends PacketEntity<T>> void registerInteractHandler(SurfInteractHandler<T> handler);

    <T extends PacketEntity<T> & Spawnable> T spawnEntity(@NotNull Class<T> entityClass, @NotNull UUID uuid, @Nullable Consumer<T> initializer);

    default <T extends PacketEntity<T> & Spawnable> T spawnEntity(@NotNull Class<T> entityClass, @NotNull UUID uuid) {
        return spawnEntity(entityClass, uuid, null);
    }

    static SurfCorePacketEntityApi get() {
        return SurfCorePacketApi.get().getPacketEntityApi();
    }
}
