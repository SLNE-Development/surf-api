package dev.slne.surf.surfapi.bukkit.api.nms;

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsStatsBridge;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsBridge {

  SurfBukkitNmsStatsBridge getStatsBridge();

  @NotNull
  static SurfBukkitNmsBridge get() {
    return SurfBukkitApi.get().getNmsBridge();
  }
}
