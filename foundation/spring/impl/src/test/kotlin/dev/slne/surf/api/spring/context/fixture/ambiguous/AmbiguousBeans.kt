package dev.slne.surf.api.spring.context.fixture.ambiguous

import dev.slne.surf.api.spring.context.fixture.Greeting
import org.springframework.stereotype.Service

/**
 * The parameter is named like a provider bean on purpose; the name must not resolve it.
 */
@Service
class AmbiguousConsumer(val providerGreeting: Greeting)
