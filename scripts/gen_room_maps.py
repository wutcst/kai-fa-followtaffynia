#!/usr/bin/env python3
"""Generate pixel-art dungeon room maps from the room descriptions doc.
Each room is a 480×256 PNG (30×16 tiles at 16px each)."""

from PIL import Image, ImageDraw
import os

TILE = 16
COLS, ROWS = 30, 16
W, H = COLS * TILE, ROWS * TILE

# ---- Palette (from docs/08) ----
WALL      = (0x3a, 0x3a, 0x4a)
WALL_DARK = (0x2a, 0x2a, 0x38)
FLOOR     = (0x4a, 0x4a, 0x52)
FLOOR_LT  = (0x5a, 0x5a, 0x62)
DOOR_WOOD = (0x6a, 0x4a, 0x2a)
DOOR_IRON = (0x90, 0x90, 0x90)
TORCH_FIRE = (0xe8, 0xa0, 0x30)
TORCH_METAL = (0x40, 0x40, 0x40)
CHEST_WOOD = (0x8a, 0x6a, 0x3a)
CHEST_GOLD = (0xc0, 0xa0, 0x30)
BARREL     = (0x7a, 0x5a, 0x3a)
BOOK       = (0x8a, 0x3a, 0x3a)
BOOK_GOLD  = (0xc0, 0xa0, 0x30)
TABLE_WOOD = (0x6a, 0x4a, 0x2a)
STONE_DARK = (0x5a, 0x5a, 0x5a)
STONE_LT   = (0x6a, 0x6a, 0x6a)
GEM_WHITE  = (0xe8, 0xe0, 0xd0)
GEM_GLOW   = (0xff, 0xe8, 0xc0)
COIN_GOLD  = (0xc0, 0xa0, 0x30)
HERB_GREEN = (0x44, 0xcc, 0x44)
SWORD_RUST = (0x8a, 0x6a, 0x4a)
SHIELD_OAK = (0x7a, 0x5a, 0x3a)
PAPER      = (0xd8, 0xc8, 0x98)
KEY_RUST   = (0xa0, 0x80, 0x40)
KEY_IRON   = (0x80, 0x80, 0x80)
CRYSTAL    = (0x88, 0xcc, 0xff)
DUST_BLUE  = (0xaa, 0xcc, 0xff)
RUNE_BLUE  = (0x44, 0x88, 0xcc)
RUNE_GLOW  = (0x88, 0xcc, 0xff)
FORGE_FIRE = (0xe8, 0x40, 0x20)
FORGE_GLOW = (0xff, 0x88, 0x40)
ANVIL      = (0x60, 0x60, 0x60)
CARPET_RED = (0x6a, 0x1a, 0x1a)
PILLAR     = (0x4a, 0x4a, 0x4a)
PILLAR_LT  = (0x5a, 0x5a, 0x5a)
THRONE     = (0x5a, 0x5a, 0x5a)
MAGIC_BLUE = (0x44, 0x88, 0xcc)
VINE       = (0x4a, 0x6a, 0x3a)
MOSS       = (0x3a, 0x5a, 0x3a)
GUARD_RED  = (0xcc, 0x44, 0x44)
HERMIT_BLUE = (0x44, 0x88, 0xcc)
MERCHANT_GOLD = (0xcc, 0xaa, 0x22)
BLACK      = (0x00, 0x00, 0x00)
WHITE      = (0xff, 0xff, 0xff)


def new_room():
    """Create a blank room image: floor with wall border."""
    img = Image.new("RGB", (W, H), FLOOR)
    draw = ImageDraw.Draw(img)
    # Wall border (2-tile thick for visual weight)
    for x in range(W):
        for y in range(H):
            tx, ty = x // TILE, y // TILE
            if tx == 0 or tx == COLS - 1 or ty == 0 or ty == ROWS - 1:
                # Wall with slight noise
                c = WALL if (tx + ty) % 3 != 0 else WALL_DARK
                img.putpixel((x, y), c)
    return img, draw


