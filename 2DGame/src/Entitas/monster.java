package Entitas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.awt.Rectangle;

import Main.GamePanel;
import Tamed.GolemMinion;

public class monster extends Entity {

    public int spawnX;
    public int spawnY;

    private monsterSprite sprite;

    // =====================
    // MONSTER STATE SYSTEM
    // =====================

    public enum MonsterState {
        IDLE,
        WANDER,
        CHASE,
        ATTACK,
        HIT,
        DEAD
    }

    MonsterState state = MonsterState.IDLE;

    // =====================
    // MONSTER STATS
    // =====================
    public Rectangle wanderArea;
    int aggroRadius = 180;
    int attackDamage = 10;
    int wanderMoveDelay = 0;

    int spriteWidth = Math.round(gp.tileSize * 1.5f);
    int spriteHeight = Math.round(gp.tileSize * 1.5f);
    int spriteOffsetY = 0;

    // ===== ATTACK SYSTEM =====
    private int attackCooldown = 0;
    private final int ATTACK_COOLDOWN_MAX = 180;

    // Kunci posisi ketika slime sudah berada dalam jarak serang.
    // Slime tidak akan mengejar lagi sampai target keluar dari jarak serang.
    private boolean holdingAttackPosition = false;

    // ===== ANIMASI (1 tick = 1/40 detik) =====
    private final int WALK_FRAME_TICKS = 6;
    private final int ATTACK_FRAME_TICKS = 4;
    private final int ATTACK_HIT_FRAME = 7;
    private final int HIT_FRAME_TICKS = 3;
    private final int DEATH_FRAME_TICKS = 5;
    private final int IDLE_FRAME_TICKS = 6;

    private int idleFrame = 0;
    private int idleTick = 0;

    // Memberi waktu agar slime bisa bergerak menjauh dari objek sebelum
    // kembali mengejar pemain.
    private int recoveryWanderTicks = 0;

    // Arah gerak wander mendukung 8 arah, termasuk diagonal.
    private int wanderDX = 0;
    private int wanderDY = 1;

    // Respawn slime setelah animasi mati dan jeda yang ditentukan.
    private final int RESPAWN_DELAY_TICKS = 300;
    private int respawnTick = 0;
    private boolean spawnPositionInitialized = false;

    private int walkFrame = 0, walkTick = 0;
    private int attackFrame = 0, attackTick = 0;
    private int hitFrame = 0, hitTick = 0;
    private int deathFrame = 0, deathTick = 0;
    private boolean deathDone = false;

    Random rand = new Random();

    public monster(GamePanel gp) {
        super(gp);
        spawnX = x;
        spawnY = y;

        name = "slime";
        showName = true;

        maxLife = 60;
        life = maxLife;

        speed = 1;
        attackRange = 60;

        sprite = new monsterSprite(gp);

        // Hitbox disesuaikan dengan skala sprite.
        float scaleX = spriteWidth / (float) monsterSprite.BASE_SIZE;
        float scaleY = spriteHeight / (float) monsterSprite.BASE_SIZE;

        solidArea = new Rectangle(
            Math.round(6 * scaleX),
            Math.round(16 * scaleY),
            Math.round(20 * scaleX),
            Math.round(12 * scaleY)
        );

        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";
        getExpValue = 20;
    }

    private boolean insideWanderArea(int newX, int newY) {
        if (wanderArea == null) {
            return true;
        }

        // Pakai center sprite sebagai titik acuan.
        int centerX = newX + (spriteWidth / 2);
        int centerY = newY + (spriteHeight / 2);

        return centerX >= wanderArea.x
            && centerX <= wanderArea.x + wanderArea.width
            && centerY >= wanderArea.y
            && centerY <= wanderArea.y + wanderArea.height;
    }

    private boolean moveIfClear(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return true;
        }

        collisionOn = false;

        gp.colision.checkTile(this);
        gp.colision.checkObject(this, dx, dy);
        gp.colision.checkEntity(this, gp.player, dx, dy);

        // Periksa monster lain.
        if (!collisionOn && gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m == this || m.isDead) {
                    continue;
                }

                if (!(m instanceof Entitas.Golem) && !(m instanceof monster)) {
                    continue;
                }

