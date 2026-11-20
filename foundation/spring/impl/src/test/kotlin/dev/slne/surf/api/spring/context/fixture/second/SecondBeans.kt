package dev.slne.surf.api.spring.context.fixture.second

import dev.slne.surf.api.spring.context.fixture.Greeting
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

/**
 * A second provider of [Greeting]; `@Primary` must not win across providers.
 */
@Service
@Primary
@Qualifier("second")
class SecondGreeting : Greeting {
    override fun greet() = "hello from second"
}
