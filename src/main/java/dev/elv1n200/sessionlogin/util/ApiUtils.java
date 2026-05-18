package dev.elv1n200.sessionlogin.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Thin wrapper around the official Mojang/Microsoft endpoints.
 * Every request targets api.minecraftservices.com only.
 */
public final class ApiUtils {

	private static final String PROFILE_URL =
			"https://api.minecraftservices.com/minecraft/profile";
	private static final String SKINS_URL =
			"https://api.minecraftservices.com/minecraft/profile/skins";
	private static final String NAME_URL =
			"https://api.minecraftservices.com/minecraft/profile/name/";

	private ApiUtils() {
	}

	/** @return {ign, undashedUuid} for the account owning {@code token}. */
	public static String[] getProfileInfo(String token) throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(PROFILE_URL);
			request.setHeader("Authorization", "Bearer " + token);
			try (CloseableHttpResponse response = client.execute(request)) {
				String body = EntityUtils.toString(
						response.getEntity(), StandardCharsets.UTF_8);
				JsonObject json = JsonParser.parseString(body).getAsJsonObject();
				if (!json.has("name") || !json.has("id")) {
					throw new IOException("Token rejected by Mojang API");
				}
				return new String[]{
						json.get("name").getAsString(),
						json.get("id").getAsString()
				};
			}
		}
	}

	/** True only if the token resolves to the currently active session. */
	public static boolean validateSession(String token) {
		try {
			String[] info = getProfileInfo(token);
			String ign = info[0];
			UUID uuid = UUID.fromString(SessionUtils.dashUuid(info[1]));

			var session = MinecraftClient.getInstance().getSession();
			return ign.equals(session.getUsername())
					&& uuid.equals(session.getUuidOrNull());
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
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost request = new HttpPost(SKINS_URL);
			request.setHeader("Authorization", "Bearer " + token);
			request.setHeader("Content-Type", "application/json");
			request.setEntity(new StringEntity(String.format(
					"{ \"variant\": \"classic\", \"url\": \"%s\"}", url)));
			try (CloseableHttpResponse response = client.execute(request)) {
				return response.getStatusLine().getStatusCode();
			}
		} catch (Exception e) {
			return -1;
		}
	}

	public static int changeName(String newName, String token) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPut request = new HttpPut(NAME_URL + newName);
			request.setHeader("Authorization", "Bearer " + token);
			try (CloseableHttpResponse response = client.execute(request)) {
				return response.getStatusLine().getStatusCode();
			}
		} catch (Exception e) {
			return -1;
		}
	}
}
