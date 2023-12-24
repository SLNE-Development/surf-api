package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.UUIDArgument;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ShowHidePacketEntity extends CommandAPICommand {
    public ShowHidePacketEntity(String commandName) {
        super(commandName);

        withArguments(new UUIDArgument("entityUuid")
                .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> CreatePacketEntity.getEntityMap().keySet().stream().map(UUID::toString).toList())),
                new PlayerArgument("player"),
                new BooleanArgument("show"));

        executes((commandSender, commandArguments) -> {
            UUID entityUuid = commandArguments.getUnchecked("entityUuid");
            Player player = commandArguments.getUnchecked("player");
            var playerUuid = player.getUniqueId();
            boolean show = Boolean.TRUE.equals(commandArguments.getUnchecked("show"));

            SurfEntity<?> surfEntity = CreatePacketEntity.getEntityMap().get(entityUuid);

            if (show) {
                surfEntity.addViewer(playerUuid);
            } else {
                surfEntity.removeViewer(playerUuid);
            }

            commandSender.sendMessage((show ? "Showed" : "Hid") + " entity with UUID " + entityUuid + " to player with UUID " + playerUuid);
        });
    }
}
