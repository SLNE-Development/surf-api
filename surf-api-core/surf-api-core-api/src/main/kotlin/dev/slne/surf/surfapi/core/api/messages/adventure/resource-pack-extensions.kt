package dev.slne.surf.surfapi.core.api.messages.adventure

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

/**
 * A DSL marker for the Resource Pack Info DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ResourcePackInfoDsl

/**
 * A DSL marker for the Resource Pack DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ResourcePackDsl

/**
 * Creates a [ResourcePackInfo] using the DSL-style builder.
 *
 * @param block The configuration block for building the resource pack info.
 * @return A configured [ResourcePackInfo] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val packInfo = resourcePackInfo {
 *     url("https://example.com/resourcepack.zip")
 * }
 * ```
 */
inline fun resourcePackInfo(block: @ResourcePackInfoDsl ResourcePackInfo.Builder.() -> Unit): ResourcePackInfo {
    return ResourcePackInfo.resourcePackInfo().apply(block).build()
}

/**
 * Asynchronously creates a [ResourcePackInfo] using the DSL-style builder and computes its hash.
 *
 * @param block The configuration block for building the resource pack info.
 * @return A computed [ResourcePackInfo] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val packInfo = asyncResourcePackInfo {
 *     url("https://example.com/resourcepack.zip")
 * }
 * ```
 */
suspend inline fun asyncResourcePackInfo(
    crossinline block: @ResourcePackInfoDsl suspend ResourcePackInfo.Builder.() -> Unit,
): ResourcePackInfo = coroutineScope {
    ResourcePackInfo.resourcePackInfo()
        .apply { block() }
        .computeHashAndBuild(Dispatchers.IO.asExecutor())
        .await()
}

/**
 * Sets the URL for the resource pack in the builder.
 *
 * @param url The URL of the resource pack.
 *
 * **Example Usage:**
 * ```kotlin
 * resourcePackInfo {
 *     url("https://example.com/resourcepack.zip")
 * }
 * ```
 */
fun ResourcePackInfo.Builder.url(url: String) {
    uri(URI.create(url))
}

/**
 * Creates a [ResourcePackRequest] using the DSL-style builder.
 *
 * @param block The configuration block for building the resource pack request.
 * @return A configured [ResourcePackRequest] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val request = resourcePackRequest {
 *     pack {
 *         url("https://example.com/resourcepack.zip")
 *     }
 * }
 * ```
 */
inline fun resourcePackRequest(block: @ResourcePackDsl ResourcePackRequest.Builder.() -> Unit): ResourcePackRequest {
    return ResourcePackRequest.resourcePackRequest().apply(block).build()
}

/**
 * Creates a [ResourcePackRequest] that adds multiple resource packs.
 *
 * @param infos A list of [ResourcePackInfo] instances to be included in the request.
 * @return A [ResourcePackRequest] adding the provided resource packs.
 * @throws IllegalArgumentException if no resource pack info is provided.
 *
 * **Example Usage:**
 * ```kotlin
 * val pack1 = resourcePackInfo { url("https://example.com/pack1.zip") }
 * val pack2 = resourcePackInfo { url("https://example.com/pack2.zip") }
 * val request = addingResourcePackRequest(pack1, pack2)
 * ```
 */
fun addingResourcePackRequest(vararg infos: ResourcePackInfo): ResourcePackRequest {
    require(infos.isNotEmpty()) { "At least one ResourcePackInfo must be provided" }
    return ResourcePackRequest.addingRequest(infos[0], *infos.drop(1).toTypedArray())
}

/**
 * Adds a single resource pack to the request using a builder.
 *
 * @param block The configuration block for the resource pack info.
 *
 * **Example Usage:**
 * ```kotlin
 * resourcePackRequest {
 *     pack {
 *         url("https://example.com/resourcepack.zip")
 *     }
 * }
 * ```
 */
inline fun ResourcePackRequest.Builder.pack(block: @ResourcePackDsl ResourcePackInfo.Builder.() -> Unit) {
    packs(resourcePackInfo(block))
}

/**
 * Adds multiple resource packs to the request using a builder.
 *
 * @param block The configuration block for defining multiple resource packs.
 *
 * **Example Usage:**
 * ```kotlin
 * resourcePackRequest {
 *     packs {
 *         pack { url("https://example.com/pack1.zip") }
 *         pack { url("https://example.com/pack2.zip") }
 *     }
 * }
 * ```
 */
inline fun ResourcePackRequest.Builder.packs(block: @ResourcePackDsl ResourcePackRequestInfoBuilder.() -> Unit) {
    packs(ResourcePackRequestInfoBuilder().apply(block).infos)
}

/**
 * Sets a simple callback for handling the success or failure of a resource pack request.
 *
 * @param block The callback function receiving the pack ID, audience, and success state.
 *
 * **Example Usage:**
 * ```kotlin
 * resourcePackRequest {
 *     simpleCallback { packId, audience, success ->
 *         if (success) {
 *             println("Resource pack $packId successfully applied to $audience.")
 *         } else {
 *             println("Failed to apply resource pack $packId to $audience.")
 *         }
 *     }
 * }
 * ```
 */
inline fun ResourcePackRequest.Builder.simpleCallback(crossinline block: @ResourcePackDsl (packId: UUID, audience: Audience, success: Boolean) -> Unit) {
    callback(
        ResourcePackCallback.onTerminal(
            { packId, audience -> block(packId, audience, true) },
            { packId, audience -> block(packId, audience, false) }
        )
    )
}

/**
 * Sets the user prompt message for accepting the resource pack.
 *
 * @param block The configuration block for creating the prompt component.
 *
 * **Example Usage:**
 * ```kotlin
 * resourcePackRequest {
 *     prompt { appendText("Would you like to apply this resource pack?") }
 * }
 * ```
 */
inline fun ResourcePackRequest.Builder.prompt(block: @ResourcePackDsl SurfComponentBuilder.() -> Unit) {
    prompt(buildText(block))
}

/**
 * A DSL builder for creating multiple [ResourcePackInfo] instances within a request.
 */
@ResourcePackDsl
class ResourcePackRequestInfoBuilder {
    @PublishedApi
    internal val infos = mutableObjectListOf<ResourcePackInfo>()


    /**
     * Adds a resource pack to the request.
     *
     * @param block The configuration block for the resource pack.
     *
     * **Example Usage:**
     * ```kotlin
     * packs {
     *     pack { url("https://example.com/pack1.zip") }
     *     pack { url("https://example.com/pack2.zip") }
     * }
     * ```
     */
    fun pack(block: @ResourcePackDsl ResourcePackInfo.Builder.() -> Unit) {
        infos.add(resourcePackInfo(block))
    }

    /**
     * Asynchronously adds a resource pack to the request.
     *
     * @param block The configuration block for the async resource pack.
     *
     * **Example Usage:**
     * ```kotlin
     * packs {
     *     asyncPack { url("https://example.com/pack1.zip") }
     * }
     * ```
     */
    suspend fun asyncPack(block: @ResourcePackDsl suspend ResourcePackInfo.Builder.() -> Unit) {
        infos.add(asyncResourcePackInfo(block))
    }
}