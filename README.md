# TrappedTnt Spigot Plugin

A Minecraft Spigot plugin for trapped TNT functionality.

## Description

TrappedTnt is a Spigot plugin that adds enhanced TNT functionality to your Minecraft server. Players can place special trapped TNT that starts fusing immediately and explodes either when the timer runs out or when a player steps too close!

## Features

- **Special Trapped TNT**: Custom TNT items with unique properties and appearance
- **Instant Fuse**: Trapped TNT starts fusing immediately when placed
- **Proximity Detonation**: TNT explodes instantly when players get too close
- **Shield Bypass**: Explosion damage ignores shield protection for enhanced lethality
- **WorldGuard Integration**: Optional region restrictions for placement (soft dependency)
- **Permission System**: Fine-grained control over who can use what features
- **Configurable Settings**: Customize explosion power, timers, and behavior
- **Admin Commands**: Give trapped TNT and manage the plugin

## Commands

- `/trappedtnt` - Show plugin information
- `/trappedtnt help` - Display available commands
- `/trappedtnt give [player] [amount]` - Give trapped TNT to a player (admin only)
- `/trappedtnt reload` - Reload plugin configuration (admin only)

## Permissions

- `trappedtnt.use` - Basic plugin usage (default: true)
- `trappedtnt.give` - Permission to give trapped TNT (default: op)
- `trappedtnt.admin` - Administrative commands (default: op)
- `trappedtnt.*` - All permissions

## How It Works

1. **Getting Trapped TNT**: Use `/trappedtnt give` to obtain special trapped TNT items
2. **Placing**: Place the trapped TNT like regular TNT (respects WorldGuard regions if configured)
3. **Immediate Fusing**: The TNT starts fusing immediately upon placement
4. **Dual Trigger**: The TNT can explode in two ways:
   - **Timer**: Normal explosion after the configured fuse time
   - **Proximity**: Instant explosion when any player steps within the TNT's hitbox

## Installation

1. Download the compiled JAR file from the releases
2. Place it in your server's `plugins` folder
3. Restart your server or use a plugin manager to load it
4. Configure the plugin by editing `config.yml` in the plugin folder

## Development

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Spigot/Paper server for testing
- WorldGuard (optional, for region restrictions)

### Building

```bash
mvn clean package
```

The compiled JAR will be available in the `target` folder.

### Development Setup

1. Clone this repository
2. Import into your IDE (VS Code with Java extensions recommended)
3. Run `mvn clean compile` to download dependencies
4. Start developing!

## Configuration

The plugin creates a `config.yml` file with various settings:

- **General Settings**: Debug mode, message prefix
- **Trapped TNT Settings**: 
  - Explosion power and block breaking
  - Fuse timer duration
  - Proximity explosion toggle
  - Shield bypass functionality
- **WorldGuard Integration**:
  - Enable/disable region restrictions
  - Specify allowed regions for placement
- **Custom Messages**: All player-facing messages are configurable

### WorldGuard Integration

TrappedTnt supports WorldGuard as a soft dependency:
- **Not Installed**: Trapped TNT works globally
- **Installed + No Regions Configured**: Works globally
- **Installed + Regions Configured**: Only works in specified regions

Example configuration:
```yaml
worldguard:
  enabled: true
  allowed-regions:
    - "pvp-arena"
    - "minefield"
    - "trap-zone"
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, issues, or feature requests, please create an issue on the GitHub repository.
