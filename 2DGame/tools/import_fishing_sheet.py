#!/usr/bin/env python3
"""
Bikin ikon item mancing + gambar senjata pas diayun dari SATU gambar sheet pixel-art
(grid 8 x 4, tiap item ada labelnya: broken_bottle, coin_pouch, ... vitality_pearl).

    python3 tools/import_fishing_sheet.py <sheet.png>              # ikon + senjata di tangan
    python3 tools/import_fishing_sheet.py <sheet.png> icons        # ikon saja
    python3 tools/import_fishing_sheet.py <sheet.png> held         # senjata di tangan saja
    python3 tools/import_fishing_sheet.py <sheet.png> preview.png  # + simpan pratinjau ikon (sesuai ukuran slot backpack)

Hasil:
    src/drop/fishing/<slug>.png         ikon 64x64 (grid asli 32x32, tiap piksel dobel)
    src/drop/fishing/held/<slug>.png    7 senjata mancing: lembar frame animasi serang 1024x256
                                        (kolom = state 1..4 x frame 1..4, baris = up / down / left / right,
                                        tiap sel 64x64). Dibaca Object/Weapon.java.

Kenapa hasilnya "pixel-art beneran" dan bukan sekadar gambar di-resize:
  * Sheet aslinya pikselnya gak teratur (ukuran piksel campur-campur). Tiap item di-pixelate ke grid 32x32 yang rapi
    (area-average + sedikit sharpen), lalu paletnya dikunci ke 20 warna (k-means di ruang Lab), piksel yatim dibuang,
    dan piksel tepi digelapkan jadi outline 1 piksel.
  * Senjata di tangan DIGAMBAR TANGAN piksel per piksel (HELD_ART di bawah) di SKALA SPRITE PLAYER: panjang 22 piksel,
    bilah 3 piksel, guard 7 piksel, sama kayak pedang / kapak bawaan. Gambar dari sheet gak dipakai di sini karena di
    ukuran segitu hasil resize cuma jadi lumpur. Bentuk & warna tiap senjata mengikuti ikonnya. Sprite diputar pakai
    "rotsprite" (diputar di resolusi 6x, diturunkan lagi, warna dikunci ke palet sprite) supaya pikselnya tetap rapi.
    Gerakan, efek tebasan, dan posisi tangan diambil dari frame bawaan (src/Aset/Sword* / *Axe*): senjata lama dipisah
    dari efek tebasan (warna persis 244,249,248), lalu diganti. Mau ubah tampilan senjata? Edit HELD_ART, jalankan ulang.

Butuh: Pillow, numpy, scipy, opencv-python.   (pip install pillow numpy scipy opencv-python)
Catatan: tools/generate_fishing_assets.py masih dipakai buat suara; ikon lamanya gak menimpa ikon ini.
"""
import math
import os
import sys

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(HERE, "..", "src"))
ICON_DIR = os.path.join(SRC, "drop", "fishing")
HELD_DIR = os.path.join(ICON_DIR, "held")
ASET = os.path.join(SRC, "Aset")

# urutan item di sheet: kiri -> kanan, atas -> bawah
SLUGS = [
    "broken_bottle", "coin_pouch", "coral_cutlass", "crab", "dried_kelp", "driftwood_hatchet", "electric_eel", "firefin_tuna",
    "jellyfish_jelly", "kraken_cleaver", "kraken_ink", "leviathan_fang", "lobster", "old_boot", "rusty_cutlass", "sailors_hatchet",
    "salmon", "sardine", "seaweed_clump", "soggy_plank", "storm_bottle", "sunken_chest", "tangled_net", "tidebreaker",
    "tin_can", "vitality_pearl",
]

# senjata: slug -> set animasi yang dipakai.
# Leviathan Fang sengaja pakai animasi Sword: frame BoneSword bawaan nyatu sama efek glow-nya (gak bisa dipisah bersih).
WEAPONS = {
    "rusty_cutlass": "Sword",
    "coral_cutlass": "Sword",
    "tidebreaker": "Sword",
    "leviathan_fang": "Sword",
    "driftwood_hatchet": "Axe",
    "sailors_hatchet": "Axe",
    "kraken_cleaver": "Axe",
}

