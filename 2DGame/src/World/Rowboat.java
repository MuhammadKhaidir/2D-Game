package World;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import Main.GamePanel;
import bahan.WaterCollision;

/**
 * Perahu sampan yang dapat dinaiki dan dikendalikan di dunia luar.
 * Koordinat worldX/worldY adalah titik tengah lambung; sprite default menghadap ke selatan.
 */
public final class Rowboat {

    private static final int SPRITE_WIDTH = 112;
    private static final int SPRITE_HEIGHT = 246;
    private static final float MAX_SPEED = 4.2f;
    private static final float ACCELERATION = 0.14f;
    private static final float WATER_SIDE_CHECK = 34f;
    private static final float WATER_FRONT_CHECK = 78f;

    private final GamePanel gp;
    private final List<WakeRipple> wake = new ArrayList<>();
    private BufferedImage sprite;

    private float worldX;
    private float worldY;
    private float velocityX;
    private float velocityY;
    /** Sudut rotasi: 0 = menghadap bawah/selatan. */
    private float heading;
    private float bobPhase;
    private float bobOffset;
    private int wakeTimer;
    private boolean mounted;
    private boolean moving;

    public Rowboat(GamePanel gp) {
        this.gp = gp;

        // Titik sandar di pantai selatan daratan hijau utama, menghadap ke laut.
        // Pusat lambung berada di air; bagian buritan menempel ke bibir pantai.
        worldX = 140.5f * gp.tileSize;
        worldY = 204.5f * gp.tileSize;

        loadSprite();
    }

    private void loadSprite() {
        try (InputStream in = getClass().getResourceAsStream("/Aset/rowboat.png")) {
            if (in == null) {
                System.err.println("ROWBOAT SPRITE NOT FOUND: /Aset/rowboat.png");
                return;
            }
            sprite = ImageIO.read(in);
        } catch (Exception ex) {
            System.err.println("FAILED TO LOAD ROWBOAT SPRITE");
            ex.printStackTrace();
        }
    }

    public boolean isMounted() {
        return mounted;
    }

    public boolean isMoving() {
        return moving;
    }

    public String getFacingDirection() {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        if (Math.abs(forwardX) > Math.abs(forwardY)) {
            return forwardX > 0f ? "right" : "left";
        }
        return forwardY > 0f ? "down" : "up";
    }

    /** Dipanggil dari GamePanel.update(), sebelum player.update(). */
    public void update() {
        bobPhase += 0.075f;
        bobOffset = (float) Math.sin(bobPhase) * 2.3f;
        updateWakeParticles();

        if (gp.gameState != GamePanel.GameState.OUTSIDE || gp.player == null) return;

        // E dipakai sebagai tombol naik/turun perahu hanya saat player berada di dekat titik sandar.
        if (gp.KeyH.interactPressed) {
            if (mounted) {
                tryDismount();
                gp.KeyH.interactPressed = false;
            } else if (!gp.player.isDead && isNearBoardingPoint()) {
                mounted = true;
                velocityX = 0f;
                velocityY = 0f;
                moving = false;
                syncPlayerToSeat();
                gp.KeyH.interactPressed = false;
            }
        }

        if (!mounted) return;
        if (gp.player.isDead) {
            mounted = false;
            moving = false;
            velocityX = velocityY = 0f;
            return;
        }

        float inputX = (gp.KeyH.rightPressed ? 1f : 0f) - (gp.KeyH.leftPressed ? 1f : 0f);
        float inputY = (gp.KeyH.downPressed ? 1f : 0f) - (gp.KeyH.upPressed ? 1f : 0f);
        float inputLength = (float) Math.sqrt(inputX * inputX + inputY * inputY);
        if (inputLength > 1f) {
            inputX /= inputLength;
            inputY /= inputLength;
        }

        if (inputLength > 0f) {
            float targetVX = inputX * MAX_SPEED;
            float targetVY = inputY * MAX_SPEED;
            velocityX += (targetVX - velocityX) * ACCELERATION;
            velocityY += (targetVY - velocityY) * ACCELERATION;
        } else {
            // Momentum perahu melambat bertahap, bukan berhenti mendadak.
            velocityX *= 0.91f;
            velocityY *= 0.91f;
        }

        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        moving = speed > 0.32f;
        if (moving) heading = (float) Math.atan2(velocityX, velocityY);

        float nextX = worldX + velocityX;
        float nextY = worldY + velocityY;
        if (speed > 0.02f) {
            if (canFloatAt(nextX, nextY, heading)) {
                worldX = nextX;
                worldY = nextY;
            } else {
                // Lambung tidak boleh masuk ke daratan; momentum dipatahkan saat menyentuh pantai.
                velocityX *= 0.18f;
                velocityY *= 0.18f;
                if (Math.abs(velocityX) < 0.12f) velocityX = 0f;
                if (Math.abs(velocityY) < 0.12f) velocityY = 0f;
            }
        }

        moving = Math.sqrt(velocityX * velocityX + velocityY * velocityY) > 0.32f;
        syncPlayerToSeat();

        if (moving && ++wakeTimer >= 6) {
            spawnWake();
            wakeTimer = 0;
        }
    }

