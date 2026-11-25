package dev.slne.surf.api.spring.context.fixture.boot.autoconfig.reverse

import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.EarlierAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.FirstTickerAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.LaterAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.SecondTickerAutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(
    classes = [
        SecondTickerAutoConfiguration::class,
        FirstTickerAutoConfiguration::class,
        LaterAutoConfiguration::class,
        EarlierAutoConfiguration::class,
    ],
)
class ReverseImport
