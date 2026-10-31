package bahan;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Seni piksel untuk Water Domain, semuanya dihitung di kode (tanpa file gambar baru), sama seperti WaterAnimator.
 * 1 piksel seni = 3 piksel layar, 1 tile = 16 x 16 piksel seni = 48 piksel layar, jadi gayanya sama dengan tile dunia.
 *
 * Isinya:
 *  - {@link Tiles}: tile lantai batu, pasir, batu tebing, dinding tebing, plus lapisan tepi (bibir batu, pasir basah, buih air).
 *  - {@link Sprite}: properti berdiri / dekorasi (pilar, obelisk rune, patung penjaga, batu, karang, kristal, kerang,
 *    daun teratai, gerbang kuil, lingkaran rune lantai, cahaya pendar).
 * Semua dibuat sekali saat Water Domain pertama kali dimasuki (lihat WaterDomainMap).
 */
public final class WaterDomainArt {

    private WaterDomainArt() {}

    public static final int ART = 16, PX = 3, T = ART * PX;
    public static final int FOAM_FRAMES = 8;
    /** bit sisi tile: utara, timur, selatan, barat */
    public static final int N = 1, E = 2, S = 4, W = 8;

    private static final double TAU = Math.PI * 2.0;

    // ------------------------------------------------------------------ palet
    static final int FL_A = 0x4A7285, FL_B = 0x41657A, FL_C = 0x5A8497, SEAM = 0x24404F, HL = 0x7BA9BD;
    static final int MOSS = 0x3F7D5E, MOSS_D = 0x2F5F49;
    static final int SAND_A = 0xD2C394, SAND_D = 0xB7A777, SAND_L = 0xE6DAB2, SAND_R = 0xC6B787;
    static final int ROCK_A = 0x0E1A26, ROCK_B = 0x122434, ROCK_C = 0x0A141E, ROCK_F = 0x1D3A52;
    static final int STONE_L = 0xB4C8D2, STONE_M = 0x8AA3B3, STONE_D = 0x5F7A8E, STONE_DD = 0x42586B, STONE_OUT = 0x2B3C4C;
    public static final int AQUA = 0x7DFCF0, AQUA_CORE = 0xE6FFFC;

    // ------------------------------------------------------------------ alat bantu