DIRS = ["up", "down", "left", "right"]
GRID = 32                                    # grid asli ikon (di-file jadi 64x64)
SHEET_BG = (35, 44, 56)                      # warna latar sheet
LABEL_BANDS = [(189, 222), (421, 453), (638, 671), (837, 872)]   # baris teks label (y awal, y akhir)


# ----------------------------------------------------------------------------------------------
# 1. IRIS SHEET -> 26 ITEM TRANSPARAN (resolusi tinggi)
# ----------------------------------------------------------------------------------------------
def slice_sheet(path):
    a = np.array(Image.open(path).convert("RGB")).astype(int)
    bg = np.array(SHEET_BG)
    d = np.abs(a - bg).sum(2)
    mask = d > 40
    for (y0, y1) in LABEL_BANDS:
        mask[y0:y1, :] = False                       # buang teks label
    lab, n = ndi.label(ndi.binary_dilation(mask, iterations=5))
    boxes = []
    for s in ndi.find_objects(lab):
        y0, y1, x0, x1 = s[0].start, s[0].stop, s[1].start, s[1].stop
        if (y1 - y0) >= 40:
            boxes.append((y0, y1, x0, x1))
    if len(boxes) != len(SLUGS):
        raise SystemExit("Ketemu %d item di sheet, harusnya %d. Cek posisi LABEL_BANDS / SHEET_BG." % (len(boxes), len(SLUGS)))

    def row_of(b):
        cy = (b[0] + b[1]) / 2
        return 0 if cy < 215 else 1 if cy < 440 else 2 if cy < 660 else 3

    boxes.sort(key=lambda b: (row_of(b), b[2]))
    out = {}
    for slug, (y0, y1, x0, x1) in zip(SLUGS, boxes):
        pad = 3
        y0, x0 = max(0, y0 - pad), max(0, x0 - pad)
        y1, x1 = min(a.shape[0], y1 + pad), min(a.shape[1], x1 + pad)
        crop = a[y0:y1, x0:x1]
        cd = np.abs(crop - bg).sum(2)
        # latar = piksel mirip warna latar yang nyambung ke tepi crop (outline gelap item tetap kepake)
        lb, _ = ndi.label(cd <= 24)
        border = set(np.unique(np.concatenate([lb[0], lb[-1], lb[:, 0], lb[:, -1]]))) - {0}
        outside = np.isin(lb, list(border))
        alpha = np.where(outside, 0.0, 1.0)
        edge = ndi.binary_dilation(outside, iterations=2) & ~outside
        alpha = np.where(edge, np.clip((cd - 24) / 80.0, 0, 1), alpha)
        rgba = np.zeros(crop.shape[:2] + (4,), np.uint8)
        rgba[..., :3] = np.clip(crop, 0, 255)
        rgba[..., 3] = (alpha * 255).astype(np.uint8)
        out[slug] = _trim(_clean_specks(rgba))
    return out


def _clean_specks(rgba, min_frac=0.02):
    """Buang bintik kecil yang kepisah dari badan item (sisa teks label / noise)."""
    a = rgba[..., 3] > 40
    lab, n = ndi.label(ndi.binary_dilation(a, iterations=3))
    if n <= 1:
        return rgba
    sizes = ndi.sum(a, lab, range(1, n + 1))
    keep = [i + 1 for i, s in enumerate(sizes) if s >= max(30, min_frac * sizes.max())]
    out = rgba.copy()
    out[~np.isin(lab, keep), 3] = 0
    return out


def _trim(rgba):
    ys, xs = np.where(rgba[..., 3] > 20)
    return rgba[ys.min():ys.max() + 1, xs.min():xs.max() + 1]


# ----------------------------------------------------------------------------------------------
# 2. PIXELATE: gambar beresolusi tinggi -> sprite kecil dengan grid rapi
# ----------------------------------------------------------------------------------------------
def _bleed(rgb, opaque):
    """Isi warna piksel transparan dengan warna terdekat yang solid (biar gak ada halo gelap pas di-resize)."""
    idx = ndi.distance_transform_edt(~opaque, return_distances=False, return_indices=True)
    return rgb[idx[0], idx[1]]


