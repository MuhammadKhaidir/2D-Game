package bahan;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Ikan kuning yang sesekali meloncat keluar dari air, melengkung di udara, lalu masuk lagi.
 *
 * Animasinya satu sprite sheet per jenis loncatan (Aset/fish_jump_a.png = loncatan pendek,
 * Aset/fish_jump_b.png = loncatan jauh). Satu sheet = 36 frame (6 kolom x 6 baris), tiap frame 240x168 piksel:
 * ikan berenang di bawah permukaan -> muncrat keluar -> melengkung -> masuk lagi + percikan -> riak melebar.
 * Latarnya transparan, jadi air animasi di bawahnya tetap kelihatan.
 *
 * Tiap titik loncat punya timer sendiri (acak 5-13 detik) dan arah acak (kiri/kanan, cermin horizontal).
 * Waktu dihitung pakai jam sistem, jadi kecepatan animasi tidak tergantung FPS game.
 *
 * Titik loncat = tengah-tengah busur loncatan di garis permukaan air. Pastikan area sekitar titiknya air semua
 * (kira-kira 270px lebar, 130px ke atas, 40px ke bawah), supaya ikan tidak "meloncat" di atas daratan.
 */
public final class FishJumpManager {

    public static final int CELL_W = 240, CELL_H = 168;
    public static final int COLS = 6, FRAMES = 36;
    /** y garis permukaan air di dalam frame (piksel dari atas frame) */
    public static final int WATER_LINE = 126;
    /** lama satu frame (ms): satu loncatan = 36 x 70 ms = 2,5 detik */
    public static final int FRAME_MS = 70;
    /** x titik tengah busur di dalam frame, per jenis loncatan (a, b) */
    private static final int[] CHORD_MID = {105, 123};
    private static final String[] SHEETS = {"/Aset/fish_jump_a.png", "/Aset/fish_jump_b.png"};

    private static final int MIN_WAIT_MS = 5000, MAX_WAIT_MS = 13000;

    private static final class Spot {
        final int x, y, variant;
        final Random rnd;
        long nextAt;
        long startedAt = -1;
        boolean flip;

        Spot(int x, int y, int variant, long seed) {
            this.x = x;
            this.y = y;
            this.variant = variant;
            this.rnd = new Random(seed);
        }
    }

    private final BufferedImage[][] cells = new BufferedImage[SHEETS.length][FRAMES];
    private final List<Spot> spots = new ArrayList<>();
    private boolean ready = false;

    /**
     * @param loader   class untuk mencari resource (pakai getClass() dari pemanggil)
     * @param spotData tiap baris {x, y, jenis}: titik di dunia (piksel) dan jenis loncatan 0/1
     */
    public FishJumpManager(Class<?> loader, int[][] spotData) {
        try {
            for (int v = 0; v < SHEETS.length; v++) {
                try (InputStream in = loader.getResourceAsStream(SHEETS[v])) {
                    if (in == null) {
                        System.out.println("FISH SHEET NOT FOUND: " + SHEETS[v]);
                        return;
                    }
                    BufferedImage sheet = ImageIO.read(in);
                    for (int f = 0; f < FRAMES; f++) {
                        cells[v][f] = sheet.getSubimage((f % COLS) * CELL_W, (f / COLS) * CELL_H, CELL_W, CELL_H);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            return;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < spotData.length; i++) {
            int[] d = spotData[i];
            Spot s = new Spot(d[0], d[1], Math.max(0, Math.min(SHEETS.length - 1, d[2])), 7919L * (i + 1) + d[0] * 31L + d[1]);
            s.nextAt = now + 1500 + s.rnd.nextInt(8000);
            spots.add(s);
        }
        ready = true;
    }

    /** Menggambar semua ikan yang sedang loncat dan kelihatan di layar. Panggil sekali per frame, di atas air. */
    public void draw(Graphics2D g2, int camX, int camY, int playerScreenX, int playerScreenY,
                     int minX, int minY, int maxX, int maxY) {
        if (!ready) return;
        long now = System.currentTimeMillis();
        for (Spot s : spots) {
            if (s.startedAt < 0) {
                if (now < s.nextAt) continue;
                s.startedAt = now;
                s.flip = s.rnd.nextBoolean();
            }
            int f = (int) ((now - s.startedAt) / FRAME_MS);
            if (f >= FRAMES) {
                s.startedAt = -1;
                s.nextAt = now + MIN_WAIT_MS + s.rnd.nextInt(MAX_WAIT_MS - MIN_WAIT_MS);
                continue;
            }
            int chord = CHORD_MID[s.variant];
            int worldX = s.flip ? s.x - (CELL_W - chord) : s.x - chord;
            int worldY = s.y - WATER_LINE;
            int sx = worldX - camX + playerScreenX;
            int sy = worldY - camY + playerScreenY;
            if (sx + CELL_W < minX || sx > maxX || sy + CELL_H < minY || sy > maxY) continue;

            BufferedImage img = cells[s.variant][f];
            if (s.flip) g2.drawImage(img, sx + CELL_W, sy, -CELL_W, CELL_H, null);   // lebar negatif = cermin horizontal
            else        g2.drawImage(img, sx, sy, CELL_W, CELL_H, null);
        }
    }
}
