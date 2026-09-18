package World;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Satu rumah = satu HouseData.
 * Simpan posisi di world, area pintu, spawn dalam, DAN konfigurasi interior.
 */
public class HouseData {

    // ── Identitas ─────────────────────────────────────────────────────────
    public final String id;

    // ── Posisi eksterior ──────────────────────────────────────────────────
    public final int worldX, worldY, width, height;

    // ── Area pintu (trigger zone E) ───────────────────────────────────────
    public final Rectangle doorArea;

    // ── Spawn player saat masuk ───────────────────────────────────────────
    public final int spawnX, spawnY;

    // ── Konfigurasi interior (diisi lewat builder atau langsung) ──────────
    public int floorCols    = 10;
    public int floorRows    = 8;
    public int floorTile    = 12;   // index tile[] di TileM
    public int exitWorldX   = 1100; // posisi world saat keluar
    
    public String gifBackgroundPath = null; // path ke GIF background interior

    public HouseData setGifBackground(String path) {
        this.gifBackgroundPath = path;
        return this;
    }
    
    public boolean autoWalls = true;

    /** Matikan dinding perimeter otomatis — atur collision sendiri full manual via addCollision(). */
    public HouseData disableAutoWalls() {
        this.autoWalls = false;
        return this;
    }
    
 // Collision tambahan di dalam interior (custom, sesuai layout gif/furniture dll)
    public final List<Rectangle> interiorCollisions = new ArrayList<>();

    /** Tambah area collision custom di interior (koordinat lokal interior). */
    public HouseData addCollision(int x, int y, int w, int h) {
        interiorCollisions.add(new Rectangle(x, y, w, h));
        return this;
    }

    // ── Exit Zone (opsional) ─────────────────────────────────────────────
    // Kalau di-set: player OTOMATIS keluar rumah begitu masuk area ini,
    // gak perlu pencet ESC atau tombol apapun.
    public Rectangle exitZone = null;

    // Default true = ESC tetap bisa dipake buat keluar seperti biasa.
    // Set false lewat disableEscExit() kalau exit HARUS lewat exitZone aja.
    public boolean escExitEnabled = true;

    /** Set area (koordinat lokal interior) yang bikin player auto-keluar rumah. */
    public HouseData setExitZone(int x, int y, int w, int h) {
        this.exitZone = new Rectangle(x, y, w, h);
        return this;
    }

    /** Matikan ESC sebagai cara keluar — exit cuma bisa lewat exitZone. 
     * @return */
    public HouseData disableEscExit() {
        this.escExitEnabled = false;
		return this;
    }

    public int exitWorldY   = 900;

    // Furniture: {imagePath, x, y, w, h, hasCollision}
    // imagePath pakai String, di-resolve waktu draw
    public final List<FurnitureEntry> furnitures = new ArrayList<>();

    // GIF animasi di dalam: {resourcePath, x, y, size}
    public final List<GifEntry> gifs = new ArrayList<>();

    // ── State ─────────────────────────────────────────────────────────────
    public boolean interiorLoaded = false;

    // ─────────────────────────────────────────────────────────────────────
    // CONSTRUCTOR — door auto di tengah-bawah
    // ─────────────────────────────────────────────────────────────────────
    public HouseData(String id,
                     int worldX, int worldY, int width, int height,
                     int spawnX, int spawnY) {
        this.id     = id;
        this.worldX = worldX;
        this.worldY = worldY;
        this.width  = width;
        this.height = height;
        this.spawnX = spawnX;
        this.spawnY = spawnY;

        int doorW = 48, doorH = 20;
        this.doorArea = new Rectangle(
            worldX + width / 2 - doorW / 2,
            worldY + height - doorH,
            doorW, doorH
        );
    }

    // CONSTRUCTOR — door area custom
    public HouseData(String id,
                     int worldX, int worldY, int width, int height,
                     Rectangle customDoor,
                     int spawnX, int spawnY) {
        this.id       = id;
        this.worldX   = worldX;
        this.worldY   = worldY;
        this.width    = width;
        this.height   = height;
        this.doorArea = customDoor;
        this.spawnX   = spawnX;
        this.spawnY   = spawnY;
    }

    // ─────────────────────────────────────────────────────────────────────
    // BUILDER METHODS — bisa di-chain
    // ─────────────────────────────────────────────────────────────────────

    /** Ukuran lantai interior (jumlah tile). */
    public HouseData setFloor(int cols, int rows, int tileIndex) {
        this.floorCols = cols;
        this.floorRows = rows;
        this.floorTile = tileIndex;
        return this;
    }

    /** Posisi world saat keluar rumah. */
    public HouseData setExit(int wx, int wy) {
        this.exitWorldX = wx;
        this.exitWorldY = wy;
        return this;
    }

    /** Tambah furniture/dekorasi di interior. */
    public HouseData addFurniture(String imagePath, int x, int y,
                                  int w, int h, boolean collision) {
        furnitures.add(new FurnitureEntry(imagePath, x, y, w, h, collision));
        return this;
    }

    /** Tambah GIF animasi di interior. */
    public HouseData addGif(String resourcePath, int x, int y, int size) {
        gifs.add(new GifEntry(resourcePath, x, y, size));
        return this;
    }

    // ─────────────────────────────────────────────────────────────────────
    // INNER RECORDS
    // ─────────────────────────────────────────────────────────────────────

    public static class FurnitureEntry {
        public final String imagePath;
        public final int x, y, w, h;
        public final boolean collision;
        public BufferedImage cachedImage; // di-load lazy

        public FurnitureEntry(String imagePath, int x, int y,
                              int w, int h, boolean collision) {
            this.imagePath = imagePath;
            this.x = x; this.y = y;
            this.w = w; this.h = h;
            this.collision = collision;
        }

        /** Load gambar, hasilnya di-cache. */
        public BufferedImage getImage(Class<?> caller) {
            if (cachedImage == null) {
                try {
                    cachedImage = ImageIO.read(
                        caller.getResourceAsStream(imagePath));
                } catch (Exception e) {
                    System.err.println("Furniture image not found: " + imagePath);
                }
            }
            return cachedImage;
        }
    }

    public static class GifEntry {
        public final String resourcePath;
        public final int x, y, size;
        public ImageIcon cachedGif;

        public GifEntry(String resourcePath, int x, int y, int size) {
            this.resourcePath = resourcePath;
            this.x = x; this.y = y;
            this.size = size;
        }

        /** Load GIF, hasilnya di-cache. */
        public ImageIcon getGif(Class<?> caller) {
            if (cachedGif == null) {
                try {
                    cachedGif = new ImageIcon(caller.getResource(resourcePath));
                } catch (Exception e) {
                    System.err.println("GIF not found: " + resourcePath);
                }
            }
            return cachedGif;
        }
    }
}