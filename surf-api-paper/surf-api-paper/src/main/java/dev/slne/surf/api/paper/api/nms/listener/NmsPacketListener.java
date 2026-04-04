package dev.slne.surf.api.paper.api.nms.listener;

import com.google.common.reflect.TypeToken;
import dev.slne.surf.api.paper.nms.NmsUseWithCaution;
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket;
import dev.slne.surf.api.shared.api.util.InternalSurfApi;
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
