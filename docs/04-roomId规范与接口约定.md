# roomId 规范与接口约定

**版本**：2.1  
**日期**：2026-05-25  
**状态**：第 1 周末 API 草案，第 2 周末定稿  

---

## 1. roomId 命名规则

| 规则 | 示例 |
|------|------|
| 小写英文字母 | `outside`, `vault` |
| 多词用连字符 `-` | `admin-office`, `hidden-shrine` |
| 全局唯一 | `outside.tmx` ↔ `roomId=outside` |
| 稳定不变 | 存档、MiniMap、任务均引用 roomId |

**方向枚举**（全项目统一，禁止 `U`/`D` 混用）：`north` / `south` / `east` / `west` / `default`。

---

## 2. 像素与瓦片规格

> 视觉风格、色板、UI、占位三阶段见 **`05-美术资源设计说明书.md`**。

| 参数 | 值 |
|------|-----|
| 图块 | **32×32 px** |
| 窗口 | **960×540 px**（30×17 格） |
| 玩家碰撞盒 | 16×16 px（格中心） |
| 移速 | 4 格/秒（128 px/s） |
| 滤波 | Nearest |

---

## 3. 完整 roomId 列表（15 房间）

### 3.1 出口表（双向成对）

| roomId | 描述 | 出口 |
|--------|------|------|
| `outside` | 主门外广场 | N→theatre, E→pub, S→lab, W→office；庭院由 **S 侧出口区** 进入 garden（见 objects 层 `exit`→garden） |
| `theatre` | 讲堂 | S→outside, E→library |
| `pub` | 酒馆 | W→outside, **S→cellar** |
| `lab` | 机房 | N→outside, E→vault |
| `office` | 行政办公室 | E→outside |
| `library` | 图书馆 | W→theatre, N→hidden-shrine, E→teleport-alcove |
| `cellar` | 地窖 | **N→pub** |
| `vault` | 金库（锁） | W→lab |
| `hidden-shrine` | 隐士神龛 | S→library |
| `garden` | 庭院 | N→outside, S→guard-room |
| `guard-room` | 守卫哨站 | N→garden, S→throne-hall, W→armory |
| `armory` | 军械库 | E→guard-room, S→forge |
| `forge` | 铁匠铺 | N→armory |
| `teleport-alcove` | 传送密室 | W→library；**isTeleport=true** |
| `throne-hall` | 王座大厅（结局） | N→guard-room |

### 3.2 传送房间

| roomId | isTeleport |
|--------|------------|
| `teleport-alcove` | **true** |

**统一规则（与 SRS/SDD 一致）**：

1. `movePlayer` 使玩家**进入** `isTeleport==true` 的房间后；  
2. **立即**执行 `currentRoom = WorldFactory.randomRoomExcept(当前 roomId)`；  
3. `entryDirection = default`，玩家落在目标房 `spawn(direction=default)`；  
4. GUI 播放闪白特效。  

不在「选择出口」时随机，而是**落进传送房之后**再随机。

### 3.3 房间拓扑图（以 §3.1 出口表为准）

```
                         [throne-hall]
                               │ N
                         [guard-room]───[armory]
                               │ N            │
                          [garden]            S
                               │              ▼
              [garden]──N──[outside]───[theatre]───[library]───[teleport-alcove]
                 │          │  │  │                    │
                 S       office lab pub              hidden-shrine
                 │                    │
            [guard-room]            [cellar]──N──[pub]

              [lab]──[vault]     [armory]──[guard-room]；[forge]──N──[armory]
```

实现时以 **§3.1 表格** 为准；示意图仅辅助答辩展示。

---

## 4. 多入口 spawn

| 对象 type | 属性 | 说明 |
|-----------|------|------|
| `spawn` | `direction=north\|south\|east\|west\|default` | 进入本房后的落点 |

**匹配规则**：从房间的 **北侧** 进入 → 使用 `direction=south` 的 spawn（玩家出现在房间南侧）。

| 场景 | 使用 spawn |
|------|------------|
| 新游戏 / 读档无方向 | `default` |
| 正常 movePlayer | 与 `entryDirection` 反向匹配 |
| moveBack 成功 | `entryDirection.opposite()` 匹配 |

`GameState.entryDirection` 必须存档。

**GUI**：`moveBack()` 或 `restoreState()` 后，必须用 `RoomScene.resolveSpawn(entryDirection)` 设置精灵坐标。

---

## 5. Tiled 对象层

