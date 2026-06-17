# GitHub Issues 任务清单

> 复制以下内容到 GitHub Issues 页面逐一创建。
> 仓库：https://github.com/wutcst/kai-fa-followtaffynia/issues

---

## Issue 1：结局画面美化（全屏像素背景 + 粒子特效）

**Assignees**：成员C

当前结局画面为纯黑底+文字渐显。需替换为：
- 光明结局：金色光晕粒子 + 王座大厅背景亮度提升
- 暗影结局：红色滤镜 + 暗影粒子下沉
- 中立结局：灰色调 + 书页飘落粒子

**Labels**：enhancement, client

---

## Issue 2：玩家死亡画面

**Assignees**：成员C

HP≤0 时全屏提示，可选「读档」「返回标题」。
`engine.isPlayerDead()` 已可用，GameScreen 接入即可。

**Labels**：bug, client

---

## Issue 3：商店 UI 弹窗

**Assignees**：成员C

merchant 以物易物逻辑已完成（`barter:` action），但交换结果以纯文本形式显示在 footer。
需改为带物品图标的弹窗——展示"你交出了 X 件杂物，获得了 Y 株草药"。

**Labels**：enhancement, client

---

## Issue 4：TMX decor 层细化

**Assignees**：成员A

当前 15 间房的 decor 装饰层比较稀疏。需在以下房间增加细节：
- theatre：破损座椅、讲台装饰
- pub：吧台细节、酒架
- library：更多散落书籍、蜡烛
- garden：花坛花卉品种增加
- throne-hall：王座细节雕刻、彩窗光斑

**Labels**：enhancement, world

---

## Issue 5：NPC 对话立绘替换

**Assignees**：成员A + 成员C

对话窗口左右两侧当前为色块占位。需替换为真实 NPC 角色图片。
A 提供立绘素材，C 接入 DialogueUi 渲染。

**Labels**：enhancement, client, world

---

## Issue 6：传送阵视觉反馈

**Assignees**：成员C

玩家走到传送阵范围内时，画面边缘加蓝色微光提示"可按 E 传送"。
当前无任何视觉提示，玩家不知道可以互动。

**Labels**：enhancement, client

---

## Issue 7：王座互动视觉提示

**Assignees**：成员C

玩家走到王座范围内时，画面边缘加金色微光提示"可按 E 触碰王座"。
当前无任何视觉提示。

**Labels**：enhancement, client

---

## Issue 8：新 NPC 战斗数据补全

**Assignees**：成员B

以下 NPC 已有 `assets/combat/*.json` 但内容可能不完整：
- priest、apprentice、scholar、follower、golem（5 个）
需检查并补全：maxHp、skills、actOptions、battleLines

**Labels**：enhancement, engine

---

## Issue 9：新 NPC 对话数据补全

**Assignees**：成员B

以下 NPC 已有 `assets/dialogue/*.json` 但内容可能不完整：
- apprentice、follower、priest、scholar（4 个）
需检查对话树节点是否完整、选项是否正确触发 action

**Labels**：enhancement, engine

---

## Issue 10：CI 代码格式检查

**Assignees**：成员B

在 `.github/workflows/maven.yml` 中添加 checkstyle 或 spotless 格式检查步骤。
不符合规范的代码提交时 CI 应报错。

**Labels**：enhancement, ci

---

## Issue 11：客户端测试补充

**Assignees**：成员C

当前 81 个测试全是 engine/infra 层，`client/` 一行测试都没有。
建议至少添加 `GameScreen` 冒烟测试和 `EncounterUi` 单元测试。

**Labels**：test, client

---

## Issue 12：最终集成测试

**Assignees**：成员A + 成员B + 成员C

完整走通三条结局路线，验证：
- 所有物品可拾取
- 所有 NPC 可对话/战斗
- 三条结局均正确触发
- 存档/读档正常工作
- 传送阵正常传送
- 王座互动正常

**Labels**：test

---

## Issue 13：实验报告 + 视频

**Assignees**：成员B（协调）+ 全员

- REPORT.md 填入模板转 docx/pdf
- 按 `docs/12-视频拍摄脚本.md` 录制视频
- 上传 B站，标题前缀 `【武理26软工实践】`

**Labels**：documentation

---

## 创建步骤

1. 打开 https://github.com/wutcst/kai-fa-followtaffynia/issues
2. 点 "New Issue"
3. 依次粘贴上面每个 Issue 的标题和内容
4. 右侧 Assignees 选择对应成员
5. 右侧 Labels 选择对应标签
