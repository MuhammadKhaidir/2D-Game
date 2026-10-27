from sprlib import *

def tri(t):  # gelombang segitiga 0..1
    t=t-math.floor(t); return 1-abs(2*t-1)

def make_pine(seed, tiers=4, snow_frac=0.55, slim=1.0, W=64, H=84, shade_green=0):
    c=Canvas(W,H); rnd=random.Random(seed); cx=W/2.0
    yy,xx=np.mgrid[0:H,0:W]; xc=xx+0.5; yc=yy+0.5
    # --- tanah salju
    disk=c.mask_ellipse(cx,H-8.5,24,6.8)
    c.outline(disk); c.put(disk,SNOW[1])
    rim=disk & (c.mask_ellipse(cx,H-9.6,23,5.3)==False)
    c.put(rim&(yc>H-8.5),SNOW[3])
    hi=disk & (yc<H-11) & (xc<cx+6); c.put(hi,SNOW[0])
    # --- batang
    base_y=H-17
    trunk=(xc>=cx-3.2)&(xc<=cx+3.2)&(yc>=base_y-3)&(yc<=H-9)
    c.outline(trunk)
    cols=np.zeros((H,W,3),np.uint8)
    for (r,g,b),cond in ((hexc('4B3223'),xc>cx+1.2),(hexc('6B4A34'),(xc<=cx+1.2)&(xc>cx-1.6)),(hexc('8A6446'),xc<=cx-1.6)):
        cols[cond]=(r,g,b)
    c.put_arr(trunk,cols)
    # akar kecil
    for dx in (-5,-4,4,5):
        m=(xc>=cx+dx-0.6)&(xc<=cx+dx+0.6)&(yc>=H-11)&(yc<=H-9)
        c.put(m,hexc('4B3223'))
    # --- tingkatan daun (dari bawah ke atas)
    y_top=3.0; y_bot=H-15.0
    th=(y_bot-y_top)/(1+(tiers-1)*0.60)
    hw_top=8.0*slim; hw_bot=(W/2-5)*slim
    noise=smooth_noise_1d(W,seed*7+1,4)
    noise2=smooth_noise_1d(W,seed*7+2,3)
    for k in reversed(range(tiers)):
        start=y_top+k*0.60*th; end=start+th
        hw=hw_top+(hw_bot-hw_top)*(k/(tiers-1.0)) if tiers>1 else hw_bot
        # silsuet segitiga dengan sisi agak bergerigi
        rel=(yc-start)/th
        halfw=hw*np.clip(rel,0,1)**0.95
        jag=np.array([ (noise2[x]-0.5)*1.6 for x in range(W)])[None,:]
        inside=(np.abs(xc-cx)<=halfw+jag*(rel>0.15))&(rel>=0)&(yc<=end)
        # bawah bergerigi (kipas daun)
        sc=np.array([tri((x-cx)/5.5)*3.2 for x in range(W)])[None,:]
        edge=np.abs(xc-cx)/max(hw,1)
        inside&=(yc<=end-sc+edge*1.5)
        c.outline(inside)
        # warna daun
        sh=np.full((H,W),2,np.int32)
        sh[xc<cx-0.30*halfw]=1
        sh[xc>cx+0.28*halfw]=3
        bottom_dist=(end-sc+edge*1.5)-yc
        sh[(bottom_dist<2.4)]=4
        # tekstur jarum: garis miring
        tex=((xx*2+yy*3+k*5)%11==0)&(sh>=2)&(sh<4)
        sh[tex]-=1
        sh=np.clip(sh+shade_green,0,4)
        cols=np.zeros((H,W,3),np.uint8)
        for i,col in enumerate(FOL): cols[sh==i]=col
        c.put_arr(inside,cols)
        # salju: tutup bagian atas tiap kolom
        for x in range(W):
            col=np.where(inside[:,x])[0]
            if len(col)==0: continue
            y0,y1=col[0],col[-1]
            e=y1-y0+1
            # tutup lebih tebal di dekat puncak tingkat, menipis ke tepi
            frac=snow_frac+0.22*(noise[x]-0.5)*2
            frac*= (1.0-0.30*min(1,abs(x+0.5-cx)/max(hw,1)))
            s=max(2,int(round(e*frac)))
            if k==0: s=max(s,int(e*0.8))
            for j in range(min(s,e)):
                y=y0+j
                if not inside[y,x]: continue
                if j==0: cc=SNOW[0]
                elif j>=s-1: cc=SNOW[3]
                elif j>=s-3: cc=SNOW[2]
                else: cc=SNOW[1]
                if x+0.5>cx+0.30*hw and j>0 and j<s-1 and cc==SNOW[1]: cc=SNOW[2]
                if x+0.5<cx-0.35*hw and j<s-3 and cc==SNOW[1] and ((x+j)%5==0): cc=SNOW[0]
                c.px[y,x,:3]=cc
    return c

if __name__=='__main__':
    import sys
    sheet=Image.new('RGBA',(5*270,360),(110,125,140,255))
    for i,(seed,t,sf,sl,sg) in enumerate([(1,4,0.55,1.0,0),(2,3,0.62,1.1,0),(3,4,0.40,0.9,0),(4,5,0.50,0.85,0),(5,4,0.70,1.0,0)]):
        c=make_pine(seed,t,sf,sl,W=64,H=84,shade_green=sg)
        sheet.alpha_composite(c.img().resize((64*4,84*4),Image.NEAREST),(i*270+6,6))
    sheet.save('pines_test.png')
