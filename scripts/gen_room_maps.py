#!/usr/bin/env python3
"""
Generate detailed pixel-art dungeon rooms at 4px-per-unit resolution.
120×64 grid → 480×256 PNG. Much finer than the 16px version.
"""

from PIL import Image, ImageDraw
import os, random, math

GRID = 4        # pixels per logical unit
COLS, ROWS = 120, 64
W, H = COLS * GRID, ROWS * GRID

# ===== PALETTE =====
WALL_BG   = (0x28, 0x2a, 0x38)
WALL_MID  = (0x32, 0x34, 0x42)
WALL_LT   = (0x3e, 0x40, 0x50)
FLOOR     = (0x3a, 0x3c, 0x48)
FLOOR_VAR = (0x36, 0x38, 0x44)
FLOOR_CRK = (0x2e, 0x30, 0x3a)
DOOR_WOOD = (0x74, 0x50, 0x2e)
DOOR_IRON = (0x88, 0x88, 0x90)
DOOR_GOLD = (0xc0, 0x90, 0x20)
TORCH_FIRE = (0xf0, 0xb0, 0x30)
TORCH_HOT  = (0xff, 0xe0, 0x40)
TORCH_BRACKET = (0x48, 0x48, 0x50)
CHEST      = (0x8a, 0x66, 0x32)
CHEST_BAND = (0xb0, 0x8a, 0x30)
TABLE      = (0x6a, 0x46, 0x28)
BARREL     = (0x7a, 0x56, 0x30)
BARREL_BAND = (0x88, 0x4a, 0x30)
BOOK_RED   = (0x8a, 0x32, 0x32)
BOOK_BLUE  = (0x32, 0x4a, 0x8a)
BOOK_GREEN = (0x4a, 0x6a, 0x32)
GEM_LIGHT  = (0xe8, 0xe0, 0xd0)
GEM_GLOW   = (0xff, 0xf0, 0xc0)
GOLD_COIN  = (0xc0, 0xa0, 0x30)
COIN_LT    = (0xe0, 0xc0, 0x40)
HERB       = (0x44, 0xcc, 0x44)
HERB_LT    = (0x66, 0xee, 0x66)
SWORD_METAL = (0x8a, 0x6a, 0x4a)
SWORD_HILT = (0x5a, 0x36, 0x22)
SHIELD_WOOD = (0x7a, 0x56, 0x32)
SHIELD_EDGE = (0x88, 0x88, 0x88)
KEY_RUST   = (0xa8, 0x84, 0x40)
KEY_IRON   = (0x84, 0x84, 0x88)
PAPER      = (0xe0, 0xd0, 0xa0)
TORCH_WOOD = (0x8a, 0x64, 0x2e)
TORCH_CLOTH = (0xd0, 0xc4, 0xa0)
CRYSTAL    = (0x88, 0xcc, 0xff)
DUST       = (0xaa, 0xcc, 0xff)
RUNE_BLUE  = (0x44, 0x88, 0xcc)
FORGE_FLAME = (0xf0, 0x40, 0x10)
FORGE_HOT   = (0xff, 0x80, 0x20)
ANVIL      = (0x60, 0x60, 0x68)
CARPET     = (0x6a, 0x1a, 0x1a)
CARPET_EDGE = (0x8a, 0x22, 0x22)
STONE_LT   = (0x6a, 0x6c, 0x76)
PILLAR     = (0x4a, 0x4c, 0x56)
PILLAR_LT  = (0x5a, 0x5c, 0x66)
THRONE_STONE = (0x58, 0x5a, 0x62)
THRONE_TOP = (0x68, 0x6a, 0x72)
BED_WOOD   = (0x5a, 0x3a, 0x2a)
BED_SHEET  = (0x9a, 0x8a, 0x7a)
GARDEN_SKY = (0x18, 0x1a, 0x38)
GARDEN_FLOOR = (0x3a, 0x4a, 0x32)
FOUNTAIN   = (0x50, 0x52, 0x5a)
VINE       = (0x4a, 0x6a, 0x3a)
MAGIC_BLUE = (0x44, 0x88, 0xcc)
RUNE_LT    = (0x88, 0xcc, 0xff)
BLACK      = (0x00, 0x00, 0x00)
WHITE      = (0xff, 0xff, 0xff)
GUARD      = (0xd0, 0x40, 0x40)
GUARD_LT   = (0xf0, 0x60, 0x60)
HERMIT_CLR = (0x44, 0x88, 0xcc)
HERMIT_LT  = (0x66, 0xaa, 0xee)
MERCHANT   = (0xd0, 0xa0, 0x20)
MERCHANT_LT = (0xf0, 0xc0, 0x40)
BEDROLL    = (0x8a, 0x7a, 0x6a)
DESK       = (0x5a, 0x3a, 0x2a)
GLASS_BLUE = (0x88, 0xaa, 0xcc)
GLASS_GREEN = (0x66, 0xaa, 0x66)
SPIDERWEB  = (0x88, 0x88, 0x88)


