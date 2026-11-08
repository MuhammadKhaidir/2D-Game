#!/usr/bin/env python3
"""
Bikin /Aset/werewolf_sheet.png dari frame PLAYER (src/Aset/*.png), jadi animasi werewolf pasti sama persis dengan player:
  - jalan  = FrontWalk1..6 / BackWalk1..6 / LeftWalk1..6 / RightWalk1..6   (6 frame x 4 arah)
  - lari   = XxxRun1..3 (dipakai buat terkam / lunge)
  - serang = attack player ke-3 ("Front (1..4)") dan ke-4 ("Front (5..8)"); attack ke-1 dan ke-2 TIDAK dipakai
  - idle   = XxxIdle1 (2 frame: napas naik-turun 1 px)
  - mati   = 3 frame, idle yang direbahkan (kayak CharXxxDead player: sempoyongan -> jatuh -> tergeletak)
Tiap frame player diubah jadi serigala: warna bulu abu-biru, telinga + moncong panjang + taring + mata kuning, cakar 3 kuku + efek sabetan cakar (frame serangan),
ekor, bulu mencuat di siluet. Hasil digedein 2x (nearest) biar kelihatan lebih besar dari player.

Layout sheet (HARUS sama dengan konstanta di Entitas/WerewolfSprite.java):
    cell 112 x 96 px, 22 kolom x 4 baris (sheet 2464 x 384)
    baris : 0 right | 1 left | 2 up | 3 down
    kolom : idle(2) | walk(6) | run(3) | attack3(4) | attack4(4) | dead(3)

Jalankan:  python3 tools/werewolf_gen/player_sheet.py          (butuh Pillow + numpy)
           python3 tools/werewolf_gen/player_sheet.py --debug   (nyimpen juga preview 3x ke werewolf_preview.png)
"""
import os
import sys
from collections import deque

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))   # folder 2DGame
ASET = os.path.join(ROOT, "src", "Aset")
OUT = os.path.join(ASET, "werewolf_sheet.png")
PREVIEW = os.path.join(os.path.dirname(__file__), "werewolf_preview.png")

CROP = (4, 6, 60, 54)          # area 56x48 dari frame player 64x64 yang dipakai (badan + telinga + ekor)
UP = 2                         # pembesaran
CELL_W = 56 * UP               # 112
CELL_H = 48 * UP               # 96
GROUND = 43                    # baris telapak kaki di frame player (1x)

# urutan baris sheet = urutan arah di game (RIGHT, LEFT, UP, DOWN) -> awalan nama file player
DIRS = [("right", "Right"), ("left", "Left"), ("up", "Back"), ("down", "Front")]

N_IDLE, N_WALK, N_RUN, N_ATK3, N_ATK4, N_DEAD = 2, 6, 3, 4, 4, 3
COLS = N_IDLE + N_WALK + N_RUN + N_ATK3 + N_ATK4 + N_DEAD

RAMP = [(20, 18, 26), (52, 50, 64), (86, 84, 100), (122, 120, 136), (160, 158, 172), (198, 196, 208)]
CH = {
    "D": RAMP[0], "B": RAMP[1], "f": RAMP[2], "F": RAMP[3], "l": RAMP[4], "L": RAMP[5],
    "I": (96, 44, 60),          # dalam telinga
    "E": (255, 214, 40),        # mata
    "N": (14, 12, 16),          # hidung
    "T": (244, 244, 244),       # taring
    "M": (120, 24, 40),         # dalam mulut
    "C": (232, 232, 240),       # cakar
    "W": (255, 255, 255),       # ujung kuku / inti sabetan
    "S": (206, 222, 255),       # sabetan cakar (terang)
    "s": (150, 172, 214),       # sabetan cakar (pudar)
}

# Template kepala: daftar (dx, dy, karakter). dy relatif ke y0 (puncak kepala player).
# FRONT / BACK: dx relatif ke tengah kepala (cx). Telinga kiri di-cermin jadi telinga kanan.
_EAR_FRONT_L = [(-4, -4, "D"), (-5, -3, "D"), (-4, -3, "F"), (-3, -3, "D"),
                (-5, -2, "D"), (-4, -2, "I"), (-3, -2, "F"), (-2, -2, "D"),
                (-5, -1, "D"), (-4, -1, "I"), (-3, -1, "F"), (-2, -1, "F")]
