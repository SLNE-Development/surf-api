package dev.slne.surf.surfapi.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TestFieldFirGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object Key : GeneratedDeclarationKey() {
        private val TEST_NAME = Name.identifier("test")
        val GENERATE_BUILDER_FQ = FqName("dev.slne.surf.surfapi.shared.api.build.GenerateBuilder")

        // Verwende DeclarationPredicate statt LookupPredicate
        private val PREDICATE = DeclarationPredicate.create {
            annotated(GENERATE_BUILDER_FQ)
        }

        private val HAS_PREDICATE = DeclarationPredicate.create {
            hasAnnotated(GENERATE_BUILDER_FQ)
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
        register(HAS_PREDICATE)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        // Verwende matches() statt lazy-geladene Liste
        if (!session.predicateBasedProvider.matches(HAS_PREDICATE, classSymbol)) {
            return emptySet()
        }
        return setOf(TEST_NAME)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        if (callableId.callableName != TEST_NAME) return emptyList()
        val owner = context?.owner ?: return emptyList()

        val property = createMemberProperty(
            owner,
            Key,
            TEST_NAME,
            returnType = session.builtinTypes.stringType.coneType.withNullability(
                true, session.typeContext
            ),
            hasBackingField = true,
            isVal = false
        )

        return listOf(property.symbol)
    }
}