package dev.slne.surf.surfapi.standalone;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.ProtocolVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.slne.surf.surfapi.core.server.CoreInstance;
import io.github.retrooper.packetevents.impl.netty.NettyManagerImpl;
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract;
import io.github.retrooper.packetevents.impl.netty.manager.protocol.ProtocolManagerAbstract;
import io.github.retrooper.packetevents.impl.netty.manager.server.ServerManagerAbstract;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class SurfApiStandaloneBootstrap {

  private static final AtomicBoolean shutdown = new AtomicBoolean(false);
  private static CoreInstance coreInstance;

  @Contract(pure = true)
  private SurfApiStandaloneBootstrap() {
  }

  @Contract(pure = true)
  public static void bootstrap() {
    preparePacketEvents();

    coreInstance = new CoreInstance();
    coreInstance.onLoad();
    coreInstance.onEnable();

    Runtime.getRuntime().addShutdownHook(new Thread(SurfApiStandaloneBootstrap::shutdown));
  }

  @Contract(pure = true)
  public static void shutdown() {
    if (shutdown.getAndSet(true)) {
      return;
    }

    coreInstance.onDisable();
    destroyPacketEvents();
  }

  private static void preparePacketEvents() {
    PacketEvents.setAPI(new NoopPacketEvents());
    PacketEvents.getAPI().load();
    PacketEvents.getAPI().init();
  }

  private static void destroyPacketEvents() {
    PacketEvents.getAPI().terminate();
  }

  private static class NoopPacketEvents extends PacketEventsAPI<Object> {

    @Override
    public void load() {
    }

    @Override
    public boolean isLoaded() {
      return false;
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isInitialized() {
      return false;
    }

    @Override
    public void terminate() {
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public Object getPlugin() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ServerManager getServerManager() {
      return ServerManagerHolder.INSTANCE;
    }

    @Override
    public ProtocolManager getProtocolManager() {
      return ProtocolManagerHolder.INSTANCE;
    }

    @Override
    public PlayerManager getPlayerManager() {
      return PlayerManagerHolder.INSTANCE;
    }

    @Override
    public NettyManager getNettyManager() {
      return NettyManagerHolder.INSTANCE;
    }

    @Override
    public ChannelInjector getInjector() {
      return ChannelInjectorHolder.INSTANCE;
    }
  }

  private static class ServerManagerHolder extends ServerManagerAbstract {

    static final ServerManagerHolder INSTANCE = new ServerManagerHolder();

    @Override
    public ServerVersion getVersion() {
      return ServerVersion.getLatest();
    }
  }

  private static class ProtocolManagerHolder extends ProtocolManagerAbstract {

    static final ProtocolManagerHolder INSTANCE = new ProtocolManagerHolder();

    @Override
    public ProtocolVersion getPlatformVersion() {
      return ProtocolVersion.UNKNOWN;
    }
  }

  private static class PlayerManagerHolder extends PlayerManagerAbstract {

    static final PlayerManagerHolder INSTANCE = new PlayerManagerHolder();

    @Override
    public int getPing(@NotNull Object player) {
      return -1;
    }

    @Override
    public Object getChannel(@NotNull Object player) {
      throw new UnsupportedOperationException("Not implemented");
    }
  }

  private static class NettyManagerHolder {

    static final NettyManagerImpl INSTANCE = new NettyManagerImpl();
  }

  private static class ChannelInjectorHolder implements ChannelInjector {

    static final ChannelInjectorHolder INSTANCE = new ChannelInjectorHolder();

    @Override
    public void inject() {
    }

    @Override
    public void uninject() {
    }

    @Override
    public void updateUser(Object channel, User user) {
    }

    @Override
    public void setPlayer(Object channel, Object player) {
    }

    @Override
    public boolean isProxy() {
      return false;
    }
  }
}