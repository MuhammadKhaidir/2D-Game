package Entitas;

import Main.GamePanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PlayerSpeechBubble — menampilkan notifikasi teks sebagai speech bubble
 * yang muncul di atas kepala player, seolah player yang "ngomong".
 * Mendukung hingga MAX_BUBBLES pesan menumpuk ke atas.
 */
public class PlayerSpeechBubble {

    private static class Bubble {
        String text;
        int lifeTimer; // sisa waktu tampil (frame)

        Bubble(String text, int lifeTimer) {
            this.text = text;
            this.lifeTimer = lifeTimer;
        }
    }

    private final GamePanel gp;
    private final List<Bubble> bubbles = new ArrayList<>();

    private static final int LIFETIME_FRAMES = 180; // 3 detik @ 60fps
    private static final int FADE_FRAMES     = 30;  // mulai fade di 0.5 detik terakhir
    private static final int MAX_BUBBLES     = 3;

    private static final int BUBBLE_HEIGHT   = 28;
    private static final int BUBBLE_GAP      = 6;
    private static final int BUBBLE_PADDING_X = 12;
    private static final int TAIL_HEIGHT     = 8;
    private static final int BASE_OFFSET_Y   = 50; // jarak bubble pertama dari atas kepala player

    public PlayerSpeechBubble(GamePanel gp) {
        this.gp = gp;
    }

    public void show(String text) {
        bubbles.add(new Bubble(text, LIFETIME_FRAMES));
        while (bubbles.size() > MAX_BUBBLES) {
            bubbles.remove(0); // buang yang paling lama kalau kepenuhan
        }
    }

    public void update() {
        if (bubbles.isEmpty()) return;
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble b = bubbles.get(i);
            b.lifeTimer--;
            if (b.lifeTimer <= 0) bubbles.remove(i);
        }
    }

    public void draw(Graphics2D g2) {
        if (bubbles.isEmpty()) return;

        Font font = new Font("Arial", Font.PLAIN, 13);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics(font);

        int playerScreenCenterX = gp.player.screenX + gp.player.solidArea.x + gp.player.solidArea.width / 2;
        int playerScreenTopY    = gp.player.screenY;

        int cursorY = playerScreenTopY - BASE_OFFSET_Y;

        // Bubble terbaru (index terakhir) digambar paling deket player, di bawah.
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            Bubble b = bubbles.get(i);

            float alpha = 1f;
            if (b.lifeTimer < FADE_FRAMES) {
                alpha = Math.max(0f, b.lifeTimer / (float) FADE_FRAMES);
            }

            int textWidth = fm.stringWidth(b.text);
            int bubbleWidth = textWidth + BUBBLE_PADDING_X * 2;
            int bubbleX = playerScreenCenterX - bubbleWidth / 2;
            int bubbleY = cursorY - BUBBLE_HEIGHT;

            drawSingleBubble(g2, b.text, bubbleX, bubbleY, bubbleWidth, BUBBLE_HEIGHT,
                    playerScreenCenterX, i == bubbles.size() - 1, alpha, fm);

            cursorY -= (BUBBLE_HEIGHT + BUBBLE_GAP);
        }
    }

    private void drawSingleBubble(Graphics2D g2, String text, int x, int y, int w, int h,
                                   int tailTargetX, boolean withTail, float alpha, FontMetrics fm) {
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Bubble body
        g2.setColor(new Color(250, 245, 235));
        g2.fillRoundRect(x, y, w, h, 14, 14);

        g2.setColor(new Color(60, 50, 40));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w, h, 14, 14);

        // Ekor segitiga, cuma di bubble paling bawah (terdekat ke player)
        if (withTail) {
            int tailBaseX = Math.max(x + 12, Math.min(tailTargetX, x + w - 12));
            int[] tx = { tailBaseX - 6, tailBaseX + 6, tailTargetX };
            int[] ty = { y + h, y + h, y + h + TAIL_HEIGHT };

            g2.setColor(new Color(250, 245, 235));
            g2.fillPolygon(tx, ty, 3);
            g2.setColor(new Color(60, 50, 40));
            g2.drawPolyline(new int[]{tx[0], tx[2]}, new int[]{ty[0], ty[2]}, 2);
            g2.drawPolyline(new int[]{tx[1], tx[2]}, new int[]{ty[1], ty[2]}, 2);
        }

        // Teks
        g2.setColor(new Color(30, 25, 20));
        int textX = x + (w - fm.stringWidth(text)) / 2;
        int textY = y + (h + fm.getAscent()) / 2 - 3;
        g2.drawString(text, textX, textY);

        g2.setComposite(oldComposite);
    }
}