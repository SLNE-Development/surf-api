package dev.slne.surf.api.spring.context.fixture.boot.autoconfig.forward

import dev.slne.surf.api.spring.context.fixture.boot.Named
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.EarlierAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.FirstTickerAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.LaterAutoConfiguration
import dev.slne.surf.api.spring.context.fixture.boot.autoconfig.SecondTickerAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ImportAutoConfiguration(
    classes = [
        EarlierAutoConfiguration::class,
        LaterAutoConfiguration::class,
        FirstTickerAutoConfiguration::class,
        SecondTickerAutoConfiguration::class,
    ],
)
class ForwardImport

/**
 * Lies in the scanned package, but only an import would apply it.
 */
@AutoConfiguration
class StrayAutoConfiguration {

    @Bean
    fun strayNamed(): Named = Named("stray")
}
