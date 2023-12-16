package dev.slne.surf.surfapi.core.api.config;

import dev.slne.surf.surfapi.core.api.config.constraint.Constraint;
import dev.slne.surf.surfapi.core.api.config.constraint.Constraints;
import dev.slne.surf.surfapi.core.api.config.serializer.ComponentSerializer;
import dev.slne.surf.surfapi.core.api.config.serializer.collections.MapSerializer;
import dev.slne.surf.surfapi.core.api.config.type.BooleanOrDefault;
import dev.slne.surf.surfapi.core.api.config.type.DoubleOrDefault;
import dev.slne.surf.surfapi.core.api.config.type.Duration;
import dev.slne.surf.surfapi.core.api.config.type.IntOr;
import io.leangen.geantyref.GenericTypeReflector;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.lang.reflect.Type;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.*;

public abstract class Configuration<C> {

    private final Path folder;
    private final Class<C> configClass;
    private final ComponentLogger logger;
    private final int configVersion;
    private final Supplier<C> getterSupplier;
    private final String configFileName;

    public Configuration(@NotNull Path folder,
                         @NotNull Class<C> configClass,
                         @NotNull  String configFileName,
                         final int configVersion,
                         Supplier<C> getterSupplier) {

        this.folder = folder;
        this.configClass = configClass;
        this.configFileName = configFileName;
        this.configVersion = configVersion;
        this.getterSupplier = getterSupplier;

        logger = ComponentLogger.logger("Configuration: " + configFileName);
    }

    @OverridingMethodsMustInvokeSuper
    protected  ObjectMapper.Factory.Builder createObjectMapperFactoryBuilder() {
        return ObjectMapper.factoryBuilder();
    }

    private ObjectMapper.Factory.Builder createObjectMapperFactoryBuilderInternal() {
        return createObjectMapperFactoryBuilder()
                .addConstraint(Constraint.class, new Constraint.Factory())
                .addConstraint(Constraints.Min.class, Number.class, new Constraints.Min.Factory())
                .addConstraint(Constraints.Max.class, Number.class, new Constraints.Max.Factory())
                .addConstraint(Constraints.Range.class, Number.class, new Constraints.Range.Factory())
                .addDiscoverer(InnerClassFieldDiscoverer.basicConfig());
    }

    protected YamlConfigurationLoader.Builder createYamlConfigurationLoaderBuilder() {
        return ConfigurationLoaders.naturallySorted();
    }

    private YamlConfigurationLoader.Builder createYamlConfigurationLoaderBuilderInternal() {
        return createYamlConfigurationLoaderBuilder()
                .defaultOptions(options -> options
                        .serializers(builder -> builder
                                .register(MapSerializer.TYPE, new MapSerializer(false))
                                .register(new ComponentSerializer())
                                .register(IntOr.Default.SERIALIZER)
                                .register(IntOr.Disabled.SERIALIZER)
                                .register(DoubleOrDefault.SERIALIZER)
                                .register(BooleanOrDefault.SERIALIZER)
                                .register(Duration.SERIALIZER)));
    }

    protected boolean isConfigType(final Type type) {
        return ConfigurationPart.class.isAssignableFrom(GenericTypeReflector.erase(type));
    }

    @OverridingMethodsMustInvokeSuper
    public C initializeConfig() throws ConfigurateException {
        return initializeConfig(creator(configClass, true));
    }

    @OverridingMethodsMustInvokeSuper
    public void reloadConfig() {
        try {
            this.initializeConfig(reloader(configClass, getterSupplier.get()));
        } catch (ConfigurateException e) {
            logger.error("Unable to reload config", e);
            throw new RuntimeException(e);
        }
    }

    protected final C initializeConfig(final CheckedFunction<ConfigurationNode, C, SerializationException> creator) throws ConfigurateException {
        final Path configFile = folder.resolve(configFileName);
        final YamlConfigurationLoader loader = createYamlConfigurationLoaderBuilderInternal()
                .defaultOptions(applyObjectMapperFactory(createObjectMapperFactoryBuilderInternal().build()))
                .path(configFile)
                .build();
        final ConfigurationNode node;

        if (Files.exists(configFile)) {
            node = loader.load();
        } else {
            node = CommentedConfigurationNode.root(loader.defaultOptions());
            node.node(ConfigurationConstants.VERSION_FIELD).raw(configVersion);
        }

        applyConfigTransformers(node);

//        final BasicConfigurationNode defaults = BasicConfigurationNode.root(loader.defaultOptions());
//        node.mergeFrom(defaults);


        final C config = creator.apply(node);
        trySaveFileNode(loader, node, configFileName);

        return config;
    }

    @ApiStatus.OverrideOnly
    protected void applyConfigTransformers(final ConfigurationNode node) throws ConfigurateException {
    }

    private void trySaveFileNode(YamlConfigurationLoader loader, ConfigurationNode node, String filename) throws ConfigurateException {
        try {
            loader.save(node);
        } catch (ConfigurateException exception) {
            if (exception.getCause() instanceof AccessDeniedException) {
                logger.warn("Unable to save Config %s, access denied".formatted(filename), exception);
            } else {
                logger.error("Unable to save Config %s".formatted(filename), exception);
                throw exception;
            }
        }
    }

    private UnaryOperator<ConfigurationOptions> applyObjectMapperFactory(final ObjectMapper.Factory factory) {
        return options -> options.serializers(builder -> builder
                .register(this::isConfigType, factory.asTypeSerializer())
                .registerAnnotatedObjects(factory));
    }

    @Contract(pure = true)
    static <T> @NotNull CheckedFunction<ConfigurationNode, T, SerializationException> creator(Class<T> type, boolean refreshNode) {
        return node -> {
            final T instance = node.require(type);
            if (refreshNode) {
                node.set(type, instance);
            }
            return instance;
        };
    }

    @Contract(pure = true)
    static <T> @NotNull CheckedFunction<ConfigurationNode, T, SerializationException> reloader(Class<T> type, T instance) {
        return node -> {
            final ObjectMapper.Mutable<T> mutable = (ObjectMapper.Mutable<T>) ((ObjectMapper.Factory) checkNotNull(
                    node.options().serializers().get(type),
                    "No serializer for type %s",
                    type
            )).get(type);

            mutable.load(instance, node);

            return instance;
        };
    }
}
