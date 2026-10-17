package Entitas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import Main.GamePanel;

/**
 * Orc — monster baru dengan 3 animasi:
 *   IDLE   4 frame
 *   WALK   6 frame
 *   ATTACK 7 frame  ← damage HANYA masuk di frame ke-5 (impact frame)
 *
 * Sistem animasi:
 *   - Tiap state punya frameIndex & frameTimer sendiri
 *   - ATTACK tidak bisa di-interrupt sampai animasi selesai
 *   - Damage dikirim tepat di frame ke-5 (index 4), tidak di awal
 */
public class Orc extends Entity {

    // =====================
    // SPRITE
    // =====================
    private OrcSprite sprite;

    // =====================
    // STATE
    // =====================
    public enum OrcState {
        IDLE, WANDER, CHASE, ATTACKING, HIT, DEAD
    }

    OrcState state = OrcState.IDLE;

    // =====================
    // STATS
    // =====================
    public java.awt.Rectangle wanderArea;

    private final int AGGRO_RADIUS   = 220;
    private final int ATTACK_RANGE   = 70;
    private final int ATTACK_DAMAGE  = 18;   // damage sekali pukul
    private final int WANDER_DELAY   = 5;    // frame skip saat wander (lebih lambat)

    int spriteWidth  = gp.tileSize * 3;   // lebih gede biar muat art
    int spriteHeight = gp.tileSize * 3;

    // =====================
    // ANIMASI WALK / IDLE
    // =====================
    private int walkFrame   = 0;   // 0–5
    private int idleFrame   = 0;   // 0–3
    private int frameTimer  = 0;
    private final int WALK_FRAME_SPEED = 8;   // tick per frame walk
    private final int IDLE_FRAME_SPEED = 12;  // tick per frame idle

    // =====================
    // ANIMASI ATTACK
    // =====================
    private int    attackFrame       = 0;    // 0–6
    private int    attackFrameTimer  = 0;
    private final int ATTACK_FRAME_SPEED = 6;  // tick per frame attack
    private final int ATTACK_TOTAL_FRAMES = 7;
    private final int DAMAGE_FRAME   = 4;    // frame ke-5 (index 4) = impact

    private boolean damageDealt      = false; // guard: damage hanya 1x per swing
    private boolean attackAnimDone   = false; // true kalau animasi sudah selesai

    // =====================
    // COOLDOWN ATTACK
    // =====================
    private int attackCooldown    = 0;
    private final int ATTACK_CD_MAX = 120;  // 3 detik @ 40fps

    // =====================
    // WANDER
    // =====================
    private int wanderMoveDelay = 0;
    private Random rand = new Random();

    // =====================
    // DEATH ANIMATION
    // =====================
    private int deadTimer = 0;
    private final int DEAD_DISPLAY_TICKS = 80; // berapa lama sprite mati ditampilkan

    // =====================
    // KONSTRUKTOR
    // =====================
    public Orc(GamePanel gp) {
        super(gp);

        name        = "orc";
        showName    = true;
        maxLife     = 120;
        life        = maxLife;
        speed       = 2;
        attackRange = ATTACK_RANGE;
        getExpValue = 45;

        sprite = new OrcSprite(gp);

        direction = "down";

        // Hitbox (sesuaikan dengan sprite Orc-mu)
        solidArea        = new java.awt.Rectangle(20, 32, 24, 20);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
    }

    // =====================
    // HELPER: dalam area
    // =====================
    private boolean insideWanderArea(int nx, int ny) {
        int cx = nx + spriteWidth  / 2;
        int cy = ny + spriteHeight / 2;
        return wanderArea != null &&
               cx >= wanderArea.x &&
               cx <= wanderArea.x + wanderArea.width &&
               cy >= wanderArea.y &&
               cy <= wanderArea.y + wanderArea.height;
    }

    // =====================
    // JARAK KE TARGET
    // =====================
    public double distanceTo(Entity target) {
        int ax = x + solidArea.x + solidArea.width  / 2;
        int ay = y + solidArea.y + solidArea.height / 2;
        int bx = target.x + target.solidArea.x + target.solidArea.width  / 2;
        int by = target.y + target.solidArea.y + target.solidArea.height / 2;
        return Math.hypot(ax - bx, ay - by);
    }

    // =====================
    // DROP LOOT
    // =====================
    private void drop() {
        int chance = rand.nextInt(100);
        if (chance < 70) {
            gp.drops.add(new Item.DropItem(x, y, Item.ItemManager.get().SLIME_GEL, 1));
        }
        // Tambahkan item Orc khusus jika sudah dibuat di ItemManager
    }

