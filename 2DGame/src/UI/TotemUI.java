package UI;

import Item.CapturedMob;
import Item.DeathTotem;
import Item.ItemManager;
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

    private static final int PANEL_W = 640;
    private static final int PANEL_H = 440;

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
    private final Color BTN_DELETE  = new Color(120, 30, 30);   // ← baru

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
        visible      = true;
        detailMode   = false;
        selectedSlot = -1;
        panelX = gp.screenWidth  / 2 - PANEL_W / 2;
        panelY = gp.screenHeight / 2 - PANEL_H / 2;
    }

    public void hide() {
        visible      = false;
        detailMode   = false;
        selectedSlot = -1;
    }

    public void update() {}

    // ── DRAW ──────────────────────────────────────────────────────────────────

    public void draw(Graphics2D g2) {
        if (!visible || gp.deathTotemManager == null) return;

        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;  // ← null guard

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
                detailMode = false;
            }
        }
    }

    // ── GRID VIEW ─────────────────────────────────────────────────────────────

    private void drawGridView(Graphics2D g2, List<CapturedMob> slots,
                               int capacity, DeathTotem totem) {
        int subX = panelX + 24;
        int subY = panelY + 75;
        int subW = PANEL_W - 48;
        int subH = PANEL_H - 130;

        g2.setColor(SUB_PANEL);
        g2.fillRect(subX, subY, subW, subH);
        g2.setColor(BG_OUTER);
        g2.drawRect(subX, subY, subW, subH);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(160, 130, 200));
        g2.drawString("Souls: " + slots.size() + " / " + capacity,
                subX + 10, subY + 18);
        g2.drawString("[ESC] Close   [Click mob] Details", subX + subW - 230, subY + 18);

        int slotSize  = 90;
        int slotPad   = 16;
        int maxCols   = 3;
        int totalGridW = maxCols * slotSize + (maxCols - 1) * slotPad;
        int gridStartX = subX + (subW - totalGridW) / 2;
        int gridStartY = subY + 35;

        for (int i = 0; i < capacity; i++) {
            int col = i % maxCols;
            int row = i / maxCols;
            int sx  = gridStartX + col * (slotSize + slotPad);
            int sy  = gridStartY + row * (slotSize + slotPad + 10);

            boolean hasMob   = i < slots.size();
            boolean isActive = hasMob && (totem.getActivePawnSlot() == i);

            g2.setColor(hasMob ? SLOT_BG : SLOT_DARK);
            g2.fillRect(sx, sy, slotSize, slotSize);
            g2.setColor(isActive ? ACCENT : (hasMob ? BORDER : new Color(60, 40, 80)));
            g2.setStroke(isActive ? new BasicStroke(2) : new BasicStroke(1));
            g2.drawRect(sx, sy, slotSize, slotSize);
            g2.setStroke(new BasicStroke(1));

            if (hasMob) {
                CapturedMob mob = slots.get(i);

                BufferedImage icon = mob.type == DeathTotem.CapturedType.GOLEM
                        ? golemIcon : skeletonIcon;
                if (icon != null) {
                    g2.drawImage(icon, sx + 10, sy + 8, slotSize - 20, slotSize - 30, null);
                }

                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                g2.setColor(ACCENT);
                String label = mob.getTypeName() + " Lv." + mob.level;
                FontMetrics fmSlot = g2.getFontMetrics();
                g2.drawString(label, sx + (slotSize - fmSlot.stringWidth(label)) / 2,
                        sy + slotSize - 8);

                if (isActive) {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g2.setColor(new Color(180, 255, 180));
                    g2.drawString("ACTIVE", sx + 22, sy + 10);
                }
            } else {
                g2.setFont(new Font("Monospaced", Font.ITALIC, 10));
                g2.setColor(new Color(70, 50, 90));
                g2.drawString("empty", sx + 27, sy + slotSize / 2 + 4);
            }
        }

        drawUpgradeButton(g2, totem);
    }

    // ── DETAIL VIEW ───────────────────────────────────────────────────────────

    private void drawDetailView(Graphics2D g2, CapturedMob mob, DeathTotem totem) {
        int subX = panelX + 24;
        int subY = panelY + 75;
        int subW = PANEL_W - 48;
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
            int fill = (int)((double) Math.min(expCurrent, expNeeded) / expNeeded * barW);
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
            int fill = (int)((double) mob.storedHP / mob.maxHP * barW);
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
        g2.drawString("Base HP  : " + mob.baseMaxHP,    subX + 24, descY + 18);
        g2.drawString("Base ATK : " + mob.baseAttack,   subX + 24, descY + 34);
        g2.drawString("EXP/lvl  : " + mob.expPerLevel + " (x level)", subX + 24, descY + 50);

        // ── ROW TOMBOL BAWAH ──────────────────────────────────────────────────
        int btnH2    = 30;
        int btnY2    = subY + subH - btnH2 - 16;

        // BACK button (kanan)
        int btnW2    = 90;
        int btnX2    = subX + subW - btnW2 - 20;
        g2.setColor(BTN_BASE);
        g2.fillRect(btnX2, btnY2, btnW2, btnH2);
        g2.setColor(BG_OUTER);
        g2.drawRect(btnX2, btnY2, btnW2, btnH2);
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(BORDER);
        g2.drawString("← BACK", btnX2 + 12, btnY2 + 19);

        // DELETE button (tengah)
        int delBtnW  = 90;
        int delBtnX  = subX + subW / 2 - delBtnW / 2;
        g2.setColor(BTN_DELETE);
        g2.fillRect(delBtnX, btnY2, delBtnW, btnH2);
        g2.setColor(new Color(220, 60, 60));
        g2.drawRect(delBtnX, btnY2, delBtnW, btnH2);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(new Color(255, 160, 160));
        g2.drawString("DELETE", delBtnX + 16, btnY2 + 19);

        // SPAWN / RECALL button (kiri)
        boolean hasActivePawn = totem.hasPawnAlive();
        int spawnBtnW  = 100;
        int spawnBtnX  = subX + 24;

        if (isActive) {
            g2.setColor(new Color(160, 60, 60));
            g2.fillRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setColor(new Color(255, 120, 120));
            g2.drawRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            g2.drawString("RECALL (T)", spawnBtnX + 8, btnY2 + 19);
        } else if (!hasActivePawn) {
            g2.setColor(new Color(60, 35, 110));
            g2.fillRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setColor(BORDER);
            g2.drawRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            g2.drawString("SPAWN", spawnBtnX + 22, btnY2 + 19);
        } else {
            g2.setColor(new Color(30, 20, 45));
            g2.fillRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setColor(new Color(60, 45, 75));
            g2.drawRect(spawnBtnX, btnY2, spawnBtnW, btnH2);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(new Color(80, 60, 100));
            g2.drawString("SPAWN", spawnBtnX + 22, btnY2 + 19);
        }
    }

    // ── UPGRADE BUTTON ────────────────────────────────────────────────────────

    private void drawUpgradeButton(Graphics2D g2, DeathTotem totem) {
        int[] cost = totem.getNextUpgradeCost();
        int btnY   = panelY + PANEL_H - 58;
        int btnX   = panelX + 24;
        int btnW   = PANEL_W - 48;
        int btnH   = 36;

        g2.setColor(cost != null ? BTN_UPGRADE : new Color(35, 28, 18));
        g2.fillRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        g2.setColor(cost != null ? new Color(220, 170, 50) : new Color(70, 60, 35));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(btnX, btnY, btnW, btnH, 10, 10);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        if (cost == null) {
            g2.setColor(new Color(110, 95, 55));
            String s = "TOTEM MAX CAPACITY (" + totem.getCapacity() + "/" + totem.getMaxCapacity() + ")";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, btnX + (btnW - fm.stringWidth(s)) / 2, btnY + 22);
        } else {
            g2.setColor(new Color(255, 210, 80));
            g2.drawString("UPGRADE → " + (totem.getCapacity() + 1) + " slots", btnX + 16, btnY + 14);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.setColor(new Color(200, 170, 100));
            String costStr = cost[0] + "G  " + cost[1] + " Bone  "
                    + cost[2] + " Skull  " + cost[3] + " Skel.Key";
            g2.drawString(costStr, btnX + 16, btnY + 28);
        }
    }

    // ── INPUT ─────────────────────────────────────────────────────────────────

    public void handleMouseClick(MouseEvent e) {
        if (!visible) return;

        // ↓↓↓ PENTING: kalau game pakai render scale, transform koordinat dulu
        // Kalau tidak pakai scale, ganti jadi: int mx = e.getX(); int my = e.getY();
        int mx = (int)(e.getX() / gp.scale);
        int my = (int)(e.getY() / gp.scale);

        if (mx < panelX || mx > panelX + PANEL_W
                || my < panelY || my > panelY + PANEL_H) {
            hide();
            return;
        }

        if (gp.deathTotemManager == null) return;
        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        List<CapturedMob> slots = totem.getSlots();

        if (!detailMode) {
            handleGridClick(e, mx, my, slots, totem);
        } else {
            handleDetailClick(e, mx, my, slots, totem);
        }
    }

    private void handleGridClick(MouseEvent e, int mx, int my,
                                  List<CapturedMob> slots, DeathTotem totem) {
        int capacity   = totem.getCapacity();
        int subX       = panelX + 24;
        int subY       = panelY + 75;
        int subW       = PANEL_W - 48;
        int slotSize   = 90;
        int slotPad    = 16;
        int maxCols    = 3;
        int totalGridW = maxCols * slotSize + (maxCols - 1) * slotPad;
        int gridStartX = subX + (subW - totalGridW) / 2;
        int gridStartY = subY + 35;

        for (int i = 0; i < capacity; i++) {
            int col = i % maxCols;
            int row = i / maxCols;
            int sx  = gridStartX + col * (slotSize + slotPad);
            int sy  = gridStartY + row * (slotSize + slotPad + 10);

            if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) {
                if (i < slots.size()) {
                    selectedSlot = i;
                    detailMode   = true;
                }
                return;
            }
        }

        int btnY = panelY + PANEL_H - 58;
        int btnX = panelX + 24;
        int btnW = PANEL_W - 48;
        int btnH = 36;
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            gp.deathTotemManager.tryUpgradeTotem();
        }
    }

    private void handleDetailClick(MouseEvent e, int mx, int my,
                                    List<CapturedMob> slots, DeathTotem totem) {
        int subX  = panelX + 24;
        int subY  = panelY + 75;
        int subW  = PANEL_W - 48;
        int subH  = PANEL_H - 100;
        int btnH2 = 30;
        int btnY2 = subY + subH - btnH2 - 16;

        // BACK button
        int btnW2 = 90;
        int btnX2 = subX + subW - btnW2 - 20;
        if (mx >= btnX2 && mx <= btnX2 + btnW2 && my >= btnY2 && my <= btnY2 + btnH2) {
            detailMode   = false;
            selectedSlot = -1;
            return;
        }

        // DELETE button
        int delBtnW = 90;
        int delBtnX = subX + subW / 2 - delBtnW / 2;
        if (mx >= delBtnX && mx <= delBtnX + delBtnW && my >= btnY2 && my <= btnY2 + btnH2) {
            totem.removeSlot(selectedSlot);  // ← tambah method ini di DeathTotem
            detailMode   = false;
            selectedSlot = -1;
            return;
        }

        // SPAWN / RECALL button
        int spawnBtnW = 100;
        int spawnBtnX = subX + 24;
        if (mx >= spawnBtnX && mx <= spawnBtnX + spawnBtnW
                && my >= btnY2 && my <= btnY2 + btnH2) {
            boolean isActive = (totem.getActivePawnSlot() == selectedSlot);
            if (isActive) {
                gp.deathTotemManager.trySpawn();
            } else if (!totem.hasPawnAlive()) {
                gp.deathTotemManager.trySpawnFromSlot(selectedSlot);
            }
        }
    }

    public void handleMouseMove(MouseEvent e) {}

    public boolean handleEsc() {
        if (!visible) return false;
        if (detailMode) {
            detailMode   = false;
            selectedSlot = -1;
        } else {
            hide();
        }
        return true;
    }
}