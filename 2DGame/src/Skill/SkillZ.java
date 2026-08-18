package Skill;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import Entitas.player;
import Main.GamePanel;

public class SkillZ extends Skill {

    // ===== EFEK SKILL =====
    private float alpha = 0f;          // transparansi gambar skill
    private int displayW = 200;        // lebar gambar skill di layar
    private int displayH = 200;        // tinggi gambar skill di layar

    // Afterimage trail
    private static final int TRAIL_MAX = 5;
    private int[] trailX = new int[TRAIL_MAX];
    private int[] trailY = new int[TRAIL_MAX];
    private float[] trailAlpha = new float[TRAIL_MAX];
    private int trailIndex = 0;

    // Dash speed
    private int dashSpeed = 12;
    private int dashFrames = 8; // frame bergerak cepat

    public SkillZ(GamePanel gp, player owner) {
        super(gp, owner);

        cooldownMax = 80;       // 2 detik di 40 FPS
        activeDuration = 20;    // 0.5 detik total

        // Ganti path ini ke aset skill kamu nanti
        // imagePath = "/Aset/SkillZ.png";
        // Sementara pakai null (tidak ada gambar), efek tetap jalan
        imagePath = "";
    }

    @Override
    protected void onActivate() {
        alpha = 1f;
        trailIndex = 0;

        // Reset trail
        for (int i = 0; i < TRAIL_MAX; i++) {
            trailX[i] = owner.x;
            trailY[i] = owner.y;
            trailAlpha[i] = 0f;
        }

        System.out.println("[SkillZ] Activated!");
    }

    @Override
    protected void onActiveUpdate() {

        // ===== DASH MOVEMENT =====
        if (activeTimer <= dashFrames) {
            // Simpan posisi lama buat trail
            trailX[trailIndex % TRAIL_MAX] = owner.x;
            trailY[trailIndex % TRAIL_MAX] = owner.y;
            trailAlpha[trailIndex % TRAIL_MAX] = 0.6f;
            trailIndex++;

            // Gerak cepat ke arah yang sedang dihadap
            switch (owner.direction) {
                case "up":    owner.y -= dashSpeed; break;
                case "down":  owner.y += dashSpeed; break;
                case "left":  owner.x -= dashSpeed; break;
                case "right": owner.x += dashSpeed; break;
            }
        }

        // Fade out alpha gambar skill
        alpha = 1f - ((float) activeTimer / activeDuration);

        // Fade out trail
        for (int i = 0; i < TRAIL_MAX; i++) {
            if (trailAlpha[i] > 0) {
                trailAlpha[i] -= 0.08f;
                if (trailAlpha[i] < 0) trailAlpha[i] = 0;
            }
        }
    }

    @Override
    protected void onEnd() {
        alpha = 0f;
        System.out.println("[SkillZ] Ended.");
    }

    @Override
    protected void onDraw(Graphics2D g2) {

        int screenX = owner.screenX;
        int screenY = owner.screenY;

        // ===== GAMBAR TRAIL (afterimage) =====
        for (int i = 0; i < TRAIL_MAX; i++) {
            if (trailAlpha[i] <= 0) continue;

            int tScreenX = trailX[i] - gp.player.x + gp.player.screenX;
            int tScreenY = trailY[i] - gp.player.y + gp.player.screenY;

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, trailAlpha[i]));
            g2.setColor(new Color(100, 180, 255)); // warna biru muda buat afterimage
            g2.fillRect(tScreenX + 4, tScreenY + 4, gp.tileSize - 8, gp.tileSize - 8);
            g2.setComposite(old);
        }

        // ===== GAMBAR EFEK DI ATAS PLAYER =====
        if (skillImage != null && alpha > 0) {
            // Gambar skill image gede di tengah player
            int imgX = screenX + gp.tileSize / 2 - displayW / 2;
            int imgY = screenY - displayH + 10;

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(skillImage, imgX, imgY, displayW, displayH, null);
            g2.setComposite(old);

        } else if (alpha > 0) {
            // Fallback kalau gambar belum ada: lingkaran efek
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.7f));
            g2.setColor(new Color(100, 200, 255));
            int r = (int)(40 * alpha);
            g2.fillOval(screenX + gp.tileSize / 2 - r, screenY + gp.tileSize / 2 - r, r * 2, r * 2);
            g2.setComposite(old);
        }

        // ===== COOLDOWN BAR (muncul setelah skill selesai) =====
        drawCooldownBar(g2, screenX, screenY);
    }

    // Bar kecil di bawah player menunjukkan sisa cooldown
    private void drawCooldownBar(Graphics2D g2, int screenX, int screenY) {
        int barW = gp.tileSize;
        int barH = 3;
        int bx = screenX;
        int by = screenY + gp.tileSize + 4;

        // Background bar
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(bx, by, barW, barH);

        // Fill bar
        float pct = getCooldownPercent();
        g2.setColor(new Color(100, 200, 255));
        g2.fillRect(bx, by, (int)(barW * pct), barH);
    }
}