package dev.elv1n200.sessionlogin.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.elv1n200.sessionlogin.SessionLogin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Privacy/QoL toggles persisted to {@code config/sessionlogin/settings.json}.
 * Read directly by the mixins so flips take effect immediately for new events.
 */
public final class Settings {

	private final Path file;

	private boolean isolatePackCache = true;
	private boolean spoofBrandAsVanilla = false;
	private boolean blockTelemetry = true;
	private boolean showToasts = true;

	public Settings(Path dir) {
		this.file = dir.resolve("settings.json");
	}

	public boolean isolatePackCache() {
		return isolatePackCache;
	}

	public boolean spoofBrandAsVanilla() {
		return spoofBrandAsVanilla;
	}

	public boolean blockTelemetry() {
		return blockTelemetry;
	}

	public boolean showToasts() {
		return showToasts;
	}

	public void setIsolatePackCache(boolean v) {
		isolatePackCache = v;
		save();
	}

	public void setSpoofBrandAsVanilla(boolean v) {
		spoofBrandAsVanilla = v;
		save();
	}

	public void setBlockTelemetry(boolean v) {
		blockTelemetry = v;
		save();
	}

	public void setShowToasts(boolean v) {
		showToasts = v;
		save();
	}

	public void load() {
		try {
			if (!Files.exists(file)) {
				save();
				return;
			}
			JsonObject o = JsonParser.parseString(Files.readString(file))
					.getAsJsonObject();
			if (o.has("isolatePackCache")) {
				isolatePackCache = o.get("isolatePackCache").getAsBoolean();
			}
			if (o.has("spoofBrandAsVanilla")) {
				spoofBrandAsVanilla = o.get("spoofBrandAsVanilla").getAsBoolean();
			}
			if (o.has("blockTelemetry")) {
				blockTelemetry = o.get("blockTelemetry").getAsBoolean();
			}
			if (o.has("showToasts")) {
				showToasts = o.get("showToasts").getAsBoolean();
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not read settings.json", e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			JsonObject o = new JsonObject();
			o.addProperty("isolatePackCache", isolatePackCache);
			o.addProperty("spoofBrandAsVanilla", spoofBrandAsVanilla);
			o.addProperty("blockTelemetry", blockTelemetry);
			o.addProperty("showToasts", showToasts);
			Files.writeString(file, o.toString());
		} catch (Exception e) {
			SessionLogin.LOGGER.warn("Could not write settings.json", e);
		}
	}
}
