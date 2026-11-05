package World;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import Main.GamePanel;
import bahan.WaterCollision;

/**
 * Perahu sampan yang dapat dinaiki dan dikendalikan di dunia luar.
 * Koordinat worldX/worldY adalah titik tengah lambung; sprite default menghadap ke selatan.
 *
 * Kemudi: WASD menentukan ARAH TUJUAN, bukan arah hadap. Lambung berputar bertahap menuju arah itu
 * (punya momentum sudut), dorongan mesin selalu searah lambung, dan gerak menyamping diredam air
 * (lunas) sehingga perahu "menyapu" lebar saat menikung, tidak langsung menghadap / berbelok.
 */
public final class Rowboat {

    private static final int SPRITE_WIDTH = 112;
    private static final int SPRITE_HEIGHT = 246;

    // ===== FISIKA GERAK (game jalan 40 FPS, semua nilai per tick) =====
    private static final float MAX_SPEED = 4.2f;
    /** Hambatan air searah lambung saat didayung. Makin kecil = makin lama akselerasi (lebih berat). */
    private static final float FORWARD_DRAG = 0.028f;
    /** Hambatan saat tidak ada input: perahu meluncur ~2,5 tile lalu berhenti. */
    private static final float COAST_DRAG = 0.036f;
    /** Dorongan penuh yang menghasilkan kecepatan akhir MAX_SPEED. */
    private static final float THRUST = MAX_SPEED * FORWARD_DRAG / (1f - FORWARD_DRAG);
    /** Sisa kecepatan menyamping per tick (lunas menahan air). Makin besar = makin licin / drifting. */
    private static final float LATERAL_GRIP = 0.86f;
    private static final float REVERSE_THROTTLE = -0.45f;

    // ===== KEMUDI =====
    /** Laju putar maksimum (rad/tick) pada kecepatan penuh, sekitar 80 derajat per detik. */
    private static final float MAX_TURN_RATE = 0.036f;
    /** Perahu pelan tetap bisa memutar sedikit (pakai dayung), tapi lebih lambat dari saat laju tinggi. */
    private static final float MIN_TURN_AUTHORITY = 0.55f;
    /**
     * Perlambatan putaran (rad/tick^2) saat mendekati arah tujuan. Makin kecil = makin awal dan landai
     * lambung mengerem putarannya (lebih berat). Profil sqrt(2*a*sudut) membuat putaran berhenti tepat
     * di arah tujuan, tidak merayap pelan-pelan dan tidak overshoot.
     */
    private static final float TURN_BRAKE = 0.0004f;
    /** Seberapa jauh kelambanan sudut (momentum) diperhitungkan saat mengerem. */
    private static final float TURN_LEAD = 0.8f;
    /** Kelambanan laju putar mengikuti keinginan (momentum sudut). Makin kecil = makin berat. */
    private static final float TURN_RESPONSE = 0.055f;
    private static final float TURN_RELEASE = 0.08f;
    /** Saat arah tujuan jauh dari arah hadap, dorongan turun ke angka ini sehingga perahu menyapu lebar. */
    private static final float TURN_THROTTLE_FLOOR = 0.40f;
    /** Kalau lambung tidak bisa berputar karena pantai selama ini, perahu otomatis mundur (3-point turn). */
    private static final int REVERSE_AFTER_BLOCKED_TICKS = 12;

    // ===== TITIK CEK AIR (lambung tidak boleh masuk daratan) =====
    private static final float WATER_SIDE_CHECK = 34f;
    private static final float WATER_FRONT_CHECK = 78f;
    private static final float BOW_QUARTER_FORWARD = 52f;
    private static final float BOW_QUARTER_SIDE = 26f;

