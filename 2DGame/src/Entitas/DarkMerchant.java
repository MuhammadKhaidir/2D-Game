package Entitas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import Main.GamePanel;

public class DarkMerchant extends Entity {

    public enum TalkState { NONE, GREETED, SHOP_OPEN }
    public TalkState talkState = TalkState.NONE;

    private BufferedImage img1, img2;
    private int animTimer = 0;
    private boolean showImg1 = true;
    public int navCooldown = 0;

    // Icon item yang dijual
    private BufferedImage iconBoneSword;
    private BufferedImage iconSkeletonKey;

    public static class ShopItem {
        public String name;
        public String description;
        public String flavorText;
        public int boneCost;
        public int skullCost;
        public BufferedImage icon;

        public ShopItem(String name, String desc, String flavor, int bone, int skull) {
            this.name       = name;
            this.description = desc;
            this.flavorText = flavor;
            this.boneCost   = bone;
            this.skullCost  = skull;
        }
    }

    @Override
    public int getSortY() {
        return y + solidArea.y + solidArea.height;
    }

    public final ShopItem[] shopItems = {
        new ShopItem(
            "Bone Sword",
            "A blade forged from the bones of fallen warriors.",
            "\"Death is just the beginning.\"",
            100, 20
        ),
        new ShopItem(
            "Skeleton Key",
            "Unlocks a seal that summons something ancient,\nmysterious, and terrifyingly dangerous.",
            "\"Do not open this unless you are ready.\"",
            40, 0
        )
    };
    public int selectedIndex = 0;

    // Dialog percakapan
    private static final String[][] DIALOG_LINES = {
        {
            "...",
            "You reek of the living.",
            "Browse my wares... if you dare."
        }
    };
    private int dialogLineIndex = 0;
    public boolean inDialog = false;

    public DarkMerchant(GamePanel gp) {
        super(gp);
        direction = "down";
        speed     = 0;
        maxLife   = 99999;
        life      = maxLife;
        showName  = true;
        npcState  = NPCState.STAY;

        solidArea = new Rectangle(10, 38, 28, 16);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        loadImages();
    }

    private void loadImages() {
        try {
            img1 = ImageIO.read(getClass().getResourceAsStream("/Aset/DarkMerchant (1).png"));
            img2 = ImageIO.read(getClass().getResourceAsStream("/Aset/DarkMerchant (2).png"));
        } catch (IOException | NullPointerException e) {
            System.out.println("[DarkMerchant] Gagal load sprite: " + e.getMessage());
        }
        try {
            iconBoneSword   = ImageIO.read(getClass().getResourceAsStream("/Aset/BoneSword.png"));
        } catch (Exception e) { iconBoneSword = null; }
        try {
            iconSkeletonKey = ImageIO.read(getClass().getResourceAsStream("/Aset/SkeletonKey.png"));
        } catch (Exception e) { iconSkeletonKey = null; }

        shopItems[0].icon = iconBoneSword;
        shopItems[1].icon = iconSkeletonKey;
    }

    @Override
    public void update() {
        animTimer++;
        if (animTimer >= 40) {
            showImg1 = !showImg1;
            animTimer = 0;
        }
        if (navCooldown > 0) navCooldown--;
    }

    // ── Dipanggil DomainManager saat E ditekan ──────────────────────────────
    public void onInteract() {
        if (talkState == TalkState.NONE) {
            // Mulai dialog
            inDialog = true;
            dialogLineIndex = 0;
            talkState = TalkState.GREETED;
        } else if (inDialog) {
            // Lanjut baris dialog
            dialogLineIndex++;
            if (dialogLineIndex >= DIALOG_LINES[0].length) {
                // Dialog habis → buka shop
                inDialog = false;
                talkState = TalkState.SHOP_OPEN;
            }
        } else if (talkState == TalkState.SHOP_OPEN) {
            // E saat shop buka = beli
            // (beli dihandle di DomainManager via handleShopInput)
        } else if (talkState == TalkState.GREETED) {
            talkState = TalkState.SHOP_OPEN;
        }
    }

