package dev.slne.surf.api.spring.context.fixture.boot.classes

import dev.slne.surf.api.spring.context.fixture.boot.HIDDEN_TYPE
import dev.slne.surf.api.spring.context.fixture.boot.Named
import org.springframework.boot.autoconfigure.condition.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.stereotype.Service

@Service
@ConditionalOnClass(name = [HIDDEN_TYPE])
class WithHiddenType

@Service
@ConditionalOnMissingClass(HIDDEN_TYPE)
class WithoutHiddenType

@Configuration
class ClassConditionConfiguration {

    @Bean
    @ConditionalOnClass(name = [HIDDEN_TYPE])
    fun hiddenTypeBean(): Named = Named("hidden type present")

    @Bean
    @ConditionalOnMissingClass(HIDDEN_TYPE)
    fun noHiddenTypeBean(): Named = Named("hidden type missing")
}

/**
 * Matches when the condition runs with the plugin's class loader.
 */
class PluginLoaderCondition : SpringBootCondition() {

    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome {
        val loaderName = context.classLoader?.name

        return if (loaderName == PLUGIN_LOADER) {
            ConditionOutcome.match("runs with the plugin's class loader")
        } else {
            ConditionOutcome.noMatch("runs with class loader '$loaderName'")
        }
    }

    companion object {
        const val PLUGIN_LOADER = "boot-plugin-loader"
    }
}

@Service
@Conditional(PluginLoaderCondition::class)
class SeesPluginLoader

/**
 * Matches when the hidden type is present or the feature property is set.
 */
class HiddenTypeOrFeature : AnyNestedCondition(ConfigurationPhase.REGISTER_BEAN) {

    @ConditionalOnClass(name = [HIDDEN_TYPE])
    class HiddenTypePresent

    @ConditionalOnProperty("surf.test.boot.nested")
    class NestedFeature
}

@Service
@Conditional(HiddenTypeOrFeature::class)
class NestedConditionBean
