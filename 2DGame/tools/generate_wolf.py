#!/usr/bin/env python3
"""
Generator sprite "Wolf" (serigala malam) — pixel-art 4 arah (kanan, kiri, atas, bawah).

Semua frame digambar langsung di resolusi aslinya (62x34 piksel, tanpa anti-alias), lalu diperbesar pakai nearest-neighbor
oleh Entitas/WolfSprite.java pas game jalan. Jadi file PNG-nya kecil dan tajam.

Output (per frame, transparan, BUKAN sprite sheet — sama kayak slime):
    src/Aset/wolf_<aksi>_<arah>_<nomor>.png
        aksi  : idle (2 frame) | walk (6) | attack (5, frame ke-3 = gigitan) | dead (1)
        arah  : right | left | up | down
        nomor : mulai dari 1
Mau ganti art sendiri? Timpa PNG-nya aja (ukuran tetap 62x34, kaki nempel di baris ke-30), kodenya gak perlu diubah.

Jalankan dari folder 2DGame:
    pip install pillow
    python tools/generate_wolf.py                       # tulis semua PNG ke src/Aset
    python tools/generate_wolf.py --preview out.png     # cuma bikin contact sheet buat dicek mata (gak nulis PNG game)

Mau ganti warna bulu? Edit blok PALETTE lalu jalankan ulang.
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ASET = Path(__file__).resolve().parent.parent / 'src' / 'Aset'

W, H = 62, 34          # HARUS sama dengan WolfSprite.BASE_W / BASE_H
GROUND = 30            # baris terakhir telapak kaki
IDLE_N, WALK_N, ATTACK_N = 2, 6, 5
DIRS = ['right', 'left', 'up', 'down']   # urutan = indeks arah di WolfSprite


# ───────────── PALETTE ─────────────
def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


OUT = rgb('14161c')     # outline luar
FAR = rgb('2b303a')     # kaki / telinga sisi jauh
FD = rgb('3f4654')      # bulu gelap (punggung)
FM = rgb('626b7c')      # bulu utama
FL = rgb('8d97a9')      # sorot
FH = rgb('b6bfce')      # sorot terang
CR = rgb('d3d7de')      # perut / moncong / dada
CD = rgb('a2a9b6')      # bayangan krem
EYE = rgb('ffc43a')
EYH = rgb('fff3ae')
NOSE = rgb('0d0e11')
MOUTH = rgb('8a1b2d')
TONGUE = rgb('d9607a')
TEETH = rgb('f6f6f0')
EARIN = rgb('7d5868')

PAL = {'D': FD, 'F': FM, 'L': FL, 'H': FH, 'C': CR, 'c': CD, 'K': NOSE, 'E': EYE, 'e': EYH, 'M': MOUTH,
       'T': TEETH, 'I': EARIN, 'B': OUT, 'A': FAR, 'S': TONGUE}


# ───────────── kanvas ─────────────
class Layer:
    def __init__(self):
        self.im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def px(self, x, y, c):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < W and 0 <= y < H:
            self.im.putpixel((x, y), c)

    def rect(self, x0, y0, x1, y1, c):
        self.d.rectangle([int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))], fill=c)

    def poly(self, pts, c):
        self.d.polygon([(int(round(x)), int(round(y))) for x, y in pts], fill=c)

    def ell(self, x0, y0, x1, y1, c):
        self.d.ellipse([int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))], fill=c)

    def line(self, p0, p1, w, c):
        n = int(max(abs(p1[0] - p0[0]), abs(p1[1] - p0[1]))) * 2 + 1
        for i in range(n + 1):
            t = i / n
            cx = p0[0] + (p1[0] - p0[0]) * t
            cy = p0[1] + (p1[1] - p0[1]) * t
            a = int(round(cx - (w - 1) / 2.0))
            b = int(round(cy - (w - 1) / 2.0))
            self.d.rectangle([a, b, a + w - 1, b + w - 1], fill=c)

    def bmp(self, rows, x, y, flip=False, pal=None):
        pal = pal or PAL
        for j, row in enumerate(rows):
            r = row[::-1] if flip else row
            for i, ch in enumerate(r):
                if ch != '.' and ch in pal:
                    self.px(x + i, y + j, pal[ch])

    def banded(self, pts, bands):
        """Isi poligon per-kolom: bands = [(batas_t, warna), ...]; t = posisi vertikal 0 (atas) .. 1 (bawah) di kolom itu."""
        m = Image.new('L', (W, H), 0)
        ImageDraw.Draw(m).polygon([(int(round(x)), int(round(y))) for x, y in pts], fill=255)
        for x in range(W):
            ys = [y for y in range(H) if m.getpixel((x, y))]
            if not ys:
                continue
            top, bot = ys[0], ys[-1]
            span = bot - top + 1
            for y in ys:
                t = (y - top + 0.5) / span
                for lim, col in bands:
                    if t <= lim:
                        self.px(x, y, col)
                        break


def bez(p0, p1, p2, t):
    a = (1 - t) ** 2
    b = 2 * (1 - t) * t
    c = t * t
    return (a * p0[0] + b * p1[0] + c * p2[0], a * p0[1] + b * p1[1] + c * p2[1])


# ───────────── post-process ─────────────
def outline(img):
    src = img.copy()
    for y in range(H):
        for x in range(W):
            if src.getpixel((x, y))[3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H and src.getpixel((nx, ny))[3]:
                    img.putpixel((x, y), OUT)
                    break
    return img


def compose(*layers):
    base = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for l in layers:
        base.alpha_composite(l.im)
    return outline(base)


def mirror(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)


# ───────────── POSE ─────────────
class Pose:
    def __init__(self, **kw):
        self.bob = 0          # badan naik(-) / turun(+)
        self.bx = 0           # badan maju(+) / mundur(-)
        self.hdx = 0          # kepala relatif ke badan
        self.hdy = 0
        self.jaw = 0.0        # 0 tutup .. 1 mangap lebar
        self.tail = 0.0       # -1 turun .. +1 naik
        self.ph = None        # fase jalan (radian); None = berdiri
        self.stride = 4.0
        self.reachF = 0       # geser kaki depan (pose serang)
        self.reachH = 0
        self.liftF = 0        # angkat kaki depan (loncat)
        self.liftH = 0
        self.ears_back = False
        self.eye = 'open'     # open | slit
        self.sway = 0         # goyang kiri-kanan (tampak depan / belakang)
        self.__dict__.update(kw)


def leg_pos(p, hip_x, ground, off):
    """Posisi telapak sebuah kaki saat jalan (trot): (x telapak, y telapak)."""
    a = p.ph + off
    return hip_x + math.sin(a) * p.stride, ground - max(0.0, math.cos(a)) * 3.0


# ═════════════════════════ TAMPAK SAMPING (hadap kanan) ═════════════════════════

HEAD_SIDE = [   # kepala hadap kanan, tanpa telinga & rahang bawah. Titik (0,0) = pojok kiri-atas.
    "..DDFFFFLL......",
    ".DFFFFFFFFLL....",
    "DFFFFFFFFFFFLL..",
    "DFFFFFFFFFFFFFFK",
    "DFFFFFFFFFFFFFFK",
    ".DFFFFFFFCCCCCCC",
    ".DFFFFFFCCCCCCC.",
]
JAW_CLOSED = [   # rahang bawah tertutup
    "..DDFFFFCCCCCC..",
    "...DDFFFFFCCC...",
    "....DFFFFFFC....",
    ".....D.DDF.D....",
]


def side_leg(layer, hip, paw, kind, col, sock):
    """Kaki 2 ruas (paha -> betis -> telapak). Kaki belakang menekuk ke depan di lutut, kaki depan sedikit ke belakang."""
    hx, hy = hip
    px, py = paw
    if kind == 'hind':
        kx, ky = (hx + px) / 2 + 3.0, hy + (py - hy) * 0.50
    else:
        kx, ky = (hx + px) / 2 - 1.0, hy + (py - hy) * 0.50
    layer.line((hx, hy), (kx, ky), 4, col)
    layer.line((kx, ky), (px, py - 1), 3, col)
    layer.rect(px - 1, py - 1, px + 2, py, sock)     # "kaos kaki" terang


def side(p):
    far, tail, body, near, head, det = [Layer() for _ in range(6)]
    bx, by = 27 + p.bx, 14 + p.bob
    walking = p.ph is not None

    hip_h, hip_f = bx - 10, bx + 9
    leg_top = by + 4

    def paw(hip_x, off, reach, lift):
        if walking:
            return leg_pos(p, hip_x, GROUND, off)
        return hip_x + reach, GROUND - lift

    # ── ekor: lebat, menggantung ke bawah-belakang, ujungnya terang ──
    root = (bx - 14, by - 4)
    ctrl = (bx - 23, by - 8 - p.tail * 3)
    tip = (bx - 24, by + 6 - p.tail * 15)
    widths = [4, 5, 6, 6, 7, 7, 6, 6, 5, 5, 4, 4, 3, 3]
    pts = [bez(root, ctrl, tip, i / (len(widths) - 1)) for i in range(len(widths))]
    for i, (tx, ty) in enumerate(pts):
        tail.line((tx, ty), (tx, ty), widths[i], FM if i < 10 else FL)
    for i in (2, 4, 6, 8):
        tx, ty = pts[i]
        tail.px(tx, ty - widths[i] // 2 + 1, FD)        # garis gelap di punggung ekor
        tail.px(tx - 1, ty + widths[i] // 2 - 1, FL)    # bulu terang di bawah
    tail.px(pts[-1][0] - 1, pts[-1][1], CR)

    # ── kaki sisi jauh ──
    fh = paw(hip_h, math.pi, p.reachH - 1, p.liftH)
    ff = paw(hip_f, 0.0, p.reachF - 1, p.liftF)
    side_leg(far, (hip_h + 2, leg_top), fh, 'hind', FAR, FD)
    side_leg(far, (hip_f - 2, leg_top), ff, 'front', FAR, FD)

    # ── badan ──
    torso = [(bx - 15, by - 2), (bx - 12, by - 6), (bx - 4, by - 7), (bx + 6, by - 8), (bx + 12, by - 6),
             (bx + 16, by - 1), (bx + 15, by + 6), (bx + 10, by + 8), (bx + 3, by + 4), (bx - 5, by + 4),
             (bx - 12, by + 5), (bx - 16, by + 2)]
    body.banded(torso, [(0.20, FD), (0.66, FM), (1.0, CR)])
    # bulu punggung (sorot) & rusuk (garis gelap)
    for i in range(-12, 12, 3):
        body.px(bx + i, by - 7 + (1 if i < -4 else 0) - (1 if i > 3 else 0), FL)
    for (ox, oy) in ((-8, -1), (-4, 0), (0, 0), (4, -1)):
        body.px(bx + ox, by + oy, FD)
        body.px(bx + ox + 1, by + oy + 1, FD)
    body.ell(bx - 12, by - 3, bx - 6, by + 4, FM)          # paha belakang
    body.px(bx - 11, by - 2, FL)
    body.px(bx - 10, by - 2, FL)
    body.ell(bx + 6, by - 4, bx + 13, by + 4, FM)          # bahu
    body.px(bx + 8, by - 3, FL)
    body.px(bx + 9, by - 3, FL)

    # ── kaki sisi dekat ──
    nh = paw(hip_h, 0.0, p.reachH, p.liftH)
    nf = paw(hip_f, math.pi, p.reachF, p.liftF)
    side_leg(near, (hip_h, leg_top), nh, 'hind', FM, FH)
    side_leg(near, (hip_f, leg_top), nf, 'front', FM, FH)
    # bayangan di bawah perut biar kaki "nempel" ke badan
    for ox in (-10, -9, 9, 10):
        near.px(bx + ox - 1, leg_top, FD)

    # ── leher + dada ──
    hx = bx + 13 + p.hdx
    hy = by - 8 + p.hdy
    neck = [(bx + 5, by - 8), (hx + 3, hy), (hx + 10, hy + 6), (bx + 15, by + 4), (bx + 11, by + 4)]
    head.poly(neck, FM)
    head.poly([(bx + 6, by - 8), (hx + 4, hy + 1), (hx + 8, hy + 3), (bx + 10, by - 2)], FD)     # surai gelap
    for i in range(0, 5):
        head.px(bx + 7 + i * 2, by - 9 + (i // 2) + (1 if i > 3 else 0), FD)                     # surai berduri
    ruff = [(bx + 10, by + 2), (bx + 16, by - 3), (bx + 18, by + 3), (bx + 17, by + 7), (bx + 14, by + 9), (bx + 12, by + 7),
            (bx + 10, by + 8)]
    head.poly(ruff, CR)
    for (ox, oy) in ((17, 3), (16, 6), (14, 9), (12, 7)):
        head.px(bx + ox, by + oy, CD)
    head.px(bx + 13, by + 10, CD)
    head.px(bx + 16, by + 8, CD)

    # ── telinga ──
    if p.ears_back:
        far_ear = [(hx + 6, hy + 1), (hx + 2, hy - 2), (hx + 9, hy + 1)]
        near_ear = [(hx + 2, hy + 2), (hx - 3, hy - 1), (hx + 7, hy + 2)]
    else:
        far_ear = [(hx + 7, hy + 1), (hx + 9, hy - 5), (hx + 11, hy + 1)]
        near_ear = [(hx + 2, hy + 1), (hx + 3, hy - 6), (hx + 8, hy + 1)]
    head.poly(far_ear, FAR)
    head.poly(near_ear, FM)
    if not p.ears_back:
        head.px(hx + 3, hy - 5, FD)
        head.px(hx + 4, hy - 4, FD)
        for (ox, oy) in ((4, -2), (4, -1), (4, 0), (5, 0)):
            head.px(hx + ox, hy + oy, EARIN)
    else:
        head.px(hx + 3, hy, EARIN)

    # ── kepala ──
    head.bmp(HEAD_SIDE, hx, hy)
    ex, ey = hx + 9, hy + 3
    if p.eye == 'open':
        head.px(ex, ey, EYE)
        head.px(ex + 1, ey, EYH)
        head.px(ex - 1, ey - 1, OUT)
        head.px(ex, ey - 1, OUT)
        head.px(ex + 1, ey - 1, FD)
    else:
        head.px(ex, ey, EYE)
        head.px(ex + 1, ey, EYE)
        head.px(ex - 1, ey - 1, OUT)
        head.px(ex, ey - 1, OUT)
        head.px(ex + 1, ey - 1, OUT)
    head.px(hx + 15, hy + 3, NOSE)
    head.px(hx + 15, hy + 4, NOSE)
    head.px(hx + 14, hy + 3, NOSE)
    # rahang bawah
    j = int(round(p.jaw * 5))
    if j == 0:
        head.bmp(JAW_CLOSED, hx, hy + 7)
        for i in range(9, 15):
            head.px(hx + i, hy + 6, CD if i < 14 else FD)
        head.px(hx + 12, hy + 6, FD)
    else:
        tipy = hy + 6 + int(round(j * 1.6))
        # interior mulut (merah) = baji antara bibir atas dan rahang bawah
        head.poly([(hx + 7, hy + 6), (hx + 15, hy + 5), (hx + 15, tipy), (hx + 8, hy + 8)], MOUTH)
        head.poly([(hx + 9, hy + 5), (hx + 15, hy + 5), (hx + 15, hy + 6)], CR)
        # rahang bawah: baji krem yang engselnya di pipi
        head.poly([(hx + 4, hy + 7), (hx + 8, hy + 8), (hx + 15, tipy), (hx + 15, tipy + 3), (hx + 8, tipy + 2),
                   (hx + 3, hy + 10)], CR)
        head.poly([(hx + 3, hy + 7), (hx + 7, hy + 8), (hx + 6, hy + 11), (hx + 2, hy + 10)], FM)
        for ox in (10, 12, 14):                                  # taring atas
            head.px(hx + ox, hy + 6, TEETH)
        head.px(hx + 13, hy + 7, TEETH)
        head.px(hx + 14, tipy - 1, TEETH)                        # taring bawah
        head.px(hx + 11, tipy - 2 if tipy - 2 > hy + 8 else hy + 8, TEETH)
        if j >= 3:
            head.rect(hx + 10, hy + 8 + (tipy - hy - 8) // 2, hx + 13, hy + 9 + (tipy - hy - 8) // 2, TONGUE)
    return compose(far, tail, body, near, head, det)


def lying_side():
    """Serigala roboh miring (dipakai semua arah)."""
    far, tail, body, near, head, det = [Layer() for _ in range(6)]
    for i, (tx, ty) in enumerate([bez((12, 26), (6, 27), (2, 29), t / 9) for t in range(10)]):
        tail.line((tx, ty), (tx, ty), 5 if i < 6 else 4, FM if i < 8 else FL)
    far.line((16, 25), (9, 20), 3, FAR)
    far.line((30, 24), (37, 18), 3, FAR)
    body.banded([(10, 23), (14, 19), (26, 18), (34, 20), (36, 27), (30, 30), (15, 30), (9, 28)],
                [(0.22, FD), (0.7, FM), (1.0, CR)])
    for (a, b) in ((16, 22), (20, 23), (24, 22)):
        body.px(a, b, FD)
        body.px(a + 1, b + 1, FD)
    near.line((20, 27), (14, 22), 3, FM)
    near.line((28, 27), (34, 21), 3, FM)
    near.rect(12, 20, 14, 21, FH)
    near.rect(33, 19, 35, 20, FH)
    near.rect(8, 18, 10, 19, FL)
    near.rect(36, 16, 38, 17, FL)
    head.poly([(33, 22), (36, 17), (39, 22)], FD)
    head.ell(33, 21, 44, 30, FM)
    head.poly([(41, 25), (52, 27), (52, 30), (41, 30)], CR)
    for (a, b) in ((52, 27), (51, 27), (51, 28)):
        head.px(a, b, NOSE)
    head.rect(46, 30, 49, 30, TONGUE)
    for (a, b) in ((38, 24), (40, 24), (39, 25), (38, 26), (40, 26)):
        head.px(a, b, NOSE)
    return compose(far, tail, body, near, head, det)


# ═════════════════════════ TAMPAK DEPAN (hadap penonton) ═════════════════════════

HEAD_FRONT_L = [   # separuh kiri kepala depan (9 kolom); sisi kanan = cermin. Lebar penuh 18, tinggi 15.
    ".....DFFF",
    "...DFFFFF",
    "..DFFFFFD",
    ".DFFFFFFD",
    ".DFFFFFFF",
    "DDFBBBFFF",
    ".DFFEeFFF",
    "DLFFFFFCC",
    "DDLFFFCCC",
    ".DDLFCCCC",
    "..DDLCCCC",
    "...DDCCCC",
    "....DCCCK",
    ".....CCCc",
    "......DDC",
]
HEAD_FRONT = [r + r[::-1] for r in HEAD_FRONT_L]


def front(p):
    far, tail, body, near, head, det = [Layer() for _ in range(6)]
    cx = 31 + p.sway
    bob = p.bob
    walking = p.ph is not None

    # kaki belakang: cuma telapak + sedikit betis yang mengintip di samping kaki depan
    for sx in (-1, 1):
        x = cx + sx * 8
        far.rect(x - 1, 24 + bob, x + 1, 29, FAR)
        far.rect(x - 2, 28, x + 2, 29, FD)

    # ekor mengintip di samping pinggul
    t = p.tail
    far.line((cx + 8, 20 + bob), (cx + 12, 18 + bob - t * 2), 3, FD)
    far.line((cx + 12, 18 + bob - t * 2), (cx + 13, 14 + bob - t * 4), 3, FM)

    # badan: ramping, bahu sedikit lebih lebar dari dada
    body.banded([(cx - 8, 12 + bob), (cx - 5, 9 + bob), (cx + 5, 9 + bob), (cx + 8, 12 + bob), (cx + 8, 20 + bob),
                 (cx + 6, 24 + bob), (cx - 6, 24 + bob), (cx - 8, 20 + bob)],
                [(0.30, FD), (1.0, FM)])
    # dada putih berbulu (berhenti di atas kaki, bawahnya bergerigi)
    body.poly([(cx - 5, 14 + bob), (cx + 5, 14 + bob), (cx + 5, 20 + bob), (cx + 3, 23 + bob), (cx + 1, 21 + bob),
               (cx, 24 + bob), (cx - 1, 21 + bob), (cx - 3, 23 + bob), (cx - 5, 20 + bob)], CR)
    for ox, oy in ((-4, 19), (-2, 21), (0, 19), (2, 21), (4, 19), (-3, 16), (3, 16)):
        body.px(cx + ox, oy + bob, CD)

    # kaki depan: panjang & ramping, ada celah di antaranya
    for sx, off in ((-1, 0.0), (1, math.pi)):
        lift = 0.0
        if walking:
            lift = max(0.0, math.cos(p.ph + off)) * 3.0
        lx = cx + sx * 5
        bot = GROUND - lift
        near.rect(lx - 1, 19 + bob, lx + 1, bot, FM)
        near.px(lx - 1 + (1 if sx > 0 else 0), 22 + bob, FL)
        near.px(lx - 1 + (1 if sx > 0 else 0), 25 + bob, FL)
        near.px(lx + (0 if sx > 0 else -1) + 1 if sx < 0 else lx - 1, 27 + bob, FD)
        near.rect(lx - 2, bot - 1, lx + 2, bot, FH)          # telapak terang

    # telinga (di belakang kepala)
    hx = cx - 9 + p.hdx
    hy = 5 + p.hdy + bob
    if p.ears_back:
        head.poly([(hx + 1, hy + 4), (hx - 3, hy + 1), (hx + 6, hy + 1)], FM)
        head.poly([(hx + 17, hy + 4), (hx + 21, hy + 1), (hx + 12, hy + 1)], FM)
    else:
        head.poly([(hx + 1, hy + 4), (hx + 2, hy - 5), (hx + 8, hy + 1)], FM)
        head.poly([(hx + 17, hy + 4), (hx + 16, hy - 5), (hx + 10, hy + 1)], FM)
        head.poly([(hx + 3, hy + 2), (hx + 3, hy - 2), (hx + 6, hy + 1)], EARIN)
        head.poly([(hx + 15, hy + 2), (hx + 15, hy - 2), (hx + 12, hy + 1)], EARIN)
        head.px(hx + 2, hy - 4, FD)
        head.px(hx + 16, hy - 4, FD)
        head.px(hx + 2, hy - 3, FD)
        head.px(hx + 16, hy - 3, FD)
    head.bmp(HEAD_FRONT, hx, hy)
    if p.eye == 'slit':       # menyipit: mata cuma garis kuning
        for ox in (4, 5, 12, 13):
            head.px(hx + ox, hy + 6, EYE)
    j = int(round(p.jaw * 5))
    if j:
        head.rect(hx + 6, hy + 12, hx + 11, hy + 12 + j, MOUTH)
        head.rect(hx + 7, hy + 12 + j, hx + 10, hy + 13 + j, CR)
        for ox in (6, 11):
            head.px(hx + ox, hy + 12, TEETH)
            head.px(hx + ox, hy + 11 + j, TEETH)
        head.rect(hx + 8, hy + 11 + j, hx + 9, hy + 12 + j, TONGUE)
    return compose(far, tail, body, near, head, det)


# ═════════════════════════ TAMPAK BELAKANG (membelakangi penonton) ═════════════════════════

def back(p):
    far, head, body, near, tail = [Layer() for _ in range(5)]
    cx = 31 + p.sway
    bob = p.bob
    walking = p.ph is not None

    # kaki depan mengintip di samping badan
    for sx in (-1, 1):
        x = cx + sx * 8
        far.rect(x - 1, 15 + bob, x + 1, 27, FAR)
        far.rect(x - 2, 26, x + 2, 27, FD)

    # kepala + telinga (jauh di depan => cuma atasnya yang kelihatan di atas pundak)
    hx = cx - 6 + p.hdx
    hy = 0 + p.hdy + bob
    if p.ears_back:
        head.poly([(hx, hy + 6), (hx - 1, hy + 1), (hx + 5, hy + 3)], FM)
        head.poly([(hx + 12, hy + 6), (hx + 13, hy + 1), (hx + 7, hy + 3)], FM)
    else:
        head.poly([(hx, hy + 6), (hx + 1, hy), (hx + 6, hy + 4)], FM)
        head.poly([(hx + 12, hy + 6), (hx + 11, hy), (hx + 6, hy + 4)], FM)
        head.px(hx + 1, hy + 1, FD)
        head.px(hx + 11, hy + 1, FD)
    head.ell(hx, hy + 4, hx + 12, hy + 14, FM)
    head.rect(hx + 5, hy + 4, hx + 7, hy + 9, FD)
    for ox in (2, 3, 9, 10):
        head.px(hx + ox, hy + 7, FL)

    # badan: punggung + pantat (lebih pendek, biar kaki belakang kelihatan panjang)
    body.banded([(cx - 10, 16 + bob), (cx - 8, 10 + bob), (cx - 4, 8 + bob), (cx + 4, 8 + bob), (cx + 8, 10 + bob),
                 (cx + 10, 16 + bob), (cx + 9, 23 + bob), (cx - 9, 23 + bob)],
                [(0.2, FL), (0.55, FD), (1.0, FM)])
    body.ell(cx - 9, 14 + bob, cx - 2, 23 + bob, FM)             # paha kiri
    body.ell(cx + 2, 14 + bob, cx + 9, 23 + bob, FM)             # paha kanan
    for ox in (-7, -6, 5, 6):
        body.px(cx + ox, 16 + bob, FL)
    for oy in (11, 13, 15):                                       # garis punggung
        body.px(cx, oy + bob, FD)
    for ox in (-6, -3, 0, 3, 6):                                  # surai
        body.px(cx + ox, 8 + bob + (abs(ox) // 5), FH)

    # kaki belakang: panjang, ada tonjolan hock, telapak terang
    for sx, off in ((-1, 0.0), (1, math.pi)):
        lift = 0.0
        if walking:
            lift = max(0.0, math.cos(p.ph + off)) * 3.0
        lx = cx + sx * 6
        bot = GROUND - lift
        near.rect(lx - 1, 22 + bob, lx + 1, bot, FM)
        near.rect(lx - 2 if sx < 0 else lx + 2, 22 + bob, lx - 2 if sx < 0 else lx + 2, 25 + bob, FM)   # hock menonjol keluar
        near.px(lx - 1, 24 + bob, FL)
        near.rect(lx - 2, bot - 1, lx + 2, bot, FH)

    # ekor menjuntai (paling dekat penonton) — diberi outline sendiri biar kebaca di atas pantat
    t = p.tail
    sw = int(round(p.sway)) + (1 if t > 0.6 else 0)
    tail_pts = [(cx, 17 + bob), (cx, 20 + bob), (cx + sw, 23 + bob - int(t * 2)), (cx + sw, 26 + bob - int(t * 3))]
    for i, (tx, ty) in enumerate(tail_pts):
        tail.line((tx, ty), (tx, ty), 7 if i < 3 else 6, OUT)
    for i, (tx, ty) in enumerate(tail_pts):
        tail.line((tx, ty), (tx, ty), 5 if i < 3 else 4, FD if i < 3 else FL)
    for oy in (19, 21, 23):
        tail.px(cx - 1, oy + bob, FM)
    tail.px(cx + 1, 22 + bob, FM)
    return compose(far, head, body, near, tail)


# ═════════════════════════ ANIMASI ═════════════════════════

def idle_pose(i):
    p = Pose()
    p.tail = (0.2, -0.3)[i]
    p.hdy = (0, 1)[i]
    p.ears_back = False
    return p


def walk_pose(f):
    ph = f / WALK_N * math.pi * 2
    p = Pose(ph=ph)
    p.bob = -1 if f % 3 == 0 else 0
    p.hdy = 1 if f % 3 == 0 else 0
    p.tail = math.sin(ph) * 0.5
    p.stride = 4.0
    return p


def attack_poses():
    a0 = Pose(bob=2, hdx=-1, hdy=2, tail=0.8, reachH=-1, ears_back=True, eye='slit', jaw=0.0)
    a1 = Pose(bob=3, bx=-1, hdx=-3, hdy=3, jaw=0.25, tail=1.0, reachH=-2, reachF=-1, ears_back=True, eye='slit')
    a2 = Pose(bob=-1, bx=2, hdx=3, hdy=2, jaw=1.0, tail=1.0, reachH=-5, reachF=4, liftH=2, liftF=3, ears_back=True, eye='slit')
    a3 = Pose(bx=2, hdx=2, hdy=4, jaw=0.5, tail=0.5, reachH=-3, reachF=3, ears_back=True, eye='slit')
    a4 = Pose(bob=1, tail=0.2)
    return [a0, a1, a2, a3, a4]


def adapt_attack_for_front(p, i):
    q = Pose(**p.__dict__)
    q.bx = 0
    # loncat ke arah kamera: kepala + dada turun & maju, mulut mangap
    q.bob = (1, 2, -1, 0, 0)[i]
    q.hdy = (0, 2, 4, 5, 1)[i] + 0
    q.hdx = 0
    q.jaw = (0.0, 0.2, 1.0, 0.5, 0.0)[i]
    q.sway = 0
    q.tail = (0.5, 1.0, 1.0, 0.6, 0.2)[i]
    q.ears_back = i in (0, 1, 2, 3)
    q.eye = 'slit' if i in (0, 1, 2, 3) else 'open'
    return q


def adapt_attack_for_back(p, i):
    q = Pose(**p.__dict__)
    q.bx = 0
    q.bob = (1, 2, -2, -1, 0)[i]
    q.hdy = (1, 2, -1, 0, 0)[i]
    q.hdx = 0
    q.tail = (0.5, 1.0, 1.0, 0.6, 0.2)[i]
    q.ears_back = i in (0, 1, 2, 3)
    q.sway = 0
    return q


def pose_for(view, action, n):
    if action == 'idle':
        p = idle_pose(n)
        if view in ('up', 'down'):
            p.sway = 0
            p.bob = (0, 0)[n]
        return p
    if action == 'walk':
        p = walk_pose(n)
        if view in ('up', 'down'):
            p.sway = (1, 1, 0, -1, -1, 0)[n]
            p.stride = 0
        return p
    ps = attack_poses()
    if view == 'down':
        return adapt_attack_for_front(ps[n], n)
    if view == 'up':
        return adapt_attack_for_back(ps[n], n)
    return ps[n]


def render(view, action, n):
    if action == 'dead':
        img = lying_side()
        return mirror(img) if view in ('left', 'up') else img
    p = pose_for(view, action, n)
    if view == 'right':
        return side(p)
    if view == 'left':
        return mirror(side(p))
    if view == 'down':
        return front(p)
    return back(p)


COUNTS = {'idle': IDLE_N, 'walk': WALK_N, 'attack': ATTACK_N, 'dead': 1}


def main():
    preview = '--preview' in sys.argv
    sheet_cells = {}
    count = 0
    if not preview:
        ASET.mkdir(parents=True, exist_ok=True)
    for action, total in COUNTS.items():
        for view in DIRS:
            for n in range(total):
                img = render(view, action, n)
                sheet_cells[(action, view, n)] = img
                if not preview:
                    img.save(ASET / f'wolf_{action}_{view}_{n + 1}.png')
                    count += 1
    if preview:
        out = Path(sys.argv[sys.argv.index('--preview') + 1])
        scale = 5
        rows = [(a, v) for a in COUNTS for v in DIRS]
        cols = max(COUNTS.values())
        cw, ch = W * scale + 6, H * scale + 6
        sheet = Image.new('RGBA', (cols * cw, len(rows) * ch), (38, 60, 52, 255))
        for r, (a, v) in enumerate(rows):
            for n in range(COUNTS[a]):
                im = sheet_cells[(a, v, n)].resize((W * scale, H * scale), Image.NEAREST)
                sheet.alpha_composite(im, (n * cw + 3, r * ch + 3))
        sheet.save(out)
        print('preview ->', out)
    else:
        print(f'{count} frame wolf ditulis ke {ASET}')


if __name__ == '__main__':
    main()
