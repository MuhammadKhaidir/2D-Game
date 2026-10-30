package Entitas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Sprite serigala 4 arah (kanan, kiri, atas, bawah), dimuat dari PNG per-frame di /Aset/ (sama kayak slime):
 *
 *     wolf_<aksi>_<arah>_<nomor>.png      aksi  = idle (2) | walk (6) | attack (5) | dead (1)
 *                                         arah  = right | left | up | down
 *                                         nomor = mulai dari 1
 *
 * PNG-nya dibuat lewat tools/generate_wolf.py (pixel-art 62x34, gak di-anti-alias). Mau pakai art sendiri?
 * Timpa PNG-nya aja dengan ukuran 62x34 dan kaki nempel di baris ke-30, kode ini gak perlu diubah.
 *
 * Semua array berbentuk [arah][frame] dengan arah = RIGHT / LEFT / UP / DOWN di bawah. Gambar diperbesar
 * pakai nearest-neighbor (tajam) sebesar `scale`.
 */
public final class WolfSprite {

    /** Ukuran kanvas gambar asli (sebelum diperbesar). */
    public static final int BASE_W = 62, BASE_H = 34;

    // Indeks arah
    public static final int RIGHT = 0, LEFT = 1, UP = 2, DOWN = 3;
    private static final String[] DIR_NAMES = { "right", "left", "up", "down" };

    // Jumlah frame tiap animasi (harus sama dengan tools/generate_wolf.py)
    public static final int IDLE_FRAMES = 2, WALK_FRAMES = 6, ATTACK_FRAMES = 5;

    public final double scale;        // boleh pecahan (mis. 1.5)
    public final int width, height;   // ukuran akhir (BASE * scale, dibulatkan)

    public final BufferedImage[][] idle   = new BufferedImage[4][IDLE_FRAMES];
    public final BufferedImage[][] walk   = new BufferedImage[4][WALK_FRAMES];
    public final BufferedImage[][] attack = new BufferedImage[4][ATTACK_FRAMES];
    public final BufferedImage[]   dead   = new BufferedImage[4];

    private static WolfSprite instance;

    /** Satu set sprite dipakai bareng semua serigala. */
    public static synchronized WolfSprite get(double scale) {
        if (instance == null || instance.scale != scale) instance = new WolfSprite(scale);
        return instance;
    }

    /** Nama arah ("right"/"left"/"up"/"down") buat Entity.direction. */
    public static String dirName(int dir) {
        return DIR_NAMES[dir];
    }

    private WolfSprite(double scale) {
        this.scale = Math.max(0.25, scale);
        this.width  = (int) Math.round(BASE_W * this.scale);
        this.height = (int) Math.round(BASE_H * this.scale);

        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < IDLE_FRAMES;   f++) idle[d][f]   = load("idle",   d, f + 1);
            for (int f = 0; f < WALK_FRAMES;   f++) walk[d][f]   = load("walk",   d, f + 1);
            for (int f = 0; f < ATTACK_FRAMES; f++) attack[d][f] = load("attack", d, f + 1);
            dead[d] = load("dead", d, 1);
        }
    }

    /** Muat satu frame lalu perbesar. Kalau file-nya gak ketemu: lapor ke log, pakai gambar kosong (game tetap jalan). */
    private BufferedImage load(String action, int dir, int n) {
        String path = "/Aset/wolf_" + action + "_" + DIR_NAMES[dir] + "_" + n + ".png";
        BufferedImage src = null;
        try (java.io.InputStream in = WolfSprite.class.getResourceAsStream(path)) {
            if (in != null) src = ImageIO.read(in);
        } catch (Exception e) {
            // jatuh ke gambar kosong di bawah
        }
        if (src == null) {
            System.err.println("[WolfSprite] Gagal load: " + path);
            src = new BufferedImage(BASE_W, BASE_H, BufferedImage.TYPE_INT_ARGB);
        }
        return scaleUp(src);
    }

    /** Perbesar dengan nearest-neighbor supaya pikselnya tetap tajam. */
    private BufferedImage scaleUp(BufferedImage src) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }
}
