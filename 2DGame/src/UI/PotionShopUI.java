package UI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import Item.Item;
import Item.ItemManager;
import Item.PotionType;
import Main.GamePanel;

/**
 * Toko Potion Merchant.
 *
 * Dibuka dari dialog PotionMerchant (opsi "Show me your potions"). Selama toko terbuka dunia game
 * berhenti (lihat GamePanel.update). Semua potion di PotionType otomatis muncul sebagai satu baris:
 * ikon, nama, efek, harga dalam Coin, dan tombol BUY.
 *
 * Kontrol: klik BUY buat beli 1 potion, ESC buat keluar.
 */
public class PotionShopUI {

    public boolean visible = false;

    // ── ukuran ──
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 490;
    private static final int ROW_W   = 500;
    private static final int ROW_H   = 56;
    private static final int ROW_GAP = 4;
    private static final int LIST_TOP = 78;      // jarak dari atas panel ke baris pertama
    private static final int BUY_W   = 76;
    private static final int BUY_H   = 32;

    // ── warna (senada sama Hotbar) ──
    private static final Color COL_DIM         = new Color(0, 0, 0, 130);
    private static final Color COL_PANEL       = new Color(30, 22, 16, 245);
    private static final Color COL_BORDER      = new Color(220, 190, 150);
    private static final Color COL_ACCENT      = new Color(250, 180, 40);
    private static final Color COL_ROW         = new Color(45, 34, 25);
    private static final Color COL_ROW_HOVER   = new Color(66, 50, 35);
    private static final Color COL_ROW_BORDER  = new Color(95, 75, 55);
    private static final Color COL_ICON_BG     = new Color(25, 20, 15);
    private static final Color COL_TEXT        = new Color(255, 235, 200);
    private static final Color COL_TEXT_DIM    = new Color(190, 170, 140);
    private static final Color COL_BUY         = new Color(60, 140, 70);
    private static final Color COL_BUY_HOVER   = new Color(85, 175, 95);
    private static final Color COL_BUY_OFF     = new Color(70, 60, 55);
    private static final Color COL_GOOD        = new Color(130, 220, 130);
    private static final Color COL_BAD         = new Color(235, 100, 90);

