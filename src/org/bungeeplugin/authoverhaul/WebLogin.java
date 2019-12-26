package org.bungeeplugin.authoverhaul;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.bungeeutils.OfflinePlayer;
import org.bungeeutils.io.TranslatableMessage;
import org.webutils.WebRequest;
import org.webutils.WebServer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import net.md_5.bungee.api.plugin.Plugin;

public class WebLogin extends WebServer {
	
	protected final String source;
	
	public WebLogin(Plugin plugin) {
		super(plugin);
		String s;
		try {
			s = Resources.toString(getClass().getClassLoader().getResource("website/AuthOverhaul/auth.html"), Charsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
			s = null;
		}
		source = s;
	}
	
	protected String translate(String source, String language) {
		String s = source;
		for (int i; (i = s.indexOf("%translate.")) != -1; s = s.substring(i + 11)) {
			String m = s.substring(i + 11);
			final String message = m.substring(0, m.indexOf("%"));
			String msg = new TranslatableMessage(AuthOverhaulPlugin.plugin, message).getMessage(language).replaceAll("'", "\'");
			source = source.replaceAll("%translate." + message + "%", msg);
			s = s.replaceAll("%translate." + message + "%", msg);
		}
		return source;
	}
	
	protected int checkName(String name) {
		if (new OfflinePlayer(name).isPremium()) return 3;
		if (RegisteredPlayer.getRegisteredPlayerByName(name) != null) return 2;
		else return 1;
	}

	@Override
	public void onRequest(WebRequest request) throws IOException {
		String path = request.getPath();
		String method = request.getRequestMethod();
		
		if (path.equals("/favicon.ico")) {
			if (method.equals("GET") || method.equals("HEAD")) {
				request.getResponseHeaders().set("Content-Type", "image/x-icon");
				File file = new File("server-icon.png");
				if (file.isFile()) {
					request.sendResponseHeaders(200);
					if (method.equals("GET")) request.getResponseBody().write(Files.readAllBytes(Paths.get(file.getPath())));
				} else request.sendResponseHeaders(404);
			} else request.sendResponseHeaders(405);
		} else if (path.equals("/auth")) {
			if (method.equals("GET")) {
				request.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
				request.sendResponseHeaders(200);
				request.getResponseBody().write(translate(source.replace("%style_url%", AuthOverhaulPlugin.plugin.styleUrl), request.getLanguage()).getBytes(StandardCharsets.UTF_8));
			} else if (method.equals("HEAD")) {
				request.sendResponseHeaders(200);
			} else if (method.equals("POST")) {
				Map<String, String> GET = request.get();
				Map<String, String> POST = request.post();
				if (GET.containsKey("name") && POST.containsKey("action")) {
					if (OfflinePlayer.getMojangStatus()) {
						String name = GET.get("name");
						if (OfflinePlayer.namePattern.matcher(name).matches()) {
							String action = POST.get("action");
							
							if (action.equals("check_name")) {
								int checkName = checkName(name);
								switch (checkName) {
								case 1: request.sendResponseHeaders(200);
								break;
								case 2: request.sendResponseHeaders(401);
								break;
								case 3: request.sendResponseHeaders(403);
								break;
								}
							} else if (action.equals("login") && POST.containsKey("password")) {
								if (!new OfflinePlayer(name).isPremium()) {
									RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(name);
									if (player != null && player.comparePasswords(POST.get("password"))) {
										player.setLastAddress(request.getRemoteAddress().getAddress().getHostAddress());
										request.sendResponseHeaders(200);
									} else request.sendResponseHeaders(404);
								} else request.sendResponseHeaders(403);
							} else if (action.equals("change_name") && POST.containsKey("password") && POST.containsKey("old_name")) {
								String oldName = POST.get("old_name");
								if (OfflinePlayer.namePattern.matcher(oldName).matches()) {
									int checkName = checkName(name);
									switch (checkName) {
									case 1:
										RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(name);
										if (player != null && player.comparePasswords(POST.get("password"))) {
											player.setName(name);
											player.setLastAddress(request.getRemoteAddress().getAddress().getHostAddress());
											request.sendResponseHeaders(200);
										} else request.sendResponseHeaders(404);
									break;
									case 2: request.sendResponseHeaders(401);
									break;
									case 3: request.sendResponseHeaders(403);
									break;
									}
								} else request.sendResponseHeaders(497);
							} else if (action.equals("change_password") && POST.containsKey("password") && POST.containsKey("new_password")) {
								String newPassword = POST.get("new_password");
								if (newPassword.length() >= 6) {
									if (!new OfflinePlayer(name).isPremium()) {
										RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(name);
										if (player != null && player.comparePasswords(POST.get("password"))) {
											player.setPassword(newPassword);
											player.setLastAddress(request.getRemoteAddress().getAddress().getHostAddress());
											request.sendResponseHeaders(200);
										} else request.sendResponseHeaders(404);
									} else request.sendResponseHeaders(403);
								} else request.sendResponseHeaders(498);
							} else if (action.equals("register") && POST.containsKey("password")) {
								int checkName = checkName(name);
								switch (checkName) {
								case 1:
									String password = POST.get("password");
									if (password.length() >= 6) {
										UUID uuid = UUID.randomUUID();
										RegisteredPlayer player = new RegisteredPlayer(uuid);
										player.setName(name);
										player.setPassword(password);
										player.setLastAddress(request.getRemoteAddress().getAddress().getHostAddress());
										request.sendResponseHeaders(200);
									} else request.sendResponseHeaders(498);
								break;
								case 2: request.sendResponseHeaders(401);
								break;
								case 3: request.sendResponseHeaders(403);
								break;
								}
							} else request.sendResponseHeaders(204);
							
						} else request.sendResponseHeaders(499);
					} else request.sendResponseHeaders(503);
				} else request.sendResponseHeaders(204);
			} else request.sendResponseHeaders(405);
		} else request.respond404();
		request.close();
	}
	
}