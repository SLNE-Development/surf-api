package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.world.entity.Entity
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies
import xyz.jpenilla.reflectionremapper.proxy.annotation.Static

@Proxies(Entity::class)
@Suppress("ClassName")
interface V26_1EntityProxy {

    @FieldGetter("FLAG_GLOWING")
    @Static
    fun getFlagGlowing(): Int

    @FieldGetter("FLAG_INVISIBLE")
    @Static
    fun getFlagInvisible(): Int

    @FieldGetter("DATA_SHARED_FLAGS_ID")
    @Static
    fun getDataFlagsSharedId(): EntityDataAccessor<Byte>
}
