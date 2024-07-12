package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound;

import static com.google.common.base.Preconditions.checkArgument;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.SignUpdatePacket;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import io.papermc.paper.math.BlockPosition;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.checkerframework.common.value.qual.ArrayLen;

public final class SignUpdatePacketImpl extends
    NmsServerboundPacketImpl<ServerboundSignUpdatePacket> implements
    SignUpdatePacket, NmsUtil {

  public SignUpdatePacketImpl(ServerboundSignUpdatePacket nmsPacket) {
    super(nmsPacket);
  }

  @Override
  public BlockPosition getPosition() {
    return toBukkit(getNmsPacket().getPos());
  }

  @Override
  public @ArrayLen(4) String[] getLines() {
    return getNmsPacket().getLines();
  }

  @Override
  public String getLine(int line) {
    checkArgument(line >= 1 && line <= 4, "Line must be between 1 and 4");

    return getLines()[line - 1];
  }

  @Override
  public boolean isFrontText() {
    return getNmsPacket().isFrontText();
  }
}
