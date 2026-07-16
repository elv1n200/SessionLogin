package dev.elv1n200.sessionlogin.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import dev.elv1n200.sessionlogin.SessionLogin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
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

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

	@Shadow
	@Final
	public File gameDirectory;

	@Unique
	private UUID sessionlogin$lastUuid = null;

	@Unique
	private String sessionlogin$lastToken = null;

	@Unique
	private ProfileKeyPairManager sessionlogin$cachedKeys = null;

	@Inject(method = "getUser", at = @At("HEAD"), cancellable = true)
	private void sessionlogin$onGetUser(CallbackInfoReturnable<User> cir) {
		if (!SessionLogin.overrideSession) {
			return;
		}
		cir.setReturnValue(SessionLogin.currentSession);
	}

	@Inject(method = "getProfileKeyPairManager", at = @At("HEAD"), cancellable = true)
	private void sessionlogin$onGetProfileKeys(
			CallbackInfoReturnable<ProfileKeyPairManager> cir) {
		if (!SessionLogin.overrideSession) {
			return;
		}

		User user = SessionLogin.currentSession;
		UUID uuid = user.getProfileId();
		String token = user.getAccessToken();

		boolean stale = sessionlogin$lastUuid == null
				|| !sessionlogin$lastUuid.equals(uuid)
				|| sessionlogin$lastToken == null
				|| !sessionlogin$lastToken.equals(token);

		if (stale) {
			sessionlogin$lastUuid = uuid;
			sessionlogin$lastToken = token;
			try {
				Path profileKeysPath = gameDirectory.toPath().resolve("profilekeys");
				sessionlogin$cachedKeys = ProfileKeyPairManager.create(
						UserApiService.OFFLINE, user, profileKeysPath);
			} catch (Exception e) {
				sessionlogin$cachedKeys = null;
			}
		}

		if (sessionlogin$cachedKeys != null) {
			cir.setReturnValue(sessionlogin$cachedKeys);
		}
	}
}
