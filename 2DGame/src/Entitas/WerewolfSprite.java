package Entitas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Sprite manusia serigala dari SATU sprite sheet /Aset/werewolf_sheet.png
 * (dibuat lewat tools/generate_werewolf.py dari gambar referensi, lihat tools/werewolf_gen/):
 *
 *     25 kolom x 4 baris, tiap sel 136 x 100 px, telapak kaki nempel di baris ke-94
 *     baris  : 0 right | 1 left | 2 back_right | 3 back_left
 *     kolom  : idle (4) | walk (8) | attack (6, frame ke-4 = cakaran) | lunge (3) | dead (4)
 *
 * Arah hadap (RIGHT/LEFT/UP/DOWN) dipetakan ke baris lewat {@link #row(int, boolean)}:
 *   RIGHT -> baris 0, LEFT -> baris 1,
 *   DOWN  -> baris 0/1 (pandangan 3/4 depan, menghadap ke sisi horizontal terakhir),
 *   UP    -> baris 2/3 (pandangan 3/4 belakang, menghadap ke sisi horizontal terakhir).
 *
 * Mau pakai art sendiri? Timpa werewolf_sheet.png dengan grid yang sama, kode ini gak perlu diubah.
 * Gambar diperbesar pakai nearest-neighbor (tajam) sebesar `scale`; 1.5 = skala yang sama dengan sprite player.
 */
public final class WerewolfSprite {

    /** Ukuran satu sel di sheet (sebelum diperbesar). HARUS sama dengan CELL_W / CELL_H di tools/werewolf_gen/anim.py. */
    public static final int CELL_W = 136, CELL_H = 100;
    /** Baris telapak kaki di dalam sel (sebelum diperbesar). */
    private static final int GROUND_ROW = 94;
    /** Baris ujung telinga di dalam sel (buat naruh HP bar tepat di atas kepala). */
    public static final int HEAD_TOP_ROW = 18;

    // Indeks arah hadap (logika game)
    public static final int RIGHT = 0, LEFT = 1, UP = 2, DOWN = 3;
    private static final String[] DIR_NAMES = { "right", "left", "up", "down" };

    // Indeks baris di sheet
    public static final int ROW_RIGHT = 0, ROW_LEFT = 1, ROW_BACK_RIGHT = 2, ROW_BACK_LEFT = 3;

    // Jumlah frame tiap animasi (harus sama dengan SPEC di tools/werewolf_gen/sheet.py)
    public static final int IDLE_FRAMES = 4, WALK_FRAMES = 8, ATTACK_FRAMES = 6, LUNGE_FRAMES = 3, DEAD_FRAMES = 4;
    private static final int COLS = IDLE_FRAMES + WALK_FRAMES + ATTACK_FRAMES + LUNGE_FRAMES + DEAD_FRAMES;

    public final double scale;         // boleh pecahan (mis. 1.5)
    public final int width, height;    // ukuran satu frame setelah diperbesar
    public final int footY;            // baris telapak kaki di frame yang sudah diperbesar (batas bawah kaki)
    public final int centerX;          // sumbu tengah badan di frame yang sudah diperbesar
    public final int headTopY;         // ujung telinga di frame yang sudah diperbesar

    // Semua array diindeks per BARIS sheet (lihat row()), bukan per arah.
    public final BufferedImage[][] idle   = new BufferedImage[4][IDLE_FRAMES];
    public final BufferedImage[][] walk   = new BufferedImage[4][WALK_FRAMES];
    public final BufferedImage[][] attack = new BufferedImage[4][ATTACK_FRAMES];
    public final BufferedImage[][] lunge  = new BufferedImage[4][LUNGE_FRAMES];
    public final BufferedImage[][] dead   = new BufferedImage[4][DEAD_FRAMES];

    private static WerewolfSprite instance;

    /** Satu set sprite dipakai bareng semua werewolf. */
    public static synchronized WerewolfSprite get(double scale) {
        if (instance == null || instance.scale != scale) instance = new WerewolfSprite(scale);
        return instance;
    }

    /** Nama arah ("right"/"left"/"up"/"down") buat Entity.direction. */
    public static String dirName(int dir) {
        return DIR_NAMES[dir];
    }

    /**
     * Baris sheet buat sebuah arah hadap. Arah vertikal (UP/DOWN) tetap menghadap ke sisi horizontal terakhir
     * (facingRight), karena art-nya pandangan 3/4.
     */
    public static int row(int dir, boolean facingRight) {
        switch (dir) {
            case RIGHT: return ROW_RIGHT;
            case LEFT:  return ROW_LEFT;
            case UP:    return facingRight ? ROW_BACK_RIGHT : ROW_BACK_LEFT;
            default:    return facingRight ? ROW_RIGHT : ROW_LEFT;     // DOWN
        }
    }

    private WerewolfSprite(double scale) {
        this.scale  = Math.max(0.25, scale);
        this.width  = (int) Math.round(CELL_W * this.scale);
        this.height = (int) Math.round(CELL_H * this.scale);
        this.footY   = (int) Math.round((GROUND_ROW + 1) * this.scale);
        this.centerX = (int) Math.round(CELL_W / 2.0 * this.scale);
        this.headTopY = (int) Math.round(HEAD_TOP_ROW * this.scale);

        BufferedImage sheet = loadSheet();

        int c0 = 0, c1 = IDLE_FRAMES, c2 = c1 + WALK_FRAMES, c3 = c2 + ATTACK_FRAMES, c4 = c3 + LUNGE_FRAMES;
        for (int r = 0; r < 4; r++) {
            for (int f = 0; f < IDLE_FRAMES;   f++) idle[r][f]   = cell(sheet, r, c0 + f);
            for (int f = 0; f < WALK_FRAMES;   f++) walk[r][f]   = cell(sheet, r, c1 + f);
            for (int f = 0; f < ATTACK_FRAMES; f++) attack[r][f] = cell(sheet, r, c2 + f);
            for (int f = 0; f < LUNGE_FRAMES;  f++) lunge[r][f]  = cell(sheet, r, c3 + f);
            for (int f = 0; f < DEAD_FRAMES;   f++) dead[r][f]   = cell(sheet, r, c4 + f);
        }
    }

    /** Muat sheet. Kalau file-nya gak ketemu: lapor ke log, pakai gambar kosong (game tetap jalan). */
    private static BufferedImage loadSheet() {
        String path = "/Aset/werewolf_sheet.png";
        BufferedImage src = null;
        try (java.io.InputStream in = WerewolfSprite.class.getResourceAsStream(path)) {
            if (in != null) src = ImageIO.read(in);
        } catch (Exception e) {
            // jatuh ke gambar kosong di bawah
        }
        if (src == null || src.getWidth() < COLS * CELL_W || src.getHeight() < 4 * CELL_H) {
            System.err.println("[WerewolfSprite] Gagal load / ukuran salah: " + path
                    + " (butuh " + (COLS * CELL_W) + "x" + (4 * CELL_H) + ")");
            src = new BufferedImage(COLS * CELL_W, 4 * CELL_H, BufferedImage.TYPE_INT_ARGB);
        }
        return src;
    }

    /** Potong satu sel dari sheet lalu perbesar. */
    private BufferedImage cell(BufferedImage sheet, int row, int col) {
        BufferedImage sub = sheet.getSubimage(col * CELL_W, row * CELL_H, CELL_W, CELL_H);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(sub, 0, 0, width, height, null);
        g.dispose();
        return out;
    }
}
