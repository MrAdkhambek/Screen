# ViewBinding IDEA Plugin

IntelliJ IDEA / Android Studio plugin providing IDE support for the ViewBinding compiler plugin.

## Overview

This plugin enhances the IDE experience for `@Screen`-annotated Fragment classes by providing resolution, visual indicators, documentation, and inspections for the generated `binding` property.

## Features

### Bundled Compiler Plugin Provider
- Provides the `viewbinding-compiler.jar` to the K2 FIR analysis engine in the IDE
- Enables real-time resolution of the generated `binding` property without building

### Gutter Line Marker
- Displays a plugin icon in the gutter next to `@Screen`-annotated classes
- Tooltip shows that a ViewBinding property is generated

### Documentation Provider
- Provides Quick Documentation (Ctrl+Q / F1) for the generated `binding` property
- Shows the binding type and generation details

### Manual Binding Inspection
- Warns when a developer manually defines `binding` in a `@Screen` class
- Prevents conflicts between manually defined and generated binding properties
- Enabled by default at WARNING level

## Requirements

- Android Studio or IntelliJ IDEA 2025.1+ (build 251+)
- Kotlin plugin installed
- K2 mode enabled (K1 is not supported)

## Installation

### From Source

1. Build the viewbinding-compiler JAR:
   ```bash
   ./gradlew :viewbinding-compiler:jar
   ```

2. Copy the JAR to the IDEA plugin libs directory:
   ```bash
   cp viewbinding-compiler/build/libs/viewbinding-compiler-1.0-SNAPSHOT.jar \
      viewbinding-idea-plugin/libs/viewbinding-compiler.jar
   ```

3. Build the IDEA plugin:
   ```bash
   cd viewbinding-idea-plugin
   ./gradlew buildPlugin
   ```

4. Install the plugin ZIP from `build/distributions/` via IDE settings.

### Development Mode

```bash
cd viewbinding-idea-plugin
./gradlew runIde
```

## Plugin ID

```
com.adkhambek.viewbinding.idea
```

## Extension Points

| Extension | Implementation | Description |
|-----------|---------------|-------------|
| `bundledFirCompilerPluginProvider` | `ViewBindingBundledCompilerPluginProvider` | Provides compiler JAR for K2 analysis |
| `codeInsight.lineMarkerProvider` | `ViewBindingLineMarkerProvider` | Gutter icon on @Screen classes |
| `lang.documentationProvider` | `ViewBindingDocumentationProvider` | Quick doc for binding property |
| `localInspection` | `ViewBindingManualBindingInspection` | Warns on manual binding definition |
