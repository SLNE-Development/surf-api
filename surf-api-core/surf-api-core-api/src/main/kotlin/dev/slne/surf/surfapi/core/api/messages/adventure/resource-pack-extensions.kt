package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.buildText
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.resource.ResourcePackCallback
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import java.net.URI
import java.util.*

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ResourcePackInfoDsl

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ResourcePackDsl

inline fun resourcePackInfo(block: @ResourcePackInfoDsl ResourcePackInfo.Builder.() -> Unit): ResourcePackInfo {
    return ResourcePackInfo.resourcePackInfo().apply(block).build()
}

suspend inline fun asyncResourcePackInfo(
    crossinline block: @ResourcePackInfoDsl suspend ResourcePackInfo.Builder.() -> Unit,
): ResourcePackInfo = coroutineScope {
    ResourcePackInfo.resourcePackInfo()
        .apply { block() }
        .computeHashAndBuild(Dispatchers.IO.asExecutor())
        .await()
}

fun ResourcePackInfo.Builder.url(url: String) {
    uri(URI.create(url))
}

inline fun resourcePackRequest(block: @ResourcePackDsl ResourcePackRequest.Builder.() -> Unit): ResourcePackRequest {
    return ResourcePackRequest.resourcePackRequest().apply(block).build()
}

fun addingResourcePackRequest(vararg infos: ResourcePackInfo): ResourcePackRequest {
    require(infos.isNotEmpty()) { "At least one ResourcePackInfo must be provided" }
    return ResourcePackRequest.addingRequest(infos[0], *infos.drop(1).toTypedArray())
}

inline fun ResourcePackRequest.Builder.pack(block: @ResourcePackDsl ResourcePackInfo.Builder.() -> Unit) {
    packs(resourcePackInfo(block))
}

inline fun ResourcePackRequest.Builder.packs(block: @ResourcePackDsl ResourcePackRequestInfoBuilder.() -> Unit) {
    packs(ResourcePackRequestInfoBuilder().apply(block).infos)
}

inline fun ResourcePackRequest.Builder.simpleCallback(crossinline block: @ResourcePackDsl (packId: UUID, audience: Audience, success: Boolean) -> Unit) {
    callback(
        ResourcePackCallback.onTerminal(
            { packId, audience -> block(packId, audience, true) },
            { packId, audience -> block(packId, audience, false) }
        )
    )
}

inline fun ResourcePackRequest.Builder.prompt(block: @ResourcePackDsl SurfComponentBuilder.() -> Unit) {
    prompt(buildText(block))
}

@ResourcePackDsl
class ResourcePackRequestInfoBuilder {
    @PublishedApi
    internal val infos = mutableObjectListOf<ResourcePackInfo>()

    fun pack(block: @ResourcePackDsl ResourcePackInfo.Builder.() -> Unit) {
        infos.add(resourcePackInfo(block))
    }

    suspend fun asyncPack(block: @ResourcePackDsl suspend ResourcePackInfo.Builder.() -> Unit) {
        infos.add(asyncResourcePackInfo(block))
    }
}