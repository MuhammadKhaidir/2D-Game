#!/usr/bin/env python3
"""
Generator sprite "Drowned Soldier" (Water Domain / Sunken Kingdom) — pixel-art 4 arah, SATU sprite sheet.

Desainnya ORIGINAL, digambar dari nol di script ini (bukan hasil recolor / edit sprite mob lain):
  prajurit kerajaan bawah laut yang sudah mati tenggelam. Kulit biru-keabuan yang lembap, mata cekung berpendar
  cyan, helm kettle penyok dengan rumput laut menjuntai, baju zirah perunggu yang berkarat jadi hijau patina
  (lambang mahkota-trisula yang sudah pudar), jubah biru kerajaan yang compang-camping, lumut di bahu, dan
  pedang berkarat yang gripil di ujungnya.

Semua frame digambar langsung di resolusi aslinya (48x48 per sel, tanpa anti-alias), lalu diperbesar 2x
(nearest-neighbor, tajam) oleh Entitas/DrownedSoldier.java pas game jalan.

Output:
    src/Aset/DrownedSoldier/drowned_soldier_sheet.png   sprite sheet (288 x 960)
    src/drop/DrownedEmblem.png                           ikon item drop "Drowned Royal Emblem" (64 x 64)

Tata letak sheet (HARUS sama dengan DrownedSoldier.java):
    kolom  = nomor frame (maks 6, sel 48 px)
    baris  = ANIM_INDEX * 4 + ARAH_INDEX
    anim   : 0 idle (4) | 1 walk (6) | 2 attack (6, frame ke-4 = tebasan kena) | 3 hurt (2) | 4 death (6)
    arah   : 0 down(depan) | 1 up(belakang) | 2 left | 3 right (cermin dari left)
    kaki   : menapak di baris ke-44 tiap sel (GROUND)

Jalankan dari folder 2DGame:
    pip install pillow
    python tools/generate_drowned_soldier.py                    # tulis sheet + ikon drop ke src/
    python tools/generate_drowned_soldier.py --preview out.png  # contact sheet diperbesar 4x buat dicek mata

Mau ganti warna? Edit blok PALETTE lalu jalankan ulang.
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent / 'src'
SHEET_PATH = ROOT / 'Aset' / 'DrownedSoldier' / 'drowned_soldier_sheet.png'
ICON_PATH = ROOT / 'drop' / 'DrownedEmblem.png'

CELL = 48
GROUND = 44
COLS = 6
ANIMS = [('idle', 4), ('walk', 6), ('attack', 6), ('hurt', 2), ('death', 6)]
DIRS = ['down', 'up', 'left', 'right']


# ───────────── PALETTE ─────────────
def rgb(h, a=255):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


PALETTE = {
    'line':      rgb('#0e171b'),
    # kulit mayat tenggelam
    'skin':      rgb('#6f9a96'), 'skin_l': rgb('#93bdb3'), 'skin_d': rgb('#486e6d'), 'skin_dd': rgb('#314e52'),
    'bone':      rgb('#c7d3c6'),
    'eye':       rgb('#9cf7ff'), 'eye_d': rgb('#3fb7c9'), 'socket': rgb('#16262b'),
    # zirah perunggu berpatina
    'arm':       rgb('#477f72'), 'arm_l': rgb('#78b8a0'), 'arm_d': rgb('#2d5851'), 'arm_dd': rgb('#1f3d3b'),
    'bronze':    rgb('#8d6c3a'), 'bronze_d': rgb('#5b4327'), 'gold': rgb('#b9a050'),
    # kain tabard / jubah kerajaan yang pudar
    'cloth':     rgb('#35558a'), 'cloth_l': rgb('#4d73ab'), 'cloth_d': rgb('#223a63'), 'cloth_dd': rgb('#17264a'),
    # kulit sepatu / sabuk
    'leather':   rgb('#3b2f2a'), 'leather_l': rgb('#5a493d'),
    # lumut & rumput laut
    'moss':      rgb('#557f33'), 'moss_l': rgb('#7aa447'), 'kelp': rgb('#2f6140'), 'kelp_l': rgb('#4d8a55'),
    'barn':      rgb('#cfd8d0'),
    # pedang berkarat
    'rust':      rgb('#a3552d'), 'rust_l': rgb('#cf8348'), 'rust_d': rgb('#5f2f1b'), 'steel': rgb('#8f9fa3'),
    # air
    'water':     rgb('#6cc4e8'), 'water_l': rgb('#d6f6ff'), 'puddle': rgb('#2b6f95'), 'puddle_l': rgb('#4aa0c4'),
    'trail':     rgb('#bfefff', 150), 'trail2': rgb('#7fd3ee', 110),
}
P = PALETTE


# ───────────── KANVAS ─────────────
class Canvas:
    def __init__(self):
        self.im = Image.new('RGBA', (CELL, CELL), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def px(self, x, y, c):
        if 0 <= x < CELL and 0 <= y < CELL:
            self.im.putpixel((int(x), int(y)), c)

    def rect(self, x0, y0, x1, y1, c):
        self.d.rectangle([x0, y0, x1, y1], fill=c)

    def ell(self, x0, y0, x1, y1, c):
        self.d.ellipse([x0, y0, x1, y1], fill=c)

    def poly(self, pts, c):
        self.d.polygon(pts, fill=c)

    def line(self, p0, p1, c, w=1):
        if w <= 1:
            self.d.line([p0, p1], fill=c)
        else:
            self.d.line([p0, p1], fill=c, width=w)

    def get(self, x, y):
        if 0 <= x < CELL and 0 <= y < CELL:
            return self.im.getpixel((x, y))
        return (0, 0, 0, 0)

    def bevel(self, x0, y0, x1, y1, base, light, dark):
        """Kotak dengan sorotan atas-kiri dan bayangan bawah-kanan (kesan logam)."""
        self.rect(x0, y0, x1, y1, base)
        self.d.line([(x0, y0), (x1, y0)], fill=light)
        self.d.line([(x0, y0), (x0, y1)], fill=light)
        self.d.line([(x0, y1), (x1, y1)], fill=dark)
        self.d.line([(x1, y0 + 1), (x1, y1)], fill=dark)

    def speckle(self, x0, y0, x1, y1, colors, n, seed):
        """Bintik acak di atas piksel yang sudah terisi (karat / lumut / bopeng)."""
        r = random.Random(seed)
        x0, y0, x1, y1 = int(x0), int(y0), int(x1), int(y1)
        for _ in range(n):
            x, y = r.randint(x0, x1), r.randint(y0, y1)
            if self.get(x, y)[3] > 0:
                self.px(x, y, r.choice(colors))

    def outline(self):
        """Garis tepi 1 px di seluruh siluet (warna gelap kehijauan), supaya semua frame konsisten."""
        src = self.im.copy()
        out = self.im
        for y in range(CELL):
            for x in range(CELL):
                if src.getpixel((x, y))[3] > 40:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < CELL and 0 <= ny < CELL and src.getpixel((nx, ny))[3] > 200:
                        out.putpixel((x, y), P['line'])
                        break


# ───────────── BAGIAN-BAGIAN TUBUH ─────────────
def kelp_strand(cv, x, y, length, sway, seed, thick=False):
    """Rumput laut menjuntai: zig-zag kecil ke bawah, ikut bergoyang."""
    r = random.Random(seed)
    cx = x
    for i in range(length):
        off = int(round(math.sin((i * 0.9) + sway * 1.3 + seed) * 1.2))
        c = P['kelp_l'] if (i + seed) % 3 == 0 else P['kelp']
        cv.px(cx + off, y + i, c)
        if thick and i % 2 == 0:
            cv.px(cx + off + 1, y + i, P['kelp'])
    return cv


SW = 16   # panjang bilah pedang (px)


def sword(cv, hand, ang, slen=SW, seed=3):
    """Pedang berkarat (bilah ramping, mata pedang gripil, pelindung tangan perunggu). ang dalam derajat layar:
    0 = kanan, 90 = bawah, 180 = kiri, 270 = atas."""
    hx, hy = hand
    a = math.radians(ang)
    ux, uy = math.cos(a), math.sin(a)
    nx, ny = -uy, ux
    gx, gy = hx + ux * 2, hy + uy * 2                      # pangkal bilah (tepat di atas pelindung)
    tx, ty = hx + ux * slen, hy + uy * slen                # ujung bilah
    mx, my = gx + (tx - gx) * 0.72, gy + (ty - gy) * 0.72
    # bilah: separuh pangkal 2 px (berkarat), ujung meruncing 1 px (gripil)
    cv.line((round(gx), round(gy)), (round(mx), round(my)), P['rust_d'], 3)
    cv.line((round(gx), round(gy)), (round(mx), round(my)), P['rust'], 2)
    cv.line((round(mx), round(my)), (round(tx), round(ty)), P['rust'], 1)
    # mata pedang yang masih agak mengkilap + bintik karat
    cv.line((round(gx + nx * 0.6), round(gy + ny * 0.6)), (round(tx + nx * 0.6), round(ty + ny * 0.6)), P['steel'], 1)
    r = random.Random(seed)
    for _ in range(7):                                  # bintik karat HANYA di sepanjang bilah
        t = r.uniform(0.15, 0.95)
        cv.px(round(gx + (tx - gx) * t), round(gy + (ty - gy) * t), r.choice([P['rust_d'], P['rust_l'], P['rust_d']]))
    cv.px(round(tx - ux * 3 + nx), round(ty - uy * 3 + ny), (0, 0, 0, 0))   # gigi gripil di tepi
    # pelindung tangan (perunggu, tampak jelas), gagang kulit, pommel
    cv.line((round(gx - nx * 3), round(gy - ny * 3)), (round(gx + nx * 3), round(gy + ny * 3)), P['bronze'], 1)
    cv.px(round(gx - nx * 3), round(gy - ny * 3), P['gold']); cv.px(round(gx + nx * 3), round(gy + ny * 3), P['gold'])
    cv.line((round(hx), round(hy)), (round(hx - ux * 2), round(hy - uy * 2)), P['leather'], 2)
    cv.px(round(hx - ux * 3), round(hy - uy * 3), P['gold'])


def sweep(cv, prev, cur, b=0, slen=SW):
    """Smear tebasan pucat berbentuk bulan sabit: area yang disapu ujung bilah dari pose sebelumnya ke pose sekarang."""
    (h0, a0), (h1, a1) = prev, cur
    da = ((a1 - a0 + 180) % 360) - 180                 # selisih sudut lewat jalur terpendek
    da = max(-75, min(75, da))                         # batasi supaya sabitnya tetap ramping
    steps = 10
    outer, inner = [], []
    for i in range(steps + 1):
        t = i / steps
        hx = h0[0] + (h1[0] - h0[0]) * t
        hy = h0[1] + (h1[1] - h0[1]) * t + b
        a = math.radians(a1 - da + da * t)
        ux, uy = math.cos(a), math.sin(a)
        outer.append((hx + ux * (slen + 3), hy + uy * (slen + 3)))
        inner.append((hx + ux * (slen * 0.72), hy + uy * (slen * 0.72)))
    cv.poly(outer + inner[::-1], P['trail2'])
    for i in range(steps):                              # tepi luar lebih terang
        cv.line((round(outer[i][0]), round(outer[i][1])), (round(outer[i + 1][0]), round(outer[i + 1][1])), P['trail'], 1)


def finish_weapon(cv, pose, hx, hy, b, seed):
    if 'ang' not in pose:
        return
    if pose.get('sweep'):
        sweep(cv, pose['sweep'], (pose['hand'], pose['ang']), b, pose.get('slen', SW))
    sword(cv, (hx, hy), pose['ang'], pose.get('slen', SW), seed)


def clamp_hand(shoulder, hand, reach=11):
    """Tangan gak boleh lebih jauh dari `reach` px dari bahu (biar lengan gak kelihatan memanjang)."""
    dx, dy = hand[0] - shoulder[0], hand[1] - shoulder[1]
    d = math.hypot(dx, dy)
    if d <= reach:
        return hand
    return (round(shoulder[0] + dx * reach / d), round(shoulder[1] + dy * reach / d))


def drips(cv, pts, phase):
    for i, (x, y) in enumerate(pts):
        yy = y + (phase * 2 + i * 3) % 7
        if yy < GROUND + 1:
            cv.px(x, yy, P['water'])
            if (phase + i) % 2 == 0:
                cv.px(x, yy - 1, P['water_l'])


# ───────────── TAMPAK DEPAN (down) ─────────────
def draw_front(pose):
    cv = Canvas()
    b = pose.get('bob', 0)
    sway = pose.get('sway', 0)
    legs = pose.get('legs', [(0, 0), (0, 0)])     # (dx, angkat) untuk kaki kiri / kanan layar
    hx, hy = pose.get('head', (0, 0))
    sink = pose.get('sink', 0)                     # badan turun (berlutut)
    kneel = pose.get('kneel', 0)                   # kaki ditekuk (pendek)
    b += sink
    lean = pose.get('lean', 0)

    # jubah / lembaran kain di belakang badan
    cv.poly([(17 + lean, 20 + b), (30 + lean, 20 + b), (31 + lean, 33 + b), (29 + sway - 1, 38 + b), (26, 36 + b), (23, 39 + b),
             (20, 36 + b), (17 - sway, 38 + b), (16 + lean, 31 + b)], P['cloth_d'])
    cv.px(20, 37 + b, P['cloth_dd']); cv.px(26, 37 + b, P['cloth_dd'])

    # kaki: paha berkain, greaves berpatina, sepatu kulit
    for i, bx in enumerate((19, 25)):
        dx, lift = legs[i]
        top = 31 + b
        bot = GROUND - lift - kneel
        if bot < top + 3:
            bot = top + 3
        x0 = bx + dx
        cv.rect(x0, top, x0 + 3, min(bot - 3, top + 4), P['cloth_d'])                # paha / rok kain
        cv.bevel(x0, min(bot - 3, top + 4) + 1, x0 + 3, bot - 2, P['arm'], P['arm_l'], P['arm_d'])   # greave
        cv.rect(x0 - 1, bot - 1, x0 + 4, bot, P['leather'])                           # sepatu
        cv.px(x0 - 1, bot - 1, P['leather_l']); cv.px(x0 + 4, bot, P['moss'])
        cv.px(x0 + 1, min(bot - 3, top + 4) + 2, P['bronze_d'])

    # lengan jauh (kanan layar), turun santai
    ax, ay = pose.get('arm2', (32, 31))
    cv.line((31 + lean, 22 + b), (ax, ay + b), P['arm_d'], 3)
    cv.rect(ax - 1, ay + b - 1, ax + 1, ay + b + 1, P['skin_d'])

    # badan: breastplate + tabard + sabuk
    cv.bevel(18 + lean, 20 + b, 29 + lean, 29 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.rect(19 + lean, 27 + b, 28 + lean, 28 + b, P['arm_d'])                          # bagian bawah pelat
    # lambang mahkota-trisula yang pudar (emas kusam)
    cv.px(23 + lean, 22 + b, P['gold']); cv.px(24 + lean, 22 + b, P['gold'])
    cv.px(21 + lean, 23 + b, P['gold']); cv.px(26 + lean, 23 + b, P['gold'])
    cv.px(22 + lean, 24 + b, P['gold']); cv.px(25 + lean, 24 + b, P['gold'])
    cv.px(23 + lean, 24 + b, P['bronze']); cv.px(24 + lean, 24 + b, P['bronze'])
    cv.px(23 + lean, 25 + b, P['bronze_d']); cv.px(24 + lean, 25 + b, P['bronze_d'])
    cv.px(23 + lean, 26 + b, P['bronze_d']); cv.px(24 + lean, 26 + b, P['bronze_d'])
    cv.px(19 + lean, 21 + b, P['arm_l']); cv.px(28 + lean, 24 + b, P['arm_dd'])       # retak
    cv.px(28 + lean, 25 + b, P['arm_dd']); cv.px(27 + lean, 26 + b, P['arm_dd'])
    cv.rect(18 + lean, 29 + b, 29 + lean, 30 + b, P['leather'])                        # sabuk
    cv.px(23 + lean, 29 + b, P['gold']); cv.px(24 + lean, 29 + b, P['bronze'])
    # tabard menjuntai (compang-camping)
    cv.rect(21 + lean, 31 + b, 26 + lean, 34 + b, P['cloth'])
    cv.px(21 + lean, 35 + b, P['cloth']); cv.px(23 + lean, 36 + b, P['cloth']); cv.px(25 + lean, 35 + b, P['cloth'])
    cv.px(24 + lean, 33 + b, P['cloth_d']); cv.px(22 + lean, 32 + b, P['cloth_l'])
    # lumut & bopeng di zirah
    cv.speckle(18 + lean, 20 + b, 29 + lean, 28 + b, [P['moss'], P['arm_dd'], P['moss_l']], 7, 11 + pose.get('seed', 0))

    # bahu (pauldron) — kiri layar utuh berlumut, kanan layar sompel
    cv.bevel(13 + lean, 19 + b, 18 + lean, 23 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.px(13 + lean, 19 + b, (0, 0, 0, 0))
    cv.rect(14 + lean, 19 + b, 17 + lean, 19 + b, P['moss'])
    cv.px(15 + lean, 18 + b, P['moss_l']); cv.px(16 + lean, 18 + b, P['moss'])
    cv.bevel(29 + lean, 19 + b, 34 + lean, 23 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.px(34 + lean, 19 + b, (0, 0, 0, 0)); cv.px(34 + lean, 20 + b, (0, 0, 0, 0)); cv.px(33 + lean, 19 + b, P['arm_dd'])
    cv.px(31 + lean, 22 + b, P['barn']); cv.px(32 + lean, 21 + b, P['barn'])           # teritip

    # lengan dekat (kiri layar) — tangan pemegang pedang
    wx, wy = clamp_hand((15 + lean, 22), pose.get('hand', (14, 31)))
    cv.line((15 + lean, 22 + b), (wx, wy + b), P['arm'], 3)
    cv.line((15 + lean, 22 + b), (wx, wy + b), P['arm_d'], 1)
    cv.rect(wx - 1, wy + b - 1, wx + 1, wy + b + 1, P['skin'])
    cv.px(wx, wy + b, P['skin_l'])

    # leher + kepala
    cv.rect(21 + lean, 18 + b, 26 + lean, 19 + b, P['skin_d'])
    ox, oy = hx + lean, hy + b
    cv.rect(19 + ox, 12 + oy, 28 + ox, 19 + oy, P['skin'])                              # wajah
    cv.rect(19 + ox, 12 + oy, 28 + ox, 12 + oy, P['skin_d'])
    cv.px(19 + ox, 18 + oy, P['skin_d']); cv.px(28 + ox, 18 + oy, P['skin_d'])
    # mata cekung berpendar
    eye = P['eye'] if not pose.get('hurt') else P['water_l']
    cv.rect(20 + ox, 14 + oy, 22 + ox, 16 + oy, P['socket']); cv.rect(25 + ox, 14 + oy, 27 + ox, 16 + oy, P['socket'])
    cv.px(21 + ox, 15 + oy, eye); cv.px(26 + ox, 15 + oy, eye)
    if not pose.get('dim'):
        cv.px(21 + ox, 14 + oy, P['eye_d']); cv.px(26 + ox, 14 + oy, P['eye_d'])
    cv.px(23 + ox, 16 + oy, P['skin_dd']); cv.px(24 + ox, 16 + oy, P['skin_dd'])         # lubang hidung
    # mulut & gigi
    if pose.get('hurt'):
        cv.rect(22 + ox, 17 + oy, 25 + ox, 19 + oy, P['socket'])
        cv.px(22 + ox, 17 + oy, P['bone']); cv.px(25 + ox, 17 + oy, P['bone'])
    else:
        cv.line((21 + ox, 18 + oy), (26 + ox, 18 + oy), P['skin_dd'])
        cv.px(22 + ox, 18 + oy, P['bone']); cv.px(24 + ox, 18 + oy, P['bone'])
    cv.px(20 + ox, 17 + oy, P['skin_d']); cv.px(27 + ox, 17 + oy, P['skin_d'])
    # helm kettle penyok + retak
    cv.ell(17 + ox, 4 + oy, 30 + ox, 14 + oy, P['arm'])
    cv.rect(17 + ox, 11 + oy, 30 + ox, 13 + oy, P['arm'])
    cv.line((17 + ox, 13 + oy), (30 + ox, 13 + oy), P['arm_d'])                          # bibir helm
    cv.line((18 + ox, 5 + oy), (26 + ox, 4 + oy), P['arm_l'])
    cv.px(19 + ox, 6 + oy, P['arm_l']); cv.px(20 + ox, 6 + oy, P['arm_l'])
    cv.rect(23 + ox, 5 + oy, 24 + ox, 12 + oy, P['bronze'])                              # jambul / strip tengah
    cv.px(23 + ox, 5 + oy, P['gold']); cv.px(24 + ox, 8 + oy, P['bronze_d'])
    cv.px(27 + ox, 7 + oy, P['arm_dd']); cv.px(28 + ox, 8 + oy, P['arm_dd']); cv.px(28 + ox, 9 + oy, P['arm_dd'])   # retak
    cv.px(17 + ox, 11 + oy, (0, 0, 0, 0)); cv.px(30 + ox, 5 + oy, (0, 0, 0, 0))         # penyok
    cv.speckle(18 + ox, 5 + oy, 29 + ox, 12 + oy, [P['moss'], P['arm_dd'], P['moss_l']], 6, 21 + pose.get('seed', 0))
    # rumput laut menjuntai dari helm
    kelp_strand(cv, 17 + ox, 13 + oy, 9 + (sway % 2), sway, 1)
    kelp_strand(cv, 30 + ox, 12 + oy, 7, sway + 1, 2)
    kelp_strand(cv, 29 + ox, 14 + oy, 5, sway + 2, 4)

    finish_weapon(cv, pose, wx, wy + b, b, 3)
    if pose.get('drips'):
        drips(cv, [(16, 36), (31, 33), (23, 40)], pose['drips'])
    return cv


# ───────────── TAMPAK BELAKANG (up) ─────────────
def draw_back(pose):
    cv = Canvas()
    b = pose.get('bob', 0)
    sway = pose.get('sway', 0)
    legs = pose.get('legs', [(0, 0), (0, 0)])
    hx, hy = pose.get('head', (0, 0))
    sink = pose.get('sink', 0)
    kneel = pose.get('kneel', 0)
    b += sink
    lean = pose.get('lean', 0)

    for i, bx in enumerate((19, 25)):
        dx, lift = legs[i]
        top = 31 + b
        bot = max(GROUND - lift - kneel, top + 3)
        x0 = bx + dx
        cv.rect(x0, top, x0 + 3, min(bot - 3, top + 4), P['cloth_d'])
        cv.bevel(x0, min(bot - 3, top + 4) + 1, x0 + 3, bot - 2, P['arm_d'], P['arm'], P['arm_dd'])
        cv.rect(x0 - 1, bot - 1, x0 + 4, bot, P['leather'])
        cv.px(x0 + 4, bot - 1, P['leather_l'])

    # lengan jauh (kiri layar)
    ax, ay = pose.get('arm2', (15, 31))
    cv.line((16 + lean, 22 + b), (ax, ay + b), P['arm_d'], 3)
    cv.rect(ax - 1, ay + b - 1, ax + 1, ay + b + 1, P['skin_d'])

    # badan: punggung dibungkus jubah compang-camping
    cv.bevel(18 + lean, 20 + b, 29 + lean, 29 + b, P['arm_d'], P['arm'], P['arm_dd'])
    cv.poly([(17 + lean, 21 + b), (30 + lean, 21 + b), (31 + lean, 33 + b), (29 + sway - 1, 39 + b), (26, 36 + b), (23, 40 + b),
             (20, 37 + b), (17 - sway, 39 + b), (16 + lean, 32 + b)], P['cloth'])
    cv.rect(17 + lean, 21 + b, 30 + lean, 22 + b, P['cloth_l'])
    cv.line((23 + lean, 23 + b), (23 + lean, 37 + b), P['cloth_d'])                         # lipatan jubah
    cv.line((26 + lean, 24 + b), (27 + lean, 36 + b), P['cloth_d'])
    cv.line((20 + lean, 25 + b), (19 + lean, 35 + b), P['cloth_d'])
    cv.px(22 + lean, 28 + b, P['gold']); cv.px(24 + lean, 28 + b, P['gold']); cv.px(23 + lean, 29 + b, P['gold'])   # lambang pudar
    cv.px(23 + lean, 30 + b, P['bronze_d'])
    cv.px(19 + lean, 38 + b, (0, 0, 0, 0)); cv.px(27 + lean, 38 + b, (0, 0, 0, 0))
    cv.speckle(17 + lean, 21 + b, 30 + lean, 38 + b, [P['cloth_dd'], P['moss'], P['cloth_l']], 8, 31)
    cv.rect(18 + lean, 29 + b, 29 + lean, 29 + b, P['leather'])

    # bahu
    cv.bevel(13 + lean, 19 + b, 18 + lean, 23 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.rect(14 + lean, 19 + b, 17 + lean, 19 + b, P['moss']); cv.px(15 + lean, 18 + b, P['moss_l'])
    cv.bevel(29 + lean, 19 + b, 34 + lean, 23 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.px(34 + lean, 19 + b, (0, 0, 0, 0)); cv.px(34 + lean, 20 + b, (0, 0, 0, 0))
    cv.px(30 + lean, 22 + b, P['barn']); cv.px(31 + lean, 21 + b, P['barn'])

    # lengan dekat (kanan layar) — pemegang pedang
    wx, wy = clamp_hand((32 + lean, 22), pose.get('hand', (33, 31)))
    cv.line((32 + lean, 22 + b), (wx, wy + b), P['arm'], 3)
    cv.line((32 + lean, 22 + b), (wx, wy + b), P['arm_d'], 1)
    cv.rect(wx - 1, wy + b - 1, wx + 1, wy + b + 1, P['skin'])

    # kepala dari belakang: helm menutupi seluruh kepala + rumput laut panjang
    cv.rect(21 + lean, 18 + b, 26 + lean, 19 + b, P['skin_d'])
    ox, oy = hx + lean, hy + b
    cv.ell(17 + ox, 4 + oy, 30 + ox, 15 + oy, P['arm'])
    cv.rect(17 + ox, 11 + oy, 30 + ox, 14 + oy, P['arm'])
    cv.line((17 + ox, 14 + oy), (30 + ox, 14 + oy), P['arm_d'])
    cv.line((18 + ox, 5 + oy), (26 + ox, 4 + oy), P['arm_l'])
    cv.rect(23 + ox, 5 + oy, 24 + ox, 13 + oy, P['bronze'])
    cv.px(23 + ox, 5 + oy, P['gold'])
    cv.px(19 + ox, 8 + oy, P['arm_dd']); cv.px(20 + ox, 9 + oy, P['arm_dd']); cv.px(20 + ox, 10 + oy, P['arm_dd'])
    cv.speckle(18 + ox, 5 + oy, 29 + ox, 13 + oy, [P['moss'], P['arm_dd'], P['moss_l']], 7, 41)
    cv.rect(20 + ox, 15 + oy, 27 + ox, 17 + oy, P['skin_d'])                             # tengkuk
    kelp_strand(cv, 19 + ox, 14 + oy, 12 + (sway % 2), sway, 5, True)
    kelp_strand(cv, 24 + ox, 15 + oy, 10, sway + 1, 6)
    kelp_strand(cv, 28 + ox, 14 + oy, 9, sway + 2, 7)

    finish_weapon(cv, pose, wx, wy + b, b, 5)
    if pose.get('drips'):
        drips(cv, [(17, 37), (30, 34), (24, 41)], pose['drips'])
    return cv


# ───────────── TAMPAK SAMPING (left) ─────────────
def draw_side(pose):
    cv = Canvas()
    b = pose.get('bob', 0)
    sway = pose.get('sway', 0)
    stride = pose.get('stride', 0)                  # + = kaki depan (kiri layar) maju
    lifts = pose.get('lifts', (0, 0))               # angkat kaki depan / belakang
    hx, hy = pose.get('head', (0, 0))
    sink = pose.get('sink', 0)
    kneel = pose.get('kneel', 0)
    lean = pose.get('lean', 0)                      # + = condong ke depan (kiri layar)
    b += sink
    L = -lean

    # jubah menjuntai di belakang (kanan layar), ikut bergoyang
    cv.poly([(27 + L, 20 + b), (30 + L, 21 + b), (33 + sway + L, 30 + b), (34 + sway * 2 + L, 38 + b), (31 + sway + L, 36 + b),
             (29 + L, 40 + b), (26 + L, 34 + b)], P['cloth_d'])
    cv.px(32 + sway + L, 36 + b, P['cloth_dd']); cv.px(30 + L, 38 + b, P['cloth_dd'])

    # kaki belakang (gelap), lalu kaki depan
    for i, (sx, col, hi) in enumerate(((26 - stride, P['arm_d'], P['arm']), (21 + stride, P['arm'], P['arm_l']))):
        lift = lifts[i]
        top = 31 + b
        bot = max(GROUND - lift - kneel, top + 3)
        cv.rect(sx, top, sx + 3, min(bot - 3, top + 4), P['cloth_d'] if i == 0 else P['cloth'])
        cv.bevel(sx, min(bot - 3, top + 4) + 1, sx + 3, bot - 2, col, hi, P['arm_dd'])
        cv.rect(sx - 2, bot - 1, sx + 3, bot, P['leather'])         # sepatu menghadap kiri
        cv.px(sx - 2, bot - 1, P['leather_l']); cv.px(sx + 3, bot, P['moss'])
        cv.px(sx + 1, min(bot - 3, top + 4) + 1, P['bronze_d'])

    # lengan jauh (belakang badan)
    ax, ay = pose.get('arm2', (27, 31))
    cv.line((26 + L, 22 + b), (ax + L, ay + b), P['arm_dd'], 3)
    cv.rect(ax + L - 1, ay + b - 1, ax + L + 1, ay + b + 1, P['skin_dd'])

    # badan
    cv.bevel(21 + L, 20 + b, 29 + L, 29 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.rect(21 + L, 20 + b, 22 + L, 29 + b, P['arm_l'])
    cv.px(22 + L, 22 + b, P['gold']); cv.px(21 + L, 23 + b, P['gold']); cv.px(22 + L, 24 + b, P['bronze'])   # lambang dari samping
    cv.px(28 + L, 23 + b, P['arm_dd']); cv.px(27 + L, 24 + b, P['arm_dd']); cv.px(28 + L, 26 + b, P['arm_dd'])
    cv.rect(21 + L, 29 + b, 29 + L, 30 + b, P['leather'])
    cv.px(21 + L, 29 + b, P['gold'])
    cv.rect(21 + L, 31 + b, 25 + L, 34 + b, P['cloth'])                          # tabard depan
    cv.px(21 + L, 35 + b, P['cloth']); cv.px(23 + L, 36 + b, P['cloth']); cv.px(22 + L, 33 + b, P['cloth_l'])
    cv.speckle(21 + L, 20 + b, 29 + L, 28 + b, [P['moss'], P['arm_dd'], P['moss_l']], 6, 51)

    # bahu
    cv.bevel(21 + L, 18 + b, 27 + L, 23 + b, P['arm'], P['arm_l'], P['arm_d'])
    cv.px(27 + L, 18 + b, (0, 0, 0, 0))
    cv.rect(22 + L, 18 + b, 26 + L, 18 + b, P['moss']); cv.px(23 + L, 17 + b, P['moss_l']); cv.px(25 + L, 17 + b, P['moss'])
    cv.px(26 + L, 22 + b, P['barn']); cv.px(25 + L, 23 + b, P['barn'])

    # lengan dekat (pemegang pedang)
    wx, wy = clamp_hand((24 + L, 21), pose.get('hand', (17, 29)))
    cv.line((24 + L, 21 + b), (wx, wy + b), P['arm'], 3)
    cv.line((24 + L, 21 + b), (wx, wy + b), P['arm_d'], 1)
    cv.rect(wx - 1, wy + b - 1, wx + 1, wy + b + 1, P['skin'])

    # leher + kepala (profil menghadap kiri)
    cv.rect(22 + L, 18 + b, 26 + L, 19 + b, P['skin_d'])
    ox, oy = hx + L, hy + b
    cv.rect(18 + ox, 12 + oy, 26 + ox, 19 + oy, P['skin'])
    cv.rect(26 + ox, 12 + oy, 26 + ox, 19 + oy, P['skin_d'])
    cv.px(17 + ox, 15 + oy, P['skin']); cv.px(17 + ox, 16 + oy, P['skin_l'])           # hidung
    cv.px(18 + ox, 19 + oy, P['skin_d'])
    # mata
    cv.rect(19 + ox, 14 + oy, 21 + ox, 16 + oy, P['socket'])
    cv.px(20 + ox, 15 + oy, P['eye'] if not pose.get('hurt') else P['water_l'])
    if not pose.get('dim'):
        cv.px(20 + ox, 14 + oy, P['eye_d'])
    # mulut
    if pose.get('hurt'):
        cv.rect(18 + ox, 17 + oy, 20 + ox, 19 + oy, P['socket']); cv.px(18 + ox, 17 + oy, P['bone'])
    else:
        cv.line((18 + ox, 18 + oy), (21 + ox, 18 + oy), P['skin_dd']); cv.px(19 + ox, 18 + oy, P['bone'])
    # helm
    cv.ell(17 + ox, 4 + oy, 29 + ox, 14 + oy, P['arm'])
    cv.rect(17 + ox, 11 + oy, 29 + ox, 13 + oy, P['arm'])
    cv.rect(15 + ox, 12 + oy, 29 + ox, 13 + oy, P['arm'])                              # bibir helm menjorok ke depan
    cv.line((15 + ox, 13 + oy), (29 + ox, 13 + oy), P['arm_d'])
    cv.line((19 + ox, 5 + oy), (26 + ox, 4 + oy), P['arm_l'])
    cv.rect(22 + ox, 5 + oy, 23 + ox, 12 + oy, P['bronze']); cv.px(22 + ox, 5 + oy, P['gold'])
    cv.px(26 + ox, 7 + oy, P['arm_dd']); cv.px(27 + ox, 8 + oy, P['arm_dd']); cv.px(27 + ox, 9 + oy, P['arm_dd'])
    cv.px(17 + ox, 6 + oy, (0, 0, 0, 0)); cv.px(15 + ox, 12 + oy, (0, 0, 0, 0))
    cv.speckle(18 + ox, 5 + oy, 28 + ox, 12 + oy, [P['moss'], P['arm_dd'], P['moss_l']], 6, 61)
    # rumput laut dari helm (menjuntai ke belakang & bawah)
    kelp_strand(cv, 27 + ox, 13 + oy, 10 + (sway % 2), sway, 8, True)
    kelp_strand(cv, 25 + ox, 14 + oy, 6, sway + 1, 9)

    finish_weapon(cv, pose, wx, wy + b, b, 7)
    if pose.get('drips'):
        drips(cv, [(19, 38), (31, 36), (23, 41)], pose['drips'])
    return cv


# ───────────── SISA-SISA (frame mati akhir, semua arah) ─────────────
def draw_remains(stage, seed):
    """Tumpukan zirah + helm + tulang + pedang di genangan air. stage 0..2 = makin mendatar / makin melebar."""
    cv = Canvas()
    g = GROUND
    pw = 9 + stage * 3                                  # setengah lebar genangan
    r = random.Random(seed)
    # genangan
    cv.ell(24 - pw, g - 4 - stage, 24 + pw, g + 1, P['puddle'])
    cv.ell(24 - pw + 3, g - 3 - stage, 24 + pw - 3, g, P['puddle_l'])
    for _ in range(4 + stage * 2):
        cv.px(24 + r.randint(-pw + 3, pw - 3), g - 2 + r.randint(-1, 1), P['water_l'])
    # jubah ambruk
    cv.poly([(15, g - 3), (20, g - 11 + stage * 2), (29, g - 12 + stage * 2), (34, g - 3), (30, g - 1), (18, g - 1)], P['cloth_d'])
    cv.poly([(18, g - 3), (22, g - 9 + stage * 2), (28, g - 9 + stage * 2), (31, g - 3)], P['cloth'])
    cv.px(21, g - 6 + stage * 2, P['cloth_l']); cv.px(26, g - 5 + stage, P['cloth_dd'])
    # breastplate terbelah
    cv.bevel(17, g - 9 + stage * 2, 24, g - 3, P['arm'], P['arm_l'], P['arm_d'])
    cv.px(19, g - 6 + stage * 2, P['gold']); cv.px(20, g - 7 + stage * 2, P['gold'])
    cv.bevel(26, g - 8 + stage * 2, 31, g - 3, P['arm_d'], P['arm'], P['arm_dd'])
    cv.px(24, g - 7 + stage * 2, (0, 0, 0, 0)); cv.px(25, g - 6 + stage * 2, (0, 0, 0, 0))
    # tulang
    cv.line((14, g - 2), (20, g - 4), P['bone'], 1); cv.line((29, g - 3), (35, g - 1), P['bone'], 1)
    cv.px(33, g - 5, P['bone']); cv.px(34, g - 5, P['skin_l'])
    # helm menggelinding
    cv.ell(10, g - 8 + stage, 17, g - 2, P['arm']); cv.rect(10, g - 4 + stage, 17, g - 2, P['arm'])
    cv.line((10, g - 2), (17, g - 2), P['arm_d']); cv.px(12, g - 7 + stage, P['arm_l']); cv.px(13, g - 7 + stage, P['arm_l'])
    cv.px(15, g - 4 + stage, P['eye'] if stage == 0 else P['eye_d']); cv.px(14, g - 4 + stage, P['socket'])
    cv.px(11, g - 1, P['kelp']); cv.px(10, g, P['kelp_l']); cv.px(12, g, P['kelp'])
    # pedang tergeletak
    sword(cv, (33, g - 4), 330 - stage * 6, 14, 9)
    # rumput laut di atas timbunan
    kelp_strand(cv, 22, g - 11 + stage * 2, 5, 1, 12); kelp_strand(cv, 27, g - 11 + stage * 2, 4, 2, 13)
    cv.speckle(14, g - 12, 34, g - 1, [P['moss'], P['moss_l']], 7, 70 + seed)
    return cv


# ───────────── POSE PER ANIMASI ─────────────
# Tampak belakang (up) selalu dibuat dengan mencerminkan pose tampak depan (down), jadi cukup dua set pose.
def mirror_pose(p):
    q = dict(p)

    def mx(pt):
        return (47 - pt[0], pt[1])
    for k in ('hand', 'arm2'):
        if k in q:
            q[k] = mx(q[k])
    if 'ang' in q:
        q['ang'] = (180 - q['ang']) % 360
    if 'sweep' in q:
        q['sweep'] = (mx(q['sweep'][0]), (180 - q['sweep'][1]) % 360)
    if 'legs' in q:
        q['legs'] = [(-d, l) for d, l in reversed(q['legs'])]
    if 'head' in q:
        q['head'] = (-q['head'][0], q['head'][1])
    if 'lean' in q:
        q['lean'] = -q['lean']
    return q


def pose_idle(view, f):
    if view == 'up':
        return mirror_pose(pose_idle('down', f))
    bob = (0, 0, 1, 1)[f]
    p = {'bob': bob, 'sway': f, 'seed': 0, 'drips': (f + 1) if f % 2 == 0 else 0}
    if view == 'down':
        p.update(legs=[(0, 0), (0, 0)], hand=(14, 31), ang=250, arm2=(32, 31), head=(0, 0))
    else:
        p.update(stride=0, lifts=(0, 0), hand=(17, 29), ang=215, arm2=(27, 30), head=(0, 0))
    return p


def pose_walk(view, f):
    if view == 'up':
        return mirror_pose(pose_walk('down', f))
    bob = 1 if f in (1, 4) else 0
    p = {'bob': bob, 'sway': (f // 2) % 4, 'seed': 0}
    if view == 'down':
        lifts = [(0, 0), (3, 0), (2, 0), (0, 0), (0, 3), (0, 2)][f]
        swing = [0, 1, 2, 0, -1, -2][f]
        p.update(legs=[(0, lifts[0]), (0, lifts[1])], hand=(14 + swing, 31), ang=250 + swing * 4, arm2=(32 - swing, 31))
    else:
        ph = f / 6.0 * 2 * math.pi
        stride = int(round(3 * math.sin(ph)))
        p.update(stride=stride,
                 lifts=(max(0, int(round(-math.cos(ph) * 3))), max(0, int(round(math.cos(ph) * 3)))),
                 hand=(17 - stride // 2, 29), ang=215 + stride * 3, arm2=(27 + stride // 2, 30))
    return p


def pose_attack(view, f):
    """6 frame: 0-1 angkat pedang, 2 awal ayun, 3 tebasan kena (frame hit), 4-5 pulih. Tiap pose = (tangan, sudut pedang)."""
    if view == 'up':
        return mirror_pose(pose_attack('down', f))
    p = {'bob': (0, 0, 1, 1, 1, 0)[f], 'sway': f % 4, 'seed': 0}
    if view == 'down':
        hands = [(14, 27), (15, 20), (19, 21), (24, 28), (25, 30), (18, 31)]
        angs = [262, 282, 320, 50, 65, 245]
        arm2 = [(32, 30), (33, 28), (33, 27), (31, 30), (31, 31), (32, 31)]
        head = [(0, 0), (0, -1), (0, 0), (0, 1), (0, 1), (0, 0)][f]
        p.update(legs=[(0, 0), (0, 0)] if f < 3 else [(-1, 0), (1, 0)], hand=hands[f], ang=angs[f], arm2=arm2[f], head=head)
        if f in (2, 3):
            p['sweep'] = (hands[f - 1], angs[f - 1])
    else:
        hands = [(25, 21), (27, 17), (22, 17), (18, 24), (17, 29), (18, 29)]
        angs = [292, 318, 255, 185, 150, 195]
        arm2 = [(28, 29), (29, 27), (29, 26), (27, 29), (27, 30), (27, 30)]
        p.update(stride=(0, 0, 0, 2, 2, 0)[f], lifts=(0, 0), hand=hands[f], ang=angs[f], arm2=arm2[f],
                 head=[(0, 0), (0, -1), (0, 0), (-1, 1), (-1, 1), (0, 0)][f], lean=[0, -1, 0, 2, 2, 1][f])
        if f in (2, 3):
            p['sweep'] = (hands[f - 1], angs[f - 1])
    return p


def pose_hurt(view, f):
    """2 frame: kepala terdongak, badan terhuyung ke belakang, mata & mulut terbuka."""
    if view == 'up':
        return mirror_pose(pose_hurt('down', f))
    p = {'bob': 1, 'sway': 1 + f, 'seed': 0, 'hurt': True, 'head': (0, -1 if f == 0 else 0)}
    if view == 'down':
        p.update(legs=[(-1, 0), (1, 0)], hand=(13, 30), ang=238, arm2=(34, 28), lean=0 if f == 0 else 1)
    else:
        p.update(stride=-1, lifts=(0, 0), hand=(18, 28), ang=232, arm2=(29, 28), lean=-1 if f == 0 else 0,
                 head=(1, -1 if f == 0 else 0))
    return p


def pose_death(view, f):
    """0 terhuyung, 1 lutut menekuk (pedang lepas), 2 berlutut (pedang jatuh), 3-5 = tumpukan sisa (lihat draw_remains)."""
    if view == 'up':
        return mirror_pose(pose_death('down', f))
    p = {'bob': 0, 'sway': f % 4, 'seed': 0, 'hurt': f == 0, 'dim': f >= 1}
    sinks = [1, 4, 8]
    heads = [(0, -1), (0, 1), (0, 3)]
    if view == 'down':
        p.update(legs=[(-1, 0), (1, 0)] if f == 0 else [(-2, 0), (2, 0)], sink=sinks[f], head=heads[f],
                 hand=[(13, 30), (12, 31), (11, 32)][f], ang=[238, 120, 175][f], slen=[16, 11, 10][f],
                 arm2=[(34, 28), (33, 31), (33, 32)][f])
    else:
        p.update(stride=-1 if f == 0 else -2, lifts=(0, 0), sink=sinks[f],
                 head=(heads[f][0] - (0 if f == 0 else 1), heads[f][1]),
                 hand=[(18, 28), (17, 30), (16, 31)][f], ang=[232, 125, 178][f], slen=[16, 11, 10][f],
                 arm2=[(29, 28), (28, 31), (28, 32)][f], lean=-1 if f == 0 else -2)
    return p


def draw_view(view, pose):
    if view == 'down':
        return draw_front(pose)
    if view == 'up':
        return draw_back(pose)
    return draw_side(pose)


def build_frame(anim, view, f):
    """Gambar 1 frame (sel 48x48). view 'right' = cermin horizontal dari 'left'."""
    real = 'left' if view == 'right' else view
    if anim == 'death' and f >= 3:
        cv = draw_remains(f - 3, 3 if view in ('left', 'down') else 4)
        if view in ('left', 'up'):
            cv.im = cv.im.transpose(Image.FLIP_LEFT_RIGHT)
            cv.d = ImageDraw.Draw(cv.im)
    else:
        pose = {'idle': pose_idle, 'walk': pose_walk, 'attack': pose_attack, 'hurt': pose_hurt, 'death': pose_death}[anim](real, f)
        cv = draw_view(real, pose)
    if not (anim == 'death' and f >= 3):
        for yy in range(GROUND + 1, CELL):            # badan yang melorot (berlutut) gak boleh nembus tanah
            for xx in range(CELL):
                cv.im.putpixel((xx, yy), (0, 0, 0, 0))
    cv.outline()
    im = cv.im
    if view == 'right':
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
    return im


def build_sheet():
    sheet = Image.new('RGBA', (COLS * CELL, len(ANIMS) * len(DIRS) * CELL), (0, 0, 0, 0))
    for ai, (anim, n) in enumerate(ANIMS):
        for di, view in enumerate(DIRS):
            row = ai * len(DIRS) + di
            for f in range(n):
                sheet.paste(build_frame(anim, view, f), (f * CELL, row * CELL))
    return sheet


# ───────────── IKON DROP: "Drowned Royal Emblem" ─────────────
def build_icon():
    """Lambang kerajaan bawah laut (medali perunggu berkarat dengan mahkota-trisula), 16x16 -> 64x64."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([1, 1, 14, 14], fill=P['line'])
    d.ellipse([2, 2, 13, 13], fill=P['bronze_d'])
    d.ellipse([3, 3, 12, 12], fill=P['bronze'])
    d.arc([3, 3, 12, 12], 200, 300, fill=P['gold'])
    for (x, y) in ((7, 4), (8, 4), (5, 5), (10, 5), (5, 6), (10, 6), (6, 7), (9, 7), (7, 7), (8, 7), (7, 8), (8, 8), (7, 9), (8, 9), (7, 10), (8, 10)):
        im.putpixel((x, y), P['gold'])
    for (x, y) in ((6, 5), (9, 5), (7, 6), (8, 6), (6, 6)):
        im.putpixel((x, y), P['bronze_d'])
    r = random.Random(4)
    for _ in range(9):
        x, y = r.randint(3, 12), r.randint(3, 12)
        if im.getpixel((x, y))[3]:
            im.putpixel((x, y), r.choice([P['arm'], P['arm_d'], P['moss']]))
    for (x, y) in ((3, 11), (4, 12), (11, 4), (12, 5)):
        im.putpixel((x, y), P['barn'])
    im.putpixel((10, 12), P['kelp']); im.putpixel((11, 12), P['kelp_l']); im.putpixel((11, 13), P['kelp'])
    return im.resize((64, 64), Image.NEAREST)