def floor(img, x0, y0, x1, y1):
    """Fill a region with stone floor texture."""
    w, h = x1 - x0 + 1, y1 - y0 + 1
    for i in range(w * h // 4 + 1):
        fx = x0 + random.randint(0, w)
        fy = y0 + random.randint(0, h)
        color = random.choice([FLOOR, FLOOR, FLOOR, FLOOR_VAR, FLOOR_CRK])
        for dx in range(random.randint(1, 3)):
            for dy in range(random.randint(1, 2)):
                px, py = fx + dx, fy + dy
                if x0 <= px <= x1 and y0 <= py <= y1:
                    img.putpixel((px, py), color)


def rect(img, x, y, w, h, color, fill=True):
    """Draw a filled or outline rectangle at pixel coords."""
    if not fill:
        for px in range(x, x + w):
            if y < H: img.putpixel((px, y), color)
            if y + h - 1 < H: img.putpixel((px, y + h - 1), color)
        for py in range(y, y + h):
            if x < W: img.putpixel((x, py), color)
            if x + w - 1 < W: img.putpixel((x + w - 1, py), color)
        return
    for px in range(max(0, x), min(W, x + w)):
        for py in range(max(0, y), min(H, y + h)):
            img.putpixel((px, py), color)


def wall_pattern(img, x0, y0, x1, y1):
    """Draw stone brick wall with variation."""
    bw, bh = 8, 6
    for bx in range(x0, x1, bw):
        offset = (bx // bw % 3) * (bh // 2)
        for by in range(y0 + offset, y1, bh):
            color = random.choice([WALL_BG, WALL_BG, WALL_MID, WALL_LT])
            rect(img, bx, by, bw - 1, bh - 1, color)
            # mortar line
            if by + bh - 1 < y1:
                for px in range(bx, min(bx + bw, x1)):
                    img.putpixel((px, by + bh - 1), WALL_BG)


def torch_fx(img, cx, cy, radius):
    """Add torch glow around a point."""
    for dx in range(-radius, radius + 1):
        for dy in range(-radius, radius + 1):
            dist = math.sqrt(dx*dx + dy*dy)
            if dist <= radius:
                px, py = cx + dx, cy + dy
                if 0 <= px < W and 0 <= py < H:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.35 - dist * 0.06)
                    nr = int(old[0] * (1 - blend) + TORCH_FIRE[0] * blend)
                    ng = int(old[1] * (1 - blend) + TORCH_FIRE[1] * blend)
                    nb = int(old[2] * (1 - blend) + TORCH_FIRE[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))


def place_torch(img, cx, cy, wall_dir):
    """wall_dir: 0=top 1=bottom 2=left 3=right"""
    # bracket
    bx, by = cx, cy
    if wall_dir == 0:  # top
        bx, by = cx - 1, cy
        rect(img, bx, by, 3, 2, TORCH_BRACKET)
        rect(img, cx - 1, cy + 2, 3, 11, TORCH_WOOD)
        rect(img, cx - 1, cy + 10, 3, 4, TORCH_CLOTH)
    elif wall_dir == 1:  # bottom
        rect(img, cx - 1, cy - 1, 3, 2, TORCH_BRACKET)
        rect(img, cx - 1, cy - 12, 3, 11, TORCH_WOOD)
        rect(img, cx - 1, cy - 13, 3, 4, TORCH_CLOTH)
    elif wall_dir == 2:  # left
        rect(img, cx, cy - 1, 2, 3, TORCH_BRACKET)
        rect(img, cx + 2, cy - 1, 11, 3, TORCH_WOOD)
        rect(img, cx + 10, cy - 1, 4, 3, TORCH_CLOTH)
    else:  # right
        rect(img, cx - 1, cy - 1, 2, 3, TORCH_BRACKET)
        rect(img, cx - 12, cy - 1, 11, 3, TORCH_WOOD)
        rect(img, cx - 13, cy - 1, 4, 3, TORCH_CLOTH)
    # flame
    for dx in range(-1, 2):
        for dy in range(-2, 1):
            fx, fy = cx + dx, cy + dy
            if wall_dir == 1: fy = cy - dy - 10
            elif wall_dir == 2: fx = cx + dx + 10
            elif wall_dir == 3: fx = cx - dx + 10
            if 0 <= fx < W and 0 <= fy < H:
                img.putpixel((fx, fy), TORCH_HOT if dy == -2 else TORCH_FIRE)
    torch_fx(img, cx, cy, 16)


def draw_door(img, cx, cy, direction, locked=False):
    """Draw a 4-wide door. direction: 0=top 1=bottom 2=left 3=right"""
    dw, dh = 10, 18
    if direction in (0, 1):
        x, y = cx - dw // 2, cy - (dh // 2 if direction == 0 else 0)
    else:
        x, y = cx - (dh // 2 if direction == 2 else 0), cy - dw // 2
    color = DOOR_IRON if locked else DOOR_WOOD
    for i in range(dw):
        for j in range(dh):
            px = x + (i if direction < 2 else j)
            py = y + (j if direction < 2 else i)
            if 0 <= px < W and 0 <= py < H:
                c = color
                if locked and direction < 2 and i == dw // 2 and 3 < j < 8:
                    c = DOOR_GOLD
                img.putpixel((px, py), c)


def draw_small_item(img, x, y, w, h, color, border=None):
    rect(img, x, y, w, h, color)
    if border:
        rect(img, x, y, w, h, border, fill=False)


def draw_barrel(img, x, y):
    r = 7
    rect(img, x, y, 14, 18, BARREL)
    rect(img, x, y + 5, 14, 2, BARREL_BAND)
    rect(img, x, y + 11, 14, 2, BARREL_BAND)


def draw_book(img, x, y, color=BOOK_RED):
    rect(img, x, y, 8, 10, color)
    rect(img, x + 1, y + 1, 2, 8, (0xff, 0xff, 0xff, 80))
    rect(img, x, y, 8, 10, BLACK, fill=False)


def draw_chest(img, x, y):
    rect(img, x, y, 14, 10, CHEST)
    rect(img, x, y + 3, 14, 2, CHEST_BAND)
    rect(img, x + 5, y - 2, 4, 2, CHEST_BAND)


def draw_gem(img, x, y, color=GEM_LIGHT, glow=GEM_GLOW):
    for dx in range(-1, 2):
        for dy in range(-1, 2):
            if abs(dx) + abs(dy) < 2:
                img.putpixel((x + dx, y + dy), WHITE)
            else:
                img.putpixel((x + dx, y + dy), color)
    for dx in range(-8, 9):
        for dy in range(-8, 9):
            dist = math.sqrt(dx*dx + dy*dy)
            if dist <= 8:
                px, py = x + dx, y + dy
                if 0 <= px < W and 0 <= py < H:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.25 - dist * 0.03)
                    nr = int(old[0] * (1 - blend) + glow[0] * blend)
                    ng = int(old[1] * (1 - blend) + glow[1] * blend)
                    nb = int(old[2] * (1 - blend) + glow[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))


def draw_npc(img, x, y, body, head=None):
    """Draw a simple NPC at position."""
    if head is None:
        head = tuple(min(c + 30, 255) for c in body)
    rect(img, x - 3, y - 10, 6, 10, body)
    rect(img, x - 2, y - 12, 4, 4, head)


def new_room(wall_left=3, wall_right=117, wall_top=3, wall_bottom=61):
    """Create a room with floor and stone walls."""
    img = Image.new("RGB", (W, H), BLACK)
    # Floor
    floor(img, wall_left, wall_top, wall_right, wall_bottom)
    # Walls
    wall_pattern(img, 0, 0, wall_left, H)
    wall_pattern(img, wall_right + 1, 0, W, H)
    wall_pattern(img, 0, 0, W, wall_top)
    wall_pattern(img, 0, wall_bottom + 1, W, H)
    return img


# ===== ROOM GENERATORS =====

def room_outside():
    img = new_room(4, 116, 4, 60)
    # 4 doors
    draw_door(img, 60, 4, 0)        # N → theatre
    draw_door(img, 60, 60, 1)       # S → lab
    draw_door(img, 116, 32, 3)      # E → pub
    draw_door(img, 4, 32, 2)        # W → office
    # Torches
    for x in [12, 108]:
        place_torch(img, x, 5, 0)
        place_torch(img, x, 59, 1)
    # Central circle
    for dx in range(-14, 15):
        for dy in range(-14, 15):
            if dx*dx + dy*dy <= 144 and abs(dx*dx + dy*dy - 100) < 80:
                img.putpixel((60 * GRID + dx, 32 * GRID + dy), FLOOR_VAR)
    # Paper
    draw_small_item(img, 58 * GRID, 45 * GRID, 6, 5, PAPER)
    return img


def room_theatre():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 60, 1)   # S → outside
    draw_door(img, 116, 32, 3)  # E → library
    # Steps
    for step in range(4):
        rect(img, 10, 12 + step * 8, 200, 6, (0x48 + step * 3, 0x48 + step * 3, 0x48 + step * 3))
    # Torch
    draw_small_item(img, 112, 48, 3, 18, TORCH_WOOD)
    draw_small_item(img, 112, 44, 3, 6, TORCH_CLOTH)
    place_torch(img, 60, 60, 1)
    return img


