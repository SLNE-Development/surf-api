package dev.slne.surf.api.processor

import dev.slne.surf.api.processor.util.nameOf
import dev.slne.surf.api.shared.api.component.Priority
import dev.slne.surf.api.shared.api.component.SurfComponentMeta
import dev.slne.surf.api.shared.api.component.processor.ComponentPostProcessor
import dev.slne.surf.api.shared.api.component.requirement.*

object ClassNames {
    val COMPONENT_META = nameOf<SurfComponentMeta>()
    val PRIORITY = nameOf<Priority>()
    val DEPENDS_ON_CLASS = nameOf<DependsOnClass>()
    val DEPENDS_ON_CLASS_NAME = nameOf<DependsOnClassName>()
    val DEPENDS_ON_ONE_PLUGIN = nameOf<DependsOnOnePlugin>()
    val DEPENDS_ON_PLUGIN = nameOf<DependsOnPlugin>()
    val DEPENDS_ON_COMPONENT = nameOf<DependsOnComponent>()
    val CONDITIONAL_ON = nameOf<ConditionalOn>()
    val CONDITIONAL_ON_ENVIRONMENT = nameOf<ConditionalOnEnvironment>()
    val CONDITIONAL_ON_MISSING_COMPONENT = nameOf<ConditionalOnMissingComponent>()
    val CONDITIONAL_ON_PROPERTY = nameOf<ConditionalOnProperty>()
    val COMPONENT_POST_PROCESSOR = nameOf<ComponentPostProcessor>()
}