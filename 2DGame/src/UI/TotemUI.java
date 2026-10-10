package UI;

import Item.CapturedMob;
import Item.DeathTotem;
import Main.GamePanel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;

public class TotemUI {

    public boolean visible = false;
    private final GamePanel gp;

    private boolean detailMode = false;
    private int selectedSlot = -1;

    // >= 0 → dialog konfirmasi hapus lagi tampil untuk slot ini
    private int confirmDeleteSlot = -1;

    // posisi mouse (koordinat game) buat efek hover
    private int hoverX = -1, hoverY = -1;

    // Set true kalau mau liat koordinat klik di console (buat debug)
    private static final boolean DEBUG_CLICK = false;

    private static final int PANEL_W   = 640;
    private static final int PANEL_H   = 440;
    private static final int SLOT_SIZE = 90;
    private static final int SLOT_PAD  = 16;
    private static final int MAX_COLS  = 3;
    private static final int DEL_SIZE  = 18;   // ukuran tombol X di pojok slot

    private final Color BG_OUTER    = new Color(20, 10, 35);
    private final Color BG_INNER    = new Color(38, 22, 58);
    private final Color SUB_PANEL   = new Color(25, 14, 42);
    private final Color SLOT_BG     = new Color(48, 30, 70);
    private final Color SLOT_DARK   = new Color(15, 8, 28);
    private final Color BORDER      = new Color(160, 100, 220);
    private final Color ACCENT      = new Color(210, 150, 255);
    private final Color BAR_HP      = new Color(80, 200, 100);
    private final Color BAR_EXP     = new Color(100, 160, 255);
    private final Color BAR_BG      = new Color(18, 12, 30);
    private final Color BTN_BASE    = new Color(60, 35, 100);
    private final Color BTN_UPGRADE = new Color(130, 95, 15);
    private final Color BTN_DELETE  = new Color(120, 30, 30);

    private final Color DEL_BORDER      = new Color(220, 60, 60);
    private final Color DEL_TEXT        = new Color(255, 170, 170);
    private final Color DISABLED_FILL   = new Color(30, 20, 45);
    private final Color DISABLED_BORDER = new Color(60, 45, 75);
    private final Color DISABLED_TEXT   = new Color(90, 75, 105);

    private int panelX, panelY;

    private BufferedImage golemIcon    = null;
    private BufferedImage skeletonIcon = null;

    public TotemUI(GamePanel gp) {
        this.gp = gp;
        loadIcons();
    }

    private void loadIcons() {
        try {
            golemIcon    = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontWalk (1).png"));
            skeletonIcon = ImageIO.read(getClass().getResourceAsStream("/Aset/SkeletonFrontWalk (1).png"));
        } catch (Exception e) {
            System.out.println("[TotemUI] Gagal load icon: " + e.getMessage());
        }
    }

    public void show() {
        visible           = true;
        detailMode        = false;
        selectedSlot      = -1;
        confirmDeleteSlot = -1;
        panelX = gp.screenWidth  / 2 - PANEL_W / 2;
        panelY = gp.screenHeight / 2 - PANEL_H / 2;
    }

    public void hide() {
        visible           = false;
        detailMode        = false;
        selectedSlot      = -1;
        confirmDeleteSlot = -1;
        hoverX = -1;
        hoverY = -1;
    }

    public void update() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  LAYOUT — satu sumber untuk draw DAN klik, jadi hitbox pasti sama
    // ══════════════════════════════════════════════════════════════════════════

    private int subLeft()  { return panelX + 24; }
    private int subTop()   { return panelY + 75; }
    private int subWidth() { return PANEL_W - 48; }

    private Rectangle panelRect() {
        return new Rectangle(panelX, panelY, PANEL_W, PANEL_H);
    }

    // ── grid ──
    private Rectangle slotRect(int i) {
        int totalGridW = MAX_COLS * SLOT_SIZE + (MAX_COLS - 1) * SLOT_PAD;
        int gridStartX = subLeft() + (subWidth() - totalGridW) / 2;
        int gridStartY = subTop() + 35;
        int col = i % MAX_COLS;
        int row = i / MAX_COLS;
        return new Rectangle(
                gridStartX + col * (SLOT_SIZE + SLOT_PAD),
                gridStartY + row * (SLOT_SIZE + SLOT_PAD + 10),
                SLOT_SIZE, SLOT_SIZE);
    }

