import numpy as np, cv2
from PIL import Image
from scipy import ndimage as ndi

from pathlib import Path
REF = str(Path(__file__).resolve().parent / 'werewolf_reference.png')

# ---------- poligon bagian badan (koordinat piksel gambar referensi) ----------
POLYS = {
 'mane':    [(48,2),(116,2),(116,66),(96,66),(88,50),(66,47),(50,38)],
 'torso':   [(84,42),(114,40),(141,58),(147,84),(139,103),(120,109),(96,107),(82,99),(79,70)],
 'legL_up': [(68,102),(106,102),(108,130),(101,153),(78,153),(69,140)],
 'legL_lo': [(44,150),(106,150),(106,190),(44,190)],
 'legR_up': [(116,94),(152,94),(160,120),(158,143),(144,147),(121,147),(114,124)],
 'legR_lo': [(110,143),(168,143),(170,190),(110,190)],
 'armR':    [(126,76),(192,72),(192,99),(158,99),(126,106)],
 'handR':   [(156,98),(204,98),(204,148),(156,148)],
 'armL_up': [(50,34),(91,34),(93,70),(82,76),(66,80),(58,99),(24,99),(24,68),(38,60)],
 'armL_lo': [(22,98),(66,98),(72,108),(71,150),(70,168),(22,168)],
 'head':    [(106,2),(170,2),(178,50),(178,72),(168,80),(158,87),(148,89),(139,86),(127,77),(111,69),(103,52),(104,24)],
}
ORDER = ['mane','torso','legL_up','legL_lo','legR_up','legR_lo','cloth','armR','handR','armL_up','armL_lo','head']

# poligon area "badan penuh" buat underlay (diisi pas bagian depan bergerak)
UNDER = {
 'torso':   [(66,38),(114,34),(144,56),(150,86),(142,108),(120,118),(96,118),(76,106),(64,72)],
 'legL_up': [(66,98),(108,98),(110,132),(103,156),(76,156),(66,140)],
 'legR_up': [(112,90),(154,90),(162,120),(160,146),(144,150),(119,150),(112,124)],
 'mane':    [(48,2),(116,2),(116,66),(96,66),(88,50),(66,47),(50,38)],
}
CLOTH_REGION = [(72,90),(128,90),(126,142),(96,152),(74,126)]

def poly_mask(shape, pts):
    m = np.zeros(shape[:2], np.uint8)
    cv2.fillPoly(m, [np.array(pts, np.int32)], 1)
    return m.astype(bool)

def figure_mask(rgb, th=18):
    lum = rgb.max(axis=2)
    dark = lum < th
    lab, n = ndi.label(dark)
    border = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:,0], lab[:,-1]]))) - {0}
    bg = np.isin(lab, list(border))
    fig = ~bg
    fl, fn = ndi.label(fig)
    sizes = ndi.sum(fig, fl, range(1, fn+1))
    fig = fl == (np.argmax(sizes)+1)
    return ndi.binary_fill_holes(fig)

def segment():
    rgb = np.array(Image.open(REF).convert('RGB'))
    M = figure_mask(rgb)
    h, w = M.shape
    label = np.full((h, w), -1, np.int32)
    for i, name in enumerate(ORDER):
        if name == 'cloth':
            r = rgb.astype(int)
            brown = ((r[:,:,0]-r[:,:,2]) > 7) & (r[:,:,0] > 40)
            brown = ndi.binary_closing(brown, iterations=1) & poly_mask(rgb.shape, CLOTH_REGION)
            label[brown & M] = i
        else:
            label[poly_mask(rgb.shape, POLYS[name]) & M] = i
    # kantong bayangan gelap di antara lengan kiri & badan -> ikut torso (tetap di belakang pas lengan gerak)
    lum = rgb.max(axis=2)
    zone = poly_mask(rgb.shape, [(56,92),(86,92),(86,130),(60,130)]) & M & (lum < 46)
    label[zone] = ORDER.index('torso')
    # piksel figur yang belum kebagian -> ikut label terdekat
    un = (label < 0) & M
    if un.any():
        idx = ndi.distance_transform_edt(label < 0, return_distances=False, return_indices=True)
        label[un] = label[idx[0][un], idx[1][un]]
    label[~M] = -1
    return rgb, M, label

if __name__ == '__main__':
    rgb, M, label = segment()
    rng = np.random.RandomState(3)
    cols = (rng.rand(len(ORDER),3)*200+55).astype(np.uint8)
    vis = np.zeros_like(rgb)
    for i in range(len(ORDER)):
        vis[label==i] = cols[i]
    over = (rgb*0.45 + vis*0.55).astype(np.uint8); over[~M] = 0
    S = 5
    im = Image.fromarray(over).resize((rgb.shape[1]*S, rgb.shape[0]*S), Image.NEAREST)
    from PIL import ImageDraw
    d = ImageDraw.Draw(im)
    for i,name in enumerate(ORDER):
        ys,xs = np.nonzero(label==i)
        if len(xs): d.text((xs.mean()*S-18, ys.mean()*S-5), name, fill=(255,255,255))
    im.save('seg_vis.png'); print('ok', {n:int((label==i).sum()) for i,n in enumerate(ORDER)})
