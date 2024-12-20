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
import io.papermc.paper.registry.RegistryKey;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.RegistrySetBuilder.RegistryBootstrap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class GeneratedKeyType<T, A> extends SimpleGenerator {

  private static final Map<ResourceKey<? extends Registry<?>>, RegistryBootstrap<?>> VANILLA_REGISTRY_ENTRIES = VanillaRegistries.BUILDER.entries.stream()
      .collect(Collectors.toMap(RegistrySetBuilder.RegistryStub::key,
          RegistrySetBuilder.RegistryStub::bootstrap));

  private static final Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryBootstrap<?>> EXPERIMENTAL_REGISTRY_ENTRIES = Map.of(); // Update for Experimental API
  private static final Map<RegistryKey<?>, String> REGISTRY_KEY_FIELD_NAMES;

  static {
    final Map<RegistryKey<?>, String> map = new HashMap<>();
    try {
      for (final Field field : RegistryKey.class.getFields()) {
        if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers()) || field.getType() != RegistryKey.class) {
          continue;
        }
        map.put((RegistryKey<?>) field.get(null), field.getName());
      }
      REGISTRY_KEY_FIELD_NAMES = Map.copyOf(map);
    } catch (final ReflectiveOperationException ex) {
      throw new RuntimeException(ex);
    }
  }

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

    final Registry<T> registry = Main.REGISTRY_ACCESS.lookupOrThrow(this.registryKey);

    for (final Holder.Reference<T> reference : registry.listElements()
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
