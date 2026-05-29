package dev.elv1n200.sessionlogin.util;

import dev.elv1n200.sessionlogin.SessionLogin;
import dev.elv1n200.sessionlogin.account.Account;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads accounts straight out of the Modrinth App's bundled SQLite database
 * ({@code %APPDATA%\ModrinthApp\app.db}) — the {@code minecraft_users}
 * table stores access tokens in plain text.
 */
public final class ModrinthImport {

	private ModrinthImport() {
	}

	public static Path findFile() {
		String appData = System.getenv("APPDATA");
		if (appData != null) {
			Path p = Paths.get(appData, "ModrinthApp", "app.db");
			if (Files.isRegularFile(p)) {
				return p;
			}
		}
		String home = System.getProperty("user.home");
		if (home != null) {
			Path mac = Paths.get(home, "Library", "Application Support",
					"ModrinthApp", "app.db");
			if (Files.isRegularFile(mac)) {
				return mac;
			}
			Path linux = Paths.get(home, ".config", "ModrinthApp", "app.db");
			if (Files.isRegularFile(linux)) {
				return linux;
			}
		}
		return null;
	}

	/**
	 * Reads {@code minecraft_users} via SQLite-JDBC.
	 * The DB is opened read-only so a running Modrinth App can't conflict.
	 */
	public static List<Account> read(Path file) {
		List<Account> out = new ArrayList<>();
		// On Windows, sqlite-jdbc parses `?` in the URL as part of the file
		// name (the backslashes confuse the URI parser), so pass the open
		// flags through Properties instead.
		String url = "jdbc:sqlite:" + file.toAbsolutePath();
		Properties props = new Properties();
		props.setProperty("open_mode", "1"); // SQLITE_OPEN_READONLY
		try (Connection con = DriverManager.getConnection(url, props);
			 Statement stmt = con.createStatement();
			 ResultSet rs = stmt.executeQuery(
					 "SELECT uuid, username, access_token "
							 + "FROM minecraft_users")) {
			while (rs.next()) {
				String uuid = rs.getString("uuid");
				String username = rs.getString("username");
				String token = rs.getString("access_token");
				if (uuid == null || username == null || token == null
						|| token.isEmpty()) {
					continue;
				}
				Account a = new Account(
						username, username,
						SessionUtils.dashUuid(uuid), token);
				a.touch();
				out.add(a);
			}
		} catch (Exception e) {
			SessionLogin.LOGGER.warn(
					"Could not read Modrinth App accounts from " + file, e);
		}
		return out;
	}
}
