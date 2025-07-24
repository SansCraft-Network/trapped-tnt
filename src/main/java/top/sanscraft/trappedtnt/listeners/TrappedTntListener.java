package top.sanscraft.trappedtnt.listeners;

import top.sanscraft.trappedtnt.TrappedTnt;
import top.sanscraft.trappedtnt.utils.TrappedTntUtils;
import top.sanscraft.trappedtnt.utils.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrappedTntListener implements Listener {
    
    private final TrappedTnt plugin;
    private final TrappedTntUtils tntUtils;
    private final WorldGuardIntegration worldGuardIntegration;
    private final Map<Location, UUID> trappedTntLocations = new HashMap<>();
    private final Set<TNTPrimed> activeTrapTnt = new HashSet<>();
    
    public TrappedTntListener(TrappedTnt plugin) {
        this.plugin = plugin;
        this.tntUtils = new TrappedTntUtils(plugin);
        this.worldGuardIntegration = new WorldGuardIntegration(plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Check if the placed block is trapped TNT
        if (!tntUtils.isTrappedTnt(item)) {
            return;
        }
        
        Location location = event.getBlock().getLocation();
        
        // Check WorldGuard permissions
        List<String> allowedRegions = plugin.getConfig().getStringList("worldguard.allowed-regions");
        if (!worldGuardIntegration.canPlaceTrappedTnt(location, allowedRegions)) {
            event.setCancelled(true);
            String message = plugin.getConfig().getString("messages.region-not-allowed", "&cYou cannot place trapped TNT in this area!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        // Schedule TNT spawning and fuse start
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (location.getBlock().getType() == Material.TNT) {
                spawnTrappedTnt(location, player);
            }
        }, 1L);
        
        // Send placement message
        String message = plugin.getConfig().getString("messages.trapped-tnt-placed", "&eTrapped TNT placed! Be careful...");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private void spawnTrappedTnt(Location location, Player placer) {
        // Remove the TNT block
        location.getBlock().setType(Material.AIR);
        
        // Spawn primed TNT entity
        Location spawnLoc = location.clone().add(0.5, 0, 0.5);
        TNTPrimed tnt = location.getWorld().spawn(spawnLoc, TNTPrimed.class);
        
        // Set fuse timer from config
        int fuseTimer = plugin.getConfig().getInt("trapped-tnt.fuse-timer", 80);
        tnt.setFuseTicks(fuseTimer);
        
        // Mark as trapped TNT with metadata
        tnt.setMetadata("trapped_tnt", new FixedMetadataValue(plugin, true));
        tnt.setMetadata("placer", new FixedMetadataValue(plugin, placer.getUniqueId().toString()));
        
        // Store location for proximity detection (legacy tracking)
        trappedTntLocations.put(location, placer.getUniqueId());
        
        // Add to active TNT tracking set for real-time collision detection
        activeTrapTnt.add(tnt);
        
        // Schedule cleanup of location tracking when TNT explodes naturally
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            trappedTntLocations.remove(location);
            activeTrapTnt.remove(tnt);
        }, fuseTimer + 5L);
        
        if (plugin.getConfig().getBoolean("general.debug", false)) {
            plugin.getLogger().info("Trapped TNT spawned at " + location + " by " + placer.getName());
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("trapped-tnt.instant-explosion-on-contact", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();
        
        // Check all active trapped TNT entities for proximity (real-time collision detection)
        TNTPrimed tntToExplode = null;
        for (TNTPrimed tnt : activeTrapTnt) {
            // Check if TNT still exists and is valid
            if (tnt == null || tnt.isDead() || !tnt.isValid()) {
                continue;
            }
            
            Location tntLoc = tnt.getLocation();
            
            // Check if player is in the same world
            if (!playerLoc.getWorld().equals(tntLoc.getWorld())) {
                continue;
            }
            
            // Calculate distance using the actual TNT entity location
            double distance = playerLoc.distance(tntLoc);
            
            if (distance <= 1.2) { // Slightly larger than TNT entity hitbox
                tntToExplode = tnt;
                break; // Only trigger one explosion per move event
            }
        }
        
        // Trigger explosion if a TNT was found
        if (tntToExplode != null) {
            // Remove from tracking immediately to prevent multiple triggers
            activeTrapTnt.remove(tntToExplode);
            
            // Trigger immediate explosion
            tntToExplode.setFuseTicks(0);
            
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("Trapped TNT triggered by proximity at " + tntToExplode.getLocation() + " by " + player.getName());
            }
        }
    }
    
    /**
     * Handle explosion events to apply custom damage through shields
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed)) {
            return;
        }
        
        TNTPrimed tnt = (TNTPrimed) event.getEntity();
        
        // Check if this is a trapped TNT using metadata
        if (!tnt.hasMetadata("trapped_tnt")) {
            return;
        }
        
        // Remove from active tracking when it explodes
        activeTrapTnt.remove(tnt);
        
        plugin.getLogger().info("Trapped TNT exploded at " + tnt.getLocation());
        
        // Apply custom damage that ignores shields if enabled
        if (plugin.getConfig().getBoolean("trapped-tnt.bypass-shields", true)) {
            applyCustomExplosionDamage(tnt, event.getYield());
        }
    }
    
    /**
     * Apply custom explosion damage that ignores shields
     */
    private void applyCustomExplosionDamage(TNTPrimed tnt, float explosionPower) {
        Location explosionLocation = tnt.getLocation();
        double maxDistance = explosionPower * 3.5; // Similar to vanilla TNT damage calculation
        
        // Get all nearby players
        for (Entity entity : explosionLocation.getWorld().getNearbyEntities(explosionLocation, maxDistance, maxDistance, maxDistance)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            
            Player player = (Player) entity;
            double distance = player.getLocation().distance(explosionLocation);
            
            // Calculate damage based on distance
            if (distance <= maxDistance) {
                double damage = calculateExplosionDamage(distance, explosionPower);
                
                if (damage > 0) {
                    // Apply damage that ignores shields
                    applyShieldIgnoringDamage(player, damage, tnt);
                }
            }
        }
    }
    
    /**
     * Calculate explosion damage based on distance and power
     */
    private double calculateExplosionDamage(double distance, float explosionPower) {
        // Base damage calculation similar to vanilla Minecraft
        double baseDamage = explosionPower * 7.0; // TNT typically does up to 65 damage at point blank
        double damageReduction = distance / (explosionPower * 2.0);
        
        // Apply damage falloff
        double finalDamage = baseDamage * (1.0 - damageReduction);
        
        // Ensure minimum damage and cap maximum
        return Math.max(0, Math.min(finalDamage, baseDamage));
    }
    
    /**
     * Apply damage to a player that ignores shield protection
     */
    private void applyShieldIgnoringDamage(Player player, double damage, TNTPrimed tnt) {
        // Store original shield state
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean hadShieldInMainHand = mainHand.getType() == Material.SHIELD;
        boolean hadShieldInOffHand = offHand.getType() == Material.SHIELD;
        
        // Temporarily remove shields to bypass protection
        if (hadShieldInMainHand) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        if (hadShieldInOffHand) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        
        // Create custom damage event (use player.damage instead of complex event creation)
        // This approach is simpler and more reliable across different Spigot versions
        
        // Apply the damage directly
        player.damage(damage);
        
        // Add knockback effect
        Vector knockback = player.getLocation().toVector().subtract(tnt.getLocation().toVector()).normalize();
        knockback.multiply(0.5); // Adjust knockback strength
        knockback.setY(Math.max(knockback.getY(), 0.1)); // Ensure some upward knockback
        player.setVelocity(player.getVelocity().add(knockback));
        
        // Send feedback message
        String message = plugin.getConfig().getString("messages.explosion-damage", 
            "&cYou took explosion damage that bypassed your shield!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        // Restore shields after a brief delay to prevent interference
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (hadShieldInMainHand) {
                player.getInventory().setItemInMainHand(mainHand);
            }
            if (hadShieldInOffHand) {
                player.getInventory().setItemInOffHand(offHand);
            }
        }, 1L); // 1 tick delay
    }
    
    /**
     * Clean up tracking when plugin reloads
     */
    public void cleanup() {
        trappedTntLocations.clear();
        activeTrapTnt.clear();
    }
}