def room_pub():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 4, 32, 2)    # W → outside
    draw_door(img, 60, 60, 1)   # S → cellar
    draw_door(img, 116, 32, 3)  # E → garden
    # Overturned table
    rect(img, 52, 40, 16, 30, TABLE)  # tabletop vertical
    rect(img, 56, 48, 30, 6, TABLE)   # legs
    # Ale mug on ground
    rect(img, 64, 55, 8, 7, (0x8a, 0x7a, 0x5a))
    rect(img, 66, 54, 4, 3, (0xb0, 0x88, 0x30))
    # Barrels
    for bx in [20, 32, 44]:
        draw_barrel(img, bx, 12)
    # Spiderweb corner
    for _ in range(3):
        rect(img, 110, 6, 0, 0, SPIDERWEB)
    # Floor (wooden)
    for y in range(4, 61):
        if y % 3 == 0:
            for x in range(4, 117):
                if (x + y) % 5 == 0:
                    img.putpixel((x * GRID, y * GRID), (0x50, 0x42, 0x34))
    return img


def room_lab():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 4, 0)    # N → outside
    draw_door(img, 60, 60, 1, locked=True)  # S → vault
    # Lab table (west wall)
    rect(img, 6, 20, 40, 4, TABLE)
    rect(img, 22, 24, 4, 20, TABLE)  # leg
    rect(img, 40, 24, 4, 20, TABLE)  # leg
    # Key on table
    draw_small_item(img, 26, 16, 6, 4, KEY_RUST)
    # Flask
    draw_small_item(img, 36, 12, 6, 8, GLASS_BLUE)
    draw_small_item(img, 38, 10, 2, 3, GLASS_BLUE)
    # Notes
    draw_small_item(img, 48, 18, 5, 4, PAPER)
    # Dark stain on floor
    for _ in range(8):
        sx = 50 + random.randint(0, 40)
        sy = 40 + random.randint(0, 16)
        rect(img, sx, sy, random.randint(2, 6), random.randint(2, 4), (0x30, 0x22, 0x18))
    place_torch(img, 12, 6, 0)
    place_torch(img, 108, 6, 0)
    return img