_EAR_BACK_L = [(-4, -4, "D"), (-5, -3, "D"), (-4, -3, "F"), (-3, -3, "D"),
               (-5, -2, "D"), (-4, -2, "F"), (-3, -2, "f"), (-2, -2, "D"),
               (-5, -1, "D"), (-4, -1, "F"), (-3, -1, "F"), (-2, -1, "F")]


def _mirror_x(pts):
    return [(-dx, dy, c) for dx, dy, c in pts]


def _tpl(rows, dx0, dy0):
    """Template dari teks: tiap karakter = satu piksel (lihat CH), '.' = kosong. Kiri-atas = (dx0, dy0)."""
    return [(dx0 + i, dy0 + j, ch) for j, row in enumerate(rows) for i, ch in enumerate(row) if ch not in ". "]


# FRONT: telinga + muka depan. Kolom dx = -5..+5 (dari tengah kepala), baris dy = 3..10 (dari puncak kepala).
# Alis miring (marah), moncong lebar berkontur gelap, hidung segitiga, mulut terbuka + taring 2 px.
FRONT = _EAR_FRONT_L + _mirror_x(_EAR_FRONT_L) + _tpl([
    "..BBBlBBB..",
    "..EEBlBEE..",
    "FFFllLllFFF",
    "FFDlNNNlDFF",
    "FFDllNllDFF",
    "FFDTMMMTDFF",
    "fFDTlllTDFf",
    "...DDDDD...",
], -5, 3)
BACK = _EAR_BACK_L + _mirror_x(_EAR_BACK_L)
# SIDE (menghadap KIRI): dx relatif ke xf = tepi muka (kiri) kepala. Menghadap kanan = cermin (xf = tepi kanan).
SIDE_EARS = [
    # telinga jauh (nongol di depan) + telinga dekat (di belakang)
    (2, -3, "D"), (1, -2, "D"), (2, -2, "I"), (3, -2, "D"), (1, -1, "D"), (2, -1, "I"), (3, -1, "F"),
    (6, -4, "D"), (5, -3, "D"), (6, -3, "F"), (7, -3, "D"),
    (4, -2, "D"), (5, -2, "I"), (6, -2, "F"), (7, -2, "F"), (8, -2, "D"),
    (4, -1, "D"), (5, -1, "I"), (6, -1, "F"), (7, -1, "F"), (8, -1, "F"),
]
# Muka samping: alis + mata, MONCONG PANJANG (nonjol 7 px ke depan, dx = -7..-1), hidung hitam di ujung,
# mulut terbuka merah + taring (T), rahang bawah, garis dagu.   Kolom dx = -8..+4, baris dy = 3..10.
SIDE = SIDE_EARS + _tpl([
    ".........BBBf",
    ".....DDDDFEEl",
    "..DDDFFFlllll",
    ".DNNNlllllllF",
    ".DNllllllllFF",
    ".DMTMMTMMFFfF",
    ".DDlllllDFFff",
    "..DDDDDDD....",
], -8, 3)


# ───────────────────────── util gambar ─────────────────────────

def load(name):
    return np.array(Image.open(os.path.join(ASET, name + ".png")).convert("RGBA"))


def opaque(a):
    return a[:, :, 3] > 0


def drop_detached_below(a):
    """Buang bayangan lonjong yang nempel di frame Run (komponen terpisah yang ada di bawah badan)."""
    m = opaque(a)
    h, w = m.shape
    lab = -np.ones((h, w), int)
    comps = []
    for sy in range(h):
        for sx in range(w):
            if m[sy, sx] and lab[sy, sx] < 0:
                q = deque([(sy, sx)])
                lab[sy, sx] = len(comps)
                pts = []
                while q:
                    y, x = q.popleft()
                    pts.append((y, x))
                    for dy in (-1, 0, 1):
                        for dx in (-1, 0, 1):
                            ny, nx = y + dy, x + dx
                            if 0 <= ny < h and 0 <= nx < w and m[ny, nx] and lab[ny, nx] < 0:
                                lab[ny, nx] = len(comps)
                                q.append((ny, nx))
                comps.append(pts)
    if len(comps) <= 1:
        return a
    main = max(comps, key=len)
    main_bottom = max(y for y, _ in main)
    out = a.copy()
    for pts in comps:
        if pts is main:
            continue
        if min(y for y, _ in pts) > main_bottom - 1:
            for y, x in pts:
                out[y, x, 3] = 0
    return out


