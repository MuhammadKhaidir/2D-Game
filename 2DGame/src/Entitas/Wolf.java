package Entitas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

import Main.GamePanel;

/**
 * Serigala — monster yang CUMA ADA PAS MALAM (jam game 18:00 - 05:59).
 *
 *  - Pas malam tiba, serigala muncul pelan-pelan (fade in) di sekitar player, di luar layar.
 *  - Jalan santai sambil mengintai (arahnya condong ke player); kalau player dekat dia ngejar dan menggigit.
 *  - Pas pagi tiba dia menghilang (fade out). Malam berikutnya dia muncul lagi dengan HP penuh,
 *    termasuk yang tadi malam dibunuh.
 *  - Kalau player jauh banget (> RELOCATE_DIST) pas malam, dia pindah lagi ke dekat player.
 *
 * Sprite 4 arah (kanan, kiri, atas, bawah) dimuat dari PNG per-frame (lihat WolfSprite;
 * dibuat lewat tools/generate_wolf.py). Arah hadap mengikuti arah jalan / arah ke player.
 *
 * Catatan penting: pas siang serigala berstatus ABSENT dan isDead = true, supaya semua kode lain
 * (serang player, skill, tabrakan, TileM) menganggapnya gak ada. Makanya GamePanel tetap harus
 * manggil update() serigala walau isDead (lihat GamePanel, sama kayak Golem / Golemite).
 */
public class Wolf extends Entity {

    public enum WolfState { ABSENT, IDLE, WANDER, CHASE, ATTACK, DYING, LEAVING }

    private WolfState state = WolfState.ABSENT;

    // ── Jam malam ──
    private static final int NIGHT_START_HOUR = 18;   // mulai 18:00
    private static final int NIGHT_END_HOUR   = 6;    // sampai 05:59

    // ── Stat ──
    private static final int    MAX_LIFE      = 80;
    private static final int    EXP_VALUE     = 35;
    private static final int    COIN_DROP     = 2;
    private static final double WANDER_SPEED  = 1.0;
    private static final double CHASE_SPEED   = 2.8;   // player jalan 2.5, lari 5.0
    private static final int    AGGRO_RADIUS  = 450;   // mulai ngejar
    private static final int    LOSE_RADIUS   = 750;   // nyerah ngejar
    private static final int    ATTACK_RANGE  = 64;    // mulai gigit
    private static final int    ATTACK_DAMAGE = 10;
    private static final int    ATTACK_COOLDOWN = 80;  // 2 detik @40 FPS
    private static final int    PROVOKED_TICKS  = 240; // abis dipukul, tetap ngejar walau agak jauh

    // ── Muncul / hilang ──
    private static final int APPEAR_DELAY_MAX = 800;   // tiap serigala muncul acak 0-20 detik setelah malam tiba
    private static final int SPAWN_MIN_DIST   = 650;   // muncul minimal sejauh ini dari player (di luar layar)
    private static final int SPAWN_MAX_DIST   = 950;
    private static final int RELOCATE_DIST    = 1500;  // kejauhan dari player -> pindah ke dekat player lagi
    private static final int STALK_RADIUS     = 1100;  // di dalam jarak ini dia jalan santai TAPI condong ke arah player (mengintai)
    private static final int FADE_IN_TICKS    = 40;
    private static final int LEAVE_TICKS      = 60;
    private static final int DEAD_LIE_TICKS   = 70;
    private static final int DEAD_FADE_TICKS  = 40;

    // ── Efek kena pukul: badan putih sekejap ──
    private static final int HIT_FLASH_FRAMES = 5;     // 5 frame @40 FPS = 0,125 detik
    private int whiteFlashTimer = 0;

    // ── Animasi ──
    private static final int WALK_FRAME_TICKS   = 7;
    private static final int IDLE_FRAME_TICKS   = 20;
    private static final int ATTACK_FRAME_TICKS = 5;
    private static final int ATTACK_FRAMES      = 5;
    private static final int BITE_FRAME         = 2;   // frame gigitan (mulut paling lebar)

    // ── Ukuran ──
    // 1.0 = ukuran lama (sprite 2x pas tileSize 48 = 124x68 px). Makin kecil angkanya, makin kecil serigalanya.
    private static final double SIZE_FACTOR = 0.75;    // 0.75 -> sprite 1.5x = 93x51 px

    private final WolfSprite sprite;
    private final int drawW, drawH;

