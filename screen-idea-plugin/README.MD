# Screen IDEA Plugin

IntelliJ IDEA / Android Studio plugin providing IDE support for the Screen compiler plugin.

## Overview

This plugin enhances the IDE experience for `@Screen`-annotated classes by providing code resolution, visual indicators, documentation, and inspections for the generated `KEY` constant.

## Features

### Bundled Compiler Plugin Provider
- Provides the `screen-compiler.jar` to the K2 FIR analysis engine in the IDE
- Enables real-time resolution of generated declarations (`KEY`, `arg`, `createScreen()`) without building

### Gutter Line Marker
- Displays a plugin icon in the gutter next to `@Screen`-annotated classes
- Tooltip shows the generated constants (e.g., `KEY = "com.example.MyFragment"`)

### Documentation Provider
- Provides Quick Documentation (Ctrl+Q / F1) for the generated `KEY` property
- Shows the constant value, type, and description

### Manual Constant Inspection
- Warns when a developer manually defines `KEY` in a companion object of a `@Screen` class
- Prevents conflicts between manually defined and generated constants
- Enabled by default at WARNING level

## Requirements

- Android Studio or IntelliJ IDEA 2025.1+ (build 251+)
- Kotlin plugin installed
- K2 mode enabled (K1 is not supported)

## Installation

### From Source

1. Build the screen-compiler JAR:
   ```bash
   ./gradlew :screen-compiler:jar
   ```

2. Copy the JAR to the IDEA plugin libs directory:
   ```bash
   cp screen-compiler/build/libs/screen-compiler-1.0-SNAPSHOT.jar \
      screen-idea-plugin/libs/screen-compiler.jar
   ```

3. Build the IDEA plugin:
   ```bash
   cd screen-idea-plugin
   ./gradlew buildPlugin
   ```

4. Install the plugin ZIP from `build/distributions/` via IDE settings.

### Development Mode

```bash
cd screen-idea-plugin
./gradlew runIde
```

This launches a sandboxed IDE instance with the plugin pre-installed.

## Plugin ID

```
com.adkhambek.screen.idea
```

## Extension Points

| Extension | Implementation | Description |
|-----------|---------------|-------------|
| `bundledFirCompilerPluginProvider` | `ScreenBundledCompilerPluginProvider` | Provides compiler JAR for K2 analysis |
| `codeInsight.lineMarkerProvider` | `ScreenLineMarkerProvider` | Gutter icon on @Screen classes |
| `lang.documentationProvider` | `ScreenDocumentationProvider` | Quick doc for KEY property |
| `localInspection` | `ScreenManualConstantInspection` | Warns on manual KEY definition |
