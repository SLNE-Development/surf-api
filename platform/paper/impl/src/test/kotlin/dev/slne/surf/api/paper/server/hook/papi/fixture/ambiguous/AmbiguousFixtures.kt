package dev.slne.surf.api.paper.server.hook.papi.fixture.ambiguous

import dev.slne.surf.api.paper.hook.papi.PapiExpansion
import org.springframework.stereotype.Component

@PapiExpansion("ambiguous", placeholders = [SharedPlaceholders::class])
class AmbiguousExpansion

interface SharedPlaceholders

@Component
class FirstPlaceholders : SharedPlaceholders

@Component
class SecondPlaceholders : SharedPlaceholders