def room_office():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 4, 32, 2)  # E → outside (no: W door)
    # Desk
    rect(img, 30, 40, 50, 4, DESK)
    rect(img, 32, 44, 4, 16, DESK)
    rect(img, 74, 44, 4, 16, DESK)
    # Scrolls
    draw_small_item(img, 48, 36, 6, 3, PAPER)
    draw_small_item(img, 56, 34, 6, 3, PAPER)
    # Key hooks
    for hx in [34, 46, 58]:
        rect(img, hx, 8, 4, 4, TABLE)
    # Iron key
    draw_small_item(img, 46, 6, 8, 4, KEY_IRON)
    # Chair
    rect(img, 68, 50, 12, 10, DESK)
    return img


def room_cellar():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 4, 0)  # N → pub
    # Water stains
    for _ in range(30):
        wx = random.randint(4, 40)
        wy = random.randint(4, 60)
        rect(img, wx, wy, random.randint(3, 10), random.randint(2, 6), (0x30, 0x38, 0x48))
    # Big barrel
    draw_barrel(img, 94, 44)
    # Smaller barrel
    draw_barrel(img, 80, 50)
    # Stairs from pub
    for step in range(6):
        rect(img, 50 + step * 4, 6 + step * 4, 16, 3, (0x50, 0x3a, 0x2a))
    # Straw on floor
    for _ in range(20):
        sx, sy = random.randint(10, 50), random.randint(20, 55)
        img.putpixel((sx, sy), (0x8a, 0x8a, 0x4a))
    place_torch(img, 60, 6, 0)
    return img


