package Entitas;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import Main.GamePanel;
import Entitas.DialogNode;

public class WeaponDealer extends Entity {

    private BufferedImage[] idleFrames = new BufferedImage[8];

    private int idleFrameIndex = 0;
    private int idleFrameTimer = 0;
    private static final int FRAME_DELAY = 7;

    private static final String DEALER_NAME = "Weapon Dealer";
    private static final String DEALER_TAG  = "[E] Talk";

    // ===== UKURAN GAMBAR — ubah angka ini sesukamu =====
    // 1 = satu tileSize (kecil), 2 = dua tileSize (normal NPC besar), dst
    // Bisa pakai desimal, misal 1.5 = 1.5x tileSize
    private static final double SCALE = 2.5;

    public WeaponDealer(GamePanel gp) {
        super(gp);

        x = gp.tileSize * 10;
        y = gp.tileSize * 26;

        direction = "down";
        speed     = 0;
        npcState  = NPCState.STAY;
        name      = DEALER_NAME;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        dialogId = "weapondealer";

        loadImages();
    }

    public void initDialog() {
        dialogRoot    = gp.dialogMap.get(dialogId);
        currentDialog = dialogRoot;
    }

    private void loadImages() {
        for (int i = 0; i <= 7; i++) {
            String filename = String.format("/NPCs/WeaponDealerNPC00%d.png", i);
            try {
                idleFrames[i] = ImageIO.read(
                    getClass().getResourceAsStream(filename)
                );
            } catch (IOException | IllegalArgumentException e) {
                System.err.println("[WeaponDealer] Gagal load: " + filename);
                idleFrames[i] = null;
            }
        }
    }

    @Override
    public void setAction() {}

    @Override
    public void update() {
        idleFrameTimer++;
        if (idleFrameTimer >= FRAME_DELAY) {
            idleFrameIndex = (idleFrameIndex + 1) % idleFrames.length;
            idleFrameTimer = 0;
        }

        if (chatTimer > 0) {
            chatTimer--;
            if (chatTimer == 0) chatText = "";
        }

        if (talkCooldown > 0) talkCooldown--;
    }

    @Override
    public void draw(Graphics2D g2) {
        // Hitung ukuran gambar dari SCALE
        int drawW = (int)(gp.tileSize * SCALE);
        int drawH = (int)(gp.tileSize * SCALE);

        // Posisi screen — kaki dealer tetap di titik y (tidak melayang)
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        // Offset Y supaya gambar tumbuh ke atas, bukan ke bawah
        // (mirip cara Entity.draw() untuk NPC besar)
        int drawX = screenX - (drawW - gp.tileSize) / 2; // tengah horizontal
        int drawY = screenY - (drawH - gp.tileSize);      // kaki tetap di bawah

        // Cull
        if (drawX + drawW < 0 || drawX > gp.screenWidth ||
            drawY + drawH < 0 || drawY > gp.screenHeight) return;

        drawShadow(g2);

        BufferedImage frame = idleFrames[idleFrameIndex];
        if (frame != null) {
            g2.drawImage(frame, drawX, drawY, drawW, drawH, null);
        } else {
            // Fallback kotak kuning
            g2.setColor(new Color(255, 200, 50, 200));
            g2.fillRect(drawX, drawY, drawW, drawH);
            g2.setColor(Color.BLACK);
            g2.drawRect(drawX, drawY, drawW, drawH);
            g2.setFont(new Font("Dialog", Font.BOLD, 10));
            g2.drawString("WD", drawX + 10, drawY + drawH / 2);
        }

      //  drawDealerLabel(g2, screenX, screenY, drawY);

        if (!chatText.isEmpty()) drawChatBubble(g2, screenX, drawY);
    }

    private void drawDealerLabel(Graphics2D g2, int screenX, int screenY, int drawY) {
        g2.setFont(new Font("Dialog", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        int nameW = fm.stringWidth(DEALER_NAME);
        int nameX = screenX + gp.tileSize / 2 - nameW / 2;
        int nameY = drawY - 8; // di atas gambar

        g2.setColor(new Color(255, 220, 60));
        g2.drawString(DEALER_NAME, nameX, nameY);

        // Tag [E] Talk — hanya muncul kalau player dekat
        int distX = Math.abs(gp.player.x - x);
        int distY = Math.abs(gp.player.y - y);
        if (distX < gp.tileSize * 2 && distY < gp.tileSize * 2) {
            g2.setFont(new Font("Dialog", Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            int tagW = fm.stringWidth(DEALER_TAG);
            int tagX = screenX + gp.tileSize / 2 - tagW / 2;
            int tagY = nameY - 14;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(tagX - 4, tagY - 12, tagW + 8, 16, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(DEALER_TAG, tagX, tagY);
        }
    }

    private void drawChatBubble(Graphics2D g2, int screenX, int drawY) {
        g2.setFont(new Font("Dialog", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();

        int bubbleW = fm.stringWidth(chatText) + 20;
        int bubbleH = 22;
        int bx = screenX + gp.tileSize / 2 - bubbleW / 2;
        int by = drawY - 48;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(bx, by, bubbleW, bubbleH, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(bx, by, bubbleW, bubbleH, 10, 10);
        g2.drawString(chatText, bx + 10, by + 15);
    }

    @Override
    public Rectangle getInteractArea() {
        return new Rectangle(
            x - gp.tileSize,
            y - gp.tileSize,
            gp.tileSize * 3,
            gp.tileSize * 3
        );
    }
}