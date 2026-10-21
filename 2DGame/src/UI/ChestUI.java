package UI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.List;

import Item.Item;
import Item.ItemManager;
import Item.ItemStack;
import Main.GamePanel;
import World.StorageChest;

/**
 * Panel peti penyimpanan. Kiri = isi peti, kanan = backpack.
 *
 * Kontrol:
 *   klik kiri  : pindahkan SEMUA item di kotak itu ke sisi sebelahnya
 *   klik kanan : pindahkan 1 item
 *   scroll     : geser daftar backpack (kalau lebih dari 30 kotak)
 *   ESC / [X]  : tutup peti
 *
 * Selama panel terbuka dunia game berhenti (lihat GamePanel.update), sama kayak toko potion.
 */
public class ChestUI {

    public boolean visible = false;

    // ── ukuran ──
    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int SLOT = 46;
    private static final int GAP  = 4;
    private static final int GRID_W = COLS * SLOT + (COLS - 1) * GAP;
    private static final int GRID_H = ROWS * SLOT + (ROWS - 1) * GAP;
    private static final int MARGIN = 24;
    private static final int PANEL_W = MARGIN * 2 + GRID_W * 2 + 64;   // 64 = jarak tengah buat panah
    private static final int PANEL_H = 396;
    private static final int GRID_TOP = 66;                             // dari atas panel
    private static final int PAGE_SLOTS = COLS * ROWS;

    // ── warna (senada sama PotionShopUI / Hotbar) ──
    private static final Color COL_DIM        = new Color(0, 0, 0, 130);
    private static final Color COL_PANEL      = new Color(30, 22, 16, 245);
    private static final Color COL_BORDER     = new Color(220, 190, 150);
    private static final Color COL_ACCENT     = new Color(250, 180, 40);
    private static final Color COL_SLOT       = new Color(45, 34, 25);
    private static final Color COL_SLOT_HOVER = new Color(80, 60, 40);
    private static final Color COL_SLOT_EDGE  = new Color(95, 75, 55);
    private static final Color COL_TEXT       = new Color(255, 235, 200);
    private static final Color COL_TEXT_DIM   = new Color(190, 170, 140);
    private static final Color COL_GOOD       = new Color(130, 220, 130);
    private static final Color COL_BAD        = new Color(235, 100, 90);

    private static final Font FONT_TITLE = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_HEAD  = new Font("Monospaced", Font.BOLD, 13);
    private static final Font FONT_AMT   = new Font("Monospaced", Font.BOLD, 11);
    private static final Font FONT_SMALL = new Font("Monospaced", Font.PLAIN, 10);

    private static final int FEEDBACK_FRAMES = 100;

    private final GamePanel gp;
    private final StorageChest chest;

    private int scrollRow = 0;          // baris pertama backpack yang kelihatan

    // hover: sisi (0 = peti, 1 = backpack) + index absolut di list sisi itu
    private int hoverSide = -1;
    private int hoverIndex = -1;

    private String feedback = "";
    private boolean feedbackGood = true;
    private int feedbackTimer = 0;

    public ChestUI(GamePanel gp, StorageChest chest) {
        this.gp = gp;
        this.chest = chest;
    }

    // ===================================================================
    // BUKA / TUTUP / UPDATE
    // ===================================================================

    public void open() {
        visible = true;
        scrollRow = 0;
        hoverSide = -1;
        hoverIndex = -1;
        feedback = "";
        feedbackTimer = 0;
        gp.KeyH.escPressed = false;
        gp.KeyH.interactPressed = false;
    }

    public void close() {
        if (!visible) return;
        visible = false;
        hoverSide = -1;
        hoverIndex = -1;
        chest.close(); // animasi tutup peti
    }

    /** Dipanggil GamePanel.update() tiap frame selama panel terbuka. */
    public void update() {
        if (!visible) return;

        if (gp.KeyH.escPressed) {
            close();
            gp.KeyH.escPressed = false;
            return;
        }
        if (feedbackTimer > 0) feedbackTimer--;
    }

    // ===================================================================
    // GEOMETRI
    // ===================================================================

    private int panelX() { return (gp.screenWidth - PANEL_W) / 2; }
    private int panelY() { return (gp.screenHeight - PANEL_H) / 2 - 10; }

    private int gridX(int side) {
        return side == 0 ? panelX() + MARGIN : panelX() + PANEL_W - MARGIN - GRID_W;
    }
    private int gridY() { return panelY() + GRID_TOP; }

    /** Kotak ke-n (0..29) di grid yang kelihatan. */
    private Rectangle slotRect(int side, int n) {
        int col = n % COLS;
        int row = n / COLS;
        return new Rectangle(gridX(side) + col * (SLOT + GAP), gridY() + row * (SLOT + GAP), SLOT, SLOT);
    }