def furify(a):
    """Warna kulit player -> bulu (6 tingkat, bayangan tetap kebawa)."""
    out = a.copy()
    rgb = a[:, :, :3].astype(float)
    lum = 0.299 * rgb[:, :, 0] + 0.587 * rgb[:, :, 1] + 0.114 * rgb[:, :, 2]
    idx = np.clip(np.round((lum - 24) / (262 - 24) * 5), 0, 5).astype(int)
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            if a[y, x, 3] > 0:
                out[y, x, :3] = RAMP[idx[y, x]]
                out[y, x, 3] = 255
    return out


def put(a, x, y, ch, only_empty=False):
    if 0 <= x < a.shape[1] and 0 <= y < a.shape[0] and ch != "." and not (only_empty and a[y, x, 3] > 0):
        a[y, x, :3] = CH[ch]
        a[y, x, 3] = 255


def head_info(a):
    """(y0, xmin, xmax, cx, ybot) kepala: puncak badan + lebar run horizontal yang nyambung ke puncak."""
    m = opaque(a)
    ys, xs = np.where(m)
    y0 = int(ys.min())
    top_x = int(np.median(xs[ys == y0]))
    xmin, xmax = top_x, top_x
    for y in range(y0, y0 + 5):
        if not m[y, top_x]:
            # run terdekat di baris ini
            cand = np.where(m[y])[0]
            if len(cand) == 0:
                continue
            tx = int(cand[np.argmin(abs(cand - top_x))])
        else:
            tx = top_x
        l = r = tx
        while l - 1 >= 0 and m[y, l - 1]:
            l -= 1
        while r + 1 < m.shape[1] and m[y, r + 1]:
            r += 1
        if y >= y0 + 1:
            xmin, xmax = min(xmin, l), max(xmax, r)
    return y0, xmin, xmax, (xmin + xmax) // 2, int(ys.max())


def apply_template(a, tpl, y0, anchor_x, mirror=False):
    """Gambar template (dx, dy, char) dengan titik acuan anchor_x (tengah kepala / tepi muka); mirror = menghadap kanan."""
    for dx, dy, c in tpl:
        put(a, anchor_x + (-dx if mirror else dx), y0 + dy, c)


def add_tail(a, kind, y0, ybot):
    m = opaque(a)
    hip = y0 + int(round(0.58 * (ybot - y0)))
    if kind in ("up", "down"):
        # dari belakang: ekor menjuntai di depan pinggang. dari depan: sedikit nongol di samping kaki.
        if kind == "up":
            xs = np.where(m[hip])[0]
            bx = int(np.median(xs)) if len(xs) else 31
            tail = ["DFFD", "DFFD", "DFlD", "DllD", "DLLD", ".DD."]
            for r, row in enumerate(tail):
                for i, c in enumerate(row):
                    put(a, bx - 1 + i, hip + r, c)
        else:
            xs = np.where(m[hip + 3])[0]
            if len(xs):
                bx = int(xs.max()) + 1
                for dx, dy, c in [(0, 0, "D"), (1, -1, "D"), (1, 0, "l"), (2, 0, "D"), (2, 1, "L"), (3, 1, "D"), (2, 2, "D")]:
                    put(a, bx + dx, hip + 3 + dy, c)
    else:
        rows = [hip - 1, hip, hip + 1]
        sgn = 1 if kind == "left" else -1          # menghadap kiri -> ekor ke kanan
        edge = [np.where(m[r])[0] for r in rows]
        edge = [e for e in edge if len(e)]
        if not edge:
            return
        xb = max(e.max() for e in edge) if sgn == 1 else min(e.min() for e in edge)
        # ekor lebat: keluar dari pinggang ke belakang lalu melengkung ke bawah
        tail = [(2, -2, "D"), (3, -2, "D"),
                (1, -1, "D"), (2, -1, "F"), (3, -1, "F"), (4, -1, "D"),
                (1, 0, "F"), (2, 0, "F"), (3, 0, "l"), (4, 0, "F"), (5, 0, "D"),
                (2, 1, "D"), (3, 1, "l"), (4, 1, "l"), (5, 1, "F"), (6, 1, "D"),
                (3, 2, "D"), (4, 2, "l"), (5, 2, "L"), (6, 2, "L"), (7, 2, "D"),
                (4, 3, "D"), (5, 3, "L"), (6, 3, "L"), (7, 3, "D"),
                (5, 4, "D"), (6, 4, "D")]
        for dx, dy, c in tail:
            put(a, xb + sgn * (dx - 2), hip + dy, c)      # pangkal ekor menempel di pinggang


