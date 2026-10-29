package Entitas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Sprite serigala yang DIGAMBAR LEWAT KODE (gak butuh file PNG), gaya pixel-art kecil + outline otomatis.
 *
 * Semua frame digambar menghadap KANAN, lalu dicerminkan buat versi KIRI.
 * Animasi:  idle 2 frame, walk 6 frame, attack 5 frame (frame ke-3 = gigitan), dead 1 frame.
 *
 * Kalau nanti mau ganti pakai PNG sendiri, cukup isi array di bawah ini dari file, Wolf.java gak perlu diubah.
 * Array-nya [arah][frame], arah 0 = kanan, 1 = kiri.
 */
public final class WolfSprite {

    /** Ukuran kanvas gambar asli (sebelum diperbesar). */
    public static final int BASE_W = 62, BASE_H = 34;

    public final int scale;
    public final int width, height;   // ukuran akhir (BASE * scale)

    public final BufferedImage[][] idle   = new BufferedImage[2][2];
    public final BufferedImage[][] walk   = new BufferedImage[2][6];
    public final BufferedImage[][] attack = new BufferedImage[2][5];
    public final BufferedImage[]   dead   = new BufferedImage[2];

    private static WolfSprite instance;

    /** Satu set sprite dipakai bareng semua serigala. */
    public static synchronized WolfSprite get(int scale) {
        if (instance == null || instance.scale != scale) instance = new WolfSprite(scale);
        return instance;
    }

    // ── Warna ──
    private static final Color OUTLINE   = new Color(0x1b1d22);
    private static final Color FUR_FAR   = new Color(0x3a3e46);   // kaki sisi jauh
    private static final Color FUR_DARK  = new Color(0x4d525b);   // punggung
    private static final Color FUR       = new Color(0x707782);   // badan
    private static final Color FUR_LIGHT = new Color(0xa6adb8);   // perut, dada, moncong
    private static final Color EYE       = new Color(0xffd23c);
    private static final Color NOSE      = new Color(0x111215);
    private static final Color MOUTH     = new Color(0x6e1620);
    private static final Color TEETH     = new Color(0xf2f2ee);
    private static final Color TONGUE    = new Color(0xd9607a);

    // ── Pose: parameter yang beda-beda tiap frame ──
    private static final class Pose {
        double bob;                 // badan naik(-)/turun(+)
        double bodyDx;              // seluruh badan maju(+)/mundur(-)
        double headDx, headDy;      // kepala relatif ke badan
        double jaw;                 // 0 = tutup, 1 = mangap lebar
        double tail;                // -1 ekor turun .. +1 ekor naik
        double phase = Double.NaN;  // fase jalan (radian); NaN = berdiri diam
        double stride = 5;          // ayunan kaki pas jalan
        double frontReach, hindReach; // geser kaki statis (dipakai pose serang)
    }

    private WolfSprite(int scale) {
        this.scale = Math.max(1, scale);
        this.width  = BASE_W * this.scale;
        this.height = BASE_H * this.scale;
        build();
    }

    private void build() {
        // IDLE: napas pelan + ekor goyang
        Pose i0 = new Pose(); i0.tail = 0.2;
        Pose i1 = new Pose(); i1.bob = 1; i1.headDy = 1; i1.tail = -0.3;
        store(idle, 0, render(i0)); store(idle, 1, render(i1));

        // WALK: 6 frame, kaki diagonal gantian
        for (int f = 0; f < 6; f++) {
            Pose p = new Pose();
            p.phase = f / 6.0 * Math.PI * 2;
            p.bob = Math.abs(Math.sin(p.phase)) > 0.7 ? -1 : 0;
            p.headDy = (f % 3 == 0) ? 1 : 0;
            p.tail = Math.sin(p.phase * 1.0) * 0.5;
            store(walk, f, render(p));
        }

        // ATTACK: jongkok -> mundur -> LUNCUR + gigit (frame 2) -> tutup -> pulih
        Pose a0 = new Pose(); a0.bob = 2; a0.headDx = -1; a0.headDy = 2; a0.tail = 0.8; a0.hindReach = -1;
        Pose a1 = new Pose(); a1.bob = 3; a1.bodyDx = -2; a1.headDx = -3; a1.headDy = 3; a1.jaw = 0.25; a1.tail = 1; a1.hindReach = -2; a1.frontReach = -1;
        Pose a2 = new Pose(); a2.bob = -1; a2.bodyDx = 3; a2.headDx = 3; a2.headDy = 2; a2.jaw = 1; a2.tail = 1; a2.hindReach = -5; a2.frontReach = 4;
        Pose a3 = new Pose(); a3.bodyDx = 2; a3.headDx = 3; a3.headDy = 4; a3.jaw = 0.35; a3.tail = 0.5; a3.hindReach = -3; a3.frontReach = 3;
        Pose a4 = new Pose(); a4.bob = 1; a4.bodyDx = 0; a4.headDx = 0; a4.jaw = 0; a4.tail = 0.2;
        store(attack, 0, render(a0)); store(attack, 1, render(a1)); store(attack, 2, render(a2));
        store(attack, 3, render(a3)); store(attack, 4, render(a4));

        // DEAD
        BufferedImage d = finish(renderDead());
        dead[0] = scaleUp(d, false);
        dead[1] = scaleUp(d, true);
    }

