package bahan;

import java.awt.image.BufferedImage;

/**
 * Menggambar "kulit" salju + es untuk satu chunk peta (di-bake sekali, lalu tinggal ditempel tiap frame).
 *
 * Hasilnya gambar RGBA {@link #PPT} piksel per tile. Piksel yang tidak tertutup salju dibiarkan transparan,
 * jadi rumput di bawahnya kelihatan: makin ke tengah biome es makin banyak piksel salju (dither + gumpalan),
 * dan makin ke pinggir makin jarang, sampai akhirnya cuma tinggal bercak salju tipis di rumput.
 * Itu yang bikin peralihan hutan -> salju halus, bukan garis potong.
 *
 * Murni fungsi dari koordinat dunia (pakai noise global), jadi dua chunk bersebelahan nyambung tanpa sambungan,
 * dan chunk yang dibuang lalu dibuat lagi hasilnya persis sama.
 */
public final class IceGround {

    private IceGround() {}

    /** piksel seni per tile (sama dengan resolusi animasi air: 48 px layar / 3 px per piksel seni) */
    public static final int PPT = 16;

    // ------------------------------------------------------------------ palet (ARGB)
    static final int SN_HI   = 0xFFFAFDFF;
    static final int SN_1    = 0xFFE9F2FB;
    static final int SN_2    = 0xFFD5E5F4;
    static final int SN_3    = 0xFFBFD3E9;
    static final int SN_RIM  = 0xFFA6BEDB;
    static final int SPARK   = 0xFFFFFFFF;

    static final int IC_HI    = 0xFFE6F6FE;
    static final int IC_LIGHT = 0xFFB9E0F6;
    static final int IC_MID   = 0xFF92C9EB;
    static final int IC_DEEP  = 0xFF74B3E0;
    static final int IC_DARK  = 0xFF5B9BCF;
    static final int IC_EDGE  = 0xFF4F89BE;
    static final int IC_CRACK_L = 0xFFF3FBFF;
    static final int IC_CRACK_D = 0xFF5A94C6;

    private static final int NS1 = 0x51A3, NS2 = 0x2B77, NS3 = 0x7C13, NS4 = 0x1D55, NS5 = 0x6E21, NS6 = 0x3F09, NS7 = 0x5B8D;

    private static final byte M_NONE = 0, M_SNOW = 1, M_ICE = 2;

    /** Hasil bake satu chunk: gambar + penanda tile mana yang tertutup penuh (tile di bawahnya tidak perlu digambar). */
    public static final class Result {
        public final BufferedImage image;
        /** [baris * size + kolom] = true kalau semua piksel tile itu tidak transparan */
        public final boolean[] covered;
        Result(BufferedImage image, boolean[] covered) { this.image = image; this.covered = covered; }
    }