def pixelate(rgba, box=(30, 30), colors=20, sharpen=0.8, outline=0.5):
    """RGBA beresolusi tinggi -> RGBA kecil: grid rapi, palet terbatas, piksel tepi digelapkan jadi outline."""
    a = rgba[..., 3] >= 128
    rgb = _bleed(rgba[..., :3], a).astype(np.float32)
    h, w = a.shape
    s = min(box[0] / w, box[1] / h)
    nw, nh = max(1, round(w * s)), max(1, round(h * s))
    small = cv2.resize(rgb, (nw, nh), interpolation=cv2.INTER_AREA)
    sa = cv2.resize(a.astype(np.float32), (nw, nh), interpolation=cv2.INTER_AREA) >= 0.5
    if sharpen:                                   # unsharp mask: detail kecil (tengkorak, kilau) tetap kebaca
        small = np.clip(small + sharpen * (small - cv2.GaussianBlur(small, (0, 0), 0.8)), 0, 255)
    lab = cv2.cvtColor(np.clip(small, 0, 255).astype(np.uint8), cv2.COLOR_RGB2LAB).astype(np.float32)
    pts = lab[sa]
    k = min(colors, max(2, len(np.unique(pts, axis=0))))
    cv2.setRNGSeed(7)
    _, labels, cent = cv2.kmeans(pts, k, None, (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 40, 0.5), 6, cv2.KMEANS_PP_CENTERS)
    q = np.zeros_like(lab)
    q[sa] = cent[labels.ravel()]
    out = np.zeros((nh, nw, 4), np.uint8)
    out[..., :3] = cv2.cvtColor(np.clip(q, 0, 255).astype(np.uint8), cv2.COLOR_LAB2RGB)
    out[..., 3] = np.where(sa, 255, 0)
    cols = out[..., :3].astype(int)               # buang piksel yatim: warnanya beda dari semua tetangga solid
    for y in range(nh):
        for x in range(nw):
            if not sa[y, x]:
                continue
            nb = [tuple(cols[yy, xx]) for yy in range(max(0, y - 1), min(nh, y + 2)) for xx in range(max(0, x - 1), min(nw, x + 2))
                  if (yy, xx) != (y, x) and sa[yy, xx]]
            if len(nb) >= 5 and tuple(cols[y, x]) not in nb:
                out[y, x, :3] = np.array(max(set(nb), key=nb.count), np.uint8)
    if outline:                                   # piksel tepi (nempel transparan, 4 arah) digelapkan
        pad = np.pad(sa, 1)
        edge = sa & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
        out[edge, :3] = (out[edge, :3].astype(np.float32) * outline).astype(np.uint8)
    return out


def to_canvas(small, size=GRID):
    h, w = small.shape[:2]
    c = np.zeros((size, size, 4), np.uint8)
    oy, ox = (size - h) // 2, (size - w) // 2
    c[oy:oy + h, ox:ox + w] = small
    return c


