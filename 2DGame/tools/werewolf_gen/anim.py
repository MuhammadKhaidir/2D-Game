import math, numpy as np, cv2
from PIL import Image
import rig, px

F = 0.44
CELL_W, CELL_H = 136, 100
X0 = -48.5
Y0 = -32.0
GROUND = 94

def ss(v):
    v = max(0.0, min(1.0, v)); return v*v*(3-2*v)
def lerp(a, b, t): return a + (b-a)*t

# ============================================================ pose builders
def base_pose():
    return {'pelvis': (0, 0, 0), 'torso': (0, 0, 0), 'head': (0, 0, 0), 'mane': (0, 0, 0),
            'armL_up': (0, 0, 0), 'armL_lo': (0, 0, 0), 'armR': (0, 0, 0), 'handR': (0, 0, 0),
            'cloth': (0, 0, 0), 'footL': (0, 0), 'footR': (0, 0), '_eye': 0.7, '_fx': 0}

def idle_pose(k, n=4):
    ph = 2*math.pi*k/n
    s = math.sin(ph); br = (1-math.cos(ph))/2
    p = base_pose()
    p['pelvis']  = (0, -1.0*s, 3.5 + 1.4*br)
    p['torso']   = (5 + 1.4*s, 0, -2.4*br, 1.0, 1.0 + 0.014*br)
    p['head']    = (1.5 - 2.0*s, 1.4*s, 1.0 + 0.8*br)
    p['mane']    = (3.2*math.sin(ph-1.0), 0, 0)
    p['armL_up'] = (3 + 2.4*s, 0, -1.2*br)
    p['armL_lo'] = (2 + 3.2*math.sin(ph-0.8), 0, 0)
    p['armR']    = (-5 - 2.4*s, 0, -1.0*br)
    p['handR']   = (3 + 4.5*math.sin(ph-1.2), 0, 0)
    p['cloth']   = (2.6*math.sin(ph-0.5), 0, 0)
    p['_eye'] = 0.55 + 0.45*br
    return p

def walk_pose(k, n=8, S=13.0, lift_h=10.0, stance=0.58):
    u = k / n; ph = 2*math.pi*u
    p = base_pose()
    def foot(uu):
        uu %= 1.0
        if uu < stance:
            return lerp(S, -S, uu/stance), 0.0
        v = (uu-stance)/(1-stance)
        return lerp(-S, S, ss(v)), -lift_h*math.sin(math.pi*v)
    p['footL'] = foot(u)
    p['footR'] = foot(u+0.5)
    bob = 0.5*(1+math.cos(2*ph))                 # 1 pas tumpuan ganda (paling rendah)
    p['pelvis']  = (0, 2.0*math.sin(ph), 3.5 + 2.8*bob)
    p['torso']   = (10 + 3.0*math.sin(ph), 0, 0)
    p['head']    = (1.5 - 2.4*math.sin(ph), 1.5, 2.0 - 0.8*bob)
    p['mane']    = (4.0*math.sin(ph-1.3), 0, 0)
    p['armL_up'] = (3 + 11*math.cos(ph), 0, 0)
    p['armL_lo'] = (5 + 7*math.cos(ph-1.0), 0, 0)
    p['armR']    = (-9 - 9*math.cos(ph), 0, 0)
    p['handR']   = (4 + 6*math.cos(ph-0.8), 0, 0)
    p['cloth']   = (5.0*math.sin(ph+0.5), 2.0*math.sin(ph), 0)
    p['_eye'] = 0.85
    return p

# --- attack: sabetan besar lengan kiri (panjang) dari belakang-bawah ke depan, lengan kanan mencakar menyusul
ATTACK_KEYS = [
    dict(torso=3,   head=-2,  dx=-3, dy=9,  armL=20,  armLlo=8,  armR=-18, handR=-8,  fL=(-7,0),  fR=(6,0),   fx=0, eye=0.8),
    dict(torso=-8,  head=-10, dx=-7, dy=12, armL=36,  armLlo=12, armR=-34, handR=-16, fL=(-10,0), fR=(8,0),   fx=0, eye=1.0),
    dict(torso=8,   head=4,   dx=5,  dy=9,  armL=-14, armLlo=2,  armR=-26, handR=-6,  fL=(-12,0), fR=(14,0),  fx=1, eye=1.0),
    dict(torso=18,  head=14,  dx=14, dy=8,  armL=-66, armLlo=-10,armR=28,  handR=16,  fL=(-14,0), fR=(18,0),  fx=2, eye=1.0),
    dict(torso=15,  head=10,  dx=12, dy=9,  armL=-92, armLlo=-16,armR=46,  handR=22,  fL=(-12,0), fR=(18,0),  fx=3, eye=1.0),
    dict(torso=8,   head=5,   dx=6,  dy=10, armL=-52, armLlo=-6, armR=18,  handR=6,   fL=(-8,0),  fR=(12,0),  fx=0, eye=0.9),
]
def attack_pose(k):
    a = ATTACK_KEYS[k]; p = base_pose()
    p['pelvis']  = (0, a['dx'], a['dy'])
    p['torso']   = (a['torso'], 0, 0)
    p['head']    = (a['head'], 0, 0)
    p['mane']    = (-a['torso']*0.5 + (7 if k in (3, 4) else 0), 0, 0)
    p['armL_up'] = (a['armL'], 0, 0); p['armL_lo'] = (a['armLlo'], 0, 0)
    p['armR']    = (a['armR'], 0, 0); p['handR'] = (a['handR'], 0, 0)
    p['cloth']   = (-a['torso']*0.5, 0, 0)
    p['footL'] = a['fL']; p['footR'] = a['fR']
    p['_eye'] = a['eye']; p['_fx'] = a['fx']
    return p