def room_library():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 4, 32, 2)    # W → theatre
    draw_door(img, 60, 4, 0)    # N → hidden-shrine
    draw_door(img, 116, 32, 3)  # E → teleport
    # Bookshelves on walls
    for shelf_y in [8, 20, 32]:
        for col in range(3):
            bx = 70 + col * 14
            rect(img, bx, shelf_y, 10, 2, TABLE)
            for bi in range(3):
                draw_book(img, bx + bi * 3 + 1, shelf_y + 3, random.choice([BOOK_RED, BOOK_BLUE, BOOK_GREEN]))
    # Fallen bookshelf
    rect(img, 30, 36, 44, 4, TABLE)
    # Books scattered
    for bx in [32, 40, 48, 56, 64]:
        draw_book(img, bx, 38, random.choice([BOOK_RED, BOOK_BLUE, BOOK_GREEN]))
    # Ancient tome (special)
    draw_small_item(img, 44, 42, 10, 8, (0x6a, 0x32, 0x18))
    draw_small_item(img, 46, 40, 2, 2, GOLD_COIN)
    # Carpet
    for y in range(50, 60):
        rect(img, 20, y, 80, 1, CARPET)
    place_torch(img, 60, 6, 0)
    return img


def room_vault():
    img = new_room(4, 116, 4, 60)
    # Polished walls
    for x in range(0, 3):
        for y in range(4, 60):
            img.putpixel((x, y * GRID), (0x3a, 0x3c, 0x46))
    for x in range(117, 120):
        for y in range(4, 60):
            img.putpixel((x, y * GRID), (0x3a, 0x3c, 0x46))
    # Heavy iron door (top)
    for dx in range(-10, 11):
        for dy in range(4, 10):
            img.putpixel((60 + dx, dy * GRID), DOOR_IRON)
    draw_door(img, 60, 4, 0, locked=True)
    # Pedestal
    rect(img, 48, 32, 24, 4, STONE_LT)
    rect(img, 52, 36, 16, 4, (0x4a, 0x4c, 0x56))
    # Light Gem
    draw_gem(img, 60, 28)
    # Gold coins
    for ci in range(5):
        cx = 84 + random.randint(0, 10)
        cy = 36 + random.randint(0, 8)
        draw_small_item(img, cx, cy, 4, 3, GOLD_COIN)
    # Chest
    draw_chest(img, 24, 48)
    return img


