import numpy as np, random, math
from PIL import Image, ImageDraw

def hexc(h):
    h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))

# palet
SNOW  = [hexc(x) for x in ('FFFFFF','EEF5FC','D6E6F5','B4CCE6','95B1D3')]   # terang -> gelap
OUTL  = hexc('15222B')
FOL   = [hexc(x) for x in ('5FAE7A','3F8A63','2B6B52','1F5142','173D36')]
ICE   = [hexc(x) for x in ('F2FCFF','C5F0FF','86D3F5','4FA8E6','2F78C4','1D4F94')]

class Canvas:
    def __init__(s,w,h):
        s.w=w;s.h=h
        s.px=np.zeros((h,w,4),np.uint8)
    def mask_poly(s,pts):
        im=Image.new('L',(s.w,s.h),0)
        ImageDraw.Draw(im).polygon([(float(x),float(y)) for x,y in pts],fill=255)
        return np.array(im)>0
    def mask_ellipse(s,cx,cy,rx,ry):
        yy,xx=np.mgrid[0:s.h,0:s.w]
        return ((xx+0.5-cx)/rx)**2+((yy+0.5-cy)/ry)**2<=1.0
    def mask_line(s,x0,y0,x1,y1,w0,w1=None):
        # garis tebal (tapering) -> mask
        if w1 is None: w1=w0
        im=Image.new('L',(s.w,s.h),0); d=ImageDraw.Draw(im)
        n=int(max(abs(x1-x0),abs(y1-y0))*2)+2
        for i in range(n+1):
            t=i/n; x=x0+(x1-x0)*t; y=y0+(y1-y0)*t; r=(w0+(w1-w0)*t)/2
            d.ellipse([x-r,y-r,x+r,y+r],fill=255)
        return np.array(im)>0
    def put(s,mask,color,alpha=255):
        s.px[mask]=(color[0],color[1],color[2],alpha)
    def put_arr(s,mask,cols):
        # cols: array (h,w,3)
        s.px[mask,:3]=cols[mask]; s.px[mask,3]=255
    def opaque(s): return s.px[:,:,3]>0
    def outline(s,mask,color=OUTL):
        # 1 px di luar mask (4-neighbour)
        m=mask
        d=np.zeros_like(m)
        d[1:,:]|=m[:-1,:]; d[:-1,:]|=m[1:,:]; d[:,1:]|=m[:,:-1]; d[:,:-1]|=m[:,1:]
        ring=d&~m
        s.put(ring,color); return ring
    def save(s,path,scale=4):
        im=Image.fromarray(s.px,'RGBA')
        im=im.resize((s.w*scale,s.h*scale),Image.NEAREST)
        im.save(path)
    def img(s): return Image.fromarray(s.px,'RGBA')

def smooth_noise_1d(n,seed,scale=4):
    rnd=random.Random(seed)
    pts=[rnd.random() for _ in range(n//scale+3)]
    out=[]
    for i in range(n):
        x=i/scale; i0=int(x); f=x-i0; f=f*f*(3-2*f)
        out.append(pts[i0]*(1-f)+pts[i0+1]*f)
    return out

def snow_caps(c, depth=2, seed=0, prob=1.0):
    """Salju di permukaan yang menghadap atas: piksel buram pertama di tiap kolom (setelah transparan)."""
    rnd=random.Random(seed)
    op=c.opaque()
    h,w=op.shape
    for x in range(w):
        y=0
        while y<h:
            if op[y,x] and (y==0 or not op[y-1,x]):
                run=0
                while y+run<h and op[y+run,x]: run+=1
                if rnd.random()<prob:
                    dd=depth+(1 if rnd.random()<0.35 else 0)
                    dd=max(1,min(dd,run-2 if run>3 else 1))
                    for k in range(dd):
                        col=SNOW[0] if k==0 else (SNOW[1] if k<dd-1 else SNOW[2])
                        c.px[y+k,x,:3]=col
                y+=run
            else:
                y+=1