LUNGE_KEYS = [
    dict(torso=12, head=8,  dx=3,  dy=7,   armL=-10, armLlo=-4, armR=-30, handR=-8,  fL=(-22,-6),  fR=(-4,-3),  eye=1.0),
    dict(torso=22, head=15, dx=9,  dy=-4,  armL=-58, armLlo=-12,armR=-58, handR=-18, fL=(-34,-20), fR=(-18,-16), eye=1.0),
    dict(torso=14, head=10, dx=11, dy=4,   armL=-50, armLlo=-10,armR=-44, handR=-22, fL=(-14,-6),  fR=(10,-2),   eye=1.0),
]
def lunge_pose(k):
    a = LUNGE_KEYS[k]; p = base_pose()
    p['pelvis']  = (0, a['dx'], a['dy'])
    p['torso']   = (a['torso'], 0, 0); p['head'] = (a['head'], 0, 0)
    p['mane']    = (-12, 0, 0)
    p['armL_up'] = (a['armL'], 0, 0); p['armL_lo'] = (a['armLlo'], 0, 0)
    p['armR']    = (a['armR'], 0, 0); p['handR'] = (a['handR'], 0, 0)
    p['cloth']   = (-8, 0, 0)
    p['footL'] = a['fL']; p['footR'] = a['fR']
    p['_eye'] = a['eye']
    return p

DEAD_KEYS = [
    dict(torso=-12, head=-16, dx=-4, dy=8,  armL=30,  armLlo=12, armR=-52, handR=-18, fL=(-8,0),  fR=(8,0),   eye=0.3, dead=False),
    dict(torso=10,  head=20,  dx=3,  dy=24, armL=-6,  armLlo=8,  armR=4,   handR=4,   fL=(-8,0),  fR=(12,0),  eye=0.0, dead=True),
    dict(torso=26,  head=28,  dx=9,  dy=34, armL=-32, armLlo=2,  armR=-6,  handR=6,   fL=(-10,0), fR=(16,0),  eye=0.0, dead=True),
    dict(torso=38,  head=36,  dx=15, dy=40, armL=-52, armLlo=-4, armR=-14, handR=8,   fL=(-12,0), fR=(20,0),  eye=0.0, dead=True),
]
def dead_pose(k):
    a = DEAD_KEYS[k]; p = base_pose()
    p['pelvis']  = (0, a['dx'], a['dy'])
    p['torso']   = (a['torso'], 0, 0); p['head'] = (a['head'], 0, 0)
    p['mane']    = (-5*k, 0, 0)
    p['armL_up'] = (a['armL'], 0, 0); p['armL_lo'] = (a['armLlo'], 0, 0)
    p['armR']    = (a['armR'], 0, 0); p['handR'] = (a['handR'], 0, 0)
    p['cloth']   = (9*k, 0, 0)
    p['footL'] = a['fL']; p['footR'] = a['fR']
    p['_eye'] = a['eye']
    if a['dead']: p['alias'] = {'head': 'head_dead'}
    return p

# ============================================================ tone / palette
def tone_fn(lift=1.0, sat=1.18, gamma=0.74, sharpen=0.6):
    def tone(small):
        a = small[..., 3:4]
        rgb = np.where(a > 1e-4, small[..., :3]/np.maximum(a, 1e-4), 0)
        if sharpen > 0:
            blur = cv2.GaussianBlur(rgb, (0, 0), 0.9); rgb = rgb + (rgb-blur)*sharpen
        rgb = np.clip(rgb, 0, 255)/255.0
        rgb = np.power(rgb, gamma)*lift
        g = rgb.mean(-1, keepdims=True); rgb = g + (rgb-g)*sat
        o = small.copy(); o[..., :3] = np.clip(rgb, 0, 1)*255*a; return o
    return tone

