package UI;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import Item.ItemManager;
import Item.ItemStack;
import Main.GamePanel;

/**
 * HOTBAR — 5 slot akses cepat di bawah layar (kayak game MMORPG).
 *
 * Cara pakai:
 *  - Tekan angka 1..5 (atau klik slotnya) → item di slot itu langsung dipakai.
 *      * consumable (potion)         → dipakai, jumlah berkurang 1
 *      * equipment (sword, axe, helm)→ pasang / lepas (sama kayak klik kanan di backpack)
 *      * Death Totem                 → sama kayak tombol T
 *  - Masukin item: seret item dari backpack ke slot hotbar, atau pilih item di backpack lalu
 *    klik tombol angka "ADD TO HOTBAR" di panel detail.
 *  - Seret item antar slot hotbar buat tukar posisi. Seret keluar dari hotbar / klik kanan = kosongin.
 *
 * Slot nyimpen JENIS item (bukan stack-nya), jadi kalau potion habis lalu dapet lagi,
 * slotnya otomatis aktif lagi. Angka di slot = total item itu di backpack.
 */
public class Hotbar {

    public static final int SLOT_COUNT = 5;
    public static final int SLOT_SIZE  = 44;
    private static final int GAP           = 6;
    private static final int BOTTOM_MARGIN = 6;

    /** Tinggi area bawah layar yang dipakai hotbar. Teks hint di bawah layar digeser naik sebesar ini. */
    public static final int RESERVED_HEIGHT = SLOT_SIZE + BOTTOM_MARGIN + 6;

    private static final int DRAG_THRESHOLD = 6; // px, biar klik biasa gak kebaca sebagai seret

    private static final Font FONT_NUM   = new Font("Monospaced", Font.BOLD, 11);
    private static final Font FONT_COUNT = new Font("Monospaced", Font.BOLD, 12);
    private static final Font FONT_TIP   = new Font("Monospaced", Font.BOLD, 11);

    private static final Color COL_SLOT      = new Color(60, 45, 35);
    private static final Color COL_SLOT_DARK = new Color(25, 20, 15);
    private static final Color COL_BORDER    = new Color(220, 190, 150);
    private static final Color COL_ACCENT    = new Color(250, 180, 40);

    private final GamePanel gp;
    private final Item.Item[] slots = new Item.Item[SLOT_COUNT];

    // ── state mouse ──
    private int hoverIndex = -1;
    private boolean active = false;      // lagi nahan klik kiri di item (hotbar / backpack)
    private boolean dragging = false;    // udah geser lebih dari DRAG_THRESHOLD
    private Item.Item dragItem = null;
    private int dragFromSlot = -1;       // index slot hotbar asal, -1 kalau dari backpack
    private int pressX, pressY, mouseX, mouseY;

    // ── efek kedip pas slot dipakai ──
    private int flashSlot = -1;
    private int flashTimer = 0;

    public Hotbar(GamePanel gp) {
        this.gp = gp;
    }

    // ===================================================================
    // DATA SLOT
    // ===================================================================

    public Item.Item getSlot(int i) {
        return (i >= 0 && i < SLOT_COUNT) ? slots[i] : null;
    }

    /** Set slot langsung (dipakai buat load save). Gak ngecek duplikat. */
    public void setSlot(int i, Item.Item item) {
        if (i >= 0 && i < SLOT_COUNT) slots[i] = item;
    }

    public void clear() {
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = null;
    }

