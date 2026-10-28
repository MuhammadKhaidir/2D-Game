#!/usr/bin/env python3
"""
Generator sprite "Dread Armor" (set armor gelap pertama).

Armor = SATU set utuh. Tiap frame animasi karakter dapat 2 layer PNG (64x64, sama kayak sprite body/baju):
  body_*  : badan (menggantikan baju dasar PlayerCloth) - dada, bahu, lengan, sarung tangan, celana, sepatu
  head_*  : helm (digambar PALING ATAS, setelah senjata - urutan sama kayak helm lama)

Sprite dibuat dari sprite yang SUDAH ADA di game, jadi pas di semua pose:
  - layer badan  = baju dasar (BaseCloth*/Cloth*) di-recolor ke palet armor gelap
  - kulit yang kelihatan (tangan, leher) -> sarung tangan / gorget
  - layer kepala = dari siluet kepala di sprite body, ditambah visor, mata ungu, dan tanduk

Jalankan dari folder 2DGame:
    pip install pillow numpy scipy
    python tools/generate_dread_armor.py

Output: src/Aset/armor/dread/{body|head}_<state>_<arah>_<nomor>.png  + src/Aset/DreadArmor.png (ikon)
  state : idle walk run jump atk1 atk2 atk3 atk4 dead
  arah  : down up left right        nomor: mulai dari 1
Mau ganti warna? Edit blok PALETTE di bawah lalu jalankan ulang.
"""
import os, sys
import numpy as np
from pathlib import Path
from scipy import ndimage as ndi
from PIL import Image

ROOT  = Path(__file__).resolve().parent.parent
ASET  = ROOT / 'src' / 'Aset'
OUT   = ASET / 'armor' / 'dread'

# ───────────── pasangan frame body <-> baju (sama persis kayak Entitas/player.java) ─────────────
DIRS = {'down':'Front','up':'Back','left':'Left','right':'Right'}

def states():
    """yield (state, dir, idx, body_file, cloth_file)"""
    out = []
    for d, D in DIRS.items():
        out.append(('idle', d, 0, f'{D}Idle1.png', f'Cloth{D}Idle.png'))
        for i in range(1, 7):
            out.append(('walk', d, i-1, f'{D}Walk{i}.png', f'Cloth{D}Walk ({i}).png'))
        for i in range(1, 4):
            out.append(('run', d, i-1, f'{D}Run{i}.png', f'Cloth{D}Run ({i}).png'))
        for j in range(2):          # body jump idx j pairs with cloth idx (j==0 ? 1 : 0)
            c = 1 if j == 0 else 0
            out.append(('jump', d, j, f'Jump{D}{j+1}.png', f'Cloth{D}Jump ({c+1}).png'))
        for i in range(1, 5):
            out.append(('atk1', d, i-1, f'{D}Attack ({i}).png', f'BaseCloth{D}2 ({i}).png'))
            out.append(('atk2', d, i-1, f'{D}Swing ({i}).png',  f'BaseCloth{D}2 ({i+4}).png'))
            out.append(('atk3', d, i-1, f'{D} ({i}).png',       f'BaseCloth{D} ({i}).png'))
            out.append(('atk4', d, i-1, f'{D} ({i+4}).png',     f'BaseCloth{D} ({i+4}).png'))
        for i in range(1, 4):
            out.append(('dead', d, i-1, f'Char{D}Dead ({i}).png', f'CharDead{D}Cloth ({i}).png'))
    return out

def load(fn):
    return Image.open(ASET / fn).convert('RGBA')




# ───────────── PALETTE ─────────────
def rgb(h): h=h.lstrip('#'); return (int(h[0:2],16),int(h[2:4],16),int(h[4:6],16),255)

OUTLINE   = rgb('181818')   # same outline colour the game's body/cloth sprites use
LINE_IN   = rgb('0f0c1a')   # inner detail line (visor, plate seams)

# plate ramp: shadow, mid, light, specular
PLATE = [rgb('221f36'), rgb('37345a'), rgb('55537f'), rgb('8c8ab8')]
# arms / pauldrons: darker than the chest so the torso reads as the main plate
ARM   = [rgb('1b182e'), rgb('2a2748'), rgb('403d6a'), rgb('7a52d0')]  # last = violet rim light
# dark leather (gauntlets, gorget, belt)
LEATH = [rgb('15111f'), rgb('241d35'), rgb('3a3052')]
# violet accents
VIO   = [rgb('3b1f6e'), rgb('6a35c4'), rgb('a463ff'), rgb('e3c9ff')]  # dark, mid, glow, core
VISOR = rgb('0a0812')


