package dev.elv1n200.sessionlogin.mixin;

import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Downloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Isolates the server-resource-pack cache per active session UUID, so a
 * server can't fingerprint your machine across alt accounts by correlating
 * cached pack files. Idea inherited from LiquidBounce / OpSec.
 *
 * <p>When {@link dev.elv1n200.sessionlogin.config.Settings#isolatePackCache()}
 * is off, the redirect is a no-op and the vanilla cache layout is used.
 */
@Mixin(Downloader.class)
public abstract class DownloaderMixin {

	@Redirect(
			method = "method_55485",
			at = @At(value = "INVOKE",
					target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"),
			require = 0)
	private Path sessionlogin$isolatePerAccount(Path original, String name) {
		if (SessionLogin.settings == null
				|| !SessionLogin.settings.isolatePackCache()) {
			return original.resolve(name);
		}
		try {
			UUID accountUuid =
					MinecraftClient.getInstance().getSession().getUuidOrNull();
			if (accountUuid == null) {
				return original.resolve(name);
			}
			return original.resolve(accountUuid.toString()).resolve(name);
		} catch (Throwable t) {
			return original.resolve(name);
		}
	}
}
