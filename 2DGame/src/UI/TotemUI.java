package UI;

import Item.CapturedMob;
import Item.DeathTotem;
import Main.GamePanel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Tampilan Death Totem — desain disamakan dengan MobIndexUI.
 * Grid  : skema coklat default (sama kayak Backpack / Mob Index).
 * Detail: warna tema ngikutin rarity mob (lihat rarityOf()).
 *
 * Semua posisi (draw & klik) berasal dari helper rect di bagian GEOMETRY,
 * jadi mengubah tampilan gak akan bikin hitbox meleset.
 */
public class TotemUI {

    public boolean visible = false;
    private final GamePanel gp;

    private boolean detailMode = false;
    private int selectedSlot = -1;

    // >= 0 → dialog konfirmasi hapus lagi tampil untuk slot ini
    private int confirmDeleteSlot = -1;

    // posisi mouse + slot yang lagi di-hover
    private int hoverX = -1, hoverY = -1;
    private int hoveredSlot = -1;

    // state animasi
    private long now = 0;
    private long openStart = 0;
    private long detailStart = 0;
    private long confirmStart = 0;
    private float openAlpha = 1f;

    // Set true kalau mau liat koordinat klik di console (buat debug)
    private static final boolean DEBUG_CLICK = false;

    private static final int PANEL_W   = 640;
    private static final int PANEL_H   = 440;
    private static final int SLOT_SIZE = 90;
    private static final int SLOT_PAD  = 16;
    private static final int MAX_COLS  = 3;
    private static final int DEL_SIZE  = 20;   // ukuran tombol X di pojok slot

    // ── PALET (default = sama kayak MobIndexUI / BackpackUI) ──────────────────
    private static final Color C_OUTER   = new Color(45, 30, 20);
    private static final Color C_INNER   = new Color(85, 65, 50);
    private static final Color C_SUB     = new Color(35, 25, 20);
    private static final Color C_SLOT    = new Color(60, 45, 35);
    private static final Color C_SLOT_DK = new Color(25, 20, 15);
    private static final Color C_BORDER  = new Color(220, 190, 150);
    private static final Color C_ACCENT  = new Color(250, 180, 40);

    private static final Color C_ACTIVE  = new Color(120, 230, 130);   // pion lagi keluar
    private static final Color C_DANGER  = new Color(206, 62, 58);

    private static final Color RED_FILL   = new Color(110, 32, 34);
    private static final Color RED_DARK   = new Color(60, 16, 18);
    private static final Color RED_BORDER = new Color(232, 92, 82);
    private static final Color RED_ACCENT = new Color(255, 130, 120);

    private static final Color RECALL_FILL   = new Color(120, 62, 36);
    private static final Color RECALL_DARK   = new Color(66, 32, 18);
    private static final Color RECALL_BORDER = new Color(240, 150, 100);
    private static final Color RECALL_ACCENT = new Color(255, 190, 120);

    private static final Color OFF_FILL   = new Color(30, 24, 22);
    private static final Color OFF_DARK   = new Color(20, 16, 14);
    private static final Color OFF_BORDER = new Color(70, 58, 50);
    private static final Color OFF_TEXT   = new Color(98, 86, 78);

    private static final Color DLG_OUTER = new Color(40, 16, 18);
    private static final Color DLG_INNER = new Color(72, 30, 30);

    // Font di-cache supaya tidak bikin object baru tiap frame
    private final Font fTitle   = new Font("Monospaced", Font.BOLD, 20);
    private final Font fName    = new Font("Monospaced", Font.BOLD, 22);
    private final Font fBold16  = new Font("Monospaced", Font.BOLD, 16);
    private final Font fBold13  = new Font("Monospaced", Font.BOLD, 13);
    private final Font fPlain13 = new Font("Monospaced", Font.PLAIN, 13);
    private final Font fBold12  = new Font("Monospaced", Font.BOLD, 12);
    private final Font fBold11  = new Font("Monospaced", Font.BOLD, 11);
    private final Font fBold10  = new Font("Monospaced", Font.BOLD, 10);
    private final Font fBold9   = new Font("Monospaced", Font.BOLD, 9);
    private final Font[] labelFonts = { fBold12, fBold11, fBold10, fBold9 };

    private int panelX, panelY;

    private BufferedImage golemIcon    = null;
    private BufferedImage golemiteIcon = null;
    private BufferedImage skeletonIcon = null;

    public TotemUI(GamePanel gp) {
        this.gp = gp;
        loadIcons();
    }

