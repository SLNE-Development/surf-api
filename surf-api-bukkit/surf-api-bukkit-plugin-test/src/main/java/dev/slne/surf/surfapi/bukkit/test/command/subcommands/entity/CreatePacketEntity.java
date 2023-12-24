package dev.slne.surf.surfapi.bukkit.test.command.subcommands.entity;

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitPacketApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.SurfBukkitPacketEntityApi;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.entity.entities.SurfLivingEntity;
import dev.slne.surf.surfapi.bukkit.api.packet.meta.EntityType;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import me.tofaa.entitylib.meta.EntityMeta;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CreatePacketEntity extends CommandAPICommand {
    private static final Map<UUID, SurfEntity<?>> ENTITY_MAP = new ConcurrentHashMap<>();

    public CreatePacketEntity(String commandName) {
        super(commandName);

        withArguments(new EntityTypeArgument("entityType"));

        executesPlayer((commandSender, commandArguments) -> {
            org.bukkit.entity.EntityType entityType = commandArguments.getUnchecked("entityType");

            final UUID entityUuid = UUID.randomUUID();
            EntityType.EntityType0<EntityMeta> byName = EntityType.getByName(entityType.key().value());
            commandSender.sendMessage(entityType.key().asString() + " -> " + EntityType.COW.getType().getName().toString());
            commandSender.sendMessage((entityType.key().value()));

            if (byName instanceof EntityType.LivingEntityType<?> livingEntityType) {
                SurfLivingEntity<?> testLivingEntity = SurfBukkitApi.get().getPacketApi().getEntityApi().createEntity(entityUuid, livingEntityType, entityMeta -> {
                    entityMeta.setCustomName(Component.text("Test Living Entity", Colors.GREEN));
                    entityMeta.setCustomNameVisible(true);
                    entityMeta.setHasGlowingEffect(true);
                    entityMeta.setSneaking(true);
                    entityMeta.setPose(EntityPose.SWIMMING);
                });

                testLivingEntity.getEquipment().setHelmet(ItemStack.builder().type(ItemTypes.DIAMOND_HELMET).build());
                testLivingEntity.addViewer(commandSender);
                testLivingEntity.spawn(commandSender.getLocation());
                ENTITY_MAP.put(entityUuid, testLivingEntity);
            } else {
                SurfEntity<EntityMeta> testEntity = SurfBukkitPacketApi.get().createEntity(entityUuid, byName, entityMeta -> {
                    entityMeta.setCustomName(Component.text("Test Entity", Colors.GREEN));
                    entityMeta.setCustomNameVisible(true);
                    entityMeta.setHasGlowingEffect(true);
                    entityMeta.setSneaking(true);
                    entityMeta.setPose(EntityPose.SWIMMING);
                });

                testEntity.addViewer(commandSender);
                ENTITY_MAP.put(entityUuid, testEntity);
            }

            SurfBukkitPacketEntityApi.get().registerInteractListener(entityUuid, (clickedEntity, interactAction, interactionHand, user, player) -> {
                commandSender.sendMessage("Interacted with entity with UUID " + entityUuid);
                // debug all parameters
                commandSender.sendMessage("clickedEntity: " + clickedEntity);
                commandSender.sendMessage("interactAction: " + interactAction);
                commandSender.sendMessage("interactionHand: " + interactionHand);
                commandSender.sendMessage("user: " + user);
                commandSender.sendMessage("player: " + player);
            });

            commandSender.sendMessage("Created entity with UUID " + entityUuid);
        });
    }

    public static Map<UUID, SurfEntity<?>> getEntityMap() {
        return ENTITY_MAP;
    }
}
