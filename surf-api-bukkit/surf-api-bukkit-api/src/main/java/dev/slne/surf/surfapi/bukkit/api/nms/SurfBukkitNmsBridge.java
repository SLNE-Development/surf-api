package dev.slne.surf.surfapi.bukkit.api.nms;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsBridge {

  @NotNull
  static SurfBukkitNmsBridge get() {
    return SurfBukkitApi.get().getNmsBridge();
  }
}
