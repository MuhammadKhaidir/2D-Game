package World;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import Entitas.player;
import Item.Item;
import Item.ItemManager;
import Item.ItemStack;
import Main.GamePanel;

/**
 * Peti penyimpanan di dunia luar.
 *
 * - Gambar tertutup : NormalChest.png
 * - Animasi buka    : OpenChest.gif  (diputar sekali, berhenti di frame terakhir)
 * - Animasi tutup   : CloseChest.gif (diputar sekali, lalu balik ke NormalChest.png)
 *
 * Alur: player dekat peti + tekan [E] -> animasi buka -> setelah selesai ChestUI kebuka.
 * Pas ChestUI ditutup -> animasi tutup. Isi peti ikut tersimpan lewat SaveManager (F5).
 */
public class StorageChest {

    // ── posisi & aset (ubah di sini kalau perlu) ──
    /** Koordinat dunia (pojok kiri-atas sprite), sama kayak worldX/worldY objek lain. */
    public static final int WORLD_X = 800;
    public static final int WORLD_Y = 1110;

    public static final String IMG_NORMAL = "/Aset/NormalChest.png";
    public static final String GIF_OPEN   = "/Aset/OpenChest.gif";
    public static final String GIF_CLOSE  = "/Aset/CloseChest.gif";

    /** Suara buka & tutup peti (file di src/SFX). */
    public static final String SFX_CHEST  = "ChestInteraction (1).wav";
    /** Volume suara peti dalam dB (maks 6.0 = paling keras; turunin kalau kekencengan). */
    public static final float  SFX_CHEST_VOL = 6.0f;

    /** Jumlah kotak di peti. */
    public static final int CAPACITY = 30;

    // ── bayangan di dasar peti (biar kelihatan napak, gak ngambang) ──
    /** Seberapa gelap bayangannya (0..1). Pohon/batu di TileM pakai 0.35. */
    private static final float SHADOW_ALPHA  = 0.35f;
    /** Lebar bayangan, kelipatan lebar peti. 1.0 = sama lebar sama peti. */
    private static final float SHADOW_WIDTH  = 1.05f;
    /** Tinggi (ketebalan) bayangan, kelipatan lebar peti. */
    private static final float SHADOW_HEIGHT = 0.30f;
    /** Seberapa banyak bayangan 'masuk' ke bawah peti (0 = semua kelihatan di bawah, 1 = semua ketutup). */
    private static final float SHADOW_TUCK   = 0.60f;

    public enum State { CLOSED, OPENING, OPEN, CLOSING }

    private final GamePanel gp;

    private BufferedImage normal;      // NormalChest.png (bisa null kalau file gak ada)
    private GifFrames openAnim;        // bisa null
    private GifFrames closeAnim;       // bisa null

    /** Ukuran gambar peti di dunia (lebar = 1 tile, tinggi ngikutin rasio NormalChest.png). */
    private final int drawW, drawH;

    private State state = State.CLOSED;
    private int frameIndex = 0;
    private int frameTick = 0;
    private boolean uiPending = false;   // true = begitu animasi buka selesai, UI harus muncul
    private boolean openedEvent = false; // dibaca GamePanel lewat consumeOpenedEvent()

    /** Isi peti. Index list = nomor kotak. */
    private final List<ItemStack> contents = new ArrayList<>();

    public StorageChest(GamePanel gp) {
        this.gp = gp;

        normal    = loadImage(IMG_NORMAL);
        openAnim  = GifFrames.load(getClass(), GIF_OPEN,  gp.FPS);
        closeAnim = GifFrames.load(getClass(), GIF_CLOSE, gp.FPS);

        drawW = 30*2;
        if (normal != null && normal.getWidth() > 0) {
            drawH = Math.max(1, Math.round(drawW * (float) normal.getHeight() / normal.getWidth()));
        } else {
            drawH = 30 *2;
        }

        // Peti padat: bagian bawah sprite jadi penghalang (player gak bisa tembus).
        gp.objCol.add(WORLD_X + 2, WORLD_Y + drawH / 2, drawW - 4, drawH - drawH / 2);
    }

