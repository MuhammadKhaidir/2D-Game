package UI;

import Item.CapturedMob;
import Item.DeathTotem;
import Main.GamePanel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Tampilan Death Totem — gaya ornamen ungu gelap + emas.
 * Semua posisi (draw & klik) berasal dari helper rect di bagian LAYOUT,
 * jadi mengubah tampilan gak akan bikin hitbox meleset.
 */
public class TotemUI {

    public boolean visible = false;
    private final GamePanel gp;

    private boolean detailMode = false;
    private int selectedSlot = -1;

    // >= 0 → dialog konfirmasi hapus lagi tampil untuk slot ini
    private int confirmDeleteSlot = -1;

    // posisi mouse buat efek hover
    private int hoverX = -1, hoverY = -1;

    // Set true kalau mau liat koordinat klik di console (buat debug)
    private static final boolean DEBUG_CLICK = false;

    private static final int PANEL_W   = 640;
    private static final int PANEL_H   = 440;
    private static final int SLOT_SIZE = 90;
    private static final int SLOT_PAD  = 16;
    private static final int MAX_COLS  = 3;
    private static final int DEL_SIZE  = 20;   // ukuran tombol X di pojok slot

    // ── PALET ─────────────────────────────────────────────────────────────────
    private static final Color BG_TOP       = new Color(24, 16, 40);
    private static final Color BG_BOTTOM    = new Color(13, 9, 24);
    private static final Color EDGE_DARK    = new Color(7, 4, 13);
    private static final Color PURPLE_LINE  = new Color(104, 68, 138);
    private static final Color PURPLE_SOFT  = new Color(58, 40, 80);
    private static final Color GOLD         = new Color(212, 146, 62);
    private static final Color GOLD_LIGHT   = new Color(246, 198, 112);
    private static final Color GOLD_DIM     = new Color(128, 86, 46);
    private static final Color CREAM        = new Color(240, 228, 204);
    private static final Color MAUVE        = new Color(190, 152, 178);
    private static final Color TEXT_SHADOW  = new Color(0, 0, 0, 170);
    private static final Color SLOT_FILL    = new Color(38, 26, 58);
    private static final Color SLOT_FILL_HI = new Color(56, 40, 82);
    private static final Color SLOT_EMPTY   = new Color(20, 13, 34);

    private static final Color BTN_DELETE   = new Color(150, 32, 36);
    private static final Color DEL_BORDER   = new Color(232, 92, 82);
    private static final Color DEL_TEXT     = new Color(255, 205, 200);

