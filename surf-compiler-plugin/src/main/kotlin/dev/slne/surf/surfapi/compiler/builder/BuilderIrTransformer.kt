package dev.slne.surf.surfapi.compiler.builder

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.findIsInstanceAnd

@OptIn(UnsafeDuringIrConstructionAPI::class)
class BuilderIrTransformer(private val pluginContext: IrPluginContext) : IrElementTransformerVoidWithContext() {
    companion object {
        val GENERATE_BUILDER_FQ = FqName("dev.slne.surf.surfapi.shared.api.build.GenerateBuilder")
        val BUILDER_DSL_FQ = FqName("dev.slne.surf.surfapi.shared.api.build.SurfBuilderDsl")
    }

    private val irFactory: IrFactory get() = pluginContext.irFactory
    private val irBuiltIns get() = pluginContext.irBuiltIns

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (!declaration.hasAnnotation(GENERATE_BUILDER_FQ)) {
            return super.visitClassNew(declaration)
        }

        val primaryConstructor = declaration.primaryConstructor ?: return super.visitClassNew(declaration)

        val builderClass = generateBuilderClass(declaration, primaryConstructor)
        declaration.declarations.add(builderClass)

        val containingFile = declaration.file
        val dslFunction = generateDslFunction(declaration, builderClass)
        containingFile.declarations.add(dslFunction)

