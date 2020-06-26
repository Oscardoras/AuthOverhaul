package me.oscardoras.authoverhaul;

import java.io.IOException;

import me.oscardoras.bungeeutils.BungeePlugin;
import me.oscardoras.bungeeutils.OfflinePlayer;
import me.oscardoras.bungeeutils.io.DataFile;
import me.oscardoras.webutils.BungeeWebServer;
import me.oscardoras.webutils.WebServer;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyReloadEvent;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

public final class AuthOverhaulPlugin extends BungeePlugin {
	
	public static AuthOverhaulPlugin plugin;
	
	public AuthOverhaulPlugin() {
		plugin = this;
	}
	
	
	public CrackPlayers crackPlayers;
	public String styleUrl = null;
	protected String address = null;
	protected WebServer webServer = null;
	
	@Override
	public void onEnable() {
		ProxyServer.getInstance().getPluginManager().registerListener(this, this);
		
		load();
	}
	
	@Override
	public void onDisable() {
		if (webServer != null) webServer.stop();
		ProxyServer.getInstance().getPluginManager().unregisterCommand(SetPasswordCommand.command);
	}
	
	@EventHandler
	public void onReload(ProxyReloadEvent e) {
		onDisable();
		
		load();
	}
	
	public void load() {
		DataFile file = new DataFile(this.getDataFolder() + "/config.yml");
		Configuration config = file.getAsYaml();
		
		if (!config.contains("crack_players")) config.set("crack_players", CrackPlayers.DISALLOW.name());
		file.save();
		
		try {
			crackPlayers = CrackPlayers.valueOf(config.getString("crack_players").toUpperCase());
		} catch (IllegalArgumentException ex) {
			crackPlayers = CrackPlayers.DISALLOW;
		}
		
		
		if (!BungeeCord.getInstance().config.isOnlineMode()) {
			getLogger().warning("The crack players mode " + crackPlayers.name() + " only has an effect if the online mode is set to true.");
			return;
		}
		
		if (crackPlayers == CrackPlayers.REGISTER) {
			ProxyServer.getInstance().getPluginManager().registerCommand(this, SetPasswordCommand.command);
			
			if (!config.contains("http.style_url")) config.set("http.style_url", "/style.css");
			styleUrl = config.getString("http.style_url");
			try {
				if (!config.contains("http.address")) config.set("http.address", "http://localhost:8123/");
				webServer = BungeeWebServer.newBungeeWebServer(this, new WebLogin());
				address = config.getString("http.address");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler
	public void onPreLogin(PreLoginEvent e) {
		PendingConnection connection = e.getConnection();
		String name = connection.getName();
		if (BungeeCord.getInstance().config.isOnlineMode()) {
			if (OfflinePlayer.namePattern.matcher(name).matches()) {
				OfflinePlayer offlinePlayer = new OfflinePlayer(name);
				if (OfflinePlayer.getMojangStatus()) {
					if (crackPlayers == CrackPlayers.ALLOW) {
						if (!offlinePlayer.isPremium()) {
							connection.setOnlineMode(false);
							connection.setUniqueId(OfflinePlayer.getOfflineUUID(name));
						}
					} else if (crackPlayers == CrackPlayers.REGISTER) {
						if (!offlinePlayer.isPremium()) {
							RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(name);
							if (player != null) {
								if (connection.getAddress().getAddress().getHostAddress().equals(player.getLastAddress())) {
									connection.setOnlineMode(false);
									connection.setUniqueId(player.getUUID());
								} else {
									e.setCancelled(true);
									e.setCancelReason(new TextComponent("You are login on this server from a new IP address, please login: " + address + "/auth?name=" + name));
								}
							} else {
								e.setCancelled(true);
								e.setCancelReason(new TextComponent("Your name is unknow from this server, please login or register: " + address + "/auth?name=" + name));
							}
						}
					}
				} else {
					RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(name);
					if (player != null) {
						if (connection.getAddress().getAddress().getHostAddress().equals(player.getLastAddress())) {
							connection.setOnlineMode(false);
							connection.setUniqueId(player.getUUID());
						}
					}
				}
			} else {
				e.setCancelled(true);
				e.setCancelReason(new TextComponent("A player name must be from 3 to 16 characters and can only be composed of uppercase and lowercase letters, numbers and _"));
			}
		}
	}
	
	@EventHandler
	public void onLogin(PostLoginEvent e) {
		ProxiedPlayer proxiedPlayer = e.getPlayer();
		RegisteredPlayer player = new RegisteredPlayer(proxiedPlayer);
		player.setName(proxiedPlayer.getName());
		player.setLastAddress(proxiedPlayer.getAddress().getAddress().getHostAddress());
	}
	
}