def room_hidden_shrine():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 60, 1)  # S → library (hidden)
    # Altar steps
    for step in range(5):
        rect(img, 40 - step * 2, 8 + step * 3, 40 + step * 4, 3, STONE_LT if step < 4 else THRONE_STONE)
    # Crystal shard
    draw_gem(img, 60, 11, color=CRYSTAL, glow=MAGIC_BLUE)
    # Murals on walls
    for _ in range(12):
        mx = random.choice([4, 116])
        my = random.randint(16, 50)
        rect(img, mx, my, 2, random.randint(8, 16), (0x4a, 0x4a, 0x6a))
    # Hermit NPC
    draw_npc(img, 80, 44, HERMIT_CLR, HERMIT_LT)
    # Staff
    rect(img, 82, 34, 2, 12, (0x6a, 0x4a, 0x2a))
    # Blue torchlight
    place_torch(img, 60, 60, 1)
    return img


def room_garden():
    img = new_room(4, 116, 4, 60)
    # Open sky top
    rect(img, 4, 4, 112, 20, GARDEN_SKY)
    # Stars
    for _ in range(20):
        sx, sy = random.randint(8, 112), random.randint(6, 22)
        img.putpixel((sx, sy), WHITE)
    # Grass floor
    for x in range(4, 117):
        for y in range(20, 60):
            if (x + y) % 5 != 0:
                img.putpixel((x, y * GRID), GARDEN_FLOOR)
    # Doors
    draw_door(img, 4, 32, 2)    # W → pub
    draw_door(img, 60, 60, 1, locked=True)  # S → guard-room
    draw_door(img, 116, 32, 3)  # E → armory
    # Fountain
    for dx in range(-6, 7):
        for dy in range(-6, 7):
            if dx*dx + dy*dy <= 36:
                px, py = 60 + dx, 32 + dy
                img.putpixel((px, py), FOUNTAIN)
    # Herbs
    draw_small_item(img, 100, 44, 6, 8, HERB)
    draw_small_item(img, 102, 42, 2, 2, HERB_LT)
    # Guard outside
    draw_npc(img, 56, 50, GUARD, GUARD_LT)
    return img


def room_guard_room():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 4, 0, locked=True)  # N → garden
    draw_door(img, 60, 60, 1)  # S → throne-hall
    # Bed
    rect(img, 12, 36, 22, 16, BED_WOOD)
    rect(img, 14, 38, 18, 4, BED_SHEET)
    # Table
    rect(img, 36, 24, 16, 4, TABLE)
    rect(img, 42, 28, 4, 16, TABLE)
    rect(img, 50, 28, 4, 16, TABLE)
    # Oil lamp
    draw_small_item(img, 42, 20, 4, 4, TORCH_FIRE)
    # Weapon rack
    rect(img, 108, 20, 4, 30, TABLE)
    # Guard NPC
    draw_npc(img, 60, 42, GUARD, GUARD_LT)
    # Spear
    rect(img, 62, 30, 2, 16, (0x88, 0x88, 0x88))
    place_torch(img, 12, 6, 0)
    return img


def room_armory():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 4, 32, 2)    # W → garden
    draw_door(img, 60, 60, 1)   # S → forge
    # Weapon racks
    for rx in [16, 40, 64, 88]:
        rect(img, rx, 8, 3, 40, TABLE)
    # Sword
    rect(img, 20, 16, 2, 28, SWORD_METAL)
    rect(img, 18, 14, 6, 4, SWORD_HILT)
    rect(img, 20, 44, 2, 2, SWORD_METAL)  # tip
    # Shield
    for dx in range(-6, 7):
        for dy in range(-6, 7):
            if dx*dx + dy*dy <= 36:
                img.putpixel((68 + dx, 28 + dy), SHIELD_WOOD)
    for dx in range(-7, 8):
        for dy in range(-7, 8):
            if 30 < dx*dx + dy*dy <= 40:
                img.putpixel((68 + dx, 28 + dy), SHIELD_EDGE)
    place_torch(img, 12, 6, 0)
    return img


