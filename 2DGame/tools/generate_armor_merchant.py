#!/usr/bin/env python3
"""
Generator sprite NPC "Armor Merchant".

Belum ada sprite khusus, jadi dibuat dari 8 frame idle NPC penjual yang sudah ada (NPCs/WeaponDealerNPC00x.png)
dengan mengganti warna rambut (biru -> coklat tembaga) dan jubah (hijau -> merah tua), supaya kelihatan beda
sama Potion Merchant yang masih pakai sprite yang sama.

Jalankan dari folder 2DGame:
    pip install pillow
    python tools/generate_armor_merchant.py

Output: src/NPCs/ArmorMerchantNPC000.png .. ArmorMerchantNPC007.png
Nanti kalau sudah punya sprite sendiri, tinggal timpa 8 file itu (nama & jumlah frame sama).
"""
from pathlib import Path

from PIL import Image

NPCS = Path(__file__).resolve().parent.parent / 'src' / 'NPCs'
FRAMES = 8

RECOLOR = {
    # rambut: biru -> coklat tembaga
    (169, 228, 243): (196, 128, 82),
    (130, 192, 221): (160, 98, 60),
    (99, 156, 199):  (124, 72, 46),
    (90, 132, 188):  (98, 56, 38),
    # jubah: hijau -> merah tua
    (100, 171, 121): (176, 64, 56),
    (89, 152, 107):  (160, 58, 52),
    (59, 103, 103):  (112, 38, 44),
}


def main():
    for i in range(FRAMES):
        im = Image.open(NPCS / f'WeaponDealerNPC00{i}.png').convert('RGBA')
        px = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                if a and (r, g, b) in RECOLOR:
                    nr, ng, nb = RECOLOR[(r, g, b)]
                    px[x, y] = (nr, ng, nb, a)
        im.save(NPCS / f'ArmorMerchantNPC00{i}.png')
    print(f'{FRAMES} frame ditulis ke {NPCS}')


if __name__ == '__main__':
    main()
