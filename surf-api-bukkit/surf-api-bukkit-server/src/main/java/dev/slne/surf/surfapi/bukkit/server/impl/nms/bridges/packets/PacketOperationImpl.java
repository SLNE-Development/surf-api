package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation;
import dev.slne.surf.surfapi.bukkit.server.nms.NmsUtil;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.bukkit.entity.Player;

public final class PacketOperationImpl implements PacketOperation, NmsUtil {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Operation operation;

  private PacketOperationImpl(Operation operation) {
    this.operation = operation;
  }

  private PacketOperationImpl() {
    this.operation = Operation.empty();
  }

  @Override
  public void execute(Player player) {
    final var connection = toNms(player).connection;
    final var packets = operation.apply(player, new LinkedList<>());

    if (packets.isEmpty()) {
      logger.atInfo()
          .atMostEvery(60, TimeUnit.SECONDS)
          .withStackTrace(StackSize.SMALL)
          .log("No packets to send for player '%s'. Is this intended behaviour?", player.getName());
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

  public static PacketOperationImpl empty() {
    return new PacketOperationImpl();
  }

  public static PacketOperationImpl complex(Operation operation) {
    return new PacketOperationImpl(operation);
  }

  public static PacketOperationImpl simple(Function<Player, Packet<? super ClientGamePacketListener>> packetSupplier) {
    return new PacketOperationImpl((player, packets) -> {
      packets.add(packetSupplier.apply(player));
      return packets;
    });
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
