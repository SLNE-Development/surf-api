package dev.slne.surf.surfapi.core.server;

import dev.slne.surf.surfapi.core.server.util.PlayerSkinFetcher;

public class CoreInstance {

    public void onLoad() {
        PlayerSkinFetcher.class.getClassLoader(); // initialize PlayerSkinFetcher
    }

    public void onEnable() {

    }

    public void onDisable() {

    }
}
