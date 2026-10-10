package Tamed;

import Item.DeathTotem;
import Item.DeathTotem.CapturedType;
import Main.GamePanel;
import SFX.SoundManager;
import java.awt.*;
import java.awt.image.BufferedImage;

import Entitas.Entity;
import Entitas.Skeleton;

public class UndeadMinion extends Entity {
	 public static final int MAX_LIFE = 200;

    // ── State ────────────────────────────────────────────────────────────────
    enum MinionState { IDLE, WALK, ATTACK, HURT, DEATH }
    private MinionState state = MinionState.IDLE;

    // ── Animasi timer ────────────────────────────────────────────────────────
    private int walkFrame   = 0, walkTimer   = 0;
    private int idleFrame   = 0, idleTimer   = 0;
    private int attackFrame = 0, attackTimer = 0;
    private int hurtTimer   = 0;
    private int deathFrame  = 0, deathTimer  = 0;
    private boolean deathDone = false;

    private static final int HURT_DUR        = 15;
    private static final int ATK_SPEED       =  6;
    private static final int ATK_COOLDOWN_MAX = 80;
    private int attackCooldown = 0;

    // ── Referensi ────────────────────────────────────────────────────────────
    private final DeathTotem ownerTotem;
    private final CapturedType minionType;
    private final SoundManager sfx = new SoundManager();

    public int spawnX, spawnY;

    private static final int DETECTION_RANGE = 250;
    private static final int ATTACK_RANGE    =  40;
    private static final int ATTACK_DAMAGE   =  20;

    public UndeadMinion(GamePanel gp, CapturedType type, DeathTotem totem) {
        super(gp);
        this.alive      = true;
        this.ownerTotem = totem;
        this.minionType = type;
        this.maxLife    = 200;
        this.life       = maxLife;
        this.speed      = 1.8;
        this.direction  = "down";
        this.solidArea  = new Rectangle(20, 20, 40, 40);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 0;   // pion tidak kasih EXP ke musuh
    }

    public boolean isAlive()        { return alive; }
    public boolean isDeathAnimDone(){ return !alive && deathDone; }

    public int getSortY() { return this.y + solidArea.y + solidArea.height; }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (state == MinionState.DEATH) { updateDeath(); return; }
        if (!alive) return;

        if (life <= 0) { die(); return; }

        if (state == MinionState.HURT) {
            hurtTimer--;
            if (hurtTimer <= 0) state = MinionState.IDLE;
            return;
        }

        if (attackCooldown > 0) attackCooldown--;
        if (isHit) { hitFlashTimer--; if (hitFlashTimer <= 0) isHit = false; }

        Skeleton target = findNearestSkeleton();
        boolean behaviorExecuted = false;

        // 1. PRIORITAS UTAMA: Menyerang Musuh (Skeleton) terdekat jika ada
        if (target != null) {
            int skCX = target.x + target.solidArea.x + target.solidArea.width  / 2;
            int skCY = target.y + target.solidArea.y + target.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width  / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = skCX - myCX;
            int distY = skCY - myCY;
            double dist = Math.sqrt(distX * distX + distY * distY);

            // Arahkan pandangan ke target musuh
            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";

            if (dist <= ATTACK_RANGE && attackCooldown == 0) {
                if (state != MinionState.ATTACK) {
                    state = MinionState.ATTACK;
                    attackFrame = 0;
                    attackTimer = 0;
                }
                behaviorExecuted = true;
            } else if (dist <= DETECTION_RANGE) {
                state = MinionState.WALK;
                int dx = 0, dy = 0;
                if (distX > 0) dx = 1; else if (distX < 0) dx = -1;
                if (distY > 0) dy = 1; else if (distY < 0) dy = -1;
                double moveSpeed = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
                int mdx = (int) Math.round(dx * moveSpeed);
                int mdy = (int) Math.round(dy * moveSpeed);

                // Cek tabrakan sebelum melangkah
                if (!checkCollisionAt(mdx, mdy)) { 
                    x += mdx; 
                    y += mdy; 
                }
                behaviorExecuted = true;
            }
        }
        

        // 2. PRIORITAS KEDUA: Ikuti Player jika tidak ada musuh dalam jangkauan deteksi
        if (!behaviorExecuted && state != MinionState.ATTACK) {
            int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2;
            int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = plCX - myCX;
            int distY = plCY - myCY;
            double dist = Math.sqrt(distX * distX + distY * distY);

            int followStopRange = 60; // Jarak aman (px) agar tidak menempel persis di tengah badan player

            if (dist > followStopRange) {
                state = MinionState.WALK;
                
                // Arahkan pandangan ke player
                if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
                else direction = distY > 0 ? "down" : "up";

                int dx = 0, dy = 0;
                if (distX > 0) dx = 1; else if (distX < 0) dx = -1;
                if (distY > 0) dy = 1; else if (distY < 0) dy = -1;
                double moveSpeed = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
                int mdx = (int) Math.round(dx * moveSpeed);
                int mdy = (int) Math.round(dy * moveSpeed);

                // Cek tabrakan sebelum melangkah mengejar player
                if (!checkCollisionAt(mdx, mdy)) { 
                    x += mdx; 
                    y += mdy; 
                }
            } else {
                state = MinionState.IDLE;
            }
        }

