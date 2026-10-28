#!/usr/bin/env python3
"""
Generator sprite "Iron Armor" (set armor kedua: besi polos, full iron).

Cara kerjanya SAMA kayak tools/generate_dread_armor.py (file itu dipakai buat urutan frame, segmentasi kepala
dan helper-nya), cuma paletnya baja/besi dan helmnya polos tanpa tanduk:
  body_* : baju dasar (PlayerCloth) di-recolor jadi pelat besi; kulit yang kelihatan (tangan, leher) -> sarung tangan / gorget besi
  head_* : helm tertutup dari siluet kepala, lengkap dengan celah visor dan jambul kecil di atas

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

    # ---- mata / visor ----
    white = (B[..., 0] == 0xf8) & (B[..., 1] == 0xf8) & (B[..., 2] == 0xf8) & h
    wy, wx = np.where(white)
    if not len(wy) and direction != 'up' and state != 'dead':
        t = _eye_template(direction)
        wy = np.array([ytop + dy for dy, dx in t])
        wx = np.array([x0 + dx for dy, dx in t])
    if len(wy):
        ey = int(np.round(np.median(wy)))
        if direction == 'left':
            xa, xb = x0, min(x1, wx.max() + 2)
        elif direction == 'right':
            xa, xb = max(x0, wx.min() - 2), x1
        else:
            xa, xb = x0 + 1, x1 - 1
        rows = (ey - 1, ey, ey + 1) if direction in ('down', 'up') else (ey - 1, ey)
        for y in rows:
            for x in range(xa, xb + 1):
                if 0 <= y < 64 and h[y, x]:
                    out[y, x] = VISOR
        # wajah bawah: pelat pelindung dagu (tone mid) dengan garis tengah di tampak depan
        for y in range(rows[-1] + 1, ybot + 1):
            for x in range(x0, x1 + 1):
                if h[y, x]:
                    t = 0 if (y >= ybot or dn[y, x] or rt[y, x]) else 1
                    out[y, x] = PLATE[t]
        if direction == 'down':
            xc = (x0 + x1) // 2
            for y in range(rows[-1] + 2, ybot):
                if h[y, xc]:
                    out[y, xc] = PLATE[0]
        for y, x in zip(wy, wx):
            out[y, x] = GLINT
    else:
        xc = (x0 + x1) // 2
        for y in range(ytop + 1, ytop + H - 3):          # tampak belakang / mati: punggung helm dengan sorot tengah
            for x in (xc, xc + 1):
                if h[y, x]:
                    out[y, x] = PLATE[3] if y < ytop + 4 else PLATE[2]
        for y in range(ybot - 2, ybot + 1):              # pelindung tengkuk lebih gelap
            for x in range(x0, x1 + 1):
                if h[y, x]:
                    out[y, x] = PLATE[0]

    # ---- jambul kecil di atas helm (bukan tanduk, cuma tonjolan pelat) ----
    crest = np.zeros((64, 64), bool)
    if state != 'dead':
        def put(x, y, col):
            if 0 <= x < 64 and 0 <= y < 64 and not h[y, x]:
                out[y, x] = col
                crest[y, x] = True
        row = np.where(h[ytop])[0]
        cl, cr = int(row.min()), int(row.max())
        for x in range(cl + 1, cr):                      # alur dasar jambul
            put(x, ytop - 1, PLATE[2] if x < (cl + cr) // 2 else PLATE[1])
        xm = (cl + cr) // 2
        for x in (xm, xm + 1) if direction in ('down', 'up') else (xm - 1, xm, xm + 1):
            put(x, ytop - 2, PLATE[3])                   # puncak jambul + kilau
    shape |= crest

    # ---- ring outline ----
    ring = (_nb(shape, -1, 0) | _nb(shape, 1, 0) | _nb(shape, 0, -1) | _nb(shape, 0, 1)) & ~shape
    body_a = body_lay[..., 3] > 0
    ok = ring & ~body_a & ((B[..., 3] == 0) | ((B[..., 0] == 0x18) & (B[..., 1] == 0x18)))
    out[ok] = OUTLINE
    return out


def full_layers(body, cloth, direction, state):
    bl, _ = body_layer(body, cloth)
    hl = helm_layer(body, cloth, direction, state, bl)
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