    private Rectangle slotDeleteRect(int i) {
        Rectangle s = slotRect(i);
        return new Rectangle(s.x + s.width - DEL_SIZE - 3, s.y + 3, DEL_SIZE, DEL_SIZE);
    }

    private Rectangle upgradeRect() {
        return new Rectangle(panelX + 24, panelY + PANEL_H - 58, PANEL_W - 48, 36);
    }

    // ── detail ──
    private int detailBtnY() {
        int subH = PANEL_H - 100;
        return subTop() + subH - 30 - 16;
    }

    private Rectangle detailBackRect() {
        return new Rectangle(subLeft() + subWidth() - 90 - 20, detailBtnY(), 90, 30);
    }

    private Rectangle detailDeleteRect() {
        return new Rectangle(subLeft() + subWidth() / 2 - 45, detailBtnY(), 90, 30);
    }

    private Rectangle detailSpawnRect() {
        return new Rectangle(subLeft() + 24, detailBtnY(), 100, 30);
    }

    // ── dialog konfirmasi ──
    private Rectangle confirmBoxRect() {
        int w = 340, h = 150;
        return new Rectangle(panelX + (PANEL_W - w) / 2, panelY + (PANEL_H - h) / 2, w, h);
    }

    private Rectangle confirmYesRect() {
        Rectangle b = confirmBoxRect();
        return new Rectangle(b.x + 30, b.y + b.height - 30 - 16, 110, 30);
    }

