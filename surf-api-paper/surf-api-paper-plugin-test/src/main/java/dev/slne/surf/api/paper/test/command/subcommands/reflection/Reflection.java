package dev.slne.surf.api.paper.test.command.subcommands.reflection;


import dev.slne.surf.api.core.reflection.SurfReflection;

public class Reflection {

    public static final SpigotRestartCommandProxy RESTART_COMMAND;

    static {
        final SurfReflection reflection = SurfReflection.Companion;

        RESTART_COMMAND = reflection.createProxy(SpigotRestartCommandProxy.class);
    }
}