    // =====================
    // WANDER
    // =====================
    private void wander() {
        actionLockCounter++;
        if (actionLockCounter > 120) {
            int r = rand.nextInt(4);
            direction = (r == 0) ? "up" : (r == 1) ? "down" : (r == 2) ? "left" : "right";
            actionLockCounter = 0;
        }

        wanderMoveDelay++;
        if (wanderMoveDelay < WANDER_DELAY) return;
        wanderMoveDelay = 0;

        int nx = x, ny = y;
        if (direction.equals("left"))  nx -= speed;
        if (direction.equals("right")) nx += speed;
        if (direction.equals("up"))    ny -= speed;
        if (direction.equals("down"))  ny += speed;

        if (insideWanderArea(nx, ny)) {
            x = nx;
            y = ny;
        } else {
            int r = rand.nextInt(4);
            direction = (r == 0) ? "up" : (r == 1) ? "down" : (r == 2) ? "left" : "right";
            actionLockCounter = 0;
        }

        tickWalkAnim();
    }

    // =====================
    // CHASE PLAYER
    // =====================
    private void chasePlayer() {
        int dx = gp.player.x - x;
        int dy = gp.player.y - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = (dx < 0) ? "left" : "right";
        } else {
            direction = (dy < 0) ? "up" : "down";
        }

        int nx = x, ny = y;
        if (direction.equals("left"))  nx -= speed;
        if (direction.equals("right")) nx += speed;
        if (direction.equals("up"))    ny -= speed;
        if (direction.equals("down"))  ny += speed;

        if (insideWanderArea(nx, ny)) {
            x = nx;
            y = ny;
        } else {
            state = OrcState.WANDER;
            returnToCenter();
        }

