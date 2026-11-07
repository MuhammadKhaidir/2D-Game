import numpy as np, cv2
from PIL import Image
import rig

OUTLINE = (11, 13, 22)

def downscale(out_hi, f, win, size):
    """out_hi premultiplied (CH,CW,4); win=(x0,y0) ref coords of cell origin; size=(W,H) game px -> straight RGBA float (H,W,4) with soft alpha"""
    x0, y0 = win
    W, H = size
    # affine ref->cell : scale f, translate
    A = np.array([[f, 0, (-x0 + rig.OX) * f], [0, f, (-y0 + rig.OY) * f]], np.float64)
    # supersample: bring into cell with area-like filtering by first blurring slightly then warping
    small = cv2.warpAffine(out_hi, A, (W, H), flags=cv2.INTER_AREA)   # (INTER_AREA for warp acts like linear; so pre-resize below)
    return small

def to_cell(out_hi, f, win, size):
    x0, y0 = win; W, H = size
    # 1) crop window in canvas coords (pad with zeros if outside)
    cx0 = int(round(x0 + rig.OX)); cy0 = int(round(y0 + rig.OY))
    cw = int(round(W / f)); ch = int(round(H / f))
    pad = np.zeros((ch, cw, 4), np.float32)
    sx0, sy0 = max(cx0, 0), max(cy0, 0)
    sx1, sy1 = min(cx0 + cw, rig.CW), min(cy0 + ch, rig.CH)
    if sx1 > sx0 and sy1 > sy0:
        pad[sy0-cy0:sy1-cy0, sx0-cx0:sx1-cx0] = out_hi[sy0:sy1, sx0:sx1]
    small = cv2.resize(pad, (W, H), interpolation=cv2.INTER_AREA)
    return small

def finalize(small, palette=None, thr=0.5, outline=True):
    a = small[..., 3]
    rgb = np.where(a[..., None] > 1e-4, small[..., :3] / np.maximum(a[..., None], 1e-4), 0)
    rgb = np.clip(rgb, 0, 255)
    mask = a >= thr
    img = np.zeros(a.shape + (4,), np.uint8)
    rgbq = rgb.copy()
    if palette is not None:
        d = ((rgb[:, :, None, :] - palette[None, None, :, :]) ** 2).sum(-1)
        rgbq = palette[d.argmin(-1)]
    img[..., :3] = np.clip(rgbq, 0, 255).astype(np.uint8)
    img[..., 3] = np.where(mask, 255, 0)
    if outline:
        from scipy import ndimage as ndi
        ring = ndi.binary_dilation(mask, structure=np.array([[0,1,0],[1,1,1],[0,1,0]], bool)) & ~mask
        img[ring, :3] = OUTLINE; img[ring, 3] = 255
    return img

def make_palette(samples_rgb, n=40, forced=()):
    im = Image.fromarray(samples_rgb.astype(np.uint8))
    q = im.quantize(colors=n, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    pal = np.array(q.getpalette()[:n*3], np.float32).reshape(-1, 3)
    if forced:
        pal = np.vstack([pal, np.array(forced, np.float32)])
    return pal
