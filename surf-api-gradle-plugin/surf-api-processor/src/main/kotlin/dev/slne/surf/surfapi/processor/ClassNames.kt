package dev.slne.surf.surfapi.processor

import dev.slne.surf.surfapi.processor.util.nameOf
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.Priority
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentPostProcessor
import dev.slne.surf.surfapi.shared.api.component.requirement.*

object ClassNames {
    val COMPONENT_META = nameOf<ComponentMeta>()
    val PRIORITY = nameOf<Priority>()
    val DEPENDS_ON_CLASS = nameOf<DependsOnClass>()
    val DEPENDS_ON_CLASS_NAME = nameOf<DependsOnClassName>()
    val DEPENDS_ON_ONE_PLUGIN = nameOf<DependsOnOnePlugin>()
    val DEPENDS_ON_PLUGIN = nameOf<DependsOnPlugin>()
    val DEPENDS_ON_COMPONENT = nameOf<DependsOnComponent>()
    val CONDITIONAL_ON = nameOf<ConditionalOn>()
    val COMPONENT_POST_PROCESSOR = nameOf<ComponentPostProcessor>()
}