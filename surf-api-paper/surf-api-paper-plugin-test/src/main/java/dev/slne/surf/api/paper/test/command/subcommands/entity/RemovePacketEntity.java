package dev.slne.surf.api.paper.test.command.subcommands.entity;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.UUIDArgument;
import java.util.UUID;

public class RemovePacketEntity extends CommandAPICommand {

    public RemovePacketEntity(String commandName) {
        super(commandName);

        withArguments(new UUIDArgument("entityUuid"));
//                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> CreatePacketEntity.getEntityMap().keySet().stream().map(UUID::toString).toList())));

        executes((commandSender, commandArguments) -> {
            UUID entityUuid = commandArguments.getUnchecked("entityUuid");

            assert entityUuid != null;

            // TODO
//            SurfBukkitPacketEntityApi.get().deleteEntity(entityUuid);
//            CreatePacketEntity.getEntityMap().remove(entityUuid);

            commandSender.sendMessage("Removed entity with UUID " + entityUuid);
        });
    }
}
