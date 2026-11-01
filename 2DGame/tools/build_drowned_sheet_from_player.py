#!/usr/bin/env python3
"""
Sprite sheet "Drowned Soldier" dalam GAYA SPRITE PLAYER.

Monsternya tetap Drowned Soldier (kulit biru-keabuan, mata cyan menyala, zirah perunggu berpatina, celana kain biru
kerajaan, helm kettle berlumut dengan rumput laut menjuntai, pedang berkarat). Yang ditiru dari player adalah
GAYA dan GERAKAN sprite-nya: sel 64x64, proporsi badan, palet kecil dengan garis tepi #181818, dan pose tiap animasi
diambil dari frame player (src/Aset/*.png) lalu di-skin ulang warnanya:

    idle    1 frame   pose {Dir}Idle1 + Cloth{Dir}Idle                       (player juga diam 1 frame)
    walk    6 frame   pose {Dir}Walk1..6 + Cloth{Dir}Walk (1..6)
    attack  4 frame   pose {Dir}Attack (1..4) + BaseCloth{Dir}2 + Sword{Dir}  = serangan player nomor 1: TUSUKAN lurus
                      (frame ke-3 = tusukan kena). Ayunan (tipe 2-4) sengaja gak dipakai.
    hurt    2 frame   player gak punya animasi hurt -> pose idle (kilatan putih ditangani DrownedSoldier.java)
    death   3 frame   pose Char{Dir}Dead (1..3) + CharDead{Dir}Cloth (1..3)   = animasi mati player

Cara kerja: warna di frame player cuma beberapa (badan 5, baju 9, pedang 10), jadi tiap warna dipetakan persis ke
warna Drowned Soldier (lihat BODY_MAP / CLOTH_MAP / SWORD_*). Helm + rumput laut digambar di atas kepala, posisinya
dicari otomatis dari tiap frame (find_head). Saat mati, helm terlepas dan tergeletak di dekat kepala.

Tata letak sheet (HARUS sama dengan DrownedSoldier.java):
    sel 64 x 64 px, kolom = nomor frame (maks 6), baris = ANIM_INDEX * 4 + ARAH_INDEX
    anim : 0 idle | 1 walk | 2 attack | 3 hurt | 4 death
    arah : 0 down(Front) | 1 up(Back) | 2 left | 3 right

Jalankan dari folder 2DGame:
    pip install pillow
    python tools/build_drowned_sheet_from_player.py                    # tulis src/Aset/DrownedSoldier/drowned_soldier_sheet.png
    python tools/build_drowned_sheet_from_player.py --preview out.png  # contact sheet 3x buat dicek mata (gak nulis ke src/)
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / 'src'
ASET = ROOT / 'Aset'
SHEET_PATH = ASET / 'DrownedSoldier' / 'drowned_soldier_sheet.png'

CELL = 64
COLS = 6
DIRS = [('down', 'Front'), ('up', 'Back'), ('left', 'Left'), ('right', 'Right')]
FRAME_COUNT = {'idle': 1, 'walk': 6, 'attack': 4, 'hurt': 2, 'death': 3}
ANIMS = ['idle', 'walk', 'attack', 'hurt', 'death']


# ───────────── PALET DROWNED SOLDIER ─────────────
def C(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


OUT = C('#181818')                                   # garis tepi: SAMA kayak player
SKIN_L, SKIN, SKIN_D = C('#93bdb3'), C('#6f9a96'), C('#486e6d')
EYE = C('#9cf7ff')
ARM_L, ARM, ARM_D, ARM_DD = C('#78b8a0'), C('#477f72'), C('#2d5851'), C('#1f3d3b')
CLOTH_L, CLOTH, CLOTH_D = C('#4d73ab'), C('#35558a'), C('#223a63')
BRONZE, BRONZE_D, GOLD = C('#8d6c3a'), C('#5b4327'), C('#b9a050')
MOSS, MOSS_L, KELP, KELP_L = C('#557f33'), C('#7aa447'), C('#2f6140'), C('#4d8a55')
RUST_L, RUST, RUST_D = C('#cf8348'), C('#a3552d'), C('#5f2f1b')
TRAIL = C('#d6f6ff')


def rgb(c):
    return (c[0], c[1], c[2])


# warna frame player -> warna Drowned Soldier (persis, gak ada tebakan)
BODY_MAP = {
    (0xf8, 0xd8, 0xb8): SKIN_L, (0xd8, 0x98, 0x78): SKIN, (0x88, 0x58, 0x48): SKIN_D,
    (0x18, 0x18, 0x18): OUT,
    (0xf8, 0xf8, 0xf8): EYE,                         # putih mata -> mata cyan menyala
}
CLOTH_MAP = {
    (0xbe, 0x56, 0x67): ARM_L, (0x8d, 0x22, 0x46): ARM, (0x5b, 0x18, 0x39): ARM_D,        # baju atas -> zirah berpatina
    (0xc7, 0xc4, 0xa2): CLOTH_L, (0xa6, 0x98, 0x78): CLOTH, (0x71, 0x65, 0x58): CLOTH_D,  # celana -> kain biru kerajaan
    (0x3c, 0x55, 0x75): BRONZE, (0x1e, 0x2b, 0x41): BRONZE_D,                              # sabuk -> perunggu
    (0x18, 0x18, 0x18): OUT,
}
SWORD_GUARD = {(0x7f, 0x49, 0x35): BRONZE_D, (0xb4, 0x7f, 0x54): BRONZE, (0xda, 0xb9, 0x88): GOLD}
SWORD_BLADE = {                                      # bilah berkarat (bukan oranye terang)
    (0xf4, 0xf9, 0xf8): C('#c28a55'), (0xdc, 0xdc, 0xcb): C('#b07a4a'), (0xa7, 0xb5, 0xa7): RUST,
    (0x7c, 0x89, 0x89): RUST_D, (0x52, 0x52, 0x5f): C('#3b2216'), (0x18, 0x18, 0x18): OUT,
}
SWORD_BURST = {                                      # frame tusukan kena: bilah jadi kilatan air putih-kebiruan
    (0xf4, 0xf9, 0xf8): TRAIL, (0xdc, 0xdc, 0xcb): TRAIL, (0xa7, 0xb5, 0xa7): C('#9fe0f3'),
    (0x7c, 0x89, 0x89): C('#6cc4e8'), (0x52, 0x52, 0x5f): C('#6cc4e8'), (0x18, 0x18, 0x18): OUT,
}
SWORD_EFFECT = {(0xff, 0xff, 0xff): TRAIL}           # garis kecepatan tusukan -> putih kebiruan


# ───────────── UTIL GAMBAR ─────────────
def load(name):
    p = ASET / name
    if not p.exists():
        sys.exit(f'Aset player tidak ketemu: {p}')
    im = Image.open(p).convert('RGBA')
    if im.size != (CELL, CELL):
        sys.exit(f'Ukuran {name} {im.size}, harusnya {CELL}x{CELL}')
    return im


def recolor(im, mapping, name):
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            if (r, g, b) not in mapping:
                sys.exit(f'{name}: warna #{r:02x}{g:02x}{b:02x} belum ada di peta warna (x={x}, y={y})')
            dst[x, y] = mapping[(r, g, b)]
    return out


def recolor_sword(im, name, burst=False):
    """Gagang -> perunggu, bilah -> berkarat (atau kilatan air di frame tusukan kena), garis kecepatan -> putih kebiruan."""
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    src, dst = im.load(), out.load()
    blade = SWORD_BURST if burst else SWORD_BLADE
    # garis kecepatan di frame ke-2 warnanya sama dengan bilah, jadi dibedakan lewat bentuk: gumpalan piksel yang
    # gak nyambung ke gagang/bilah utama dianggap efek (putih kebiruan)
    guard = {(x, y) for y in range(im.height) for x in range(im.width)
             if src[x, y][3] and src[x, y][:3] in SWORD_GUARD}
    rest = {(x, y) for y in range(im.height) for x in range(im.width)
            if src[x, y][3] and src[x, y][:3] not in SWORD_GUARD}
    main, todo = set(), [(gx + dx, gy + dy) for gx, gy in guard for dx in (-1, 0, 1) for dy in (-1, 0, 1)]
    while todo:                                      # flood-fill dari gagang, 8 arah
        p = todo.pop()
        if p in rest and p not in main:
            main.add(p)
            todo.extend((p[0] + dx, p[1] + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1))
    streak = rest - main if guard else set()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            k = (r, g, b)
            if (x, y) in streak:
                dst[x, y] = TRAIL if k != (0x18, 0x18, 0x18) else OUT
                continue
            for m in (SWORD_GUARD, blade, SWORD_EFFECT):
                if k in m:
                    dst[x, y] = m[k]
                    break
            else:
                sys.exit(f'{name}: warna pedang #{r:02x}{g:02x}{b:02x} belum ada di peta (x={x}, y={y})')
    return out


def put(px, x, y, c, only_empty=False):
    if 0 <= x < CELL and 0 <= y < CELL:
        if only_empty and px[x, y][3] != 0:
            return
        px[x, y] = c


def speckle_armor(im, seed):
    """Bintik lumut di zirah (stabil per posisi, jadi gak berkedip antar frame)."""
    px = im.load()
    for y in range(CELL):
        for x in range(CELL):
            if px[x, y] == ARM and (x * 7 + y * 13 + seed) % 15 == 0:
                px[x, y] = MOSS


# ───────────── CARI KEPALA ─────────────
def find_head(body):
    """Kepala berdiri: baris pertama yang punya deretan piksel >= 9, lalu ditelusuri naik ke puncak kepala.
    Hasil (xl, xr, y_top): batas kiri/kanan kepala dan baris paling atas."""
    px = body.load()
    for y in range(CELL):
        run, best, end = 0, 0, 0
        for x in range(CELL):
            if px[x, y][3] > 0:
                run += 1
                if run > best:
                    best, end = run, x
            else:
                run = 0
        if best >= 9:
            seed_x = end - best // 2
            top = y
            while top > 0 and any(px[x, top - 1][3] > 0 for x in range(max(0, seed_x - 4), min(CELL, seed_x + 5))):
                top -= 1
            ys = min(top + 3, CELL - 1)                      # baris kubah kepala (belum kena bahu / lengan)
            dome = [x for x in range(max(0, seed_x - 9), min(CELL, seed_x + 10)) if px[x, ys][3] > 0]
            mid = (min(dome) + max(dome)) / 2.0
            yy = min(top + 5, CELL - 1)
            xs = [x for x in range(max(0, seed_x - 9), min(CELL, seed_x + 10)) if px[x, yy][3] > 0]
            lo, hi = min(xs), max(xs)
            if hi - lo + 1 > 13:                             # baris itu ikut bahu / lengan (pose menusuk): pakai lebar kepala player (12 px)
                lo = int(round(mid - 5.5))
                hi = lo + 11
            return (lo, hi, top)
    return None


def head_blob(body):
    """Kepala rebah: titik tertebal pada siluet (lingkaran terbesar yang muat). Return (cx, cy, r, centroid)."""
    px = body.load()
    pts = [(x, y) for y in range(CELL) for x in range(CELL) if px[x, y][3] > 0]
    pset = set(pts)
    edge = [(x, y) for y in range(CELL) for x in range(CELL) if (x, y) not in pset]
    best = (-1, 0, 0)
    for (x, y) in pts:
        d = min((x - ex) ** 2 + (y - ey) ** 2 for (ex, ey) in edge)
        if d > best[0]:
            best = (d, x, y)
    mx = sum(p[0] for p in pts) / len(pts)
    my = sum(p[1] for p in pts) / len(pts)
    return best[1], best[2], math.sqrt(best[0]), (mx, my)


# ───────────── HELM + RUMPUT LAUT ─────────────
def kelp(px, x, y, length, phase):
    for i in range(length):
        off = int(round(math.sin(i * 0.9 + phase) * 0.8))
        put(px, x + off, y + i, KELP_L if (i + int(phase)) % 3 == 0 else KELP)


def draw_helm(im, view, xl, xr, yt, seed):
    """Helm kettle (zirah berpatina + sulur perunggu di tengah + lumut) dan rumput laut, di atas kepala player."""
    px = im.load()
    rnd = random.Random(seed)
    cx = (xl + xr) / 2.0
    a = (xr - xl + 1) / 2.0 + 1.4
    ybase = yt + 6
    b = 7.6
    ytop = ybase - 7
    face = {'left': -1, 'right': 1}.get(view, 0)     # sisi wajah (bibir helm menjorok ke depan di tampak samping)
    cells = {}
    for y in range(ytop, ybase + 1):
        dy = (ybase - y) / b
        hw = a * math.sqrt(max(0.0, 1 - dy * dy))
        lo, hi = cx - hw, cx + hw
        if face and y >= ybase - 1:
            if face < 0:
                lo -= 2
            else:
                hi += 2
        for x in range(int(math.floor(lo)), int(math.ceil(hi)) + 1):
            if lo - 0.01 <= x <= hi + 0.01:
                r = y - ytop
                if y == ybase:
                    c = ARM_D
                elif r <= 1 or (x < cx - hw * 0.35 and r <= 3):
                    c = ARM_L
                elif x > cx + hw * 0.55 and r >= 2:
                    c = ARM_D
                else:
                    c = ARM
                cells[(x, y)] = c
    # sulur perunggu di tengah (depan/belakang/samping)
    rx = int(round(cx - 0.5))
    for y in range(ytop + 1, ybase):
        for x in (rx, rx + 1):
            if (x, y) in cells:
                cells[(x, y)] = GOLD if y == ytop + 1 else BRONZE
    # bintik lumut
    free = [k for k, v in cells.items() if v == ARM]
    for k in rnd.sample(free, min(4, len(free))):
        cells[k] = rnd.choice([MOSS, MOSS_L])
    # bayangan helm di dahi
    for (x, y) in list(cells):
        if y == ybase and (x, y + 1) not in cells and px[x, y + 1][3] > 0 and px[x, y + 1] in (SKIN, SKIN_L):
            px[x, y + 1] = SKIN_D
    for (x, y), c in cells.items():
        put(px, x, y, c)
    # garis tepi 1 px (#181818) di sekeliling helm, gak menimpa kulit
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, -1)):
            n = (x + dx, y + dy)
            if n not in cells and 0 <= n[0] < CELL and 0 <= n[1] < CELL and px[n[0], n[1]][3] == 0:
                px[n[0], n[1]] = OUT
    # rumput laut menjuntai
    if view == 'down':
        kelp(px, xl - 1, ybase - 1, 9, 1.0)
        kelp(px, xr + 1, ybase - 1, 7, 2.0)
    elif view == 'up':
        kelp(px, int(cx) - 3, ybase, 9, 1.0)
        kelp(px, int(cx) + 1, ybase, 11, 2.0)
        kelp(px, int(cx) + 4, ybase, 8, 3.0)
    elif view == 'left':
        kelp(px, xr, ybase, 10, 1.0)
        kelp(px, xr - 2, ybase, 7, 2.0)
    else:
        kelp(px, xl, ybase, 10, 1.0)
        kelp(px, xl + 2, ybase, 7, 2.0)


def draw_fallen_helm(im, cx, cy):
    """Helm terlepas dan tergeletak di tanah (dipakai di frame mati)."""
    px = im.load()
    cells = {}
    for y in range(-3, 1):
        hw = 4.6 * math.sqrt(max(0.0, 1 - (y / 3.4) ** 2))
        for x in range(int(-math.ceil(hw)), int(math.ceil(hw)) + 1):
            if abs(x) <= hw + 0.01:
                cells[(cx + x, cy + y)] = ARM_L if (y <= -2 and x < 0) else (ARM_D if y == 0 else ARM)
    cells[(cx, cy - 2)] = GOLD
    cells[(cx + 1, cy - 2)] = BRONZE
    cells[(cx - 2, cy - 3)] = MOSS
    for (x, y), c in cells.items():
        put(px, x, y, c)
    for (x, y) in cells:
        for dx, dy in ((1, 0), (-1, 0), (0, -1), (0, 1)):
            n = (x + dx, y + dy)
            if n not in cells and 0 <= n[0] < CELL and 0 <= n[1] < CELL and px[n[0], n[1]][3] == 0:
                px[n[0], n[1]] = OUT
    put(px, cx + 5, cy, KELP)
    put(px, cx + 6, cy, KELP_L)
    put(px, cx + 6, cy + 1, KELP)


# ───────────── RAKIT FRAME ─────────────
def frame(anim, view, d, f):
    """Satu sel 64x64. view = 'down'/'up'/'left'/'right', d = awalan nama file player ('Front'/'Back'/'Left'/'Right')."""
    seed = ord(view[0]) * 31 + f * 7 + len(anim)         # stabil antar jalan (bintik lumut gak berubah-ubah)
    if anim in ('idle', 'hurt'):
        body = load(f'{d}Idle1.png')
        cloth = load(f'Cloth{d}Idle.png')
        sword = None
    elif anim == 'walk':
        body = load(f'{d}Walk{f + 1}.png')
        cloth = load(f'Cloth{d}Walk ({f + 1}).png')
        sword = None
    elif anim == 'attack':
        body = load(f'{d}Attack ({f + 1}).png')
        cloth = load(f'BaseCloth{d}2 ({f + 1}).png')
        sword = load(f'Sword{d} ({f + 1}).png')
    else:
        body = load(f'Char{d}Dead ({f + 1}).png')
        cloth = load(f'CharDead{d}Cloth ({f + 1}).png')
        sword = None

    head = None
    fallen = None
    if anim == 'death' and f >= 1:
        fallen = head_blob(body)
    else:
        head = find_head(body)
        if head is None:
            sys.exit(f'Kepala tidak ketemu di frame {anim} {view} {f}')

    out = Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))
    out.alpha_composite(recolor(body, BODY_MAP, f'{anim}/{d}/{f} badan'))
    cloth_rc = recolor(cloth, CLOTH_MAP, f'{anim}/{d}/{f} baju')
    speckle_armor(cloth_rc, seed)
    out.alpha_composite(cloth_rc)
    if sword is not None:
        out.alpha_composite(recolor_sword(sword, f'{anim}/{d}/{f} pedang', burst=(f == 2)))
    if head is not None:
        draw_helm(out, view, head[0], head[1], head[2], seed)
    else:
        cx, cy, r, (mx, my) = fallen
        ux, uy = cx - mx, cy - my
        n = math.hypot(ux, uy) or 1.0
        hx = int(round(cx + ux / n * (r + 5)))
        hy = int(round(cy + r - 1))                      # helm tergeletak di lantai sejajar dasar kepala (gak melayang)
        hx = max(7, min(CELL - 8, hx))
        hy = max(5, min(CELL - 3, hy))
        draw_fallen_helm(out, hx, hy)
    return out


def build_sheet():
    sheet = Image.new('RGBA', (COLS * CELL, len(ANIMS) * len(DIRS) * CELL), (0, 0, 0, 0))
    for ai, anim in enumerate(ANIMS):
        for di, (view, d) in enumerate(DIRS):
            for f in range(FRAME_COUNT[anim]):
                sheet.paste(frame(anim, view, d, f), (f * CELL, (ai * len(DIRS) + di) * CELL))
    return sheet


def build_preview(sheet, scale=3):
    bg = Image.new('RGBA', (sheet.width * scale, sheet.height * scale), (22, 40, 58, 255))
    bg.alpha_composite(sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST))
    return bg


def main(argv):
    sheet = build_sheet()
    if '--preview' in argv:
        out = Path(argv[argv.index('--preview') + 1])
        build_preview(sheet).save(out)
        print('preview ->', out)
        return
    SHEET_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(SHEET_PATH, optimize=True)
    print('sheet ->', SHEET_PATH, sheet.size)


if __name__ == '__main__':
    main(sys.argv[1:])
