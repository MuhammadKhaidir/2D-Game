package Entitas;

import Main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Golemite — versi kecil dari Golem.
 * Perilaku sama seperti Golem (IDLE -> WANDER, kena pukul -> CHASE -> ATTACK,
 * mati -> respawn), tapi cuma punya 2 animasi: Walk (6 frame) dan Punch (4 frame).
 * Tidak ada Smash, Spawn anim, Idle anim, maupun Dead anim.
 */
public class Golemite extends Entity {

    // ── Sprite (static: di-load sekali, dipakai semua instance) ──
    // Index arah: 0 = down/front, 1 = up/back, 2 = left, 3 = right
    private static final String[] DIR_NAMES = {"Front", "Back", "Left", "Right"};
    private static final int WALK_FRAMES   = 6;
    private static final int ATTACK_FRAMES = 4;
    private static final BufferedImage[][] WALK  = new BufferedImage[4][WALK_FRAMES];
    private static final BufferedImage[][] PUNCH = new BufferedImage[4][ATTACK_FRAMES];
    private static boolean imagesLoaded = false;

    // ── State machine ──
    enum GolemiteState { IDLE, WANDER, CHASE, ATTACK, DYING, DEAD }
    private GolemiteState state = GolemiteState.IDLE;

    // ── Anim timers ──
    private int walkFrameIndex = 0, walkAnimTimer = 0;
    private int atkFrameIndex  = 0, atkAnimTimer  = 0;
    private static final int WALK_FRAME_DURATION   = 8;
    private static final int ATTACK_FRAME_DURATION = 6;
    private static final int HIT_FRAME             = 2; // frame ke-3 (index 2) = momen pukulan kena

    // ── Mati (nggak ada anim dead, jadi fade out) ──
    private int dyingTimer = 0;
    private static final int DEATH_FADE_FRAMES = 30; // 0.5 detik @60fps

    // ── Wander ──
    public Rectangle wanderArea = null;
    private int wanderTimer     = 0;
    private int wanderIdleTimer = 0;
    private static final Random RNG = new Random();

    // ── Respawn (spawnX / spawnY pakai punya Entity) ──
    private boolean isSpawnSet   = false;
    private boolean active       = true;   // pengganti "alive" (biar gak nabrak field Entity.alive)
    private int respawnCounter   = 0;
    private static final int RESPAWN_DELAY = 20 * 60;

    // ── Combat ──
    private int attackCd                     = 0;
    private static final int ATTACK_COOLDOWN_MAX = 90;
    private static final int ATTACK_DAMAGE       = 80;
    private static final int ATTACK_RANGE        = 55;   // jarak pusat-ke-pusat buat mulai nyerang
    private static final int DETECTION_RANGE     = 300;  // lewat ini, Golemite nyerah ngejar
    private static final int ATTACK_REACH        = 45;   // panjang hitbox pukulan
    private static final int ATTACK_SPREAD       = 24;   // setengah lebar hitbox pukulan
    private boolean damageDealtThisSwing         = false;

    // ── Tampilan (sesuaikan dengan ukuran sprite kau) ──
    private static final int DRAW_SIZE_MULT = 3;   // ukuran gambar = tileSize * ini
    
    private static final double SPRITE_LOWER = 0.11; // fraksi dari drawSize. 0 = posisi lama, makin gede makin turun

    public Golemite(GamePanel gp) {
        super(gp);
        this.maxLife   = 1500;
        this.life      = maxLife;
        this.speed     = 1.0;
        this.direction = "down";
        this.solidArea = new Rectangle(14, 28, 20, 20);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 400;
        loadImages();
    }

    public boolean isAlive() {
        return active;
    }

