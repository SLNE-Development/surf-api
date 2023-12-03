package dev.slne.surf.surfapi.bukkit.server;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import static dev.slne.surf.surfapi.core.server.maven.MavenVersions.*;

/**
 * The BukkitLoader class is an implementation of the PluginLoader interface.
 * It is responsible for loading and configuring the classpath for Bukkit plugins.
 * It uses a MavenLibraryResolver to resolve dependencies and add them to the classpath.
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public class BukkitLoader implements PluginLoader {

    /**
     * The resolver variable is an instance of the MavenLibraryResolver class.
     * It is used to resolve dependencies and add them to the classpath for the BukkitLoader class.
     * The resolver is initialized with the default constructor.
     */
    private final MavenLibraryResolver resolver = new MavenLibraryResolver();

    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {

        // Repositories
        addRepo("central", "https://repo1.maven.org/maven2/");
        addRepo("sonatype", "https://repository.sonatype.org/content/groups/public/");
        addRepo("papermc", "https://repo.papermc.io/repository/maven-public/");
        addRepo("codemc-releases", "https://repo.codemc.io/repository/maven-releases/");
        addRepo("codemc-snapshots", "https://repo.codemc.io/repository/maven-snapshots/");
        addRepo("sonatype-snapshots", "https://oss.sonatype.org/content/repositories/snapshots/");
        addRepo("jitpack", "https://jitpack.io/");

        // Dependencies
        addDependency("com.google.guava", "guava", GUAVA_VERSION);
        addDependency("com.github.ben-manes.caffeine", "caffeine", CAFFEINE_VERSION);
        addDependency("com.google.code.gson", "gson", GSON_VERSION);
        addDependency("org.apache.commons", "commons-lang3", COMMONS_LANG3_VERSION);
        addDependency("org.apache.commons", "commons-text", COMMONS_TEXT_VERSION);
        addDependency("com.github.retrooper.packetevents", "spigot", PACKET_EVENTS_SPIGOT_VERSION);
        addDependency("com.github.Tofaa2", "EntityLib", ENTITY_LIB_VERSION);
        addDependency("com.github.megavexnetwork.scoreboard-library", "scoreboard-library-api", SCOREBOARD_LIBRARY_VERSION);
        addDependency("com.github.megavexnetwork.scoreboard-library", "scoreboard-library-implementation", SCOREBOARD_LIBRARY_VERSION);
        addDependency("com.github.megavexnetwork.scoreboard-library", "scoreboard-library-packetevents", SCOREBOARD_LIBRARY_VERSION);
        addDependency("com.github.megavexnetwork.scoreboard-library", "scoreboard-library-modern", SCOREBOARD_LIBRARY_VERSION);

        pluginClasspathBuilder.addLibrary(resolver);
    }

    /**
     * Adds a repository URL to the MavenLibraryResolver.
     *
     * @param id The ID of the repository.
     * @param url The URL of the repository.
     */
    private void addRepo(@NotNull String id, @NotNull String url) {
        resolver.addRepository(new RemoteRepository.Builder(id, "default", url).build());
    }

    /**
     * Adds a dependency to the resolver.
     *
     * @param group   The group ID of the dependency.
     * @param artifact   The artifact ID of the dependency.
     * @param version   The version of the dependency.
     */
    private void addDependency(@NotNull String group, @NotNull String artifact, @NotNull String version) {
        resolver.addDependency(new Dependency(new DefaultArtifact("%s:%s:%s".formatted(group, artifact, version)), null));
    }
}
