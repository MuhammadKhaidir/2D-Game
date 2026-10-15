package UI;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import Main.GamePanel;

public class MobIndexUI {
    public boolean visible = false;
    private boolean detailMode = false; // false = Grid View, true = Detail View
    private int selectedMobIndex = 0;

    // State hover (diisi lewat handleMouseMove)
    private int hoveredSlot = -1;
    private int hoverButton = 0; // 0 = none, 1 = BACK, 2 = PREV, 3 = NEXT

    // State animasi
    private long now = 0;
    private long openStart = 0;
    private long detailStart = 0;
    private boolean wasVisible = false;
    private float openAlpha = 1f;

    private final int panelW = 640;
    private final int panelH = 440;
    private static final int PANEL_Y = 60;

    // Skema warna default (Grid View) disamakan dengan BackpackUI
    private Color colorBgOuterDefault   = new Color(45, 30, 20);
    private Color colorBgInnerDefault   = new Color(85, 65, 50);
    private Color colorSubPanelDefault  = new Color(35, 25, 20);
    private Color colorSlotDefault      = new Color(60, 45, 35);
    private Color colorSlotDarkDefault  = new Color(25, 20, 15);
    private Color colorBorderDefault    = new Color(220, 190, 150);
    private Color colorAccentDefault    = new Color(250, 180, 40);

    // Font di-cache supaya tidak bikin object baru tiap frame
    private final Font fTitle  = new Font("Monospaced", Font.BOLD, 20);
    private final Font fName   = new Font("Monospaced", Font.BOLD, 22);
    private final Font fBig    = new Font("Monospaced", Font.BOLD, 30);
    private final Font fBold13 = new Font("Monospaced", Font.BOLD, 13);
    private final Font fPlain13 = new Font("Monospaced", Font.PLAIN, 13);
    private final Font fBold12 = new Font("Monospaced", Font.BOLD, 12);
    private final Font fBold11 = new Font("Monospaced", Font.BOLD, 11);
    private final Font fBold10 = new Font("Monospaced", Font.BOLD, 10);

    // Data Mob internal
    private List<MobData> mobList = new ArrayList<>();

    // Grid Configuration
    private final int maxCols = 6;
    private final int slotSize = 76;
    private final int slotPadding = 14;
    private final int rowStride = 76 + 40;  // tinggi slot + ruang label nama
    private final int totalSlots = 12;      // jumlah kotak (sisa yang kosong tampil sebagai "???")

    // Inner class untuk menampung data blueprint mob
    public static class MobData {
        public String name;
        public String description;
        public int hp;
        public int dmg;
        public double speed;
        public String drops;
        public String rarity;
        public Image icon;
        public final List<String[]> dropList; // hasil parse "drops" -> {nama, persen}

        public MobData(String name, String rarity, String description, int hp, int dmg, double speed, String drops, Image icon) {
            this.name = name;
            this.rarity = rarity;
            this.description = description;
            this.hp = hp;
            this.dmg = dmg;
            this.speed = speed;
            this.drops = drops;
            this.icon = icon;
            this.dropList = parseDrops(drops);
        }

        // "Bone (15%), Skull (5%), Skeleton Key (0,5%)" -> [{Bone,15%}, {Skull,5%}, {Skeleton Key,0,5%}]
        private static List<String[]> parseDrops(String raw) {
            List<String[]> out = new ArrayList<>();
            if (raw == null || !raw.contains("(")) return out;
            for (String part : raw.split("(?<=\\)),\\s*")) {
                int p = part.lastIndexOf('(');
                if (p < 0) {
                    out.add(new String[]{part.trim(), ""});
                    continue;
                }
                String name = part.substring(0, p).trim();
                String pct = part.substring(p + 1).replace(")", "").trim();
                out.add(new String[]{name, pct});
            }
            return out;
        }
    }

