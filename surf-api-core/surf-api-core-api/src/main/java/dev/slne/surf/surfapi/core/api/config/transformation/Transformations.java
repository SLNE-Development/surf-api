package dev.slne.surf.surfapi.core.api.config.transformation;

import dev.slne.surf.surfapi.core.api.config.ConfigurationConstants;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import static org.spongepowered.configurate.NodePath.path;

public class Transformations {
    private Transformations() {
    }

    public static void moveFromRoot(final ConfigurationTransformation.Builder builder, final String key, final String... parents) {
        moveFromRootAndRename(builder, key, key, parents);
    }

    public static void moveFromRootAndRename(final ConfigurationTransformation.Builder builder, final String oldKey, final String newKey, final String... parents) {
        moveFromRootAndRename(builder, path(oldKey), newKey, parents);
    }

    public static void moveFromRootAndRename(final ConfigurationTransformation.Builder builder, final NodePath oldKey, final String newKey, final String... parents) {
        builder.addAction(oldKey, (path, value) -> {
            final Object[] newPath = new Object[parents.length + 1];
            newPath[parents.length] = newKey;
            System.arraycopy(parents, 0, newPath, 0, parents.length);
            return newPath;
        });
    }

    public static ConfigurationTransformation.VersionedBuilder versionedBuilder() {
        return ConfigurationTransformation.versionedBuilder().versionKey(ConfigurationConstants.VERSION_FIELD);
    }
}
