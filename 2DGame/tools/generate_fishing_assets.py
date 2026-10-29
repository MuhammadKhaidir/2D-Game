#!/usr/bin/env python3
"""
Generator ikon + suara buat sistem mancing (Item/FishingLoot, Item/FishingManager).

    python3 tools/generate_fishing_assets.py            # ikon + suara
    python3 tools/generate_fishing_assets.py icons      # ikon saja
    python3 tools/generate_fishing_assets.py sfx        # suara saja
    python3 tools/generate_fishing_assets.py icons --force   # timpa ikon yang sudah ada (default: dibiarkan)

CATATAN: ikon yang dipakai sekarang berasal dari gambar sheet pixel-art, dibuat lewat tools/import_fishing_sheet.py
(ikon + gambar senjata di tangan). Bagian ikon di file ini tinggal jadi cadangan (ikon gambar-kode) dan gak menimpa
ikon yang sudah ada kecuali pakai --force.

Hasil:
    src/drop/fishing/<slug>.png     ikon 64x64 (digambar 16x16 lalu di-scale 4x nearest, sama kayak ikon ikan lama)
    src/SFX/fishing/<nama>.wav      efek suara (disintesis lewat kode, bukan sampel orang lain)

Mau pakai gambar sendiri? Timpa saja file PNG-nya dengan nama yang sama (64x64 atau ukuran bebas), gak perlu ubah Java.
Butuh: Pillow + numpy.
"""
import math
import os
import sys
import wave

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(HERE, "..", "src"))
ICON_DIR = os.path.join(SRC, "drop", "fishing")
SFX_DIR = os.path.join(SRC, "SFX", "fishing")

# ----------------------------------------------------------------------------------------------
# PALET (satu huruf = satu warna)
# ----------------------------------------------------------------------------------------------
PAL = {
    '.': None,
    'k': 0x2B1A14,                                   # outline gelap
    'w': 0xFFFFFF, 'W': 0xE6EDF0, 'j': 0xC8C8C8,
    's': 0xB8C4CC, 'S': 0x8C9AA4,
    'e': 0x9A9A9A, 'E': 0x5E5E5E,
    'b': 0x5E86B0, 'B': 0x3F5F85, 'l': 0xC9D8E6,
    'r': 0xD23A2E, 'R': 0x8E241C, 'q': 0xFF7A6B,
    'o': 0xF08A24, 'O': 0xB35A12, 'f': 0xFFC070,
    'y': 0xF2B930, 'Y': 0xD98A1C, 'z': 0xFFE9A0,
    'g': 0x4FA85A, 'G': 0x2F6B3A, 'h': 0x9BDB8B,
    'n': 0xB07A44, 'N': 0x7A5230, 'm': 0xD9B380, 'd': 0x4A2E18,
    'p': 0xF29BB0, 'P': 0xC75B7A,
    'u': 0x8E5BD0, 'U': 0x5A3A99, 'v': 0xC7A6F2,
    'c': 0x35C7E8, 'C': 0x1E8DB0, 'x': 0xA8EEFF,
    'a': 0x2EC4B6, 'A': 0x1B7F78, 'i': 0x8FF0E4,
    't': 0x6B7A4A,
    '3': 0xFF9E7A, '4': 0xE8714F,
}


def rgba(c, a=255):
    return None if c is None else ((c >> 16) & 255, (c >> 8) & 255, c & 255, a)


def from_map(rows, extra=None):
    assert len(rows) == 16, "harus 16 baris, dapat %d" % len(rows)
    pal = dict(PAL)
    if extra:
        pal.update(extra)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, "baris %d panjangnya %d (harus 16): %r" % (y, len(row), row)
        for x, ch in enumerate(row):
            col = pal[ch]
            if col is not None:
                img.putpixel((x, y), rgba(col))
    return img


def darker(c, f):
    return (int(((c >> 16) & 255) * f) << 16) | (int(((c >> 8) & 255) * f) << 8) | int((c & 255) * f)


