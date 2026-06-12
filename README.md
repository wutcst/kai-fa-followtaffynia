# Chronicle of the Lost Realms（失落 Realm 编年史）

WHUT 软件工程课设 — 像素风 2D 桌面 RPG，基于 World of Zuul 框架扩展。

## 技术栈

- **语言**：Java 8+
- **构建**：Maven
- **2D 框架**：LibGDX 1.12.1
- **地图**：Tiled 1.10+（`.tmx`，Kenney Tiny Dungeon 瓦片集）
- **测试**：JUnit 5（80 个测试）

## 快速开始

```bash
mvn exec:java
```

## 操作

| 按键 | 功能 |
|------|------|
| WASD / ↑↓←→ | 移动 |
| E | 互动（对话 / 拾取） |
| I | 背包 |
| Q | 调查 |
| B | 回退 |
| M | 世界地图 |
| J | 攻击 |
| F | 冲刺 |
| F5 / F9 | 保存 / 读档 |
| ESC | 暂停菜单 |

**遭遇 NPC**：1=交流 2=回合制战斗 3=离开 4=UT弹幕战斗

## 世界

15 个互联房间，3 个 NPC（守卫 / 隐士 / 商人），3 种结局（光明 / 暗影 / 中立）。

## 文档

- `docs/01-需求规格说明书.md`
- `docs/02-软件设计说明书.md`
- `docs/03-项目计划与三人分工.md`
- `docs/07-当前开发进度.md`

## 团队

| 成员 | 职责 |
|------|------|
| A | 世界构建 / 地图 / 对话数据 |
| B | 引擎 / 战斗系统 / 结局 |
| C | 客户端 / UI / 存档 |

## 素材来源

- 瓦片集：[Kenney Tiny Dungeon](https://kenney.nl/assets/tiny-dungeon)（CC0）
- 角色精灵：[Tiny Questers Warrior](https://pixelfrog-assets.itch.io/tiny-questers-warrior)（CC0）
- UI 素材：Kenney RPG UI Pack（CC0）
