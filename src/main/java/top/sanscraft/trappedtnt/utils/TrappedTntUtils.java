package top.sanscraft.trappedtnt.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class TrappedTntUtils {
    
    private final NamespacedKey trappedTntKey;
    
    public TrappedTntUtils(Plugin plugin) {
        this.trappedTntKey = new NamespacedKey(plugin, "trapped_tnt");
    }
    
    /**
     * Creates a trapped TNT item with special NBT data
     */
    public ItemStack createTrappedTnt(int amount) {
        ItemStack item = new ItemStack(Material.TNT, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set custom name and lore
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Trapped TNT");
            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "A dangerous explosive that triggers",
                ChatColor.GRAY + "when players get too close!",
                ChatColor.DARK_RED + "" + ChatColor.ITALIC + "Handle with extreme care..."
            );
            meta.setLore(lore);
            
            // Add persistent data to mark as trapped TNT
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            dataContainer.set(trappedTntKey, PersistentDataType.BYTE, (byte) 1);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Checks if an item is a trapped TNT
     */
    public boolean isTrappedTnt(ItemStack item) {
        if (item == null || item.getType() != Material.TNT) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        return dataContainer.has(trappedTntKey, PersistentDataType.BYTE);
    }
    
    /**
     * Gets the NamespacedKey used for trapped TNT identification
     */
    public NamespacedKey getTrappedTntKey() {
        return trappedTntKey;
    }
}
