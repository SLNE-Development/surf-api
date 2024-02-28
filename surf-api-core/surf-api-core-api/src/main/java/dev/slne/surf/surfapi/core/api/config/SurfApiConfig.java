package dev.slne.surf.surfapi.core.api.config;

import com.google.common.base.Suppliers;
import dev.slne.surf.surfapi.core.api.SurfCoreApi;
import dev.slne.surf.surfapi.core.api.messages.Colors;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultObject;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;

@ConfHeader({"The configuration for the Surf API.",
    "This configuration is used to provide a uniform appearance across all Surf plugins.",
    "Use the minimessage format. This may help you: https://webui.advntr.dev/"})
@Internal
public interface SurfApiConfig {

  @ConfKey("prefix")
  @ConfComments({
      "The prefix for all Surf messages. This is used to provide a uniform appearance across all Surf plugins.",
      "Use the minimessage format."})
  @DefaultObject("defaultPrefix")
  Component prefix();

  static @NotNull Component defaultPrefix() {
    return Component.text(">> ", Colors.DARK_SPACER)
        .append(Component.text("Surf", Colors.PREFIX_COLOR))
        .append(Component.text(" | ", Colors.DARK_SPACER));
  }

  static SurfApiConfig getInstance() {
    return Holder.INSTANCE.get();
  }

  class Holder {
    private static final Supplier<SurfApiConfig> INSTANCE = Suppliers.memoize(
        () -> SurfCoreApi.getCore()
            .createConfig(SurfApiConfig.class, SurfCoreApi.getCore().getDataFolder(),
                "config.yml"));
  }
}
