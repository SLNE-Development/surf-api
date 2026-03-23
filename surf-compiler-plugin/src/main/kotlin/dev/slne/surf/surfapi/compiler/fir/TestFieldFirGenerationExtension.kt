package dev.slne.surf.surfapi.compiler.fir

import dev.slne.surf.surfapi.compiler.builder.BuilderIrTransformer.Companion.GENERATE_BUILDER_FQ
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

class TestFieldFirGenerationExtension(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object Key : GeneratedDeclarationKey() {
        private val TEST_NAME = Name.identifier("test")
        private val PREDICATE = LookupPredicate.create {
            annotated(GENERATE_BUILDER_FQ)
        }
    }

    private val matchedClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        if (classSymbol in matchedClasses) {
            return setOf(TEST_NAME)
        }
        return emptySet()
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (owner !in matchedClasses) return emptyList()
        if (callableId.callableName != TEST_NAME) return emptyList()

        val property = createMemberProperty(
            owner,
            Key,
            TEST_NAME,
            returnType = session.builtinTypes.stringType.coneType.withNullability(true, session.typeContext),
            hasBackingField = true,
            isVal = false
        )

        return listOf(property.symbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}