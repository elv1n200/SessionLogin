package dev.elv1n200.sessionlogin.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Thin wrapper around the official Mojang/Microsoft endpoints, using the JDK's
 * built-in {@link HttpClient} so the mod has no external HTTP dependency to
 * bundle. Every request targets api.minecraftservices.com only.
 */
public final class ApiUtils {

	private static final String PROFILE_URL =
			"https://api.minecraftservices.com/minecraft/profile";
	private static final String SKINS_URL =
			"https://api.minecraftservices.com/minecraft/profile/skins";
	private static final String NAME_URL =
			"https://api.minecraftservices.com/minecraft/profile/name/";

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();

	private ApiUtils() {
	}

	/** @return {ign, undashedUuid} for the account owning {@code token}. */
	public static String[] getProfileInfo(String token) throws IOException {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(PROFILE_URL))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + token)
					.GET()
					.build();
			HttpResponse<String> response =
					CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int code = response.statusCode();
			if (code != 200) {
				throw new IOException("Mojang API HTTP " + code);
			}
			JsonObject json = JsonParser.parseString(response.body())
					.getAsJsonObject();
			if (!json.has("name") || !json.has("id")) {
				throw new IOException("Token rejected by Mojang API");
			}
			return new String[]{
					json.get("name").getAsString(),
					json.get("id").getAsString()
			};
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Request failed: " + e.getMessage(), e);
		}
	}

	/** True only if the token resolves to the currently active session. */
	public static boolean validateSession(String token) {
		try {
			String[] info = getProfileInfo(token);
			String ign = info[0];
			UUID uuid = UUID.fromString(SessionUtils.dashUuid(info[1]));
			var session = Minecraft.getInstance().getUser();
			return ign.equals(session.getName())
					&& uuid.equals(session.getProfileId());
		} catch (Exception e) {
			return false;
		}
	}

	/** True if the token still resolves to a Minecraft profile. */
	public static boolean tokenLooksValid(String token) {
		try {
			getProfileInfo(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static int changeSkin(String url, String token) {
		try {
			String body = String.format(
					"{ \"variant\": \"classic\", \"url\": \"%s\"}", url);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(SKINS_URL))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + token)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();
			return CLIENT.send(request,
					HttpResponse.BodyHandlers.discarding()).statusCode();
		} catch (Exception e) {
			return -1;
		}
	}

	public static int changeName(String newName, String token) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(NAME_URL + newName))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + token)
					.PUT(HttpRequest.BodyPublishers.noBody())
					.build();
			return CLIENT.send(request,
					HttpResponse.BodyHandlers.discarding()).statusCode();
		} catch (Exception e) {
			return -1;
		}
	}
}
