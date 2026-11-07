import math
import numpy as np, cv2
from scipy import ndimage as ndi
import seg

# ---------- canvas (koordinat referensi digeser OX,OY supaya muat gerakan besar) ----------
OX, OY = 40, 60
CW, CH = 212 + 2*OX + 40, 187 + OY + 40     # 332 x 287

# pivot sendi (koordinat referensi)
PIV = {
 'root':    (106, 182),
 'pelvis':  (112, 104),
 'torso':   (110, 102),
 'mane':    (104, 52),
 'head':    (122, 68),
 'armL_up': (70, 56),
 'armL_lo': (46, 99),
 'armR':    (134, 90),
 'handR':   (172, 100),
 'legL_up': (88, 106),
 'legL_lo': (80, 151),
 'legR_up': (127, 102),
 'legR_lo': (146, 140),
 'cloth':   (108, 96),
}
PARENT = {
 'torso': 'pelvis', 'cloth': 'pelvis',
 'mane': 'torso', 'head': 'torso', 'armL_up': 'torso', 'armR': 'torso',
 'armL_lo': 'armL_up', 'handR': 'armR',
}
# tulang kaki (koordinat referensi, pose diam): pinggul H, lutut K, pergelangan/telapak A
LEG = {
 'L': dict(H=(88, 106), K=(80, 151), A=(66, 176), up='legL_up', lo='legL_lo'),
 'R': dict(H=(127, 102), K=(146, 140), A=(144, 176), up='legR_up', lo='legR_lo'),
}
DRAW = ['mane','torso','legL_up','legL_lo','legR_up','legR_lo','cloth','armR','handR','armL_up','armL_lo','head']
EYES = [(141.5, 48.5), (158.0, 48.5)]   # posisi mata merah di referensi (ikut transform kepala)
SNOUT_TIP = (164, 68)

def to_canvas(img4):
    """RGBA float (h,w,4) premultiplied -> kanvas CHxCW"""
    out = np.zeros((CH, CW, 4), np.float32)
    h, w = img4.shape[:2]
    out[OY:OY+h, OX:OX+w] = img4
    return out

def build_parts():
    rgb, M, label = seg.segment()
    h, w = M.shape
    idx = {n: i for i, n in enumerate(seg.ORDER)}
    lab = lambda n: (label == idx[n])
    parts = {}
    rgbf = rgb.astype(np.float32)
    # urutan 'depan' terhadap tiap bagian (yang menutupinya)
    front_of = {
        'torso':   ['mane_dummy','armL_up','armR','handR','cloth','head','legL_up','legR_up','armL_lo'],
        'legL_up': ['legL_lo','cloth','armL_lo'],
        'legR_up': ['legR_lo','cloth'],
        'armL_up': ['armL_lo'],
        'armR':    ['handR','head'],
        'mane':    ['head','armL_up','torso'],
    }
    # pixel dasar tiap bagian
    for n in seg.ORDER:
        m = lab(n)
        a = m.astype(np.float32)
        img = np.zeros((h, w, 4), np.float32)
        img[..., :3] = rgbf * a[..., None]
        img[..., 3] = a
        parts[n] = img

    # ---- underlay: salin warna asli bagian depan di sekitar tepi (sambungan mulus), lalu inpaint area dalam ----
    for n, fronts in front_of.items():
        own = lab(n)
        frontm = np.zeros_like(own)
        for f in fronts:
            if f in idx: frontm |= lab(f)
        ring = ndi.binary_dilation(own, iterations=7) & M & frontm & ~own
        extra = ring.copy()
        if n in seg.UNDER:
            deep = seg.poly_mask(rgb.shape, seg.UNDER[n]) & M & frontm & ~own
            extra |= deep
        if not extra.any():
            continue
        base = rgb.copy()
        known = own | ring
        # inpaint area dalam (yang belum 'known') dari piksel milik bagian sendiri
        need = extra & ~known
        if need.any():
            src = rgb.copy()
            hole = (~own) & (need | (~M))     # isi hanya yang perlu, sisanya dianggap kosong
            inp = cv2.inpaint(src, (~own).astype(np.uint8)*255, 4, cv2.INPAINT_TELEA)
            base = np.where(need[..., None], inp, base)
        fill = extra
        img = parts[n]
        for c in range(3):
            img[..., c] = np.where(fill, base[..., c].astype(np.float32), img[..., c])
        img[..., 3] = np.where(fill, 1.0, img[..., 3])
        # premultiplied
        for c in range(3):
            img[..., c] = img[..., c] * (img[..., 3] > 0)
    import backview
    parts.update(backview.make_back_parts(rgb, M, label, {k: v.copy() for k, v in parts.items()}))
    # varian kepala mati: mata merah dipadamkan
    hd = parts['head'].copy()
    a = np.maximum(hd[..., 3], 1e-6)
    r, g, b = hd[..., 0]/a, hd[..., 1]/a, hd[..., 2]/a
    red = (hd[..., 3] > 0) & (r > 110) & (r > 1.45*g) & (r > 1.35*b) & (g < 140)
    # (mata + sedikit gusi/lidah merah ikut gelap; gigi putih tetap)
    eye_zone = np.zeros(red.shape, bool); eye_zone[40:56, 134:164] = True
    red &= eye_zone
    for c, v in enumerate((34, 32, 46)):
        hd[..., c] = np.where(red, v*hd[..., 3], hd[..., c])
    parts['head_dead'] = hd
    return {n: to_canvas(img) for n, img in parts.items()}

# ---------- matriks ----------
def rot_about(deg, cx, cy):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, cx - c*cx + s*cy], [s, c, cy - s*cx - c*cy], [0, 0, 1]], np.float64)

