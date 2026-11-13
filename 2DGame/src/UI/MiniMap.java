package UI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

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
 * - PENJELAJAHAN: objek (rumah, pohon, jembatan, reruntuhan, peti, titik pohon/batu) cuma muncul di area yang
 *   sudah dihampiri player (radius REVEAL_RADIUS_TILES dari posisinya). Sisanya cuma tanah yang digelapkan.
 *   Area yang sudah dijelajahi ikut tersimpan di save (lihat exportExplored / importExplored).
 * - Lapisan statis (tanah + objek dunia) digambar SEKALI, lalu gambar cache cuma di-update di piksel yang baru
 *   terbuka, jadi per frame ongkosnya kecil: tinggal drawImage + titik pohon/batu + marker player.
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

    // ===== PENJELAJAHAN (ubah di sini kalau mau) =====
    /** Seberapa jauh (dalam tile) area di sekitar player ikut terbuka di peta. Layar game sendiri lebarnya 16 tile. */
    private static final int REVEAL_RADIUS_TILES = 24;
    /** Gelap tanah yang belum dijelajahi: 0 = tampil normal (cuma objeknya yang disembunyikan), 1 = hitam penuh. */
    private static final float UNEXPLORED_DIM = 0.55f;

    // ===== WARNA =====
    private static final Color FRAME      = new Color(220, 190, 150);
    private static final Color FRAME_DARK = new Color(45, 30, 20);
    private static final Color BUTTON_BG  = new Color(60, 45, 35);
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

    private volatile BufferedImage base; // cache resolusi per-tile agar tetap jelas saat di-zoom (yang dipajang di layar)
    private int[] terrainPx;             // tanah penuh warna, 1 piksel = 1 tile (RGB)
    private int[] objectPx;              // objek dunia (ARGB, alpha 0 = tidak ada objek di piksel itu)
    private boolean[] explored;          // 1 sel = 1 piksel peta = 1 tile; true kalau sudah dihampiri player
    private byte[] pendingExplored;      // hasil load save yang datang sebelum peta sempat di-bake
    private int lastRevealX = Integer.MIN_VALUE, lastRevealY = Integer.MIN_VALUE;
    private int mapW, mapH;              // ukuran cache dalam tile/pixel
    private int previewW, previewH;      // ukuran minimap kecil di layar
    private double baseScaleX, baseScaleY;
    private double previewScaleX, previewScaleY;

    // Tampilan peta besar.
    private boolean expanded;
    private double expandedZoom = 1.0;
    private double panX, panY;
    private boolean dragging;
    private int lastDragX, lastDragY;
    private Rectangle previewBounds;
    private Rectangle expandedBounds;
    private Rectangle expandedViewport;
    private Rectangle closeButton, zoomInButton, zoomOutButton, resetButton;

    private static final double EXPANDED_ZOOM_MIN = 1.0;
    private static final double EXPANDED_ZOOM_MAX = 6.0;
    private static final double EXPANDED_ZOOM_STEP = 0.25;

    public MiniMap(GamePanel gp) {
        this.gp = gp;
    }

    // =====================================================================
    //  PENJELAJAHAN (fog of war)
    // =====================================================================

    /**
     * Dipanggil tiap frame selama player di dunia luar (GamePanel.update): buka area di sekitar player.
     * Jalan terus walau minimap lagi tersembunyi, jadi jalur yang dilewati tetap tercatat.
     */
    public void update() {
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return; // di dalam rumah/domain koordinatnya beda dunia
        ensureBaked();
        int tx = (int) Math.floor((gp.player.x + gp.tileSize / 2.0) * baseScaleX);
        int ty = (int) Math.floor((gp.player.y + gp.tileSize / 2.0) * baseScaleY);
        if (tx == lastRevealX && ty == lastRevealY) return;  // belum pindah tile
        synchronized (this) {
            lastRevealX = tx;
            lastRevealY = ty;
            reveal(tx, ty);
        }
    }

    /** Tandai lingkaran di sekitar (cx, cy) sebagai sudah dijelajahi dan terangkan piksel yang baru terbuka. */
    private void reveal(int cx, int cy) {
        if (explored == null || base == null) return;
        final int r = REVEAL_RADIUS_TILES, r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            int y = cy + dy;
            if (y < 0 || y >= mapH) continue;
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy > r2) continue;
                int x = cx + dx;
                if (x < 0 || x >= mapW) continue;
                int i = y * mapW + x;
                if (explored[i]) continue;
                explored[i] = true;
                base.setRGB(x, y, pixelAt(i));
            }
        }
    }

    /** true kalau titik dunia (px) ini sudah pernah dihampiri player. */
    private boolean isExploredAt(double worldX, double worldY) {
        boolean[] ex = explored;
        if (ex == null) return false;
        int x = (int) Math.floor(worldX * baseScaleX), y = (int) Math.floor(worldY * baseScaleY);
        if (x < 0 || y < 0 || x >= mapW || y >= mapH) return false;
        return ex[y * mapW + x];
    }

    /** Hapus semua area yang sudah dijelajahi (dipanggil GamePanel.setupGame: new game / load game mulai dari nol). */
    public synchronized void resetExplored() {
        pendingExplored = null;
        lastRevealX = lastRevealY = Integer.MIN_VALUE;
        if (explored == null || base == null) return;
        java.util.Arrays.fill(explored, false);
        rebuildBase(base);
    }

    /**
     * Area yang sudah dijelajahi sebagai byte[] untuk disimpan di save: 8 byte header (lebar, tinggi peta) + bitset.
     * Bisa null kalau belum ada apa-apa.
     */
    public synchronized byte[] exportExplored() {
        if (explored == null) return pendingExplored;   // peta belum di-bake: teruskan data hasil load apa adanya
        java.util.BitSet bs = new java.util.BitSet(explored.length);
        for (int i = 0; i < explored.length; i++) if (explored[i]) bs.set(i);
        byte[] bits = bs.toByteArray();
        return java.nio.ByteBuffer.allocate(8 + bits.length).putInt(mapW).putInt(mapH).put(bits).array();
    }

    /** Kebalikan exportExplored (dari save). null / format tidak cocok = diabaikan, save lama tetap aman. */
    public synchronized void importExplored(byte[] data) {
        if (data == null || data.length < 8) return;
        if (explored == null || base == null) {         // peta baru di-bake nanti saat player pertama kali di luar
            pendingExplored = data;
            return;
        }
        if (applyExplored(data)) rebuildBase(base);
    }

    private boolean applyExplored(byte[] data) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(data);
        if (bb.getInt() != mapW || bb.getInt() != mapH) return false;   // ukuran dunia berubah sejak save dibuat
        byte[] bits = new byte[bb.remaining()];
        bb.get(bits);
        java.util.BitSet bs = java.util.BitSet.valueOf(bits);
        for (int i = bs.nextSetBit(0); i >= 0 && i < explored.length; i = bs.nextSetBit(i + 1)) explored[i] = true;
        return true;
    }

    private synchronized void ensureBaked() {
        if (base == null) bake();
    }

    /** Warna piksel i di gambar peta: belum dijelajahi = tanah digelapkan; sudah = tanah + objek. */
    private int pixelAt(int i) {
        if (!explored[i]) return dim(terrainPx[i]);
        int o = objectPx[i];
        return (o >>> 24) != 0 ? (o & 0xFFFFFF) : terrainPx[i];
    }

    private void rebuildBase(BufferedImage target) {
        int[] px = new int[mapW * mapH];
        for (int i = 0; i < px.length; i++) px[i] = pixelAt(i);
        target.setRGB(0, 0, mapW, mapH, px, 0, mapW);
    }

    private static int dim(int rgb) {
        float k = 1f - UNEXPLORED_DIM;
        int r = (int) (((rgb >> 16) & 0xFF) * k), g = (int) (((rgb >> 8) & 0xFF) * k), b = (int) ((rgb & 0xFF) * k);
        return r << 16 | g << 8 | b;
    }

    // =====================================================================
    //  GAMBAR PER FRAME
    // =====================================================================

    public void draw(Graphics2D g2) {
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return; // di dalam rumah/domain koordinatnya beda dunia
        ensureBaked();

        if (expanded) {
            drawExpanded(g2);
            return;
        }

        int panelW = previewW + BORDER * 2;
        int panelH = previewH + BORDER * 2;
        int px = gp.screenWidth - panelW - MARGIN;
        int py = MARGIN;
        int mx = px + BORDER;
        int my = py + BORDER;
        previewBounds = new Rectangle(px, py, panelW, panelH);

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
        g2.setClip(new RoundRectangle2D.Float(mx, my, previewW, previewH, 4, 4));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.drawImage(base, mx, my, previewW, previewH, null);

        drawBreakables(g2, mx, my, previewScaleX, previewScaleY);
        drawChest(g2, mx, my, previewScaleX, previewScaleY);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawPlayer(g2, mx, my, previewScaleX, previewScaleY, previewW, previewH);

        // garis tipis di tepi dalam supaya peta "tenggelam" rapi di bingkai
        g2.setClip(oldClip);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawRoundRect(mx, my, previewW - 1, previewH - 1, 4, 4);

        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /** Pohon & batu yang bisa ditebang/dihancurkan: yang sedang hancur tidak ditampilkan. */
    private void drawBreakables(Graphics2D g2, int mx, int my, double sx, double sy) {
        breakable[] list = gp.tileM.breakables;
        int[] tiles = gp.tileM.breakableTile;
        if (list == null || tiles == null) return;

        for (int i = 0; i < list.length; i++) {
            breakable b = list[i];
            if (b == null || b.destroyed) continue;
            if (!isExploredAt(b.worldX + b.width / 2.0, b.worldY + b.height / 2.0)) continue; // belum dihampiri: jangan dibocorkan
            g2.setColor(tiles[i] == 55 ? TREE : ROCK); // 55 = pohon, sisanya batu
            int cx = mx + (int) Math.round((b.worldX + b.width / 2.0) * sx);
            int cy = my + (int) Math.round((b.worldY + b.height / 2.0) * sy);
            g2.fillRect(cx - 1, cy - 1, 2, 2);
        }
    }

    private void drawChest(Graphics2D g2, int mx, int my, double sx, double sy) {
        if (gp.chest == null) return;
        if (!isExploredAt(World.StorageChest.WORLD_X + gp.tileSize / 2.0, World.StorageChest.WORLD_Y + gp.tileSize / 2.0)) return;
        int cx = mx + (int) Math.round((World.StorageChest.WORLD_X + gp.tileSize / 2.0) * sx);
        int cy = my + (int) Math.round((World.StorageChest.WORLD_Y + gp.tileSize / 2.0) * sy);
        g2.setColor(BUILDING_O);
        g2.fillRect(cx - 2, cy - 2, 5, 5);
        g2.setColor(CHEST);
        g2.fillRect(cx - 1, cy - 1, 3, 3);
    }

    /** Marker player: titik merah berbingkai putih + cincin berdenyut biar gampang dicari. */
    private void drawPlayer(Graphics2D g2, int mx, int my, double sx, double sy, int viewW, int viewH) {
        // pusat sprite player (player.x/y = pojok kiri-atas kotak 1 tile)
        double wx = gp.player.x + gp.tileSize / 2.0;
        double wy = gp.player.y + gp.tileSize / 2.0;
        float cx = mx + clamp((float) (wx * sx), 0, viewW - 1);
        float cy = my + clamp((float) (wy * sy), 0, viewH - 1);

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
    //  INTERAKSI PETA BESAR
    // =====================================================================

    public boolean isExpanded() {
        return expanded;
    }

    /** Klik minimap kecil untuk membuka peta besar; klik di luar / tombol X untuk menutup. */
    public boolean handleMousePressed(MouseEvent e) {
        if (gp.gameState != GamePanel.GameState.OUTSIDE || e.getButton() != MouseEvent.BUTTON1) return false;

        int x = e.getX();
        int y = e.getY();

        if (!expanded) {
            if (previewBounds != null && previewBounds.contains(x, y)) {
                expanded = true;
                expandedZoom = 1.0;
                panX = 0;
                panY = 0;
                dragging = false;
                return true;
            }
            return false;
        }

        if (closeButton != null && closeButton.contains(x, y)) {
            expanded = false;
            dragging = false;
            return true;
        }
        if (zoomInButton != null && zoomInButton.contains(x, y)) {
            zoomAt(expandedViewport.x + expandedViewport.width / 2,
                    expandedViewport.y + expandedViewport.height / 2, EXPANDED_ZOOM_STEP);
            return true;
        }
        if (zoomOutButton != null && zoomOutButton.contains(x, y)) {
            zoomAt(expandedViewport.x + expandedViewport.width / 2,
                    expandedViewport.y + expandedViewport.height / 2, -EXPANDED_ZOOM_STEP);
            return true;
        }
        if (resetButton != null && resetButton.contains(x, y)) {
            expandedZoom = 1.0;
            panX = 0;
            panY = 0;
            return true;
        }
        if (expandedViewport != null && expandedViewport.contains(x, y)) {
            dragging = true;
            lastDragX = x;
            lastDragY = y;
            return true;
        }

        // Klik di panel/header hanya ditangkap; klik di luar panel menutup peta.
        if (expandedBounds != null && expandedBounds.contains(x, y)) return true;
        expanded = false;
        dragging = false;
        return true;
    }

    public boolean handleMouseDragged(MouseEvent e) {
        if (!expanded || !dragging) return false;
        panX += e.getX() - lastDragX;
        panY += e.getY() - lastDragY;
        lastDragX = e.getX();
        lastDragY = e.getY();
        clampPan();
        return true;
    }

    public boolean handleMouseReleased(MouseEvent e) {
        if (!dragging) return false;
        dragging = false;
        return true;
    }

    /** Scroll mouse di atas peta besar = zoom peta, bukan zoom dunia. */
    public boolean handleMouseWheel(MouseWheelEvent e) {
        if (!expanded) return false;
        if (expandedViewport != null && expandedViewport.contains(e.getX(), e.getY())) {
            zoomAt(e.getX(), e.getY(), -e.getWheelRotation() * EXPANDED_ZOOM_STEP);
        }
        return true;
    }

    private void zoomAt(double mouseX, double mouseY, double amount) {
        if (expandedViewport == null || mapW <= 0 || mapH <= 0) return;
        double oldScale = fitScale() * expandedZoom;
        double mapPointX = (mouseX - mapOriginX(oldScale)) / oldScale;
        double mapPointY = (mouseY - mapOriginY(oldScale)) / oldScale;
        double nextZoom = Math.max(EXPANDED_ZOOM_MIN,
                Math.min(EXPANDED_ZOOM_MAX, expandedZoom + amount));
        if (nextZoom == expandedZoom) return;

        expandedZoom = nextZoom;
        double newScale = fitScale() * expandedZoom;
        panX = mouseX - (expandedViewport.x + (expandedViewport.width - mapW * newScale) / 2.0
                + mapPointX * newScale);
        panY = mouseY - (expandedViewport.y + (expandedViewport.height - mapH * newScale) / 2.0
                + mapPointY * newScale);
        clampPan();
    }

    private double fitScale() {
        if (expandedViewport == null || mapW <= 0 || mapH <= 0) return 1.0;
        return Math.min(expandedViewport.width / (double) mapW,
                expandedViewport.height / (double) mapH);
    }

    private double mapOriginX(double scale) {
        return expandedViewport.x + (expandedViewport.width - mapW * scale) / 2.0 + panX;
    }

    private double mapOriginY(double scale) {
        return expandedViewport.y + (expandedViewport.height - mapH * scale) / 2.0 + panY;
    }

    private void clampPan() {
        if (expandedViewport == null) return;
        double scale = fitScale() * expandedZoom;
        double maxPanX = Math.max(0, (mapW * scale - expandedViewport.width) / 2.0);
        double maxPanY = Math.max(0, (mapH * scale - expandedViewport.height) / 2.0);
        panX = Math.max(-maxPanX, Math.min(maxPanX, panX));
        panY = Math.max(-maxPanY, Math.min(maxPanY, panY));
    }

    private void drawExpanded(Graphics2D g2) {
        int panelX = 14;
        int panelY = 14;
        int panelW = gp.screenWidth - 28;
        int panelH = gp.screenHeight - 28;
        expandedBounds = new Rectangle(panelX, panelY, panelW, panelH);
        expandedViewport = new Rectangle(panelX + 12, panelY + 44, panelW - 24, panelH - 76);
        clampPan();

        Composite oldComposite = g2.getComposite();
        Shape oldClip = g2.getClip();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(oldComposite);

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(FRAME_DARK);
        g2.fillRoundRect(panelX - 2, panelY - 2, panelW + 4, panelH + 4, 14, 14);
        g2.setColor(FRAME);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
        g2.setColor(FRAME_DARK);
        g2.fillRoundRect(panelX + 4, panelY + 4, panelW - 8, panelH - 8, 9, 9);

        g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 15));
        g2.setColor(new Color(255, 225, 175));
        g2.drawString("WORLD MAP", panelX + 16, panelY + 27);

        int controlY = panelY + 9;
        closeButton = new Rectangle(panelX + panelW - 38, controlY, 28, 24);
        resetButton = new Rectangle(closeButton.x - 65, controlY, 58, 24);
        zoomInButton = new Rectangle(resetButton.x - 32, controlY, 28, 24);
        zoomOutButton = new Rectangle(zoomInButton.x - 32, controlY, 28, 24);
        drawMapButton(g2, zoomOutButton, "-");
        drawMapButton(g2, zoomInButton, "+");
        drawMapButton(g2, resetButton, "RESET");
        drawMapButton(g2, closeButton, "X");

        g2.setColor(new Color(18, 20, 19));
        g2.fillRect(expandedViewport.x, expandedViewport.y,
                expandedViewport.width, expandedViewport.height);
        g2.setColor(FRAME);
        g2.drawRect(expandedViewport.x, expandedViewport.y,
                expandedViewport.width, expandedViewport.height);

        double scale = fitScale() * expandedZoom;
        int drawW = Math.max(1, (int) Math.round(mapW * scale));
        int drawH = Math.max(1, (int) Math.round(mapH * scale));
        int mapX = (int) Math.round(mapOriginX(scale));
        int mapY = (int) Math.round(mapOriginY(scale));

        g2.setClip(expandedViewport);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(base, mapX, mapY, drawW, drawH, null);

        double markerScale = scale / gp.tileSize;
        drawBreakables(g2, mapX, mapY, markerScale, markerScale);
        drawChest(g2, mapX, mapY, markerScale, markerScale);
        drawPlayer(g2, mapX, mapY, markerScale, markerScale, drawW, drawH);

        g2.setClip(oldClip);
        g2.setColor(new Color(255, 235, 200));
        g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10));
        g2.drawString("ZOOM " + Math.round(expandedZoom * 100) + "%  |  SCROLL TO ZOOM  |  DRAG TO PAN  |  CLICK OUTSIDE TO CLOSE",
                panelX + 16, panelY + panelH - 10);

        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        if (oldInterpolation != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
    }

    private void drawMapButton(Graphics2D g2, Rectangle rect, String label) {
        g2.setColor(BUTTON_BG);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);
        g2.setColor(FRAME);
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);
        g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int ty = rect.y + (rect.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(label, tx, ty);
    }

    // =====================================================================
    //  LAPISAN STATIS (dibuat sekali)
    // =====================================================================

    private void bake() {
        TileM tm = gp.tileM;
        int cols = tm.loadedCols > 0 ? tm.loadedCols : gp.maxWorldCol;
        int rows = tm.loadedRows > 0 ? tm.loadedRows : gp.maxWorldRow;

        // Cache satu pixel per tile supaya peta besar tetap tajam saat diperbesar.
        mapW = cols;
        mapH = rows;
        if (cols >= rows) {
            previewW = MAP_LONG_SIDE;
            previewH = Math.max(1, Math.round(MAP_LONG_SIDE * (float) rows / cols));
        } else {
            previewH = MAP_LONG_SIDE;
            previewW = Math.max(1, Math.round(MAP_LONG_SIDE * (float) cols / rows));
        }
        baseScaleX = (double) mapW / (cols * gp.tileSize);
        baseScaleY = (double) mapH / (rows * gp.tileSize);
        previewScaleX = (double) previewW / (cols * gp.tileSize);
        previewScaleY = (double) previewH / (rows * gp.tileSize);

        // 1) tanah: tiap pixel minimap = rata-rata warna tile yang jatuh di pixel itu
        int[] tileColor = averageTileColors(tm);
        long[] sr = new long[mapW * mapH];
        long[] sg = new long[mapW * mapH];
        long[] sb = new long[mapW * mapH];
        int[] cnt = new int[mapW * mapH];

        for (int col = 0; col < cols; col++) {
            int tx = Math.min(mapW - 1, col * mapW / cols);
            for (int row = 0; row < rows; row++) {
                int t = tm.minimapTile(col, row); // salju / es dihitung dari generator, tidak tergantung chunk yang termuat
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

        terrainPx = new int[mapW * mapH];
        for (int i = 0; i < terrainPx.length; i++) {
            int c = Math.max(1, cnt[i]);
            terrainPx[i] = (int) (sr[i] / c) << 16 | (int) (sg[i] / c) << 8 | (int) (sb[i] / c);
        }

        // 2) objek dunia: yang kecil dulu, bangunan paling atas. Digambar ke lapisan sendiri (transparan di tempat
        //    kosong) supaya bisa disembunyikan di area yang belum dijelajahi.
        int[][] objs = tm.getWorldObjects();
        BufferedImage layer = new BufferedImage(mapW, mapH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = layer.createGraphics();
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
        objectPx = layer.getRGB(0, 0, mapW, mapH, null, 0, mapW);

        // 3) area yang sudah dijelajahi (dari save kalau ada), lalu gabungkan jadi gambar yang dipajang.
        //    `base` diisi paling akhir supaya draw() tidak pernah melihat gambar setengah jadi.
        explored = new boolean[mapW * mapH];
        lastRevealX = lastRevealY = Integer.MIN_VALUE;
        if (pendingExplored != null) {
            applyExplored(pendingExplored);
            pendingExplored = null;
        }
        BufferedImage img = new BufferedImage(mapW, mapH, BufferedImage.TYPE_INT_RGB);
        rebuildBase(img);
        base = img;
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
            case 25: case 26: case 27:          // jembatan vertikal
            case 73: case 74: case 75:          // jembatan horizontal
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
        double cx = (o[0] + o[2] / 2.0) * baseScaleX;
        double cy = (o[1] + o[3] / 2.0) * baseScaleY;
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

        int w = Math.max(minSize, (int) Math.round(o[2] * baseScaleX));
        int h = Math.max(minSize, (int) Math.round(o[3] * baseScaleY));
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
