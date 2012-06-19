package org.x3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	Logger log;
	Server server = Bukkit.getServer();
	PluginManager pm = server.getPluginManager();
	String motd;
	HashMap<String, ArrayList<String>> mail = null;

	public void onEnable() {
		log = this.getLogger();
		try {
			log.info("Loading mail...");
			this.mail = loadMail();
		} catch (Exception ex) {
			log.info("Mail loading failed! Generating empty map...");
			mail = new HashMap<String, ArrayList<String>>();
			save(mail);
		}
		log.info("Mail loaded!");
		pm.registerEvents(this, this);
		getConfig().options().copyDefaults(true);
		saveConfig();

		// Command: /rules
		this.getCommand("rules").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command command,
					String label, String[] args) {
				if (args.length > 0) {
					return false;
				}
				Player player = (Player) sender;
				List<String> rulesList = getConfig().getStringList("rules");
				player.sendMessage(ChatColor.AQUA
						+ "===== x3server rules =====");
				for (String rule : rulesList) {
					player.sendMessage(rule);
				}
				log.info(player.getDisplayName() + " requested a rules list.");
				return true;
			}

		});

		// Command: /motd
		this.getCommand("motd").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command command,
					String label, String[] args) {
				if (args.length > 0) {
					return false;
				}
				Player player = (Player) sender;
				String motd = getConfig().getString("motd").replace("%p",
						player.getDisplayName());
				player.sendMessage("MOTD: " + ChatColor.YELLOW
						+ motd.replaceAll("(&([a-f0-9]))", "\u00A7$2"));
				log.info(player.getDisplayName() + " requested MOTD.");
				return true;
			}

		});

		// Command: /mods
		this.getCommand("mods").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command command,
					String label, String[] args) {
				if (args.length > 0) {
					return false;
				}
				Player player = (Player) sender;
				List<String> modList = getConfig().getStringList("mod-names");
				String mods = "";
				for (String m : modList) {
					mods += m + ", ";
				}
				player.sendMessage("Mods: " + mods + ChatColor.GREEN
						+ "x3chaos (admin)");
				log.info(player.getDisplayName() + " requested a mod list.");
				return true;
			}

		});

		// Command: /mail
		this.getCommand("mail").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command command,
					String label, String[] args) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					// "/mail" || "/mail help"
					if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
						return false;
					}
					if (args.length == 1) {
						// "/mail get"
						if (args[0].equalsIgnoreCase("get")) {
							ArrayList<String> playerMail = getMail(player
									.getDisplayName().toLowerCase());
							if (playerMail != null) {
								player.sendMessage(ChatColor.GRAY
										+ "===== MAIL: "
										+ player.getDisplayName() + " =====");
								for (String s : playerMail) {
									player.sendMessage(s);
								}
								mail.remove(player.getDisplayName()
										.toLowerCase());
							} else {
								player.sendMessage(ChatColor.GREEN
										+ "You have no unread messages.");
							}
							log.info("[MAIL] " + player.getDisplayName()
									+ " read their own mail.");
							return true;
						} else if (args[0].equalsIgnoreCase("purge")) {
							if (player.hasPermission("x3.mail.purge")) {
								mail = new HashMap<String, ArrayList<String>>();
								save(mail);
								player.sendMessage(ChatColor.GREEN
										+ "Purged and reloaded mail.");
								for (Player p : server.getOnlinePlayers()) {
									p.sendMessage(ChatColor.GREEN
											+ player.getDisplayName()
											+ " purged and reloaded mail.");
								}
								log.severe("[MAIL] " + player.getDisplayName()
										+ " purged and reloaded mail!");
								return true;
							} else {
								player.sendMessage(ChatColor.RED
										+ "Wrong answer, McFly.");
								return true;
							}
						}
						return false;
					}
					// "/mail get [target]"
					if (args[0].equalsIgnoreCase("get")) {
						String target = args[1].toLowerCase();
						if (target
								.equals(player.getDisplayName().toLowerCase())) {
							player.performCommand("mail get");
							return true;
						}
						if (getMail(target) != null) {
							player.sendMessage(ChatColor.GREEN + args[1]
									+ " has " + getMail(target).size()
									+ " unread messages.");
							log.info("[MAIL] " + player.getDisplayName()
									+ " requested " + args[1]
									+ "'s unread count.");
						} else {
							player.sendMessage(ChatColor.GREEN + args[1]
									+ " has no unread messages.");
						}
						return true;
					}
					// "/mail <target> <message>"
					String target = args[0].toLowerCase();
					if (target.equals("console")) {
						player.sendMessage(ChatColor.RED
								+ "Console cannot receive mail!");
						return true;
					}
					String message = target + ": " + complete(args, 1);
					if (getMail(target) != null) {
						ArrayList<String> playerMail = getMail(target);
						playerMail.add(message);
						mail.remove(target);
						mail.put(target, playerMail);
					} else {
						ArrayList<String> playerMail = new ArrayList<String>();
						playerMail.add(message);
						mail.put(target, playerMail);
					}
					player.sendMessage(ChatColor.GREEN + "Sent message to "
							+ args[0] + "!");
					if (isOnline(target)) {
						server.getPlayer(target).sendMessage(
								ChatColor.GREEN + "You have new mail!");
					}
					return true;
				} else {
					if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
						return false;
					}
					if (args.length == 1) {
						if (args[0].equalsIgnoreCase("get")) {
							log.info("[MAIL] Console cannot receive mail!");
							return true;
						} else
							return false;
					}
					if (args[0].equalsIgnoreCase("get")) {
						String target = args[1].toLowerCase();
						if (getMail(target) != null) {
							log.info("[MAIL] " + target + "'s mail:");
							for (String s : getMail(target)) {
								log.info("[MAIL] " + s);
							}
						} else {
							log.info("[MAIL] " + target
									+ " has no unread mail.");
						}
						return true;
					}
					String target = args[0].toLowerCase();
					String message = "CONSOLE: " + complete(args, 1);
					if (getMail(target) != null) {
						ArrayList<String> playerMail = getMail(target);
						playerMail.add(message);
						mail.remove(target);
						mail.put(target, playerMail);
					} else {
						ArrayList<String> playerMail = new ArrayList<String>();
						playerMail.add(message);
						mail.put(target, playerMail);
					}
					log.info("[MAIL] Sent message to " + target);
					if (isOnline(target)) {
						server.getPlayer(target).sendMessage(
								ChatColor.GREEN + "You have new mail!");
					}
					return true;
				}
			}

		});

	}

	public ArrayList<String> getMail(String target) {
		return (mail.containsKey(target)) ? mail.get(target) : null;
	}

	public void severe(String message) {
		log.severe(message);
	}

	public void log(String message) {
		log.info(message);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		player.sendMessage(ChatColor.YELLOW
				+ getConfig().getString("motd").replace("%p",
						player.getDisplayName()));
		if (getMail(player.getDisplayName().toLowerCase()) != null) {
			int ct = getMail(player.getDisplayName().toLowerCase()).size();
			player.sendMessage(ChatColor.GREEN + "You have " + ct
					+ " unread messages.");
		}
	}

	public String complete(String[] a, int split) {
		String s = "";
		for (int i = split; i < a.length; i++) {
			s += a[i] + " ";
		}
		return s.trim();
	}

	private boolean isOnline(String player) {
		for (Player p : server.getOnlinePlayers()) {
			if (p.getDisplayName().equalsIgnoreCase(player)) {
				return true;
			}
		}
		return false;
	}

	public void save(HashMap<String, ArrayList<String>> map) {
		File file = new File(getDataFolder(), "mail");
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(file));
			oos.writeObject(map);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, ArrayList<String>> loadMail() throws IOException,
			ClassNotFoundException {
		File file = new File(getDataFolder(), "mail");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		HashMap<String, ArrayList<String>> result = (HashMap<String, ArrayList<String>>) ois
				.readObject();
		ois.close();
		return result;
	}

	@SuppressWarnings("unchecked")
	public HashMap<Location, String> loadTeleporters() throws IOException,
			ClassNotFoundException {
		File file = new File(getDataFolder(), "teleporters");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		HashMap<Location, String> result = (HashMap<Location, String>) ois
				.readObject();
		ois.close();
		return result;
	}

	public void onDisable() {
		save(this.mail);
	}

}
