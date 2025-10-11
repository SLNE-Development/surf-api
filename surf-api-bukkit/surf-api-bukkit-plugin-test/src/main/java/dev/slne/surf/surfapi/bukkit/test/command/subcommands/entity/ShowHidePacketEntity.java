package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.PlayerProfileArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import java.util.UUID;
import org.bukkit.entity.Player;

public class ShowHidePacketEntity extends CommandAPICommand {

  public ShowHidePacketEntity(String commandName) {
    super(commandName);

    withArguments(new UUIDArgument("entityUuid"),
//                .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> CreatePacketEntity.getEntityMap().keySet().stream().map(UUID::toString).toList())),
        new PlayerProfileArgument("player"),
        new BooleanArgument("show"));

    executes((commandSender, commandArguments) -> {
      UUID entityUuid = commandArguments.getUnchecked("entityUuid");
      Player player = commandArguments.getUnchecked("player");
      var playerUuid = player.getUniqueId();
      boolean show = Boolean.TRUE.equals(commandArguments.getUnchecked("show"));

//            SurfEntity<?> surfEntity = CreatePacketEntity.getEntityMap().get(entityUuid);
//
//            if (show) {
//                surfEntity.addViewer(playerUuid);
//            } else {
//                surfEntity.removeViewer(playerUuid);
//            }

      commandSender.sendMessage(
          (show ? "Showed" : "Hid") + " entity with UUID " + entityUuid + " to player with UUID "
              + playerUuid);
    });
  }
}