    private List<ItemStack> backpackList() { return ItemManager.inventory; }

    private int maxScrollRow() {
        int rows = (backpackList().size() + COLS - 1) / COLS;
        return Math.max(0, rows - ROWS);
    }

    /** Index absolut di list sisi tsb untuk titik mouse, atau -1. Hasil tulis ke out[0]=side. */
    private int hitTest(java.awt.Point p, int[] outSide) {
        for (int side = 0; side < 2; side++) {
            for (int n = 0; n < PAGE_SLOTS; n++) {
                if (slotRect(side, n).contains(p)) {
                    outSide[0] = side;
                    return side == 0 ? n : n + scrollRow * COLS;
                }
            }
        }
        return -1;
    }

    // ===================================================================
    // MOUSE
    // ===================================================================

    public void handleMouseMove(MouseEvent e) {
        if (!visible) return;
        int[] side = {-1};
        int idx = hitTest(e.getPoint(), side);
        hoverSide = idx >= 0 ? side[0] : -1;
        hoverIndex = idx;
    }

    public void handleMouseWheel(int rotation) {
        if (!visible) return;
        scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow + rotation));
    }

    public void handleMouseClick(MouseEvent e) {
        if (!visible) return;
        boolean left = e.getButton() == MouseEvent.BUTTON1;
        boolean right = e.getButton() == MouseEvent.BUTTON3;
        if (!left && !right) return;

        if (left && CloseButton.rect(panelX(), panelY(), PANEL_W, 12, 8).contains(e.getPoint())) {
            close();
            return;
        }

        int[] side = {-1};
        int idx = hitTest(e.getPoint(), side);
        if (idx < 0) return;

        if (side[0] == 0) takeFromChest(idx, left);
        else              storeFromBackpack(idx, left);
    }

    // ===================================================================
    // PINDAH BARANG
    // ===================================================================

    private void takeFromChest(int index, boolean all) {
        List<ItemStack> list = chest.getContents();
        if (index >= list.size()) return;
        ItemStack s = list.get(index);
        String name = s.item.name;
        int moved = chest.withdraw(index, all ? s.amount : 1);
        if (moved > 0) showFeedback("Took " + moved + "x " + name, true);
    }

    private void storeFromBackpack(int index, boolean all) {
        List<ItemStack> list = backpackList();
        if (index >= list.size()) return;
        ItemStack s = list.get(index);
        if (s == null || s.item == null) return;
        Item item = s.item;

        // item yang lagi dipakai gak boleh masuk peti (nanti player masih "pegang" item yang sudah hilang)
        if (gp.backpack != null && gp.backpack.isEquipped(item)) {
            showFeedback("Unequip " + item.name + " first.", false);
            return;
        }
        if (chest.room(item) <= 0) {
            showFeedback("The chest is full.", false);
            return;
        }

        int want = all ? s.amount : 1;
        String name = item.name;
        int moved = chest.deposit(item, want);
        if (moved <= 0) {
            showFeedback("The chest is full.", false);
        } else if (moved < want) {
            showFeedback("Stored " + moved + "x " + name + " (chest full)", true);
        } else {
            showFeedback("Stored " + moved + "x " + name, true);
        }
        scrollRow = Math.min(scrollRow, maxScrollRow()); // list backpack bisa memendek
    }

    private void showFeedback(String message, boolean good) {
        feedback = message;
        feedbackGood = good;
        feedbackTimer = FEEDBACK_FRAMES;
    }

    // ===================================================================
    // GAMBAR
    // ===================================================================

    public void draw(Graphics2D g2) {
        if (!visible) return;

        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();

        g2.setColor(COL_DIM);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        drawPanel(g2);
        drawChestGrid(g2);
        drawBackpackGrid(g2);
        drawHoverInfo(g2);
        drawFooter(g2);

        g2.setFont(oldFont);
        g2.setStroke(oldStroke);
    }

    private void drawPanel(Graphics2D g2) {
        int px = panelX();
        int py = panelY();

        g2.setColor(COL_PANEL);
        g2.fillRoundRect(px, py, PANEL_W, PANEL_H, 20, 20);
        g2.setColor(COL_BORDER);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, PANEL_W, PANEL_H, 20, 20);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(FONT_TITLE);
        g2.setColor(COL_ACCENT);
        drawCentered(g2, "Storage Chest", px + PANEL_W / 2, py + 32);

        g2.setColor(new Color(COL_BORDER.getRed(), COL_BORDER.getGreen(), COL_BORDER.getBlue(), 120));
        g2.drawLine(px + 20, py + 44, px + PANEL_W - 20, py + 44);

        // judul tiap grid
        g2.setFont(FONT_HEAD);
        g2.setColor(COL_TEXT);
        g2.drawString("Chest  " + chest.usedSlots() + "/" + StorageChest.CAPACITY, gridX(0), py + 60);
        g2.drawString("Backpack", gridX(1), py + 60);

        // panah dua arah di tengah
        int cx = px + PANEL_W / 2;
        int cy = gridY() + GRID_H / 2;
        g2.setColor(COL_BORDER);
        g2.fillPolygon(new Polygon(new int[]{cx - 14, cx - 14, cx - 26}, new int[]{cy - 24, cy - 8, cy - 16}, 3));
        g2.fillPolygon(new Polygon(new int[]{cx + 14, cx + 14, cx + 26}, new int[]{cy + 8, cy + 24, cy + 16}, 3));
        g2.drawLine(cx - 14, cy - 16, cx + 20, cy - 16);
        g2.drawLine(cx - 20, cy + 16, cx + 14, cy + 16);

        CloseButton.draw(g2, CloseButton.rect(px, py, PANEL_W, 12, 8));
    }

    private void drawChestGrid(Graphics2D g2) {
        List<ItemStack> list = chest.getContents();
        for (int n = 0; n < PAGE_SLOTS; n++) {
            ItemStack s = n < list.size() ? list.get(n) : null;
            drawSlot(g2, slotRect(0, n), s, hoverSide == 0 && hoverIndex == n, false);
        }
    }

    private void drawBackpackGrid(Graphics2D g2) {
        List<ItemStack> list = backpackList();
        for (int n = 0; n < PAGE_SLOTS; n++) {
            int abs = n + scrollRow * COLS;
            ItemStack s = abs < list.size() ? list.get(abs) : null;
            boolean equipped = s != null && s.item != null && gp.backpack != null && gp.backpack.isEquipped(s.item);
            drawSlot(g2, slotRect(1, n), s, hoverSide == 1 && hoverIndex == abs, equipped);
        }

        // scrollbar tipis kalau isi backpack lebih dari satu halaman
        int max = maxScrollRow();
        if (max > 0) {
            int trackX = gridX(1) + GRID_W + 5;
            int trackY = gridY();
            g2.setColor(COL_SLOT_EDGE);
            g2.fillRect(trackX, trackY, 4, GRID_H);
            int thumbH = Math.max(18, GRID_H * ROWS / (ROWS + max));
            int thumbY = trackY + (GRID_H - thumbH) * scrollRow / max;
            g2.setColor(COL_ACCENT);
            g2.fillRect(trackX, thumbY, 4, thumbH);
        }
    }

    private void drawSlot(Graphics2D g2, Rectangle r, ItemStack s, boolean hover, boolean equipped) {
        g2.setColor(hover && s != null ? COL_SLOT_HOVER : COL_SLOT);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g2.setColor(COL_SLOT_EDGE);
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);

        if (s == null || s.item == null) return;

        if (s.item.icon != null) {
            g2.drawImage(s.item.icon, r.x + 5, r.y + 5, r.width - 10, r.height - 10, null);
        }
        if (s.amount > 1) {
            g2.setFont(FONT_AMT);
            String t = String.valueOf(s.amount);
            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + r.width - fm.stringWidth(t) - 3;
            int ty = r.y + r.height - 4;
            g2.setColor(Color.BLACK);
            g2.drawString(t, tx + 1, ty + 1);
            g2.setColor(COL_TEXT);
            g2.drawString(t, tx, ty);
        }
        if (equipped) {
            g2.setFont(FONT_AMT);
            g2.setColor(COL_ACCENT);
            g2.drawString("E", r.x + 4, r.y + 13);
        }
    }

    /** Nama item di bawah kursor (di tengah bawah grid). */
    private void drawHoverInfo(Graphics2D g2) {
        if (hoverSide < 0 || hoverIndex < 0) return;
        List<ItemStack> list = hoverSide == 0 ? chest.getContents() : backpackList();
        if (hoverIndex >= list.size()) return;
        ItemStack s = list.get(hoverIndex);
        if (s == null || s.item == null) return;

        g2.setFont(FONT_HEAD);
        g2.setColor(COL_TEXT);
        String label = s.item.name + "  x" + s.amount;
        drawCentered(g2, label, panelX() + PANEL_W / 2, gridY() + GRID_H + 24);
    }

    private void drawFooter(Graphics2D g2) {
        int px = panelX();
        int py = panelY();
        int cx = px + PANEL_W / 2;

        if (feedbackTimer > 0 && !feedback.isEmpty()) {
            g2.setFont(FONT_HEAD);
            g2.setColor(feedbackGood ? COL_GOOD : COL_BAD);
            drawCentered(g2, feedback, cx, py + PANEL_H - 40);
        }

        g2.setFont(FONT_SMALL);
        g2.setColor(COL_TEXT_DIM);
        drawCentered(g2, "Left-click: move all    Right-click: move 1    ESC: close", cx, py + PANEL_H - 16);
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }
}