# ───────────── SEGMENTASI KEPALA ─────────────
SKIN = [(0x88,0x58,0x48),(0xd8,0x98,0x78),(0xf8,0xd8,0xb8)]

HEAD_ROWS = 11

def arr(im): return np.array(im)

def head_mask(body, cloth):
    """Return (head_mask, other_skin_mask) boolean arrays 64x64.
    head = largest 8-connected blob of skin-coloured body pixels NOT covered by cloth,
    plus enclosed eye pixels (white / black) inside it."""
    B = arr(body); C = arr(cloth)
    balpha = B[...,3] > 0
    calpha = C[...,3] > 0
    skin = np.zeros(balpha.shape, bool)
    for s in SKIN:
        skin |= (B[...,0]==s[0]) & (B[...,1]==s[1]) & (B[...,2]==s[2])
    skin &= balpha
    U = skin & ~calpha
    lab, n = ndi.label(U, structure=np.ones((3,3)))
    if n == 0: return np.zeros_like(U), U
    sizes = ndi.sum(U, lab, range(1, n+1))
    k = int(np.argmax(sizes)) + 1
    head = lab == k
    # include eyes / enclosed non-skin pixels
    filled = ndi.binary_fill_holes(head)
    head = filled & balpha & ~calpha
    # cut the head at HEAD_ROWS below its top (everything below = neck / arm / hand)
    ys = np.where(head.any(1))[0]
    ytop = ys.min()
    cut = np.zeros_like(head); cut[ytop:ytop+HEAD_ROWS, :] = True
    head &= cut
    # keep only the blob that contains the top row (drop stray bits)
    lab2, n2 = ndi.label(head, structure=np.ones((3,3)))
    if n2 > 1:
        sizes2 = ndi.sum(head, lab2, range(1, n2+1))
        head = lab2 == (int(np.argmax(sizes2)) + 1)
    other = U & ~head
    return head, other



# ───────────── LAYER BADAN ─────────────
def px(a, c):  # tuple compare helper
    return (a[...,0]==c[0]) & (a[...,1]==c[1]) & (a[...,2]==c[2])

CLOTH_MAP = {
    (0x71,0x65,0x58): PLATE[0],   # khaki shadow   -> plate shadow
    (0xa6,0x98,0x78): PLATE[1],   # khaki mid      -> plate mid
    (0xc7,0xc4,0xa2): PLATE[2],   # beige light    -> plate light
    (0x5b,0x18,0x39): VIO[0],     # dark red       -> violet trim (collar, sleeve underside)
    (0x8d,0x22,0x46): ARM[1],     # red mid        -> arm / pauldron plate
    (0xbe,0x56,0x67): ARM[3],     # red light      -> violet rim light on pauldrons
    (0x1e,0x2b,0x41): LEATH[0],   # dark blue      -> belt / under-armour
    (0x3c,0x55,0x75): VIO[2],     # mid blue       -> glowing violet gems
}
SKIN_MAP = {  # bare skin that is not under the cloth (hands, neck) -> gauntlets / gorget
    (0x88,0x58,0x48): LEATH[0],
    (0xd8,0x98,0x78): LEATH[1],
    (0xf8,0xd8,0xb8): LEATH[2],
}

def body_layer(body, cloth):
    B = np.array(body); C = np.array(cloth)
    out = np.zeros((64,64,4), np.uint8)
    # cloth silhouette -> plate
    for src, dst in CLOTH_MAP.items():
        out[px(C, src) & (C[...,3]>0)] = dst
    out[px(C, (0x18,0x18,0x18)) & (C[...,3]>0)] = OUTLINE
    h, other = head_mask(body, cloth)
    for src, dst in SKIN_MAP.items():
        out[other & px(B, src)] = dst
    return out, h



# ───────────── HELM ─────────────
def _nb(mask, dy, dx):
    """value of mask at (y+dy, x+dx) for every (y,x); False outside."""
    out = np.zeros_like(mask)
    ys = slice(max(0,-dy), 64-max(0,dy)); xs = slice(max(0,-dx), 64-max(0,dx))
    yd = slice(max(0,dy), 64-max(0,-dy)); xd = slice(max(0,dx), 64-max(0,-dx))
    out[ys, xs] = mask[yd, xd]
    return out