    // ────────────────────────────────────────────────────────────
    //  LOAD SPRITE
    // ────────────────────────────────────────────────────────────
    private static synchronized void loadImages() {
        if (imagesLoaded) return;

        for (int d = 0; d < 4; d++) {
            for (int i = 0; i < WALK_FRAMES; i++) {
                WALK[d][i] = loadImage("golemite" + DIR_NAMES[d] + "Walk (" + (i + 1) + ").png");
            }
            for (int i = 0; i < ATTACK_FRAMES; i++) {
                PUNCH[d][i] = loadImage("golemite" + DIR_NAMES[d] + "Punch (" + (i + 1) + ").png");
            }
        }

        imagesLoaded = true;
        System.out.println("[Golemite] Selesai load sprite.");
    }

    private static BufferedImage loadImage(String fileName) {
        try (InputStream is = Golemite.class.getResourceAsStream("/Aset/" + fileName)) {
            if (is == null) {
                System.out.println("[Golemite] File tidak ketemu: /Aset/" + fileName);
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            System.out.println("[Golemite] Gagal load " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private int dirIndex() {
        switch (direction) {
            case "up":    return 1;
            case "left":  return 2;
            case "right": return 3;
            default:      return 0; // "down"
        }
    }

    // ────────────────────────────────────────────────────────────
    //  TARGET
    // ────────────────────────────────────────────────────────────
    private Entity findNearestThreat() {
        Entity nearest = gp.player;
        double minDist = distCenterToCenter(this, gp.player);

        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) continue;
                GolemMinion gm = (GolemMinion) m;
                if (!gm.isAlive()) continue;
                double d = distCenterToCenter(this, gm);
                if (d < minDist) { minDist = d; nearest = gm; }
            }
        }

        return nearest;
    }

    private double distCenterToCenter(Entity a, Entity b) {
        int acx = a.x + a.solidArea.x + a.solidArea.width  / 2;
        int acy = a.y + a.solidArea.y + a.solidArea.height / 2;
        int bcx = b.x + b.solidArea.x + b.solidArea.width  / 2;
        int bcy = b.y + b.solidArea.y + b.solidArea.height / 2;
        int dx = acx - bcx, dy = acy - bcy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────
    @Override
    public void update() {
        if (!isSpawnSet) { spawnX = x; spawnY = y; isSpawnSet = true; }

        // ── 1. Mati & respawn ──
        if (!active) {
            respawnCounter++;
            if (respawnCounter >= RESPAWN_DELAY) respawnGolemite();
            return;
        }

        if (life <= 0 && state != GolemiteState.DYING) {
            state       = GolemiteState.DYING;
            dyingTimer  = 0;
            solidArea.x = -9999;
            solidArea.y = -9999;
            return;
        }

        if (state == GolemiteState.DYING) {
            updateDying();
            return;
        }

        if (attackCd > 0) attackCd--;

        // ── 2. Hitung jarak ke target ──
        Entity target = findNearestThreat();

        int gCX   = x + solidArea.x + solidArea.width  / 2;
        int gCY   = y + solidArea.y + solidArea.height / 2;
        int tCX   = target.x + target.solidArea.x + target.solidArea.width  / 2;
        int tCY   = target.y + target.solidArea.y + target.solidArea.height / 2;
        int distX = tCX - gCX;
        int distY = tCY - gCY;
        double dist = Math.sqrt(distX * distX + distY * distY);

        if (state == GolemiteState.CHASE || state == GolemiteState.ATTACK) {
            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";
        }

        int dx = 0, dy = 0;
        boolean wantsToMove = false;

        // ── 3. State machine ──
        switch (state) {

            case IDLE:
                walkFrameIndex = 0;
                walkAnimTimer  = 0;
                wanderIdleTimer++;
                if (wanderIdleTimer >= 120) {
                    wanderIdleTimer = 0;
                    pickNewWanderDirection();
                    state = GolemiteState.WANDER;
                }
                break;

            case WANDER:
                updateWalkAnim();
                wantsToMove = true;
                wanderTimer--;
                if (wanderTimer <= 0) {
                    state           = GolemiteState.IDLE;
                    wanderIdleTimer = 0;
                }
                break;

            case CHASE:
                if (dist <= ATTACK_RANGE && attackCd == 0) {
                    state                = GolemiteState.ATTACK;
                    atkFrameIndex        = 0;
                    atkAnimTimer         = 0;
                    damageDealtThisSwing = false;
                } else if (dist > DETECTION_RANGE) {
                    state           = GolemiteState.IDLE;
                    wanderIdleTimer = 0;
                } else {
                    updateWalkAnim();
                    wantsToMove = true;
                }
                break;

            case ATTACK:
                atkAnimTimer++;
                if (atkAnimTimer >= ATTACK_FRAME_DURATION) {
                    atkAnimTimer = 0;
                    atkFrameIndex++;

                    if (atkFrameIndex == HIT_FRAME && !damageDealtThisSwing) {
                        gp.playSE("GolemPunch.wav"); // ganti / hapus kalau mau suara sendiri
                        hitTarget(target, dist);
                        damageDealtThisSwing = true;
                    }
                    if (atkFrameIndex >= ATTACK_FRAMES) {
                        atkFrameIndex = 0;
                        attackCd      = ATTACK_COOLDOWN_MAX;
                        state         = GolemiteState.CHASE;
                    }
                }
                break;

            case DYING:
            case DEAD:
                break;
        }

        // ── 4. Gerak + collision (per sumbu, sama seperti Golem) ──
        if (wantsToMove) {
            if (state == GolemiteState.CHASE) {
                int xStep = 0, yStep = 0;
                if (distX > 0) xStep = 1; else if (distX < 0) xStep = -1;
                if (distY > 0) yStep = 1; else if (distY < 0) yStep = -1;

                double moveSpeed = (xStep != 0 && yStep != 0) ? speed * 0.707 : speed;
                dx = (int) Math.round(xStep * moveSpeed);
                dy = (int) Math.round(yStep * moveSpeed);

            } else if (state == GolemiteState.WANDER) {
                switch (direction) {
                    case "up":    dy = -(int) speed; break;
                    case "down":  dy =  (int) speed; break;
                    case "left":  dx = -(int) speed; break;
                    case "right": dx =  (int) speed; break;
                }
            }

            boolean outOfWanderArea = false;
            if (state == GolemiteState.WANDER && wanderArea != null) {
                int nextCX = x + dx + solidArea.x + solidArea.width  / 2;
                int nextCY = y + dy + solidArea.y + solidArea.height / 2;
                if (!wanderArea.contains(nextCX, nextCY)) outOfWanderArea = true;
            }

            if (outOfWanderArea) {
                state           = GolemiteState.IDLE;
                wanderIdleTimer = 0;
            } else {

                boolean isBlockedX = false;
                boolean isBlockedY = false;

                if (dx != 0) {
                    String tempDir = direction;
                    direction = dx > 0 ? "right" : "left";

                    this.collisionOn = false;
                    gp.colision.checkTile(this);
                    gp.colision.checkObject(this, dx, 0);
                    gp.colision.checkEntity(this, gp.player, dx, 0);
                    checkEntityListCollision(dx, 0);

                    if (!this.collisionOn) x += dx;
                    else isBlockedX = true;

                    direction = tempDir;
                }

                if (dy != 0) {
                    String tempDir = direction;
                    direction = dy > 0 ? "down" : "up";

                    this.collisionOn = false;
                    gp.colision.checkTile(this);
                    gp.colision.checkObject(this, 0, dy);
                    gp.colision.checkEntity(this, gp.player, 0, dy);
                    checkEntityListCollision(0, dy);

                    if (!this.collisionOn) y += dy;
                    else isBlockedY = true;

                    direction = tempDir;
                }

                if ((dx != 0 && isBlockedX) || (dy != 0 && isBlockedY)) {
                    state           = GolemiteState.IDLE;
                    wanderIdleTimer = 0;
                    walkFrameIndex  = 0;
                    walkAnimTimer   = 0;
                }
            }
        }
    }

    private void pickNewWanderDirection() {
        String[] dirs = {"up", "down", "left", "right"};
        direction   = dirs[RNG.nextInt(4)];
        wanderTimer = 40 + RNG.nextInt(60);
    }

    // ────────────────────────────────────────────────────────────
    //  COMBAT
    // ────────────────────────────────────────────────────────────
    private void hitTarget(Entity target, double dist) {
        if (dist > ATTACK_RANGE * 1.5) return;

        Rectangle attackBox = getAttackBox();
        Rectangle targetBox = new Rectangle(
            target.x + target.solidArea.x, target.y + target.solidArea.y,
            target.solidArea.width, target.solidArea.height);

        if (attackBox != null && attackBox.intersects(targetBox)) {
            dealDamage(target, ATTACK_DAMAGE);
            if (target instanceof player) ((player) target).applyKnockback(direction);
        }
    }

    private void dealDamage(Entity target, int dmg) {
        if (target instanceof player) {
            ((player) target).takeDamage(dmg);
        } else if (target instanceof GolemMinion) {
            ((GolemMinion) target).takeDamage(dmg);
        }
    }

    private Rectangle getAttackBox() {
        int cx = x + solidArea.x + solidArea.width  / 2;
        int cy = y + solidArea.y + solidArea.height / 2;

        switch (direction) {
            case "up":    return new Rectangle(cx - ATTACK_SPREAD, cy - solidArea.height / 2 - ATTACK_REACH, ATTACK_SPREAD * 2, ATTACK_REACH);
            case "down":  return new Rectangle(cx - ATTACK_SPREAD, cy + solidArea.height / 2,                ATTACK_SPREAD * 2, ATTACK_REACH);
            case "left":  return new Rectangle(cx - solidArea.width / 2 - ATTACK_REACH, cy - ATTACK_SPREAD, ATTACK_REACH, ATTACK_SPREAD * 2);
            case "right": return new Rectangle(cx + solidArea.width / 2,                cy - ATTACK_SPREAD, ATTACK_REACH, ATTACK_SPREAD * 2);
        }
        return null;
    }

    private void checkEntityListCollision(int dx, int dy) {
        if (this.collisionOn) return;

        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) continue;
                if (!((GolemMinion) m).isAlive()) continue;
                gp.colision.checkEntity(this, m, dx, dy);
                if (this.collisionOn) return;
            }
        }

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m == this || m.isDead) continue;
                if (!(m instanceof monster)) continue;
                gp.colision.checkEntity(this, m, dx, dy);
                if (this.collisionOn) return;
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    //  ANIMASI / DEATH / RESPAWN
    // ────────────────────────────────────────────────────────────
    private void updateWalkAnim() {
        walkAnimTimer++;
        if (walkAnimTimer >= WALK_FRAME_DURATION) {
            walkAnimTimer  = 0;
            walkFrameIndex = (walkFrameIndex + 1) % WALK_FRAMES;
        }
    }

    private void updateDying() {
        dyingTimer++;
        if (dyingTimer >= DEATH_FADE_FRAMES) {
            finalizeDeath();
        }
    }

    private void finalizeDeath() {
        active         = false;
        state          = GolemiteState.DEAD;
        respawnCounter = 0;
        // isDead sengaja nggak di-set true, sama seperti Golem.
        gp.player.killMonster(this);
    }

    public void respawnGolemite() {
        active               = true;
        isDead               = false;
        life                 = maxLife;
        x                    = spawnX;
        y                    = spawnY;
        state                = GolemiteState.IDLE;
        direction            = "down";
        walkFrameIndex       = 0;
        walkAnimTimer        = 0;
        atkFrameIndex        = 0;
        atkAnimTimer         = 0;
        attackCd             = 0;
        respawnCounter       = 0;
        dyingTimer           = 0;
        wanderTimer          = 0;
        wanderIdleTimer      = 0;
        damageDealtThisSwing = false;
        solidArea.x          = solidAreaDefaultX;
        solidArea.y          = solidAreaDefaultY;
    }

    // ────────────────────────────────────────────────────────────
    //  DAMAGE
    // ────────────────────────────────────────────────────────────
    @Override
    public void takeDamage(int dmg) {
        if (!active || state == GolemiteState.DYING || state == GolemiteState.DEAD) return;
        life -= dmg;
        // Kalau lagi mukul, jangan diinterupsi — biarin animasinya selesai.
        if (state != GolemiteState.ATTACK) {
            state = GolemiteState.CHASE;
        }
    }

    // Jaga-jaga kalau player manggil versi 2 parameter (punya Entity)
    @Override
    public void takeDamage(int dmg, player attacker) {
        takeDamage(dmg);
    }

    // ────────────────────────────────────────────────────────────
    //  DRAW
    // ────────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics2D g2) {
        if (!active || state == GolemiteState.DEAD) return;

        int drawSize = gp.tileSize * DRAW_SIZE_MULT;
        int sx = x - gp.player.x + gp.player.screenX;
        int sy = y - gp.player.y + gp.player.screenY;

        if (sx + drawSize < -drawSize || sx > gp.screenWidth  + drawSize ||
            sy + drawSize < -drawSize || sy > gp.screenHeight + drawSize) return;

        // gambar dipusatkan di atas tile Golemite
        int offset = (drawSize - gp.tileSize) / 2;
        int drawX  = sx - offset;
        int drawY  = sy - offset;                                // patokan bayangan (posisi "tanah")
        int spriteY = drawY + (int) (drawSize * SPRITE_LOWER);   // sprite diturunin dari sini
        
        

        float fade = 1f;
        if (state == GolemiteState.DYING) {
            fade = Math.max(0f, 1f - (float) dyingTimer / DEATH_FADE_FRAMES);
        }

        Composite oldComp = g2.getComposite();

        // ── Bayangan ──
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f * fade));
        g2.setColor(Color.BLACK);
        int shadowW = (int) (drawSize * 0.55);
        int shadowH = shadowW / 4;
        g2.fillOval(drawX + drawSize / 2 - shadowW / 2,
                    drawY + drawSize - shadowH - drawSize / 7,
                    shadowW, shadowH);

        // ── Sprite ──
        int d = dirIndex();
        BufferedImage img;
        if (state == GolemiteState.ATTACK) {
            img = PUNCH[d][Math.min(atkFrameIndex, ATTACK_FRAMES - 1)];
        } else {
            // IDLE = diam di walk frame pertama
            int f = (state == GolemiteState.CHASE || state == GolemiteState.WANDER) ? walkFrameIndex : 0;
            img = WALK[d][f];
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        if (img != null) {
            g2.drawImage(img, drawX, spriteY, drawSize, drawSize, null);   // <- spriteY
        } else {
            g2.setColor(new Color(100, 100, 100, 200));
            g2.fillRect(drawX, spriteY, drawSize, drawSize);               // <- spriteY
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.drawString("GOLEMITE", drawX + 4, spriteY + drawSize / 2);  // <- spriteY
        }
        g2.setComposite(oldComp);

        // ── HP bar ──
        if (state != GolemiteState.DYING && life < maxLife) {
            int barW   = 40, barH = 5;
            int hpFill = (int) ((double) life / maxLife * barW);
            int barX   = drawX + drawSize / 2 - barW / 2;
            int barY   = drawY + 12; // naik/turunin sesuai tinggi sprite
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, barY, barW, barH);
            g2.setColor(new Color(200, 50, 50));
            g2.fillRect(barX, barY, hpFill, barH);
        }
    }
}