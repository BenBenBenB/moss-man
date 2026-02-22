# Source Code Research Methodology (Fabric/Minecraft)

This document outlines the steps taken to research and resolve type mismatches and missing method signatures in the Minecraft/Fabric environment when documentation is unavailable.

## 1. Identify the Cache Location
Fabric projects use Gradle and Loom. Compiled and remapped Minecraft jars are stored in the `.gradle` directory.
- **Path**: `.gradle/loom-cache/minecraftMaven/net/minecraft/`
- **Jars of interest**: `minecraft-merged-*.jar` or `minecraft-common-*.jar`

## 2. Locate the Target Class
Use PowerShell to find which jar contains the class causing compilation errors.
```powershell
Get-ChildItem -Path .gradle -Filter "*.jar" -Recurse | ForEach-Object { 
    $jar = $_.FullName
    jar tf $jar | Select-String "TargetClassName" | ForEach-Object { Write-Output $jar }
} | Select-Object -Unique
```

## 3. Inspect Class Signatures
Once the jar is identified, use `javap` to inspect the available methods.

### Option A: Via Classpath (Direct)
```powershell
javap -cp "path/to/extracted/minecraft.jar" net.minecraft.package.ClassName
```

### Option B: Via Extraction (If Direct Fails)
If classloading fails due to dependencies, extract the file first:
```powershell
jar xf "path/to/minecraft.jar" net/minecraft/package/ClassName.class
javap net/minecraft/package/ClassName.class
```

## 4. Specific Findings for 1.21.11
- **GameProfileArgumentType.getProfileArgument**: Returns `Collection<PlayerConfigEntry>`.
- **PlayerConfigEntry**: Is a Java `Record`. Use `target.id()` and `target.name()` (NOT `getId()`/`getName()`).
- **GameProfile**: Also follows record-style accessors (`id()`, `name()`) in modern versions.

## 5. Reference Source
The Fabric API source can also be referenced at https://github.com/FabricMC/fabric-api for additional context on API-side hooks.