    // E lagi saat shop buka → tutup
    public void onInteractWhileShopOpen() {
        talkState = TalkState.GREETED;
        inDialog  = false;
    }

    public void closeShop() {
        talkState = TalkState.NONE;
        inDialog  = false;
        dialogLineIndex = 0;
    }

    public boolean isShopOpen()  { return talkState == TalkState.SHOP_OPEN; }
    public boolean isInDialog()  { return inDialog; }

    public String getCurrentDialogLine() {
        if (!inDialog) return "";
        return DIALOG_LINES[0][dialogLineIndex];
    }

    // ── Draw sprite ─────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        int drawW = gp.tileSize * 2;
        int drawH = gp.tileSize * 2;

        // Shadow — tepat di bawah kaki sprite (baris paling bawah)
        int shadowW = drawW / 3;
        int shadowH = shadowW / 4;
        int shadowX = screenX + drawW / 2 - shadowW / 2;
        int shadowY = screenY + drawH - 30; // 10px dari bawah sprite
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g2.setColor(Color.BLACK);
        g2.fillOval(shadowX, shadowY, shadowW, shadowH);
        g2.setComposite(old);

        // Sprite
        BufferedImage img = showImg1 ? img1 : img2;
        if (img == null) {
            g2.setColor(new Color(120, 0, 200));
            g2.fillRect(screenX, screenY, drawW, drawH);
        } else {
            g2.drawImage(img, screenX, screenY, drawW, drawH, null);
        }

        drawOverhead(g2);

