package bahan;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Seni piksel kolam portal air (tanpa file gambar baru, semuanya dihitung di kode, sama kayak WaterAnimator).
 *
 * Satu kolam = F frame (sama dengan WaterAnimator.F, jadi loop-nya mulus). Ukuran dalam "piksel seni":
 * 1 piksel seni = 3 piksel layar, 1 tile = 16 piksel seni, persis kayak tile aslinya.
 *
 * Isi satu frame (dari bawah ke atas):
 *  1. permukaan air: rumus gelombang yang SAMA dengan air dunia (WaterAnimator.artPixel), jadi riak dan kilau
 *     di kolam kelihatan satu keluarga dengan danau di sekitarnya;
 *  2. pusaran dua lengan yang berputar pelan + tiga cincin riak yang mengembang dari tengah + inti yang berdenyut;
 *  3. buih putih di tepi air yang maju-mundur (sama gayanya dengan buih pantai);
 *  4. tepi tanah berlumut + batu-batu bulat, dan empat batu rune yang berpendar bergantian;
 *  5. percikan kecil yang naik dari permukaan.
 * Bentuk kolam sengaja tidak elips polos: radiusnya digoyang beberapa gelombang sudut supaya kelihatan alami.
 */
public final class WaterPortalArt {

    private WaterPortalArt() {}

    public enum Style { PORTAL, EXIT }

    /** kanvas (piksel seni): 9 x 7 tile */
    public static final int W = 144, H = 112;
    public static final int CX = W / 2, CY = H / 2;
    /** radius air (piksel seni) -> kolam kira-kira 5,75 x 3,75 tile */
    public static final int RX = 46, RY = 30;
    /** tebal tepi tanah/batu */
    public static final int RIM = 9;
    public static final int F = WaterAnimator.F;
    /** piksel layar per piksel seni */
    public static final int SCALE = 3;

    private static final double TAU = Math.PI * 2.0;
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    // ------------------------------------------------------------------ geometri (dipakai juga oleh World.WaterPool)

    /** Bentuk air: radius ternormalisasi pada sudut th (1.0 = elips polos). */
    public static double waterShape(double th) {
        return 1.0 + 0.045 * Math.sin(3 * th + 0.6) + 0.035 * Math.sin(5 * th + 2.1) + 0.022 * Math.sin(7 * th + 4.0);
    }

    /** Panjang (piksel seni) satu satuan radius ternormalisasi pada sudut th. */
    private static double radiusAt(double th) {
        double c = RX * Math.cos(th), s = RY * Math.sin(th);
        return Math.sqrt(c * c + s * s);
    }

    /**
     * Jarak bertanda ke tepi air, dalam piksel seni. Negatif = di dalam air, positif = di luar.
     * (dx, dy) = posisi relatif terhadap pusat kolam, dalam piksel seni.
     */
    public static double signedDist(double dx, double dy) {
        double nx = dx / RX, ny = dy / RY;
        double rho = Math.sqrt(nx * nx + ny * ny);
        if (rho < 1e-6) return -RY;
        double th = Math.atan2(ny, nx);
        return (rho - waterShape(th)) * radiusAt(th);
    }

    /**
     * Penghalang jalan untuk air kolam yang berpusat di (cx, cy) piksel dunia: pita-pita persegi setebal 6 px yang mengikuti
     * bentuk elips air, menyusut 2 piksel seni dari tepi. Tepi tanah dan batu di sekelilingnya tetap bisa diinjak.
     */
    public static java.util.List<java.awt.Rectangle> blockerStrips(int cx, int cy) {
        java.util.List<java.awt.Rectangle> out = new java.util.ArrayList<>();
        int band = 6;
        for (int dy = -RY * SCALE; dy < RY * SCALE; dy += band) {
            double yc = dy + band / 2.0;
            int ext = 0;
            for (int dx = 0; dx < RX * SCALE + 30; dx++) {
                if (signedDist(dx / (double) SCALE, yc / SCALE) < -2.0) ext = dx; else break;
            }
            if (ext > 0) out.add(new java.awt.Rectangle(cx - ext, cy + dy, ext * 2, band));
        }
        return out;
    }

