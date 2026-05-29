package dev.elv1n200.sessionlogin.util;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy player-head cache. First call for a UUID returns null and kicks off a
 * background fetch; later calls return the registered {@link Identifier} once
 * the image is loaded. Renderers should treat null as "draw fallback".
 *
 * <p>Image source is the Crafatar CDN — small, fast, no auth, returns a 16x16
 * head with the hat overlay applied. Failure is silent: a UUID that can't be
 * fetched just keeps returning null forever (until a relaunch).
 */
public final class SkinCache {

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private static final Map<UUID, Identifier> CACHE = new ConcurrentHashMap<>();
	private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();

	private SkinCache() {
	}

	/** @return the head texture id, or null while loading / on failure. */
	public static Identifier head(String uuidString) {
		if (uuidString == null || uuidString.isEmpty()) {
			return null;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(SessionUtils.dashUuid(uuidString));
		} catch (Exception e) {
			return null;
		}
		Identifier hit = CACHE.get(uuid);
		if (hit != null) {
			return hit;
		}
		if (LOADING.add(uuid)) {
			new Thread(() -> load(uuid), "SLSkinFetch-" + uuid).start();
		}
		return null;
	}

	private static void load(UUID uuid) {
		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create("https://crafatar.com/avatars/"
							+ uuid + "?size=16&overlay"))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<byte[]> resp = CLIENT.send(req,
					HttpResponse.BodyHandlers.ofByteArray());
			if (resp.statusCode() != 200) {
				return;
			}
			NativeImage img = NativeImage.read(
					new ByteArrayInputStream(resp.body()));
			Identifier id = Identifier.of(SessionLogin.MOD_ID,
					"head/" + uuid.toString().replace('-', '_').toLowerCase());

			MinecraftClient.getInstance().execute(() -> {
				try {
					NativeImageBackedTexture tex =
							new NativeImageBackedTexture(() -> "sl-head", img);
					MinecraftClient.getInstance().getTextureManager()
							.registerTexture(id, tex);
					CACHE.put(uuid, id);
				} catch (Exception e) {
					SessionLogin.LOGGER.debug("Could not register head", e);
				}
			});
		} catch (Exception e) {
			// fall through – stays null, fallback drawn
		} finally {
			LOADING.remove(uuid);
		}
	}
}
