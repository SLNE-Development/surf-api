package dev.slne.surf.api.gen.generator.types;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dev.slne.surf.api.gen.Main;
import dev.slne.surf.api.gen.generator.SimpleGenerator;
import dev.slne.surf.api.gen.generator.utils.Annotations;
import dev.slne.surf.api.gen.generator.utils.Formatting;
import dev.slne.surf.api.gen.generator.utils.Javadocs;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class GeneratedKeyType<T, A> extends SimpleGenerator {

  private final ResourceKey<? extends Registry<T>> registryKey;

  public GeneratedKeyType(final String keysClassName, final String pkg,
      final ResourceKey<? extends Registry<T>> registryKey) {
    super(keysClassName, pkg);
    this.registryKey = registryKey;
  }

  private TypeSpec.Builder keyHolderType() {
    return classBuilder(this.className)
        .addModifiers(PUBLIC, FINAL)
        .addAnnotations(Annotations.CLASS_HEADER)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE)
            .build()
        );
  }

  @Override
  protected TypeSpec getTypeSpec() {
    final TypeSpec.Builder typeBuilder = this.keyHolderType();

    final Registry<T> registry = Main.REGISTRY_ACCESS.registryOrThrow(this.registryKey);

    for (final Holder.Reference<T> reference : registry.holders()
        .sorted(Formatting.alphabeticKeyOrder(reference -> reference.key().location().getPath()))
        .toList()) {
      final ResourceKey<T> key = reference.key();
      final String keyPath = key.location().getPath();
      final String fieldName = Formatting.formatKeyAsField(keyPath);
      final FieldSpec.Builder fieldBuilder = FieldSpec.builder(Key.class, fieldName, PUBLIC, STATIC,
              FINAL)
          .initializer("key($S)", keyPath)
          .addJavadoc(Javadocs.getVersionDependentField("{@code $L}"), key.location().toString());
      typeBuilder.addField(fieldBuilder.build());
    }
    return typeBuilder.build();
  }

  @Override
  protected JavaFile.Builder file(final JavaFile.Builder builder) {
    return builder
        .skipJavaLangImports(true)
        .addStaticImport(Key.class, "key")
        .indent("    ");
  }
}
