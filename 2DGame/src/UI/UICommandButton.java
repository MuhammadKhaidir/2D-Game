package UI;

import java.awt.*;

public class UICommandButton {

    public Rectangle bounds;
    public String text;
    public boolean hovered = false;

    public UICommandButton(int x, int y, int w, int h, String text) {
        this.bounds = new Rectangle(x, y, w, h);
        this.text = text;
    }

    public void draw(Graphics2D g2) {

        // background
        g2.setColor(hovered ? new Color(90, 90, 90) : new Color(50, 50, 50));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        // border
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        // text center
        FontMetrics fm = g2.getFontMetrics();
        int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = bounds.y + (bounds.height + fm.getAscent()) / 2 - 3;

        g2.drawString(text, textX, textY);
    }

    public boolean contains(int mx, int my) {
        return bounds.contains(mx, my);
    }
}