def tangled_net():
    """Jaring digambar sendiri: aslinya terlalu halus buat grid 32x32 (jadi noise). Mesh belah ketupat + simpul + pelampung."""
    W, H = 28, 24
    img = np.zeros((GRID, GRID, 4), np.uint8)
    ox, oy = (GRID - W) // 2, (GRID - H) // 2
    base, mesh, mesh_hi, knot, knot_sh, line = (22, 38, 46), (66, 112, 114), (118, 170, 160), (222, 205, 160), (140, 118, 82), (10, 16, 20)
    inside = np.zeros((GRID, GRID), bool)
    for y in range(H):
        for x in range(W):
            # sudut dipotong 2 piksel biar kelihatan lunak
            if (x + y < 2) or (W - 1 - x + y < 2) or (x + H - 1 - y < 2) or (W - 1 - x + H - 1 - y < 2):
                continue
            inside[oy + y, ox + x] = True
    for y in range(GRID):
        for x in range(GRID):
            if not inside[y, x]:
                continue
            u, v = x - ox, y - oy
            c = base
            if (u + v) % 6 == 0 or (u - v) % 6 == 0:
                c = mesh_hi if (u + v) % 12 == 0 else mesh
            img[y, x] = (*c, 255)
    for v in range(1, H, 6):                       # simpul di perpotongan garis
        for u in range(0, W, 6):
            for dy in (0, 1):
                for dx in (0, 1):
                    yy, xx = oy + v + dy, ox + u + dx
                    if 0 <= yy < GRID and 0 <= xx < GRID and inside[yy, xx]:
                        img[yy, xx] = (*(knot if (dx + dy) < 2 else knot_sh), 255)
    for (cx, cy) in ((ox + 1, oy + 1), (ox + W - 2, oy + 1), (ox + 1, oy + H - 2), (ox + W - 2, oy + H - 2)):   # pelampung di sudut
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) < 3 and 0 <= cy + dy < GRID and 0 <= cx + dx < GRID:
                    img[cy + dy, cx + dx] = (*(knot if (dx <= 0 and dy <= 0) else knot_sh), 255)
                    inside[cy + dy, cx + dx] = True
    pad = np.pad(inside, 1)
    edge = inside & ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    img[edge, :3] = line
    return img


# (jumlah warna, sharpen) per ikon. Default 14 warna: paling bersih buat benda bulat / organik. Item yang detail kecilnya
# penting (tengkorak di botol tinta, petir, gembok peti) dikasih palet lebih banyak biar detailnya gak hilang.
ICON_STYLE = {
    "kraken_ink": (20, 0.8), "storm_bottle": (20, 0.8), "sunken_chest": (20, 0.8), "vitality_pearl": (18, 0.7),
    "coin_pouch": (18, 0.7), "tin_can": (18, 0.7), "broken_bottle": (16, 0.6),
}


def make_icon(slug, rgba):
    if slug == "tangled_net":
        canvas = tangled_net()
    else:
        colors, sharpen = ICON_STYLE.get(slug, (14, 0.6))
        canvas = to_canvas(pixelate(rgba, box=(GRID - 2, GRID - 2), colors=colors, sharpen=sharpen))
    return Image.fromarray(canvas, "RGBA").resize((GRID * 2, GRID * 2), Image.NEAREST)


# ----------------------------------------------------------------------------------------------
# 3. SENJATA DI TANGAN
# ----------------------------------------------------------------------------------------------
ARC = (244, 249, 248)          # warna efek tebasan di frame bawaan (cuma dipakai efek)
SS = 6                         # faktor supersample buat rotsprite

SWORD_TAIL = {(127, 73, 53), (180, 127, 84), (218, 185, 136)}      # gagang / hand guard
SWORD_TIP = {(167, 181, 167), (124, 137, 137), (220, 220, 203), (255, 255, 255)}   # bilah
AXE_TAIL = {(69, 37, 56), (138, 79, 87), (213, 132, 100)}          # tangkai
AXE_TIP = {(138, 195, 217), (79, 142, 180), (199, 231, 231)}       # kepala kapak
FAMILY = {"Sword": (SWORD_TAIL, SWORD_TIP), "Axe": (AXE_TAIL, AXE_TIP)}


def base_frame_name(wtype, state, direction, i):
    f = i + 1
    D = {"up": "Back", "down": "Front", "left": "Left", "right": "Right"}[direction]
    if wtype == "Sword":
        return {1: "Sword%s (%d)" % (D, f), 2: "Swing%s (%d)" % (D, f), 3: "%sSw (%d)" % (D, f), 4: "%sSw (%d)" % (D, i + 5)}[state]
    return {1: "%sAxeThrust (%d)" % (D, f), 2: "%sAxeBash (%d)" % (D, f), 3: "%sAxeSwing (%d)" % (D, f), 4: "%sAxeSwing (%d)" % (D, i + 5)}[state]


def load_base(wtype, state, direction, i):
    return Image.open(os.path.join(ASET, base_frame_name(wtype, state, direction, i) + ".png")).convert("RGBA")