    // ===== WAKE (jejak gelombang) =====
    private static final int WAKE_LIFE = 90;
    private static final float STERN_OFFSET = SPRITE_HEIGHT * 0.42f;
    /** tan(sudut V). Gelombang Kelvin asli ~19,5 derajat; dibuat sedikit lebih rapat biar enak dilihat. */
    private static final float VEE_SPREAD = 0.30f;
    private static final float VEE_START_HALF_WIDTH = 30f;
    private static final int MAX_FOAM_BITS = 140;

    // ===== EFEK DARI SILUET SPRITE =====
    private static final int FX_PAD = 10;
    private static final Color FOAM = new Color(236, 248, 253);

    private final GamePanel gp;
    private final List<WakeSample> wake = new ArrayList<>();
    private final List<FoamBit> foam = new ArrayList<>();
    private final Random rng = new Random(7);
    private final Path2D.Float wakePath = new Path2D.Float();
    private final Line2D.Float wakeLine = new Line2D.Float();

    private BufferedImage sprite;
    private BufferedImage shadowImg;   // siluet lambung, gelap + blur
    private BufferedImage foamBandImg; // pita busa tebal di sekeliling lambung
    private BufferedImage ringImg;     // pita tipis, dipakai riak yang melebar saat diam

    private float worldX;
    private float worldY;
    private float velocityX;
    private float velocityY;
    /** Sudut hadap lambung: 0 = menghadap bawah/selatan, PI/2 = kanan/timur. */
    private float heading;
    private float angularVelocity;
    private int turnBlockedTicks;
    private float bobPhase;
    private float bobOffset;
    private float rockAmplitude = 0.018f;
    private int tick;
    private boolean mounted;
    private boolean moving;

    private float wakeDistance;
    private int wakeId;
    /** 0..1: seberapa kuat busa / riak di lambung digambar. Turun ke 0 kalau lambung menyentuh darat. */
    private float hullFoamStrength;

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
            buildHullEffects();
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

    // =====================================================================================
    //  UPDATE
    // =====================================================================================

    /** Dipanggil dari GamePanel.update(), sebelum player.update(). */
    public void update() {
        tick++;
        bobPhase += 0.075f;
        bobOffset = (float) Math.sin(bobPhase) * 2.3f;

        ageWake();
        updateGameplay();
        refreshWakeGeometry();

        float rockTarget = moving ? 0.008f : 0.018f;
        rockAmplitude += (rockTarget - rockAmplitude) * 0.06f;
        float foamTarget = (sprite != null && hullClearOfLand()) ? 1f : 0f;
        hullFoamStrength += (foamTarget - hullFoamStrength) * 0.12f;
    }

    private void updateGameplay() {
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
                angularVelocity = 0f;
                turnBlockedTicks = 0;
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
            angularVelocity = 0f;
            return;
        }

