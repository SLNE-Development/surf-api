package dev.slne.surf.api.gen.generator.types

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import dev.slne.surf.api.gen.data.GenericRegistry
import dev.slne.surf.api.gen.generator.SimpleGenerator
import dev.slne.surf.api.gen.generator.utils.Annotations
import dev.slne.surf.api.gen.generator.utils.Formatting
import dev.slne.surf.api.gen.generator.utils.Javadocs
import net.kyori.adventure.key.Key
import javax.lang.model.element.Modifier.*

class GeneratedKeyType(
    keysClassName: String,
    pkg: String,
    val registry: GenericRegistry,
) : SimpleGenerator(keysClassName, pkg) {

    override val typeSpec: TypeSpec
        get() {
            val typeBuilder = keyHolderType()

            for ((keyPath, _) in registry.entries.toSortedMap(Formatting.ALPHABETIC_KEY_ORDER)) {
                val fieldName = Formatting.formatKeyAsField(keyPath.removePrefix("minecraft:"))
                val fieldBuilder =
                    FieldSpec.builder(Key::class.java, fieldName, PUBLIC, STATIC, FINAL)
                        .initializer("key(\$S)", keyPath)
                        .addJavadoc(Javadocs.getVersionDependentField("{@code \$L}"), keyPath)
                typeBuilder.addField(fieldBuilder.build())
            }

            return typeBuilder.build()
        }

    override fun buildFile(builder: JavaFile.Builder): JavaFile.Builder =
        builder.skipJavaLangImports(true)
            .addStaticImport(Key::class.java, "key")
            .indent("  ")


    private fun keyHolderType() = classBuilder(this.className)
        .addModifiers(PUBLIC, FINAL)
        .addAnnotations(Annotations.classHeader)
        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .build()
        )
}