    private int dir = WolfSprite.RIGHT;   // arah hadap: WolfSprite.RIGHT / LEFT / UP / DOWN
    private boolean moving = false;
    private int walkFrame = 0, walkTick = 0;
    private int idleFrame = 0, idleTick = 0;
    private int attackFrame = 0, attackTick = 0;
    private boolean biteDone = false;

    // ── Status ──
    private boolean lastNight = false;
    private boolean wantAppear = false;
    private int appearCountdown = 0;
    private int fadeIn = 0;
    private int leaveTimer = 0;
    private int dyingTimer = 0;
    private int attackCooldown = 0;
    private int provokedTimer = 0;

    // ── Gerak ──
    private double accX = 0, accY = 0;      // sisa gerak pecahan (speed bukan bilangan bulat)
    private double wanderDX = 0, wanderDY = 1;
    private int wanderTimer = 0, pauseTimer = 0;

    private final Random rand = new Random();

    public Wolf(GamePanel gp) {
        super(gp);

        name        = "wolf";
        showName    = true;
        maxLife     = MAX_LIFE;
        life        = maxLife;
        getExpValue = EXP_VALUE;
        coinDrop    = COIN_DROP;
        speed       = WANDER_SPEED;
        attackRange = ATTACK_RANGE;
        direction   = "right";

        double scale = Math.max(1, gp.tileSize / 24) * SIZE_FACTOR;   // tileSize 48 -> 2 * 0.75 = sprite 1.5x
        sprite = WolfSprite.get(scale);
        drawW  = sprite.width;
        drawH  = sprite.height;

        // hitbox simetris di tengah badan (kanan/kiri sama aja), setinggi kaki
        solidArea = new Rectangle((int) Math.round(14 * scale), (int) Math.round(20 * scale),
                                  (int) Math.round(34 * scale), Math.max(1, (int) Math.round(10 * scale)));
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        becomeAbsent();   // awalnya belum ada (siang)
    }

    // ───────────────────────── Status publik ─────────────────────────

    /** True kalau serigala lagi ada dan masih bisa dipukul / ditarget skill. */
    public boolean isAlive() {
        return state == WolfState.IDLE || state == WolfState.WANDER
            || state == WolfState.CHASE || state == WolfState.ATTACK;
    }

    private boolean isNight() {
        int h = gp.world.hour;
        return h >= NIGHT_START_HOUR || h < NIGHT_END_HOUR;
    }

    // ───────────────────────── Update utama ─────────────────────────

    @Override
    public void update() {

        // ── Siang <-> malam ──
        boolean night = isNight();
        if (night != lastNight) {
            lastNight = night;
            if (night) {
                wantAppear = true;
                appearCountdown = rand.nextInt(APPEAR_DELAY_MAX);
            } else {
                wantAppear = false;
                startLeaving();
            }
        }

        if (whiteFlashTimer > 0) whiteFlashTimer--;

        switch (state) {
            case ABSENT:
                if (wantAppear) {
                    if (appearCountdown > 0) appearCountdown--;
                    else if (placeNearPlayer()) {
                        wantAppear = false;
                        enterWorld();
                    }
                }
                return;

            case DYING:
                dyingTimer++;
                if (dyingTimer >= DEAD_LIE_TICKS + DEAD_FADE_TICKS) becomeAbsent();
                return;

            case LEAVING:
                leaveTimer++;
                if (leaveTimer >= LEAVE_TICKS) becomeAbsent();
                return;

            default:
                break;
        }

        // ── Hidup ──
        moving = false;
        if (fadeIn < FADE_IN_TICKS) fadeIn++;
        if (attackCooldown > 0) attackCooldown--;
        if (provokedTimer > 0) provokedTimer--;

        boolean hunting = !gp.player.isDead;
        double dist = hunting ? distanceToPlayer() : Double.MAX_VALUE;

        switch (state) {
            case IDLE:
            case WANDER:
                wander();
                if (hunting && dist < AGGRO_RADIUS) state = WolfState.CHASE;
                else if (dist > RELOCATE_DIST && hunting) relocate();
                break;

            case CHASE:
                if (!hunting || (dist > LOSE_RADIUS && provokedTimer == 0)) {
                    state = WolfState.WANDER;
                    break;
                }
                faceTowardPlayer();
                if (dist <= ATTACK_RANGE) {
                    if (attackCooldown == 0) startAttack();
                    // kalau masih cooldown: diam di tempat, nunggu giliran gigit lagi
                } else {
                    double[] v = dirToPlayer();
                    if (step(v[0], v[1], CHASE_SPEED)) moving = true;
                }
                break;

            case ATTACK:
                updateAttack();
                break;

            default:
                break;
        }

        tickAnimation();
    }

