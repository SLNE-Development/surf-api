package dev.slne.surf.surfapi.core.api.config.impl;

import dev.slne.surf.surfapi.core.api.config.Configuration;
import dev.slne.surf.surfapi.core.api.config.transformation.Transformations;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class BasicConfiguration<C> extends Configuration<C> {

  private final Consumer<C> configSetter;
  private final @Nullable String header;
  private final @Nullable UnaryOperator<ConfigurationTransformation.VersionedBuilder> versionUpdater;
  private final @Nullable UnaryOperator<ConfigurationTransformation.Builder> transformationUpdater;

  public BasicConfiguration(Path folder,
      Class<C> configClass,
      Consumer<C> configSetter,
      String configFileName,
      int configVersion,
      @Nullable String header,
      Supplier<C> getterSupplier,
      @Nullable UnaryOperator<ConfigurationTransformation.VersionedBuilder> versionUpdater,
      @Nullable UnaryOperator<ConfigurationTransformation.Builder> transformationUpdater) {

    super(folder, configClass, configFileName, configVersion, getterSupplier);
    this.configSetter = configSetter;

    this.header = header;
    this.versionUpdater = versionUpdater;
    this.transformationUpdater = transformationUpdater;
  }

  @Override
  protected YamlConfigurationLoader.Builder createYamlConfigurationLoaderBuilder() {
    return super.createYamlConfigurationLoaderBuilder()
        .defaultOptions(options -> options.header(header));
  }

  @Override
  public C initializeConfig() throws ConfigurateException {
    C c = super.initializeConfig();
    configSetter.accept(c);
    return c;
  }

  @Override
  protected void applyConfigTransformers(ConfigurationNode node) throws ConfigurateException {
    if (transformationUpdater != null) {
      ConfigurationTransformation.Builder builder = transformationUpdater.apply(
          ConfigurationTransformation.builder());
      builder.build().apply(node);
    }

    if (versionUpdater != null) {
      ConfigurationTransformation.VersionedBuilder versionedBuilder = versionUpdater.apply(
          Transformations.versionedBuilder());
      versionedBuilder.build().apply(node);
    }
  }
}