                gp.colision.checkEntity(this, m, dx, dy);
                if (collisionOn) {
                    break;
                }
            }
        }

        // Periksa GolemMinion yang masih hidup.
        if (!collisionOn
            && gp.deathTotemManager != null
            && gp.deathTotemManager.getMinions() != null) {

            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) {
                    continue;
                }

                if (!((GolemMinion) m).isAlive()) {
                    continue;
                }

                gp.colision.checkEntity(this, m, dx, dy);
                if (collisionOn) {
                    break;
                }
            }
        }

        // Gerakan hanya dilakukan jika tidak ada tabrakan.
        if (!collisionOn) {
            x += dx;
            y += dy;
            return true;
        }

        return false;
    }

    // =====================
    // IDLE ANIMATION
    // =====================

    private void enterIdle() {
        state = MonsterState.IDLE;
        idleFrame = 0;
        idleTick = 0;
        wanderMoveDelay = 0;
        actionLockCounter = 0;
        recoveryWanderTicks = 0;
        holdingAttackPosition = false;
    }

    private void randomizeDirection() {
        // Delapan arah gerakan: empat kardinal dan empat diagonal.
        int[][] directions = {
            { 0, -1}, // up
            { 0,  1}, // down
            {-1,  0}, // left
            { 1,  0}, // right
            {-1, -1}, // up-left
            { 1, -1}, // up-right
            {-1,  1}, // down-left
            { 1,  1}  // down-right
        };

        int[] selected = directions[rand.nextInt(directions.length)];
        wanderDX = selected[0];
        wanderDY = selected[1];
        setFacingFromVector(wanderDX, wanderDY);
    }

    // Sprite hanya memiliki animasi 4 arah; gerakan tetap bisa diagonal.
    private void setFacingFromVector(int dx, int dy) {
        if (dx == 0 && dy == 0) return;

        if (dx != 0 && (dy == 0 || Math.abs(dx) >= Math.abs(dy))) {
            direction = dx < 0 ? "left" : "right";
        } else {
            direction = dy < 0 ? "up" : "down";
        }
    }

    // Menghadap target tanpa mengubah posisi slime.
    private void faceTarget(Entity target) {
        if (target == null) return;

        int dx =
            (target.x + target.solidArea.x + target.solidArea.width / 2)
            - (x + solidArea.x + solidArea.width / 2);
        int dy =
            (target.y + target.solidArea.y + target.solidArea.height / 2)
            - (y + solidArea.y + solidArea.height / 2);

        setFacingFromVector(Integer.signum(dx), Integer.signum(dy));
    }

    // Jika langkah diagonal terhalang, coba menyusuri rintangan lewat salah
    // satu sumbu supaya slime tidak mudah stuck.
    private boolean moveWithSliding(int dx, int dy) {
        if (dx != 0 && dy != 0) {
            if (moveIfClear(dx, dy)) {
                return true;
            }

            boolean movedX = moveIfClear(dx, 0);
            boolean movedY = moveIfClear(0, dy);
            return movedX || movedY;
        }

        return moveIfClear(dx, dy);
    }

    private void updateIdle() {
        idleTick++;
        if (idleTick < IDLE_FRAME_TICKS) {
            return;
        }

        idleTick = 0;
        idleFrame++;

        // Seluruh frame idle sudah dimainkan.
        if (idleFrame >= monsterSprite.IDLE_FRAMES) {
            idleFrame = 0;
            randomizeDirection();
            state = MonsterState.WANDER;
            recoveryWanderTicks = 30;
        }
    }

    // =====================
    // DISTANCE CHECK
    // =====================

    public double distanceTo(Entity target) {
        if (target == null || target.solidArea == null || solidArea == null) {
            return Double.MAX_VALUE;
        }

        int thisCenterX = x + solidArea.x + solidArea.width / 2;
        int thisCenterY = y + solidArea.y + solidArea.height / 2;
        int targetCenterX = target.x + target.solidArea.x + target.solidArea.width / 2;
        int targetCenterY = target.y + target.solidArea.y + target.solidArea.height / 2;

        int dx = thisCenterX - targetCenterX;
        int dy = thisCenterY - targetCenterY;

        return Math.sqrt(dx * dx + dy * dy);
    }


    // =====================
    // CHASE PLAYER
    // =====================

    public void chaseTarget(Entity target) {
        if (target == null || holdingAttackPosition) return;

        int dx =
            (target.x + target.solidArea.x + target.solidArea.width / 2)
            - (x + solidArea.x + solidArea.width / 2);
        int dy =
            (target.y + target.solidArea.y + target.solidArea.height / 2)
            - (y + solidArea.y + solidArea.height / 2);

        int stepX = (int) Math.round(Integer.signum(dx) * speed);
        int stepY = (int) Math.round(Integer.signum(dy) * speed);
        setFacingFromVector(Integer.signum(dx), Integer.signum(dy));

        int newX = x + stepX;
        int newY = y + stepY;

        if (insideWanderArea(newX, newY)) {
            // Jika terhalang, jangan paksa slime masuk ke state IDLE/WANDER.
            // Itu mencegahnya berjalan acak tepat setelah fase menyerang.
            moveWithSliding(stepX, stepY);
        } else {
            // Target di luar area jelajah: arahkan slime kembali ke tengah.
            state = MonsterState.WANDER;
            holdingAttackPosition = false;

            if (wanderArea != null) {
                int centerX = wanderArea.x + wanderArea.width / 2;
                int centerY = wanderArea.y + wanderArea.height / 2;

                wanderDX = Integer.signum(centerX - x);
                wanderDY = Integer.signum(centerY - y);

                if (wanderDX == 0 && wanderDY == 0) {
                    randomizeDirection();
                } else {
                    setFacingFromVector(wanderDX, wanderDY);
                }
            } else {
                randomizeDirection();
            }

            actionLockCounter = 0;
            wanderMoveDelay = 0;
        }
    }

    // =====================
    // RANDOM MOVEMENT
    // =====================

    public void wander() {
        actionLockCounter++;
        if (actionLockCounter > 120) {
            randomizeDirection();
            actionLockCounter = 0;
        }

        wanderMoveDelay++;
        if (wanderMoveDelay < 4) return;
        wanderMoveDelay = 0;

        int stepX = (int) Math.round(wanderDX * speed);
        int stepY = (int) Math.round(wanderDY * speed);
        int newX = x + stepX;
        int newY = y + stepY;

        if (insideWanderArea(newX, newY)) {
            if (!moveWithSliding(stepX, stepY)) {
                enterIdle();
            }
        } else {
            enterIdle();
        }
    }

    // =====================
    // ATTACK
    // =====================

    private void startAttack() {
        state = MonsterState.ATTACK;
        holdingAttackPosition = true;
        attackFrame = 0;
        attackTick = 0;
        attackCooldown = ATTACK_COOLDOWN_MAX;
    }

    private void updateAttack() {
        // Tidak ada pemanggilan chaseTarget() ketika state ATTACK,
        // sehingga posisi slime terkunci sepanjang animasi serangan.
        attackTick++;
        if (attackTick < ATTACK_FRAME_TICKS) return;

        attackTick = 0;
        attackFrame++;

        if (attackFrame == ATTACK_HIT_FRAME) {
            dealAttackDamage();
        }

        if (attackFrame >= monsterSprite.ATTACK_FRAMES) {
            attackFrame = 0;
            idleFrame = 0;
            idleTick = 0;
            state = MonsterState.CHASE;
            // holdingAttackPosition sengaja tidak di-reset di sini.
            // Slime akan tetap diam selama target masih di jarak serang.
        }
    }

    private void dealAttackDamage() {
        Entity target = findNearestThreat();
        if (target == null || distanceTo(target) > attackRange + 30) return;

        if (target instanceof player) {
            ((player) target).takeDamageFrom(attackDamage, this);
        } else if (target instanceof GolemMinion) {
            ((GolemMinion) target).takeDamage(attackDamage);
        }
    }

    private void animateWalk() {
        walkTick++;
        if (walkTick >= WALK_FRAME_TICKS) {
            walkTick = 0;
            walkFrame = (walkFrame + 1) % monsterSprite.WALK_FRAMES;
        }
    }

    private void updateHit() {
        hitTick++;
        if (hitTick >= HIT_FRAME_TICKS) {
            hitTick = 0;
            hitFrame++;
            if (hitFrame >= monsterSprite.HIT_FRAMES) {
                hitFrame = 0;
                state = MonsterState.CHASE;
            }
        }
    }

    private void updateDeath() {
        if (deathDone) return;

        deathTick++;
        if (deathTick >= DEATH_FRAME_TICKS) {
            deathTick = 0;
            deathFrame++;
            if (deathFrame >= monsterSprite.DEATH_FRAMES) {
                deathFrame = monsterSprite.DEATH_FRAMES - 1;
                deathDone = true;
            }
        }
    }

    /**
     * Tetap true selama animasi mati dan masa tunggu respawn agar
     * GamePanel/TileM tidak menghapus slime sebelum hidup kembali.
     */
    public boolean isDeathAnimPlaying() {
        return state == MonsterState.DEAD
            && (!deathDone || respawnTick < RESPAWN_DELAY_TICKS);
    }

    // Panggil dari kode spawn setelah koordinat slime ditentukan.
    public void setSpawnPosition(int spawnX, int spawnY) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.x = spawnX;
        this.y = spawnY;
        this.spawnPositionInitialized = true;
    }

    private void respawnMonster() {
        x = spawnX;
        y = spawnY;

        life = maxLife;
        isDead = false;
        state = MonsterState.WANDER;

        idleFrame = 0;
        idleTick = 0;
        walkFrame = 0;
        walkTick = 0;
        attackFrame = 0;
        attackTick = 0;
        hitFrame = 0;
        hitTick = 0;
        deathFrame = 0;
        deathTick = 0;
        deathDone = false;

        respawnTick = 0;
        attackCooldown = 0;
        recoveryWanderTicks = 0;
        regenTimer = 0;
        actionLockCounter = 0;
        wanderMoveDelay = 0;
        collisionOn = false;
        holdingAttackPosition = false;

        randomizeDirection();
    }

    /** Target slime = entity terdekat antara player dan GolemMinion aktif. */
    private Entity findNearestThreat() {
        if (gp.player == null) return null;

        Entity nearest = gp.player;
        double minDist = distanceTo(gp.player);

        if (gp.deathTotemManager != null
            && gp.deathTotemManager.getMinions() != null) {

            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) continue;
                if (!((GolemMinion) m).isAlive()) continue;

                double d = distanceTo(m);
                if (d < minDist) {
                    minDist = d;
                    nearest = m;
                }
            }
        }

        return nearest;
    }

    // =====================
    // DAMAGE SYSTEM
    // =====================

    public void takeDamage(int dmg, Entity attacker) {
        if (state == MonsterState.DEAD) return;

        life -= dmg;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);
        regenTimer = 0;

        if (life <= 0) {
            life = 0;
            die();
            return;
        }

        // Jika sedang menyerang, biarkan animasi serangan selesai.
        if (state != MonsterState.ATTACK) {
            state = MonsterState.HIT;
            hitFrame = 0;
            hitTick = 0;
        }
    }

    // Pukulan player dan skill masuk lewat Entity.takeDamage(...).
    @Override
    public void takeDamage(int dmg) {
        takeDamage(dmg, (Entity) null);
    }

    @Override
    public void takeDamage(int dmg, player attacker) {
        takeDamage(dmg, (Entity) attacker);
    }

    public void die() {
        if (state == MonsterState.DEAD) return;

        state = MonsterState.DEAD;
        holdingAttackPosition = false;
        deathFrame = 0;
        deathTick = 0;
        deathDone = false;
        respawnTick = 0;
        isDead = true;

        gp.player.exp += this.getExpValue;
        gp.player.checkLevelUp();
        gp.player.giveCoinReward(this);
        gp.player.givePotionReward(this);
        Main.Log.d("Slime mati");
    }

    // =====================
    // AI UPDATE
    // =====================

    @Override
    public void update() {
        // Tangkap posisi aktual sekali, karena spawner bisa mengatur x/y
        // setelah constructor monster dipanggil.
        if (!spawnPositionInitialized) {
            spawnX = x;
            spawnY = y;
            spawnPositionInitialized = true;
        }

        if (state == MonsterState.DEAD) {
            if (!deathDone) {
                updateDeath();
            } else {
                respawnTick++;
                if (respawnTick >= RESPAWN_DELAY_TICKS) {
                    respawnMonster();
                }
            }
            return;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (recoveryWanderTicks > 0) {
            recoveryWanderTicks--;
        }

        Entity threat = findNearestThreat();
        double dist = distanceTo(threat);

        // HP REGEN
        if (life < maxLife) {
            regenTimer++;
            if (regenTimer >= REGEN_DELAY) {
                life += REGEN_RATE;
                if (life > maxLife) {
                    life = maxLife;
                }
                regenTimer = 0;
            }
        }

        switch (state) {
            case IDLE:
                updateIdle();
                break;

            case WANDER:
                wander();

                if (state == MonsterState.WANDER) {
                    animateWalk();
                }

                // Jangan langsung mengejar lagi selama masa pemulihan.
                if (state == MonsterState.WANDER
                    && recoveryWanderTicks == 0
                    && dist < aggroRadius) {
                    state = MonsterState.CHASE;
                }
                break;

            case CHASE:
                if (threat == null) {
                    holdingAttackPosition = false;
                    state = MonsterState.WANDER;
                    break;
                }

                // PENTING: cek jarak serang SEBELUM memanggil chaseTarget.
                // Ini mencegah slime maju satu langkah lagi pada setiap tick
                // setelah animasi serangan selesai.
                if (dist <= attackRange) {
                    holdingAttackPosition = true;
                    faceTarget(threat);

                    // Saat menunggu cooldown, tampilkan animasi idle,
                    // bukan animasi jalan di tempat.
                    walkFrame = 0;
                    walkTick = 0;
                    idleTick++;
                    if (idleTick >= IDLE_FRAME_TICKS) {
                        idleTick = 0;
                        idleFrame = (idleFrame + 1) % monsterSprite.IDLE_FRAMES;
                    }

                    if (attackCooldown == 0) {
                        startAttack();
                    }
                    break;
                }

                if (dist > aggroRadius) {
                    holdingAttackPosition = false;
                    state = MonsterState.WANDER;
                    break;
                }

                // Hanya bergerak mendekati target jika target di luar jarak serang.
                holdingAttackPosition = false;
                chaseTarget(threat);
                if (state == MonsterState.CHASE) {
                    animateWalk();
                }
                break;

            case ATTACK:
                updateAttack();
                break;

            case HIT:
                updateHit();
                break;

            case DEAD:
                // Ditangani di awal update().
                break;
        }

        super.update();
    }

    // =====================
    // HP BAR
    // =====================

    public void drawHPBar(Graphics2D g2, int screenX, int screenY) {
        if (life >= maxLife) return;

        int barWidth = 40;
        int barHeight = 5;
        int hpWidth = (int) ((double) life / maxLife * barWidth);

        int barX = screenX + solidArea.x + (solidArea.width - barWidth) / 2;
        int barY = screenY + spriteOffsetY + solidArea.y - 12;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(barX, barY, barWidth, barHeight);
        g2.setColor(Color.RED);
        g2.fillRect(barX, barY, hpWidth, barHeight);
    }

    // Bayangan slime.
    public void drawShadow(Graphics2D g2, int screenX, int screenY) {
        int shadowWidth = Math.round(spriteWidth * 0.62f);
        int shadowHeight = Math.max(5, spriteHeight / 4);
        int shadowX = screenX + (spriteWidth - shadowWidth) / 2;
        int shadowY = screenY + spriteHeight - shadowHeight + 5;

        java.awt.Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);
        g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
        g2.setComposite(oldComposite);
    }

    @Override
    public void draw(Graphics2D g2) {
        // Sembunyikan slime setelah animasi mati selesai sampai respawn.
        if (state == MonsterState.DEAD && deathDone) {
            return;
        }

        BufferedImage image;
        switch (state) {
            case IDLE:
                image = sprite.getIdle(direction, idleFrame);
                break;

            case DEAD:
                image = sprite.getDeath(direction, deathFrame);
                break;

            case HIT:
                image = sprite.getHit(direction, hitFrame);
                break;

            case ATTACK:
                image = sprite.getAttack(direction, attackFrame);
                break;

            case CHASE:
                // Slime yang sudah berada di jarak serang tetap diam dan
                // menampilkan animasi idle saat menunggu cooldown.
                if (holdingAttackPosition) {
                    image = sprite.getIdle(direction, idleFrame);
                } else {
                    image = sprite.getWalk(direction, walkFrame);
                }
                break;

            case WANDER:
            default:
                image = sprite.getWalk(direction, walkFrame);
                break;
        }

        // Hindari crash/artefak gambar jika sprite tidak tersedia.
        if (image == null) return;

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        drawShadow(g2, screenX, screenY + spriteOffsetY);

        // Pusatkan sprite bila ukuran kanvas animasinya berbeda.
        int drawW = Math.round(spriteWidth * image.getWidth() / (float) monsterSprite.BASE_SIZE);
        int drawH = Math.round(spriteHeight * image.getHeight() / (float) monsterSprite.BASE_SIZE);
        int drawX = screenX - (drawW - spriteWidth) / 2;
        int drawY = screenY + spriteOffsetY - (drawH - spriteHeight) / 2;

        g2.drawImage(image, drawX, drawY, drawW, drawH, null);

        if (state != MonsterState.DEAD) {
            drawHPBar(g2, screenX, screenY);
        }
    }
}
