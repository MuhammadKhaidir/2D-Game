package Entitas;

import Item.DeathTotem;
import Main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GolemMinion extends Entity {

    enum MinionState { IDLE, WALK, ATTACK, SMASH, SPAWN, DEATH }
    private MinionState state = MinionState.IDLE;

    // ── Anim timers ──────────────────────────────────────────────────────
    private int walkFrame = 0, walkTimer   = 0;
    private int idleFrame = 0, idleTimer   = 0;
    private int atkFrame  = 0, atkTimer    = 0;
    private int smashFrame= 0, smashTimer  = 0;
    private int spawnFrame= 0, spawnTimer  = 0;
    private int deathFrame= 0, deathTimer  = 0;
    private boolean deathDone = false;

    private static final int ATK_SPEED       =  8;
    private static final int ATK_COOLDOWN_MAX = 140;
    private int attackCooldown = 0;

    private static final int DETECTION_RANGE = 320;
    private static final int ATTACK_RANGE    =  70;
    private static final int ATTACK_DAMAGE   =  200;

    // ── Smash: skill pasif pembuka, AOE, cooldown 10 detik ──
    private static final int SMASH_SPEED        =  7;
    private static final int SMASH_COOLDOWN_MAX = 600; // 10 detik @60fps
    private static final int SMASH_DAMAGE       = 350;
    private static final int SMASH_RADIUS       =  90;
    private int smashCooldown = 0;

    private static final int DRAW_SIZE_MULT = 3; // sama kayak Golem asli

    private final DeathTotem ownerTotem;
    public int  spawnX, spawnY;
    
    private static final int THREAT_RADIUS_TO_PLAYER = 200;

    public GolemMinion(GamePanel gp, DeathTotem totem) {
        super(gp);
        this.ownerTotem = totem;
        this.alive       = true; 
        this.maxLife    = 4000;   // lebih tanky dari skeleton minion
        this.life       = maxLife;
        this.speed      = 0.9;    // lambat tapi kuat
        this.direction  = "down";
        this.solidArea = new Rectangle(14, 28, 20, 20);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 0;

        // Mulai dengan animasi munculnya minion
        this.state      = MinionState.SPAWN;
        this.spawnFrame  = 0;
        this.spawnTimer  = 0;
        
        gp.playSE("GolemSpawn.wav", 6.0f);
    }

    public boolean isAlive()         { return alive; }
    public boolean isDeathAnimDone() { return !alive && deathDone; }

    @Override
    public int getSortY() { return this.y + solidArea.y + solidArea.height; }

    // ── UPDATE ───────────────────────────────────────────────────────────

    @Override
    public void update() {
        if (state == MinionState.DEATH) { updateDeath(); return; }
        if (state == MinionState.SPAWN) { updateSpawnAnim(); return; }
        if (!alive) return;
        if (life <= 0) { die(); return; }

        if (attackCooldown > 0) attackCooldown--;
        if (smashCooldown  > 0) smashCooldown--;

        Entity target = findThreatNearPlayer();
        boolean actedOnEnemy = false;
        

        // 1. Prioritas utama: serang Skeleton / Golem liar / monster / SkeletonKing terdekat
        if (target != null) {
            int skCX = target.x + target.solidArea.x + target.solidArea.width  / 2;
            int skCY = target.y + target.solidArea.y + target.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width  / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = skCX - myCX, distY = skCY - myCY;
            double dist = Math.sqrt(distX*distX + distY*distY);

            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";

            if (dist <= ATTACK_RANGE && attackCooldown == 0) {
                if (state != MinionState.ATTACK && state != MinionState.SMASH) {
                    if (smashCooldown == 0) {
                        // Skill pasif: serangan pembuka pakai Smash (AOE)
                        state = MinionState.SMASH; smashFrame = 0; smashTimer = 0;
                    } else {
                        state = MinionState.ATTACK; atkFrame = 0; atkTimer = 0;
                    }
                }
                actedOnEnemy = true;
            } else if (dist <= DETECTION_RANGE) {
                if (state != MinionState.ATTACK && state != MinionState.SMASH) {
                    state = MinionState.WALK;
                    int dx = distX > 0 ? 1 : (distX < 0 ? -1 : 0);
                    int dy = distY > 0 ? 1 : (distY < 0 ? -1 : 0);
                    double spd = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
                    moveIfClear((int)Math.round(dx*spd), (int)Math.round(dy*spd));
                }
                actedOnEnemy = true;
            }
        }

        // 2. Prioritas kedua: ikuti player
        if (!actedOnEnemy && state != MinionState.ATTACK && state != MinionState.SMASH) {
            int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
            int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width  / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = plCX - myCX, distY = plCY - myCY;
            double dist = Math.sqrt(distX*distX + distY*distY);

            if (dist > 80) {
                state = MinionState.WALK;
                if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
                else direction = distY > 0 ? "down" : "up";
                int dx = distX > 0 ? 1 : (distX < 0 ? -1 : 0);
                int dy = distY > 0 ? 1 : (distY < 0 ? -1 : 0);
                double spd = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
                moveIfClear((int)Math.round(dx*spd), (int)Math.round(dy*spd));
            } else {
                state = MinionState.IDLE;
                
                // ── PERBAIKAN DI SINI ──
                // Biar pas udah sampai/deket player, Golem tetep noleh/ngadep ke posisi player berada
                if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
                else direction = distY > 0 ? "down" : "up";
            }
        }

        updateAnimation(target);
    }

    private void moveIfClear(int mdx, int mdy) {
        if (mdx == 0 && mdy == 0) {
            if (state != MinionState.ATTACK && state != MinionState.SMASH) state = MinionState.IDLE;
            return;
        }

        // Simpan arah asli minion
        String oldDirection = direction;

        // Set arah sementara mengikuti pergerakan dx/dy untuk kalkulasi engine checkEntity
        if (mdx > 0) direction = "right";
        else if (mdx < 0) direction = "left";
        else if (mdy > 0) direction = "down";
        else if (mdy < 0) direction = "up";

        this.collisionOn = false;

        // 1. Cek collision tile map/tembok
        if (gp.domainManager != null) {
            Rectangle next = new Rectangle(x + solidArea.x + mdx, y + solidArea.y + mdy, solidArea.width, solidArea.height);
            if (gp.domainManager.checkCollision(next)) {
                this.collisionOn = false;
            }
        }

        // 2. 🔴 FIX: Gunakan sistem engine resmi untuk cek tabrakan dengan Player
        if (!this.collisionOn) {
            gp.colision.checkEntity(this, gp.player, mdx, mdy);
        }

        // 3. Cek collision dengan musuh (skeleton)
        if (!this.collisionOn && gp.domainManager != null && gp.domainManager.getSkeletons() != null) {
            for (Skeleton sk : gp.domainManager.getSkeletons()) {
                if (sk == null || !sk.alive) continue;
                gp.colision.checkEntity(this, sk, mdx, mdy);
            }
        }
        
        if (!this.collisionOn && gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m.isDead) continue;
                if (!(m instanceof Golem) && !(m instanceof monster)) continue;
                gp.colision.checkEntity(this, m, mdx, mdy);
            }
        }

        // 4. FIX: Cek collision dengan SkeletonKing (boss), biar golem gak numpuk/tembus sama boss
        if (!this.collisionOn && gp.domainManager != null && gp.domainManager.getBossSkeleton() != null) {
            SkeletonKing king = gp.domainManager.getBossSkeleton();
            if (king.alive) {
                gp.colision.checkEntity(this, king, mdx, mdy);
            }
        }

        // Eksekusi jalan jika aman
        if (!this.collisionOn) {
            x += mdx;
            y += mdy;
        } else {
            if (state != MinionState.ATTACK && state != MinionState.SMASH) {
                state = MinionState.IDLE; // Berhenti jika mentok
            }
        }

        // Kembalikan arah asli agar visual menghadap target tidak rusak
        direction = oldDirection;
    }

    private Entity findThreatNearPlayer() {
        int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;

        Entity nearest = null;
        double minDist = Double.MAX_VALUE;

        // 1. Skeleton (domain)
        if (gp.domainManager != null && gp.domainManager.getSkeletons() != null) {
            for (Skeleton sk : gp.domainManager.getSkeletons()) {
                if (sk == null || !sk.alive) continue;
                double d = distToPlayer(sk, plCX, plCY);
                if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = sk; }
            }
        }

        // 2. Golem liar & monster (slime) — sama-sama di gp.monsters
        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m.isDead) continue;
                if (m == this) continue; // jaga-jaga, jangan target diri sendiri
                if (!(m instanceof Golem) && !(m instanceof monster)) continue;

                double d = distToPlayer(m, plCX, plCY);
                if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = m; }
            }
        }

        // 3. FIX: SkeletonKing (boss Tomb) — biar golem juga inisiatif nyerang boss
        if (gp.domainManager != null && gp.domainManager.getBossSkeleton() != null) {
            SkeletonKing king = gp.domainManager.getBossSkeleton();
            if (king.alive) {
                double d = distToPlayer(king, plCX, plCY);
                if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = king; }
            }
        }

        return nearest;
    }

    private double distToPlayer(Entity e, int plCX, int plCY) {
        int cx = e.x + e.solidArea.x + e.solidArea.width  / 2;
        int cy = e.y + e.solidArea.y + e.solidArea.height / 2;
        int dx = cx - plCX, dy = cy - plCY;
        return Math.sqrt(dx*dx + dy*dy);
    }

    private void updateAnimation(Entity target)  {
        switch (state) {
            case IDLE:
                idleTimer++;
                if (idleTimer >= 30) { idleFrame = (idleFrame+1) % 2; idleTimer = 0; }
                break;
            case WALK:
                walkTimer++;
                if (walkTimer >= 8) { 
                    walkFrame = (walkFrame + 1) % 6; 
                    walkTimer = 0; 
                    if (walkFrame == 2 || walkFrame == 5) {
                        gp.playSE("GolemFootStep.wav");
                    }
                }
                break;
            case ATTACK:
                atkTimer++;
                if (atkTimer >= ATK_SPEED) {
                    atkTimer = 0;
                    int maxF = direction.equals("down") ? 5 : 6;
                    atkFrame++;
                    if (atkFrame == 3 && target != null) hitTarget(target);
                    if (atkFrame >= maxF) {
                        atkFrame = 0; attackCooldown = ATK_COOLDOWN_MAX;
                        state = MinionState.IDLE;
                    }
                }
                break;
            case SMASH:
                smashTimer++;
                if (smashTimer >= SMASH_SPEED) {
                    smashTimer = 0;
                    smashFrame++;

                    if (smashFrame == 4) {
                        gp.playSE("GolemSmash.wav", 1.0f);
                    }
                    if (smashFrame == 3) {
                        hitAreaSmash();
                    }
                    if (smashFrame >= 5) {
                        smashFrame      = 0;
                        smashCooldown   = SMASH_COOLDOWN_MAX;
                        attackCooldown  = ATK_COOLDOWN_MAX;
                        state           = MinionState.IDLE;
                    }
                }
                break;
        }
    }

    private void hitTarget(Entity target) {
        Rectangle atk = getAttackBox();
        Rectangle tgt = new Rectangle(
            target.x + target.solidArea.x, target.y + target.solidArea.y,
            target.solidArea.width, target.solidArea.height);
        if (atk != null && atk.intersects(tgt)) {
            int lifeBefore = target.life;
            dealDamage(target, ATTACK_DAMAGE);
            gp.playSE("GolemPunch.wav", -12f);
            // EXP hanya kalau target mati akibat hit ini
            if (lifeBefore > 0 && target.life <= 0 && gp.deathTotemManager != null) {
                gp.deathTotemManager.onPawnKill(target.getExpValue);
            }
        }
    }

    
    /**
     * Helper buat manggil takeDamage yang signature-nya beda-beda
     * tergantung tipe musuhnya (Skeleton/Golem pakai 1 parameter,
     * monster pakai 2 parameter termasuk attacker, SkeletonKing pakai 1 parameter).
     */
    private void dealDamage(Entity target, int dmg) {
        if (target instanceof Skeleton) {
            ((Skeleton) target).takeDamage(dmg);
        } else if (target instanceof Golem) {
            ((Golem) target).takeDamage(dmg);
        } else if (target instanceof monster) {
            ((monster) target).takeDamage(dmg, this);
        } else if (target instanceof SkeletonKing) {
            ((SkeletonKing) target).takeDamage(dmg);
        }
    }

 // ── Smash: AOE, semua musuh (Skeleton/Golem liar/monster/SkeletonKing) dalam radius kena ──
    private void hitAreaSmash() {
        int myCX = x + solidArea.x + solidArea.width  / 2;
        int myCY = y + solidArea.y + solidArea.height / 2;

        if (gp.domainManager != null && gp.domainManager.getSkeletons() != null) {
            for (Skeleton sk : gp.domainManager.getSkeletons()) {
                if (sk == null || !sk.alive) continue;
                if (distFromPoint(sk, myCX, myCY) <= SMASH_RADIUS) {
                    int lifeBefore = sk.life;
                    sk.takeDamage(SMASH_DAMAGE);
                    if (lifeBefore > 0 && sk.life <= 0 && gp.deathTotemManager != null) {
                        gp.deathTotemManager.onPawnKill(sk.getExpValue);
                    }
                }
            }
        }

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m.isDead) continue;
                if (!(m instanceof Golem) && !(m instanceof monster)) continue;
                if (distFromPoint(m, myCX, myCY) <= SMASH_RADIUS) {
                    int lifeBefore = m.life;
                    dealDamage(m, SMASH_DAMAGE);
                    if (lifeBefore > 0 && m.life <= 0 && gp.deathTotemManager != null) {
                        gp.deathTotemManager.onPawnKill(m.getExpValue);
                    }
                }
            }
        }

        // FIX: Boss SkeletonKing juga kena AOE smash kalau lagi ada di radius
        if (gp.domainManager != null && gp.domainManager.getBossSkeleton() != null) {
            SkeletonKing king = gp.domainManager.getBossSkeleton();
            if (king.alive && distFromPoint(king, myCX, myCY) <= SMASH_RADIUS) {
                int lifeBefore = king.life;
                king.takeDamage(SMASH_DAMAGE);
                if (lifeBefore > 0 && king.life <= 0 && gp.deathTotemManager != null) {
                    gp.deathTotemManager.onPawnKill(king.getExpValue);
                }
            }
        }
    
        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m.isDead) continue;
                if (!(m instanceof Golem) && !(m instanceof monster)) continue;
                if (distFromPoint(m, myCX, myCY) <= SMASH_RADIUS) {
                    dealDamage(m, SMASH_DAMAGE);
                }
            }
        }
    }

    private double distFromPoint(Entity e, int px, int py) {
        int cx = e.x + e.solidArea.x + e.solidArea.width  / 2;
        int cy = e.y + e.solidArea.y + e.solidArea.height / 2;
        int dx = cx - px, dy = cy - py;
        return Math.sqrt(dx*dx + dy*dy);
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

    @Override
    public void takeDamage(int dmg) {
        if (!alive || state == MinionState.DEATH || state == MinionState.SPAWN) return;
        life -= dmg;
        isHit = true; hitFlashTimer = 8;
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
            deathTimer = 0; deathFrame++;
            if (deathFrame >= 7) { deathFrame = 3; deathDone = true; }
        }
    }

    private void updateSpawnAnim() {
        spawnTimer++;
        if (spawnTimer >= 8) {
            spawnTimer = 0;
            spawnFrame++;
            if (spawnFrame >= 5) {
                spawnFrame = 0;
                state = MinionState.IDLE;
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!alive && state != MinionState.DEATH) return;

        int drawSize = gp.tileSize * DRAW_SIZE_MULT;
        int sx = x - gp.player.x + gp.player.screenX - gp.tileSize;
        int sy = y - gp.player.y + gp.player.screenY - gp.tileSize;
        if (sx+drawSize < -drawSize || sx > gp.screenWidth+drawSize ||
            sy+drawSize < -drawSize || sy > gp.screenHeight+drawSize) return;

        // Shadow
        if (state != MinionState.DEATH) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(Color.BLACK);
            int sw = (int)(drawSize*0.5), sh = sw/4;
            g2.fillOval(sx+drawSize/2-sw/2, sy+drawSize-sh-20, sw, sh);
            g2.setComposite(old);
        }

        // Telegraph AOE smash (opsional, hapus blok ini kalau ga mau ada lingkaran radiusnya)
        if (state == MinionState.SMASH) {
            int gcx = sx + drawSize / 2;
            int gcy = sy + drawSize / 2;
            Composite prevComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setColor(new Color(140, 80, 255));
            g2.fillOval(gcx - SMASH_RADIUS, gcy - SMASH_RADIUS, SMASH_RADIUS * 2, SMASH_RADIUS * 2);
            g2.setComposite(prevComp);
        }

        BufferedImage charImg = getFrame();
        if (charImg != null) {
            g2.drawImage(charImg, sx, sy, drawSize, drawSize, null);
        }

        // Tint ungu
        if (state != MinionState.DEATH) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g2.setColor(new Color(140, 80, 255));
            g2.fillRect(sx, sy, drawSize, drawSize);
            g2.setComposite(old);
        }

        // HP bar warna ungu
        if (state != MinionState.DEATH && life < maxLife) {
            int barW = 60, barH = 6;
            int fill = (int)((double)life / maxLife * barW);
            int barX = sx + drawSize/2 - barW/2;
            int barY = sy - 10;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, barY, barW, barH);
            g2.setColor(new Color(140, 80, 255));
            g2.fillRect(barX, barY, fill, barH);
        }
    }

    private BufferedImage getFrame() {
        switch (state) {
            case SPAWN: {
                int safeIdx = Math.min(spawnFrame, 4);
                return Golem.S_spawn[safeIdx];
            }
            case WALK:
                switch (direction) {
                    case "down":  return Golem.S_walkFront[walkFrame];
                    case "up":    return Golem.S_walkBack[walkFrame];
                    case "left":  return Golem.S_walkLeft[walkFrame];
                    default:      return Golem.S_walkRight[walkFrame];
                }
            case ATTACK: {
                int maxF = direction.equals("down") ? 5 : 6;
                int safeIdx = Math.min(atkFrame, maxF-1);
                switch (direction) {
                    case "down":  return Golem.S_attackFront[safeIdx];
                    case "up":    return Golem.S_attackBack[safeIdx];
                    case "left":  return Golem.S_attackLeft[safeIdx];
                    default:      return Golem.S_attackRight[safeIdx];
                }
            }
            case SMASH: {
                int safeIdx = Math.min(smashFrame, 4);
                switch (direction) {
                    case "down":  return Golem.S_smashFront[safeIdx];
                    case "up":    return Golem.S_smashBack[safeIdx];
                    case "left":  return Golem.S_smashLeft[safeIdx];
                    default:      return Golem.S_smashRight[safeIdx];
                }
            }
            case DEATH: {
                int safeIdx = Math.min(deathFrame, 3);
                return Golem.S_dead[safeIdx];
            }
            default: // IDLE, DEATH
                switch (direction) {
                    case "down":  return Golem.S_idleFront[idleFrame % 2];
                    case "up":    return Golem.S_idleBack[idleFrame % 2];
                    case "left":  return Golem.S_idleLeft[idleFrame % 2];
                    default:      return Golem.S_idleRight[idleFrame % 2];
                }
        }
    }
}