
package net.thonereach.smp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Thonereach extends JavaPlugin implements Listener {

    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Location> homes = new HashMap<>();

    private String prefix() {
        return ChatColor.DARK_AQUA + "[Thonereach] " + ChatColor.GRAY;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Thonereach SMP is starting!");
    }

    private double balance(UUID id) {
        if (!balances.containsKey(id)) {
            balances.put(id, getConfig().getDouble("starting-balance", 100.0));
        }
        return balances.get(id);
    }

    @Override
    public boolean onCommand(
            org.bukkit.command.CommandSender sender,
            org.bukkit.command.Command command,
            String label,
            String[] args) {

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("balance")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            p.sendMessage(prefix() + "Balance: $" +
                    String.format("%.2f", balance(p.getUniqueId())));
            return true;
        }

        if (cmd.equals("pay")) {
            if (!(sender instanceof Player) || args.length != 2) {
                sender.sendMessage(prefix() + "Usage: /pay <player> <amount>");
                return true;
            }

            Player p = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                p.sendMessage(prefix() + "Player not found.");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(prefix() + "Enter a valid amount.");
                return true;
            }

            if (amount <= 0 || Double.isInfinite(amount) ||
                    Double.isNaN(amount)) {
                p.sendMessage(prefix() + "Amount must be positive.");
                return true;
            }

            if (balance(p.getUniqueId()) < amount) {
                p.sendMessage(prefix() + "Not enough money.");
                return true;
            }

            balances.put(p.getUniqueId(), balance(p.getUniqueId()) - amount);
            balances.put(target.getUniqueId(), balance(target.getUniqueId()) + amount);

            p.sendMessage(prefix() + "You paid " + target.getName() +
                    " $" + String.format("%.2f", amount));
            target.sendMessage(prefix() + "You received $" +
                    String.format("%.2f", amount) + " from " + p.getName());
            return true;
        }

        if (cmd.equals("setspawn")) {
            if (!(sender instanceof Player) ||
                    !sender.hasPermission("thonereach.admin")) {
                sender.sendMessage(prefix() + "No permission.");
                return true;
            }

            Location loc = ((Player) sender).getLocation();
            getConfig().set("spawn.world", loc.getWorld().getName());
            getConfig().set("spawn.x", loc.getX());
            getConfig().set("spawn.y", loc.getY());
            getConfig().set("spawn.z", loc.getZ());
            getConfig().set("spawn.yaw", loc.getYaw());
            getConfig().set("spawn.pitch", loc.getPitch());
            saveConfig();

            sender.sendMessage(prefix() + "Spawn saved!");
            return true;
        }

        if (cmd.equals("spawn")) {
            if (!(sender instanceof Player)) return true;

            String world = getConfig().getString("spawn.world");
            if (world == null || Bukkit.getWorld(world) == null) {
                sender.sendMessage(prefix() + "Spawn has not been set.");
                return true;
            }

            Location loc = new Location(
                    Bukkit.getWorld(world),
                    getConfig().getDouble("spawn.x"),
                    getConfig().getDouble("spawn.y"),
                    getConfig().getDouble("spawn.z"),
                    (float) getConfig().getDouble("spawn.yaw"),
                    (float) getConfig().getDouble("spawn.pitch")
            );

            ((Player) sender).teleport(loc);
            sender.sendMessage(prefix() + "Teleported to spawn!");
            return true;
        }

        if (cmd.equals("sethome")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            homes.put(p.getUniqueId(), p.getLocation());
            p.sendMessage(prefix() + "Home saved!");
            return true;
        }

        if (cmd.equals("home")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            Location home = homes.get(p.getUniqueId());

            if (home == null) {
                p.sendMessage(prefix() + "You have no home set.");
                return true;
            }

            p.teleport(home);
            p.sendMessage(prefix() + "Welcome home!");
            return true;
        }

        if (cmd.equals("stats")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            UUID id = p.getUniqueId();

            int k = kills.containsKey(id) ? kills.get(id) : 0;
            int d = deaths.containsKey(id) ? deaths.get(id) : 0;

            p.sendMessage(ChatColor.AQUA + "---- THONEREACH STATS ----");
            p.sendMessage(ChatColor.GRAY + "Kills: " + k);
            p.sendMessage(ChatColor.GRAY + "Deaths: " + d);
            p.sendMessage(ChatColor.GRAY + "Balance: $" +
                    String.format("%.2f", balance(id)));
            return true;
        }

        if (cmd.equals("msg")) {
            if (!(sender instanceof Player) || args.length < 2) {
                sender.sendMessage(prefix() + "Usage: /msg <player> <message>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix() + "Player not found.");
                return true;
            }

            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) message.append(" ");
                message.append(args[i]);
            }

            target.sendMessage(ChatColor.LIGHT_PURPLE + "[From " +
                    sender.getName() + "] " + message);
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[To " +
                    target.getName() + "] " + message);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        deaths.put(victimId,
                deaths.containsKey(victimId) ? deaths.get(victimId) + 1 : 1);

        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerId = killer.getUniqueId();
            kills.put(killerId,
                    kills.containsKey(killerId) ? kills.get(killerId) + 1 : 1);
        }
    }
}