    private Rectangle confirmNoRect() {
        Rectangle b = confirmBoxRect();
        return new Rectangle(b.x + b.width - 30 - 110, b.y + b.height - 30 - 16, 110, 30);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW
    // ══════════════════════════════════════════════════════════════════════════

    public void draw(Graphics2D g2) {
        if (!visible || gp.deathTotemManager == null) return;

        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        List<CapturedMob> slots = totem.getSlots();
        int capacity = totem.getCapacity();

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(panelX + 8, panelY + 8, PANEL_W, PANEL_H);
        g2.setColor(BG_OUTER);
        g2.fillRect(panelX, panelY, PANEL_W, PANEL_H);
        g2.setColor(BG_INNER);
        g2.fillRect(panelX + 6, panelY + 6, PANEL_W - 12, PANEL_H - 12);
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(panelX + 8, panelY + 8, PANEL_W - 16, PANEL_H - 16);
        g2.setStroke(new BasicStroke(1));

        String title = "DEATH TOTEM";
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (PANEL_W - titleW) / 2;
        g2.setColor(BG_OUTER);
        g2.fillRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(BORDER);
        g2.drawRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(Color.BLACK);
        g2.drawString(title, titleX + 2, panelY + 40);
        g2.setColor(ACCENT);
        g2.drawString(title, titleX, panelY + 38);

        if (!detailMode) {
            drawGridView(g2, slots, capacity, totem);
        } else {
            if (selectedSlot >= 0 && selectedSlot < slots.size()) {
                drawDetailView(g2, slots.get(selectedSlot), totem);
            } else {
                detailMode   = false;
                selectedSlot = -1;
            }
        }

        // dialog konfirmasi selalu paling atas
        if (confirmDeleteSlot >= 0) {
            drawConfirmDialog(g2, slots);
        }
    }

    // ── GRID VIEW ─────────────────────────────────────────────────────────────

    private void drawGridView(Graphics2D g2, List<CapturedMob> slots,
                              int capacity, DeathTotem totem) {
        int subX = subLeft();
        int subY = subTop();
        int subW = subWidth();
        int subH = PANEL_H - 130;

        g2.setColor(SUB_PANEL);
        g2.fillRect(subX, subY, subW, subH);
        g2.setColor(BG_OUTER);
        g2.drawRect(subX, subY, subW, subH);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(160, 130, 200));
        g2.drawString("Souls: " + slots.size() + " / " + capacity, subX + 10, subY + 18);
        String hint = "[ESC] Close  [Click] Details  [X] Delete";
        FontMetrics fmHint = g2.getFontMetrics();
        g2.drawString(hint, subX + subW - fmHint.stringWidth(hint) - 10, subY + 18);

        for (int i = 0; i < capacity; i++) {
            Rectangle r = slotRect(i);

            boolean hasMob   = i < slots.size();
            boolean isActive = hasMob && (totem.getActivePawnSlot() == i);
            boolean hovered  = hasMob && r.contains(hoverX, hoverY);

            g2.setColor(hasMob ? (hovered ? SLOT_BG.brighter() : SLOT_BG) : SLOT_DARK);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor((isActive || hovered) ? ACCENT
                    : (hasMob ? BORDER : new Color(60, 40, 80)));
            g2.setStroke(isActive ? new BasicStroke(2) : new BasicStroke(1));
            g2.drawRect(r.x, r.y, r.width, r.height);
            g2.setStroke(new BasicStroke(1));

            if (hasMob) {
                CapturedMob mob = slots.get(i);

                BufferedImage icon = mob.type == DeathTotem.CapturedType.GOLEM
                        ? golemIcon : skeletonIcon;
                if (icon != null) {
                    g2.drawImage(icon, r.x + 10, r.y + 8, SLOT_SIZE - 20, SLOT_SIZE - 30, null);
                }

                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                g2.setColor(ACCENT);
                String label = mob.getTypeName() + " Lv." + mob.level;
                FontMetrics fmSlot = g2.getFontMetrics();
                g2.drawString(label, r.x + (SLOT_SIZE - fmSlot.stringWidth(label)) / 2,
                        r.y + SLOT_SIZE - 8);

                if (isActive) {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g2.setColor(new Color(180, 255, 180));
                    g2.drawString("ACTIVE", r.x + 22, r.y + 10);
                }

                // tombol hapus (X) di pojok kanan atas slot
                Rectangle del = slotDeleteRect(i);
                if (isActive) {
                    // pion lagi keluar → gak bisa dihapus, harus recall dulu
                    drawButton(g2, del, "X", DISABLED_FILL, DISABLED_BORDER,
                            DISABLED_TEXT, 11, false);
                } else {
                    drawButton(g2, del, "X", BTN_DELETE, DEL_BORDER, DEL_TEXT, 11,
                            del.contains(hoverX, hoverY));
                }
            } else {
                g2.setFont(new Font("Monospaced", Font.ITALIC, 10));
                g2.setColor(new Color(70, 50, 90));
                g2.drawString("empty", r.x + 27, r.y + SLOT_SIZE / 2 + 4);
            }
        }

        drawUpgradeButton(g2, totem);
    }

    // ── DETAIL VIEW ───────────────────────────────────────────────────────────