    // ───────────────────────── Muncul / hilang ─────────────────────────

    /** Cari tempat muncul di sekitar player (di luar layar, gak nabrak collision). True kalau ketemu. */
    private boolean placeNearPlayer() {
        int pcx = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int pcy = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        int margin = gp.tileSize * 2;

        for (int i = 0; i < 40; i++) {
            double ang  = rand.nextDouble() * Math.PI * 2;
            double dist = SPAWN_MIN_DIST + rand.nextDouble() * (SPAWN_MAX_DIST - SPAWN_MIN_DIST);
            int cx = pcx + (int) (Math.cos(ang) * dist);   // titik tengah hitbox
            int cy = pcy + (int) (Math.sin(ang) * dist);
            int nx = cx - solidAreaDefaultX - solidArea.width  / 2;
            int ny = cy - solidAreaDefaultY - solidArea.height / 2;

            if (cx < margin || cy < margin || cx > gp.worldWidth - margin || cy > gp.worldHeight - margin) continue;
            if (blockedAt(nx, ny, solidAreaDefaultX, solidAreaDefaultY, gp.tileSize / 2)) continue;

            x = nx;
            y = ny;
            spawnX = nx;
            spawnY = ny;
            return true;
        }
        return false;
    }

    /** Dipanggil setelah tempat muncul ketemu: hidupkan serigala dengan HP penuh. */
    private void enterWorld() {
        life = maxLife;
        isDead = false;
        isKnockback = false;
        showHitbox();
        state = WolfState.IDLE;
        fadeIn = 0;
        attackCooldown = 0;
        provokedTimer = 0;
        accX = accY = 0;
        pauseTimer = rand.nextInt(60);
        wanderTimer = 0;
        setDir(rand.nextInt(4));
    }

    /** Kejauhan dari player pas malam: pindah ke dekat player lagi (fade in ulang). */
    private void relocate() {
        if (placeNearPlayer()) {
            fadeIn = 0;
            state = WolfState.IDLE;
            wanderTimer = 0;
            pauseTimer = rand.nextInt(60);
        }
    }

    /** Pagi tiba: kalau lagi ada & hidup, menghilang pelan-pelan. */
    private void startLeaving() {
        if (state == WolfState.ABSENT || state == WolfState.DYING || state == WolfState.LEAVING) return;
        state = WolfState.LEAVING;
        leaveTimer = 0;
        hideHitbox();      // gak bisa dipukul / dihalangi lagi selama menghilang (isDead tetap false biar masih digambar)
    }

    /** Beneran gak ada di dunia: semua kode lain menganggap serigala ini gak ada. */
    private void becomeAbsent() {
        state = WolfState.ABSENT;
        isDead = true;
        hideHitbox();
    }

    private void hideHitbox() { solidArea.x = -9999; solidArea.y = -9999; }
    private void showHitbox() { solidArea.x = solidAreaDefaultX; solidArea.y = solidAreaDefaultY; }

    // ───────────────────────── Gerak ─────────────────────────

    private void wander() {
        if (pauseTimer > 0) {              // lagi diam (IDLE)
            pauseTimer--;
            state = WolfState.IDLE;
            return;
        }
        state = WolfState.WANDER;

        wanderTimer--;
        if (wanderTimer <= 0) {
            wanderTimer = 60 + rand.nextInt(90);
            if (rand.nextInt(3) == 0) {    // kadang berhenti dulu
                pauseTimer = 40 + rand.nextInt(80);
                return;
            }
            pickWanderDirection();
        }

        if (step(wanderDX, wanderDY, WANDER_SPEED)) {
            moving = true;
            faceVector(wanderDX, wanderDY);
        } else {
            wanderTimer = 0;               // mentok: cari arah baru
        }
    }

    private void pickWanderDirection() {
        double a;
        if (!gp.player.isDead && distanceToPlayer() < STALK_RADIUS && rand.nextInt(100) < 65) {
            // mengintai: arahnya ke player, melenceng sedikit biar gak lurus banget
            double[] v = dirToPlayer();
            a = Math.atan2(v[1], v[0]) + (rand.nextDouble() - 0.5) * 1.0;
        } else {
            a = rand.nextDouble() * Math.PI * 2;
        }
        wanderDX = Math.cos(a);
        wanderDY = Math.sin(a);
    }

