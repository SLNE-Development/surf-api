package dev.slne.surf.surfapi.core.api.config.serializer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.ApiStatus;
import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

import java.nio.file.Path;
import java.util.List;

/**
 * These are the default serializers that are always enabled when creating a configuration via
 * {@link dev.slne.surf.surfapi.core.api.config.SurfConfigManager#create(Class, Path, String)}
 */
@ApiStatus.NonExtendable
public final class DefaultSerializers {

    public static final List<ValueSerialiser<?>> DEFAULTS = List.of(
            new ComponentSerializer()
    );

    public static final class ComponentSerializer implements ValueSerialiser<Component> {

        @Override
        public Class<Component> getTargetClass() {
            return Component.class;
        }

        @Override
        public Component deserialise(FlexibleType flexibleType) throws BadValueException {
            return MiniMessage.miniMessage().deserialize(flexibleType.getString());
        }

        @Override
        public Object serialise(Component value, Decomposer decomposer) {
            return MiniMessage.miniMessage().serialize(value);
        }
    }
}
