#!/usr/bin/env python3
"""
Generator peta collision domain GolemHide (golemHide_walk.png).

Gambar golemHide.png itu gua yang bentuknya gak kotak-kotak, jadi collision pakai Rectangle gak bakal pas.
Script ini baca gambarnya, cari lantai tanah (warna coklat) lalu bikin mask hitam-putih:
    putih (255) = lantai, boleh dilewati player & monster
    hitam (0)   = tembok batu / rangka kayu / peti / di luar gambar

Ukuran mask = ukuran interior domain di DomainManager (800x600), jadi 1 piksel mask = 2 px dunia
(gambar background digambar 2x). Mask ini bisa di-edit manual pakai editor gambar apa aja
(cat putih = bisa dilewati, cat hitam = tembok), tinggal simpan dengan nama & ukuran yang sama.

Jalankan dari folder 2DGame:
    pip install pillow numpy scipy opencv-python
    python tools/generate_golem_mask.py
    python tools/generate_golem_mask.py --preview preview.png   # (opsional) mask ditumpuk ke gambar buat dicek

Output: src/latar/golemHide_walk.png
"""
import argparse
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

LATAR = Path(__file__).resolve().parent.parent / 'src' / 'latar'
SRC = LATAR / 'golemHide.png'
OUT = LATAR / 'golemHide_walk.png'

MASK_W, MASK_H = 800, 600          # = GH_W x GH_H di DomainManager
MAX_STONE_AREA = 700               # batu kecil di tengah lantai (dalam px gambar) dianggap bisa dilewati

# Koordinat di bawah dalam piksel golemHide.png asli (1447x1087): (x0, y0, x1, y1)
# Rangka kayu & peti warnanya sama kayak tanah, jadi gak kedeteksi otomatis -> dikasih tembok manual.
SOLID_RECTS = [
    (598, 70, 638, 190),    # tiang kiri pintu tambang atas
    (711, 70, 751, 190),    # tiang kanan pintu tambang atas
    (598, 70, 751, 112),    # balok atas pintu tambang
    (1110, 352, 1196, 447), # rangka pintu tambang kanan
    (1110, 447, 1136, 486), # kaki rangka kanan
    (1062, 436, 1122, 498), # tumpukan peti
]
# Bagian dalam terowongan atas gelap (kedeteksi tembok), padahal itu jalan ke titik keluar domain.
# Batas atas (y=130) sengaja agak turun dari balok kayu supaya kepala karakter gak nembus balok.
WALK_RECTS = [
    (638, 130, 711, 192),
]


def build_floor(rgba):
    rgb = rgba[..., :3]
    solid = rgba[..., 3] > 128
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV_FULL).astype(np.float32)
    hue, sat, val = hsv[..., 0] * 360 / 255, hsv[..., 1] / 255, hsv[..., 2] / 255
    dirt = (solid & (hue >= 18) & (hue <= 46) & (sat >= 0.28) & (sat <= 0.75)
            & (val >= 0.30) & (val <= 0.90))

    m = dirt.astype(np.uint8)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)))
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (15, 15)))

    lab, n = ndi.label(m)
    sizes = ndi.sum(m, lab, range(1, n + 1))
    floor = lab == (int(np.argmax(sizes)) + 1)          # cuma lantai utama, bercak tanah terpisah dibuang

    # batu kecil yang "bolong" di tengah lantai -> isi, biar gak nyangkut di kerikil
    holes, hn = ndi.label(~floor)
    h, w = floor.shape
    for i in range(1, hn + 1):
        comp = holes == i
        ys, xs = np.nonzero(comp)
        touches_border = ys.min() == 0 or xs.min() == 0 or ys.max() == h - 1 or xs.max() == w - 1
        if not touches_border and comp.sum() < MAX_STONE_AREA:
            floor[comp] = True
    return floor


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--preview', help='simpan gambar preview (mask ditumpuk ke background) ke path ini')
    args = ap.parse_args()

    img = Image.open(SRC).convert('RGBA')
    rgba = np.array(img)
    floor = build_floor(rgba)

    for x0, y0, x1, y1 in SOLID_RECTS:
        floor[y0:y1, x0:x1] = False
    for x0, y0, x1, y1 in WALK_RECTS:
        floor[y0:y1, x0:x1] = True

    # kecilin ke ukuran interior domain (1 piksel = 2 px dunia), >= 50% lantai baru dianggap lantai
    small = cv2.resize(floor.astype(np.float32), (MASK_W, MASK_H), interpolation=cv2.INTER_AREA) >= 0.5
    lab, n = ndi.label(small)
    sizes = ndi.sum(small, lab, range(1, n + 1))
    small = lab == (int(np.argmax(sizes)) + 1)

    Image.fromarray((small * 255).astype(np.uint8), 'L').save(OUT)
    print(f'ditulis: {OUT} ({MASK_W}x{MASK_H}, lantai = {small.mean() * 100:.1f}% area)')

    if args.preview:
        big = cv2.resize(small.astype(np.uint8), (img.width, img.height), interpolation=cv2.INTER_NEAREST) > 0
        base = np.full((img.height, img.width, 3), 255, np.uint8)
        art = rgba[..., 3] > 128
        base[art] = rgba[..., :3][art]
        base[big] = (0.5 * base[big] + 0.5 * np.array([0, 255, 0])).astype(np.uint8)
        base[~big & art] = (0.6 * base[~big & art] + 0.4 * np.array([255, 0, 0])).astype(np.uint8)
        Image.fromarray(base).save(args.preview)
        print(f'preview: {args.preview}')


if __name__ == '__main__':
    main()
