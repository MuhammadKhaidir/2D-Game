package Entitas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Sprite manusia serigala dari SATU sprite sheet /Aset/werewolf_sheet.png.
 * Sheet ini dibuat dari frame PLAYER (lewat tools/werewolf_gen/player_sheet.py), jadi pose & animasinya sama persis
 * dengan player: arah beneran 4 (kanan / kiri / atas / bawah), bukan pandangan 3/4.
 *
 *     22 kolom x 4 baris, tiap sel 112 x 96 px, telapak kaki nempel di baris ke-75
 *     baris  : 0 right | 1 left | 2 up | 3 down   (sama dengan indeks arah RIGHT / LEFT / UP / DOWN di bawah)
 *     kolom  : idle (2) | walk (6) | run (3) | attack3 (4) | attack4 (4) | dead (3)
 *
 *   idle    = XxxIdle1 player (frame ke-2 = napas)
 *   walk    = XxxWalk1..6 player
 *   run     = XxxRun1..3 player, dipakai buat terkam (lunge)
 *   attack3 = serangan player ke-3 ("Front (1..4)"), attack4 = serangan player ke-4 ("Front (5..8)").
 *             Serangan player ke-1 dan ke-2 sengaja tidak dipakai.
 *   dead    = idle yang direbahkan (sempoyongan -> jatuh -> tergeletak)
 *
 * Mau pakai art sendiri? Timpa werewolf_sheet.png dengan grid yang sama, kode ini gak perlu diubah.
 * Gambar diperbesar pakai nearest-neighbor (tajam) sebesar `scale`; 1.5 = skala yang sama dengan sprite player.
 */
public final class WerewolfSprite {

    /** Ukuran satu sel di sheet (sebelum diperbesar). HARUS sama dengan CELL_W / CELL_H di tools/werewolf_gen/player_sheet.py. */
    public static final int CELL_W = 112, CELL_H = 96;
    /** Baris telapak kaki di dalam sel (sebelum diperbesar). */
    private static final int GROUND_ROW = 75;
    /** Kolom sumbu tengah badan di dalam sel (sebelum diperbesar). */
    private static final int CENTER_X = 55;
    /** Baris ujung telinga di dalam sel (buat naruh HP bar tepat di atas kepala). */
    public static final int HEAD_TOP_ROW = 10;

    // Indeks arah hadap (logika game) = indeks baris di sheet
    public static final int RIGHT = 0, LEFT = 1, UP = 2, DOWN = 3;
    private static final String[] DIR_NAMES = { "right", "left", "up", "down" };

    // Indeks baris di sheet
    public static final int ROW_RIGHT = 0, ROW_LEFT = 1, ROW_UP = 2, ROW_DOWN = 3;

    // Jumlah frame tiap animasi (harus sama dengan N_* di tools/werewolf_gen/player_sheet.py)
    public static final int IDLE_FRAMES = 2, WALK_FRAMES = 6, RUN_FRAMES = 3, ATTACK3_FRAMES = 4, ATTACK4_FRAMES = 4, DEAD_FRAMES = 3;
    private static final int COLS = IDLE_FRAMES + WALK_FRAMES + RUN_FRAMES + ATTACK3_FRAMES + ATTACK4_FRAMES + DEAD_FRAMES;

    public final double scale;         // boleh pecahan (mis. 1.5)
    public final int width, height;    // ukuran satu frame setelah diperbesar
    public final int footY;            // baris telapak kaki di frame yang sudah diperbesar (batas bawah kaki)
    public final int centerX;          // sumbu tengah badan di frame yang sudah diperbesar
    public final int headTopY;         // ujung telinga di frame yang sudah diperbesar

    // Semua array diindeks per BARIS sheet (= arah hadap).
    public final BufferedImage[][] idle    = new BufferedImage[4][IDLE_FRAMES];
    public final BufferedImage[][] walk    = new BufferedImage[4][WALK_FRAMES];
    public final BufferedImage[][] run     = new BufferedImage[4][RUN_FRAMES];
    public final BufferedImage[][] attack3 = new BufferedImage[4][ATTACK3_FRAMES];
    public final BufferedImage[][] attack4 = new BufferedImage[4][ATTACK4_FRAMES];
    public final BufferedImage[][] dead    = new BufferedImage[4][DEAD_FRAMES];

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

    private WerewolfSprite(double scale) {
        this.scale  = Math.max(0.25, scale);
        this.width  = (int) Math.round(CELL_W * this.scale);
        this.height = (int) Math.round(CELL_H * this.scale);
        this.footY   = (int) Math.round((GROUND_ROW + 1) * this.scale);
        this.centerX = (int) Math.round(CENTER_X * this.scale);
        this.headTopY = (int) Math.round(HEAD_TOP_ROW * this.scale);

        BufferedImage sheet = loadSheet();

        int c0 = 0;
        int c1 = c0 + IDLE_FRAMES;
        int c2 = c1 + WALK_FRAMES;
        int c3 = c2 + RUN_FRAMES;
        int c4 = c3 + ATTACK3_FRAMES;
        int c5 = c4 + ATTACK4_FRAMES;
        for (int r = 0; r < 4; r++) {
            for (int f = 0; f < IDLE_FRAMES;    f++) idle[r][f]    = cell(sheet, r, c0 + f);
            for (int f = 0; f < WALK_FRAMES;    f++) walk[r][f]    = cell(sheet, r, c1 + f);
            for (int f = 0; f < RUN_FRAMES;     f++) run[r][f]     = cell(sheet, r, c2 + f);
            for (int f = 0; f < ATTACK3_FRAMES; f++) attack3[r][f] = cell(sheet, r, c3 + f);
            for (int f = 0; f < ATTACK4_FRAMES; f++) attack4[r][f] = cell(sheet, r, c4 + f);
            for (int f = 0; f < DEAD_FRAMES;    f++) dead[r][f]    = cell(sheet, r, c5 + f);
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
