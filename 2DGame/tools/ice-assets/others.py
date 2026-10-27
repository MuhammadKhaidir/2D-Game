from sprlib import *
from pines import tri

BARK=[hexc(x) for x in ('A58B76','7E6757','5C4A3E','3E3029')]
BARK_OUT=hexc('1B1512')
ROCK=[hexc(x) for x in ('C9D8E8','9FB6CE','7690AE','56708F','3D5473')]
ROCK_OUT=hexc('14202E')

def snow_mound(c, cx, cy, rx, ry):
    H,W=c.h,c.w
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    disk=c.mask_ellipse(cx,cy,rx,ry)
    c.outline(disk); c.put(disk,SNOW[1])
    low=disk & ~c.mask_ellipse(cx,cy-1.4,rx-1,ry-1.2)
    c.put(low&(yc>cy),SNOW[3])
    c.put(disk&(yc<cy-ry*0.35)&(xc<cx+rx*0.2),SNOW[0])
    return disk

def make_dead(seed, W=84, H=104, K=1.42):
    rnd=random.Random(seed)
    c=Canvas(W,H); cx=W/2.0
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    snow_mound(c,cx,H-9.5,27,7.6)
    tree=np.zeros((H,W),bool)
    def seg(x0,y0,x1,y1,w0,w1):
        nonlocal tree
        tree|=c.mask_line(x0,y0,x1,y1,w0,w1)
    def branch(x,y,ang,length,wd,depth):
        x1=x+math.sin(ang)*length; y1=y-math.cos(ang)*length
        w1=max(1.15,wd*0.60)
        mx=(x+x1)/2+rnd.uniform(-1.5,1.5); my=(y+y1)/2+rnd.uniform(-1.2,1.2)
        seg(x,y,mx,my,wd,(wd+w1)/2); seg(mx,my,x1,y1,(wd+w1)/2,w1)
        if depth>=2 or length<7: return
        for sgn in (-1,1):
            if rnd.random()<0.85:
                branch(x1,y1,ang+sgn*rnd.uniform(0.45,0.85),length*rnd.uniform(0.55,0.70),w1,depth+1)
    lean=rnd.uniform(-0.10,0.10)
    top_x=cx+lean*34*K; top_y=H-11-46*K
    mid_x=cx+lean*16*K+rnd.uniform(-1.5,1.5); mid_y=H-11-26*K
    seg(cx,H-11,mid_x,mid_y,8.4,6.0)
    seg(mid_x,mid_y,top_x,top_y,6.0,3.2)
    tree|=c.mask_poly([(cx-8.5,H-10),(cx-3.5,H-23),(cx+3.5,H-23),(cx+8.5,H-10)])
    ts=sorted([rnd.uniform(0.22,0.92) for _ in range(4)])
    side=rnd.choice([-1,1])
    for t in ts:
        by=(H-24)+((top_y)-(H-24))*t
        bx=cx+(top_x-cx)*t*0.9
        ang=side*(1.20-0.70*t+rnd.uniform(-0.15,0.15))
        ln=rnd.uniform(14,20)*K*(1.05-0.45*t)
        branch(bx,by,ang,ln,max(2.2,3.8-1.6*t),0)
        side=-side
    branch(top_x,top_y,0.36+lean,13*K,2.8,0)
    branch(top_x,top_y,-0.40+lean,12*K,2.8,0)
    c.outline(tree,BARK_OUT)
    left=np.zeros_like(tree); right=np.zeros_like(tree)
    left[:,1:]=~tree[:,:-1]; right[:,:-1]=~tree[:,1:]
    cols=np.zeros((H,W,3),np.uint8); cols[:]=BARK[2]
    cols[tree&left]=BARK[1]
    cols[tree&right]=BARK[3]
    tex=((xx*3+yy*5)%13==0)&tree
    cols[tex]=BARK[3]
    c.put_arr(tree,cols)
    snow_caps(c,depth=2,seed=seed,prob=0.95)
    op=c.opaque()
    for x in range(1,W-1):
        for y in range(10,H-26):
            if tree[y,x] and not op[y+1,x] and rnd.random()<0.05:
                L=rnd.randint(1,3)
                for k in range(1,L+1):
                    if y+k<H and not op[y+k,x]: c.px[y+k,x]=(*ICE[1],255)
    return c