    private void store(BufferedImage[][] arr, int frame, BufferedImage base) {
        BufferedImage done = finish(base);
        arr[0][frame] = scaleUp(done, false);
        arr[1][frame] = scaleUp(done, true);
    }

    // ───────────────────────── Menggambar satu frame (hadap kanan) ─────────────────────────

    private static BufferedImage render(Pose p) {
        BufferedImage img = new BufferedImage(BASE_W, BASE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        final double ground = 30;
        double bx = 21 + p.bodyDx;          // pusat badan
        double by = 17 + p.bob;
        boolean walking = !Double.isNaN(p.phase);

        // posisi pangkal kaki
        double hipX = bx - 8, shX = bx + 8, legTop = by + 3;

        // ---- kaki sisi jauh (gelap) ----
        legs(g, hipX + 2, legTop, ground, p, walking, Math.PI, p.hindReach, true, FUR_FAR);
        legs(g, shX - 2, legTop, ground, p, walking, 0, p.frontReach, false, FUR_FAR);

        // ---- ekor ----
        double tipY = by - 3 - p.tail * 6;
        poly(g, FUR_DARK, bx - 11, by - 3, bx - 12, by + 1, bx - 19, tipY + 1, bx - 20, tipY - 2, bx - 16, tipY - 2);
        poly(g, FUR, bx - 19, tipY + 1, bx - 20, tipY - 2, bx - 17, tipY - 1);   // ujung ekor lebih terang

        // ---- badan (3 lapis supaya ada bayangan) ----
        g.setColor(FUR_DARK); g.fillOval(r(bx - 13), r(by - 7), 27, 13);
        g.setColor(FUR);      g.fillOval(r(bx - 13), r(by - 5), 27, 11);
        g.setColor(FUR_LIGHT);g.fillOval(r(bx - 10), r(by + 1), 20, 5);

        // ---- kaki sisi dekat ----
        legs(g, hipX, legTop, ground, p, walking, 0, p.hindReach, true, FUR);
        legs(g, shX, legTop, ground, p, walking, Math.PI, p.frontReach, false, FUR);

        // ---- leher + dada ----
        double hx = bx + 17 + p.headDx;     // pusat kepala
        double hy = by - 5 + p.headDy;
        poly(g, FUR, bx + 6, by - 7, hx - 3, hy - 4, hx + 2, hy + 5, bx + 11, by + 4);
        poly(g, FUR_LIGHT, bx + 10, by - 1, hx - 1, hy + 3, hx + 1, hy + 6, bx + 12, by + 5);

        // ---- telinga (belakang kepala) ----
        poly(g, FUR_FAR, hx - 6, hy - 3, hx - 4, hy - 10, hx - 1, hy - 4);
        poly(g, FUR_DARK, hx - 2, hy - 4, hx + 1, hy - 11, hx + 4, hy - 4);

        // ---- kepala ----
        g.setColor(FUR);      g.fillOval(r(hx - 6), r(hy - 5), 12, 10);
        g.setColor(FUR_LIGHT);poly(g, FUR_LIGHT, hx + 2, hy - 1, hx + 13, hy + 1, hx + 13, hy + 3, hx + 3, hy + 5);   // moncong
        g.setColor(FUR);      g.fillRect(r(hx + 1), r(hy - 2), 5, 2);                                                // jembatan hidung

        // ---- rahang bawah / mulut ----
        if (p.jaw > 0.2) {
            double open = p.jaw * 5;
            poly(g, MOUTH, hx + 3, hy + 3, hx + 13, hy + 3, hx + 12, hy + 3 + open, hx + 3, hy + 5 + open * 0.6);
            poly(g, FUR_LIGHT, hx + 3, hy + 5 + open * 0.6, hx + 12, hy + 3 + open, hx + 12, hy + 5 + open, hx + 4, hy + 7 + open * 0.6);
            if (p.jaw > 0.6) {
                g.setColor(TONGUE); g.fillRect(r(hx + 6), r(hy + 4), 4, 1);
            }
            g.setColor(TEETH);                                  // taring atas & bawah
            g.fillRect(r(hx + 10), r(hy + 3), 1, 2);
            g.fillRect(r(hx + 7),  r(hy + 3), 1, 1);
            g.fillRect(r(hx + 11), r(hy + 3 + open) - 1, 1, 2);
        }

        // ---- mata & hidung ----
        g.setColor(EYE);  g.fillRect(r(hx + 2), r(hy - 3), 3, 2);
        g.setColor(NOSE); g.fillRect(r(hx + 2) + 1, r(hy - 3), 1, 1);   // pupil
        g.fillRect(r(hx + 12), r(hy), 2, 2);                              // hidung

        g.dispose();
        return img;
    }

    /** Sepasang kaki (belakang atau depan) dengan 2 ruas. */
    private static void legs(Graphics2D g, double hipX, double hipY, double ground, Pose p,
                             boolean walking, double phaseOffset, double reach, boolean hind, Color c) {
        double pawX = hipX + reach, pawY = ground;
        if (walking) {
            double ph = p.phase + phaseOffset + (hind ? 0 : Math.PI);
            pawX = hipX + Math.sin(ph) * p.stride;
            pawY = ground - Math.max(0, Math.cos(ph)) * 3;
        }
        double kneeX = (hipX + pawX) / 2 + (hind ? 2 : -1.5);
        double kneeY = (hipY + pawY) / 2;
        thick(g, c, hipX, hipY, kneeX, kneeY, 4);
        thick(g, c, kneeX, kneeY, pawX, pawY - 1, 3);
        if (c == FUR) g.setColor(FUR_LIGHT);                                        // telapak sisi dekat lebih terang
        else          g.setColor(c);
        g.fillRect(r(pawX) - 1, r(pawY) - 1, 4, 2);
    }

    private static BufferedImage renderDead() {
        BufferedImage img = new BufferedImage(BASE_W, BASE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // ekor
        poly(g, FUR_DARK, 11, 25, 12, 28, 4, 29, 3, 27, 6, 25);
        // kaki kaku mencuat miring keluar (belakang miring ke kiri, depan miring ke kanan)
        thick(g, FUR_FAR, 15, 24, 9, 20, 3);   thick(g, FUR_FAR, 30, 23, 35, 18, 3);
        thick(g, FUR,     19, 25, 14, 22, 3);  thick(g, FUR,     27, 25, 32, 21, 3);
        g.setColor(FUR_LIGHT);
        g.fillRect(7, 19, 3, 2); g.fillRect(34, 17, 3, 2); g.fillRect(12, 21, 3, 2); g.fillRect(31, 20, 3, 2);
        // badan rebah
        g.setColor(FUR_DARK);  g.fillOval(10, 20, 28, 10);
        g.setColor(FUR);       g.fillOval(10, 22, 28, 8);
        g.setColor(FUR_LIGHT); g.fillOval(13, 26, 22, 4);
        // kepala di tanah
        poly(g, FUR_DARK, 34, 22, 36, 17, 39, 22);                 // telinga
        g.setColor(FUR);       g.fillOval(33, 21, 12, 9);
        poly(g, FUR_LIGHT, 41, 25, 51, 27, 51, 29, 41, 30);       // moncong
        g.setColor(TONGUE);    g.fillRect(46, 29, 4, 2);          // lidah keluar
        g.setColor(NOSE);      g.fillRect(50, 26, 2, 2);          // hidung
        // mata X
        g.setColor(NOSE);
        g.fillRect(38, 24, 1, 1); g.fillRect(40, 24, 1, 1); g.fillRect(39, 25, 1, 1); g.fillRect(38, 26, 1, 1); g.fillRect(40, 26, 1, 1);

        g.dispose();
        return img;
    }

    // ───────────────────────── Helper gambar ─────────────────────────

    private static int r(double v) { return (int) Math.round(v); }

    private static void poly(Graphics2D g, Color c, double... pts) {
        int n = pts.length / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = r(pts[i * 2]); ys[i] = r(pts[i * 2 + 1]); }
        g.setColor(c);
        g.fillPolygon(xs, ys, n);
    }

    private static void thick(Graphics2D g, Color c, double x1, double y1, double x2, double y2, double w) {
        double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy);
        if (len < 0.01) return;
        double nx = -dy / len * w / 2, ny = dx / len * w / 2;
        poly(g, c, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny);
    }

    /** Tambah outline gelap 1 piksel di sekeliling siluet, biar kelihatan kayak pixel-art. */
    private static BufferedImage finish(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                if ((argb >>> 24) != 0) { out.setRGB(x, y, argb | 0xFF000000); continue; }
                boolean edge = (x > 0     && (src.getRGB(x - 1, y) >>> 24) != 0)
                            || (x < w - 1 && (src.getRGB(x + 1, y) >>> 24) != 0)
                            || (y > 0     && (src.getRGB(x, y - 1) >>> 24) != 0)
                            || (y < h - 1 && (src.getRGB(x, y + 1) >>> 24) != 0);
                if (edge) out.setRGB(x, y, OUTLINE.getRGB());
            }
        }
        return out;
    }

    /** Perbesar dengan nearest-neighbor (tajam), sekalian bisa dicerminkan buat arah kiri. */
    private BufferedImage scaleUp(BufferedImage src, boolean flip) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (flip) g.drawImage(src, width, 0, -width, height, null);
        else      g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }
}
