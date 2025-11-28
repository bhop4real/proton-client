# Proton Client

A feature-rich Minecraft 1.8.9 utility client built with Forge and Mixin.

## About

Proton is a client-side modification for Minecraft 1.8.9 that provides various gameplay enhancements, modules, and utilities. Built using Minecraft Forge and SpongePowered Mixin for bytecode manipulation.

## Features

### Core Modules

- **AutoClicker** - Automated clicking with advanced randomization and pattern scripting
- **AimAssist** - Assists with aiming at targets
- ~~**KillAura** - Automated combat module~~ Currently broken, and not planned to fix
- **Speed** - Movement speed enhancements
- **Velocity** - Knockback reduction
- **Reach** - Extended reach distance
- **ESP** - Entity visualization through walls
- **Backtrack** - Hit registration improvements
- **Sprint** - Automatic sprinting
- **Eagle** - Bridge building assistance
- **Fullbright** - Full brightness lighting
- **Targets** - Target selection utilities

### Advanced Features

- **Click GUI** - Customizable module configuration interface with themes
- **HUD** - Customizable heads-up display
- **HUD Designer** - Visual HUD layout editor
- **Music Player** - In-game music player with playlist support
- **Click Sampler** - Record and analyze click patterns
- **Click Model Trainer** - Machine learning-based click pattern training
- **Config Manager** - Save and load module configurations
- **Command System** - In-game command interface

### Special Capabilities

- **Pattern Scripting API** - JavaScript-based custom click patterns for AutoClicker
- **Machine Learning Integration** - ML-based click pattern learning and generation
- **Theme System** - Customizable GUI themes
- **Settings Management** - Persistent configuration storage

## Requirements

- **Minecraft**: 1.8.9
- **Forge**: 11.15.1.2318-1.8.9
- **Java**: 8 or higher
- **Gradle**: (included via wrapper)

## Building

1. Clone the repository:
```bash
git clone https://github.com/bhop4real/proton-client.git
cd proton-client
```

2. Build the project:
```bash
# On Windows
gradlew.bat build

# On Linux/Mac
./gradlew build
```

3. The compiled mod JAR will be in `build/libs/proton-1.0.jar`

## Installation

1. Install Minecraft Forge 1.8.9 (version 11.15.1.2318)
2. Place the `proton-1.0.jar` file in your `.minecraft/mods/` directory
3. Launch Minecraft with the Forge profile

## Usage

### Basic Controls

- Press the **Click GUI key** (default: `RSHIFT`) to open the module configuration interface
- Modules can be toggled via the Click GUI or using commands

### Module Configuration

Each module has customizable settings accessible through the Click GUI:
- **Boolean Settings** - Toggle options on/off
- **Int/Double Settings** - Numeric values with min/max ranges
- **Enum Settings** - Dropdown selections
- **Color Settings** - Color pickers for visual modules
- **Keybind Settings** - Custom keybindings

### AutoClicker Pattern Scripting

The AutoClicker module supports custom JavaScript patterns. See [PATTERN_SCRIPT_API.md](PATTERN_SCRIPT_API.md) for detailed documentation.

Example pattern script:
```javascript
function getNextDelay(lastDelay, config, state) {
    // Custom pattern logic
    var targetCPS = randomDoubleMinMax(config.minCps, config.maxCps);
    return 1000 / targetCPS;
}
```

### Music Player

The Music Player module allows you to:
- Play MP3 files from your local library
- Create and manage playlists
- Control playback (play, pause, skip, shuffle, repeat)
- Access via the Music Player GUI

Place MP3 files in `.minecraft/proton/music/` to add them to your library.

## Configuration

Configuration files are stored in `.minecraft/proton/`:
- `config.json` - Main client configuration
- `settings.properties` - Module settings
- `hud_layout.json` - HUD layout configuration
- `themes/` - Custom GUI themes
- `patterns/` - AutoClicker pattern scripts
- `music/` - Music library directory

## Project Structure

```
proton-client/
├── src/main/java/com/bhop4real/proton/
│   ├── client/
│   │   ├── module/          # Module system
│   │   ├── gui/             # GUI components
│   │   ├── settings/        # Settings management
│   │   ├── music/           # Music player
│   │   ├── learning/        # ML features
│   │   └── util/            # Utilities
│   ├── common/              # Common proxy
│   └── mixins/              # Mixin classes
├── src/main/resources/
│   ├── assets/              # Textures, fonts, shaders
│   └── mixins.proton.json   # Mixin configuration
└── build.gradle             # Build configuration
```

## Development

### Setting up Development Environment

1. Import the project into your IDE (IntelliJ IDEA recommended)
2. Run `gradlew setupDecompWorkspace` to set up the development environment
3. Run `gradlew genIntellijRuns` to generate IDE run configurations

### Key Technologies

- **Minecraft Forge** - Modding framework
- **SpongePowered Mixin** - Bytecode manipulation
- **Gradle Shadow** - Dependency shading
- **JavaScript (Nashorn)** - Pattern scripting engine

## License

This project includes various licenses from dependencies:
- Minecraft Forge License
- SpongePowered Mixin License
- Paulscode SoundSystem Licenses

See individual license files in the repository for details.

## Credits

- **Author**: bhop4real
- **Contributors**: Cursor
- Built with Minecraft Forge and SpongePowered Mixin

## Disclaimer

This is a utility client for Minecraft 1.8.9. Use responsibly and in accordance with server rules and Minecraft's Terms of Service. The authors are not responsible for any consequences resulting from the use of this software.

~~For issues, feature requests, or questions, please open an issue on the [GitHub repository](https://github.com/bhop4real/proton-client).~~

This client is coded by Cursor AI.