        updateAnimation(target);
    }

    /**
     * Memeriksa apakah langkah mdx, mdy berikutnya menabrak dinding atau skeleton lain.
     * @return true jika menabrak, false jika aman.
     */
    private boolean checkCollisionAt(int mdx, int mdy) {
        Rectangle nextBox = new Rectangle(
            x + solidArea.x + mdx, 
            y + solidArea.y + mdy,
            solidArea.width, 
            solidArea.height
        );

        // 1. Cek tabrakan dengan batasan dinding domain (map)
        if (gp.domainManager != null && gp.domainManager.checkCollision(nextBox)) {
            return true;
        }

        // 2. Cek tabrakan dengan Skeleton (Musuh) lain agar tidak saling menembus
        if (gp.domainManager != null) {
            for (Skeleton sk : gp.domainManager.getSkeletons()) {
                if (sk == null || !sk.alive) continue;
                
                Rectangle skBox = new Rectangle(
                    sk.x + sk.solidArea.x, 
                    sk.y + sk.solidArea.y,
                    sk.solidArea.width, 
                    sk.solidArea.height
                );
                
                if (nextBox.intersects(skBox)) {
                    return true; // Ada halangan monster lain di depan
                }
            }
        }
        return false;
    }

    private Skeleton findNearestSkeleton() {
        if (gp.domainManager == null) return null;
        Skeleton nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Skeleton sk : gp.domainManager.getSkeletons()) {
            if (sk == null || !sk.alive) continue;
            int cx = sk.x + sk.solidArea.x + sk.solidArea.width  / 2;
            int cy = sk.y + sk.solidArea.y + sk.solidArea.height / 2;
            int mx = x   + solidArea.x     + solidArea.width  / 2;
            int my = y   + solidArea.y     + solidArea.height / 2;
            double d = Math.sqrt((cx - mx) * (cx - mx) + (cy - my) * (cy - my));
            if (d < minDist) { minDist = d; nearest = sk; }
        }
        return nearest;
    }

    private void updateAnimation(Skeleton target) {
        switch (state) {
            case IDLE:
                idleTimer++;
                if (idleTimer >= 20) { idleFrame = (idleFrame + 1) % 2; idleTimer = 0; }
                break;
            case WALK:
                walkTimer++;
                if (walkTimer >= 8) { walkFrame = (walkFrame + 1) % 6; walkTimer = 0; }
                break;
            case ATTACK:
                attackTimer++;
                if (attackTimer >= ATK_SPEED) {
                    attackTimer = 0;
                    attackFrame++;
                    if (attackFrame == 2 && target != null) hitTarget(target);
                    if (attackFrame >= 4) {
                        attackFrame    = 0;
                        attackCooldown = ATK_COOLDOWN_MAX;
                        state          = MinionState.IDLE;
                    }
                }
                break;
        }
    }

    private void hitTarget(Skeleton target) {
        Rectangle atk = getAttackBox();
        Rectangle tgt = new Rectangle(
            target.x + target.solidArea.x, target.y + target.solidArea.y,
            target.solidArea.width, target.solidArea.height);
        if (atk != null && atk.intersects(tgt)) {
            int lifeBefore = target.life;
            target.takeDamage(ATTACK_DAMAGE);
            sfx.swingSound();
            // EXP hanya kalau target mati akibat hit ini
            if (lifeBefore > 0 && target.life <= 0 && gp.deathTotemManager != null) {
                gp.deathTotemManager.onPawnKill(target.getExpValue);
            }
        }
    }

    private Rectangle getAttackBox() {
        int reach = 55;
        switch (direction) {
            case "up":    return new Rectangle(x, y - reach, gp.tileSize, reach);
            case "down":  return new Rectangle(x, y + gp.tileSize, gp.tileSize, reach);
            case "left":  return new Rectangle(x - reach, y, reach, gp.tileSize);
            case "right": return new Rectangle(x + gp.tileSize, y, reach, gp.tileSize);
        }
        return null;
    }

    private void die() {
        alive = false;
        state = MinionState.DEATH;
        deathFrame = 0; deathTimer = 0; deathDone = false;
        solidArea.x = -9999; solidArea.y = -9999;
        if (ownerTotem != null) ownerTotem.releasePawn();
    }

    private void updateDeath() {
        if (deathDone) return;
        deathTimer++;
        if (deathTimer >= 10) {
            deathTimer = 0;
            deathFrame++;
            if (deathFrame >= 3) { deathFrame = 2; deathDone = true; }
        }
    }

    // ── TAKE DAMAGE (bisa diserang skeleton) ─────────────────────────────────

    @Override
    public void takeDamage(int dmg) {
        if (!alive || state == MinionState.DEATH) return;
        life -= dmg;
        isHit = true;
        hitFlashTimer = 8;
        sfx.playWoosh();
        if (life > 0) {
            state = MinionState.HURT;
            hurtTimer = HURT_DUR;
            attackFrame = 0;
            attackTimer = 0;
        }
    }

    // ── DRAW ─────────────────────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g2) {
        if (!alive && state != MinionState.DEATH) return;

        int drawSize = gp.tileSize * 2;
        int sx = x - gp.player.x + gp.player.screenX - gp.tileSize / 2;
        int sy = y - gp.player.y + gp.player.screenY - gp.tileSize / 3;
        if (sx + drawSize < 0 || sx > gp.screenWidth || sy + drawSize < 0 || sy > gp.screenHeight) return;

        BufferedImage charImg = null, wpImg = null;

        switch (state) {
            case DEATH:  charImg = getDeathFrame();                        break;
            case HURT:   charImg = getHurtFrame();                         break;
            case ATTACK: charImg = getAttackFrame(); wpImg = getWeaponFrame(); break;
            case WALK:   charImg = getWalkFrame();                         break;
            default:     charImg = getIdleFrame();                         break;
        }

        // Shadow
        if (state != MinionState.DEATH) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.setColor(Color.BLACK);
            int sw = drawSize / 3, sh = sw / 3;
            g2.fillOval(sx + drawSize/2 - sw/2, sy + drawSize - sh - 24, sw, sh);
            g2.setComposite(old);
        }

        // Sprite karakter
        if (charImg != null) g2.drawImage(charImg, sx, sy, drawSize, drawSize, null);
        if (wpImg   != null) g2.drawImage(wpImg,   sx, sy, drawSize, drawSize, null);

        // Tint hijau agar beda dari skeleton musuh
        if (state != MinionState.DEATH) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setColor(new Color(80, 255, 120));
            g2.fillRect(sx, sy, drawSize, drawSize);
            g2.setComposite(old);
        }

        // HP bar warna hijau
        if (state != MinionState.DEATH) {
            int barW = 40, barH = 5;
            int fill = (int)((double) life / maxLife * barW);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(sx + drawSize/2 - barW/2, sy - 8, barW, barH);
            g2.setColor(new Color(80, 200, 80));
            g2.fillRect(sx + drawSize/2 - barW/2, sy - 8, fill, barH);
        }
    }

    // ── Frame selectors (pakai shared Skeleton static cache) ─────────────────

    private BufferedImage getWalkFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_walkFront[walkFrame];
            case "up":    return Skeleton.S_walkBack[walkFrame];
            case "left":  return Skeleton.S_walkLeft[walkFrame];
            default:      return Skeleton.S_walkRight[walkFrame];
        }
    }
    private BufferedImage getIdleFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_idleFront[idleFrame];
            case "up":    return Skeleton.S_idleBack[idleFrame];
            case "left":  return Skeleton.S_idleLeft[idleFrame];
            default:      return Skeleton.S_idleRight[idleFrame];
        }
    }
    private BufferedImage getAttackFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_swing1Front[attackFrame];
            case "up":    return Skeleton.S_swing1Back[attackFrame];
            case "left":  return Skeleton.S_swing1Left[attackFrame];
            default:      return Skeleton.S_swing1Right[attackFrame];
        }
    }
    private BufferedImage getWeaponFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_wp1Front[attackFrame];
            case "up":    return Skeleton.S_wp1Back[attackFrame];
            case "left":  return Skeleton.S_wp1Left[attackFrame];
            default:      return Skeleton.S_wp1Right[attackFrame];
        }
    }
    private BufferedImage getHurtFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_hurtFront;
            case "up":    return Skeleton.S_hurtBack;
            case "left":  return Skeleton.S_hurtLeft;
            default:      return Skeleton.S_hurtRight;
        }
    }
    private BufferedImage getDeathFrame() {
        switch (direction) {
            case "down":  return Skeleton.S_deathFront[deathFrame];
            case "up":    return Skeleton.S_deathBack[deathFrame];
            case "left":  return Skeleton.S_deathLeft[deathFrame];
            default:      return Skeleton.S_deathRight[deathFrame];
        }
    }
}