    private void drawDetailView(Graphics2D g2, CapturedMob mob, DeathTotem totem) {
        int subX = subLeft();
        int subY = subTop();
        int subW = subWidth();
        int subH = PANEL_H - 100;

        g2.setColor(SUB_PANEL);
        g2.fillRect(subX, subY, subW, subH);
        g2.setColor(BG_OUTER);
        g2.drawRect(subX, subY, subW, subH);

        int imgSize = 96;
        int imgX    = subX + 24;
        int imgY    = subY + 24;

        g2.setColor(SLOT_BG);
        g2.fillRect(imgX, imgY, imgSize, imgSize);
        g2.setColor(SLOT_DARK);
        g2.fillRect(imgX, imgY + imgSize - 4, imgSize, 4);
        g2.fillRect(imgX + imgSize - 4, imgY, 4, imgSize);
        g2.setColor(BG_OUTER);
        g2.drawRect(imgX, imgY, imgSize, imgSize);

        BufferedImage icon = mob.type == DeathTotem.CapturedType.GOLEM
                ? golemIcon : skeletonIcon;
        if (icon != null) {
            g2.drawImage(icon, imgX + 8, imgY + 8, imgSize - 16, imgSize - 16, null);
        }

        int infoX = imgX + imgSize + 24;

        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString(mob.getTypeName().toUpperCase(), infoX, imgY + 18);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(ACCENT);
        boolean isActive = (totem.getActivePawnSlot() == selectedSlot);
        g2.drawString(isActive ? "[ACTIVE]" : "[STORED]", infoX, imgY + 34);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        g2.drawString("Level   : " + mob.level + " / " + mob.getMaxLevel(), infoX, imgY + 56);
        g2.drawString("HP      : " + mob.storedHP + " / " + mob.maxHP,      infoX, imgY + 74);
        g2.drawString("ATK     : " + mob.getCurrentAttack(),                 infoX, imgY + 92);

        g2.setColor(BG_OUTER);
        g2.fillRect(subX + 20, subY + 135, subW - 40, 3);
        g2.setColor(SLOT_BG);
        g2.fillRect(subX + 20, subY + 138, subW - 40, 1);

        int barX = subX + 24;
        int barY = subY + 155;
        int barW = subW - 48;
        int barH = 14;

        int expCurrent = mob.currentExp;
        int expNeeded  = mob.expNeededForNextLevel();

        g2.setColor(BAR_BG);
        g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
        if (expNeeded > 0 && mob.level < mob.getMaxLevel()) {
            int fill = (int) ((double) Math.min(expCurrent, expNeeded) / expNeeded * barW);
            g2.setColor(BAR_EXP);
            g2.fillRoundRect(barX, barY, fill, barH, 6, 6);
        } else {
            g2.setColor(ACCENT);
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
        }
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        String expLabel = mob.level >= mob.getMaxLevel()
                ? "MAX LEVEL"
                : "EXP: " + expCurrent + " / " + expNeeded;
        FontMetrics fmBar = g2.getFontMetrics();
        g2.drawString(expLabel, barX + (barW - fmBar.stringWidth(expLabel)) / 2,
                barY + barH - 2);

        int hpBarY = barY + barH + 12;
        g2.setColor(BAR_BG);
        g2.fillRoundRect(barX, hpBarY, barW, barH, 6, 6);
        if (mob.maxHP > 0) {
            int fill = (int) ((double) mob.storedHP / mob.maxHP * barW);
            g2.setColor(BAR_HP);
            g2.fillRoundRect(barX, hpBarY, fill, barH, 6, 6);
        }
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        String hpLabel = "HP: " + mob.storedHP + " / " + mob.maxHP;
        FontMetrics fmHp = g2.getFontMetrics();
        g2.drawString(hpLabel, barX + (barW - fmHp.stringWidth(hpLabel)) / 2,
                hpBarY + barH - 2);

        int descY = hpBarY + barH + 24;
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(BORDER);
        g2.drawString("— Base Stats (Level 1) —", subX + 24, descY);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.setColor(new Color(200, 180, 230));
        g2.drawString("Base HP  : " + mob.baseMaxHP,  subX + 24, descY + 18);
        g2.drawString("Base ATK : " + mob.baseAttack, subX + 24, descY + 34);
        g2.drawString("EXP/lvl  : " + mob.expPerLevel + " (x level)", subX + 24, descY + 50);

        // ── ROW TOMBOL BAWAH ──────────────────────────────────────────────────

        // BACK (kanan)
        Rectangle back = detailBackRect();
        drawButton(g2, back, "← BACK", BTN_BASE, BG_OUTER, BORDER, 13,
                back.contains(hoverX, hoverY));

        // DELETE (tengah) — mati kalau pion ini lagi aktif
        Rectangle del = detailDeleteRect();
        if (isActive) {
            drawButton(g2, del, "DELETE", DISABLED_FILL, DISABLED_BORDER,
                    DISABLED_TEXT, 12, false);
        } else {
            drawButton(g2, del, "DELETE", BTN_DELETE, DEL_BORDER, DEL_TEXT, 12,
                    del.contains(hoverX, hoverY));
        }

        // SPAWN / RECALL (kiri)
        Rectangle spawn = detailSpawnRect();
        boolean hasActivePawn = totem.hasPawnAlive();
        if (isActive) {
            drawButton(g2, spawn, "RECALL (T)", new Color(160, 60, 60),
                    new Color(255, 120, 120), Color.WHITE, 12,
                    spawn.contains(hoverX, hoverY));
        } else if (!hasActivePawn) {
            drawButton(g2, spawn, "SPAWN", new Color(60, 35, 110), BORDER,
                    Color.WHITE, 12, spawn.contains(hoverX, hoverY));
        } else {
            drawButton(g2, spawn, "SPAWN", DISABLED_FILL, DISABLED_BORDER,
                    new Color(80, 60, 100), 12, false);
        }
    }

