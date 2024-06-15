package dev.slne.surf.surfapi.core.server.util;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

public class PlayerSkinFetcher {

  public static final PlayerSkinFetcher INSTANCE = new PlayerSkinFetcher();
  private final OkHttpClient client = new OkHttpClient.Builder().build();
  private final AsyncLoadingCache<UUID, List<TextureProperty>> SKIN_CACHE = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(10))
      .buildAsync(this::fetchSkin);

  public void fetchSkin(UUID playerUuid, Consumer<List<TextureProperty>> callback) {
    SKIN_CACHE.get(playerUuid).thenAccept(callback);
  }

  private List<TextureProperty> fetchSkin(@NotNull UUID uuid) {
    final Request request = new Request.Builder()
        .url("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid
            + "?unsigned=false")
        .build();

    final Call call = client.newCall(request);

    try (final Response response = call.execute()) {
      final ResponseBody body = response.body();

      if (body == null) {
        return List.of();
      }

      final String responseString = body.string();
      final JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
      final JsonArray properties = jsonObject.get("properties").getAsJsonArray();

      return properties.asList().stream()
          .map(JsonElement::getAsJsonObject)
          .map(property -> new TextureProperty(
              property.get("name").getAsString(),
              property.get("value").getAsString(),
              property.has("signature") ? property.get("signature").getAsString() : null
          ))
          .toList();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
