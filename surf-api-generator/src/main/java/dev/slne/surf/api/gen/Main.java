package dev.slne.surf.api.gen;

import com.mojang.logging.LogUtils;
import dev.slne.surf.api.gen.generator.Generators;
import dev.slne.surf.api.gen.generator.SourceGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.slf4j.Logger;

public class Main {

  private static final Logger LOGGER = LogUtils.getLogger();
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
    layers = WorldLoader.loadAndReplaceLayer(resourceManager, layers, RegistryLayer.WORLDGEN,
        RegistryDataLoader.WORLDGEN_REGISTRIES);
    REGISTRY_ACCESS = layers.compositeAccess().freeze();
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