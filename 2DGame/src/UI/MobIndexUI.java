package UI;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import Main.GamePanel;

public class MobIndexUI {
    public boolean visible = false;
    private boolean detailMode = false; // Flag status: false = Grid View, true = Detail View
    private int selectedMobIndex = 0;

    private int panelW = 640;
    private int panelH = 440;

    // Skema warna default (Grid View) disamakan dengan BackpackUI
    private Color colorBgOuterDefault   = new Color(45, 30, 20);   
    private Color colorBgInnerDefault   = new Color(85, 65, 50);   
    private Color colorSubPanelDefault  = new Color(35, 25, 20);   
    private Color colorSlotDefault      = new Color(60, 45, 35);   
    private Color colorSlotDarkDefault  = new Color(25, 20, 15);   
    private Color colorBorderDefault    = new Color(220, 190, 150);   
    private Color colorAccentDefault    = new Color(250, 180, 40);  

    // Data Mob internal
    private List<MobData> mobList = new ArrayList<>();

    // Grid Configuration
    private final int maxCols = 6;       // Jumlah kolom grid kesamping
    private final int slotSize = 76;     // Ukuran kotak gambar monster
    private final int slotPadding = 14;  // Jarak antar kotak

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

        public MobData(String name, String rarity, String description, int hp, int dmg, double speed, String drops, Image icon) {
            this.name = name;
            this.rarity = rarity; 
            this.description = description;
            this.hp = hp;
            this.dmg = dmg;
            this.speed = speed;
            this.drops = drops;
            this.icon = icon;
        }
    }

    public MobIndexUI(GamePanel gp) {
        mobList.add(new MobData("SKELETON", "COMMON", "A reanimated undead warrior bound by dark residual magic. It wanders dark tomb dungeons clutching brittle weapons, can be tamed.", 400, 15, 1.5, "Bone (15%), Skull (5%), Skeleton Key (0,5%), Death Totem (0,1%)", gp.setup("SkeletonFrontWalk (1)")));
        mobList.add(new MobData("SLIME", "COMMON", "A bouncy, gelatinous creature. Mildly aggressive but dangerously sticky. Drops chemically reactive gel.", 50, 5, 2, "None", gp.setup("Sticklooter_carrot_idle3")));
        mobList.add(new MobData("STONE GOLEM", "EPIC", "A massive, powerful construct carved from living solid stone. Awakens from deep underground to crush intruders, can be tamed.", 5000, 200, 1, "NONE", gp.setup("GolemFrontWalk (6)")));
    }

    // Helper Method untuk menentukan warna teks/aksen berdasarkan Rarity
    private Color getRarityColor(String rarity) {
        switch (rarity.toUpperCase()) {
            case "COMMON": return new Color(180, 180, 180); // Abu-abu terang
            case "EPIC":   return new Color(190, 80, 255);  // Ungu Terang Cerah
            default:       return Color.WHITE;
        }
    }

    // Helper Method untuk menentukan warna Tema Background berdasarkan Rarity
    // Index: 0=Outer, 1=Inner, 2=SubPanel, 3=Slot/Button, 4=SlotDark, 5=Border
    private Color[] getRarityThemeColors(String rarity) {
        Color[] theme = new Color[6];
        switch (rarity.toUpperCase()) {
            case "COMMON":
                theme[0] = new Color(35, 35, 35);       // Outer (Abu Gelap Banget)
                theme[1] = new Color(60, 60, 60);       // Inner (Abu Medium)
                theme[2] = new Color(25, 25, 25);       // Sub Panel
                theme[3] = new Color(45, 45, 45);       // Slot / Button Base
                theme[4] = new Color(20, 20, 20);       // Slot Dark
                theme[5] = new Color(170, 170, 170);   // Border (Perak/Sliver)
                break;
            case "EPIC":
                theme[0] = new Color(30, 15, 40);       // Outer (Ungu Gelap Mistis)
                theme[1] = new Color(55, 30, 75);       // Inner (Ungu Medium RPG)
                theme[2] = new Color(20, 10, 30);       // Sub Panel
                theme[3] = new Color(45, 20, 65);       // Slot / Button Base
                theme[4] = new Color(15, 5, 25);        // Slot Dark
                theme[5] = new Color(220, 140, 255);    // Border (Ungu Neon Soft)
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

    public void draw(Graphics2D g2, int screenW, int screenH, GamePanel gp) {
        if (!visible) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int panelX = screenW / 2 - panelW / 2;
        int panelY = 60;

        // Tentukan skema warna aktif berdasarkan mode (Grid vs Detail)
        Color currentBgOuter, currentBgInner, currentBorder, currentAccent;
        
        if (detailMode) {
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

        // Frame Utama (Dinamis)
        g2.setColor(currentBgOuter);
        g2.fillRect(panelX, panelY, panelW, panelH);
        
        g2.setColor(currentBgInner);
        g2.fillRect(panelX + 6, panelY + 6, panelW - 12, panelH - 12);
        
        g2.setColor(currentBorder);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(panelX + 8, panelY + 8, panelW - 16, panelH - 16);

        // Header Title
        String title = "BESTIARY / MOB INDEX";
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;
        
        g2.setColor(currentBgOuter);
        g2.fillRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(currentBorder);
        g2.drawRect(titleX - 20, panelY + 18, titleW + 40, 30);
        
        g2.setColor(Color.BLACK);
        g2.drawString(title, titleX + 2, panelY + 40); 
        g2.setColor(currentAccent);
        g2.drawString(title, titleX, panelY + 38);   

        // PERCABANGAN TAMPILAN
        if (!detailMode) {
            drawGridView(g2, panelX, panelY);
        } else {
            drawDetailView(g2, panelX, panelY);
        }
    }

    // TAMPILAN 1: GRID UTAMA (Tetap Cokelat Klasik ala Backpack)
    private void drawGridView(Graphics2D g2, int panelX, int panelY) {
        int gridSubX = panelX + 24;
        int gridSubY = panelY + 75;
        int gridSubW = panelW - 48;
        int gridSubH = panelH - 100;

        g2.setColor(colorSubPanelDefault);
        g2.fillRect(gridSubX, gridSubY, gridSubW, gridSubH);
        g2.setColor(colorBgOuterDefault);
        g2.drawRect(gridSubX, gridSubY, gridSubW, gridSubH);

        int totalGridW = (maxCols * slotSize) + ((maxCols - 1) * slotPadding);
        int gridStartX = gridSubX + (gridSubW - totalGridW) / 2;
        int gridStartY = gridSubY + 30;

        for (int i = 0; i < mobList.size(); i++) {
            int col = i % maxCols;
            int row = i / maxCols;

            int slotX = gridStartX + col * (slotSize + slotPadding);
            int slotY = gridStartY + row * (slotSize + slotPadding);

            g2.setColor(colorSlotDarkDefault);
            g2.fillRect(slotX, slotY, slotSize, slotSize);
            g2.setColor(colorBgOuterDefault);
            g2.drawRect(slotX, slotY, slotSize, slotSize);

            MobData mob = mobList.get(i);
            if (mob.icon != null) {
                g2.drawImage(mob.icon, slotX + 6, slotY + 6, slotSize - 12, slotSize - 12, null);
            } else {
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.setColor(new Color(130, 110, 95));
                String shortName = mob.name.length() > 6 ? mob.name.substring(0, 6) : mob.name;
                g2.drawString(shortName, slotX + 8, slotY + (slotSize / 2) + 4);
            }
        }
    }

    // TAMPILAN 2: DETAIL MONSTER (Full Custom BG Rarity)
    private void drawDetailView(Graphics2D g2, int panelX, int panelY) {
        int subX = panelX + 24;
        int subY = panelY + 75;
        int subW = panelW - 48;
        int subH = panelH - 100;

        MobData activeMob = mobList.get(selectedMobIndex);
        
        // Ambil data skema warna tema kelangkaan aktif
        Color[] theme = getRarityThemeColors(activeMob.rarity);
        Color cOuter   = theme[0];
        Color cSub     = theme[2];
        Color cSlot    = theme[3];
        Color cSlotDark= theme[4];
        Color cBorder  = theme[5];
        Color cAccent  = getRarityColor(activeMob.rarity);

        // Background Sub Panel Tengah (Dinamis)
        g2.setColor(cSub);
        g2.fillRect(subX, subY, subW, subH);
        g2.setColor(cOuter);
        g2.drawRect(subX, subY, subW, subH);

        // 1. Box Gambar Besar Kiri (Dinamis)
        int imgSize = 96;
        int imgX = subX + 24;
        int imgY = subY + 24;

        g2.setColor(cSlot);
        g2.fillRect(imgX, imgY, imgSize, imgSize);
        g2.setColor(cSlotDark);
        g2.fillRect(imgX, imgY + imgSize - 4, imgSize, 4);
        g2.fillRect(imgX + imgSize - 4, imgY, 4, imgSize);
        g2.setColor(cOuter);
        g2.drawRect(imgX, imgY, imgSize, imgSize);

        if (activeMob.icon != null) {
            g2.drawImage(activeMob.icon, imgX + 8, imgY + 8, imgSize - 16, imgSize - 16, null);
        } else {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(new Color(150, 150, 150));
            g2.drawString("NO IMAGE", imgX + 20, imgY + (imgSize / 2) + 4);
        }

        // 2. Data Nama, Rarity & Status Teknis
        int infoX = imgX + imgSize + 24;
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.setColor(Color.WHITE); // Menggunakan warna putih agar nama terbaca jelas di BG gelap mana pun
        g2.drawString(activeMob.name, infoX, imgY + 14);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(cAccent);
        g2.drawString("[" + activeMob.rarity + "]", infoX, imgY + 32);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        g2.drawString("HP    : " + activeMob.hp, infoX, imgY + 54);
        g2.drawString("DMG   : " + activeMob.dmg, infoX, imgY + 72);
        g2.drawString("SPEED : " + activeMob.speed, infoX, imgY + 90);

        // Divider Line Pixel-Art (Dinamis)
        g2.setColor(cOuter);
        g2.fillRect(subX + 20, subY + 135, subW - 40, 3);
        g2.setColor(cSlot);
        g2.fillRect(subX + 20, subY + 138, subW - 40, 1);

        // 3. Deskripsi Lore & Behavior
        int descX = subX + 24;
        int descY = subY + 160;
        int descW = subW - 48;

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(cBorder);
        g2.drawString("- Lore & Behavior -", descX, descY);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        drawWrappedString(g2, activeMob.description, descX, descY + 22, descW, 16);

        // 4. Deskripsi Drop Item
        int dropsY = descY + 75;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(cAccent);
        g2.drawString("- Potential Drops -", descX, dropsY);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        drawWrappedString(g2, activeMob.drops, descX, dropsY + 22, descW, 16);

        // 5. Tombol BACK (Dinamis)
        int btnW = 90;
        int btnH = 30;
        int btnX = subX + subW - btnW - 20;
        int btnY = subY + subH - btnH - 16;

        g2.setColor(cSlot);
        g2.fillRect(btnX, btnY, btnW, btnH);
        g2.setColor(cOuter);
        g2.drawRect(btnX, btnY, btnW, btnH);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(cBorder);
        g2.drawString("BACK", btnX + 28, btnY + 19);
    }

    private void drawWrappedString(Graphics2D g2, String text, int x, int y, int wrapWidth, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (fm.stringWidth(line + word) < wrapWidth) {
                line.append(word).append(" ");
            } else {
                g2.drawString(line.toString(), x, y);
                line = new StringBuilder(word).append(" ");
                y += lineHeight;
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, y);
        }
    }

    public void handleMouseClick(MouseEvent e, GamePanel gp) {
        if (!visible) return;

        int mx = e.getPoint().x;
        int my = e.getPoint().y;

        int screenW = gp.screenWidth;
        int panelX = screenW / 2 - panelW / 2;
        int panelY = 60;

        int subX = panelX + 24;
        int subY = panelY + 75;
        int subW = panelW - 48;
        int subH = panelH - 100;

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (!detailMode) {
                int totalGridW = (maxCols * slotSize) + ((maxCols - 1) * slotPadding);
                int gridStartX = subX + (subW - totalGridW) / 2;
                int gridStartY = subY + 30;

                for (int i = 0; i < mobList.size(); i++) {
                    int col = i % maxCols;
                    int row = i / maxCols;

                    int slotX = gridStartX + col * (slotSize + slotPadding);
                    int slotY = gridStartY + row * (slotSize + slotPadding);

                    if (mx >= slotX && mx <= slotX + slotSize && my >= slotY && my <= slotY + slotSize) {
                        selectedMobIndex = i;
                        detailMode = true; 
                        if (gp != null) gp.playSE("select"); 
                        return;
                    }
                }
            } else {
                int btnW = 90;
                int btnH = 30;
                int btnX = subX + subW - btnW - 20;
                int btnY = subY + subH - btnH - 16;

                if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                    detailMode = false; 
                    if (gp != null) gp.playSE("cursor"); 
                }
            }
        }
    }
}