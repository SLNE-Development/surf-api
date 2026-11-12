package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.ClassName
import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import dev.slne.surf.nats.gradle.generator.DefaultTypeMappings
import dev.slne.surf.nats.gradle.generator.GeneratorHarness
import dev.slne.surf.nats.gradle.generator.TypeMapping
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.MappedClass
import dev.slne.surf.nats.gradle.schema.loadSchema
import dev.slne.surf.nats.gradle.validate.Problems
import java.io.File

/**
 * Writes `.proto` sources into [root] and resolves their contract with [ContractResolver].
 *
 * The sources are resolved on the first lookup and must be free of problems. Declarations are looked up by their
 * proto full name.
 */
internal class ContractHarness(root: File, private val mappings: List<TypeMapping> = emptyList()) {
    private val sources = GeneratorHarness(root)
    private val loaded by lazy { loadSchema(listOf(sourceDir), emptyList()) }
    private val contract by lazy(::resolve)

    val sourceDir: File get() = sources.sourceDir

    fun proto(path: String, content: String): ContractHarness {
        sources.proto(path, content)
        return this
    }

    fun members(message: String): List<MessageMember> =
        contract.members(loaded.schema.getType(message) as MessageType)

    fun entries(enumType: String): Map<String, EnumConstant> =
        contract.entries(loaded.schema.getType(enumType) as EnumType)

    fun methods(service: String): List<ContractMethod> =
        contract.methods(checkNotNull(loaded.schema.getService(service)))

    private fun resolve(): ResolvedContract {
        val problems = Problems()
        val mapped = (DefaultTypeMappings.common + mappings).associate {
            it.protoType to MappedClass(ClassName.bestGuess(it.kotlinClass), ClassName.bestGuess(it.serializerClass))
        }
        val types = KotlinTypeResolver(loaded.schema, loaded.sourceFiles, mapped, problems)
        val contract = ContractResolver(types).resolve(loaded.sourceFiles)

        problems.throwIfAny()
        return contract
    }
}
