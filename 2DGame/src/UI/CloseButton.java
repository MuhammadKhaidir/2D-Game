package UI;

import java.awt.*;

/**
 * Tombol silang (X) kecil di pojok kanan atas panel/bar.
 * Dipakai bareng sama semua panel biar gambar & hitbox-nya selalu sama.
 */
public class CloseButton {

    public static final int SIZE = 26;

    /** Posisi default: di dalam bingkai panel, sejajar sama judul. */
    public static Rectangle rect(int panelX, int panelY, int panelW) {
        return rect(panelX, panelY, panelW, 18, 18);
    }

    /** right = jarak dari sisi kanan panel, top = jarak dari sisi atas panel. */
    public static Rectangle rect(int panelX, int panelY, int panelW, int right, int top) {
        return new Rectangle(panelX + panelW - SIZE - right, panelY + top, SIZE, SIZE);
    }

    public static void draw(Graphics2D g2, Rectangle r) {
        Stroke oldStroke = g2.getStroke();

        g2.setColor(new Color(110, 30, 25));
        g2.fillRect(r.x, r.y, r.width, r.height);

        g2.setColor(new Color(255, 120, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(r.x, r.y, r.width, r.height);

        int p = 8;
        g2.setColor(Color.WHITE);
        g2.drawLine(r.x + p, r.y + p, r.x + r.width - p, r.y + r.height - p);
        g2.drawLine(r.x + r.width - p, r.y + p, r.x + p, r.y + r.height - p);

        g2.setStroke(oldStroke);
    }
}