        tickWalkAnim();
    }

    private void returnToCenter() {
        if (wanderArea == null) return;
        int cx = wanderArea.x + wanderArea.width  / 2;
        int cy = wanderArea.y + wanderArea.height / 2;
        int dx = cx - x, dy = cy - y;
        direction = (Math.abs(dx) > Math.abs(dy))
                ? (dx < 0 ? "left" : "right")
                : (dy < 0 ? "up"   : "down");
    }

    // =====================
    // ANIMASI WALK TICK
    // =====================
    private void tickWalkAnim() {
        frameTimer++;
        if (frameTimer >= WALK_FRAME_SPEED) {
            frameTimer = 0;
            walkFrame = (walkFrame + 1) % 6;
        }
    }

    // =====================
    // ANIMASI IDLE TICK
    // =====================
    private void tickIdleAnim() {
        frameTimer++;
        if (frameTimer >= IDLE_FRAME_SPEED) {
            frameTimer = 0;
            idleFrame = (idleFrame + 1) % 4;
        }
    }

    // =====================
    // START ATTACK (inisiasi)
    // =====================
    private void startAttack() {
        if (attackCooldown > 0) {
            // Masih cooldown → kembali chase
            state = OrcState.CHASE;
            return;
        }
        // Reset semua variabel animasi attack
        attackFrame      = 0;
        attackFrameTimer = 0;
        damageDealt      = false;
        attackAnimDone   = false;
    }

    // =====================
    // UPDATE ANIMASI ATTACK  ← INTI SISTEM
    //
    // Dipanggil setiap tick selama state == ATTACKING.
    // Damage baru dikirim ketika attackFrame == DAMAGE_FRAME.
    // State berpindah ke CHASE setelah frame terakhir selesai.
    // =====================
    private void updateAttackAnim() {
        attackFrameTimer++;

        if (attackFrameTimer >= ATTACK_FRAME_SPEED) {
            attackFrameTimer = 0;

            // ── DAMAGE FRAME ──────────────────────────────
            // Frame ke-5 (index 4): baru kirim damage ke player
            if (attackFrame == DAMAGE_FRAME && !damageDealt) {
                double dist = distanceTo(gp.player);
                if (dist <= ATTACK_RANGE + 10) {          // sedikit toleransi
                    gp.player.takeDamage(ATTACK_DAMAGE);
                }
                damageDealt    = true;
                attackCooldown = ATTACK_CD_MAX;
            }

            attackFrame++;

            // ── ANIMASI SELESAI ───────────────────────────
            if (attackFrame >= ATTACK_TOTAL_FRAMES) {
                attackFrame    = 0;
                attackAnimDone = true;
                state          = OrcState.CHASE;   // kembali chase setelah selesai
            }
        }
    }

    // =====================
    // TERIMA DAMAGE
    // =====================

    // Dipanggil dari player.punch / serangan player
    @Override
    public void takeDamage(int dmg, player attacker) {
        if (state == OrcState.DEAD) return;
        life -= dmg;
        regenTimer = 0;

        // Jika sedang attack, biarkan animasinya selesai dulu
        if (state != OrcState.ATTACKING) {
            state = OrcState.HIT;
        }

        if (life <= 0) {
            life = 0;
            die();
        }
    }

    // Overload tanpa attacker (dipanggil internal / legacy)
    @Override
    public void takeDamage(int dmg) {
        takeDamage(dmg, (player) null);
    }

    // =====================
    // MATI
    // =====================
    public void die() {
        if (state == OrcState.DEAD) return;
        state  = OrcState.DEAD;
        isDead = true;
        drop();
        gp.player.exp += getExpValue;
        gp.player.checkLevelUp();
        gp.player.giveCoinReward(this); // koin pasti drop
        gp.player.givePotionReward(this); // small 50% / medium 20%
        Main.Log.d("Orc mati!");
    }

    // =====================
    // UPDATE UTAMA (AI)
    // =====================
    @Override
    public void update() {

        // ── DEAD STATE ──────────────────────────────────
        if (state == OrcState.DEAD) {
            deadTimer++;
            // setelah sekian tick, tandai untuk dihapus dari array (opsional)
            return;
        }

        // ── COOLDOWN ────────────────────────────────────
        if (attackCooldown > 0) attackCooldown--;

        // ── JIKA SEDANG MENYERANG → FREEZE TOTAL ────────
        // Tidak ada gerakan, tidak ada super.update() (supaya
        // animasi walk/idle di Entity tidak ikut jalan & ganggu).
        // Satu-satunya yang update adalah frame attack.
        if (state == OrcState.ATTACKING) {
            updateAttackAnim();
            return;   // ← BERHENTI TOTAL, tidak gerak, tidak super.update()
        }

        // ── HP REGEN ────────────────────────────────────
        if (life < maxLife) {
            regenTimer++;
            if (regenTimer >= REGEN_DELAY) {
                life = Math.min(life + REGEN_RATE, maxLife);
                regenTimer = 0;
            }
        }

        double dist = distanceTo(gp.player);

        // ── STATE MACHINE ───────────────────────────────
        switch (state) {

            case IDLE:
                tickIdleAnim();
                state = OrcState.WANDER;
                break;

            case WANDER:
                wander();
                if (dist < AGGRO_RADIUS) state = OrcState.CHASE;
                break;

            case CHASE:
                chasePlayer();
                if (dist < ATTACK_RANGE) {
                    state = OrcState.ATTACKING;
                    startAttack();
                }
                if (dist > AGGRO_RADIUS) state = OrcState.WANDER;
                break;

            case HIT:
                // sekilas kena pukul → langsung chase lagi
                state = OrcState.CHASE;
                break;

            case DEAD:
                break;

            default:
                break;
        }

        super.update();
    }

    // =====================
    // HP BAR
    // =====================
    private void drawHPBar(Graphics2D g2, int sx, int sy) {
        if (life >= maxLife) return;
        int bw = 48, bh = 5;
        int hw = (int) ((double) life / maxLife * bw);
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(sx, sy - 12, bw, bh);
        g2.setColor(Color.RED);
        g2.fillRect(sx, sy - 12, hw, bh);
    }

    // =====================
    // BAYANGAN
    // =====================
    private void drawShadow(Graphics2D g2, int sx, int sy) {
        int sw = spriteWidth - 30, sh = spriteHeight / 5;
        int ox = sx + (spriteWidth - sw) / 2;
        int oy = sy + spriteHeight - sh + 4;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2.setColor(Color.BLACK);
        g2.fillOval(ox, oy, sw, sh);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // =====================
    // DRAW
    // =====================
    @Override
    public void draw(Graphics2D g2) {

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        BufferedImage img = getCurrentFrame();

        drawShadow(g2, screenX, screenY);

        if (img != null) {
            // Saat kena hit: efek merah kilat
            if (state == OrcState.HIT) {
                java.awt.AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
                g2.setComposite(ac);
            }
            g2.drawImage(img, screenX, screenY, spriteWidth, spriteHeight, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        drawHPBar(g2, screenX, screenY);

        // Nama
        if (showName) {
            g2.setFont(new Font("Dialog", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(name, screenX + 4, screenY - 15);
        }
    }

    // =====================
    // PILIH FRAME SESUAI STATE
    // =====================
    private BufferedImage getCurrentFrame() {

        // ── DEAD ──
        if (state == OrcState.DEAD) {
            return sprite.dead[0];
        }

        // ── ATTACK ──
        if (state == OrcState.ATTACKING) {
            // clamp supaya tidak out of bounds
            int f = Math.min(attackFrame, sprite.attack.length - 1);
            return sprite.attack[f];
        }

        // ── IDLE (tidak bergerak / state IDLE) ──
        if (state == OrcState.IDLE) {
            int f = Math.min(idleFrame, sprite.idle.length - 1);
            return sprite.idle[f];
        }

        // ── WALK (WANDER / CHASE / HIT) ──
        int f = Math.min(walkFrame, 5);   // 6 frame: 0–5
        switch (direction) {
            case "up":    return sprite.walkUp[f];
            case "left":  return sprite.walkLeft[f];
            case "right": return sprite.walkRight[f];
            default:      return sprite.walkDown[f];
        }
    }
}