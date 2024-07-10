package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import java.util.LinkedList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.bukkit.entity.Player;

public final class PacketOperationImpl implements PacketOperation, NmsUtil {

  private Operation operation;

  public PacketOperationImpl(Operation operation) {
    this.operation = operation;
  }

  public PacketOperationImpl() {
    this.operation = Operation.empty();
  }

  @Override
  public void execute(Player player) {
    final var connection = toNms(player).connection;
    final var packets = operation.apply(player, new LinkedList<>());

    if (packets.isEmpty()) {
      // TODO: 10.07.2024 18:41 - log
      return;
    }

    if (packets.size() == 1) {
      connection.send(packets.getFirst());
      return;
    }

    connection.send(new ClientboundBundlePacket(packets));
  }

  @Override
  public PacketOperationImpl add(PacketOperation other) {
    checkArgument(other instanceof PacketOperationImpl,
        "operation must be an instance of PacketOperationImpl");

    this.operation = this.operation.andThen(((PacketOperationImpl) other).operation);
    return this;
  }

  @FunctionalInterface
  public interface Operation {

    LinkedList<Packet<? super ClientGamePacketListener>> apply(Player player,
        LinkedList<Packet<? super ClientGamePacketListener>> packets);

    default Operation andThen(Operation after) {
      checkNotNull(after, "after");

      return (player, packets) -> after.apply(player, apply(player, packets));
    }

    static Operation empty() {
      return (player, packets) -> packets;
    }
  }
}
