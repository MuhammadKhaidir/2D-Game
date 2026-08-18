package UI;

import Entitas.player;
import Main.GamePanel;
import java.awt.*;
import java.awt.event.MouseEvent;

public class DeathScreenUI {

    private final GamePanel gp;
    private final player owner;

    public boolean visible = false;
    private Rectangle buttonBounds;

    public DeathScreenUI(GamePanel gp, player owner) {
        this.gp = gp;
        this.owner = owner;
    }

    public void draw(Graphics2D g2) {
        if (!visible) return;

        // Overlay gelap nutup seluruh layar
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Teks "YOU ARE DEAD"
        g2.setFont(new Font("Serif", Font.BOLD, 48));
        g2.setColor(new Color(200, 30, 30));
        FontMetrics fmTitle = g2.getFontMetrics();
        String title = "YOU ARE DEAD";
        int titleX = gp.screenWidth / 2 - fmTitle.stringWidth(title) / 2;
        int titleY = gp.screenHeight / 2 - 60;
        g2.drawString(title, titleX, titleY);

        // Tombol Respawn
        int btnW = 200, btnH = 50;
        int btnX = gp.screenWidth / 2 - btnW / 2;
        int btnY = gp.screenHeight / 2;
        buttonBounds = new Rectangle(btnX, btnY, btnW, btnH);

        g2.setColor(new Color(40, 40, 40, 230));
        g2.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);

        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fmBtn = g2.getFontMetrics();
        String btnText = "RESPAWN";
        int btnTextX = btnX + (btnW - fmBtn.stringWidth(btnText)) / 2;
        int btnTextY = btnY + (btnH + fmBtn.getAscent()) / 2 - 4;
        g2.drawString(btnText, btnTextX, btnTextY);
    }

    public void handleClick(MouseEvent e) {
        if (!visible || buttonBounds == null) return;
        if (buttonBounds.contains(e.getPoint())) {
            visible = false;
            owner.respawnNow();
        }
    }
}