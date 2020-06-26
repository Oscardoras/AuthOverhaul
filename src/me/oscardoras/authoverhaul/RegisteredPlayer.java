package me.oscardoras.authoverhaul;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.oscardoras.bungeeutils.OfflinePlayer;
import me.oscardoras.bungeeutils.io.DataFile;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class RegisteredPlayer {
	
	private final static DataFile file = new DataFile("players.yml");
	
	
	protected final UUID uuid;
	
	public RegisteredPlayer(UUID uuid) {
		this.uuid = uuid;
	}
	
	public RegisteredPlayer(ProxiedPlayer player) {
		this.uuid = player.getUniqueId();
	}
	
	public UUID getUUID() {
		return uuid;
	}
	
	public String getName() {
		Configuration config = file.getAsYaml();
		if (config.contains(uuid.toString() + ".name")) return config.getString(uuid.toString() + ".name");
		return null;
	}
	
	public boolean setName(String name) {
		if (OfflinePlayer.namePattern.matcher(name).matches()) {
			Configuration config = file.getAsYaml();
			config.set(uuid.toString() + ".name", name);
			file.save();
			return true;
		} else return false;
	}
	
	public String getLastAddress() {
		Configuration config = file.getAsYaml();
		if (config.contains(uuid.toString() + ".last_address")) return config.getString(uuid.toString() + ".last_address");
		return null;
	}
	
	public void setLastAddress(String lastAddress) {
		Configuration config = file.getAsYaml();
		config.set(uuid.toString() + ".last_address", lastAddress);
		file.save();
	}
	
	public boolean comparePasswords(String password) {
		Configuration config = file.getAsYaml();
		if (config.contains(uuid.toString() + ".password")) return config.getString(uuid.toString() + ".password").equals(sha256(password, uuid));
		return false;
	}
	
	public boolean setPassword(String password) {
		if (password.length() >= 6) {
			Configuration config = file.getAsYaml();
			config.set(uuid.toString() + ".password", sha256(password, uuid));
			file.save();
			return true;
		} return false;
	}
	
	public static List<RegisteredPlayer> getRegisteredPlayers() {
		List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
		Configuration config = file.getAsYaml();
		for (String player : config.getKeys()) {
			try {
				UUID uuid = UUID.fromString(player);
				if (uuid.equals(uuid)) players.add(new RegisteredPlayer(uuid));
			} catch (IllegalArgumentException ex) {}
		}
		return players;
	}
	
	public static RegisteredPlayer getRegisteredPlayerByName(String name) {
		if (name != null) for (RegisteredPlayer player : getRegisteredPlayers()) if (name.equals(player.getName())) return player;
		return null;
	}
	
	@Override
	public boolean equals(Object object) {
		if (object != null) {
			if (object instanceof RegisteredPlayer) return uuid.equals(((RegisteredPlayer) object).uuid);
			if (object instanceof OfflinePlayer) return uuid.equals(((OfflinePlayer) object).getUUID());
			if (object instanceof ProxiedPlayer) return uuid.equals(((ProxiedPlayer) object).getUniqueId());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
	
	protected static String sha256(String password, UUID uuid) {
		StringBuilder hashString = new StringBuilder();
		if (password != null) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
				messageDigest.update((password + uuid.toString() + "BungeeCord").getBytes());
				byte[] hash = messageDigest.digest();
				
				for (int i = 0; i < hash.length; i++) {
					String hex = Integer.toHexString(hash[i]);
					if (hex.length() == 1) {
						hashString.append('0');
						hashString.append(hex.charAt(hex.length() - 1));
					} else {
						hashString.append(hex.substring(hex.length() - 2));
					}
				}
			} catch (NoSuchAlgorithmException ex) {
				ex.printStackTrace();
			}
		}
		return hashString.toString();
	}
	
}