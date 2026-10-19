package Tamed;

import Item.DeathTotem;
import Main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;

import Entitas.Entity;
import Entitas.Golem;
import Entitas.Golemite;
import Entitas.Skeleton;
import Entitas.SkeletonKing;
import Entitas.monster;

/**
 * GolemiteMinion — versi tamed dari Golemite (tier RARE).
 * Sama kayak Golemite liar: cuma punya animasi Walk (6 frame) dan Punch (4 frame),
 * jadi nggak ada Smash / Spawn anim / Dead anim (mati = fade out).
 * Prioritas: serang musuh yang deket player, kalau nggak ada -> ikutin player.
 */
public class GolemiteMinion extends GolemMinion {

    public static final int MAX_LIFE = 1200;

    enum GState { IDLE, WALK, ATTACK, DEATH }
    private GState state = GState.IDLE;

    // ── Anim ─────────────────────────────────────────────────────────────
    private static final int WALK_FRAMES           = 6;
    private static final int ATTACK_FRAMES         = 4;
    private static final int WALK_FRAME_DURATION   = 8;
    private static final int ATTACK_FRAME_DURATION = 6;
    private static final int HIT_FRAME             = 2; // frame ke-3 = momen pukulan kena

    private int walkFrame = 0, walkTimer = 0;
    private int atkFrame  = 0, atkTimer  = 0;
    private boolean damageDealtThisSwing = false;

    // ── Spawn fade-in & death fade-out (karena nggak ada anim spawn/dead) ──
    private static final int SPAWN_FADE_FRAMES = 20;
    private static final int DEATH_FADE_FRAMES = 30;
    private int spawnFade  = 0;
    private int deathTimer = 0;
    private boolean deathDone = false;

    // ── Combat ───────────────────────────────────────────────────────────
    private static final int ATTACK_COOLDOWN_MAX = 90;
    private static final int ATTACK_DAMAGE       = 80;
    private static final int ATTACK_RANGE        = 55;
    private static final int DETECTION_RANGE     = 300;
    private static final int ATTACK_REACH        = 45;
    private static final int ATTACK_SPREAD       = 24;
    private static final int THREAT_RADIUS_TO_PLAYER = 200;
    private static final int FOLLOW_STOP_RANGE   = 70;
    private int attackCooldown = 0;

    // ── Tampilan (sama kayak Golemite liar) ──────────────────────────────
    private static final int    DRAW_SIZE_MULT = 3;
    private static final double SPRITE_LOWER   = 0.11;

    private final DeathTotem ownerTotem;

    public GolemiteMinion(GamePanel gp, DeathTotem totem) {
        super(gp, totem);   // GolemMinion udah mainin suara spawn
        this.ownerTotem = totem;
        this.alive      = true;
        this.maxLife    = MAX_LIFE;
        this.life       = maxLife;
        this.speed      = 1.0;
        this.direction  = "down";
        this.solidArea  = new Rectangle(14, 28, 20, 20);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 0; // pion nggak kasih EXP ke musuh

        Golemite.preloadImages();
    }

    @Override
    public boolean isDeathAnimDone() { return !alive && deathDone; }

    @Override
    public int getSortY() { return this.y + solidAreaDefaultY + 20; }

    // ────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────────────
    @Override
    public void update() {
        if (spawnFade < SPAWN_FADE_FRAMES) spawnFade++;

        if (state == GState.DEATH) { updateDeath(); return; }
        if (!alive) return;
        if (life <= 0) { die(); return; }

        if (attackCooldown > 0) attackCooldown--;

        Entity target = findThreatNearPlayer();
        boolean actedOnEnemy = false;

        // 1. Prioritas utama: serang musuh terdekat dari player
        if (target != null) {
            int tCX = target.x + target.solidArea.x + target.solidArea.width  / 2;
            int tCY = target.y + target.solidArea.y + target.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width  / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = tCX - myCX, distY = tCY - myCY;
            double dist = Math.sqrt(distX * distX + distY * distY);

            if (state != GState.ATTACK) {
                if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
                else direction = distY > 0 ? "down" : "up";
            }

            if (state == GState.ATTACK) {
                actedOnEnemy = true; // lagi mukul, jangan diinterupsi
            } else if (dist <= ATTACK_RANGE && attackCooldown == 0) {
                state = GState.ATTACK;
                atkFrame = 0; atkTimer = 0;
                damageDealtThisSwing = false;
                actedOnEnemy = true;
            } else if (dist <= DETECTION_RANGE && dist > ATTACK_RANGE) {
                state = GState.WALK;
                moveToward(distX, distY);
                actedOnEnemy = true;
            } else if (dist <= ATTACK_RANGE) {
                // udah deket tapi masih cooldown: diem aja nungguin
                state = GState.IDLE;
                actedOnEnemy = true;
            }
        }

        // 2. Prioritas kedua: ikuti player
        if (!actedOnEnemy && state != GState.ATTACK) {
            int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
            int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
            int myCX = x + solidArea.x + solidArea.width  / 2;
            int myCY = y + solidArea.y + solidArea.height / 2;
            int distX = plCX - myCX, distY = plCY - myCY;
            double dist = Math.sqrt(distX * distX + distY * distY);

            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";

            if (dist > FOLLOW_STOP_RANGE) {
                state = GState.WALK;
                moveToward(distX, distY);
            } else {
                state = GState.IDLE;
            }
        }

        updateAnimation(target);
    }

