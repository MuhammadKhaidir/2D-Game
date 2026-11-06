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

import Item.ItemManager;
import Main.GamePanel;
import Main.QuestManager;

/**
 * Panel Bounty Board.
 *
 * Dibuka dari BountyBoard (tekan [E] dekat papan). Selama panel kebuka dunia game berhenti (lihat GamePanel.update).
 * Menampilkan 3 misi dari QuestManager.QUESTS. Tombol tiap baris berubah sesuai status:
 *   ACCEPT : belum ada misi aktif, klik buat ambil misi ini
 *   ACTIVE : misi ini yang lagi diambil (progress ditampilkan)
 *   CLAIM  : misi ini sudah selesai, klik buat ambil hadiah
 *   (abu)  : ada misi lain yang lagi aktif, jadi misi ini terkunci
 *
 * Kontrol: klik tombol, ESC / tombol silang buat keluar.
 */
public class BountyBoardUI {

    public boolean visible = false;

    // ── ukuran ──
    private static final int PANEL_W = 540;
    private static final int PANEL_H = 360;
    private static final int ROW_W   = 500;
    private static final int ROW_H   = 68;
    private static final int ROW_GAP = 6;
    private static final int LIST_TOP = 78;
    private static final int BTN_W   = 92;
    private static final int BTN_H   = 34;

    // ── warna (senada sama PotionShopUI) ──
    private static final Color COL_DIM         = new Color(0, 0, 0, 130);
    private static final Color COL_PANEL       = new Color(30, 22, 16, 245);
    private static final Color COL_BORDER      = new Color(220, 190, 150);
    private static final Color COL_ACCENT      = new Color(250, 180, 40);
    private static final Color COL_ROW         = new Color(45, 34, 25);
    private static final Color COL_ROW_HOVER   = new Color(66, 50, 35);
    private static final Color COL_ROW_BORDER  = new Color(95, 75, 55);
    private static final Color COL_TEXT        = new Color(255, 235, 200);
    private static final Color COL_TEXT_DIM    = new Color(190, 170, 140);
    private static final Color COL_BTN         = new Color(60, 140, 70);
    private static final Color COL_BTN_HOVER   = new Color(85, 175, 95);
    private static final Color COL_BTN_CLAIM   = new Color(200, 140, 30);
    private static final Color COL_BTN_CLAIM_H = new Color(235, 175, 50);
    private static final Color COL_BTN_OFF     = new Color(70, 60, 55);
    private static final Color COL_BAR_BG      = new Color(25, 20, 15);
    private static final Color COL_BAR         = new Color(90, 180, 100);
    private static final Color COL_GOOD        = new Color(130, 220, 130);
    private static final Color COL_BAD         = new Color(235, 100, 90);

    // ── font ──
    private static final Font FONT_TITLE  = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_NAME   = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_DESC   = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_PRICE  = new Font("Monospaced", Font.BOLD, 14);
    private static final Font FONT_SMALL  = new Font("Monospaced", Font.PLAIN, 10);
    private static final Font FONT_BUTTON = new Font("Monospaced", Font.BOLD, 13);

    private static final int FEEDBACK_FRAMES = 100;

    private final GamePanel gp;

    private int hoverRow = -1;
    private boolean hoverBtn = false;

    private String feedback = "";
    private boolean feedbackGood = true;
    private int feedbackTimer = 0;

    public BountyBoardUI(GamePanel gp) {
        this.gp = gp;
    }

    // ===================================================================
    // BUKA / TUTUP / UPDATE
    // ===================================================================

    public void open() {
        visible = true;
        hoverRow = -1;
        hoverBtn = false;
        feedback = "";
        feedbackTimer = 0;
        gp.KeyH.escPressed = false;
        gp.KeyH.interactPressed = false;
    }

    public void close() {
        visible = false;
        hoverRow = -1;
        hoverBtn = false;
    }

