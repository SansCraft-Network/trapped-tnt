package top.sanscraft.trappedtnt.listeners;

import top.sanscraft.trappedtnt.TrappedTnt;
import top.sanscraft.trappedtnt.utils.TrappedTntUtils;
import top.sanscraft.trappedtnt.utils.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
     * Handle explosion events to track trapped TNT explosions
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
        
        // Note: We now handle damage modification in onEntityDamageByEntity
        // instead of applying custom damage here
    }
    
    /**
     * Handle damage events from trapped TNT to apply shield penalties
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Only handle damage from TNT explosions
        if (!(event.getDamager() instanceof TNTPrimed)) {
            return;
        }
        
        TNTPrimed tnt = (TNTPrimed) event.getDamager();
        
        // Check if this is a trapped TNT using metadata
        if (!tnt.hasMetadata("trapped_tnt")) {
            return;
        }
        
        // Only handle damage to players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        double originalDamage = event.getFinalDamage();
        
        // Check if player is actively blocking with a shield
        boolean isBlocking = player.isBlocking();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean hasShield = (mainHand.getType() == Material.SHIELD) || (offHand.getType() == Material.SHIELD);
        
        if (isBlocking && hasShield) {
            // Always calculate what the damage would be without shield blocking
            // This ensures consistent 300% penalty regardless of partial vs perfect blocks
            double unshieldedDamage = calculateUnshieldedExplosionDamage(player, tnt);
            
            // Check if the calculated unshielded damage is above threshold
            double damageThreshold = plugin.getConfig().getDouble("trapped-tnt.shield-blocking-damage-threshold", 1.0);
            if (unshieldedDamage < damageThreshold) {
                return;
            }
            
            // Apply configurable damage multiplier for blocking players
            double damageMultiplier = plugin.getConfig().getDouble("trapped-tnt.shield-blocking-damage-multiplier", 3.0);
            double newDamage = unshieldedDamage * damageMultiplier;
            
            // Set the new damage amount (overriding the 0 damage from perfect shield block)
            event.setDamage(newDamage);
            
            // Add enhanced knockback for blocking players
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Vector knockback = player.getLocation().toVector().subtract(tnt.getLocation().toVector()).normalize();
                knockback.multiply(1.0); // Enhanced knockback for blocking players
                knockback.setY(Math.max(knockback.getY(), 0.2)); // Ensure good upward knockback
                player.setVelocity(player.getVelocity().add(knockback));
            }, 1L);
            
            // Send special message to blocking player
            String message = plugin.getConfig().getString("messages.shield-blocking-damage", 
                "&cYour shield was useless! You took &4300% damage &cfor trying to block the trapped TNT!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("Player " + player.getName() + " was blocking with shield (original: " + originalDamage + 
                    ", calculated unshielded: " + unshieldedDamage + ") - applying " + 
                    (damageMultiplier * 100) + "% damage (" + newDamage + ")");
            }
            
        } else if (originalDamage > 0) {
            // Handle non-blocking cases (original logic for when damage > 0)
            double damageThreshold = plugin.getConfig().getDouble("trapped-tnt.shield-blocking-damage-threshold", 1.0);
            if (originalDamage < damageThreshold) {
                return;
            }
            
            if (hasShield && plugin.getConfig().getBoolean("trapped-tnt.bypass-shields", true)) {
                // Player has shield but isn't blocking - send bypass message
                String message = plugin.getConfig().getString("messages.shield-bypass-damage", 
                    "&cYour shield couldn't protect you from the trapped TNT explosion!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }
    
    /**
     * Calculate what explosion damage would be without shield blocking
     */
    private double calculateUnshieldedExplosionDamage(Player player, TNTPrimed tnt) {
        Location playerLoc = player.getLocation();
        Location explosionLoc = tnt.getLocation();
        
        // Calculate distance from explosion center
        double distance = playerLoc.distance(explosionLoc);
        
        // Get explosion power from config or use default TNT power
        float explosionPower = (float) plugin.getConfig().getDouble("trapped-tnt.explosion-power", 4.0);
        
        // Calculate damage using vanilla-like formula
        // This mimics Minecraft's explosion damage calculation
        double maxDistance = explosionPower * 3.5;
        
        if (distance >= maxDistance) {
            return 0.0; // Too far away for any damage
        }
        
        // Base damage calculation (similar to vanilla)
        double baseDamage = explosionPower * 7.0; // TNT base damage
        double damageReduction = distance / (explosionPower * 2.0);
        double rawDamage = baseDamage * (1.0 - damageReduction);
        
        // Apply environmental damage reduction (blocks, etc.)
        // Simplified version - in reality Minecraft does raytracing for block protection
        double environmentProtection = Math.min(0.8, distance / maxDistance); // More protection at distance
        double finalDamage = rawDamage * (1.0 - environmentProtection * 0.3);
        
        // Ensure damage is positive and reasonable
        return Math.max(0, Math.min(finalDamage, baseDamage));
    }
    
    /**
     * Clean up tracking when plugin reloads
     */
    public void cleanup() {
        trappedTntLocations.clear();
        activeTrapTnt.clear();
    }
}
