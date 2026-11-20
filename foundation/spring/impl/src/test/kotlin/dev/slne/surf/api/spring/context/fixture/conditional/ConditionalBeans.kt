package dev.slne.surf.api.spring.context.fixture.conditional

import dev.slne.surf.api.spring.ConditionalOnPlugin
import dev.slne.surf.api.spring.context.fixture.Optional
import org.springframework.stereotype.Service

@Service
@ConditionalOnPlugin("Missing")
class MissingAdapter : Optional {
    override fun answer() = "missing"
}

@Service
@ConditionalOnPlugin("Present")
class PresentAdapter : Optional {
    override fun answer() = "present"
}

@Service
class OptionalUser(val optional: Optional?)
