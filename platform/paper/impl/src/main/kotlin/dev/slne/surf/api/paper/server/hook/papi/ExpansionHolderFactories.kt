package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.spring.ConditionalOnPlugin
import org.springframework.context.annotation.Fallback
import org.springframework.stereotype.Component

/**
 * Creates holders that register their expansion with PlaceholderAPI.
 *
 * Present only while PlaceholderAPI is installed; PlaceholderAPI's types load on the first
 * [create].
 */
@Component
@ConditionalOnPlugin("PlaceholderAPI")
internal class PapiExpansionHolderFactory : ExpansionHolderFactory {

    override fun create(definition: ExpansionDefinition): ExpansionHolder {
        return PlaceholderApiExpansion(definition)
    }
}

/**
 * Creates holders that do nothing, for a server without PlaceholderAPI.
 */
@Component
@Fallback
internal class NoopExpansionHolderFactory : ExpansionHolderFactory {

    override fun create(definition: ExpansionDefinition): ExpansionHolder = NoopHolder

    private object NoopHolder : ExpansionHolder {
        override fun attach() {}

        override fun detach() {}
    }
}
