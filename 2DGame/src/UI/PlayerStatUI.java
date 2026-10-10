package UI;

import java.awt.*;
import java.awt.event.MouseEvent;
import Entitas.player;
import Main.GamePanel;

public class PlayerStatUI {

    private GamePanel gp;
    private player player;
    public boolean visible = false;

    private Color colorBorder = Color.WHITE;
    private Color colorAccent = new Color(255, 200, 60);
    private Color colorPlus   = new Color(80, 220, 80);
    private Color colorReset  = new Color(230, 80, 80);

    private Rectangle btnHP, btnStamina, btnDamage, btnSpeed, btnDodge, btnDef;
    private Rectangle btnReset;

    // Stat awal player, diambil sekali waktu UI dibuat (sebelum save di-load).
    // Dipakai buat reset: poin terpakai = (stat sekarang - stat awal) / kenaikan per poin.
    private final int startMaxLife;
    private final int startMaxStamina;
    private final int startPunchDamage;
    private final double startSpeed;
    private final int startDodge;
    private final int startDef;

    public PlayerStatUI(GamePanel gp, player player) {
        this.gp = gp;
        this.player = player;

        startMaxLife     = player.maxLife;
        startMaxStamina  = player.maxStamina;
        startPunchDamage = player.punchDamage;
        startSpeed       = player.baseSpeed;
        startDodge       = player.dodge;
        startDef         = player.def;
    }

    public void draw(Graphics2D g2) {
        if (!visible) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int winW = 360;
        int winH = 480;
        int winX = gp.screenWidth / 2 - winW / 2;
        int winY = gp.screenHeight / 2 - winH / 2;

        g2.setColor(Color.BLACK);
        g2.fillRect(winX, winY, winW, winH);
        g2.setColor(colorBorder);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(winX + 4, winY + 4, winW - 8, winH - 8);

        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        String title = "PLAYER STATS";
        g2.setColor(colorAccent);
        g2.drawString(title, getCenterX(g2, title, winX, winW), winY + 45);
        g2.setColor(colorBorder);
        g2.fillRect(winX + 30, winY + 58, winW - 60, 2);

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(new Color(180, 180, 180));
        g2.drawString("Level : " + player.level, winX + 30, winY + 82);
        g2.drawString("EXP   : " + player.exp + " / " + player.nextLevelExp, winX + 30, winY + 100);

        if (player.attributePoints > 0) {
            g2.setColor(colorPlus);
            g2.drawString("Points : " + player.attributePoints + " available", winX + 30, winY + 118);
        } else {
            g2.setColor(new Color(100, 100, 100));
            g2.drawString("Points : 0", winX + 30, winY + 118);
        }

        // Tombol RESET: selalu tampil, redup kalau stat masih di nilai awal
        boolean canReset = totalSpent() > 0;
        int rx = winX + winW - 130;
        int ry = winY + 88;
        int rw = 100;
        int rh = 24;
        btnReset = new Rectangle(rx, ry, rw, rh);

        g2.setColor(canReset ? new Color(90, 30, 30) : new Color(40, 40, 40));
        g2.fillRoundRect(rx, ry, rw, rh, 6, 6);
        g2.setColor(canReset ? colorReset : new Color(100, 100, 100));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(rx, ry, rw, rh, 6, 6);
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        int tw = g2.getFontMetrics().stringWidth("RESET");
        g2.drawString("RESET", rx + (rw - tw) / 2, ry + 17);

        g2.setColor(colorBorder);
        g2.fillRect(winX + 30, winY + 128, winW - 60, 1);

        int sx = winX + 30;
        int sy = winY + 155;
        int gap = 42;

        g2.setFont(new Font("Monospaced", Font.BOLD, 15));

        // RACE tidak bisa di-upgrade
        drawStatRow(g2, "RACE",    "HUMAN",                                             sx, sy,       winW, winX, false, null);
        // HP tampilkan life/maxLife supaya terlihat nilai sekarang
        btnHP      = drawStatRow(g2, "HP",      player.life + " / " + player.maxLife,   sx, sy+gap,   winW, winX, true,  btnHP);
        btnDef     = drawStatRow(g2, "DEF",     player.def + "",                         sx, sy+gap*2, winW, winX, true,  btnDef);
        btnDamage  = drawStatRow(g2, "DAMAGE",  player.punchDamage + "",                 sx, sy+gap*3, winW, winX, true,  btnDamage);
        // STAMINA tampilkan stamina/maxStamina supaya terlihat nilai sekarang
        btnStamina = drawStatRow(g2, "STAMINA", (int)player.stamina + " / " + player.maxStamina, sx, sy+gap*4, winW, winX, true, btnStamina);
        btnSpeed   = drawStatRow(g2, "SPEED",   String.format("%.1f", player.baseSpeed), sx, sy+gap*5, winW, winX, true,  btnSpeed);
        btnDodge   = drawStatRow(g2, "DODGE",   player.dodge + "% (max 50)",             sx, sy+gap*6, winW, winX, true,  btnDodge);

        // Sprite preview
        int spriteX = winX + winW / 2 - gp.tileSize / 2;
        int spriteY = winY + winH - 85;
        if (player.stand != null)
            g2.drawImage(player.stand, spriteX, spriteY, gp.tileSize, gp.tileSize, null);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(Color.GRAY);
        String hint = "[TAB] TO CLOSE   |   click [+] to upgrade";
        g2.drawString(hint, getCenterX(g2, hint, winX, winW), winY + winH - 14);
    }

