package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.living.mob.pathfinder.monster.zombie;

import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.entity.villager.VillagerData;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfession;
import com.github.retrooper.packetevents.protocol.entity.villager.profession.VillagerProfessions;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerType;
import com.github.retrooper.packetevents.protocol.entity.villager.type.VillagerTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.living.mob.pathfinder.monster.zombie.PacketZombieVillager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PacketZombieVillagerImpl extends PacketZombieImpl<PacketZombieVillager> implements PacketZombieVillager {

    private static final Supplier<VillagerData> DEFAULT_VILLAGER_DATA = () -> new VillagerData(VillagerTypes.PLAINS, VillagerProfessions.NONE, 1);

    public PacketZombieVillagerImpl(UUID uuid) {
        super(uuid, EntityTypes.ZOMBIE_VILLAGER);
    }

    @Override
    public boolean converting() {
        return get(CONVERTING_INDEX, false);
    }

    @Override
    public void converting(boolean converting) {
        set(CONVERTING_INDEX, converting);
        afterSet();
    }

    @Override
    public @NotNull VillagerType villagerType() {
        return getOrCreateVillagerData().getType();
    }

    @Override
    public void villagerType(@NotNull VillagerType type) {
        editVillagerData(data -> data.setType(type));
    }

    @Override
    public VillagerProfession villagerProfession() {
        return getOrCreateVillagerData().getProfession();
    }

    @Override
    public void villagerProfession(@NotNull VillagerProfession profession) {
        editVillagerData(data -> data.setProfession(profession));
    }

    @Override
    public int villagerLevel() {
        return getOrCreateVillagerData().getLevel();
    }

    @Override
    public void villagerLevel(int level) {
        editVillagerData(data -> data.setLevel(level));
    }

    private void editVillagerData(@NotNull Consumer<VillagerData> consumer) {
        final VillagerData villagerData = getOrCreateVillagerData();
        consumer.accept(villagerData);

        set(VILLAGER_DATA_INDEX, EntityDataTypes.VILLAGER_DATA, villagerData);
        afterSet();
    }

    private @NotNull VillagerData getOrCreateVillagerData() {
        VillagerData villagerData = get(VILLAGER_DATA_INDEX, null);

        if (villagerData == null) {
            villagerData = DEFAULT_VILLAGER_DATA.get();
            set(VILLAGER_DATA_INDEX, EntityDataTypes.VILLAGER_DATA, villagerData);
        }

        return villagerData;
    }
}