def trans(tx, ty):
    return np.array([[1,0,tx],[0,1,ty],[0,0,1]], np.float64)

def scale_about(sx, sy, cx, cy):
    return np.array([[sx,0,cx-sx*cx],[0,sy,cy-sy*cy],[0,0,1]], np.float64)

def local(name, pose):
    """transform lokal sendi: pose[name] = (rot_deg, dx, dy) atau (rot, dx, dy, sx, sy)"""
    v = pose.get(name, (0, 0, 0))
    rot, dx, dy = v[0], v[1], v[2]
    cx, cy = PIV[name]
    M = trans(dx, dy) @ rot_about(rot, cx, cy)
    if len(v) >= 5:
        M = M @ scale_about(v[3], v[4], cx, cy)
    return M

def _ang(v):
    return math.atan2(v[1], v[0])

def solve_leg(leg, hip_w, target):
    """2-bone IK. return (matriks paha, matriks betis). lutut selalu menekuk ke arah +x (depan)."""
    H, K, A = leg['H'], leg['K'], leg['A']
    L1 = math.hypot(K[0]-H[0], K[1]-H[1]); L2 = math.hypot(A[0]-K[0], A[1]-K[1])
    dx, dy = target[0]-hip_w[0], target[1]-hip_w[1]
    d = math.hypot(dx, dy)
    d = min(max(d, abs(L1-L2)+1.0), (L1+L2)*0.999)
    cosa = max(-1.0, min(1.0, (L1*L1 + d*d - L2*L2) / (2*L1*d)))
    al = math.acos(cosa)
    be = math.atan2(dy, dx)
    c1 = (hip_w[0] + L1*math.cos(be+al), hip_w[1] + L1*math.sin(be+al))
    c2 = (hip_w[0] + L1*math.cos(be-al), hip_w[1] + L1*math.sin(be-al))
    kn = c1 if c1[0] >= c2[0] else c2
    a1 = math.atan2(kn[1]-hip_w[1], kn[0]-hip_w[0])
    # ujung betis sebenarnya (kalau target kejauhan, tetap konsisten)
    a2 = math.atan2(target[1]-kn[1], target[0]-kn[0]) if d < (L1+L2)*0.999 else a1
    a1r = _ang((K[0]-H[0], K[1]-H[1])); a2r = _ang((A[0]-K[0], A[1]-K[1]))
    r1 = math.degrees(a1 - a1r); r2 = math.degrees(a2 - a2r)
    Mt = trans(hip_w[0]-H[0], hip_w[1]-H[1]) @ rot_about(r1, H[0], H[1])
    Ms = trans(kn[0]-K[0], kn[1]-K[1]) @ rot_about(r2, K[0], K[1])
    return Mt, Ms

def world_mats(pose):
    mats = {}
    def get(n):
        if n in mats: return mats[n]
        if n == 'pelvis':
            m = local('pelvis', pose)
        else:
            m = get(PARENT[n]) @ local(n, pose)
        mats[n] = m
        return m
    for n in ('torso','cloth','mane','head','armL_up','armL_lo','armR','handR'): get(n)
    mats['pelvis'] = get('pelvis')
    # kaki: IK. pose['footL'] / ['footR'] = (offset x, offset y) target telapak di dunia (relatif posisi diam)
    for side, key in (('L','footL'), ('R','footR')):
        leg = LEG[side]
        off = pose.get(key, (0, 0))
        hip_w = tuple((mats['pelvis'] @ np.array([leg['H'][0], leg['H'][1], 1.0]))[:2])
        tgt = (leg['A'][0] + off[0], leg['A'][1] + off[1])
        Mt, Ms = solve_leg(leg, hip_w, tgt)
        mats[leg['up']] = Mt
        mats[leg['lo']] = Ms
    return mats

def render_hi(parts, pose, order=None, extra=None):
    """komposit resolusi referensi -> (CH,CW,4) premultiplied"""
    mats = world_mats(pose)
    out = np.zeros((CH, CW, 4), np.float32)
    # matriks bekerja di koordinat referensi; kanvas punya offset (OX,OY)
    shift = trans(OX, OY); unshift = trans(-OX, -OY)
    for n in (order or pose.get('order') or DRAW):
        if pose.get('hide') and n in pose['hide']: continue
        W = shift @ mats[n] @ unshift
        pn = pose.get('alias', {}).get(n, n)
        src = cv2.warpAffine(parts[pn], W[:2], (CW, CH), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=0)
        if extra and n in extra: src = extra[n](src)
        a = src[..., 3:4]
        out = src + out * (1 - a)
    return out, mats

def pt(mats, name, p):
    q = mats[name] @ np.array([p[0], p[1], 1.0])
    return q[0], q[1]

if __name__ == '__main__':
    parts = build_parts()
    out, mats = render_hi(parts, {})
    a = out[..., 3:4]
    rgb = np.where(a > 0, out[..., :3] / np.maximum(a, 1e-6), 0)
    from PIL import Image
    ref = np.array(Image.open(seg.REF).convert('RGB')).astype(np.float32)
    crop = rgb[OY:OY+ref.shape[0], OX:OX+ref.shape[1]]
    M = seg.figure_mask(ref.astype(np.uint8))
    diff = np.abs(crop - ref)[M].mean()
    print('rest-pose mean abs diff vs reference (figure px):', diff)
    vis = np.clip(rgb*1.0, 0, 255).astype(np.uint8)
    bg = np.full_like(vis, (86,112,132)); al = (a>0.5)
    vis = np.where(al, vis, bg)
    Image.fromarray(vis).resize((CW*3, CH*3), Image.NEAREST).save('rest.png')