图层名：**objects**

| type | 属性 |
|------|------|
| `spawn` | `direction` |
| `exit` | `direction`, `targetRoomId` |
| `item` | `itemId` |
| `npc` | `npcId` |
| `door` | `lockId`, `direction` |
| `trigger` | `questId` |

---

## 6. 对话树 JSON

路径：`assets/dialogue/<npcId>.json`

```json
{
  "npcId": "guard",
  "startNode": "greeting",
  "nodes": {
    "greeting": {
      "text": "站住！报上名来。",
      "options": [
        {"text": "我是编年史者。", "next": "friendly"},
        {"text": "让开。", "next": "hostile"}
      ]
    },
    "friendly": {
      "text": "久仰，请进。",
      "action": "unlock:guard-gate",
      "next": null
    },
    "hostile": {
      "text": "今天你别想过去。",
      "next": null
    }
  }
}
```

| 字段 | 说明 |
|------|------|
| `action` | 可选，如 `unlock:<lockId>`、`reputation:+5` |
| `next: null` | 对话结束 |

A 编写 JSON；B 解析；C 渲染。

---

## 7. 精灵表

> 风格、色板、UI 与三阶段占位策略见 **`05-美术资源设计说明书.md`**。本节仅列技术布局。

### 7.1 玩家 `assets/sprites/player.png`

4 行 × 4 列，每帧 32×32：每行一个方向 idle + walk×3，8 FPS。

### 7.2 NPC `assets/sprites/npc-<npcId>.png`

1×4 或单帧站立。

### 7.3 物品 `assets/sprites/items.png`

16×16 或 32×32 图标集，按 itemId 索引。

---

## 8. 负重与 magic cookie

| 参数 | 值 |
|------|-----|
| 初始 maxWeight | 50 |
| cookie 增加 | +20 |
| cookie 自重 | 1 |
| 轻/普/重物品 | 2–5 / 8–15 / 20–30 |

**随机 cookie**（FR-D04）：新游戏时从 `cellar`、`library`、`hidden-shrine` 中随机 1 间，在 objects 层放置 `itemId=magic-cookie`（或由 WorldFactory 动态注入）。

---

## 9. lockId

示例：`vault-door`、`guard-gate`。已解锁集合存 `GameState.unlockedLocks`。

---

## 10. GameEngine API（冻结）

```java
// Domain
String Room.getRoomId();
boolean Room.isTeleport();
RoomScene Room.getScene();

// Engine — GUI 唯一调用入口
boolean GameEngine.movePlayer(Direction direction);
boolean GameEngine.takeItem(String itemId);
boolean GameEngine.dropItem(String itemId);
void GameEngine.dropAllItems();
boolean GameEngine.useItem(String itemId);
void GameEngine.eatItem(String itemId);
Dialogue GameEngine.talkNpc(String npcId);
String GameEngine.look();
boolean GameEngine.moveBack();
GameState GameEngine.captureState();
void GameEngine.restoreState(GameState state);

// World
Room WorldFactory.build(String playerName);
Room WorldFactory.getRoom(String roomId);
Room WorldFactory.randomRoomExcept(String excludeRoomId);

// Client
GameScreen(GameEngine engine);  // 禁止直接改 Room
```

**冻结节奏**：第 1 周末提交草案 PR；第 2 周末合并定稿，C 据此对接 GameScreen。

---

## 11. GameState 字段

| 字段 | 说明 |
|------|------|
| `playerName` | 标题屏输入 |
| `currentRoomId` | 当前房 |
| `playerX`, `playerY` | 像素坐标 |
| `facing` | up/down/left/right |
| `entryDirection` | 进入当前房的方向（back/spawn 用） |
| `hp`, `maxHp`, `maxWeight`, `reputation` | |
| `inventory` | itemId 列表 |
| `unlockedLocks` | |
| `questStates` | |
| `exploredRoomIds` | |
| `roomHistory` | roomId 栈 |

---

## 12. RoomScene 归属

| 角色 | 权限 |
|------|------|
| A | 创建/修改 tmx、RoomScene |
| B | 只读，用于 Engine |
| C | 只读，用于渲染与 spawn 落点 |

---

## 13. 分支边界

| 分支 | 职责 |
|------|------|
| `feature/worldAndMap` | tmx、tileset、dialogue、WorldFactory |
| `feature/engineAndPlay` | GameEngine、Quest、单测 |
| `feature/clientAndDocu` | client、Save/Load |

---

*文档结束*
