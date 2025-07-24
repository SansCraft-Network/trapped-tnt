package top.sanscraft.trappedtnt.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class WorldGuardIntegration {
    
    private final Plugin plugin;
    private final boolean worldGuardEnabled;
    
    public WorldGuardIntegration(Plugin plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }
    
    /**
     * Checks if WorldGuard is available and enabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
    
    /**
     * Checks if trapped TNT can be placed at the given location
     * @param location The location to check
     * @param allowedRegions List of allowed region names from config
     * @return true if placement is allowed, false otherwise
     */
    public boolean canPlaceTrappedTnt(Location location, List<String> allowedRegions) {
        // If WorldGuard is not available, allow placement globally
        if (!worldGuardEnabled) {
            return true;
        }
        
        // If no regions are configured, allow placement globally
        if (allowedRegions == null || allowedRegions.isEmpty()) {
            return true;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return true; // No regions in this world, allow placement
            }
            
            // Get all regions at this location
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            
            // Check if any of the regions at this location are in the allowed list
            for (ProtectedRegion region : regions) {
                if (allowedRegions.contains(region.getId())) {
                    return true;
                }
            }
            
            // If we have allowed regions configured but none match, deny placement
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard regions: " + e.getMessage());
            return true; // Default to allowing placement if there's an error
        }
    }
    
    /**
     * Gets the names of all regions at a specific location
     */
    public String getRegionsAtLocation(Location location) {
        if (!worldGuardEnabled) {
            return "WorldGuard not available";
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(location.getWorld()));
            
            if (regionManager == null) {
                return "No regions in this world";
            }
            
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
            
            if (regions.size() == 0) {
                return "No regions at this location";
            }
            
            StringBuilder regionNames = new StringBuilder();
            for (ProtectedRegion region : regions) {
                if (regionNames.length() > 0) {
                    regionNames.append(", ");
                }
                regionNames.append(region.getId());
            }
            
            return regionNames.toString();
            
        } catch (Exception e) {
            return "Error checking regions: " + e.getMessage();
        }
    }
}