        // Hint interaksi
        int dist = (int) Math.hypot(x - gp.player.x, y - gp.player.y);
        if (dist < gp.tileSize * 2 && !isShopOpen()) {
            drawInteractHint(g2, screenX, screenY, drawH);
        }
    }

    private void drawInteractHint(Graphics2D g2, int sx, int sy, int drawH) {
        String hint = "[E] Talk";
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int tw  = fm.stringWidth(hint);
        int bx  = sx + gp.tileSize - tw / 2;
        int by  = sy - 8;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(bx - 8, by - 14, tw + 16, 20, 8, 8);
        g2.setColor(new Color(255, 220, 80));
        g2.drawString(hint, bx, by);
    }

    // ── Draw dialog box (gaya NPC biasa) ────────────────────────────────────
    public void drawDialogBox(Graphics2D g2) {
        if (!inDialog) return;

        String line = getCurrentDialogLine();

        int bx = 30;
        int by = gp.screenHeight - 190;
        int bw = gp.screenWidth - 60;
        int bh = 150;

        // Background
        g2.setColor(new Color(5, 0, 15, 230));
        g2.fillRoundRect(bx, by, bw, bh, 24, 24);

        // Border ungu
        g2.setColor(new Color(140, 60, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(bx, by, bw, bh, 24, 24);
        g2.setStroke(new BasicStroke(1));

        // Nama merchant
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(200, 120, 255));
        // Separator tipis
        g2.setColor(new Color(140, 60, 200, 120));
        g2.drawLine(bx + 20, by + 35, bx + bw - 20, by + 35);

        // Teks dialog (support newline)
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(new Color(230, 220, 255));
        int tx = bx + 24;
        int ty = by + 60;
        for (String row : line.split("\n")) {
            g2.drawString(row, tx, ty);
            ty += 20;
        }

        // Prompt lanjut
        boolean isLast = (dialogLineIndex == DIALOG_LINES[0].length - 1);
        String prompt = isLast ? "[E] Open Shop" : "[E] Continue";
        g2.setFont(new Font("Monospaced", Font.ITALIC, 11));
        g2.setColor(new Color(160, 130, 200));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(prompt, bx + bw - fm.stringWidth(prompt) - 20, by + bh - 14);
    }

    // ── Draw shop UI ─────────────────────────────────────────────────────────
    public void drawShopUI(Graphics2D g2) {
        if (!isShopOpen()) return;

        int panelW = 480;
        int panelH = 300;
        int panelX = gp.screenWidth  / 2 - panelW / 2;
        int panelY = gp.screenHeight / 2 - panelH / 2;

        // Background
        g2.setColor(new Color(5, 0, 15, 240));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        // Border
        g2.setColor(new Color(140, 60, 200));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // Judul
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2.setColor(new Color(200, 120, 255));
        String title = "✦  Dark Merchant's Wares  ✦";
        FontMetrics fmT = g2.getFontMetrics();
        g2.drawString(title, panelX + panelW / 2 - fmT.stringWidth(title) / 2, panelY + 30);

        // Garis bawah judul
        g2.setColor(new Color(140, 60, 200, 160));
        g2.drawLine(panelX + 20, panelY + 38, panelX + panelW - 20, panelY + 38);

        // Daftar item
        for (int i = 0; i < shopItems.length; i++) {
            drawShopItem(g2, shopItems[i], i, panelX, panelY + 50 + i * 110);
        }

        // Footer
        g2.setFont(new Font("Monospaced", Font.ITALIC, 10));
        g2.setColor(new Color(120, 100, 160));
        String footer = "W/S  Navigate    E  Buy    ESC  Leave";
        FontMetrics fmF = g2.getFontMetrics();
        g2.drawString(footer,
            panelX + panelW / 2 - fmF.stringWidth(footer) / 2,
            panelY + panelH - 12);
    }

    private void drawShopItem(Graphics2D g2, ShopItem item, int index, int px, int py) {
        boolean selected = (index == selectedIndex);
        int rowH = 100;
        int rowW = 440;
        int rx   = px + 20;

        // Row background
        if (selected) {
            g2.setColor(new Color(100, 40, 160, 100));
            g2.fillRoundRect(rx, py, rowW, rowH, 12, 12);
            g2.setColor(new Color(180, 100, 255, 180));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(rx, py, rowW, rowH, 12, 12);
            g2.setStroke(new BasicStroke(1));
        } else {
            g2.setColor(new Color(30, 10, 50, 80));
            g2.fillRoundRect(rx, py, rowW, rowH, 12, 12);
            g2.setColor(new Color(80, 40, 120, 80));
            g2.drawRoundRect(rx, py, rowW, rowH, 12, 12);
        }

        // Icon item (kotak 64x64)
        int iconX = rx + 12;
        int iconY = py + 18;
        int iconS = 64;
        g2.setColor(new Color(20, 5, 35));
        g2.fillRoundRect(iconX, iconY, iconS, iconS, 8, 8);
        g2.setColor(new Color(100, 50, 160));
        g2.drawRoundRect(iconX, iconY, iconS, iconS, 8, 8);
        if (item.icon != null) {
            g2.drawImage(item.icon, iconX + 4, iconY + 4, iconS - 8, iconS - 8, null);
        } else {
            // Placeholder "?" kalau icon null
            g2.setFont(new Font("Monospaced", Font.BOLD, 28));
            g2.setColor(new Color(160, 100, 220));
            g2.drawString("?", iconX + 22, iconY + 42);
        }

        // Nama item
        int textX = iconX + iconS + 16;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(selected ? new Color(255, 220, 100) : new Color(210, 190, 240));
        String arrow = selected ? "► " : "  ";
        g2.drawString(arrow + item.name, textX, py + 30);

        // Deskripsi (support newline)
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(170, 155, 200));
        int descY = py + 48;
        for (String row : item.description.split("\n")) {
            g2.drawString(row, textX, descY);
            descY += 15;
        }

        // Flavor text italic
        g2.setFont(new Font("Monospaced", Font.ITALIC, 10));
        g2.setColor(new Color(120, 100, 160));
        g2.drawString(item.flavorText, textX, py + 84);

        // Harga — teks saja, tidak perlu icon
        String priceStr = buildPriceStr(item);
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(selected ? new Color(255, 200, 60) : new Color(180, 160, 100));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(priceStr, rx + rowW - fm.stringWidth(priceStr) - 14, py + rowH - 12);
    }

    private String buildPriceStr(ShopItem item) {
        if (item.skullCost > 0)
            return item.boneCost + " Bone  +  " + item.skullCost + " Skull";
        return item.boneCost + " Bone";
    }
}