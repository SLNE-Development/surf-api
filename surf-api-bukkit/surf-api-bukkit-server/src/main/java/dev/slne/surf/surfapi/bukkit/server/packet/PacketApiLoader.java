package dev.slne.surf.surfapi.bukkit.server.packet;

import com.github.retrooper.packetevents.PacketEvents;
import dev.slne.surf.surfapi.bukkit.api.packet.SurfBukkitInteractListener;
import dev.slne.surf.surfapi.bukkit.server.BukkitMain;
import dev.slne.surf.surfapi.bukkit.server.exceptions.packet.UnableToSetupEntityCounterException;
import dev.slne.surf.surfapi.bukkit.server.impl.packet.SurfBukkitPacketApiImpl;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.entity.WrapperEntity;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The PacketApiLoader class is responsible for loading and initializing the packet API.
 * It sets up packet events, entity lib, and entity counter.
 */
@ApiStatus.Internal
public final class PacketApiLoader {

    /**
     * Represents a BukkitMain plugin instance.
     * This variable is used for loading the packet API.
     */
    private final BukkitMain plugin;

    /**
     * The entityCounterAtomic variable is an instance of the AtomicInteger class.
     * It represents the entity counter used in the PacketApiLoader class.
     * This counter is responsible for generating unique IDs for entities.
     * <p>
     * The entity counter is initialized and used in the setupEntityCounter method of the PacketApiLoader class.
     * It is set up by retrieving the NMS entity class, retrieving the entity counter field, and initializing the entity counter through reflection.
     * If any error occurs during the setup process, an UnableToSetupEntityCounterException is thrown.
     * <p>
     * The entityCounterAtomic variable can be accessed and modified by any method within the PacketApiLoader class.
     * It is primarily used to generate and track entity IDs.
     */
    private AtomicInteger entityCounterAtomic;

    /**
     * This class represents a PacketApiLoader.
     *
     * @param plugin The BukkitMain instance used for loading the packet API.
     */
    public PacketApiLoader(BukkitMain plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the necessary components for the onLoad event.
     */
    public void onLoad() {
        setupPacketEvents();
        setupEntityLib();
        setupEntityCounter();
    }

    /**
     * Initializes the plugin by calling the init method of the PacketEvents API.
     */
    public void onEnable() {
        PacketEvents.getAPI().init();
    }

    /**
     * This method is called when the plugin is being disabled.
     * It initializes the PacketEvents API.
     */
    public void onDisable() {
        PacketEvents.getAPI().init();
    }

    /**
     * Sets up the packet events by configuring the PacketEvents API.
     * This method should be called during the onLoad event.
     */
    private void setupPacketEvents() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
        PacketEvents.getAPI().load();
    }

    /**
     * Sets up the EntityLib by initializing PacketEvents API,
     * enabling entity interactions, and configuring the interaction processor.
     * This method should be called during the initialization of the PacketApiLoader.
     */
    private void setupEntityLib() {
        EntityLib.init(PacketEvents.getAPI());
        EntityLib.enableEntityInteractions();
        EntityLib.setInteractionProcessor((wrapperEntity, interactAction, interactionHand, user) -> {
            final Player player = Bukkit.getPlayer(user.getUUID());
            final List<SurfBukkitInteractListener> listeners = ((SurfBukkitPacketApiImpl) plugin.getSurfBukkitApi().getPacketApi()).getInteractListeners();

            for (SurfBukkitInteractListener listener : listeners) {
                listener.onInteract(wrapperEntity, interactAction, interactionHand, user, player);
            }
        });
    }

    /**
     * Sets up the entity counter by performing the following steps:
     * 1. Retrieves the NMS entity class using {@link #getNmsEntityClass()}.
     * 2. Retrieves the entity counter field using {@link #getEntityCounterField(Class)}.
     * 3. Initializes the entity counter by calling {@link #initializeEntityCounter(Field)}.
     * If any of these steps fail, an {@link UnableToSetupEntityCounterException} is thrown.
     */
    private void setupEntityCounter() {
        try {
            Class<?> nmsEntity = getNmsEntityClass();
            Field entityCounter = getEntityCounterField(nmsEntity);
            initializeEntityCounter(entityCounter);
        } catch (UnableToSetupEntityCounterException e) {
            ComponentLogger.logger().warn("Unable to setup entity counter. This is a bug, please report it to the developers.", e);
        }
    }

    /**
     * Retrieves the NMS (net.minecraft.world.entity.Entity) entity class.
     * This method is used to get the class object representing the NMS entity class.
     *
     * @return The NMS entity class.
     * @throws UnableToSetupEntityCounterException If the NMS entity class is not found.
     */
    private @NotNull Class<?> getNmsEntityClass() throws UnableToSetupEntityCounterException {
        try {
            return Class.forName("net.minecraft.world.entity.Entity");
        } catch (ClassNotFoundException e) {
            throw new UnableToSetupEntityCounterException("Class not found: net.minecraft.world.entity.Entity", e);
        }
    }

    /**
     * Retrieves the entity counter field of the given NMS entity class.
     *
     * @param nmsEntity The NMS entity class.
     * @return The entity counter field.
     * @throws UnableToSetupEntityCounterException If the entity counter field is not found.
     */
    private Field getEntityCounterField(@NotNull Class<?> nmsEntity) throws UnableToSetupEntityCounterException {
        try {
            return nmsEntity.getField("ENTITY_COUNTER");
        } catch (NoSuchFieldException e) {
            return getObfuscatedEntityCounterField(nmsEntity);
        }
    }

    /**
     * Retrieves the obfuscated entity counter field of the given NMS entity class.
     *
     * @param nmsEntity The NMS entity class.
     * @return The obfuscated entity counter field.
     * @throws UnableToSetupEntityCounterException If the obfuscated entity counter field is not found.
     */
    private @NotNull Field getObfuscatedEntityCounterField(@NotNull Class<?> nmsEntity) throws UnableToSetupEntityCounterException {
        try {
            return nmsEntity.getField("b");
        } catch (NoSuchFieldException ex) {
            throw new UnableToSetupEntityCounterException("Could not find net.minecraft.world.entity.Entity.ENTITY_COUNTER field", ex);
        }
    }

    /**
     * Initializes the entity counter field by retrieving the field object through reflection.
     * It sets the accessibility of the field to true, gets the current value of the field,
     * and sets the ID_PROVIDER to be a lambda function that increments and returns the value of the field.
     *
     * @param entityCounter The entity counter field.
     * @throws UnableToSetupEntityCounterException If an error occurs while setting up the entity counter.
     */
    private void initializeEntityCounter(@NotNull Field entityCounter) throws UnableToSetupEntityCounterException {
        try {
            entityCounter.setAccessible(true);
            entityCounterAtomic = (AtomicInteger) entityCounter.get(null);
            WrapperEntity.ID_PROVIDER = entityCounterAtomic::getAndIncrement;
        } catch (IllegalAccessException e) {
            throw new UnableToSetupEntityCounterException("Could not access net.minecraft.world.entity.Entity.ENTITY_COUNTER field", e);
        }
    }
}
