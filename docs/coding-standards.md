# 编码规范文档

## 一、核心原则

### 1. 单一职责原则 (Single Responsibility Principle)

**一个类只做一件事。**

- 每个类有且仅有一个修改的理由
- 如果描述一个类时需要用"和"连接多个事情，就该拆分
- 类超过 **300 行** 必须审视是否可以拆分

```
// ❌ 错误：一个类做多件事
class GameScreen {
    handleInput()     // 输入处理
    drawHud()         // UI渲染
    loadMap()         // 地图加载
    saveGame()        // 存档
}

// ✅ 正确：拆分为四个类
class GameScreen      // 纯编排调度
class InputHandler    // 输入处理
class HudRenderer     // UI渲染
class RoomController  // 地图加载
```

### 2. 高内聚 (High Cohesion)

**类内部的所有方法必须围绕同一件事情。**

- 类的每个方法都应该直接服务于该类的唯一职责
- 如果某个方法不操作 `this` 的任何字段，它可能不属于这个类
- 静态工具方法放在专门的工具类中，不与业务类混在一起

### 3. 低耦合 (Low Coupling)

**减少类与类之间的依赖关系。通过构造函数明确声明依赖。**

原则：
- 构造函数参数不超过 **5 个**，超过说明类做了太多事或需要引入中间对象
- 不依赖具体实现，依赖抽象（接口）
- 不使用 `new` 在方法内部创建依赖对象，全部通过构造函数注入
- engine 层 **绝对不能** 导入 LibGDX 的任何类

```java
// ✅ 正确：显式声明依赖
class PlayerMovementController {
    PlayerMovementController(NpcPlaceholderManager npcManager, float tile, ...) {
        // 我需要什么，构造函数说得一清二楚
    }
}

// ❌ 错误：隐藏依赖
class PlayerMovementController {
    void move() {
        NpcPlaceholderManager npm = new NpcPlaceholderManager(...);  // 隐藏了依赖
    }
}
```

### 4. 单向依赖

依赖方向不可逆：
```
infra/  ←  domain  ←  engine  ←  client
(数据)     (模型)     (逻辑)     (UI)
```

- 下层不懂上层。domain 不知道 engine 怎么用自己，engine 不知道 client 怎么展示
- 上层可以依赖下层，同层之间尽量少依赖

---

## 二、类设计规范

### 类的规模

| 规模 | 状态 | 措施 |
|------|------|------|
| ≤ 200 行 | 健康 | 保持 |
| 200~300 行 | 关注 | 审视是否可以拆分 |
| 300~500 行 | 警告 | 必须讨论拆分方案 |
| > 500 行 | 违规 | 必须拆分 |

### 方法的规模

- 单个方法不超过 **40 行**
- 方法只做一件事，命名准确反映其行为
- 组合优于继承，委托优于组合

### 字段规模

- 实例字段不超过 **8 个**，超过说明类承担了太多状态
- 静态字段只放常量，不放可变状态

### 构造函数

- 参数不超过 **5 个**
- 仅做赋值，不做任何计算或调用
- 复杂构建使用 Builder 模式或分步初始化

---

## 三、包结构规范

```
cn.edu.whut.sept.zuul
├── domain/    数据模型，纯 POJO，零依赖
├── engine/    游戏逻辑，纯 Java，零 LibGDX
│   └── effect/  物品使用效果（策略模式）
│       └── combat/  战斗物品效果
├── client/    LibGDX UI 层
│   ├── screen/   Screen 实现 + 功能组件
│   ├── render/   渲染器
│   └── ui/    UI 工具 / 皮肤 / 纹理工厂
└── infra/     基础设施
    ├── 数据加载（CombatLoader, DialogueLoader）
    ├── 世界工厂（WorldFactory）
    └── 序列化/日志（GameState, GameLogger）
```

### 包导入规则

| 包 | 可以导入 | 禁止导入 |
|----|---------|----------|
| domain | 无（或 Java 标准库） | engine, client, infra, LibGDX |
| engine | domain, Java 标准库 | client, LibGDX |
| client | domain, engine, infra | — |
| infra | domain, Java 标准库 | client, LibGDX（除 SaveGameService 外） |

---

## 四、命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| Screen 实现 | `XxxScreen` | `GameScreen`, `TitleScreen` |
| 控制器 | `XxxController` | `RoomController`, `CameraController` |
| 渲染器 | `XxxRenderer` | `HudRenderer`, `UtCombatRenderer` |
| 管理器 | `XxxManager` | `CombatManager`, `DialogueManager` |
| 面板 | `XxxPanel` | `InventoryPanel` |
| UI 处理器 | `XxxUi` 或 `XxxInputHandler` | `DialogueUi`, `InventoryInputHandler` |
| 工厂 | `XxxFactory` | `TextureFactory`, `WorldFactory` |
| 工具类 | `XxxUtils` | `UiDrawUtils` |

