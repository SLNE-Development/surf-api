package dev.slne.surf.api.spring

import org.springframework.stereotype.Component

/**
 * Spring's [Component] stereotype under a name that does not clash with Adventure's text component.
 *
 * `@SpringComponent` registers a bean exactly like `@Component`, so a file can use both without an
 * import alias. `dev.slne.surf.api.messages.AdventureComponent` covers the other side.
 */
typealias SpringComponent = Component
