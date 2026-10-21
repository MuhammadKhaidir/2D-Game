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
    
    int spriteWidth = gp.tileSize * 2;
    int spriteHeight = gp.tileSize * 2;

    int spriteOffsetY = 0; // geser kebawah
    
    // ===== ATTACK SYSTEM =====
    private int attackCooldown = 0;
    private final int ATTACK_COOLDOWN_MAX = 180;

    // ===== ANIMASI (1 tick = 1/40 detik) =====
    private final int WALK_FRAME_TICKS   = 6;
    private final int ATTACK_FRAME_TICKS = 4;
    private final int ATTACK_HIT_FRAME   = 7;  // frame ke-8: di sini damage kena
    private final int HIT_FRAME_TICKS    = 3;
    private final int DEATH_FRAME_TICKS  = 5;

    private int walkFrame = 0,   walkTick = 0;
    private int attackFrame = 0, attackTick = 0;
    private int hitFrame = 0,    hitTick = 0;
    private int deathFrame = 0,  deathTick = 0;
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

        // Hitbox = badan slime di dalam sprite (sprite 32x32 digambar 96x96 -> 1 px sprite = 3 px layar).
        // Sebelumnya pakai default Entity (0,0,45,45) = pojok kiri-atas sprite, padahal slime-nya
        // ada di kanan-bawah, makanya hitbox "melayang" jauh di atas gambar.
        int px = spriteWidth / monsterSprite.BASE_SIZE;
        solidArea = new Rectangle(6 * px, 16 * px, 20 * px, 12 * px);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";
        getExpValue = 20;
    }

    private boolean insideWanderArea(int newX, int newY) {
        // pakai center sprite sebagai titik acuan
        int centerX = newX + (spriteWidth / 2);
        int centerY = newY + (spriteHeight / 2);

        return centerX >= wanderArea.x &&
               centerX <= wanderArea.x + wanderArea.width &&
               centerY >= wanderArea.y &&
               centerY <= wanderArea.y + wanderArea.height;
    }
    
    private void moveIfClear(int dx, int dy) {
        if (dx == 0 && dy == 0) return;

        this.collisionOn = false;
        gp.colision.checkTile(this);
        gp.colision.checkObject(this, dx, dy);
        gp.colision.checkEntity(this, gp.player, dx, dy);

        if (!this.collisionOn && gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m == this || m.isDead) continue;
                if (!(m instanceof Entitas.Golem) && !(m instanceof monster)) continue;
                gp.colision.checkEntity(this, m, dx, dy);
                if (this.collisionOn) break;
            }
        }

        if (!this.collisionOn && gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) continue;
                if (!((GolemMinion) m).isAlive()) continue;
                gp.colision.checkEntity(this, m, dx, dy);
                if (this.collisionOn) break;
            }
        }

        if (!this.collisionOn) {
            x += dx;
            y += dy;
        }
    }

    // =====================
    // DISTANCE CHECK
    // =====================

    public double distanceTo(Entity target) {

        int thisCenterX = x + solidArea.x + solidArea.width / 2;
        int thisCenterY = y + solidArea.y + solidArea.height / 2;

        int targetCenterX = target.x + target.solidArea.x + target.solidArea.width / 2;
        int targetCenterY = target.y + target.solidArea.y + target.solidArea.height / 2;

        int dx = thisCenterX - targetCenterX;
        int dy = thisCenterY - targetCenterY;

        return Math.sqrt(dx * dx + dy * dy);
    }
    
    // DROP AN DARI SLIME
    public void drop() {
        if(!name.equals("slime")) return;

        int chance = rand.nextInt(100);

        if(chance < 80){
            gp.drops.add(new Item.DropItem(x, y, Item.ItemManager.get().SLIME_GEL, 1));
        }

        if(chance < 20){
            gp.drops.add(new Item.DropItem(x, y, Item.ItemManager.get().SLIME_GEL, 2));
        }

        Main.Log.d("Drop keluar di map!");
    }
    
    
    // =====================
    // CHASE PLAYER
    // =====================

    public void chaseTarget(Entity target) {

        // pakai titik tengah hitbox (sama kayak distanceTo), bukan pojok kiri-atas sprite
        int dx = (target.x + target.solidArea.x + target.solidArea.width / 2)
               - (x + solidArea.x + solidArea.width / 2);
        int dy = (target.y + target.solidArea.y + target.solidArea.height / 2)
               - (y + solidArea.y + solidArea.height / 2);

        if (Math.abs(dx) > Math.abs(dy)) {
            direction = (dx < 0) ? "left" : "right";
        } else {
            direction = (dy < 0) ? "up" : "down";
        }

        int newX = x;
        int newY = y;

        if (direction.equals("left"))  newX -= speed;
        if (direction.equals("right")) newX += speed;
        if (direction.equals("up"))    newY -= speed;
        if (direction.equals("down"))  newY += speed;
        
        if (insideWanderArea(newX, newY)) {
            moveIfClear(newX - x, newY - y);
        } else {
            // target di luar area → berhenti chase, balik wander
            state = MonsterState.WANDER;
            // PERBAIKAN: paksa arah menuju tengah wanderArea supaya cepat balik
            int centerX = wanderArea.x + wanderArea.width / 2;
            int centerY = wanderArea.y + wanderArea.height / 2;
            int toCenterDx = centerX - x;
            int toCenterDy = centerY - y;
            if (Math.abs(toCenterDx) > Math.abs(toCenterDy)) {
                direction = (toCenterDx < 0) ? "left" : "right";
            } else {
                direction = (toCenterDy < 0) ? "up" : "down";
            }
        }
    }
    
    // =====================
    // RANDOM MOVEMENT
    // =====================

    public void wander() {

        actionLockCounter++;

        if (actionLockCounter > 120) {
            int r = rand.nextInt(4);
            if (r == 0) direction = "up";
            if (r == 1) direction = "down";
            if (r == 2) direction = "left";
            if (r == 3) direction = "right";
            actionLockCounter = 0;
        }

        wanderMoveDelay++;
        if (wanderMoveDelay < 4) return;
        wanderMoveDelay = 0;

        int newX = x;
        int newY = y;

        if (direction.equals("left"))  newX -= speed;
        if (direction.equals("right")) newX += speed;
        if (direction.equals("up"))    newY -= speed;
        if (direction.equals("down"))  newY += speed;

        if (insideWanderArea(newX, newY)) {
            moveIfClear(newX - x, newY - y);
        } else {
            // PERBAIKAN KUNCI: langsung re-random arah supaya tidak stuck
            // coba arah yang berlawanan atau random baru
            int r = rand.nextInt(4);
            if (r == 0) direction = "up";
            if (r == 1) direction = "down";
            if (r == 2) direction = "left";
            if (r == 3) direction = "right";
            actionLockCounter = 0; // reset counter biar langsung re-random lagi kalau masih mentok
        }
    }

    // =====================
    // ATTACK
    // =====================

    private void startAttack() {
        state = MonsterState.ATTACK;
        attackFrame = 0;
        attackTick = 0;
        attackCooldown = ATTACK_COOLDOWN_MAX;
    }

    private void updateAttack() {
        attackTick++;
        if (attackTick < ATTACK_FRAME_TICKS) return;

        attackTick = 0;
        attackFrame++;

        if (attackFrame == ATTACK_HIT_FRAME) dealAttackDamage();

        if (attackFrame >= monsterSprite.ATTACK_FRAMES) {
            attackFrame = 0;
            state = MonsterState.CHASE;
        }
    }

    // damage baru kena di frame ATTACK_HIT_FRAME, itu pun kalau target belum kabur
    private void dealAttackDamage() {
        Entity target = findNearestThreat();
        if (distanceTo(target) > attackRange + 30) return;

        if (target instanceof player) {
            ((player) target).takeDamage(attackDamage);
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

    /** true selama animasi mati masih jalan (dipakai GamePanel & TileM biar slime tetap di-update dan digambar). */
    public boolean isDeathAnimPlaying() {
        return state == MonsterState.DEAD && !deathDone;
    }

    /**
     * Target slime = entity terdekat antara player dan pion GolemMinion aktif.
     */
    private Entity findNearestThreat() {
        Entity nearest = gp.player;
        double minDist = distanceTo(gp.player);

        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (m == null || !(m instanceof GolemMinion)) continue;
                if (!((GolemMinion) m).isAlive()) continue;
                double d = distanceTo(m);
                if (d < minDist) { minDist = d; nearest = m; }
            }
        }

        return nearest;
    }

    // =====================
    // DAMAGE SYSTEM
    // =====================

    public void takeDamage(int dmg, Entity attacker) {

        if(state == MonsterState.DEAD) return;

        life -= dmg;
        regenTimer = 0;

        if(life <= 0){
            life = 0;
            die();
            return;
        }

        // lagi nyerang -> biarin animasi serangnya kelar dulu
        if(state != MonsterState.ATTACK){
            state = MonsterState.HIT;
            hitFrame = 0;
            hitTick = 0;
        }
    }

    // Pukulan player & skill masuk lewat Entity.takeDamage(int) / (int, player),
    // jadi dua ini harus diarahkan ke takeDamage di atas supaya anim hit/mati jalan.
    @Override
    public void takeDamage(int dmg) {
        takeDamage(dmg, (Entity) null);
    }

    @Override
    public void takeDamage(int dmg, player attacker) {
        takeDamage(dmg, (Entity) attacker);
    }

    public void die(){

        if(state == MonsterState.DEAD) return;

        state = MonsterState.DEAD;
        deathFrame = 0;
        deathTick = 0;
        deathDone = false;
        isDead = true;
        drop();
        
        gp.player.exp += this.getExpValue;
        gp.player.checkLevelUp();
        gp.player.giveCoinReward(this); // koin pasti drop
        gp.player.givePotionReward(this); // small 50% / medium 20%
        Main.Log.d("Slime mati");
    }

    // =====================
    // AI UPDATE
    // =====================

    @Override
    public void update() {

        if(state == MonsterState.DEAD){
            updateDeath();
            return;
        }

        if (attackCooldown > 0) attackCooldown--;

        Entity threat = findNearestThreat();
        double dist = distanceTo(threat);

     // HP REGEN
        if(life < maxLife){

            regenTimer++;

            if(regenTimer >= REGEN_DELAY){

                life += REGEN_RATE;

                if(life > maxLife){
                    life = maxLife;
                }

                regenTimer = 0;
            }
        }
        
        switch(state){

            case IDLE:

                state = MonsterState.WANDER;

                break;

            case WANDER:

                wander();
                animateWalk();

                if(dist < aggroRadius){
                    state = MonsterState.CHASE;
                }

                break;

            case CHASE:

                chaseTarget(threat);
                animateWalk();

                if(state == MonsterState.CHASE && dist < attackRange && attackCooldown == 0){
                    startAttack();
                }

                if(dist > aggroRadius){
                    state = MonsterState.WANDER;
                }

                break;

            case ATTACK:

                updateAttack();

                break;

            case HIT:

                updateHit();

                break;

            case DEAD:
                break;
        }

        super.update();
    }

    // =====================
    // HP BAR
    // =====================

    public void drawHPBar(Graphics2D g2, int screenX, int screenY) {

        // ❗ kalau HP full → jangan gambar bar
        if(life >= maxLife) return;

        int barWidth = 40;
        int barHeight = 5;

        int hpWidth = (int)((double) life / maxLife * barWidth);

        // taruh tepat di atas badan slime (tengah hitbox), bukan di pojok sprite
        int barX = screenX + solidArea.x + (solidArea.width - barWidth) / 2;
        int barY = screenY + spriteOffsetY + solidArea.y - 12;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(barX, barY, barWidth, barHeight);

        g2.setColor(Color.RED);
        g2.fillRect(barX, barY, hpWidth, barHeight);
    }
    
    // bayangn si slime
    public void drawShadow(Graphics2D g2, int screenX, int screenY){

        int shadowWidth = spriteWidth - 35;
        int shadowHeight = spriteHeight / 4;

        int shadowX = screenX + (spriteWidth - shadowWidth) / 2;
        int shadowY = screenY + spriteHeight - shadowHeight + 5;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(Color.BLACK);

        g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
    
    @Override
    public void draw(Graphics2D g2) {

        BufferedImage image;

        switch(state){

            case DEAD:
                image = sprite.getDeath(direction, deathFrame);
                break;

            case HIT:
                image = sprite.getHit(direction, hitFrame);
                break;

            case ATTACK:
                image = sprite.getAttack(direction, attackFrame);
                break;

            default:
                image = sprite.getWalk(direction, walkFrame);
                break;
        }

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        // gambar bayangan dulu
        drawShadow(g2, screenX, screenY + spriteOffsetY);

        // gambar slime. Kanvas sprite bisa beda ukuran (serang 64x64, lainnya 32x32),
        // jadi skalanya ikut ukuran kanvas lalu dipusatkan di kotak sprite biar badannya
        // tetap sama gede & tetap di posisi yang sama antar animasi.
        int drawW = Math.round(spriteWidth  * image.getWidth()  / (float) monsterSprite.BASE_SIZE);
        int drawH = Math.round(spriteHeight * image.getHeight() / (float) monsterSprite.BASE_SIZE);
        int drawX = screenX - (drawW - spriteWidth) / 2;
        int drawY = screenY + spriteOffsetY - (drawH - spriteHeight) / 2;

        g2.drawImage(image, drawX, drawY, drawW, drawH, null);

        if(state != MonsterState.DEAD) drawHPBar(g2, screenX, screenY);
    }
}