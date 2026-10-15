package World;

import Main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 * HouseManager — handle semua rumah di world.
 */
public class HouseManager {

    public boolean insideHouse = false;
    public int currentHouseIndex = -1;

    private final List<HouseData> houses = new ArrayList<>();

    // Runtime interior state
    private int[][] floorMap;
    private final List<Rectangle> wallCollisions = new ArrayList<>();
    private HouseData activeHouse = null;

    private javax.swing.ImageIcon bgGif = null;
    private boolean useGifBackground = false;
    
    // JLabel rahasia untuk memancing Java Swing menggerakkan animasi GIF dengan benar
    private final JLabel gifObserverLabel = new JLabel(); 
    
    // Fade transition
    private int fadeAlpha = 0;
    private boolean fading = false;
    private boolean fadeIn = false;
    private Runnable onFadeComplete;

    private final GamePanel gp;

    // ─────────────────────────────────────────────────────────────────────
    // KONSTRUKTOR
    // ─────────────────────────────────────────────────────────────────────
    public HouseManager(GamePanel gp) {
        this.gp = gp;
        registerAllHouses();
    }

    // ─────────────────────────────────────────────────────────────────────
    // DAFTAR SEMUA RUMAH
    // ─────────────────────────────────────────────────────────────────────
    private void registerAllHouses() {
        int ts = gp.tileSize;

        // ── Rumah Utama ───────────────────────────────────────────────────
        HouseData rumahUtama = new HouseData("Main House", 995, 705, 250, 200, 300, 250)
            .setFloor(10, 8, 12)
            .setExit(1100, 910)
            .addFurniture("/Aset/Anvil.png",           ts*4, ts*3, ts,   ts,   true)
            .addFurniture("/Aset/swordShieldDecor.png", ts*7, ts*1, ts,   ts,   true)
            .addFurniture("/Aset/Box.png",              ts*1, ts*5, ts,   ts,   true)
            .addFurniture("/Aset/Box.png",              ts*2, ts*5, ts,   ts,   true)
            .addGif("/Aset/CampFire.gif", ts*2, ts*2, 64);
        houses.add(rumahUtama);

        // ── Kedai Herbal ──────────────────────────────────────────────────
        HouseData kedaiHerbal = new HouseData("kastil", 1205, 800, 200, 160, 200, 150)
            .setFloor(8, 6, 13)
            .setExit(1300, 850)
            .addFurniture("/Aset/Box.png",   ts*1, ts*1, ts, ts, true)
            .addFurniture("/Aset/Box.png",   ts*3, ts*1, ts, ts, true)
            .addFurniture("/Aset/Box.png",   ts*5, ts*1, ts, ts, true)
            .addFurniture("/Aset/Anvil.png", ts*3, ts*4, ts, ts, true);
        houses.add(kedaiHerbal);

     // ── Rumah Blacksmith (Monastery) ──────────────────────────────────
        HouseData blacksmith = new HouseData(
        	    "monastery",
        	    1640, 400, 300, 300,
        	    new Rectangle(1700, 640, 160, 110),
        	    620, 315
        	)
        	.setFloor(14, 10, 12)
        	.setExit(1755, 695)
        	.setGifBackground("/Aset/mockup6.gif")
        	.disableAutoWalls()
        	.addCollision(0, 0, 672, 100)      // tembok atas
        	.addCollision(0, 0, 230, 140)      // tembok atas
        	.addCollision(435, 0, 220, 140)    // tembok atas
        	.addCollision(0, 440, 672, 40)    // tembok bawah
        	.addCollision(0, 0, 40, 480)      // tembok kiri
        	.addCollision(632, 0, 40, 300)    // tembok kanan
        	.addCollision(632, 370, 40, 50)    // tembok kanan
        	.addCollision(632, 0, 40, 300)    // tembok kanan
        	.addCollision(665, 245, 40, 300)    // tembok kanan
        	
        	.addCollision(255, 190, 90, 35)// contoh: pilar/meja di tengah ruangan
        	.addCollision(420, 190, 100, 35)
        	.addCollision(425, 285, 55, 100)
        	.addCollision(260, 285, 55, 100)
        	.addCollision(320, 90, 30, 30);
        	houses.add(blacksmith);

        // ── Rumah Penyihir ──────────────────────────────────────────────
        houses.add(WitchHouse.create(gp));
    }