    public MobIndexUI(GamePanel gp) {
        mobList.add(new MobData("SKELETON", "COMMON", "A reanimated undead warrior bound by dark residual magic. It wanders dark tomb dungeons clutching brittle weapons, can be tamed.", 400, 15, 1.5, "Bone (15%), Skull (5%), Skeleton Key (0,5%), Death Totem (0,1%), Coin (100%)", gp.setup("SkeletonFrontWalk (1)")));
        mobList.add(new MobData("SLIME", "COMMON", "A bouncy, gelatinous creature. Mildly aggressive but dangerously sticky. Drops chemically reactive gel.", 50, 5, 2, "Coin (100%)", gp.setup("Sticklooter_carrot_idle3")));
        mobList.add(new MobData("STONE GOLEM", "EPIC", "A massive, powerful construct carved from living solid stone. Awakens from deep underground to crush intruders, can be tamed.", 5000, 200, 1, "Coin (100%)", gp.setup("GolemFrontWalk (6)")));
    }

    // =====================================================================
    //  RARITY HELPERS
    // =====================================================================

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
                theme[0] = colorBgOuterDefault;
                theme[1] = colorBgInnerDefault;
                theme[2] = colorSubPanelDefault;
                theme[3] = colorSlotDefault;
                theme[4] = colorSlotDarkDefault;
                theme[5] = colorBorderDefault;
                break;
        }
        return theme;
    }

    // =====================================================================
    //  GEOMETRY (dipakai bareng oleh draw, click, dan hover)
    // =====================================================================

    private Rectangle getSubRect(int panelX, int panelY) {
        return new Rectangle(panelX + 24, panelY + 75, panelW - 48, panelH - 100);
    }

    private Rectangle getSlotRect(int i, Rectangle sub) {
        int totalGridW = (maxCols * slotSize) + ((maxCols - 1) * slotPadding);
        int gridStartX = sub.x + (sub.width - totalGridW) / 2;
        int gridStartY = sub.y + 36;
        int col = i % maxCols;
        int row = i / maxCols;
        return new Rectangle(gridStartX + col * (slotSize + slotPadding), gridStartY + row * rowStride, slotSize, slotSize);
    }

    private Rectangle getBackRect(Rectangle sub) {
        return new Rectangle(sub.x + sub.width - 90 - 20, sub.y + sub.height - 30 - 14, 90, 30);
    }

    private Rectangle getPrevRect(Rectangle sub) {
        return new Rectangle(sub.x + 20, sub.y + sub.height - 30 - 14, 30, 30);
    }

    private Rectangle getNextRect(Rectangle sub) {
        return new Rectangle(sub.x + 20 + 90, sub.y + sub.height - 30 - 14, 30, 30);
    }

    // =====================================================================
    //  DRAW
    // =====================================================================

    public void draw(Graphics2D g2, int screenW, int screenH, GamePanel gp) {
        if (!visible) {
            wasVisible = false;
            return;
        }

        now = System.currentTimeMillis();
        if (!wasVisible) { // baru dibuka -> mulai animasi buka panel
            wasVisible = true;
            openStart = now;
            detailStart = now;
            hoveredSlot = -1;
            hoverButton = 0;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        Stroke oldStroke = g2.getStroke();
        Composite oldComp = g2.getComposite();

        // Animasi buka: panel meluncur dari atas + fade in
        float openE = ease(clamp01((now - openStart) / 260f));
        openAlpha = openE;
        int slideY = Math.round((1f - openE) * -18f);
        g2.translate(0, slideY);
        if (openE < 1f) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, openE));

        int panelX = screenW / 2 - panelW / 2;
        int panelY = PANEL_Y;

        // Tentukan skema warna aktif berdasarkan mode
        Color currentBgOuter, currentBgInner, currentBorder, currentAccent;
        if (detailMode && !mobList.isEmpty()) {
            MobData activeMob = mobList.get(selectedMobIndex);
            Color[] theme = getRarityThemeColors(activeMob.rarity);
            currentBgOuter = theme[0];
            currentBgInner = theme[1];
            currentBorder  = theme[5];
            currentAccent  = getRarityColor(activeMob.rarity);
        } else {
            currentBgOuter = colorBgOuterDefault;
            currentBgInner = colorBgInnerDefault;
            currentBorder  = colorBorderDefault;
            currentAccent  = colorAccentDefault;
        }

        // Shadow Overlay Belakang
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(panelX + 8, panelY + 8, panelW, panelH);

        // Frame Utama
        g2.setColor(currentBgOuter);
        g2.fillRect(panelX, panelY, panelW, panelH);

        g2.setColor(currentBgInner);
        g2.fillRect(panelX + 6, panelY + 6, panelW - 12, panelH - 12);

        g2.setStroke(new BasicStroke(2));
        g2.setColor(currentBorder);
        g2.drawRect(panelX + 8, panelY + 8, panelW - 16, panelH - 16);

        // Garis dalam tipis (bingkai ganda)
        g2.setColor(withAlpha(currentBorder, 70));
        frameRect(g2, panelX + 13, panelY + 13, panelW - 26, panelH - 26, 1);

        // Ornamen sudut yang berdenyut
        int cornerA = 170 + (int) (85 * Math.sin(now / 500.0));
        drawCorners(g2, panelX, panelY, panelW, panelH, withAlpha(currentAccent, cornerA));

        // Header Title
        String title = "BESTIARY / MOB INDEX";
        g2.setFont(fTitle);
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;

        g2.setColor(currentBgOuter);
        g2.fillRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(currentBorder);
        g2.drawRect(titleX - 20, panelY + 18, titleW + 40, 30);

        // Diamond kiri-kanan judul
        int dA = 150 + (int) (105 * Math.sin(now / 350.0));
        g2.setColor(withAlpha(currentAccent, dA));
        drawDiamond(g2, titleX - 38, panelY + 33, 5);
        drawDiamond(g2, titleX + titleW + 38, panelY + 33, 5);

        g2.setColor(Color.BLACK);
        g2.drawString(title, titleX + 2, panelY + 40);
        g2.setColor(currentAccent);
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

        // PERCABANGAN TAMPILAN
        if (!detailMode) {
            drawGridView(g2, panelX, panelY);
        } else {
            drawDetailView(g2, panelX, panelY);
        }

        // Restore state graphics
        g2.translate(0, -slideY);
        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
    }

    // =====================================================================
    //  TAMPILAN 1: GRID
    // =====================================================================

    private void drawGridView(Graphics2D g2, int panelX, int panelY) {
        Rectangle sub = getSubRect(panelX, panelY);

        g2.setColor(colorSubPanelDefault);
        g2.fillRect(sub.x, sub.y, sub.width, sub.height);
        g2.setColor(colorBgOuterDefault);
        g2.drawRect(sub.x, sub.y, sub.width, sub.height);

        drawEmbers(g2, sub, colorAccentDefault);

        // Header kecil
        g2.setFont(fBold11);
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(new Color(150, 125, 105));
        g2.drawString("- SELECT ENTRY -", sub.x + 16, sub.y + 22);
        String disc = "DISCOVERED " + mobList.size() + " / " + totalSlots;
        g2.drawString(disc, sub.x + sub.width - hfm.stringWidth(disc) - 16, sub.y + 22);

        int slotCount = Math.max(totalSlots, mobList.size());

        for (int i = 0; i < slotCount; i++) {
            Rectangle r = getSlotRect(i, sub);

            // Slot terkunci (belum ada mob)
            if (i >= mobList.size()) {
                drawLockedSlot(g2, r.x, r.y, i);
                continue;
            }

            MobData mob = mobList.get(i);
            Color rc = getRarityColor(mob.rarity);
            int tier = getRarityTier(mob.rarity);
            boolean hover = (i == hoveredSlot);
            double pulse = 0.5 + 0.5 * Math.sin(now / 450.0 + i * 1.3);

            int sx = r.x;
            int sy = r.y + (hover ? -4 : 0); // slot naik sedikit saat di-hover

            // Glow di belakang slot (berdenyut untuk rarity tinggi)
            if (tier >= 1 || hover) {
                int a = hover ? 110 : (int) ((25 + 35 * pulse) * tier);
                int la = Math.min(255, a / 3);
                for (int g = 3; g >= 1; g--) {
                    g2.setColor(withAlpha(rc, la));
                    g2.fillRect(sx - g * 2, sy - g * 2, slotSize + g * 4, slotSize + g * 4);
                }
            }

            // Dasar slot + tint warna rarity
            g2.setColor(colorSlotDarkDefault);
            g2.fillRect(sx, sy, slotSize, slotSize);
            g2.setColor(withAlpha(rc, hover ? 55 : 26));
            g2.fillRect(sx + 2, sy + 2, slotSize - 4, slotSize - 4);

            // Bevel pixel-art
            g2.setColor(withAlpha(Color.WHITE, 22));
            g2.fillRect(sx + 2, sy + 2, slotSize - 4, 2);
            g2.fillRect(sx + 2, sy + 2, 2, slotSize - 4);
            g2.setColor(withAlpha(Color.BLACK, 90));
            g2.fillRect(sx + 2, sy + slotSize - 4, slotSize - 4, 2);
            g2.fillRect(sx + slotSize - 4, sy + 2, 2, slotSize - 4);

            // Border
            g2.setColor(hover ? lerp(rc, Color.WHITE, 0.35f) : lerp(colorBgOuterDefault, rc, 0.6f));
            frameRect(g2, sx, sy, slotSize, slotSize, 2);

            // Stripe rarity di bawah
            g2.setColor(withAlpha(rc, hover ? 230 : 160));
            g2.fillRect(sx + 3, sy + slotSize - 6, slotSize - 6, 3);

            // Icon mob: idle bobbing + bayangan lantai
            int bob = (int) Math.round(Math.sin(now / 380.0 + i * 1.7) * 2);
            int iconSize = slotSize - 16 + (hover ? 6 : 0);
            int ix = sx + (slotSize - iconSize) / 2;
            int iy = sy + (slotSize - iconSize) / 2 + 2 + bob;

            int shW = slotSize - 36 + bob * 2;
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(sx + (slotSize - shW) / 2, sy + slotSize - 15, shW, 6);

            if (mob.icon != null) {
                g2.drawImage(mob.icon, ix, iy, iconSize, iconSize, null);
            } else {
                g2.setFont(fBold11);
                g2.setColor(new Color(130, 110, 95));
                String shortName = mob.name.length() > 6 ? mob.name.substring(0, 6) : mob.name;
                g2.drawString(shortName, sx + 8, sy + (slotSize / 2) + 4);
            }

            // Pips rarity (kiri atas) + nomor entry (kanan atas)
            g2.setColor(rc);
            for (int p = 0; p <= tier; p++) {
                g2.fillRect(sx + 6 + p * 6, sy + 6, 4, 4);
            }
            g2.setFont(fBold10);
            FontMetrics nfm = g2.getFontMetrics();
            String no = String.format("%02d", i + 1);
            g2.setColor(new Color(150, 130, 115));
            g2.drawString(no, sx + slotSize - nfm.stringWidth(no) - 6, sy + 14);

            // Spark yang mengitari border untuk EPIC ke atas
            if (tier >= 2) {
                drawOrbitSpark(g2, sx, sy, slotSize, slotSize, rc, 2600L - tier * 300L, 5);
            }

            // Label nama di bawah slot
            g2.setFont(fBold10);
            FontMetrics lfm = g2.getFontMetrics();
            String label = mob.name;
            while (label.length() > 3 && lfm.stringWidth(label) > slotSize + 8) {
                label = label.substring(0, label.length() - 1);
            }
            g2.setColor(hover ? Color.WHITE : rc);
            g2.drawString(label, r.x + (slotSize - lfm.stringWidth(label)) / 2, r.y + slotSize + 16);
        }

        // Info bar di bawah: detail singkat saat hover, atau hint berkedip
        int barX = sub.x + 16;
        int barY = sub.y + sub.height - 52;
        int barW = sub.width - 32;
        int barH = 36;
        g2.setColor(colorSlotDarkDefault);
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(colorBgOuterDefault);
        frameRect(g2, barX, barY, barW, barH, 2);

        if (hoveredSlot >= 0 && hoveredSlot < mobList.size()) {
            MobData m = mobList.get(hoveredSlot);
            Color rc = getRarityColor(m.rarity);
            g2.setColor(rc);
            g2.fillRect(barX + 2, barY + 2, 4, barH - 4);

            g2.setFont(fBold13);
            FontMetrics bfm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(m.name, barX + 16, barY + 23);
            g2.setColor(rc);
            g2.drawString("[" + m.rarity + "]", barX + 16 + bfm.stringWidth(m.name) + 10, barY + 23);

            String stats = "HP " + m.hp + "   DMG " + m.dmg + "   SPD " + m.speed;
            g2.setColor(new Color(220, 195, 160));
            g2.drawString(stats, barX + barW - bfm.stringWidth(stats) - 14, barY + 23);
        } else {
            int a = 130 + (int) (90 * Math.sin(now / 420.0));
            g2.setFont(fBold12);
            FontMetrics bfm = g2.getFontMetrics();
            String hint = "Click a monster to inspect its data";
            g2.setColor(withAlpha(new Color(210, 180, 140), a));
            g2.drawString(hint, barX + (barW - bfm.stringWidth(hint)) / 2, barY + 23);
        }
    }

    private void drawLockedSlot(Graphics2D g2, int x, int y, int idx) {
        g2.setColor(new Color(22, 17, 13));
        g2.fillRect(x, y, slotSize, slotSize);
        g2.setColor(new Color(70, 55, 45));
        drawDashedRect(g2, x, y, slotSize, slotSize, 6, 4);

        int a = 90 + (int) (30 * Math.sin(now / 700.0 + idx));
        g2.setFont(fBig);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(90, 70, 58, a));
        g2.drawString("?", x + (slotSize - fm.stringWidth("?")) / 2, y + slotSize / 2 + 10);

        g2.setFont(fBold10);
        fm = g2.getFontMetrics();
        g2.setColor(new Color(85, 68, 56));
        g2.drawString("???", x + (slotSize - fm.stringWidth("???")) / 2, y + slotSize + 16);
    }

    // =====================================================================
    //  TAMPILAN 2: DETAIL
    // =====================================================================

    private void drawDetailView(Graphics2D g2, int panelX, int panelY) {
        if (mobList.isEmpty()) return;

        Rectangle sub = getSubRect(panelX, panelY);
        MobData mob = mobList.get(selectedMobIndex);

        Color[] th = getRarityThemeColors(mob.rarity);
        Color cOuter = th[0];
        Color cSub = th[2];
        Color cSlot = th[3];
        Color cSlotDark = th[4];
        Color cBorder = th[5];
        Color cAccent = getRarityColor(mob.rarity);
        int tier = getRarityTier(mob.rarity);

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

        // Nilai maksimum buat skala bar
        double maxHp = 1, maxDmg = 1, maxSpd = 1;
        for (MobData m : mobList) {
            maxHp = Math.max(maxHp, m.hp);
            maxDmg = Math.max(maxDmg, m.dmg);
            maxSpd = Math.max(maxSpd, m.speed);
        }

        // ---------- 1. Box gambar besar ----------
        int imgSize = 96;
        int imgX = sub.x + 24;
        int imgY = sub.y + 20;

        // Aura glow di belakang box
        double pulse = 0.5 + 0.5 * Math.sin(now / 500.0);
        int auraA = Math.min(60, (int) ((8 + 10 * pulse) * (1 + tier * 0.6)));
        for (int g = 4; g >= 1; g--) {
            g2.setColor(withAlpha(cAccent, auraA));
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
        g2.setColor(cBorder);
        frameRect(g2, imgX, imgY, imgSize, imgSize, 2);

        // Mob: bobbing + bayangan
        int bob = (int) Math.round(Math.sin(now / 420.0) * 3);
        int shW = 46 + bob * 2;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(imgX + (imgSize - shW) / 2, imgY + imgSize - 16, shW, 8);

        if (mob.icon != null) {
            g2.drawImage(mob.icon, imgX + 8, imgY + 6 + bob, imgSize - 16, imgSize - 16, null);
        } else {
            g2.setFont(fPlain13);
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("NO IMAGE", imgX + 20, imgY + (imgSize / 2) + 4);
        }

        // Scanline halus yang turun di dalam box
        int scanY = imgY + 3 + (int) ((now / 28) % (imgSize - 8));
        g2.setColor(withAlpha(cAccent, 45));
        g2.fillRect(imgX + 3, scanY, imgSize - 6, 2);

        if (tier >= 1) {
            drawOrbitSpark(g2, imgX, imgY, imgSize, imgSize, cAccent, 3200L - tier * 400L, 6);
        }

        // ---------- 2. Nama, rarity, stat bar ----------
        int infoX = imgX + imgSize + 24;
        int infoRight = sub.x + sub.width - 24;

        g2.setFont(fName);
        g2.setColor(Color.BLACK);
        g2.drawString(mob.name, infoX + 2, imgY + 20);
        g2.setColor(Color.WHITE);
        g2.drawString(mob.name, infoX, imgY + 18);

        // Nomor entry di kanan atas
        g2.setFont(fBold12);
        FontMetrics nfm = g2.getFontMetrics();
        String entryNo = "No. " + String.format("%02d", selectedMobIndex + 1);
        g2.setColor(withAlpha(cBorder, 200));
        g2.drawString(entryNo, infoRight - nfm.stringWidth(entryNo), imgY + 12);

        // Badge rarity + pips berkelap-kelip
        String rtxt = mob.rarity.toUpperCase();
        int bw = nfm.stringWidth(rtxt) + 16;
        int by = imgY + 26;
        g2.setColor(cSlot);
        g2.fillRect(infoX, by, bw, 16);
        g2.setColor(cAccent);
        frameRect(g2, infoX, by, bw, 16, 1);
        g2.drawString(rtxt, infoX + 8, by + 12);
        for (int p = 0; p <= tier; p++) {
            int a = 170 + (int) (85 * Math.sin(now / 280.0 + p * 0.9));
            g2.setColor(withAlpha(cAccent, a));
            drawDiamond(g2, infoX + bw + 16 + p * 14, by + 8, 5);
        }

        // Stat bar animasi (isi naik + angka count-up, tiap bar sedikit delay)
        float a0 = ease(clamp01((t - 250) / 650f));
        float a1 = ease(clamp01((t - 370) / 650f));
        float a2 = ease(clamp01((t - 490) / 650f));
        int barW = 250;

        drawStatBar(g2, "HP", String.valueOf(Math.round(mob.hp * a0)),
                statRatioSqrt(mob.hp, maxHp), a0, infoX, imgY + 48, barW,
                new Color(220, 70, 70), cSlotDark, cBorder);
        drawStatBar(g2, "DMG", String.valueOf(Math.round(mob.dmg * a1)),
                statRatioSqrt(mob.dmg, maxDmg), a1, infoX, imgY + 66, barW,
                new Color(240, 150, 50), cSlotDark, cBorder);
        drawStatBar(g2, "SPEED", String.format(Locale.US, "%.1f", mob.speed * a2),
                (float) (mob.speed / maxSpd), a2, infoX, imgY + 84, barW,
                new Color(80, 200, 230), cSlotDark, cBorder);

        // ---------- Divider dengan kilau berjalan ----------
        int divY = sub.y + 128;
        g2.setColor(cOuter);
        g2.fillRect(sub.x + 20, divY, sub.width - 40, 3);
        g2.setColor(cSlot);
        g2.fillRect(sub.x + 20, divY + 3, sub.width - 40, 1);
        int glintX = sub.x + 20 + (int) (((now % 2400L) / 2400.0) * (sub.width - 40 - 24));
        g2.setColor(withAlpha(cAccent, 130));
        g2.fillRect(glintX, divY, 24, 3);

        // ---------- 3. Lore (efek ketik) ----------
        int descX = sub.x + 24;
        int descY = sub.y + 150;
        int descW = sub.width - 48;

        g2.setFont(fBold13);
        g2.setColor(cBorder);
        g2.drawString("- Lore & Behavior -", descX, descY);

        g2.setFont(fPlain13);
        FontMetrics lfm = g2.getFontMetrics();
        List<String> lines = wrapText(lfm, mob.description, descW);
        int total = 0;
        for (String ln : lines) total += ln.length();
        int shown = (int) Math.max(0L, (t - 350) / 14);
        int remaining = shown;
        int ly = descY + 22;
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

        // ---------- 4. Drops sebagai chip ----------
        int dropsY = sub.y + 226;
        g2.setFont(fBold13);
        g2.setColor(cAccent);
        g2.drawString("- Potential Drops -", descX, dropsY);

        g2.setFont(fBold12);
        FontMetrics cfm = g2.getFontMetrics();

        if (mob.dropList.isEmpty()) {
            g2.setColor(new Color(150, 150, 150));
            String none = (mob.drops == null || mob.drops.equalsIgnoreCase("none")) ? "No drops." : mob.drops;
            g2.drawString(none, descX, dropsY + 22);
        } else {
            int cx = descX;
            int cy = sub.y + 238;
            int chipH = 22;
            for (int i = 0; i < mob.dropList.size(); i++) {
                long appear = 450L + i * 130L;
                if (t < appear) break; // chip muncul satu per satu

                String[] d = mob.dropList.get(i);
                int nameW = cfm.stringWidth(d[0]);
                int pctW = d[1].isEmpty() ? 0 : cfm.stringWidth(d[1]);
                int chipW = 12 + nameW + (pctW > 0 ? 10 + pctW : 0) + 10;
                if (cx + chipW > descX + descW) {
                    cx = descX;
                    cy += chipH + 6;
                }

                float pop = ease(clamp01((t - appear) / 220f));
                int oy = Math.round((1f - pop) * 6f);
                Color dc = dropColor(d[1]);
                int top = cy + oy;

                g2.setColor(cSlotDark);
                g2.fillRect(cx, top, chipW, chipH);
                boolean ultraRare = isUltraRare(d[1]);
                int borderA = ultraRare ? 170 + (int) (85 * Math.sin(now / 260.0 + i)) : 200;
                g2.setColor(withAlpha(dc, borderA));
                frameRect(g2, cx, top, chipW, chipH, 1);
                g2.fillRect(cx + 1, top + 1, 3, chipH - 2);

                g2.setColor(Color.WHITE);
                g2.drawString(d[0], cx + 12, top + 15);
                if (pctW > 0) {
                    g2.setColor(dc);
                    g2.drawString(d[1], cx + 12 + nameW + 10, top + 15);
                }
                cx += chipW + 8;
            }
        }

        // ---------- 5. Tombol bawah: PREV / NEXT / BACK ----------
        Rectangle back = getBackRect(sub);
        Rectangle prev = getPrevRect(sub);
        Rectangle next = getNextRect(sub);

        drawButton(g2, back, "BACK", hoverButton == 1, cSlot, cSlotDark, cBorder, cAccent);
        drawArrowButton(g2, prev, true, hoverButton == 2, cSlot, cSlotDark, cBorder, cAccent);
        drawArrowButton(g2, next, false, hoverButton == 3, cSlot, cSlotDark, cBorder, cAccent);

        g2.setFont(fBold13);
        FontMetrics pfm = g2.getFontMetrics();
        String counter = (selectedMobIndex + 1) + " / " + mobList.size();
        int midX = prev.x + prev.width;
        int midW = next.x - midX;
        g2.setColor(Color.WHITE);
        g2.drawString(counter, midX + (midW - pfm.stringWidth(counter)) / 2, prev.y + 20);

        // Kembalikan transform & composite
        g2.translate(-dx, 0);
        g2.setComposite(prevComp);
    }

    // =====================================================================
    //  KOMPONEN UI
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

    private void drawButton(Graphics2D g2, Rectangle r, String text, boolean hover,
                            Color cSlot, Color cDark, Color cBorder, Color cAccent) {
        g2.setColor(hover ? lerp(cSlot, cAccent, 0.30f) : cSlot);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(cDark);
        g2.fillRect(r.x + 2, r.y + r.height - 5, r.width - 4, 3);
        g2.setColor(hover ? cAccent : cBorder);
        frameRect(g2, r.x, r.y, r.width, r.height, 2);

        g2.setFont(fBold13);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(hover ? Color.WHITE : cBorder);
        g2.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + 19);
    }

    private void drawArrowButton(Graphics2D g2, Rectangle r, boolean left, boolean hover,
                                 Color cSlot, Color cDark, Color cBorder, Color cAccent) {
        g2.setColor(hover ? lerp(cSlot, cAccent, 0.30f) : cSlot);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.setColor(cDark);
        g2.fillRect(r.x + 2, r.y + r.height - 5, r.width - 4, 3);
        g2.setColor(hover ? cAccent : cBorder);
        frameRect(g2, r.x, r.y, r.width, r.height, 2);

        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2 - 1;
        int nudge = hover ? (left ? -1 : 1) : 0; // panah sedikit bergeser saat hover
        g2.setColor(hover ? Color.WHITE : cBorder);
        if (left) {
            g2.fillPolygon(new int[]{cx + 4 + nudge, cx - 5 + nudge, cx + 4 + nudge},
                           new int[]{cy - 6, cy, cy + 6}, 3);
        } else {
            g2.fillPolygon(new int[]{cx - 4 + nudge, cx + 5 + nudge, cx - 4 + nudge},
                           new int[]{cy - 6, cy, cy + 6}, 3);
        }
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

    // Skala akar supaya mob lemah (misal HP 50) tetap kelihatan di bar
    private float statRatioSqrt(double v, double max) {
        if (max <= 0) return 0f;
        return (float) Math.max(0.04, Math.sqrt(v / max));
    }

    private double parsePct(String pct) {
        try {
            return Double.parseDouble(pct.replace("%", "").replace(",", ".").trim());
        } catch (Exception e) {
            return 100;
        }
    }

    private boolean isUltraRare(String pct) {
        return parsePct(pct) < 1;
    }

    private Color dropColor(String pct) {
        double v = parsePct(pct);
        if (v < 1)  return new Color(255, 200, 60);   // sangat langka (emas)
        if (v < 10) return new Color(120, 200, 255);  // langka (biru)
        return new Color(200, 200, 200);              // umum (abu)
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

    // Panggil dari mouseMoved milik listener kamu supaya efek hover aktif
    public void handleMouseMove(MouseEvent e, GamePanel gp) {
        int prevSlot = hoveredSlot;
        hoveredSlot = -1;
        hoverButton = 0;
        if (!visible) return;

        int mx = e.getX();
        int my = e.getY();
        Rectangle sub = getSubRect(gp.screenWidth / 2 - panelW / 2, PANEL_Y);

        if (!detailMode) {
            for (int i = 0; i < mobList.size(); i++) {
                if (getSlotRect(i, sub).contains(mx, my)) {
                    hoveredSlot = i;
                    break;
                }
            }
            if (hoveredSlot != -1 && hoveredSlot != prevSlot && gp != null) gp.playSE("cursor");
        } else {
            if (getBackRect(sub).contains(mx, my)) hoverButton = 1;
            else if (getPrevRect(sub).contains(mx, my)) hoverButton = 2;
            else if (getNextRect(sub).contains(mx, my)) hoverButton = 3;
        }
    }

    public void handleMouseClick(MouseEvent e, GamePanel gp) {
        if (!visible) return;
        if (e.getButton() != MouseEvent.BUTTON1) return;

        int mx = e.getX();
        int my = e.getY();
        Rectangle sub = getSubRect(gp.screenWidth / 2 - panelW / 2, PANEL_Y);

        if (!detailMode) {
            for (int i = 0; i < mobList.size(); i++) {
                if (getSlotRect(i, sub).contains(mx, my)) {
                    selectedMobIndex = i;
                    detailMode = true;
                    detailStart = System.currentTimeMillis(); // restart animasi detail
                    hoverButton = 0;
                    if (gp != null) gp.playSE("select");
                    return;
                }
            }
        } else {
            if (getBackRect(sub).contains(mx, my)) {
                detailMode = false;
                hoveredSlot = -1;
                if (gp != null) gp.playSE("cursor");
            } else if (getPrevRect(sub).contains(mx, my)) {
                selectedMobIndex = (selectedMobIndex - 1 + mobList.size()) % mobList.size();
                detailStart = System.currentTimeMillis();
                if (gp != null) gp.playSE("cursor");
            } else if (getNextRect(sub).contains(mx, my)) {
                selectedMobIndex = (selectedMobIndex + 1) % mobList.size();
                detailStart = System.currentTimeMillis();
                if (gp != null) gp.playSE("cursor");
            }
        }
    }
}