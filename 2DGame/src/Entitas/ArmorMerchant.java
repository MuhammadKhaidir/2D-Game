package Entitas;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

import Main.GamePanel;

/**
 * NPC penjual armor (dan Fishing Rod). Berdiri diam di sebelah rumah tempat spawn, animasi idle, dan kalau di-talk (E)
 * muncul dialog; pilih "Show me your wares" buat buka toko (UI.ArmorShopUI). Bayarnya pakai Coin.
 *
 * Strukturnya sengaja sama kayak {@link PotionMerchant}.
 */
public class ArmorMerchant extends Entity {

    public static final String DIALOG_ID = "armormerchant";
    private static final String DISPLAY_NAME = "Armor Merchant";
    private static final String TALK_HINT    = "[E] Talk";

    // ===== SPRITE =====
    // 8 frame idle, dibuat tools/generate_armor_merchant.py (recolor sprite penjual lama).
    // Kalau nanti punya sprite sendiri, timpa file-nya atau ganti prefix ini.
    private static final String SPRITE_PREFIX = "/NPCs/ArmorMerchantNPC00";
    private static final int FRAME_COUNT = 8;
    private static final int FRAME_DELAY = 7;

    // Ukuran gambar dalam satuan tileSize (2.5 = 2.5x tileSize). Bisa desimal.
    private static final double SCALE = 2.5;

    // Posisi spawn (kolom, baris tile): di rumput sebelah kanan rumah spawn, pas di atas jalan.
    private static final int SPAWN_COL = 27;
    private static final int SPAWN_ROW = 20;

    private final BufferedImage[] idleFrames = new BufferedImage[FRAME_COUNT];
    private int frameIndex = 0;
    private int frameTimer = 0;

    public ArmorMerchant(GamePanel gp) {
        super(gp);

        x = gp.tileSize * SPAWN_COL;
        y = gp.tileSize * SPAWN_ROW;

        direction = "down";
        speed     = 0;
        npcState  = NPCState.STAY;
        name      = DISPLAY_NAME;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        dialogId = DIALOG_ID;

        loadFrames();
    }

    // ===================================================================
    // DIALOG
    // ===================================================================

    /** Daftarin dialog merchant ke dialogMap. Dipanggil GamePanel.setupDialogs(). */
    public static void registerDialog(GamePanel gp) {
        DialogNode root = new DialogNode(
            "Iron plate, hammered by hand. A good set of armor saves more lives than any potion. "
          + "And if you fancy a quiet afternoon by the water, I've got fishing rods too."
        );
        root.addOption("Show me your wares.", null, () -> gp.armorShop.open());
        root.addOption("Nevermind.", null);

        gp.dialogMap.put(DIALOG_ID, root);
    }

    public void initDialog() {
        dialogRoot    = gp.dialogMap.get(dialogId);
        currentDialog = dialogRoot;
    }

    // ===================================================================
    // SPRITE
    // ===================================================================

    private void loadFrames() {
        for (int i = 0; i < FRAME_COUNT; i++) {
            String path = SPRITE_PREFIX + i + ".png";
            try (InputStream is = getClass().getResourceAsStream(path)) {
                idleFrames[i] = (is != null) ? ImageIO.read(is) : null;
            } catch (IOException e) {
                idleFrames[i] = null;
            }
            if (idleFrames[i] == null) {
                System.err.println("[ArmorMerchant] Gagal load: " + path);
            }
        }
    }

    /** Kotak gambar di layar. Kaki NPC tetap di titik (x, y); gambar "tumbuh" ke atas dan melebar ke kiri-kanan. */
    private Rectangle spriteBounds() {
        int w = (int) (gp.tileSize * SCALE);
        int h = (int) (gp.tileSize * SCALE);

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        return new Rectangle(
            screenX - (w - gp.tileSize) / 2,
            screenY - (h - gp.tileSize),
            w, h
        );
    }

    // ===================================================================
    // UPDATE & DRAW
    // ===================================================================

    @Override
    public void setAction() {}

    @Override
    public void update() {
        frameTimer++;
        if (frameTimer >= FRAME_DELAY) {
            frameIndex = (frameIndex + 1) % idleFrames.length;
            frameTimer = 0;
        }

        if (talkCooldown > 0) talkCooldown--;
    }

    @Override
    public void draw(Graphics2D g2) {
        Rectangle b = spriteBounds();

        // di luar layar → gak usah digambar
        if (b.x + b.width < gp.viewMinX() || b.x > gp.viewMaxX() ||
            b.y + b.height < gp.viewMinY() || b.y > gp.viewMaxY()) return;

        drawShadow(g2);

        BufferedImage frame = idleFrames[frameIndex];
        if (frame != null) {
            g2.drawImage(frame, b.x, b.y, b.width, b.height, null);
        } else {
            // fallback kalau sprite gak ketemu: kotak abu
            g2.setColor(new Color(170, 180, 190, 200));
            g2.fillRect(b.x, b.y, b.width, b.height);
            g2.setColor(Color.BLACK);
            g2.drawRect(b.x, b.y, b.width, b.height);
            g2.setFont(new Font("Dialog", Font.BOLD, 10));
            g2.drawString("AM", b.x + 10, b.y + b.height / 2);
        }
    }

    /** Nama di atas kepala + hint "[E] Talk" kalau player cukup dekat. */
    @Override
    public void drawOverhead(Graphics2D g2) {
        Rectangle b = spriteBounds();
        int centerX = b.x + b.width / 2;
        int nameY = b.y - 6;

        g2.setFont(new Font("Dialog", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 215, 230));
        g2.drawString(DISPLAY_NAME, centerX - fm.stringWidth(DISPLAY_NAME) / 2, nameY);

        if (gp.player.getInteractArea().intersects(getInteractArea())) {
            g2.setFont(new Font("Dialog", Font.PLAIN, 11));
            fm = g2.getFontMetrics();
            int tagW = fm.stringWidth(TALK_HINT);
            int tagX = centerX - tagW / 2;
            int tagY = nameY - 16;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(tagX - 4, tagY - 12, tagW + 8, 16, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(TALK_HINT, tagX, tagY);
        }
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
