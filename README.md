# ⚔️ Chronicle of the Lost Realms

> *"王座之下，绝不容你放肆。"* — 王座守卫，HP 还剩一半时

[![Java 8+](https://img.shields.io/badge/Java-8+-f89820?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-Build-c71a36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![LibGDX 1.12.1](https://img.shields.io/badge/LibGDX-1.12.1-e74c3c?style=flat-square)](https://libgdx.com/)
[![Tests 80/80](https://img.shields.io/badge/tests-80/80-brightgreen?style=flat-square)](https://junit.org/junit5/)

**WHUT 软件工程课设。一个 15 房间的像素地牢。三个 NPC。三种结局。以及一个会说话的 Undertale 战斗系统。**

---

## 这不是又一个 Zuul 翻版

World of Zuul 给了一个骨架——房间、出口、命令。我们在这个骨架上装了一整套东西：

- **真正的 2D 地图**，不是文本描述。Tiled 编辑器手绘，Kenney Tiny Dungeon 瓦片，角色在地牢里走来走去。
- **Undertale 式弹幕战斗**，不是回合制菜单。FIGHT 是节奏攻击，ACT 影响敌人状态，MERCY 两次能饶恕——但第一次 NPC 会嘲讽你。
- **NPC 有台词，战斗中也有。** 开局战吼、半血挣扎、濒死怒吼、被饶恕后的语气转变——全写在 `assets/combat/*.json` 里，彩色标签驱动。
- **MERCY 和解后自动切对话。** 守卫从"不知天高地厚的闯入者"变成"是我看走了眼"——然后正常交谈窗口弹出，无缝衔接。

跑起来：`mvn clean compile exec:java`，按 E 遇敌，按 2 开打，按两次 4 试试。

---

## 地牢里有什么

```
地窖入口 ──→ 走廊 ──→ 武器库（生锈长剑）
                │
                ├──→ 锻造间
                ├──→ 王座大厅 ← 守卫把守（Undertale 战斗）
                ├──→ 传送室（随机跳转）
                └──→ 宝库（上锁，需要钥匙）
                
        共 15 个房间，非线性探索
```

三个 NPC：

| 谁 | 在哪 | 怎么对待 |
|---|---|---|
| **王座守卫** | 王座大厅 | 打也行，饶也行。饶两次他态度会变 |
| **隐士** | 地牢深处 | 提供线索，推动任务线 |
| **商人** | 走廊附近 | 交易（开发中） |

根据你怎么对待守卫和隐士，走向三种结局：光明、暗影、中立。

---

## Undertale 战斗怎么玩

进入战斗后：

```
┌──────────────────────────────────────┐
│          [NPC HP ████░░░░]           │
│                                      │
│     ● ← 你的灵魂（WASD 移动）        │
│     ○ ○  ○ ← 弹幕（躲！）           │
│                                      │
│         [节奏攻击条 ██|░░]           │
│                                      │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐│
│  │1 FIGHT│ │2 ACT │ │3 ITEM│ │4 MERCY││
│  └──────┘ └──────┘ └──────┘ └──────┘│
└──────────────────────────────────────┘
```

NPC 会在关键时刻弹出底部台词——开局、半血、濒死、被饶恕。按 Enter 继续，然后敌人会反击（弹幕来了）。

**你的回合：**
- **1 FIGHT** — 进入节奏攻击，看准时机按 Enter，越靠近中心伤害越高
- **2 ACT** — 调查/交谈/威吓，影响敌人状态
- **3 ITEM** — 使用背包里的物品
- **4 MERCY** — 饶恕。第一次 NPC 嘲讽你然后继续揍你，第二次才真的和解

---

## 操作一图流

```
移动     WASD / 方向键
互动     E（对话 / 拾取 / 遇敌）
背包     I          世界地图  M
攻击     J          冲刺      F
调查     Q          回退      B
存档     F5         读档      F9
暂停     ESC

战斗     1=FIGHT  2=ACT  3=ITEM  4=MERCY
         Enter=攻击/台词继续  WASD=躲弹幕
```

---

## 跑起来

```bash
# 编译 + 运行
mvn clean compile exec:java

# 跑测试（80 个，全绿）
mvn test
```

需要 Java 8+、Maven、支持 OpenGL 2.0 的 GPU。

---

## 项目怎么组织的

```
src/main/java/cn/edu/whut/sept/zuul/
├── client/          # LibGDX 屏幕、渲染器、Overlay
│   ├── screen/      # GameScreen / EncounterUi / 对话UI
│   └── render/      # UtCombatRenderer（弹幕/灵魂/节奏条）
├── engine/          # 游戏逻辑心脏
│   ├── GameEngine           # 总调度
│   ├── UndertaleCombatEngine # UT 战斗引擎（菜单→弹幕→循环）
│   └── effect/              # 物品效果 / 战斗状态
├── domain/          # 数据模型
│   ├── NpcCombatDef          # NPC 战斗 JSON 映射（含 BattleLine 台词）
│   ├── Dialogue              # 对话树模型
│   └── Player / Room / Item
└── infra/           # JSON 加载、存档服务、状态持久化

assets/
├── combat/guard.json    # 守卫战斗数据 + battleLines 台词
├── combat/hermit.json   # 隐士战斗数据
├── dialogue/            # NPC 对话 JSON
├── maps/                # Tiled .tmx 地图
└── images/audio/        # 精灵、音效

docs/   # 需求规格、设计说明、分工、开发进度
```

分层思路：**Client 只管画**，**Engine 只管逻辑**，**Domain 只管数据**，**Infra 只管读写**。渲染器不碰引擎状态，引擎不碰 LibGDX API。

---

## 文档

正经课设文档都在 `docs/` 下面：

- `01-需求规格说明书.md`
- `02-软件设计说明书.md`
- `03-项目计划与三人分工.md`
- `07-当前开发进度.md`

---

## 技术选型

| 为什么用这个 | 技术 |
|---|---|
| 课设硬要求 | Java 8 |
| 2D 游戏最省事的框架 | LibGDX 1.12.1 |
| 地图不用手写坐标 | Tiled 编辑器 (.tmx) |
| 编译不用配 classpath | Maven |
| 80 个测试保证改不坏 | JUnit 5 |

素材：Kenney Tiny Dungeon（CC0）、Pixelfrog Tiny Questers（CC0）。

---

## 团队

三人课设，按模块分工：

- **成员 A** — 世界构建 / 地图制作 / NPC 对话数据
- **成员 B** — 引擎 / 战斗系统 / 结局逻辑
- **成员 C** — 客户端渲染 / UI / 存档系统

---

<p align="center">
  <i>一个 Zuul 骨架 + LibGDX 皮囊 + Undertale 灵魂的小地牢。</i>
</p>
