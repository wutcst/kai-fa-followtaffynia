# 🎮 Chronicle of the Lost Realms（失落 Realm 编年史）

> WHUT 软件工程课设 — 像素风 2D 桌面 RPG，基于 World of Zuul 框架扩展，集成 Undertale 式弹幕战斗系统。

![Java](https://img.shields.io/badge/Java-8+-f89820?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-c71a36?style=for-the-badge&logo=apachemaven&logoColor=white)
![LibGDX](https://img.shields.io/badge/LibGDX-1.12.1-e74c3c?style=for-the-badge&logo=libgdx&logoColor=white)
![JUnit](https://img.shields.io/badge/JUnit-5-25a162?style=for-the-badge&logo=junit5&logoColor=white)
![Tests](https://img.shields.io/badge/Tests-80/80-success?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Playable-success?style=for-the-badge)

---

## 📖 项目简介

**Chronicle of the Lost Realms** 是一款以失落王国为舞台的像素风桌面 RPG。玩家在一个由 15 个房间构成的非线性地牢中探索，收集物品、解锁通路、遭遇 NPC，并最终触发三种不同的结局。

项目从经典 *World of Zuul* 文本冒险出发，进行了大幅扩展：

- 🗺️ **Tiled 地图**：使用 Kenney Tiny Dungeon 瓦片集构建完整 2D 地牢
- ⚔️ **双战斗系统**：传统回合制 + Undertale 式弹幕节奏战
- 💬 **分支对话**：NPC 对话树 + 玩家选项 + 立绘画中画
- 🎒 **物品系统**：背包、重量、使用效果、物品组合
- 🏁 **多结局**：光明 / 暗影 / 中立三种结局路线
- 💾 **存档系统**：F5 保存 / F9 读档，支持完整状态恢复

它不是一个简单的 Zuul 翻版，而是一个拥有战斗特效、对话分支、世界地图和完整游戏闭环的中型 Java 游戏项目。

---

## 🖼️ 游戏预览

| 世界探索 | Undertale 战斗 | 对话系统 |
| --- | --- | --- |
| 15 房间地牢 + 世界地图 | 弹幕躲避 + 节奏攻击 | NPC 分支对话 + 立绘 |

> 运行 `mvn exec:java` 进入游戏后按 **E** 与 NPC 互动，按 **2** 进入 Undertale 战斗体验弹幕系统。

---

## ✨ 核心特色

| Feature | Description |
| --- | --- |
| 🎮 桌面 2D 游戏 | 基于 LibGDX 框架，像素风完整游戏窗口体验 |
| 🗺️ Tiled 地图 | 15 个互联房间，Tiled 编辑器制作，支持碰撞图层和出口触发 |
| ⚔️ 双战斗系统 | 传统攻/防/逃回合制 + Undertale 式 FIGHT/ACT/ITEM/MERCY 弹幕节奏战 |
| 🎯 战斗台词系统 | 开战吼/血量阈值/玩家行动后 NPC 弹出画中画台词，支持颜色标签和新行 |
| 💬 分支对话 | NPC 对话树支持多选项分支、立绘颜色块、玩家选择高亮 |
| 🎒 物品系统 | 背包、物品重量、使用效果、钥匙解锁门禁、近身拾取 |
| 🧭 世界地图 | M 键打开全局地图，显示已探索区域和当前位置 |
| 💾 保存 / 读取 | F5 保存 / F9 读档，完整保存玩家状态、房间、物品、战斗进度 |
| 🏁 多结局 | 根据 NPC 击杀/饶恕/对话选择走向光明/暗影/中立结局 |
| 🧪 自动化测试 | 80 个 JUnit 5 测试覆盖核心模块，`mvn test` 一键验证 |

---

## 🧭 游戏世界

游戏世界由 15 个互相连接的地牢房间组成，每个房间承担不同的探索职责。

| 房间 | 描述 |
| --- | --- |
| 地窖入口 | 游戏起点，连接地上与地下世界 |
| 走廊 | 核心枢纽，通往多个方向的通道 |
| 武器库 | 存放生锈长剑等武器 |
| 锻造间 | 锻造与修理相关区域 |
| 王座大厅 | 王座守卫把守的重要区域 |
| 传送室 | 随机传送到其他房间 |
| 宝库 | 上锁房间，需要钥匙解锁 |
| 更多… | 共 15 个房间，支持探索、回头路和锁门机制 |

玩家需要探索房间、拾取物品、使用钥匙、遭遇 NPC，并逐步揭开地牢的秘密。

---

## 👥 NPC 与结局

| NPC | 位置 | 特点 |
| --- | --- | --- |
| 王座守卫 (Guard) | 王座大厅 | Undertale 战斗 + MERCY 双连和解 |
| 隐士 (Hermit) | 地牢深处 | 提供线索和任务对话 |
| 商人 (Merchant) | 走廊附近 | 物品交易（开发中） |

**三种结局：**

1. 🏆 **光明结局** — MERCY 守卫 + 完成隐士任务
2. 🌑 **暗影结局** — 击杀守卫 + 强攻通关
3. ⚖️ **中立结局** — 回避战斗 + 直接通关

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────┐
│                  GameScreen                  │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ 地图渲染  │ │ HUD/UI   │ │ 战斗 Overlay│ │
│  │ Tiled Map│ │ 背包/Footer│ │ UT/传统战斗│ │
│  └──────────┘ └──────────┘ └─────────────┘ │
├─────────────────────────────────────────────┤
│                GameEngine                    │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ Room 管理│ │ NPC 对话  │ │ CombatManager│ │
│  │ 物品拾取  │ │ 任务状态  │ │ 战斗引擎调度  │ │
│  └──────────┘ └──────────┘ └─────────────┘ │
├─────────────────────────────────────────────┤
│              Domain / Infra                   │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐ │
│  │ NpcCombat│ │ Dialogue │ │ SaveGame    │ │
│  │ Def JSON │ │ Loader   │ │ Service     │ │
│  └──────────┘ └──────────┘ └─────────────┘ │
└─────────────────────────────────────────────┘
```

项目采用清晰的分层结构：

- **Client 层**：游戏画面渲染、UI 布局、输入处理、Overlay 管理
- **Engine 层**：游戏逻辑、战斗引擎、对话推进、结局判定
- **Domain 层**：数据模型（物品、NPC、对话、战斗定义）
- **Infra 层**：数据加载、存档服务、日志、世界拓扑

---

## 📁 项目结构

```text
.
├── src/main/java/cn/edu/whut/sept/zuul/
│   ├── client/                    # 客户端（LibGDX Screen）
│   │   ├── screen/                # GameScreen, TitleScreen, EncounterUi
│   │   ├── render/                # UtCombatRenderer, PlayerRenderer
│   │   └── ui/                    # HUD, 世界地图, UI 绘制工具
│   ├── engine/                    # 游戏引擎
│   │   ├── GameEngine.java        # 核心调度
│   │   ├── CombatManager.java     # 战斗管理器
│   │   ├── UndertaleCombatEngine.java  # UT 弹幕战斗引擎
│   │   └── effect/                # 战斗效果 / 物品效果
│   ├── domain/                    # 数据模型
│   │   ├── Player.java, Room.java, Item.java
│   │   ├── Dialogue.java          # 对话模型
│   │   └── NpcCombatDef.java      # NPC 战斗定义（含 BattleLine）
│   └── infra/                     # 基础设施
│       ├── CombatLoader.java, DialogueLoader.java
│       ├── SaveGameService.java
│       └── GameState.java
├── src/test/java/cn/edu/whut/sept/zuul/
│   └── *Test.java                 # 80 个 JUnit 5 测试
├── assets/
│   ├── maps/                      # Tiled .tmx 地图文件
│   ├── combat/                    # NPC 战斗 JSON（guard.json, hermit.json）
│   ├── dialogue/                  # NPC 对话 JSON
│   ├── images/                    # 精灵图、UI 素材
│   └── audio/                     # 音效与音乐
├── docs/                          # 项目文档
│   ├── 01-需求规格说明书.md
│   ├── 02-软件设计说明书.md
│   ├── 03-项目计划与三人分工.md
│   └── 07-当前开发进度.md
├── pom.xml                        # Maven 配置
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.6+
- 支持 OpenGL 2.0+ 的显卡

### 运行游戏

```bash
mvn exec:java
```

### 运行测试

```bash
mvn test
```

80 个自动化测试，覆盖 World 解析、物品、战斗、对话加载、存档等核心模块。

### 完整编译 + 运行

```bash
mvn clean compile exec:java
```

---

## 🎛️ 操作指南

### 探索

| 按键 | 功能 |
| --- | --- |
| WASD / ↑↓←→ | 移动角色 |
| E | 互动（对话 / 拾取物品 / 遭遇 NPC） |
| Q | 调查当前房间 |
| B | 回退到上一个房间 |
| M | 打开 / 关闭世界地图 |
| I | 打开 / 关闭背包 |

### 战斗

| 按键 | 功能 |
| --- | --- |
| J | 近战攻击 |
| F | 冲刺（闪避） |

**遭遇 NPC 菜单：**
- **1** — 交谈（分支对话）
- **2** — Undertale 弹幕战斗（FIGHT/ACT/ITEM/MERCY）

### Undertale 战斗

| 按键 | 功能 |
| --- | --- |
| 1 / 2 / 3 / 4 | FIGHT / ACT / ITEM / MERCY |
| Enter / Space | 节奏攻击确认 / 战斗台词继续 |
| WASD | 弹幕躲避（敌人回合） |

### 系统

| 按键 | 功能 |
| --- | --- |
| F5 | 保存游戏 |
| F9 | 读取存档 |
| ESC | 暂停菜单 |

---

## 🧪 测试

```bash
mvn test
```

测试覆盖：

- ✅ World 解析与房间连接验证（7 项）
- ✅ 物品系统（5 项）
- ✅ 战斗引擎（Undertale + 传统回合制）
- ✅ 对话加载与分支逻辑
- ✅ 存档/读档完整流程
- ✅ NPC 战斗定义 JSON 解析
- ✅ 游戏引擎核心流程

---

## 🧱 技术栈

| Layer | Technology |
| --- | --- |
| Language | Java 8 |
| Build | Maven |
| Game Framework | LibGDX 1.12.1 |
| Map Editor | Tiled 1.10+ (.tmx) |
| Testing | JUnit 5 |
| Assets | Kenney Tiny Dungeon, Pixelfrog Tiny Questers |

---

## 🌟 项目亮点

- 将经典 Zuul 文本冒险升级为 LibGDX 桌面 2D RPG，具备完整游戏体验
- 创新的 **Undertale 式弹幕战斗**：节奏攻击 + WASD 弹幕躲避 + MERCY 双连和解
- **战斗台词画中画**：NPC 在战斗中弹出底部对话 Overlay，彩色标签 + 换行支持
- **丝滑的 MERCY → 交谈衔接**：二次 MERCY 后战斗无缝关闭，自动切入分支对话
- 15 房间非线性地牢，支持锁门、回头路、传送等探索机制
- F5/F9 完整状态存档系统，涵盖位置、物品、战斗、对话进度
- 80 个自动化测试，核心模块覆盖率完整，`mvn test` 一键验证

---

## 👥 团队

| 成员 | 职责 |
| --- | --- |
| A | 世界构建 / 地图制作 / 对话数据 |
| B | 引擎 / 战斗系统 / 结局逻辑 |
| C | 客户端渲染 / UI / 存档系统 |

---

## 📌 Project Identity

**Chronicle of the Lost Realms** 展示了一个经典教学项目如何逐步成长为可运行、可战斗、可对话、可存档的完整 2D RPG 系统。它结合了面向对象设计、LibGDX 图形渲染、数据驱动战斗配置、分支对话引擎和自动化测试，是一个具有完整工程结构的中型 Java 游戏项目。

---

## 📎 素材来源

- 瓦片集：[Kenney Tiny Dungeon](https://kenney.nl/assets/tiny-dungeon)（CC0）
- 角色精灵：[Tiny Questers Warrior](https://pixelfrog-assets.itch.io/tiny-questers-warrior)（CC0）
- UI 素材：Kenney RPG UI Pack（CC0）
