package dev.slne.surf.surfapi.bukkit.server.impl;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.server.impl.packet.SurfBukkitPacketApiImpl;
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * The SurfBukkitApiImpl class is an implementation of the SurfBukkitApi interface.
 * It extends the SurfCoreApiImpl class and provides additional functionality specific to the Bukkit platform.
 * This class provides access to the SurfBukkitApi instance.
 * It is recommended to use the static {@link SurfBukkitApi#get()} method to retrieve the instance.
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * SurfBukkitApi surfApi = SurfBukkitApi.get();
 * }</pre>
 * </p>
 *
 * @see SurfBukkitApi
 * @see SurfCoreApiImpl
 */
@ApiStatus.Internal
public class SurfBukkitApiImpl extends SurfCoreApiImpl<SurfBukkitPacketApi> implements SurfBukkitApi {

    public SurfBukkitApiImpl() {
        super(new SurfBukkitPacketApiImpl());
    }
}
