package dev.elv1n200.sessionlogin.util;

import net.minecraft.resources.Identifier;

/**
 * Player-head cache.
 *
 * <p><b>26.x note:</b> the texture-upload pipeline (NativeImage /
 * NativeImageBackedTexture / TextureManager) changed substantially in the
 * unobfuscated era. Rendered player heads are a purely cosmetic touch, so for
 * the 26.x port this returns {@code null} and screens draw their letter/box
 * fallback instead. The Crafatar-based head fetch can be restored later.
 */
public final class SkinCache {

	private SkinCache() {
	}

	/** @return always null on 26.x — callers draw a fallback avatar. */
	public static Identifier head(String uuidString) {
		return null;
	}
}
