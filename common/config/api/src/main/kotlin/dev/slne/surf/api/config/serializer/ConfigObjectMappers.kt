package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.config.constraints.*
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.spongepowered.configurate.kotlin.dataClassFieldDiscoverer
import org.spongepowered.configurate.kotlin.extensions.addConstraint
import org.spongepowered.configurate.objectmapping.ObjectMapper

/**
 * Creates the object mapper factory of config files, with Kotlin data class support and surf-api's
 * constraint annotations.
 *
 * It lives next to the constraint factories, which are internal to this module.
 */
@InternalSurfApi
object ConfigObjectMappers {

    /**
     * Returns a new factory.
     *
     * A factory caches a mapper per type, so each set of files gets its own; a shared one would
     * keep the classes of closed plugins alive.
     */
    fun createFactory(): ObjectMapper.Factory {
        return ObjectMapper.factoryBuilder()
            .addDiscoverer(dataClassFieldDiscoverer())
            .addConstraint(PositiveNumber.Companion.Factory)
            .addConstraint(PositiveNumber.Companion.FactoryIntOr)
            .addConstraint(PositiveNumber.Companion.FactoryDoubleOr)
            .addConstraint(NegativeNumber.Companion.Factory)
            .addConstraint(NegativeNumber.Companion.FactoryIntOr)
            .addConstraint(NegativeNumber.Companion.FactoryDoubleOr)
            .addConstraint(MinNumber.Companion.Factory)
            .addConstraint(MinNumber.Companion.FactoryIntOr)
            .addConstraint(MinNumber.Companion.FactoryDoubleOr)
            .addConstraint(MaxNumber.Companion.Factory)
            .addConstraint(MaxNumber.Companion.FactoryIntOr)
            .addConstraint(MaxNumber.Companion.FactoryDoubleOr)
            .addConstraint(NotBlank.Companion.Factory)
            .addConstraint(Trimmed.Companion.Factory)
            .addConstraint(MaxLength.Companion.Factory)
            .addConstraint(MaxLength.Companion.FactoryStringOrDefault)
            .addConstraint(MinLength.Companion.Factory)
            .addConstraint(MinLength.Companion.FactoryStringOrDefault)
            .addConstraint(StartsWith.Companion.Factory)
            .addConstraint(StartsWith.Companion.FactoryStringOrDefault)
            .addConstraint(EndsWith.Companion.Factory)
            .addConstraint(EndsWith.Companion.FactoryStringOrDefault)
            .addConstraint(Contains.Companion.Factory)
            .addConstraint(Contains.Companion.FactoryStringOrDefault)
            .addConstraint(Range.Companion.Factory)
            .addConstraint(Range.Companion.FactoryIntOr)
            .addConstraint(Range.Companion.FactoryDoubleOr)
            .addConstraint(MinSize.Companion.Factory)
            .addConstraint(MaxSize.Companion.Factory)
            .addConstraint(NotEmpty.Companion.Factory)
            .addConstraint(NoDuplicates.Companion.Factory)
            .addConstraint(MinDuration.Companion.Factory)
            .addConstraint(MinDuration.Companion.FactoryDurationOrDisabled)
            .addConstraint(MaxDuration.Companion.Factory)
            .addConstraint(MaxDuration.Companion.FactoryDurationOrDisabled)
            .addConstraint(DisallowValues.Companion.Factory)
            .addConstraint(Namespace.Companion.Factory)
            .addConstraint(ExistingFile.Companion.Factory)
            .addConstraint(Directory.Companion.Factory)
            .addConstraint(WritablePath.Companion.Factory)
            .build()
    }
}
