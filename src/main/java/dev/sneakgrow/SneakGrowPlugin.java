package dev.sneakgrow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SneakGrowPlugin extends JavaPlugin implements Listener {

    private int radius;
    private int verticalRange;
    private double chance;
    private int cooldownTicks;
    private boolean particles;
    private Set<String> enabledWorlds;
    private Set<Material> growable;
    private boolean entitiesEnabled;
    private double entityChance;
    private int entityAgeBoost;
    private int breedCooldownBoost;
    private Set<EntityType> entityTypes;

    private final Map<UUID, Long> lastUse = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadSettings() {
        reloadConfig();
        radius = Math.max(1, Math.min(16, getConfig().getInt("radius", 4)));
        verticalRange = Math.max(0, Math.min(8, getConfig().getInt("vertical-range", 1)));
        chance = Math.max(0.0, Math.min(1.0, getConfig().getDouble("chance", 0.35)));
        cooldownTicks = Math.max(0, getConfig().getInt("cooldown-ticks", 5));
        particles = getConfig().getBoolean("particles", true);
        enabledWorlds = new HashSet<>(getConfig().getStringList("enabled-worlds"));
        entitiesEnabled = getConfig().getBoolean("entities.enabled", true);
        entityChance = Math.max(0.0, Math.min(1.0, getConfig().getDouble("entities.chance", 0.5)));
        entityAgeBoost = Math.max(1, getConfig().getInt("entities.age-boost-ticks", 2400));
        breedCooldownBoost = Math.max(0, getConfig().getInt("entities.breed-cooldown-boost-ticks", 600));
        entityTypes = EnumSet.noneOf(EntityType.class);
        for (String name : getConfig().getStringList("entities.types")) {
            try {
                entityTypes.add(EntityType.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Unknown entity type in entities.types: " + name);
            }
        }
        growable = EnumSet.noneOf(Material.class);
        for (String name : getConfig().getStringList("growable")) {
            Material m = Material.matchMaterial(name);
            if (m == null) {
                getLogger().warning("Unknown material in growable: " + name);
            } else {
                growable.add(m);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // しゃがみ始めたときのみ

        Player player = event.getPlayer();
        if (!player.hasPermission("sneakgrow.use")) return;

        World world = player.getWorld();
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(world.getName())) return;

        long now = world.getFullTime();
        Long last = lastUse.get(player.getUniqueId());
        if (last != null && now - last < cooldownTicks) return;
        lastUse.put(player.getUniqueId(), now);

        growAround(player.getLocation());
        if (entitiesEnabled) growEntities(player.getLocation());
    }

    /** 周囲の家畜の成長を早める（子供は成長までの時間、繁殖後の親は次の繁殖までの時間を短縮する） */
    private void growEntities(Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double r = radius;
        for (Entity entity : center.getWorld().getNearbyEntities(center, r, r, r)) {
            if (!entityTypes.contains(entity.getType())) continue;
            if (!(entity instanceof org.bukkit.entity.Ageable animal)) continue;
            if (random.nextDouble() >= entityChance) continue;

            int age = animal.getAge();
            if (age < 0) {
                // 子供: 成長までの時間を短縮
                animal.setAge(Math.min(0, age + entityAgeBoost));
            } else if (age > 0 && breedCooldownBoost > 0) {
                // 繁殖後の親: 次の繁殖までのクールダウンを短縮
                animal.setAge(Math.max(0, age - breedCooldownBoost));
            } else {
                continue;
            }
            if (particles) {
                entity.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        entity.getLocation().add(0, entity.getHeight() / 2, 0), 3, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    private void growAround(Location center) {
        World world = center.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        double maxDistSq = (radius + 0.5) * (radius + 0.5);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > maxDistSq) continue;
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    int bx = cx + x, by = cy + y, bz = cz + z;
                    if (!world.isChunkLoaded(bx >> 4, bz >> 4)) continue;
                    Block block = world.getBlockAt(bx, by, bz);
                    if (!growable.contains(block.getType())) continue;
                    if (random.nextDouble() >= chance) continue;
                    // 骨粉と同じ処理。成長できないブロックでは false が返る
                    if (!block.applyBoneMeal(BlockFace.UP)) continue;

                    if (particles) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER,
                                block.getLocation().add(0.5, 0.5, 0.5), 3, 0.25, 0.25, 0.25, 0);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastUse.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            sender.sendMessage("SneakGrow: config reloaded.");
            return true;
        }
        return false;
    }
}