    /**
     * @return gambar (size*PPT) x (size*PPT) beserta tile yang tertutup penuh, atau null kalau chunk ini sama sekali tidak ada salju.
     */
    public static Result bake(WorldGen gen, int col0, int row0, int size) {
        final int W = size * PPT;
        final int PW = W + 2;                 // lebar dengan pinggiran 1 px (untuk bayangan tepi yang pas di batas chunk)

        // --- ice() dan depth() di kisi 1/2 tile, mulai dari 1 tile sebelum chunk
        final int GS = (size + 2) * 2 + 1;
        float[] iceG = new float[GS * GS];
        float[] depG = new float[GS * GS];
        float maxIce = 0f;
        for (int gy = 0; gy < GS; gy++) {
            for (int gx = 0; gx < GS; gx++) {
                float tx = col0 - 1 + gx * 0.5f, ty = row0 - 1 + gy * 0.5f;
                float iv = gen.ice(tx, ty);
                iceG[gy * GS + gx] = iv;
                if (iv > maxIce) maxIce = iv;
                depG[gy * GS + gx] = iv > 0.55f ? gen.depth(tx, ty, iv) : -9f;
            }
        }
        if (maxIce < 0.04f) return null;

        // --- tile mana yang boleh tertutup salju (pinggiran 1 tile)
        final int TS = size + 2;
        boolean[] elig = new boolean[TS * TS];
        for (int r = 0; r < TS; r++) {
            for (int c = 0; c < TS; c++) {
                int cc = col0 - 1 + c, rr = row0 - 1 + r;
                boolean ok = false;
                if (cc >= 0 && rr >= 0 && cc < WorldGen.WORLD_COLS && rr < WorldGen.WORLD_ROWS) {
                    ok = WorldGen.isGrassLike(gen.baseTile(cc, rr));
                }
                elig[r * TS + c] = ok;
            }
        }

        byte[] mask = new byte[PW * PW];
        int[] colr = new int[PW * PW];
        final int gx0 = col0 * PPT - 1, gy0 = row0 * PPT - 1;      // koordinat piksel seni dunia dari piksel (0,0) berpinggiran
        boolean any = false;

        for (int py = 0; py < PW; py++) {
            int gy = gy0 + py;
            int tr = Math.floorDiv(gy, PPT) - (row0 - 1);
            float v = (py + 0.5f) / 8f;                            // posisi di kisi 1/2 tile (1 sel = 8 piksel seni)
            int v0 = (int) v; if (v0 > GS - 2) v0 = GS - 2;
            float fv = v - v0;
            for (int px = 0; px < PW; px++) {
                int gx = gx0 + px;
                int tc = Math.floorDiv(gx, PPT) - (col0 - 1);
                if (!elig[tr * TS + tc]) continue;

                float u = (px + 0.5f) / 8f;
                int u0 = (int) u; if (u0 > GS - 2) u0 = GS - 2;
                float fu = u - u0;
                int i00 = v0 * GS + u0;
                float iv = lerp2(iceG[i00], iceG[i00 + 1], iceG[i00 + GS], iceG[i00 + GS + 1], fu, fv);
                if (iv < 0.03f) continue;

                float white = WorldGen.rnd(gx, gy, NS3);
                float clump = 0.55f * WorldGen.vnoise(gx * 0.060f, gy * 0.060f, NS1)
                            + 0.30f * WorldGen.vnoise(gx * 0.150f + 7f, gy * 0.150f + 3f, NS2)
                            + 0.15f * WorldGen.vnoise(gx * 0.330f + 2f, gy * 0.330f + 9f, NS4);
                float t = 0.84f * clump + 0.16f * white;
                t = (t - 0.20f) / 0.60f;
                if (t > 1f) t = 1f;
                float cover = smoothstep(0.04f, 0.70f, iv) * 1.10f;
                boolean snow = cover > t;

                // ---- danau / sungai beku
                boolean ice = false;
                float dpn = -9f;
                if (iv > 0.6f) {
                    float d00 = depG[i00], d01 = depG[i00 + 1], d10 = depG[i00 + GS], d11 = depG[i00 + GS + 1];
                    if (d00 > -8f && d01 > -8f && d10 > -8f && d11 > -8f) {
                        float dp = lerp2(d00, d01, d10, d11, fu, fv);
                        dpn = dp + (WorldGen.vnoise(gx * 0.22f, gy * 0.22f, NS4) - 0.5f) * 0.11f + (white - 0.5f) * 0.02f;
                        ice = dpn > 0f;
                    }
                }
                if (!snow && !ice) continue;

                int idx = py * PW + px;
                if (ice) {
                    mask[idx] = M_ICE;
                    colr[idx] = iceColor(gx, gy, dpn, white);
                } else {
                    mask[idx] = M_SNOW;
                    colr[idx] = snowColor(gx, gy, white, dpn > -0.34f && dpn <= 0f);
                }
                any = true;
            }
        }
        if (!any) return null;

        // ---- tepi: ketebalan salju (bawah gelap, atas terang) + garis bayangan di tepi es
        int[] out = new int[W * W];
        for (int py = 0; py < W; py++) {
            for (int px = 0; px < W; px++) {
                int idx = (py + 1) * PW + (px + 1);
                byte m = mask[idx];
                if (m == M_NONE) continue;
                byte up = mask[idx - PW], dn = mask[idx + PW];
                int c = colr[idx];
                if (m == M_SNOW) {
                    if (dn == M_NONE) c = SN_RIM;
                    else if (dn == M_ICE) c = SN_3;
                    else if (up == M_NONE) c = SN_HI;
                } else {
                    if (up == M_SNOW) c = IC_EDGE;
                    else if (dn == M_SNOW) c = IC_HI;
                    else if (mask[idx - 1] == M_SNOW || mask[idx + 1] == M_SNOW) c = IC_DARK;
                }
                out[py * W + px] = c;
            }
        }
        boolean[] covered = new boolean[size * size];
        for (int tr = 0; tr < size; tr++) {
            for (int tc = 0; tc < size; tc++) {
                boolean full = true;
                scan:
                for (int y = tr * PPT; y < (tr + 1) * PPT; y++) {
                    int base = y * W + tc * PPT;
                    for (int x = 0; x < PPT; x++) {
                        if ((out[base + x] >>> 24) == 0) { full = false; break scan; }
                    }
                }
                covered[tr * size + tc] = full;
            }
        }
        BufferedImage img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, W, W, out, 0, W);
        return new Result(img, covered);
    }

    // ------------------------------------------------------------------ warna

    private static int snowColor(int gx, int gy, float white, boolean bank) {
        float drift = 0.80f * WorldGen.fbm(gx * 0.030f, gy * 0.030f, NS5, 3) + 0.20f * WorldGen.vnoise(gx * 0.12f, gy * 0.12f, NS6);
        if (bank) drift += 0.14f;
        // garis angin tipis (jengger salju yang tertiup)
        float ridge = (float) Math.sin((gx + gy * 0.55f) * 0.15f + WorldGen.vnoise(gx * 0.03f, gy * 0.03f, NS7) * 11f);
        if (white < 0.0065f) return SPARK;
        if (ridge > 0.968f && drift > 0.40f) return SN_HI;
        if (white > 0.994f) return SN_2;
        if (drift < 0.27f) return SN_3;
        if (drift < 0.38f) return SN_2;
        if (drift > 0.63f) return SN_HI;
        return SN_1;
    }

    private static int iceColor(int gx, int gy, float dpn, float white) {
        // retakan: jarak ke tepi sel Voronoi (sel besar ~ 2 tile)
        final float CS = 34f;
        float px = gx / CS, py = gy / CS;
        int cx = (int) Math.floor(px), cy = (int) Math.floor(py);
        float f1 = 1e9f, f2 = 1e9f;
        int id1 = 0, id2 = 0;
        for (int j = -1; j <= 1; j++) {
            for (int i = -1; i <= 1; i++) {
                int ax = cx + i, ay = cy + j;
                float sx = ax + WorldGen.rnd(ax, ay, 0x1A2B);
                float sy = ay + WorldGen.rnd(ax, ay, 0x3C4D);
                float dx = (sx - px) * CS, dy = (sy - py) * CS;
                float d = dx * dx + dy * dy;
                int id = WorldGen.h2(ax, ay, 0x55);
                if (d < f1) { f2 = f1; id2 = id1; f1 = d; id1 = id; }
                else if (d < f2) { f2 = d; id2 = id; }
            }
        }
        float gap = (float) (Math.sqrt(f2) - Math.sqrt(f1));
        if (((id1 ^ id2) & 3) != 0) {
            if (gap < 0.9f) return IC_CRACK_L;
            if (gap < 1.7f) return IC_CRACK_D;
        }
        // goresan pantulan miring
        float streak = (gx * 0.8f + gy * 1.0f) * 0.05f + WorldGen.vnoise(gx * 0.04f, gy * 0.04f, NS6) * 6f;
        float fr = streak - (float) Math.floor(streak);
        if (fr < 0.06f) return IC_HI;
        float n = WorldGen.vnoise(gx * 0.07f, gy * 0.07f, NS2);
        float depthBias = Math.min(1f, dpn / 1.6f);
        float lvl = 0.55f * n + 0.45f * depthBias;
        if (white < 0.012f) return IC_HI;
        if (lvl < 0.30f) return IC_LIGHT;
        if (lvl < 0.50f) return IC_MID;
        if (lvl < 0.70f) return IC_DEEP;
        return IC_DARK;
    }

    // ------------------------------------------------------------------ util

    private static float lerp2(float a, float b, float c, float d, float fu, float fv) {
        float top = a + (b - a) * fu, bot = c + (d - c) * fu;
        return top + (bot - top) * fv;
    }

    private static float smoothstep(float a, float b, float x) {
        float t = (x - a) / (b - a);
        if (t < 0f) t = 0f; else if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }
}
