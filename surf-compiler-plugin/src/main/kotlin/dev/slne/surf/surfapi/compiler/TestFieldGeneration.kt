package dev.slne.surf.surfapi.compiler

import dev.slne.surf.surfapi.compiler.builder.BuilderIrTransformer.Companion.GENERATE_BUILDER_FQ
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.Name

class TestFieldGeneration : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitClassNew(declaration: IrClass): IrStatement {
                if (!declaration.hasAnnotation(GENERATE_BUILDER_FQ)) {
                    return super.visitClassNew(declaration)
                }

                // add a dummy test field
                declaration.addField {
                    name = Name.identifier("test")
                    type = pluginContext.irBuiltIns.stringType.makeNullable()
                    isFinal = false
                }.apply {
                    initializer = pluginContext.irFactory.createExpressionBody(
                        DeclarationIrBuilder(pluginContext, symbol).irNull()
                    )
                }

                return super.visitClassNew(declaration)
            }
        }, null)
    }
}