def make_rock(seed, kind=0, W=52, H=44):
    rnd=random.Random(seed)
    c=Canvas(W,H); cx=W/2.0
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    body=np.zeros((H,W),bool)
    def blob(bx,by,rx,ry,n=11,jit=0.16):
        pts=[]
        phase=rnd.random()*6.28
        for i in range(n):
            a=i/n*6.2832
            r=1+rnd.uniform(-jit,jit)
            pts.append((bx+math.cos(a)*rx*r, by+math.sin(a)*ry*r))
        return c.mask_poly(pts)
    if kind==0:
        body|=blob(cx,H*0.56,19,15)
    elif kind==1:
        pts=[(cx-14,H-8),(cx-17,H*0.52),(cx-8,6),(cx+4,3),(cx+15,H*0.45),(cx+17,H-8)]
        body|=c.mask_poly(pts)
    else:
        body|=blob(cx-8,H*0.62,13,11)
        body|=blob(cx+10,H*0.58,12,14)
    sh=snow_mound(c,cx,H-7.2,22,5.2)
    c.outline(body,ROCK_OUT)
    # warna batu: cahaya dari kiri-atas
    ys,xs=np.where(body)
    bx0,bx1,by0,by1=xs.min(),xs.max(),ys.min(),ys.max()
    u=(xc-(bx0+bx1)/2)/max(1,(bx1-bx0)/2); v=(yc-(by0+by1)/2)/max(1,(by1-by0)/2)
    light=-0.55*u-0.7*v
    idx=np.full((H,W),2,np.int32)
    idx[light>0.25]=1; idx[light>0.62]=0
    idx[light<-0.28]=3; idx[light<-0.7]=4
    cols=np.zeros((H,W,3),np.uint8)
    for i,col in enumerate(ROCK): cols[idx==i]=col
    c.put_arr(body,cols)
    # retak
    for _ in range(3):
        x=rnd.uniform(bx0+6,bx1-6); y=rnd.uniform(by0+8,by1-4)
        m=c.mask_line(x,y,x+rnd.uniform(-5,5),y+rnd.uniform(3,8),1.0)
        c.put(m&body,ROCK[4])
    # salju menutupi bagian atas
    noise=smooth_noise_1d(W,seed*11+3,3)
    for x in range(W):
        col=np.where(body[:,x])[0]
        if len(col)==0: continue
        y0=col[0]; e=col[-1]-y0+1
        s=int(round(e*(0.34+0.22*noise[x])))
        for j in range(max(2,s)):
            y=y0+j
            if y>=H or not body[y,x]: break
            cc=SNOW[0] if j==0 else (SNOW[3] if j>=s-1 else (SNOW[2] if j>=s-2 else SNOW[1]))
            c.px[y,x,:3]=cc
    # kilau es
    for _ in range(3):
        x=int(rnd.uniform(bx0+4,bx1-4)); y=int(rnd.uniform(by0+s+1,by1-3))
        if y<H and body[y,x] and c.px[y,x,0]<200: c.px[y,x,:3]=ICE[1]
    return c

def crystal_poly(cx,base_y,w,h,ang):
    hb=base_y
    top=base_y-h
    sh=top+w*0.55
    pts_local=[(0,top),(w/2,sh),(w/2,hb-w*0.2),(0,hb+w*0.18),(-w/2,hb-w*0.2),(-w/2,sh)]
    ca,sa=math.cos(ang),math.sin(ang)
    out=[]
    for (px,py) in pts_local:
        dx,dy=px,py-base_y
        out.append((cx+dx*ca-dy*sa, base_y+dx*sa+dy*ca))
    return out

