package bahan;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Gambar-gambar biome es + alat pengolah gambar yang dipakai biome itu:
 *  - sprite cemara / pohon mati / batu es / kristal / cahaya kristal (folder Aset/ice/, dibuat oleh tools/make_ice_assets.py)
 *  - animasi kena-pukul dan tumbang untuk pohon es (dibuat dari sprite-nya, jadi tidak perlu aset animasi baru)
 *  - {@link #frost}: pohon biasa jadi "kena embun beku" (puncak daun bersalju, warna lebih dingin)
 *  - {@link #snowShore}: tile tepi pantai versi salju (bagian darat putih, bagian air tidak diubah)
 */
public final class IceSprites {

    public final BufferedImage[] pine = new BufferedImage[3];
    public final BufferedImage[] dead = new BufferedImage[2];
    public final BufferedImage[] rock = new BufferedImage[3];
    public final BufferedImage[] crystal = new BufferedImage[3];
    public BufferedImage glow;
    public final BufferedImage[] drift = new BufferedImage[2], pebble = new BufferedImage[2],
            shard = new BufferedImage[2], tuft = new BufferedImage[2];

    /** animasi pohon es (dibuat sekali per sprite, dipakai bersama semua pohon sejenis) */
    public final BufferedImage[][] pineHit = new BufferedImage[3][];
    public final BufferedImage[][] pineDown = new BufferedImage[3][];
    public final BufferedImage[][] deadHit = new BufferedImage[2][];
    public final BufferedImage[][] deadDown = new BufferedImage[2][];

    public IceSprites(Class<?> anchor) {
        for (int i = 0; i < pine.length; i++) pine[i] = load(anchor, "pine_" + i);
        for (int i = 0; i < dead.length; i++) dead[i] = load(anchor, "dead_" + i);
        for (int i = 0; i < rock.length; i++) rock[i] = load(anchor, "rock_" + i);
        for (int i = 0; i < crystal.length; i++) crystal[i] = load(anchor, "crystal_" + i);
        glow = load(anchor, "glow");
        for (int i = 0; i < 2; i++) {
            drift[i] = load(anchor, "drift_" + i);
            pebble[i] = load(anchor, "pebble_" + i);
            shard[i] = load(anchor, "shard_" + i);
            tuft[i] = load(anchor, "tuft_" + i);
        }
        for (int i = 0; i < pine.length; i++) {
            pineHit[i] = hitFrames(pine[i]);
            pineDown[i] = downFrames(pine[i]);
        }
        for (int i = 0; i < dead.length; i++) {
            deadHit[i] = hitFrames(dead[i]);
            deadDown[i] = downFrames(dead[i]);
        }
    }

    private static BufferedImage load(Class<?> anchor, String name) {
        try (InputStream in = anchor.getResourceAsStream("/Aset/ice/" + name + ".png")) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("[IceSprites] Gambar tidak ditemukan: /Aset/ice/" + name + ".png");
        // supaya game tetap jalan: kotak putih-biru polos
        BufferedImage fallback = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) fallback.setRGB(x, y, 0xFFDCEBF8);
        return fallback;
    }

    // ================================================================== animasi pohon es

    /** 4 frame goyang waktu dipukul: bergeser kiri-kanan sedikit sambil miring dari pangkalnya. */
    static BufferedImage[] hitFrames(BufferedImage full) {
        BufferedImage idle = half(full);   // animasi cuma sekejap, setengah resolusi cukup (hemat memori)
        double[] dx = {-0.030, 0.026, -0.016, 0.010};
        double[] rot = {-0.035, 0.030, -0.018, 0.010};
        BufferedImage[] out = new BufferedImage[4];
        int w = idle.getWidth(), h = idle.getHeight();
        for (int i = 0; i < out.length; i++) {
            out[i] = blank(w, h);
            Graphics2D g = out[i].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            AffineTransform t = new AffineTransform();
            t.translate(dx[i] * w, 0);
            t.rotate(rot[i], w / 2.0, h * 0.88);
            g.drawImage(idle, t, null);
            g.dispose();
        }
        return out;
    }

    /** 6 frame tumbang: miring lalu "ambruk" menumpuk ke pangkal (tinggi mengecil, melebar), bagian bawah tetap menempel tanah. */
    static BufferedImage[] downFrames(BufferedImage full) {
        BufferedImage idle = half(full);
        double[] rot = {0.05, 0.10, 0.17, 0.25, 0.32, 0.38};
        double[] sy = {0.94, 0.82, 0.66, 0.48, 0.32, 0.20};
        double[] sx = {1.00, 1.04, 1.10, 1.18, 1.26, 1.34};
        BufferedImage[] out = new BufferedImage[6];
        int w = idle.getWidth(), h = idle.getHeight();
        for (int i = 0; i < out.length; i++) {
            out[i] = blank(w, h);
            Graphics2D g = out[i].createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            AffineTransform t = new AffineTransform();
            t.translate(w / 2.0, h * 0.90);
            t.rotate(rot[i]);
            t.scale(sx[i] > 1.3 ? 1.3 : sx[i], sy[i]);
            t.translate(-w / 2.0, -h * 0.90);
            if (i >= 4) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, i == 4 ? 0.85f : 0.7f));
            g.drawImage(idle, t, null);
            g.dispose();
        }
        return out;
    }

    /** Setengah ukuran, tanpa penghalusan (sprite ini dibuat 4x dari piksel seninya, jadi tetap tajam). */
    private static BufferedImage half(BufferedImage src) {
        int w = Math.max(1, src.getWidth() / 2), h = Math.max(1, src.getHeight() / 2);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setRGB(x, y, src.getRGB(x * 2, y * 2));
        return out;
    }

    private static BufferedImage blank(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    // ================================================================== pohon biasa "kena embun beku"

    /**
     * Versi beku dari gambar pohon biasa (kotak seni 12 px pada gambar 768 px): warna daun digeser ke hijau-biru dingin,
     * puncak kanopi diberi tutup salju 1-3 kotak, dan beberapa kotak daun paling terang jadi bercak salju.
     * Pola acaknya ditentukan oleh posisi kotak, jadi semua frame animasi pohon (diam / dipukul / tumbang) konsisten.
     */
    public static BufferedImage frost(BufferedImage src) {
        final int B = 12;
        int w = src.getWidth(), h = src.getHeight();
        int gw = w / B, gh = h / B;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        int[] res = px.clone();

        // warna tiap kotak = piksel tengahnya
        int[] cell = new int[gw * gh];
        for (int by = 0; by < gh; by++) for (int bx = 0; bx < gw; bx++)
            cell[by * gw + bx] = px[(by * B + B / 2) * w + bx * B + B / 2];

        boolean[] snow = new boolean[gw * gh];
        int[] snowCol = new int[gw * gh];
        for (int bx = 0; bx < gw; bx++) {
            int by = 0;
            while (by < gh && (cell[by * gw + bx] >>> 24) < 128) by++;
            if (by >= gh) continue;
            if (!isLeaf(cell[by * gw + bx])) continue;
            int depth = 1 + (WorldGen.h2(bx, 3, 77) >>> 5) % 2 + (((WorldGen.h2(bx / 2, 5, 78) >>> 5) % 3 == 0) ? 1 : 0);
            for (int k = 0; k < depth && by + k < gh; k++) {
                int idx = (by + k) * gw + bx;
                if ((cell[idx] >>> 24) < 128) break;
                snow[idx] = true;
                snowCol[idx] = k == 0 ? 0xFFFFFFFF : (k < depth - 1 ? 0xFFEAF3FB : 0xFFD3E3F3);
            }
        }
        // bercak salju di kanopi: kotak daun yang paling terang
        for (int by = 1; by < gh; by++) {
            for (int bx = 0; bx < gw; bx++) {
                int idx = by * gw + bx;
                int c = cell[idx];
                if ((c >>> 24) < 128 || snow[idx] || !isLeaf(c)) continue;
                int lum = ((c >> 16 & 255) * 3 + (c >> 8 & 255) * 5 + (c & 255)) / 9;
                if (lum > 150 && (WorldGen.h2(bx, by, 79) >>> 6) % 100 < 46) {
                    snow[idx] = true;
                    snowCol[idx] = ((WorldGen.h2(bx, by, 80) >>> 6) % 3 == 0) ? 0xFFFFFFFF : 0xFFEAF3FB;
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = px[y * w + x];
                if ((p >>> 24) == 0) continue;
                int idx = (y / B) * gw + (x / B);
                if (idx < snow.length && snow[idx]) { res[y * w + x] = snowCol[idx]; continue; }
                if (isLeaf(p)) res[y * w + x] = cool(p);
            }
        }
        out.setRGB(0, 0, w, h, res, 0, w);
        return out;
    }

    private static boolean isLeaf(int p) {
        int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
        return g > r + 6 && g >= b;
    }

    /** geser warna daun hijau ke hijau kebiruan yang lebih pucat */
    private static int cool(int p) {
        int a = p >>> 24, r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
        float k = 0.30f;
        r = (int) (r * (1 - k) + 128 * k);
        g = (int) (g * (1 - k) + 188 * k);
        b = (int) (b * (1 - k) + 186 * k);
        return a << 24 | r << 16 | g << 8 | b;
    }

    // ================================================================== tile tepi pantai bersalju

    /**
     * Ubah tile tepi pantai (air + rumput + tanah di sisinya) jadi versi salju: rumput jadi salju putih-biru, tanah tepi
     * jadi es abu-biru. Piksel air (warna dasar 0x3B8FCA) DIBIARKAN persis sama, karena WaterAnimator mengenali
     * "bagian daratan" dari piksel yang bukan warna air itu.
     */
    public static BufferedImage snowShore(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0 || (p & 0xFFFFFF) == 0x3B8FCA) { out.setRGB(x, y, p); continue; }
                int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
                float lum = (r * 0.3f + g * 0.55f + b * 0.15f) / 255f;
                int q;
                if (g > r + 4 && g >= b) {                           // rumput -> salju
                    float t = clamp((lum - 0.20f) / 0.40f);
                    q = mix(0xD5E5F4, 0xFAFDFF, t);
                } else if (r > b + 8) {                              // tanah tepi -> es abu-biru
                    float t = clamp((lum - 0.18f) / 0.34f);
                    q = mix(0xA6BEDB, 0xC9DAEC, t);
                } else {
                    q = mix(0xC8DAEC, 0xF2F8FD, clamp(lum));
                }
                out.setRGB(x, y, a << 24 | q);
            }
        }
        return out;
    }

    private static float clamp(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    /** gambar polos satu warna (dipakai tile "salju" / "es" khusus minimap) */
    public static BufferedImage flat(int rgb) {
        BufferedImage im = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) im.setRGB(x, y, 0xFF000000 | rgb);
        return im;
    }
}
