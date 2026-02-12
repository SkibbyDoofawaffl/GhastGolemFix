package com.blockbuster.ghastgolem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.IronGolem;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class GhastGolemFix extends JavaPlugin {

    private BukkitTask task;

    // config values (loaded on start/reload)
    private boolean enabled;
    private long tickInterval;
    private double radius;
    private double hitRange;
    private double damage;

    private boolean worldsEnabled;
    private List<String> allowedWorlds;

    private boolean areaEnabled;
    private String areaWorld;
    private double minX, minY, minZ, maxX, maxY, maxZ;

    private boolean stackedNameOnly;
    private List<String> nameMustContainAny;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        startTaskIfEnabled();
        getLogger().info("GhastGolemFix enabled.");
    }

    @Override
    public void onDisable() {
        stopTask();
    }

    // --------------------
    // Commands
    // --------------------
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ghastgolem")) return false;

        if (args.length == 0) {
            sender.sendMessage("§eGhastGolemFix§7: /ghastgolem <status|reload|on|off>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status" -> {
                sender.sendMessage("§eGhastGolemFix§7 status:");
                sender.sendMessage(" §7Enabled: " + (enabled ? "§aYES" : "§cNO"));
                sender.sendMessage(" §7Task running: " + (task != null ? "§aYES" : "§cNO"));
                sender.sendMessage(" §7Radius: §f" + radius + " §7HitRange: §f" + hitRange + " §7Damage: §f" + damage);
                sender.sendMessage(" §7Interval (ticks): §f" + tickInterval);
                if (worldsEnabled) sender.sendMessage(" §7World filter: §f" + allowedWorlds);
                if (areaEnabled) sender.sendMessage(" §7Area filter: §f" + areaWorld + " [" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + "]");
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("ghastgolem.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                reloadConfig();
                loadSettings();
                restartTask();
                sender.sendMessage("§aGhastGolemFix reloaded.");
                return true;
            }
            case "on" -> {
                if (!sender.hasPermission("ghastgolem.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                enabled = true;
                getConfig().set("enabled", true);
                saveConfig();
                restartTask();
                sender.sendMessage("§aGhastGolemFix enabled.");
                return true;
            }
            case "off" -> {
                if (!sender.hasPermission("ghastgolem.admin")) {
                    sender.sendMessage("§cNo permission.");
                    return true;
                }
                enabled = false;
                getConfig().set("enabled", false);
                saveConfig();
                restartTask();
                sender.sendMessage("§cGhastGolemFix disabled.");
                return true;
            }
            default -> {
                sender.sendMessage("§eGhastGolemFix§7: /ghastgolem <status|reload|on|off>");
                return true;
            }
        }
    }

    // --------------------
    // Task lifecycle
    // --------------------
    private void restartTask() {
        stopTask();
        startTaskIfEnabled();
    }

    private void startTaskIfEnabled() {
        if (!enabled) return;

        // minimum sane interval
        if (tickInterval < 1) tickInterval = 1;

        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, tickInterval);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // --------------------
    // Core logic
    // --------------------
    private void tick() {
        for (World world : Bukkit.getWorlds()) {

            // world filter
            if (worldsEnabled && (allowedWorlds == null || !allowedWorlds.contains(world.getName()))) {
                continue;
            }

            for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {

                // area filter
                if (areaEnabled && !isInArea(golem.getLocation())) continue;

                Ghast closest = null;
                double best = radius;

                for (Entity e : world.getNearbyEntities(golem.getLocation(), radius, radius, radius)) {
                    if (!(e instanceof Ghast ghast)) continue;

                    if (stackedNameOnly) {
                        String name = ghast.getCustomName();
                        if (name == null) continue;

                        boolean ok = false;
                        for (String must : nameMustContainAny) {
                            if (must != null && !must.isEmpty() && name.contains(must)) {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) continue;
                    }

                    double dist = ghast.getLocation().distance(golem.getLocation());
                    if (dist < best) {
                        best = dist;
                        closest = ghast;
                    }
                }

                if (closest == null) continue;

                // Encourage engagement (may or may not path, but harmless)
                try {
                    golem.setTarget(closest);
                } catch (Throwable ignored) { }

                // Apply damage credited to golem when in range
                if (best <= hitRange) {
                    closest.damage(damage, golem);
                }
            }
        }
    }

    private boolean isInArea(Location loc) {
        if (!areaEnabled) return true;
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(areaWorld)) return false;

        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    // --------------------
    // Config load
    // --------------------
    private void loadSettings() {
        enabled = getConfig().getBoolean("enabled", true);
        tickInterval = getConfig().getLong("tick-interval", 10L);
        radius = getConfig().getDouble("radius", 25.0);
        hitRange = getConfig().getDouble("hit-range", 3.5);
        damage = getConfig().getDouble("damage", 7.0);

        worldsEnabled = getConfig().getBoolean("world-filter.enabled", false);
        allowedWorlds = getConfig().getStringList("world-filter.worlds");

        areaEnabled = getConfig().getBoolean("area-filter.enabled", false);
        areaWorld = getConfig().getString("area-filter.world", "world");
        minX = getConfig().getDouble("area-filter.min.x", -30000000);
        minY = getConfig().getDouble("area-filter.min.y", -64);
        minZ = getConfig().getDouble("area-filter.min.z", -30000000);
        maxX = getConfig().getDouble("area-filter.max.x", 30000000);
        maxY = getConfig().getDouble("area-filter.max.y", 320);
        maxZ = getConfig().getDouble("area-filter.max.z", 30000000);

        stackedNameOnly = getConfig().getBoolean("stacked-name-only", true);
        nameMustContainAny = getConfig().getStringList("stacked-name-must-contain-any");
        if (nameMustContainAny == null || nameMustContainAny.isEmpty()) {
            nameMustContainAny = List.of("x Ghast", "× Ghast");
        }
    }
}
