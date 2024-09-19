package dev.slne.surf.surfapi.standalone.impl;

import dev.slne.surf.surfapi.core.api.packet.entity.EntityIdProvider;
import dev.slne.surf.surfapi.core.server.impl.packet.SurfCorePacketEntityApiImpl;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class SurfStandalonePacketEntityApiImpl extends SurfCorePacketEntityApiImpl {

  public SurfStandalonePacketEntityApiImpl() {
    super(EntityIdProvider.basic());
  }
}