    static int rgb(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    static int mix(int a, int b, double t) {
        if (t <= 0) return a;
        if (t >= 1) return b;
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return rgb((int) Math.round(ar + (br - ar) * t), (int) Math.round(ag + (bg - ag) * t), (int) Math.round(ab + (bb - ab) * t));
    }

    static int shade(int c, double f) {
        return rgb((int) Math.round(((c >> 16) & 255) * f), (int) Math.round(((c >> 8) & 255) * f), (int) Math.round((c & 255) * f));
    }

    static int hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    static double rnd01(int x, int y, int s) {
        return (hash(x, y, s) & 0xFFFF) / 65535.0;
    }

    /** Kanvas piksel seni ARGB (0 = transparan). */
    static final class Canvas {
        final int w, h;
        final int[] p;

        Canvas(int w, int h) { this.w = w; this.h = h; this.p = new int[w * h]; }

        boolean in(int x, int y) { return x >= 0 && y >= 0 && x < w && y < h; }

        void set(int x, int y, int rgb) { if (in(x, y)) p[y * w + x] = 0xFF000000 | rgb; }

        boolean solid(int x, int y) { return in(x, y) && (p[y * w + x] >>> 24) != 0; }

        int get(int x, int y) { return in(x, y) ? p[y * w + x] & 0xFFFFFF : 0; }

        /** campur warna di atas piksel yang ada (alpha 0..1) */
        void blend(int x, int y, int rgb, double a) {
            if (!in(x, y) || a <= 0) return;
            int i = y * w + x;
            int dst = p[i];
            int da = (dst >>> 24) & 255;
            if (a >= 0.999) { p[i] = 0xFF000000 | rgb; return; }
            if (da == 0) { p[i] = ((int) Math.round(a * 255) << 24) | rgb; return; }
            int na = Math.min(255, da + (int) Math.round((255 - da) * a));
            p[i] = (na << 24) | mix(dst & 0xFFFFFF, rgb, a);
        }

        void rect(int x, int y, int rw, int rh, int rgb) {
            for (int yy = y; yy < y + rh; yy++) for (int xx = x; xx < x + rw; xx++) set(xx, yy, rgb);
        }

        void line(int x0, int y0, int x1, int y1, int rgb) {
            int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
            for (int guard = 0; guard < 4000; guard++) {
                set(x0, y0, rgb);
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 >= dy) { err += dy; x0 += sx; }
                if (e2 <= dx) { err += dx; y0 += sy; }
            }
        }

        void disc(double cx, double cy, double rx, double ry, int rgb) {
            for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++) {
                for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++) {
                    double nx = (x + 0.5 - cx) / rx, ny = (y + 0.5 - cy) / ry;
                    if (nx * nx + ny * ny <= 1.0) set(x, y, rgb);
                }
            }
        }

        /** Gelapkan piksel tepi (yang bertetangga dengan transparan) supaya sprite punya garis tepi. */
        void outline(double f) {
            int[] src = p.clone();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = src[y * w + x];
                    if ((c >>> 24) == 0) continue;
                    boolean edge = !isSolid(src, x - 1, y) || !isSolid(src, x + 1, y) || !isSolid(src, x, y - 1) || !isSolid(src, x, y + 1);
                    if (edge) p[y * w + x] = 0xFF000000 | shade(c & 0xFFFFFF, f);
                }
            }
        }

        private boolean isSolid(int[] a, int x, int y) { return in(x, y) && (a[y * w + x] >>> 24) != 0; }

        BufferedImage image(int scale) {
            int W2 = w * scale, H2 = h * scale;
            BufferedImage img = new BufferedImage(W2, H2, BufferedImage.TYPE_INT_ARGB);
            int[] d = new int[W2 * H2];
            for (int y = 0; y < H2; y++) {
                int row = (y / scale) * w;
                for (int x = 0; x < W2; x++) d[y * W2 + x] = p[row + x / scale];
            }
            img.setRGB(0, 0, W2, H2, d, 0, W2);
            return img;
        }
    }

    /** Sprite dengan titik pijak (kaki) di dalam gambar, dalam piksel layar. */
    public static final class Sprite {
        public final BufferedImage img;
        public final int w, h, footX, footY;

        Sprite(BufferedImage img, int footArtX, int footArtY) {
            this.img = img;
            this.w = img.getWidth();
            this.h = img.getHeight();
            this.footX = footArtX * PX;
            this.footY = footArtY * PX;
        }
    }

    // ================================================================== TILE

    public static final class Tiles {
        public final BufferedImage[] floor = new BufferedImage[4];
        public final BufferedImage[] sand = new BufferedImage[3];
        public final BufferedImage[] rock = new BufferedImage[4];
        public final BufferedImage[] face = new BufferedImage[3];
        /** lapisan tepi batu (sisi terbuka N/E/W), per bitmask */
        public final BufferedImage[] rockEdge = new BufferedImage[16];
        /** bibir lantai batu yang menghadap air dalam, per bitmask sisi berair */
        public final BufferedImage[] floorLip = new BufferedImage[16];
        /** tepi tipis lantai batu yang menghadap air dangkal (bisa dilangkahi), per bitmask */
        public final BufferedImage[] floorEdge = new BufferedImage[16];
        /** tepi pasir basah, per bitmask sisi berair */
        public final BufferedImage[] sandWet = new BufferedImage[16];
        /** buih di tile air, per bitmask sisi yang berbatasan dengan darat, per frame */
        public final BufferedImage[][] foam = new BufferedImage[16][FOAM_FRAMES];
    }

    public static Tiles buildTiles() {
        Tiles t = new Tiles();
        for (int v = 0; v < t.floor.length; v++) t.floor[v] = floorTile(v).image(PX);
        for (int v = 0; v < t.sand.length; v++) t.sand[v] = sandTile(v).image(PX);
        for (int v = 0; v < t.rock.length; v++) t.rock[v] = rockTile(v).image(PX);
        for (int v = 0; v < t.face.length; v++) t.face[v] = faceTile(v).image(PX);
        for (int m = 0; m < 16; m++) {
            t.rockEdge[m] = rockEdge(m).image(PX);
            t.floorLip[m] = floorLip(m).image(PX);
            t.floorEdge[m] = floorEdge(m).image(PX);
            t.sandWet[m] = sandWet(m).image(PX);
            for (int f = 0; f < FOAM_FRAMES; f++) t.foam[m][f] = foam(m, f).image(PX);
        }
        return t;
    }

    private static void slab(Canvas c, int x0, int y0, int w, int h) {
        for (int x = x0; x < x0 + w; x++) { c.set(x, y0, HL); c.set(x, y0 + h - 1, SEAM); }
        for (int y = y0; y < y0 + h; y++) { c.set(x0, y, HL); c.set(x0 + w - 1, y, SEAM); }
        c.set(x0 + w - 1, y0, FL_C);
        c.set(x0, y0 + h - 1, FL_B);
    }

    static Canvas floorTile(int v) {
        Canvas c = new Canvas(ART, ART);
        int seed = 100 + v * 7;
        for (int y = 0; y < ART; y++) {
            for (int x = 0; x < ART; x++) {
                double n = rnd01(x, y, seed);
                c.set(x, y, n < 0.20 ? FL_B : (n > 0.86 ? FL_C : FL_A));
            }
        }
        switch (v) {
            case 0: slab(c, 0, 0, 8, 16); slab(c, 8, 0, 8, 16); break;
            case 1: slab(c, 0, 0, 16, 8); slab(c, 0, 8, 16, 8); break;
            case 2: slab(c, 0, 0, 8, 8); slab(c, 8, 0, 8, 8); slab(c, 0, 8, 8, 8); slab(c, 8, 8, 8, 8); break;
            default:
                slab(c, 0, 0, 16, 16);
                for (int i = 0; i < 9; i++) c.set(3 + i, 12 - (int) (i * 0.9), SEAM);       // retakan diagonal
                c.set(5, 12, FL_B); c.set(9, 6, FL_B);
                break;
        }
        // lumut di sekitar sambungan
        for (int y = 0; y < ART; y++) {
            for (int x = 0; x < ART; x++) {
                if (rnd01(x, y, seed + 3) > 0.935) c.set(x, y, rnd01(x, y, seed + 4) > 0.5 ? MOSS : MOSS_D);
            }
        }
        return c;
    }

    static Canvas sandTile(int v) {
        Canvas c = new Canvas(ART, ART);
        int seed = 200 + v * 11;
        for (int y = 0; y < ART; y++) {
            for (int x = 0; x < ART; x++) {
                double n = rnd01(x, y, seed);
                int col = n < 0.12 ? SAND_D : (n > 0.90 ? SAND_L : SAND_A);
                // riak pasir: garis landai putus-putus
                double wave = Math.sin(TAU * (x / 16.0 + v * 0.31) + y * 0.7);
                if ((y == 4 + v || y == 11 - v) && wave > 0.15) col = SAND_R;
                c.set(x, y, col);
            }
        }
        // kerikil kecil
        for (int k = 0; k < 2 + v; k++) {
            int x = 1 + (int) (rnd01(k, v, seed + 9) * 13), y = 1 + (int) (rnd01(v, k, seed + 10) * 13);
            c.set(x, y, 0x9A8B63); c.set(x + 1, y, 0xB9AA7F); c.set(x, y + 1, 0x85774F);
        }
        return c;
    }

    static Canvas rockTile(int v) {
        Canvas c = new Canvas(ART, ART);
        int seed = 300 + v * 13;
        for (int y = 0; y < ART; y++) {
            for (int x = 0; x < ART; x++) {
                double n = rnd01(x, y, seed);
                int col = n < 0.22 ? ROCK_C : (n > 0.80 ? ROCK_B : ROCK_A);
                if (n > 0.975) col = ROCK_F;
                c.set(x, y, col);
            }
        }
        // retakan gelap
        int sx = 2 + (int) (rnd01(v, 1, seed) * 10), sy = 2 + (int) (rnd01(v, 2, seed) * 10);
        for (int i = 0; i < 5; i++) c.set(sx + i, sy + (i % 2), 0x070E15);
        return c;
    }

    static Canvas faceTile(int v) {
        Canvas c = new Canvas(ART, ART);
        int seed = 400 + v * 17;
        int[] strip = {0x1D4260, 0x183850, 0x20496A, 0x15324A};
        for (int x = 0; x < ART; x++) {
            int col = strip[(int) (rnd01(x / 3, v, seed) * strip.length) % strip.length];
            for (int y = 2; y < 13; y++) {
                double n = rnd01(x, y, seed + 1);
                int cc = n > 0.9 ? shade(col, 1.25) : (n < 0.18 ? shade(col, 0.82) : col);
                if (y > 8) cc = shade(cc, 0.86);
                c.set(x, y, cc);
            }
        }
        for (int x = 0; x < ART; x++) {
            c.set(x, 0, 0x4C86AA);                                 // bibir atas tebing
            c.set(x, 1, rnd01(x, 0, seed + 2) > 0.5 ? 0x336A8C : 0x2D5F80);
            c.set(x, 13, 0x102739);
            c.set(x, 14, 0x0D2132);
            c.set(x, 15, 0x0A1B29);
        }
        // rembesan air: garis terang tipis yang turun
        for (int k = 0; k < 2; k++) {
            int x = 2 + (int) (rnd01(k, v, seed + 5) * 12), len = 4 + (int) (rnd01(v, k, seed + 6) * 5);
            for (int y = 3; y < 3 + len; y++) c.set(x, y, 0x3A7396);
        }
        return c;
    }

    static Canvas rockEdge(int mask) {
        Canvas c = new Canvas(ART, ART);
        for (int i = 0; i < ART; i++) {
            if ((mask & N) != 0) { c.set(i, 0, 0x2F5C7B); c.set(i, 1, 0x1F3F57); }
            if ((mask & E) != 0) { c.set(ART - 1, i, 0x2F5C7B); c.set(ART - 2, i, 0x1F3F57); }
            if ((mask & W) != 0) { c.set(0, i, 0x2F5C7B); c.set(1, i, 0x1F3F57); }
        }
        return c;
    }

    static Canvas floorLip(int mask) {
        Canvas c = new Canvas(ART, ART);
        for (int i = 0; i < ART; i++) {
            if ((mask & N) != 0) { c.set(i, 0, 0x28444F); c.set(i, 1, 0x6A98AC); }
            if ((mask & E) != 0) { c.set(ART - 1, i, 0x28444F); c.set(ART - 2, i, 0x5C8A9E); }
            if ((mask & W) != 0) { c.set(0, i, 0x28444F); c.set(1, i, 0x5C8A9E); }
        }
        if ((mask & S) != 0) {   // sisi depan peron: dinding batu yang turun ke air
            for (int x = 0; x < ART; x++) {
                c.set(x, 11, 0x7FAEC2);
                c.set(x, 12, 0x3E6377);
                c.set(x, 13, 0x2F4F62);
                c.set(x, 14, 0x243F51);
                c.set(x, 15, 0x1A3040);
            }
            for (int y = 12; y < ART; y++) { c.set(0, y, 0x1A3040); c.set(8, y, 0x1A3040); }
        }
        return c;
    }

    static Canvas floorEdge(int mask) {
        Canvas c = new Canvas(ART, ART);
        for (int i = 0; i < ART; i++) {
            if ((mask & N) != 0) { c.set(i, 0, 0x2F5A6B); c.set(i, 1, 0x5E8EA3); }
            if ((mask & S) != 0) { c.set(i, ART - 1, 0x2F5A6B); c.set(i, ART - 2, 0x3E6B80); }
            if ((mask & E) != 0) { c.set(ART - 1, i, 0x2F5A6B); c.set(ART - 2, i, 0x3E6B80); }
            if ((mask & W) != 0) { c.set(0, i, 0x2F5A6B); c.set(1, i, 0x5E8EA3); }
        }
        return c;
    }

    static Canvas sandWet(int mask) {
        Canvas c = new Canvas(ART, ART);
        for (int i = 0; i < ART; i++) {
            for (int d = 0; d < 3; d++) {
                double a = d == 0 ? 0.95 : (d == 1 ? 0.7 : 0.4);
                if ((mask & N) != 0) c.blend(i, d, 0x9A8D66, a);
                if ((mask & S) != 0) c.blend(i, ART - 1 - d, 0x9A8D66, a);
                if ((mask & E) != 0) c.blend(ART - 1 - d, i, 0x9A8D66, a);
                if ((mask & W) != 0) c.blend(d, i, 0x9A8D66, a);
            }
        }
        return c;
    }

    /** Buih di tile air: pita putus-putus di sisi yang berbatasan dengan darat, lebarnya maju-mundur mengikuti frame f. */
    static Canvas foam(int mask, int f) {
        Canvas c = new Canvas(ART, ART);
        double phi = (double) f / FOAM_FRAMES;
        for (int i = 0; i < ART; i++) {
            double wv = 0.5 + 0.5 * Math.sin(TAU * (i / 16.0 * 2.0 - phi));
            int depth = 1 + (int) Math.round(1.4 * wv);
            for (int d = 0; d < depth; d++) {
                int col = d == 0 ? 0xEEF9FF : 0x9CD4F2;
                if (d > 0 && rnd01(i, d, f % 4) > 0.72) continue;
                if (d == 0 && rnd01(i, 0, 17 + (f >> 1)) > 0.86) continue;
                if ((mask & N) != 0) c.set(i, d, col);
                if ((mask & S) != 0) c.set(i, ART - 1 - d, col);
                if ((mask & E) != 0) c.set(ART - 1 - d, i, col);
                if ((mask & W) != 0) c.set(d, i, col);
            }
        }
        return c;
    }

    // ================================================================== SPRITE

    private static Sprite sprite(Canvas c, int footX, int footY) {
        return new Sprite(c.image(PX), footX, footY);
    }

    // ---- pilar -------------------------------------------------------

    public static Sprite pillar(int seed, boolean broken) {
        Canvas c = new Canvas(20, 46);
        Random rnd = new Random(seed * 7919L + (broken ? 13 : 5));
        int[] col = {STONE_L, STONE_L, STONE_M, STONE_M, STONE_M, STONE_M, STONE_D, STONE_D, STONE_DD, STONE_DD};
        int top = broken ? 15 + rnd.nextInt(10) : 10;
        // kaki pilar
        c.rect(3, 39, 14, 5, STONE_M);
        c.rect(3, 39, 14, 1, STONE_L);
        c.rect(14, 39, 3, 5, STONE_D);
        c.rect(3, 43, 14, 1, STONE_DD);
        // batang
        for (int y = top; y < 39; y++) {
            for (int x = 5; x < 15; x++) {
                int cc = col[x - 5];
                if ((x == 8 || x == 12) && y % 2 == 0) cc = shade(cc, 0.88);     // alur
                c.set(x, y, cc);
            }
        }
        if (!broken) {
            c.rect(3, 6, 14, 4, STONE_M);                                          // kepala pilar
            c.rect(3, 6, 14, 1, STONE_L);
            c.rect(14, 6, 3, 4, STONE_D);
            c.rect(2, 3, 16, 3, STONE_L);
            c.rect(14, 3, 4, 3, STONE_M);
            c.rect(2, 5, 16, 1, STONE_D);
        } else {
            for (int x = 5; x < 15; x++) {                                         // puncak patah bergerigi
                int cut = top + rnd.nextInt(4);
                for (int y = top; y < cut; y++) { if (c.solid(x, y)) c.p[y * c.w + x] = 0; }
                c.set(x, cut, STONE_L);
            }
            for (int k = 0; k < 3; k++) {                                          // serpihan di kaki
                int rx = 1 + rnd.nextInt(16), ry = 41 + rnd.nextInt(3);
                c.rect(rx, ry, 2 + rnd.nextInt(2), 2, rnd.nextBoolean() ? STONE_M : STONE_D);
            }
        }
        // lumut + noda air di bagian bawah
        for (int y = top; y < 44; y++) {
            for (int x = 3; x < 17; x++) {
                if (!c.solid(x, y)) continue;
                double bottom = (y - 28) / 16.0;
                if (bottom > 0 && rnd.nextDouble() < bottom * 0.5) c.set(x, y, rnd.nextBoolean() ? MOSS : MOSS_D);
                else if (rnd.nextDouble() < 0.035) c.set(x, y, STONE_OUT);
            }
        }
        c.outline(0.62);
        return sprite(c, 10, 44);
    }

    /** Kotak tabrakan kaki pilar relatif ke titik pijak (piksel layar): {dx, dy, lebar, tinggi}. */
    public static int[] pillarFoot() { return new int[] {-18, -20, 36, 17}; }

    // ---- obelisk rune ------------------------------------------------

    private static final String[] GLYPHS = {
        ".#.|###|.#.|#.#|...", "#.#|.#.|###|.#.|...", "##.|.##|#.#|.#.|...", ".##|#..|.#.|##.|...",
    };

    /** {badan, lapisan rune bersinar} */
    public static Sprite[] obelisk(int seed) {
        Canvas body = new Canvas(16, 46);
        Canvas glow = new Canvas(16, 46);
        for (int y = 6; y < 41; y++) {
            int half = 3 + (y - 6) * 3 / 35;                                       // makin ke bawah makin lebar
            for (int x = 8 - half; x <= 7 + half; x++) {
                double n = rnd01(x, y, seed);
                int cc = x < 8 ? (n > 0.8 ? 0x4A647B : 0x3A546A) : (n > 0.8 ? 0x2B4054 : 0x1F3041);
                body.set(x, y, cc);
            }
        }
        for (int y = 0; y < 6; y++) {                                              // puncak piramida
            int half = 1 + y / 2;
            for (int x = 8 - half; x <= 7 + half; x++) body.set(x, y, x < 8 ? 0x5B7B94 : 0x34495C);
        }
        body.rect(3, 41, 10, 3, 0x2A3F52);                                         // alas
        body.rect(2, 43, 12, 2, 0x1D2D3C);
        body.rect(3, 41, 10, 1, 0x4A647B);
        for (int g = 0; g < 4; g++) {
            String[] rows = GLYPHS[(seed + g) % GLYPHS.length].split("\\|");
            int gy = 11 + g * 7;
            for (int ry = 0; ry < 4; ry++) {
                for (int rx = 0; rx < 3; rx++) {
                    if (rows[ry].charAt(rx) == '#') {
                        body.set(6 + rx, gy + ry, 0x2A5E78);                       // rune padam
                        glow.set(6 + rx, gy + ry, AQUA);
                    }
                }
            }
        }
        body.outline(0.6);
        return new Sprite[] {sprite(body, 8, 44), sprite(glow, 8, 44)};
    }

    // ---- patung penjaga ---------------------------------------------

    /** {badan, mata bersinar}; menghadap ke bawah, memegang trisula. */
    public static Sprite[] statue(int seed) {
        Canvas c = new Canvas(28, 56);
        Canvas g = new Canvas(28, 56);
        // alas
        c.rect(3, 46, 22, 8, STONE_M);
        c.rect(3, 46, 22, 1, STONE_L);
        c.rect(17, 46, 8, 8, STONE_D);
        c.rect(3, 53, 22, 1, STONE_DD);
        c.rect(5, 44, 18, 2, STONE_L);
        // jubah (trapesium)
        for (int y = 18; y < 44; y++) {
            int half = 4 + (y - 18) * 5 / 26;
            for (int x = 13 - half; x <= 14 + half; x++) {
                int cc = x < 11 ? STONE_L : (x < 15 ? STONE_M : (x < 19 ? STONE_D : STONE_DD));
                if (rnd01(x, y, seed) > 0.93) cc = shade(cc, 0.85);
                c.set(x, y, cc);
            }
        }
        for (int y = 30; y < 44; y += 3) c.set(13, y, STONE_DD);                  // lipatan
        // bahu + lengan
        c.rect(7, 18, 14, 4, STONE_M);
        c.rect(7, 18, 14, 1, STONE_L);
        // kepala berkerudung
        c.disc(14, 12, 5.2, 6.0, STONE_M);
        c.disc(13, 11, 3.6, 4.4, STONE_L);
        c.rect(11, 11, 6, 4, STONE_DD);                                            // bayangan wajah di dalam kerudung
        g.set(12, 12, AQUA); g.set(15, 12, AQUA);
        c.set(12, 12, 0x2A5E78); c.set(15, 12, 0x2A5E78);
        // trisula di tangan kanan (sisi kiri layar)
        c.rect(3, 6, 2, 40, 0x6B8396);
        c.set(3, 6, STONE_L); c.set(3, 7, STONE_L);
        c.rect(1, 4, 1, 5, 0x8FB4C6); c.rect(3, 2, 2, 5, 0xA9CBDA); c.rect(6, 4, 1, 5, 0x8FB4C6);
        c.rect(1, 8, 6, 1, 0x8FB4C6);
        // lumut di alas
        for (int x = 3; x < 25; x++) for (int y = 46; y < 54; y++) if (rnd01(x, y, seed + 3) > 0.9) c.set(x, y, MOSS);
        c.outline(0.6);
        return new Sprite[] {sprite(c, 14, 54), sprite(g, 14, 54)};
    }

    // ---- batu --------------------------------------------------------

    public static Sprite boulder(int seed, int wArt, int hArt) {
        Canvas c = new Canvas(wArt + 4, hArt + 5);
        double cx = (wArt + 4) / 2.0, cy = hArt * 0.55 + 1;
        // bayangan
        for (int y = 0; y < c.h; y++) for (int x = 0; x < c.w; x++) {
            double nx = (x + 0.5 - cx - 0.5) / (wArt / 2.0 + 1.5), ny = (y + 0.5 - (c.h - 2.5)) / 2.2;
            if (nx * nx + ny * ny <= 1.0) c.blend(x, y, 0x0B1822, 0.45);
        }
        for (int y = 0; y < c.h; y++) {
            for (int x = 0; x < c.w; x++) {
                double ang = Math.atan2(y + 0.5 - cy, x + 0.5 - cx);
                double r = 1.0 + 0.10 * Math.sin(3 * ang + seed) + 0.06 * Math.sin(5 * ang + seed * 0.7);
                double nx = (x + 0.5 - cx) / (wArt / 2.0 * r), ny = (y + 0.5 - cy) / (hArt * 0.5 * r);
                double d2 = nx * nx + ny * ny;
                if (d2 > 1.0) continue;
                double lum = -0.55 * nx - 0.85 * ny + (rnd01(x, y, seed) - 0.5) * 0.35;
                int cc = lum > 0.42 ? 0x6E8799 : (lum > -0.05 ? 0x516B7E : 0x374E60);
                if (d2 > 0.8) cc = shade(cc, 0.78);
                if (ny < -0.2 && rnd01(x, y, seed + 7) > 0.55 && d2 < 0.85) cc = rnd01(x, y, seed + 8) > 0.5 ? MOSS : MOSS_D;   // lumut di atas
                c.set(x, y, cc);
            }
        }
        c.outline(0.6);
        return sprite(c, (wArt + 4) / 2, hArt + 2);
    }

    // ---- karang ------------------------------------------------------

    static final int[][] CORAL_PAL = {
        {0xFF8FB1, 0xFFC0D4, 0xC8527C},   // merah muda
        {0xFFA24D, 0xFFCB8C, 0xC4671F},   // jingga
        {0xA070E8, 0xCDAEFF, 0x6B3FB0},   // ungu
        {0x35C9B5, 0x86F3E2, 0x1F8C80},   // toska
    };

    public static Sprite coral(int kind, int pal, int seed) {
        int[] p = CORAL_PAL[Math.floorMod(pal, CORAL_PAL.length)];
        Random rnd = new Random(seed * 31L + kind * 977L + pal);
        Canvas c;
        int fx, fy;
        switch (Math.floorMod(kind, 4)) {
            case 0: {   // bercabang
                c = new Canvas(30, 32);
                branch(c, 15, 30, -Math.PI / 2, 9.5, 2.2, 3, p, rnd);
                fx = 15; fy = 30;
                break;
            }
            case 1: {   // kipas
                c = new Canvas(28, 26);
                int bx = 14, by = 24;
                for (int a = -72; a <= 72; a += 3) {
                    double ang = Math.toRadians(a - 90);
                    double len = 13 + 3 * Math.sin(a * 0.23 + seed) + (a % 12 == 0 ? 2 : 0);
                    for (double r = 2; r <= len; r += 0.5) {
                        int x = (int) Math.round(bx + Math.cos(ang) * r), y = (int) Math.round(by + Math.sin(ang) * r * 0.95);
                        boolean rib = (a % 12 == 0);
                        if (rib || (x + y) % 3 != 0) c.set(x, y, rib ? p[1] : (r > len - 2 ? p[1] : p[0]));
                    }
                }
                c.rect(12, 23, 4, 3, p[2]);
                fx = 14; fy = 25;
                break;
            }
            case 2: {   // tabung
                c = new Canvas(24, 24);
                int[] xs = {3, 8, 13, 18};
                for (int i = 0; i < xs.length; i++) {
                    int h = 9 + rnd.nextInt(10);
                    int x0 = xs[i] + rnd.nextInt(2);
                    for (int y = 23 - h; y < 23; y++) {
                        for (int x = x0; x < x0 + 4; x++) c.set(x, y, x == x0 ? p[1] : (x >= x0 + 3 ? p[2] : p[0]));
                    }
                    c.disc(x0 + 2, 23 - h, 2.2, 1.6, p[1]);
                    c.disc(x0 + 2, 23 - h, 1.4, 0.9, shade(p[2], 0.6));
                }
                fx = 12; fy = 23;
                break;
            }
            default: {  // bulat beralur
                c = new Canvas(22, 14);
                for (int y = 0; y < 13; y++) {
                    for (int x = 0; x < 22; x++) {
                        double nx = (x + 0.5 - 11) / 10.5, ny = (y + 0.5 - 12) / 11.0;
                        if (nx * nx + ny * ny > 1.0 || y > 12) continue;
                        double groove = Math.sin(x * 0.95 + Math.sin(y * 0.8) * 2.2 + seed);
                        double lum = -0.5 * nx - 0.9 * ny;
                        int cc = groove > 0.35 ? p[2] : (lum > 0.25 ? p[1] : p[0]);
                        c.set(x, y, cc);
                    }
                }
                fx = 11; fy = 12;
                break;
            }
        }
        c.outline(0.58);
        return sprite(c, fx, fy);
    }

    private static void branch(Canvas c, double x, double y, double ang, double len, double thick, int depth, int[] p, Random rnd) {
        double ex = x + Math.cos(ang) * len, ey = y + Math.sin(ang) * len;
        int steps = (int) Math.ceil(len * 2);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = x + (ex - x) * t, py = y + (ey - y) * t;
            double rad = thick * (1.0 - 0.35 * t) / 2.0;
            c.disc(px, py, rad + 0.35, rad + 0.35, p[0]);
            c.set((int) Math.round(px - rad * 0.6), (int) Math.round(py), p[1]);
            c.set((int) Math.round(px + rad * 0.8), (int) Math.round(py), p[2]);
        }
        if (depth <= 0) { c.set((int) Math.round(ex), (int) Math.round(ey), p[1]); return; }
        int kids = depth == 3 ? 3 : 2;
        for (int k = 0; k < kids; k++) {
            double off = (k - (kids - 1) / 2.0) * (0.55 + 0.15 * rnd.nextDouble()) + (rnd.nextDouble() - 0.5) * 0.25;
            branch(c, ex, ey, ang + off, len * (0.68 + 0.1 * rnd.nextDouble()), Math.max(1.0, thick * 0.72), depth - 1, p, rnd);
        }
    }

    // ---- kristal -----------------------------------------------------

    public static final int[][] CRYSTAL_PAL = {
        {0xA6F6EE, 0x4FD8C8, 0x239C9A},   // aqua
        {0xD9C6FF, 0x9C7BF0, 0x5B3FB5},   // ungu
        {0xFFD6EA, 0xF58FC0, 0xB0467F},   // merah muda
    };

    public static Sprite crystal(int pal, int seed) {
        int[] p = CRYSTAL_PAL[Math.floorMod(pal, CRYSTAL_PAL.length)];
        Random rnd = new Random(seed * 131L + pal);
        Canvas c = new Canvas(26, 30);
        int n = 3 + rnd.nextInt(3);
        int[][] spec = new int[n][3];
        for (int i = 0; i < n; i++) {
            spec[i][0] = 5 + (int) ((i + 0.5) * 16.0 / n) + rnd.nextInt(3) - 1;        // x tengah
            spec[i][1] = 9 + rnd.nextInt(14);                                           // tinggi
            spec[i][2] = 4 + rnd.nextInt(2);                                            // lebar
        }
        // yang pendek digambar dulu
        java.util.Arrays.sort(spec, (a, b) -> Integer.compare(a[1], b[1]));
        for (int[] s : spec) {
            int bx = s[0], h = s[1], w = s[2], by = 28;
            for (int dx = -w / 2; dx <= w / 2; dx++) {
                int top = by - h + (Math.abs(dx) * 2 > w ? 3 : (Math.abs(dx) > 0 ? 1 : 0)) + (dx == 0 ? 0 : 1);
                for (int y = top; y <= by; y++) {
                    int cc = dx < 0 ? p[0] : (dx == 0 ? p[1] : p[2]);
                    if (dx == 0 && y > by - 3) cc = p[2];
                    c.set(bx + dx, y, cc);
                }
            }
            c.set(bx - 1, by - h + 3, 0xFFFFFF);                                        // kilau
        }
        c.outline(0.55);
        return sprite(c, 13, 28);
    }

    // ---- kerang, daun teratai ---------------------------------------

    public static Sprite shell(int seed) {
        Canvas c = new Canvas(10, 9);
        boolean conch = seed % 3 == 0;
        if (!conch) {
            for (int y = 0; y < 7; y++) {
                for (int x = 0; x < 10; x++) {
                    double nx = (x + 0.5 - 5) / 4.6, ny = (y + 0.5 - 6.2) / 6.0;
                    if (nx * nx + ny * ny > 1.0 || y > 6) continue;
                    c.set(x, y, (x % 2 == 0) ? 0xE6CFC6 : 0xF6E7E0);
                }
            }
            c.rect(3, 6, 4, 1, 0xB98E86);
        } else {
            c.disc(4.5, 5, 3.6, 2.8, 0xF2D6BE);
            c.disc(7.5, 4.2, 1.6, 1.4, 0xE0B79A);
            for (int x = 2; x < 7; x += 2) c.set(x, 3 + (x % 3), 0xC9977A);
        }
        c.outline(0.7);
        return sprite(c, 5, 7);
    }

    public static Sprite lilyPad(int seed, boolean flower) {
        Canvas c = new Canvas(14, 9);
        int sz = 5 + seed % 2;
        c.disc(7, 4.5, sz + 0.5, 3.2, 0x3F9A55);
        c.disc(7, 4.2, sz - 0.5, 2.5, 0x58B96B);
        int notch = Math.floorMod(seed, 4);
        for (int i = 0; i < 6; i++) {                                                  // takik bentuk V
            int x = 7 + (notch < 2 ? i : -i), y = 4 + (notch % 2 == 0 ? -(i / 3) : (i / 3));
            c.p[Math.max(0, Math.min(c.h - 1, y)) * c.w + Math.max(0, Math.min(c.w - 1, x))] = 0;
        }
        if (flower) { c.set(7, 4, 0xFFFFFF); c.set(6, 4, 0xF6A6C8); c.set(8, 4, 0xF6A6C8); c.set(7, 3, 0xF6A6C8); c.set(7, 5, 0xF6A6C8); }
        c.outline(0.72);
        return sprite(c, 7, 5);
    }

    // ---- cahaya pendar ----------------------------------------------

    /** Pendar bundar berundak (gaya piksel) dengan warna rgb. Radius dalam piksel seni. */
    public static Sprite glow(int rgb, int radius) {
        int size = radius * 2 + 1;
        Canvas c = new Canvas(size, size);
        double[] alpha = {0.34, 0.22, 0.13, 0.07};
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - radius, y - radius) / radius;
                if (d >= 1.0) continue;
                int band = Math.min(3, (int) (d * 4));
                c.blend(x, y, rgb, alpha[band]);
            }
        }
        return sprite(c, radius, radius);
    }

    // ---- gerbang kuil ------------------------------------------------

    /** {badan, rune pintu bersinar}. Gerbang besar yang disegel, lebar 56 piksel seni. */
    public static Sprite[] templeGate() {
        Canvas c = new Canvas(56, 64);
        Canvas g = new Canvas(56, 64);
        // pintu (gelap) di antara tiang
        c.rect(12, 15, 32, 43, 0x0B1A2A);
        for (int y = 15; y < 58; y++) {
            for (int x = 12; x < 44; x++) {
                double n = rnd01(x, y, 91);
                if (n > 0.93) c.set(x, y, 0x12283C);
            }
            c.set(27, y, 0x050E17); c.set(28, y, 0x16314A);
        }
        for (int x = 12; x < 44; x += 4) { c.set(x, 20, 0x2A4E68); c.set(x, 50, 0x2A4E68); }
        // tiang kiri & kanan
        int[] col = {STONE_L, STONE_L, STONE_M, STONE_M, STONE_M, STONE_D, STONE_D, STONE_DD, STONE_DD, STONE_DD};
        for (int y = 14; y < 58; y++) {
            for (int x = 0; x < 10; x++) {
                int cc = col[x];
                if ((x == 3 || x == 7) && y % 2 == 0) cc = shade(cc, 0.88);
                c.set(2 + x, y, cc);
                c.set(44 + x, y, col[x]);
            }
        }
        // balok atas (lintel)
        c.rect(0, 3, 56, 11, STONE_M);
        c.rect(0, 3, 56, 2, STONE_L);
        c.rect(0, 12, 56, 2, STONE_D);
        c.rect(40, 5, 16, 7, STONE_D);
        for (int x = 4; x < 52; x += 8) c.set(x, 8, STONE_DD);
        // batu kunci di tengah
        c.rect(22, 0, 12, 15, STONE_L);
        c.rect(30, 0, 4, 15, STONE_M);
        c.rect(22, 14, 12, 1, STONE_D);
        // tangga
        c.rect(0, 58, 56, 3, STONE_M);
        c.rect(0, 58, 56, 1, STONE_L);
        c.rect(3, 56, 50, 2, STONE_L);
        c.rect(0, 61, 56, 3, STONE_D);
        c.rect(0, 63, 56, 1, STONE_DD);
        // lumut dan noda
        for (int y = 14; y < 64; y++) for (int x = 0; x < 56; x++)
            if (c.solid(x, y) && y > 44 && rnd01(x, y, 5) > 0.88) c.set(x, y, rnd01(x, y, 6) > 0.5 ? MOSS : MOSS_D);
        // segel rune: lingkaran + gelombang
        int cx = 28, cy = 36;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 56; x++) {
                double d = Math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.0);
                boolean ring = Math.abs(d - 11) < 0.8 || Math.abs(d - 7) < 0.6;
                boolean spoke = d < 11 && d > 2 && (Math.abs((x - cx)) < 0.6 || Math.abs((y - cy)) < 0.6);
                boolean wave = d < 6 && Math.abs((y - cy) - 2.0 * Math.sin((x - cx) * 0.9)) < 0.8;
                if (ring || spoke || wave) {
                    c.set(x, y, 0x2A5E78);
                    g.set(x, y, AQUA);
                }
            }
        }
        // kilau glyph di batu kunci
        String[] rows = GLYPHS[1].split("\\|");
        for (int ry = 0; ry < 4; ry++) for (int rx = 0; rx < 3; rx++) if (rows[ry].charAt(rx) == '#') {
            c.set(27 + rx, 4 + ry, 0x2A5E78);
            g.set(27 + rx, 4 + ry, AQUA);
        }
        c.outline(0.62);
        return new Sprite[] {sprite(c, 28, 62), sprite(g, 28, 62)};
    }

    // ---- lingkaran rune di lantai -----------------------------------

    /** {dasar, pendar}: lingkaran rune berjari-jari R piksel seni. Gambar bertransparan, digambar di atas lantai. */
    public static Sprite[] runeCircle(int R) {
        int size = R * 2 + 5, cx = R + 2, cy = R + 2;
        Canvas base = new Canvas(size, size);
        Canvas glow = new Canvas(size, size);
        int marks = Math.max(12, R / 5);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
                double d = Math.hypot(dx, dy);
                if (d > R + 1.5) continue;
                double ang = Math.atan2(dy, dx);
                boolean on = false;
                if (Math.abs(d - R) <= 1.1) on = true;                                          // cincin luar
                if (Math.abs(d - (R - 7)) <= 0.7) on = true;                                    // cincin dalam
                if (d > R * 0.6 - 0.7 && d < R * 0.6 + 0.7) on = true;                          // cincin tengah
                if (d < R - 8 && d > R - 14) {                                                  // tanda rune berkeliling
                    double seg = TAU / marks;
                    double a = Math.floorMod((long) Math.round((ang + Math.PI) / seg * 1000), 1000L) / 1000.0;
                    double within = Math.abs(((ang + Math.PI) % seg) - seg / 2) * d;
                    int k = (int) Math.floor((ang + Math.PI) / seg);
                    if (within < 1.15 && (k % 2 == 0 || d < R - 10)) on = true;
                }
                double lim = R * 0.6;
                if (d < lim) {                                                                  // bintang 8 sudut
                    for (int k = 0; k < 8; k++) {
                        double a = k * Math.PI / 4;
                        double perp = Math.abs(-Math.sin(a) * dx + Math.cos(a) * dy);
                        double along = Math.cos(a) * dx + Math.sin(a) * dy;
                        if (perp < 0.65 && along > 0) on = true;
                    }
                }
                double dais = R * 0.2;
                if (Math.abs(d - dais) <= 0.9) on = true;                                       // dais tengah
                if (d < dais - 1.5) {
                    for (int k = -1; k <= 1; k++) {
                        if (Math.abs(dy - k * 4.5 - 1.8 * Math.sin(dx * 0.65)) < 0.7) on = true; // tiga gelombang
                    }
                }
                if (on) {
                    base.set(x, y, 0x2E6279);
                    glow.set(x, y, AQUA);
                }
            }
        }
        return new Sprite[] {sprite(base, cx, cy), sprite(glow, cx, cy)};
    }
}