    /** Mencegah player menjalankan movement biasa selama sedang duduk di perahu. */
    private void syncPlayerToSeat() {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        float seatX = worldX - forwardX * 14f;
        float seatY = worldY - forwardY * 14f;
        gp.player.x = Math.round(seatX - gp.tileSize / 2f);
        gp.player.y = Math.round(seatY - gp.tileSize / 2f);
        gp.player.direction = getFacingDirection();
        gp.player.collisionOn = false;
    }

    private boolean isNearBoardingPoint() {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        float boardX = worldX - forwardX * (SPRITE_HEIGHT * 0.47f);
        float boardY = worldY - forwardY * (SPRITE_HEIGHT * 0.47f);
        float playerX = gp.player.x + gp.tileSize / 2f;
        float playerY = gp.player.y + gp.tileSize / 2f;
        float dx = playerX - boardX;
        float dy = playerY - boardY;
        return dx * dx + dy * dy <= 178f * 178f;
    }

    private boolean tryDismount() {
        // Cari titik darat terdekat di sekeliling perahu; kalau terlalu jauh dari pantai, tetap di perahu.
        for (int radius = 88; radius <= 208; radius += 8) {
            for (int degree = 0; degree < 360; degree += 15) {
                double a = Math.toRadians(degree);
                float candidateX = worldX + (float) Math.cos(a) * radius;
                float candidateY = worldY + (float) Math.sin(a) * radius;
                if (!isLandPoint(candidateX, candidateY)) continue;

                gp.player.x = Math.round(candidateX - gp.tileSize / 2f);
                gp.player.y = Math.round(candidateY - gp.tileSize / 2f);
                gp.player.direction = getFacingDirection();
                gp.player.collisionOn = false;
                mounted = false;
                moving = false;
                velocityX = velocityY = 0f;
                return true;
            }
        }
        return false;
    }

    private boolean isLandPoint(float x, float y) {
        int col = (int) Math.floor(x / gp.tileSize);
        int row = (int) Math.floor(y / gp.tileSize);
        if (col < 0 || row < 0 || col >= gp.maxWorldCol || row >= gp.maxWorldRow) return false;
        if (gp.tileM == null || gp.tileM.gen == null) return false;
        int tile = gp.tileM.gen.baseTile(col, row);
        return !WaterCollision.isWaterTile(tile) && gp.tileM.gen.isLand(col, row);
    }

    private boolean canFloatAt(float x, float y, float direction) {
        float forwardX = (float) Math.sin(direction);
        float forwardY = (float) Math.cos(direction);
        float sideX = -forwardY;
        float sideY = forwardX;
        return isWaterPoint(x, y)
                && isWaterPoint(x + sideX * WATER_SIDE_CHECK, y + sideY * WATER_SIDE_CHECK)
                && isWaterPoint(x - sideX * WATER_SIDE_CHECK, y - sideY * WATER_SIDE_CHECK)
                && isWaterPoint(x + forwardX * WATER_FRONT_CHECK, y + forwardY * WATER_FRONT_CHECK);
    }

    private boolean isWaterPoint(float x, float y) {
        int col = (int) Math.floor(x / gp.tileSize);
        int row = (int) Math.floor(y / gp.tileSize);
        if (col < 0 || row < 0 || col >= gp.maxWorldCol || row >= gp.maxWorldRow) return false;
        if (gp.tileM == null || gp.tileM.gen == null) return false;

        int tile = gp.tileM.gen.baseTile(col, row);
        float localX = x / gp.tileSize - col;
        float localY = y / gp.tileSize - row;
        return WaterCollision.isWaterAt(tile, localX, localY);
    }

    private void spawnWake() {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        float sideX = -forwardY;
        float sideY = forwardX;
        float rearX = worldX - forwardX * (SPRITE_HEIGHT * 0.37f);
        float rearY = worldY - forwardY * (SPRITE_HEIGHT * 0.37f);
        wake.add(new WakeRipple(rearX + sideX * 19f, rearY + sideY * 19f, 5.5f));
        wake.add(new WakeRipple(rearX - sideX * 19f, rearY - sideY * 19f, 5.5f));
        if (wake.size() > 36) wake.remove(0);
    }

