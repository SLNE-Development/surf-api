package dev.slne.surf.surfapi.core.api.packet;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import java.util.UUID;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SurfCorePacketApi {

  static SurfCorePacketApi get() {
    return SurfCoreApi.getCore().getPacketApi();
  }

  SurfCorePacketEntityApi getPacketEntityApi();

  void sendPacket(UUID viewer, PacketWrapper<?> packet);
}
