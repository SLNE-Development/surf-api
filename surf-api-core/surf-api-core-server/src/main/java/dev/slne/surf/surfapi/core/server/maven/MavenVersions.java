package dev.slne.surf.surfapi.core.server.maven;

import java.io.IOException;
import java.util.Properties;

/**
 * A utility class providing access to the versions of Maven artifacts.
 *
 * <p>
 * This class provides static final fields for commonly used Maven artifacts such as Guava, Caffeine, GSON, Commons Lang3, and Commons Text.
 * The versions of these artifacts are loaded from a properties file named "maven-versions.properties".
 * </p>
 *
 * <p>
 * Usage:
 * <pre>{@code
 * String guavaVersion = MavenVersions.GUAVA_VERSION;
 * String caffeineVersion = MavenVersions.CAFFEINE_VERSION;
 * String gsonVersion = MavenVersions.GSON_VERSION;
 * String lang3Version = MavenVersions.COMMONS_LANG3_VERSION;
 * String textVersion = MavenVersions.COMMONS_TEXT_VERSION;
 * }</pre>
 * </p>
 *
 * <p>
 * Note: The properties file "maven-versions.properties" must be present on the classpath and should contain the following keys:
 * <ul>
 *   <li>guava</li>
 *   <li>caffeine</li>
 *   <li>gson</li>
 *   <li>commons-lang3</li>
 *   <li>commons-text</li>
 * </ul>
 * </p>
 */
public final class MavenVersions {

    /**
     * The version of the Guava artifact.
     */
    public static final String GUAVA_VERSION;

    /**
     * The version of the Caffeine artifact.
     */
    public static final String CAFFEINE_VERSION;

    /**
     * The version of the Gson artifact.
     */
    public static final String GSON_VERSION;

    /**
     * The version of the Commons Lang3 artifact.
     */
    public static final String COMMONS_LANG3_VERSION;

    /**
     * The version of the Commons Text artifact.
     */
    public static final String COMMONS_TEXT_VERSION;

    /**
     * The version of the Packet Events artifact.
     */
    public static final String PACKET_EVENTS_SPIGOT_VERSION;

    /**
     * The version of the Entity Lib artifact.
     */
    public static final String ENTITY_LIB_VERSION;

    /**
     * The version of the Scoreboard Library artifact.
     */
    public static final String SCOREBOARD_LIBRARY_VERSION;

    static {
        // load properties file
        Properties properties = new Properties();

        try {
            properties.load(MavenVersions.class.getResourceAsStream("/maven-versions.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GUAVA_VERSION = properties.getProperty("guava");
        CAFFEINE_VERSION = properties.getProperty("caffeine");
        GSON_VERSION = properties.getProperty("gson");
        COMMONS_LANG3_VERSION = properties.getProperty("commons-lang3");
        COMMONS_TEXT_VERSION = properties.getProperty("commons-text");
        PACKET_EVENTS_SPIGOT_VERSION = properties.getProperty("packet-events-spigot");
        ENTITY_LIB_VERSION = properties.getProperty("entity-lib");
        SCOREBOARD_LIBRARY_VERSION = properties.getProperty("scoreboard-lib");
    }
}
