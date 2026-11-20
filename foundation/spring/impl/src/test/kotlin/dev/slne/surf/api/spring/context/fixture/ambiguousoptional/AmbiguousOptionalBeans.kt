package dev.slne.surf.api.spring.context.fixture.ambiguousoptional

import dev.slne.surf.api.spring.context.fixture.Greeting
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

/**
 * Asks for a greeting without insisting on one, while two providers offer it.
 */
@Service
class OptionalGreetingUser(val greeting: ObjectProvider<Greeting>)
