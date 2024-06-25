package dev.slne.surf.surfapi.core.api.config;

import dev.slne.surf.surfapi.core.api.config.serializer.ModernSerializers;
import java.io.Serial;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ScopedConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader.Builder;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public final class SurfConfigManagerModern<C> {

  private static final ComponentLogger LOGGER = ComponentLogger.logger("SurfConfigManagerModern");
  private static final @Language("RegExp") String YAML_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(yml|yaml)$";
  private static final @Language("RegExp") String JSON_CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(json)$";


  private final ConfigurationLoader<? extends ConfigurationNode> loader;
  private final ConfigurationNode node;
  private final Class<C> configClass;
  private volatile C config;

  @Contract(pure = true)
  private SurfConfigManagerModern(Class<C> configClass, C config,
      ConfigurationLoader<? extends ConfigurationNode> loader,
      ConfigurationNode node) {
    this.configClass = configClass;
    this.config = config;
    this.loader = loader;
    this.node = node;
  }

  @Contract(pure = true)
  public C getConfig() {
    return config;
  }

  public void save() {
    try {
      node.set(configClass, config);
      loader.save(node);
    } catch (ConfigurateException e) {
      LOGGER.error("Failed to save config", e);
      throw new RuntimeException(e);
    }
  }

  public C reloadFromFile() {
    try {
      ConfigurationNode reloadedNode = loader.load();
      final C reloadedConfig = reloadedNode.get(configClass);

      node.set(configClass, reloadedConfig);
      loader.save(node);
      config = reloadedConfig;

      return config;
    } catch (ConfigurateException e) {
      LOGGER.error("Failed to reload config", e);
      throw new RuntimeException(e);
    }
  }

  @Contract("_, _, _ -> new")
  public static <C> @NotNull SurfConfigManagerModern<C> yaml(Class<C> configClass,
      @NotNull Path configFolder,
      @ModernYamlConfigFileNamePattern String configFileName) {
    return buildConfigManager("https://yamlchecker.com/", YamlConfigurationLoader.builder(),
        configClass, configFolder, configFileName);
  }


  @Contract("_, _, _ -> new")
  public static <C> @NotNull SurfConfigManagerModern<C> json(Class<C> configClass,
      @NotNull Path configFolder,
      @ModernJsonConfigFileNamePattern String configFileName) {
    return buildConfigManager("https://jsonlint.com/", JacksonConfigurationLoader.builder(),
        configClass, configFolder, configFileName);
  }

  @Contract("_, _, _, _, _ -> new")
  private static <C, T extends Builder<T, L>, L extends AbstractConfigurationLoader<?>> @NotNull SurfConfigManagerModern<C> buildConfigManager(
      String verifyToolUrl,
      @NotNull Builder<T, L> builder,
      Class<C> configClass,
      @NotNull Path configFolder, String configFileName
  ) {
    final L loader = builder
        .path(configFolder.resolve(configFileName))
        .defaultOptions(
            configurationOptions -> configurationOptions.serializers(ModernSerializers.SERIALIZERS))
        .build();

    try {
      final ScopedConfigurationNode<?> node = loader.load();
      final C config = node.get(configClass);

      loader.save(node);
      node.set(configClass, config);

      return new SurfConfigManagerModern<>(configClass, config, loader, node);
    } catch (SerializationException e) {
      LOGGER.error(
          Component.text("Failed to load config, please verify the file is correct")
              .append(Component.text(
                  "You can use a tool like %s to verify the file".formatted(verifyToolUrl))),
          e);
      throw new SerializationConfigException(e);
    } catch (ConfigurateException e) {
      LOGGER.error("Failed to load config", e);
      throw new LoadConfigException(e);
    }
  }

  @Pattern(YAML_CONFIG_FILE_NAME_PATTERN)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @Documented
  public @interface ModernYamlConfigFileNamePattern {

  }

  @Pattern(JSON_CONFIG_FILE_NAME_PATTERN)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE_USE)
  @Documented
  public @interface ModernJsonConfigFileNamePattern {

  }

  public static class LoadConfigException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 9079792924817337725L;

    public LoadConfigException(ConfigurateException e) {
      super(e);
    }
  }

  public static class SerializationConfigException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1667135777002012679L;

    public SerializationConfigException(SerializationException e) {
      super(e);
    }
  }
}
