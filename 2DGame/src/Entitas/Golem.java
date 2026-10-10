package Entitas;

import Main.GamePanel;
import Tamed.GolemMinion;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;

public class Golem extends Entity {

    public static BufferedImage[] S_dead = new BufferedImage[7];
    
    public static BufferedImage[] S_walkFront   = new BufferedImage[6];
    public static BufferedImage[] S_walkBack    = new BufferedImage[6];
    public static BufferedImage[] S_walkLeft    = new BufferedImage[6];
    public static BufferedImage[] S_walkRight   = new BufferedImage[6];

    public static BufferedImage[] S_idleFront   = new BufferedImage[2];
    public static BufferedImage[] S_idleBack    = new BufferedImage[2];
    public static BufferedImage[] S_idleLeft    = new BufferedImage[2];
    public static BufferedImage[] S_idleRight   = new BufferedImage[2];

    public static BufferedImage[] S_attackFront = new BufferedImage[5];
    public static BufferedImage[] S_attackBack  = new BufferedImage[6];
    public static BufferedImage[] S_attackLeft  = new BufferedImage[6];
    public static BufferedImage[] S_attackRight = new BufferedImage[6];

    // ── Smash (skill pasif, serangan pembuka, AOE) ──
    public static BufferedImage[] S_smashFront  = new BufferedImage[5];
    public static BufferedImage[] S_smashBack   = new BufferedImage[5];
    public static BufferedImage[] S_smashLeft   = new BufferedImage[5];
    public static BufferedImage[] S_smashRight  = new BufferedImage[5];

    // ── Spawn (animasi bangkit) ──
    public static BufferedImage[] S_spawn       = new BufferedImage[5];

    private static boolean imagesLoaded = false;
    
    private int deadFrameIndex  = 0, deadAnimTimer  = 0;
    private static final double DEAD_DRAW_SCALE = 1.0; // sprite mati digedein 1.4x dari ukuran normal
    private static final int    DEAD_OFFSET_X   = -3;  // geser ke kiri (px)
    private static final int    DEAD_OFFSET_Y   = 3;   // geser ke bawah (px)
    

    private BufferedImage[] deadFrames = new BufferedImage[7];

    // ── Walk frames (6 per arah) ──
    private BufferedImage[] walkFrontFrames = new BufferedImage[6];
    private BufferedImage[] walkBackFrames  = new BufferedImage[6];
    private BufferedImage[] walkLeftFrames  = new BufferedImage[6];
    private BufferedImage[] walkRightFrames = new BufferedImage[6];

    // ── Idle frames (2 per arah) ──
    private BufferedImage[] idleFrontFrames = new BufferedImage[2];
    private BufferedImage[] idleBackFrames  = new BufferedImage[2];
    private BufferedImage[] idleLeftFrames  = new BufferedImage[2];
    private BufferedImage[] idleRightFrames = new BufferedImage[2];

    // ── Attack frames (Front=5, sisanya=6) ──
    private BufferedImage[] attackFrontFrames = new BufferedImage[5];
    private BufferedImage[] attackBackFrames  = new BufferedImage[6];
    private BufferedImage[] attackLeftFrames  = new BufferedImage[6];
    private BufferedImage[] attackRightFrames = new BufferedImage[6];

    // ── Smash frames (5 per arah, cuma buat visual — damage-nya AOE) ──
    private BufferedImage[] smashFrontFrames = new BufferedImage[5];
    private BufferedImage[] smashBackFrames  = new BufferedImage[5];
    private BufferedImage[] smashLeftFrames  = new BufferedImage[5];
    private BufferedImage[] smashRightFrames = new BufferedImage[5];

    // ── Spawn frames (5, ga tergantung arah) ──
    private BufferedImage[] spawnFrames = new BufferedImage[5];

    // ── State machine ──
    enum GolemState { IDLE, WANDER, CHASE, ATTACK, SMASH, SPAWN, DYING, DEAD }
    GolemState golemState = GolemState.IDLE;

    // ── Anim timers ──
    private int walkFrameIndex   = 0, walkAnimTimer   = 0;
    private int idleFrameIndex   = 0, idleAnimTimer   = 0;
    private int attackFrameIndex = 0, attackAnimTimer = 0;
    private int smashFrameIndex  = 0, smashAnimTimer  = 0;
    private int spawnFrameIndex  = 0, spawnAnimTimer  = 0;
    private static final int DEAD_FRAME_COUNT    = 7;
    private static final int DEAD_FRAME_DURATION = 8;

