package dev.slne.surf.surfapi.core.api.config;

import dev.slne.surf.surfapi.core.api.config.serializer.DefaultSerializers;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.intellij.lang.annotations.Language;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import space.arim.dazzleconf.ConfigurationFactory;
import space.arim.dazzleconf.ConfigurationOptions;
import space.arim.dazzleconf.error.ConfigFormatSyntaxException;
import space.arim.dazzleconf.error.InvalidConfigException;
import space.arim.dazzleconf.ext.snakeyaml.CommentMode;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlConfigurationFactory;
import space.arim.dazzleconf.ext.snakeyaml.SnakeYamlOptions;
import space.arim.dazzleconf.helper.ConfigurationHelper;
import space.arim.dazzleconf.sorter.AnnotationBasedSorter;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.lang.annotation.*;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.*;

@ApiStatus.NonExtendable
@ApiStatus.Experimental
@ParametersAreNonnullByDefault
public final class SurfConfigManager<C> {
    private static final ComponentLogger LOGGER = ComponentLogger.logger("SurfConfigManager");
    private static final @Language("RegExp") String CONFIG_FILE_NAME_PATTERN = "^[a-zA-Z0-9_-]+\\.(yml|yaml)$";

    private final ConfigurationHelper<C> helper;
    private volatile C config;

    private SurfConfigManager(ConfigurationHelper<C> helper) {
        this.helper = checkNotNull(helper, "helper");
    }

    public @NotNull C reloadConfig() {
        try {
            config = helper.reloadConfigData();
        } catch (IOException e) {
            LOGGER.error("Failed to reload config", e);
            throw new RuntimeException(e);
        } catch (ConfigFormatSyntaxException e) {
            config = helper.getFactory().loadDefaults();
            LOGGER.error("""
                    Failed to reload config due to syntax error.
                    Using default config instead.
                    Check the YAML syntax with a tool like https://yamlchecker.com/
                    """, e);
        } catch (InvalidConfigException e) {
            config = helper.getFactory().loadDefaults();
            LOGGER.error("""
                    Failed to reload config due to invalid config.
                    Using default config instead.
                    Check the config values and try again.
                    """, e);
        }

        return config;
    }

    public @NotNull C getOrCreateConfig() {
        return config == null ? reloadConfig() : config;
    }

    public @CheckForNull C getConfig() {
        return config;
    }

    public static <C> SurfConfigManager<C> create(Class<C> configClass,
                                                  Path configFolder,
                                                  @ConfigFileNamePattern String configFileName) {
        checkNotNull(configClass, "configClass");
        checkNotNull(configFolder, "configFolder");
        checkNotNull(configFileName, "configFileName");

        final SnakeYamlOptions options = new SnakeYamlOptions.Builder()
                .commentMode(CommentMode.fullComments())
                .build();
        final ConfigurationFactory<C> factory = SnakeYamlConfigurationFactory.create(
                configClass,
                new ConfigurationOptions.Builder()
                        .addSerialisers(DefaultSerializers.DEFAULTS)
                        .setCreateSingleElementCollections(true)
                        .sorter(new AnnotationBasedSorter())
                        .build(),
                options
        );

        return new SurfConfigManager<>(new ConfigurationHelper<>(configFolder, configFileName, factory));
    }

    @Pattern(CONFIG_FILE_NAME_PATTERN)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @Documented
    public @interface ConfigFileNamePattern {
    }
}