def door_h(img, draw, tx, ty, width=2, locked=False):
    """Horizontal door at tile coords, 'width' tiles wide."""
    color = DOOR_IRON if locked else DOOR_WOOD
    for dx in range(width):
        for py in range(TILE):
            px = (tx + dx) * TILE
            py_y = ty * TILE + py
            if py < 2 or py >= TILE - 2:
                continue  # keep wall on top/bottom of door
            if locked and py in (TILE//2 - 1, TILE//2):
                img.putpixel((px + TILE//2, py_y), DOOR_IRON)
            else:
                img.putpixel((px + TILE//2, py_y), color)


def torch(img, tx, ty, wall_side):
    """Place a torch on a wall tile. wall_side: 'n','s','e','w'."""
    cx = tx * TILE + TILE // 2
    cy = ty * TILE + 2
    if wall_side == 'n':
        cy = ty * TILE + 13
    elif wall_side == 's':
        cy = ty * TILE + 2
    elif wall_side == 'e':
        cx = tx * TILE + 2
        cy = ty * TILE + TILE // 2
    elif wall_side == 'w':
        cx = tx * TILE + 13
        cy = ty * TILE + TILE // 2
    # Fire glow
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            if dx*dx + dy*dy <= 9:
                px, py = cx + dx, cy + dy
                if 0 <= px < W and 0 <= py < H:
                    r = max(0, 4 - abs(dx) - abs(dy))
                    blend = min(1.0, r / 4)
                    old = img.getpixel((px, py))
                    nr = int(old[0] + (TORCH_FIRE[0] - old[0]) * blend * 0.3)
                    ng = int(old[1] + (TORCH_FIRE[1] - old[1]) * blend * 0.3)
                    nb = int(old[2] + (TORCH_FIRE[2] - old[2]) * blend * 0.3)
                    img.putpixel((px, py), (nr, ng, nb))


def draw_item(img, tx, ty, color, w_tiles, h_tiles, label=""):
    """Draw a simple colored rectangle for an item."""
    px = tx * TILE + (TILE - w_tiles * TILE) // 2
    py = ty * TILE + (TILE - h_tiles * TILE) // 2
    for dx in range(w_tiles * TILE):
        for dy in range(h_tiles * TILE):
            x, y = px + dx, py + dy
            if 0 <= x < W and 0 <= y < H:
                img.putpixel((x, y), color)


def draw_npc(img, tx, ty, color):
    """Draw NPC as 1x1 tile colored block."""
    draw_item(img, tx, ty, color, 1, 1)


# ============ ROOM DEFINITIONS ============

def room_outside():
    img, draw = new_room()
    # Doors: N(col14-15 row0), S(col14-15 row15), E(col29 row7-8)
    door_h(img, draw, 14, 0)
    door_h(img, draw, 14, 15)
    door_h(img, draw, 28, 7)
    # West door at col0 row7-8 (vertical) — use horizontal on west wall
    door_h(img, draw, 0, 7)
    # Torches in corners
    torch(img, 2, 0, 'n'); torch(img, 27, 0, 'n')
    torch(img, 2, 15, 's'); torch(img, 27, 15, 's')
    # Paper on floor near center-down
    draw_item(img, 14, 9, PAPER, 1, 1)
    # Central circular stone
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx*dx + dy*dy <= 4:
                px = 15*TILE + dx*TILE//2
                py = 8*TILE + dy*TILE//2
                if 0 <= px < W and 0 <= py < H:
                    img.putpixel((px, py), STONE_DARK)
    return img


def room_vault():
    img, draw = new_room()
    # Iron door at north (locked look)
    door_h(img, draw, 12, 0, width=4, locked=True)
    # Polished stone walls
    for x in range(W):
        for y in range(H):
            tx, ty = x // TILE, y // TILE
            if tx <= 1 or tx >= COLS-2 or ty <= 1 or ty >= ROWS-2:
                img.putpixel((x, y), (0x3a, 0x3a, 0x42))
    # Pedestal at center
    draw_item(img, 13, 6, STONE_DARK, 4, 4)
    # Gem on pedestal
    draw_item(img, 14, 5, GEM_WHITE, 2, 2)
    for dx in range(-6, 7):
        for dy in range(-6, 7):
            if dx*dx + dy*dy <= 16:
                px = 15*TILE + dx
                py = 6*TILE + dy
                if 0 <= px < W and 0 <= py < H:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.15 - (abs(dx)+abs(dy))*0.01)
                    nr = int(old[0] * (1-blend) + GEM_GLOW[0] * blend)
                    ng = int(old[1] * (1-blend) + GEM_GLOW[1] * blend)
                    nb = int(old[2] * (1-blend) + GEM_GLOW[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))
    # Gold coins to the right
    draw_item(img, 18, 7, COIN_GOLD, 2, 1)
    torch(img, 2, 7, 'e'); torch(img, 27, 7, 'w')
    return img


def room_armory():
    img, draw = new_room()
    door_h(img, draw, 14, 0)  # W garden
    door_h(img, draw, 14, 15) # S forge
    # Weapon racks on north wall
    for tx in range(4, 26, 3):
        for dy in range(TILE):
            if 0 <= tx*TILE + dy < W:
                img.putpixel((tx*TILE + TILE//2, 1*TILE + dy), TABLE_WOOD)
    # Sword on rack
    draw_item(img, 7, 3, SWORD_RUST, 6, 1)
    draw_item(img, 7, 2, (0x5a, 0x3a, 0x2a), 1, 1)  # hilt
    # Shield
    draw_item(img, 20, 3, SHIELD_OAK, 5, 5)
    torch(img, 14, 0, 'n')
    return img


def room_forge():
    img, draw = new_room()
    door_h(img, draw, 14, 0)  # N armory
    # Forge furnace in center
    draw_item(img, 12, 6, ANVIL, 6, 6)
    for dx in range(-10, 11):
        for dy in range(-10, 11):
            if dx*dx + dy*dy <= 50:
                px = 15*TILE + dx
                py = 9*TILE + dy
                if 0 <= px < W and 0 <= py < H:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.3 - (abs(dx)+abs(dy))*0.02)
                    nr = int(old[0] * (1-blend) + FORGE_FIRE[0] * blend)
                    ng = int(old[1] * (1-blend) + FORGE_FIRE[1] * blend)
                    nb = int(old[2] * (1-blend) + FORGE_FIRE[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))
    # Anvil
    draw_item(img, 13, 8, ANVIL, 4, 2)
    # Merchant NPC
    draw_npc(img, 6, 8, MERCHANT_GOLD)
    return img


def room_library():
    img, draw = new_room()
    door_h(img, draw, 14, 0)  # hidden shrine N
    door_h(img, draw, 14, 15) # theatre S? wait — library connects W theatre, N shrine, E teleport
    # Redesign: W door (theatre), N door (shrine), E door (teleport)
    door_h(img, draw, 0, 7)    # W → theatre
    door_h(img, draw, 13, 0)   # N → hidden-shrine
    door_h(img, draw, 28, 7)   # E → teleport
    # Bookshelves on S wall
    for tx in range(2, 28, 3):
        for dy in range(TILE*2):
            px = tx * TILE
            py_y = 14 * TILE + dy // 2
            if 0 <= py_y < H:
                img.putpixel((px + dy % 2, py_y), BOOK)
    # Fallen bookshelf center
    for dx in range(TILE*4):
        x = 8*TILE + dx
        y = 10*TILE
        if 0 <= x < W and 0 <= y < H:
            img.putpixel((x, y + (dx//TILE)%2), TABLE_WOOD)
    # Ancient tome
    draw_item(img, 14, 9, (0x6a, 0x3a, 0x1a), 4, 3)
    torch(img, 2, 2, 'n'); torch(img, 27, 2, 'n')
    return img


def room_cellar():
    img, draw = new_room()
    door_h(img, draw, 14, 0)  # N → pub
    # Water stains on walls
    for dy in range(H//2):
        for x in range(W):
            if x // TILE in (0, 1) or py2 // TILE in (0, 1):
                if (x + dy) % 5 == 0:
                    px2, py2 = x, H // 2 + dy
                    if 0 <= py2 < H:
                        old = img.getpixel((px2, py2))
                        img.putpixel((px2, py2), (min(old[0], 0x3a + (dy % 3) * 5), old[1], min(old[2] + 5, 0x5a)))
    # Barrel
    draw_item(img, 22, 10, BARREL, 6, 5)
    torch(img, 14, 2, 'n')
    return img


def room_lab():
    img, draw = new_room()
    door_h(img, draw, 14, 0)   # N → outside
    door_h(img, draw, 12, 15, width=4, locked=True)  # S → vault (locked)
    # Lab table on west
    for dy in range(TILE*5):
        x = 4*TILE
        y = 5*TILE + dy
        if 0 <= y < H:
            img.putpixel((x + dy%2, y), TABLE_WOOD)
    # Key on table
    draw_item(img, 5, 8, KEY_RUST, 2, 1)
    # Glassware
    draw_item(img, 8, 7, (0x88, 0xaa, 0xcc), 2, 2)
    torch(img, 2, 2, 'n'); torch(img, 27, 2, 'n')
    return img


def room_office():
    img, draw = new_room()
    door_h(img, draw, 0, 7)  # E → outside (only exit on east wall)
    # Desk on south
    draw_item(img, 10, 12, (0x5a, 0x3a, 0x2a), 10, 2)
    # Key hooks on north wall
    for i in range(3):
        draw_item(img, 8 + i*5, 2, TABLE_WOOD, 1, 1)
    # Iron key on middle hook
    draw_item(img, 13, 1, KEY_IRON, 2, 1)
    torch(img, 14, 0, 'n')
    return img


def room_theatre():
    img, draw = new_room()
    door_h(img, draw, 14, 15)  # S → outside
    door_h(img, draw, 28, 7)   # E → library
    # Steps on north side
    for step in range(3):
        draw_item(img, 2, 4 + step*2, (0x4a + step*5, 0x4a + step*5, 0x4a + step*5), 26, 1)
    # Torch leaning against east wall
    draw_item(img, 27, 10, (0x8a, 0x6a, 0x3a), 1, 4)
    draw_item(img, 27, 9, (0xc8, 0xc0, 0xa0), 1, 2)
    torch(img, 2, 2, 'n')
    return img


def room_pub():
    img, draw = new_room()
    door_h(img, draw, 14, 0)   # N → ? No — pub connects W outside, S cellar, E garden
    door_h(img, draw, 0, 7)    # W → outside
    door_h(img, draw, 14, 15)  # S → cellar
    door_h(img, draw, 28, 7)   # E → garden
    # Overturned table center
    draw_item(img, 12, 7, TABLE_WOOD, 6, 1)
    draw_item(img, 13, 6, TABLE_WOOD, 1, 4)
    # Ale mug
    draw_item(img, 14, 8, (0x8a, 0x7a, 0x5a), 2, 2)
    draw_item(img, 15, 8, (0xa0, 0x80, 0x30), 1, 1)
    # Barrels
    draw_item(img, 4, 4, BARREL, 3, 3)
    draw_item(img, 7, 4, BARREL, 3, 3)
    torch(img, 14, 14, 's')
    return img


def room_garden():
    img, draw = new_room()
    door_h(img, draw, 0, 7)    # W → pub
    door_h(img, draw, 12, 15, width=4, locked=True)  # S → guard-room (locked)
    door_h(img, draw, 28, 7)   # E → armory
    # Night sky (top of image is open ceiling)
    for x in range(W):
        for y in range(0, TILE*2):
            img.putpixel((x, y), (0x20, 0x20, 0x40))
    # Stars
    import random
    rng = random.Random(42)
    for _ in range(15):
        sx = rng.randint(0, W-1)
        sy = rng.randint(0, TILE*2 - 1)
        img.putpixel((sx, sy), WHITE)
    # Fountain center
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx*dx + dy*dy <= 4:
                img.putpixel((15*TILE + dx*TILE//2, 8*TILE + dy*TILE//2), STONE_DARK)
    # Herbs
    draw_item(img, 22, 10, HERB_GREEN, 2, 2)
    # Guard NPC near south door
    draw_npc(img, 13, 12, GUARD_RED)
    return img


def room_guard_room():
    img, draw = new_room()
    door_h(img, draw, 12, 0, width=4, locked=True)  # N → garden (locked)
    door_h(img, draw, 14, 15)  # S → throne-hall
    # Bed
    draw_item(img, 4, 8, (0x5a, 0x3a, 0x2a), 5, 3)
    # Weapon rack
    draw_item(img, 24, 6, TABLE_WOOD, 1, 6)
    # Torch
    torch(img, 2, 2, 'n'); torch(img, 27, 2, 'n')
    # Guard NPC
    draw_npc(img, 14, 9, GUARD_RED)
    return img


def room_hidden_shrine():
    img, draw = new_room()
    door_h(img, draw, 14, 15)  # S → library (hidden door)
    # Altar N wall
    for step in range(3):
        draw_item(img, 10 - step, 4 + step*2, STONE_DARK, 10 + step*2, 1)
    # Crystal shard
    draw_item(img, 14, 3, CRYSTAL, 2, 2)
    # Blue glow
    for dx in range(-5, 6):
        for dy in range(-5, 6):
            if dx*dx + dy*dy <= 25:
                px = 15*TILE + dx
                py = 4*TILE + dy
                if 0 <= px < W and 0 <= py < H:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.2 - (abs(dx)+abs(dy))*0.03)
                    nr = int(old[0] * (1-blend) + MAGIC_BLUE[0] * blend)
                    ng = int(old[1] * (1-blend) + MAGIC_BLUE[1] * blend)
                    nb = int(old[2] * (1-blend) + MAGIC_BLUE[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))
    # Hermit NPC
    draw_npc(img, 20, 9, HERMIT_BLUE)
    return img


def room_teleport_alcove():
    img, draw = new_room()
    door_h(img, draw, 0, 7)  # W → library
    # Dark stone walls
    for x in range(W):
        for y in range(H):
            tx, ty = x // TILE, y // TILE
            if tx <= 1 or tx >= COLS-2 or ty <= 1 or ty >= ROWS-2:
                img.putpixel((x, y), (0x2a, 0x2a, 0x3a))
    # Teleport circle center
    for dx in range(-6, 7):
        for dy in range(-6, 7):
            if dx*dx + dy*dy <= 36 and abs(dx*dx + dy*dy - 30) < 10:
                px = 15*TILE + dx
                py = 8*TILE + dy
                if 0 <= px < W and 0 <= py < H:
                    img.putpixel((px, py), RUNE_GLOW)
    # Dust scattered
    import random
    rng2 = random.Random(123)
    for _ in range(40):
        dx = rng2.randint(-5, 5)
        dy = rng2.randint(-5, 5)
        px = 15*TILE + rng2.randint(-8, 8)
        py = 8*TILE + rng2.randint(-8, 8)
        if 0 <= px < W and 0 <= py < H:
            img.putpixel((px, py), DUST_BLUE)
    return img


def room_throne_hall():
    img, draw = new_room()
    door_h(img, draw, 14, 0)  # N → guard-room
    # Pillars along both sides
    for col in [4, 7, 10, 13, 16, 19, 22, 25]:
        draw_item(img, col, 4, PILLAR, 2, 8)
        torch(img, col, 5, 'e')
    # Red carpet center
    draw_item(img, 13, 1, CARPET_RED, 4, 14)
    # Throne at south
    for step in range(5):
        draw_item(img, 14 - step, 14 - step, (0x55 - step*2, 0x55 - step*2, 0x55 - step*2), 2 + step*2, 1)
    # Throne seat
    draw_item(img, 12, 10, THRONE, 6, 4)
    # Gem on throne (dark)
    draw_item(img, 14, 9, (0x88, 0x88, 0x88), 2, 1)
    # Torches on pillars
    for col in [4, 7, 10, 13, 16, 19, 22, 25]:
        torch(img, col, 4, 'e')
    return img


# ============ GENERATE ALL ============

ROOMS = {
    "01_outside": room_outside,
    "02_theatre": room_theatre,
    "03_pub": room_pub,
    "04_lab": room_lab,
    "05_office": room_office,
    "06_library": room_library,
    "07_cellar": room_cellar,
    "08_vault": room_vault,
    "09_hidden-shrine": room_hidden_shrine,
    "10_garden": room_garden,
    "11_guard-room": room_guard_room,
    "12_armory": room_armory,
    "13_forge": room_forge,
    "14_teleport-alcove": room_teleport_alcove,
    "15_throne-hall": room_throne_hall,
}


def main():
    outdir = "docs/room-maps"
    os.makedirs(outdir, exist_ok=True)

    for name, fn in ROOMS.items():
        img = fn()
        path = os.path.join(outdir, f"{name}.png")
        img.save(path)
        print(f"  ✓ {path}")

    print(f"\n15 room maps saved to {outdir}/")


if __name__ == "__main__":
    main()
