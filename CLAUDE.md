# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean compile exec:java   # build and run
mvn test                       # run all tests (80)
mvn compile                    # compile only
```

Java 8+, Maven, LibGDX 1.12.1, OpenGL 2.0+.

## Architecture

Four-layer separation with strict boundaries:

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Client** | `client/` | LibGDX screens, rendering, HUD, UI — never touches engine state directly |
| **Engine** | `engine/` | Game logic: room traversal, combat, quests, endings — no LibGDX imports |
| **Domain** | `domain/` | Data models: Player, Room, Item, Dialogue, DialogueNode, NpcCombatDef |
| **Infra** | `infra/` | JSON loading, save/load via GameState serialization |

Client → Engine → Domain; Infra is shared.

### Key client sub-packages

- `client/screen/` — screens (`GameScreen`, `TitleScreen`), `CameraController`, `HudRenderer`, `DialogueUi`, `EncounterUi`, `InventoryPanel`
- `client/ui/` — `GameUiSkin` (NinePatch textures from Kenney RPG pack), `UiDrawUtils`, `GameFonts` (FreeType Chinese font), `WorldMapRenderer`
- `client/render/` — `UtCombatRenderer` (Undertale bullet-hell), `PlayerRenderer`

## Design Resolution & HUD (current feature branch)

The game uses a **fixed design resolution of 1280×720** (16:9). All UI coordinates are in this virtual space, mapped to the physical screen by `CameraController`:

- `CameraController.update()` computes `scale = min(screenW/1280, screenH/720)` and centering offsets
- Two cameras: `worldCamera` (map pixel coords) and `uiCamera` (always ortho 1280×720)
- `applyFullViewport()` — maps design area to physical screen (may letterbox on non-16:9)
- `applyWorldViewport()` — maps world sub-rect within design space

**HUD bar backgrounds** are drawn at full physical screen width via a separate `OrthographicCamera` + `ShapeRenderer` pass in `GameScreen.render()`, bypassing design-viewport clipping for letterbox coverage.

### Top bar (10% height = 72px design)

`HudRenderer.drawHud()` — single-row, 5 equal-width zones with `|` dividers:

```
[玩家 Name] | 生命 ████ 100/100 | 房间 vault | 负重 ████ 25/50 | 声望 150
```

Font: `smallFont` (FreeType 22px × 0.85 scale) for all zones. Progress bars 16px tall.

### Bottom bar (10% height = 72px design)

`HudRenderer.drawFooter()` — 60/40 split:

- **Left 60%**: inset panel with "日志" label + scrollable `actionMessage` text
- **Right 40%**: 7-column key hints (double-row: key name above, function below)
  - WASD/移动, SPACE/冲刺, E/互动, Q/调查, I/背包, M/地图, ESC/菜单

### CameraController constants

- `TOP_MARGIN = 80` (72 bar + 8 gap), `BOTTOM_MARGIN = 72`
- `WORLD_MARGIN_H = 12` (horizontal padding around world viewport)

## Dialogue System

`DialogueUi` manages dialogue state, pagination, and choice handling. Key behavior:

- Dialogue JSON loaded from `assets/dialogue/*.json` via `DialogueManager`
- Multi-page NPC responses split on `\n\n`; Enter advances pages
- Number keys (1-9) select player choices → `playerLastChoice` is set
- `playerLastChoice` is cleared on Enter (page advance) and at dialogue end
- NPC name rendered above NPC portrait (left), "你" above player portrait (right)
- Player choice displayed below NPC response, right-aligned

## Undertale Combat

`UndertaleCombatEngine` handles the bullet-hell combat loop. Entry via `E` near NPC → encounter menu (1 talk / 2 kill / 3 leave / 4 UT combat).

Combat flow: menu → battle line (NPC taunt at thresholds) → bullet pattern → player dodge (WASD) → rhythm attack (Enter timing) → repeat.

Battle line thresholds and text in `assets/combat/*.json` under `battleLines`.

## Map System

Tiled `.tmx` maps in `assets/maps/`, rendered with LibGDX `OrthogonalTiledMapRenderer`. Tile size 32×32. Rooms loaded by `RoomController`, wall collision via tile layer, exits detected by overlap.

## Key Files

| File | Role |
|------|------|
| `GameScreen.java` | Main game screen: input, render orchestration, dialogue/combat overlays |
| `CameraController.java` | Design-resolution viewport system, world/UI camera management |
| `HudRenderer.java` | Top bar (5 zones) + bottom bar (log + key hints) + pause menu |
| `DialogueUi.java` | Dialogue state machine, pagination, choice handling |
| `UndertaleCombatEngine.java` | UT bullet-hell combat engine |
| `GameEngine.java` | Central game state: player, rooms, quests, locks, endings |
| `GameUiSkin.java` | Kenney RPG NinePatch textures for windows, buttons, bars |
| `UiDrawUtils.java` | Text/bars/chips/icon drawing utilities |
| `GameFonts.java` | FreeType font loading (22px base, Chinese support) |
| `DesktopLauncher.java` | LWJGL3 entry point, 960×540 default window |
| `RpgMain.java` | `Game` subclass, owns SpriteBatch, GameFonts, GameAudio |