    // ── UPGRADE BUTTON ────────────────────────────────────────────────────────

    private void drawUpgradeButton(Graphics2D g2, DeathTotem totem) {
        int[] cost = totem.getNextUpgradeCost();
        Rectangle r = upgradeRect();

        g2.setColor(cost != null ? BTN_UPGRADE : new Color(35, 28, 18));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(cost != null ? new Color(220, 170, 50) : new Color(70, 60, 35));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        if (cost == null) {
            g2.setColor(new Color(110, 95, 55));
            String s = "TOTEM MAX CAPACITY (" + totem.getCapacity() + "/" + totem.getMaxCapacity() + ")";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, r.x + (r.width - fm.stringWidth(s)) / 2, r.y + 22);
        } else {
            g2.setColor(new Color(255, 210, 80));
            g2.drawString("UPGRADE → " + (totem.getCapacity() + 1) + " slots", r.x + 16, r.y + 14);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.setColor(new Color(200, 170, 100));
            String costStr = cost[0] + "G  " + cost[1] + " Bone  "
                    + cost[2] + " Skull  " + cost[3] + " Skel.Key";
            g2.drawString(costStr, r.x + 16, r.y + 28);
        }
    }

    // ── DIALOG KONFIRMASI HAPUS ───────────────────────────────────────────────

    private void drawConfirmDialog(Graphics2D g2, List<CapturedMob> slots) {
        if (confirmDeleteSlot < 0 || confirmDeleteSlot >= slots.size()) {
            confirmDeleteSlot = -1;
            return;
        }
        CapturedMob mob = slots.get(confirmDeleteSlot);

        // redupin panel di belakang
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(panelX, panelY, PANEL_W, PANEL_H);

        Rectangle box = confirmBoxRect();
        g2.setColor(BG_OUTER);
        g2.fillRect(box.x, box.y, box.width, box.height);
        g2.setColor(DEL_BORDER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(box.x, box.y, box.width, box.height);
        g2.setStroke(new BasicStroke(1));

        int cx = box.x + box.width / 2;

        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.setColor(DEL_TEXT);
        drawCentered(g2, "DELETE SOUL?", cx, box.y + 32);

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        drawCentered(g2, mob.getTypeName() + " Lv." + mob.level, cx, box.y + 60);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(200, 180, 230));
        drawCentered(g2, "Permanent, gak bisa dibalikin.", cx, box.y + 82);

        Rectangle yes = confirmYesRect();
        Rectangle no  = confirmNoRect();
        drawButton(g2, yes, "YES, DELETE", BTN_DELETE, DEL_BORDER, DEL_TEXT, 12,
                yes.contains(hoverX, hoverY));
        drawButton(g2, no, "CANCEL", BTN_BASE, BORDER, Color.WHITE, 12,
                no.contains(hoverX, hoverY));
    }

    // ── DRAW HELPERS ──────────────────────────────────────────────────────────

    private void drawButton(Graphics2D g2, Rectangle r, String label,
                            Color fill, Color border, Color text,
                            int fontSize, boolean hover) {
        g2.setColor(hover ? fill.brighter() : fill);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(border);
        g2.drawRect(r.x, r.y, r.width, r.height);

        g2.setFont(new Font("Monospaced", Font.BOLD, fontSize));
        FontMetrics f = g2.getFontMetrics();
        int tx = r.x + (r.width - f.stringWidth(label)) / 2;
        int ty = r.y + (r.height - f.getHeight()) / 2 + f.getAscent();
        g2.setColor(text);
        g2.drawString(label, tx, ty);
    }

    private void drawCentered(Graphics2D g2, String s, int centerX, int y) {
        FontMetrics f = g2.getFontMetrics();
        g2.drawString(s, centerX - f.stringWidth(s) / 2, y);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INPUT
    // ══════════════════════════════════════════════════════════════════════════

    // CATATAN: gp.scale = 3 itu skala TILE (16 x 3), BUKAN skala render layar.
    // Game digambar langsung di ukuran asli (screenWidth x screenHeight), jadi
    // koordinat mouse dipakai apa adanya. JANGAN dibagi gp.scale.
    private int toGameX(int rawX) { return rawX; }
    private int toGameY(int rawY) { return rawY; }

    public void handleMouseClick(MouseEvent e) {
        if (!visible) return;

        int mx = toGameX(e.getX());
        int my = toGameY(e.getY());

        if (DEBUG_CLICK) {
            System.out.println("[TotemUI] click raw=(" + e.getX() + "," + e.getY()
                    + ") game=(" + mx + "," + my + ") panel=(" + panelX + "," + panelY
                    + "," + PANEL_W + "," + PANEL_H + ")"
                    + " detail=" + detailMode + " confirm=" + confirmDeleteSlot);
        }

        if (gp.deathTotemManager == null) return;
        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        List<CapturedMob> slots = totem.getSlots();

        // dialog konfirmasi itu modal: klik di luar tombolnya diabaikan
        if (confirmDeleteSlot >= 0) {
            handleConfirmClick(mx, my, slots);
            return;
        }

        if (!panelRect().contains(mx, my)) {
            hide();
            return;
        }

        if (!detailMode) {
            handleGridClick(mx, my, slots, totem);
        } else {
            handleDetailClick(mx, my, totem);
        }
    }

    private void handleGridClick(int mx, int my, List<CapturedMob> slots, DeathTotem totem) {
        // cuma slot yang ada isinya yang bisa diklik
        for (int i = 0; i < slots.size(); i++) {
            // tombol X dicek dulu, karena posisinya di dalam area slot
            if (slotDeleteRect(i).contains(mx, my)) {
                requestDelete(totem, i);
                return;
            }
            if (slotRect(i).contains(mx, my)) {
                selectedSlot = i;
                detailMode   = true;
                return;
            }
        }

        if (upgradeRect().contains(mx, my)) {
            gp.deathTotemManager.tryUpgradeTotem();
        }
    }

    private void handleDetailClick(int mx, int my, DeathTotem totem) {
        if (detailBackRect().contains(mx, my)) {
            detailMode   = false;
            selectedSlot = -1;
            return;
        }

        if (detailDeleteRect().contains(mx, my)) {
            requestDelete(totem, selectedSlot);
            return;
        }

        if (detailSpawnRect().contains(mx, my)) {
            boolean isActive = (totem.getActivePawnSlot() == selectedSlot);
            if (isActive) {
                gp.deathTotemManager.trySpawn();
            } else if (!totem.hasPawnAlive()) {
                gp.deathTotemManager.trySpawnFromSlot(selectedSlot);
            }
        }
    }

    private void handleConfirmClick(int mx, int my, List<CapturedMob> slots) {
        if (confirmDeleteSlot >= slots.size()) {   // slot udah gak ada
            confirmDeleteSlot = -1;
            return;
        }

        if (confirmYesRect().contains(mx, my)) {
            gp.deathTotemManager.tryDeleteSlot(confirmDeleteSlot);
            confirmDeleteSlot = -1;
            detailMode        = false;
            selectedSlot      = -1;
        } else if (confirmNoRect().contains(mx, my)) {
            confirmDeleteSlot = -1;
        }
    }

    // Minta hapus slot: pion aktif ditolak (manager kasih pesan), sisanya konfirmasi dulu
    private void requestDelete(DeathTotem totem, int slot) {
        if (totem.getActivePawnSlot() == slot) {
            gp.deathTotemManager.tryDeleteSlot(slot);
            return;
        }
        confirmDeleteSlot = slot;
    }

    public void handleMouseMove(MouseEvent e) {
        hoverX = toGameX(e.getX());
        hoverY = toGameY(e.getY());
    }

    public boolean handleEsc() {
        if (!visible) return false;
        if (confirmDeleteSlot >= 0) {
            confirmDeleteSlot = -1;
        } else if (detailMode) {
            detailMode   = false;
            selectedSlot = -1;
        } else {
            hide();
        }
        return true;
    }
}