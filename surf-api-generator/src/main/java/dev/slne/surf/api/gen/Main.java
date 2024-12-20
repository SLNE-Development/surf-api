package dev.slne.surf.api.gen;

import com.google.common.util.concurrent.MoreExecutors;
import dev.slne.surf.api.gen.generator.Generators;
import dev.slne.surf.api.gen.generator.SourceGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry.PendingTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.flag.FeatureFlags;

public class Main {

  private static final ComponentLogger LOGGER = ComponentLogger.logger();
  public static final RegistryAccess.Frozen REGISTRY_ACCESS;

  static {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
    Bootstrap.validate();

    final PackRepository resourcePackRepository = ServerPacksSource.createVanillaTrustedRepository();
    resourcePackRepository.reload();
    final MultiPackResourceManager resourceManager = new MultiPackResourceManager(
        PackType.SERVER_DATA,
        resourcePackRepository.getAvailablePacks().stream().map(Pack::open).toList());
    LayeredRegistryAccess<RegistryLayer> layers = RegistryLayer.createRegistryAccess();
    final List<PendingTags<?>> pendingTags = TagLoader.loadTagsForExistingRegistries(
        resourceManager, layers.getLayer(RegistryLayer.STATIC));
    final List<HolderLookup.RegistryLookup<?>> worldGenLayer = TagLoader.buildUpdatedLookups(
        layers.getAccessForLoading(RegistryLayer.WORLDGEN), pendingTags);
    final RegistryAccess.Frozen frozenWorldgenRegistries = RegistryDataLoader.load(resourceManager,
        worldGenLayer, RegistryDataLoader.WORLDGEN_REGISTRIES);
    layers = layers.replaceFrom(RegistryLayer.WORLDGEN, frozenWorldgenRegistries);
    REGISTRY_ACCESS = layers.compositeAccess().freeze();
    final ReloadableServerResources reloadableServerResources = ReloadableServerResources.loadResources(
        resourceManager,
        layers,
        pendingTags,
        FeatureFlags.VANILLA_SET,
        Commands.CommandSelection.DEDICATED,
        0,
        MoreExecutors.directExecutor(),
        MoreExecutors.directExecutor()
    ).join();
    reloadableServerResources.updateStaticRegistryTags();
  }

  public static void main(String[] args) {
    System.out.println("Hello world!");
    generate(Path.of("surf-api-core/surf-api-core-api/src/main/java"),
        Generators.CORE_API_GENERATORS);
  }

  private static void generate(Path output, SourceGenerator[] generators) {
    try {
//      if (Files.exists(output)) {
//        PathUtils.deleteDirectory(output);
//      }
      Files.createDirectories(output);

      for (final SourceGenerator generator : generators) {
        generator.writeToFile(output);
      }

      LOGGER.info("Files written to {}", output.toAbsolutePath());
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}