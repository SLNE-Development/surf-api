package dev.slne.surf.surfapi.bukkit.api.nms.listener;

import com.google.common.reflect.TypeToken;
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.NmsPacket;
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

@NmsUseWithCaution
@NullMarked
public interface NmsPacketListener<Packet extends NmsPacket> {

    @ApiStatus.Internal
    @InternalSurfApi
    default Class<? super Packet> getPacketClass() {
        TypeToken<Packet> typeToken = new TypeToken<Packet>(getClass()) {
        };

        return typeToken.getRawType();
    }
}