    public int indexOf(Item.Item item) {
        if (item == null) return -1;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slots[i] != null && same(slots[i], item)) return i;
        }
        return -1;
    }

    /** Masukin item ke slot. Satu jenis item cuma boleh ada di satu slot (kalau udah ada, dipindah). */
    public void assign(int slot, Item.Item item) {
        if (slot < 0 || slot >= SLOT_COUNT || item == null) return;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i != slot && slots[i] != null && same(slots[i], item)) slots[i] = null;
        }
        slots[slot] = item;
    }

    public void clearSlot(int slot) {
        if (slot >= 0 && slot < SLOT_COUNT) slots[slot] = null;
    }

    private void swapSlots(int a, int b) {
        if (a == b) return;
        Item.Item tmp = slots[a];
        slots[a] = slots[b];
        slots[b] = tmp;
    }

    private static boolean same(Item.Item a, Item.Item b) {
        return a == b || (a != null && b != null && a.name != null && a.name.equals(b.name));
    }

    // ── baca isi backpack ──

    private int countOf(Item.Item item) {
        int total = 0;
        for (ItemStack s : ItemManager.inventory) {
            if (s != null && s.item != null && same(s.item, item)) total += s.amount;
        }
        return total;
    }

    private ItemStack findStack(Item.Item item) {
        for (ItemStack s : ItemManager.inventory) {
            if (s != null && s.item != null && s.amount > 0 && same(s.item, item)) return s;
        }
        return null;
    }

    // ===================================================================
    // PAKAI ITEM
    // ===================================================================

    /** Boleh pakai hotbar sekarang? (gak boleh pas dialog, menu, mati, shop, dll.) */
    public boolean canUse() {
        if (gp.mainMenu != null && gp.mainMenu.active) return false;
        if (gp.dialogOn || gp.inspectOn || gp.chatMode) return false;
        if (gp.player == null || gp.player.isDead) return false;
        if (gp.deathScreenUI != null && gp.deathScreenUI.visible) return false;
        if (gp.totemUI != null && gp.totemUI.visible) return false;
        if (gp.potionShop != null && gp.potionShop.visible) return false;
        if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null) {
            Entitas.DarkMerchant dm = gp.domainManager.darkMerchant;
            if (dm != null && (dm.isShopOpen() || dm.isInDialog())) return false;
            if (gp.domainManager.isBossConfirmOpen()) return false;
        }
        return true;
    }

    /** Dipanggil KeyHandler pas player tekan angka (index 0..4 = tombol 1..5). */
    public void onNumberKey(int index) {
        if (index < 0 || index >= SLOT_COUNT) return;
        if (!canUse()) return;
        use(index);
    }

    public void use(int index) {
        Item.Item item = getSlot(index);
        if (item == null) return;

        flashSlot = index;
        flashTimer = 8;

        ItemStack stack = findStack(item);
        if (stack == null) {
            say("No " + item.name + " left.");
            return;
        }

        // 1. Consumable (Health Potion, dll.)
        if (item.onUse != null) {
            if (item.onUse.use(gp)) {
                stack.amount--;
                if (stack.amount <= 0) ItemManager.inventory.remove(stack);
            }
            return;
        }

        // 2. Equipment → pasang / lepas
        if (gp.backpack != null && gp.backpack.getEquipSlotFor(item) != -1) {
            gp.backpack.toggleEquip(stack, gp);
            say((gp.backpack.isEquipped(item) ? "Equipped " : "Unequipped ") + item.name);
            return;
        }

        // 3. Death Totem → sama kayak tombol T
        if (same(item, ItemManager.get().DEATH_TOTEM) && gp.deathTotemManager != null) {
            gp.deathTotemManager.trySpawn();
            return;
        }

        say("Can't use " + item.name + ".");
    }

    private void say(String msg) {
        if (gp.speechBubble != null) gp.speechBubble.show(msg);
    }

    public void update() {
        if (flashTimer > 0) flashTimer--;
    }

    // ===================================================================
    // GEOMETRI
    // ===================================================================

    private int totalWidth() { return SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * GAP; }
    private int originX()    { return (gp.screenWidth - totalWidth()) / 2; }
    private int originY()    { return gp.screenHeight - BOTTOM_MARGIN - SLOT_SIZE; }

    public Rectangle slotRect(int i) {
        return new Rectangle(originX() + i * (SLOT_SIZE + GAP), originY(), SLOT_SIZE, SLOT_SIZE);
    }

    /** Area seluruh hotbar (termasuk plat di belakang slot). */
    public Rectangle getBounds() {
        return new Rectangle(originX() - 8, originY() - 5, totalWidth() + 16, SLOT_SIZE + 10);
    }

    public int slotAt(int mx, int my) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (slotRect(i).contains(mx, my)) return i;
        }
        return -1;
    }

    // ===================================================================
    // MOUSE
    // ===================================================================

    /** Panel penuh (stat / mob index) lagi nutup hotbar → klik jangan tembus ke hotbar. */
    private boolean coveredByPanel() {
        return (gp.statUI != null && gp.statUI.visible)
            || (gp.mobIndexUI != null && gp.mobIndexUI.visible);
    }

    /** True kalau lagi nyeret item (udah lewat threshold geser). Dipakai BackpackUI buat bedain klik biasa vs seret. */
    public boolean isDragging() { return dragging; }

    /** Dipanggil BackpackUI pas player mulai klik item di grid inventory (calon drag). */
    public void beginDrag(Item.Item item, int mx, int my) {
        if (item == null) return;
        active = true;
        dragging = false;
        dragItem = item;
        dragFromSlot = -1;
        pressX = mouseX = mx;
        pressY = mouseY = my;
    }

    /** @return true kalau klik ini kena hotbar dan sudah ditangani (jadi GamePanel jangan lanjut / jangan nonjok). */
    public boolean handleMousePressed(MouseEvent e) {
        if (coveredByPanel()) return false;
        int mx = e.getX(), my = e.getY();
        if (!getBounds().contains(mx, my)) return false;
        if (!canUse()) return true; // lagi dialog dsb: klik ditelan aja

        int idx = slotAt(mx, my);
        if (e.getButton() == MouseEvent.BUTTON3) {
            if (idx >= 0) slots[idx] = null;            // klik kanan = kosongin slot
        } else if (e.getButton() == MouseEvent.BUTTON1 && idx >= 0 && slots[idx] != null) {
            active = true;
            dragging = false;
            dragItem = slots[idx];
            dragFromSlot = idx;
            pressX = mouseX = mx;
            pressY = mouseY = my;
        }
        return true;
    }

    public void handleMouseDragged(MouseEvent e) {
        if (!active) return;
        mouseX = e.getX();
        mouseY = e.getY();
        hoverIndex = slotAt(mouseX, mouseY);
        if (!dragging) {
            int dx = mouseX - pressX, dy = mouseY - pressY;
            if (dx * dx + dy * dy >= DRAG_THRESHOLD * DRAG_THRESHOLD) dragging = true;
        }
    }

    public void handleMouseReleased(MouseEvent e) {
        if (!active) return;
        int idx = slotAt(e.getX(), e.getY());

        if (dragging) {
            if (idx >= 0) {
                if (dragFromSlot >= 0) swapSlots(dragFromSlot, idx);   // geser antar slot hotbar = tukar
                else assign(idx, dragItem);                            // dari backpack = masukin
            } else if (dragFromSlot >= 0) {
                slots[dragFromSlot] = null;                            // dilepas di luar hotbar = dibuang dari hotbar
            }
        } else if (dragFromSlot >= 0 && idx == dragFromSlot && e.getButton() == MouseEvent.BUTTON1) {
            if (canUse()) use(dragFromSlot);                           // klik biasa = pakai
        }

        active = false;
        dragging = false;
        dragItem = null;
        dragFromSlot = -1;
    }

    public void handleMouseMoved(MouseEvent e) {
        hoverIndex = coveredByPanel() ? -1 : slotAt(e.getX(), e.getY());
    }

    // ===================================================================
    // GAMBAR
    // ===================================================================

    /** Gambar hotbar (dipanggil sebelum dialog box, biar dialog nutupin hotbar). */
    public void draw(Graphics2D g2) {
        Rectangle b = getBounds();

        // plat di belakang slot
        g2.setColor(new Color(20, 14, 10, 150));
        g2.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
        g2.setColor(new Color(220, 190, 150, 90));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(b.x, b.y, b.width, b.height, 10, 10);

        for (int i = 0; i < SLOT_COUNT; i++) drawSlot(g2, i);
    }

    private void drawSlot(Graphics2D g2, int i) {
        Rectangle r = slotRect(i);
        Item.Item item = slots[i];
        int count = (item != null) ? countOf(item) : 0;
        boolean equipped = item != null && gp.backpack != null && gp.backpack.isEquipped(item);
        boolean hover = (i == hoverIndex);
        boolean beingDragged = dragging && dragFromSlot == i;

        // dasar slot (gaya sama kayak slot backpack)
        g2.setColor(COL_SLOT);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(COL_SLOT_DARK);
        g2.fillRect(r.x, r.y + r.height - 3, r.width, 3);
        g2.fillRect(r.x + r.width - 3, r.y, 3, r.height);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRect(r.x, r.y, r.width, 3);
        g2.fillRect(r.x, r.y, 3, r.height);

        // ikon
        if (item != null && item.icon != null) {
            int size = SLOT_SIZE - 12;
            if ("Soul of Skeleton".equalsIgnoreCase(item.name)) size = SLOT_SIZE - 6; // ikon ini emang dibesarin di backpack
            Composite old = g2.getComposite();
            if (count <= 0 || beingDragged) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
            }
            g2.drawImage(item.icon, r.x + (SLOT_SIZE - size) / 2, r.y + (SLOT_SIZE - size) / 2, size, size, null);
            g2.setComposite(old);
        }

        // kedip pas dipakai
        if (flashTimer > 0 && flashSlot == i) {
            g2.setColor(new Color(255, 255, 255, Math.min(255, 30 + flashTimer * 20)));
            g2.fillRect(r.x, r.y, r.width, r.height);
        }

        // border: gold kalau lagi di-equip / jadi target drop, terang kalau di-hover
        if (equipped || (dragging && hover)) {
            g2.setColor(COL_ACCENT);
            g2.setStroke(new BasicStroke(2));
        } else if (hover) {
            g2.setColor(COL_BORDER);
            g2.setStroke(new BasicStroke(1));
        } else {
            g2.setColor(new Color(20, 15, 10));
            g2.setStroke(new BasicStroke(1));
        }
        g2.drawRect(r.x, r.y, r.width, r.height);
        g2.setStroke(new BasicStroke(1));

        // nomor slot (kiri atas)
        g2.setFont(FONT_NUM);
        g2.setColor(Color.BLACK);
        g2.drawString(String.valueOf(i + 1), r.x + 5, r.y + 14);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString(String.valueOf(i + 1), r.x + 4, r.y + 13);

        // jumlah (kanan bawah): tampil kalau > 1, atau 0 (merah) kalau habis
        if (item != null && (count > 1 || count <= 0)) {
            String amt = String.valueOf(count);
            g2.setFont(FONT_COUNT);
            FontMetrics fm = g2.getFontMetrics();
            int tx = r.x + r.width - fm.stringWidth(amt) - 4;
            int ty = r.y + r.height - 5;
            g2.setColor(Color.BLACK);
            g2.drawString(amt, tx + 1, ty + 1);
            g2.setColor(count <= 0 ? new Color(255, 90, 80) : Color.WHITE);
            g2.drawString(amt, tx, ty);
        }

        // huruf E (kanan atas) kalau lagi dipakai/equipped
        if (equipped) {
            g2.setFont(FONT_NUM);
            g2.setColor(Color.BLACK);
            g2.drawString("E", r.x + r.width - 11, r.y + 14);
            g2.setColor(COL_ACCENT);
            g2.drawString("E", r.x + r.width - 12, r.y + 13);
        }
    }

    /**
     * Gambar lapisan paling atas: tooltip nama item + ikon yang lagi diseret.
     * Dipanggil paling akhir di paintComponent supaya gak ketutup panel UI lain.
     */
    public void drawOverlay(Graphics2D g2) {
        // tooltip
        if (!dragging && hoverIndex >= 0 && slots[hoverIndex] != null && !coveredByPanel()) {
            Item.Item item = slots[hoverIndex];
            int count = countOf(item);
            String text = item.name + (count > 0 ? "  x" + count : "  (none)");
            g2.setFont(FONT_TIP);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 16;
            int h = 22;
            Rectangle r = slotRect(hoverIndex);
            int x = Math.max(4, Math.min(gp.screenWidth - w - 4, r.x + r.width / 2 - w / 2));
            int y = r.y - h - 10;
            g2.setColor(new Color(20, 14, 10, 235));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(COL_BORDER);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x, y, w, h, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(text, x + 8, y + 15);
        }

        // ikon yang lagi diseret (ngikutin kursor)
        if (dragging && dragItem != null && dragItem.icon != null) {
            int size = SLOT_SIZE - 8;
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g2.drawImage(dragItem.icon, mouseX - size / 2, mouseY - size / 2, size, size, null);
            g2.setComposite(old);
        }
    }
}
