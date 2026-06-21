# Cosmic Out

[中文 README](README.zh-CN.md)

Mindustry Java library mod for campaign starmap, sector logistics, and planetary logistics. Blocks and sprites are defined by dependent mods, not shipped here.

- Min game version: `158` · Main class: `mod.CosmicOut` · Name: `cosmic-out`
- Example mod: [`example/`](example/) (`cosmic-out-example`)

## Overview

| System | Scope | Type prefix |
|--------|-------|-------------|
| Sector logistics | sector → sector (same planet) | `Item` / `Liquid` / `Payload` + `LaunchPad` / `LandingPad` |
| Planetary logistics | planet → planet (starmap) | `Planetary` + same suffixes |

Also includes starmap UI, sector/planet logistics stats.

## Starmap

Place `starmap.hjson` in the mod root. All enabled mods are scanned on load by `StarMapScanner`.

```hjson
planets: [
  { name: sun, axis0: 0, axis120: 0 }
  { name: erekir, axis0: 2, axis120: 0 }
  { name: gier, axis0: 2, axis120: 3, size: 16 }
]
```

| Field | Default | Description |
|-------|---------|-------------|
| `name` | — | Planet content ID (must exist in game) |
| `axis0` | `0` | Hex coordinate, axis 0 |
| `axis120` | `0` | Hex coordinate, 120° axis (third axis = `-(axis0 + axis120)`) |
| `size` | `1.5` | Display size on starmap |

Unknown planets are skipped. Duplicate entries for the same planet are overwritten by later mods. Planets not on the starmap cannot be used as `Planetary*` destinations.

Example: [`example/starmap.hjson`](example/starmap.hjson)

## Block Types

Add `dependencies: [cosmic-out]` in `mod.hjson`. Examples: [`example/content/blocks/`](example/content/blocks/). All twelve types are registered in `ClassMapRegister`.

### Sector (6)

Launch pads target a `Sector`; landing pads receive from other sectors.

| type | New fields |
|------|------------|
| `ItemLaunchPad` | `maxPath` |
| `ItemLandingPad` | — |
| `LiquidLaunchPad` | `maxPath`, `launchVolume` |
| `LiquidLandingPad` | `landingVolume` |
| `PayloadLaunchPad` | `maxPath`, `payloadCapacity`, `payloadLaunchCount` |
| `PayloadLandingPad` | `payloadSpeed`, `payloadRotateSpeed` |

### Planetary (6)

Launch pads target a `Planet`; landing pads receive from other planets.

| type | New fields |
|------|------------|
| `PlanetaryItemLaunchPad` | `maxPath` |
| `PlanetaryItemLandingPad` | — |
| `PlanetaryLiquidLaunchPad` | `maxPath`, `launchVolume` |
| `PlanetaryLiquidLandingPad` | `landingVolume` |
| `PlanetaryPayloadLaunchPad` | `maxPath`, `payloadCapacity`, `payloadLaunchCount` |
| `PlanetaryPayloadLandingPad` | `payloadSpeed`, `payloadRotateSpeed` |

### Field defaults

| Field | Default | Description |
|-------|---------|-------------|
| `maxPath` | `0` | Max path length (`0` = no limit display); used for path validation when > 0 |
| `launchVolume` | `100` | Liquid volume per launch |
| `landingVolume` | `100` | Liquid volume per landing |
| `payloadCapacity` | `1` | Max payloads held |
| `payloadLaunchCount` | `1` | Payloads launched at once |
| `payloadSpeed` | `0.7` | Landing pad output speed |
| `payloadRotateSpeed` | `5` | Landing pad output rotation speed |
| `consumeLiquidAmount` | vanilla | Set to `-1` on landing pads to disable landing liquid consumption |

Vanilla `LaunchPad` / `LandingPad` fields still apply. Payload blocks need sprites (`-pod`, `-preview`, `-light`, `-in`, `-out`); see [`example/sprites/`](example/sprites/).