    private void loadIcons() {
        try {
            golemIcon    = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontWalk (1).png"));
            golemiteIcon = ImageIO.read(getClass().getResourceAsStream("/Aset/golemiteFrontWalk (1).png"));
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
        hoveredSlot       = -1;
        openStart         = System.currentTimeMillis();
        detailStart       = openStart;
        panelX = gp.screenWidth  / 2 - PANEL_W / 2;
        panelY = gp.screenHeight / 2 - PANEL_H / 2;
    }

    public void hide() {
        visible           = false;
        detailMode        = false;
        selectedSlot      = -1;
        confirmDeleteSlot = -1;
        hoveredSlot       = -1;
        hoverX = -1;
        hoverY = -1;
    }

    public void update() {}

    // =====================================================================
    //  RARITY HELPERS (sama kayak MobIndexUI)
    // =====================================================================

    // Rarity tiap tipe mob. Ubah di sini kalau mau warna detail beda.
    private String rarityOf(CapturedMob mob) {
        switch (mob.type) {
            case GOLEM:    return "EPIC";
            case GOLEMITE: return "RARE";
            default:       return "COMMON";
        }
    }

    private Color getRarityColor(String rarity) {
        switch (rarity.toUpperCase()) {
            case "COMMON":    return new Color(180, 180, 180);
            case "RARE":      return new Color(90, 160, 255);
            case "EPIC":      return new Color(190, 80, 255);
            case "LEGENDARY": return new Color(255, 190, 40);
            default:          return Color.WHITE;
        }
    }

    // 0 = common, 1 = rare, 2 = epic, 3 = legendary (dipakai buat glow, pips, spark)
    private int getRarityTier(String rarity) {
        switch (rarity.toUpperCase()) {
            case "RARE":      return 1;
            case "EPIC":      return 2;
            case "LEGENDARY": return 3;
            default:          return 0;
        }
    }

    // Index: 0=Outer, 1=Inner, 2=SubPanel, 3=Slot/Button, 4=SlotDark, 5=Border
    private Color[] getRarityThemeColors(String rarity) {
        Color[] theme = new Color[6];
        switch (rarity.toUpperCase()) {
            case "COMMON":
                theme[0] = new Color(35, 35, 35);
                theme[1] = new Color(60, 60, 60);
                theme[2] = new Color(25, 25, 25);
                theme[3] = new Color(45, 45, 45);
                theme[4] = new Color(20, 20, 20);
                theme[5] = new Color(170, 170, 170);
                break;
            case "RARE":
                theme[0] = new Color(15, 25, 45);
                theme[1] = new Color(30, 55, 95);
                theme[2] = new Color(10, 18, 35);
                theme[3] = new Color(20, 40, 75);
                theme[4] = new Color(8, 14, 28);
                theme[5] = new Color(120, 180, 255);
                break;
            case "EPIC":
                theme[0] = new Color(30, 15, 40);
                theme[1] = new Color(55, 30, 75);
                theme[2] = new Color(20, 10, 30);
                theme[3] = new Color(45, 20, 65);
                theme[4] = new Color(15, 5, 25);
                theme[5] = new Color(220, 140, 255);
                break;
            case "LEGENDARY":
                theme[0] = new Color(45, 30, 10);
                theme[1] = new Color(95, 65, 20);
                theme[2] = new Color(30, 20, 5);
                theme[3] = new Color(75, 50, 15);
                theme[4] = new Color(25, 15, 3);
                theme[5] = new Color(255, 210, 90);
                break;
            default:
                theme[0] = C_OUTER;
                theme[1] = C_INNER;
                theme[2] = C_SUB;
                theme[3] = C_SLOT;
                theme[4] = C_SLOT_DK;
                theme[5] = C_BORDER;
                break;
        }
        return theme;
    }

    // =====================================================================
    //  GEOMETRY — satu sumber untuk draw DAN klik, jadi hitbox pasti sama
    // =====================================================================

    private Rectangle panelRect() {
        return new Rectangle(panelX, panelY, PANEL_W, PANEL_H);
    }

    private Rectangle subRect() {
        return new Rectangle(panelX + 24, panelY + 75, PANEL_W - 48, PANEL_H - 100);
    }

    // ── grid ──
    private Rectangle slotRect(int i) {
        Rectangle sub = subRect();
        int totalGridW = MAX_COLS * SLOT_SIZE + (MAX_COLS - 1) * SLOT_PAD;
        int gridStartX = sub.x + (sub.width - totalGridW) / 2;
        int gridStartY = sub.y + 36;
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

    private Rectangle infoBarRect() {
        Rectangle sub = subRect();
        return new Rectangle(sub.x + 16, sub.y + 247, sub.width - 32, 26);
    }

    private Rectangle upgradeRect() {
        Rectangle sub = subRect();
        return new Rectangle(sub.x + 16, sub.y + sub.height - 54 - 8, sub.width - 32, 54);
    }

    // ── detail ──
    private int detailBtnY() {
        Rectangle sub = subRect();
        return sub.y + sub.height - 30 - 14;
    }

    private Rectangle detailSpawnRect() {
        Rectangle sub = subRect();
        return new Rectangle(sub.x + 20, detailBtnY(), 110, 30);
    }

    private Rectangle detailDeleteRect() {
        Rectangle sub = subRect();
        return new Rectangle(sub.x + (sub.width - 100) / 2, detailBtnY(), 100, 30);
    }

    private Rectangle detailBackRect() {
        Rectangle sub = subRect();
        return new Rectangle(sub.x + sub.width - 100 - 20, detailBtnY(), 100, 30);
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

    // hover cuma aktif kalau dialog konfirmasi lagi gak tampil
    private boolean hov(Rectangle r) {
        return confirmDeleteSlot < 0 && r.contains(hoverX, hoverY);
    }

    // =====================================================================
    //  DRAW — UTAMA
    // =====================================================================

    public void draw(Graphics2D g2) {
        if (!visible || gp.deathTotemManager == null) return;

        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        now = System.currentTimeMillis();

        List<CapturedMob> slots = totem.getSlots();
        int capacity = totem.getCapacity();

        CapturedMob detailMob = null;
        if (detailMode) {
            if (selectedSlot >= 0 && selectedSlot < slots.size()) {
                detailMob = slots.get(selectedSlot);
            } else {
                detailMode   = false;
                selectedSlot = -1;
            }
        }

        Object oldAA     = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Stroke oldStroke = g2.getStroke();
        Composite oldComp = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Animasi buka: panel meluncur dari atas + fade in
        float openE = ease(clamp01((now - openStart) / 260f));
        openAlpha = openE;
        int slideY = Math.round((1f - openE) * -18f);
        g2.translate(0, slideY);
        if (openE < 1f) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, openE));

        // Skema warna aktif: grid = coklat default, detail = ngikutin rarity mob
        Color bgOuter, bgInner, border, accent;
        if (detailMob != null) {
            String rar = rarityOf(detailMob);
            Color[] th = getRarityThemeColors(rar);
            bgOuter = th[0];
            bgInner = th[1];
            border  = th[5];
            accent  = getRarityColor(rar);
        } else {
            bgOuter = C_OUTER;
            bgInner = C_INNER;
            border  = C_BORDER;
            accent  = C_ACCENT;
        }

        // Shadow belakang
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(panelX + 8, panelY + 8, PANEL_W, PANEL_H);

        // Frame utama
        g2.setColor(bgOuter);
        g2.fillRect(panelX, panelY, PANEL_W, PANEL_H);
        g2.setColor(bgInner);
        g2.fillRect(panelX + 6, panelY + 6, PANEL_W - 12, PANEL_H - 12);

        g2.setStroke(new BasicStroke(2));
        g2.setColor(border);
        g2.drawRect(panelX + 8, panelY + 8, PANEL_W - 16, PANEL_H - 16);

        // Garis dalam tipis (bingkai ganda)
        g2.setColor(withAlpha(border, 70));
        frameRect(g2, panelX + 13, panelY + 13, PANEL_W - 26, PANEL_H - 26, 1);

        // Ornamen sudut yang berdenyut
        int cornerA = 170 + (int) (85 * Math.sin(now / 500.0));
        drawCorners(g2, panelX, panelY, PANEL_W, PANEL_H, withAlpha(accent, cornerA));

        drawTitle(g2, bgOuter, border, accent);

        if (detailMob != null) {
            drawDetailView(g2, detailMob, totem);
        } else {
            drawGridView(g2, slots, capacity, totem);
        }

        // tombol silang buat nutup Totem UI
        CloseButton.draw(g2, CloseButton.rect(panelX, panelY, PANEL_W));

        // dialog konfirmasi selalu paling atas
        if (confirmDeleteSlot >= 0) {
            drawConfirmDialog(g2, slots);
        }

        // Restore state graphics
        g2.translate(0, -slideY);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
        if (oldAA != null)     g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        g2.setColor(Color.WHITE);
    }

    private void drawTitle(Graphics2D g2, Color bgOuter, Color border, Color accent) {
        String title = "DEATH TOTEM";
        g2.setFont(fTitle);
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (PANEL_W - titleW) / 2;

        g2.setColor(bgOuter);
        g2.fillRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(border);
        g2.drawRect(titleX - 20, panelY + 18, titleW + 40, 30);

        // Diamond kiri-kanan judul
        int dA = 150 + (int) (105 * Math.sin(now / 350.0));
        g2.setColor(withAlpha(accent, dA));
        drawDiamond(g2, titleX - 38, panelY + 33, 5);
        drawDiamond(g2, titleX + titleW + 38, panelY + 33, 5);

        g2.setColor(Color.BLACK);
        g2.drawString(title, titleX + 2, panelY + 40);
        g2.setColor(accent);
        g2.drawString(title, titleX, panelY + 38);

        // Kilau (shine) yang lewat di judul tiap beberapa detik
        double phase = (now % 3600L) / 1100.0;
        if (phase < 1.0) {
            int sx = titleX - 10 + (int) (phase * (titleW + 20));
            Shape oldClip = g2.getClip();
            g2.setClip(sx, panelY + 18, 12, 30);
            g2.setColor(Color.WHITE);
            g2.drawString(title, titleX, panelY + 38);
            g2.setClip(oldClip);
        }
    }

    // =====================================================================
    //  TAMPILAN 1: GRID
    // =====================================================================

    private void drawGridView(Graphics2D g2, List<CapturedMob> slots, int capacity, DeathTotem totem) {
        Rectangle sub = subRect();

        g2.setColor(C_SUB);
        g2.fillRect(sub.x, sub.y, sub.width, sub.height);
        g2.setColor(C_OUTER);
        g2.drawRect(sub.x, sub.y, sub.width, sub.height);

        drawEmbers(g2, sub, C_ACCENT);

        // Header kecil
        g2.setFont(fBold11);
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(new Color(150, 125, 105));
        g2.drawString("- SELECT SOUL -", sub.x + 16, sub.y + 22);
        String cnt = "SOULS " + slots.size() + " / " + capacity;
        g2.drawString(cnt, sub.x + sub.width - hfm.stringWidth(cnt) - 16, sub.y + 22);

        for (int i = 0; i < capacity; i++) {
            Rectangle r = slotRect(i);

            if (i >= slots.size()) {
                drawEmptySlot(g2, r, i);
                continue;
            }

            boolean isActive = (totem.getActivePawnSlot() == i);
            boolean hover    = (i == hoveredSlot) && confirmDeleteSlot < 0;
            drawSoulSlot(g2, r, slots.get(i), i, isActive, hover);
        }

        drawInfoBar(g2, slots, totem);
        drawUpgradeButton(g2, totem);
    }

    private void drawSoulSlot(Graphics2D g2, Rectangle r, CapturedMob mob, int i,
                              boolean isActive, boolean hover) {
        String rarity = rarityOf(mob);
        Color rc = getRarityColor(rarity);
        int tier = getRarityTier(rarity);
        Color gc = isActive ? C_ACTIVE : rc;   // warna glow / aksen
        double pulse = 0.5 + 0.5 * Math.sin(now / 450.0 + i * 1.3);

        int sx = r.x;
        int sy = r.y + (hover ? -4 : 0);       // slot naik sedikit saat di-hover

        // Glow di belakang slot (berdenyut untuk rarity tinggi / pion aktif)
        if (tier >= 1 || hover || isActive) {
            int a = hover ? 110 : (isActive ? (int) (60 + 50 * pulse) : (int) ((25 + 35 * pulse) * tier));
            int la = Math.min(255, a / 3);
            for (int g = 3; g >= 1; g--) {
                g2.setColor(withAlpha(gc, la));
                g2.fillRect(sx - g * 2, sy - g * 2, SLOT_SIZE + g * 4, SLOT_SIZE + g * 4);
            }
        }

        // Dasar slot + tint warna rarity
        g2.setColor(C_SLOT_DK);
        g2.fillRect(sx, sy, SLOT_SIZE, SLOT_SIZE);
        g2.setColor(withAlpha(rc, hover ? 55 : 26));
        g2.fillRect(sx + 2, sy + 2, SLOT_SIZE - 4, SLOT_SIZE - 4);

        // Bevel pixel-art
        g2.setColor(withAlpha(Color.WHITE, 22));
        g2.fillRect(sx + 2, sy + 2, SLOT_SIZE - 4, 2);
        g2.fillRect(sx + 2, sy + 2, 2, SLOT_SIZE - 4);
        g2.setColor(withAlpha(Color.BLACK, 90));
        g2.fillRect(sx + 2, sy + SLOT_SIZE - 4, SLOT_SIZE - 4, 2);
        g2.fillRect(sx + SLOT_SIZE - 4, sy + 2, 2, SLOT_SIZE - 4);

        // Border
        Color border = hover ? lerp(rc, Color.WHITE, 0.35f) : lerp(C_OUTER, rc, 0.6f);
        if (isActive) border = lerp(border, C_ACTIVE, 0.65f);
        g2.setColor(border);
        frameRect(g2, sx, sy, SLOT_SIZE, SLOT_SIZE, 2);

        // Stripe rarity di bawah
        g2.setColor(withAlpha(gc, hover ? 230 : 160));
        g2.fillRect(sx + 3, sy + SLOT_SIZE - 6, SLOT_SIZE - 6, 3);

        // Icon mob: idle bobbing + bayangan lantai
        int bob = (int) Math.round(Math.sin(now / 380.0 + i * 1.7) * 2);
        int shW = 40 + bob * 2;
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(sx + (SLOT_SIZE - shW) / 2, sy + 61, shW, 6);

        int box = 46 + (hover ? 6 : 0);
        BufferedImage icon = iconFor(mob);
        if (icon != null) {
            drawIconFit(g2, icon, sx + (SLOT_SIZE - box) / 2, sy + 40 + bob - box / 2, box, box);
        } else {
            g2.setFont(fBold11);
            g2.setColor(new Color(130, 110, 95));
            String shortName = mob.getTypeName().length() > 6 ? mob.getTypeName().substring(0, 6) : mob.getTypeName();
            g2.drawString(shortName, sx + 8, sy + 44);
        }

        // Pips rarity (kiri atas)
        g2.setColor(rc);
        for (int p = 0; p <= tier; p++) {
            g2.fillRect(sx + 6 + p * 6, sy + 6, 4, 4);
        }

        // Tag ACTIVE (tengah atas, berkedip pelan)
        if (isActive) {
            g2.setFont(fBold9);
            FontMetrics afm = g2.getFontMetrics();
            int aa = 180 + (int) (75 * Math.sin(now / 300.0));
            g2.setColor(withAlpha(C_ACTIVE, aa));
            g2.drawString("ACTIVE", sx + (SLOT_SIZE - afm.stringWidth("ACTIVE")) / 2, sy + 14);
        }

        // Spark yang mengitari border (EPIC ke atas, atau pion aktif)
        if (tier >= 2 || isActive) {
            drawOrbitSpark(g2, sx, sy, SLOT_SIZE, SLOT_SIZE, gc, 2600L - Math.max(tier, 1) * 300L, 5);
        }

        // Label: font mengecil otomatis kalau kepanjangan
        drawFitLabel(g2, mob.getTypeName() + " Lv." + mob.level,
                sx + SLOT_SIZE / 2, sy + SLOT_SIZE - 11, SLOT_SIZE - 10,
                hover ? Color.WHITE : new Color(235, 220, 195));

        // Tombol hapus (X) di pojok kanan atas — dikunci kalau pion lagi keluar
        Rectangle del = slotDeleteRect(i);
        drawButton(g2, del, "X", hov(del), !isActive, RED_FILL, RED_DARK, RED_BORDER, RED_ACCENT, fBold12);
    }

    private void drawEmptySlot(Graphics2D g2, Rectangle r, int idx) {
        g2.setColor(new Color(22, 17, 13));
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(new Color(70, 55, 45));
        drawDashedRect(g2, r.x, r.y, r.width, r.height, 6, 4);

        int a = 90 + (int) (30 * Math.sin(now / 700.0 + idx));
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2 - 6;
        g2.setColor(new Color(90, 70, 58, a));
        drawDiamond(g2, cx, cy, 10);
        g2.setColor(new Color(22, 17, 13));
        drawDiamond(g2, cx, cy, 6);

        g2.setFont(fBold10);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(85, 68, 56));
        g2.drawString("EMPTY", cx - fm.stringWidth("EMPTY") / 2, r.y + r.height / 2 + 24);
    }

