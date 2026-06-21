# Cosmic Out

[English README](README.md)

Mindustry Java 库模组，提供战役星图、区块物流与行星物流。本模组不包含可放置方块，需由依赖它的模组定义方块与贴图。

- 最低版本：`158` · 主类：`mod.CosmicOut` · 名称：`cosmic-out`
- 示例模组：[`example/`](example/)（`cosmic-out-example`）

## 简介

| 体系   | 范围           | type 前缀                                                    |
|------|--------------|------------------------------------------------------------|
| 区块物流 | 同行星，区块 → 区块  | `Item` / `Liquid` / `Payload` + `LaunchPad` / `LandingPad` |
| 行星物流 | 星图网络，行星 → 行星 | `Planetary` + 同上                                           |

以及星图 UI、区块/行星物流统计

## 星图注册

在模组根目录放置 `starmap.hjson`，内容加载时由 `StarMapScanner` 扫描所有已启用模组。

```hjson
planets: [
  { name: sun, axis0: 0, axis120: 0 }
  { name: erekir, axis0: 2, axis120: 0 }
  { name: gier, axis0: 2, axis120: 3, size: 16 }
]
```

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `name` | — | 行星内容 ID（须已在游戏中注册） |
| `axis0` | `0` | 六边形坐标轴 0 |
| `axis120` | `0` | 六边形坐标轴 120°（第三轴 = `-(axis0 + axis120)`） |
| `size` | `1.5` | 星图显示尺寸 |

未知行星会被跳过；同一行星重复注册时后加载的覆盖先前的。未注册星图的行星不能作为 `Planetary*` 目标。

示例：[`example/starmap.hjson`](example/starmap.hjson)

## 方块类型

在 `mod.hjson` 中添加 `dependencies: [cosmic-out]`。示例见 [`example/content/blocks/`](example/content/blocks/)。十二种 type 已在 `ClassMapRegister` 中注册。

### 区块（6 种）

发射台目标为 `Sector`，接收台从其他区块接收。

| type                | 新增 field                                |
|---------------------|-----------------------------------------|
| `ItemLaunchPad`     | —                                       |
| `ItemLandingPad`    | —                                       |
| `LiquidLaunchPad`   | `launchVolume`                          |
| `LiquidLandingPad`  | `landingVolume`                         |
| `PayloadLaunchPad`  | `payloadCapacity`, `payloadLaunchCount` |
| `PayloadLandingPad` | `payloadSpeed`, `payloadRotateSpeed`    |

### 行星（6 种）

发射台目标为 `Planet`，接收台从其他行星接收。

| type                         | 新增 field                                           |
|------------------------------|----------------------------------------------------|
| `PlanetaryItemLaunchPad`     | `maxPath`                                          |
| `PlanetaryItemLandingPad`    | —                                                  |
| `PlanetaryLiquidLaunchPad`   | `maxPath`, `launchVolume`                          |
| `PlanetaryLiquidLandingPad`  | `landingVolume`                                    |
| `PlanetaryPayloadLaunchPad`  | `maxPath`, `payloadCapacity`, `payloadLaunchCount` |
| `PlanetaryPayloadLandingPad` | `payloadSpeed`, `payloadRotateSpeed`               |

### field 默认值

| field                 | 默认值   | 说明                            |
|-----------------------|-------|-------------------------------|
| `maxPath`             | `0`   | 最大路径长度（`0` 不显示限制）；> 0 时用于路径校验 |
| `launchVolume`        | `100` | 单次发射液体量                       |
| `landingVolume`       | `100` | 单次接收液体量                       |
| `payloadCapacity`     | `1`   | 发射台载荷上限                       |
| `payloadLaunchCount`  | `1`   | 单次发射载荷数                       |
| `payloadSpeed`        | `0.7` | 接收台输出速度                       |
| `payloadRotateSpeed`  | `5`   | 接收台输出旋转速度                     |
| `consumeLiquidAmount` | 原版默认  | 接收台设为 `-1` 可关闭落地液体消耗          |

仍可使用原版 `LaunchPad` / `LandingPad` 的全部 field。载荷类需配套贴图（`-pod`、`-preview`、`-light`、`-in`、`-out`），见 [`example/sprites/`](example/sprites/)。