def draw_crystal(c,cx,base_y,w,h,ang=0.0,hue=0):
    H,W=c.h,c.w
    pts=crystal_poly(cx,base_y,w,h,ang)
    m=c.mask_poly(pts)
    c.outline(m,hexc('12305C'))
    # permukaan: 3 sisi (kiri terang, kanan gelap, atas)
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    ca,sa=math.cos(-ang),math.sin(-ang)
    lx=(xc-cx)*ca-(yc-base_y)*sa; ly=(xc-cx)*sa+(yc-base_y)*ca+base_y
    top=base_y-h; sh=top+w*0.55
    cols=np.zeros((H,W,3),np.uint8)
    face_l=(lx<0)
    cols[:]=ICE[2]
    cols[face_l]=ICE[1]
    cols[~face_l]=ICE[3]
    toprow=(ly<sh)
    cols[toprow&face_l]=ICE[0]
    cols[toprow&~face_l]=ICE[2]
    # gradien ke bawah (lebih gelap)
    low=ly>base_y-h*0.28
    cols[low&face_l]=ICE[2]
    cols[low&~face_l]=ICE[4]
    deep=ly>base_y-w*0.05
    cols[deep&~face_l]=ICE[5]
    c.put_arr(m,cols)
    # garis tepi tengah (rusuk)
    rib=m&(np.abs(lx)<0.7)&(ly>sh-1)
    c.put(rib,ICE[0])
    # kilau
    hi=m&(lx<-w*0.28)&(lx>-w*0.40)&(ly>sh+3)&(ly<base_y-h*0.3)
    c.put(hi,(255,255,255))
    return m

def make_crystal(seed, kind=0, W=56, H=70):
    rnd=random.Random(seed)
    c=Canvas(W,H); cx=W/2.0
    snow_mound(c,cx,H-6.8,21,5.0)
    items=[]
    if kind==0:
        items=[(cx-13,H-12,13,26,-0.28),(cx+13,H-11,12,23,0.30),(cx,H-10,18,44,0.0)]
    elif kind==1:
        items=[(cx-11,H-11,15,30,-0.18),(cx+10,H-11,15,35,0.14),(cx,H-9,13,22,0.04)]
    else:
        items=[(cx-9,H-10,17,48,-0.08),(cx+12,H-11,12,26,0.22)]
    for (x,b,w,h,a) in items:
        draw_crystal(c,x,b,w,h,a)
    # kerlip
    op=c.opaque()
    for _ in range(5):
        x=rnd.randint(8,W-9); y=rnd.randint(6,H-20)
        if op[y,x] and op[y,x-1] and op[y,x+1]:
            c.px[y,x,:3]=(255,255,255)
    return c

def make_glow(w=160,h=110):
    yy,xx=np.mgrid[0:h,0:w]
    d=np.sqrt(((xx+0.5-w/2)/(w/2))**2+((yy+0.5-h*0.5)/(h/2))**2)
    a=np.clip(1-d,0,1)**2
    px=np.zeros((h,w,4),np.uint8)
    px[:,:,0]=170; px[:,:,1]=225; px[:,:,2]=255
    px[:,:,3]=(a*150).astype(np.uint8)
    return Image.fromarray(px,'RGBA')

if __name__=='__main__':
    sheet=Image.new('RGBA',(1500,640),(150,175,200,255))
    x=6
    for i in range(2):
        c=make_dead(10+i); im=c.img().resize((c.w*3,c.h*3),Image.NEAREST); sheet.alpha_composite(im,(x,6)); x+=im.width+10
    for i in range(3):
        c=make_rock(20+i,i); im=c.img().resize((c.w*4,c.h*4),Image.NEAREST); sheet.alpha_composite(im,(x,6)); x+=im.width+6
    x=6
    for i in range(3):
        c=make_crystal(30+i,i); im=c.img().resize((c.w*4,c.h*4),Image.NEAREST); sheet.alpha_composite(im,(x,320)); x+=im.width+10
    g=make_glow(); sheet.alpha_composite(g,(x,360))
    sheet.save('others_test.png')