    private void drawFitLabel(Graphics2D g2, String s, int centerX, int baseY, int maxW, Color c) {
        for (Font f : labelFonts) {
            g2.setFont(f);
            if (g2.getFontMetrics().stringWidth(s) <= maxW) break;
        }
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(s) / 2;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(s, x + 1, baseY + 1);
        g2.setColor(c);
        g2.drawString(s, x, baseY);
    }

    // Info bar di bawah grid: detail singkat saat hover, atau hint berkedip
    private void drawInfoBar(Graphics2D g2, List<CapturedMob> slots, DeathTotem totem) {
        Rectangle bar = infoBarRect();
        g2.setColor(C_SLOT_DK);
        g2.fillRect(bar.x, bar.y, bar.width, bar.height);
        g2.setColor(C_OUTER);
        frameRect(g2, bar.x, bar.y, bar.width, bar.height, 2);

        int baseY = bar.y + 18;

        if (hoveredSlot >= 0 && hoveredSlot < slots.size() && confirmDeleteSlot < 0) {
            CapturedMob m = slots.get(hoveredSlot);
            String rar = rarityOf(m);
            Color rc = getRarityColor(rar);

            g2.setColor(rc);
            g2.fillRect(bar.x + 2, bar.y + 2, 4, bar.height - 4);

            g2.setFont(fBold13);
            FontMetrics bfm = g2.getFontMetrics();
            String name = m.getTypeName().toUpperCase() + " Lv." + m.level;
            g2.setColor(Color.WHITE);
            g2.drawString(name, bar.x + 16, baseY);
            g2.setColor(rc);
            g2.drawString("[" + rar + "]", bar.x + 16 + bfm.stringWidth(name) + 10, baseY);

            String stats = "HP " + m.storedHP + "/" + m.maxHP + "   ATK " + m.getCurrentAttack();
            g2.setColor(new Color(220, 195, 160));
            g2.drawString(stats, bar.x + bar.width - bfm.stringWidth(stats) - 14, baseY);
        } else {
            int a = 130 + (int) (90 * Math.sin(now / 420.0));
            g2.setFont(fBold12);
            FontMetrics bfm = g2.getFontMetrics();
            String hint = "Click a soul to inspect  |  [X] Delete  |  [ESC] Close";
            g2.setColor(withAlpha(new Color(210, 180, 140), a));
            g2.drawString(hint, bar.x + (bar.width - bfm.stringWidth(hint)) / 2, baseY);
        }
    }

