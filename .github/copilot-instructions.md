<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# TrappedTnt Spigot Plugin Development Instructions

This is a Minecraft Spigot plugin project written in Java using Maven for build management.

## Project Structure
- Main plugin class: `top.sanscraft.trappedtnt.TrappedTnt`
- Author: SansNom
- Organization: top.sanscraft
- Spigot API version: 1.20.4
- Java version: 17
- Maven build system

## Development Guidelines
1. Follow Spigot API conventions and best practices
2. Use proper event handling with `@EventHandler` annotations
3. Implement proper permission checks for commands
4. Use ChatColor for message formatting
5. Always check for null values when working with player objects
6. Use the plugin's logger for debug and info messages
7. Follow the existing code style and naming conventions

## Key Dependencies
- Spigot API (provided scope)
- Maven Shade Plugin for creating fat JARs

## Common Patterns
- Event listeners should be registered in the `onEnable()` method
- Commands should be handled in the `onCommand()` method
- Configuration should be loaded using `getConfig()` and `saveDefaultConfig()`
- Use `getServer().getScheduler()` for delayed or repeating tasks

## Testing
- Test the plugin on a local Spigot server
- Verify all commands and permissions work correctly
- Check compatibility with the target Minecraft version