def room_forge():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 4, 0)  # N → armory
    # Forge furnace
    for dx in range(-8, 9):
        for dy in range(-8, 9):
            if dx*dx + dy*dy <= 64:
                img.putpixel((60 + dx, 30 + dy), FORGE_HOT if dx*dx + dy*dy < 25 else FORGE_FLAME)
    # Light from forge
    for dx in range(-30, 31):
        for dy in range(-30, 31):
            dist = math.sqrt(dx*dx + dy*dy)
            if dist <= 30:
                px, py = 60 + dx, 30 + dy
                if 4 < px < 116 and 4 < py < 60:
                    old = img.getpixel((px, py))
                    blend = max(0, 0.4 - dist * 0.012)
                    nr = int(old[0] * (1 - blend) + FORGE_FLAME[0] * blend)
                    ng = int(old[1] * (1 - blend) + FORGE_FLAME[1] * blend)
                    nb = int(old[2] * (1 - blend) + FORGE_FLAME[2] * blend)
                    img.putpixel((px, py), (nr, ng, nb))
    # Anvil
    rect(img, 48, 44, 24, 10, ANVIL)
    rect(img, 44, 42, 4, 14, ANVIL)
    rect(img, 72, 42, 4, 14, ANVIL)
    # Merchant NPC
    draw_npc(img, 24, 40, MERCHANT, MERCHANT_LT)
    # Weapons on wall
    for wx in [90, 98, 106]:
        rect(img, wx, 10, 2, 16, SWORD_METAL)
    return img


def room_teleport_alcove():
    img = new_room(4, 116, 4, 60)
    # Dark stone walls
    rect(img, 0, 0, 4, H, (0x22, 0x24, 0x32))
    rect(img, 116, 0, 4, H, (0x22, 0x24, 0x32))
    rect(img, 0, 0, W, 4, (0x22, 0x24, 0x32))
    rect(img, 0, 60, W, 4, (0x22, 0x24, 0x32))
    draw_door(img, 4, 32, 2)  # W → library
    # Rune circle
    for dx in range(-16, 17):
        for dy in range(-16, 17):
            d = math.sqrt(dx*dx + dy*dy)
            if 12 < d < 16 or abs(d - 8) < 1.5:
                img.putpixel((60 + dx, 32 + dy), RUNE_LT)
            elif abs(d - 4) < 1 and abs(dx) > 2 and abs(dy) > 2:
                img.putpixel((60 + dx, 32 + dy), RUNE_BLUE)
    # Warp dust
    for _ in range(60):
        dx, dy = random.randint(-12, 12), random.randint(-12, 12)
        if math.sqrt(dx*dx + dy*dy) < 14:
            img.putpixel((60 + dx, 32 + dy), DUST)
    return img


def room_throne_hall():
    img = new_room(4, 116, 4, 60)
    draw_door(img, 60, 4, 0)  # N → guard-room
    # Pillars
    for px in [20, 36, 60, 84, 100]:
        rect(img, px - 2, 8, 4, 44, PILLAR)
        rect(img, px - 2, 8, 4, 3, PILLAR_LT)
        rect(img, px - 2, 50, 4, 2, PILLAR_LT)
    # Carpet
    rect(img, 50, 4, 20, 56, CARPET)
    rect(img, 50, 4, 20, 2, CARPET_EDGE)
    rect(img, 50, 58, 20, 2, CARPET_EDGE)
    # Throne dais
    for step in range(6):
        rect(img, 42 - step * 2, 56 - step * 2, 36 + step * 4, 2, (0x4a + step * 2, 0x4c + step * 2, 0x56 + step * 2))
    # Throne
    rect(img, 48, 42, 8, 14, THRONE_STONE)
    rect(img, 44, 40, 16, 4, THRONE_TOP)
    rect(img, 46, 38, 12, 4, THRONE_TOP)
    # Gem in throne
    draw_small_item(img, 50, 36, 4, 4, (0x80, 0x80, 0x88))
    # Torches on pillars
    for px in [20, 36, 60, 84, 100]:
        place_torch(img, px, 10, 0)
    return img


# ===== GENERATE ALL =====

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
    print(f"\n15 room maps → {outdir}/")


if __name__ == "__main__":
    main()