    private void updateWakeParticles() {
        Iterator<WakeRipple> it = wake.iterator();
        while (it.hasNext()) {
            WakeRipple particle = it.next();
            particle.life--;
            particle.radius += 0.20f;
            if (particle.life <= 0) it.remove();
        }
    }

    /** Digambar setelah ground/air, sebelum player dan objek yang berdiri. */
    public void draw(Graphics2D parent) {
        if (sprite == null) return;

        int worldWidth = gp.maxWorldCol * gp.tileSize;
        int worldHeight = gp.maxWorldRow * gp.tileSize;
        int camX = gp.player.x;
        int camY = gp.player.y;
        if (camX < gp.player.screenX) camX = gp.player.screenX;
        if (camX > worldWidth - gp.player.screenX) camX = worldWidth - gp.player.screenX;
        if (camY < gp.player.screenY) camY = gp.player.screenY;
        if (camY > worldHeight - gp.player.screenY) camY = worldHeight - gp.player.screenY;

        float screenX = worldX - camX + gp.player.screenX;
        float screenY = worldY - camY + gp.player.screenY + bobOffset;
        if (screenX + SPRITE_WIDTH < -100 || screenX - SPRITE_WIDTH > gp.screenWidth + 100
                || screenY + SPRITE_HEIGHT < -100 || screenY - SPRITE_HEIGHT > gp.screenHeight + 100) return;

        Graphics2D g2 = (Graphics2D) parent.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Bayangan lembut di bawah lambung.
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        g2.setColor(new Color(4, 18, 31));
        g2.fill(new Ellipse2D.Float(screenX - 39f, screenY - 91f, 78f, 186f));

        // Jejak riak memudar di belakang perahu.
        drawWake(g2, camX, camY);

        // Riak kecil saat perahu diam, supaya tetap terasa mengapung.
        if (!moving) drawIdleRipples(g2, screenX, screenY);

        float sway = (float) Math.sin(bobPhase * 0.62f) * (moving ? 0.008f : 0.018f);
        AffineTransform boatTransform = new AffineTransform();
        boatTransform.translate(screenX, screenY);
        boatTransform.rotate(-heading + sway);
        boatTransform.translate(-SPRITE_WIDTH / 2.0, -SPRITE_HEIGHT / 2.0);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(sprite, boatTransform, null);

        drawHint(g2, screenX, screenY);
        g2.dispose();
    }

    private void drawWake(Graphics2D g2, int camX, int camY) {
        for (WakeRipple particle : wake) {
            float progress = particle.life / 30f;
            float alpha = 0.30f * Math.max(0f, Math.min(1f, progress));
            float x = particle.x - camX + gp.player.screenX;
            float y = particle.y - camY + gp.player.screenY;
            float rx = particle.radius;
            float ry = particle.radius * 0.58f;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(new Color(177, 229, 244));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(x - rx, y - ry, rx * 2f, ry * 2f));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.42f));
            g2.fill(new Ellipse2D.Float(x - rx * 0.48f, y - ry * 0.48f, rx * 0.96f, ry * 0.96f));
        }
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void drawIdleRipples(Graphics2D g2, float screenX, float screenY) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(bobPhase * 0.8f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.13f + pulse * 0.09f));
        g2.setColor(new Color(178, 225, 242));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new Ellipse2D.Float(screenX - 43f, screenY - 9f, 86f, 30f));
        g2.draw(new Ellipse2D.Float(screenX - 35f, screenY + 1f, 70f, 20f));
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void drawHint(Graphics2D g2, float screenX, float screenY) {
        if (!mounted && !isNearBoardingPoint()) return;
        String label = mounted ? "E  (Get out of the boat)" : "E (Get on boat)";
        Font oldFont = g2.getFont();
        g2.setFont(new Font("Arial", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(label) + 18;
        int x = Math.round(screenX - width / 2f);
        int y = Math.round(screenY - SPRITE_HEIGHT / 2f - 29f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g2.setColor(new Color(11, 20, 27));
        g2.fillRoundRect(x, y, width, 22, 8, 8);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(new Color(239, 247, 250));
        g2.drawString(label, x + 9, y + 15);
        g2.setFont(oldFont);
    }

    private static final class WakeRipple {
        final float x;
        final float y;
        float radius;
        int life = 30;

        WakeRipple(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }
}
