package dev.slne.surf.surfapi.bukkit.server.nms;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
@ParametersAreNonnullByDefault
public interface NmsUtil {

  default ServerPlayer toNms(Player player) {
    return ((CraftPlayer) player).getHandle();
  }
}
