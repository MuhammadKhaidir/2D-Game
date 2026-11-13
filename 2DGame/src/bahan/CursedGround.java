package bahan;

import java.awt.image.BufferedImage;

/**
 * Menggambar "kulit" tanah terkutuk + jurang di ujung dunia untuk satu chunk peta (di-bake sekali, lalu tinggal
 * ditempel tiap frame). Cara kerjanya sama dengan {@link IceGround}: gambar RGBA {@link IceGround#PPT} piksel per tile,
 * piksel yang tidak tertutup dibiarkan transparan sehingga rumput di bawahnya kelihatan. Makin ke tengah biome makin
 * banyak piksel tanah abu-abu gelap (dither + gumpalan), makin ke pinggir makin jarang, jadi hutan perlahan "membusuk"
 * menjadi tanah terkutuk, bukan garis potong.
 *
 * Tambahan dibanding salju:
 *  - sel jurang ({@link WorldGen#isVoid}) digambar penuh: hitam keunguan berbintang, dan tepi daratan di sebelahnya
 *    digerogoti noise (rapuh) dengan bibir tebing yang menyala
 *
 * Murni fungsi dari koordinat dunia, jadi chunk bersebelahan nyambung dan hasilnya selalu sama.
 */
public final class CursedGround {

    private CursedGround() {}

    private static final int PPT = IceGround.PPT;

    // ------------------------------------------------------------------ palet (ARGB)
    private static final int G_HI   = 0xFF4B3C60;
    private static final int G_1    = 0xFF352B45;
    private static final int G_2    = 0xFF2B2238;
    private static final int G_3    = 0xFF221A2C;
    private static final int G_RIM  = 0xFF130D1A;
    private static final int ASH    = 0xFF74698A;
    private static final int SPARK  = 0xFFC9A0FF;

    private static final int V_0 = 0xFF06030A, V_1 = 0xFF0C0714, V_2 = 0xFF150B24;
    private static final int STAR = 0xFF7A52BE, STAR_R = 0xFFB03A5E;
    private static final int LIP_1 = 0xFFA06AF0, LIP_2 = 0xFF5C34A0;

    private static final int NS1 = 0x4D11, NS2 = 0x2A63, NS3 = 0x79C1, NS4 = 0x1E37, NS5 = 0x6B05, NS6 = 0x3A9D,
                             NS7 = 0x5F29, NS8 = 0x2C71, NS9 = 0x7D43, NS10 = 0x13B9;

    private static final byte M_NONE = 0, M_GROUND = 1, M_VOID = 2, M_LIP = 3;