def make_drift(seed, W=44, H=22):
    """Gundukan salju rendah, bentuk organik (gabungan beberapa bulatan), tanpa garis tepi gelap."""
    rnd=random.Random(seed)
    c=Canvas(W,H)
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    body=np.zeros((H,W),bool)
    n=3+seed%2
    for i in range(n):
        bx=W*(0.22+0.56*i/(n-1))+rnd.uniform(-2,2); by=H*rnd.uniform(0.56,0.68)
        rx=W*rnd.uniform(0.16,0.27); ry=H*rnd.uniform(0.20,0.34)
        body|=c.mask_ellipse(bx,by,rx,ry)
    # tepi bawah bergerigi
    for x in range(W):
        col=np.where(body[:,x])[0]
        if len(col) and rnd.random()<0.35: body[col[-1],x]=False
    cols=np.zeros((H,W,3),np.uint8); cols[:]=SNOW[1]
    ys=np.where(body.any(axis=1))[0]; y0,y1=ys.min(),ys.max()
    rel=(yc-y0)/max(1,(y1-y0+1))
    cols[rel<0.40]=SNOW[0]
    cols[rel>0.62]=SNOW[2]
    low=body & ~np.roll(body,-1,axis=0)
    cols[low]=SNOW[3]
    cols[(rel>0.86)]=SNOW[3]
    c.put_arr(body,cols)
    return c

def make_pebbles(seed, W=40, H=26):
    rnd=random.Random(seed)
    c=Canvas(W,H)
    for (bx,by,rx,ry) in [(W*0.32,H*0.62,8,6),(W*0.64,H*0.58,6.5,5),(W*0.50,H*0.78,5,3.6)][: (3 if seed%2==0 else 2)]:
        m=c.mask_ellipse(bx,by,rx,ry)
        c.outline(m,ROCK_OUT)
        yy,xx=np.mgrid[0:H,0:W]; yc=yy+0.5; xc=xx+0.5
        cols=np.zeros((H,W,3),np.uint8); cols[:]=ROCK[2]
        cols[(yc<by-ry*0.1)|(xc<bx-rx*0.4)]=ROCK[1]
        cols[(yc>by+ry*0.35)|(xc>bx+rx*0.5)]=ROCK[3]
        c.put_arr(m,cols)
        cap=m&(yc<by-ry*0.15)
        c.put(cap,SNOW[1])
        c.put(m&(yc<by-ry*0.55),SNOW[0])
    # lapisan salju di bawah
    sn=c.mask_ellipse(W/2,H*0.86,W*0.42,H*0.14)&~c.opaque()
    c.put(sn,SNOW[2])
    return c

def make_shard(seed, W=36, H=44):
    rnd=random.Random(seed)
    c=Canvas(W,H)
    snow_mound(c,W/2,H-5,13,3.6)
    items=[(W*0.38,H-7,8,24,-0.10),(W*0.66,H-7,6,16,0.22)] if seed%2==0 else [(W*0.5,H-7,9,30,0.0),(W*0.28,H-7,5,13,-0.30),(W*0.74,H-7,5,12,0.30)]
    for (x,b,w,h,a) in items:
        draw_crystal(c,x,b,w,h,a)
    return c

def make_tuft(seed, W=34, H=32):
    rnd=random.Random(seed)
    c=Canvas(W,H)
    sn=c.mask_ellipse(W/2,H-6,W*0.40,5.0)
    c.put(sn,SNOW[1]); c.put(sn&(np.mgrid[0:H,0:W][0]+0.5>H-5),SNOW[3])
    blade=[hexc('B59A67'),hexc('8C7248'),hexc('6A5536')]
    for i in range(9):
        x=W/2+rnd.uniform(-9,9); h=rnd.uniform(11,22); lean=rnd.uniform(-0.45,0.45)
        m=c.mask_line(x,H-7,x+lean*h,H-7-h,1.5,1.0)
        c.put(m,blade[rnd.randint(0,2)])
        # ujung bersalju
        tip=c.mask_ellipse(x+lean*h,H-7-h,1.4,1.2)
        c.put(tip,SNOW[0])
    return c
