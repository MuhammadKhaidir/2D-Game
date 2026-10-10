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

    private int attackFreeze = 0;
    private final int ATTACK_FREEZE_MAX = 15;

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

        mfront1 = sprite.mfront1;
        mfront2 = sprite.mfront2;
        mback1  = sprite.mback1;
        mback2  = sprite.mback2;
        mleft1  = sprite.mleft1;
        mleft2  = sprite.mleft2;
        mright1 = sprite.mright1;
        mright2 = sprite.mright2;

        mstand = sprite.mstand;

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

        System.out.println("Drop keluar di map!");
    }
    
    
    // =====================
    // CHASE PLAYER
    // =====================

    public void chaseTarget(Entity target) {

        int dx = target.x - x;
        int dy = target.y - y;

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

    public void attack() {

        if (attackCooldown > 0) return;

        Entity target = findNearestThreat();
        if (target instanceof player) {
            ((player) target).takeDamage(attackDamage);
        } else if (target instanceof GolemMinion) {
            ((GolemMinion) target).takeDamage(attackDamage);
        }

        attackCooldown = ATTACK_COOLDOWN_MAX;
        attackFreeze = ATTACK_FREEZE_MAX;

        imgNum = 3;
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

        state = MonsterState.HIT;

        if(life <= 0){
            life = 0;
            die();
        }
    }

    public void die(){

        state = MonsterState.DEAD;
        isDead = true;
        drop();
        
        gp.player.exp += this.getExpValue;
        gp.player.checkLevelUp();
        System.out.println("Slime mati");
    }

    // =====================
    // AI UPDATE
    // =====================

    @Override
    public void update() {

        if(state == MonsterState.DEAD) return;

        if (attackCooldown > 0) attackCooldown--;

        if (attackFreeze > 0) {
            attackFreeze--;
            return;
        }

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

                if(dist < aggroRadius){
                    state = MonsterState.CHASE;
                }

                break;

            case CHASE:

                chaseTarget(threat);

                if(dist < attackRange){
                    state = MonsterState.ATTACK;
                }

                if(dist > aggroRadius){
                    state = MonsterState.WANDER;
                }

                break;

            case ATTACK:

                attack();
                state = MonsterState.CHASE;

                break;

            case HIT:

                state = MonsterState.CHASE;

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

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(screenX, screenY - 10, barWidth, barHeight);

        g2.setColor(Color.RED);
        g2.fillRect(screenX, screenY - 10, hpWidth, barHeight);
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

        BufferedImage image = null;

        switch(direction){

            case "up":
                image = (imgNum == 1) ? mback1 : mback2;
                break;

            case "down":
                image = (imgNum == 1) ? mfront1 : mfront2;
                break;

            case "left":
                image = (imgNum == 1) ? mleft1 : mleft2;
                break;

            case "right":
                image = (imgNum == 1) ? mright1 : mright2;
                break;
        }

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        // gambar bayangan dulu
        drawShadow(g2, screenX, screenY + spriteOffsetY);

        // gambar slime
        g2.drawImage(image, screenX, screenY + spriteOffsetY, spriteWidth, spriteHeight, null);

        drawHPBar(g2, screenX, screenY);
    }
}