def split_frame(rgba, wtype):
    a = np.array(rgba)
    rgb = a[..., :3].astype(int)
    op = a[..., 3] > 0
    eff = op & (rgb[..., 0] == ARC[0]) & (rgb[..., 1] == ARC[1]) & (rgb[..., 2] == ARC[2])
    wep = op & ~eff
    tail_set, tip_set = FAMILY[wtype]
    tail = np.zeros(op.shape, bool)
    tip = np.zeros(op.shape, bool)
    for y, x in zip(*np.nonzero(wep)):
        c = tuple(rgb[y, x])
        if c in tail_set: tail[y, x] = True
        elif c in tip_set: tip[y, x] = True
    return eff, wep, tail, tip


def pose_from_pixels(mask, tail=None):
    """PCA: sumbu diarahkan dari gagang ke ujung. t_lo/t_hi = ujung gagang/ujung senjata sepanjang sumbu."""
    ys, xs = np.nonzero(mask)
    pts = np.stack([xs, ys], 1).astype(float) + 0.5
    m = pts.mean(0)
    cov = np.cov((pts - m).T) if len(pts) > 2 else np.eye(2)
    u = np.linalg.eigh(cov)[1][:, 1]
    tc = None
    if tail is not None and tail.sum() >= 3:
        ty, tx = np.nonzero(tail)
        tc = np.array([tx.mean() + 0.5, ty.mean() + 0.5])
        rest = mask & ~tail
        if rest.sum() >= 1:
            ry, rx = np.nonzero(rest)
            if np.dot(u, np.array([rx.mean() + 0.5, ry.mean() + 0.5]) - tc) < 0:
                u = -u
    elif u[0] < 0 or (abs(u[0]) < 1e-6 and u[1] > 0):
        u = -u
    v = np.array([-u[1], u[0]])
    t = (pts - m) @ u
    n = (pts - m) @ v
    t_lo, t_hi = t.min() - 0.5, t.max() + 0.5
    far = n[t > (t_lo + t_hi) / 2]
    return dict(m=m, u=u, v=v, t_lo=t_lo, t_hi=t_hi, skew=float(far.mean()) if len(far) else 0.0,
                t_anchor=None if tc is None else float((tc - m) @ u))


def _rgb(hexs):
    return tuple(int(hexs[i:i + 2], 16) for i in (0, 2, 4))


# Senjata di tangan digambar piksel per piksel, tegak (ujung di atas, gagang di bawah), 22 piksel panjangnya, proporsi sama
# kayak pedang / kapak bawaan (bilah 3 piksel, guard 7 piksel, gagang 3 piksel). Bentuk & warnanya mengikuti ikon di backpack.
# Satu huruf = satu warna (lihat palet di tiap senjata). '.' = transparan.
_SWORD_BOTTOM = [
    ".GyyyyyG.",   # guard
    ".GgggggG.",
    "..hhHhh..",   # pangkal gagang
    "...kHk...",
    "...HhH...",
    "...hmh...",   # ujung gagang
    "....h....",
]
_WING_BOTTOM = ["GyyyyyyyG", ".GgggggG."] + _SWORD_BOTTOM[2:]
_GOLD = dict(y="F0BA34", g="C88E24", G="8C5C1C", m="F2CE70", k="34221A")

