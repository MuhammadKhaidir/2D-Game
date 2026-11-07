import numpy as np, cv2
from scipy import ndimage as ndi
import seg

def _fur_detail(rgb, box, sigma=3.0):
    x0, y0, x1, y1 = box
    patch = rgb[y0:y1, x0:x1].astype(np.float32)
    low = cv2.GaussianBlur(patch, (0, 0), sigma)
    return patch - low, patch.reshape(-1, 3).mean(0)

def _tile(detail, h, w, rng):
    ph, pw = detail.shape[:2]
    out = np.zeros((h, w, 3), np.float32)
    for y in range(0, h, ph):
        for x in range(0, w, pw):
            t = detail
            if rng.rand() < .5: t = t[:, ::-1]
            if rng.rand() < .5: t = t[::-1]
            t = np.roll(t, (rng.randint(ph), rng.randint(pw)), (0, 1))
            hh, ww = min(ph, h-y), min(pw, w-x)
            out[y:y+hh, x:x+ww] = t[:hh, :ww]
    return out

def make_back_parts(rgb, M, label, parts_ref):
    """parts_ref: dict nama -> gambar RGBA premultiplied ukuran referensi (sebelum kanvas). return {torso_back, head_back}"""
    rng = np.random.RandomState(7)
    h, w = M.shape
    idx = {n: i for i, n in enumerate(seg.ORDER)}
    det_b, mean_b = _fur_detail(rgb, (58, 44, 86, 66), 3.0)       # bulu bahu (lembut, cerah)
    det_h, mean_h = _fur_detail(rgb, (96, 28, 112, 62), 2.5)      # bulu leher/pipi (lebih kasar)
    det = _tile(det_b, h, w, rng)*1.25
    det2 = _tile(det_h, h, w, rng)*1.1
    out = {}

    # ---------- punggung ----------
    T = parts_ref['torso']; at = T[..., 3] > 0.5
    ys, xs = np.nonzero(at)
    y_top, y_bot = ys.min(), ys.max()
    yy, xx = np.mgrid[0:h, 0:w]
    grad = 1.12 - 0.34*np.clip((yy - y_top)/max(1, (y_bot - y_top)), 0, 1)       # cerah di bahu, gelap di pinggang
    edge = ndi.distance_transform_edt(at)
    vign = 0.80 + 0.20*np.clip(edge/9.0, 0, 1)
    spine = 1 - 0.20*np.exp(-((xx - 112.0)/3.2)**2)                                # alur tulang belakang
    blade = 1 + 0.07*(np.exp(-((xx-96.0)/9)**2 - ((yy-62.0)/9)**2) + np.exp(-((xx-130.0)/9)**2 - ((yy-62.0)/9)**2))
    shade = (grad*vign*spine*blade)[..., None]
    col = np.clip((mean_b*0.93 + det)*shade, 0, 255)
    tb = np.zeros_like(T)
    tb[..., :3] = col*at[..., None]; tb[..., 3] = at
    # tuft bulu berbentuk V sepanjang tulang belakang + jambul di tengkuk
    dark = np.clip(mean_b*0.62, 0, 255); light = np.clip(mean_b*1.22 + 6, 0, 255)
    def px_(x, y, c):
        x = int(round(x)); y = int(round(y))
        if 0 <= x < w and 0 <= y < h and at[y, x]:
            tb[y, x, :3] = c; tb[y, x, 3] = 1.0
    for i, y in enumerate(range(50, 102, 5)):
        L = 8 - i*0.5
        for t in range(int(L)):
            px_(112 - t, y + t*0.55, dark); px_(112 + t, y + t*0.55, dark)
            px_(112 - t, y + t*0.55 - 1, light); px_(112 + t, y + t*0.55 - 1, light)
    crest = np.zeros((h, w), np.uint8)
    for k, x in enumerate(range(98, 128, 4)):
        top = 30 - (4 if k % 2 == 0 else 0) - (3 if 2 <= k <= 5 else 0)
        cv2.fillPoly(crest, [np.array([(x-2, 46), (x+1, top), (x+3, 46)], np.int32)], 1)
    crest = crest.astype(bool) & ~at
    tb[crest, :3] = np.clip(mean_b*0.9 + det[crest]*0.8, 0, 255); tb[crest, 3] = 1.0
    out['torso_back'] = tb

    # ---------- belakang kepala ----------
    H = parts_ref['head']; ah = H[..., 3] > 0.5
    # buang moncong: potong di kanan garis tengkorak, sisakan sedikit tonjolan pipi/moncong
    ell = np.zeros((h, w), np.uint8)
    cv2.ellipse(ell, (131, 44), (28, 32), 0, 0, 360, 1, -1)                       # tengkorak
    cv2.ellipse(ell, (157, 64), (11, 8), 20, 0, 360, 1, -1)                       # tonjolan pipi/moncong (sisi)
    cv2.ellipse(ell, (121, 74), (20, 8), 0, 0, 360, 1, -1)                        # tengkuk
    ear_l0 = seg.poly_mask(rgb.shape, [(114, 8), (119, 5), (129, 28), (129, 40), (117, 38), (113, 20)]) & ah
    ear_r0 = seg.poly_mask(rgb.shape, [(143, 11), (147, 8), (160, 28), (159, 37), (146, 35)]) & ah
    ab = (ell.astype(bool) | ear_l0 | ear_r0)
    ab = cv2.GaussianBlur(ab.astype(np.float32), (0, 0), 1.0) > 0.5
    # telinga: pertahankan piksel asli (hilangkan merah muda bagian dalam)
    ears = (ear_l0 | ear_r0) & ab & ah
    ed = ndi.distance_transform_edt(ab)
    vign_h = (0.78 + 0.22*np.clip(ed/8.0, 0, 1))
    top_l = 1.10 - 0.25*np.clip((yy-10)/70.0, 0, 1)
    top_l = top_l*np.where(yy > 64, 0.72, 1.0)                                       # bayangan leher di bawah kepala
    base = np.clip((mean_h*1.0 + det2)*(vign_h*top_l)[..., None], 0, 255)
    img = np.where(ears[..., None], H[..., :3]/np.maximum(H[..., 3:4], 1e-6), base)
    # de-pink telinga
    r, g, b = img[..., 0], img[..., 1], img[..., 2]
    pink = ears & (r > g + 14)
    img[..., 0] = np.where(pink, (g + b)/2 + 2, r)
    # telinga: rim gelap + bagian dalam sedikit lebih terang supaya kedua telinga kebaca dari belakang
    er = ears & ~ndi.binary_erosion(ears, iterations=1)
    img = np.where(er[..., None], img*0.55, img)
    inner = ears & ndi.binary_erosion(ears, iterations=2)
    img = np.where(inner[..., None], np.clip(img*1.18 + 6, 0, 255), img)
    hb = np.zeros_like(H)
    hb[..., :3] = img*ab[..., None]; hb[..., 3] = ab
    out['head_back'] = hb
    return out
