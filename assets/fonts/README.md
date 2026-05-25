# 字体说明

GUI 中文显示依赖 **TrueType 字体**。程序按以下顺序查找：

1. `assets/fonts/game.ttf`（推荐放入项目，便于跨平台打包）
2. `assets/fonts/NotoSansSC-Regular.ttf`
3. Windows 系统字体：`msyh.ttc`（微软雅黑）、`simhei.ttf` 等

当前项目内置 `Fusion Pixel Font` 的简体中文像素字体版本，文件名为 `game.ttf`。
若中文仍显示为方框，请将任意 `.ttf` 中文字体复制为 `assets/fonts/game.ttf` 后重新运行。

推荐免费字体（需在 README / 本目录 CREDITS 注明出处）：

- [Fusion Pixel Font](https://github.com/TakWolf/fusion-pixel-font)（SIL Open Font License）
- [Noto Sans SC](https://fonts.google.com/noto/specimen/Noto+Sans+SC)（SIL Open Font License）
- [思源黑体 Source Han Sans](https://github.com/adobe-fonts/source-han-sans)