HELD_ART = {
    "rusty_cutlass": ([".....t...", "....lwd..", "....lrd..", "...lwd...", "...lwd...", "...rwdd..", "...lwRd..", "...lwdd..",
                       "...lrdd..", "...lwdd..", "...rwdd..", "...lwd...", "...lwR...", "...lwd...", "...ldd..."] + _SWORD_BOTTOM,
                      dict(t="EBE3D0", l="BEB4AA", w="E1D7C8", d="78706C", r="B26430", R="824626", h="966440", H="604028",
                           y="F0BA34", g="C88E24", G="8C5C1C", m="C89A5A", k="34221A")),
    "coral_cutlass": (["....t....", "...lwd...", "...lwds..", "..slwd...", "...lwds..", "...lwd...", "..slwds..", "...lwd...",
                       "...lwds..", "..slwd...", "...lwds..", "...lwd...", "..slwds..", "...lwd...", "...ldd..."] + _SWORD_BOTTOM,
                      dict(t="FFEBF2", l="FFC4D4", w="FFDEE8", d="D67696", s="F096B2", h="966440", H="604028", **{k: v for k, v in _GOLD.items() if k not in "hH"})),
    "tidebreaker": (["....t....", "....w....", "....wb...", "...lwb...", "...lwb...", "...lwB...", "...lxb...", "...lwb...", "...lwB...",
                     "...lxb...", "...lwb...", "...lwB...", "...lwb...", "...lbB...", "...bbB..."] + _WING_BOTTOM,
                    dict(t="EBFCFF", l="6EC8F0", w="BEF0FC", b="368CD6", B="1E5CAA", x="FFFFFF", h="465A98", H="28305C", **_GOLD)),
    "leviathan_fang": (["......t..", "......c..", ".....ca..", ".....ca..", "....caA..", "....caA..", "....caA..", "...caaA..", "...caaA..", "...caaA..",
                        "...cAA...", "...caA...", "...caA...", "...caA...", "...iIi...", "..kkkkk..", "...iIi...", "...iIi...", "..kkkkk..",
                        "...iIi...", "...iIi...", "....I...."],
                       dict(t="DCFFFA", c="96F0E4", a="2EC4B6", A="1B7F78", i="EEE6CE", I="B0A68C", k="50422E")),
    "driftwood_hatchet": (["..DqD.....", "..DqkEk...", "..DqESSk..", "..gqESSSk.", "..gGESSSs.", "..gqESSsk.", "..gGkSsk..", "..DqDkk...",
                           "..DpD.....", "..DqD.....", "..DpD.....", "..DqD.....", "..gGg.....", "..DqD.....", "..DpD.....", "..DqD.....",
                           "..gGg.....", "..DqD.....", "..DpD.....", "..DqD.....", "..kDk.....", "...k......"],
                          dict(D="64442C", q="D6B078", p="AA8254", E="D6D2C8", S="A4A098", s="6E6A64", k="30281F", g="34845A", G="1E5438")),
    "sailors_hatchet": (["..DqD.....", "..DqkEEk..", "..DqESSSEk", "..DqESSSSs", "..DqESSSSs", "..DqESSSSs", "..DqESSSEk", "..DqkSSsk.",
                         "..DqDkkk..", "..DpD.....", "..DqD.....", "..bbb.....", "..DqD.....", "..DpD.....", "..DqD.....", "..bbb.....",
                         "..DpD.....", "..DqD.....", "..DpD.....", "..DqD.....", "..kDk.....", "...k......"],
                        dict(D="54322A", q="CE8C5C", p="965C3C", b="4670B4", E="D0D8E4", S="96A4B8", s="68748C", k="343C50")),
    "kraken_cleaver": (["..DqD......", "..DqkvvUk..", "..DqvvuuUk.", "..DqvuuuUUk", "..DqvuuuUUk", "..DqkuuuUVk", "..DqkuuUVVk", "..DqDkuUVk.",
                        "..DpDkUVk..", "..DqD.kk...", "..DpD......", "..DqD......", "..DqD......", "..DpD......", "..DqD......", "..DpD......",
                        "..DqD......", "..DpD......", "..DqD......", ".kDqDk.....", ".kDpDk.....", "..kkk......"],
                       dict(D="2E1C48", q="8E64C8", p="5E3C94", v="BE96FF", u="8E5CD2", U="583696", V="341E60", k="1A0E30")),
}


