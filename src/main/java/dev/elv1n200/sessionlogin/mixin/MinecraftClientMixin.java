package dev.elv1n200.sessionlogin.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Shadow
	@Final
	public File runDirectory;

	@Unique
	private UUID sessionlogin$lastUuid = null;

	@Unique
	private String sessionlogin$lastToken = null;

	@Unique
	private ProfileKeys sessionlogin$cachedKeys = null;

	@Inject(method = "getSession", at = @At("HEAD"), cancellable = true)
	private void sessionlogin$onGetSession(CallbackInfoReturnable<Session> cir) {
		if (!SessionLogin.overrideSession) {
			return;
		}
		cir.setReturnValue(SessionLogin.currentSession);
	}

	@Inject(method = "getProfileKeys", at = @At("HEAD"), cancellable = true)
	private void sessionlogin$onGetProfileKeys(CallbackInfoReturnable<ProfileKeys> cir) {
		if (!SessionLogin.overrideSession) {
			return;
		}

		Session session = SessionLogin.currentSession;
		UUID uuid = session.getUuidOrNull();
		String token = session.getAccessToken();

		boolean stale = sessionlogin$lastUuid == null
				|| !sessionlogin$lastUuid.equals(uuid)
				|| sessionlogin$lastToken == null
				|| !sessionlogin$lastToken.equals(token);

		if (stale) {
			sessionlogin$lastUuid = uuid;
			sessionlogin$lastToken = token;
			try {
				Path profileKeysPath = runDirectory.toPath().resolve("profilekeys");
				sessionlogin$cachedKeys = ProfileKeys.create(
						UserApiService.OFFLINE, session, profileKeysPath);
			} catch (Exception e) {
				sessionlogin$cachedKeys = null;
			}
		}

		if (sessionlogin$cachedKeys != null) {
			cir.setReturnValue(sessionlogin$cachedKeys);
		}
	}
}
