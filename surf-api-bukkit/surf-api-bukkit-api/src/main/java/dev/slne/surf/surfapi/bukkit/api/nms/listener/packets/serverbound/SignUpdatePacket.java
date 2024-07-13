package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound;

import io.papermc.paper.math.BlockPosition;
import org.checkerframework.common.value.qual.ArrayLen;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Range;

@NonExtendable
public non-sealed interface SignUpdatePacket extends NmsServerboundPacket {

  BlockPosition getPosition();

  @ArrayLen(4)
  String[] getLines();

  String getLine(@Range(from = 1, to = 4) int line);

  boolean isFrontText();
}