    private BufferedImage loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("[StorageChest] WARNING: " + path + " tidak ditemukan.");
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("[StorageChest] Gagal membaca " + path + ": " + e);
            return null;
        }
    }

    // ===================================================================
    // POSISI & INTERAKSI
    // ===================================================================

    public Rectangle getInteractArea() {
        return new Rectangle(WORLD_X - 12, WORLD_Y - 12, drawW + 24, drawH + 24);
    }

    /** Dipakai buat Y-sort (urutan gambar terhadap player/NPC/monster). */
    public int getSortY() {
        return WORLD_Y + drawH;
    }

    public boolean isPlayerNear(player p) {
        return p != null && p.getInteractArea().intersects(getInteractArea());
    }

    public State getState() { return state; }

    /**
     * Player menekan [E]. Return true kalau interaksi diterima (peti mulai membuka),
     * artinya tombol E sebaiknya dianggap sudah dipakai.
     */
    public boolean tryOpen(player p) {
        if (!isPlayerNear(p)) return false;
        if (state != State.CLOSED && state != State.CLOSING) return false;

        state = State.OPENING;
        gp.playSE(SFX_CHEST, SFX_CHEST_VOL); // suara buka
        frameIndex = 0;
        frameTick = 0;
        uiPending = true;
        if (openAnim == null) finishOpening(); // gak ada GIF -> langsung terbuka
        return true;
    }

    /** Dipanggil ChestUI pas ditutup: mulai animasi tutup. */
    public void close() {
        uiPending = false;
        if (state == State.CLOSED || state == State.CLOSING) return;

        state = State.CLOSING;
        gp.playSE(SFX_CHEST, SFX_CHEST_VOL); // suara tutup
        frameIndex = 0;
        frameTick = 0;
        if (closeAnim == null) state = State.CLOSED; // gak ada GIF -> langsung tertutup
    }

    /** True sekali saja, tepat saat peti selesai terbuka dan UI harus ditampilkan. */
    public boolean consumeOpenedEvent() {
        boolean e = openedEvent;
        openedEvent = false;
        return e;
    }

    // ===================================================================
    // UPDATE & DRAW
    // ===================================================================

    /** Dipanggil GamePanel.update() tiap frame. */
    public void update() {
        if (state == State.OPENING) {
            if (advance(openAnim)) finishOpening();
        } else if (state == State.CLOSING) {
            if (advance(closeAnim)) state = State.CLOSED;
        }
    }

    /** Maju satu tick. Return true kalau animasi sudah lewat frame terakhir. */
    private boolean advance(GifFrames anim) {
        if (anim == null) return true;
        // Frame terakhir GIF punya delay 1 detik (pose "ditahan"). Gak usah ditunggu:
        // begitu frame terakhir muncul, animasi dianggap selesai.
        if (frameIndex >= anim.size() - 1) return true;
        frameTick++;
        if (frameTick >= anim.delayTicks[frameIndex]) {
            frameTick = 0;
            frameIndex++;
            if (frameIndex >= anim.size() - 1) return true; // sampai frame terakhir -> langsung selesai
        }
        return false;
    }

    private void finishOpening() {
        state = State.OPEN;
        if (uiPending) {
            uiPending = false;
            if (isPlayerNear(gp.player)) {
                openedEvent = true;   // UI boleh muncul
            } else {
                close();              // player keburu pergi -> tutup lagi
            }
        }
    }

    /** Gambar peti. Dipanggil dari TileM.drawYSorted (di dalam transform zoom dunia). */
    public void draw(Graphics2D g2, int camX, int camY) {
        int sx = WORLD_X - camX + gp.player.screenX;
        int sy = WORLD_Y - camY + gp.player.screenY;

        if (sx + drawW < gp.viewMinX() || sx > gp.viewMaxX()) return;
        if (sy + drawH < gp.viewMinY() || sy > gp.viewMaxY()) return;

        drawShadow(g2, sx, sy); // SEBELUM peti, supaya peti nutup bagian atas bayangan

        BufferedImage img = currentImage();
        if (img != null) {
            // lebar pas 1 tile, rata bawah (kalau GIF beda rasio sama PNG, gak gepeng)
            int h = Math.max(1, Math.round(drawW * (float) img.getHeight() / img.getWidth()));
            g2.drawImage(img, sx, sy + drawH - h, drawW, h, null);
        } else {
            drawPlaceholder(g2, sx, sy);
        }

        if (state == State.CLOSED && !(gp.chestUI != null && gp.chestUI.visible) && isPlayerNear(gp.player)) {
            drawPrompt(g2, sx + drawW / 2, sy - 6);
        }
    }

    private BufferedImage currentImage() {
        switch (state) {
            case OPENING:
            case OPEN:
                if (openAnim != null) return openAnim.frames[Math.min(frameIndex, openAnim.size() - 1)];
                break;
            case CLOSING:
                if (closeAnim != null) return closeAnim.frames[Math.min(frameIndex, closeAnim.size() - 1)];
                break;
            default:
                break;
        }
        return normal;
    }

    /** Bayangan elips di dasar peti. Gayanya sama kayak bayangan pohon/batu di TileM.drawYSorted. */
    private void drawShadow(Graphics2D g2, int sx, int sy) {
        int w = Math.round(drawW * SHADOW_WIDTH);
        int h = Math.max(4, Math.round(drawW * SHADOW_HEIGHT));
        int x = sx + (drawW - w) / 2;                          // rata tengah terhadap peti
        int y = sy + drawH - Math.round(h * SHADOW_TUCK);      // sebagian masuk ke bawah peti

        java.awt.Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SHADOW_ALPHA));
        g2.setColor(Color.BLACK);
        g2.fillOval(x, y, w, h);
        g2.setComposite(oc);
    }

    /** Kalau NormalChest.png hilang, peti tetap kelihatan & bisa dipakai. */
    private void drawPlaceholder(Graphics2D g2, int sx, int sy) {
        g2.setColor(new Color(120, 78, 40));
        g2.fillRect(sx, sy, drawW, drawH);
        g2.setColor(new Color(60, 38, 18));
        g2.drawRect(sx, sy, drawW - 1, drawH - 1);
        g2.setColor(new Color(250, 180, 40));
        g2.fillRect(sx + drawW / 2 - 4, sy + drawH / 2 - 4, 8, 8);
    }

    private void drawPrompt(Graphics2D g2, int centerX, int bottomY) {
        Font old = g2.getFont();
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String text = "[E] Open";
        int w = fm.stringWidth(text) + 12;
        int h = fm.getHeight() + 2;
        int x = centerX - w / 2;
        int y = bottomY - h;

        java.awt.Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(new Color(30, 22, 16));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setComposite(oc);
        g2.setColor(new Color(220, 190, 150));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(255, 235, 200));
        g2.drawString(text, x + 6, y + h - 5);
        g2.setFont(old);
    }

    // ===================================================================
    // ISI PETI
    // ===================================================================

    /** Daftar isi (read-only buat UI & save). Jangan dimodifikasi langsung dari luar. */
    public List<ItemStack> getContents() {
        return contents;
    }

    /** Kosongkan peti (dipakai saat load save). */
    public void clearContents() {
        contents.clear();
    }

    /** Masukkan item langsung ke peti tanpa nyentuh backpack (dipakai saat load save). Return jumlah yang muat. */
    public int restore(Item item, int amount) {
        int fit = Math.min(amount, room(item));
        if (fit > 0) addToChest(item, fit);
        return fit;
    }

    public int usedSlots() {
        return contents.size();
    }

    /** Berapa banyak item ini yang masih muat di peti (isi kotak yang sama dulu, baru kotak kosong). */
    public int room(Item item) {
        if (item == null) return 0;
        int max = Math.max(1, item.maxStack);
        int room = 0;
        for (ItemStack s : contents) {
            if (s.isSame(item)) room += Math.max(0, max - s.amount);
        }
        room += (CAPACITY - contents.size()) * max;
        return room;
    }

    /**
     * Pindahkan item dari backpack ke peti.
     * @return jumlah yang benar-benar dipindah (0 kalau peti penuh)
     */
    public int deposit(Item item, int amount) {
        if (item == null || amount <= 0) return 0;
        int fit = Math.min(amount, room(item));
        if (fit <= 0) return 0;

        ItemManager im = ItemManager.get();
        fit = Math.min(fit, im.count(item));            // jangan lebih dari yang dimiliki
        if (fit <= 0 || !im.remove(item, fit)) return 0;

        addToChest(item, fit);
        return fit;
    }

    /**
     * Pindahkan item dari kotak peti ke backpack.
     * @return jumlah yang dipindah (0 kalau kotak kosong / index salah)
     */
    public int withdraw(int slotIndex, int amount) {
        if (slotIndex < 0 || slotIndex >= contents.size() || amount <= 0) return 0;
        ItemStack stack = contents.get(slotIndex);
        int n = Math.min(amount, stack.amount);
        if (n <= 0) return 0;

        ItemManager.get().add(stack.item, n);
        stack.amount -= n;
        if (stack.amount <= 0) contents.remove(slotIndex);
        return n;
    }

    private void addToChest(Item item, int amount) {
        int max = Math.max(1, item.maxStack);
        int left = amount;
        for (ItemStack s : contents) {                   // 1. genapkan kotak yang sudah ada
            if (left <= 0) break;
            if (!s.isSame(item) || s.amount >= max) continue;
            int add = Math.min(max - s.amount, left);
            s.amount += add;
            left -= add;
        }
        while (left > 0 && contents.size() < CAPACITY) { // 2. sisanya ke kotak baru
            int add = Math.min(max, left);
            contents.add(new ItemStack(item, add));
            left -= add;
        }
    }
}