# crescent horns (offsets from the dome's side pixel at row ytop+3; left horn, mirrored for right)
HORN_FRONT = [(-1,1),(-1,0),(-2,0),(-2,-1),(-3,-1),(-2,-2),(-3,-2),(-2,-3),(-3,-3),(-2,-4)]
# swept-back horn in profile, offsets from anchor, 'back' direction = +x (mirrored for right-facing)
HORN_SIDE  = [(-1,1),(-1,0),(-1,-1),(-2,0),(-2,-1),(-2,-2),(-1,-2),(-2,-3),(-3,-3),(-3,-4),(-4,-4),(-4,-5)]

_EYE_TEMPLATE = {}
def _eye_template(direction):
    """eye-white offsets (dy, dx) relative to (ytop, x0) of the idle frame, used when a frame has no eyes drawn."""
    if direction not in _EYE_TEMPLATE:
        D = {'down':'Front','left':'Left','right':'Right'}[direction]
        B = load(f'{D}Idle1.png'); C = load(f'Cloth{D}Idle.png')
        h, _ = head_mask(B, C); A = np.array(B)
        ys, xs = np.where(h)
        w = (A[...,0]==0xf8)&(A[...,1]==0xf8)&(A[...,2]==0xf8)&h
        wy, wx = np.where(w)
        _EYE_TEMPLATE[direction] = [(int(y-ys.min()), int(x-xs.min())) for y,x in zip(wy,wx)]
    return _EYE_TEMPLATE[direction]

def helm_layer(body, cloth, direction, state, body_lay):
    B = np.array(body)
    h, _ = head_mask(body, cloth)
    out = np.zeros((64,64,4), np.uint8)
    if not h.any(): return out
    ys, xs = np.where(h)
    ytop, ybot, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
    H = ybot-ytop+1; W = x1-x0+1
    shape = h.copy()

    # ---- base plate shading (top-left lit, edge-lit) ----
    up, lf = ~_nb(h,-1,0), ~_nb(h,0,-1)
    dn, rt = ~_nb(h,1,0),  ~_nb(h,0,1)
    for y, x in zip(ys, xs):
        ty = (y-ytop)/max(1,H-1); tx = (x-x0)/max(1,W-1)
        L = 1.0 - 0.65*ty - 0.30*tx
        t = 1
        if L > 0.66: t = 2
        elif L < 0.22: t = 0
        if up[y,x] or lf[y,x]: t = min(3, t+1) if t < 2 else 2
        if dn[y,x] or rt[y,x]: t = max(0, t-1)
        out[y,x] = PLATE[t]
    for dx in (2,3):
        yy, xx = ytop+1, x0+dx
        if h[yy,xx]: out[yy,xx] = PLATE[3]

    # ---- eyes / visor ----
    white = (B[...,0]==0xf8)&(B[...,1]==0xf8)&(B[...,2]==0xf8)&h
    wy, wx = np.where(white)
    if not len(wy) and direction != 'up' and state != 'dead':
        t = _eye_template(direction)
        wy = np.array([ytop+dy for dy,dx in t]); wx = np.array([x0+dx for dy,dx in t])
    if len(wy):
        ey = int(np.round(np.median(wy)))
        if direction == 'left':    xa, xb = x0, min(x1, wx.max()+2)
        elif direction == 'right': xa, xb = max(x0, wx.min()-2), x1
        else:                      xa, xb = x0+1, x1-1
        rows = (ey-1, ey, ey+1) if direction in ('down','up') else (ey-1, ey)
        for y in rows:
            for x in range(xa, xb+1):
                if 0<=y<64 and h[y,x]: out[y,x] = VISOR
        # lower face: mouth-guard plate (mid tone) with a centre seam on the front view
        for y in range(rows[-1]+1, ybot+1):
            for x in range(x0, x1+1):
                if h[y,x]:
                    t = 0 if (y >= ybot or dn[y,x] or rt[y,x]) else 1
                    out[y,x] = PLATE[t]
        if direction == 'down':
            xc = (x0+x1)//2
            for y in range(rows[-1]+2, ybot):
                if h[y,xc]: out[y,xc] = PLATE[0]
        for y, x in zip(wy, wx):
            out[y,x] = VIO[2]
        blk = (B[...,0]==0x18)&h
        for y, x in zip(wy, wx):
            for dx in (-1,1):
                if 0<=x+dx<64 and blk[y,x+dx]:
                    out[y,x+dx] = VIO[3] if direction=='down' else VIO[1]
    else:
        xc = (x0+x1)//2
        for y in range(ytop+1, ytop+H-3):
            for x in (xc, xc+1):
                if h[y,x]: out[y,x] = PLATE[3] if y < ytop+4 else PLATE[2]
        for y in range(ybot-2, ybot+1):          # nape guard darker
            for x in range(x0, x1+1):
                if h[y,x]: out[y,x] = PLATE[0]

    # ---- horns ----
    horns = np.zeros((64,64), bool)
    if state != 'dead':
        def put(x,y,col):
            if 0<=x<64 and 0<=y<64 and not h[y,x]:
                out[y,x] = col; horns[y,x] = True
        if direction in ('down','up'):
            yb = ytop+3
            row = np.where(h[yb])[0]; xl, xr = int(row.min()), int(row.max())
            n = len(HORN_FRONT)
            for k,(dx,dy) in enumerate(HORN_FRONT):
                col = ARM[2] if dx == -1 or (dx==-2 and dy>=0) else ARM[1]
                if k == n-1: col = VIO[2]
                if dx <= -3: col = ARM[1]
                put(xl+dx, yb+dy, col); put(xr-dx, yb+dy, ARM[1] if dx<=-3 else (VIO[2] if k==n-1 else ARM[2]))
        else:
            sgn = 1 if direction=='left' else -1       # back of the head = +x when facing left
            row = np.where(h[ytop])[0]
            xtb = int(row.max()) if sgn==1 else int(row.min())
            ax = xtb - sgn*2
            # (row offset above the crown, x offsets toward the back)
            spike = [(1,(-1,0,1)), (2,(0,1,2)), (3,(1,2,3)), (4,(2,3)), (5,(3,))]
            for k,(up_,offs) in enumerate(spike):
                for j,o in enumerate(offs):
                    col = ARM[2] if k < 2 else ARM[1]
                    if k == len(spike)-1: col = VIO[2]
                    put(ax + sgn*o, ytop-up_, col)
    shape |= horns

    # ---- outline ring ----
    ring = (_nb(shape,-1,0)|_nb(shape,1,0)|_nb(shape,0,-1)|_nb(shape,0,1)) & ~shape
    body_a = body_lay[...,3] > 0
    ok = ring & ~body_a & ((B[...,3]==0) | ((B[...,0]==0x18)&(B[...,1]==0x18)))
    out[ok] = OUTLINE
    return out

