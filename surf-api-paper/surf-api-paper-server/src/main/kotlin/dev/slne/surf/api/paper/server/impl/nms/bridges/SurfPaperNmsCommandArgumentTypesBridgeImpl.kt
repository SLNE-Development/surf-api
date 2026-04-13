package dev.slne.surf.api.paper.server.impl.nms.bridges

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommandArgumentTypesBridge
import dev.slne.surf.api.paper.server.nms.AdventureNBT
import dev.slne.surf.api.paper.server.reflection.Reflection
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.matcher.ElementMatchers
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minecraft.commands.arguments.CompoundTagArgument
import java.lang.reflect.InvocationHandler
import java.util.concurrent.ConcurrentHashMap

@NmsUseWithCaution
class SurfPaperNmsCommandArgumentTypesBridgeImpl : SurfPaperNmsCommandArgumentTypesBridge {
    init {
    }

    override fun compoundTag(): ArgumentType<*> {
        return CompoundTagArgument.compoundTag()
    }

    override fun getCompoundTag(ctx: CommandContext<*>, key: String): CompoundBinaryTag {
        val nms = CompoundTagArgument.getCompoundTag(ctx, key)
        return AdventureNBT.fromNms(nms)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <B : Any, C : Any> wrap(
        base: ArgumentType<B>,
        converter: OpenedResultConverter<B, C>
    ): ArgumentType<C> {
        val wrappedConverter = OpenedResultConverterImpl.of(converter)
        val wrapped = Reflection.VANILLA_ARGUMENT_PROVIDER_IMPL_PROXY.wrap(
            Reflection.VANILLA_ARGUMENT_PROVIDER_PROXY.provider(),
            base,
            wrappedConverter
        ) as ArgumentType<C>

        return wrapped
    }


    fun interface OpenedResultConverter<T, R> {
        @Throws(CommandSyntaxException::class)
        fun convert(type: T): R
    }

    object OpenedResultConverterImpl {
        private val converterCache = ConcurrentHashMap<String, Any>()

        @Suppress("UNCHECKED_CAST")
        fun <B : Any, C : Any> of(converter: OpenedResultConverter<B, C>): Any {
            val cacheKey = "${converter.javaClass.name}_${System.identityHashCode(converter)}"

            return converterCache.computeIfAbsent(cacheKey) {
                createConverter(converter)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <B : Any, C : Any> createConverter(converter: OpenedResultConverter<B, C>): Any {
            val resultConverterInterface = Class.forName(
                "io.papermc.paper.command.brigadier.argument.VanillaArgumentProviderImpl\$ResultConverter"
            )

            val handler = InvocationHandler { _, method, args ->
                when (method.name) {
                    "convert" -> converter.convert(args[0] as B)
                    else -> throw UnsupportedOperationException("Unknown method: ${method.name}")
                }
            }

            val dynamicType = ByteBuddy()
                .with(TypeValidation.DISABLED)
                .subclass(Any::class.java)
                .implement(resultConverterInterface)
                .name("io.papermc.paper.command.brigadier.argument.GeneratedResultConverter\$${System.nanoTime()}")
                .method(ElementMatchers.any())
                .intercept(InvocationHandlerAdapter.of(handler))
                .make()

            val loadedClass = dynamicType.load(
                resultConverterInterface.classLoader,
                ClassLoadingStrategy.Default.INJECTION
            ).loaded

            return loadedClass.getDeclaredConstructor().newInstance()
        }


        class InterceptorHolder<B : Any, C : Any>(
            private val converter: OpenedResultConverter<B, C>
        ) {
            @RuntimeType
            @Throws(Exception::class)
            fun convert(@RuntimeType input: B): C {
                return converter.convert(input)
            }
        }
    }
}