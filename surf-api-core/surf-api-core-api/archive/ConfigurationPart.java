package dev.slne.surf.surfapi.core.api.config;

/**
 * Marker interface for unique sections of a configuration.
 */
public abstract class ConfigurationPart {

  public static abstract class Post extends ConfigurationPart {

    public abstract void postProcess();
  }
}