    /** Dipanggil GamePanel.update() tiap frame selama panel terbuka. */
    public void update() {
        if (!visible) return;

        gp.KeyH.interactPressed = false; // E yang masih ketahan jangan sampai kebaca lagi pas panel ditutup
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

    private Rectangle btnRect(int i) {
        Rectangle row = rowRect(i);
        return new Rectangle(row.x + row.width - BTN_W - 10, row.y + (ROW_H - BTN_H) / 2, BTN_W, BTN_H);
    }

    // ===================================================================
    // MOUSE
    // ===================================================================

    public void handleMouseMove(MouseEvent e) {
        if (!visible) return;
        hoverRow = -1;
        hoverBtn = false;
        for (int i = 0; i < QuestManager.QUESTS.length; i++) {
            if (rowRect(i).contains(e.getPoint())) {
                hoverRow = i;
                hoverBtn = btnRect(i).contains(e.getPoint());
                break;
            }
        }
    }

    public void handleMouseClick(MouseEvent e) {
        if (!visible || e.getButton() != MouseEvent.BUTTON1) return;

        // tombol silang di pojok kanan atas panel: tutup panel
        if (CloseButton.rect(panelX(), panelY(), PANEL_W, 12, 8).contains(e.getPoint())) {
            close();
            return;
        }
        QuestManager qm = gp.quests;
        for (int i = 0; i < QuestManager.QUESTS.length; i++) {
            if (!btnRect(i).contains(e.getPoint())) continue;

            if (qm.getActive() == i && qm.isReady()) {
                int reward = qm.claim();
                showFeedback("Reward claimed: +" + reward + " Coin", true);
            } else if (!qm.hasActive()) {
                qm.accept(i);
                showFeedback("Bounty accepted: " + QuestManager.QUESTS[i].title(), true);
            } else if (qm.getActive() != i) {
                showFeedback("Finish your current bounty first.", false);
            }
            return;
        }
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

        // redupin layar di belakang panel
        g2.setColor(COL_DIM);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        drawPanel(g2, coins, im);
        for (int i = 0; i < QuestManager.QUESTS.length; i++) {
            drawRow(g2, i, im);
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

        g2.setFont(FONT_TITLE);
        g2.setColor(COL_ACCENT);
        drawCentered(g2, "Bounty Board", px + PANEL_W / 2, py + 32);

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

        CloseButton.draw(g2, CloseButton.rect(px, py, PANEL_W, 12, 8));
    }

    private void drawRow(Graphics2D g2, int index, ItemManager im) {
        QuestManager qm = gp.quests;
        QuestManager.Quest q = QuestManager.QUESTS[index];
        Rectangle row = rowRect(index);
        Rectangle btn = btnRect(index);

        boolean isActive = qm.getActive() == index;
        boolean ready = isActive && qm.isReady();
        boolean locked = qm.hasActive() && !isActive;

        // latar baris
        g2.setColor(index == hoverRow ? COL_ROW_HOVER : COL_ROW);
        g2.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
        g2.setColor(isActive ? COL_ACCENT : COL_ROW_BORDER);
        g2.drawRoundRect(row.x, row.y, row.width, row.height, 12, 12);

        // judul + keterangan
        int textX = row.x + 14;
        g2.setFont(FONT_NAME);
        g2.setColor(locked ? COL_TEXT_DIM : COL_TEXT);
        g2.drawString(q.title(), textX, row.y + 24);

        g2.setFont(FONT_DESC);
        if (isActive) {
            int shown = Math.min(qm.getProgress(), q.goal);
            g2.setColor(ready ? COL_GOOD : COL_TEXT_DIM);
            g2.drawString(ready ? "Completed! Claim your reward." : "Progress: " + shown + " / " + q.goal, textX, row.y + 42);

            // progress bar
            int barW = 190, barH = 8, barY = row.y + 50;
            g2.setColor(COL_BAR_BG);
            g2.fillRoundRect(textX, barY, barW, barH, 6, 6);
            g2.setColor(COL_BAR);
            g2.fillRoundRect(textX, barY, (int) Math.round(barW * (shown / (double) q.goal)), barH, 6, 6);
            g2.setColor(COL_ROW_BORDER);
            g2.drawRoundRect(textX, barY, barW, barH, 6, 6);
        } else {
            g2.setColor(COL_TEXT_DIM);
            g2.drawString("Defeat " + q.goal + " " + q.targetName + ".", textX, row.y + 42);
        }

        // hadiah
        int rewardX = row.x + 268;
        g2.setFont(FONT_SMALL);
        g2.setColor(COL_TEXT_DIM);
        g2.drawString("Reward", rewardX, row.y + 26);
        drawCoinIcon(g2, im, rewardX, row.y + 33, 18);
        g2.setFont(FONT_PRICE);
        g2.setColor(locked ? COL_TEXT_DIM : COL_ACCENT);
        g2.drawString(String.valueOf(q.reward), rewardX + 24, row.y + 47);

        // tombol
        String label;
        Color fill;
        boolean enabled = true;
        boolean hover = index == hoverRow && hoverBtn;
        if (ready) {
            label = "CLAIM";
            fill = hover ? COL_BTN_CLAIM_H : COL_BTN_CLAIM;
        } else if (isActive) {
            label = "ACTIVE";
            fill = COL_BTN_OFF;
            enabled = false;
        } else if (locked) {
            label = "ACCEPT";
            fill = COL_BTN_OFF;
            enabled = false;
        } else {
            label = "ACCEPT";
            fill = hover ? COL_BTN_HOVER : COL_BTN;
        }
        g2.setColor(fill);
        g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);
        g2.setColor(enabled ? new Color(220, 255, 220) : new Color(130, 115, 105));
        g2.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);
        g2.setFont(FONT_BUTTON);
        g2.setColor(enabled ? Color.WHITE : new Color(150, 135, 125));
        drawCentered(g2, label, btn.x + btn.width / 2, btn.y + 22);
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
        drawCentered(g2, "One bounty at a time    ESC to leave", cx, py + PANEL_H - 16);
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
