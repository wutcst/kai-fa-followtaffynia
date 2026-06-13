# Audio Assets

The game loads audio from this folder through `GameAudio`.

## Sound effects

Short sound effects live in `sfx/`. The file names are intentionally simple, so
they can be replaced without changing code:

- `click_*.ogg`: menu and dialogue clicks
- `step_*.ogg`: walking variants
- `pickup_*.ogg`: item pickup variants
- `door_*.ogg`: room transition variants
- `save.ogg`, `load.ogg`, `error.ogg`
- `attack_*.ogg`, `hit.ogg`, `dash.ogg`
- `menu_open.ogg`, `menu_close.ogg`
- `use_*.ogg`

## Music

Background music lives in `music/`:

- `title.wav`: title screen
- `explore.wav`: normal exploration
- `dungeon.wav`: dangerous or enclosed rooms
- `combat.wav`: combat

To replace music later, keep the same file names or update
`GameAudio.musicPathFor`.