    // ─────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────
    public void update() {
        handleFade();
        checkEnterHouse();
        checkExitZone();
    }

    private void checkEnterHouse() {
        if (insideHouse) return;
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return;
        if (!gp.KeyH.interactPressed) return;

        Rectangle playerArea = gp.player.getInteractArea();

        for (int i = 0; i < houses.size(); i++) {
            HouseData h = houses.get(i);
            if (playerArea.intersects(h.doorArea)) {
                gp.KeyH.interactPressed = false;
                currentHouseIndex = i;
                startEnter(h);
                return;
            }
        }
    }

    /**
     * Cek apakah player lagi berdiri di exitZone rumah yang aktif.
     * Kalau iya, keluar otomatis tanpa perlu ESC/tombol apapun.
     */
    private void checkExitZone() {
        if (!insideHouse || activeHouse == null || fading) return;
        if (activeHouse.exitZone == null) return;

        // TODO: kalau Player udah punya hitbox sendiri (misal solidArea atau
        // getCollisionArea()), pake itu di sini biar deteksinya lebih akurat
        // daripada nebak x,y + tileSize kayak sekarang.
        Rectangle playerBox = new Rectangle(gp.player.x, gp.player.y, gp.tileSize, gp.tileSize);

        if (playerBox.intersects(activeHouse.exitZone)) {
            exitHouse();
        }
    }

    /**
     * Dipanggil dari KeyHandler/GamePanel sebelum proses keluar via ESC,
     * misalnya: if (gp.houseManager.isEscExitAllowed()) gp.houseManager.exitHouse();
     */
    public boolean isEscExitAllowed() {
        return activeHouse == null || activeHouse.escExitEnabled;
    }

    private void startEnter(HouseData h) {
        startFade(false, () -> {
            buildInteriorFor(h);
            activeHouse = h;
            insideHouse = true;
            gp.gameState = GamePanel.GameState.INSIDE;
            gp.player.x = h.spawnX;
            gp.player.y = h.spawnY;
            startFade(true, null);
        });
    }

