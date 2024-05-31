package dev.slne.surf.surfapi.bukkit.api.nms.bridges;

import dev.slne.surf.surfapi.bukkit.api.nms.SurfBukkitNmsBridge;
import javax.annotation.ParametersAreNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
@ParametersAreNonnullByDefault
public interface SurfBukkitNmsStatsBridge {

  @NotNull
  static SurfBukkitNmsStatsBridge get() {
    return SurfBukkitNmsBridge.get().getStatsBridge();
  }
}
