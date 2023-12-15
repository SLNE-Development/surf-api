package dev.slne.surf.surfapi.velocity.server.impl;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
import dev.slne.surf.surfapi.velocity.api.SurfVelocityApi;
import dev.slne.surf.surfapi.velocity.api.packet.SurfVelocityPacketApi;
import dev.slne.surf.surfapi.velocity.server.VelocityMain;
import dev.slne.surf.surfapi.velocity.server.impl.packet.SurfVelocityPacketApiImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.*;

/**
 * The SurfVelocityApiImpl class is an implementation of the SurfCoreApiImpl class.
 * It provides additional functionality specific to Surf Velocity.
 *
 * <p>
 * Example usage:
 * {@snippet :
 * import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
 * SurfCoreApiImpl<SurfVelocityPacketApi> surfApi = new SurfVelocityApiImpl();}
 * </p>
 */
@ApiStatus.Internal
public class SurfVelocityApiImpl extends SurfCoreApiImpl<SurfVelocityPacketApi> implements SurfVelocityApi {

    /**
     * The SurfVelocityApiImpl class is an implementation of the SurfCoreApiImpl class.
     * It provides additional functionality specific to Surf Velocity.
     * <p>
     * Example usage:
     * {@code  SurfCoreApiImpl<SurfVelocityPacketApi> surfApi = new SurfVelocityApiImpl();}
     */
    public SurfVelocityApiImpl() {
        super(new SurfVelocityPacketApiImpl());
    }

    @Override
    public ExecutorService getExecutorService() {
        return VelocityMain.getInstance().getExecutorService();
    }

    @Override
    public void sendPlayerToServer(UUID playerUuid, String server) {
        checkNotNull(playerUuid, "playerUuid");
        checkNotNull(server, "server");

        ProxyServer proxy = VelocityMain.getInstance().getServer();
        proxy.getPlayer(playerUuid).ifPresent(player -> proxy.getServer(server).ifPresent(player::createConnectionRequest));
    }
}
