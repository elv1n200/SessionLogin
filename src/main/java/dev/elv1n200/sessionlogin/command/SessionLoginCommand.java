package dev.elv1n200.sessionlogin.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;
import dev.elv1n200.sessionlogin.util.Notifier;
import dev.elv1n200.sessionlogin.util.SessionUtils;
import dev.elv1n200.sessionlogin.util.TokenUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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

			dispatcher.register(ClientCommands.literal("sl")
					.executes(ctx -> {
						help(ctx.getSource());
						return 1;
					})
					.then(ClientCommands.literal("list").executes(ctx -> {
						list(ctx.getSource());
						return 1;
					}))
					.then(ClientCommands.literal("current").executes(ctx -> {
						current(ctx.getSource());
						return 1;
					}))
					.then(ClientCommands.literal("restore").executes(ctx -> {
						SessionUtils.restoreSession();
						send(ctx.getSource(),
								Component.literal("Restored original session")
										.withStyle(ChatFormatting.GREEN));
						return 1;
					}))
					.then(ClientCommands.literal("switch").then(
							ClientCommands.argument(
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
		send(src, Component.literal("/sl list | current | switch <label> | restore")
				.withStyle(ChatFormatting.GRAY));
	}

	private static void list(FabricClientCommandSource src) {
		if (SessionLogin.accountStore.accounts().isEmpty()) {
			send(src, Component.literal("No saved accounts").withStyle(ChatFormatting.GRAY));
			return;
		}
		for (Account a : SessionLogin.accountStore.accounts()) {
			String tag = a.isOffline() ? " [offline]"
					: " [" + TokenUtils.expiryLabel(a.token()) + "]";
			send(src, Component.literal("- " + a.label() + " (" + a.username() + ")")
					.withStyle(ChatFormatting.WHITE)
					.append(Component.literal(tag).withStyle(ChatFormatting.DARK_GRAY)));
		}
	}

	private static void current(FabricClientCommandSource src) {
		String name = SessionUtils.getUsername();
		boolean original = SessionUtils.isOriginalActive();
		Component t = Component.literal("Active: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(name).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(original ? " (original)" : " (swapped)")
						.withStyle(original ? ChatFormatting.GRAY : ChatFormatting.GREEN));
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
					send(src, Component.literal("Vault is locked")
							.withStyle(ChatFormatting.RED));
					return;
				}
				a.touch();
				SessionLogin.accountStore.save();
				Notifier.loggedIn(a.username());
				send(src, Component.literal("Switched to " + a.username())
						.withStyle(ChatFormatting.GREEN));
				return;
			}
		}
		send(src, Component.literal("No account named '" + label + "'")
				.withStyle(ChatFormatting.RED));
	}

	private static void send(FabricClientCommandSource src, Component msg) {
		src.sendFeedback(Component.literal("[SL] ").withStyle(ChatFormatting.GOLD)
				.append(msg));
	}
}
