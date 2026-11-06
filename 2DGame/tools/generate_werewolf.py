#!/usr/bin/env python3
"""
Generator sprite "Werewolf" (manusia serigala daratan es) — pixel-art 4 arah (kanan, kiri, atas, bawah).

Semua frame digambar langsung di resolusi aslinya (72x80 piksel, tanpa anti-alias), lalu diperbesar pakai
nearest-neighbor oleh Entitas/WerewolfSprite.java pas game jalan. Skala 1.5x (tileSize/32), sama kayak sprite player
(64x64 digambar 96x96), jadi ukuran "piksel"-nya serasi.

Output: SATU sprite sheet  src/Aset/werewolf_sheet.png  (1080 x 320)
    baris (atas -> bawah) : right, left, up, down
    kolom (kiri -> kanan) : idle x2 | walk x6 | attack x6 (frame ke-3 & 4 = cakaran) | dead x1   -> 15 kolom
    tiap sel              : 72 x 80, telapak kaki nempel di baris ke-74 (GROUND)

Jalankan dari folder 2DGame:
    pip install pillow numpy
    python tools/generate_werewolf.py                      # tulis src/Aset/werewolf_sheet.png
    python tools/generate_werewolf.py --preview out.png    # contact sheet diperbesar 3x buat dicek mata (gak nulis sheet game)

Mau ganti warna bulu? Edit blok PALETTE lalu jalankan ulang.
Mau pakai art sendiri? Timpa werewolf_sheet.png dengan grid yang sama (15 kolom x 4 baris, sel 72x80).
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ASET = Path(__file__).resolve().parent.parent / 'src' / 'Aset'

W, H = 72, 80          # HARUS sama dengan WerewolfSprite.CELL_W / CELL_H
GROUND = 74            # baris terakhir telapak kaki
CX = 36                # sumbu tengah badan
IDLE_N, WALK_N, ATTACK_N, DEAD_N = 2, 6, 6, 1
COLS = IDLE_N + WALK_N + ATTACK_N + DEAD_N          # 15
DIRS = ['right', 'left', 'up', 'down']              # urutan baris = indeks arah di WerewolfSprite


# ───────────── PALETTE ─────────────
def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


OUT = rgb('0b0d16')                      # outline luar
EDGE = rgb('1b2034')                     # garis tepi antar-bagian (lengan di depan badan)
FUR = (rgb('4b5679'), rgb('76839f'), rgb('2a3049'))    # (utama, sorot, bayangan) bulu biru-abu
FUR_BACK = (rgb('3c4666'), rgb('5f6b8c'), rgb('222739'))  # sisi jauh / punggung, lebih gelap
SNOUT = (rgb('68739a'), rgb('8f9bbd'), rgb('3b4262'))
CHEST = (rgb('5d6a90'), rgb('8794b6'), rgb('353c5a'))
CLOTH = (rgb('6e4b33'), rgb('93694a'), rgb('432c1d'))
EAR_IN = rgb('7a4d63')
EYE = rgb('ff2e2e')
EYE_HI = rgb('ffb9a6')
EYE_GLOW = rgb('8a1522')
NOSE = rgb('0c0d12')
MOUTH = rgb('6e0f1d')
TONGUE = rgb('d1506b')
TEETH = rgb('f4f0e4')
CLAW = rgb('efe7cf')
CLAW_D = rgb('b3a98d')
BELT = rgb('3a2517')
SLASH = rgb('e8eefc')
SLASH_D = rgb('9fb0dc')


# ───────────── util ─────────────
def hn(x, y, s=0):
    """angka acak deterministik 0..1 per piksel (supaya bulu gak berkedip antar render)."""
    h = (x * 374761393 + y * 668265263 + s * 2147483647 + 1442695040888963407) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    h ^= h >> 16
    return (h & 0xFFFF) / 65535.0


def sh(M, dx, dy):
    """S[y,x] = M[y-dy, x-dx] (geser, sisi kosong = False)."""
    S = np.zeros_like(M)
    ys = slice(max(0, dy), H + min(0, dy))
    xs = slice(max(0, dx), W + min(0, dx))
    yd = slice(max(0, -dy), H + min(0, -dy))
    xd = slice(max(0, -dx), W + min(0, -dx))
    S[ys, xs] = M[yd, xd]
    return S


def lerp(a, b, t):
    return a + (b - a) * t


def ik(sh_, hd, l1, l2, sign):
    """Dua tulang (bahu->siku->tangan). Kembalikan posisi siku. sign = arah tekuk siku (+1/-1)."""
    dx, dy = hd[0] - sh_[0], hd[1] - sh_[1]
    d = math.hypot(dx, dy)
    d = max(d, 0.001)
    dmax = (l1 + l2) * 0.985
    if d > dmax:
        k = dmax / d
        dx, dy, d = dx * k, dy * k, dmax
    ux, uy = dx / d, dy / d
    a = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
    h = math.sqrt(max(0.0, l1 * l1 - a * a))
    px, py = -uy, ux
    return (sh_[0] + ux * a + sign * px * h, sh_[1] + uy * a + sign * py * h), (sh_[0] + dx, sh_[1] + dy)


class Mask:
    def __init__(self):
        self.im = Image.new('L', (W, H), 0)
        self.d = ImageDraw.Draw(self.im)

    @staticmethod
    def _p(pts):
        return [(int(round(x)), int(round(y))) for x, y in pts]

    def poly(self, pts):
        self.d.polygon(self._p(pts), fill=255)
        return self

    def ell(self, x0, y0, x1, y1):
        self.d.ellipse([int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))], fill=255)
        return self

    def circ(self, cx, cy, r):
        return self.ell(cx - r, cy - r, cx + r, cy + r)

    def limb(self, p0, p1, w0, w1):
        """kapsul meruncing dari p0 (tebal w0) ke p1 (tebal w1)."""
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        d = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / d, dx / d
        quad = [(p0[0] + nx * w0 / 2, p0[1] + ny * w0 / 2), (p1[0] + nx * w1 / 2, p1[1] + ny * w1 / 2),
                (p1[0] - nx * w1 / 2, p1[1] - ny * w1 / 2), (p0[0] - nx * w0 / 2, p0[1] - ny * w0 / 2)]
        self.poly(quad)
        self.circ(p0[0], p0[1], w0 / 2.0)
        self.circ(p1[0], p1[1], w1 / 2.0)
        return self

    def arr(self):
        return np.array(self.im) > 0


class Fig:
    """Kanvas satu frame: tumpuk bagian badan dari belakang ke depan, tiap bagian di-shade sendiri."""

    def __init__(self):
        self.img = np.zeros((H, W, 4), np.uint8)
        self.fur = np.zeros((H, W), bool)
        self.top = []          # gambar langsung di atas hasil shading (mata, taring, cakar, slash)

    def part(self, mask, pal, fur=True, seed=0, strands=True, edge=False):
        M = mask.arr() if isinstance(mask, Mask) else mask
        base, light, dark = pal
        up, left = sh(M, 0, 1), sh(M, 1, 0)
        lite = M & (~up | ~left)
        dn2, rt2 = sh(M, 0, -2), sh(M, -2, 0)
        dk = M & (~dn2 | ~rt2)
        col = np.zeros((H, W, 4), np.uint8)
        yy, xx = np.mgrid[0:H, 0:W]
        for y, x in zip(*np.nonzero(M)):
            c = base
            n = hn(int(x), int(y), seed + 3)
            if strands and ((int(x) + 2 * int(y)) % 6 == 0) and n < 0.6:
                c = dark
            elif n < 0.07:
                c = dark
            elif n > 0.94:
                c = light
            if dk[y, x]:
                c = dark
            if lite[y, x] and not dk[y, x]:
                c = light
            col[y, x] = c
        if edge:                      # garis tepi gelap: bagian ini kebaca terpisah dari badan di belakangnya
            rim = M & (~sh(M, 0, 1) | ~sh(M, 1, 0) | ~sh(M, 0, -1) | ~sh(M, -1, 0))
            col[rim] = EDGE
        self.img[M] = col[M]
        if fur:
            self.fur |= M

    def px(self, x, y, c):
        self.top.append((int(round(x)), int(round(y)), c))

    def line(self, p0, p1, c, w=1):
        n = int(max(abs(p1[0] - p0[0]), abs(p1[1] - p0[1]))) * 2 + 1
        for i in range(n + 1):
            t = i / n
            cx, cy = lerp(p0[0], p1[0], t), lerp(p0[1], p1[1], t)
            for ox in range(w):
                for oy in range(w):
                    self.px(cx + ox, cy + oy, c)

    def finish(self, tufts=True):
        img = self.img
        A = img[:, :, 3] > 0
        if tufts:
            near = np.zeros_like(A)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                near |= sh(self.fur & A, dx, dy)
            for y, x in zip(*np.nonzero(near & ~A)):
                if y < GROUND - 5 and hn(int(x), int(y), 7) < 0.20:
                    img[y, x] = FUR[2]
        for x, y, c in self.top:
            if 0 <= x < W and 0 <= y < H:
                img[y, x] = c
        A = img[:, :, 3] > 0
        edge = np.zeros_like(A)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            edge |= sh(A, dx, dy)
        edge &= ~A
        img[edge] = OUT
        return Image.fromarray(img, 'RGBA')


# ───────────── bagian badan yang dipakai ulang ─────────────
def claws(fig, hand, along, n=3, length=4, spread=2.2, col=CLAW):
    """cakar keluar dari tangan; along = arah (dx,dy) dari lengan bawah."""
    d = math.hypot(*along) or 1.0
    ux, uy = along[0] / d, along[1] / d
    px, py = -uy, ux
    for i in range(n):
        off = (i - (n - 1) / 2.0) * spread
        sx, sy = hand[0] + px * off, hand[1] + py * off
        ex, ey = sx + ux * length + px * off * 0.25, sy + uy * length + py * off * 0.25
        fig.line((sx, sy), (ex, ey), col)
        fig.px(ex, ey, CLAW_D)


def fur_arm(fig, shoulder, hand, sign, pal, w=(8, 6, 6), l1=12.5, l2=12.5, edge=False):
    elbow, hand = ik(shoulder, hand, l1, l2, sign)
    m = Mask()
    m.limb(shoulder, elbow, w[0], w[1]).limb(elbow, hand, w[1], w[2]).circ(hand[0], hand[1], w[2] / 2.0 + 0.7)
    fig.part(m, pal, seed=11, edge=edge)
    return elbow, hand


def paw(fig, foot, face, pal, big=True):
    """telapak + 3 cakar. face: 'down' | 'up' | 'right' | 'left'."""
    fx, fy = foot
    m = Mask()
    if face in ('down', 'up'):
        m.ell(fx - 5, fy - 3, fx + 5, fy + 1.5)
    elif face == 'right':
        m.ell(fx - 3, fy - 3, fx + 7, fy + 1.5)
    else:
        m.ell(fx - 7, fy - 3, fx + 3, fy + 1.5)
    fig.part(m, pal, seed=21, strands=False)
    if face == 'down':
        for i, ox in enumerate((-3, 0, 3)):
            fig.px(fx + ox, fy + 2, CLAW)
            fig.px(fx + ox, fy + 3, CLAW_D)
    elif face == 'right':
        for i, oy in enumerate((-2, -1, 0)):
            fig.px(fx + 7, fy + oy + 1, CLAW)
            fig.px(fx + 8, fy + oy + 1, CLAW_D)
    elif face == 'left':
        for i, oy in enumerate((-2, -1, 0)):
            fig.px(fx - 7, fy + oy + 1, CLAW)
            fig.px(fx - 8, fy + oy + 1, CLAW_D)


def loincloth(fig, x0, x1, y0, y1, flare=1, seed=5):
    """cawat cokelat dengan ujung bawah compang-camping."""
    m = Mask()
    pts = [(x0, y0), (x1, y0), (x1 + flare, y1 - 1)]
    n = int(x1 - x0 + 2 * flare)
    for i in range(n + 1):
        xx = x1 + flare - i
        yy = y1 + (1 if (i % 3 == 0) else (-1 if (i % 3 == 1) else 0))
        pts.append((xx, yy))
    pts.append((x0 - flare, y1 - 1))
    m.poly(pts)
    fig.part(m, CLOTH, fur=False, seed=seed, strands=False)
    fig.line((x0, y0), (x1, y0), BELT, 2)
    fig.px(CX if x1 - x0 > 8 else (x0 + x1) / 2, y0 + 1, CLAW_D)


# ───────────── pose ─────────────
class Pose:
    def __init__(self, **kw):
        self.ub = 0                  # geser vertikal badan atas (napas / langkah)
        self.lean = 0                # condong ke depan (px, tampak samping)
        self.fl = (0, 0)             # kaki kiri/dekat: (geser x, angkat)
        self.fr = (0, 0)             # kaki kanan/jauh
        self.hl = (0, 0)             # tangan kiri/dekat: offset dari posisi diam
        self.hr = (0, 0)
        self.jaw = 0                 # 0 tutup .. 3 mangap lebar
        self.slash = 0               # 0 gak ada, 1 = busur cakaran awal, 2 = busur lebar
        self.armspecial = None       # 'L' / 'R' = lengan yang menyerang (override hl / hr jadi posisi absolut)
        self.hlabs = None
        self.hrabs = None
        self.__dict__.update(kw)


def idle_pose(i):
    return Pose(ub=-i, hl=(0, i), hr=(0, i), jaw=i)


def walk_pose(f):
    ph = 2 * math.pi * f / WALK_N
    sA, sB = math.sin(ph), math.sin(ph + math.pi)
    cA, cB = math.cos(ph), math.cos(ph + math.pi)
    return Pose(ub=int(round(1.2 * abs(sA))),
                fl=(6.0 * sA, 4.0 * max(0.0, cA)), fr=(6.0 * sB, 4.0 * max(0.0, cB)),
                hl=(-5.0 * sA, -2.0 * max(0.0, -cA)), hr=(-5.0 * sB, -2.0 * max(0.0, -cB)),
                jaw=0)


# Serangan: tangan penyerang (absolut relatif ke bahu), condong badan, mulut, busur cakar
ATTACK_SEQ = [
    # (dx, dy dari bahu, lean, jaw, slash)
    ((-4, -13), -1, 1, 0),
    ((-9, -17), -2, 2, 0),
    ((13, -5), 3, 3, 1),
    ((21, 10), 4, 3, 2),
    ((14, 19), 2, 2, 0),
    ((5, 23), 0, 1, 0),
]


def attack_pose(i):
    (hx, hy), lean, jaw, sl = ATTACK_SEQ[i]
    return Pose(lean=lean, jaw=jaw, slash=sl, hlabs=(hx, hy), ub=1 if i in (2, 3) else 0)


# ───────────── tampak SAMPING (menghadap kanan) ─────────────
def side(p):
    f = Fig()
    ub, ln = p.ub, p.lean
    ox = -5                                  # geser ke kiri: ruang buat moncong & cakaran ke depan
    hip = (33 + ox + ln * 0.3, 52 + ub * 0.5)
    sho = (41 + ox + ln, 31 + ub)
    head = (51 + ox + ln, 19 + ub + (1 if p.jaw >= 3 else 0))

    # ekor (paling belakang)
    tail = Mask()
    tail.limb((hip[0] - 4, hip[1] - 2), (21 + ox, 57 + ub * 0.3), 6, 7).limb((21 + ox, 57 + ub * 0.3), (13 + ox, 64), 7, 5)
    f.part(tail, FUR_BACK, seed=31)

    # kaki jauh (gelap) — digitigrade: lutut ke depan, tumit ke belakang, telapak ke depan
    def leg(hx, hy, foot_dx, lift, pal, seed):
        fx = hx + 4 + foot_dx
        fy = GROUND - lift
        ankle_t = (fx - 4, fy - 5)
        knee, ankle = ik((hx, hy), ankle_t, 13, 12, -1)
        m = Mask()
        m.limb((hx, hy), knee, 11, 8).limb(knee, ankle, 8, 5).limb(ankle, (fx, fy - 1), 5, 6)
        f.part(m, pal, seed=seed)
        paw(f, (fx + 1, fy), 'right', pal)
    leg(hip[0] - 1, hip[1], p.fr[0], p.fr[1], FUR_BACK, 41)

    # lengan jauh
    far_hand = (sho[0] - 2 + 3 + p.hr[0], sho[1] + 24 + p.hr[1])
    far_el, far_h = ik((sho[0] - 3, sho[1] + 1), far_hand, 12.5, 12.5, 1)
    m = Mask()
    m.limb((sho[0] - 3, sho[1] + 1), far_el, 7, 6).limb(far_el, far_h, 6, 5).circ(far_h[0], far_h[1], 3)
    f.part(m, FUR_BACK, seed=12)
    claws(f, (far_h[0], far_h[1]), (1, 3), 3, 4, 1.8, CLAW_D)

    # badan: punuk punggung membungkuk, dada depan
    torso = Mask()
    torso.poly([(hip[0] - 9, hip[1] - 14), (hip[0] - 5, hip[1] - 25), (sho[0] - 2, sho[1] - 7), (sho[0] + 6, sho[1] - 5),
                (sho[0] + 10, sho[1] + 3), (sho[0] + 8, sho[1] + 13), (hip[0] + 7, hip[1] + 1), (hip[0] - 7, hip[1] + 2)])
    f.part(torso, FUR, seed=51)
    chest = Mask()
    chest.poly([(sho[0] + 1, sho[1] + 1), (sho[0] + 9, sho[1] + 4), (sho[0] + 7, sho[1] + 12), (hip[0] + 5, hip[1] - 4),
                (hip[0] - 1, hip[1] - 6)])
    f.part(chest, CHEST, seed=52, strands=False)

    # duri bulu di punggung
    for k in range(6):
        t = k / 5.0
        bx = lerp(hip[0] - 7, sho[0] + 1, t)
        by = lerp(hip[1] - 14, sho[1] - 8, t) - 1
        f.line((bx, by), (bx - 2, by - 3), FUR[2])

    # kaki dekat
    leg(hip[0] + 3, hip[1], p.fl[0], p.fl[1], FUR, 42)

    loincloth(f, hip[0] - 1, hip[0] + 11, hip[1] - 1, hip[1] + 11, 1, 61)

    # kepala (depan), telinga, moncong, rahang
    far_ear = Mask().poly([(head[0] - 2, head[1] - 7), (head[0] - 6, head[1] - 17), (head[0] + 2, head[1] - 9)])
    f.part(far_ear, FUR_BACK, seed=71, strands=False)
    skull = Mask().ell(head[0] - 8, head[1] - 8, head[0] + 7, head[1] + 6)
    f.part(skull, FUR, seed=72)
    mane = Mask().poly([(head[0] - 9, head[1] - 2), (head[0] - 5, head[1] + 7), (head[0] + 3, head[1] + 9), (sho[0] + 3, sho[1] + 2),
                        (sho[0] - 3, sho[1] - 6)])
    f.part(mane, FUR_BACK, seed=73)
    ear = Mask().poly([(head[0] - 5, head[1] - 6), (head[0] - 8, head[1] - 17), (head[0] + 1, head[1] - 8)])
    f.part(ear, FUR, seed=74, strands=False)
    f.px(head[0] - 5, head[1] - 10, EAR_IN)
    f.px(head[0] - 5, head[1] - 11, EAR_IN)
    jawo = {0: 0, 1: 1, 2: 2, 3: 4}[p.jaw]
    lower = Mask().poly([(head[0] + 3, head[1] + 3 + jawo * 0.4), (head[0] + 15, head[1] + 4 + jawo), (head[0] + 13, head[1] + 7 + jawo),
                         (head[0] + 4, head[1] + 8 + jawo * 0.6)])
    f.part(lower, SNOUT, seed=75, strands=False)
    upper = Mask().poly([(head[0] + 2, head[1] - 3), (head[0] + 9, head[1] - 4), (head[0] + 16, head[1] - 1), (head[0] + 16, head[1] + 2),
                         (head[0] + 3, head[1] + 3)])
    f.part(upper, SNOUT, seed=76, strands=False)
    f.px(head[0] + 16, head[1] - 1, NOSE)
    f.px(head[0] + 16, head[1], NOSE)
    f.px(head[0] + 15, head[1] - 2, NOSE)
    # mulut
    if p.jaw >= 1:
        my = head[1] + 3
        for xx in range(int(head[0] + 4), int(head[0] + 15)):
            f.px(xx, my, MOUTH)
        if p.jaw >= 2:
            for xx in range(int(head[0] + 5), int(head[0] + 14)):
                f.px(xx, my + 1, MOUTH)
            f.px(head[0] + 8, my + 2, TONGUE)
            f.px(head[0] + 9, my + 2, TONGUE)
    for ox in (8, 12):
        f.px(head[0] + ox, head[1] + 3 + 1, TEETH)
        f.px(head[0] + ox, head[1] + 4 + 1, TEETH)
    # mata merah menyala + alis
    f.px(head[0] + 5, head[1] - 4, FUR[2])
    f.px(head[0] + 6, head[1] - 4, FUR[2])
    f.px(head[0] + 7, head[1] - 3, FUR[2])
    f.px(head[0] + 5, head[1] - 3, EYE)
    f.px(head[0] + 6, head[1] - 3, EYE)
    f.px(head[0] + 5, head[1] - 2, EYE_GLOW)
    f.px(head[0] + 6, head[1] - 2, EYE_GLOW)
    f.px(head[0] + 5, head[1] - 3, EYE_HI)

    # lengan dekat (menyerang atau ayun)
    if p.hlabs:
        near_hand = (sho[0] + p.hlabs[0] + 3, sho[1] + p.hlabs[1] + 1)
    else:
        near_hand = (sho[0] + 3 + 3 + p.hl[0], sho[1] + 24 + p.hl[1])
    ne, nh = fur_arm(f, (sho[0] + 3, sho[1] + 1), near_hand, 1, FUR, w=(9, 7, 6), edge=True)
    along = (nh[0] - ne[0], nh[1] - ne[1])
    claws(f, nh, along, 3, 5, 2.0)
    if p.slash:
        _slash_arc_side(f, sho, p.slash)
    return f.finish()


def _slash_arc_side(f, sho, level):
    pts = [(sho[0] + 19, sho[1] - 6), (sho[0] + 25, sho[1] + 2), (sho[0] + 28, sho[1] + 12), (sho[0] + 25, sho[1] + 22)]
    if level == 1:
        pts = pts[:3]
    for i in range(len(pts) - 1):
        f.line(pts[i], pts[i + 1], SLASH, 1)
        f.line((pts[i][0] + 1, pts[i][1] + 1), (pts[i + 1][0] + 1, pts[i + 1][1] + 1), SLASH_D, 1)


# ───────────── tampak DEPAN (menghadap bawah / ke arah kamera) ─────────────
def front(p):
    f = Fig()
    ub = p.ub
    hipy = 52 + ub * 0.5
    shy = 31 + ub
    head = (CX, 19 + ub + (1 if p.jaw >= 3 else 0) + (p.lean // 2))

    def leg(sx, foot_dx, lift, pal, seed):
        hx = CX + sx * 6
        fx = CX + sx * 8 + foot_dx * 0.4
        fy = GROUND - lift
        knee, foot = ik((hx, hipy), (fx, fy - 4), 12, 11, -sx)      # lutut sedikit keluar
        m = Mask()
        m.limb((hx, hipy), knee, 11, 8).limb(knee, (fx, fy - 2), 8, 7)
        f.part(m, pal, seed=seed)
        paw(f, (fx, fy), 'down', pal)

    leg(-1, p.fl[0] * 0.5, p.fl[1], FUR, 41)
    leg(1, p.fr[0] * 0.5, p.fr[1], FUR, 42)

    torso = Mask().poly([(CX - 20, shy + 1), (CX - 11, shy - 7), (CX + 11, shy - 7), (CX + 20, shy + 1), (CX + 18, shy + 11),
                         (CX + 10, hipy + 1), (CX - 10, hipy + 1), (CX - 18, shy + 11)])
    f.part(torso, FUR, seed=51)
    chest = Mask().poly([(CX - 11, shy - 3), (CX + 11, shy - 3), (CX + 8, shy + 12), (CX + 5, hipy - 2), (CX - 5, hipy - 2), (CX - 8, shy + 12)])
    f.part(chest, CHEST, seed=52, strands=False)
    for yy in (shy + 3, shy + 7, shy + 11):            # garis tulang rusuk
        f.px(CX - 4, yy, CHEST[2])
        f.px(CX + 4, yy, CHEST[2])
        f.px(CX - 3, yy + 1, CHEST[2])
        f.px(CX + 3, yy + 1, CHEST[2])

    loincloth(f, CX - 8, CX + 8, hipy - 1, hipy + 11, 2, 61)

    # lengan: kiri layar = sisi 'L', kanan layar = sisi 'R'
    def arm(sx, hand_off, absn, pal):
        shoulder = (CX + sx * 17, shy + 1)
        if absn is not None:
            hand = (shoulder[0] + (absn[0] if sx > 0 else -absn[0]), shoulder[1] + absn[1])
        else:
            hand = (shoulder[0] + sx * 1 + hand_off[0] * 0.3, shoulder[1] + 26 + hand_off[1])
        el, h = fur_arm(f, shoulder, hand, -sx, pal, w=(9, 7, 6))
        claws(f, h, (h[0] - el[0], h[1] - el[1]), 3, 5, 2.2)
        return h

    # lengan non-penyerang dulu
    if p.hlabs:
        arm(1, p.hr, None, FUR)
        arm(-1, p.hl, p.hlabs, FUR)
    else:
        arm(-1, p.hl, None, FUR)
        arm(1, p.hr, None, FUR)

    _head_front(f, head, p, back=False)
    if p.slash:
        _slash_arc_front(f, shy, p.slash)
    return f.finish()


def _slash_arc_front(f, shy, level):
    pts = [(CX - 18, shy - 10), (CX - 8, shy - 2), (CX + 2, shy + 8), (CX + 12, shy + 17)]
    if level == 1:
        pts = pts[:3]
    for i in range(len(pts) - 1):
        f.line(pts[i], pts[i + 1], SLASH, 1)
        f.line((pts[i][0] + 1, pts[i][1] + 1), (pts[i + 1][0] + 1, pts[i + 1][1] + 1), SLASH_D, 1)


def _head_front(f, head, p, back):
    hx, hy = head
    mane = Mask().poly([(hx - 15, hy + 6), (hx - 11, hy - 3), (hx - 6, hy - 9), (hx + 6, hy - 9), (hx + 11, hy - 3), (hx + 15, hy + 6),
                        (hx + 11, hy + 12), (hx - 11, hy + 12)])
    f.part(mane, FUR_BACK, seed=73)
    for ox, ln in ((-15, 3), (-12, 4), (12, 4), (15, 3)):   # jumbai bulu di leher
        f.line((hx + ox, hy + 7), (hx + ox + (1 if ox > 0 else -1), hy + 7 + ln), FUR[2])
    ear_l = Mask().poly([(hx - 10, hy - 3), (hx - 13, hy - 15), (hx - 3, hy - 8)])
    ear_r = Mask().poly([(hx + 10, hy - 3), (hx + 13, hy - 15), (hx + 3, hy - 8)])
    f.part(ear_l, FUR, seed=74, strands=False)
    f.part(ear_r, FUR, seed=75, strands=False)
    if not back:
        for sx in (-1, 1):
            f.px(hx + sx * 10, hy - 8, EAR_IN)
            f.px(hx + sx * 10, hy - 9, EAR_IN)
            f.px(hx + sx * 9, hy - 7, EAR_IN)
    skull = Mask().ell(hx - 11, hy - 9, hx + 11, hy + 9)
    f.part(skull, FUR, seed=72)
    if back:
        for k in range(5):                                  # jambul tengkuk
            f.line((hx - 6 + k * 3, hy - 6), (hx - 6 + k * 3, hy - 9), FUR[2])
        return
    jawo = {0: 0, 1: 1, 2: 2, 3: 3}[p.jaw]
    snout = Mask().poly([(hx - 6, hy), (hx + 6, hy), (hx + 5, hy + 9 + jawo), (hx, hy + 11 + jawo), (hx - 5, hy + 9 + jawo)])
    f.part(snout, SNOUT, seed=76, strands=False)
    # mata merah menyala, alis tebal
    for sx in (-1, 1):
        ex = hx + sx * 6
        f.line((ex - sx * 3, hy - 5), (ex + sx * 2, hy - 3), FUR[2], 1)
        f.px(ex, hy - 2, EYE)
        f.px(ex + sx, hy - 2, EYE)
        f.px(ex, hy - 1, EYE_GLOW)
        f.px(ex + sx, hy - 1, EYE_GLOW)
        f.px(ex, hy - 2, EYE_HI)
    # hidung & mulut
    f.px(hx - 1, hy + 3, NOSE)
    f.px(hx, hy + 3, NOSE)
    f.px(hx + 1, hy + 3, NOSE)
    f.px(hx, hy + 4, NOSE)
    if p.jaw >= 1:
        my = hy + 7
        for xx in range(hx - 4, hx + 5):
            f.px(xx, my, MOUTH)
            if p.jaw >= 2:
                f.px(xx, my + 1, MOUTH)
        if p.jaw >= 2:
            f.px(hx - 1, my + 2, TONGUE)
            f.px(hx, my + 2, TONGUE)
            f.px(hx + 1, my + 2, TONGUE)
    f.px(hx - 3, hy + 7, TEETH)
    f.px(hx + 3, hy + 7, TEETH)
    f.px(hx - 3, hy + 8, TEETH)
    f.px(hx + 3, hy + 8, TEETH)


# ───────────── tampak BELAKANG (menghadap atas / menjauh dari kamera) ─────────────
def back(p):
    f = Fig()
    ub = p.ub
    hipy = 52 + ub * 0.5
    shy = 31 + ub
    head = (CX, 20 + ub + (p.lean // 2))

    # ekor menjulur ke bawah di belakang
    tail = Mask()
    tail.limb((CX, hipy), (CX + 2, hipy + 10), 6, 7).limb((CX + 2, hipy + 10), (CX + 1, hipy + 17), 7, 5)
    f.part(tail, FUR_BACK, seed=31)

    def leg(sx, foot_dx, lift, pal, seed):
        hx = CX + sx * 6
        fx = CX + sx * 8 + foot_dx * 0.4
        fy = GROUND - lift
        knee, foot = ik((hx, hipy), (fx, fy - 4), 12, 11, -sx)
        m = Mask()
        m.limb((hx, hipy), knee, 11, 8).limb(knee, (fx, fy - 2), 8, 7)
        f.part(m, pal, seed=seed)
        paw(f, (fx, fy), 'up', pal)

    leg(-1, p.fl[0] * 0.5, p.fl[1], FUR_BACK, 41)
    leg(1, p.fr[0] * 0.5, p.fr[1], FUR_BACK, 42)

    torso = Mask().poly([(CX - 20, shy + 1), (CX - 11, shy - 8), (CX + 11, shy - 8), (CX + 20, shy + 1), (CX + 18, shy + 11),
                         (CX + 10, hipy + 1), (CX - 10, hipy + 1), (CX - 18, shy + 11)])
    f.part(torso, FUR, seed=51)
    for k in range(6):                                       # duri bulu sepanjang tulang punggung
        yy = shy - 5 + k * 4
        f.line((CX, yy), (CX, yy + 2), FUR[2])
        f.px(CX - 1, yy + 1, FUR[2])
        f.px(CX + 1, yy + 1, FUR[2])

    # cawat tampak belakang: cuma ujung tali + lipatan bawah
    loincloth(f, CX - 7, CX + 7, hipy - 1, hipy + 6, 1, 62)

    def arm(sx, hand_off, absn, pal):
        shoulder = (CX + sx * 17, shy + 1)
        if absn is not None:
            hand = (shoulder[0] + (absn[0] if sx > 0 else -absn[0]), shoulder[1] + absn[1])
        else:
            hand = (shoulder[0] + sx * 1 + hand_off[0] * 0.3, shoulder[1] + 26 + hand_off[1])
        el, h = fur_arm(f, shoulder, hand, -sx, pal, w=(9, 7, 6))
        claws(f, h, (h[0] - el[0], h[1] - el[1]), 3, 5, 2.2)

    if p.hlabs:
        arm(1, p.hr, None, FUR)
        arm(-1, p.hl, p.hlabs, FUR)
    else:
        arm(-1, p.hl, None, FUR)
        arm(1, p.hr, None, FUR)

    _head_front(f, head, p, back=True)
    if p.slash:
        _slash_arc_front(f, shy, p.slash)
    return f.finish()


# ───────────── MATI (rebah menyamping) ─────────────
def dead():
    f = Fig()
    gy = GROUND
    # ekor & kaki di kiri, kepala di kanan, badan tergeletak
    tail = Mask().limb((11, gy - 5), (4, gy - 2), 6, 4)
    f.part(tail, FUR_BACK, seed=31)
    legs = Mask().limb((20, gy - 8), (9, gy - 4), 10, 7).limb((24, gy - 5), (12, gy - 1), 8, 6)
    f.part(legs, FUR_BACK, seed=41)
    paw(f, (10, gy - 1), 'left', FUR_BACK)
    body = Mask().ell(18, gy - 20, 52, gy - 1)
    f.part(body, FUR, seed=51)
    belly = Mask().ell(26, gy - 10, 48, gy - 1)
    f.part(belly, CHEST, seed=52, strands=False)
    loincloth(f, 22, 33, gy - 8, gy - 1, 1, 61)
    arm = Mask().limb((46, gy - 11), (60, gy - 5), 8, 6).circ(60, gy - 5, 3.5)
    f.part(arm, FUR, seed=11)
    claws(f, (61, gy - 5), (1, 0.5), 3, 4, 1.8)
    head = Mask().ell(50, gy - 18, 66, gy - 4)
    f.part(head, FUR, seed=72)
    snout = Mask().poly([(60, gy - 11), (69, gy - 9), (69, gy - 5), (60, gy - 5)])
    f.part(snout, SNOUT, seed=76, strands=False)
    ear = Mask().poly([(55, gy - 15), (53, gy - 22), (60, gy - 17)])
    f.part(ear, FUR, seed=74, strands=False)
    f.px(69, gy - 9, NOSE)
    f.px(69, gy - 8, NOSE)
    f.line((62, gy - 8), (68, gy - 7), FUR[2])            # mata mati: garis gelap, tanpa cahaya
    f.line((60, gy - 5), (68, gy - 5), MOUTH)
    f.px(66, gy - 4, TEETH)
    f.px(63, gy - 4, TEETH)
    f.px(66, gy - 3, TONGUE)
    return f.finish()


# ───────────── render semua ─────────────
def mirror(img):
    return img.transpose(Image.FLIP_LEFT_RIGHT)


def pose_for(kind, n):
    if kind == 'idle':
        return idle_pose(n)
    if kind == 'walk':
        return walk_pose(n)
    return attack_pose(n)


def render_dir(d):
    frames = []
    seq = [('idle', i) for i in range(IDLE_N)] + [('walk', i) for i in range(WALK_N)] + [('attack', i) for i in range(ATTACK_N)]
    for kind, n in seq:
        p = pose_for(kind, n)
        if d == 'right':
            frames.append(side(p))
        elif d == 'left':
            frames.append(mirror(side(p)))
        elif d == 'up':
            frames.append(back(p))
        else:
            frames.append(front(p))
    corpse = dead()
    frames.append(corpse if d != 'left' else mirror(corpse))
    return frames


def build_sheet():
    sheet = Image.new('RGBA', (COLS * W, len(DIRS) * H), (0, 0, 0, 0))
    for r, d in enumerate(DIRS):
        for c, fr in enumerate(render_dir(d)):
            sheet.paste(fr, (c * W, r * H))
    return sheet


def main():
    sheet = build_sheet()
    if '--preview' in sys.argv:
        i = sys.argv.index('--preview')
        out = Path(sys.argv[i + 1]) if i + 1 < len(sys.argv) else Path('werewolf_preview.png')
        bg = Image.new('RGBA', sheet.size, (86, 112, 132, 255))      # latar kebiruan biar kelihatan siluetnya
        bg.alpha_composite(sheet)
        bg = bg.resize((bg.width * 3, bg.height * 3), Image.NEAREST)
        bg.convert('RGB').save(out)
        print('preview ->', out)
        return
    ASET.mkdir(parents=True, exist_ok=True)
    path = ASET / 'werewolf_sheet.png'
    sheet.save(path)
    print('ditulis', path, sheet.size)


if __name__ == '__main__':
    main()