    /**
     * @return gambar (size*PPT) x (size*PPT) beserta tile yang tertutup penuh, atau null kalau chunk ini sama sekali
     *         tidak terkena kutukan.
     */
    public static IceGround.Result bake(WorldGen gen, int col0, int row0, int size) {
        final int W = size * PPT;
        final int PW = W + 2;                 // lebar dengan pinggiran 1 px (untuk bayangan tepi yang pas di batas chunk)

        // --- curse() di kisi 1/2 tile, mulai dari 1 tile sebelum chunk
        final int GS = (size + 2) * 2 + 1;
        float[] curG = new float[GS * GS];
        float maxCur = 0f;
        for (int gy = 0; gy < GS; gy++) {
            for (int gx = 0; gx < GS; gx++) {
                float tx = col0 - 1 + gx * 0.5f, ty = row0 - 1 + gy * 0.5f;
                float cv = gen.curse(tx, ty);
                curG[gy * GS + gx] = cv;
                if (cv > maxCur) maxCur = cv;
            }
        }

        // --- tile mana yang jurang, tile mana yang boleh tertutup tanah (pinggiran 1 tile)
        final int TS = size + 2;
        boolean[] voidT = new boolean[TS * TS];
        boolean[] elig = new boolean[TS * TS];
        boolean anyVoid = false;
        for (int r = 0; r < TS; r++) {
            for (int c = 0; c < TS; c++) {
                int cc = col0 - 1 + c, rr = row0 - 1 + r;
                if (cc < 0 || rr < 0 || cc >= WorldGen.WORLD_COLS || rr >= WorldGen.WORLD_ROWS) continue;
                boolean v = gen.isVoid(cc, rr);
                voidT[r * TS + c] = v;
                if (v) anyVoid = true;
                else elig[r * TS + c] = WorldGen.isGrassLike(gen.baseTile(cc, rr));
            }
        }
        if (maxCur < 0.04f && !anyVoid) return null;

        byte[] mask = new byte[PW * PW];
        int[] colr = new int[PW * PW];
        final int gx0 = col0 * PPT - 1, gy0 = row0 * PPT - 1;      // koordinat piksel seni dunia dari piksel (0,0) berpinggiran
        boolean any = false;

        for (int py = 0; py < PW; py++) {
            int gy = gy0 + py;
            int tr = Math.floorDiv(gy, PPT) - (row0 - 1);
            int ly = Math.floorMod(gy, PPT);
            float v = (py + 0.5f) / 8f;                            // posisi di kisi 1/2 tile (1 sel = 8 piksel seni)
            int v0 = (int) v; if (v0 > GS - 2) v0 = GS - 2;
            float fv = v - v0;
            for (int px = 0; px < PW; px++) {
                int gx = gx0 + px;
                int tc = Math.floorDiv(gx, PPT) - (col0 - 1);
                int lx = Math.floorMod(gx, PPT);
                int ti = tr * TS + tc;
                int idx = py * PW + px;

                // ---- jurang: digambar penuh
                if (voidT[ti]) {
                    mask[idx] = M_VOID;
                    colr[idx] = voidColor(gx, gy);
                    any = true;
                    continue;
                }
                if (!elig[ti]) continue;

                // ---- tepi daratan di sebelah jurang: digerogoti noise, dengan bibir yang menyala
                float dv = 99f;
                if (tc > 0 && voidT[ti - 1]) dv = Math.min(dv, lx);
                if (tc < TS - 1 && voidT[ti + 1]) dv = Math.min(dv, PPT - 1 - lx);
                if (tr > 0 && voidT[ti - TS]) dv = Math.min(dv, ly);
                if (tr < TS - 1 && voidT[ti + TS]) dv = Math.min(dv, PPT - 1 - ly);
                if (tr > 0 && tc > 0 && voidT[ti - TS - 1]) dv = Math.min(dv, (float) Math.hypot(lx, ly));
                if (tr > 0 && tc < TS - 1 && voidT[ti - TS + 1]) dv = Math.min(dv, (float) Math.hypot(PPT - 1 - lx, ly));
                if (tr < TS - 1 && tc > 0 && voidT[ti + TS - 1]) dv = Math.min(dv, (float) Math.hypot(lx, PPT - 1 - ly));
                if (tr < TS - 1 && tc < TS - 1 && voidT[ti + TS + 1]) dv = Math.min(dv, (float) Math.hypot(PPT - 1 - lx, PPT - 1 - ly));
                if (dv < 90f) {
                    float e = 2.2f + 3.4f * WorldGen.vnoise(gx * 0.30f, gy * 0.30f, NS8) + 1.3f * WorldGen.rnd(gx, gy, NS9);
                    if (dv < e - 1.4f) {
                        mask[idx] = M_VOID;
                        colr[idx] = voidColor(gx, gy);
                        any = true;
                        continue;
                    }
                    if (dv < e) {
                        mask[idx] = M_LIP;
                        colr[idx] = (dv < e - 0.6f) ? LIP_1 : LIP_2;
                        any = true;
                        continue;
                    }
                }

                // ---- tanah terkutuk
                float u = (px + 0.5f) / 8f;
                int u0 = (int) u; if (u0 > GS - 2) u0 = GS - 2;
                float fu = u - u0;
                int i00 = v0 * GS + u0;
                float cv = lerp2(curG[i00], curG[i00 + 1], curG[i00 + GS], curG[i00 + GS + 1], fu, fv);
                if (cv < 0.03f) continue;

                float white = WorldGen.rnd(gx, gy, NS3);
                float clump = 0.55f * WorldGen.vnoise(gx * 0.060f, gy * 0.060f, NS1)
                            + 0.30f * WorldGen.vnoise(gx * 0.150f + 7f, gy * 0.150f + 3f, NS2)
                            + 0.15f * WorldGen.vnoise(gx * 0.330f + 2f, gy * 0.330f + 9f, NS4);
                float t = 0.84f * clump + 0.16f * white;
                t = (t - 0.20f) / 0.60f;
                if (t > 1f) t = 1f;
                float cover = smoothstep(0.04f, 0.70f, cv) * 1.10f;
                if (!(cover > t)) continue;

                mask[idx] = M_GROUND;
                colr[idx] = groundColor(gx, gy, white, cv);
                any = true;
            }
        }
        if (!any) return null;

        // ---- tepi: ketebalan tanah (bawah gelap, atas terang)
        int[] out = new int[W * W];
        for (int py = 0; py < W; py++) {
            for (int px = 0; px < W; px++) {
                int idx = (py + 1) * PW + (px + 1);
                byte m = mask[idx];
                if (m == M_NONE) continue;
                int c = colr[idx];
                if (m == M_GROUND) {
                    byte up = mask[idx - PW], dn = mask[idx + PW];
                    if (dn == M_NONE) c = G_RIM;
                    else if (up == M_NONE) c = G_HI;
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
        return new IceGround.Result(img, covered);
    }

    // ------------------------------------------------------------------ warna

    private static int groundColor(int gx, int gy, float white, float cv) {
        float drift = 0.80f * WorldGen.fbm(gx * 0.030f, gy * 0.030f, NS5, 3) + 0.20f * WorldGen.vnoise(gx * 0.12f, gy * 0.12f, NS6);

        if (white < 0.0045f && cv > 0.5f) return SPARK;
        if (white > 0.992f) return ASH;
        if (drift < 0.30f) return G_3;
        if (drift < 0.42f) return G_2;
        if (drift > 0.66f) return G_HI;
        return G_1;
    }

    private static int voidColor(int gx, int gy) {
        float w = WorldGen.rnd(gx, gy, NS7);
        if (w < 0.0035f) return STAR;
        if (w < 0.0046f) return STAR_R;
        float warp = WorldGen.vnoise(gx * 0.02f, gy * 0.02f, NS8) * 6f;
        float sw = 0.6f * WorldGen.vnoise(gx * 0.045f + warp, gy * 0.060f, NS10) + 0.4f * WorldGen.vnoise(gx * 0.11f, gy * 0.11f + warp, NS9);
        if (sw > 0.62f) return V_2;
        if (sw > 0.46f) return V_1;
        return V_0;
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
