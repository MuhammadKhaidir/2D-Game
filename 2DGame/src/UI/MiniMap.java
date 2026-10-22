package UI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import Main.GamePanel;
import Object.breakable;
import bahan.Tile;
import bahan.TileM;

/**
 * Minimap di pojok kanan atas (cuma tampil di dunia luar).
 *
 * - Menampilkan SELURUH peta (area yang benar-benar terisi di map01.txt), diperkecil.
 * - Isinya: tanah/air/jalan/salju, objek (pohon, batu, jembatan, semak, reruntuhan),
 *   bangunan, peti, dan posisi player. MONSTER SENGAJA TIDAK DIGAMBAR.
 * - Lapisan statis (tile + objek dunia) digambar SEKALI ke gambar cache, jadi per frame
 *   ongkosnya kecil: tinggal drawImage + titik pohon/batu (yang bisa ditebang & respawn) + marker player.
 * - Posisi player dipetakan dengan skala yang sama persis dengan peta, jadi akurat.
 */
public class MiniMap {

    // ===== UKURAN & POSISI (ubah di sini kalau mau) =====
    /** Sisi terpanjang area peta di layar (px). Sisi satunya mengikuti rasio peta asli. */
    private static final int MAP_LONG_SIDE = 150;
    /** Jarak panel dari tepi kanan & atas layar (px). */
    private static final int MARGIN = 10;
    /** Tebal bingkai (px). */
    private static final int BORDER = 3;
    /** Transparansi panel (1 = pekat). Dibuat sedikit tembus biar gak nutupin gameplay. */
    private static final float PANEL_ALPHA = 0.88f;

    // ===== WARNA =====
    private static final Color FRAME      = new Color(220, 190, 150);
    private static final Color FRAME_DARK = new Color(45, 30, 20);
    private static final Color BUILDING   = new Color(222, 150, 78);
    private static final Color BUILDING_O = new Color(70, 35, 12);
    private static final Color RUINS      = new Color(165, 165, 178);
    private static final Color BRIDGE     = new Color(150, 105, 60);
    private static final Color TREE       = new Color(20, 66, 26);
    private static final Color BUSH       = new Color(44, 112, 48);
    private static final Color ROCK       = new Color(135, 135, 145);
    private static final Color CHEST      = new Color(255, 214, 40);

    // jenis objek
    private static final int K_NONE = 0, K_BUILDING = 1, K_RUINS = 2, K_BRIDGE = 3, K_TREE = 4, K_BUSH = 5, K_ROCK = 6;

    private final GamePanel gp;

    private BufferedImage base;          // lapisan statis yang sudah jadi
    private int mapW, mapH;              // ukuran area peta di layar
    private double scaleX, scaleY;       // px layar per px dunia

    public MiniMap(GamePanel gp) {
        this.gp = gp;
    }

    // =====================================================================
    //  GAMBAR PER FRAME
    // =====================================================================

