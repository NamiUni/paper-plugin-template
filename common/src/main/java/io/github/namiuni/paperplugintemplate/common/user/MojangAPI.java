package io.github.namiuni.paperplugintemplate.common.user;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MojangAPI {

    private static final String NAME = "name";
    private static final String MOJANG_API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final Gson gson;

    @Inject
    MojangAPI(final Gson gson) {
        this.gson = gson;
    }

    public Optional<String> findName(final UUID uuid) {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOJANG_API_URL + uuid))
                .GET()
                .build();

        try (final var client = HttpClient.newHttpClient()) {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final int statusCode = response.statusCode();

            if (statusCode == 200) {
                final JsonObject jsonObject = this.gson.fromJson(response.body(), JsonObject.class);
                return Optional.ofNullable(jsonObject.get(NAME).getAsString());
            }
        } catch (final Exception _) {
            // ignored
        }

        return Optional.empty();
    }
}
