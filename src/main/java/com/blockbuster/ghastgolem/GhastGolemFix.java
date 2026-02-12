package com.blockbuster.ghastgolem;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.IronGolem;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class GhastGolemFix extends JavaPlugin {

    private static final double RADIUS = 25.0;
    private static final double HIT_RANGE = 3.5;
    private static final double DAMAGE = 7.0;

    @Override
    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (IronGolem golem : world.getEntitiesByClass(IronGolem.class)) {

                        Ghast closest = null;
                        double best = RADIUS;

                        for (Entity e : world.getNearbyEntities(golem.getLocation(), RADIUS, RADIUS, RADIUS)) {
                            if (!(e instanceof Ghast ghast)) continue;

                            String name = ghast.getCustomName();
                            if (name == null) continue;
                            if (!(name.contains("Ghast") && (name.contains("x") || name.contains("×")))) continue;

                            double dist = ghast.getLocation().distance(golem.getLocation());
                            if (dist < best) {
                                best = dist;
                                closest = ghast;
                            }
                        }

                        if (closest == null) continue;

                        golem.setTarget(closest);

                        if (best <= HIT_RANGE) {
                            closest.damage(DAMAGE, golem);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 1L, 10L);

        getLogger().info("GhastGolemFix enabled.");
    }
}