    private static final Color DISABLED_FILL   = new Color(30, 20, 45);
    private static final Color DISABLED_BORDER = new Color(64, 48, 82);
    private static final Color DISABLED_TEXT   = new Color(98, 82, 114);

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
        return new Rectangle(s.x + s.width - DEL_SIZE - 5, s.y + 5, DEL_SIZE, DEL_SIZE);
    }

    private Rectangle upgradeRect() {
        return new Rectangle(panelX + 20, panelY + PANEL_H - 84, PANEL_W - 40, 56);
    }

    // ── detail ──
    private int detailBtnY() {
        return panelY + 382;
    }

    private Rectangle detailBackRect() {
        return new Rectangle(panelX + PANEL_W - 44 - 90, detailBtnY(), 90, 30);
    }

    private Rectangle detailDeleteRect() {
        return new Rectangle(panelX + PANEL_W / 2 - 45, detailBtnY(), 90, 30);
    }

    private Rectangle detailSpawnRect() {
        return new Rectangle(panelX + 44, detailBtnY(), 100, 30);
    }

    // ── dialog konfirmasi ──
    private Rectangle confirmBoxRect() {
        int w = 360, h = 160;
        return new Rectangle(panelX + (PANEL_W - w) / 2, panelY + (PANEL_H - h) / 2, w, h);
    }

    private Rectangle confirmYesRect() {
        Rectangle b = confirmBoxRect();
        return new Rectangle(b.x + 28, b.y + b.height - 30 - 18, 130, 30);
    }

    private Rectangle confirmNoRect() {
        Rectangle b = confirmBoxRect();
        return new Rectangle(b.x + b.width - 28 - 130, b.y + b.height - 30 - 18, 130, 30);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW — PRIMITIF ORNAMEN
    // ══════════════════════════════════════════════════════════════════════════

    private void drawDiamond(Graphics2D g2, int cx, int cy, int r, Color fill, Color outline) {
        int[] xs = { cx, cx + r, cx, cx - r };
        int[] ys = { cy - r, cy, cy + r, cy };
        g2.setColor(fill);
        g2.fillPolygon(xs, ys, 4);
        if (outline != null) {
            g2.setColor(outline);
            g2.drawPolygon(xs, ys, 4);
        }
    }

    /** Siku (L) di empat pojok. len = panjang lengan, th = tebal. */
    private void drawCornerBrackets(Graphics2D g2, int x, int y, int w, int h,
                                    int len, int th, Color c) {
        g2.setColor(c);
        g2.fillRect(x, y, len, th);                       // kiri-atas
        g2.fillRect(x, y, th, len);
        g2.fillRect(x + w - len, y, len, th);             // kanan-atas
        g2.fillRect(x + w - th, y, th, len);
        g2.fillRect(x, y + h - th, len, th);              // kiri-bawah
        g2.fillRect(x, y + h - len, th, len);
        g2.fillRect(x + w - len, y + h - th, len, th);    // kanan-bawah
        g2.fillRect(x + w - th, y + h - len, th, len);
    }

    private void drawText(Graphics2D g2, String s, int x, int y, Color c) {
        g2.setColor(TEXT_SHADOW);
        g2.drawString(s, x + 1, y + 1);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    private void drawTextCentered(Graphics2D g2, String s, int centerX, int y, Color c) {
        FontMetrics f = g2.getFontMetrics();
        drawText(g2, s, centerX - f.stringWidth(s) / 2, y, c);
    }

    private int spacedWidth(FontMetrics fm, String s, int spacing) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += fm.charWidth(s.charAt(i));
        return w + spacing * (s.length() - 1);
    }

    private void drawSpaced(Graphics2D g2, String s, int x, int y, int spacing, Color c) {
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(c);
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            g2.drawString(String.valueOf(s.charAt(i)), cx, y);
            cx += fm.charWidth(s.charAt(i)) + spacing;
        }
    }

    private void drawIconFit(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        if (img == null) return;
        double s = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) Math.round(img.getWidth() * s);
        int dh = (int) Math.round(img.getHeight() * s);

        Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // pixel art tetap tajam
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        if (old != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
    }

    private BufferedImage iconFor(CapturedMob mob) {
        return mob.type == DeathTotem.CapturedType.GOLEM ? golemIcon : skeletonIcon;
    }

    private void drawButton(Graphics2D g2, Rectangle r, String label,
                            Color fill, Color border, Color text,
                            int fontSize, boolean hover) {
        Color base = hover ? fill.brighter() : fill;
        g2.setPaint(new GradientPaint(0, r.y, base.brighter(), 0, r.y + r.height, base));
        g2.fillRect(r.x, r.y, r.width, r.height);

        g2.setColor(EDGE_DARK);
        g2.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
        g2.setColor(border);
        g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g2.setColor(new Color(255, 255, 255, 45));          // kilau tipis di tepi atas
        g2.drawLine(r.x + 1, r.y + 1, r.x + r.width - 3, r.y + 1);

        g2.setFont(new Font("Monospaced", Font.BOLD, fontSize));
        FontMetrics f = g2.getFontMetrics();
        int tx = r.x + (r.width - f.stringWidth(label)) / 2;
        int ty = r.y + (r.height - f.getHeight()) / 2 + f.getAscent();
        drawText(g2, label, tx, ty, text);
    }

    private void drawDivider(Graphics2D g2, int y, boolean centerDiamond) {
        int x1 = panelX + 24;
        int x2 = panelX + PANEL_W - 24;
        g2.setColor(PURPLE_LINE);
        g2.drawLine(x1, y, x2, y);
        drawDiamond(g2, x1, y, 4, BG_TOP, GOLD);
        drawDiamond(g2, x2, y, 4, BG_TOP, GOLD);
        if (centerDiamond) {
            drawDiamond(g2, panelX + PANEL_W / 2, y, 4, BG_TOP, GOLD);
        }
    }

    /** Bar bertepi emas (dipakai EXP & HP di detail view). */
    private void drawBar(Graphics2D g2, int x, int y, int w, int h, double frac,
                         Color top, Color bottom, String label) {
        g2.setColor(EDGE_DARK);
        g2.fillRect(x - 2, y - 2, w + 4, h + 4);
        g2.setColor(GOLD_DIM);
        g2.drawRect(x - 2, y - 2, w + 3, h + 3);
        g2.setColor(new Color(14, 9, 24));
        g2.fillRect(x, y, w, h);

        int fill = (int) Math.round(Math.max(0, Math.min(1, frac)) * w);
        if (fill > 0) {
            g2.setPaint(new GradientPaint(0, y, top, 0, y + h, bottom));
            g2.fillRect(x, y, fill, h);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawLine(x, y + 1, x + fill - 1, y + 1);
        }

        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        FontMetrics f = g2.getFontMetrics();
        int ty = y + (h - f.getHeight()) / 2 + f.getAscent();
        drawText(g2, label, x + (w - f.stringWidth(label)) / 2, ty, CREAM);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW — PANEL, JUDUL, HEADER
    // ══════════════════════════════════════════════════════════════════════════

    public void draw(Graphics2D g2) {
        if (!visible || gp.deathTotemManager == null) return;

        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        List<CapturedMob> slots = totem.getSlots();
        int capacity = totem.getCapacity();

        drawPanelFrame(g2);
        drawTitle(g2);

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

        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.WHITE);
    }

    private void drawPanelFrame(Graphics2D g2) {
        int x = panelX, y = panelY, w = PANEL_W, h = PANEL_H;

        // bayangan jatuh
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(x + 8, y + 8, w, h);

        // dasar gradasi
        g2.setPaint(new GradientPaint(0, y, BG_TOP, 0, y + h, BG_BOTTOM));
        g2.fillRect(x, y, w, h);

        // tepi: gelap → emas redup → ungu
        g2.setColor(EDGE_DARK);
        g2.drawRect(x, y, w - 1, h - 1);
        g2.setColor(GOLD_DIM);
        g2.drawRect(x + 3, y + 3, w - 7, h - 7);
        g2.setColor(PURPLE_LINE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x + 8, y + 8, w - 17, h - 17);
        g2.setStroke(new BasicStroke(1));

        // siku emas di empat pojok + titik kilau
        drawCornerBrackets(g2, x + 5, y + 5, w - 10, h - 10, 18, 3, GOLD);
        g2.setColor(GOLD_LIGHT);
        g2.fillRect(x + 12, y + 12, 3, 3);
        g2.fillRect(x + w - 15, y + 12, 3, 3);
        g2.fillRect(x + 12, y + h - 15, 3, 3);
        g2.fillRect(x + w - 15, y + h - 15, 3, 3);

        // ornamen tengah atas (menonjol di garis tepi)
        int cx = x + w / 2;
        int[] ox = { cx - 34, cx + 34, cx + 22, cx - 22 };
        int[] oy = { y, y, y + 12, y + 12 };
        g2.setColor(new Color(34, 22, 54));
        g2.fillPolygon(ox, oy, 4);
        g2.setColor(PURPLE_LINE);
        g2.drawPolygon(ox, oy, 4);
        drawDiamond(g2, cx, y + 7, 6, GOLD, EDGE_DARK);
        drawDiamond(g2, cx, y + 7, 2, GOLD_LIGHT, null);
    }

    private void drawTitle(Graphics2D g2) {
        String title = "DEATH TOTEM";
        g2.setFont(new Font("Serif", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int spacing  = 2;
        int textW    = spacedWidth(fm, title, spacing);
        int plaqueW  = textW + 56;
        int plaqueH  = 36;
        int px = panelX + (PANEL_W - plaqueW) / 2;
        int py = panelY + 17;

        g2.setColor(new Color(22, 13, 36));
        g2.fillRect(px, py, plaqueW, plaqueH);
        g2.setColor(EDGE_DARK);
        g2.drawRect(px - 1, py - 1, plaqueW + 1, plaqueH + 1);
        g2.setColor(GOLD);
        g2.drawRect(px, py, plaqueW - 1, plaqueH - 1);
        g2.setColor(GOLD_DIM);
        g2.drawRect(px + 3, py + 3, plaqueW - 7, plaqueH - 7);
        drawCornerBrackets(g2, px + 1, py + 1, plaqueW - 2, plaqueH - 2, 6, 2, GOLD_LIGHT);

        // berlian di kiri, kanan, dan bawah plakat
        int midY = py + plaqueH / 2;
        drawDiamond(g2, px - 4, midY, 6, GOLD, EDGE_DARK);
        drawDiamond(g2, px + plaqueW + 3, midY, 6, GOLD, EDGE_DARK);
        drawDiamond(g2, panelX + PANEL_W / 2, py + plaqueH + 3, 4, GOLD, EDGE_DARK);

        int tx = px + (plaqueW - textW) / 2;
        int ty = py + (plaqueH - fm.getHeight()) / 2 + fm.getAscent();
        drawSpaced(g2, title, tx + 2, ty + 2, spacing, TEXT_SHADOW);
        drawSpaced(g2, title, tx, ty, spacing, CREAM);
    }

    private void drawHeaderBar(Graphics2D g2, String left, String right) {
        drawDivider(g2, panelY + 66, true);
        drawDivider(g2, panelY + 92, false);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        FontMetrics f = g2.getFontMetrics();
        drawText(g2, left, panelX + 34, panelY + 83, CREAM);
        drawText(g2, right, panelX + PANEL_W - 34 - f.stringWidth(right), panelY + 83, CREAM);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW — GRID VIEW
    // ══════════════════════════════════════════════════════════════════════════

    private void drawGridView(Graphics2D g2, List<CapturedMob> slots,
                              int capacity, DeathTotem totem) {
        drawHeaderBar(g2,
                "Souls: " + slots.size() + " / " + capacity,
                "[ESC] Close  [Click] Details  [X] Delete");

        for (int i = 0; i < capacity; i++) {
            Rectangle r = slotRect(i);

            boolean hasMob   = i < slots.size();
            boolean isActive = hasMob && (totem.getActivePawnSlot() == i);
            boolean hovered  = hasMob && r.contains(hoverX, hoverY);

            drawSlot(g2, r, hasMob, isActive, hovered);

            if (hasMob) {
                CapturedMob mob = slots.get(i);

                drawIconFit(g2, iconFor(mob), r.x + 10, r.y + 10, SLOT_SIZE - 20, SLOT_SIZE - 38);

                // label: kecilin font otomatis kalau kepanjangan
                String label = mob.getTypeName() + " Lv." + mob.level;
                int fs = 12;
                g2.setFont(new Font("Monospaced", Font.BOLD, fs));
                while (g2.getFontMetrics().stringWidth(label) > SLOT_SIZE - 10 && fs > 8) {
                    fs--;
                    g2.setFont(new Font("Monospaced", Font.BOLD, fs));
                }
                drawTextCentered(g2, label, r.x + SLOT_SIZE / 2, r.y + SLOT_SIZE - 9, CREAM);

                if (isActive) {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                    drawText(g2, "ACTIVE", r.x + 9, r.y + 18, new Color(170, 255, 170));
                }

                // tombol hapus (X) di pojok kanan atas slot
                Rectangle del = slotDeleteRect(i);
                if (isActive) {
                    // pion lagi keluar → gak bisa dihapus, harus recall dulu
                    drawButton(g2, del, "X", DISABLED_FILL, DISABLED_BORDER,
                            DISABLED_TEXT, 12, false);
                } else {
                    drawButton(g2, del, "X", BTN_DELETE, DEL_BORDER, Color.WHITE, 12,
                            del.contains(hoverX, hoverY));
                }
            } else {
                g2.setFont(new Font("Monospaced", Font.ITALIC, 11));
                drawTextCentered(g2, "empty", r.x + SLOT_SIZE / 2, r.y + SLOT_SIZE / 2 + 4,
                        new Color(78, 58, 100));
            }
        }

        drawUpgradeButton(g2, totem);
    }

    private void drawSlot(Graphics2D g2, Rectangle r, boolean hasMob,
                          boolean isActive, boolean hovered) {
        if (!hasMob) {
            g2.setColor(SLOT_EMPTY);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.setColor(new Color(58, 42, 78));
            g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            g2.setColor(new Color(36, 26, 52));
            g2.drawRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7);
            return;
        }

        Color top    = hovered ? SLOT_FILL_HI : SLOT_FILL;
        Color bottom = hovered ? SLOT_FILL : new Color(28, 19, 44);
        g2.setPaint(new GradientPaint(0, r.y, top, 0, r.y + r.height, bottom));
        g2.fillRect(r.x, r.y, r.width, r.height);

        Color frame = (isActive || hovered) ? GOLD_LIGHT : GOLD;
        g2.setColor(EDGE_DARK);
        g2.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
        g2.setColor(frame);
        g2.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g2.setColor(PURPLE_LINE);
        g2.drawRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7);
        drawCornerBrackets(g2, r.x, r.y, r.width, r.height, 9, 3, frame);

        if (isActive) {   // glow hijau tipis buat pion yang lagi keluar
            g2.setColor(new Color(170, 255, 170, 90));
            g2.drawRect(r.x - 3, r.y - 3, r.width + 5, r.height + 5);
        }
    }

    // ── UPGRADE BANNER ────────────────────────────────────────────────────────

    private void drawUpgradeButton(Graphics2D g2, DeathTotem totem) {
        int[] cost = totem.getNextUpgradeCost();
        Rectangle r = upgradeRect();
        boolean enabled = cost != null;
        boolean hover   = enabled && r.contains(hoverX, hoverY);

        Color topC = enabled ? new Color(70, 46, 26) : new Color(34, 26, 22);
        Color botC = enabled ? new Color(44, 28, 17) : new Color(24, 18, 16);
        if (hover) { topC = topC.brighter(); botC = botC.brighter(); }
        g2.setPaint(new GradientPaint(0, r.y, topC, 0, r.y + r.height, botC));
        g2.fillRect(r.x, r.y, r.width, r.height);

        Color gold = enabled ? (hover ? GOLD_LIGHT : GOLD) : GOLD_DIM;
        g2.setColor(EDGE_DARK);
        g2.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
        g2.setColor(gold);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
        g2.setStroke(new BasicStroke(1));
        drawCornerBrackets(g2, r.x, r.y, r.width, r.height, 10, 3, gold);

        if (!enabled) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            String s = "TOTEM MAX CAPACITY (" + totem.getCapacity() + "/" + totem.getMaxCapacity() + ")";
            drawTextCentered(g2, s, r.x + r.width / 2, r.y + r.height / 2 + 5,
                    new Color(150, 128, 92));
            return;
        }

        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        drawText(g2, "UPGRADE \u2192 " + (totem.getCapacity() + 1) + " slots",
                r.x + 24, r.y + 24, CREAM);

        // garis tipis dengan berlian di ujung
        int dy = r.y + 31;
        g2.setColor(GOLD_DIM);
        g2.drawLine(r.x + 18, dy, r.x + r.width - 18, dy);
        drawDiamond(g2, r.x + 18, dy, 3, GOLD_DIM, null);
        drawDiamond(g2, r.x + r.width - 18, dy, 3, GOLD_DIM, null);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        String costStr = cost[0] + "G  " + cost[1] + " Bone  "
                + cost[2] + " Skull  " + cost[3] + " Skel.Key";
        drawText(g2, costStr, r.x + 24, r.y + 48, MAUVE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW — DETAIL VIEW
    // ══════════════════════════════════════════════════════════════════════════

    private void drawDetailView(Graphics2D g2, CapturedMob mob, DeathTotem totem) {
        boolean isActive = (totem.getActivePawnSlot() == selectedSlot);

        drawHeaderBar(g2, "Soul #" + (selectedSlot + 1), "[ESC] Back");

        int left   = panelX + 44;
        int top    = panelY + 108;
        int innerW = PANEL_W - 88;

        // potret
        Rectangle portrait = new Rectangle(left, top, 100, 100);
        drawSlot(g2, portrait, true, isActive, false);
        drawIconFit(g2, iconFor(mob), portrait.x + 10, portrait.y + 10, 80, 80);

        // info
        int infoX = left + 100 + 26;
        g2.setFont(new Font("Serif", Font.BOLD, 24));
        drawText(g2, mob.getTypeName().toUpperCase(), infoX, top + 22, CREAM);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        drawText(g2, isActive ? "[ACTIVE]" : "[STORED]", infoX, top + 40,
                isActive ? new Color(170, 255, 170) : GOLD);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        drawText(g2, "Level   : " + mob.level + " / " + mob.getMaxLevel(), infoX, top + 62, CREAM);
        drawText(g2, "HP      : " + mob.storedHP + " / " + mob.maxHP,      infoX, top + 80, CREAM);
        drawText(g2, "ATK     : " + mob.getCurrentAttack(),                 infoX, top + 98, CREAM);

        int divY = top + 100 + 16;
        drawDivider(g2, divY, false);

        // bar EXP & HP
        int barH = 16;
        int expY = divY + 16;
        int hpY  = expY + barH + 12;

        int expCurrent = mob.currentExp;
        int expNeeded  = mob.expNeededForNextLevel();
        boolean maxed  = mob.level >= mob.getMaxLevel();
        if (expNeeded > 0 && !maxed) {
            drawBar(g2, left, expY, innerW, barH,
                    (double) Math.min(expCurrent, expNeeded) / expNeeded,
                    new Color(140, 196, 255), new Color(68, 118, 214),
                    "EXP: " + expCurrent + " / " + expNeeded);
        } else {
            drawBar(g2, left, expY, innerW, barH, 1.0, GOLD_LIGHT, GOLD, "MAX LEVEL");
        }

        double hpFrac = mob.maxHP > 0 ? (double) mob.storedHP / mob.maxHP : 0;
        drawBar(g2, left, hpY, innerW, barH, hpFrac,
                new Color(120, 228, 138), new Color(50, 158, 80),
                "HP: " + mob.storedHP + " / " + mob.maxHP);

        // base stats
        int descY = hpY + barH + 28;
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        drawText(g2, "\u2014 Base Stats (Level 1) \u2014", left, descY, GOLD);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        drawText(g2, "Base HP  : " + mob.baseMaxHP,  left, descY + 18, MAUVE);
        drawText(g2, "Base ATK : " + mob.baseAttack, left, descY + 34, MAUVE);
        drawText(g2, "EXP/lvl  : " + mob.expPerLevel + " (x level)", left, descY + 50, MAUVE);

        // ── ROW TOMBOL BAWAH ──────────────────────────────────────────────────

        // SPAWN / RECALL (kiri)
        Rectangle spawn = detailSpawnRect();
        boolean hasActivePawn = totem.hasPawnAlive();
        if (isActive) {
            drawButton(g2, spawn, "RECALL (T)", new Color(150, 52, 52),
                    new Color(240, 124, 112), Color.WHITE, 12,
                    spawn.contains(hoverX, hoverY));
        } else if (!hasActivePawn) {
            drawButton(g2, spawn, "SPAWN", new Color(66, 40, 112),
                    new Color(176, 126, 236), Color.WHITE, 12,
                    spawn.contains(hoverX, hoverY));
        } else {
            drawButton(g2, spawn, "SPAWN", DISABLED_FILL, DISABLED_BORDER,
                    DISABLED_TEXT, 12, false);
        }

        // DELETE (tengah) — mati kalau pion ini lagi aktif
        Rectangle del = detailDeleteRect();
        if (isActive) {
            drawButton(g2, del, "DELETE", DISABLED_FILL, DISABLED_BORDER,
                    DISABLED_TEXT, 12, false);
        } else {
            drawButton(g2, del, "DELETE", BTN_DELETE, DEL_BORDER, DEL_TEXT, 12,
                    del.contains(hoverX, hoverY));
        }

        // BACK (kanan)
        Rectangle back = detailBackRect();
        drawButton(g2, back, "\u2190 BACK", new Color(46, 30, 74), GOLD_DIM, CREAM, 12,
                back.contains(hoverX, hoverY));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAW — DIALOG KONFIRMASI HAPUS
    // ══════════════════════════════════════════════════════════════════════════

    private void drawConfirmDialog(Graphics2D g2, List<CapturedMob> slots) {
        if (confirmDeleteSlot < 0 || confirmDeleteSlot >= slots.size()) {
            confirmDeleteSlot = -1;
            return;
        }
        CapturedMob mob = slots.get(confirmDeleteSlot);

        // redupin panel di belakang
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(panelX + 8, panelY + 8, PANEL_W - 16, PANEL_H - 16);

        Rectangle box = confirmBoxRect();
        Color red = new Color(206, 62, 58);

        g2.setPaint(new GradientPaint(0, box.y, new Color(36, 18, 34),
                0, box.y + box.height, new Color(18, 10, 22)));
        g2.fillRect(box.x, box.y, box.width, box.height);

        g2.setColor(EDGE_DARK);
        g2.drawRect(box.x - 1, box.y - 1, box.width + 1, box.height + 1);
        g2.setColor(red);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(box.x + 1, box.y + 1, box.width - 3, box.height - 3);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(110, 40, 44));
        g2.drawRect(box.x + 6, box.y + 6, box.width - 13, box.height - 13);
        drawCornerBrackets(g2, box.x, box.y, box.width, box.height, 12, 3, red);

        int cx = box.x + box.width / 2;

        g2.setFont(new Font("Serif", Font.BOLD, 20));
        drawTextCentered(g2, "DELETE SOUL?", cx, box.y + 38, new Color(255, 196, 190));

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        drawTextCentered(g2, mob.getTypeName() + " Lv." + mob.level, cx, box.y + 68, CREAM);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        drawTextCentered(g2, "This soul will be lost forever.", cx, box.y + 90, MAUVE);

        Rectangle yes = confirmYesRect();
        Rectangle no  = confirmNoRect();
        drawButton(g2, yes, "YES, DELETE", BTN_DELETE, DEL_BORDER, DEL_TEXT, 12,
                yes.contains(hoverX, hoverY));
        drawButton(g2, no, "CANCEL", new Color(46, 30, 74), GOLD_DIM, CREAM, 12,
                no.contains(hoverX, hoverY));
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