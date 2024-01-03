package dev.slne.surf.surfapi.core.api.packet.entity.entities.basic;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3i;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.CanBeSpawned;
import dev.slne.surf.surfapi.core.api.packet.entity.annotation.NeedsRespawn;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.PacketEntity;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.Spawnable;
import dev.slne.surf.surfapi.core.api.util.BlockStateFactory;
import org.jetbrains.annotations.NotNull;

@CanBeSpawned
public interface PacketFallingBlock extends PacketEntity<PacketFallingBlock>, Spawnable {

    int SPAWN_POSITION_INDEX = 8;

    Vector3i spawnPosition();

    void spawnPosition(@NotNull Vector3i spawnPosition);

    WrappedBlockState blockState();

    @NeedsRespawn
    void blockState(@NotNull WrappedBlockState blockState);

    default void blockState(@NotNull StateType type) {
        blockState(BlockStateFactory.of(type));
    }
}
