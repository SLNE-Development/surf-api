package dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection;

import dev.slne.surf.surfapi.core.api.reflection.annontation.Static;
import dev.slne.surf.surfapi.core.api.reflection.annontation.SurfProxy;

@SurfProxy(qualifiedName = "org.spigotmc.RestartCommand")
public interface SpigotRestartCommandProxy {

    @Static
    void restart();
}