    // ────────────────────────────────────────────────────────────────────
    //  GERAK + COLLISION (pola sama kayak Golemite liar, per sumbu)
    // ────────────────────────────────────────────────────────────────────
    private void moveToward(int distX, int distY) {
        int xStep = Integer.compare(distX, 0);
        int yStep = Integer.compare(distY, 0);
        double moveSpeed = (xStep != 0 && yStep != 0) ? speed * 0.707 : speed;
        int dx = (int) Math.round(xStep * moveSpeed);
        int dy = (int) Math.round(yStep * moveSpeed);

        String faceDir = direction;

        if (dx != 0) {
            direction = dx > 0 ? "right" : "left";
            collisionOn = false;
            checkWorldCollision(dx, 0);
            if (!collisionOn) gp.colision.checkEntity(this, gp.player, dx, 0);
            if (!collisionOn) checkEnemyCollision(dx, 0);
            if (!collisionOn) x += dx;
        }

        if (dy != 0) {
            direction = dy > 0 ? "down" : "up";
            collisionOn = false;
            checkWorldCollision(0, dy);
            if (!collisionOn) gp.colision.checkEntity(this, gp.player, 0, dy);
            if (!collisionOn) checkEnemyCollision(0, dy);
            if (!collisionOn) y += dy;
        }

        direction = faceDir; // sprite tetap ngadep ke target
    }

