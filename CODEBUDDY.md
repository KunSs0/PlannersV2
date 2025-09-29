<system-reminder>
This is a reminder that your todo list is currently empty. DO NOT mention this to the user explicitly because they are already aware. If you are working on tasks that would benefit from a todo list please use the TodoWrite tool to create one. If not, please feel free to ignore. Again do not mention this message to the user.
</system-reminder>

# Planners Minecraft Plugin

## Project Overview

Planners is a Minecraft Bukkit plugin built with Kotlin and Gradle, using the TabooLib framework. It provides a comprehensive skill system with animated entities, particle effects, and custom scripting capabilities.

## Build Commands

- **Build:** `gradlew.bat clean build` (Windows) or `./gradlew clean build` (macOS/Linux)
- **Build artifacts:** Located in `./build/libs` folder
- **Direct deployment:** The build is configured to deploy directly to `F:\minecraft\server\paper-1.20.1\plugins` (configured in `build.gradle.kts:40`)

## Architecture

### Core Components

1. **Plugin Entry Point:** `Planners.kt` - Main plugin class using TabooLib framework
2. **API Layer:** `com.gitee.planners.api` - Public API for skill casting and variable management
3. **Core System:** `com.gitee.planners.core` - Skill, route, and player management
4. **Module System:** `com.gitee.planners.module` - Specialized functionality (entities, particles, compatibility)
5. **Scripting Engine:** Kether-based scripting system in `com.gitee.planners.module.kether`

### Key Design Patterns

- **Immutable Configuration:** Uses immutable pattern for skills, routes, and variables
- **Event-Driven Architecture:** Custom events for skill casting, player interactions
- **Modular Design:** Separate modules for entities, particles, world interactions
- **Registry Pattern:** Central registries for selectors, actions, and components

### Domain Model

- **Skills:** Configurable abilities with variables, cooldowns, and effects
- **Routes:** Skill progression paths and leveling systems
- **Animated Entities:** Custom entity system with proxy entities and metadata
- **Particle Effects:** Animated particle systems with shapes and transformations
- **Target System:** Flexible targeting for entities, locations, and blocks

## Technology Stack

- **Framework:** TabooLib 6.2.3 with Bukkit integration
- **Language:** Kotlin with Java 8 compatibility
- **Scripting:** Kether scripting engine
- **Database:** SQLite with optional database support
- **Dependencies:**
  - Minecraft libraries (NMS, Bukkit)
  - Math libraries (EJML for matrix operations)
  - Plugin compatibility (ModelEngine, WorldGuard, PlaceholderAPI)

## Development Notes

### Code Structure

- Primary code in Kotlin (`src/main/kotlin`)
- Some legacy Java files (`src/main/java`)
- Configuration-driven architecture
- Extension-based design for adding new functionality

### Plugin Integration

- Supports multiple Minecraft versions (1.12.2 to 1.20.1)
- Optional dependencies for enhanced functionality
- Modular compatibility system for other plugins

### Testing

- Test files located in `src/test/kotlin`
- Includes vector math, bounding box, and entity finder tests

## Important Files

- `build.gradle.kts` - Build configuration with dependency management
- `Planners.kt` - Main plugin entry point
- `PlannersAPI.kt` - Public API for external integration
- `Registries.kt` - Central registry for all plugin components

## Configuration

The plugin uses TabooLib's configuration system with auto-reload capabilities. Main configuration structure is defined through annotations in the main plugin class.