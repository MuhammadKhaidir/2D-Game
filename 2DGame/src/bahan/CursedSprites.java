package bahan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Gambar-gambar biome terkutuk. Semuanya dilukis lewat kode (tanpa file aset), sebagai "piksel seni" kecil yang
 * diperbesar {@link #Z} kali tanpa penghalusan, jadi gayanya sama dengan sprite biome es:
 *  - pohon kutukan yang meliuk (3 variasi) + animasi kena-pukul / tumbang (dibuat dari sprite-nya)
 *  - batu obsidian, kristal ungu, nisan, semak duri, tumpukan tulang, cahaya ungu di bawah kristal
 *  - {@link #wither}: pohon biasa yang layu (daun jadi ungu keabu-abuan dan rontok sebagian), dipakai di pinggiran biome
 */
public final class CursedSprites {

    /** tiap piksel seni diperbesar segini kali */
    static final int Z = 4;

    public final BufferedImage[] tree = new BufferedImage[3];
    public final BufferedImage[][] treeHit = new BufferedImage[3][];
    public final BufferedImage[][] treeDown = new BufferedImage[3][];
    public final BufferedImage[] obsidian = new BufferedImage[3];
    public final BufferedImage[] crystal = new BufferedImage[3];
    public final BufferedImage[] tomb = new BufferedImage[2];
    public final BufferedImage[] thorn = new BufferedImage[2];
    public final BufferedImage[] bones = new BufferedImage[2];
    public final BufferedImage glow;

    /** pohon biasa yang layu: frame diam / dipukul / tumbang (dari frame pohon biasa) */
    public final BufferedImage[] witherIdle, witherHit, witherDown;

    // ------------------------------------------------------------------ palet (ARGB)
    private static final int OUT      = 0xFF120A18;
    private static final int BARK     = 0xFF33243B;
    private static final int BARK_HI  = 0xFF5A4469;
    private static final int BARK_LO  = 0xFF1D1325;
    private static final int LEAF_A   = 0xFF4A2358;
    private static final int LEAF_B   = 0xFF6B2F7F;
    private static final int LEAF_C   = 0xFF8E4AA6;
    private static final int WISP     = 0xFFE3B5FF;
    private static final int VEIN     = 0xFFA24BFF;
    private static final int VEIN_L   = 0xFFE0B3FF;

    public CursedSprites(BufferedImage[] oakIdle, BufferedImage[] oakHit, BufferedImage[] oakDown) {
        for (int i = 0; i < tree.length; i++) {
            tree[i] = paintTree(i);
            treeHit[i] = IceSprites.hitFrames(tree[i]);
            treeDown[i] = IceSprites.downFrames(tree[i]);
        }
        for (int i = 0; i < obsidian.length; i++) obsidian[i] = paintObsidian(i);
        for (int i = 0; i < crystal.length; i++) crystal[i] = paintCrystal(i);
        for (int i = 0; i < tomb.length; i++) tomb[i] = paintTomb(i);
        for (int i = 0; i < thorn.length; i++) thorn[i] = paintThorn(i);
        for (int i = 0; i < bones.length; i++) bones[i] = paintBones(i);
        glow = paintGlow();
        witherIdle = witherAll(oakIdle);
        witherHit = witherAll(oakHit);
        witherDown = witherAll(oakDown);
    }

    // ================================================================== kanvas piksel seni

    private static final class Art {
        final BufferedImage img;
        final Graphics2D g;

        Art(int w, int h) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        void poly(int argb, int... xy) {
            Polygon p = new Polygon();
            for (int i = 0; i + 1 < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
            g.setColor(new Color(argb, true));
            g.fillPolygon(p);
        }

        /** poligon yang dibesarkan s kali dari titik pusatnya (dipakai buat garis tepi) */
        void polyOutline(int outArgb, int fillArgb, float s, int... xy) {
            float cx = 0, cy = 0;
            int n = xy.length / 2;
            for (int i = 0; i < n; i++) { cx += xy[2 * i]; cy += xy[2 * i + 1]; }
            cx /= n; cy /= n;
            int[] big = new int[xy.length];
            for (int i = 0; i < n; i++) {
                big[2 * i] = Math.round(cx + (xy[2 * i] - cx) * s);
                big[2 * i + 1] = Math.round(cy + (xy[2 * i + 1] - cy) * s);
            }
            poly(outArgb, big);
            poly(fillArgb, xy);
        }

        void line(int argb, float w, float x0, float y0, float x1, float y1) {
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(argb, true));
            g.draw(new Line2D.Float(x0, y0, x1, y1));
        }

        void curve(int argb, float w, float x0, float y0, float cx, float cy, float x1, float y1) {
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(argb, true));
            g.draw(new QuadCurve2D.Float(x0, y0, cx, cy, x1, y1));
        }

        void oval(int argb, float x, float y, float w, float h) {
            g.setColor(new Color(argb, true));
            g.fill(new Ellipse2D.Float(x, y, w, h));
        }

        void rect(int argb, int x, int y, int w, int h) {
            g.setColor(new Color(argb, true));
            g.fillRect(x, y, w, h);
        }

        void dot(int argb, int x, int y) {
            rect(argb, x, y, 1, 1);
        }

        BufferedImage done() {
            g.dispose();
            int w = img.getWidth() * Z, h = img.getHeight() * Z;
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D og = out.createGraphics();
            og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            og.drawImage(img, 0, 0, w, h, null);
            og.dispose();
            return out;
        }
    }

    // ================================================================== pohon kutukan

    /** satu ruas dahan: {x0, y0, x1, y1, tebal} */
    private static void grow(List<float[]> segs, List<float[]> tips, Random rnd,
                             float x, float y, float angleDeg, int steps, float thick, int depth) {
        for (int i = 0; i < steps; i++) {
            angleDeg += (rnd.nextFloat() - 0.5f) * 34f;
            float len = 4.2f + rnd.nextFloat() * 2.4f;
            float nx = x + (float) Math.cos(Math.toRadians(angleDeg)) * len;
            float ny = y + (float) Math.sin(Math.toRadians(angleDeg)) * len;
            segs.add(new float[] {x, y, nx, ny, thick});
            if (depth < 2 && i >= 1 && i < steps - 1 && rnd.nextFloat() < 0.78f) {
                float side = rnd.nextBoolean() ? 1f : -1f;
                grow(segs, tips, rnd, nx, ny, angleDeg + side * (38f + rnd.nextFloat() * 26f),
                        3 + rnd.nextInt(3), Math.max(1.4f, thick * 0.68f), depth + 1);
            }
            x = nx; y = ny;
            thick = Math.max(1.3f, thick * 0.84f);
        }
        tips.add(new float[] {x, y});
    }

    private static BufferedImage paintTree(int variant) {
        final int W = 48, H = 58;
        Art a = new Art(W, H);
        Random rnd = new Random(0xC0FFEEL + variant * 7919L);
        float bx = W / 2f, by = 53f;

        a.oval(0x55000000, 7, 49, 34, 8);                           // bayangan di tanah

        List<float[]> segs = new ArrayList<>();
        List<float[]> tips = new ArrayList<>();
        grow(segs, tips, rnd, bx, by, -90f + (rnd.nextFloat() - 0.5f) * 14f, 6, 7.4f, 0);

        // akar yang mencengkeram tanah
        for (int i = 0; i < 4; i++) {
            float side = (i % 2 == 0) ? -1f : 1f;
            float len = 6f + rnd.nextFloat() * 5f;
            segs.add(new float[] {bx + side * 1.5f, by - 1f, bx + side * len, by + 1.5f + rnd.nextFloat() * 2f, 3.2f});
        }

        // garis tepi dulu, lalu kulit kayu, lalu sorot terang di kiri dan bayangan di kanan
        for (float[] s : segs) a.line(OUT, s[4] + 2.2f, s[0], s[1], s[2], s[3]);
        for (float[] s : segs) a.line(BARK, s[4], s[0], s[1], s[2], s[3]);
        for (float[] s : segs) if (s[4] > 2.6f) {
            a.line(BARK_HI, Math.max(1f, s[4] * 0.30f), s[0] - s[4] * 0.24f, s[1], s[2] - s[4] * 0.24f, s[3]);
            a.line(BARK_LO, Math.max(1f, s[4] * 0.28f), s[0] + s[4] * 0.26f, s[1], s[2] + s[4] * 0.26f, s[3]);
        }

        // sedikit daun kusam di ujung dahan, dengan serpihan cahaya
        int[] leafCols = {LEAF_A, LEAF_B, LEAF_C};
        for (float[] t : tips) {
            int n = 2 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                float ox = (rnd.nextFloat() - 0.5f) * 8f, oy = (rnd.nextFloat() - 0.5f) * 6f - 1f;
                float w = 4f + rnd.nextFloat() * 4f, h = 3f + rnd.nextFloat() * 3f;
                a.oval(OUT, t[0] + ox - w / 2 - 0.8f, t[1] + oy - h / 2 - 0.8f, w + 1.6f, h + 1.6f);
            }
        }
        for (float[] t : tips) {
            int n = 2 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) {
                float ox = (rnd.nextFloat() - 0.5f) * 8f, oy = (rnd.nextFloat() - 0.5f) * 6f - 1f;
                float w = 4f + rnd.nextFloat() * 4f, h = 3f + rnd.nextFloat() * 3f;
                a.oval(leafCols[rnd.nextInt(3)], t[0] + ox - w / 2, t[1] + oy - h / 2, w, h);
            }
            if (rnd.nextFloat() < 0.7f) a.dot(WISP, Math.round(t[0] + (rnd.nextFloat() - 0.5f) * 6f), Math.round(t[1] + 2f + rnd.nextFloat() * 4f));
        }

        // lubang di batang dengan dua mata ungu yang menyala
        float[] mid = segs.get(Math.min(2, segs.size() - 1));
        int ex = Math.round((mid[0] + mid[2]) / 2f), ey = Math.round((mid[1] + mid[3]) / 2f);
        a.oval(0xFF08040B, ex - 2.5f, ey - 3f, 5f, 6f);
        a.dot(WISP, ex - 1, ey - 1);
        a.dot(WISP, ex + 1, ey - 1);
        a.dot(VEIN, ex - 1, ey);
        a.dot(VEIN, ex + 1, ey);
        return a.done();
    }

    // ================================================================== obsidian

    private static BufferedImage paintObsidian(int v) {
        Art a = new Art(32, 28);
        Random rnd = new Random(500L + v * 31L);
        a.oval(0x55000000, 1, 22, 30, 6);
        int[] p = {4, 24, 2, 16, 6, 9, 12, 3, 19, 4, 25, 9, 29, 17, 27, 24, 16, 26};
        for (int i = 0; i < p.length; i++) p[i] += rnd.nextInt(3) - 1;
        a.polyOutline(0xFF0C070F, 0xFF241A31, 1.10f, p);
        // sisi terang di kiri, sisi gelap di kanan
        a.poly(0xFF3D2E52, p[4 - 2], p[5 - 2], p[6], p[7], p[6] + 1, 13, p[2] + 2, 17);
        a.poly(0xFF181021, p[8], p[9], p[10], p[11], p[12], p[13], p[12] - 7, 17);
        a.line(0xFF77579E, 1f, p[6], p[7], p[6] + 1, 13);
        a.line(0xFF5B4380, 1f, p[2] + 1, p[3] + 1, p[4], p[5]);
        a.line(0xFF77579E, 1f, p[4], p[5], p[6], p[7]);
        // retakan yang bersinar
        if (v != 1) {
            float x = 14 + rnd.nextInt(3), y = 25;
            for (int i = 0; i < 4; i++) {
                float nx = x + (rnd.nextFloat() - 0.4f) * 3f, ny = y - 4f - rnd.nextFloat() * 2f;
                a.line(VEIN, 1f, x, y, nx, ny);
                a.dot(VEIN_L, Math.round(nx), Math.round(ny));
                x = nx; y = ny;
            }
        }
        return a.done();
    }

    // ================================================================== kristal kutukan

    private static BufferedImage paintCrystal(int v) {
        Art a = new Art(28, 36);
        int[][] pal = {
            {0xFF8A4DF0, 0xFF5B2BB0, 0xFFD2B0FF},
            {0xFFC45BE0, 0xFF8A2BA8, 0xFFF0B8FF},
            {0xFF6C7CF2, 0xFF3F45B4, 0xFFC2C8FF},
        };
        int[] c = pal[v % 3];
        a.oval(0x55000000, 1, 30, 26, 6);
        a.poly(0xFF1B1224, 2, 34, 4, 30, 12, 29, 20, 29, 25, 31, 26, 34, 14, 35);
        int h1 = 1 + (v == 1 ? 3 : 0);
        // pecahan kiri dan kanan (lebih kecil), lalu pecahan utama di depan
        int[][] left = {{4, 31}, {4, 19}, {7, 12}, {10, 19}, {10, 31}};
        int[][] right = {{19, 31}, {19, 22}, {22, 16}, {25, 22}, {25, 31}};
        shard(a, left, c);
        shard(a, right, c);
        int[][] mainS = {{9, 31}, {9, 12}, {14, h1}, {19, 12}, {19, 31}};
        shard(a, mainS, c);
        a.line(c[2], 1f, 10, 29, 10, 12);
        a.line(c[2], 1f, 10, 12, 14, h1 + 1);
        a.dot(0xFFFFFFFF, 12, 9);
        return a.done();
    }

    private static void shard(Art a, int[][] pts, int[] c) {
        int n = pts.length;
        int[] flat = new int[n * 2];
        for (int i = 0; i < n; i++) { flat[2 * i] = pts[i][0]; flat[2 * i + 1] = pts[i][1]; }
        int topIdx = 0;
        for (int i = 0; i < n; i++) if (pts[i][1] < pts[topIdx][1]) topIdx = i;
        a.polyOutline(0xFF0E0716, c[1], 1.12f, flat);
        int cx = pts[topIdx][0];
        // sisi kiri lebih terang
        a.poly(c[0], flat[0], flat[1], pts[1][0], pts[1][1], pts[topIdx][0], pts[topIdx][1], cx, pts[0][1]);
    }

    // ================================================================== nisan

    private static BufferedImage paintTomb(int v) {
        Art a = new Art(24, 30);
        a.oval(0x66000000, 1, 25, 22, 5);
        a.oval(0xFF2A2030, 1, 23, 22, 6);                                  // gundukan tanah
        a.rect(0xFF1A1620, 2, 10, 20, 16);
        a.oval(0xFF1A1620, 2, 2, 20, 16);
        a.rect(0xFF6B6679, 4, 10, 16, 15);
        a.oval(0xFF6B6679, 4, 4, 16, 12);
        a.rect(0xFF8A859C, 4, 10, 3, 15);                                  // sisi terang
        a.rect(0xFF4A4558, 17, 10, 3, 15);                                 // sisi gelap
        a.rect(0xFF8A859C, 6, 5, 3, 2);
        if (v == 0) {                                                       // salib
            a.rect(0xFF2C2735, 11, 8, 2, 11);
            a.rect(0xFF2C2735, 8, 11, 8, 2);
        } else {                                                            // rune ungu
            a.g.setStroke(new BasicStroke(1f));
            a.g.setColor(new Color(VEIN, true));
            a.g.draw(new Ellipse2D.Float(8, 10, 8, 8));
            a.dot(VEIN_L, 12, 14);
            a.dot(VEIN_L, 12, 10);
        }
        a.line(0xFF2C2735, 1f, 7, 26, 9, 21);                              // retakan di kaki nisan
        a.line(0xFF2C2735, 1f, 9, 21, 8, 17);
        a.line(0xFF2C2735, 1f, 9, 21, 12, 22);
        return a.done();
    }

    // ================================================================== semak duri

    private static BufferedImage paintThorn(int v) {
        Art a = new Art(26, 24);
        Random rnd = new Random(900L + v * 17L);
        a.oval(0x55000000, 3, 19, 20, 5);
        int n = 11;
        float[][] sp = new float[n][6];
        for (int i = 0; i < n; i++) {
            float k = i - (n - 1) / 2f;
            float ang = (float) Math.toRadians(-90 + k * 14f + (rnd.nextFloat() - 0.5f) * 8f);
            float len = 11f - Math.abs(k) * 0.9f + rnd.nextFloat() * 4f;
            float x0 = 13 + k * 1.3f, y0 = 22f;
            float x1 = x0 + (float) Math.cos(ang) * len, y1 = y0 + (float) Math.sin(ang) * len;
            float bend = (rnd.nextFloat() - 0.5f) * 8f;
            float cx = (x0 + x1) / 2 + (float) -Math.sin(ang) * bend, cy = (y0 + y1) / 2 + (float) Math.cos(ang) * bend;
            sp[i] = new float[] {x0, y0, cx, cy, x1, y1};
        }
        for (float[] s : sp) a.curve(OUT, 3.4f, s[0], s[1], s[2], s[3], s[4], s[5]);
        for (float[] s : sp) a.curve(0xFF1A1224, 2.2f, s[0], s[1], s[2], s[3], s[4], s[5]);
        for (float[] s : sp) {
            a.curve(0xFF44335A, 1f, s[0] - 0.6f, s[1], s[2] - 0.6f, s[3], s[4], s[5]);
            a.dot(0xFFD03A62, Math.round(s[4]), Math.round(s[5]));
        }
        return a.done();
    }

    // ================================================================== tulang

    private static BufferedImage paintBones(int v) {
        Art a = new Art(20, 10);
        final int BONE = 0xFFD9D2C0, BONE_D = 0xFF9A9384, BONE_OUT = 0xFF2A2430;
        if (v == 0) {                                                       // tengkorak dan satu tulang
            a.oval(BONE_OUT, 1, 1, 9, 8);
            a.oval(BONE, 2, 1, 7, 6);
            a.rect(BONE, 3, 6, 5, 2);
            a.dot(0xFF1A1420, 3, 3); a.dot(0xFF1A1420, 6, 3);
            a.dot(BONE_D, 4, 7); a.dot(BONE_D, 6, 7);
            a.line(BONE_OUT, 3.2f, 11, 8, 18, 4);
            a.line(BONE, 2f, 11, 8, 18, 4);
            a.dot(BONE, 11, 7); a.dot(BONE, 18, 3);
        } else {                                                            // tulang bersilang
            a.line(BONE_OUT, 3.2f, 3, 2, 16, 8);
            a.line(BONE_OUT, 3.2f, 3, 8, 16, 2);
            a.line(BONE, 2f, 3, 2, 16, 8);
            a.line(BONE_D, 1f, 3, 8, 16, 2);
            a.line(BONE, 2f, 3, 8, 16, 2);
            a.dot(BONE, 2, 1); a.dot(BONE, 17, 9); a.dot(BONE, 2, 9); a.dot(BONE, 17, 1);
        }
        return a.done();
    }

    // ================================================================== cahaya ungu di bawah kristal

    private static BufferedImage paintGlow() {
        int w = 160, h = 110;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setPaint(new RadialGradientPaint(w / 2f, w / 2f, w / 2f, new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(170, 90, 255, 150), new Color(120, 50, 200, 60), new Color(90, 30, 160, 0)}));
        g.scale(1.0, h / (double) w);
        g.fillRect(0, 0, w, w);
        g.dispose();
        return im;
    }

    // ================================================================== pohon biasa yang layu

    private static BufferedImage[] witherAll(BufferedImage[] in) {
        BufferedImage[] out = new BufferedImage[in.length];
        IntStream.range(0, in.length).parallel().forEach(i -> out[i] = in[i] == null ? null : wither(in[i]));
        return out;
    }

    /**
     * Versi layu dari gambar pohon biasa: daun jadi ungu keabu-abuan, sebagian kotak daun (12 px) rontok, batang
     * sedikit lebih gelap dan keunguan. Pola rontoknya ditentukan oleh posisi kotak, jadi semua frame animasi konsisten.
     */
    public static BufferedImage wither(BufferedImage src) {
        final int B = 12;
        int w = src.getWidth(), h = src.getHeight();
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        int[] res = px.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = px[y * w + x];
                int a = p >>> 24;
                if (a == 0) continue;
                int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
                if (g > r + 6 && g >= b) {                                           // daun
                    if ((WorldGen.h2(x / B, y / B, 141) >>> 6) % 100 < 26) { res[y * w + x] = 0; continue; }   // rontok
                    float lum = (r * 0.3f + g * 0.55f + b * 0.15f) / 255f;
                    float t = Math.max(0f, Math.min(1f, (lum - 0.12f) / 0.5f));
                    res[y * w + x] = a << 24 | mix(0x2B1D36, 0x7B5E8C, t);
                } else {                                                             // batang dan lainnya
                    res[y * w + x] = a << 24 | ((int) (r * 0.78f) + 6) << 16 | ((int) (g * 0.70f)) << 8 | ((int) (b * 0.86f) + 10);
                }
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, res, 0, w);
        return out;
    }

    private static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }
}