    private void checkWorldCollision(int dx, int dy) {
        if (gp.gameState == GamePanel.GameState.DOMAIN) {
            Rectangle next = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy,
                                           solidArea.width, solidArea.height);
            if (gp.domainManager != null && gp.domainManager.checkCollision(next)) {
                collisionOn = true;
            }
        } else {
            gp.colision.checkTile(this);
            gp.colision.checkObject(this, dx, dy);
        }
    }

    /** Biar nggak numpuk/tembus sama musuh. */
    private void checkEnemyCollision(int dx, int dy) {
        if (gp.domainManager != null) {
            if (gp.domainManager.getSkeletons() != null) {
                for (Skeleton sk : gp.domainManager.getSkeletons()) {
                    if (sk == null || !sk.alive) continue;
                    gp.colision.checkEntity(this, sk, dx, dy);
                    if (collisionOn) return;
                }
            }
            if (gp.domainManager.getGolemites() != null) {
                for (Golemite gl : gp.domainManager.getGolemites()) {
                    if (!isValidTarget(gl)) continue;
                    gp.colision.checkEntity(this, gl, dx, dy);
                    if (collisionOn) return;
                }
            }
            SkeletonKing king = gp.domainManager.getBossSkeleton();
            if (king != null && king.alive) {
                gp.colision.checkEntity(this, king, dx, dy);
                if (collisionOn) return;
            }
        }
        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (!isValidTarget(m)) continue;
                if (!(m instanceof Golem) && !(m instanceof Golemite) && !(m instanceof monster)) continue;
                gp.colision.checkEntity(this, m, dx, dy);
                if (collisionOn) return;
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  TARGET
    // ────────────────────────────────────────────────────────────────────
    private boolean isValidTarget(Entity e) {
        if (e == null || e == this || e.isDead || e.life <= 0) return false;
        if (e instanceof Golemite && !((Golemite) e).isAlive()) return false;
        return true;
    }

    private Entity findThreatNearPlayer() {
        int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;

        Entity nearest = null;
        double minDist = Double.MAX_VALUE;

        if (gp.domainManager != null) {
            if (gp.domainManager.getSkeletons() != null) {
                for (Skeleton sk : gp.domainManager.getSkeletons()) {
                    if (sk == null || !sk.alive) continue;
                    double d = distToPoint(sk, plCX, plCY);
                    if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = sk; }
                }
            }
            if (gp.domainManager.getGolemites() != null) {
                for (Golemite gl : gp.domainManager.getGolemites()) {
                    if (!isValidTarget(gl)) continue;
                    double d = distToPoint(gl, plCX, plCY);
                    if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = gl; }
                }
            }
            SkeletonKing king = gp.domainManager.getBossSkeleton();
            if (king != null && king.alive) {
                double d = distToPoint(king, plCX, plCY);
                if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = king; }
            }
        }

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (!isValidTarget(m)) continue;
                if (!(m instanceof Golem) && !(m instanceof Golemite) && !(m instanceof monster)) continue;
                double d = distToPoint(m, plCX, plCY);
                if (d <= THREAT_RADIUS_TO_PLAYER && d < minDist) { minDist = d; nearest = m; }
            }
        }

        return nearest;
    }

    private double distToPoint(Entity e, int px, int py) {
        int cx = e.x + e.solidArea.x + e.solidArea.width  / 2;
        int cy = e.y + e.solidArea.y + e.solidArea.height / 2;
        int dx = cx - px, dy = cy - py;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ────────────────────────────────────────────────────────────────────
    //  ANIMASI + COMBAT
    // ────────────────────────────────────────────────────────────────────
    private void updateAnimation(Entity target) {
        switch (state) {
            case WALK:
                walkTimer++;
                if (walkTimer >= WALK_FRAME_DURATION) {
                    walkTimer = 0;
                    walkFrame = (walkFrame + 1) % WALK_FRAMES;
                }
                break;

            case ATTACK:
                atkTimer++;
                if (atkTimer >= ATTACK_FRAME_DURATION) {
                    atkTimer = 0;
                    atkFrame++;

                    if (atkFrame == HIT_FRAME && !damageDealtThisSwing) {
                        if (target != null) hitTarget(target);
                        damageDealtThisSwing = true;
                    }
                    if (atkFrame >= ATTACK_FRAMES) {
                        atkFrame = 0;
                        attackCooldown = ATTACK_COOLDOWN_MAX;
                        state = GState.IDLE;
                    }
                }
                break;

            default: // IDLE: diem di walk frame pertama
                walkFrame = 0;
                walkTimer = 0;
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

    private void dealDamage(Entity target, int dmg) {
        if (target instanceof Skeleton) {
            ((Skeleton) target).takeDamage(dmg);
            ((Skeleton) target).knockbackFrom(this);
        } else if (target instanceof Golem) {
            ((Golem) target).takeDamage(dmg);
        } else if (target instanceof Golemite) {
            ((Golemite) target).takeDamage(dmg);
        } else if (target instanceof monster) {
            ((monster) target).takeDamage(dmg, this);
        } else if (target instanceof SkeletonKing) {
            ((SkeletonKing) target).takeDamage(dmg);
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

    // ────────────────────────────────────────────────────────────────────
    //  DAMAGE / DEATH
    // ────────────────────────────────────────────────────────────────────
    @Override
    public void takeDamage(int dmg) {
        if (!alive || state == GState.DEATH) return;
        life -= dmg;
        isHit = true;
        hitFlashTimer = 8;
    }

    private void die() {
        alive = false;
        state = GState.DEATH;
        deathTimer = 0;
        deathDone = false;
        solidArea.x = -9999;
        solidArea.y = -9999;
        if (ownerTotem != null) ownerTotem.releasePawn();
    }

    private void updateDeath() {
        if (deathDone) return;
        deathTimer++;
        if (deathTimer >= DEATH_FADE_FRAMES) deathDone = true;
    }

    // ────────────────────────────────────────────────────────────────────
    //  DRAW
    // ────────────────────────────────────────────────────────────────────
    private int dirIndex() {
        switch (direction) {
            case "up":    return 1;
            case "left":  return 2;
            case "right": return 3;
            default:      return 0;
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!alive && state != GState.DEATH) return;
        if (state == GState.DEATH && deathDone) return;

        int drawSize = gp.tileSize * DRAW_SIZE_MULT;
        int offset   = (drawSize - gp.tileSize) / 2;
        int sx = x - gp.player.x + gp.player.screenX - offset;
        int sy = y - gp.player.y + gp.player.screenY - offset;

        if (sx + drawSize < gp.viewMinX() - drawSize || sx > gp.viewMaxX() + drawSize ||
            sy + drawSize < gp.viewMinY() - drawSize || sy > gp.viewMaxY() + drawSize) return;

        int spriteY = sy + (int) (drawSize * SPRITE_LOWER);

        float fade = 1f;
        if (state == GState.DEATH) {
            fade = Math.max(0f, 1f - (float) deathTimer / DEATH_FADE_FRAMES);
        } else if (spawnFade < SPAWN_FADE_FRAMES) {
            fade = (float) spawnFade / SPAWN_FADE_FRAMES;
        }

        Composite old = g2.getComposite();

        // Bayangan
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.40f * fade));
        g2.setColor(Color.BLACK);
        int shadowW = (int) (drawSize * 0.55);
        int shadowH = shadowW / 4;
        g2.fillOval(sx + drawSize / 2 - shadowW / 2,
                    sy + drawSize - shadowH - drawSize / 7,
                    shadowW, shadowH);

        // Sprite
        int d = dirIndex();
        BufferedImage img = (state == GState.ATTACK)
                ? Golemite.getPunchFrame(d, atkFrame)
                : Golemite.getWalkFrame(d, state == GState.WALK ? walkFrame : 0);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        if (img != null) {
            g2.drawImage(img, sx, spriteY, drawSize, drawSize, null);
        }
        g2.setComposite(old);

        // HP bar ungu (penanda pion tamed, sama kayak GolemMinion)
        if (state != GState.DEATH && life < maxLife) {
            int barW = 40, barH = 5;
            int fill = (int) ((double) life / maxLife * barW);
            int barX = sx + drawSize / 2 - barW / 2;
            int barY = sy + 12;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(barX, barY, barW, barH);
            g2.setColor(new Color(140, 80, 255));
            g2.fillRect(barX, barY, fill, barH);
        }
    }
}