    // ── UPGRADE BUTTON ────────────────────────────────────────────────────────

    private void drawUpgradeButton(Graphics2D g2, DeathTotem totem) {
        int[] cost = totem.getNextUpgradeCost();
        Rectangle r = upgradeRect();
        boolean enabled = cost != null;
        boolean hover   = enabled && hov(r);

        Color fill   = enabled ? (hover ? lerp(C_SLOT, C_ACCENT, 0.25f) : C_SLOT) : C_SLOT_DK;
        Color border = enabled ? (hover ? C_ACCENT : C_BORDER) : new Color(85, 70, 58);

        g2.setColor(fill);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(C_SLOT_DK);
        g2.fillRect(r.x + 2, r.y + r.height - 5, r.width - 4, 3);
        g2.setColor(border);
        frameRect(g2, r.x, r.y, r.width, r.height, 2);
        drawCorners(g2, r.x, r.y, r.width, r.height, withAlpha(border, enabled ? 200 : 110));

        int cx = r.x + r.width / 2;

        if (!enabled) {
            g2.setFont(fBold13);
            FontMetrics fm = g2.getFontMetrics();
            String s = "TOTEM MAX CAPACITY (" + totem.getCapacity() + "/" + totem.getMaxCapacity() + ")";
            g2.setColor(new Color(150, 128, 92));
            g2.drawString(s, cx - fm.stringWidth(s) / 2, r.y + r.height / 2 + 5);
            return;
        }

        // Kilau yang lewat di tombol
        Shape oldClip = g2.getClip();
        g2.clipRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        int gx = r.x + (int) (((now % 2800L) / 2800.0) * (r.width + 60)) - 40;
        g2.setColor(withAlpha(C_ACCENT, hover ? 60 : 32));
        g2.fillRect(gx, r.y, 24, r.height);
        g2.setClip(oldClip);

        // Judul
        g2.setFont(fBold16);
        FontMetrics tfm = g2.getFontMetrics();
        String head = "UPGRADE \u2192 " + (totem.getCapacity() + 1) + " slots";
        int hx = cx - tfm.stringWidth(head) / 2;
        g2.setColor(Color.BLACK);
        g2.drawString(head, hx + 2, r.y + 25);
        g2.setColor(hover ? Color.WHITE : C_BORDER);
        g2.drawString(head, hx, r.y + 23);

        // Garis tipis dengan diamond di ujung
        int dy = r.y + 30;
        g2.setColor(withAlpha(C_BORDER, 80));
        g2.fillRect(r.x + 24, dy, r.width - 48, 1);
        g2.setColor(C_ACCENT);
        drawDiamond(g2, r.x + 24, dy, 3);
        drawDiamond(g2, r.x + r.width - 24, dy, 3);

        // Biaya
        g2.setFont(fBold12);
        FontMetrics cfm = g2.getFontMetrics();
        String costStr = cost[0] + "G  " + cost[1] + " Bone  "
                + cost[2] + " Skull  " + cost[3] + " Skel.Key";
        g2.setColor(new Color(210, 180, 140));
        g2.drawString(costStr, cx - cfm.stringWidth(costStr) / 2, r.y + 46);
    }

