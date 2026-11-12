package dev.slne.surf.api.paper.test.command.subcommands.reflection;


import dev.slne.surf.api.reflection.Static;
import dev.slne.surf.api.reflection.SurfProxy;

@SurfProxy(qualifiedName = "org.spigotmc.RestartCommand")
public interface SpigotRestartCommandProxy {

    @Static
    void restart();
}
