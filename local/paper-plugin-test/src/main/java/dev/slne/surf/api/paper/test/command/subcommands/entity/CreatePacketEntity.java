package dev.slne.surf.api.paper.test.command.subcommands.entity;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;

public class CreatePacketEntity extends CommandAPICommand {
//    private static final Map<UUID, SurfEntity<?>> ENTITY_MAP = new ConcurrentHashMap<>();

    public CreatePacketEntity(String commandName) {
        super(commandName);

        withArguments(new EntityTypeArgument("entityType"));

        executesPlayer((commandSender, commandArguments) -> {
//            org.bukkit.entity.EntityType entityType = commandArguments.getUnchecked("entityType");
//
//            final UUID entityUuid = UUID.randomUUID();
//            EntityType.EntityType0<EntityMeta> byName = EntityType.getByName(entityType.key().value());
//            commandSender.sendMessage(entityType.key().asString() + " -> " + EntityType.COW.getType().getName().toString());
//            commandSender.sendMessage((entityType.key().value()));
//
//            if (byName instanceof EntityType.LivingEntityType<?> livingEntityType) {
//                SurfLivingEntity<?> testLivingEntity = SurfBukkitApi.get().getPacketApi().getEntityApi().createEntity(entityUuid, livingEntityType, entityMeta -> {
//                    entityMeta.setCustomName(Component.text("Test Living Entity", Colors.GREEN));
//                    entityMeta.setCustomNameVisible(true);
//                    entityMeta.setHasGlowingEffect(true);
//                    entityMeta.setSneaking(true);
//                    entityMeta.setPose(EntityPose.SWIMMING);
//                });
//
//                testLivingEntity.getEquipment().setHelmet(ItemStack.builder().type(ItemTypes.DIAMOND_HELMET).build());
//                testLivingEntity.addViewer(commandSender);
//                testLivingEntity.spawn(commandSender.getLocation());
//                ENTITY_MAP.put(entityUuid, testLivingEntity);
//            } else {
//                SurfEntity<EntityMeta> testEntity = SurfBukkitPacketApi.get().createEntity(entityUuid, byName, entityMeta -> {
//                    entityMeta.setCustomName(Component.text("Test Entity", Colors.GREEN));
//                    entityMeta.setCustomNameVisible(true);
//                    entityMeta.setHasGlowingEffect(true);
//                    entityMeta.setSneaking(true);
//                    entityMeta.setPose(EntityPose.SWIMMING);
//                });
//
//                testEntity.addViewer(commandSender);
//                ENTITY_MAP.put(entityUuid, testEntity);
//            }
//
//            SurfBukkitPacketEntityApi.get().registerInteractListener(entityUuid, (clickedEntity, interactAction, interactionHand, user, player) -> {
//                commandSender.sendMessage("Interacted with entity with UUID " + entityUuid);
//                // debug all parameters
//                commandSender.sendMessage("clickedEntity: " + clickedEntity);
//                commandSender.sendMessage("interactAction: " + interactAction);
//                commandSender.sendMessage("interactionHand: " + interactionHand);
//                commandSender.sendMessage("user: " + user);
//                commandSender.sendMessage("player: " + player);
//            });
//
//            commandSender.sendMessage("Created entity with UUID " + entityUuid);
        });
    }

//    public static Map<UUID, SurfEntity<?>> getEntityMap() {
//        return ENTITY_MAP;
//    }
}
