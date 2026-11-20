package net.thonereach.smp;

import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public class Thonereach extends JavaPlugin implements Listener {
    private final Map<UUID, UUID> requests = new HashMap<UUID, UUID>();
    private final Map<UUID, Boolean> requestHere = new HashMap<UUID, Boolean>();
    private final Map<UUID, Set<UUID>> ignored = new HashMap<UUID, Set<UUID>>();
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> deaths = new HashMap<UUID, Integer>();

    private String prefix() { return ChatColor.DARK_AQUA + "[Starlit] " + ChatColor.GRAY; }

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() {
                Location afk = getWarp("afk");
                if (afk == null) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(afk.getWorld()) && p.getLocation().distanceSquared(afk) <= 36.0) {
                        addShards(p.getUniqueId(), 1);
                        p.sendMessage(prefix() + ChatColor.LIGHT_PURPLE + "+1 shard for being AFK.");
                        updateSidebar(p);
                    }
                }
            }
        }, 1200L, 1200L);
        getLogger().info("Starlit-SMP core enabled for Paper 1.12.2.");
    }

    @Override public void onDisable() { saveConfig(); }

    private String path(UUID id, String key) { return "players." + id.toString() + "." + key; }
    private double cash(UUID id) { return getConfig().getDouble(path(id, "cash"), getConfig().getDouble("starting-cash", 100.0)); }
    private int shards(UUID id) { return getConfig().getInt(path(id, "shards"), 0); }
    private void setCash(UUID id, double value) { getConfig().set(path(id, "cash"), value); saveConfig(); }
    private void addShards(UUID id, int value) { getConfig().set(path(id, "shards"), shards(id) + value); saveConfig(); }
    private void touch(Player p) { getConfig().set(path(p.getUniqueId(), "name"), p.getName()); }

    private Location readLocation(String base) {
        String world = getConfig().getString(base + ".world");
        if (world == null || Bukkit.getWorld(world) == null) return null;
        return new Location(Bukkit.getWorld(world), getConfig().getDouble(base + ".x"),
            getConfig().getDouble(base + ".y"), getConfig().getDouble(base + ".z"),
            (float)getConfig().getDouble(base + ".yaw"), (float)getConfig().getDouble(base + ".pitch"));
    }
    private void writeLocation(String base, Location l) {
        getConfig().set(base + ".world", l.getWorld().getName());
        getConfig().set(base + ".x", l.getX()); getConfig().set(base + ".y", l.getY());
        getConfig().set(base + ".z", l.getZ()); getConfig().set(base + ".yaw", l.getYaw());
        getConfig().set(base + ".pitch", l.getPitch()); saveConfig();
    }
    private Location getWarp(String name) { return readLocation("warps." + name.toLowerCase()); }

    private void updateSidebar(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("starlit", "dummy");
        obj.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "STARLIT SMP");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.getScore(ChatColor.GRAY + " ").setScore(6);
        obj.getScore(ChatColor.YELLOW + "Cash: " + ChatColor.WHITE + "$" + String.format(Locale.US, "%.2f", cash(p.getUniqueId()))).setScore(5);
        obj.getScore(ChatColor.LIGHT_PURPLE + "Shards: " + ChatColor.WHITE + shards(p.getUniqueId())).setScore(4);
        obj.getScore(ChatColor.GRAY + "  ").setScore(3);
        obj.getScore(ChatColor.AQUA + "Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(2);
        obj.getScore(ChatColor.DARK_GRAY + "starlit-smp").setScore(1);
        p.setScoreboard(board);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) {
        touch(e.getPlayer()); saveConfig(); updateSidebar(e.getPlayer());
    }
    @EventHandler public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        deaths.put(victim.getUniqueId(), deaths.containsKey(victim.getUniqueId()) ? deaths.get(victim.getUniqueId()) + 1 : 1);
        Player killer = victim.getKiller();
        if (killer != null) kills.put(killer.getUniqueId(), kills.containsKey(killer.getUniqueId()) ? kills.get(killer.getUniqueId()) + 1 : 1);
    }

    private void tell(CommandSender s, String msg) { s.sendMessage(prefix() + msg); }
    private boolean playerOnly(CommandSender s) { if (!(s instanceof Player)) { tell(s, "Players only."); return false; } return true; }
    private void teleport(Player p, Location l, String message) {
        if (l == null) { tell(p, "That location has not been set."); return; }
        p.teleport(l); tell(p, message); updateSidebar(p);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String c = command.getName().toLowerCase(Locale.ROOT);
        Player p = sender instanceof Player ? (Player)sender : null;

        if (c.equals("bal")) {
            if (!playerOnly(sender)) return true;
            tell(p, "Cash: " + ChatColor.GREEN + "$" + String.format(Locale.US, "%.2f", cash(p.getUniqueId()))); return true;
        }
        if (c.equals("shards")) {
            if (!playerOnly(sender)) return true;
            tell(p, "Shards: " + ChatColor.LIGHT_PURPLE + shards(p.getUniqueId())); return true;
        }
        if (c.equals("pay")) {
            if (!playerOnly(sender)) return true;
            if (args.length != 2) { tell(p, "Usage: /pay <player> <amount>"); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || target.equals(p)) { tell(p, "Player not found (or you selected yourself)."); return true; }
            double amount;
            try { amount = Double.parseDouble(args[1]); } catch (Exception ex) { tell(p, "Enter a valid amount."); return true; }
            if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount) || amount > cash(p.getUniqueId())) { tell(p, "Invalid amount or insufficient cash."); return true; }
            setCash(p.getUniqueId(), cash(p.getUniqueId()) - amount);
            setCash(target.getUniqueId(), cash(target.getUniqueId()) + amount);
            tell(p, "Paid " + target.getName() + " $" + String.format(Locale.US, "%.2f", amount) + ".");
            tell(target, "Received $" + String.format(Locale.US, "%.2f", amount) + " from " + p.getName() + ".");
            updateSidebar(p); updateSidebar(target); return true;
        }
        if (c.equals("setspawn")) {
            if (!sender.hasPermission("starlit.admin") || !playerOnly(sender)) { tell(sender, "No permission."); return true; }
            writeLocation("spawn", p.getLocation()); tell(p, "Spawn saved."); return true;
        }
        if (c.equals("spawn")) {
            if (!playerOnly(sender)) return true;
            teleport(p, readLocation("spawn"), "Teleported to spawn."); return true;
        }
        if (c.equals("sethome") || c.equals("home") || c.equals("delhome")) {
            if (!playerOnly(sender)) return true;
            String base = "players." + p.getUniqueId() + ".home";
            if (c.equals("sethome")) { writeLocation(base, p.getLocation()); tell(p, "Home saved."); }
            else if (c.equals("delhome")) { getConfig().set(base, null); saveConfig(); tell(p, "Home deleted."); }
            else teleport(p, readLocation(base), "Welcome home.");
            return true;
        }
        if (c.equals("setwarp")) {
            if (!sender.hasPermission("starlit.admin") || !playerOnly(sender)) { tell(sender, "No permission."); return true; }
            if (args.length != 1) { tell(p, "Usage: /setwarp <name>"); return true; }
            writeLocation("warps." + args[0].toLowerCase(), p.getLocation()); tell(p, "Warp '" + args[0] + "' saved."); return true;
        }
        if (c.equals("warp")) {
            if (!playerOnly(sender)) return true;
            if (args.length != 1) { tell(p, "Usage: /warp <name>"); return true; }
            teleport(p, getWarp(args[0]), "Teleported to " + args[0] + "."); return true;
        }
        if (c.equals("tpa") || c.equals("tpahere")) {
            if (!playerOnly(sender)) return true;
            if (args.length != 1) { tell(p, "Usage: /" + c + " <player>"); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || target.equals(p)) { tell(p, "Player not found (or you selected yourself)."); return true; }
            requests.put(target.getUniqueId(), p.getUniqueId()); requestHere.put(target.getUniqueId(), c.equals("tpahere"));
            tell(target, p.getName() + (c.equals("tpahere") ? " wants you to teleport to them" : " wants to teleport to you") + ". Use /tpaccept or /tpdeny.");
            tell(p, "Teleport request sent to " + target.getName() + ".");
            Bukkit.getScheduler().runTaskLater(this, new Runnable() { public void run() {
                if (requests.get(target.getUniqueId()) != null && requests.get(target.getUniqueId()).equals(p.getUniqueId())) {
                    requests.remove(target.getUniqueId()); requestHere.remove(target.getUniqueId());
                    if (p.isOnline()) tell(p, "Your teleport request expired.");
                    if (target.isOnline()) tell(target, "Teleport request expired.");
                }
            }}, 1200L);
            return true;
        }
        if (c.equals("tpaccept") || c.equals("tpdeny")) {
            if (!playerOnly(sender)) return true;
            UUID from = requests.remove(p.getUniqueId());
            Boolean here = requestHere.remove(p.getUniqueId());
            if (from == null) { tell(p, "You have no pending request."); return true; }
            Player requester = Bukkit.getPlayer(from);
            if (c.equals("tpdeny") || requester == null) { tell(p, "Request denied."); if (requester != null) tell(requester, "Your request was denied."); return true; }
            if (Boolean.TRUE.equals(here)) requester.teleport(p.getLocation()); else p.teleport(requester.getLocation());
            tell(p, "Teleport request accepted."); tell(requester, "Teleport request accepted."); return true;
        }
        if (c.equals("msg")) {
            if (args.length < 2) { tell(sender, "Usage: /msg <player> <message>"); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { tell(sender, "Player not found."); return true; }
            if (p != null && ignored.containsKey(target.getUniqueId()) && ignored.get(target.getUniqueId()).contains(p.getUniqueId())) { tell(p, "That player is ignoring you."); return true; }
            String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            target.sendMessage(ChatColor.LIGHT_PURPLE + "[From " + sender.getName() + "] " + message);
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "[To " + target.getName() + "] " + message); return true;
        }
        if (c.equals("ignore")) {
            if (!playerOnly(sender)) return true;
            if (args.length != 1) { tell(p, "Usage: /ignore <player>"); return true; }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { tell(p, "Player not found; they must be online to ignore them."); return true; }
            Set<UUID> set = ignored.get(p.getUniqueId());
            if (set == null) { set = new HashSet<UUID>(); ignored.put(p.getUniqueId(), set); }
            if (set.remove(target.getUniqueId())) tell(p, "No longer ignoring " + target.getName() + ".");
            else { set.add(target.getUniqueId()); tell(p, "Now ignoring " + target.getName() + "."); }
            return true;
        }
        if (c.equals("afk")) {
            if (!playerOnly(sender)) return true;
            teleport(p, getWarp("afk"), "Welcome to the AFK zone. Earn 1 shard per minute while nearby."); return true;
        }
        if (c.equals("rtp")) {
            if (!playerOnly(sender)) return true;
            World w = p.getWorld();
            int x = (int)(Math.random() * 2001) - 1000, z = (int)(Math.random() * 2001) - 1000;
            int y = w.getHighestBlockYAt(x, z) + 1;
            Location dest = new Location(w, x + 0.5, y, z + 0.5);
            if (dest.getBlock().isLiquid() || dest.getBlock().getType() == Material.AIR && dest.clone().add(0,-1,0).getBlock().isLiquid()) { tell(p, "Unsafe spot. Try /rtp again."); return true; }
            teleport(p, dest, "Randomly teleported."); return true;
        }
        if (c.equals("baltop")) {
            if (!playerOnly(sender)) return true;
            tell(p, ChatColor.GOLD + "Top balances (saved players):");
            if (getConfig().getConfigurationSection("players") != null) {
                List<String> rows = new ArrayList<String>();
                for (String id : getConfig().getConfigurationSection("players").getKeys(false)) {
                    String name = getConfig().getString("players." + id + ".name", id.substring(0, 8));
                    rows.add(name + ": $" + String.format(Locale.US, "%.2f", getConfig().getDouble("players." + id + ".cash", 100.0)));
                }
                Collections.sort(rows, Collections.reverseOrder());
                for (int i=0; i<Math.min(5, rows.size()); i++) tell(p, (i+1) + ". " + rows.get(i));
            }
            return true;
        }
        if (c.equals("stats")) {
            if (!playerOnly(sender)) return true;
            tell(p, ChatColor.AQUA + "Kills: " + kills.containsKey(p.getUniqueId()) + " | Deaths: " + deaths.containsKey(p.getUniqueId()));
            tell(p, "Cash: $" + String.format(Locale.US, "%.2f", cash(p.getUniqueId())) + " | Shards: " + shards(p.getUniqueId())); return true;
        }
        if (c.equals("sell") || c.equals("shop")) {
            if (!playerOnly(sender)) return true;
            if (c.equals("shop")) { tell(p, "Sell prices: cobblestone $0.10, dirt $0.05, oak_log $1.00, iron_ingot $3.00, gold_ingot $5.00, diamond $25.00. Use /sell all."); return true; }
            if (args.length != 1 || !args[0].equalsIgnoreCase("all")) { tell(p, "Usage: /sell all (supported items only)"); return true; }
            Map<Material, Double> prices = new LinkedHashMap<Material, Double>();
            prices.put(Material.COBBLESTONE, 0.10); prices.put(Material.DIRT, 0.05);
            prices.put(Material.LOG, 1.00); prices.put(Material.IRON_INGOT, 3.00);
            prices.put(Material.GOLD_INGOT, 5.00); prices.put(Material.DIAMOND, 25.00);
            double total = 0;
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null || !prices.containsKey(item.getType())) continue;
                total += prices.get(item.getType()) * item.getAmount(); item.setAmount(0);
            }
            if (total <= 0) { tell(p, "No supported items to sell."); return true; }
            setCash(p.getUniqueId(), cash(p.getUniqueId()) + total);
            tell(p, "Sold items for $" + String.format(Locale.US, "%.2f", total) + "."); updateSidebar(p); return true;
        }
        if (c.equals("phantom")) { tell(sender, "Phantoms were added in Minecraft 1.13; this server is 1.12.2, so this toggle cannot work here."); return true; }
        if (c.equals("settings")) { tell(sender, "Settings: use /ignore <player> to manage ignored players. More menu options are coming."); return true; }
        if (c.equals("ah") || c.equals("orders")) { tell(sender, "This feature is not enabled in the current core build yet."); return true; }
        if (c.equals("lobby")) { tell(sender, "Lobby switching needs a configured network proxy; this single-server setup has no lobbies configured."); return true; }
        return false;
    }
}