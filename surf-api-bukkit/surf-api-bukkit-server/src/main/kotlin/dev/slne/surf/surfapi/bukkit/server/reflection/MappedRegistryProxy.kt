package dev.slne.surf.surfapi.bukkit.server.reflection

import it.unimi.dsi.fastutil.objects.ObjectList
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.MappedRegistry
import net.minecraft.core.RegistrationInfo
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(MappedRegistry::class)
interface MappedRegistryProxy {

    @FieldSetter("frozen")
    fun setFrozen(instance: MappedRegistry<*>, frozen: Boolean)

    @FieldGetter("byKey")
    fun <T> getByKey(instance: MappedRegistry<T>): MutableMap<ResourceKey<T>, Holder.Reference<T>>

    @FieldGetter("byLocation")
    fun <T> getByLocation(instance: MappedRegistry<T>): MutableMap<ResourceLocation, Holder.Reference<T>>

    @FieldGetter("byValue")
    fun <T> getByValue(instance: MappedRegistry<T>): MutableMap<T, Holder.Reference<T>>

    @FieldGetter("byId")
    fun <T> getById(instance: MappedRegistry<T>): ObjectList<Holder.Reference<T>>

    @FieldGetter("toId")
    fun <T> getToId(instance: MappedRegistry<T>): Reference2IntMap<T>

    @FieldGetter("registrationInfos")
    fun <T> getRegistrationInfos(instance: MappedRegistry<T>): MutableMap<ResourceKey<T>, RegistrationInfo>

    @FieldGetter("frozenTags")
    fun <T> getFrozenTags(instance: MappedRegistry<T>): MutableMap<TagKey<T>, HolderSet.Named<T>>

    @FieldGetter("temporaryUnfrozenMap")
    fun <T> getTemporaryUnfrozenMap(instance: MappedRegistry<T>): MutableMap<ResourceLocation, T>


    @MethodName("refreshTagsInHolders")
    fun refreshTagsInHolders(instance: MappedRegistry<*>)
}