def fish_icon(body, belly, fin, bw, bh, tail=3, spots=None, stripe=None, eye_dark=True, glow=None):
    """Ikan samping (kepala kiri, ekor kanan) digambar tanpa antialias."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    line = darker(body, 0.40)
    bx = 1
    by = 8 - bh // 2
    tail_x = bx + bw - 1
    d.polygon([(tail_x, 8), (15, 8 - tail), (15, 8 + tail)], fill=rgba(fin))
    d.polygon([(bx + bw // 2 - 2, by), (bx + bw // 2 + 1, by - 3), (bx + bw // 2 + 2, by)], fill=rgba(fin))
    d.ellipse([bx, by, bx + bw - 1, by + bh - 1], fill=rgba(body))
    belly_layer = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    bd = ImageDraw.Draw(belly_layer)
    bd.ellipse([bx, by, bx + bw - 1, by + bh - 1], fill=rgba(belly))
    for y in range(8, 16):                      # perut = setengah bawah badan, lebih terang
        for x in range(16):
            if belly_layer.getpixel((x, y))[3] and img.getpixel((x, y))[3]:
                img.putpixel((x, y), rgba(belly))
    d.ellipse([bx, by, bx + bw - 1, by + bh - 1], outline=rgba(line))
    d.line([(bx + 3, by + 2), (bx + 3, by + bh - 3)], fill=rgba(line))
    img.putpixel((bx + 1, by + 2), rgba(0xFFFFFF))
    img.putpixel((bx + 1, by + 3), rgba(0x1A1A1A if eye_dark else 0xD23A2E))
    if spots:
        for (x, y) in spots:
            img.putpixel((x, y), rgba(darker(body, 0.55)))
    if stripe:
        for x in range(bx + 4, bx + bw - 2):
            img.putpixel((x, 8), rgba(stripe))
    if glow:
        for (x, y) in glow:
            img.putpixel((x, y), rgba(0xFFFFFF, 230))
    return img


# ----------------------------------------------------------------------------------------------
# IKON (hand-drawn 16x16)
# ----------------------------------------------------------------------------------------------
MAPS = {}

# ---------- SAMPAH ----------
MAPS["broken_bottle"] = [
    "................",
    "......kkkk......",
    "......kgGk......",
    "......khGk......",
    "......kgGk......",
    ".....kghGGk.....",
    "....kghGGGGk....",
    "....kgGGGGGk....",
    "....kgGGGGGk....",
    "....kgGGGGGk....",
    "....kghGkGGk....",
    "....kk.kk.kGk...",
    ".........k.kk.k.",
    ".......kgk...kk.",
    "......kgGGk.....",
    ".......kkk......",
]
MAPS["old_boot"] = [
    "................",
    "................",
    "....kkkkk.......",
    "...kNNNNNk......",
    "...kNnnNNk......",
    "...kNmnNNk......",
    "...kNnnNNk......",
    "...kNnnNNkkk....",
    "...kNnnnNNNNkk..",
    "..kkNnnnnnnNNNk.",
    ".kNNNnnnnnnnNNk.",
    ".kNnnnnnnnnnNNk.",
    ".kNNNNNNNNNNNNk.",
    ".kddddddddddddk.",
    "..kkkkkkkkkkkk..",
    "................",
]
MAPS["tin_can"] = [
    "................",
    "................",
    "....kkkkkkkk....",
    "...kjWWwwWWjk...",
    "...kkSSSSSSkk...",
    "...kSjjWWjjSk...",
    "...kSjjWjjjSk...",
    "...kSjjjjOOSk...",
    "...kSjjjOOOSk...",
    "...kSjjjjOOSk...",
    "...kSjjjjjjSk...",
    "...kSSjjjjSSk...",
    "....kSSSSSSk....",
    ".....kkkkkk.....",
    "................",
    "................",
]
MAPS["seaweed_clump"] = [
    "................",
    "..g.............",
    "..Gg....g.......",
    "..Gg...gG...g...",
    "...Gg..gG..Gg...",
    "...Gg.gGg..Gg...",
    ".g.GgGGg.g.Gg...",
    ".Gg.GGg.gGgGg.g.",
    ".GGgGhGgGGgGgGg.",
    "..GGGGGGGGGGGG..",
    "...GGhGGGGhGGG..",
    "....GGGGGGGGG...",
    ".....kGGGGGk....",
    "................",
    "................",
    "................",
]
MAPS["soggy_plank"] = [
    "................",
    "................",
    "................",
    ".kkkkkkkkkkkkkk.",
    ".kNnnnnnnnnnnNk.",
    ".kNnmnnnnndnnNk.",
    ".kNnnnnnnnnnnNk.",
    ".kNtnnnnnnnnnNk.",
    ".kNNNNNNNNNNNNk.",
    ".kddddddddddddk.",
    "..kkkkkkkkkkkk..",
    "..tt............",
    "...tt.t.........",
    "................",
    "................",
    "................",
]
MAPS["tangled_net"] = [
    "................",
    "................",
    "..eEeEeEeEeEeE..",
    "..E.e.E.e.E.e.E.",
    "..e.E.e.E.e.E.e.",
    "..EeEeEeEeEeEeE.",
    "..e.E.e.E.e.E.e.",
    "..E.e.E.e.E.e.E.",
    "..eEeEeEeEeEeEe.",
    "..E.e.E.e.E.e.E.",
    "..e.E.e.E.e.E.e.",
    "..EeEeEeEeEeEeE.",
    "...t.....t......",
    "................",
    "................",
    "................",
]

# ---------- MAKANAN ----------
MAPS["dried_kelp"] = [
    "................",
    "................",
    "...kkkkkkkkkk...",
    "..kGGGGGGGGGGk..",
    "..kGggggggggGk..",
    "..kGgGGGGGGgGk..",
    "..kGgGggggGgGk..",
    "..kGgGgGGgGgGk..",
    "..kGgGggggGgGk..",
    "..kGgGGGGGGgGk..",
    "..kGggggggggGk..",
    "..kGGGGGGGGGGk..",
    "...kkkkkkkkkk...",
    "................",
    "................",
    "................",
]
MAPS["crab"] = [
    "................",
    "..rr........rr..",
    ".rRRr......rRRr.",
    ".rR.r......r.Rr.",
    "..rr.r....r.rr..",
    "...r.krrrrk.r...",
    "....rrqrrqrr....",
    "...rrrrrrrrrr...",
    "..rrrrrRRrrrrr..",
    ".R.rrrrrrrrrr.R.",
    "R..RrrrrrrrrR..R",
    "...R.R....R.R...",
    "..R..R....R..R..",
    "................",
    "................",
    "................",
]
MAPS["lobster"] = [
    "................",
    ".rr.......rr....",
    "rRRr.....rRRr...",
    "rR.r.....r.Rr...",
    ".rrrk...krrr....",
    "..rrrkkkrrr.....",
    "...rrrrrrrrr.k..",
    "..krrqrrrrrrrrk.",
    ".krrrrrRRrrrrrrk",
    "..krrrrrrrrrrrk.",
    "...kkrRrRrRrkk..",
    "....R.rRrRrR....",
    "...R..RRrRRR....",
    "...R...RRR.R....",
    "................",
    "................",
]

# ---------- BUFF ----------
MAPS["jellyfish_jelly"] = [
    "................",
    "......kkkk......",
    "....kkccccckk...",
    "...kcxxcccccck..",
    "..kcxxcccccccCk.",
    "..kcxcccccccCCk.",
    "..kccccccccCCCk.",
    "..kcccccccCCCCk.",
    "..kCCCCCCCCCCCk.",
    "...kkkkkkkkkkk..",
    "...c.c.c.c.c.c..",
    "....c.c.c.c.c...",
    "...c.c...c.c.c..",
    "....c.....c.....",
    "................",
    "................",
]
MAPS["storm_bottle"] = [
    "................",
    "......nnnn......",
    "......nNNn......",
    ".....kkkkkk.....",
    "......kwwk......",
    "......kwwk......",
    ".....kwggwk.....",
    "....kwEeEeEk....",
    "...kwEejEeeEk...",
    "...kwEEyyEEEk...",
    "...kwEeeyyEek...",
    "...kwEeEyEEEk...",
    "...kwEEEyEEwk...",
    "....kwEEyEwk....",
    ".....kkkkkk.....",
    "................",
]
MAPS["kraken_ink"] = [
    "................",
    "......nnnn......",
    "......nNNn......",
    ".....kkkkkk.....",
    "......kwwk......",
    "......kwwk......",
    ".....kwggwk.....",
    "....kwUuuuUk....",
    "...kwUuvuuuUk...",
    "...kwUuvuuuUk...",
    "...kwUUuuuUUk...",
    "...kwUUUuUUUk...",
    "...kwUUUUUUUk...",
    "....kwUUUUUk....",
    ".....kkkkkk.....",
    "................",
]
MAPS["vitality_pearl"] = [
    "................",
    "................",
    "................",
    "....kkkkkkkk....",
    "...kPppppppPk...",
    "..kPpPpPpPpPPk..",
    "..kkkkkkkkkkkk..",
    "..kPpppwwpppPk..",
    "..kPpppwwwppPk..",
    "..kPppppwwppPk..",
    "...kPppppppPk...",
    "....kPPPPPPk....",
    ".....kkkkkk.....",
    "................",
    "................",
    "................",
]

# ---------- HARTA ----------
MAPS["coin_pouch"] = [
    "................",
    "................",
    "......kk.kk.....",
    ".....kNNkNNk....",
    "......kNNNk.....",
    ".....kkrrrkk....",
    "....kNnnnnnNk...",
    "...kNnnnnmnnNk..",
    "..kNnnnyyynnnNk.",
    "..kNnnyYzYynnNk.",
    "..kNnnyzyyynnNk.",
    "..kNnnnyyynnnNk.",
    "...kNnnnnnnnNk..",
    "....kNNNNNNNk...",
    ".....kkkkkkk....",
    "................",
]
MAPS["sunken_chest"] = [
    "................",
    "................",
    "...kkkkkkkkkk...",
    "..kNnnnnnnnnNk..",
    "..kNnGnnnnGnNk..",
    "..kkkkkkkkkkkk..",
    "..kEeEeyyEeEek..",
    "..kNnnnyYnnnNk..",
    "..kNtnnyYnntNk..",
    "..kNnnnnnnnnNk..",
    "..kNNNNNNNNNNk..",
    "..kddddddddddk..",
    "...kkkkkkkkkk...",
    "................",
    "................",
    "................",
]

# ---------- SENJATA (ikon; animasi serangannya pakai frame Sword / Axe / BoneSword yang diberi warna) ----------


# ----------------------------------------------------------------------------------------------
# IKON PROSEDURAL: senjata + belut (digambar di kanvas 64x64 lalu di-sample ke 16x16, outline otomatis)
# ----------------------------------------------------------------------------------------------
def _canvas():
    return Image.new("RGBA", (64, 64), (0, 0, 0, 0))


def _bez(p0, p1, p2, n=28):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        pts.append((x, y))
    return pts


def _dense(pts, step=1.2):
    out = [pts[0]]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        n = max(1, int(math.hypot(x1 - x0, y1 - y0) / step))
        for k in range(1, n + 1):
            out.append((x0 + (x1 - x0) * k / n, y0 + (y1 - y0) * k / n))
    return out


def _brush(d, pts, r0, r1, color, off=(0, 0)):
    pts = _dense(pts)
    n = len(pts) - 1
    for i, (x, y) in enumerate(pts):
        r = r0 + (r1 - r0) * (i / max(1, n))
        d.ellipse([x - r + off[0], y - r + off[1], x + r + off[0], y + r + off[1]], fill=rgba(color))


def _finish(img64, outline=0x2B1A14):
    small = img64.resize((16, 16), Image.NEAREST)
    px = small.load()
    # buang piksel setengah transparan, lalu tambah outline 4-arah
    for y in range(16):
        for x in range(16):
            if px[x, y][3] < 128:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = px[x, y][:3] + (255,)
    out = small.copy()
    po = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] and px[nx, ny][:3] != (0, 0, 0):
                        po[x, y] = rgba(outline)
                        break
    return out


def sword_icon(blade, edge, shine, guard, grip, pommel, curve=0.0, width=5.0, tip=(60, 4), serrate=False, glow=None):
    """Pedang diagonal: gagang kiri-bawah, ujung kanan-atas. curve>0 = bilah melengkung (cutlass / taring)."""
    img = _canvas()
    d = ImageDraw.Draw(img)
    # gagang + pommel
    _brush(d, [(8, 56), (17, 47)], 3.0, 3.0, grip)
    d.ellipse([3, 53, 11, 61], fill=rgba(pommel))
    # bilah
    c = (23, 41)
    base = (c[0] + 3, c[1] - 3)
    ctrl = ((base[0] + tip[0]) / 2 + curve * 9, (base[1] + tip[1]) / 2 + curve * 9)
    path = _bez(base, ctrl, tip)
    _brush(d, path, width + 1.4, 1.2, edge)
    _brush(d, path, width - 0.4, 0.6, blade)
    _brush(d, path, width * 0.45, 0.4, shine, off=(-1.6, -1.6))
    if serrate:                       # gerigi di sisi tajam (kanan-bawah)
        for i in range(5, len(path) - 4, 3):
            x, y = path[i]
            d.polygon([(x + 3, y + 3), (x + 7, y + 6), (x + 2, y + 7)], fill=rgba(edge))
    # guard (tegak lurus bilah)
    _brush(d, [(14, 33), (32, 51)], 2.6, 2.6, guard)
    _brush(d, [(15, 33), (17, 35)], 3.0, 3.0, guard)
    _brush(d, [(30, 49), (32, 51)], 3.0, 3.0, guard)
    img = _finish(img)
    if glow:
        for (x, y) in glow:
            img.putpixel((x, y), rgba(0xFFFFFF))
    return img


def axe_icon(head, edge, shine, wood, wood_dark, bind=None, big=False, spikes=False):
    img = _canvas()
    d = ImageDraw.Draw(img)
    _brush(d, [(8, 58), (40, 26)], 3.2, 3.2, wood_dark)
    _brush(d, [(9, 57), (40, 26)], 2.0, 2.0, wood)
    if big:
        poly = [(30, 8), (46, 2), (60, 14), (60, 30), (46, 40), (34, 34), (38, 22)]
    else:
        poly = [(32, 12), (47, 5), (59, 17), (50, 30), (37, 30), (36, 20)]
    d.polygon(poly, fill=rgba(edge))
    inner = [(34 + (x - 34) * 0.78, 18 + (y - 18) * 0.78) for (x, y) in poly]
    d.polygon(inner, fill=rgba(head))
    d.line([(inner[1][0], inner[1][1]), (inner[2][0], inner[2][1] + 1)], fill=rgba(shine), width=3)
    if spikes:
        for (x, y) in ((58, 12), (60, 22), (54, 32)):
            d.polygon([(x - 2, y - 3), (x + 5, y), (x - 2, y + 3)], fill=rgba(edge))
    if bind:
        _brush(d, [(34, 31), (40, 37)], 2.2, 2.2, bind)
    return _finish(img)


def eel_icon():
    img = _canvas()
    d = ImageDraw.Draw(img)
    pts = [(8 + i * 3.2, 34 + 11 * math.sin(i * 0.62 + 0.4)) for i in range(0, 17)]
    _brush(d, pts, 6.0, 2.2, 0x1E8DB0)
    _brush(d, pts, 4.2, 1.2, 0x35C7E8)
    _brush(d, pts, 2.0, 0.6, 0xA8EEFF, off=(0, -1.8))
    d.ellipse([9, 30, 15, 36], fill=rgba(0xFFFFFF))
    d.ellipse([11, 31, 15, 35], fill=rgba(0x1A1A1A))
    img = _finish(img)
    for (x, y) in [(12, 1), (11, 2), (12, 3), (11, 4), (3, 12), (4, 13), (3, 14), (13, 12), (14, 13), (13, 14)]:
        img.putpixel((x, y), rgba(0xF2B930))
    for (x, y) in [(11, 3), (12, 2), (3, 13), (14, 14)]:
        img.putpixel((x, y), rgba(0xFFFFFF))
    return img



def build_icons():
    os.makedirs(ICON_DIR, exist_ok=True)
    icons = {}
    for slug, rows in MAPS.items():
        icons[slug] = from_map(rows)

    icons["rusty_cutlass"] = sword_icon(0xC8742C, 0x7A3E12, 0xF0A860, 0x4A2E18, 0x4A2E18, 0x7A5230,
                                        curve=-0.55, width=4.6, tip=(58, 7))
    icons["coral_cutlass"] = sword_icon(0xF29BB0, 0xA83D5E, 0xFFE0E8, 0xF2B930, 0x7A5230, 0xF2B930,
                                        curve=-0.55, width=4.8, tip=(58, 7), glow=[(13, 3)])
    icons["leviathan_fang"] = sword_icon(0x2EC4B6, 0x136A64, 0xBFFFF6, 0xE6E0C8, 0xE6E0C8, 0xB8B09A,
                                         curve=1.3, width=5.2, tip=(56, 3), serrate=True)
    icons["tidebreaker"] = sword_icon(0x35C7E8, 0x1E6FA0, 0xE6FBFF, 0xF2B930, 0x2F4F7A, 0xF2B930,
                                      curve=0.0, width=5.4, tip=(61, 3), glow=[(14, 1), (11, 4)])
    icons["driftwood_hatchet"] = axe_icon(0xD9B380, 0x7A5230, 0xF3DDB8, 0xA98C66, 0x5C4631, bind=0x3B7A57)
    icons["sailors_hatchet"] = axe_icon(0xB8C4CC, 0x56606A, 0xFFFFFF, 0xB07A44, 0x4A2E18, bind=0x3F6FB0)
    icons["kraken_cleaver"] = axe_icon(0x8E5BD0, 0x3A2470, 0xD9C2FF, 0x5A3A99, 0x2A1A55, big=True, spikes=True)
    icons["electric_eel"] = eel_icon()

    # ikan: digambar lewat kode biar bentuknya konsisten sama ikan lama (Small / Big / Golden)
    icons["sardine"] = fish_icon(0x8FB3C9, 0xE9F2F7, 0x5E86A6, 11, 4, tail=2, stripe=0x3F6F94)
    icons["salmon"] = fish_icon(0xE0735A, 0xFFC2A8, 0xC24E3A, 12, 6, tail=3, spots=[(6, 6), (8, 6), (10, 7)])
    icons["firefin_tuna"] = fish_icon(0x3B5C8C, 0xB9CFE6, 0xF2701E, 11, 7, tail=4, stripe=0xF2B930,
                                      glow=[(2, 2), (13, 5)])

    out = {}
    for slug, img in icons.items():
        big = img.resize((64, 64), Image.NEAREST)
        path = os.path.join(ICON_DIR, slug + ".png")
        # ikon yang sudah ada TIDAK ditimpa (sekarang ikonnya dari gambar sheet: tools/import_fishing_sheet.py).
        # Mau balik ke ikon gambar-kode ini: jalankan dengan --force.
        if FORCE_ICONS or not os.path.exists(path):
            big.save(path)
        out[slug] = big
    return out


def preview(icons, path):
    names = sorted(icons)
    cols = 8
    rows = (len(names) + cols - 1) // cols
    cell = 110
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (52, 58, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        im = icons[n].resize((96, 96), Image.NEAREST)
        x, y = (i % cols) * cell + 7, (i // cols) * cell + 2
        sheet.alpha_composite(im, (x, y))
        d.text((x, y + 94), n[:16], fill=(220, 220, 220, 255))
    sheet.convert("RGB").save(path)


# ----------------------------------------------------------------------------------------------
# SUARA (disintesis: sinus + noise + envelope, 22.05 kHz mono 16-bit)
# ----------------------------------------------------------------------------------------------
SR = 22050


def t_axis(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(n, attack=0.005, decay=0.15):
    t = np.arange(n) / SR
    a = np.minimum(1.0, t / max(attack, 1e-4))
    d = np.exp(-t / max(decay, 1e-4))
    return a * d


def lowpass(x, k):
    """moving-average lowpass sederhana (k = lebar jendela)."""
    if k <= 1:
        return x
    ker = np.ones(k) / k
    return np.convolve(x, ker, mode="same")


def tone(freq, sec, decay=0.2, harmonics=((1, 1.0),), attack=0.004):
    t = t_axis(sec)
    f = freq if np.isscalar(freq) else freq
    if np.isscalar(f):
        phase = 2 * np.pi * f * t
    else:
        phase = 2 * np.pi * np.cumsum(f) / SR
    y = np.zeros_like(t)
    for h, amp in harmonics:
        y += amp * np.sin(phase * h)
    return y * env(len(t), attack, decay)


def noise(sec, seed=0):
    rng = np.random.default_rng(seed)
    return rng.uniform(-1, 1, int(SR * sec))


def save_wav(name, y, gain=0.8):
    os.makedirs(SFX_DIR, exist_ok=True)
    y = np.asarray(y, dtype=np.float64)
    peak = np.max(np.abs(y)) or 1.0
    y = y / peak * gain
    fade = int(SR * 0.004)
    y[-fade:] *= np.linspace(1, 0, fade)
    data = (y * 32767).astype("<i2")
    with wave.open(os.path.join(SFX_DIR, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def mix(*parts):
    n = max(len(p) for p, _ in parts)
    y = np.zeros(n)
    for p, off in parts:
        s = int(off * SR)
        end = min(n, s + len(p))
        if s < n:
            y[s:end] += p[: end - s]
    return y


def build_sfx():
    # cast: whoosh naik-turun (noise disaring, dibuka lalu ditutup) + ayunan joran
    n = noise(0.45, 1)
    sweep = np.concatenate([lowpass(n[: int(SR * 0.2)], 14), lowpass(n[int(SR * 0.2):], 40)])
    e = np.sin(np.linspace(0, np.pi, len(sweep))) ** 1.5
    save_wav("cast", sweep * e * 1.4)

    # splash: noise terang meluruh + "bloop" turun
    s = noise(0.5, 2)
    s = (s - lowpass(s, 6)) * env(len(s), 0.002, 0.09)          # highpass kasar
    bl = tone(np.linspace(520, 180, int(SR * 0.22)), 0.22, decay=0.07)
    save_wav("splash", mix((s, 0), (bl * 0.8, 0.02)))

    # nibble: tuk kecil
    save_wav("nibble", tone(np.linspace(420, 300, int(SR * 0.1)), 0.1, decay=0.03) * 0.8)

    # bite: plop dalam + ping tinggi (peringatan yang jelas)
    plop = tone(np.linspace(300, 120, int(SR * 0.18)), 0.18, decay=0.06)
    ping = tone(1320, 0.28, decay=0.09, harmonics=((1, 1.0), (2, 0.35)))
    save_wav("bite", mix((plop, 0), (ping * 0.9, 0.05)))

    # reel: klik ratchet berulang
    clicks = np.zeros(int(SR * 0.6))
    for i in range(9):
        off = int(SR * (0.02 + i * 0.065))
        c = (noise(0.02, 10 + i) * env(int(SR * 0.02), 0.0005, 0.004))
        clicks[off:off + len(c)] += c * 0.9
    clicks += lowpass(noise(0.6, 3), 30) * 0.18 * np.sin(np.linspace(0, np.pi, len(clicks)))
    save_wav("reel", clicks)

    # miss: nada turun lemas
    save_wav("miss", tone(np.linspace(380, 170, int(SR * 0.4)), 0.4, decay=0.2, harmonics=((1, 1), (2, 0.2))))

    # junk: "boing" konyol
    wob = 1 + 0.25 * np.sin(2 * np.pi * 11 * t_axis(0.4))
    save_wav("junk", tone(np.linspace(250, 90, int(SR * 0.4)) * wob, 0.4, decay=0.12,
                          harmonics=((1, 1), (3, 0.3))) * 0.9)

    # catch: pop kecil + chime naik (makin langka, makin panjang)
    pop = tone(np.linspace(260, 620, int(SR * 0.09)), 0.09, decay=0.03)

    def chime(notes, step, ring):
        parts = [(pop * 0.7, 0)]
        for i, f in enumerate(notes):
            parts.append((tone(f, ring, decay=ring * 0.45, harmonics=((1, 1.0), (2, 0.3), (3, 0.1))), 0.08 + i * step))
        return mix(*parts)

    save_wav("catch_common", chime([784], 0.0, 0.25))
    save_wav("catch_uncommon", chime([659, 880], 0.09, 0.3))
    save_wav("catch_rare", chime([659, 880, 1047], 0.09, 0.4))
    save_wav("catch_epic", chime([523, 659, 784, 1047, 1319], 0.08, 0.5))
    save_wav("catch_legendary", chime([523, 659, 784, 1047, 1319, 1568, 2093], 0.075, 0.7))


FORCE_ICONS = "--force" in sys.argv

if __name__ == "__main__":
    argv = [a for a in sys.argv[1:] if not a.startswith("--")]
    what = argv[0] if len(argv) > 0 else "all"
    if what in ("all", "icons"):
        ic = build_icons()
        print("ikon:", len(ic), "->", ICON_DIR, "(ikon yang sudah ada dibiarkan)" if not FORCE_ICONS else "(ditimpa)")
        if len(argv) > 1:
            preview(ic, argv[1])
    if what in ("all", "sfx"):
        build_sfx()
        print("sfx ->", SFX_DIR)
