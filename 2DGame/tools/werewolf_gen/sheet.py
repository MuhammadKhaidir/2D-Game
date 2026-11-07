"""Rakit sprite sheet werewolf: 25 kolom x 4 baris, sel 136 x 100.

kolom (kiri -> kanan): idle x4 | walk x8 | attack x6 (frame ke-4 = cakaran) | lunge x3 | dead x4
baris (atas -> bawah): right | left | back_right | back_left
  - right      : pandangan 3/4 depan menghadap kanan (juga dipakai untuk arah BAWAH saat terakhir menghadap kanan)
  - left       : cermin 'right'                      (juga dipakai untuk arah BAWAH saat terakhir menghadap kiri)
  - back_right : pandangan 3/4 belakang (jalan ke ATAS, terakhir menghadap kanan)
  - back_left  : cermin 'back_right'
"""
import numpy as np
from PIL import Image
import rig, anim

SPEC = [('idle', 4), ('walk', 8), ('attack', 6), ('lunge', 3), ('dead', 4)]
COLS = sum(n for _, n in SPEC)
ROWS = ['right', 'left', 'back_right', 'back_left']
POSES = {'idle': anim.idle_pose, 'walk': anim.walk_pose, 'attack': anim.attack_pose,
         'lunge': anim.lunge_pose, 'dead': anim.dead_pose}


def to_back(p):
    q = dict(p)
    q['alias'] = {'head': 'head_back', 'torso': 'torso_back'}
    q['_eye'] = 0
    h = q['head']
    q['head'] = (h[0], h[1] - 12, h[2] + 2)      # kepala digeser ke tengah punggung
    q['hide'] = list(q.get('hide', [])) + ['mane']
    return q


def lift_to_fit(im, bottom=None):
    """kalau ada bagian badan yang turun melewati tepi bawah sel, geser gambar ke atas seperlunya."""
    H = im.shape[0]
    bottom = H - 2 if bottom is None else bottom
    ys = np.nonzero(im[..., 3] > 0)[0]
    over = ys.max() - bottom
    if over <= 0:
        return im
    out = np.zeros_like(im)
    out[:H - over] = im[over:]
    return out


def render_all():
    parts = rig.build_parts()
    tone = anim.tone_fn()
    pal = anim.build_palette(parts, tone)
    rows = {r: [] for r in ROWS}
    for kind, n in SPEC:
        for k in range(n):
            p = POSES[kind](k)
            front = anim.render_pose(parts, p, pal, tone)
            back = anim.render_pose(parts, to_back(p), pal, tone)
            if kind == 'dead':
                front, back = lift_to_fit(front), lift_to_fit(back)
            rows['right'].append(front)
            rows['left'].append(front[:, ::-1].copy())
            rows['back_right'].append(back)
            rows['back_left'].append(back[:, ::-1].copy())
    return rows


def build_sheet(rows=None):
    rows = rows or render_all()
    W, H = anim.CELL_W, anim.CELL_H
    sheet = Image.new('RGBA', (COLS * W, len(ROWS) * H), (0, 0, 0, 0))
    for r, name in enumerate(ROWS):
        for c, im in enumerate(rows[name]):
            sheet.paste(Image.fromarray(im, 'RGBA'), (c * W, r * H))
    return sheet, rows


def check_margins(rows):
    """pastikan gak ada frame yang nempel/terpotong di tepi sel."""
    W, H = anim.CELL_W, anim.CELL_H
    worst = (99, 99, 99, 99)
    for name, frames in rows.items():
        for i, im in enumerate(frames):
            ys, xs = np.nonzero(im[..., 3] > 0)
            m = (xs.min(), W - 1 - xs.max(), ys.min(), H - 1 - ys.max())
            worst = tuple(min(a, b) for a, b in zip(worst, m))
            if min(m) < 1:
                print('  PERINGATAN: frame', name, i, 'nempel tepi sel (kiri,kanan,atas,bawah) =', m)
    print('margin terkecil (kiri,kanan,atas,bawah):', worst)
    return worst


def save_png(sheet_img, path):
    """simpan sebagai PNG indexed + transparansi (lossless, ~3.5x lebih kecil); kalau warnanya > 255, simpan RGBA biasa."""
    a = np.array(sheet_img)
    opaque = a[..., 3] > 0
    cols = np.unique(a[opaque][:, :3], axis=0)
    if len(cols) > 255:
        sheet_img.save(path, optimize=True)
        return
    lut = {tuple(c): i + 1 for i, c in enumerate(cols)}
    idx = np.zeros(opaque.shape, np.uint8)
    idx[opaque] = [lut[tuple(p)] for p in a[opaque][:, :3]]
    pim = Image.fromarray(idx, 'P')
    pal = [0, 0, 0] + [v for c in cols for v in c]
    pim.putpalette(pal + [0] * (768 - len(pal)))
    pim.save(path, optimize=True, transparency=0)


def contact_sheet(rows, path, scale=2, bg=(205, 222, 236)):
    sheet, _ = build_sheet(rows)
    base = Image.new('RGBA', sheet.size, bg + (255,))
    base.alpha_composite(sheet)
    base = base.resize((base.width * scale, base.height * scale), Image.NEAREST).convert('RGB')
    base.save(path)
    print('preview ->', path, base.size)


def gifs(rows, outdir, scale=3, bg=(205, 222, 236)):
    """GIF animasi per gerakan (baris right) buat dicek mata."""
    from pathlib import Path
    outdir = Path(outdir); outdir.mkdir(parents=True, exist_ok=True)
    W, H = anim.CELL_W, anim.CELL_H
    durations = {'idle': 220, 'walk': 90, 'attack': 100, 'lunge': 90, 'dead': 150}
    c = 0
    for kind, n in SPEC:
        frames = []
        for k in range(n):
            base = Image.new('RGBA', (W, H), bg + (255,))
            base.alpha_composite(Image.fromarray(rows['right'][c + k], 'RGBA'))
            frames.append(base.resize((W * scale, H * scale), Image.NEAREST).convert('P', palette=Image.ADAPTIVE))
        c += n
        frames[0].save(outdir / f'werewolf_{kind}.gif', save_all=True, append_images=frames[1:],
                       duration=durations[kind], loop=0, disposal=2)
        print('gif ->', outdir / f'werewolf_{kind}.gif')
