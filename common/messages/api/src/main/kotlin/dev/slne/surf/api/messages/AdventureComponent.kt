package dev.slne.surf.api.messages

import net.kyori.adventure.text.Component

/**
 * Adventure's text [Component] under a name that does not clash with Spring's `@Component`.
 *
 * `AdventureComponent` is the same type as [Component], so a file can use both without an import
 * alias. [dev.slne.surf.api.spring.SpringComponent] covers the other side.
 */
typealias AdventureComponent = Component
