package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import com.google.common.reflect.TypeToken;
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;

@NmsUseWithCaution
public interface NmsPacketListener<Packet extends NmsPacket> {

  default Class<? super Packet> getPacketClass() {
    TypeToken<Packet> typeToken = new TypeToken<Packet>(getClass()) {
    };

    return typeToken.getRawType();
  }
}