    private static double rimThickness(double th) {
        return RIM * (0.88 + 0.14 * Math.sin(3 * th + 0.9) + 0.10 * Math.sin(5 * th + 2.0));
    }

    // ------------------------------------------------------------------ palet

    private static final int MOSS_A = 0x4A6B3F, MOSS_B = 0x3E5C36, MOSS_C = 0x5F8548, MOSS_D = 0x2F4A2E;
    private static final int[] STONE_PORTAL = {0xB4BCC8, 0x8E9AAC, 0x606E88, 0x3A4560};   // terang, sedang, gelap, garis tepi
    private static final int[] STONE_EXIT   = {0xD6CDB6, 0xAEA48C, 0x7E7462, 0x4E463A};
    private static final int AQUA = 0x58E6D8;

    private static int rgb(int r, int g, int b) {
        return (Math.max(0, Math.min(255, r)) << 16) | (Math.max(0, Math.min(255, g)) << 8) | Math.max(0, Math.min(255, b));
    }

    private static int mix(int a, int b, double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return rgb((int) Math.round(ar + (br - ar) * t), (int) Math.round(ag + (bg - ag) * t), (int) Math.round(ab + (bb - ab) * t));
    }

    /** Satu tingkat lebih terang di palet air (tetap di palet, jadi gayanya tetap piksel seni). */
    private static int lift(int c) {
        switch (c) {
            case WaterAnimator.DEEP:    return WaterAnimator.BASE;
            case WaterAnimator.SWELL_D: return WaterAnimator.BASE;
            case WaterAnimator.BASE:    return WaterAnimator.LIGHT;
            case WaterAnimator.SWELL_L: return WaterAnimator.LIGHT;
            case WaterAnimator.LIGHT:   return WaterAnimator.CREST;
            case WaterAnimator.CREST:   return WaterAnimator.SPARK;
            default:                    return 0xFFFFFF;
        }
    }

    private static int dim(int c) {
        switch (c) {
            case WaterAnimator.SPARK: return WaterAnimator.CREST;
            case WaterAnimator.CREST: return WaterAnimator.LIGHT;
            case WaterAnimator.LIGHT: return WaterAnimator.BASE;
            case WaterAnimator.SWELL_L: return WaterAnimator.BASE;
            case WaterAnimator.BASE:  return WaterAnimator.DEEP;
            default:                  return 0x2A6FA8;
        }
    }

    private static double bayer(int x, int y) {
        return (BAYER[(y & 3) * 4 + (x & 3)] + 0.5) / 16.0;
    }

    private static int hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static double rnd01(int x, int y, int s) {
        return (hash(x, y, s) & 0xFFFF) / 65535.0;
    }

    // ------------------------------------------------------------------ pembuatan frame

