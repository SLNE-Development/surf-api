package dev.slne.surf.api.spring.context.fixture.transitive

import dev.slne.surf.api.spring.context.fixture.Greeting
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

/**
 * Depends only on the consumer plugin, so the provider's export must stay out of reach.
 */
@Service
class TransitiveConsumer(val greeting: ObjectProvider<Greeting>)
