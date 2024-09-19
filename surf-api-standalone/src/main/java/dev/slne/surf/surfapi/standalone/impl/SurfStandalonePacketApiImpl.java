package dev.slne.surf.surfapi.standalone.impl;

import dev.slne.surf.surfapi.core.api.packet.SurfCorePacketEntityApi;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketApiImpl;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class SurfStandalonePacketApiImpl extends SurfCorePacketApiImpl {

  private final SurfStandalonePacketEntityApiImpl packetApi = new SurfStandalonePacketEntityApiImpl();

  @Override
  public SurfCorePacketEntityApi getPacketEntityApi() {
    return packetApi;
  }
}