def add_claws(a, y0, ybot, vdown=0.0):
    """Cakar: 3 kuku runcing (5 px, ujungnya putih) yang menempel di ujung kedua tangan. vdown = condong ke bawah."""
    m = opaque(a)
    cxs = np.where(m)[1]
    mid = (cxs.min() + cxs.max()) / 2.0
    rows = [y for y in range(y0 + 9, min(y0 + 22, ybot - 6)) if m[y].any()]
    if not rows:
        return
    for sgn in (-1, 1):
        best = None
        for y in rows:
            xs = np.where(m[y])[0]
            x = xs.min() if sgn < 0 else xs.max()
            if best is None or (x - best[0]) * sgn > 0:
                best = (x, y)
        if best and abs(best[0] - mid) >= 5:
            tx, ty = best
            for vy in (-0.6, 0.0, 0.6):
                for k in range(1, 6):
                    put(a, tx + int(round(sgn * k * 0.95)), ty + int(round((vy + vdown) * k)), "W" if k == 5 else "C")


def _bez(p0, p1, p2, t):
    return ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1])


# Busur sabetan: 3 garis sejajar = (awal, kontrol, akhir, geser antar garis) dalam koordinat lokal (u, v).
# side: u = maju dari tepi muka; front / back: u = ke kanan dari tengah kepala. v = turun dari puncak kepala.
_ARC = {
    "side": ((7, 2), (22, 7), (15, 22), (-3, 1)),
    "front": ((-13, 8), (6, 6), (14, 25), (-3, -1)),
    "back": ((-14, 3), (0, -3), (13, 15), (-3, -1)),
}


def slash_fx(a, kind, variant, f, y0, ax, sg=1):
    """Efek sabetan cakar (3 garis putih-biru). f = indeks frame serangan 0..3 (damage masuk di f=1 = sabetan penuh,
    f=2 memudar, f=3 tinggal sisa). variant 3 = menebas atas -> bawah, variant 4 = bawah -> atas (gantian tiap serangan).
    ax = tengah kepala (front / back) atau tepi muka (side), sg = +1 menghadap kanan / -1 kiri."""
    if f == 0:
        return
    key = "side" if kind in ("left", "right") else ("front" if kind == "down" else "back")
    st, ct, en, (ou, ov) = _ARC[key]
    t0, t1, cols = {1: (0.0, 1.0, "WSS"), 2: (0.2, 1.0, "SsS"), 3: (0.5, 1.0, "sss")}[f]
    eo = key != "front"        # sabetan ke arah kamera menimpa badan; sisi & belakang cuma di area kosong
    for i in range(3):
        p0, p1, p2 = [(q[0] + ou * i, q[1] + ov * i) for q in (st, ct, en)]
        if variant == 4:
            p0, p1, p2 = [(q[0] if key == "side" else -q[0], 24 - q[1]) for q in (p0, p1, p2)]
        for k in range(41):
            t = t0 + (t1 - t0) * k / 40
            u, v = _bez(p0, p1, p2, t)
            x = int(round(ax + sg * u if key == "side" else ax + u))
            y = int(round(y0 + v))
            put(a, x, y, cols[i], only_empty=eo)
            if f == 1 and 0.25 < t < 0.75:      # bagian tengah dibikin tebal 2 px
                put(a, x + (0 if key == "side" else 1), y + (1 if key == "side" else 0), cols[i], only_empty=eo)


def add_tufts(a, y_from):
    """Bulu mencuat di siluet (jarang, cuma di bawah kepala)."""
    m = opaque(a)
    h, w = m.shape
    out = a.copy()
    for y in range(y_from, h - 2):
        for x in range(1, w - 1):
            if m[y, x]:
                continue
            nb = m[y + 1, x] or m[y, x - 1] or m[y, x + 1]
            if nb and ((x * 31 + y * 17) % 9 == 0):
                out[y, x, :3] = RAMP[2]
                out[y, x, 3] = 255
    return out