        return super.visitClassNew(declaration)
    }

    private fun generateBuilderClass(
        targetClass: IrClass,
        primaryConstructor: IrConstructor
    ): IrClass {
        // build the Builder class
        val builderClass = irFactory.buildClass {
            name = Name.identifier("Builder")
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }
        builderClass.parent = targetClass
        builderClass.createThisReceiverParameter()

        // Add @SurfBuilderDsl annotation
        addDslMarkerAnnotation(builderClass)

        // Generate no-arg constructor
        val builderConstructor = builderClass.addConstructor {
            isPrimary = true
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irDelegatingConstructorCall(irBuiltIns.anyClass.owner.constructors.first())
                +IrInstanceInitializerCallImpl(
                    startOffset, endOffset,
                    builderClass.symbol,
                    irBuiltIns.unitType
                )
            }
        }

        // For each constructor parameter, generate a backing property + setter
        val paramInfos = primaryConstructor.nonDispatchParameters
            .map { param ->
                ParameterInfo(
                    name = param.name,
                    type = param.type,
                    hasDefault = param.defaultValue != null,
                    isNullable = param.type.isNullable(),
                    isList = param.type.isListType(),
                    isSet = param.type.isSetType(),
                    isMap = param.type.isMapType(),
                    elementType = param.type.getCollectionElementType(),
                    keyType = param.type.getMapKeyType(),
                    valueType = param.type.getMapValueType(),
                    elementHasBuilder = param.type.getCollectionElementType()
                        ?.classOrNull?.owner?.hasAnnotation(GENERATE_BUILDER_FQ) == true,
                    indexInParameters = param.indexInParameters
                )
            }

        for (info in paramInfos) {
            when {
                info.isList -> generateListProperty(builderClass, info)
                info.isSet -> generateSetProperty(builderClass, info)
                info.isMap -> generateMapProperty(builderClass, info)
                else -> generateScalarProperty(builderClass, info)
            }
        }

        // Generate build() function
        generateBuildFunction(builderClass, targetClass, primaryConstructor, paramInfos)

        return builderClass
    }

    private fun generateScalarProperty(builderClass: IrClass, info: ParameterInfo) {
        // Private var _name: Type? = null
        val backingField = builderClass.addField {
            name = Name.identifier("_${info.name}")
            type = info.type.makeNullable()
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = false
        }.apply {
            initializer = irFactory.createExpressionBody(
                DeclarationIrBuilder(pluginContext, symbol).irNull()
            )
        }

        builderClass.addFunction {
            name = info.name
            returnType = builderClass.defaultType
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.apply {
            val dispatchParam = buildReceiverParameter {
                kind = IrParameterKind.DispatchReceiver
                type = builderClass.defaultType
            }.also { it.parent = this }

            val valueParam = buildValueParameter(this) {
                this.name = info.name
                this.type = info.type
                kind = IrParameterKind.Regular
            }

            parameters = listOf(dispatchParam, valueParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irSetField(
                    irGet(dispatchParam),
                    backingField,
                    irGet(valueParam)
                )
                +irReturn(irGet(dispatchParam))
            }
        }
    }

    private fun generateListProperty(builderClass: IrClass, info: ParameterInfo) {
        val elementType = info.elementType ?: return
        val mutableListType = irBuiltIns.mutableListClass.typeWith(elementType)

        // private val _name: MutableList<T> = mutableListOf()
        val backingField = builderClass.addField {
            name = Name.identifier("_${info.name.asString()}")
            type = mutableListType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
        }.apply {
            initializer = irFactory.createExpressionBody(
                DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(
                            CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("mutableListOf"))
                        ).first { it.owner.hasShape(regularParameters = 0) }
                    ).apply {
                        typeArguments[0] = elementType
                    }
                }
            )
        }

        // fun addName(value: T): Builder
        val singularName = info.name.asString()
            .removeSuffix("s")
            .removeSuffix("List")
            .removeSuffix("list")

        builderClass.addFunction {
            name = Name.identifier("add${singularName.replaceFirstChar { it.uppercase() }}")
            returnType = builderClass.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            val dispatchParam = buildReceiverParameter {
                kind = IrParameterKind.DispatchReceiver
                type = builderClass.defaultType
            }.also { it.parent = this }

            val valueParam = buildValueParameter(this) {
                this.name = Name.identifier("value")
                this.type = elementType
                kind = IrParameterKind.Regular
            }

            parameters = listOf(dispatchParam, valueParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val addFun = irBuiltIns.mutableListClass.functionByName("add")
                +irCall(addFun).apply {
                    // arguments[0] = dispatch (the MutableList), arguments[1] = element
                    arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }] =
                        irGetField(irGet(dispatchParam), backingField)
                    arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.Regular }] =
                        irGet(valueParam)
                }
                +irReturn(irGet(dispatchParam))
            }
        }

        // DSL block if element has @GenerateBuilder
        if (info.elementHasBuilder) {
            val elementClass = elementType.classOrNull?.owner ?: return
            val elementBuilderClass = elementClass.declarations
                .findIsInstanceAnd<IrClass> { it.name.asString() == "Builder" }

            // This won't exist yet if the element class hasn't been processed.
            // The transformer processes files top-down, so ordering matters.
            // We add a DSL function that creates a new builder, applies the block, and adds.
            builderClass.addFunction {
                name = Name.identifier(singularName)
                returnType = builderClass.defaultType
                visibility = DescriptorVisibilities.PUBLIC
                isInline = true
            }.apply {
                val dispatchParam = buildReceiverParameter {
                    kind = IrParameterKind.DispatchReceiver
                    type = builderClass.defaultType
                }.also { it.parent = this }

                // block: ElementBuilder.() -> Unit
                val lambdaType = irBuiltIns.functionN(1).typeWith(
                    elementBuilderClass?.defaultType ?: elementType,
                    irBuiltIns.unitType
                )
                val blockParam = buildValueParameter(this) {
                    this.name = Name.identifier("block")
                    this.type = lambdaType
                    kind = IrParameterKind.Regular
                }

                parameters = listOf(dispatchParam, blockParam)

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    // val builder = ElementBuilder()
                    // block(builder)
                    // _list.add(builder.build())
                    // return this
                    if (elementBuilderClass != null) {
                        val builderVar = irTemporary(
                            irCallConstructor(
                                elementBuilderClass.constructors.first().symbol,
                                emptyList()
                            )
                        )

                        val invokeFun = irBuiltIns.functionN(1).functions.first { it.name.asString() == "invoke" }
                        +irCall(invokeFun).apply {
                            val invokeDispatch =
                                invokeFun.parameters.first { it.kind == IrParameterKind.DispatchReceiver }
                            arguments[invokeDispatch.indexInParameters] = irGet(blockParam)
                            val invokeRegular = invokeFun.parameters.first { it.kind == IrParameterKind.Regular }
                            arguments[invokeRegular.indexInParameters] = irGet(builderVar)
                        }

                        // _list.add(builder.build())
                        val buildFun = elementBuilderClass.functions
                            .find { it.name.asString() == "build" }

                        if (buildFun != null) {
                            val addFun = irBuiltIns.mutableListClass.functionByName("add")
                            +irCall(addFun).apply {
                                arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }] =
                                    irGetField(irGet(dispatchParam), backingField)
                                arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.Regular }] =
                                    irCall(buildFun.symbol).apply {
                                        val buildDispatch =
                                            buildFun.parameters.first { it.kind == IrParameterKind.DispatchReceiver }
                                        arguments[buildDispatch.indexInParameters] = irGet(builderVar)
                                    }
                            }
                        }
                    }
                    +irReturn(irGet(dispatchParam))
                }
            }
        }
    }

    private fun generateSetProperty(builderClass: IrClass, info: ParameterInfo) {
        val elementType = info.elementType ?: return
        val mutableSetType = irBuiltIns.mutableSetClass.typeWith(elementType)

        val backingField = builderClass.addField {
            name = Name.identifier("_${info.name}")
            type = mutableSetType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
        }.apply {
            initializer = irFactory.createExpressionBody(
                DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(
                            CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("mutableSetOf"))
                        ).first { it.owner.hasShape(regularParameters = 0) }
                    ).apply {
                        typeArguments[0] = elementType
                    }
                }
            )
        }

        val singularName = info.name.asString().removeSuffix("s")
            .removeSuffix("Set")
            .removeSuffix("set")
        builderClass.addFunction {
            name = Name.identifier("add${singularName.replaceFirstChar { it.uppercase() }}")
            returnType = builderClass.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            val dispatchParam = buildReceiverParameter {
                kind = IrParameterKind.DispatchReceiver
                type = builderClass.defaultType
            }.also { it.parent = this }

            val valueParam = buildValueParameter(this) {
                this.name = Name.identifier("value")
                this.type = elementType
                kind = IrParameterKind.Regular
            }

            parameters = listOf(dispatchParam, valueParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val addFun = irBuiltIns.mutableSetClass.functionByName("add")
                +irCall(addFun).apply {
                    arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }] =
                        irGetField(irGet(dispatchParam), backingField)
                    arguments[addFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.Regular }] =
                        irGet(valueParam)
                }
                +irReturn(irGet(dispatchParam))
            }
        }
    }

    private fun generateMapProperty(builderClass: IrClass, info: ParameterInfo) {
        val keyType = info.keyType ?: return
        val valueType = info.valueType ?: return
        val mutableMapType = irBuiltIns.mutableMapClass.typeWith(keyType, valueType)

        val backingField = builderClass.addField {
            name = Name.identifier("_${info.name}")
            type = mutableMapType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
        }.apply {
            initializer = irFactory.createExpressionBody(
                DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(
                        pluginContext.referenceFunctions(
                            CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("mutableMapOf"))
                        ).first { it.owner.hasShape(regularParameters = 0) }
                    ).apply {
                        typeArguments[0] = keyType
                        typeArguments[1] = valueType
                    }
                }
            )
        }

        builderClass.addFunction {
            name = Name.identifier("put${info.name.asString().replaceFirstChar { it.uppercase() }}")
            returnType = builderClass.defaultType
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            val dispatchParam = buildReceiverParameter {
                kind = IrParameterKind.DispatchReceiver
                type = builderClass.defaultType
            }.also { it.parent = this }

            val keyParam = buildValueParameter(this) {
                this.name = Name.identifier("key")
                this.type = keyType
                kind = IrParameterKind.Regular
            }

            val valParam = buildValueParameter(this) {
                this.name = Name.identifier("value")
                this.type = valueType
                kind = IrParameterKind.Regular
            }

            parameters = listOf(dispatchParam, keyParam, valParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val putFun = irBuiltIns.mutableMapClass.functionByName("put")
                +irCall(putFun).apply {
                    arguments[putFun.owner.parameters.indexOfFirst { it.kind == IrParameterKind.DispatchReceiver }] =
                        irGetField(irGet(dispatchParam), backingField)
                    // put() has 2 regular params: key and value
                    val regularParams = putFun.owner.parameters.filter { it.kind == IrParameterKind.Regular }
                    arguments[regularParams[0].indexInParameters] = irGet(keyParam)
                    arguments[regularParams[1].indexInParameters] = irGet(valParam)
                }
                +irReturn(irGet(dispatchParam))
            }
        }
    }

    private fun generateBuildFunction(
        builderClass: IrClass,
        targetClass: IrClass,
        primaryConstructor: IrConstructor,
        paramInfos: List<ParameterInfo>
    ) {
        builderClass.addFunction {
            name = Name.identifier("build")
            returnType = targetClass.defaultType
            visibility = DescriptorVisibilities.INTERNAL
            modality = Modality.FINAL
        }.apply {
            val dispatchParam = buildReceiverParameter {
                kind = IrParameterKind.DispatchReceiver
                type = builderClass.defaultType
            }.also { it.parent = this }

            parameters = listOf(dispatchParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                // requireNotNull checks for required params
                for (info in paramInfos) {
                    if (info.isRequired) {
                        val field = builderClass.fields.first {
                            it.name.asString() == "_${info.name}"
                        }

                        // requireNotNull(_name) { "Required property 'name' must be set" }
                        val requireNotNull = pluginContext.referenceFunctions(
                            CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("requireNotNull"))
                        ).first { it.owner.hasShape(regularParameters = 2) }

                        +irCall(requireNotNull).apply {
                            typeArguments[0] = info.type
                            // arguments: requireNotNull has no dispatch receiver, only regular params
                            val regularParams =
                                requireNotNull.owner.parameters.filter { it.kind == IrParameterKind.Regular }
                            arguments[regularParams[0].indexInParameters] = irGetField(
                                irGet(dispatchParam), field
                            )
                            // Lambda for message
                            arguments[regularParams[1].indexInParameters] = irString(
                                "Required property '${info.name}' must be set in ${targetClass.name} builder"
                            )
                        }
                    }
                }

                // return TargetClass(param1 = _param1!!, param2 = _param2, ...)
                val constructorCall = IrConstructorCallImpl.fromSymbolOwner(
                    startOffset, endOffset,
                    targetClass.defaultType,
                    primaryConstructor.symbol
                )

                paramInfos.forEach { info ->
                    val field = builderClass.fields.first {
                        it.name.asString() == "_${info.name}"
                    }
                    val fieldGet = irGetField(irGet(dispatchParam), field)

                    val argument = when {
                        info.isRequired -> irImplicitCast(fieldGet, info.type) // _field!!
                        info.isList -> {
                            // _field.toList()
                            val toListFun = pluginContext.referenceFunctions(
                                CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("toList"))
                            ).first()
                            irCall(toListFun).apply {
                                // toList() is an extension function: extension receiver param
                                val extParam =
                                    toListFun.owner.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }
                                arguments[extParam.indexInParameters] = fieldGet
                            }
                        }

                        info.isSet -> {
                            val toSetFun = pluginContext.referenceFunctions(
                                CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("toSet"))
                            ).first()
                            irCall(toSetFun).apply {
                                val extParam =
                                    toSetFun.owner.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }
                                arguments[extParam.indexInParameters] = fieldGet
                            }
                        }

                        info.isMap -> {
                            val toMapFun = pluginContext.referenceFunctions(
                                CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier("toMap"))
                            ).first()
                            irCall(toMapFun).apply {
                                val extParam =
                                    toMapFun.owner.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }
                                arguments[extParam.indexInParameters] = fieldGet
                            }
                        }
                        // Optional scalar: _field ?: defaultValue
                        else -> fieldGet
                    }

                    // Use indexInParameters to place argument in the constructor call
                    constructorCall.arguments[info.indexInParameters] = argument
                }

                +irReturn(constructorCall)
            }
        }
    }

    private fun generateDslFunction(
        targetClass: IrClass,
        builderClass: IrClass
    ): IrSimpleFunction {
        return irFactory.buildFun {
            name = targetClass.name
            returnType = targetClass.defaultType
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            isInline = true
        }.apply {
            parent = targetClass.file

            val lambdaType = irBuiltIns.functionN(1).typeWith(
                builderClass.defaultType,
                irBuiltIns.unitType
            )
            val blockParam = buildValueParameter(this) {
                this.name = Name.identifier("block")
                this.type = lambdaType
                kind = IrParameterKind.Regular
            }

            parameters = listOf(blockParam)

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                // val builder = Builder()
                val builderVar = irTemporary(
                    irCallConstructor(builderClass.constructors.first().symbol, emptyList())
                )

                // block(builder) — invoke the lambda
                val invokeFun = lambdaType.classOrNull!!.owner.functions.first { it.name.asString() == "invoke" }
                +irCall(invokeFun).apply {
                    // invoke() has dispatch receiver (the Function object) + regular param (the Builder)
                    val invokeDispatch = invokeFun.parameters.first { it.kind == IrParameterKind.DispatchReceiver }
                    arguments[invokeDispatch.indexInParameters] = irGet(blockParam)
                    val invokeRegular = invokeFun.parameters.first { it.kind == IrParameterKind.Regular }
                    arguments[invokeRegular.indexInParameters] = irGet(builderVar)
                }

                // return builder.build()
                val buildFun = builderClass.functions.first { it.name.asString() == "build" }
                +irReturn(irCall(buildFun).apply {
                    val buildDispatch = buildFun.parameters.first { it.kind == IrParameterKind.DispatchReceiver }
                    arguments[buildDispatch.indexInParameters] = irGet(builderVar)
                })
            }
        }
    }

    private fun addDslMarkerAnnotation(builderClass: IrClass) {
        val dslAnnotationClass = pluginContext.referenceClass(
            ClassId.topLevel(BUILDER_DSL_FQ)
        ) ?: return

        val builder = DeclarationIrBuilder(pluginContext, builderClass.symbol)
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(
            startOffset = builder.startOffset,
            endOffset = builder.endOffset,
            type = dslAnnotationClass.defaultType,
            constructorSymbol = dslAnnotationClass.owner.constructors.first().symbol
        )

        builderClass.annotations += constructorCall
    }

    private fun IrType.isListType(): Boolean {
        return classOrNull?.isSubtypeOfClass(irBuiltIns.listClass) ?: false
    }

    private fun IrType.isSetType(): Boolean {
        return classOrNull?.isSubtypeOfClass(irBuiltIns.setClass) ?: false
    }

    private fun IrType.isMapType(): Boolean {
        return classOrNull?.isSubtypeOfClass(irBuiltIns.mapClass) ?: false
    }

    private fun IrType.getCollectionElementType(): IrType? {
        if (!isListType() && !isSetType()) return null
        return (this as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull
    }

    private fun IrType.getMapKeyType(): IrType? {
        if (!isMapType()) return null
        return (this as? IrSimpleType)?.arguments?.getOrNull(0)?.typeOrNull
    }

    private fun IrType.getMapValueType(): IrType? {
        if (!isMapType()) return null
        return (this as? IrSimpleType)?.arguments?.getOrNull(1)?.typeOrNull
    }

    data class ParameterInfo(
        val name: Name,
        val type: IrType,
        val hasDefault: Boolean,
        val isNullable: Boolean,
        val isList: Boolean,
        val isSet: Boolean,
        val isMap: Boolean,
        val elementType: IrType? = null,
        val keyType: IrType? = null,
        val valueType: IrType? = null,
        val elementHasBuilder: Boolean = false,
        val indexInParameters: Int = -1
    ) {
        val isRequired: Boolean
            get() = !isNullable && !hasDefault && !isList && !isSet && !isMap
    }
}