    /** Jalan satu langkah ke arah (ux,uy) dengan kecepatan spd; geser nyusur tembok kalau mentok. */
    private boolean step(double ux, double uy, double spd) {
        accX += ux * spd;
        accY += uy * spd;
        int dx = (int) accX, dy = (int) accY;   // dibulatkan ke 0, sisanya disimpan buat frame berikutnya
        accX -= dx;
        accY -= dy;
        if (dx == 0 && dy == 0) return true;    // langkah terlalu kecil buat 1 piksel, tapi tetap dianggap jalan

        boolean moved = false;
        if (!blockedAt(x + dx, y + dy, solidArea.x, solidArea.y, 0)) {
            x += dx; y += dy; moved = true;
        } else {
            if (dx != 0 && !blockedAt(x + dx, y, solidArea.x, solidArea.y, 0)) { x += dx; moved = true; }
            if (dy != 0 && !blockedAt(x, y + dy, solidArea.x, solidArea.y, 0)) { y += dy; moved = true; }
        }
        return moved;
    }

    /** True kalau hitbox serigala di posisi (nx,ny) nabrak dunia, player, atau monster lain. */
    private boolean blockedAt(int nx, int ny, int hbX, int hbY, int pad) {
        Rectangle box = new Rectangle(nx + hbX - pad, ny + hbY - pad, solidArea.width + pad * 2, solidArea.height + pad * 2);

        for (Rectangle r : gp.objCol.objects) {
            if (box.intersects(r)) return true;
        }

        if (!gp.player.isDead) {
            Rectangle pb = new Rectangle(gp.player.x + gp.player.solidArea.x, gp.player.y + gp.player.solidArea.y,
                                         gp.player.solidArea.width, gp.player.solidArea.height);
            if (box.intersects(pb)) return true;
        }

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m == this || m.isDead || m.solidArea == null) continue;
                Rectangle mb = new Rectangle(m.x + m.solidArea.x, m.y + m.solidArea.y, m.solidArea.width, m.solidArea.height);
                if (box.intersects(mb)) return true;
            }
        }
        return false;
    }

    // ───────────────────────── Jarak & arah ke player ─────────────────────────

    private int centerX() { return x + solidArea.x + solidArea.width  / 2; }
    private int centerY() { return y + solidArea.y + solidArea.height / 2; }

    private double distanceToPlayer() {
        int px = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int py = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        return Math.hypot(px - centerX(), py - centerY());
    }

    private double[] dirToPlayer() {
        int px = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int py = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        double dx = px - centerX(), dy = py - centerY();
        double len = Math.hypot(dx, dy);
        if (len == 0) return new double[] { 0, 0 };
        return new double[] { dx / len, dy / len };
    }

    private void faceTowardPlayer() {
        int px = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int py = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        int dx = px - centerX(), dy = py - centerY();
        if (Math.abs(dx) + Math.abs(dy) > 8) faceVector(dx, dy);   // zona mati 8px biar gak kedip-kedip
    }

    private void setDir(int d) {
        dir = d;
        direction = WolfSprite.dirName(d);   // Entity.direction ikut disamakan
    }

    /**
     * Pilih arah hadap (kanan/kiri/atas/bawah) dari sebuah vektor (gerak atau arah ke target).
     * Sumbu yang sedang dipakai dipertahankan sampai sumbu lainnya jelas lebih dominan (1,5x),
     * supaya serigala gak kedip-kedip atas<->kanan pas arahnya nyaris diagonal.
     */
    private void faceVector(double dx, double dy) {
        double ax = Math.abs(dx), ay = Math.abs(dy);
        if (ax + ay < 1e-6) return;
        boolean horizontal = (dir == WolfSprite.RIGHT || dir == WolfSprite.LEFT);
        boolean switchAxis = horizontal ? ay > ax * 1.5 : ax > ay * 1.5;
        boolean useHorizontal = switchAxis ? !horizontal : horizontal;
        if (useHorizontal) setDir(dx >= 0 ? WolfSprite.RIGHT : WolfSprite.LEFT);
        else               setDir(dy >= 0 ? WolfSprite.DOWN  : WolfSprite.UP);
    }

    // ───────────────────────── Serang ─────────────────────────

    private void startAttack() {
        state = WolfState.ATTACK;
        attackFrame = 0;
        attackTick = 0;
        biteDone = false;
        faceTowardPlayer();
    }

    private void updateAttack() {
        attackTick++;
        if (attackTick < ATTACK_FRAME_TICKS) return;
        attackTick = 0;
        attackFrame++;

        // frame gigitan: damage masuk SEKALI, cuma kalau player masih dalam jangkauan
        if (attackFrame == BITE_FRAME && !biteDone) {
            biteDone = true;
            if (!gp.player.isDead && distanceToPlayer() <= ATTACK_RANGE + 26) {
                gp.player.takeDamageFrom(ATTACK_DAMAGE, this);
            }
        }

        if (attackFrame >= ATTACK_FRAMES) {
            attackFrame = 0;
            attackCooldown = ATTACK_COOLDOWN;
            state = WolfState.CHASE;
        }
    }

    // ───────────────────────── Kena damage & mati ─────────────────────────

    @Override
    public void takeDamage(int dmg, player attacker) {
        if (!isAlive()) return;     // lagi muncul/hilang/mati: gak bisa dipukul

        life -= dmg;
        whiteFlashTimer = HIT_FLASH_FRAMES;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);

        if (life <= 0) {
            life = 0;
            die();
            return;
        }

        provokedTimer = PROVOKED_TICKS;
        // lagi nggigit: biarin animasinya selesai, jangan diinterupsi
        if (state != WolfState.ATTACK) state = WolfState.CHASE;
    }

    private void die() {
        state = WolfState.DYING;
        dyingTimer = 0;
        hideHitbox();
        gp.player.killMonster(this);   // exp, level up, koin, potion
    }

    // ───────────────────────── Animasi ─────────────────────────

    private void tickAnimation() {
        if (state == WolfState.ATTACK) return;
        if (moving) {
            // ngejar = kaki gerak lebih cepat
            walkTick += (state == WolfState.CHASE) ? 2 : 1;
            if (walkTick >= WALK_FRAME_TICKS) {
                walkTick = 0;
                walkFrame = (walkFrame + 1) % 6;
            }
            idleTick = 0;
        } else {
            idleTick++;
            if (idleTick >= IDLE_FRAME_TICKS) {
                idleTick = 0;
                idleFrame = (idleFrame + 1) % 2;
            }
            walkTick = 0;
        }
    }

    private BufferedImage currentFrame() {
        int d = dir;
        switch (state) {
            case DYING:  return sprite.dead[d];
            case ATTACK: return sprite.attack[d][Math.min(attackFrame, ATTACK_FRAMES - 1)];
            default:     break;
        }
        // (sengaja bukan ternary: compiler Eclipse pernah salah ngetik ternary bertipe referensi jadi Object -> VerifyError)
        if (moving) return sprite.walk[d][walkFrame % WolfSprite.WALK_FRAMES];
        return sprite.idle[d][idleFrame % WolfSprite.IDLE_FRAMES];
    }

    private float alpha() {
        switch (state) {
            case ABSENT:  return 0f;
            case LEAVING: return 1f - leaveTimer / (float) LEAVE_TICKS;
            case DYING:   return dyingTimer <= DEAD_LIE_TICKS ? 1f
                                : 1f - (dyingTimer - DEAD_LIE_TICKS) / (float) DEAD_FADE_TICKS;
            default:      return Math.min(1f, fadeIn / (float) FADE_IN_TICKS);
        }
    }

    // ───────────────────────── Gambar ─────────────────────────

    @Override
    public void draw(Graphics2D g2) {
        if (state == WolfState.ABSENT) return;

        float a = Math.max(0f, Math.min(1f, alpha()));
        if (a <= 0f) return;

        int sx = x - gp.player.x + gp.player.screenX;
        int sy = y - gp.player.y + gp.player.screenY;

        Composite old = g2.getComposite();

        // bayangan di bawah kaki
        if (state != WolfState.DYING) {
            int sw = drawW * 11 / 20, sh = (int) Math.round(sprite.scale * 8);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f * a));
            g2.setColor(Color.BLACK);
            g2.fillOval(sx + (drawW - sw) / 2, sy + (int) Math.round(30 * sprite.scale) - sh / 2, sw, sh);
        }

        BufferedImage img = currentFrame();
        if (whiteFlashTimer > 0) img = HitFlash.whiteVersion(img);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g2.drawImage(img, sx, sy, null);
        g2.setComposite(old);

        // HP bar + nama (cuma pas hidup & sudah muncul penuh)
        if (isAlive() && fadeIn >= FADE_IN_TICKS) {
            int bw = 48, bh = 5;
            int bx = sx + (drawW - bw) / 2;
            int by = sy - 2;
            if (life < maxLife) {
                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(bx, by, bw, bh);
                g2.setColor(Color.RED);
                g2.fillRect(bx, by, (int) ((double) life / maxLife * bw), bh);
            }
            if (showName) {
                g2.setFont(new Font("Dialog", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(name, bx + 4, by - 3);
            }
        }
    }
}
