package dev.slne.surf.surfapi.bukkit.api.packet.entity.entities;

import me.tofaa.entitylib.entity.WrapperEntityEquipment;
import me.tofaa.entitylib.entity.WrapperLivingEntity;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

@ApiStatus.NonExtendable
@ApiStatus.Internal
public final class SurfLivingEntity<M extends LivingEntityMeta> extends SurfEntity<M> {
    public SurfLivingEntity(WrapperLivingEntity entity, Class<M> metaClass) {
        super(entity, metaClass);
    }

    public WrapperEntityEquipment getEquipment() {
        return ((WrapperLivingEntity) entity).getEquipment();
    }

    @Contract(pure = true)
    @Override
    public WrapperLivingEntity getEntity() {
        return ((WrapperLivingEntity) entity);
    }
}
