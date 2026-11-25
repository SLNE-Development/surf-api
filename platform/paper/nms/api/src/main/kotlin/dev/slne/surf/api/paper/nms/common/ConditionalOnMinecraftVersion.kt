package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.shared.internal.nms.NmsVersion
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Registers the annotated configuration only if [value] is the NMS version selected for the server.
 *
 * Each NMS version module declares one `@Configuration` carrying this annotation. Its
 * `@ComponentScan` picks up the module's [NmsComponent] classes, so the beans of every other
 * version module are never registered or loaded.
 *
 * The condition reads the [NmsVersion] bean named [NmsVersionResolver.BEAN_NAME]; the Paper runtime
 * registers it before the root context scans.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Conditional(OnMinecraftVersionCondition::class)
annotation class ConditionalOnMinecraftVersion(val value: NmsVersion)

/**
 * Evaluates [ConditionalOnMinecraftVersion] against the selected [NmsVersion].
 *
 * Spring instantiates it reflectively; nothing else should.
 */
class OnMinecraftVersionCondition internal constructor() : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val annotation = metadata.annotations.get(ConditionalOnMinecraftVersion::class.java)
        if (!annotation.isPresent) return true

        val required = annotation.getEnum("value", NmsVersion::class.java)

        return required == selectedVersion(context)
    }

    private fun selectedVersion(context: ConditionContext): NmsVersion {
        val beanFactory = checkNotNull(context.beanFactory) {
            "@ConditionalOnMinecraftVersion needs a bean factory to find the selected NMS version"
        }

        check(beanFactory.containsBean(NmsVersionResolver.BEAN_NAME)) {
            "@ConditionalOnMinecraftVersion was evaluated in a context without the " +
                "'${NmsVersionResolver.BEAN_NAME}' bean; the Paper runtime registers it"
        }

        return beanFactory.getBean<NmsVersion>(NmsVersionResolver.BEAN_NAME)
    }
}
