package top.sanscraft.trappedtnt;

import top.sanscraft.trappedtnt.listeners.TrappedTntListener;
import top.sanscraft.trappedtnt.utils.TrappedTntUtils;
import top.sanscraft.trappedtnt.utils.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TrappedTnt extends JavaPlugin {
    
    private TrappedTntUtils tntUtils;
    private WorldGuardIntegration worldGuardIntegration;
    private TrappedTntListener trappedTntListener;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("TrappedTnt plugin has been enabled!");
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize utilities
        tntUtils = new TrappedTntUtils(this);
        worldGuardIntegration = new WorldGuardIntegration(this);
        
        // Register events
        registerEvents();
        
        // Register tab completer for the main command
        this.getCommand("trappedtnt").setTabCompleter(this);
        
        // Check WorldGuard integration
        if (worldGuardIntegration.isWorldGuardEnabled()) {
            getLogger().info("WorldGuard detected! Region restrictions are available.");
        } else {
            getLogger().info("WorldGuard not detected. Trapped TNT will work globally.");
        }
        
        // Send a message to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[TrappedTnt] Plugin loaded successfully!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (trappedTntListener != null) {
            trappedTntListener.cleanup();
        }
        getLogger().info("TrappedTnt plugin has been disabled!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[TrappedTnt] Plugin unloaded!");
    }

    private void registerEvents() {
        // Register event listeners here
        trappedTntListener = new TrappedTntListener(this);
        getServer().getPluginManager().registerEvents(trappedTntListener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("trappedtnt")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "TrappedTnt v" + getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Use /trappedtnt help for available commands");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help":
                    showHelp(sender);
                    return true;
                    
                case "reload":
                    if (!sender.hasPermission("trappedtnt.admin")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin!");
                        return true;
                    }
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "TrappedTnt configuration reloaded!");
                    return true;
                    
                case "give":
                    if (!sender.hasPermission("trappedtnt.give")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to give trapped TNT!");
                        return true;
                    }
                    return handleGiveCommand(sender, args);
                    
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /trappedtnt help for available commands");
                    return true;
            }
        }
        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== TrappedTnt Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/trappedtnt - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/trappedtnt help - Show this help message");
        if (sender.hasPermission("trappedtnt.give")) {
            sender.sendMessage(ChatColor.YELLOW + "/trappedtnt give [player] [amount] - Give trapped TNT");
        }
        if (sender.hasPermission("trappedtnt.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/trappedtnt reload - Reload plugin configuration");
        }
    }
    
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        // Usage: /trappedtnt give [player] [amount]
        Player targetPlayer = null;
        int amount = 1;
        
        if (args.length >= 2) {
            // Get target player
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found!");
                return true;
            }
        } else {
            // Give to sender if they're a player
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a player when using this command from console!");
                sender.sendMessage(ChatColor.YELLOW + "Usage: /trappedtnt give <player> [amount]");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount! Please enter a number between 1 and 64.");
                return true;
            }
        }
        
        // Create and give trapped TNT
        ItemStack trappedTnt = tntUtils.createTrappedTnt(amount);
        targetPlayer.getInventory().addItem(trappedTnt);
        
        // Send messages
        String message = getConfig().getString("messages.trapped-tnt-given", "&aYou have been given &6{amount} &atrapped TNT!")
            .replace("{amount}", String.valueOf(amount));
        targetPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        if (!sender.equals(targetPlayer)) {
            sender.sendMessage(ChatColor.GREEN + "Given " + amount + " trapped TNT to " + targetPlayer.getName());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!command.getName().equalsIgnoreCase("trappedtnt")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - main subcommands
            List<String> subcommands = new ArrayList<>();
            subcommands.add("help");
            
            if (sender.hasPermission("trappedtnt.admin")) {
                subcommands.add("reload");
            }
            
            if (sender.hasPermission("trappedtnt.give")) {
                subcommands.add("give");
            }
            
            // Filter subcommands based on what the user has typed
            String partial = args[0].toLowerCase();
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
                    
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Second argument for "give" command - player names
            if (sender.hasPermission("trappedtnt.give")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
            
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Third argument for "give" command - amount suggestions
            if (sender.hasPermission("trappedtnt.give")) {
                return Arrays.asList("1", "2", "4", "8", "16", "32", "64");
            }
        }
        
        return completions;
    }
}