    // ── Wander ──
    public Rectangle wanderArea = null;
    private int wanderTimer     = 0;
    private int wanderIdleTimer = 0;
    private boolean wanderIdle  = false;
    private static final Random RNG = new Random();

    // ── Provoked ──
    private boolean isProvoked = false;

    // ── Respawn ──
    public int spawnX, spawnY;
    private boolean isSpawnSet    = false;
    private int golemRespawnTimer = 0;
    private final int RESPAWN_DELAY = 20 * 60;
    private boolean alive = true;

    // ── Combat ──
    private int attackCooldown            = 0;
    private final int ATTACK_COOLDOWN_MAX = 120;
    private final int ATTACK_DAMAGE       = 200;
    private final int ATTACK_RANGE        = 100;
    private final int DETECTION_RANGE     = 350;
    private boolean damageDealtThisSwing  = false;

    // ── Smash (skill pasif, cooldown 10 detik, AOE radius) ──
    private int smashCooldown             = 0;
    private final int SMASH_COOLDOWN_MAX  = 600; // 10 detik @60fps
    private final int SMASH_DAMAGE        = 400;
    private final int SMASH_RADIUS        = 90;  // radius area damage dari titik tengah Golem
    

    // ── Ukuran gambar: 3x tileSize ──
    private final int DRAW_SIZE_MULT = 3;

    public Golem(GamePanel gp) {
        super(gp);
        this.maxLife   = 5000;
        this.life      = maxLife;
        this.speed     = 1.0;
        this.direction = "down";
        // Di constructor, UBAH solidArea jadi lebih kecil dan centered:
        this.solidArea = new Rectangle(14, 28, 20, 20);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 1000;
        loadImages();
    }

