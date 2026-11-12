package dev.slne.surf.api.paper.server.util.registry;

/**
 * Represents an object that can be frozen mostly used in registries.
 */
public interface Frozable<Self extends Frozable<Self>> {

    Self freeze();
}
