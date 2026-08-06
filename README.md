# Survival Assistant PLUS

 Survival Assistant PLUS is a client-side Fabric utility mod for Minecraft 26.1.2. It ports and preserves the original assistant features while updating the project to the current Fabric, Yarn, malilib, and Mod Menu stack.

## Features

- Silent ender pearl and firework use from inventory
- Firework and totem inventory warnings
- Ender pearl trajectory rendering
- Freecam with configurable speed and hand rendering
- Auto Tool, Auto Eat, and Auto Totem helpers
- Quick Shulker-based restocking support
- Ghost Hand container interaction
- Container ESP and Block ESP
- Meteor-style nametags
- Better Chat filtering and command-completion filtering
- Spectator teleport list completion toggle
- Zhou Li-style chat rewrite feature through an OpenAI-compatible API

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.1 or newer
- Fabric API
- malilib
- Mod Menu
- Java 25

Some optional features require extra mods:

- Quick Shulker is required for shulker restocking.

## Configuration

Open the mod configuration through Mod Menu or the configured hotkey. Most features are toggleable and can be adjusted through malilib config entries.

The Zhou Li chat rewrite feature requires an OpenAI-compatible API base URL, API key, and model name. Messages starting with `/`, `!`, or `!!` are ignored.

## Building

```bash
./gradlew build
```

The built jar will be generated under `build/libs/`.

## Branch

This branch targets Minecraft/Fabric 26.1.2.
