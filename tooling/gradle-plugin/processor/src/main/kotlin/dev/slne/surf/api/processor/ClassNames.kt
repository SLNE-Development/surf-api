package dev.slne.surf.api.processor

import dev.slne.surf.api.processor.util.nameOf
import dev.slne.surf.api.processor.util.shortNameOf
import dev.slne.surf.api.shared.api.reflection.*
import dev.slne.surf.api.shared.internal.nms.NmsProviderMarker

object ClassNames {
    val NMS_PROVIDER = nameOf<NmsProviderMarker>()

    val GENERATE_REFLECTION = nameOf<GenerateReflection>()
    val UNSPECIFIED_REFLECTION_TARGET = nameOf<UnspecifiedReflectionTarget>()
    val REFLECTED_METHOD = nameOf<ReflectedMethod>()
    val REFLECTED_CONSTRUCTOR = nameOf<ReflectedConstructor>()
    val REFLECTED_FIELD = nameOf<ReflectedField>()
    val REFLECTED_VAR_HANDLE = nameOf<ReflectedVarHandle>()
    val CONSTANT_INT_ARGUMENT = nameOf<ConstantIntArgument>()
    val CONSTANT_LONG_ARGUMENT = nameOf<ConstantLongArgument>()
    val CONSTANT_BOOLEAN_ARGUMENT = nameOf<ConstantBooleanArgument>()
    val CONSTANT_STRING_ARGUMENT = nameOf<ConstantStringArgument>()
}

object ShortClassNames {
    val GENERATE_REFLECTION = shortNameOf<GenerateReflection>()
    val CONSTANT_INT_ARGUMENT = shortNameOf<ConstantIntArgument>()
    val CONSTANT_LONG_ARGUMENT = shortNameOf<ConstantLongArgument>()
    val CONSTANT_BOOLEAN_ARGUMENT = shortNameOf<ConstantBooleanArgument>()
    val CONSTANT_STRING_ARGUMENT = shortNameOf<ConstantStringArgument>()
}
