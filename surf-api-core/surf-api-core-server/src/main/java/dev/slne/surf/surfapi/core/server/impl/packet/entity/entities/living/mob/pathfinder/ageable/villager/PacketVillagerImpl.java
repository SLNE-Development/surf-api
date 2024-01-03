package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.ageable.villager;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.entity.villager.VillagerData;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfession;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfessions;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerType;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.ageable.villager.PacketVillager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.*;

public final class PacketVillagerImpl extends PacketAbstractVillagerImpl<PacketVillager> implements PacketVillager {

    private static final Supplier<VillagerData> DEFAULT_VILLAGER_DATA = () -> new VillagerData(VillagerTypes.PLAINS, VillagerProfessions.NONE, 1);

    public PacketVillagerImpl(UUID uuid) {
        super(uuid, EntityTypes.VILLAGER);
    }

    @Override
    public VillagerType villagerType() {
        return getVillagerData().getType();
    }

    @Override
    public void villagerType(@NotNull VillagerType type) {
        checkNotNull(type, "Type cannot be null");
        editVillagerData(data -> data.setType(type));
    }

    @Override
    public VillagerProfession profession() {
        return getVillagerData().getProfession();
    }

    @Override
    public void profession(@NotNull VillagerProfession profession) {
        checkNotNull(profession, "Profession cannot be null");
        editVillagerData(data -> data.setProfession(profession));
    }

    @Override
    public int villagerLevel() {
        return getVillagerData().getLevel();
    }

    @Override
    public void villagerLevel(int level) {
        checkArgument(level >= 1 && level <= 5, "Level must be between 1 and 5");
        editVillagerData(data -> data.setLevel(level));
    }

    private void editVillagerData(@NotNull Consumer<VillagerData> villagerData) {
        final VillagerData data = getVillagerData();

        villagerData.accept(data);

        villagerData(data);
        afterSet();
    }

    private VillagerData getVillagerData() {
        VillagerData villagerData = get(VILLAGER_DATA_INDEX, null);

        if (villagerData == null) {
            villagerData = DEFAULT_VILLAGER_DATA.get();
            villagerData(villagerData);
        }

        return villagerData;
    }

    private void villagerData(VillagerData villagerData) {
        set(VILLAGER_DATA_INDEX, EntityDataTypes.VILLAGER_DATA, villagerData);
    }
}
