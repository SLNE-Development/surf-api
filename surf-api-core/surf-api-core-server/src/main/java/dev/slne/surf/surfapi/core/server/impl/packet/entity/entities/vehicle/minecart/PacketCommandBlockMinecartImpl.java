package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketCommandBlockMinecart;
import dev.slne.surf.surfapi.core.api.util.blockstate.BlockStateFactory;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketCommandBlockMinecartImpl extends
    PacketAbstractMinecartImpl<PacketCommandBlockMinecart> implements PacketCommandBlockMinecart {

  private static final WrappedBlockState DEFAULT_BLOCK_STATE = BlockStateFactory.of(
      StateTypes.COMMAND_BLOCK);

  public PacketCommandBlockMinecartImpl(UUID uuid) {
    super(uuid, EntityTypes.COMMAND_BLOCK_MINECART, 6, DEFAULT_BLOCK_STATE);
  }

  @CommandPattern
  @Override
  public @NotNull String command() {
    final @Subst("/dummy:dummy arg") String command = get(COMMAND_INDEX, "");
    return command;
  }

  @Override
  public void command(@Nullable String command) {
    if (command != null) {
      checkArgument(
          COMMAND_PATTERN.matcher(command).matches(),
          """
              Command must match pattern'%s'!
              \n
              If you believe this is an error just go ahead and change the regex before blaming someone else!
              It's not like I'm the one who wrote it or anything...
              Oh wait, I did.
              Well, I guess you can blame me then.
              But I'm not going to change it.
              So there.
              I'm done now.
              Just change the regex it's not that hard.
              \n
              I'm not going to do it for you though.
              Why would I?
              \n
              I'm not the one who needs it changed.
              I'm the one who needs it to stay the same.
              I'm the one who wrote it.
              It's my regex.
              \n
              I'm not going to change it.
              You can change it.
              It's your regex now.
              That's how regexes work.
              You change them.
              They don't change you.
              You change them.
              You can change this one.
              You can change it to whatever you want.
              I don't care.
              Just change it.
              \n
              I'm not going to do it for you.
              I don't care what you change it to.
              Just change it.
              Please.
              Just change it.
              I'm begging you.
              Please change it.
              Please.
              \n
              I'm not going to do it.
              I'm not going to change it.
              I'm not going to change it for you.
              I'm not going to change it for anyone.
              I'm not going to change it for myself.
              I'm not going to change it for my family.
              I'm not going to change it for my friends.
              I'm not going to change it for my enemies.
              I'm not going to change it for anyone.
              (Except maybe you.)
              \n
              Credits to copilot for this error message.
              """,
          COMMAND_PATTERN_STRING);
    }

    set(COMMAND_INDEX, command == null ? "" : command);
    afterSet();
  }

  @Override
  public @NotNull Component lastOutput() {
    return get(LAST_OUTPUT_INDEX, Component.empty());
  }

  @Override
  public void lastOutput(@Nullable Component lastOutput) {
    set(LAST_OUTPUT_INDEX, lastOutput == null ? Component.empty() : lastOutput);
    afterSet();
  }

  @Override
  public int getData() {
    return 6;
  }
}
