package UI;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import Entitas.PlayerBuffs;
import Entitas.PlayerBuffs.Buff;
import Item.Item;
import Item.ItemManager;
import Item.PotionType;
import Main.GamePanel;

/**
 * Deretan ikon buff yang lagi aktif, tepat di atas hotbar. Tiap ikon punya sisa waktu (m:ss);
 * 10 detik terakhir angkanya berubah merah. Kalau tidak ada buff aktif, tidak menggambar apa-apa.
 */
public class BuffBar {

    private static final int CELL_W = 42;
    private static final int CELL_H = 46;
    private static final int GAP    = 4;
    private static final int ICON   = 30;
    private static final int WARN_SECONDS = 10;

    private static final Font FONT_TIME = new Font("Monospaced", Font.BOLD, 10);

    private final GamePanel gp;

    public BuffBar(GamePanel gp) {
        this.gp = gp;
    }

    public void draw(Graphics2D g2) {
        if (gp.player == null || gp.player.isDead) return;
        PlayerBuffs buffs = gp.player.buffs;

        int active = 0;
        for (Buff buff : Buff.values()) {
            if (buffs.isActive(buff)) active++;
        }
        if (active == 0) return;

        Rectangle hotbar = gp.hotbar != null
            ? gp.hotbar.getBounds()
            : new Rectangle(0, gp.screenHeight, 0, 0);

        int totalW = active * CELL_W + (active - 1) * GAP;
        int x = (gp.screenWidth - totalW) / 2;
        int y = hotbar.y - CELL_H - 6;

        Font oldFont = g2.getFont();
        g2.setFont(FONT_TIME);
        FontMetrics fm = g2.getFontMetrics();

        for (Buff buff : Buff.values()) {
            if (!buffs.isActive(buff)) continue;

            int seconds = buffs.secondsLeft(buff);

            g2.setColor(new Color(25, 20, 15, 210));
            g2.fillRoundRect(x, y, CELL_W, CELL_H, 8, 8);
            g2.setColor(new Color(220, 190, 150));
            g2.drawRoundRect(x, y, CELL_W, CELL_H, 8, 8);

            Item potion = potionFor(buff);
            if (potion != null && potion.icon != null) {
                g2.drawImage(potion.icon, x + (CELL_W - ICON) / 2, y + 3, ICON, ICON, null);
            }

            String time = PlayerBuffs.formatTime(seconds);
            g2.setColor(seconds <= WARN_SECONDS ? new Color(235, 100, 90) : Color.WHITE);
            g2.drawString(time, x + (CELL_W - fm.stringWidth(time)) / 2, y + CELL_H - 6);

            x += CELL_W + GAP;
        }

        g2.setFont(oldFont);
    }

    /** Ikon buff = ikon potion yang ngasih buff itu. */
    private Item potionFor(Buff buff) {
        for (PotionType type : PotionType.values()) {
            if (type.buff == buff) return ItemManager.get().getPotion(type);
        }
        return null;
    }
}