    // =====================================================================
    //  TAMPILAN 2: DETAIL
    // =====================================================================

    private void drawDetailView(Graphics2D g2, CapturedMob mob, DeathTotem totem) {
        boolean isActive = (totem.getActivePawnSlot() == selectedSlot);
        Rectangle sub = subRect();

        String rarity = rarityOf(mob);
        Color[] th = getRarityThemeColors(rarity);
        Color cOuter    = th[0];
        Color cSub      = th[2];
        Color cSlot     = th[3];
        Color cSlotDark = th[4];
        Color cBorder   = th[5];
        Color cAccent   = getRarityColor(rarity);
        int tier = getRarityTier(rarity);

        // Background sub panel
        g2.setColor(cSub);
        g2.fillRect(sub.x, sub.y, sub.width, sub.height);
        g2.setColor(cOuter);
        g2.drawRect(sub.x, sub.y, sub.width, sub.height);
        drawEmbers(g2, sub, cAccent);

        // Konten meluncur masuk dari kanan + fade
        long t = now - detailStart;
        float slideE = ease(clamp01(t / 320f));
        int dx = Math.round((1f - slideE) * 28f);
        Composite prevComp = g2.getComposite();
        g2.translate(dx, 0);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(slideE * openAlpha)));

        // ---------- 1. Box gambar besar ----------
        int imgSize = 96;
        int imgX = sub.x + 24;
        int imgY = sub.y + 20;
        Color auraC = isActive ? C_ACTIVE : cAccent;

        double pulse = 0.5 + 0.5 * Math.sin(now / 500.0);
        int auraA = Math.min(60, (int) ((8 + 10 * pulse) * (1 + tier * 0.6)));
        if (isActive) auraA = Math.max(auraA, (int) (28 + 24 * pulse));
        for (int g = 4; g >= 1; g--) {
            g2.setColor(withAlpha(auraC, auraA));
            g2.fillRect(imgX - g * 3, imgY - g * 3, imgSize + g * 6, imgSize + g * 6);
        }

        g2.setColor(cSlot);
        g2.fillRect(imgX, imgY, imgSize, imgSize);
        g2.setColor(cSlotDark);
        g2.fillRect(imgX, imgY + imgSize - 4, imgSize, 4);
        g2.fillRect(imgX + imgSize - 4, imgY, 4, imgSize);
        g2.setColor(withAlpha(Color.WHITE, 25));
        g2.fillRect(imgX, imgY, imgSize - 4, 3);
        g2.fillRect(imgX, imgY, 3, imgSize - 4);
        g2.setColor(isActive ? lerp(cBorder, C_ACTIVE, 0.7f) : cBorder);
        frameRect(g2, imgX, imgY, imgSize, imgSize, 2);

        // Mob: bobbing + bayangan
        int bob = (int) Math.round(Math.sin(now / 420.0) * 3);
        int shW = 46 + bob * 2;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(imgX + (imgSize - shW) / 2, imgY + imgSize - 16, shW, 8);

        BufferedImage icon = iconFor(mob);
        if (icon != null) {
            drawIconFit(g2, icon, imgX + 8, imgY + 6 + bob, imgSize - 16, imgSize - 16);
        } else {
            g2.setFont(fPlain13);
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("NO IMAGE", imgX + 20, imgY + (imgSize / 2) + 4);
        }

        // Scanline halus yang turun di dalam box
        int scanY = imgY + 3 + (int) ((now / 28) % (imgSize - 8));
        g2.setColor(withAlpha(auraC, 45));
        g2.fillRect(imgX + 3, scanY, imgSize - 6, 2);

        if (tier >= 1 || isActive) {
            drawOrbitSpark(g2, imgX, imgY, imgSize, imgSize, auraC, 3200L - Math.max(tier, 1) * 400L, 6);
        }

        // ---------- 2. Nama, badge, stat bar ----------
        int infoX = imgX + imgSize + 24;
        int infoRight = sub.x + sub.width - 24;

        g2.setFont(fName);
        String nameStr = mob.getTypeName().toUpperCase();
        g2.setColor(Color.BLACK);
        g2.drawString(nameStr, infoX + 2, imgY + 20);
        g2.setColor(Color.WHITE);
        g2.drawString(nameStr, infoX, imgY + 18);

        // Nomor soul di kanan atas
        g2.setFont(fBold12);
        FontMetrics nfm = g2.getFontMetrics();
        String entryNo = "SOUL #" + (selectedSlot + 1);
        g2.setColor(withAlpha(cBorder, 200));
        g2.drawString(entryNo, infoRight - nfm.stringWidth(entryNo), imgY + 12);

        // Badge rarity
        String rtxt = rarity.toUpperCase();
        int bw = nfm.stringWidth(rtxt) + 16;
        int by = imgY + 26;
        g2.setColor(cSlot);
        g2.fillRect(infoX, by, bw, 16);
        g2.setColor(cAccent);
        frameRect(g2, infoX, by, bw, 16, 1);
        g2.drawString(rtxt, infoX + 8, by + 12);

        // Badge status (ACTIVE / STORED)
        String stxt = isActive ? "ACTIVE" : "STORED";
        int sbw = nfm.stringWidth(stxt) + 16;
        int sbx = infoX + bw + 8;
        Color sc = isActive ? withAlpha(C_ACTIVE, 170 + (int) (85 * Math.sin(now / 300.0))) : cBorder;
        g2.setColor(cSlot);
        g2.fillRect(sbx, by, sbw, 16);
        g2.setColor(sc);
        frameRect(g2, sbx, by, sbw, 16, 1);
        g2.drawString(stxt, sbx + 8, by + 12);

        // Pips rarity berkelap-kelip
        for (int p = 0; p <= tier; p++) {
            int a = 170 + (int) (85 * Math.sin(now / 280.0 + p * 0.9));
            g2.setColor(withAlpha(cAccent, a));
            drawDiamond(g2, sbx + sbw + 16 + p * 14, by + 8, 5);
        }

        // Stat bar animasi (isi naik + angka count-up, tiap bar sedikit delay)
        float a0 = ease(clamp01((t - 250) / 650f));
        float a1 = ease(clamp01((t - 370) / 650f));
        float a2 = ease(clamp01((t - 490) / 650f));
        int barW = 250;

        int maxLv = Math.max(1, mob.getMaxLevel());
        drawStatBar(g2, "LV", Math.round(mob.level * a0) + " / " + maxLv,
                clamp01((float) mob.level / maxLv), a0, infoX, imgY + 48, barW,
                new Color(240, 190, 70), cSlotDark, cBorder);

        double hpFrac = mob.maxHP > 0 ? (double) mob.storedHP / mob.maxHP : 0;
        drawStatBar(g2, "HP", Math.round(mob.storedHP * a1) + " / " + mob.maxHP,
                clamp01((float) hpFrac), a1, infoX, imgY + 66, barW,
                new Color(220, 70, 70), cSlotDark, cBorder);

        int expCurrent = mob.currentExp;
        int expNeeded  = mob.expNeededForNextLevel();
        boolean maxed  = mob.level >= mob.getMaxLevel();
        if (expNeeded > 0 && !maxed) {
            int shownExp = Math.min(expCurrent, expNeeded);
            drawStatBar(g2, "EXP", Math.round(shownExp * a2) + " / " + expNeeded,
                    clamp01((float) shownExp / expNeeded), a2, infoX, imgY + 84, barW,
                    new Color(90, 170, 240), cSlotDark, cBorder);
        } else {
            drawStatBar(g2, "EXP", "MAX", 1f, a2, infoX, imgY + 84, barW,
                    new Color(255, 210, 90), cSlotDark, cBorder);
        }

        // ---------- Divider dengan kilau berjalan ----------
        int divY = sub.y + 128;
        g2.setColor(cOuter);
        g2.fillRect(sub.x + 20, divY, sub.width - 40, 3);
        g2.setColor(cSlot);
        g2.fillRect(sub.x + 20, divY + 3, sub.width - 40, 1);
        int glintX = sub.x + 20 + (int) (((now % 2400L) / 2400.0) * (sub.width - 40 - 24));
        g2.setColor(withAlpha(cAccent, 130));
        g2.fillRect(glintX, divY, 24, 3);

        // ---------- 3. Soul Data sebagai chip ----------
        int descX = sub.x + 24;
        int descW = sub.width - 48;

        g2.setFont(fBold13);
        g2.setColor(cAccent);
        g2.drawString("- Soul Data -", descX, sub.y + 150);

        String[][] data = {
                { "ATK",      String.valueOf(mob.getCurrentAttack()) },
                { "BASE HP",  String.valueOf(mob.baseMaxHP) },
                { "BASE ATK", String.valueOf(mob.baseAttack) },
                { "EXP/LV",   mob.expPerLevel + " x lvl" }
        };

        g2.setFont(fBold12);
        FontMetrics cfm = g2.getFontMetrics();
        Color valC = lerp(cAccent, Color.WHITE, 0.3f);
        int cx = descX;
        int cy = sub.y + 162;
        int chipH = 22;
        for (int i = 0; i < data.length; i++) {
            long appear = 450L + i * 130L;
            if (t < appear) break; // chip muncul satu per satu

            String lbl = data[i][0];
            String val = data[i][1];
            int lw = cfm.stringWidth(lbl);
            int vw = cfm.stringWidth(val);
            int chipW = 12 + lw + 10 + vw + 10;
            if (cx + chipW > descX + descW) {
                cx = descX;
                cy += chipH + 6;
            }

            float pop = ease(clamp01((t - appear) / 220f));
            int oy = Math.round((1f - pop) * 6f);
            int top = cy + oy;

            g2.setColor(cSlotDark);
            g2.fillRect(cx, top, chipW, chipH);
            g2.setColor(withAlpha(valC, 200));
            frameRect(g2, cx, top, chipW, chipH, 1);
            g2.fillRect(cx + 1, top + 1, 3, chipH - 2);

            g2.setColor(Color.WHITE);
            g2.drawString(lbl, cx + 12, top + 15);
            g2.setColor(valC);
            g2.drawString(val, cx + 12 + lw + 10, top + 15);
            cx += chipW + 8;
        }

        // ---------- 4. Status (efek ketik) ----------
        g2.setFont(fBold13);
        g2.setColor(cBorder);
        g2.drawString("- Status -", descX, sub.y + 214);

        String note;
        if (isActive) {
            note = "This soul is currently out of the totem. Recall it before you can delete it.";
        } else if (totem.hasPawnAlive()) {
            note = "Another soul is already out of the totem. Recall it first to spawn this one.";
        } else {
            note = "This soul is stored safely inside the totem. Spawn it to bring it out.";
        }

        g2.setFont(fPlain13);
        FontMetrics lfm = g2.getFontMetrics();
        List<String> lines = wrapText(lfm, note, descW);
        int total = 0;
        for (String ln : lines) total += ln.length();
        int shown = (int) Math.max(0L, (t - 600) / 14);
        int remaining = shown;
        int ly = sub.y + 236;
        int curX = descX;
        int curY = ly;
        g2.setColor(Color.WHITE);
        for (String ln : lines) {
            if (remaining <= 0) break;
            int n = Math.min(ln.length(), remaining);
            String part = ln.substring(0, n);
            g2.drawString(part, descX, ly);
            curX = descX + lfm.stringWidth(part);
            curY = ly;
            remaining -= ln.length();
            ly += 16;
        }
        if (shown < total && (now / 220) % 2 == 0) { // kursor berkedip selama mengetik
            g2.setColor(cAccent);
            g2.fillRect(curX + 2, curY - 10, 7, 12);
        }

        // ---------- 5. Tombol bawah: SPAWN/RECALL, DELETE, BACK ----------
        Rectangle spawn = detailSpawnRect();
        if (isActive) {
            drawButton(g2, spawn, "RECALL (T)", hov(spawn), true,
                    RECALL_FILL, RECALL_DARK, RECALL_BORDER, RECALL_ACCENT, fBold13);
        } else {
            drawButton(g2, spawn, "SPAWN", hov(spawn), !totem.hasPawnAlive(),
                    cSlot, cSlotDark, cBorder, cAccent, fBold13);
        }

        Rectangle del = detailDeleteRect();
        drawButton(g2, del, "DELETE", hov(del), !isActive,
                RED_FILL, RED_DARK, RED_BORDER, RED_ACCENT, fBold13);

        Rectangle back = detailBackRect();
        drawButton(g2, back, "BACK", hov(back), true,
                cSlot, cSlotDark, cBorder, cAccent, fBold13);

        // Kembalikan transform & composite
        g2.translate(-dx, 0);
        g2.setComposite(prevComp);
    }

    // =====================================================================
    //  DIALOG KONFIRMASI HAPUS
    // =====================================================================

    private void drawConfirmDialog(Graphics2D g2, List<CapturedMob> slots) {
        if (confirmDeleteSlot < 0 || confirmDeleteSlot >= slots.size()) {
            confirmDeleteSlot = -1;
            return;
        }
        CapturedMob mob = slots.get(confirmDeleteSlot);

        float e = ease(clamp01((now - confirmStart) / 200f));

        // redupin panel di belakang
        g2.setColor(withAlpha(Color.BLACK, (int) (170 * e)));
        g2.fillRect(panelX + 8, panelY + 8, PANEL_W - 16, PANEL_H - 16);

        Composite prev = g2.getComposite();
        int oy = Math.round((1f - e) * 10f);
        g2.translate(0, oy);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e));

        Rectangle box = confirmBoxRect();

        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(box.x + 6, box.y + 6, box.width, box.height);

        g2.setColor(DLG_OUTER);
        g2.fillRect(box.x, box.y, box.width, box.height);
        g2.setColor(DLG_INNER);
        g2.fillRect(box.x + 5, box.y + 5, box.width - 10, box.height - 10);

        g2.setColor(C_DANGER);
        frameRect(g2, box.x + 7, box.y + 7, box.width - 14, box.height - 14, 2);
        g2.setColor(withAlpha(C_DANGER, 70));
        frameRect(g2, box.x + 12, box.y + 12, box.width - 24, box.height - 24, 1);

        int cornerA = 170 + (int) (85 * Math.sin(now / 400.0));
        drawCorners(g2, box.x, box.y, box.width, box.height, withAlpha(C_DANGER, cornerA));

        int cx = box.x + box.width / 2;

        // Judul + diamond berdenyut
        g2.setFont(fTitle);
        FontMetrics tfm = g2.getFontMetrics();
        String head = "DELETE SOUL?";
        int hw = tfm.stringWidth(head);
        int dA = 150 + (int) (105 * Math.sin(now / 350.0));
        g2.setColor(withAlpha(C_DANGER, dA));
        drawDiamond(g2, cx - hw / 2 - 22, box.y + 33, 5);
        drawDiamond(g2, cx + hw / 2 + 22, box.y + 33, 5);
        g2.setColor(Color.BLACK);
        g2.drawString(head, cx - hw / 2 + 2, box.y + 42);
        g2.setColor(new Color(255, 196, 190));
        g2.drawString(head, cx - hw / 2, box.y + 40);

        g2.setFont(fBold13);
        FontMetrics mfm = g2.getFontMetrics();
        String mobLine = mob.getTypeName().toUpperCase() + " Lv." + mob.level;
        g2.setColor(Color.WHITE);
        g2.drawString(mobLine, cx - mfm.stringWidth(mobLine) / 2, box.y + 70);

        g2.setFont(fPlain13);
        FontMetrics pfm = g2.getFontMetrics();
        String warn = "This soul will be lost forever.";
        g2.setColor(new Color(215, 160, 150));
        g2.drawString(warn, cx - pfm.stringWidth(warn) / 2, box.y + 94);

        Rectangle yes = confirmYesRect();
        Rectangle no  = confirmNoRect();
        drawButton(g2, yes, "YES, DELETE", yes.contains(hoverX, hoverY), true,
                RED_FILL, RED_DARK, RED_BORDER, RED_ACCENT, fBold13);
        drawButton(g2, no, "CANCEL", no.contains(hoverX, hoverY), true,
                C_SLOT, C_SLOT_DK, C_BORDER, C_ACCENT, fBold13);

        g2.setComposite(prev);
        g2.translate(0, -oy);
    }

    // =====================================================================
    //  KOMPONEN UI (disamakan dengan MobIndexUI)
    // =====================================================================

    private void drawStatBar(Graphics2D g2, String label, String valueText, float ratio, float anim,
                             int x, int y, int barW, Color fill, Color cDark, Color cBorder) {
        int barX = x + 62;
        int barH = 12;

        g2.setFont(fBold12);
        g2.setColor(Color.WHITE);
        g2.drawString(label, x, y + 11);

        g2.setColor(cDark);
        g2.fillRect(barX, y, barW, barH);
        g2.setColor(withAlpha(cBorder, 200));
        frameRect(g2, barX, y, barW, barH, 1);

        int innerW = barW - 4;
        int fillW = Math.round(innerW * ratio * anim);
        if (fillW > 0) {
            g2.setColor(fill);
            g2.fillRect(barX + 2, y + 2, fillW, barH - 4);
            g2.setColor(withAlpha(Color.WHITE, 60));
            g2.fillRect(barX + 2, y + 2, fillW, 2);

            // Kilau yang menyapu isi bar
            int shineX = (int) (((now % 2400L) / 2400.0) * (innerW + 20)) - 10;
            if (shineX >= 0 && shineX + 4 <= fillW) {
                g2.setColor(withAlpha(Color.WHITE, 80));
                g2.fillRect(barX + 2 + shineX, y + 2, 4, barH - 4);
            }

            // Segmen pixel
            g2.setColor(withAlpha(cDark, 150));
            for (int sx = 8; sx < fillW; sx += 8) {
                g2.fillRect(barX + 2 + sx, y + 2, 1, barH - 4);
            }
        }

        g2.setFont(fBold12);
        g2.setColor(Color.WHITE);
        g2.drawString(valueText, barX + barW + 10, y + 11);
    }

    private void drawButton(Graphics2D g2, Rectangle r, String text, boolean hover, boolean enabled,
                            Color cFill, Color cDark, Color cBorder, Color cAccent, Font font) {
        Color fill = cFill, dark = cDark, border = cBorder, txt = cBorder;
        boolean h = hover && enabled;
        if (!enabled) {
            fill = OFF_FILL;
            dark = OFF_DARK;
            border = OFF_BORDER;
            txt = OFF_TEXT;
        }

        g2.setColor(h ? lerp(fill, cAccent, 0.30f) : fill);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(dark);
        g2.fillRect(r.x + 2, r.y + r.height - 5, r.width - 4, 3);
        g2.setColor(h ? cAccent : border);
        frameRect(g2, r.x, r.y, r.width, r.height, 2);

        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(h ? Color.WHITE : txt);
        g2.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, ty);
    }

    // Partikel "ember" yang melayang naik (stateless, murni dari waktu)
    private void drawEmbers(Graphics2D g2, Rectangle area, Color c) {
        Shape oldClip = g2.getClip();
        g2.clipRect(area.x + 2, area.y + 2, area.width - 3, area.height - 3);
        for (int i = 0; i < 22; i++) {
            int seed = i * 7919 + 13;
            double speed = 14 + (seed % 17); // px per detik
            double travel = (now / 1000.0) * speed + (seed % 997);
            double life = (travel % area.height) / area.height; // 0..1 (naik)
            int px = area.x + 10 + (seed * 31 % Math.max(1, area.width - 20)) + (int) (Math.sin(now / 900.0 + i) * 6);
            int py = area.y + area.height - (int) (life * area.height);
            int a = (int) (Math.sin(life * Math.PI) * 90);
            int sz = (i % 3 == 0) ? 3 : 2;
            g2.setColor(withAlpha(c, a));
            g2.fillRect(px, py, sz, sz);
        }
        g2.setClip(oldClip);
    }

    // Titik cahaya yang berputar mengelilingi border (dengan ekor)
    private void drawOrbitSpark(Graphics2D g2, int x, int y, int w, int h, Color c, long period, int trail) {
        int perim = 2 * (w + h);
        int head = (int) (((now % period) / (double) period) * perim);
        for (int k = 0; k < trail; k++) {
            int pos = ((head - k * 3) % perim + perim) % perim;
            int px, py;
            if (pos < w) {
                px = x + pos;
                py = y;
            } else if (pos < w + h) {
                px = x + w;
                py = y + (pos - w);
            } else if (pos < 2 * w + h) {
                px = x + w - (pos - w - h);
                py = y + h;
            } else {
                px = x;
                py = y + h - (pos - 2 * w - h);
            }
            int a = Math.max(0, 235 - k * (235 / trail));
            g2.setColor(withAlpha(lerp(c, Color.WHITE, 0.5f), a));
            g2.fillRect(px - 1, py - 1, 3, 3);
        }
    }

    private void drawCorners(Graphics2D g2, int x, int y, int w, int h, Color c) {
        int len = 14, t = 3, o = 3;
        g2.setColor(c);
        g2.fillRect(x + o, y + o, len, t);
        g2.fillRect(x + o, y + o, t, len);
        g2.fillRect(x + w - o - len, y + o, len, t);
        g2.fillRect(x + w - o - t, y + o, t, len);
        g2.fillRect(x + o, y + h - o - t, len, t);
        g2.fillRect(x + o, y + h - o - len, t, len);
        g2.fillRect(x + w - o - len, y + h - o - t, len, t);
        g2.fillRect(x + w - o - t, y + h - o - len, t, len);
    }

    private void drawDiamond(Graphics2D g2, int cx, int cy, int r) {
        g2.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
    }

    // Border kotak pakai fillRect -> tidak tergantung ketebalan stroke
    private void frameRect(Graphics2D g2, int x, int y, int w, int h, int th) {
        g2.fillRect(x, y, w, th);
        g2.fillRect(x, y + h - th, w, th);
        g2.fillRect(x, y, th, h);
        g2.fillRect(x + w - th, y, th, h);
    }

    private void drawDashedRect(Graphics2D g2, int x, int y, int w, int h, int dash, int gap) {
        for (int dx = 0; dx < w; dx += dash + gap) {
            int len = Math.min(dash, w - dx);
            g2.fillRect(x + dx, y, len, 2);
            g2.fillRect(x + dx, y + h - 2, len, 2);
        }
        for (int dy = 0; dy < h; dy += dash + gap) {
            int len = Math.min(dash, h - dy);
            g2.fillRect(x, y + dy, 2, len);
            g2.fillRect(x + w - 2, y + dy, 2, len);
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
        switch (mob.type) {
            case GOLEM:    return golemIcon;
            case GOLEMITE: return golemiteIcon;
            default:       return skeletonIcon;
        }
    }

    // =====================================================================
    //  UTIL
    // =====================================================================

    private List<String> wrapText(FontMetrics fm, String text, int wrapWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) <= wrapWidth) {
                line = new StringBuilder(test);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private Color lerp(Color a, Color b, float t) {
        t = clamp01(t);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private float ease(float t) { // ease-out cubic
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    // =====================================================================
    //  INPUT
    // =====================================================================

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

        // tombol silang di pojok kanan atas panel: tutup Totem UI
        if (CloseButton.rect(panelX, panelY, PANEL_W).contains(mx, my)) {
            hide();
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
                detailStart  = System.currentTimeMillis(); // restart animasi detail
                hoveredSlot  = -1;
                gp.playSE("select");
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
            hoveredSlot  = -1;
            gp.playSE("cursor");
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
            hoveredSlot       = -1;
        } else if (confirmNoRect().contains(mx, my)) {
            confirmDeleteSlot = -1;
            gp.playSE("cursor");
        }
    }

    // Minta hapus slot: pion aktif ditolak (manager kasih pesan), sisanya konfirmasi dulu
    private void requestDelete(DeathTotem totem, int slot) {
        if (totem.getActivePawnSlot() == slot) {
            gp.deathTotemManager.tryDeleteSlot(slot);
            return;
        }
        confirmDeleteSlot = slot;
        confirmStart      = System.currentTimeMillis();
        hoveredSlot       = -1;
        gp.playSE("cursor");
    }

    public void handleMouseMove(MouseEvent e) {
        hoverX = toGameX(e.getX());
        hoverY = toGameY(e.getY());

        int prev = hoveredSlot;
        hoveredSlot = -1;

        if (!visible || detailMode || confirmDeleteSlot >= 0) return;
        if (gp.deathTotemManager == null) return;
        DeathTotem totem = gp.deathTotemManager.getTotem();
        if (totem == null) return;

        int n = totem.getSlots().size();
        for (int i = 0; i < n; i++) {
            if (slotRect(i).contains(hoverX, hoverY)) {
                hoveredSlot = i;
                break;
            }
        }
        if (hoveredSlot != -1 && hoveredSlot != prev) gp.playSE("cursor");
    }

    public boolean handleEsc() {
        if (!visible) return false;
        if (confirmDeleteSlot >= 0) {
            confirmDeleteSlot = -1;
        } else if (detailMode) {
            detailMode   = false;
            selectedSlot = -1;
            hoveredSlot  = -1;
        } else {
            hide();
        }
        return true;
    }
}