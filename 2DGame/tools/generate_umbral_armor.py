#!/usr/bin/env python3
"""
Generator sprite "Umbral Knight" (set armor gelap ketiga: baja arang + mantel merah tua + jambul merah, mata merah menyala).

Armor = SATU set utuh. Tiap frame animasi karakter dapat 2 layer PNG (64x64, sama kayak sprite body/baju):
  body_*  : badan (menggantikan baju dasar PlayerCloth) - dada, bahu, lengan, sarung tangan, celana, sepatu
  head_*  : helm + jambul (digambar PALING ATAS, setelah senjata)

Sprite dibuat dari sprite yang SUDAH ADA di game (badan + baju dasar), jadi pas di semua pose:
  - layer badan  = baju dasar (BaseCloth*/Cloth*) di-recolor: khaki -> pelat baja, merah -> mantel, biru -> sabuk
  - kulit yang kelihatan (tangan, leher) -> sarung tangan / gorget
  - layer kepala = siluet kepala dari sprite body -> helm tertutup dengan visor, mata merah, jambul

Jalankan dari folder 2DGame:
    pip install pillow numpy
    python tools/generate_umbral_armor.py

Output: src/Aset/armor/umbral/{body|head}_<state>_<arah>_<nomor>.png  + src/Aset/UmbralArmor.png (ikon)
  state : idle walk run jump atk1 atk2 atk3 atk4 dead
  arah  : down up left right        nomor: mulai dari 1
Mau ganti warna? Edit blok PALETTE di bawah lalu jalankan ulang.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASET = ROOT / 'src' / 'Aset'
OUT = ASET / 'armor' / 'umbral'

# ───────────── pasangan frame body <-> baju (sama persis kayak Entitas/player.java) ─────────────
DIRS = {'down': 'Front', 'up': 'Back', 'left': 'Left', 'right': 'Right'}


def states():
    """yield (state, dir, idx, body_file, cloth_file)"""
    out = []
    for d, D in DIRS.items():
        out.append(('idle', d, 0, f'{D}Idle1.png', f'Cloth{D}Idle.png'))
        for i in range(1, 7):
            out.append(('walk', d, i - 1, f'{D}Walk{i}.png', f'Cloth{D}Walk ({i}).png'))
        for i in range(1, 4):
            out.append(('run', d, i - 1, f'{D}Run{i}.png', f'Cloth{D}Run ({i}).png'))
        for j in range(2):          # index jump BODY j berpasangan dengan index baju (j==0 ? 1 : 0)
            c = 1 if j == 0 else 0
            out.append(('jump', d, j, f'Jump{D}{j + 1}.png', f'Cloth{D}Jump ({c + 1}).png'))
        for i in range(1, 5):
            out.append(('atk1', d, i - 1, f'{D}Attack ({i}).png', f'BaseCloth{D}2 ({i}).png'))
            out.append(('atk2', d, i - 1, f'{D}Swing ({i}).png', f'BaseCloth{D}2 ({i + 4}).png'))
            out.append(('atk3', d, i - 1, f'{D} ({i}).png', f'BaseCloth{D} ({i}).png'))
            out.append(('atk4', d, i - 1, f'{D} ({i + 4}).png', f'BaseCloth{D} ({i + 4}).png'))
        for i in range(1, 4):
            out.append(('dead', d, i - 1, f'Char{D}Dead ({i}).png', f'CharDead{D}Cloth ({i}).png'))
    return out


def load64(fn):
    """RGBA 64x64 (frame lompat 128px = 64px yang digandakan, jadi diperkecil lagi tanpa blur)."""
    im = Image.open(ASET / fn).convert('RGBA')
    if im.size != (64, 64):
        im = im.resize((64, 64), Image.NEAREST)
    return np.array(im)


# ───────────── warna sumber (palet sprite asli) ─────────────
SKIN = {(0xf8, 0xd8, 0xb8): 2, (0xd8, 0x98, 0x78): 1, (0x88, 0x58, 0x48): 0}
KHAKI = {(0xc7, 0xc4, 0xa2): 2, (0xa6, 0x98, 0x78): 1, (0x71, 0x65, 0x58): 0}
RED = {(0xbe, 0x56, 0x67): 2, (0x8d, 0x22, 0x46): 1, (0x5b, 0x18, 0x39): 0}
NAVY = {(0x3c, 0x55, 0x75): 1, (0x1e, 0x2b, 0x41): 0}


# ───────────── PALETTE ─────────────
def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


STEEL = [H('1f1f26'), H('393941'), H('55555f')]          # pelat dada / kaki (baja arang, netral)
SPEC = H('9b9ba8')                                       # kilap tepi logam
GAUNT = [H('17171d'), H('2a2a32'), H('42424d')]          # sarung tangan / leher
MANTLE = [H('3d0c1c'), H('7a1a32'), H('c2354f')]         # bahu / mantel / jambul merah (gelap, tengah, terang)
BELT = [H('241b12'), H('5a4122'), H('a47b34')]           # sabuk perunggu
HELM = [H('1a1a21'), H('32323b'), H('50505b')]
HELM_EDGE = H('85858f')
VISOR = H('07070b')
EYE_HOT, EYE_CORE = H('ff7a52'), H('e8221f')
OUTLINE = H('0c0c10')
GEM = [H('7a1230'), H('d63a5c'), H('ff9aa8')]

# jambul: (dx dari tengah kepala, dy ke atas dari baris teratas helm, nada MANTLE 0=gelap 1=tengah 2=terang)
PLUME_FB = [(-1, 0, 1), (0, 0, 2), (1, 0, 1), (2, 0, 0),
            (-1, 1, 1), (0, 1, 2), (1, 1, 1), (2, 1, 0),
            (0, 2, 2), (1, 2, 1), (2, 2, 0),
            (0, 3, 2), (1, 3, 1),
            (1, 4, 2)]
# samping: dx positif = ke BELAKANG kepala (dicerminkan otomatis untuk arah kanan)
PLUME_SIDE = [(-2, 0, 1), (-1, 0, 2), (0, 0, 2), (1, 0, 1), (2, 0, 1), (3, 0, 0),
              (-1, 1, 1), (0, 1, 2), (1, 1, 1), (2, 1, 1), (3, 1, 0), (4, 1, 0),
              (0, 2, 2), (1, 2, 2), (2, 2, 1), (3, 2, 0), (4, 2, 0), (5, 2, 0),
              (1, 3, 2), (2, 3, 1), (3, 3, 0)]


def _lut(img, table):
    """peta per-piksel -> nilai tabel (atau -9 kalau warna tidak ada di tabel)."""
    out = np.full(img.shape[:2], -9, dtype=np.int16)
    for rgb, v in table.items():
        out[(img[..., 0] == rgb[0]) & (img[..., 1] == rgb[1]) & (img[..., 2] == rgb[2])] = v
    return out

def analyse(body, cloth):
    ba, ca = body[..., 3] > 0, cloth[..., 3] > 0
    d = dict(ba=ba, ca=ca)
    d['b_skin'] = _lut(body, SKIN)
    d['b_out'] = (body[..., 0] == 0x18) & (body[..., 1] == 0x18) & (body[..., 2] == 0x18) & ba
    d['b_white'] = (body[..., 0] == 0xf8) & (body[..., 1] == 0xf8) & (body[..., 2] == 0xf8) & ba
    d['c_khaki'], d['c_red'], d['c_navy'] = _lut(cloth, KHAKI), _lut(cloth, RED), _lut(cloth, NAVY)
    d['c_out'] = (cloth[..., 0] == 0x18) & (cloth[..., 1] == 0x18) & (cloth[..., 2] == 0x18) & ca
    return d

# ---------------------------------------------------------------- kepala
def find_head(d, R=5.2):
    vis = np.pad(d['ba'] & ~d['ca'], 7)
    h, w = vis.shape
    offs = [(dy, dx) for dy in range(-6, 7) for dx in range(-6, 7) if dy * dy + dx * dx <= R * R]
    best, bc = -1, None
    for cy in range(7, h - 7):
        for cx in range(7, w - 7):
            s = 0
            for dy, dx in offs:
                if vis[cy + dy, cx + dx]: s += 1
            if s > best or (s == best and cy < bc[0]):
                best, bc = s, (cy, cx)
    return (bc[0] - 7, bc[1] - 7), best

def head_mask(d, c, rx=6.0, ry=6.2):
    cy, cx = c
    ys, xs = np.mgrid[0:64, 0:64]
    ell = ((xs - cx) / rx) ** 2 + ((ys - cy) / ry) ** 2 <= 1.0
    return ell & d['ba'] & ~d['ca']

# ---------------------------------------------------------------- bangun satu frame
def _nb4(m):
    """tetangga 4-arah (atas, bawah, kiri, kanan) dari mask m."""
    p = np.pad(m, 1)
    return p[:-2, 1:-1], p[2:, 1:-1], p[1:-1, :-2], p[1:-1, 2:]

def build(body, cloth, dirkey, state):
    d = analyse(body, cloth)
    ba, ca = d['ba'], d['ca']
    F = ba | ca
    cen, _ = find_head(d)
    hm = head_mask(d, cen, 6.5, 6.7)
    # tutup sisa piksel kepala yang menempel di luar elips (telinga / tepi) di atas garis dagu
    up, dn, lf, rt = _nb4(hm)
    near = (up | dn | lf | rt) & ba & ~ca & ~hm
    ys = np.mgrid[0:64, 0:64][0]
    hm = hm | (near & (ys <= cen[0] + 5))
    ysh, xsh = np.nonzero(hm)
    hy0, hy1, hx0, hx1 = ysh.min(), ysh.max(), xsh.min(), xsh.max()
    hcx = (hx0 + hx1) / 2.0

    bodyL = np.zeros((64, 64, 4), np.uint8)
    headL = np.zeros((64, 64, 4), np.uint8)
    up_f, dn_f, lf_f, rt_f = _nb4(F)
    edge = ~(up_f & dn_f & lf_f & rt_f)           # tepi siluet (ada tetangga kosong)

    def put(L, y, x, rgb):
        if 0 <= y < 64 and 0 <= x < 64:
            L[y, x, :3] = rgb; L[y, x, 3] = 255

    # ---- badan
    for y in range(64):
        for x in range(64):
            if not F[y, x] or hm[y, x]:
                continue
            if ca[y, x]:
                if d['c_out'][y, x]: c = OUTLINE
                elif d['c_khaki'][y, x] >= 0: c = STEEL[d['c_khaki'][y, x]]
                elif d['c_red'][y, x] >= 0: c = MANTLE[d['c_red'][y, x]]
                elif d['c_navy'][y, x] >= 0: c = BELT[d['c_navy'][y, x]]
                else: c = OUTLINE
            else:
                if d['b_out'][y, x]: c = OUTLINE
                elif d['b_skin'][y, x] >= 0: c = GAUNT[d['b_skin'][y, x]]
                elif d['b_white'][y, x]: c = GAUNT[2]
                else: c = OUTLINE
            put(bodyL, y, x, c)

    # ---- helm
    eyes = []
    for y in range(64):
        for x in range(64):
            if not hm[y, x]:
                continue
            if d['b_out'][y, x]:
                if edge[y, x] or not (hm[y, x - 1] and hm[y, x + 1]):
                    put(headL, y, x, OUTLINE)
                else:
                    put(headL, y, x, HELM[0])       # bagian dalam (mata / mulut) -> ditimpa visor di bawah
            elif d['b_white'][y, x]:
                eyes.append((y, x)); put(headL, y, x, HELM[0])
            elif d['b_skin'][y, x] >= 0:
                put(headL, y, x, HELM[d['b_skin'][y, x]])
            else:
                put(headL, y, x, OUTLINE)

    # visor + mata menyala (hanya kalau kelihatan mata: depan / samping)
    if eyes:
        ey0, ey1 = min(e[0] for e in eyes), max(e[0] for e in eyes)
        pup = []
        for (y, x) in eyes:
            for dx in (-1, 1):
                if d['b_out'][y, x + dx] and hm[y, x + dx]: pup.append((y, x + dx))
        exs = [e[1] for e in eyes] + [p[1] for p in pup]
        if dirkey == 'down':
            x0, x1 = min(exs) - 1, max(exs) + 1
        elif dirkey == 'left':
            x0, x1 = hx0 + 1, max(exs) + 1
        else:
            x0, x1 = min(exs) - 1, hx1 - 1
        for y in range(ey0, ey1 + 1):
            for x in range(x0, x1 + 1):
                if hm[y, x]: put(headL, y, x, VISOR)
        for (y, x) in eyes:
            put(headL, y, x, EYE_HOT if y == ey0 else EYE_CORE)
        # alis logam di atas visor, dan dagu
        for x in range(x0, x1 + 1):
            if hm[ey0 - 1, x] and tuple(headL[ey0 - 1, x, :3]) != OUTLINE: put(headL, ey0 - 1, x, HELM_EDGE)
        if dirkey == 'down':
            xc = int(round(hcx))
            for y in range(ey1 + 1, min(hy1 - 1, ey1 + 2) + 1):
                if hm[y, xc]: put(headL, y, xc, HELM[0])
            yv = ey1 + 3
            for x in (xc - 2, xc + 2):
                if 0 <= yv < 64 and hm[yv, x] and hm[yv, x - 1] and hm[yv, x + 1]: put(headL, yv, x, HELM[0])

    # ---- hiasan: jambul merah di tengah helm
    if state != 'dead':
        if dirkey in ('down', 'up'):
            xc = int(np.floor(hcx))
            for y in range(hy0 + 1, hy0 + 5):
                for x in (xc, xc + 1):
                    if hm[y, x] and tuple(headL[y, x, :3]) not in (OUTLINE, VISOR, EYE_HOT, EYE_CORE):
                        put(headL, y, x, MANTLE[2] if x == xc else MANTLE[1])
        else:
            for x in range(hx0 + 2, hx1 - 1):
                y = hy0 + 1
                if hm[y, x] and tuple(headL[y, x, :3]) != OUTLINE: put(headL, y, x, MANTLE[1])

    # ---- jambul merah tua (bulu kuda) di puncak helm: sirip tegak di depan/belakang, menyapu ke belakang di samping
    if state != 'dead':
        cells = []
        if dirkey in ('down', 'up'):
            xc = int(np.floor(hcx))
            for (dx, dy, t) in PLUME_FB:
                cells.append((xc + dx, hy0 - dy, t))
        else:
            xcs = int(round(hcx))
            sgn = 1 if dirkey == 'left' else -1       # menghadap kiri -> belakang kepala = kanan
            for (dx, dy, t) in PLUME_SIDE:
                cells.append((xcs + sgn * dx, hy0 - dy, t))
        plumeset = set()
        for (x, y, t) in cells:
            plumeset.add((y, x))
            put(headL, y, x, MANTLE[t])
        for (y, x) in list(plumeset):
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if (yy, xx) in plumeset: continue
                if 0 <= yy < 64 and 0 <= xx < 64 and headL[yy, xx, 3] == 0:
                    put(headL, yy, xx, OUTLINE)

    # ---- kilap tepi logam (cahaya dari kiri-atas)
    def rim(L, tones, spec, allmask):
        for y in range(1, 63):
            for x in range(1, 63):
                if L[y, x, 3] and tuple(L[y, x, :3]) == tones:
                    a = (not allmask[y - 1, x]) or tuple(L[y - 1, x, :3]) == OUTLINE
                    b = (not allmask[y, x - 1]) or tuple(L[y, x - 1, :3]) == OUTLINE
                    if a or b:
                        L[y, x, :3] = spec
    allm = (bodyL[..., 3] > 0) | (headL[..., 3] > 0)
    rim(bodyL, STEEL[2], SPEC, allm)
    rim(bodyL, GAUNT[2], STEEL[2], allm)
    rim(headL, HELM[2], HELM_EDGE, allm)

    # ---- permata di dada & gesper emas (tampak depan saja, pose tegak)
    if dirkey == 'down' and state in ('idle', 'walk', 'run', 'jump'):
        kh = (d['c_khaki'] >= 0)
        rows = np.nonzero(kh.any(axis=1))[0]
        if len(rows):
            r0 = rows.min()
            xs = np.nonzero(kh[r0:r0 + 4].any(axis=0))[0]
            xc = int(np.median(xs))
            if bodyL[r0 + 2, xc, 3]: put(bodyL, r0 + 2, xc, GEM[1])
        nv = (d['c_navy'] >= 0)
        rows = np.nonzero(nv.any(axis=1))[0]
        if len(rows):
            r = int(np.median(rows)); xs = np.nonzero(nv[r])[0]
            if len(xs): put(bodyL, r, int(np.median(xs)), BELT[2])
    return bodyL, headL, hm


# ───────────── ICON ─────────────
def make_icon(size=80, scale=2):
    """Ikon inventory: seluruh set (helm + jambul + dada + sepatu), pixel art 2x, sama gaya dengan ikon armor lain."""
    B, C = load64('FrontIdle1.png'), load64('ClothFrontIdle.png')
    bl, hl, _ = build(B, C, 'down', 'idle')
    layer = Image.alpha_composite(Image.fromarray(bl), Image.fromarray(hl))
    bb = layer.getbbox()
    crop = layer.crop(bb).resize(((bb[2] - bb[0]) * scale, (bb[3] - bb[1]) * scale), Image.NEAREST)
    icon = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    icon.alpha_composite(crop, ((size - crop.width) // 2, (size - crop.height) // 2))
    return icon


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    for st, d, i, bfile, cfile in states():
        body, cloth = load64(bfile), load64(cfile)
        bl, hl, _ = build(body, cloth, d, st)
        Image.fromarray(bl).save(OUT / f'body_{st}_{d}_{i + 1}.png')
        Image.fromarray(hl).save(OUT / f'head_{st}_{d}_{i + 1}.png')
        n += 2
    make_icon().save(ASET / 'UmbralArmor.png')
    print(f'{n} frame + ikon ditulis ke {OUT}')


if __name__ == '__main__':
    main()