class ArtSprite:
    """Senjata di tangan hasil gambar tangan (HELD_ART), posisi tegak (ujung di atas, gagang di bawah)."""

    def __init__(self, rows, palette):
        width = max(len(r) for r in rows)
        rows = [r.ljust(width, ".") for r in rows]
        assert len(rows) == 22, "senjata harus 22 baris, dapat %d" % len(rows)
        pal = {k: _rgb(v) for k, v in palette.items()}
        px = np.zeros((len(rows), width, 4), np.uint8)
        for y, r in enumerate(rows):
            for x, ch in enumerate(r):
                if ch != ".":
                    px[y, x] = (*pal[ch], 255)
        self.px = px
        self.palette = np.unique(px[px[..., 3] > 0][:, :3], axis=0)
        m = px[..., 3] > 0
        ys, xs = np.nonzero(m)
        h, w = m.shape
        low = ys >= h * 0.6
        self.axis_x = float(np.median(xs[low])) + 0.5          # garis gagang (di sprite tegak)
        far = ys < h * 0.5
        self.skew = float(xs[far].mean() + 0.5 - self.axis_x) if far.any() else 0.0
        self.length = h

    def placed(self, center_xy, u, flip, size=64):
        """Putar (rotsprite) dan taruh sprite; return (rgb, mask) ukuran size x size."""
        sp = self.px[:, ::-1].copy() if flip else self.px
        h, w = sp.shape[:2]
        ax = (w - self.axis_x) if flip else self.axis_x
        big = np.repeat(np.repeat(sp, SS, 0), SS, 1)
        theta = math.atan2(u[1], u[0]) + math.pi / 2
        c, s = math.cos(theta), math.sin(theta)
        R = np.array([[c, -s], [s, c]])
        src_c = np.array([ax, h / 2.0]) * SS
        dst_c = np.array(center_xy, float) * SS
        A = np.zeros((2, 3))
        A[:, :2] = R
        A[:, 2] = dst_c - R @ src_c
        S = size * SS
        pm = big.astype(np.float32)
        pm[..., :3] *= pm[..., 3:4] / 255.0
        warped = cv2.warpAffine(pm, A, (S, S), flags=cv2.INTER_NEAREST, borderValue=(0, 0, 0, 0))
        small = cv2.resize(warped, (size, size), interpolation=cv2.INTER_AREA)
        al = small[..., 3] / 255.0
        mask = al >= 0.5
        rgb = np.where(al[..., None] > 0.01, small[..., :3] / np.maximum(al[..., None], 1e-3), 0)
        d = ((rgb[:, :, None, :] - self.palette[None, None, :, :].astype(np.float32)) ** 2).sum(3)       # kunci ke palet sprite
        rgb = self.palette[d.argmin(2)]
        return rgb, mask


def analyze_all(wtype):
    info = {}
    for st in (1, 2, 3, 4):
        for d in DIRS:
            for i in range(4):
                fr = load_base(wtype, st, d, i)
                eff, wep, tail, tip = split_frame(fr, wtype)
                pose = pose_from_pixels(wep, tail) if wep.sum() >= 4 else None
                info[(st, d, i)] = dict(frame=fr, eff=eff, wep=wep, pose=pose, ntip=int(tip.sum()))
    Ls = [v["pose"]["t_hi"] - v["pose"]["t_lo"] for v in info.values() if v["pose"]]
    L_nom = float(np.percentile(Ls, 80))                # panjang senjata bawaan yang utuh (gak ketutup efek)
    rs = [(v["pose"]["t_anchor"] - v["pose"]["t_lo"]) / (v["pose"]["t_hi"] - v["pose"]["t_lo"])
          for v in info.values()
          if v["pose"] and v["pose"]["t_anchor"] is not None and (v["pose"]["t_hi"] - v["pose"]["t_lo"]) >= 0.93 * L_nom]
    return info, L_nom, float(np.median(rs))


