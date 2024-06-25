package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import java.util.LinkedList;
import java.util.Objects;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.bukkit.entity.Player;

public final class PacketOperationImpl implements PacketOperation, NmsUtil {

  private Operation operation;

  public PacketOperationImpl(Operation operation) {
    this.operation = operation;
  }

  @Override
  public void execute(Player player) {
    toNms(player).connection.send(new ClientboundBundlePacket(
        Objects.requireNonNull(operation).apply(player, new LinkedList<>())));

  }

  @Override
  public PacketOperationImpl add(PacketOperation operation) {
    final PacketOperationImpl other = (PacketOperationImpl) operation;
    this.operation = this.operation.andThen(other.operation);
    return this;
  }

  @FunctionalInterface
  public interface Operation {

    LinkedList<Packet<? super ClientGamePacketListener>> apply(Player player,
        LinkedList<Packet<? super ClientGamePacketListener>> packets);

    default Operation andThen(Operation after) {
      Objects.requireNonNull(after);
      return (player, packets) -> {
        LinkedList<Packet<? super ClientGamePacketListener>> result = apply(player, packets);
        return after.apply(player, result);
      };
    }
  }
}