    public void exitHouse() {
        if (activeHouse == null) return;
        final int exitX = activeHouse.exitWorldX;
        final int exitY = activeHouse.exitWorldY;

        startFade(false, () -> {
            insideHouse = false;
            currentHouseIndex = -1;
            activeHouse = null;
            gp.gameState = GamePanel.GameState.OUTSIDE;
            gp.player.x = exitX;
            gp.player.y = exitY;
            startFade(true, null);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // BUILD INTERIOR
    // ─────────────────────────────────────────────────────────────────────
    private void buildInteriorFor(HouseData h) {
        wallCollisions.clear();
        int ts = gp.tileSize;

        // Reset dulu
        useGifBackground = false;
        bgGif = null;
        gifObserverLabel.setIcon(null);

        // Cek apakah rumah ini pakai GIF background
        if (h.gifBackgroundPath != null && !h.gifBackgroundPath.isEmpty()) {
            try {
                java.net.URL url = getClass().getResource(h.gifBackgroundPath);
                if (url != null) {
                    bgGif = new javax.swing.ImageIcon(url);
                    gifObserverLabel.setIcon(bgGif);
                    useGifBackground = true;
                    Main.Log.d("[HouseManager] GIF background loaded: " + h.gifBackgroundPath);
                } else {
                    System.out.println("[HouseManager] PERINGATAN: GIF tidak ketemu di: " + h.gifBackgroundPath);
                }
            } catch (Exception e) {
                System.out.println("[HouseManager] Gagal load GIF: " + h.gifBackgroundPath);
                e.printStackTrace();
            }
        }

     // Lantai
        floorMap = new int[h.floorCols][h.floorRows];
        for (int c = 0; c < h.floorCols; c++)
            for (int r = 0; r < h.floorRows; r++)
                floorMap[c][r] = h.floorTile;

        // Dinding perimeter otomatis (opsional)
        int totalW = h.floorCols * ts;
        int totalH = h.floorRows * ts;
        if (h.autoWalls) {
            wallCollisions.add(new Rectangle(0,    -ts,    totalW, ts));
            wallCollisions.add(new Rectangle(0,    totalH, totalW, ts));
            wallCollisions.add(new Rectangle(-ts,  0,      ts, totalH));
            wallCollisions.add(new Rectangle(totalW, 0,    ts, totalH));
        }

        // Collision furniture
        for (HouseData.FurnitureEntry f : h.furnitures) {
            if (f.collision) {
                wallCollisions.add(new Rectangle(f.x, f.y, f.w, f.h));
            }
        }

        // Collision custom — full manual, x,y,w,h apa adanya, gak ada multiplier/auto apapun
        for (Rectangle r : h.interiorCollisions) {
            wallCollisions.add(r);
        }
    }
    // ─────────────────────────────────────────────────────────────────────
    // DRAW INTERIOR
    // ─────────────────────────────────────────────────────────────────────
    public void draw(Graphics2D g2) {
        if (activeHouse == null) return;

        g2.setColor(new Color(15, 10, 8));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        int psx = gp.player.screenX;
        int psy = gp.player.screenY;
        int camX = gp.player.x;
        int camY = gp.player.y;

        // ── BAGIAN DUNIA (ikut ke-zoom): lantai, tembok, furniture, gif ──
        java.awt.geom.AffineTransform baseTx = gp.applyWorldZoom(g2);
        drawFloor(g2, camX, camY, psx, psy, activeHouse);

        if (!useGifBackground) {
            drawWalls(g2, camX, camY, psx, psy, activeHouse);
        }

        drawFurniture(g2, camX, camY, psx, psy, activeHouse);
        drawGifs(g2, camX, camY, psx, psy, activeHouse);
        g2.setTransform(baseTx);

        // ── BAGIAN UI (layar normal, gak ke-zoom) ──
        drawHint(g2, activeHouse);
        drawFadeOverlay(g2);

        //if (gp.debugCollision)
          //  drawDebugWalls(g2, camX, camY, psx, psy);
    }
    
    private void drawFloor(Graphics2D g2, int camX, int camY, int psx, int psy, HouseData h) {
        int ts = gp.tileSize;
        int totalW = h.floorCols * ts;
        int totalH = h.floorRows * ts;

        if (useGifBackground && bgGif != null) {
            int sx = 0 - camX + psx;
            int sy = 0 - camY + psy;
            g2.drawImage(bgGif.getImage(), sx, sy, totalW, totalH, gp);
            return;
        }

        // Tile grid biasa untuk rumah non-GIF
        for (int c = 0; c < h.floorCols; c++) {
            for (int r = 0; r < h.floorRows; r++) {
                int tileNum = floorMap[c][r];
                int sx = c * ts - camX + psx;
                int sy = r * ts - camY + psy;
                if (sx + ts < gp.viewMinX() || sx > gp.viewMaxX() || sy + ts < gp.viewMinY() || sy > gp.viewMaxY()) continue;

                BufferedImage img = (gp.tileM.tile[tileNum] != null) ? gp.tileM.tile[tileNum].image : null;
                if (img != null) {
                    g2.drawImage(img, sx, sy, ts, ts, null);
                } else {
                    g2.setColor(new Color(139, 90, 43));
                    g2.fillRect(sx, sy, ts, ts);
                }
            }
        }
    }
    
    private void drawWalls(Graphics2D g2, int camX, int camY, int psx, int psy, HouseData h) {
        int ts = gp.tileSize;
        int totalW = h.floorCols * ts;
        int totalH = h.floorRows * ts;
        int originSX = 0 - camX + psx;
        int originSY = 0 - camY + psy;

        drawWallStrip(g2, originSX,         originSY - ts,       totalW,       ts);
        drawWallStrip(g2, originSX,         originSY + totalH,   totalW,       ts);
        drawWallStrip(g2, originSX - ts,    originSY - ts,       ts, totalH + ts * 2);
        drawWallStrip(g2, originSX + totalW,originSY - ts,       ts, totalH + ts * 2);
    }

    private void drawWallStrip(Graphics2D g2, int sx, int sy, int w, int h) {
        g2.setColor(new Color(70, 45, 25));
        g2.fillRect(sx, sy, w, h);
        g2.setColor(new Color(50, 30, 12));
        int brickH = 14, brickW = 32;
        for (int row = sy; row < sy + h; row += brickH) {
            int offset = ((row - sy) / brickH % 2 == 0) ? 0 : brickW / 2;
            for (int col = sx + offset; col < sx + w; col += brickW)
                g2.drawRect(col, row, brickW, brickH);
        }
        g2.setColor(new Color(40, 25, 10));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(sx, sy, w - 1, h - 1);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawFurniture(Graphics2D g2, int camX, int camY, int psx, int psy, HouseData h) {
        for (HouseData.FurnitureEntry f : h.furnitures) {
            int sx = f.x - camX + psx;
            int sy = f.y - camY + psy;
            if (sx + f.w < gp.viewMinX() || sx > gp.viewMaxX() || sy + f.h < gp.viewMinY() || sy > gp.viewMaxY()) continue;

            BufferedImage img = f.getImage(getClass());
            if (img != null) {
                g2.drawImage(img, sx, sy, f.w, f.h, null);
            } else {
                g2.setColor(new Color(100, 100, 100));
                g2.fillRect(sx, sy, f.w, f.h);
            }
        }
    }

    private void drawGifs(Graphics2D g2, int camX, int camY, int psx, int psy, HouseData h) {
        for (HouseData.GifEntry gif : h.gifs) {
            javax.swing.ImageIcon icon = gif.getGif(getClass());
            if (icon == null) continue;
            int sx = gif.x - camX + psx;
            int sy = gif.y - camY + psy;
            if (sx + gif.size < gp.viewMinX() || sx > gp.viewMaxX() || sy + gif.size < gp.viewMinY() || sy > gp.viewMaxY()) continue;
            g2.drawImage(icon.getImage(), sx, sy, gif.size, gif.size, gp);  // ← ganti ke gp
        }
    }

    private void drawHint(Graphics2D g2, HouseData h) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 220, 120, 200));
        String hint = "[ " + h.id + " ]  |  ESC = Keluar";
        int x = gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(hint) / 2;
        g2.drawString(hint, x, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 16);
    }

    private void drawFadeOverlay(Graphics2D g2) {
        if (fadeAlpha <= 0) return;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha / 255f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(old);
    }

    private void drawDebugWalls(Graphics2D g2, int camX, int camY, int psx, int psy) {
        g2.setColor(new Color(255, 0, 0, 120));
        for (Rectangle r : wallCollisions) {
            g2.drawRect(r.x - camX + psx, r.y - camY + psy, r.width, r.height);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // FADE SYSTEM
    // ─────────────────────────────────────────────────────────────────────
    private void startFade(boolean in, Runnable onComplete) {
        fadeIn = in;
        fadeAlpha = in ? 255 : 0;
        fading = true;
        onFadeComplete = onComplete;
    }

    private void handleFade() {
        if (!fading) return;
        int speed = 12;
        if (fadeIn) {
            fadeAlpha -= speed;
            if (fadeAlpha <= 0) { fadeAlpha = 0; fading = false; }
        } else {
            fadeAlpha += speed;
            if (fadeAlpha >= 255) {
                fadeAlpha = 255;
                fading = false;
                if (onFadeComplete != null) {
                    onFadeComplete.run();
                    onFadeComplete = null;
                    fading = true;
                    fadeIn = true;
                }
            }
        }
    }

    public boolean checkCollision(Rectangle playerBox) {
        for (Rectangle wall : wallCollisions)
            if (playerBox.intersects(wall)) return true;
        return false;
    }
    
    
    
    
    
    //domain
    public boolean isInsideAnyHouse() {
        return insideHouse;
    }
}