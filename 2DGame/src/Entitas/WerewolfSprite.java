package Entitas;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * Sprite manusia serigala 4 arah (kanan, kiri, atas, bawah), dimuat dari SATU sprite sheet /Aset/werewolf_sheet.png
 * (dibuat lewat tools/generate_werewolf.py):
 *
 *     15 kolom x 4 baris, tiap sel 72 x 80 px, telapak kaki nempel di baris ke-74
 *     baris  : right, left, up, down
 *     kolom  : idle (2) | walk (6) | attack (6, frame ke-3 & ke-4 = cakaran) | dead (1)
 *
 * Mau pakai art sendiri? Timpa werewolf_sheet.png dengan grid yang sama, kode ini gak perlu diubah.
 * Gambar diperbesar pakai nearest-neighbor (tajam) sebesar `scale`; 1.5 = skala yang sama dengan sprite player.
 */
public final class WerewolfSprite {

    /** Ukuran satu sel di sheet (sebelum diperbesar). */
    public static final int CELL_W = 72, CELL_H = 80;
    /** Baris telapak kaki di dalam sel (HARUS sama dengan GROUND di tools/generate_werewolf.py). */
    private static final int GROUND_ROW = 74;

    // Indeks arah
    public static final int RIGHT = 0, LEFT = 1, UP = 2, DOWN = 3;
    private static final String[] DIR_NAMES = { "right", "left", "up", "down" };

    // Jumlah frame tiap animasi (harus sama dengan tools/generate_werewolf.py)
    public static final int IDLE_FRAMES = 2, WALK_FRAMES = 6, ATTACK_FRAMES = 6;
    private static final int COLS = IDLE_FRAMES + WALK_FRAMES + ATTACK_FRAMES + 1;

    public final double scale;         // boleh pecahan (mis. 1.5)
    public final int width, height;    // ukuran satu frame setelah diperbesar
    public final int footY;            // baris telapak kaki di frame yang sudah diperbesar (batas bawah kaki)
    public final int centerX;          // sumbu tengah badan di frame yang sudah diperbesar

    public final BufferedImage[][] idle   = new BufferedImage[4][IDLE_FRAMES];
    public final BufferedImage[][] walk   = new BufferedImage[4][WALK_FRAMES];
    public final BufferedImage[][] attack = new BufferedImage[4][ATTACK_FRAMES];
    public final BufferedImage[]   dead   = new BufferedImage[4];

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
        this.centerX = (int) Math.round(CELL_W / 2.0 * this.scale);

        BufferedImage sheet = loadSheet();

        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < IDLE_FRAMES;   f++) idle[d][f]   = cell(sheet, d, f);
            for (int f = 0; f < WALK_FRAMES;   f++) walk[d][f]   = cell(sheet, d, IDLE_FRAMES + f);
            for (int f = 0; f < ATTACK_FRAMES; f++) attack[d][f] = cell(sheet, d, IDLE_FRAMES + WALK_FRAMES + f);
            dead[d] = cell(sheet, d, COLS - 1);
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
            System.err.println("[WerewolfSprite] Gagal load / ukuran salah: " + path);
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
