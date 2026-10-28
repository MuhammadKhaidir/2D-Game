#!/usr/bin/env python3
"""
Generator sprite "Iron Armor" (set armor kedua: besi polos, full iron).

Cara kerjanya SAMA kayak tools/generate_dread_armor.py (file itu dipakai buat urutan frame, segmentasi kepala
dan helper-nya), cuma paletnya baja/besi dan helmnya polos tanpa tanduk:
  body_* : baju dasar (PlayerCloth) di-recolor jadi pelat besi; kulit yang kelihatan (tangan, leher) -> sarung tangan / gorget besi
  head_* : helm tertutup (bentuk knight: jambul/garis tengah, pita dahi, 2 paku, celah visor bentuk T) dari siluet kepala

Jalankan dari folder 2DGame:
    pip install pillow numpy scipy
    python tools/generate_iron_armor.py

Output: src/Aset/armor/iron/{body|head}_<state>_<arah>_<nomor>.png  + src/Aset/IronArmor.png (ikon)
Mau ganti warna? Edit blok PALETTE di bawah lalu jalankan ulang.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_dread_armor import ASET, states, load, head_mask, px, _nb, _eye_template  # noqa: E402

OUT = ASET / 'armor' / 'iron'


# ───────────── PALETTE ─────────────
def rgb(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


OUTLINE = rgb('181818')   # outline yang sama kayak sprite body/baju di game
VISOR   = rgb('0b0d10')   # celah mata helm
GLINT   = rgb('9fc0d8')   # kilau dingin di celah visor

# pelat besi: shadow, mid, light, specular
PLATE  = [rgb('3b424b'), rgb('6a7480'), rgb('9aa5b1'), rgb('d6dde3')]
# bahu / lengan: sedikit lebih gelap dari dada supaya dada kebaca sebagai pelat utama
ARM    = [rgb('2d333a'), rgb('56606b'), rgb('808b97'), rgb('b9c3cc')]
# kulit coklat (sabuk)
LEATH  = [rgb('33261c'), rgb('54402f'), rgb('775a40')]
# perunggu (kancing / gesper)
BRONZE = [rgb('6a4a28'), rgb('a4733a'), rgb('dcaa5e')]

CLOTH_MAP = {
    (0x71, 0x65, 0x58): PLATE[0],    # khaki shadow   -> pelat shadow
    (0xa6, 0x98, 0x78): PLATE[1],    # khaki mid      -> pelat mid
    (0xc7, 0xc4, 0xa2): PLATE[2],    # beige light    -> pelat light
    (0x5b, 0x18, 0x39): ARM[0],      # merah tua      -> kerah / bawah lengan
    (0x8d, 0x22, 0x46): ARM[1],      # merah mid      -> pelat bahu / lengan
    (0xbe, 0x56, 0x67): ARM[2],      # merah terang   -> sorot di bahu
    (0x1e, 0x2b, 0x41): LEATH[1],    # biru tua       -> sabuk kulit
    (0x3c, 0x55, 0x75): BRONZE[2],   # biru mid       -> kancing / gesper perunggu
}
SKIN_MAP = {   # kulit yang gak ketutup baju (tangan, leher) -> sarung tangan / gorget besi
    (0x88, 0x58, 0x48): ARM[0],
    (0xd8, 0x98, 0x78): ARM[1],
    (0xf8, 0xd8, 0xb8): ARM[2],
}


# ───────────── LAYER BADAN ─────────────
def body_layer(body, cloth):
    B = np.array(body)
    C = np.array(cloth)
    out = np.zeros((64, 64, 4), np.uint8)
    cloth_a = C[..., 3] > 0
    out[cloth_a] = PLATE[1]                       # jaga-jaga: warna baju yang gak ada di CLOTH_MAP tetap jadi pelat
    for src, dst in CLOTH_MAP.items():
        out[px(C, src) & cloth_a] = dst
    out[px(C, (0x18, 0x18, 0x18)) & cloth_a] = OUTLINE
    h, other = head_mask(body, cloth)
    for src, dst in SKIN_MAP.items():
        out[other & px(B, src)] = dst
    return out, h


def bulk_torso(out, h, state, direction):
    """Badan harus >= kepala: baris dada yang lebih sempit dari helm (lebar kepala + outline) dilebarin pakai pelat bahu.
    Tampak depan/belakang: bahu minimal 1 px lebih lebar dari helm; samping: dada/punggung dilebarin sampai selebar helm."""
    if state == 'dead' or not h.any():
        return out
    ys, xs = np.where(h)
    ybot = ys.max()
    head_w = (xs.max() - xs.min() + 1) + 2
    target = head_w + (2 if direction in ('down', 'up') else 0)
    added = np.zeros(out.shape[:2], bool)
    for y in range(ybot + 2, ybot + 7):
        row = np.where(out[y, :, 3] > 0)[0]
        if not len(row):
            continue
        a, b = row.min(), row.max()
        need = target - (b - a + 1)
        if need <= 0:
            continue
        kl = need // 2
        kr = need - kl
        for side, k, col in ((-1, kl, ARM[2]), (1, kr, ARM[0])):
            if k <= 0:
                continue
            edge = a if side < 0 else b
            if tuple(out[y, edge]) == tuple(OUTLINE):
                out[y, edge] = ARM[1]
            for j in range(1, k + 1):
                x = edge + side * j
                if 0 <= x < 64:
                    out[y, x] = col if j == k else ARM[1]
                    added[y, x] = True
    ring = (_nb(added, -1, 0) | _nb(added, 1, 0) | _nb(added, 0, -1) | _nb(added, 0, 1)) & ~added & (out[..., 3] == 0)
    out[ring] = OUTLINE
    return out


# ───────────── HELM ─────────────
def helm_layer(body, cloth, direction, state, body_lay):
    B = np.array(body)
    h, _ = head_mask(body, cloth)
    out = np.zeros((64, 64, 4), np.uint8)
    if not h.any():
        return out
    ys, xs = np.where(h)
    ytop, ybot, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
    H = ybot - ytop + 1
    W = x1 - x0 + 1
    shape = h.copy()

    # ---- shading pelat (terang di kiri-atas, tepi bawah/kanan digelapin) ----
    up, lf = ~_nb(h, -1, 0), ~_nb(h, 0, -1)
    dn, rt = ~_nb(h, 1, 0), ~_nb(h, 0, 1)
    for y, x in zip(ys, xs):
        ty = (y - ytop) / max(1, H - 1)
        tx = (x - x0) / max(1, W - 1)
        L = 1.0 - 0.65 * ty - 0.30 * tx
        t = 1
        if L > 0.66:
            t = 2
        elif L < 0.22:
            t = 0
        if up[y, x] or lf[y, x]:
            t = min(3, t + 1) if t < 2 else 2
        if dn[y, x] or rt[y, x]:
            t = max(0, t - 1)
        out[y, x] = PLATE[t]
    for dx in (2, 3):
        yy, xx = ytop + 1, x0 + dx
        if h[yy, xx]:
            out[yy, xx] = PLATE[3]

    # ---- helm knight: pita dahi + visor T + paku (acuan: gambar helm baja) ----
    cx = (x0 + x1) / 2.0
    white = (B[..., 0] == 0xf8) & (B[..., 1] == 0xf8) & (B[..., 2] == 0xf8) & h
    wy, wx = np.where(white)
    if not len(wy) and direction != 'up' and state != 'dead':
        t = _eye_template(direction)
        wy = np.array([ytop + dy for dy, dx in t])
        wx = np.array([x0 + dx for dy, dx in t])

    def paint(y, x, col):
        if 0 <= y < 64 and 0 <= x < 64 and h[y, x]:
            out[y, x] = col

    if len(wy):                                          # tampak depan / samping (ada wajah)
        ey = int(np.round(np.median(wy)))
        band, wing = ey - 3, (ey - 2, ey - 1)           # pita dahi, lalu 2 baris sayap visor
        for y, x in zip(ys, xs):
            if direction in ('down', 'up'):
                d = int(abs(x - cx))                    # 0 = tengah
                edge_l, edge_r = (x == x0), (x == x1)
            else:
                d = (x - x0) if direction == 'left' else (x1 - x)   # 0 = ujung depan wajah
                edge_l = edge_r = False
            if y == band:                                # pita dahi terang
                out[y, x] = PLATE[2] if (x in (x0, x1)) else PLATE[3]
            elif y > band:                               # pipi / pelindung dagu
                t = 1
                if direction in ('down', 'up'):
                    t = 2 if edge_l else (0 if edge_r else 1)
                if dn[y, x]:
                    t = 0
                out[y, x] = PLATE[t]
        if direction in ('down', 'up'):
            for y in range(band + 1, ybot + 1):          # batang hidung (tengah) + celah T
                for x in range(x0, x1 + 1):
                    d = int(abs(x - cx))
                    if y in wing:
                        if 1 <= d <= 3: paint(y, x, VISOR)
                        elif d == 0:    paint(y, x, PLATE[2])
                    elif y == ey:
                        if d <= 1:      paint(y, x, VISOR)
                    elif y > ey:
                        if d == 0:      paint(y, x, VISOR)
            for y in range(ytop, band):                  # garis tengah dari jambul sampai pita
                for x in range(x0, x1 + 1):
                    if int(abs(x - cx)) == 0:
                        paint(y, x, PLATE[3])
            paint(ytop + 3, x0 + 3, PLATE[3])            # 2 paku di dahi
            paint(ytop + 3, x1 - 3, PLATE[3])
        else:
            front = (lambda x: x - x0) if direction == 'left' else (lambda x: x1 - x)
            for y in range(band + 1, ybot + 1):
                for x in range(x0, x1 + 1):
                    d = front(x)
                    if y in wing and d <= 6:    paint(y, x, VISOR)
                    elif y >= ey and d <= (2 if y == ey else 1): paint(y, x, VISOR)
            for x in range(x0, x1 + 1):                  # rabung atas helm dilihat dari samping
                paint(ytop, x, PLATE[3])
            paint(ytop + 3, (x0 + 5) if direction == 'left' else (x1 - 5), PLATE[3])   # paku
    else:                                                # tampak belakang / mati: punggung helm
        xc = int(cx)
        for y in range(ytop, ybot + 1):
            for x in (xc, xc + 1):
                paint(y, x, PLATE[3] if y < ytop + 5 else PLATE[2])
        if ytop + 5 <= ybot:
            for x in range(x0, x1 + 1):
                paint(ytop + 5, x, PLATE[2] if x not in (xc, xc + 1) else PLATE[3])
        for y in range(ybot - 2, ybot + 1):              # pelindung tengkuk lebih gelap
            for x in range(x0, x1 + 1):
                paint(y, x, PLATE[0])

    # ---- tonjolan kecil di puncak helm (1 baris saja, tidak menambah tinggi kepala) ----
    crest = np.zeros((64, 64), bool)
    if state != 'dead':
        xm = int(cx)
        xs_c = (xm, xm + 1) if direction in ('down', 'up') else (xm - 1, xm, xm + 1)
        for x in xs_c:
            if 0 <= x < 64 and not h[ytop - 1, x]:
                out[ytop - 1, x] = PLATE[3]
                crest[ytop - 1, x] = True
    shape |= crest

    # ---- ring outline ----
    ring = (_nb(shape, -1, 0) | _nb(shape, 1, 0) | _nb(shape, 0, -1) | _nb(shape, 0, 1)) & ~shape
    body_a = body_lay[..., 3] > 0
    ok = ring & ~body_a & ((B[..., 3] == 0) | ((B[..., 0] == 0x18) & (B[..., 1] == 0x18)))
    out[ok] = OUTLINE
    return out


def full_layers(body, cloth, direction, state):
    bl, h = body_layer(body, cloth)
    hl = helm_layer(body, cloth, direction, state, bl)
    bl = bulk_torso(bl, h, state, direction)
    return bl, hl


# ───────────── ICON ─────────────
def make_icon(size=80, scale=2):
    """Ikon inventory / toko: set armor tampak depan (helm + dada + sepatu), pixel art 2x."""
    B = load('FrontIdle1.png')
    C = load('ClothFrontIdle.png')
    bl, hl = full_layers(B, C, 'down', 'idle')
    layer = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    layer.alpha_composite(Image.fromarray(bl))
    layer.alpha_composite(Image.fromarray(hl))
    bb = layer.getbbox()
    crop = layer.crop(bb).resize(((bb[2] - bb[0]) * scale, (bb[3] - bb[1]) * scale), Image.NEAREST)
    icon = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    icon.alpha_composite(crop, ((size - crop.width) // 2, (size - crop.height) // 2))
    return icon


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    for st, d, i, bfile, cfile in states():
        body, cloth = load(bfile), load(cfile)
        bl, hl = full_layers(body, cloth, d, st)
        Image.fromarray(bl).save(OUT / f'body_{st}_{d}_{i + 1}.png')
        Image.fromarray(hl).save(OUT / f'head_{st}_{d}_{i + 1}.png')
        n += 2
    make_icon().save(ASET / 'IronArmor.png')
    print(f'{n} frame + ikon ditulis ke {OUT}')


if __name__ == '__main__':
    main()
