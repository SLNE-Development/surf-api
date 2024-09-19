package dev.slne.surf.surfapi.standalone;

import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.SurfCoreApiAccess;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
final class SurfStandaloneApiAccess extends SurfCoreApiAccess {

  @Internal
  protected static void setInstance(SurfCoreApi instance) {
    SurfCoreApiAccess.setInstance(instance);
  }
}
