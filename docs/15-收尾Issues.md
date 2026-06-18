# GitHub Issues — 冲刺收尾

> 复制到 https://github.com/wutcst/kai-fa-followtaffynia/issues 逐一创建

---

## Issue A：祭司/信徒战斗改为切磋（半血投降+直接给物品）

**Assignees**：成员B
**Labels**：bug, engine

祭司（priest）和信徒（follower）的试炼战斗同属切磋性质，应和学徒一样：
- `markDefeated: false`
- 半血自动投降→物品直接进背包（光明印记/暗影之契）
- 战后 NPC 不消失，可重复对话

---

## Issue B：中立结局 CG 粒子效果调整

**Assignees**：成员C
**Labels**：enhancement, client

中立结局当前粒子复用默认书页效果，应改为：
- 灰金色粒子缓慢飘落（`docs/14-结局CG美术需求.md` 已定义）
- 王座剪影颜色匹配 NEUTRAL 的灰金色标题色

---

## Issue C：对话立绘检查

**Assignees**：成员B + 成员C
**Labels**：bug, client

验证 8 个 NPC 的对话立绘是否正确显示：
- guard, hermit, merchant, priest, scholar, apprentice, follower, golem
- 特别注意 guard_neutral.json 和 apprentice greeting_balance 节点

---

## Issue D：最终集成测试——三条线走通

**Assignees**：成员A + 成员B + 成员C
**Labels**：test

- 光明线：祭司→金库→隐士→学徒→守卫和平→王座（验证 LIGHT）
- 暗影线：信徒→杀守卫→王座（验证 SHADOW）
- 中立线：拒绝光暗→学者→学徒→守卫中立→王座（验证 NEUTRAL）
- 随便进王座（验证 FAKE）
- 存档/读档往返
- 传送阵互动
- 商人以物易物
