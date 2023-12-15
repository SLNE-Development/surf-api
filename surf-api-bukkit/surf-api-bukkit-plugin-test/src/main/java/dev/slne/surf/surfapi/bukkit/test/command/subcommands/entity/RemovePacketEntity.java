package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.UUIDArgument;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;

import java.util.UUID;

public class RemovePacketEntity extends CommandAPICommand {
    public RemovePacketEntity(String commandName) {
        super(commandName);

        withArguments(new UUIDArgument("entityUuid")
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> CreatePacketEntity.getEntityMap().keySet().stream().map(UUID::toString).toList())));

        executes((commandSender, commandArguments) -> {
            UUID entityUuid = commandArguments.getUnchecked("entityUuid");

            assert entityUuid != null;

            SurfBukkitPacketEntityApi.get().deleteEntity(entityUuid);
            CreatePacketEntity.getEntityMap().remove(entityUuid);

            commandSender.sendMessage("Removed entity with UUID " + entityUuid);
        });
    }
}