    private Rectangle drawStatRow(Graphics2D g2, String label, String value,
                                   int x, int y, int winW, int winX,
                                   boolean upgradeable, Rectangle existingBtn) {
        g2.setColor(new Color(160, 160, 160));
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2.drawString(label + " :", x, y);

        g2.setColor(Color.WHITE);
        int valX = winX + winW - 160;
        g2.drawString(value, valX, y);

        if (upgradeable && player.attributePoints > 0) {
            int btnX = winX + winW - 45;
            int btnY = y - 16;
            int btnW = 30;
            int btnH = 20;
            Rectangle btn = new Rectangle(btnX, btnY, btnW, btnH);

            g2.setColor(new Color(40, 80, 40));
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
            g2.setColor(colorPlus);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.drawString("+", btnX + 9, btnY + 14);

            return btn;
        }
        return existingBtn;
    }

    public void handleClick(MouseEvent e) {
        if (!visible) return;

        int mx = e.getX(), my = e.getY();

        // Reset dicek duluan, karena harus tetap bisa diklik walau poin sudah 0
        if (btnReset != null && btnReset.contains(mx, my)) {
            resetStats();
            return;
        }

        if (player.attributePoints <= 0) return;

        if (btnHP != null && btnHP.contains(mx, my)) {
            player.maxLife += 10;
            player.life = Math.min(player.life + 10, player.maxLife);
            player.attributePoints--;
        } else if (btnStamina != null && btnStamina.contains(mx, my)) {
            player.maxStamina += 10;
            player.stamina = Math.min((int)player.stamina + 10, player.maxStamina);
            player.attributePoints--;
        } else if (btnDamage != null && btnDamage.contains(mx, my)) {
            player.punchDamage += 5;
            player.attributePoints--;
        } else if (btnSpeed != null && btnSpeed.contains(mx, my)) {
            player.baseSpeed = Math.round((player.baseSpeed + 0.1) * 10.0) / 10.0;
            player.attributePoints--;
        } else if (btnDodge != null && btnDodge.contains(mx, my)) {
            if (player.dodge < 50) {
                player.dodge++;
                player.attributePoints--;
            }
        } else if (btnDef != null && btnDef.contains(mx, my)) {
            player.def++;
            player.attributePoints--;
        }
    }

    // ===== Hitung poin yang sudah terpakai, langsung dari stat player sekarang =====
    private int spentHP()      { return Math.max(0, (player.maxLife - startMaxLife) / 10); }
    private int spentStamina() { return Math.max(0, (player.maxStamina - startMaxStamina) / 10); }
    private int spentDamage()  { return Math.max(0, (player.punchDamage - startPunchDamage) / 5); }
    private int spentSpeed()   { return Math.max(0, (int) Math.round((player.baseSpeed - startSpeed) / 0.1)); }
    private int spentDodge()   { return Math.max(0, player.dodge - startDodge); }
    private int spentDef()     { return Math.max(0, player.def - startDef); }

    private int totalSpent() {
        return spentHP() + spentStamina() + spentDamage() + spentSpeed() + spentDodge() + spentDef();
    }

    // Balikin semua stat ke nilai awal, lalu kembalikan semua poin yang terpakai
    private void resetStats() {
        int total = totalSpent();   // hitung dulu sebelum stat diubah
        if (total == 0) return;

        player.maxLife = startMaxLife;
        player.life = Math.min(player.life, player.maxLife);

        player.maxStamina = startMaxStamina;
        player.stamina = Math.min(player.stamina, player.maxStamina);

        player.punchDamage = startPunchDamage;
        player.baseSpeed = startSpeed;
        player.dodge = startDodge;
        player.def = startDef;

        player.attributePoints += total;
    }

    private int getCenterX(Graphics2D g2, String text, int winX, int winW) {
        int len = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return winX + (winW / 2) - (len / 2);
    }
}