    public static BufferedImage[] build(Style style) {
        final int n = W * H;
        double[] sd = new double[n];     // jarak bertanda ke tepi air (piksel seni)
        double[] rr = new double[n];     // radius ternormalisasi terhadap bentuk kolam (0 pusat, 1 tepi)
        double[] ang = new double[n];
        double[] thick = new double[n];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double dx = x + 0.5 - CX, dy = y + 0.5 - CY;
                double nx = dx / RX, ny = dy / RY;
                double rho = Math.sqrt(nx * nx + ny * ny);
                double th = Math.atan2(ny, nx);
                int i = y * W + x;
                ang[i] = th;
                double ws = waterShape(th);
                rr[i] = rho / ws;
                sd[i] = (rho - ws) * radiusAt(th);
                thick[i] = rimThickness(th);
            }
        }

        // batu tepi: sama untuk semua frame (digambar ulang tiap frame di atas dasar tepi, bersama rune yang berpendar)
        Stone[] stones = makeStones(style);

        BufferedImage[] out = new BufferedImage[F];
        for (int f = 0; f < F; f++) out[f] = renderFrame(style, f, sd, rr, ang, thick, stones);
        return out;
    }

    private static final class Stone {
        double x, y, a, b;
        int rune = -1;        // -1 = batu biasa, 0..3 = batu rune (indeks glyph)
        int seed;
    }

    private static Stone[] makeStones(Style style) {
        Random rnd = new Random(style == Style.PORTAL ? 4107L : 9301L);
        java.util.List<Stone> list = new java.util.ArrayList<>();
        int count = 30;
        for (int k = 0; k < count; k++) {
            double ph = TAU * k / count + (rnd.nextDouble() - 0.5) * 0.12;
            double ws = waterShape(ph);
            double t = rimThickness(ph);
            double mid = (t * (0.45 + 0.2 * rnd.nextDouble())) / radiusAt(ph);   // lebih sedikit ke tengah tepi
            double rho = ws + mid;
            Stone s = new Stone();
            s.x = CX + RX * rho * Math.cos(ph);
            s.y = CY + RY * rho * Math.sin(ph);
            s.a = 3.1 + rnd.nextDouble() * 2.3;
            s.b = 2.5 + rnd.nextDouble() * 1.7;
            s.seed = rnd.nextInt();
            list.add(s);
        }
        // empat batu rune di diagonal (arah mata angin dibiarkan kosong supaya player bisa berdiri di tepi)
        double[] runeAngles = {Math.PI / 4 + 0.05, 3 * Math.PI / 4 - 0.04, 5 * Math.PI / 4 + 0.03, 7 * Math.PI / 4 - 0.05};
        for (int k = 0; k < 4; k++) {
            double ph = runeAngles[k];
            double ws = waterShape(ph);
            double t = rimThickness(ph);
            double rho = ws + (t * 0.55) / radiusAt(ph);
            Stone s = new Stone();
            s.x = CX + RX * rho * Math.cos(ph);
            s.y = CY + RY * rho * Math.sin(ph);
            s.a = 6.2;
            s.b = 5.2;
            s.rune = k;
            s.seed = 1000 + k;
            list.add(s);
        }
        list.sort((p, q) -> Double.compare(p.y, q.y));   // yang di bawah menimpa yang di atas
        return list.toArray(new Stone[0]);
    }

    private static BufferedImage renderFrame(Style style, int f, double[] sd, double[] rr, double[] ang, double[] thick, Stone[] stones) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        int[] px = new int[W * H];                         // ARGB, 0 = transparan
        double phi = (double) f / F;
        double swell = 0.5 - 0.5 * Math.cos(TAU * phi);
        boolean portal = style == Style.PORTAL;

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                double s = sd[i];
                if (s < 0) {
                    px[i] = 0xFF000000 | waterPixel(x, y, f, s, rr[i], ang[i], phi, swell, portal);
                } else if (s < thick[i]) {
                    px[i] = ringPixel(x, y, s, thick[i], style);
                }
            }
        }

        // batu dan rune
        for (Stone st : stones) drawStone(px, st, style, f);

        // percikan naik dari permukaan
        drawSparks(px, f, style);

        img.setRGB(0, 0, W, H, px, 0, W);
        return img;
    }

    private static int waterPixel(int x, int y, int f, double s, double r, double th, double phi, double swell, boolean portal) {
        int c = WaterAnimator.artPixel(x + 17, y + 41, f);
        double dy = y + 0.5 - CY;

        // bayangan di bawah tepi atas (tepi bagian utara menaungi air)
        if (dy < -4 && -s < 3.0 * (-dy / RY)) c = dim(c);

        // pusaran dua lengan, berputar setengah putaran per loop (simetris 2x, jadi mulus)
        if (r < 0.82) {
            double arm = Math.sin(2 * th + 3.4 * r - TAU * phi);
            double edgeFade = r > 0.62 ? (0.82 - r) / 0.20 : 1.0;
            double thr = 0.80 + (1.0 - edgeFade) * 0.2;
            if (arm > thr) c = lift(c);
            else if (arm > thr - 0.12 && bayer(x, y) < (arm - (thr - 0.12)) / 0.12) c = lift(c);
        }

        // tiga cincin riak yang mengembang dari tengah dan memudar
        for (int k = 0; k < 3; k++) {
            double ph = (phi + k / 3.0) % 1.0;
            double radius = 0.10 + 0.82 * ph;
            double d = Math.abs(r - radius);
            if (d < 0.045) {
                double inten = 1.0 - ph;
                if (bayer(x, y) < inten * 1.25) c = (inten > 0.55 && d < 0.022) ? WaterAnimator.SPARK : WaterAnimator.CREST;
            }
        }

        // cahaya radial: tengah lebih terang (didither, bukan gradasi halus)
        double g = Math.pow(Math.max(0, 1.0 - r), 1.6) * 0.62;
        if (bayer(x, y) < g) c = lift(c);

        // inti yang berdenyut
        double coreR = 0.10 + 0.035 * Math.sin(TAU * phi);
        if (r < coreR * 0.55) c = 0xFFFFFF;
        else if (r < coreR) c = WaterAnimator.SPARK;
        else if (r < coreR + 0.07 && bayer(x, y) < 0.45) c = WaterAnimator.CREST;

        // warna khas portal: sedikit lebih toska dari air biasa
        c = mix(c, portal ? AQUA : 0xFFE9A8, portal ? 0.16 + 0.12 * Math.max(0, 1 - r) : 0.10 + 0.10 * Math.max(0, 1 - r));

        // buih di tepi air, maju-mundur (gaya sama dengan buih pantai)
        double edge = -s;
        double lim = 1.05 + 1.5 * swell;
        if (edge < lim) {
            double brk = Math.sin(TAU * ((2.0 * x + 3.0 * y) / 16.0 - 2.0 * phi));
            if (edge <= 1.05) {
                if (brk > 0.9 - 1.5 * swell) c = WaterAnimator.SPARK;
            } else if (brk > -0.2) {
                c = WaterAnimator.CREST;
            }
        }
        return c;
    }

    private static int ringPixel(int x, int y, double s, double thick, Style style) {
        double q = rnd01(x, y, 7);
        // tepi luar berumbai: makin ke luar makin jarang, jadi menyatu dengan rumput di sekitarnya
        double fray = (thick - s) / 2.4;
        if (fray < 1.0 && rnd01(x, y, 11) > fray) return 0;
        int base;
        if (style == Style.PORTAL) {
            base = q < 0.10 ? MOSS_D : q < 0.38 ? MOSS_B : q < 0.90 ? MOSS_A : MOSS_C;
            if (s < 1.6) base = mix(base, MOSS_D, 0.55);                    // bibir basah dekat air
        } else {
            base = q < 0.12 ? 0x8E8062 : q < 0.40 ? 0xA89A78 : q < 0.92 ? 0xB8AA86 : 0xCABC98;
            if (s < 1.6) base = mix(base, 0x7A6E52, 0.5);
        }
        return 0xFF000000 | base;
    }

    private static void put(int[] px, int x, int y, int rgb, double alpha) {
        if (x < 0 || y < 0 || x >= W || y >= H) return;
        int i = y * W + x;
        int dst = px[i];
        int da = (dst >>> 24) & 255;
        if (alpha >= 0.999) { px[i] = 0xFF000000 | rgb; return; }
        int a = (int) Math.round(alpha * 255);
        if (da == 0) { px[i] = (a << 24) | rgb; return; }
        int mixed = mix(dst & 0xFFFFFF, rgb, alpha);
        int na = Math.min(255, da + (int) Math.round((255 - da) * alpha));
        px[i] = (na << 24) | mixed;
    }

    private static final String[] GLYPHS = {
        ".#.|###|.#.|#.#",
        "#.#|.#.|###|.#.",
        "##.|.##|#.#|.#.",
        ".##|#..|.#.|##.",
    };

    private static void drawStone(int[] px, Stone st, Style style, int f) {
        int[] pal = style == Style.PORTAL ? STONE_PORTAL : STONE_EXIT;
        int x0 = (int) Math.floor(st.x - st.a - 2), x1 = (int) Math.ceil(st.x + st.a + 2);
        int y0 = (int) Math.floor(st.y - st.b - 3), y1 = (int) Math.ceil(st.y + st.b + 3);

        // bayangan jatuh di bawah-kanan batu
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double nx = (x + 0.5 - (st.x + 1.0)) / (st.a + 0.4), ny = (y + 0.5 - (st.y + 1.6)) / (st.b + 0.3);
                if (nx * nx + ny * ny <= 1.0) {
                    int i = y * W + x;
                    if (x >= 0 && y >= 0 && x < W && y < H && ((px[i] >>> 24) & 255) != 0) put(px, x, y, 0x1B2A24, 0.42);
                }
            }
        }
        // badan batu
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double nx = (x + 0.5 - st.x) / st.a, ny = (y + 0.5 - st.y) / st.b;
                double d2 = nx * nx + ny * ny;
                if (d2 > 1.0) continue;
                double lum = -0.62 * nx - 0.80 * ny + (rnd01(x, y, st.seed) - 0.5) * 0.35;
                int c;
                if (d2 > 0.74) c = lum > 0.38 ? pal[0] : pal[3];
                else if (lum > 0.42) c = pal[0];
                else if (lum > -0.12) c = pal[1];
                else c = pal[2];
                // lumut menempel di bagian atas beberapa batu
                if (style == Style.PORTAL && st.rune < 0 && ny < -0.15 && ((st.seed >> 3) & 1) == 0 && rnd01(x, y, st.seed + 5) > 0.55 && d2 < 0.8) c = MOSS_C;
                put(px, x, y, c, 1.0);
            }
        }
        if (st.rune >= 0) drawRune(px, st, style, f);
    }

    private static void drawRune(int[] px, Stone st, Style style, int f) {
        double pulse = 0.5 + 0.5 * Math.sin(TAU * f / F + st.rune * Math.PI / 2.0);   // bergantian searah jarum jam
        int glow = style == Style.PORTAL ? 0x7DFCF0 : 0xFFD866;
        int core = style == Style.PORTAL ? 0xE6FFFC : 0xFFF3C4;

        // pendar di sekeliling batu
        for (int y = (int) (st.y - st.b - 5); y <= (int) (st.y + st.b + 5); y++) {
            for (int x = (int) (st.x - st.a - 5); x <= (int) (st.x + st.a + 5); x++) {
                double nx = (x + 0.5 - st.x) / st.a, ny = (y + 0.5 - st.y) / st.b;
                double d = Math.sqrt(nx * nx + ny * ny);
                if (d > 1.0 && d < 1.85) {
                    double a = (1.0 - (d - 1.0) / 0.85) * (0.12 + 0.46 * pulse);
                    if (bayer(x, y) < a * 1.6) put(px, x, y, glow, Math.min(0.75, a * 1.5));
                }
            }
        }
        // glyph di tengah batu (4 baris x 3 kolom)
        String[] rows = GLYPHS[st.rune % GLYPHS.length].split("\\|");
        int gx = (int) Math.round(st.x) - 1, gy = (int) Math.round(st.y) - 2;
        for (int ry = 0; ry < rows.length; ry++) {
            for (int rx = 0; rx < 3; rx++) {
                if (rows[ry].charAt(rx) == '#') put(px, gx + rx, gy + ry, pulse > 0.45 ? core : glow, 1.0);
            }
        }
    }

    private static void drawSparks(int[] px, int f, Style style) {
        Random rnd = new Random(style == Style.PORTAL ? 77L : 78L);
        int n = 14;
        for (int k = 0; k < n; k++) {
            double ang = rnd.nextDouble() * TAU;
            double rad = Math.sqrt(rnd.nextDouble()) * 0.92;
            double sx = CX + RX * rad * Math.cos(ang);
            double sy = CY + RY * rad * Math.sin(ang);
            double speed = (k % 3 == 0) ? 2.0 : 1.0;                       // bilangan bulat -> loop mulus
            double ph = ((double) f / F * speed + rnd.nextDouble()) % 1.0;
            double rise = 18.0 * ph;
            double a = Math.sin(Math.PI * ph);
            int x = (int) Math.round(sx + 1.5 * Math.sin(TAU * (ph + k * 0.13)));
            int y = (int) Math.round(sy - rise);
            if (a < 0.12) continue;
            int col = (k % 4 == 0) ? 0xFFFFFF : (style == Style.PORTAL ? 0xBFFFF8 : 0xFFF0B8);
            put(px, x, y, col, Math.min(1.0, a * 1.1));
            if (k % 4 == 0 && a > 0.6) {                                    // beberapa percikan berbentuk tanda tambah kecil
                put(px, x - 1, y, col, a * 0.55);
                put(px, x + 1, y, col, a * 0.55);
                put(px, x, y - 1, col, a * 0.55);
                put(px, x, y + 1, col, a * 0.55);
            }
        }
    }
}
