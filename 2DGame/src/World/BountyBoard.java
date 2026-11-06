package World;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import Entitas.player;
import Main.GamePanel;

/**
 * Papan Bounty di dunia luar (dekat peti penyimpanan).
 *
 * Alur: player dekat papan + tekan [E] -> BountyBoardUI kebuka. Dunia berhenti selama panel kebuka (lihat GamePanel.update).
 * Gambarnya dibikin pakai Graphics2D, jadi gak butuh file gambar. Posisi bisa diubah lewat WORLD_X / WORLD_Y.
 */
public class BountyBoard {

    /** Koordinat dunia (pojok kiri-atas papan), sama kayak worldX/worldY objek lain. */
    public static final int WORLD_X = 656;
    public static final int WORLD_Y = 1110;

    public static final int DRAW_W = 96;
    public static final int DRAW_H = 84;

    private final GamePanel gp;

    public BountyBoard(GamePanel gp) {
        this.gp = gp;
        // Papan padat: bagian bawah (kaki papan) jadi penghalang, sama kayak peti.
        gp.objCol.add(WORLD_X + 8, WORLD_Y + DRAW_H / 2, DRAW_W - 16, DRAW_H / 2);
    }

    public Rectangle getInteractArea() {
        return new Rectangle(WORLD_X - 12, WORLD_Y - 12, DRAW_W + 24, DRAW_H + 24);
    }

    /** Dipakai buat Y-sort (urutan gambar terhadap player/NPC/monster). */
    public int getSortY() {
        return WORLD_Y + DRAW_H;
    }

    public boolean isPlayerNear(player p) {
        return p != null && p.getInteractArea().intersects(getInteractArea());
    }

    /**
     * Player menekan [E]. Return true kalau panel dibuka, artinya tombol E dianggap sudah dipakai.
     */
    public boolean tryOpen(player p) {
        if (!isPlayerNear(p)) return false;
        if (gp.bountyUI == null || gp.bountyUI.visible) return false;
        gp.bountyUI.open();
        return true;
    }

    /** Gambar papan. Dipanggil dari TileM.drawYSorted (di dalam transform zoom dunia). */
    public void draw(Graphics2D g2, int camX, int camY) {
        int sx = WORLD_X - camX + gp.player.screenX;
        int sy = WORLD_Y - camY + gp.player.screenY;

        if (sx + DRAW_W < gp.viewMinX() || sx > gp.viewMaxX()) return;
        if (sy + DRAW_H < gp.viewMinY() || sy > gp.viewMaxY()) return;

        // bayangan di dasar papan
        java.awt.Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);
        g2.fillOval(sx + 2, sy + DRAW_H - 10, DRAW_W - 4, 20);
        g2.setComposite(oc);

        // dua tiang kayu
        Color wood = new Color(120, 78, 40);
        Color woodDark = new Color(60, 38, 18);
        g2.setColor(wood);
        g2.fillRect(sx + 12, sy + 40, 10, DRAW_H - 40);
        g2.fillRect(sx + DRAW_W - 22, sy + 40, 10, DRAW_H - 40);
        g2.setColor(woodDark);
        g2.drawRect(sx + 12, sy + 40, 10, DRAW_H - 41);
        g2.drawRect(sx + DRAW_W - 22, sy + 40, 10, DRAW_H - 41);

        // papan
        int bx = sx + 2, by = sy + 4, bw = DRAW_W - 4, bh = 54;
        g2.setColor(wood);
        g2.fillRoundRect(bx, by, bw, bh, 8, 8);
        g2.setColor(woodDark);
        g2.drawRoundRect(bx, by, bw, bh, 8, 8);
        g2.setColor(new Color(150, 105, 60));
        g2.fillRect(bx + 5, by + 5, bw - 10, bh - 10);

        // tiga kertas misi
        g2.setColor(new Color(240, 225, 190));
        g2.fillRect(bx + 10, by + 10, 22, 30);
        g2.fillRect(bx + 37, by + 8, 22, 34);
        g2.fillRect(bx + 64, by + 12, 18, 28);
        g2.setColor(new Color(160, 60, 50));
        g2.fillOval(bx + 18, by + 11, 5, 5);
        g2.fillOval(bx + 45, by + 9, 5, 5);
        g2.fillOval(bx + 70, by + 13, 5, 5);
        g2.setColor(new Color(110, 85, 60));
        for (int i = 0; i < 3; i++) {
            g2.drawLine(bx + 13, by + 22 + i * 5, bx + 28, by + 22 + i * 5);
            g2.drawLine(bx + 40, by + 21 + i * 5, bx + 55, by + 21 + i * 5);
            g2.drawLine(bx + 67, by + 22 + i * 5, bx + 78, by + 22 + i * 5);
        }

        if (!(gp.bountyUI != null && gp.bountyUI.visible) && isPlayerNear(gp.player)) {
            drawPrompt(g2, sx + DRAW_W / 2, sy - 6);
        }
    }

    private void drawPrompt(Graphics2D g2, int centerX, int bottomY) {
        Font old = g2.getFont();
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String text = "[E] Bounty Board";
        int w = fm.stringWidth(text) + 12;
        int h = fm.getHeight() + 2;
        int x = centerX - w / 2;
        int y = bottomY - h;

        java.awt.Composite oc = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2.setColor(new Color(30, 22, 16));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setComposite(oc);
        g2.setColor(new Color(220, 190, 150));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(255, 235, 200));
        g2.drawString(text, x + 6, y + h - 5);
        g2.setFont(old);
    }
}
