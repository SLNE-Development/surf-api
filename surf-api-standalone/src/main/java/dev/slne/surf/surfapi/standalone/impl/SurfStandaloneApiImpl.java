package dev.slne.surf.surfapi.standalone.impl;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketApi;
import dev.slne.surf.surfapi.core.server.impl.SurfCoreApiImpl;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public final class SurfStandaloneApiImpl extends SurfCoreApiImpl<SurfCorePacketApi> {

  public SurfStandaloneApiImpl() {
    super(new SurfStandalonePacketApiImpl());
  }

  @Override
  public void sendPlayerToServer(UUID playerUuid, String server) {
    throw new UnsupportedOperationException("sendPlayerToServer is not supported in standalone mode");
  }

  @Override
  public Optional<Object> getPlayer(@NotNull UUID playerUuid) {
    throw new UnsupportedOperationException("getPlayer is not supported in standalone mode");
  }

  @Override
  public Path getDataFolder() {
    return Path.of("api-data");
  }
}
