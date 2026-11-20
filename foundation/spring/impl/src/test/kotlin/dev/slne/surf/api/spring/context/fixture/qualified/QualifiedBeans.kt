package dev.slne.surf.api.spring.context.fixture.qualified

import dev.slne.surf.api.spring.context.fixture.Greeting
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class QualifiedConsumer(@param:Qualifier("second") val greeting: Greeting)