def build_palette(parts, tone):
    out, _ = rig.render_hi(parts, {})
    small = tone(px.to_cell(out, F, (X0, Y0), (CELL_W, CELL_H)))
    a = small[..., 3]
    rgbp = np.where(a[..., None] > 0.5, small[..., :3]/np.maximum(a[..., None], 1e-4), 0)
    return px.make_palette(rgbp[a > 0.5][None, :, :], 48,
        forced=[(255,46,46),(255,185,166),(244,240,228),(239,231,207),(138,21,34),(232,238,252),(159,176,220),(34,32,46)])

# ============================================================ overlay: mata & efek sabet
EYE_C = (255, 46, 46); EYE_HI = (255, 185, 166); EYE_GLOW = (138, 21, 34)
SLASH = (232, 238, 252); SLASH_D = (159, 176, 220)

def ref_to_cell(x, y): return (x - X0)*F, (y - Y0)*F

def put(img, x, y, c, a=255):
    x = int(round(x)); y = int(round(y))
    if 0 <= x < CELL_W and 0 <= y < CELL_H:
        img[y, x, :3] = c; img[y, x, 3] = a

def draw_eyes(img, mats, inten):
    if inten <= 0.05: return
    for i, e in enumerate(rig.EYES):
        ex, ey = rig.pt(mats, 'head', e)
        cx, cy = ref_to_cell(ex, ey); cx = int(math.floor(cx)); cy = int(math.floor(cy))
        w = 2 if i == 0 else 1
        for dx in range(w): put(img, cx+dx, cy, EYE_C)
        if inten > 0.6:
            if i == 0: put(img, cx, cy, EYE_HI)
            for dx in range(w):
                if 0 <= cy+1 < CELL_H and img[cy+1, cx+dx, 3] > 0: put(img, cx+dx, cy+1, EYE_GLOW)
        if inten > 0.95 and i == 0:
            for ox in (-1, 2):
                if img[cy, cx+ox, 3] > 0: put(img, cx+ox, cy, EYE_GLOW)

TIP_L = (44, 160)     # ujung cakar lengan kiri (koordinat referensi)
def slash_fx(img, pose, a0, a1, level):
    n = 16; pts = []
    for i in range(n+1):
        q = dict(pose); q['armL_up'] = (lerp(a0, a1, i/n), 0, 0)
        m = rig.world_mats(q)
        x, y = rig.pt(m, 'armL_lo', TIP_L)
        pts.append(ref_to_cell(x, y))
    for s_i, off in enumerate((-4.0, 0.0, 4.0)):
        for i in range(len(pts)-1):
            t = i/(len(pts)-1)
            if level == 1 and t < 0.45: continue
            if level == 3 and (i % 3 == 2 or t > 0.8): continue
            if level >= 2 and t < 0.12: continue
            x0, y0 = pts[i]; x1, y1 = pts[i+1]
            dx, dy = x1-x0, y1-y0; L = math.hypot(dx, dy) or 1
            nx, ny = -dy/L, dx/L
            o = off*(0.5 + 0.5*t)
            steps = int(L*2)+1
            for j in range(steps+1):
                u = j/steps
                X = lerp(x0, x1, u) + nx*o; Y = lerp(y0, y1, u) + ny*o
                put(img, X, Y, SLASH if (s_i == 1 or level == 2) else SLASH_D)
                if level == 2 and s_i == 1: put(img, X+1, Y+1, SLASH_D)

# ============================================================ render
def render_pose(parts, pose, pal, tone):
    out, mats = rig.render_hi(parts, pose)
    small = px.to_cell(out, F, (X0, Y0), (CELL_W, CELL_H))
    img = px.finalize(tone(small), pal)
    draw_eyes(img, mats, pose.get('_eye', 0.7))
    fx = pose.get('_fx', 0)
    if fx == 1: slash_fx(img, pose, 22, -20, 1)
    elif fx == 2: slash_fx(img, pose, -8, -70, 2)
    elif fx == 3: slash_fx(img, pose, -52, -96, 3)
    return img

def strip(frames, bg=(205,222,236), scale=3, path='strip.png', cols=None):
    cols = cols or len(frames); rows = (len(frames)+cols-1)//cols
    sheet = Image.new('RGBA', (cols*CELL_W, rows*CELL_H), bg+(255,))
    for i, im in enumerate(frames):
        sheet.alpha_composite(Image.fromarray(im, 'RGBA'), ((i%cols)*CELL_W, (i//cols)*CELL_H))
    sheet = sheet.resize((sheet.width*scale, sheet.height*scale), Image.NEAREST).convert('RGB')
    sheet.save(path); print(path, sheet.size)
