# Chronicle of the Lost Realms

> 基于 World of Zuul 框架拓展的 2D 像素地牢探索 RPG —— WHUT 软件工程课程设计

[![Java 8+](https://img.shields.io/badge/Java-8+-f89820?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-Build-c71a36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![LibGDX 1.12.1](https://img.shields.io/badge/LibGDX-1.12.1-e74c3c?style=flat-square)](https://libgdx.com/)
[![Tests 80/80](https://img.shields.io/badge/tests-80/80-brightgreen?style=flat-square)](https://junit.org/junit5/)

---

## 一、项目简介

本项目以经典教学框架 **World of Zuul**（Michael Kolling & David J. Barnes, *Objects First with Java*）为基础，使用 **LibGDX** 游戏框架构建了一个完整的 2D 像素 RPG。玩家在一个由 15 个手绘 Tiled 地图组成的失落 Realm 中探索，与 8 个 NPC 对话或战斗，收集 20 余种物品，推进 3 条任务线，最终根据玩家的抉择走向 4 种不同结局。

项目的核心创新在于将原本纯文本交互的 Zuul 框架，彻底改造为具备图形化客户端、弹幕战斗、多线叙事和完整存档机制的现代 RPG 游戏。

**技术成果**：15 张 TMX 地图 | 8 个 NPC（6 可战斗） | 20+ 物品 | 3 条任务线 | 4 种结局 | 80 个单元测试 | 完整存档/读档

---

## 二、团队与分工

| 成员 | 负责模块 | 主要产出 |
|------|----------|----------|
| dcr_coof | 世界与地图 | 15 张 TMX 地图（30×15 格，手绘 tileset）；房间拓扑连接；8 个 NPC 的对话 JSON 与战斗 JSON；TMX 对象层 NPC/出口/生成点/传送阵/王座配置；物品放置；tileset 制作 |
| huayif | 引擎与玩法 | GameEngine 全部 API（40+ 公开方法）；Undertale 弹幕战斗引擎（五阶段 + 4 种弹幕模式 + 节奏攻击 + MERCY 双连 + 战斗台词）；QuestManager（3 条任务线）；EndingEvaluator（4 结局多条件判定）；物品效果策略模式；DialogueActionExecutor；80 个单元测试 |
| PercyGeza | 客户端与存档 | LibGDX 屏幕体系（TitleScreen / GameScreen / EndingScreen）；CameraController 设计分辨率系统（1280×720 双摄像机）；HudRenderer（顶栏 5 区 + 底栏日志/按键提示）；DialogueUi（对话状态机 + 选项分支）；EncounterUi（遭遇菜单 1交谈/2战斗/3离开）；InventoryPanel（背包面板）；WorldMapRenderer（世界地图）；SaveGameService（Java 序列化存档）；GameUiSkin（Kenney RPG 九宫格皮肤）；GameAudio（音频 + 程序化波形合成回退）；PlayerRenderer / NpcRenderer（精灵渲染） |

**分支策略**：三人分别维护 `feature/worldAndMap`、`feature/engineAndPlay`、`feature/clientAndDocu` 三个特性分支，按里程碑分阶段合入 `master`。提交遵循 `feat(world)` / `feat(engine)` / `feat(client)` / `test` / `docs` 前缀规范。

---

## 三、游戏概述

### 故事背景

失落的 Realm 中隐藏着古老王座。玩家作为编年史者踏入这片废墟，在探索中遭遇守卫、隐士、祭司、信徒等角色。每一次对话选择与战斗胜负，都将牵引玩家走向光与暗的不同终点。

### 核心玩法

探索 15 个房间 → 收集钥匙与物品 → 与 NPC 对话或战斗 → 解开上锁区域 → 抵达王座大厅 → 触发结局判定。

游戏在玩家进入 `throne-hall` 房间并触碰王座时，调用 `EndingEvaluator.evaluate(player, defeatedNpcs, playerFlags)` 综合玩家持有的物品、声望值、NPC 存活状态和玩家标记，判定最终结局。

### 四种结局

| 结局 | 判定条件（与 EndingEvaluator 源码严格一致） |
|------|------|
| **光明 (LIGHT)** | 持有 `light-mark`（光明印记）+ `gem-light`（光之宝石）+ `guard-medal`（守卫勋章），声望 ≥ 0，守卫存活，隐士存活 |
| **暗影 (SHADOW)** | 持有 `shadow-pact`（暗影之契），且（守卫死亡 **或** 声望 < 0） |
| **中立 (NEUTRAL)** | 持有 `balance-book`（平衡之书）+ `guard-medal`（守卫勋章），拥有 `refused-priest` 和 `refused-follower` 标记（表示拒绝了祭司和信徒的邀请），声望 ≥ 0，守卫存活，隐士存活 |
| **虚幻 (FAKE)** | 以上三个真结局条件均不满足 —— 王座无回应，是唯一的兜底结局 |

### 各结局达成路线

**光明结局路线**：
```
outside → 北 → theatre → E键祭司 → 选「接受试炼」→ 战斗胜利 → 获 light-mark
outside → 南 → lab → 捡 key-vault → 南 → vault → 魔像战 → 获 gem-light
vault → 北 → lab → 北 → outside → 西 → office → 捡 key-guard
outside → 北 → theatre → 东 → library → 北 → hidden-shrine → 隐士对话（+5声望）→ 获 crystal-shard
hidden-shrine → 南 → library → 西 → theatre → 南 → outside → 东 → pub → 东 → garden
garden → 东 → armory → E键学徒 → 切磋获胜 → 获 guard-medal
armory → 西 → garden → 南 → guard-room → E键守卫 → 选「我是编年史者」→ 和平通行 → 南 → throne-hall → 触碰王座 → LIGHT
```

**暗影结局路线**（两种达成方式）：
- 方式 A：cellar 遇信徒 → 接受暗影洗礼 → 战斗获胜 → 获 `shadow-pact` → garden → 南 → guard-room → 杀死守卫（声望 -15）→ throne-hall → **SHADOW**
- 方式 B：获 `shadow-pact` → 不杀守卫但通过其他方式使声望 < 0 → throne-hall → **SHADOW**

**中立结局路线**：
```
outside → 北 → theatre → E键祭司 → 先问暗影 → 选「我拒绝暗影之路」→ 获得 refused-priest 标记
outside → 东 → pub → 南 → cellar → E键信徒 → 选「我拒绝」→ 获得 refused-follower 标记
cellar → 北 → pub → 西 → outside → 北 → theatre → 东 → library → E键学者 → 获 balance-book
library → 西 → theatre → 南 → outside → 东 → pub → 东 → garden → 东 → armory
armory → E键学徒 → 切磋获胜 → 获 guard-medal
armory → 西 → garden → 南 → guard-room → 和平通行 → 南 → throne-hall → 触碰王座 → NEUTRAL
```

**虚幻结局**：以上任一真结局条件不完整（如缺任意关键物品、或未拒绝光暗邀请却也没满足光明/暗影条件）→ 触碰王座 → **FAKE**

---

## 四、操作说明

### 探索操作

| 按键 | 功能 | 说明 |
|------|------|------|
| W/A/S/D 或方向键 | 移动 | 32×32 网格移动 |
| SPACE | 冲刺 | 加速移动，有冷却时间 |
| J | 攻击 | 近战攻击 |
| E | 互动 | 对话 / 拾取物品 / 触发 NPC 遭遇 / 触碰王座 / 传送阵 |
| Q | 调查 | 查看当前房间描述 |
| I | 背包 | 打开/关闭物品列表，选中后 U 键使用 |
| M | 世界地图 | 打开/关闭已探索房间拓扑图 |
| B | 回退 | 返回上一个房间（使用 roomHistory 栈） |
| ESC | 暂停 | 暂停菜单（返回标题 / 继续游戏） |

### 存档操作

| 按键 | 功能 |
|------|------|
| F5 | 快速保存（保存至 `saves/slot1.sav`） |
| F9 | 快速读取（恢复上次保存的游戏状态） |
| T | 返回标题画面 |

### 遭遇菜单

靠近 NPC 按 E 弹出遭遇菜单：

| 按键 | 功能 |
|------|------|
| 1 | 交谈（分支对话） |
| 2 | Undertale 弹幕战斗 |
| 3 | 离开 |

> 注：商人（merchant）不可战斗，其遭遇菜单中选项 2 不可用。

### Undertale 战斗操作

| 按键 | 功能 | 说明 |
|------|------|------|
| 1 | FIGHT | 进入节奏攻击阶段 |
| 2 | ACT | 调查/交谈/威吓，影响敌人状态 |
| 3 | ITEM | 使用背包物品 |
| 4 | MERCY | 饶恕（需连续使用两次才生效） |
| Enter / Space | 攻击确认 | 节奏攻击判定 / 推进战斗台词 |
| WASD | 躲避 | 敌人回合躲避弹幕 |

### 对话中操作

| 按键 | 功能 |
|------|------|
| Enter / Space | 翻页（多页对话） |
| 数字键 1-9 | 选择玩家回复选项 |

---

## 五、技术架构

### 5.1 分层设计

项目遵循严格的**四层架构**，依赖方向为 **Client → Engine → Domain**，Infra 为共享基础设施层。

```
┌──────────────────────────────────────────────────────┐
│  client/   LibGDX 屏幕 · 渲染器 · UI · HUD · 音频   │  ← 依赖 LibGDX
│            24 个类，不含任何游戏逻辑                    │
├──────────────────────────────────────────────────────┤
│  engine/   游戏逻辑核心（零 LibGDX 引用）               │  ← 纯 Java
│            GameEngine · Combat · Quest · Ending       │
├──────────────────────────────────────────────────────┤
│  domain/   纯数据模型（POJO）                          │  ← 无业务逻辑
│            Player · Room · Item · Dialogue · NpcDef   │
├──────────────────────────────────────────────────────┤
│  infra/    基础设施（跨层共享）                         │  ← JSON/存档
│            WorldFactory · SaveGameService · Loader    │
└──────────────────────────────────────────────────────┘
```

**关键边界规则**：
- Client 通过 `GameEngine` 公开 API 操作游戏状态，**绝不**直接修改 Room / Player 字段
- Engine 不导入任何 LibGDX 类（`com.badlogic.gdx.*`），可脱离图形界面独立运行和测试
- Domain 为纯 POJO，不含任何业务逻辑或框架依赖
- 80 个单元测试全部覆盖 Engine 和 Infra 层，无需图形环境即可运行

### 5.2 GameEngine API 接口

所有 GUI 操作通过 Engine 的公开方法调用，杜绝字符串命令分发。核心方法包括：

**移动与探索**：`movePlayer(Direction)`（含碰撞/锁门/传送/结局判定）、`moveBack()`、`look()`、`tryTriggerEnding(cx, cy)`、`tryTriggerTeleport(cx, cy)`

**物品管理**：`takeItem(id)`（含负重检查）、`dropItem(id)`、`dropAllItems()`、`useItem(id)`、`eatItem(id)`、`checkItemUse(id)`、`barterJunkForHerbs()`（商人以物易物）

**NPC 交互**：`startNpcEncounter(npcId)`（返回 EncounterMenu，含 canTalk/canFight 状态）、`talkNpc(npcId)`、`talkNpcWithPrefix(npcId, prefix)`、`chooseDialogueOption(index)`、`endDialogue()`

**战斗**：`startCombat(npcId)`、`startCombat(npcId, mode)`、`combatAction(action, itemIdOrNull)`、`applyCombatOutcome()`

**状态查询**：`getPlayer()`、`getCurrentRoom()`、`getEntryDirection()`、`isInDialogue()`、`isInCombat()`、`isPlayerDead()`、`getCurrentEnding()`、`getLastMessage()`、`isUndertaleCombat()`

**标记与门锁**：`setFlag(flag)`、`hasFlag(flag)`、`isLockUnlocked(lockId)`、`unlockLock(lockId)`、`giveItem(itemId)`

**存档**：`captureState()`、`restoreState(state)`

### 5.3 设计分辨率系统

游戏采用固定设计分辨率 **1280×720**（16:9），所有 UI 坐标在此虚拟空间中定义。`CameraController` 负责将设计空间映射到物理屏幕：

- **缩放计算**：`scale = min(screenWidth / 1280, screenHeight / 720)`，保持等比缩放
- **黑边处理**：非 16:9 屏幕自动添加 letterbox 黑边（上下或左右居中）
- **双摄像机体系**：
  - `worldCamera`：跟随玩家，渲染地图像素坐标（Tiled 地图空间）
  - `uiCamera`：固定正交投影 1280×720，渲染所有 HUD 与 UI 面板
- **关键常量**：`TOP_MARGIN = 80`（72px 顶栏 + 8px 间距）、`BOTTOM_MARGIN = 72`、`WORLD_MARGIN_H = 12`

HUD 栏背景使用独立的 `OrthographicCamera` + `ShapeRenderer` 绘制，按物理屏幕宽度渲染，确保在 letterbox 区域也能完整覆盖。

### 5.4 游戏主循环 (GameScreen.render)

每帧执行顺序：
1. 检查结局状态 → 若已触发则绘制结局画面并返回
2. 检查战斗状态 → 若活跃则更新战斗引擎 + 渲染战斗画面
3. 处理背包/世界地图面板输入（若面板打开）
4. 更新玩家移动（WASD 输入 → 碰撞检测 → 位移）
5. 检测出口重叠（玩家碰撞箱 vs TMX 出口对象）→ 调用 `engine.movePlayer()`
6. 检测交互对象重叠（物品/NPC/王座/传送阵）→ 显示互动提示
7. 渲染世界层：Tiled 地图（ground → wall → decor）+ 按 Y 坐标排序的实体精灵
8. 渲染 UI 层：顶栏 HUD + 底栏日志/按键提示 + 对话/背包/地图覆盖面板
9. 淡入淡出过渡（跨房间切换时，0.3-0.5 秒）

### 5.5 技术栈

| 维度 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 8+ |
| 游戏框架 | LibGDX（LWJGL3 后端） | 1.12.1 |
| 构建工具 | Maven | 3.x |
| 地图编辑 | Tiled | 1.10+ |
| 字体渲染 | FreeType（Fusion Pixel Font，支持中文） | gdx-freetype |
| 测试框架 | JUnit 5 | 5.10.2 |
| 音频 | WAV 音乐 + OGG 音效 + 程序化波形合成回退 | — |
| 存档 | Java 序列化（GameState implements Serializable） | — |
| 代码检查 | Checkstyle | 3.3.1 |
| CI | GitHub Actions（每个 push 自动 `mvn test`） | — |

---

## 六、核心系统设计

### 6.1 地图与房间系统

#### 6.1.1 TMX 地图规范

全部 15 张地图使用 **Tiled 编辑器** 手绘，统一规格：

| 参数 | 值 |
|------|-----|
| 地图尺寸 | 30 格 × 15 格（tiles） |
| 瓦片大小 | 32 × 32 px |
| 底图尺寸 | 960 × 480 px |
| 图层顺序 | `ground`（地板）→ `wall`（碰撞）→ `decor`（装饰）→ `objects`（交互对象） |

**图块集**：
- `tilesets/realm-tiles.png`：自制地牢 tileset
- `assets/kenney_tiny-dungeon/`：Kenney Tiny Dungeon 瓦片（CC0）

**objects 层对象规范**（全部为 32×32 矩形）：

| 对象 type | 用途 | 关键属性 |
|-----------|------|----------|
| `spawn` | 玩家生成点 | `direction=north/south/east/west/default` |
| `exit` | 房间出口 | `direction` + `targetRoomId` |
| `door` | 上锁门（覆盖在 exit 上） | `lockId` + `direction` |
| `npc` | NPC 放置 | `npcId`（由 NpcPlaceholderManager 解析） |
| `item` | 物品放置 | `itemId`（由 ItemPlaceholderManager 解析） |
| `trigger` | 触发器（王座/传送阵） | `actionType`（由 RoomController 解析） |

#### 6.1.2 房间连接拓扑

以下为完整的 15 房间双向连接表（与 `WorldFactory.java` 严格一致）：

| 起始房间 | 方向 | 目标房间 | 备注 |
|----------|------|----------|------|
| outside | N | theatre | 起始房间，4 出口 |
| outside | E | pub | |
| outside | S | lab | |
| outside | W | office | |
| theatre | E | library | |
| pub | S | cellar | |
| pub | E | garden | |
| lab | E | vault | vault 有锁 `vault-door`，需 `key-vault` |
| library | N | hidden-shrine | |
| library | E | teleport-alcove | 传送房 |
| garden | S | guard-room | guard-room 有锁 `guard-gate`，需 `key-guard` 或守卫对话开门 |
| garden | E | armory | |
| guard-room | S | throne-hall | 结局房间 |
| guard-room | W | armory | |
| armory | S | forge | |

> 以上所有连接均为**双向**。例如 `outside → N → theatre` 同时意味着 `theatre → S → outside`。

#### 6.1.3 特殊房间

| roomId | 特殊属性 | 说明 |
|--------|---------|------|
| `outside` | 起始房间 | 新游戏/读档入口，4 个出口 + 5 个 spawn 点 |
| `vault` | 上锁 `lockId=vault-door` | 需 `key-vault`（在 lab 找到） |
| `guard-room` | 上锁 `lockId=guard-gate` | 需 `key-guard`（在 office 找到）或守卫对话开门 |
| `teleport-alcove` | 传送 `isTeleport=true` | 进入后立即随机传送到另一房间（throne-hall 除外） |
| `throne-hall` | 结局房间 | 触碰王座触发结局判定，被传送排除 |

#### 6.1.4 生成点 (Spawn) 方向匹配规则

玩家从房间 X 通过出口进入房间 Y 时，`entryDirection` 记录进入方向。房间 Y 根据 `entryDirection` 选取对应的 spawn 点放置玩家：

| 进入方向（entryDirection） | 匹配的 spawn direction | spawn 在地图上的位置 |
|--------------------------|----------------------|-------------------|
| NORTH | south | 地图下方（玩家从北侧进入，出现在南侧） |
| SOUTH | north | 地图上方 |
| EAST | west | 地图左侧 |
| WEST | east | 地图右侧 |
| DEFAULT | default | 地图中央 |

**设计原则**：spawn 放在"进来的对侧"，模拟玩家从该方向走进房间的视觉效果。

#### 6.1.5 墙壁碰撞与出口检测

- **碰撞检测**：读取 Tiled 地图 `wall` 图层的瓦片数据。任何非零格子视为阻挡，玩家无法穿越。检测基于玩家碰撞箱（32×32 矩形）与墙壁格子的 AABB 重叠判定。
- **出口检测**：每帧遍历当前房间 `objects` 层中 `type=exit` 的对象矩形。当玩家碰撞箱与 exit 矩形发生重叠时，调用 `engine.movePlayer(exitDirection)`。Engine 内部执行锁门检查、房间切换、传送解析和结局判定。
- **交互对象检测**：同理检测 `type=trigger` 对象（王座/传送阵）和物品拾取区域，在接近时显示 E 键互动提示。

#### 6.1.6 传送机制

传送房（`teleport-alcove`）的工作流程：
1. 玩家通过 `movePlayer` 成功进入传送房
2. 在同一 `movePlayer` 调用内，Engine 立即执行 `currentRoom = WorldFactory.randomRoomExcept(fromId)`
3. `entryDirection` 置为 `DEFAULT`
4. 触发闪白特效 + 加载新地图
5. 玩家落在目标房的 `default` spawn 点

传送排除 `throne-hall`（结局房间不可通过随机传送到达），`roomHistory` 仍记录进入传送房之前的历史，保证 `moveBack` 可正常回退。

---

### 6.2 NPC 系统

#### 6.2.1 NPC 列表

游戏中共 8 个 NPC，其中 6 个可战斗（仅 merchant 被 `CombatManager.startNpcEncounter()` 排除战斗，scholar 无战斗 JSON 但代码未硬编码排除）：

| NPC ID | 位置 | 可战斗 | 对话 | 描述 |
|--------|------|--------|------|------|
| guard | garden / guard-room | 是 | guard.json | 王座守卫，根据游戏状态在两房间切换出现 |
| hermit | hidden-shrine | 是 | hermit.json | 守秘隐士，提供线索 |
| merchant | forge | 否 | merchant.json | 商人，以物易物（破烂换药草） |
| priest | theatre | 是 | priest.json | 守光祭司，光明路线引导者 |
| follower | cellar | 是 | follower.json | 暗影信徒，暗影路线引导者 |
| scholar | library | 否 | scholar.json / scholar_neutral.json | 学者，中立路线引导者 |
| apprentice | armory | 是 | apprentice.json | 学徒，切磋可获得守卫勋章 |
| golem | vault | 是 | — | 金库魔像，自动触发战斗守卫宝石 |

#### 6.2.2 守卫位置状态机

`NpcPlaceholderManager.shouldSkip()` 通过状态机决定守卫在 `garden` 还是 `guard-room` 出现：

| 游戏状态 | garden 守卫 | guard-room 守卫 |
|----------|-----------|----------------|
| 门锁着，未互动 | 出现（守卫在门外） | 不出现 |
| 友好对话开门 | 消失 | 出现（守卫进入室内） |
| 战斗杀死 | 永久消失 | 永久消失 |
| 用钥匙自行开门 | 消失 | 出现（守卫在里面） |

---

### 6.3 对话系统

#### 6.3.1 数据驱动

对话数据以 JSON 格式存储在 `assets/dialogue/<npcId>.json`（共 8 个文件，scholar 有普通和中立两个版本），由 `DialogueLoader` 加载为 `DialogueTree`（对话节点树）结构。每个节点包含：

```json
{
  "id": "greeting",
  "text": "你来到了王座之前。\n\n守卫凝视着你。",
  "actions": ["unlock:guard-gate", "reputation:+5"],
  "options": [
    { "text": "我是编年史者，请求通行。", "nextId": "guard_pass" },
    { "text": "我偏要闯过去！", "nextId": "guard_fight" }
  ]
}
```

#### 6.3.2 UI 渲染

`DialogueUi` 管理对话状态机，渲染规格：
- **位置**：底部占屏幕 28% 高度，暗色背景 + 橙色边框
- **NPC 立绘区**（左侧）：NPC 说话时高亮，玩家选择时变灰
- **玩家立绘区**（右侧）：当前说话方高亮，对方变灰
- **对话文本**：居中显示，多页以 `\n\n` 分割，Enter 翻页
- **选项显示**：仅在翻完所有页面后显示金色数字选项（`isAtChoicePoint()` 判定），避免干扰阅读
- **玩家选择回显**：选择后绿色高亮显示「玩家说的话」，NPC 回应同时显示
- **世界空间气泡**：靠近 NPC 时头顶显示对话提示

#### 6.3.3 对话动作 (DialogueAction)

`DialogueActionExecutor` 解析节点中的 `actions` 数组，支持以下动作指令：

| 动作格式 | 效果 | 示例 |
|----------|------|------|
| `unlock:<门ID>` | 解锁指定门锁 | `unlock:guard-gate` — 守卫对话开门 |
| `reputation:<±N>` | 调整声望值 | `reputation:-15` — 杀害守卫后声望下降 |
| `give:<物品ID>` | 给予玩家物品 | `give:balance-book` — 学者给予平衡之书 |
| `flag:<标记>` | 设置玩家标记（影响结局判定） | `flag:refused-priest` — 拒绝祭司邀请 |
| `barter` | 触发物物交换 | 商人：破烂换药草（每件换 1 株） |
| `quest:complete:<ID>` | 完成任务 | 预留扩展 |

#### 6.3.4 对话与战斗的衔接

- **对话后切战斗**：玩家选择挑衅/接受审判选项 → 节点 action 触发 `engine.startCombat()`
- **战斗 MERCY 后切对话**：第二次 MERCY 和解成功 → 战斗引擎调用 `talkNpcWithPrefix()` → 从 `mercy_` 前缀节点开始 → `startDialogue()` 无缝切入对话窗口

---

### 6.4 战斗系统

游戏提供两种战斗模式（通过 `CombatSystem` 接口切换），默认为 Undertale 弹幕模式。

#### 6.4.1 Undertale 弹幕战斗

**五阶段状态机**（`UndertaleCombatPhase` 枚举）：

```
┌──────────┐   FIGHT    ┌───────────┐   判定完成   ┌────────────┐
│  MENU     │ ────────→ │ FIGHT_BAR │ ──────────→ │ ENEMY_TURN  │
│ 1/2/3/4  │            │ 节奏攻击   │             │ NPC弹幕反击 │
└──────────┘            └───────────┘             └──────┬─────┘
     │                                                    │
     ├──── ACT ──→ ACT_TEXT ──→ ENEMY_TURN ──────────────┘
     │
     ├──── ITEM ──→ MENU (物品使用后回到菜单，再进敌人回合)
     │
     └──── MERCY ──→ 第1次: 台词嘲讽 + ENEMY_TURN
                     第2次: 台词和解 → RESULT → 切对话
```

**各阶段详细说明**：

| 阶段 | 玩家操作 | 描述 |
|------|----------|------|
| **MENU** | 按 1-4 选择 | 显示四按钮：FIGHT / ACT / ITEM / MERCY |
| **FIGHT_BAR** | Enter 时机判定 | 光标以 1.55× 速度左右摆动。完美（中心 ±5%）→ 100% 伤害 / 正常（±25%）→ 80% / 偏离（±50%）→ 50% |
| **ACT_TEXT** | 自动过渡 | ACT 结果文本展示后直入敌人回合 |
| **ENEMY_TURN** | WASD 躲避弹幕 | 持续 5.8 秒，NPC 释放弹幕图案。灵魂红心碰撞箱 16×16 |
| **RESULT** | 自动过渡 | 判定胜负 → 胜利奖励 / 死亡 / MERCY 和解 |

**4 种弹幕模式**（`BulletPattern`，每回合随机选取）：
- **WAVE**（波浪型，10 发，间隔 4）：弹幕呈正弦波曲线从上方下坠
- **BURST**（爆发型，14 发，间隔 4）：从 NPC 位置向四周呈放射状扩散
- **SCATTER**（随机散射型，11 发，间隔 4）：随机方向、随机速度的弹幕群
- **SPIRAL**（螺旋型，18 发，间隔 4）：弹幕围绕中心点螺旋旋转扩散

**ACT 技能效果**（JSON 配置 `actOptions`）：

| ACT 动作 | 效果 | 持续 |
|----------|------|------|
| 调查 | 弹幕减速 30%（`bulletSlow`） | 1 回合 |
| 交谈 | 敌人攻击力下降（`atkDebuff`） | 永久 |
| 威吓 | 敌人防御下降，受到伤害 +6（`defDebuff`） | 永久 |

**MERCY 双连机制**：
- **第 1 次**：NPC 弹出嘲讽台词（`mercy1` battleLine），嘲讽后進入 ENEMY_TURN，弹幕照常
- **第 2 次**：NPC 弹出和解台词（`mercy2` battleLine），战斗结束，`mercyExited = true`，调用方检测后切入对话

**战斗台词系统 (BattleLine)**：
NPC 在特定场景弹出底部画中画台词（占屏幕 28% 高度），彩色边框 + NPC 名字 + 台词正文。Enter 继续，台词期间屏蔽战斗输入。

| 台词键 | 触发条件 | `phaseAfterBattleLine`（后续阶段） |
|--------|----------|-------------------------------|
| `start` | 战斗开始 | MENU（玩家先手） |
| `hp50` | NPC HP ≤ 50%（`ratio <= 0.5f`） | ENEMY_TURN（NPC 反击） |
| `hp10` | NPC HP ≤ 10%（`ratio <= 0.1f`，濒死） | ENEMY_TURN |
| `mercy1` | 第 1 次 MERCY | ENEMY_TURN（NPC 嘲讽 + 弹幕） |
| `mercy2` | 第 2 次 MERCY | RESULT → 和解 → 切换对话 |

台词文本支持颜色标签（`red` / `green` / `blue` / `pink`），存储在 `assets/combat/<npcId>.json` 的 `battleLines` 字段。

#### 6.4.2 传统回合制战斗（备选）

通过 `CombatSystem` 接口切换。标准攻/防/逃/用物品循环，`startCombat(npcId)` 默认使用此模式，`startCombat(npcId, CombatMode.UNDERTALE)` 使用弹幕模式。

#### 6.4.3 战斗数据配置

所有战斗属性通过 `assets/combat/<npcId>.json` 配置（共 6 个文件：guard / hermit / priest / follower / apprentice / golem）：

```json
{
  "npcId": "guard",
  "maxHp": 120,
  "skills": { "slash": { "damage": 15 }, "charge": { "damage": 25 } },
  "actOptions": [
    { "name": "调查", "effect": "slowDown:30" },
    { "name": "交谈", "effect": "enemyAtkDown:3" }
  ],
  "battleLines": {
    "start": { "text": "王座之下，绝不容你放肆。", "color": "red" },
    "hp50": { "text": "…有点本事。但还不够！", "color": "pink" },
    "hp10": { "text": "我…不能倒下…", "color": "blue" },
    "mercy1": { "text": "饶了我？不知天高地厚的闯入者！", "color": "red" },
    "mercy2": { "text": "…是我看走了眼。", "color": "green" }
  }
}
```

#### 6.4.4 战斗渲染

`UtCombatRenderer` 负责全部战斗画面的渲染，包括：战斗场景背景绘制、弹幕实体渲染（圆形/矩形 + 颜色区分）、玩家灵魂红心渲染（红色 16×16）、节奏攻击条渲染（判定区域 + 摆动光标）、NPC/玩家 HP 条、底部四按钮菜单、战斗台词画中画。

---

### 6.5 物品与效果系统

#### 6.5.1 Effect 策略模式

20 余种物品通过 **effect 字符串** 驱动行为。`UseEffectRegistry` 维护 effect 前缀与 `UseEffect` 实现类的映射，新增物品效果只需注册新的策略类。

| effect 示例 | 实现类 | 效果描述 |
|-------------|--------|----------|
| `heal:20` | `HealEffect` | 恢复 20 点 HP |
| `unlock:vault-door` | `UnlockDoorEffect` | 解锁金库门锁 |
| `light:full` | `LightEffect` | 照明效果 |
| `reputation:+5` | `ReputationEffect` | 声望 +5 |
| `maxWeight:+20` | `MaxWeightEffect` | 永久增加负重上限 20 |
| `passive:UT战斗攻击+8，回合制攻击+5` | `PassiveHeldEffect` | 背包中持有即生效的被动增益 |

物品效果还包括 `barter`（物物交换，由对话 action 触发）和 `lore`（知识类物品，仅可阅读）。

#### 6.5.2 负重系统

- 玩家基础负重：**50**
- 每件物品有 `weight` 属性（double 类型）：小物品 0.1~0.5，装备类 14~15
- 拾取时计算当前负重 + 物品重量，超过上限则拾取失败并提示「拾取失败（可能超重）」
- `eatItem("magic-cookie")` 可永久增加负重上限 +20

#### 6.5.3 物品拾取与丢弃

- **拾取**：`engine.takeItem(itemId)` — 从房间移除物品 → 检查负重 → 放入背包（或失败放回房间）
- **丢弃**：`engine.dropItem(itemId)` — 从背包移除 → 放回当前房间
- **全部丢弃**：`engine.dropAllItems()` — 清空背包，所有物品放回当前房间
- **使用**：`engine.useItem(itemId)` → `checkItemUse()` 检查可用性 → `tryUseItem()` 触发 effect 策略

---

### 6.6 任务系统

`QuestManager` 管理 3 条任务线，监听房间进入、物品拾取等事件自动更新状态：

| 任务 ID | 任务名 | 完成条件 |
|---------|--------|----------|
| `vault-seal` | 金库封印 | 拾取 `gem-light`（`onItemTaken` 检测） |
| `throne-approach` | 王座之路 | 进入 `throne-hall`（`onRoomEntered` 检测） |
| `realm-explorer` | Realm 探索者 | 累计探索 ≥ 8 个房间 |

任务状态存储为 Map（`questId → "active" | "completed"`），由 HUD 侧面板读取并动态显示目标文字。

---

### 6.7 结局系统

#### 6.7.1 判定逻辑

`EndingEvaluator.evaluate(Player, Set<String> defeatedNpcs, Set<String> playerFlags)` 综合三项数据判定：

```java
// 光明结局：三步缺一不可（印记 + 宝石 + 勋章）
if (light-mark && gem-light && guard-medal && reputation >= 0 && !guardDead && !hermitDead)
    → LIGHT

// 暗影结局：拥抱暗影，以力破门（契 + 守卫死 或 声望负）
if (shadow-pact && (guardDead || reputation < 0))
    → SHADOW

// 中立结局：拒绝光暗两条邀请，走第三条路
if (balance-book && guard-medal && refused-priest && refused-follower
    && reputation >= 0 && !guardDead && !hermitDead)
    → NEUTRAL

// 不满足任何真结局 → 假结局
→ FAKE
```

**三种真结局与虚幻结局的关系**：LIGHT / SHADOW / NEUTRAL 各需严格满足各自的物品 + 标记 + 声望 + NPC 存活条件。任一条件不满足则跌入 FAKE。FAKE 不代表"失败"——它同样是游戏叙事的一部分，表达主角未能寻得王座的回应。

#### 6.7.2 触发时机

`GameScreen` 中 E 键互动检测王座触发器 → `engine.tryTriggerEnding(cx, cy)` → 内部调用 `endingEvaluator.evaluate()` 并将结果存入 `currentEnding` 字段。

`GameScreen.render()` 首行检查 `engine.getCurrentEnding()`：
- `null` 或 `NONE` → 正常游戏渲染
- 已触发结局 → 跳过所有游戏渲染，显示结局 CG（黑底 + 0.6s 渐显 + 居中标题/描述 + 按 T 返回标题）

#### 6.7.3 结局画面

| 结局 | 标题 | 描述 | 美术方向 |
|------|------|------|----------|
| LIGHT | 光明结局 | 你携光明宝石步入王座大厅，Realm 的记忆被重新点亮。 | 金色光晕 |
| SHADOW | 暗影结局 | 暗影吞噬了王座，编年史者在沉默中离去。 | 暗红滤镜 + 灰烬粒子 |
| NEUTRAL | 中立结局 | 你触碰了王座，却知晓它不属于你。你已走过光暗两端，了解 Realm 的一切。你选择等待永恒之王，以编年史者之名辅佐左右。 | 蓝色粒子 |
| FAKE | 虚幻结局 | 王座空无一物，你所追寻的一切不过是泡影。 | 灰色 + 灰烬飘落 |

---

### 6.8 存档系统

#### 6.8.1 GameState 序列化

`GameState` 实现 `Serializable`，完整快照游戏运行时状态：

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerName` | String | 玩家姓名 |
| `currentRoomId` | String | 当前房间 ID |
| `playerX`, `playerY` | float | 玩家像素坐标 |
| `facing` | Direction | 玩家朝向 |
| `entryDirection` | Direction | 入口方向（影响读档后 spawn） |
| `hp`, `maxWeight`, `reputation` | int | 玩家属性 |
| `inventory` | List\<Item\> | 背包内容 |
| `unlockedLocks` | Set\<String\> | 已解锁门锁 |
| `defeatedNpcs` | Set\<String\> | 已击败 NPC |
| `exploredRoomIds` | Set\<String\> | 已探索房间 |
| `roomHistory` | Deque\<String\> | 房间历史栈（支持 moveBack） |
| `allRoomItems` | Map\<String, List\<Item\>\> | 所有房间的物品状态 |
| `playerFlags` | Map\<String, String\> | 玩家标记（影响结局判定） |
| `questStates` | Map\<String, String\> | 任务状态 |

#### 6.8.2 存取流程

- **保存（F5）**：`SaveGameService.save(engine.captureState())` → 序列化写入 `saves/slot1.sav`
- **读取（F9）**：`SaveGameService.load()` → 反序列化 → `engine.restoreState(state)` → `GameScreen.loadRoom()` + 设置玩家坐标/spawn
- **容错**：无存档时读档提示「没有可用的存档」

---

### 6.9 HUD 与 UI 系统

#### 6.9.1 顶栏 (Top Bar)

设计分辨率 **1280×72**，5 等分区（每区 256px），`|` 竖线分隔：

```
[玩家 Name] | 生命 ████░░ 100/100 | 房间 vault | 负重 ████░░ 25/50 | 声望 150
```

- 生命条：16px 高，红色填充，右侧数字显示 HP/最大HP
- 负重条：16px 高，黄色填充，右侧数字显示当前/上限
- 字体：`smallFont`（FreeType 22px × 0.85 缩放）

#### 6.9.2 底栏 (Bottom Bar)

设计分辨率 **1280×72**，6:4 分割：

- **左侧 60%**：嵌入面板，顶部"日志"标签 + 可滚动消息文本（`actionMessage`），显示最近操作反馈
- **右侧 40%**：7 列按键提示（双行：键名在上，功能在下）

| 按键 | 功能 |
|------|------|
| WASD | 移动 |
| SPACE | 冲刺 |
| J | 攻击 |
| E | 互动 |
| Q | 调查 |
| I | 背包 |
| M | 地图 |

#### 6.9.3 其他 UI 面板

| 面板 | 触发方式 | 描述 |
|------|----------|------|
| **背包面板** | I 键 | 物品列表（含名称/重量/效果描述），选中物品显示详情，支持 U 键使用。被动装备显示「被动」标签 |
| **世界地图** | M 键 | 节点图式渲染 15 房间的拓扑连接，`WorldMapRenderer` 根据 `WorldMapTopology` 坐标矩阵绘制，已探索房间高亮 |
| **暂停菜单** | ESC 键 | 继续游戏 / 保存 / 读档 / 返回标题 |
| **对话 UI** | 自动触发 | 底部 28% 暗色面板 + 橙色边框 + NPC/玩家立绘区 + 选项数字 |
| **遭遇菜单** | 靠近 NPC 按 E | 3 选项：1=交谈 / 2=UT 战斗 / 3=离开 |
| **战斗画面** | 战斗激活时 | 灵魂红心 + 弹幕 + HP 条 + 节奏攻击条 + 四按钮 + 台词画中画 |

---

### 6.10 音频系统

`GameAudio` 管理全部音频播放，具备容错回退机制：

- **4 首音乐音轨**（WAV）：标题画面 / 探索 / 地下城 / 战斗，根据场景自动切换
- **12+ 音效**（OGG）：点击、脚步、冲刺、攻击、拾取、开门、存档、读档、错误提示等
- **程序化回退**：音效文件缺失时，自动使用波形合成（正弦波/方波/三角波/噪声波）生成替代音效
- **音频来源**：Kenney（CC0）、Juhani Junkala（CC0）

### 6.11 精灵与渲染

#### 6.11.1 玩家精灵

`PlayerRenderer` 使用 **Pixelfrog Tiny Questers 战士精灵表**（CC0），包含 4 方向 × 4 动作帧（行走/空闲/攻击/死亡），每帧 32×32 像素。根据 WASD 输入方向切换精灵行，根据移动/静止状态切换动画帧。使用 `TextureFilter.Nearest` 保持像素清晰度。

#### 6.11.2 NPC 渲染

`NpcRenderer` 支持 8 个 NPC（guard / hermit / merchant / priest / follower / scholar / apprentice / golem）的精灵渲染，每个 NPC 有对应的立绘纹理用于对话界面。NPC 通过 `NpcPlaceholderManager` 根据 TMX 对象层的 `type=npc` 对象和游戏状态管理显示/隐藏。

#### 6.11.3 世界空间交互提示

`InteractionRenderer` 在可交互对象（NPC / 物品 / 王座 / 传送阵）附近绘制 E 键互动提示。接近 NPC 时头顶显示世界空间对话气泡。

---

## 七、测试覆盖

全部 80 个测试覆盖 Engine 和 Infra 层，无需图形环境即可运行，CI 在每个 push 上自动执行：

| 测试类 | 数量 | 覆盖范围 |
|--------|------|----------|
| `GameEngineTest` | 26 | 移动（四方向）、传送、门锁、物品拾取/丢弃/使用、cookie 食用、负重边界、传送解析 |
| `GameEngineDialogueQuestTest` | 10 | 对话触发、对话动作解锁、任务进度更新、结局判定流程、存档往返 |
| `CombatEngineTest` | 14 | 攻击/防御/ACT技能/阶段切换/逃跑/死亡/锈剑被动/隐士战斗 |
| `EndingEvaluatorTest` | 13 | LIGHT/SHADOW/NEUTRAL/FAKE 四结局全部路径 + 边界条件 |
| `EndingFlowTest` | 5 | 完整通关流程端到端验证 |
| `WorldTopologyTest` | 7 | 全部 15 房间双向出口正确性、可达性验证 |
| `CombatLoaderTest` | 2 | guard + hermit 战斗 JSON 正确加载与解析 |
| `DialogueLoaderTest` | 2 | 对话 JSON 加载、节点树构建与选项解析 |
| `SaveGameServiceTest` | 1 | 存档序列化往返完整性 |
| **合计** | **80** | `mvn test` 全部通过 |

---

## 八、构建与运行

### 环境要求

- **Java 8+**（推荐 JDK 11 或 17）
- **Maven 3.x**
- 支持 **OpenGL 2.0+** 的 GPU

### 命令

```bash
# 编译并运行
mvn clean compile exec:java

# 仅编译
mvn compile

# 运行全部测试（80 个）
mvn test

# 打包胖 JAR（含全部依赖）
mvn package
java -jar target/zuul-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 起始窗口

960×540（可自由缩放），60 FPS，Vsync，通过 LWJGL3 后端启动。

---

## 九、数据驱动设计

项目核心内容与代码逻辑解耦，通过数据文件驱动游戏行为：

| 内容 | 格式 | 位置 | 说明 |
|------|------|------|------|
| 地图 | `.tmx` (Tiled) | `assets/maps/` | 15 张地图，含墙壁/装饰/对象图层 |
| 对话 | `.json` | `assets/dialogue/` | 8 个对话文件，节点树 + 选项 + 动作 |
| 战斗 | `.json` | `assets/combat/` | 6 个文件，NPC 属性 + 技能 + ACT 选项 + 台词阈值 |
| 物品效果 | effect 字符串 | `WorldFactory.java` + `UseEffectRegistry` | 策略模式，新增物品仅需注册 effect |
| 音频 | `.wav` / `.ogg` | `assets/audio/` | 音乐 + 音效 + 程序化波形合成回退 |
| 字体 | `.ttf` | `assets/fonts/` | Fusion Pixel Font（支持中文，SIL OFL） |
| UI 皮肤 | `.png` (NinePatch) | `assets/ui/kenney-rpg/` | Kenney RPG UI Pack（CC0） |
| 精灵表 | `.png` | `assets/role/` | Pixelfrog Tiny Questers（CC0） |

---

## 十、项目结构

```
├── pom.xml                          # Maven 配置（依赖/插件/主类）
├── CLAUDE.md                        # Claude Code 项目指南
├── README.md                        # 本文件
│
├── src/main/java/cn/edu/whut/sept/zuul/
│   ├── client/                      # 客户端层（24 类）
│   │   ├── RpgMain.java            # LibGDX Game 入口，持有 SpriteBatch / Fonts / Audio
│   │   ├── DesktopLauncher.java    # LWJGL3 启动器，窗口配置
│   │   ├── screen/                 # 屏幕：GameScreen / TitleScreen / CameraController
│   │   ├── ui/                     # UI：HudRenderer / DialogueUi / EncounterUi /
│   │   │                           #      InventoryPanel / InventoryInputHandler /
│   │   │                           #      WorldMapRenderer / WorldMapTopology / GameUiSkin
│   │   ├── render/                 # 渲染：UtCombatRenderer / PlayerRenderer / NpcRenderer /
│   │   │                           #        InteractionRenderer
│   │   └── GameAudio.java         # 音频系统（音乐 + 音效 + 程序化合成）
│   ├── engine/                      # 引擎层（25 类，零 LibGDX 引用）
│   │   ├── GameEngine.java         # 核心引擎（40+ 公开方法）
│   │   ├── UndertaleCombatEngine.java  # UT 弹幕战斗（五阶段 + 4 弹幕模式 + 节奏攻击）
│   │   ├── UndertaleCombatPhase.java   # 战斗阶段枚举（MENU/FIGHT_BAR/ACT_TEXT/ENEMY_TURN/RESULT）
│   │   ├── CombatEngine.java       # 传统回合制备选
│   │   ├── CombatManager.java      # 战斗生命周期管理
│   │   ├── QuestManager.java       # 3 任务管理（vault-seal/throne-approach/realm-explorer）
│   │   ├── EndingEvaluator.java    # 4 结局多条件判定
│   │   ├── EndingType.java         # 结局枚举（LIGHT/SHADOW/NEUTRAL/FAKE/NONE）
│   │   ├── DialogueManager.java    # 对话状态机
│   │   ├── DialogueActionExecutor.java  # 对话动作解析
│   │   ├── ItemManager.java        # 物品拾取/丢弃/使用 + 负重检查
│   │   ├── Bullet.java / BulletPattern.java  # 弹幕实体与模式
│   │   └── effect/                 # 效果策略：UseEffect 接口 + UseEffectRegistry
│   ├── domain/                      # 领域模型层（9 类，纯 POJO）
│   │   ├── Player.java             # 玩家：姓名/HP/负重/声望/背包
│   │   ├── Room.java               # 房间：ID/描述/锁/传送/出口/物品
│   │   ├── Item.java               # 物品：ID/名称/重量/effect 字符串
│   │   ├── Direction.java          # 方向枚举（NORTH/SOUTH/EAST/WEST/DEFAULT）
│   │   ├── RoomScene.java          # TMX 场景元数据 + spawn 匹配
│   │   ├── Dialogue.java / DialogueNode.java / DialogueTree.java  # 对话树模型
│   │   └── NpcCombatDef.java       # NPC 战斗属性 + BattleLine 台词
│   └── infra/                       # 基础设施层（6 类）
│       ├── WorldFactory.java        # 世界构建：15 房间 + 物品 + 连接
│       ├── GameState.java           # 可序列化游戏状态快照（14 字段）
│       ├── SaveGameService.java    # Java 序列化存档服务
│       ├── CombatLoader.java        # 战斗 JSON 加载 + battleLines 解析
│       ├── DialogueLoader.java      # 对话 JSON 加载
│       └── GameLogger.java          # 统一日志
│
├── src/test/java/                   # 测试（80 个）
│   ├── GameEngineTest.java          # 26 测试
│   ├── GameEngineDialogueQuestTest.java  # 10 测试
│   ├── CombatEngineTest.java        # 14 测试
│   ├── EndingEvaluatorTest.java     # 13 测试
│   ├── EndingFlowTest.java          # 5 测试
│   ├── WorldTopologyTest.java       # 7 测试
│   ├── CombatLoaderTest.java        # 2 测试
│   ├── DialogueLoaderTest.java      # 2 测试
│   └── SaveGameServiceTest.java     # 1 测试
│
├── assets/                           # 游戏资源
│   ├── maps/                        # 15 张 TMX 地图
│   ├── dialogue/                    # 8 个对话 JSON
│   ├── combat/                      # 6 个战斗 JSON（guard/hermit/priest/follower/apprentice/golem）
│   ├── audio/music/ + sfx/          # WAV 音乐 + OGG 音效
│   ├── fonts/                       # Fusion Pixel Font (game.ttf)
│   ├── role/                        # Pixelfrog 精灵表
│   ├── ui/kenney-rpg/               # Kenney RPG UI 九宫格皮肤
│   ├── tilesets/                    # realm-tiles.png（自制 tileset）
│   ├── kenney_tiny-dungeon/         # Kenney Tiny Dungeon 瓦片（CC0）
│   └── CREDITS.md                   # 素材来源与许可
│
├── docs/                             # 设计文档
│   ├── 01-需求规格说明书.md
│   ├── 02-软件设计说明书.md
│   ├── 03-项目计划与三人分工.md
│   ├── 07-当前开发进度.md
│   ├── 08-地图结构规格.md
│   ├── 11-三个结局.md
│   └── ...
│
└── .github/workflows/               # CI/CD
    └── maven.yml                    # 每次 push 自动 mvn test
```

---

## 十一、参考资料与素材来源

### 参考资料

- **World of Zuul** — Michael Kolling & David J. Barnes, *Objects First with Java*（项目基础架构）
- **Undertale** — Toby Fox, 2015（战斗系统设计灵感）
- **LibGDX 官方 Wiki** — [libgdx.com/wiki](https://libgdx.com/wiki/)（游戏框架文档）
- **Tiled 编辑器文档** — [mapeditor.org](https://www.mapeditor.org/)（TMX 地图格式参考）

### 美术与音频素材

| 素材 | 来源 | 许可 |
|------|------|------|
| 地图瓦片 | Kenney Tiny Dungeon | CC0 |
| 玩家精灵表 | Pixelfrog Tiny Questers | CC0 |
| UI 皮肤（九宫格） | Kenney RPG UI Pack | CC0 |
| 中文字体 | Fusion Pixel Font | SIL Open Font License |
| 音乐音轨 | Kenney / Juhani Junkala | CC0 |
| 音效 | Kenney / 程序化波形合成 | CC0 / 自制 |
| 自制 tileset | 成员 dcr_coof 手绘 | 自制 |

完整素材清单与许可详见 `assets/CREDITS.md`。

### 内部文档

课设相关设计文档位于 `docs/` 目录，包括需求规格说明书、软件设计说明书（含架构图与序列图）、项目计划与分工、地图结构规格、结局设计等。

---

<p align="center">
  <i>一个 Zuul 骨架 + LibGDX 皮囊 + Undertale 灵魂的小地牢。</i>
</p>