    // ── font ──
    private static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_NAME   = new Font("Monospaced", Font.BOLD, 13);
    private static final Font FONT_DESC   = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_PRICE  = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN, 10);
    private static final Font FONT_BUTTON = new Font("Monospaced", Font.BOLD, 13);

    private static final int FEEDBACK_FRAMES = 100;

    private final GamePanel gp;
    private final PotionType[] stock = PotionType.values();

    private int hoverRow = -1;
    private boolean hoverBuy = false;

    private String feedback = "";
    private boolean feedbackGood = true;
    private int feedbackTimer = 0;

    public PotionShopUI(GamePanel gp) {
        this.gp = gp;
    }

    // ===================================================================
    // BUKA / TUTUP / UPDATE
    // ===================================================================

    public void open() {
        visible = true;
        hoverRow = -1;
        hoverBuy = false;
        feedback = "";
        feedbackTimer = 0;
        gp.KeyH.escPressed = false;
    }

    public void close() {
        visible = false;
        hoverRow = -1;
        hoverBuy = false;
    }

    /** Dipanggil GamePanel.update() tiap frame selama toko terbuka. */
    public void update() {
        if (!visible) return;

        if (gp.KeyH.escPressed) {
            close();
            gp.KeyH.escPressed = false;
            return;
        }
        if (feedbackTimer > 0) feedbackTimer--;
    }

    // ===================================================================
    // GEOMETRI
    // ===================================================================

    private int panelX() { return (gp.screenWidth - PANEL_W) / 2; }
    private int panelY() { return (gp.screenHeight - PANEL_H) / 2 - 14; }

    private Rectangle rowRect(int i) {
        int x = panelX() + (PANEL_W - ROW_W) / 2;
        int y = panelY() + LIST_TOP + i * (ROW_H + ROW_GAP);
        return new Rectangle(x, y, ROW_W, ROW_H);
    }

    private Rectangle buyRect(int i) {
        Rectangle row = rowRect(i);
        return new Rectangle(row.x + row.width - BUY_W - 10, row.y + (ROW_H - BUY_H) / 2, BUY_W, BUY_H);
    }

    // ===================================================================
    // MOUSE
    // ===================================================================

    public void handleMouseMove(MouseEvent e) {
        if (!visible) return;
        hoverRow = -1;
        hoverBuy = false;
        for (int i = 0; i < stock.length; i++) {
            if (rowRect(i).contains(e.getPoint())) {
                hoverRow = i;
                hoverBuy = buyRect(i).contains(e.getPoint());
                break;
            }
        }
    }

    public void handleMouseClick(MouseEvent e) {
        if (!visible || e.getButton() != MouseEvent.BUTTON1) return;

        // tombol silang di pojok kanan atas panel: tutup toko
        if (CloseButton.rect(panelX(), panelY(), PANEL_W, 12, 8).contains(e.getPoint())) {
            close();
            return;
        }
        for (int i = 0; i < stock.length; i++) {
            if (buyRect(i).contains(e.getPoint())) {
                buy(stock[i]);
                return;
            }
        }
    }

    // ===================================================================
    // BELI
    // ===================================================================

    private void buy(PotionType type) {
        ItemManager im = ItemManager.get();
        Item potion = im.getPotion(type);

        if (im.count(potion) >= potion.maxStack) {
            showFeedback("You can't carry any more " + type.itemName + ".", false);
            return;
        }
        if (!im.remove(im.COIN, type.price)) {
            showFeedback("Not enough coins!", false);
            return;
        }

        im.add(potion, 1);
        showFeedback("Bought " + type.itemName + " (-" + type.price + " coins)", true);
    }

    private void showFeedback(String message, boolean good) {
        feedback = message;
        feedbackGood = good;
        feedbackTimer = FEEDBACK_FRAMES;
    }

    // ===================================================================
    // GAMBAR
    // ===================================================================

    public void draw(Graphics2D g2) {
        if (!visible) return;

        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();

        ItemManager im = ItemManager.get();
        int coins = im.count(im.COIN);

        // redupin layar di belakang toko
        g2.setColor(COL_DIM);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        drawPanel(g2, coins, im);

        for (int i = 0; i < stock.length; i++) {
            drawRow(g2, i, stock[i], im, coins);
        }

        drawFooter(g2);

        g2.setFont(oldFont);
        g2.setStroke(oldStroke);
    }

    private void drawPanel(Graphics2D g2, int coins, ItemManager im) {
        int px = panelX();
        int py = panelY();

        g2.setColor(COL_PANEL);
        g2.fillRoundRect(px, py, PANEL_W, PANEL_H, 20, 20);
        g2.setColor(COL_BORDER);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(px, py, PANEL_W, PANEL_H, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // judul
        g2.setFont(FONT_TITLE);
        g2.setColor(COL_ACCENT);
        drawCentered(g2, "Potion Merchant", px + PANEL_W / 2, py + 32);

        g2.setColor(new Color(COL_BORDER.getRed(), COL_BORDER.getGreen(), COL_BORDER.getBlue(), 120));
        g2.drawLine(px + 20, py + 44, px + PANEL_W - 20, py + 44);

        // saldo coin
        int listLeft = rowRect(0).x;
        g2.setFont(FONT_PRICE);
        g2.setColor(COL_TEXT);
        g2.drawString("Your coins:", listLeft, py + 66);
        int textW = g2.getFontMetrics().stringWidth("Your coins:");
        drawCoinIcon(g2, im, listLeft + textW + 8, py + 52, 18);
        g2.setColor(COL_ACCENT);
        g2.drawString(String.valueOf(coins), listLeft + textW + 32, py + 66);

        // tombol silang buat nutup toko
        CloseButton.draw(g2, CloseButton.rect(px, py, PANEL_W, 12, 8));
    }

    private void drawRow(Graphics2D g2, int index, PotionType type, ItemManager im, int coins) {
        Rectangle row = rowRect(index);
        Rectangle buy = buyRect(index);
        Item potion = im.getPotion(type);
        boolean affordable = coins >= type.price;

        // latar baris
        g2.setColor(index == hoverRow ? COL_ROW_HOVER : COL_ROW);
        g2.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
        g2.setColor(COL_ROW_BORDER);
        g2.drawRoundRect(row.x, row.y, row.width, row.height, 12, 12);

        // ikon
        int iconBox = 44;
        int iconX = row.x + 8;
        int iconY = row.y + (ROW_H - iconBox) / 2;
        g2.setColor(COL_ICON_BG);
        g2.fillRoundRect(iconX, iconY, iconBox, iconBox, 8, 8);
        g2.setColor(COL_ROW_BORDER);
        g2.drawRoundRect(iconX, iconY, iconBox, iconBox, 8, 8);
        if (potion != null && potion.icon != null) {
            g2.drawImage(potion.icon, iconX + 4, iconY + 4, iconBox - 8, iconBox - 8, null);
        }

        // nama + efek
        int textX = iconX + iconBox + 10;
        g2.setFont(FONT_NAME);
        g2.setColor(COL_TEXT);
        g2.drawString(type.itemName, textX, row.y + 24);
        g2.setFont(FONT_DESC);
        g2.setColor(COL_TEXT_DIM);
        g2.drawString(type.shortDescription(), textX, row.y + 42);

        // harga + jumlah yang dimiliki
        int priceX = row.x + 296;
        drawCoinIcon(g2, im, priceX, row.y + 10, 18);
        g2.setFont(FONT_PRICE);
        g2.setColor(affordable ? COL_ACCENT : COL_BAD);
        g2.drawString(String.valueOf(type.price), priceX + 24, row.y + 24);
        g2.setFont(FONT_SMALL);
        g2.setColor(COL_TEXT_DIM);
        g2.drawString("Owned: " + (potion != null ? im.count(potion) : 0), priceX, row.y + 44);

        // tombol BUY
        boolean buyHover = index == hoverRow && hoverBuy;
        g2.setColor(!affordable ? COL_BUY_OFF : (buyHover ? COL_BUY_HOVER : COL_BUY));
        g2.fillRoundRect(buy.x, buy.y, buy.width, buy.height, 10, 10);
        g2.setColor(affordable ? new Color(220, 255, 220) : new Color(130, 115, 105));
        g2.drawRoundRect(buy.x, buy.y, buy.width, buy.height, 10, 10);
        g2.setFont(FONT_BUTTON);
        g2.setColor(affordable ? Color.WHITE : new Color(150, 135, 125));
        drawCentered(g2, "BUY", buy.x + buy.width / 2, buy.y + 21);
    }

    private void drawFooter(Graphics2D g2) {
        int px = panelX();
        int py = panelY();
        int cx = px + PANEL_W / 2;

        if (feedbackTimer > 0 && !feedback.isEmpty()) {
            g2.setFont(FONT_NAME);
            g2.setColor(feedbackGood ? COL_GOOD : COL_BAD);
            drawCentered(g2, feedback, cx, py + PANEL_H - 36);
        }

        g2.setFont(FONT_SMALL);
        g2.setColor(COL_TEXT_DIM);
        drawCentered(g2, "Click BUY to purchase    ESC to leave", cx, py + PANEL_H - 16);
    }

    // ── helper gambar ──

    private void drawCoinIcon(Graphics2D g2, ItemManager im, int x, int y, int size) {
        BufferedImage icon = im.COIN != null ? im.COIN.icon : null;
        if (icon != null) {
            g2.drawImage(icon, x, y, size, size, null);
        } else {
            g2.setColor(COL_ACCENT);
            g2.fillOval(x, y, size, size);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int centerX, int baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }
}
