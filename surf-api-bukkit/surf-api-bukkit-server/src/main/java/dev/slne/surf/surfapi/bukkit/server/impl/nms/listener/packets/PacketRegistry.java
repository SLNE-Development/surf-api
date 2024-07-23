package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets;

import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket;
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.CommandSuggestionPacketImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.RenameItemPacketImpl;
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.SignUpdatePacketImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketRegistry {

  private static final Object2ObjectMap<Class<? extends Packet<?>>, ServerboundPacketFactory<?, ?>> SERVERBOUND_PACKETS = new Object2ObjectOpenHashMap<>();
  private static final Object2ObjectMap<Class<? extends Packet<?>>, ClientboundPacketFactory<?, ?>> CLIENTBOUND_PACKETS = new Object2ObjectOpenHashMap<>();

  static {
    // @formatter:off
    registerServerboundPacket(ServerboundSignUpdatePacket.class, SignUpdatePacketImpl::new);
    registerServerboundPacket(ServerboundRenameItemPacket.class, RenameItemPacketImpl::new);
    registerServerboundPacket(ServerboundCommandSuggestionPacket.class, CommandSuggestionPacketImpl::new);
    // @formatter:on
  }

  private static <Nms extends Packet<ServerGamePacketListener>, Api extends NmsServerboundPacket> void registerServerboundPacket(
      Class<Nms> nmsClass, ServerboundPacketFactory<Nms, Api> factory) {
    SERVERBOUND_PACKETS.put(nmsClass, factory);
  }

  public static <Nms extends Packet<?>> @Nullable NmsServerboundPacket createServerboundPacketOrNull(
      @NotNull Nms packet) {
    final ServerboundPacketFactory<Nms, ?> factory = (ServerboundPacketFactory<Nms, ?>) SERVERBOUND_PACKETS.get(
        packet.getClass());
    return factory == null ? null : factory.create(packet);
  }

  private static <Nms extends Packet<ClientGamePacketListener>, Api extends NmsClientboundPacket> void registerClientboundPacket(
      Class<Nms> nmsClass, ClientboundPacketFactory<Nms, Api> factory) {
    CLIENTBOUND_PACKETS.put(nmsClass, factory);
  }

  public static <Nms extends Packet<?>> @Nullable NmsClientboundPacket createClientboundPacketOrNull(
      @NotNull Nms packet) {
    final ClientboundPacketFactory<Nms, ?> factory = (ClientboundPacketFactory<Nms, ?>) CLIENTBOUND_PACKETS.get(
        packet.getClass());
    return factory == null ? null : factory.create(packet);
  }

  @FunctionalInterface
  private interface ServerboundPacketFactory<Nms extends Packet<?>, Api extends NmsServerboundPacket> {

    Api create(Nms packet);
  }

  @FunctionalInterface
  private interface ClientboundPacketFactory<Nms extends Packet<?>, Api extends NmsClientboundPacket> {

    Api create(Nms packet);
  }
}
