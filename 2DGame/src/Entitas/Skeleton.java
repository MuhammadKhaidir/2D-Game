package Entitas;

import Item.ItemManager;
import Main.GamePanel;
import SFX.SoundManager;
import Tamed.GolemMinion;
import Tamed.GolemiteMinion;
import Tamed.UndeadMinion;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class Skeleton extends Entity {

    // ══════════════════════════════════════════════════════
    //  STATIC IMAGE CACHE — dimuat SEKALI, dipakai semua instance
    //  (field static tidak private agar UndeadMinion bisa pakai cache ini)
    // ══════════════════════════════════════════════════════
	public static BufferedImage[] S_walkFront  = new BufferedImage[6];
	public static BufferedImage[] S_walkBack   = new BufferedImage[6];
	public static BufferedImage[] S_walkLeft   = new BufferedImage[6];
	public static BufferedImage[] S_walkRight  = new BufferedImage[6];

	public static BufferedImage[] S_idleFront  = new BufferedImage[2];
	public static BufferedImage[] S_idleBack   = new BufferedImage[2];
	public static BufferedImage[] S_idleLeft   = new BufferedImage[2];
	public static BufferedImage[] S_idleRight  = new BufferedImage[2];

	public static BufferedImage[] S_swing1Front = new BufferedImage[4];
	public static BufferedImage[] S_swing1Back  = new BufferedImage[4];
	public static BufferedImage[] S_swing1Left  = new BufferedImage[4];
	public static BufferedImage[] S_swing1Right = new BufferedImage[4];

	public static BufferedImage[] S_swing2Front = new BufferedImage[4];
	public static BufferedImage[] S_swing2Back  = new BufferedImage[4];
	public static BufferedImage[] S_swing2Left  = new BufferedImage[4];
	public static BufferedImage[] S_swing2Right = new BufferedImage[4];

	public static BufferedImage[] S_wp1Front = new BufferedImage[4];
	public static BufferedImage[] S_wp1Back  = new BufferedImage[4];
	public static BufferedImage[] S_wp1Left  = new BufferedImage[4];
	public static BufferedImage[] S_wp1Right = new BufferedImage[4];

	public static BufferedImage[] S_wp2Front = new BufferedImage[4];
	public static BufferedImage[] S_wp2Back  = new BufferedImage[4];
	public static BufferedImage[] S_wp2Left  = new BufferedImage[4];
	public static BufferedImage[] S_wp2Right = new BufferedImage[4];

	public static BufferedImage S_hurtFront, S_hurtBack, S_hurtLeft, S_hurtRight;

	public static BufferedImage[] S_deathFront = new BufferedImage[3];
	public static BufferedImage[] S_deathBack  = new BufferedImage[3];
	public static BufferedImage[] S_deathLeft  = new BufferedImage[3];
	public static BufferedImage[] S_deathRight = new BufferedImage[3];

    private static boolean imagesLoaded = false;
    
    public int attackDamage = 15;
    
    private int noDamageTimer = 0; // Menghitung waktu sejak terakhir kena hit

    public static synchronized void preloadImages() {
        if (imagesLoaded) return;
        try {
            Class<?> cls = Skeleton.class;
            for (int i = 0; i < 6; i++) {
                S_walkFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonFrontWalk (" + (i+1) + ").png"));
                S_walkBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonBackWalk ("  + (i+1) + ").png"));
                S_walkLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonLeftWalk ("  + (i+1) + ").png"));
                S_walkRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonRightWalk (" + (i+1) + ").png"));
            }
            for (int i = 0; i < 2; i++) {
                S_idleFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonIdleFront (" + (i+1) + ").png"));
                S_idleBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonIdleBack ("  + (i+1) + ").png"));
                S_idleLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonIdleLeft ("  + (i+1) + ").png"));
                S_idleRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonIdleRight (" + (i+1) + ").png"));
            }
            for (int i = 0; i < 4; i++) {
                S_swing1Front[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingFront (" + (i+1) + ").png"));
                S_swing1Back[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingBack ("  + (i+1) + ").png"));
                S_swing1Left[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingLeft ("  + (i+1) + ").png"));
                S_swing1Right[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingRight (" + (i+1) + ").png"));
            }
            for (int i = 0; i < 4; i++) {
                S_swing2Front[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingFront (" + (i+5) + ").png"));
                S_swing2Back[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingBack ("  + (i+5) + ").png"));
                S_swing2Left[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingLeft ("  + (i+5) + ").png"));
                S_swing2Right[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonSwingRight (" + (i+5) + ").png"));
            }
            for (int i = 0; i < 4; i++) {
                S_wp1Front[i] = ImageIO.read(cls.getResourceAsStream("/Aset/FrontSw (" + (i+1) + ").png"));
                S_wp1Back[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/BackSw ("  + (i+1) + ").png"));
                S_wp1Left[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/LeftSw ("  + (i+1) + ").png"));
                S_wp1Right[i] = ImageIO.read(cls.getResourceAsStream("/Aset/RightSw (" + (i+1) + ").png"));
            }
            for (int i = 0; i < 4; i++) {
                S_wp2Front[i] = ImageIO.read(cls.getResourceAsStream("/Aset/FrontSw (" + (i+5) + ").png"));
                S_wp2Back[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/BackSw ("  + (i+5) + ").png"));
                S_wp2Left[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/LeftSw ("  + (i+5) + ").png"));
                S_wp2Right[i] = ImageIO.read(cls.getResourceAsStream("/Aset/RightSw (" + (i+5) + ").png"));
            }
            S_hurtFront = ImageIO.read(cls.getResourceAsStream("/Aset/SkelFrontHurt.png"));
            S_hurtBack  = ImageIO.read(cls.getResourceAsStream("/Aset/SkelBackHurt.png"));
            S_hurtLeft  = ImageIO.read(cls.getResourceAsStream("/Aset/SkelLeftHurt.png"));
            S_hurtRight = ImageIO.read(cls.getResourceAsStream("/Aset/SkelRightHurt.png"));
            for (int i = 0; i < 3; i++) {
                S_deathFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkelFrontDeath (" + (i+1) + ").png"));
                S_deathBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkelBackDeath ("  + (i+1) + ").png"));
                S_deathLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkelLeftDeath ("  + (i+1) + ").png"));
                S_deathRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkelRightDeath (" + (i+1) + ").png"));
            }
            imagesLoaded = true;
            Main.Log.d("[Skeleton] Semua gambar berhasil di-preload (shared cache).");
        } catch (Exception e) {
            System.out.println("[Skeleton] Gagal preload gambar: " + e.getMessage());
        }
    }

    // ── State machine ──
    enum SkeletonState { IDLE, WALK, ATTACK1, ATTACK2, HURT, DEATH }
    SkeletonState skState = SkeletonState.IDLE;

    int walkFrameIndex = 0, walkAnimTimer = 0;
    int idleFrameIndex = 0, idleAnimTimer = 0;
    int attackFrameIndex = 0, attackTimer = 0, attackCooldown = 0;
    
    

    private int hurtTimer = 0;
    private final int HURT_DURATION = 15;

    // ── Efek kena pukul: badan putih sekejap ──
    private static final int HIT_FLASH_FRAMES = 5;   // 5 frame @40 FPS = 0,125 detik
    private int whiteFlashTimer = 0;                 // > 0 = lagi putih. Jalan sendiri, gak ikut ketahan state HURT
    /** Siluet putih tiap sprite, dibuat sekali pas pertama dibutuhkan lalu dipakai ulang. */
    private static final Map<BufferedImage, BufferedImage> WHITE_CACHE = new IdentityHashMap<>();

    private int deathFrameIndex = 0, deathAnimTimer = 0;
    private boolean deathAnimDone = false;

    public int spawnX, spawnY;
    private boolean isSpawnPointSet = false;
    private int respawnTimer = 0;
    private final int RESPAWN_DELAY = 30 * 60;
    public boolean alive = true;   // public agar DeathTotemManager bisa cek
    public boolean noRespawn = false;
    public boolean noDrop = false; // NEW: true kalau ini pion summonan boss

    public Rectangle wanderArea;
    int wanderTimer = 0;
    boolean hasTarget = false;
    int detectionRange = 120;
    int attackRange    = 50;

    private final SoundManager sfx = new SoundManager();

    public Skeleton(GamePanel gp) {
        super(gp);
        this.maxLife = 400;
        this.life    = maxLife;
        this.speed   = 1.5;
        this.direction = "down";
        this.solidArea = new Rectangle(20, 24, 24, 24);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 30;
        this.attackDamage = 15;
    }

    public int getSortY() {
        return this.y + solidArea.y + solidArea.height;
    }

    @Override
    public void update() {
        if (!isSpawnPointSet) { spawnX = x; spawnY = y; isSpawnPointSet = true; }

        if (whiteFlashTimer > 0) whiteFlashTimer--;

        if (skState == SkeletonState.DEATH) { updateDeathAnimation(); return; }

        if (life <= 0 && alive) {
            alive = false;
            skState = SkeletonState.DEATH;
            isKnockback = false; knockbackCounter = 0;
            deathFrameIndex = 0; deathAnimTimer = 0; deathAnimDone = false;
            this.solidArea.x = -9999; this.solidArea.y = -9999;
            dropItems();
            gp.player.killMonster(this);
            return;
        }

        if (!alive) {
            if (!noRespawn) { respawnTimer++; if (respawnTimer >= RESPAWN_DELAY) respawn(); }
            return;
        }

        if (isKnockback) updateKnockback();

        if (skState == SkeletonState.HURT) {
            hurtTimer--;
            if (hurtTimer <= 0) skState = SkeletonState.IDLE;
            return;
        }
        if (life < maxLife) {
            noDamageTimer++;
            if (noDamageTimer >= 600) { // 600 frame = 10 detik bebas hit (asumsi 60 FPS)
                if (noDamageTimer % 5 == 0) { // Setiap 5 frame, pulihkan 1 HP
                    life += 1;
                    if (life > maxLife) life = maxLife;
                }
            }
        } else {
            noDamageTimer = 0; // Reset ke 0 kalau HP sudah full
        }

        if (isHit) { hitFlashTimer--; if (hitFlashTimer <= 0) isHit = false; }
        if (attackCooldown > 0) attackCooldown--;

        // ── TARGETING: Cari Target Terdekat (Player atau Minion) ──
        Entity target = gp.player; 

        int skCX = x + solidArea.x + solidArea.width / 2;
        int skCY = y + solidArea.y + solidArea.height / 2;
        
        int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2;
        int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;

        int distX = plCX - skCX;
        int distY = plCY - skCY;
        double minDist = Math.sqrt(distX * distX + distY * distY);

        if (gp.deathTotemManager != null) {
        	for (Entity minion : gp.deathTotemManager.getMinions()) {
        	    if (minion == null) continue;

        	    // ── UBAH: cek alive via instanceof ──
        	    boolean minionAlive = false;
        	    if (minion instanceof UndeadMinion) minionAlive = ((UndeadMinion) minion).alive;
        	    if (minion instanceof GolemMinion)  minionAlive = ((GolemMinion)  minion).alive;
            if (minion instanceof GolemiteMinion) minionAlive = minion.alive;
        	    if (!minionAlive) continue;

        	    int mnCX = minion.x + minion.solidArea.x + minion.solidArea.width / 2;
        	    int mnCY = minion.y + minion.solidArea.y + minion.solidArea.height / 2;
        	    int mDistX = mnCX - skCX;
        	    int mDistY = mnCY - skCY;
        	    double mDist = Math.sqrt(mDistX * mDistX + mDistY * mDistY);

        	    if (mDist < minDist) {
        	        minDist = mDist;
        	        target  = minion;
        	        distX   = mDistX;
        	        distY   = mDistY;
        	    }
        	}
        }
        
        double dist = minDist; 
        // ───────────────────────────────────────────────────────────────────

        if (dist <= attackRange && attackCooldown == 0) {
            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";

            if (skState != SkeletonState.ATTACK1 && skState != SkeletonState.ATTACK2) {
                skState = SkeletonState.ATTACK1; attackFrameIndex = 0; attackTimer = 0;
            }
        } 
        else if (dist <= detectionRange) {
            if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
            else direction = distY > 0 ? "down" : "up";

            skState = SkeletonState.WALK;
            int dx = 0, dy = 0;
            if (distX > 0) dx = 1; else if (distX < 0) dx = -1;
            if (distY > 0) dy = 1; else if (distY < 0) dy = -1;
            double moveSpeed = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
            dx = (int) Math.round(dx * moveSpeed);
            dy = (int) Math.round(dy * moveSpeed);
            
            this.collisionOn = false;
            Rectangle nextBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
            boolean tileCollision = gp.domainManager.checkCollision(nextBox);
            if (gp.colision != null) gp.colision.checkPlayer(this);
            
            // Perbaikan Utama: Cek tabrakan dengan minion saat mengejar target
            boolean minionCollision = checkMinionCollision(dx, dy, target);
            
            if (!tileCollision && !this.collisionOn && !minionCollision) { 
                x += dx; 
                y += dy; 
            }
        } 
        else {
            wanderTimer++;
            
            if (wanderTimer >= 120) {
                int i = (int)(Math.random() * 5);
                if (i == 0) { direction = "up"; skState = SkeletonState.WALK; }
                else if (i == 1) { direction = "down"; skState = SkeletonState.WALK; }
                else if (i == 2) { direction = "left"; skState = SkeletonState.WALK; }
                else if (i == 3) { direction = "right"; skState = SkeletonState.WALK; }
                else { skState = SkeletonState.IDLE; }
                wanderTimer = 0;
            }

            if (skState == SkeletonState.WALK) {
                int dx = 0, dy = 0;
                if (direction.equals("up"))    dy = -(int) Math.round(speed);
                if (direction.equals("down"))  dy = (int) Math.round(speed);
                if (direction.equals("left"))  dx = -(int) Math.round(speed);
                if (direction.equals("right")) dx = (int) Math.round(speed);

                this.collisionOn = false;
                Rectangle nextBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
                
                boolean tileCollision = gp.domainManager.checkCollision(nextBox);
                
                if (gp.colision != null) {
                    gp.colision.checkPlayer(this); 
                }

                // Perbaikan Utama: Cek tabrakan dengan minion saat berpatroli bebas
                boolean minionCollision = checkMinionCollision(dx, dy, target);

                if (!tileCollision && !this.collisionOn && !minionCollision) {
                    x += dx;
                    y += dy;
                } else {
                    skState = SkeletonState.IDLE;
                    wanderTimer = 0;
                }
            }
        }

        if (skState == SkeletonState.IDLE) {
            idleAnimTimer++;
            if (idleAnimTimer >= 20) { idleFrameIndex = (idleFrameIndex + 1) % 2; idleAnimTimer = 0; }
        } else { idleFrameIndex = 0; idleAnimTimer = 0; }

        if (skState == SkeletonState.WALK) {
            walkAnimTimer++;
            if (walkAnimTimer >= 8) { walkFrameIndex = (walkFrameIndex + 1) % 6; walkAnimTimer = 0; }
        }

        if (skState == SkeletonState.ATTACK1 || skState == SkeletonState.ATTACK2) {
        	if (attackFrameIndex == 0 && attackTimer == 0) {
                sfx.swingSound();
            }
            attackTimer++;
            if (attackTimer >= 6) {
                attackTimer = 0; attackFrameIndex++;
                if (attackFrameIndex == 2) hitTarget(target);
                if (attackFrameIndex >= 4) {
                    attackFrameIndex = 0;
                    if (skState == SkeletonState.ATTACK1) skState = SkeletonState.ATTACK2;
                    else { skState = SkeletonState.IDLE; attackCooldown = 60; }
                }
            }
        }
    }

    /**
     * FUNGSI COLLISION FIX: Memeriksa tabrakan prediktif dengan UndeadMinion.
     * Jika menabrak minion yang diincarnya, otomatis memaksa skeleton melakukan animasi serang.
     */
    private boolean checkMinionCollision(int dx, int dy, Entity currentTarget) {
        if (gp.deathTotemManager == null) return false;

        Rectangle nextBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);

        for (Entity minion : gp.deathTotemManager.getMinions()) {
            if (minion == null) continue;

            // ── UBAH: cek alive via instanceof ──
            boolean minionAlive = false;
            if (minion instanceof UndeadMinion) minionAlive = ((UndeadMinion) minion).alive;
            if (minion instanceof GolemMinion)  minionAlive = ((GolemMinion)  minion).alive;
            if (minion instanceof GolemiteMinion) minionAlive = minion.alive;
            if (!minionAlive) continue;

            Rectangle minionBox = new Rectangle(
                minion.x + minion.solidArea.x,
                minion.y + minion.solidArea.y,
                minion.solidArea.width,
                minion.solidArea.height
            );

            if (nextBox.intersects(minionBox)) {
                if (minion == currentTarget && attackCooldown == 0) {
                    if (skState != SkeletonState.ATTACK1 && skState != SkeletonState.ATTACK2) {
                        skState = SkeletonState.ATTACK1;
                        attackFrameIndex = 0;
                        attackTimer = 0;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private void updateDeathAnimation() {
        if (deathAnimDone) {
            if (!noRespawn) { respawnTimer++; if (respawnTimer >= RESPAWN_DELAY) respawn(); }
            return;
        }
        deathAnimTimer++;
        if (deathAnimTimer >= 10) {
            deathAnimTimer = 0; deathFrameIndex++;
            if (deathFrameIndex >= 3) { deathFrameIndex = 2; deathAnimDone = true; }
        }
    }

    public void respawn() {
        this.alive = true; this.isDead = false;
        this.life = maxLife; this.x = spawnX; this.y = spawnY;
        this.skState = SkeletonState.IDLE; this.direction = "down";
        this.attackFrameIndex = 0; this.walkFrameIndex = 0;
        this.idleFrameIndex = 0; this.deathFrameIndex = 0;
        this.deathAnimDone = false; this.hurtTimer = 0;
        this.attackCooldown = 0; this.respawnTimer = 0; this.isHit = false;
        this.isKnockback = false; this.knockbackCounter = 0; this.kbReturning = false;
        this.whiteFlashTimer = 0;
        this.solidArea.x = this.solidAreaDefaultX;
        this.solidArea.y = this.solidAreaDefaultY;
        this.noDamageTimer = 0;
        Main.Log.d("[Skeleton] Bangkit dari kubur di koordinat awal!");
    }

    private void dropItems() {
    	if (noDrop) return;
        ItemManager im = ItemManager.get();
        double roll;
        roll = Math.random() * 100;
        if (roll < 100) {
            int b = 1+(int)(Math.random()*20);
            im.add(im.BONE,b);
            if (gp.speechBubble != null) gp.speechBubble.show("Obtained " + b + "x Bone");
        }
        roll = Math.random() * 100;
        if (roll < 100)  {
            im.add(im.SKULL,10);
            if (gp.speechBubble != null) gp.speechBubble.show("Obtained 1x Skull");
        }
        roll = Math.random() * 100;
        if (roll < 50)  {
            im.add(im.SKELETON_KEY,1);
            if (gp.speechBubble != null) gp.speechBubble.show("Rare drop: Skeleton Key!");
        }
        roll = Math.random() * 100;
        if (roll < 0.1)  {
            im.add(im.DEATH_TOTEM,1);
            if (gp.speechBubble != null) gp.speechBubble.show("Very rare drop: Death Totem!");
        }

        if (gp.deathTotemManager != null) {
            gp.deathTotemManager.tryCapture(this);
        }
    }

    private void hitTarget(Entity target) {
        Rectangle atk = getAttackBox(); 
        Rectangle tgt = new Rectangle(
            target.x + target.solidArea.x, 
            target.y + target.solidArea.y,
            target.solidArea.width, 
            target.solidArea.height
        );
        
        if (atk != null && atk.intersects(tgt)) {
            if (target instanceof player) ((player) target).takeDamageFrom(this.attackDamage, this);
            else target.takeDamage(this.attackDamage);
        }
    }

    private Rectangle getAttackBox() {
        // Pusat badan skeleton di world space
        int cx = x + solidArea.x + solidArea.width  / 2;
        int cy = y + solidArea.y + solidArea.height / 2;
        int reach  = 45; // jangkauan ke depan
        int spread = 20; // lebar area serang

        switch (direction) {
            case "up":    return new Rectangle(cx - spread, cy - solidArea.height/2 - reach, spread*2, reach);
            case "down":  return new Rectangle(cx - spread, cy + solidArea.height/2,         spread*2, reach);
            case "left":  return new Rectangle(cx - solidArea.width/2 - reach, cy - spread,  reach, spread*2);
            case "right": return new Rectangle(cx + solidArea.width/2,         cy - spread,  reach, spread*2);
        }
        return null;
    }
    
    
    @Override
    public void draw(Graphics2D g2) {
        if (!alive && skState != SkeletonState.DEATH) return;

        int drawSize = gp.tileSize * 2;
        int sx = x - gp.player.x + gp.player.screenX - gp.tileSize / 2;
        int sy = y - gp.player.y + gp.player.screenY - gp.tileSize / 3;
        if (sx + drawSize < gp.viewMinX() || sx > gp.viewMaxX() || sy + drawSize < gp.viewMinY() || sy > gp.viewMaxY()) return;

        BufferedImage charImg = null, weaponImg = null;

        if (skState == SkeletonState.DEATH) {
            switch (direction) {
                case "down":  charImg = S_deathFront[deathFrameIndex]; break;
                case "up":    charImg = S_deathBack[deathFrameIndex];  break;
                case "left":  charImg = S_deathLeft[deathFrameIndex];  break;
                case "right": charImg = S_deathRight[deathFrameIndex]; break;
            }
        } else if (skState == SkeletonState.HURT) {
            switch (direction) {
                case "down":  charImg = S_hurtFront; break;
                case "up":    charImg = S_hurtBack;  break;
                case "left":  charImg = S_hurtLeft;  break;
                case "right": charImg = S_hurtRight; break;
            }
        } else if (skState == SkeletonState.ATTACK1) {
            switch (direction) {
                case "down":  charImg = S_swing1Front[attackFrameIndex]; weaponImg = S_wp1Front[attackFrameIndex]; break;
                case "up":    charImg = S_swing1Back[attackFrameIndex];  weaponImg = S_wp1Back[attackFrameIndex];  break;
                case "left":  charImg = S_swing1Left[attackFrameIndex];  weaponImg = S_wp1Left[attackFrameIndex];  break;
                case "right": charImg = S_swing1Right[attackFrameIndex]; weaponImg = S_wp1Right[attackFrameIndex]; break;
            }
        } else if (skState == SkeletonState.ATTACK2) {
            switch (direction) {
                case "down":  charImg = S_swing2Front[attackFrameIndex]; weaponImg = S_wp2Front[attackFrameIndex]; break;
                case "up":    charImg = S_swing2Back[attackFrameIndex];  weaponImg = S_wp2Back[attackFrameIndex];  break;
                case "left":  charImg = S_swing2Left[attackFrameIndex];  weaponImg = S_wp2Left[attackFrameIndex];  break;
                case "right": charImg = S_swing2Right[attackFrameIndex]; weaponImg = S_wp2Right[attackFrameIndex]; break;
            }
        } else if (skState == SkeletonState.WALK) {
            switch (direction) {
                case "down":  charImg = S_walkFront[walkFrameIndex]; break;
                case "up":    charImg = S_walkBack[walkFrameIndex];  break;
                case "left":  charImg = S_walkLeft[walkFrameIndex];  break;
                case "right": charImg = S_walkRight[walkFrameIndex]; break;
            }
        } else {
            switch (direction) {
                case "down":  charImg = S_idleFront[idleFrameIndex]; break;
                case "up":    charImg = S_idleBack[idleFrameIndex];  break;
                case "left":  charImg = S_idleLeft[idleFrameIndex];  break;
                case "right": charImg = S_idleRight[idleFrameIndex]; break;
            }
        }
        if (skState != SkeletonState.DEATH && life < maxLife) {
            int barW = 40, barH = 5;
            int hpFill = (int)((double) life / maxLife * barW);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(sx + drawSize/2 - barW/2, sy - 8, barW, barH);
            g2.setColor(new Color(200, 50, 50));
            g2.fillRect(sx + drawSize/2 - barW/2, sy - 8, hpFill, barH);
        }

        if (skState != SkeletonState.DEATH) {
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(Color.BLACK);
            int shadowW = drawSize / 3, shadowH = shadowW / 3;
            g2.fillOval(sx + drawSize/2 - shadowW/2, sy + drawSize - shadowH - 24, shadowW, shadowH);
            g2.setComposite(oldComp);
        }

        boolean flash = whiteFlashTimer > 0;
        if (charImg != null)   g2.drawImage(flash ? whiteVersion(charImg)   : charImg,   sx, sy, drawSize, drawSize, null);
        if (weaponImg != null) g2.drawImage(flash ? whiteVersion(weaponImg) : weaponImg, sx, sy, drawSize, drawSize, null);

    } 

    @Override
    public void takeDamage(int dmg) {
    	noDamageTimer = 0;
        if (!alive) return;
        if (skState == SkeletonState.DEATH) return;
        life -= dmg; isHit = true; hitFlashTimer = 8;
        whiteFlashTimer = HIT_FLASH_FRAMES;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);
        if (life > 0) {
            skState = SkeletonState.HURT; hurtTimer = HURT_DURATION;
            attackFrameIndex = 0; attackTimer = 0;
        }
    }

    // ══════════════════════════════════════════════════════
    //  KNOCKBACK
    //  Dipanggil SETELAH takeDamage() oleh siapa pun yang nge-hit skeleton
    //  (player, UndeadMinion, GolemMinion, GolemiteMinion), contoh:
    //      sk.takeDamage(dmg);
    //      sk.knockbackFrom(this);
    // ══════════════════════════════════════════════════════
    // Cuma kedorong dikit, terus balik lagi ke titik tempat dia kena pukul (kayak kaget / kesentak).
    private static final int KNOCKBACK_DISTANCE  = 8;   // px, jarak dorongan maksimal dari titik kena pukul
    private static final int KNOCKBACK_OUT_STEP  = 4;   // px/frame pas kedorong   (8 / 4 = 2 frame)
    private static final int KNOCKBACK_BACK_STEP = 2;   // px/frame pas balik      (8 / 2 = 4 frame)

    private int kbOriginX, kbOriginY;     // titik tempat dia kena pukul = tempat balik
    private int kbTargetX, kbTargetY;     // ujung dorongan
    private boolean kbReturning = false;  // false = lagi kedorong, true = lagi balik ke titik asal

    /** Dorong skeleton sedikit menjauh dari attacker, lalu dia balik ke titik awal. Arah dorong ditentukan dari posisi relatif, bukan arah hadap attacker. */
    public void knockbackFrom(Entity attacker) {
        if (attacker == null || !alive || life <= 0 || skState == SkeletonState.DEATH) return;

        int aCX = attacker.x + attacker.solidArea.x + attacker.solidArea.width  / 2;
        int aCY = attacker.y + attacker.solidArea.y + attacker.solidArea.height / 2;
        int sCX = x + solidArea.x + solidArea.width  / 2;
        int sCY = y + solidArea.y + solidArea.height / 2;
        int dx = sCX - aCX;
        int dy = sCY - aCY;

        String pushDir;
        if (dx == 0 && dy == 0) {
            // posisi persis sama: fallback ke arah hadap attacker
            pushDir = (attacker.direction != null) ? attacker.direction : "down";
        } else if (Math.abs(dx) > Math.abs(dy)) {
            pushDir = dx > 0 ? "right" : "left";
        } else {
            pushDir = dy > 0 ? "down" : "up";
        }

        // Kalau kena pukul lagi pas masih kedorong / lagi balik, titik asalnya tetap yang pertama
        // (jadi dipukul bertubi-tubi gak bikin dia melorot pelan-pelan).
        if (!isKnockback) { kbOriginX = x; kbOriginY = y; }

        applyKnockback(pushDir); // dari Entity: set isKnockback, counter = 0, dan arah dorong
        kbTargetX = kbOriginX + knockbackDirectionX * KNOCKBACK_DISTANCE;
        kbTargetY = kbOriginY + knockbackDirectionY * KNOCKBACK_DISTANCE;
        kbReturning = false;
    }

    private void updateKnockback() {
        int goalX = kbReturning ? kbOriginX : kbTargetX;
        int goalY = kbReturning ? kbOriginY : kbTargetY;
        int step  = kbReturning ? KNOCKBACK_BACK_STEP : KNOCKBACK_OUT_STEP;

        // maju ke titik tujuan, gak boleh kelewatan
        int dx = Integer.compare(goalX, x) * Math.min(step, Math.abs(goalX - x));
        int dy = Integer.compare(goalY, y) * Math.min(step, Math.abs(goalY - y));

        if (dx != 0 || dy != 0) {
            Rectangle nextBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
            if (gp.domainManager != null && gp.domainManager.checkCollision(nextBox)) {
                // mentok tembok domain: kalau lagi kedorong, langsung balik; kalau lagi balik, berhenti di situ
                if (kbReturning) endKnockback(); else kbReturning = true;
                return;
            }
            x += dx;
            y += dy;
        }

        if (x == goalX && y == goalY) {
            if (kbReturning) endKnockback();   // udah balik ke titik awal
            else kbReturning = true;           // udah sampai ujung dorongan, sekarang balik
        }
    }

    private void endKnockback() {
        isKnockback = false;
        knockbackCounter = 0;
        kbReturning = false;
    }

    /** Siluet putih sebuah sprite (alpha dipertahankan, warna jadi putih semua). Di-cache per gambar. */
    private static BufferedImage whiteVersion(BufferedImage src) {
        BufferedImage w = WHITE_CACHE.get(src);
        if (w != null) return w;
        int iw = src.getWidth(), ih = src.getHeight();
        w = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < ih; py++) {
            for (int px = 0; px < iw; px++) {
                int a = src.getRGB(px, py) >>> 24;
                if (a != 0) w.setRGB(px, py, (a << 24) | 0xFFFFFF);
            }
        }
        WHITE_CACHE.put(src, w);
        return w;
    }
}