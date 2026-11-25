package dev.slne.surf.testing.fixture

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.config.SurfConfig
import dev.slne.surf.api.spring.ConditionalOnPlugin
import kotlinx.coroutines.CoroutineScope
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.CountDownLatch

// Lives outside dev.slne.surf.api, so surf-api's root scan never registers these classes.

@ConfigSerializable
@SurfConfig("runner.yml")
data class RunnerConfig(val greeting: String = "hello")

class ConfigReader(val handle: ConfigHandle<RunnerConfig>, val scope: CoroutineScope)

class OptionalLibrary

const val OPTIONAL_LIBRARY = "dev.slne.surf.testing.fixture.OptionalLibrary"

data class Variant(val name: String)

@Configuration
class RunnerConfiguration {

    @Bean
    fun configReader(
        handle: ConfigHandle<RunnerConfig>,
        scope: CoroutineScope,
    ): ConfigReader = ConfigReader(handle, scope)
}

@Configuration
class VariantConfiguration {

    @Bean
    @ConditionalOnClass(name = [OPTIONAL_LIBRARY])
    fun libraryVariant(): Variant = Variant("library")

    @Bean
    @ConditionalOnMissingClass(OPTIONAL_LIBRARY)
    fun plainVariant(): Variant = Variant("plain")

    @Bean
    @ConditionalOnPlugin("LuckPerms")
    fun luckPermsVariant(): Variant = Variant("luckperms")
}

class ScheduledCounter {
    val runs = CountDownLatch(2)

    @Scheduled(fixedDelay = 10)
    suspend fun count() {
        runs.countDown()
    }
}
