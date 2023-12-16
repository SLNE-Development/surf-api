package dev.slne.surf.surfapi.core.api.config.impl;

import dev.slne.surf.surfapi.core.api.config.ConfigurationConstants;
import dev.slne.surf.surfapi.core.api.config.ConfigurationPart;
import dev.slne.surf.surfapi.core.api.config.constraint.Constraint;
import dev.slne.surf.surfapi.core.api.config.constraint.Constraints;
import dev.slne.surf.surfapi.core.api.config.type.BooleanOrDefault;
import dev.slne.surf.surfapi.core.api.config.type.IntOr;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.nio.file.Path;

public class ConfigExample {
    public static void main(String[] args) {
        BasicConfiguration<TestConfig> loader = new BasicConfiguration<>(
                Path.of("test"),
                TestConfig.class,
                TestConfig::set,
                "test.yml",
                TestConfig.CONFIG_VERSION,
                "This is a test config header",
                versionedBuilder -> versionedBuilder,
                builder -> builder
        );

        try {
            loader.initializeConfig();
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal", "NotNullFieldNotInitialized", "InnerClassMayBeStatic"})
    private static class TestConfig extends ConfigurationPart {
        public static final int CONFIG_VERSION = 1;
        private static TestConfig instance;

        public static TestConfig get() {
            return instance;
        }

        public static void set(TestConfig instance) {
            TestConfig.instance = instance;
        }

        @Setting(ConfigurationConstants.VERSION_FIELD)
        public int version = CONFIG_VERSION;

        public int testInt = 1;

        @Comment("This is a test boolean with a comment")
        public boolean testBool = true;

        @Required
        public String requiredString;

        @Constraints.Min(4)
        @Constraint(Constraints.Positive.class)
        public int minPositiveInt = 10;

        @Constraints.Range(min = 5, max = 10)
        public int rangedInt = 5;

        @Comment("Can be either a number or 'default'")
        public IntOr.Default defaultInt = IntOr.Default.USE_DEFAULT;

        @Comment("Can be either a number or 'disabled'")
        public IntOr.Disabled disabledInt = IntOr.Disabled.DISABLED;

        @Comment("Can be either a boolean or 'default'")
        public BooleanOrDefault defaultBool = BooleanOrDefault.USE_DEFAULT;

        public Test subConfig;

        public class Test extends ConfigurationPart {
            public int testInt = 2;
            public Component testComponent = Component.text("test");
        }

        public Post postProcessPart;

        public class Post extends ConfigurationPart.Post {
            @Override
            public void postProcess() {
                System.out.println("Post processing");
            }
        }

        public SerializableRecord record = new SerializableRecord("test");

        @ConfigSerializable
        public record SerializableRecord(@Comment("This is required") @Required String requiredString, int testInt) {
            public SerializableRecord(@Comment("This is required") String requiredString) {
                this(requiredString, 1);
            }
        }
    }
}