    @Override
    public int getSortY() {
        return this.y + solidArea.y + solidArea.height;
    }

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
        return Math.sqrt(dx*dx + dy*dy);
    }

    private void loadImages() {
        try {
            for (int i = 0; i < 6; i++) {
                walkFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontWalk (" + (i+1) + ").png"));
                walkBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemBackWalk (" + (i+1) + ").png"));
                walkLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemLeftWalk (" + (i+1) + ").png"));
                walkRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemRightWalk (" + (i+1) + ").png"));
                S_walkFront[i] = walkFrontFrames[i];
                S_walkBack[i]  = walkBackFrames[i];
                S_walkLeft[i]  = walkLeftFrames[i];
                S_walkRight[i] = walkRightFrames[i];
            }
            for (int i = 0; i < 2; i++) {
                idleFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontIdle (" + (i+1) + ").png"));
                idleBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemBackIdle (" + (i+1) + ").png"));
                idleLeftFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemLeftIdle (" + (i+1) + ").png"));
                idleRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemRightIdle (" + (i+1) + ").png"));
                S_idleFront[i] = idleFrontFrames[i];
                S_idleBack[i]  = idleBackFrames[i];
                S_idleLeft[i]  = idleLeftFrames[i];
                S_idleRight[i] = idleRightFrames[i];
            }
            for (int i = 0; i < 5; i++) {
                attackFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontAttack (" + (i+1) + ").png"));
                S_attackFront[i] = attackFrontFrames[i];
            }
            for (int i = 0; i < 6; i++) {
                attackBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemBackAttack (" + (i+1) + ").png"));
                attackLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemLeftAttack (" + (i+1) + ").png"));
                attackRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemRightAttack (" + (i+1) + ").png"));
                S_attackBack[i]  = attackBackFrames[i];
                S_attackLeft[i]  = attackLeftFrames[i];
                S_attackRight[i] = attackRightFrames[i];
            }
            for (int i = 0; i < 5; i++) {
                smashFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontSmash"+ (i+1) + ".png"));
                smashBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemBackSmash" + (i+1) + ".png"));
                smashLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemLeftSmash" + (i+1) + ".png"));
                smashRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemRightSmash" + (i+1) + ".png"));
                S_smashFront[i] = smashFrontFrames[i];
                S_smashBack[i]  = smashBackFrames[i];
                S_smashLeft[i]  = smashLeftFrames[i];
                S_smashRight[i] = smashRightFrames[i];

                spawnFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemSpawn" + (i+1) + ".png"));
                S_spawn[i] = spawnFrames[i];
            }
            
            for (int i = 0; i < 7; i++) {
                deadFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/GolemFrontDead" + (i+1) + ".png"));
                S_dead[i] = deadFrames[i];
            }
            
            imagesLoaded = true;
            System.out.println("[Golem] Semua sprite loaded OK!");
        } catch (Exception e) {
            System.out.println("[Golem] Gagal load: " + e.getMessage());
        }
    }

    @Override
    public void update() {
        if (!isSpawnSet) { spawnX = x; spawnY = y; isSpawnSet = true; }

        // ── 1. Handle Mati & Respawn ──
        if (!alive) {
            golemRespawnTimer++;
            if (golemRespawnTimer >= RESPAWN_DELAY) respawnGolem();
            return;
        }

        // ── 1b. Handle animasi Spawn (rooted, ga bisa diserang/gerak) ──
        if (golemState == GolemState.SPAWN) {
            updateSpawnAnim();
            return;
        }
        
        if (life <= 0 && alive && golemState != GolemState.DYING) {
            golemState     = GolemState.DYING;
            deadFrameIndex = 0;
            deadAnimTimer  = 0;
            solidArea.x    = -9999;
            solidArea.y    = -9999;
            return;
        }

        if (golemState == GolemState.DYING) {
            updateDeadAnim();
            return;
        }
        

        if (attackCooldown > 0) attackCooldown--;
        if (smashCooldown  > 0) smashCooldown--;

        Entity target = findNearestThreat();
        
        int goCX  = x + solidArea.x + solidArea.width  / 2;
        int goCY  = y + solidArea.y + solidArea.height / 2;
        int plCX  = target.x + target.solidArea.x + target.solidArea.width  / 2;
        int plCY  = target.y + target.solidArea.y + target.solidArea.height / 2;
        int distX = plCX - goCX;
        int distY = plCY - goCY;
        double dist = Math.sqrt(distX * distX + distY * distY);

        if (golemState == GolemState.CHASE || golemState == GolemState.ATTACK || golemState == GolemState.SMASH) {
            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";
        }

        int dx = 0, dy = 0;
        boolean wantsToMove = false;

        switch (golemState) {

            case IDLE:
                updateIdleAnim();
                wanderIdleTimer++;
                if (wanderIdleTimer >= 120) {
                    wanderIdleTimer = 0;
                    pickNewWanderDirection();
                    golemState = GolemState.WANDER;
                }
                break;

            case WANDER:
                updateWalkAnim();
                wantsToMove = true;
                wanderTimer--;
                if (wanderTimer <= 0) {
                    golemState      = GolemState.IDLE;
                    wanderIdleTimer = 0;
                }
                break;

            case CHASE:
                if (dist <= ATTACK_RANGE && attackCooldown == 0) {
                    if (smashCooldown == 0) {
                        // Skill pasif: serangan pembuka pakai Smash (AOE)
                        golemState           = GolemState.SMASH;
                        smashFrameIndex      = 0;
                        smashAnimTimer       = 0;
                        damageDealtThisSwing = false;
                    } else {
                        golemState           = GolemState.ATTACK;
                        attackFrameIndex     = 0;
                        attackAnimTimer      = 0;
                        damageDealtThisSwing = false;
                    }
                } else if (dist > DETECTION_RANGE) {
                    isProvoked      = false;
                    golemState      = GolemState.IDLE;
                    wanderIdleTimer = 0;
                } else {
                    updateWalkAnim();
                    wantsToMove = true;
                }
                break;

            case ATTACK:
                attackAnimTimer++;
                int maxF = direction.equals("down") ? 5 : 6;
                if (attackAnimTimer >= 7) {
                    attackAnimTimer = 0;
                    attackFrameIndex++;
                    
                    if (attackFrameIndex == maxF - 1) {
                         gp.playSE("GolemPunch.wav"); 
                    }

                    if (attackFrameIndex == 3 && !damageDealtThisSwing) {
                        hitPlayer(target, dist);
                        damageDealtThisSwing = true;
                    }
                    if (attackFrameIndex >= maxF) {
                        attackFrameIndex = 0;
                        attackCooldown   = ATTACK_COOLDOWN_MAX;
                        golemState       = GolemState.CHASE; 
                    }
                }
                break;

            case SMASH:
                smashAnimTimer++;
                if (smashAnimTimer >= 7) {
                    smashAnimTimer = 0;
                    smashFrameIndex++;

                    if (smashFrameIndex == 4) {
                        gp.playSE("GolemSmash.wav", 1.0f);
                    }

                    if (smashFrameIndex == 3 && !damageDealtThisSwing) {
                        hitPlayerSmash(target, dist);
                        damageDealtThisSwing = true;
                    }
                    if (smashFrameIndex >= 5) {
                        smashFrameIndex = 0;
                        smashCooldown   = SMASH_COOLDOWN_MAX;
                        attackCooldown  = ATTACK_COOLDOWN_MAX;
                        golemState      = GolemState.CHASE;
                    }
                }
                break;

            case SPAWN:
                // ditangani di updateSpawnAnim() / early return di atas
                break;

            case DEAD:
                break;
        }

        if (wantsToMove) {
            if (golemState == GolemState.CHASE) {
                int xStep = 0, yStep = 0;
                if (distX > 0) xStep = 1; else if (distX < 0) xStep = -1;
                if (distY > 0) yStep = 1; else if (distY < 0) yStep = -1;

                double moveSpeed = (xStep != 0 && yStep != 0) ? speed * 0.707 : speed;
                dx = (int) Math.round(xStep * moveSpeed);
                dy = (int) Math.round(yStep * moveSpeed);

            } else if (golemState == GolemState.WANDER) {
                switch (direction) {
                    case "up":    dy = -(int) speed; break;
                    case "down":  dy =  (int) speed; break;
                    case "left":  dx = -(int) speed; break;
                    case "right": dx =  (int) speed; break;
                }
            }

            boolean outOfWanderArea = false;
            if (golemState == GolemState.WANDER && wanderArea != null) {
                int nextCX = x + dx + solidArea.x + solidArea.width / 2;
                int nextCY = y + dy + solidArea.y + solidArea.height / 2;
                if (!wanderArea.contains(nextCX, nextCY)) outOfWanderArea = true;
            }

            if (outOfWanderArea) {
                golemState = GolemState.IDLE;
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
                    
                    if (!this.collisionOn) {
                        x += dx;
                    } else {
                        isBlockedX = true;
                    }
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
                    
                    if (!this.collisionOn) {
                        y += dy;
                    } else {
                        isBlockedY = true;
                    }
                    direction = tempDir; 
                }

                if ((dx != 0 && isBlockedX) || (dy != 0 && isBlockedY)) {
                    golemState = GolemState.IDLE;
                    wanderIdleTimer = 0;
                    walkFrameIndex = 0;
                    walkAnimTimer = 0;
                }
            }
        }
    }

    private void pickNewWanderDirection() {
        String[] dirs = {"up", "down", "left", "right"};
        direction   = dirs[RNG.nextInt(4)];
        wanderTimer = 40 + RNG.nextInt(60);
    }

    private void hitPlayer(Entity target, double dist) {
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

    // ── Smash sekarang AOE: kena kalau jarak pusat-ke-pusat <= SMASH_RADIUS,
    //     ga peduli arah hadap Golem (kanan/kiri/atas/bawah/diagonal semua kena) ──
    private void hitPlayerSmash(Entity target, double dist) {
        if (dist > SMASH_RADIUS) return;

        dealDamage(target, SMASH_DAMAGE);

        if (target instanceof player) {
            // Knockback ngikutin posisi relatif Golem -> player, bukan arah hadap Golem
            int gcx = x + solidArea.x + solidArea.width  / 2;
            int gcy = y + solidArea.y + solidArea.height / 2;
            int pcx = target.x + target.solidArea.x + target.solidArea.width  / 2;
            int pcy = target.y + target.solidArea.y + target.solidArea.height / 2;
            ((player) target).applyKnockback(getKnockbackDirection(gcx, gcy, pcx, pcy));
        }
    }

    private void dealDamage(Entity target, int dmg) {
        if (target instanceof player) {
            ((player) target).takeDamage(dmg);
        } else if (target instanceof GolemMinion) {
            ((GolemMinion) target).takeDamage(dmg);
        }
    }

    private String getKnockbackDirection(int fromX, int fromY, int toX, int toY) {
        int ddx = toX - fromX;
        int ddy = toY - fromY;
        if (Math.abs(ddx) > Math.abs(ddy)) {
            return ddx > 0 ? "right" : "left";
        } else {
            return ddy > 0 ? "down" : "up";
        }
    }

    private Rectangle getAttackBox() {
        int cx = x + solidArea.x + solidArea.width  / 2;
        int cy = y + solidArea.y + solidArea.height / 2;
        int reach  = 55;
        int spread = 28;

        switch (direction) {
            case "up":    return new Rectangle(cx - spread, cy - solidArea.height/2 - reach, spread*2, reach);
            case "down":  return new Rectangle(cx - spread, cy + solidArea.height/2,         spread*2, reach);
            case "left":  return new Rectangle(cx - solidArea.width/2 - reach, cy - spread,  reach, spread*2);
            case "right": return new Rectangle(cx + solidArea.width/2,         cy - spread,  reach, spread*2);
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
    
    private void updateWalkAnim() {

        walkAnimTimer++;

        if (walkAnimTimer >= 8) {

            walkFrameIndex = (walkFrameIndex + 1) % 6;

            walkAnimTimer = 0;

            if (walkFrameIndex == 2 || walkFrameIndex == 5) {

                // Cek jarak Golem ke player
                double distance = distCenterToCenter(this, gp.player);

                // Footstep hanya terdengar kalau Golem dekat player
                if (distance <= 500) {
                    gp.playSE("GolemFootStep.wav");
                }
            }
        }
    }

    private void updateIdleAnim() {
        idleAnimTimer++;
        if (idleAnimTimer >= 30) { idleFrameIndex = (idleFrameIndex + 1) % 2; idleAnimTimer = 0; }
    }
    
    private void updateDeadAnim() {
        deadAnimTimer++;
        if (deadAnimTimer >= DEAD_FRAME_DURATION) {
            deadAnimTimer = 0;
            deadFrameIndex++;
            if (deadFrameIndex >= DEAD_FRAME_COUNT) {
                deadFrameIndex = DEAD_FRAME_COUNT - 1; // hold frame terakhir
                finalizeDeath();
            }
        }
    }
    
    private void finalizeDeath() {
        alive      = false;
        golemState = GolemState.DEAD;
        // isDead SENGAJA gak di-set true di sini.
        // Kalo di-set true, kemungkinan ada sistem lain (GamePanel / killMonster)
        // yang ngecek isDead buat skip gambar, jadi mayatnya ilang instan
        // pas animasi kelar — padahal maunya baru ilang pas respawn.
        // Status "mati"-nya cukup diwakilin alive + golemState di file ini.

        if (gp.deathTotemManager != null) {
            gp.deathTotemManager.tryCapture(this);
        }

        gp.player.killMonster(this);
    }

    private void updateSpawnAnim() {
        spawnAnimTimer++;
        if (spawnAnimTimer >= 8) {
            spawnAnimTimer = 0;
            spawnFrameIndex++;
            if (spawnFrameIndex >= 5) {
                spawnFrameIndex = 0;
                golemState = GolemState.IDLE;
            }
        }
    }
    
    

    @Override
    public void takeDamage(int dmg) {
        if (!alive || golemState == GolemState.DEAD || golemState == GolemState.SPAWN) return;
        life -= dmg;
        isProvoked = true;
        
        // ── PERBAIKAN DI SINI ──
        // Kalau lagi ATTACK atau SMASH, jangan diinterupsi, biarin animasinya selesai dulu.
        if (golemState != GolemState.ATTACK && golemState != GolemState.SMASH) {
            golemState = GolemState.CHASE; 
        }
    }
    
    public void respawnGolem() {
        alive                = true;
        isDead               = false;
        life                 = maxLife;
        x                    = spawnX;
        y                    = spawnY;
        golemState           = GolemState.SPAWN;
        direction            = "down";
        walkFrameIndex       = 0;
        idleFrameIndex       = 0;
        attackFrameIndex     = 0;
        smashFrameIndex      = 0;
        spawnFrameIndex      = 0;
        spawnAnimTimer       = 0;
        attackCooldown       = 0;
        smashCooldown        = 0;
        golemRespawnTimer    = 0;
        wanderTimer          = 0;
        wanderIdleTimer      = 0;
        deadFrameIndex       = 0;
        deadAnimTimer        = 0;
        isProvoked           = false;
        damageDealtThisSwing = false;
        solidArea.x          = solidAreaDefaultX;
        solidArea.y          = solidAreaDefaultY;
        System.out.println("[Golem] Bangkit kembali!");
        
        gp.playSE("GolemSpawn.wav");
    }

    @Override
    public void draw(Graphics2D g2) {
       // if (!alive) return;

        int drawSize = gp.tileSize * DRAW_SIZE_MULT;
        int sx = x - gp.player.x + gp.player.screenX;
        int sy = y - gp.player.y + gp.player.screenY;

        if (sx + drawSize < -drawSize || sx > gp.screenWidth  + drawSize ||
            sy + drawSize < -drawSize || sy > gp.screenHeight + drawSize) return;

        int drawX = sx - gp.tileSize;
        int drawY = sy - gp.tileSize;

        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f));
        g2.setColor(Color.BLACK);
        int shadowW = (int)(drawSize * 0.55);
        int shadowH = shadowW / 4;
        g2.fillOval(drawX + drawSize / 2 - shadowW / 2, drawY + drawSize - shadowH - 20, shadowW, shadowH);
        g2.setComposite(oldComp);

        // ── Telegraph AOE smash (opsional, hapus blok ini kalau ga mau ada lingkaran radiusnya) ──
        if (golemState == GolemState.SMASH) {
            int gcx = sx + solidArea.x + solidArea.width  / 2;
            int gcy = sy + solidArea.y + solidArea.height / 2;
            Composite prevComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setColor(new Color(255, 60, 60));
            g2.fillOval(gcx - SMASH_RADIUS, gcy - SMASH_RADIUS, SMASH_RADIUS * 2, SMASH_RADIUS * 2);
            g2.setComposite(prevComp);
        }

        BufferedImage charImg = null;
        if (golemState == GolemState.DYING || golemState == GolemState.DEAD) {
            int safeIdx = Math.min(deadFrameIndex, DEAD_FRAME_COUNT - 1);
            charImg = deadFrames[safeIdx];
        
        }else if (golemState == GolemState.SPAWN) {
            int safeIdx = Math.min(spawnFrameIndex, 4);
            charImg = spawnFrames[safeIdx];
        } else if (golemState == GolemState.SMASH) {
            int safeIdx = Math.min(smashFrameIndex, 4);
            switch (direction) {
                case "down":  charImg = smashFrontFrames[safeIdx]; break;
                case "up":    charImg = smashBackFrames[safeIdx];  break;
                case "left":  charImg = smashLeftFrames[safeIdx];  break;
                case "right": charImg = smashRightFrames[safeIdx]; break;
            }
        } else if (golemState == GolemState.ATTACK) {
            int maxF    = direction.equals("down") ? 5 : 6;
            int safeIdx = Math.min(attackFrameIndex, maxF - 1);
            switch (direction) {
                case "down":  charImg = attackFrontFrames[safeIdx]; break;
                case "up":    charImg = attackBackFrames[safeIdx];  break;
                case "left":  charImg = attackLeftFrames[safeIdx];  break;
                case "right": charImg = attackRightFrames[safeIdx]; break;
            }
        } else if (golemState == GolemState.CHASE || golemState == GolemState.WANDER) {
            switch (direction) {
                case "down":  charImg = walkFrontFrames[walkFrameIndex]; break;
                case "up":    charImg = walkBackFrames[walkFrameIndex];  break;
                case "left":  charImg = walkLeftFrames[walkFrameIndex];  break;
                case "right": charImg = walkRightFrames[walkFrameIndex]; break;
            }
        } else {
            switch (direction) {
                case "down":  charImg = idleFrontFrames[idleFrameIndex]; break;
                case "up":    charImg = idleBackFrames[idleFrameIndex];  break;
                case "left":  charImg = idleLeftFrames[idleFrameIndex];  break;
                case "right": charImg = idleRightFrames[idleFrameIndex]; break;
            }
        }

        if (charImg != null) {
            if (golemState == GolemState.DYING || golemState == GolemState.DEAD) {
                int deadDrawSize = (int) (drawSize * DEAD_DRAW_SCALE);
                // kompensasi biar titik tengahnya tetap sama walau gambar digedein, baru ditambah offset geser
                int deadDrawX = drawX - (deadDrawSize - drawSize) / 2 + DEAD_OFFSET_X;
                int deadDrawY = drawY - (deadDrawSize - drawSize) / 2 + DEAD_OFFSET_Y;
                g2.drawImage(charImg, deadDrawX, deadDrawY, deadDrawSize, deadDrawSize, null);
            } else {
                g2.drawImage(charImg, drawX, drawY, drawSize, drawSize, null);
            }
        } else {
            g2.setColor(new Color(100, 100, 100, 200));
            g2.fillRect(drawX, drawY, drawSize, drawSize);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.drawString("GOLEM", drawX + 10, drawY + drawSize / 2);
        }
        
        if (golemState != GolemState.DEAD && golemState != GolemState.DYING && life < maxLife) {
            int barW   = 60, barH = 6;
            int hpFill = (int)((double) life / maxLife * barW);
            int barX   = drawX + drawSize / 2 - barW / 2;
            int barY   = drawY - 10;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, barY, barW, barH);
            g2.setColor(new Color(200, 50, 50));
            g2.fillRect(barX, barY, hpFill, barH);
        }
    }
}