    public void draw(Graphics2D g2) {
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return; // di dalam rumah/domain koordinatnya beda dunia
        if (base == null) bake();

        int panelW = mapW + BORDER * 2;
        int panelH = mapH + BORDER * 2;
        int px = gp.screenWidth - panelW - MARGIN;
        int py = MARGIN;
        int mx = px + BORDER;
        int my = py + BORDER;

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, PANEL_ALPHA));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // bingkai
        g2.setColor(FRAME_DARK);
        g2.fillRoundRect(px - 1, py - 1, panelW + 2, panelH + 2, 10, 10);
        g2.setColor(FRAME);
        g2.fillRoundRect(px, py, panelW, panelH, 8, 8);

        // isi peta (dipotong sesuai sudut bingkai)
        g2.setClip(new RoundRectangle2D.Float(mx, my, mapW, mapH, 4, 4));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.drawImage(base, mx, my, null);

        drawBreakables(g2, mx, my);
        drawChest(g2, mx, my);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPlayer(g2, mx, my);

        // garis tipis di tepi dalam supaya peta "tenggelam" rapi di bingkai
        g2.setClip(oldClip);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRoundRect(mx, my, mapW - 1, mapH - 1, 4, 4);

        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /** Pohon & batu yang bisa ditebang/dihancurkan: yang sedang hancur tidak ditampilkan. */
    private void drawBreakables(Graphics2D g2, int mx, int my) {
        breakable[] list = gp.tileM.breakables;
        int[] tiles = gp.tileM.breakableTile;
        if (list == null || tiles == null) return;

        for (int i = 0; i < list.length; i++) {
            breakable b = list[i];
            if (b == null || b.destroyed) continue;
            g2.setColor(tiles[i] == 55 ? TREE : ROCK); // 55 = pohon, sisanya batu
            int cx = mx + (int) Math.round((b.worldX + b.width / 2.0) * scaleX);
            int cy = my + (int) Math.round((b.worldY + b.height / 2.0) * scaleY);
            g2.fillRect(cx - 1, cy - 1, 2, 2);
        }
    }

    private void drawChest(Graphics2D g2, int mx, int my) {
        if (gp.chest == null) return;
        int cx = mx + (int) Math.round((World.StorageChest.WORLD_X + gp.tileSize / 2.0) * scaleX);
        int cy = my + (int) Math.round((World.StorageChest.WORLD_Y + gp.tileSize / 2.0) * scaleY);
        g2.setColor(BUILDING_O);
        g2.fillRect(cx - 2, cy - 2, 5, 5);
        g2.setColor(CHEST);
        g2.fillRect(cx - 1, cy - 1, 3, 3);
    }

    /** Marker player: titik merah berbingkai putih + cincin berdenyut biar gampang dicari. */
    private void drawPlayer(Graphics2D g2, int mx, int my) {
        // pusat sprite player (player.x/y = pojok kiri-atas kotak 1 tile)
        double wx = gp.player.x + gp.tileSize / 2.0;
        double wy = gp.player.y + gp.tileSize / 2.0;
        float cx = mx + clamp((float) (wx * scaleX), 0, mapW - 1);
        float cy = my + clamp((float) (wy * scaleY), 0, mapH - 1);

        // cincin berdenyut
        float phase = (System.currentTimeMillis() % 1400L) / 1400f;
        float ringR = 3.5f + phase * 5f;
        g2.setColor(new Color(255, 255, 255, (int) (170 * (1f - phase))));
        g2.drawOval(Math.round(cx - ringR), Math.round(cy - ringR), Math.round(ringR * 2), Math.round(ringR * 2));

        // titik player
        g2.setColor(Color.WHITE);
        g2.fillOval(Math.round(cx - 4), Math.round(cy - 4), 8, 8);
        g2.setColor(new Color(230, 40, 40));
        g2.fillOval(Math.round(cx - 3), Math.round(cy - 3), 6, 6);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // =====================================================================
    //  LAPISAN STATIS (dibuat sekali)
    // =====================================================================

    private void bake() {
        TileM tm = gp.tileM;
        int cols = tm.loadedCols > 0 ? tm.loadedCols : gp.maxWorldCol;
        int rows = tm.loadedRows > 0 ? tm.loadedRows : gp.maxWorldRow;

        // sisi terpanjang = MAP_LONG_SIDE, rasio peta dijaga supaya jarak & posisi tidak mulur
        if (cols >= rows) {
            mapW = MAP_LONG_SIDE;
            mapH = Math.max(1, Math.round(MAP_LONG_SIDE * (float) rows / cols));
        } else {
            mapH = MAP_LONG_SIDE;
            mapW = Math.max(1, Math.round(MAP_LONG_SIDE * (float) cols / rows));
        }
        scaleX = (double) mapW / (cols * gp.tileSize);
        scaleY = (double) mapH / (rows * gp.tileSize);

        // 1) tanah: tiap pixel minimap = rata-rata warna tile yang jatuh di pixel itu
        int[] tileColor = averageTileColors(tm);
        long[] sr = new long[mapW * mapH];
        long[] sg = new long[mapW * mapH];
        long[] sb = new long[mapW * mapH];
        int[] cnt = new int[mapW * mapH];

        for (int col = 0; col < cols; col++) {
            int tx = Math.min(mapW - 1, col * mapW / cols);
            for (int row = 0; row < rows; row++) {
                int t = tm.mapTileNum[col][row];
                if (t == 55 || t == 40 || t == 41) t = 0; // sama kayak TileM.draw: objek breakable di atas rumput
                if (t < 0 || t >= tileColor.length) t = 0;
                int ty = Math.min(mapH - 1, row * mapH / rows);
                int rgb = tileColor[t];
                int i = ty * mapW + tx;
                sr[i] += (rgb >> 16) & 0xFF;
                sg[i] += (rgb >> 8) & 0xFF;
                sb[i] += rgb & 0xFF;
                cnt[i]++;
            }
        }

        base = new BufferedImage(mapW, mapH, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                int i = y * mapW + x;
                int c = Math.max(1, cnt[i]);
                int rgb = (int) (sr[i] / c) << 16 | (int) (sg[i] / c) << 8 | (int) (sb[i] / c);
                base.setRGB(x, y, rgb);
            }
        }

        // 2) objek dunia: yang kecil dulu, bangunan paling atas
        int[][] objs = tm.getWorldObjects();
        Graphics2D bg = base.createGraphics();
        if (objs != null) {
            for (int[] o : objs) {
                int kind = kindOf(o[4]);
                if (kind == K_NONE || kind == K_BUILDING) continue;
                paintObject(bg, o, kind);
            }
            for (int[] o : objs) {
                if (kindOf(o[4]) == K_BUILDING) paintObject(bg, o, K_BUILDING);
            }
        }
        bg.dispose();
    }

    /** Warna rata-rata tiap index tile (piksel transparan dicampur dengan warna rumput). */
    private int[] averageTileColors(TileM tm) {
        int n = tm.tile.length;
        int[] avg = new int[n];
        float[] coverage = new float[n];
        final int fallback = 0x3C5A32;

        for (int i = 0; i < n; i++) {
            Tile t = tm.tile[i];
            if (t == null || t.image == null) { avg[i] = fallback; coverage[i] = 0f; continue; }
            BufferedImage img = t.image;
            long r = 0, g = 0, b = 0, a = 0;
            int w = img.getWidth(), h = img.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = img.getRGB(x, y);
                    int al = (p >>> 24) & 0xFF;
                    if (al == 0) continue;
                    r += (long) ((p >> 16) & 0xFF) * al;
                    g += (long) ((p >> 8) & 0xFF) * al;
                    b += (long) (p & 0xFF) * al;
                    a += al;
                }
            }
            if (a == 0) { avg[i] = fallback; coverage[i] = 0f; continue; }
            avg[i] = (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
            coverage[i] = a / (255f * w * h);
        }

        // campur tile yang bolong-bolong dengan rumput dominan (tile 4) supaya gak bolong hitam
        int grass = avg[4];
        for (int i = 0; i < n; i++) {
            float c = coverage[i];
            if (c >= 0.999f || c == 0f) continue;
            int r = Math.round(((avg[i] >> 16) & 0xFF) * c + ((grass >> 16) & 0xFF) * (1 - c));
            int g = Math.round(((avg[i] >> 8) & 0xFF) * c + ((grass >> 8) & 0xFF) * (1 - c));
            int b = Math.round((avg[i] & 0xFF) * c + (grass & 0xFF) * (1 - c));
            avg[i] = r << 16 | g << 8 | b;
        }
        return avg;
    }

    /** Index tile objek dunia -> jenis penanda di minimap. Dekorasi kecil sengaja dilewati biar tidak ramai. */
    private static int kindOf(int tileIndex) {
        switch (tileIndex) {
            case 11:            // rumah
            case 63:            // monastery
            case 70:            // rumah penyihir / jungle house
            case 72:            // tambang golem
                return K_BUILDING;
            case 49: case 50: case 52:
                return K_RUINS;
            case 25: case 26: case 27:
                return K_BRIDGE;
            case 5: case 10:
                return K_TREE;
            case 66:
                return K_BUSH;
            default:
                return K_NONE;
        }
    }

    private void paintObject(Graphics2D g, int[] o, int kind) {
        // o = {x, y, w, h, tileIndex, isFlat, shadow}
        // pusat objek di dunia -> pixel minimap; objek yang terlalu kecil dibesarkan ke ukuran minimum
        double cx = (o[0] + o[2] / 2.0) * scaleX;
        double cy = (o[1] + o[3] / 2.0) * scaleY;
        if (cx < 0 || cy < 0 || cx >= mapW || cy >= mapH) return; // di luar peta (mis. data salah ketik)

        int minSize;
        Color fill;
        switch (kind) {
            case K_BUILDING: minSize = 5; fill = BUILDING; break;
            case K_RUINS:    minSize = 3; fill = RUINS;    break;
            case K_BRIDGE:   minSize = 2; fill = BRIDGE;   break;
            case K_TREE:     minSize = 2; fill = TREE;     break;
            default:         minSize = 1; fill = BUSH;     break;
        }

        int w = Math.max(minSize, (int) Math.round(o[2] * scaleX));
        int h = Math.max(minSize, (int) Math.round(o[3] * scaleY));
        int x = (int) Math.round(cx - w / 2.0);
        int y = (int) Math.round(cy - h / 2.0);

        if (kind == K_BUILDING) {
            g.setColor(BUILDING_O);
            g.fillRect(x - 1, y - 1, w + 2, h + 2);
        }
        g.setColor(fill);
        g.fillRect(x, y, w, h);
    }
}
