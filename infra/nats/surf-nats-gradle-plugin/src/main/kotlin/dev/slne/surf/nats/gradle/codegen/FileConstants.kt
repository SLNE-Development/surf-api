package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import dev.slne.surf.nats.protocol.ProtocolNaming

/**
 * Collects the file-private constants that the method descriptors of one generated file share.
 *
 * Descriptor lambdas return these instead of allocating per call: [noArguments] for rpcs without parameters and
 * [instance] for fieldless request messages, whose generated `equals` compares by type only.
 */
internal class FileConstants {
    private var noArguments: PropertySpec? = null
    private val instances = LinkedHashMap<ClassName, PropertySpec>()
    private val instanceNames = HashSet<String>()

    /**
     * `NO_ARGUMENTS` first, then the instances in first-use order.
     */
    val properties: List<PropertySpec> get() = listOfNotNull(noArguments) + instances.values

    fun noArguments(): PropertySpec {
        return noArguments ?: PropertySpec.builder(NO_ARGUMENTS, NULLABLE_ARRAY, KModifier.PRIVATE)
            .initializer("emptyArray()")
            .build()
            .also { noArguments = it }
    }

    fun instance(className: ClassName): PropertySpec = instances.getOrPut(className) {
        val base = className.simpleNames.joinToString("_") { ProtocolNaming.kebabCase(it).uppercase().replace('-', '_') } + "_INSTANCE"
        var name = base
        var suffix = 2
        while (!instanceNames.add(name)) name = "${base}_${suffix++}"
        PropertySpec.builder(name, className, KModifier.PRIVATE).initializer("%T()", className).build()
    }

    private companion object {
        const val NO_ARGUMENTS = "NO_ARGUMENTS"
        val NULLABLE_ARRAY = ARRAY.parameterizedBy(ANY.copy(nullable = true))
    }
}