- 方法名用动词开头：`drawHud()`, `loadCurrentRoom()`, `applyMovement()`
- 布尔查询用 `is` / `has` / `can` 开头：`isOpen()`, `isInCombat()`, `canMove()`
- 不使用拼音、不缩写（除非是通用缩写如 `Hp`, `Npc`, `Ut`）

---

## 五、禁止事项

### 上帝类
- **禁止** 一个类超过 500 行
- **禁止** 一个类有超过 3 种不同职责
- 如果你要说"这个类负责 A、B、C"，说明该拆了

### 隐藏依赖
- **禁止** 在方法内部 `new` 依赖对象
- **禁止** 使用 `Singleton` / `static` 持有可变状态
- 所有依赖通过构造函数明确传入

### 硬编码数据
- **禁止** 游戏数据（道具坐标、NPC 位置、房间拓扑）写在业务逻辑类中
- 应放在 JSON/YAML 配置文件或专用数据类中，与逻辑分离

### 跨层依赖
- **禁止** engine 层导入任何 LibGDX 类
- **禁止** domain 层导入 engine 或 client

### 死代码
- **禁止** 保留永远不会执行的代码（如 `if (false)` 块）
- **禁止** 保留被注释掉的旧代码——Git 有历史记录

---

## 六、新增功能步骤

当需要新增功能时，按以下顺序执行：

1. **确认职责归属**：这个功能属于哪个已有的类？如果是全新的职责，创建新类
2. **声明依赖**：新类需要哪些其他对象？写进构造函数
3. **实现**：控制类不超过 300 行，方法不超过 40 行
4. **集成**：在 GameScreen / GameEngine 等编排类中添加字段和初始化
5. **验证**：`mvn checkstyle:check` 与 `mvn verify` 均通过；涉及 UI 或玩法时再运行游戏测试

---

## 七、自动化质量门禁

每次提交到 `master`、`feature/**` 或面向 `master` 的 Pull Request，GitHub Actions 会自动执行：

1. 编译 Java 源码
2. Checkstyle 规范检查
3. 88 项 JUnit 测试
4. Maven 胖 JAR 打包与产物上传

Checkstyle 与 Maven `verify` 生命周期绑定，任一 error 级违规都会返回非零退出码并阻断 CI。当前规则覆盖命名、行长、未使用导入、星号导入、空块、修饰符顺序、字符串比较以及 `equals/hashCode` 一致性。测试方法允许使用 `场景_预期结果` 形式的下划线命名。

---

## 八、当前项目结构速查

```
69 个源文件，8330 行

client/screen/
  GameScreen.java         454行 — 编排层，唯一职责：组装+调度
  RoomController.java     247行 — 地图加载/出口/spawn
  CameraController.java    80行 — 视口管理
  PlayerMovementController.java 266行 — 移动/碰撞/冲刺
  DialogueUi.java         178行 — 对话渲染+输入
  EncounterUi.java        205行 — 遭遇菜单+战斗输入
  InteractionRenderer.java 119行 — 玩家绘制+交互提示
  InventoryPanel.java     224行 — 背包渲染
  InventoryInputHandler.java 79行 — 背包输入
  HudRenderer.java        141行 — HUD渲染
  ItemPlaceholderManager.java 166行 — 道具占位
  NpcPlaceholderManager.java  170行 — NPC占位
  TitleScreen.java        229行 — 标题画面

client/render/
  PlayerRenderer.java     187行 — 角色动画
  UtCombatRenderer.java   196行 — UT战斗渲染

client/ui/
  GameUiSkin.java         355行 — 皮肤绘制API
  TextureFactory.java     212行 — 程序化纹理
  UiDrawUtils.java        256行 — UI绘制工具
  WorldMapRenderer.java   122行 — 地图节点渲染
  WorldMapTopology.java    79行 — 地图拓扑数据
  GameFonts.java          128行 — 字体加载

engine/
  GameEngine.java         506行 — 外观编排
  ItemManager.java        116行 — 物品增删改用
  DialogueManager.java    107行 — 对话流程
  CombatManager.java      159行 — 战斗生命周期
  QuestManager.java       112行 — 任务状态
  CombatEngine.java       234行 — 回合制战斗
  UndertaleCombatEngine.java  333行 — UT弹幕战斗
  ...

domain/ — 10个数据模型类（无游戏逻辑）
infra/  — 6个基础设施类（数据加载/持久化）
```