def wolfify(a, kind, atk=None):
    """a: frame player 64x64 RGBA (1x) -> frame werewolf 64x64 RGBA (1x).
    atk = None (bukan serangan) atau (variant 3/4, indeks frame serangan 0..3) -> pakai cakar + sabetan."""
    a = furify(a)
    y0, xmin, xmax, cx, ybot = head_info(a)
    a = add_tufts(a, y0 + 10)
    if atk:
        add_claws(a, y0, ybot, 0.35 if kind in ("up", "down") else 0.0)    # sebelum muka & ekor (jangan nyangkut moncong / ujung ekor)
    if kind in ("left", "right"):
        mirror = kind == "right"
        xf = xmax if mirror else xmin
        apply_template(a, SIDE, y0, xf, mirror)
    elif kind == "down":
        apply_template(a, FRONT, y0, cx)
    else:
        apply_template(a, BACK, y0, cx)
    add_tail(a, kind, y0, ybot)
    if atk:
        if kind in ("left", "right"):
            slash_fx(a, kind, atk[0], atk[1], y0, xmax if kind == "right" else xmin, 1 if kind == "right" else -1)
        else:
            slash_fx(a, kind, atk[0], atk[1], y0, cx)
    return a


def breathe(a):
    """Frame idle ke-2: kepala + bahu turun 1 px (napas)."""
    m = opaque(a)
    ys = np.where(m.any(axis=1))[0]
    y0, ybot = int(ys.min()), int(ys.max())
    cut = y0 + int(0.45 * (ybot - y0))
    out = a.copy()
    out[y0:cut + 1] = 0
    out[y0 + 1:cut + 2] = a[y0:cut + 1]
    # baris di bawah 'cut+1' tetap; yang ketimpa cuma 1 baris
    return out


def rot_fall(a, kind, deg):
    """Rebahkan frame (sudah jadi werewolf) memutar di telapak kaki, lalu naruh lagi di tanah."""
    h, w = a.shape[:2]
    sgn = 1 if kind == "right" else -1          # jatuh ke belakang
    ys, xs = np.where(opaque(a))
    px = (xs.min() + xs.max()) / 2.0
    img = Image.fromarray(a).rotate(sgn * deg, resample=Image.NEAREST, center=(px, GROUND + 1), expand=False)
    b = np.array(img)
    ys, xs = np.where(opaque(b))
    if len(ys) == 0:
        return b
    dy = GROUND - int(ys.max())
    dx = int(round((31.5 - (xs.min() + xs.max()) / 2.0) * (0.5 if deg < 80 else 1.0)))
    out = np.zeros_like(b)
    for y in range(h):
        for x in range(w):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and b[y, x, 3] > 0:
                out[ny, nx] = b[y, x]
    return out


def cell(a):
    img = Image.fromarray(a).crop(CROP).resize((CELL_W, CELL_H), Image.NEAREST)
    return img


def build():
    sheet = Image.new("RGBA", (COLS * CELL_W, 4 * CELL_H), (0, 0, 0, 0))
    for r, (kind, pre) in enumerate(DIRS):
        frames = []
        base = wolfify(drop_detached_below(load(pre + "Idle1")), kind)
        frames += [base, breathe(base)]                                                    # idle
        for i in range(1, 7):
            frames.append(wolfify(load(f"{pre}Walk{i}"), kind))                            # walk
        for i in range(1, 4):
            frames.append(wolfify(drop_detached_below(load(f"{pre}Run{i}")), kind))        # run (terkam)
        for i in range(1, 5):
            frames.append(wolfify(load(f"{pre} ({i})"), kind, (3, i - 1)))                 # attack 3 (cakar + sabetan)
        for i in range(5, 9):
            frames.append(wolfify(load(f"{pre} ({i})"), kind, (4, i - 5)))                 # attack 4 (cakar + sabetan)
        for deg in (28, 58, 90):
            frames.append(rot_fall(base, kind, deg))                                       # dead
        assert len(frames) == COLS, len(frames)
        for c, f in enumerate(frames):
            sheet.alpha_composite(cell(f), (c * CELL_W, r * CELL_H))
    sheet.save(OUT)
    print("ditulis:", OUT, sheet.size)
    return sheet


if __name__ == "__main__":
    s = build()
    if "--debug" in sys.argv:
        z = 3
        s.resize((s.width * z // 2, s.height * z // 2), Image.NEAREST).save(PREVIEW)
        print("preview:", PREVIEW)