def compose_frame(v, spr, anchor, r_tail, allow_flip, flip_hint, margin=1):
    a = np.array(v["frame"])
    h, w = a.shape[:2]
    eff, wep, base = v["eff"], v["wep"], v["pose"]
    if not allow_flip:
        flip = False
    elif flip_hint is not None:
        flip = flip_hint
    else:
        flip = bool(np.sign(spr.skew) != np.sign(base["skew"]))
    L = float(spr.length)
    t_mid = anchor - r_tail * L + L / 2.0                 # gagang tetap di tangan, ujungnya yang memanjang
    center = base["m"] + base["u"] * t_mid
    rgb, m = spr.placed(center, base["u"], flip, w)
    ys, xs = np.nonzero(m)
    if len(xs):                                           # jangan sampai kepotong tepi frame
        dx = dy = 0
        if xs.min() < margin: dx = margin - xs.min()
        if xs.max() > w - 1 - margin: dx = (w - 1 - margin) - xs.max()
        if ys.min() < margin: dy = margin - ys.min()
        if ys.max() > h - 1 - margin: dy = (h - 1 - margin) - ys.max()
        if dx or dy:
            rgb, m = spr.placed(center + np.array([dx, dy]), base["u"], flip, w)
    out = np.zeros_like(a)
    out[eff] = a[eff]
    hole = wep & ~m                                       # tambal efek yang dulu ketutup senjata lama
    effm = eff.copy()
    for _ in range(2):
        nb = ndi.convolve(effm.astype(int), np.ones((3, 3), int), mode="constant") - effm.astype(int)
        effm = effm | (hole & ~effm & (nb >= 3))
    out[effm & ~eff] = (ARC[0], ARC[1], ARC[2], 255)
    out[m] = np.concatenate([rgb[m], np.full((int(m.sum()), 1), 255)], 1).astype(np.uint8)
    return Image.fromarray(out, "RGBA"), flip


def build_held_sheet(spr, wtype):
    allow_flip = (wtype == "Axe")
    info, L_nom, r_tail = analyze_all(wtype)
    sheet = Image.new("RGBA", (16 * 64, 4 * 64), (0, 0, 0, 0))
    for st in (1, 2, 3, 4):
        for r, d in enumerate(DIRS):
            last_flip = None
            for i in range(4):
                v = info[(st, d, i)]
                p = v["pose"]
                if p is None:
                    frame = v["frame"]
                else:
                    anchor = p["t_anchor"] if p["t_anchor"] is not None else p["t_lo"] + r_tail * (p["t_hi"] - p["t_lo"])
                    hint = last_flip if (v["ntip"] < 20 and last_flip is not None) else None   # kepala kapak gak kelihatan: ikut frame sebelumnya
                    frame, used = compose_frame(v, spr, anchor, r_tail, allow_flip, hint)
                    if v["ntip"] >= 20:
                        last_flip = used
                sheet.paste(frame, (((st - 1) * 4 + i) * 64, r * 64))
    return sheet


# ----------------------------------------------------------------------------------------------
def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    sheet_path = sys.argv[1]
    rest = sys.argv[2:]
    what = next((a for a in rest if a in ("icons", "held")), "all")
    preview_path = next((a for a in rest if a.lower().endswith(".png")), None)

    crops = slice_sheet(sheet_path)
    os.makedirs(HELD_DIR, exist_ok=True)
    icons = {}
    if what in ("all", "icons"):
        for slug, rgba in crops.items():
            icons[slug] = make_icon(slug, rgba)
            icons[slug].save(os.path.join(ICON_DIR, slug + ".png"))
        print("ikon:", len(icons), "->", ICON_DIR)
    if what in ("all", "held"):
        for slug, wtype in WEAPONS.items():
            build_held_sheet(ArtSprite(*HELD_ART[slug]), wtype).save(os.path.join(HELD_DIR, slug + ".png"), optimize=True)
            print("  senjata di tangan:", slug, "(%s)" % wtype)
        print("held ->", HELD_DIR)
    if preview_path and icons:
        cols, Z, ss = 9, 3, 52                     # pratinjau = ikon 40px di slot backpack 52px (seperti di game), diperbesar 3x
        rows = (len(icons) + cols - 1) // cols
        P = Image.new("RGBA", (cols * (ss * Z + 4) + 4, rows * (ss * Z + 4) + 4), (25, 25, 32, 255))
        for k, ic in enumerate(icons.values()):
            slot = Image.new("RGBA", (ss, ss), (54, 50, 66, 255))
            small = ic.resize((40, 40), Image.NEAREST)
            slot.paste(small, (6, 6), small)
            P.paste(slot.resize((ss * Z, ss * Z), Image.NEAREST), (4 + (k % cols) * (ss * Z + 4), 4 + (k // cols) * (ss * Z + 4)))
        P.save(preview_path)
        print("pratinjau ->", preview_path)


if __name__ == "__main__":
    main()