def full_layers(body, cloth, direction, state):
    bl, h = body_layer(body, cloth)
    hl = helm_layer(body, cloth, direction, state, bl)
    return bl, hl


# ───────────── ICON ─────────────
def make_icon(size=80, scale=2):
    """Ikon inventory: set armor tanpa badan (helm + dada + sepatu), pixel art 2x."""
    B = load('FrontIdle1.png'); C = load('ClothFrontIdle.png')
    bl, hl = full_layers(B, C, 'down', 'idle')
    # bagian leher/bahu yang tadinya ketutup kulit ikut digambar supaya tampak seperti satu set utuh
    layer = Image.new('RGBA', (64,64), (0,0,0,0))
    layer.alpha_composite(Image.fromarray(bl)); layer.alpha_composite(Image.fromarray(hl))
    bb = layer.getbbox()
    crop = layer.crop(bb).resize(((bb[2]-bb[0])*scale, (bb[3]-bb[1])*scale), Image.NEAREST)
    icon = Image.new('RGBA', (size, size), (0,0,0,0))
    icon.alpha_composite(crop, ((size-crop.width)//2, (size-crop.height)//2))
    return icon


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    for st, d, i, bfile, cfile in states():
        body, cloth = load(bfile), load(cfile)
        bl, hl = full_layers(body, cloth, d, st)
        Image.fromarray(bl).save(OUT / f'body_{st}_{d}_{i+1}.png')
        Image.fromarray(hl).save(OUT / f'head_{st}_{d}_{i+1}.png')
        n += 2
    make_icon().save(ASET / 'DreadArmor.png')
    print(f'{n} frame + ikon ditulis ke {OUT}')

if __name__ == '__main__':
    main()
