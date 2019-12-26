package org.bungeeplugin.authoverhaul;

import java.util.ArrayList;
import java.util.List;

import org.bungeeutils.BungeeCommand;
import org.bungeeutils.io.SendMessage;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class SetPasswordCommand extends BungeeCommand {
	
	public final static SetPasswordCommand command = new SetPasswordCommand();
	
	
	public SetPasswordCommand() {
		super("setpassword", "authoverhaul.command.setpassword");
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length >= 2) {
			RegisteredPlayer player = RegisteredPlayer.getRegisteredPlayerByName(args[0]);
			if (player != null) {
				if (args[1].length() >= 6) {
					player.setPassword(args[1]);
					SendMessage.send(sender, "Password changed");
				} else SendMessage.send(sender, ChatColor.RED + "Password must be at least 6 characters");
			} else SendMessage.send(sender, ChatColor.RED + "Player not found");
		} else SendMessage.send(sender, ChatColor.RED + "Not enough arguments, usage: /setpassword <player> <password>");
	}
	
	@Override
	public List<String> complete(CommandSender sender, String[] args) {
		List<String> list = new ArrayList<String>();
		if (args.length == 1) {
			for (RegisteredPlayer player : RegisteredPlayer.getRegisteredPlayers()) {
				list.add(player.getName());
			}
		}
		return list;
	}
	
}