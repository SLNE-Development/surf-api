package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import com.google.common.reflect.TypeToken;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@Internal
public sealed interface NmsPacketListener<Packet extends NmsPacket> permits
    NmsClientboundPacketListener, NmsServerboundPacketListener {

  @NonExtendable
  default Class<? super Packet> getPacketClass() {
    final TypeToken<Packet> typeToken = new TypeToken<>(getClass()) {
    };
    return typeToken.getRawType();
  }
}
