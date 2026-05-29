package dev.elv1n200.sessionlogin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.Notifier;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side {@code /sl} command for quick session control without opening
 * the GUI. Subcommands: list, current, switch &lt;label&gt;, restore.
 */
public final class SessionLoginCommand {

	private SessionLoginCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, ra) -> {
			SuggestionProvider<FabricClientCommandSource> labelSuggestions =
					(ctx, builder) -> {
						for (Account a : SessionLogin.accountStore.accounts()) {
							builder.suggest(a.label());
						}
						return builder.buildFuture();
					};

			dispatcher.register(ClientCommandManager.literal("sl")
					.executes(ctx -> {
						help(ctx.getSource());
						return 1;
					})
					.then(ClientCommandManager.literal("list").executes(ctx -> {
						list(ctx.getSource());
						return 1;
					}))
					.then(ClientCommandManager.literal("current").executes(ctx -> {
						current(ctx.getSource());
						return 1;
					}))
					.then(ClientCommandManager.literal("restore").executes(ctx -> {
						SessionUtils.restoreSession();
						send(ctx.getSource(),
								Text.literal("Restored original session")
										.formatted(Formatting.GREEN));
						return 1;
					}))
					.then(ClientCommandManager.literal("switch").then(
							ClientCommandManager.argument(
									"label", StringArgumentType.greedyString())
									.suggests(labelSuggestions)
									.executes(ctx -> {
										String label = StringArgumentType.getString(ctx, "label");
										switchTo(ctx.getSource(), label);
										return 1;
									}))));
		});
	}

	private static void help(FabricClientCommandSource src) {
		send(src, Text.literal("/sl list | current | switch <label> | restore")
				.formatted(Formatting.GRAY));
	}

	private static void list(FabricClientCommandSource src) {
		if (SessionLogin.accountStore.accounts().isEmpty()) {
			send(src, Text.literal("No saved accounts").formatted(Formatting.GRAY));
			return;
		}
		for (Account a : SessionLogin.accountStore.accounts()) {
			String tag = a.isOffline() ? " [offline]"
					: " [" + TokenUtils.expiryLabel(a.token()) + "]";
			send(src, Text.literal("- " + a.label() + " (" + a.username() + ")")
					.formatted(Formatting.WHITE)
					.append(Text.literal(tag).formatted(Formatting.DARK_GRAY)));
		}
	}

	private static void current(FabricClientCommandSource src) {
		String name = SessionUtils.getUsername();
		boolean original = SessionUtils.isOriginalActive();
		Text t = Text.literal("Active: ").formatted(Formatting.GRAY)
				.append(Text.literal(name).formatted(Formatting.WHITE))
				.append(Text.literal(original ? " (original)" : " (swapped)")
						.formatted(original ? Formatting.GRAY : Formatting.GREEN));
		send(src, t);
	}

	private static void switchTo(FabricClientCommandSource src, String label) {
		for (Account a : SessionLogin.accountStore.accounts()) {
			if (a.label().equalsIgnoreCase(label)
					|| a.username().equalsIgnoreCase(label)) {
				if (a.isOffline()) {
					SessionUtils.setSession(SessionUtils.createSession(
							a.username(), a.uuid(), ""));
				} else if (a.hasToken()) {
					SessionUtils.setSession(SessionUtils.createSession(
							a.username(), a.uuid(), a.token()));
				} else {
					send(src, Text.literal("Vault is locked")
							.formatted(Formatting.RED));
					return;
				}
				a.touch();
				SessionLogin.accountStore.save();
				Notifier.loggedIn(a.username());
				send(src, Text.literal("Switched to " + a.username())
						.formatted(Formatting.GREEN));
				return;
			}
		}
		send(src, Text.literal("No account named '" + label + "'")
				.formatted(Formatting.RED));
	}

	private static void send(FabricClientCommandSource src, Text msg) {
		src.sendFeedback(Text.literal("[SL] ").formatted(Formatting.GOLD)
				.append(msg));
	}
}