        sail();
    }

    /** Satu tick fisika perahu: kemudi bertahap, dorongan searah lambung, lunas, tabrakan pantai. */
    private void sail() {
        float inputX = (gp.KeyH.rightPressed ? 1f : 0f) - (gp.KeyH.leftPressed ? 1f : 0f);
        float inputY = (gp.KeyH.downPressed ? 1f : 0f) - (gp.KeyH.upPressed ? 1f : 0f);
        boolean hasInput = inputX != 0f || inputY != 0f;

        float fx = (float) Math.sin(heading);
        float fy = (float) Math.cos(heading);
        float speedFraction = Math.min(1f, Math.abs(velocityX * fx + velocityY * fy) / MAX_SPEED);

        // ---- 1. KEMUDI: WASD = arah tujuan; lambung berputar pelan menujunya ----
        float diff = 0f;
        float desiredTurn = 0f;
        if (hasInput) {
            float desiredHeading = (float) Math.atan2(inputX, inputY);
            diff = wrapAngle(desiredHeading - heading);
            // Putar balik tepat 180 derajat: tetap lanjut ke arah yang sudah berjalan, jangan bolak-balik.
            if (Math.abs(diff) > 3.0f && Math.abs(angularVelocity) > 0.002f) {
                diff = Math.copySign(Math.abs(diff), angularVelocity);
            }
            // Kemudi makin efektif saat ada aliran air (laju tinggi).
            float authority = MIN_TURN_AUTHORITY + (1f - MIN_TURN_AUTHORITY) * speedFraction;
            float maxRate = MAX_TURN_RATE * authority;
            // Sisa sudut SETELAH momentum sudut habis meluncur; kalau negatif, kemudi melawan (mengerem).
            float remaining = diff - angularVelocity / TURN_RESPONSE * TURN_LEAD;
            if (Math.abs(remaining) > 0.003f) {
                desiredTurn = Math.copySign(
                        Math.min(maxRate, (float) Math.sqrt(2f * TURN_BRAKE * Math.abs(remaining))), remaining);
            }
        }
        angularVelocity += (desiredTurn - angularVelocity) * (hasInput ? TURN_RESPONSE : TURN_RELEASE);
        if (!hasInput && Math.abs(angularVelocity) < 0.0002f) angularVelocity = 0f;

        boolean rotationBlocked = false;
        if (angularVelocity != 0f) {
            float nextHeading = wrapAngle(heading + angularVelocity);
            if (canFloatAt(worldX, worldY, nextHeading)) {
                heading = nextHeading;
            } else {
                // Lambung akan menyapu daratan; putaran terhenti.
                angularVelocity = 0f;
                rotationBlocked = true;
            }
        }
        turnBlockedTicks = rotationBlocked ? turnBlockedTicks + 1 : 0;

        // ---- 2. DORONGAN: selalu searah lambung; makin menyimpang dari tujuan makin lemah ----
        float throttle = 0f;
        if (hasInput) {
            float alignment = Math.max(0f, (float) Math.cos(diff));
            throttle = TURN_THROTTLE_FLOOR + (1f - TURN_THROTTLE_FLOOR) * alignment;
            if (turnBlockedTicks > REVERSE_AFTER_BLOCKED_TICKS && Math.abs(diff) > 0.5f) {
                throttle = REVERSE_THROTTLE; // terjepit pantai: mundur dulu supaya bisa memutar
            }
        }

        fx = (float) Math.sin(heading);
        fy = (float) Math.cos(heading);
        float sx = -fy;
        float sy = fx;
        velocityX += fx * THRUST * throttle;
        velocityY += fy * THRUST * throttle;

        // ---- 3. AIR: hambatan searah lambung kecil, menyamping besar ----
        float forwardSpeed = velocityX * fx + velocityY * fy;
        float lateralSpeed = velocityX * sx + velocityY * sy;
        forwardSpeed *= 1f - (hasInput ? FORWARD_DRAG : COAST_DRAG);
        lateralSpeed *= LATERAL_GRIP;
        velocityX = fx * forwardSpeed + sx * lateralSpeed;
        velocityY = fy * forwardSpeed + sy * lateralSpeed;

        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > MAX_SPEED) {
            velocityX *= MAX_SPEED / speed;
            velocityY *= MAX_SPEED / speed;
            speed = MAX_SPEED;
        }

        // ---- 4. GERAK + TABRAKAN PANTAI (menyusuri pantai, bukan berhenti mendadak) ----
        if (speed > 0.02f) {
            float nextX = worldX + velocityX;
            float nextY = worldY + velocityY;
            if (canFloatAt(nextX, nextY, heading)) {
                worldX = nextX;
                worldY = nextY;
            } else if (canFloatAt(nextX, worldY, heading)) {
                worldX = nextX;
                velocityY *= 0.35f;
            } else if (canFloatAt(worldX, nextY, heading)) {
                worldY = nextY;
                velocityX *= 0.35f;
            } else {
                velocityX *= 0.2f;
                velocityY *= 0.2f;
            }
        }

        speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        moving = speed > 0.32f;
        syncPlayerToSeat();

        if (speed > 0.35f) emitWake(speed);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float wrapAngle(float a) {
        final float twoPi = (float) (Math.PI * 2.0);
        while (a > Math.PI) a -= twoPi;
        while (a <= -Math.PI) a += twoPi;
        return a;
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

    // =====================================================================================
    //  PENUMPANG
    // =====================================================================================

    private int cameraX() {
        int worldWidth = gp.maxWorldCol * gp.tileSize;
        int camX = gp.player.x;
        if (camX < gp.player.screenX) camX = gp.player.screenX;
        if (camX > worldWidth - gp.player.screenX) camX = worldWidth - gp.player.screenX;
        return camX;
    }

    private int cameraY() {
        int worldHeight = gp.maxWorldRow * gp.tileSize;
        int camY = gp.player.y;
        if (camY < gp.player.screenY) camY = gp.player.screenY;
        if (camY > worldHeight - gp.player.screenY) camY = worldHeight - gp.player.screenY;
        return camY;
    }

    /**
     * Apply the boat's floating motion to the passenger's complete render layer.
     * The passenger remains at a stable world/collision position; only the visual
     * pose follows the hull's vertical bob and subtle rocking around the boat centre.
     * Call this on a child Graphics2D before drawing the player or overhead text.
     */
    public void applyPassengerMotion(Graphics2D g2) {
        if (!mounted || gp.player == null) return;

        float pivotX = worldX - cameraX() + gp.player.screenX;
        float pivotY = worldY - cameraY() + gp.player.screenY;

        AffineTransform passengerPose = new AffineTransform();
        passengerPose.translate(pivotX, pivotY + bobOffset);
        passengerPose.rotate(getRockingAngle());
        passengerPose.translate(-pivotX, -pivotY);
        g2.transform(passengerPose);
    }

    private float getRockingAngle() {
        return (float) Math.sin(bobPhase * 0.62f) * rockAmplitude;
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
                angularVelocity = 0f;
                return true;
            }
        }
        return false;
    }

    // =====================================================================================
    //  AIR / DARAT
    // =====================================================================================

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
        float bowX = x + forwardX * BOW_QUARTER_FORWARD;
        float bowY = y + forwardY * BOW_QUARTER_FORWARD;
        return isWaterPoint(x, y)
                && isWaterPoint(x + sideX * WATER_SIDE_CHECK, y + sideY * WATER_SIDE_CHECK)
                && isWaterPoint(x - sideX * WATER_SIDE_CHECK, y - sideY * WATER_SIDE_CHECK)
                && isWaterPoint(x + forwardX * WATER_FRONT_CHECK, y + forwardY * WATER_FRONT_CHECK)
                && isWaterPoint(bowX + sideX * BOW_QUARTER_SIDE, bowY + sideY * BOW_QUARTER_SIDE)
                && isWaterPoint(bowX - sideX * BOW_QUARTER_SIDE, bowY - sideY * BOW_QUARTER_SIDE);
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

    /** true kalau seluruh badan lambung (plus sedikit margin) benar-benar di atas air. */
    private boolean hullClearOfLand() {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        float sideX = -forwardY;
        float sideY = forwardX;
        float half = SPRITE_WIDTH / 2f + 8f;
        float length = SPRITE_HEIGHT * 0.45f;
        return isWaterPoint(worldX + sideX * half, worldY + sideY * half)
                && isWaterPoint(worldX - sideX * half, worldY - sideY * half)
                && isWaterPoint(worldX + forwardX * length, worldY + forwardY * length)
                && isWaterPoint(worldX - forwardX * length, worldY - forwardY * length);
    }

    // =====================================================================================
    //  WAKE: jejak gelombang di belakang perahu
    //  Tiap tick buritan merekam satu titik. Dari rekaman itu digambar (1) pita busa turbulen di
    //  tengah yang melebar & memudar, (2) dua lengan gelombang berbentuk V (sudut tetap, seperti
    //  gelombang Kelvin) dengan satu lengan sekunder di luarnya, (3) butiran busa. Karena jejak
    //  mengikuti rekaman posisi, saat perahu menikung jejaknya ikut melengkung.
    // =====================================================================================

    private void emitWake(float speed) {
        float forwardX = (float) Math.sin(heading);
        float forwardY = (float) Math.cos(heading);
        float sideX = -forwardY;
        float sideY = forwardX;
        float power = clamp(speed / MAX_SPEED, 0f, 1f);

        float sternX = worldX - forwardX * STERN_OFFSET;
        float sternY = worldY - forwardY * STERN_OFFSET;
        wakeDistance += speed;
        wake.add(new WakeSample(sternX, sternY, sideX, sideY, wakeDistance, power, wakeId++));

        // Butiran busa di buritan (pusaran dayung).
        int bits = rng.nextFloat() < power ? 2 : (rng.nextFloat() < 0.4f ? 1 : 0);
        for (int i = 0; i < bits; i++) {
            float lateral = (rng.nextFloat() * 2f - 1f) * (8f + 16f * power);
            float bx = sternX + sideX * lateral;
            float by = sternY + sideY * lateral;
            if (!isWaterPoint(bx, by)) continue;
            float drift = (rng.nextFloat() * 2f - 1f) * 0.28f;
            addFoam(bx, by, sideX * drift - forwardX * 0.12f, sideY * drift - forwardY * 0.12f,
                    34 + rng.nextInt(30), 2 + rng.nextInt(3));
        }

        // Percikan di haluan, hanya saat laju tinggi.
        if (power > 0.5f && rng.nextFloat() < power * 0.55f) {
            float dir = rng.nextBoolean() ? 1f : -1f;
            float bx = worldX + forwardX * SPRITE_HEIGHT * 0.30f + sideX * dir * 30f;
            float by = worldY + forwardY * SPRITE_HEIGHT * 0.30f + sideY * dir * 30f;
            if (isWaterPoint(bx, by)) {
                float out = 0.35f + rng.nextFloat() * 0.45f;
                addFoam(bx, by, sideX * dir * out + forwardX * 0.1f, sideY * dir * out + forwardY * 0.1f,
                        22 + rng.nextInt(18), 2 + rng.nextInt(2));
            }
        }
    }

    private void addFoam(float x, float y, float vx, float vy, int life, int size) {
        if (foam.size() >= MAX_FOAM_BITS) foam.remove(0);
        foam.add(new FoamBit(x, y, vx, vy, life, size));
    }

    private void ageWake() {
        Iterator<WakeSample> it = wake.iterator();
        while (it.hasNext()) {
            WakeSample s = it.next();
            if (++s.age >= WAKE_LIFE) it.remove();
        }
        Iterator<FoamBit> fi = foam.iterator();
        while (fi.hasNext()) {
            FoamBit b = fi.next();
            b.x += b.vx;
            b.y += b.vy;
            b.vx *= 0.95f;
            b.vy *= 0.95f;
            if (--b.life <= 0) fi.remove();
        }
    }

    /** Lebar pita / posisi lengan tiap sampel melebar seiring jarak dan waktu; cek juga mana yang masih di air. */
    private void refreshWakeGeometry() {
        for (WakeSample s : wake) {
            float travelled = wakeDistance - s.dist;
            s.fade = 1f - (float) s.age / WAKE_LIFE;
            s.band = 9f + 0.09f * travelled + 0.05f * s.age;
            s.arm = VEE_START_HALF_WIDTH + VEE_SPREAD * travelled + 0.12f * s.age;
            s.arm2 = s.arm * 1.45f + 8f;

            s.okBandL = isWaterPoint(s.x - s.sideX * s.band, s.y - s.sideY * s.band);
            s.okBandR = isWaterPoint(s.x + s.sideX * s.band, s.y + s.sideY * s.band);
            s.okArmL = isWaterPoint(s.x - s.sideX * s.arm, s.y - s.sideY * s.arm);
            s.okArmR = isWaterPoint(s.x + s.sideX * s.arm, s.y + s.sideY * s.arm);
            s.okArm2L = isWaterPoint(s.x - s.sideX * s.arm2, s.y - s.sideY * s.arm2);
            s.okArm2R = isWaterPoint(s.x + s.sideX * s.arm2, s.y + s.sideY * s.arm2);
        }
    }

    // =====================================================================================
    //  EFEK DARI SILUET SPRITE (bayangan + busa lambung ikut berputar persis sama dengan lambung)
    // =====================================================================================

    private void buildHullEffects() {
        int w = sprite.getWidth() + FX_PAD * 2;
        int h = sprite.getHeight() + FX_PAD * 2;
        boolean[][] solid = new boolean[w][h];
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                if ((sprite.getRGB(x, y) >>> 24) > 40) solid[x + FX_PAD][y + FX_PAD] = true;
            }
        }

        shadowImg = blur(maskImage(solid, null, 0x061428), 3);
        foamBandImg = blur(maskImage(dilate(solid, 5), solid, 0xECF8FD), 1);
        ringImg = blur(maskImage(dilate(solid, 2), solid, 0xECF8FD), 1);
    }

    private static boolean[][] dilate(boolean[][] src, int radius) {
        int w = src.length;
        int h = src[0].length;
        boolean[][] out = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!src[x][y]) continue;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (dx * dx + dy * dy > radius * radius) continue;
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < w && ny < h) out[nx][ny] = true;
                    }
                }
            }
        }
        return out;
    }

    /** Gambar satu warna dengan alpha penuh di piksel yang `on` dan bukan `exclude`; lainnya transparan. */
    private static BufferedImage maskImage(boolean[][] on, boolean[][] exclude, int rgb) {
        int w = on.length;
        int h = on[0].length;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean visible = on[x][y] && (exclude == null || !exclude[x][y]);
                img.setRGB(x, y, visible ? (0xFF000000 | rgb) : rgb);
            }
        }
        return img;
    }

    private static BufferedImage blur(BufferedImage src, int radius) {
        int size = radius * 2 + 1;
        float[] data = new float[size * size];
        java.util.Arrays.fill(data, 1f / (size * size));
        ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    // =====================================================================================
    //  DRAW
    // =====================================================================================

    /** Digambar setelah ground/air, sebelum player dan objek yang berdiri. */
    public void draw(Graphics2D parent) {
        if (sprite == null) return;

        int camX = cameraX();
        int camY = cameraY();
        float screenX = worldX - camX + gp.player.screenX;
        float waterY = worldY - camY + gp.player.screenY;   // permukaan air (tidak ikut naik-turun)
        float screenY = waterY + bobOffset;                  // lambung ikut naik-turun
        if (screenX + SPRITE_WIDTH < -100 || screenX - SPRITE_WIDTH > gp.screenWidth + 100
                || screenY + SPRITE_HEIGHT < -100 || screenY - SPRITE_HEIGHT > gp.screenHeight + 100) return;

        Graphics2D g2 = (Graphics2D) parent.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int ox = gp.player.screenX - camX;
        int oy = gp.player.screenY - camY;

        drawShadow(g2, screenX, waterY);
        drawWake(g2, ox, oy);
        drawFoamBits(g2, ox, oy);
        drawHullWater(g2, screenX, waterY);

        float sway = getRockingAngle();
        AffineTransform boatTransform = new AffineTransform();
        boatTransform.translate(screenX, screenY);
        boatTransform.rotate(-heading + sway);
        boatTransform.translate(-SPRITE_WIDTH / 2.0, -SPRITE_HEIGHT / 2.0);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(sprite, boatTransform, null);

        drawHint(g2, screenX, screenY);
        g2.dispose();
    }

    /**
     * Bayangan = siluet lambung itu sendiri (ikut berputar), jatuh ke kanan-bawah seperti bayangan objek lain
     * di game ini. Saat lambung terangkat ombak bayangan sedikit menjauh dan memudar, saat turun mendekat.
     */
    private void drawShadow(Graphics2D g2, float screenX, float waterY) {
        if (shadowImg == null) return;
        float lift = -bobOffset;                         // + = lambung terangkat
        float spread = 1f + lift * 0.12f;
        float alpha = clamp(0.50f - lift * 0.03f, 0.3f, 0.6f);

        AffineTransform t = new AffineTransform();
        t.translate(screenX + 9f * spread, waterY + 7f * spread);
        t.rotate(-heading + getRockingAngle() * 0.5f);
        t.translate(-(SPRITE_WIDTH / 2.0 + FX_PAD), -(SPRITE_HEIGHT / 2.0 + FX_PAD));

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(shadowImg, t, null);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    /** Busa yang menempel di sekeliling lambung saat bergerak, dan riak yang melebar saat perahu diam. */
    private void drawHullWater(Graphics2D g2, float screenX, float waterY) {
        if (foamBandImg == null || ringImg == null || hullFoamStrength < 0.02f) return;

        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        float power = clamp(speed / MAX_SPEED, 0f, 1f);
        float idle = clamp(1f - speed / 1.4f, 0f, 1f);
        float rock = getRockingAngle() * 0.5f;
        double ax = SPRITE_WIDTH / 2.0 + FX_PAD;
        double ay = SPRITE_HEIGHT / 2.0 + FX_PAD;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Busa di lambung: makin cepat makin tebal, berdenyut tipis.
        float shimmer = 0.88f + 0.12f * (float) Math.sin(tick * 0.31f);
        float foamAlpha = (0.16f + 0.52f * power) * shimmer * hullFoamStrength;
        AffineTransform t = new AffineTransform();
        t.translate(screenX, waterY);
        t.rotate(-heading + rock);
        t.translate(-ax, -ay);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(foamAlpha, 0f, 1f)));
        g2.drawImage(foamBandImg, t, null);

        // Riak melebar saat perahu hampir diam: dua cincin bergantian.
        if (idle > 0.05f) {
            for (int k = 0; k < 2; k++) {
                float p = ((tick + k * 45) % 90) / 90f;
                float scale = 1f + 0.26f * p;
                float a = 0.95f * (1f - p) * (1f - p) * idle * hullFoamStrength;
                AffineTransform r = new AffineTransform();
                r.translate(screenX, waterY);
                r.rotate(-heading + rock);
                r.scale(scale, scale);
                r.translate(-ax, -ay);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp(a, 0f, 1f)));
                g2.drawImage(ringImg, r, null);
            }
        }

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private void drawWake(Graphics2D g2, int ox, int oy) {
        int n = wake.size();
        if (n < 2) return;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Stroke thick = new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke thin = new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

        for (int i = 0; i < n - 1; i++) {
            WakeSample a = wake.get(i);
            WakeSample b = wake.get(i + 1);
            float strength = 0.35f + 0.65f * a.power;
            float fade = Math.max(0f, a.fade);

            // (1) pita busa turbulen di tengah: lebar & pudar, inti lebih terang
            if (a.okBandL && a.okBandR && b.okBandL && b.okBandR) {
                fillBand(g2, a, b, 1f, ox, oy, 0.36f * strength * (float) Math.pow(fade, 1.3));
                fillBand(g2, a, b, 0.42f, ox, oy, 0.42f * strength * (float) Math.pow(fade, 1.8));
            }

            // (2) lengan V utama (putus-putus ala kilau air di tileset) dan lengan sekunder di luarnya
            float armAlpha = 0.92f * strength * (float) Math.pow(fade, 1.1);
            boolean dash1 = a.id % 10 < 7;
            boolean dash2 = a.id % 12 >= 2 && a.id % 12 < 8;
            g2.setStroke(fade > 0.5f ? thick : thin);
            if (dash1) {
                strokeArm(g2, a, b, -1f, a.arm, b.arm, a.okArmL && b.okArmL, ox, oy, armAlpha);
                strokeArm(g2, a, b, 1f, a.arm, b.arm, a.okArmR && b.okArmR, ox, oy, armAlpha);
            }
            g2.setStroke(thin);
            if (dash2) {
                strokeArm(g2, a, b, -1f, a.arm2, b.arm2, a.okArm2L && b.okArm2L, ox, oy, armAlpha * 0.55f);
                strokeArm(g2, a, b, 1f, a.arm2, b.arm2, a.okArm2R && b.okArm2R, ox, oy, armAlpha * 0.55f);
            }
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private void fillBand(Graphics2D g2, WakeSample a, WakeSample b, float widthScale, int ox, int oy, float alpha) {
        if (alpha <= 0.004f) return;
        float aw = a.band * widthScale;
        float bw = b.band * widthScale;
        wakePath.reset();
        wakePath.moveTo(a.x - a.sideX * aw + ox, a.y - a.sideY * aw + oy);
        wakePath.lineTo(b.x - b.sideX * bw + ox, b.y - b.sideY * bw + oy);
        wakePath.lineTo(b.x + b.sideX * bw + ox, b.y + b.sideY * bw + oy);
        wakePath.lineTo(a.x + a.sideX * aw + ox, a.y + a.sideY * aw + oy);
        wakePath.closePath();
        g2.setColor(withAlpha(FOAM, alpha));
        g2.fill(wakePath);
    }

    private void strokeArm(Graphics2D g2, WakeSample a, WakeSample b, float side, float armA, float armB,
                           boolean ok, int ox, int oy, float alpha) {
        if (!ok || alpha <= 0.01f) return;
        wakeLine.setLine(a.x + a.sideX * armA * side + ox, a.y + a.sideY * armA * side + oy,
                b.x + b.sideX * armB * side + ox, b.y + b.sideY * armB * side + oy);
        g2.setColor(withAlpha(FOAM, alpha));
        g2.draw(wakeLine);
    }

    private static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(clamp(alpha, 0f, 1f) * 255f));
    }

    private void drawFoamBits(Graphics2D g2, int ox, int oy) {
        for (FoamBit bit : foam) {
            float life = (float) bit.life / bit.maxLife;
            int size = Math.max(1, Math.round(bit.size * (0.5f + 0.5f * life)));
            g2.setColor(withAlpha(FOAM, 0.85f * life));
            g2.fillRect(Math.round(bit.x + ox - size / 2f), Math.round(bit.y + oy - size / 2f), size, size);
        }
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

    // =====================================================================================

    /** Satu rekaman posisi buritan. Geometri turunannya dihitung ulang tiap tick di refreshWakeGeometry(). */
    private static final class WakeSample {
        final float x;
        final float y;
        final float sideX;
        final float sideY;
        final float dist;
        final float power;
        final int id;
        int age;
        float fade = 1f;
        float band;
        float arm;
        float arm2;
        boolean okBandL;
        boolean okBandR;
        boolean okArmL;
        boolean okArmR;
        boolean okArm2L;
        boolean okArm2R;

        WakeSample(float x, float y, float sideX, float sideY, float dist, float power, int id) {
            this.x = x;
            this.y = y;
            this.sideX = sideX;
            this.sideY = sideY;
            this.dist = dist;
            this.power = power;
            this.id = id;
        }
    }

    private static final class FoamBit {
        float x;
        float y;
        float vx;
        float vy;
        int life;
        final int maxLife;
        final int size;

        FoamBit(float x, float y, float vx, float vy, int life, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
        }
    }
}