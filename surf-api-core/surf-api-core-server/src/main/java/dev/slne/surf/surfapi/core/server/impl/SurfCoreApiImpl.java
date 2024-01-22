package dev.slne.surf.surfapi.core.server.impl;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.config.SurfConfigManager;
import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.api.reflection.SurfReflection;
import dev.slne.surf.surfapi.core.server.config.SurfConfigTracker;
import dev.slne.surf.surfapi.core.server.impl.reflection.SurfReflectionImpl;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.google.common.base.Preconditions.*;

/**
 * The SurfCoreApiImpl class is an implementation of the SurfCoreApi interface.
 * It provides the functionality to access the SurfCoreApi instance.
 */
@ApiStatus.Internal
public abstract class SurfCoreApiImpl<PacketImpl extends SurfCorePacketApi> implements SurfCoreApi {

    /**
     * The packetApi variable is a private final field of type PacketImpl.
     * It is a member of the SurfCoreApiImpl class, which is an implementation of the SurfCoreApi interface.
     * This variable holds an instance of the PacketImpl class, which extends the SurfCorePacketApi interface.
     * It provides the functionality to access the SurfCorePacketApi instance.
     *
     * @see SurfCoreApiImpl
     * @see SurfCorePacketApi
     */
    private final PacketImpl packetApi;

    private final SurfReflection reflection;
    private final SurfConfigTracker configTracker;

    /**
     * Creates a new instance of the SurfCoreApiImpl class with the provided PacketImpl object.
     *
     * @param packetApi the PacketImpl object to be used
     */
    protected SurfCoreApiImpl(PacketImpl packetApi) {
        this.packetApi = packetApi;
        this.reflection = new SurfReflectionImpl();
        this.configTracker = new SurfConfigTracker();
    }

    @Override
    public PacketImpl getPacketApi() {
        return packetApi;
    }

    @Override
    public SurfReflection getReflection() {
        return reflection;
    }

    @Override
    public <C> C createConfig(@NotNull Class<C> configClass, @NotNull Path configFolder, @NotNull @SurfConfigManager.ConfigFileNamePattern String configFileName) {
        checkNotNull(configClass, "configClass");
        checkNotNull(configFolder, "configFolder");
        checkNotNull(configFileName, "configFileName");

        configTracker.registerConfig(configClass, configFolder, configFileName);
        configTracker.reloadConfig(configClass);
        return configTracker.getConfig(configClass);
    }

    @Override
    public <C> C getConfig(@NotNull Class<C> configClass) {
        checkNotNull(configClass, "configClass");

        return configTracker.getConfig(configClass);
    }

    @Override
    public <C> C reloadConfig(@NotNull Class<C> configClass) {
        checkNotNull(configClass, "configClass");

        return configTracker.reloadConfig(configClass);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