def build_preview(sheet, scale=4):
    """Contact sheet diperbesar, dengan latar gelap + label baris (buat dicek mata, bukan dipakai game)."""
    bg = Image.new('RGBA', (sheet.width * scale + 90, sheet.height * scale), (22, 40, 58, 255))
    big = sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST)
    bg.alpha_composite(big, (90, 0))
    d = ImageDraw.Draw(bg)
    for ai, (anim, n) in enumerate(ANIMS):
        for di, view in enumerate(DIRS):
            y = (ai * 4 + di) * CELL * scale
            d.text((4, y + 4), f'{anim}\n{view}', fill=(230, 240, 255, 255))
            d.line([(90, y), (bg.width, y)], fill=(255, 255, 255, 30))
    return bg


def main(argv):
    sheet = build_sheet()
    if '--preview' in argv:
        out = Path(argv[argv.index('--preview') + 1])
        build_preview(sheet).save(out)
        print('preview ->', out)
        return
    SHEET_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(SHEET_PATH)
    ICON_PATH.parent.mkdir(parents=True, exist_ok=True)
    build_icon().save(ICON_PATH)
    print('sheet ->', SHEET_PATH, sheet.size)
    print('icon  ->', ICON_PATH)


if __name__ == '__main__':
    main(sys.argv[1:])
