package Entitas;
import Main.KeyHandler;
import SFX.SoundManager;
import Object.breakable;
import Object.PlayerCloth;
import Object.Weapon;
import Object.PlayerArmor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import Main.GamePanel;
import Object.PlayerCloth;

public class player extends Entity {
    KeyHandler KeyH;
    private SoundManager sound;
    public boolean isWeaponEquipped = false;

    public int maxLife = 100;
    public int life = maxLife;
    public boolean isDead = false;

    // Buff sementara dari boost potion (damage, speed, attack speed, max HP)
    public final PlayerBuffs buffs = new PlayerBuffs(this);


    private int noDamageTimer = 0;
    private int regenTickTimer = 0;
    private static final int REGEN_DELAY_FRAMES = 300; 
    private static final double REGEN_PERCENT_PER_SECOND = 0.02; 
    
    public int def   = 2;
    public int dodge = 0;
    public BufferedImage stand;
    
    // SKILL PLAYER
    public Skill.SkillManager skillManager;
    
    // ===== WEAPON SYSTEM =====
    public Weapon currentWeapon;
    public PlayerCloth currentCloth;
    
    // ===== ARMOR SYSTEM =====
    // Armor = SATU set utuh (helm + badan + sarung tangan + celana + sepatu). null = cuma pakai baju dasar.
    // Selama armor dipakai, layer badannya MENGGANTIKAN baju dasar (currentCloth), layer helmnya digambar paling atas.
    public PlayerArmor currentArmor;
    
    // ===== ATTACK ANIMATION VARIABLES =====
    int attackFrameIndex = 0;
    int attackTotalTimer = 0;
    int attackTimer = 0;
    int attackType = 0;
    int comboWindowTimer = 0;
    boolean hasComboInput = false;
    boolean attackHit = false;
    
    // --- ANIMASI KARAKTER ---
    BufferedImage[] attackUpFrames = new BufferedImage[4];
    BufferedImage[] attackDownFrames = new BufferedImage[4];
    BufferedImage[] attackLeftFrames = new BufferedImage[4];
    BufferedImage[] attackRightFrames = new BufferedImage[4];
    
    BufferedImage[] swingUpFrames = new BufferedImage[4];
    BufferedImage[] swingDownFrames = new BufferedImage[4];
    BufferedImage[] swingLeftFrames = new BufferedImage[4];
    BufferedImage[] swingRightFrames = new BufferedImage[4];
    
    BufferedImage[] state3UpFrames = new BufferedImage[4];
    BufferedImage[] state4DownFrames = new BufferedImage[4];
    BufferedImage[] state3DownFrames = new BufferedImage[4];
    BufferedImage[] state3LeftFrames = new BufferedImage[4];
    BufferedImage[] state3RightFrames = new BufferedImage[4];
    
    BufferedImage[] state4UpFrames = new BufferedImage[4];
    BufferedImage[] state4LeftFrames = new BufferedImage[4];
    BufferedImage[] state4RightFrames = new BufferedImage[4];
    
    BufferedImage[] idleFrontFrames = new BufferedImage[2];
    BufferedImage[] idleBackFrames  = new BufferedImage[2];
    BufferedImage[] idleLeftFrames  = new BufferedImage[2];
    BufferedImage[] idleRightFrames = new BufferedImage[2];

    int idleAnimTimer = 0;
    int idleFrameIndex = 0;
    private int deadFrameIndex = 0, deadAnimTimer = 0;
    private static final int DEAD_FRAME_COUNT    = 3;
    private static final int DEAD_FRAME_DURATION = 8;
    
    // --- ANIMASI WEAPON ---
    BufferedImage[] wpState1Up = new BufferedImage[4];
    BufferedImage[] wpState1Down = new BufferedImage[4];
    BufferedImage[] wpState1Left = new BufferedImage[4];
    BufferedImage[] wpState1Right = new BufferedImage[4];
    
    BufferedImage[] wpState2Up = new BufferedImage[4];
    BufferedImage[] wpState2Down = new BufferedImage[4];
    BufferedImage[] wpState2Left = new BufferedImage[4];
    BufferedImage[] wpState2Right = new BufferedImage[4];
    
    BufferedImage[] wpState3Up = new BufferedImage[4];
    BufferedImage[] wpState3Down = new BufferedImage[4];
    BufferedImage[] wpState3Left = new BufferedImage[4];
    BufferedImage[] wpState3Right = new BufferedImage[4];
    
    BufferedImage[] wpState4Up = new BufferedImage[4];
    BufferedImage[] wpState4Down = new BufferedImage[4];
    BufferedImage[] wpState4Left = new BufferedImage[4];
    BufferedImage[] wpState4Right = new BufferedImage[4];
    
    // Walk frames (6 frame per arah)
    BufferedImage[] walkFrontFrames = new BufferedImage[6];
    BufferedImage[] walkBackFrames = new BufferedImage[6];
    BufferedImage[] walkLeftFrames = new BufferedImage[6];
    BufferedImage[] walkRightFrames = new BufferedImage[6];
    
    // Run frames (3 frame per arah)
    BufferedImage[] runFrontFrames = new BufferedImage[3];
    BufferedImage[] runBackFrames = new BufferedImage[3];
    BufferedImage[] runLeftFrames = new BufferedImage[3];
    BufferedImage[] runRightFrames = new BufferedImage[3];
    
    // ===== JUMP FRAMES =====
    BufferedImage[] jumpUpFrames = new BufferedImage[2];
    BufferedImage[] jumpDownFrames = new BufferedImage[2];
    BufferedImage[] jumpLeftFrames = new BufferedImage[2];
    BufferedImage[] jumpRightFrames = new BufferedImage[2];
    
 // ===== DEAD FRAMES (3 frame per arah) =====
    BufferedImage[] deadFrontFrames = new BufferedImage[3];
    BufferedImage[] deadBackFrames  = new BufferedImage[3];
    BufferedImage[] deadLeftFrames  = new BufferedImage[3];
    BufferedImage[] deadRightFrames = new BufferedImage[3];
    
    BufferedImage hi;

    public int punchDamage = 10;
    public int attack;
    
    public int getTotalDamage() {
        int total = punchDamage;
        if (isWeaponEquipped && currentWeapon != null) {
            total += currentWeapon.damage;
        }
        return buffs.scaleDamage(total);
    }

    /** Max HP tanpa bonus dari boost potion. Ini yang disimpan ke save & dipakai stat UI. */
    public int getBaseMaxLife() {
        return maxLife - buffs.getMaxHpBonus();
    }
    
    public final int screenX;
    public final int screenY;
    public boolean isMoving = false;
    boolean staminaLocked = false;
    int jumpCooldown = 0;
    final int JUMP_COOLDOWN_TIME = 40;
    public double baseSpeed = 2.5;
    double sprintMultiplier = 2.0;
    
    // Stamina
    public int maxStamina = 500;
    public int stamina = maxStamina;
    double staminaDrain = 0.1;
    int staminaRegen = 3;
    boolean canSprint = true;
    boolean showStaminaBar = false;
    int animIndex = 0;
    boolean wasMoving = false;
    
    public Item.DeathTotem deathTotem = null;
    
    enum AttackState { NONE, WINDUP, HIT, RECOVER }
    AttackState attackState = AttackState.NONE;
    boolean wooshPlayed = false;
    
    enum JumpState { NONE, PREP, AIR, LAND }
    JumpState jumpState = JumpState.NONE;
    int jumpTimer = 0;
    String jumpDirection;

    public player(GamePanel gp, KeyHandler KeyH) {
        super(gp);
        this.KeyH = KeyH;
        
        screenX = gp.screenWidth/2 - (gp.tileSize / 2);
        screenY = gp.screenHeight/2 - (gp.tileSize / 2);
        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
        
        setDefaultValues();
        skillManager = new Skill.SkillManager(gp, this);
        
        currentWeapon = new Weapon("Battle Axe", "Axe"); 
        this.isWeaponEquipped = true; 
        
        currentCloth = new PlayerCloth("BaseCampCloth", isWeaponEquipped);
        
        sound = new SoundManager();
        getPlayerImage();
    }

    // ===== ARMOR API =====

    /** Pasang satu set armor (null = lepas, balik ke baju dasar). */
    public void setArmor(Item.ArmorSet set) {
        currentArmor = PlayerArmor.of(set);
        punchTintCache.clear(); // siluet afterimage SkillV tergantung armor yang dipakai
    }

    public Item.ArmorSet getArmorSet() {
        return currentArmor != null ? currentArmor.set : null;
    }

    /** Bonus defense flat dari armor yang dipakai (0 kalau gak pakai armor). */
    public int getArmorDefense() {
        return currentArmor != null ? currentArmor.set.defense : 0;
    }

    /** Pengurang damage persen dari armor yang dipakai (0 kalau gak pakai armor). */
    public int getArmorDamageReduction() {
        return currentArmor != null ? currentArmor.set.damageReduction : 0;
    }

    /** Gambar karakter menghadap depan (idle) lengkap dengan armor/baju, buat preview di backpack. */
    public void drawPreview(Graphics2D g2, int x, int y, int size) {
        BufferedImage body = idleFrontFrames[0];
        if (body == null) return;
        g2.drawImage(body, x, y, size, size, null);
        if (currentArmor != null) {
            BufferedImage layer = currentArmor.getBody("idle", 0, "down");
            if (layer != null) g2.drawImage(layer, x, y, size, size, null);
            layer = currentArmor.getHead("idle", 0, "down");
            if (layer != null) g2.drawImage(layer, x, y, size, size, null);
        } else if (currentCloth != null) {
            BufferedImage cloth = currentCloth.getIdleFrame("down");
            if (cloth != null) g2.drawImage(cloth, x, y, size, size, null);
        }
    }

    public Rectangle getInteractArea() {
        return new Rectangle(x - 20, y - 20, gp.tileSize + 40, gp.tileSize + 40);
    }

    public void setDefaultValues() {
        x = gp.tileSize * 13;
        y = gp.tileSize * 20;
        speed = baseSpeed;
        direction = "down";
    }

    public void syncWeaponImages() {
        if (currentWeapon != null) {
            wpState1Up    = currentWeapon.wpState1Up;
            wpState1Down  = currentWeapon.wpState1Down;
            wpState1Left  = currentWeapon.wpState1Left;
            wpState1Right = currentWeapon.wpState1Right;

            wpState2Up    = currentWeapon.wpState2Up;
            wpState2Down  = currentWeapon.wpState2Down;
            wpState2Left  = currentWeapon.wpState2Left;
            wpState2Right = currentWeapon.wpState2Right;

            wpState3Up    = currentWeapon.wpState3Up;
            wpState3Down  = currentWeapon.wpState3Down;
            wpState3Left  = currentWeapon.wpState3Left;
            wpState3Right = currentWeapon.wpState3Right;

            wpState4Up    = currentWeapon.wpState4Up;
            wpState4Down  = currentWeapon.wpState4Down;
            wpState4Left  = currentWeapon.wpState4Left;
            wpState4Right = currentWeapon.wpState4Right;
        }
        if (currentCloth != null) {
            currentCloth.loadWeaponStates();
        }
    }

    public void getPlayerImage() {
        try {
            for (int i = 0; i < 4; i++) {
                attackUpFrames[i]    = ImageIO.read(getClass().getResourceAsStream("/Aset/BackAttack (" + (i + 1) + ").png"));
                attackDownFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/FrontAttack (" + (i + 1) + ").png"));
                attackLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/LeftAttack (" + (i + 1) + ").png"));
                attackRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/RightAttack (" + (i + 1) + ").png"));

                swingUpFrames[i]    = ImageIO.read(getClass().getResourceAsStream("/Aset/BackSwing (" + (i + 1) + ").png"));
                swingDownFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/FrontSwing (" + (i + 1) + ").png"));
                swingLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/LeftSwing (" + (i + 1) + ").png"));
                swingRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/RightSwing (" + (i + 1) + ").png"));

                state3UpFrames[i]    = ImageIO.read(getClass().getResourceAsStream("/Aset/Back (" + (i + 1) + ").png"));
                state3DownFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/Front (" + (i + 1) + ").png"));
                state3LeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/Left (" + (i + 1) + ").png"));
                state3RightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/Right (" + (i + 1) + ").png"));

                state4UpFrames[i]    = ImageIO.read(getClass().getResourceAsStream("/Aset/Back (" + (i + 5) + ").png"));
                state4DownFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/Front (" + (i + 5) + ").png"));
                state4LeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/Left (" + (i + 5) + ").png"));
                state4RightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/Right (" + (i + 5) + ").png"));
            }
            
            for (int i = 0; i < 3; i++) {
            	deadFrontFrames[i] = readImg("/Aset/CharFrontDead (" + (i + 1) + ").png");
            	deadBackFrames[i]  = readImg("/Aset/CharBackDead (" + (i + 1) + ").png");
            	deadLeftFrames[i]  = readImg("/Aset/CharLeftDead (" + (i + 1) + ").png");
            	deadRightFrames[i] = readImg("/Aset/CharRightDead (" + (i + 1) + ").png");
            }

            syncWeaponImages();

            for (int i = 0; i < 2; i++) {
                jumpUpFrames[i]    = ImageIO.read(getClass().getResourceAsStream("/Aset/JumpBack" + (i + 1) + ".png"));
                jumpDownFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/JumpFront" + (i + 1) + ".png"));
                jumpLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/JumpLeft" + (i + 1) + ".png"));
                jumpRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/JumpRight" + (i + 1) + ".png"));
            }

            for (int i = 0; i < 6; i++) {
                walkFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/FrontWalk" + (i + 1) + ".png"));
                walkBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/BackWalk" + (i + 1) + ".png"));
                walkLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/LeftWalk" + (i + 1) + ".png"));
                walkRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/RightWalk" + (i + 1) + ".png"));
            }

            for (int i = 0; i < 3; i++) {
                runFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/FrontRun" + (i + 1) + ".png"));
                runBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/BackRun" + (i + 1) + ".png"));
                runLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/LeftRun" + (i + 1) + ".png"));
                runRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/RightRun" + (i + 1) + ".png"));
            }
            
            for (int i = 0; i < 2; i++) {
            idleFrontFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/FrontIdle1.png"));
            idleBackFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/BackIdle1.png"));
            idleLeftFrames[i]  = ImageIO.read(getClass().getResourceAsStream("/Aset/LeftIdle1.png"));
            idleRightFrames[i] = ImageIO.read(getClass().getResourceAsStream("/Aset/RightIdle1.png"));
            }

            hi = ImageIO.read(getClass().getResourceAsStream("/Aset/Hi!!.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        stand = idleFrontFrames[0];
    }
    
    private BufferedImage readImg(String path) {
        java.io.InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[player] Asset GA KETEMU: " + path);
            return null;
        }
        try {
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("[player] Gagal baca: " + path);
            return null;
        }
    }
    public void startPunch() {
        if (jumpState != JumpState.NONE) return;
        if (skillManager != null && skillManager.skillV.isActive) return; // SkillV: input dikunci

        if (attackState == AttackState.NONE) {
            if (comboWindowTimer > 0) {
                attackState = AttackState.WINDUP;
                attackTimer = 0;
                attackTotalTimer = 0;
                attackHit = false;
                wooshPlayed = false;
                comboWindowTimer = 0;
                if (isWeaponEquipped) {
                    attackType = (attackType % 4) + 1;
                } else {
                    attackType = (attackType % 2) + 1;
                }
            } else {
                attackState = AttackState.WINDUP;
                attackType = 1;
                attackTimer = 0;
                attackTotalTimer = 0;
                attackHit = false;
                wooshPlayed = false;
                hasComboInput = false;
                comboWindowTimer = 0;
            }
        } else {
            if (isWeaponEquipped && attackType < 4) {
                hasComboInput = true;
            } else if (!isWeaponEquipped && attackType < 2) {
                hasComboInput = true;
            }
        }
    }

    public void takeDamage(int dmg) {
        if (isDead) return;
        if (skillManager != null && skillManager.skillV.isActive) return; // SkillV: player kebal selama skill
        if (dodge > 0) {
            int chance = Math.min(dodge, 50);
            if ((int)(Math.random() * 100) < chance) return;
        }
        // defense flat (stat DEF + armor) dulu, baru pengurang persen dari armor
        int flat = dmg - def - getArmorDefense();
        int actualDmg = Math.max(1, (int) Math.round(flat * (1.0 - getArmorDamageReduction() / 100.0)));
        life -= actualDmg;
        isHit = true;
        hitFlashTimer = 10;
        noDamageTimer = 0; // reset regen delay tiap kali kena damage
        if (life <= 0) {
            life = 0;
            startDying();
        }
    }
    
    private void startDying() {
        isDead         = true;  // dipake buat nge-root player & block update normal selama animasi mati
        deadFrameIndex = 0;
        deadAnimTimer  = 0;
        buffs.clear();          // semua boost potion hilang kalau mati

        // batalin state lain biar ga numpuk/ganggu animasi mati
        attackState = AttackState.NONE;
        jumpState   = JumpState.NONE;
        isMoving    = false;
    }

    private void updateDeadAnim() {
        if (deadFrameIndex >= DEAD_FRAME_COUNT - 1) return; // udah di frame terakhir, nunggu klik respawn
        deadAnimTimer++;
        if (deadAnimTimer >= DEAD_FRAME_DURATION) {
            deadAnimTimer = 0;
            deadFrameIndex++;
        }
    }
    
    public Rectangle getHitbox() {
        return new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }

    public void onDeath() {
        Main.Log.d("PLAYER DEAD");
        x = gp.tileSize * 13;
        y = gp.tileSize * 20;
        life = maxLife;
        isDead         = false;
        deadFrameIndex = 0;
        deadAnimTimer  = 0;
    }
    
    /** Dipanggil dari tombol Respawn (DeathScreenUI) */
    public void respawnNow() {
        onDeath();
    }

    /** Dicek GamePanel tiap frame buat nentuin kapan munculin tombol respawn */
    public boolean isDeathAnimDone() {
        return isDead && deadFrameIndex >= DEAD_FRAME_COUNT - 1;
    }

    public void drawHPBar(Graphics2D g2) {
        int barWidth = 40;
        int barHeight = 5;
        int x = screenX;
        int y = screenY - 10;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, barWidth, barHeight);
        int hpWidth = (int)((double) life / maxLife * barWidth);
        g2.setColor(Color.RED);
        g2.fillRect(x, y, hpWidth, barHeight);
    }

    public void checkLevelUp() {
        if (exp >= nextLevelExp) {
            level++;
            exp = exp - nextLevelExp;
            nextLevelExp = nextLevelExp + 100;
            attributePoints += 3;
            Main.Log.d("LEVEL UP! Sekarang Level " + level);
        }
    }

    /**
     * Koin PASTI didapat setiap kill monster, langsung masuk backpack.
     * Jumlahnya diatur lewat Entity.coinDrop (default 1).
     */
    public void giveCoinReward(Entity target) {
        // pion summonan boss (noDrop) nggak ngasih koin, biar gak bisa di-farm
        if (target instanceof Skeleton && ((Skeleton) target).noDrop) return;

        int amount = Math.max(1, target.coinDrop);
        Item.ItemManager im = Item.ItemManager.get();
        im.add(im.COIN, amount);
        if (gp.speechBubble != null) gp.speechBubble.show("+" + amount + " Coin");
    }

    /**
     * Roll drop Small/Medium HP Potion tiap monster mati (peluangnya di ItemManager).
     * Pion summonan boss (noDrop) dilewati, sama kayak koin, biar gak bisa di-farm.
     */
    public void givePotionReward(Entity target) {
        if (target instanceof Skeleton && ((Skeleton) target).noDrop) return;
        Item.ItemManager.get().rollPotionDrops(gp);
    }

    public void killMonster(Entity target) {
        this.exp += target.getExpValue;
        checkLevelUp();
        giveCoinReward(target);
        givePotionReward(target);
        // ── TAMBAHAN: trigger capture Death Totem ──
        if (gp.deathTotemManager != null) {
            gp.deathTotemManager.tryCapture(target);
        }
    }

    int walkFrameIndex = 0;
    int runFrameIndex  = 0;
    final int[] RUN_ANIM_ORDER = {0, 1, 2, 1};
    int runAnimCycle = 0;
    int walkAnimTimer  = 0;
    int runAnimTimer  = 0;
    boolean isSprinting = false;

    public void update() {
        buffs.update();
        this.attack = getTotalDamage();
        gp.initInspectButtons();
        if (jumpCooldown > 0) jumpCooldown--;
        if (isHit) {
            hitFlashTimer--;
            if (hitFlashTimer <= 0) isHit = false;
        }
     // ===== HP REGEN (mulai 5 detik tanpa kena damage, nambah sekali tiap 1 detik) =====
        if (!isDead && life < maxLife) {
            noDamageTimer++;
            if (noDamageTimer >= REGEN_DELAY_FRAMES) {
                regenTickTimer++;
                if (regenTickTimer >= 60) { // 1 detik
                    regenTickTimer = 0;
                    int regenAmount = (int) Math.round(maxLife * REGEN_PERCENT_PER_SECOND);
                    life += regenAmount;
                    if (life > maxLife) life = maxLife;
                }
            }
        } else {
            regenTickTimer = 0;
        }

        skillManager.update();
        if (KeyH.vPressed) {
            skillManager.activateV();
            KeyH.vPressed = false;
        }

        // SkillV lagi jalan: input dikunci (gerak, serang, lompat, skill lain dilewati)
        if (skillManager.skillV.isActive) {
            KeyH.zPressed = false;
            KeyH.xPressed = false;
            KeyH.cPressed = false;
            isMoving = false;
            return;
        }

        if (KeyH.zPressed) {
            skillManager.activateZ();
            KeyH.zPressed = false;
        }
        if (KeyH.xPressed) {
            skillManager.activateX();
            KeyH.xPressed = false;
        }
        if (KeyH.cPressed) {
            skillManager.toggleC();
            KeyH.cPressed = false;
        }

        // ── Tombol T untuk spawn/recall pion DeathTotem ──
        if (KeyH.tPressed) {
            if (gp.deathTotemManager != null) {
                gp.deathTotemManager.trySpawn();
            }
            KeyH.tPressed = false;
        }

        // ===== JUMP INPUT =====
        int jumpStaminaCost = 120;
        if (KeyH.spacePressed && jumpState == JumpState.NONE && stamina >= jumpStaminaCost && jumpCooldown == 0) {
            staminaLocked = true;
            showStaminaBar = true;
            stamina -= jumpStaminaCost;
            jumpState = JumpState.PREP;
            jumpTimer = 0;
            jumpDirection = direction;
            jumpCooldown = JUMP_COOLDOWN_TIME;
        }

        // ===== SPRINT + STAMINA =====
        double currentBaseSpeed = buffs.scaleSpeed(baseSpeed); // sudah termasuk boost dari Speed Potion
        if (KeyH.shiftPressed && stamina > 0 && canSprint && (KeyH.upPressed || KeyH.downPressed || KeyH.leftPressed || KeyH.rightPressed)) {
            speed = currentBaseSpeed * sprintMultiplier;
            stamina -= staminaDrain;
            showStaminaBar = true;
            if (stamina <= 0) {
                stamina = 0;
                canSprint = false;
            }
        } else {
            speed = currentBaseSpeed;
            if (stamina < maxStamina && jumpState == JumpState.NONE) stamina += staminaRegen;
            if (stamina >= maxStamina) {
                stamina = maxStamina;
                canSprint = true;
                if (!staminaLocked) showStaminaBar = false;
            }
        }

        // ===== ATTACK STATE MACHINE =====
        if (attackState != AttackState.NONE) {
            attackTimer++;
            attackTotalTimer++;
            if (buffs.consumeExtraAttackTick()) { // Attack Speed Potion: animasi serangan lebih cepat
                attackTimer++;
                attackTotalTimer++;
            }
            attackFrameIndex = attackTotalTimer / 5;
            if (attackFrameIndex > 3) attackFrameIndex = 3;

            switch (attackState) {
                case WINDUP:
                    if (!wooshPlayed) {
                        sound.playWoosh();
                        wooshPlayed = true;
                    }
                    if (attackTimer >= 4) {
                        attackTimer = 0;
                        attackState = AttackState.HIT;
                    }
                    break;
                case HIT:
                    if (!attackHit && attackTimer >= 4) {
                        Rectangle attackBox = getAttackArea();
                        boolean hitSomething = false;

                        // HIT MONSTER
                        if (gp.monsters != null) {
                            for (Entity m : gp.monsters) {
                                if (m == null || m.isDead) continue;
                                Rectangle mBox = new Rectangle(m.x + m.solidArea.x, m.y + m.solidArea.y, m.solidArea.width, m.solidArea.height);
                                if (attackBox.intersects(mBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    m.takeDamage(damageDealt);
                                    hitSomething = true;
                                    attackHit = true;
                                    if (currentWeapon != null) {
                                        currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                                    } else {
                                        sound.playPunch();
                                    }
                                    break;
                                }
                            }
                        }

                        // HIT SKELETON DI DOMAIN
                        if (!attackHit && gp.domainManager != null) {
                            for (Entitas.Skeleton sk : gp.domainManager.getSkeletons()) {
                                if (sk == null || sk.isDead) continue;
                                Rectangle skBox = new Rectangle(sk.x + sk.solidArea.x, sk.y + sk.solidArea.y, sk.solidArea.width, sk.solidArea.height);
                                if (attackBox.intersects(skBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    sk.takeDamage(damageDealt);
                                    sk.knockbackFrom(this);
                                    hitSomething = true;
                                    attackHit = true;
                                    if (currentWeapon != null) currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                                    else sound.playPunch();
                                    break;
                                }
                            }
                        }
                        
                        
                     // HIT GOLEMITE DI DOMAIN
                        if (!attackHit && gp.domainManager != null) {
                            for (Entitas.Golemite gl : gp.domainManager.getGolemites()) {
                                if (gl == null || !gl.isAlive()) continue;
                                Rectangle glBox = new Rectangle(gl.x + gl.solidArea.x, gl.y + gl.solidArea.y, gl.solidArea.width, gl.solidArea.height);
                                if (attackBox.intersects(glBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    gl.takeDamage(damageDealt);
                                    hitSomething = true;
                                    attackHit = true;
                                    if (currentWeapon != null) currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                                    else sound.playPunch();
                                    break;
                                }
                            }
                        }
                     // HIT SKELETON SUMMONAN BOSS (di dalam boss room) — NEW
                        if (!attackHit && gp.domainManager != null) {
                            for (Entitas.Skeleton sk : gp.domainManager.getRoomSkeletons()) {
                                if (sk == null || sk.isDead) continue;
                                Rectangle skBox = new Rectangle(sk.x + sk.solidArea.x, sk.y + sk.solidArea.y, sk.solidArea.width, sk.solidArea.height);
                                if (attackBox.intersects(skBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    sk.takeDamage(damageDealt);
                                    sk.knockbackFrom(this);
                                    hitSomething = true;
                                    attackHit = true;
                                    if (currentWeapon != null) currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                                    else sound.playPunch();
                                    break;
                                }
                            }
                        }

                        // HIT BOSS SKELETON KING
                        if (!attackHit && gp.domainManager != null && gp.domainManager.getBossSkeleton() != null) {
                            Entitas.SkeletonKing king = gp.domainManager.getBossSkeleton();
                            if (king.alive) {
                                Rectangle kingBox = new Rectangle(king.x + king.solidArea.x, king.y + king.solidArea.y, king.solidArea.width, king.solidArea.height);
                                if (attackBox.intersects(kingBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    king.takeDamage(damageDealt);
                                    hitSomething = true;
                                    attackHit = true;
                                    if (currentWeapon != null) currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                                    else sound.playPunch();
                                }
                            }
                        }

                        // HIT NPC
                        if (!attackHit && gp.npc != null) {
                            for (Entity npc : gp.npc) {
                                if (npc == null || npc.isDead) continue;
                                Rectangle npcBox = new Rectangle(npc.x + npc.solidArea.x, npc.y + npc.solidArea.y, npc.solidArea.width, npc.solidArea.height);
                                if (attackBox.intersects(npcBox)) {
                                    int currentAtk = getTotalDamage();
                                    int damageDealt = (attackType >= 2) ? (int)(currentAtk * 1.5) : currentAtk;
                                    npc.takeDamage(damageDealt);
                                    npc.applyKnockback(direction);
                                    hitSomething = true;
                                    attackHit = true;
                                    break;
                                }
                            }
                        }

                        // HIT BREAKABLE
                        if (!attackHit && gp.tileM.breakables != null) {
                            for (breakable b : gp.tileM.breakables) {
                                if (b == null || b.destroyed) continue;
                                if (attackBox.intersects(b.solidArea)) {
                                    b.takeDamage(getTotalDamage(), gp);
                                    hitSomething = true;
                                    attackHit = true;
                                    break;
                                }
                            }
                        }

                        if (hitSomething) {
                            if (currentWeapon != null) {
                                currentWeapon.playAttackSound(attackType, isWeaponEquipped);
                            } else {
                                sound.playPunch();
                            }
                        }
                    }

                    if (attackTimer >= 5) {
                        attackTimer = 0;
                        attackState = AttackState.RECOVER;
                    }
                    break;
                case RECOVER:
                    int recoverLimit = isWeaponEquipped ? 10 : 5;
                    if (attackTimer >= recoverLimit) {
                        if (hasComboInput) {
                            attackState = AttackState.WINDUP;
                            attackTimer = 0;
                            attackTotalTimer = 0;
                            attackHit = false;
                            wooshPlayed = false;
                            hasComboInput = false;
                            attackType++;
                        } else {
                            attackState = AttackState.NONE;
                            comboWindowTimer = 25;
                            attackTimer = 0;
                            attackTotalTimer = 0;
                            attackFrameIndex = 0;
                        }
                    }
                    break;
            }
            return;
        } else {
            if (comboWindowTimer > 0) {
                comboWindowTimer--;
                if (comboWindowTimer == 0) {
                    attackType = 0;
                }
            }
        }
        
        if (isDead) {
            updateDeadAnim();
            return;
        }
        this.attack = getTotalDamage();
        gp.initInspectButtons();

        // ===== JUMP STATE =====
     // ===== JUMP STATE =====
        if (jumpState != JumpState.NONE) {
            showStaminaBar = true;
            staminaLocked = true;
            direction = jumpDirection;
            collisionOn = true;

            switch (jumpState) {
                case PREP:
                    jumpTimer++;
                    if (jumpTimer > 10) {
                        jumpTimer = 0;
                        jumpState = JumpState.AIR;
                    }
                    break;
                case AIR:
                    jumpTimer++;
                    int jdx = 0, jdy = 0, jumpSpeed = 9;
                    switch (direction) {
                        case "up":    jdy = -jumpSpeed; break;
                        case "down":  jdy =  jumpSpeed; break;
                        case "left":  jdx = -jumpSpeed; break;
                        case "right": jdx =  jumpSpeed; break;
                    }
                    collisionOn = false;
                    gp.colision.checkObject(this, jdx, jdy);
                    for (int i = 0; i < gp.npc.length; i++) {
                        if (gp.npc[i] != null) gp.colision.checkEntity(this, gp.npc[i], jdx, jdy);
                    }
                    if (gp.monsters != null) {
                        for (Entity m : gp.monsters) {
                            if (m != null && !m.isDead) gp.colision.checkEntity(this, m, jdx, jdy);
                        }
                    }

                 // 🔴 FIX: Cek tabrakan dengan GolemMinion saat melompat
                    if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
                        for (Entity gm : gp.deathTotemManager.getMinions()) {
                            if (gm instanceof Tamed.GolemMinion && gm.alive) {
                                gp.colision.checkEntity(this, gm, jdx, jdy);
                            }
                        }
                    }

                    if (!collisionOn) { x += jdx; y += jdy; }
                    if (jumpTimer > 15) { jumpTimer = 0; jumpState = JumpState.LAND; }
                    break;
                case LAND:
                    jumpTimer++;
                    if (jumpTimer > 8) {
                        jumpTimer = 0;
                        jumpState = JumpState.NONE;
                        staminaLocked = false;
                    }
                    break;
            }
            return;
        }

     // ===== MOVEMENT =====
        int dx = 0, dy = 0;
        boolean moving = false;
        if (KeyH.upPressed)    { dy -= speed; direction = "up";   moving = true; }
        if (KeyH.downPressed)  { dy += speed; direction = "down";  moving = true; }
        if (KeyH.leftPressed)  { dx -= speed; direction = "left";  moving = true; }
        if (KeyH.rightPressed) { dx += speed; direction = "right"; moving = true; }

        if (gp.gameState == GamePanel.GameState.OUTSIDE) {
            collisionOn = false;
            gp.colision.checkObject(this, dx, dy);
            for (int i = 0; i < gp.npc.length; i++) {
                if (gp.npc[i] != null) gp.colision.checkEntity(this, gp.npc[i], dx, dy);
            }
            
            if (gp.monsters != null) {
                for (Entity m : gp.monsters) {
                    if (m != null && !m.isDead) {
                        gp.colision.checkEntity(this, m, dx, dy);
                    }
                }
            }
            
            // 🔴 FIX: Menggunakan dx dan dy (bukan jdx/jdy) agar deteksi jalan normal berfungsi
            if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
                for (Entity gm : gp.deathTotemManager.getMinions()) {
                    if (gm instanceof Tamed.GolemMinion && gm.alive) {
                        gp.colision.checkEntity(this, gm, dx, dy);
                    }
                }
            }
            
            if (!collisionOn) { x += dx; y += dy; }
            
        } else if (gp.gameState == GamePanel.GameState.INSIDE) {
            collisionOn = false;
            Rectangle nextPlayerBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
            if (gp.houseManager.checkCollision(nextPlayerBox)) collisionOn = true;
            
            // 🔴 FIX: Tambahkan deteksi minion saat berada di dalam rumah (INSIDE)
            if (!collisionOn && gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
                for (Entity gm : gp.deathTotemManager.getMinions()) {
                    if (gm instanceof Tamed.GolemMinion && gm.alive) {
                        gp.colision.checkEntity(this, gm, dx, dy);
                    }
                }
            }
            
            if (!collisionOn) { x += dx; y += dy; }        
            
        } else if (gp.gameState == GamePanel.GameState.DOMAIN) {
            collisionOn = false;

            Rectangle nextPlayerBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);

            if (gp.domainManager.checkCollision(nextPlayerBox)) {
                collisionOn = true;
            }

            if (!collisionOn && gp.domainManager != null && gp.domainManager.getSkeletons() != null) {
                for (Entitas.Skeleton sk : gp.domainManager.getSkeletons()) {
                    if (sk != null && !sk.isDead) { 
                        Rectangle skBox = new Rectangle(sk.x + sk.solidArea.x, sk.y + sk.solidArea.y, sk.solidArea.width, sk.solidArea.height);
                        if (nextPlayerBox.intersects(skBox)) {
                            collisionOn = true;
                            break;
                        }
                    }
                }
            }
            
            if (!collisionOn && gp.domainManager != null) {
                for (Entitas.Golemite gl : gp.domainManager.getGolemites()) {
                    if (gl == null || !gl.isAlive()) continue;
                    Rectangle glBox = new Rectangle(gl.x + gl.solidArea.x, gl.y + gl.solidArea.y, gl.solidArea.width, gl.solidArea.height);
                    if (nextPlayerBox.intersects(glBox)) {
                        collisionOn = true;
                        break;
                    }
                }
            }
            
            if (!collisionOn && gp.monsters != null) {
                for (Entity m : gp.monsters) {
                    if (m != null && !m.isDead) {
                        Rectangle mBox = new Rectangle(m.x + m.solidArea.x, m.y + m.solidArea.y, m.solidArea.width, m.solidArea.height);
                        if (nextPlayerBox.intersects(mBox)) {
                            collisionOn = true;
                            break;
                        }
                    }
                }
            }

            // 🔴 FIX: Tambahin collision antara player dan SkeletonKing (boss), biar gak saling tembus
            if (!collisionOn && gp.domainManager != null && gp.domainManager.getBossSkeleton() != null) {
                Entitas.SkeletonKing king = gp.domainManager.getBossSkeleton();
                if (king.alive) {
                    Rectangle kingBox = new Rectangle(king.x + king.solidArea.x, king.y + king.solidArea.y, king.solidArea.width, king.solidArea.height);
                    if (nextPlayerBox.intersects(kingBox)) {
                        collisionOn = true;
                    }
                }
            }

            // 🔴 FIX: Satukan sistem menggunakan checkEntity bawaan engine agar presisi
            if (!collisionOn && gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
                for (Entity gm : gp.deathTotemManager.getMinions()) {
                    if (gm instanceof Tamed.GolemMinion && gm.alive) {
                        gp.colision.checkEntity(this, gm, dx, dy);
                    }
                }
            }

            if (!collisionOn) { 
                x += dx; 
                y += dy; 
            }
        }

        wasMoving = moving;
        isMoving = moving;

        // ===== ANIMASI WALK / RUN =====
        isSprinting = KeyH.shiftPressed && moving && stamina > 0 && canSprint;
        if (moving) {
            if (isSprinting) {
                runAnimTimer++;
                if (runAnimTimer >= 6) {
                    runAnimCycle = (runAnimCycle + 1) % RUN_ANIM_ORDER.length;
                    runFrameIndex = RUN_ANIM_ORDER[runAnimCycle];
                    runAnimTimer = 0;
                }
                walkFrameIndex = 0;
            } else {
                walkAnimTimer++;
                if (walkAnimTimer >= 8) {
                    walkFrameIndex = (walkFrameIndex + 1) % 6;
                    walkAnimTimer = 0;
                }
                runFrameIndex = 0;
            }
        } else {
            walkFrameIndex = 0;
            runAnimCycle = 0;
            runFrameIndex = 0;
            walkAnimTimer = 0;
            runAnimTimer = 0;
            
            idleAnimTimer++;
            if (idleAnimTimer >= 10) {
                idleFrameIndex = (idleFrameIndex + 1) % idleFrontFrames.length;
                idleAnimTimer = 0;
            }
        }
        wasMoving = moving;
    }

    public Rectangle getAttackArea() {
        int reach = 15;
        if (attackType >= 2) {
            switch (direction) {
                case "up":    return new Rectangle(x - 12, y - reach, gp.tileSize + 24, reach);
                case "down":  return new Rectangle(x - 12, y + gp.tileSize, gp.tileSize + 24, reach);
                case "left":  return new Rectangle(x - reach, y - 12, reach, gp.tileSize + 24);
                case "right": return new Rectangle(x + gp.tileSize, y - 12, reach, gp.tileSize + 24);
            }
        } else {
            switch (direction) {
                case "up":    return new Rectangle(x, y - reach, gp.tileSize, reach);
                case "down":  return new Rectangle(x, y + gp.tileSize, gp.tileSize, reach);
                case "left":  return new Rectangle(x - reach, y, reach, gp.tileSize);
                case "right": return new Rectangle(x + gp.tileSize, y, reach, gp.tileSize);
            }
        }
        return null;
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;
        drawHPBar(g2);

        if (isDead) {
            int safeIdx = Math.min(deadFrameIndex, DEAD_FRAME_COUNT - 1);
            switch (direction) {
                case "down":  image = deadFrontFrames[safeIdx]; break;
                case "up":    image = deadBackFrames[safeIdx];  break;
                case "left":  image = deadLeftFrames[safeIdx];  break;
                case "right": image = deadRightFrames[safeIdx]; break;
            }

            int drawSize = gp.tileSize * 2;
            int drawX = screenX - gp.tileSize / 2;
            int drawY = screenY - (gp.tileSize / 3);

            drawShadow(g2);

            if (image != null) {
                g2.drawImage(image, drawX, drawY, drawSize, drawSize, null);
            }

            if (currentArmor != null) {
                BufferedImage armorBody = currentArmor.getBody("dead", safeIdx, direction);
                if (armorBody != null) g2.drawImage(armorBody, drawX, drawY, drawSize, drawSize, null);
                BufferedImage armorHead = currentArmor.getHead("dead", safeIdx, direction);
                if (armorHead != null) g2.drawImage(armorHead, drawX, drawY, drawSize, drawSize, null);
            } else if (currentCloth != null) {
                BufferedImage clothImage = currentCloth.getDeadFrame(safeIdx, direction);
                if (clothImage != null) {
                    g2.drawImage(clothImage, drawX, drawY, drawSize, drawSize, null);
                }
            }
            return;
        }

        // SkillV: frame attack di-loop oleh skill (frame-nya diatur SkillV, kita tinggal gambar)
        if (skillManager.skillV.isPunching()) {
            skillManager.draw(g2); // afterimage di belakang player
            drawShadow(g2);
            drawPunchFrame(g2, skillManager.skillV.getPunchFrame(), direction, screenX, screenY, 1f, null);
            return;
        }


        if (attackState != AttackState.NONE) {
            if (attackType == 1) {
                switch (direction) {
                    case "up":    image = attackUpFrames[attackFrameIndex];    break;
                    case "down":  image = attackDownFrames[attackFrameIndex];  break;
                    case "left":  image = attackLeftFrames[attackFrameIndex];  break;
                    case "right": image = attackRightFrames[attackFrameIndex]; break;
                }
            } else if (attackType == 2) {
                switch (direction) {
                    case "up":    image = swingUpFrames[attackFrameIndex];    break;
                    case "down":  image = swingDownFrames[attackFrameIndex];  break;
                    case "left":  image = swingLeftFrames[attackFrameIndex];  break;
                    case "right": image = swingRightFrames[attackFrameIndex]; break;
                }
            } else if (attackType == 3) {
                switch (direction) {
                    case "up":    image = state3UpFrames[attackFrameIndex];    break;
                    case "down":  image = state3DownFrames[attackFrameIndex];  break;
                    case "left":  image = state3LeftFrames[attackFrameIndex];  break;
                    case "right": image = state3RightFrames[attackFrameIndex]; break;
                }
            } else if (attackType == 4) {
                switch (direction) {
                    case "up":    image = state4UpFrames[attackFrameIndex];    break;
                    case "down":  image = state4DownFrames[attackFrameIndex];  break;
                    case "left":  image = state4LeftFrames[attackFrameIndex];  break;
                    case "right": image = state4RightFrames[attackFrameIndex]; break;
                }
            }
 
            int attackDrawSize = gp.tileSize * 2;
            int attackDrawX    = screenX - gp.tileSize / 2;
            int attackDrawY    = screenY - (gp.tileSize / 3);
 
            drawShadow(g2);
 
            if (image != null) {
                g2.drawImage(image, attackDrawX, attackDrawY, attackDrawSize, attackDrawSize, null);
            }
 
            if (currentArmor != null) {
                BufferedImage armorBody = currentArmor.getBody(PlayerArmor.attackState(attackType), attackFrameIndex, direction);
                if (armorBody != null) {
                    g2.drawImage(armorBody, attackDrawX, attackDrawY, attackDrawSize, attackDrawSize, null);
                }
            } else if (currentCloth != null) {
                boolean canDrawCloth = (attackType <= 2) || (isWeaponEquipped && currentCloth.hasWeaponStates());
                if (canDrawCloth) {
                    BufferedImage clothImage = currentCloth.getFrame(attackType, attackFrameIndex, direction);
                    if (clothImage != null) {
                        g2.drawImage(clothImage, attackDrawX, attackDrawY, attackDrawSize, attackDrawSize, null);
                    }
                }
            }
 
            if (isWeaponEquipped && currentWeapon != null) {
                BufferedImage weaponImage = null;
                if (attackType == 1) {
                    switch (direction) {
                        case "up":    weaponImage = currentWeapon.wpState1Up[attackFrameIndex];    break;
                        case "down":  weaponImage = currentWeapon.wpState1Down[attackFrameIndex];  break;
                        case "left":  weaponImage = currentWeapon.wpState1Left[attackFrameIndex];  break;
                        case "right": weaponImage = currentWeapon.wpState1Right[attackFrameIndex]; break;
                    }
                } else if (attackType == 2) {
                    switch (direction) {
                        case "up":    weaponImage = currentWeapon.wpState2Up[attackFrameIndex];    break;
                        case "down":  weaponImage = currentWeapon.wpState2Down[attackFrameIndex];  break;
                        case "left":  weaponImage = currentWeapon.wpState2Left[attackFrameIndex];  break;
                        case "right": weaponImage = currentWeapon.wpState2Right[attackFrameIndex]; break;
                    }
                } else if (attackType == 3) {
                    switch (direction) {
                        case "up":    weaponImage = currentWeapon.wpState3Up[attackFrameIndex];    break;
                        case "down":  weaponImage = currentWeapon.wpState3Down[attackFrameIndex];  break;
                        case "left":  weaponImage = currentWeapon.wpState3Left[attackFrameIndex];  break;
                        case "right": weaponImage = currentWeapon.wpState3Right[attackFrameIndex]; break;
                    }
                } else if (attackType == 4) {
                    switch (direction) {
                        case "up":    weaponImage = currentWeapon.wpState4Up[attackFrameIndex];    break;
                        case "down":  weaponImage = currentWeapon.wpState4Down[attackFrameIndex];  break;
                        case "left":  weaponImage = currentWeapon.wpState4Left[attackFrameIndex];  break;
                        case "right": weaponImage = currentWeapon.wpState4Right[attackFrameIndex]; break;
                    }
                }
 
                if (weaponImage != null) {
                    g2.drawImage(weaponImage, attackDrawX, attackDrawY, attackDrawSize, attackDrawSize, null);
                }
            }

            if (currentArmor != null) {
                BufferedImage armorHead = currentArmor.getHead(PlayerArmor.attackState(attackType), attackFrameIndex, direction);
                if (armorHead != null) {
                    g2.drawImage(armorHead, attackDrawX, attackDrawY, attackDrawSize, attackDrawSize, null);
                }
            }
            return;
        }

        skillManager.draw(g2);

        if (jumpState != JumpState.NONE) {
            int jumpFrameIndex = 0;
            switch (jumpState) {
                case PREP: jumpFrameIndex = 0; break;
                case AIR:  jumpFrameIndex = 1; break;
                case LAND: jumpFrameIndex = 0; break;
                default:   jumpFrameIndex = 0; break;
            }
            switch (jumpDirection) {
                case "up":    image = jumpUpFrames[jumpFrameIndex];    break;
                case "down":  image = jumpDownFrames[jumpFrameIndex];  break;
                case "left":  image = jumpLeftFrames[jumpFrameIndex];  break;
                case "right": image = jumpRightFrames[jumpFrameIndex]; break;
            }
            drawStaminaBar(g2);
            drawShadow(g2);

            int jumpDrawSize = gp.tileSize * 2;
            int jumpDrawX = screenX - gp.tileSize / 2;
            int jumpDrawY = screenY - (gp.tileSize / 3);
            if (jumpState == JumpState.AIR) jumpDrawY -= 16;

            g2.drawImage(image, jumpDrawX, jumpDrawY, jumpDrawSize, jumpDrawSize, null);

            if (currentArmor != null) {
                // sprite armor disimpan per index frame jump BODY (beda sama baju dasar yang indexnya kebalik)
                BufferedImage armorBody = currentArmor.getBody("jump", jumpFrameIndex, jumpDirection);
                if (armorBody != null) g2.drawImage(armorBody, jumpDrawX, jumpDrawY, jumpDrawSize, jumpDrawSize, null);
                BufferedImage armorHead = currentArmor.getHead("jump", jumpFrameIndex, jumpDirection);
                if (armorHead != null) g2.drawImage(armorHead, jumpDrawX, jumpDrawY, jumpDrawSize, jumpDrawSize, null);
            } else if (currentCloth != null) {
                int clothJumpFrameIndex = (jumpFrameIndex == 0) ? 1 : 0;
                BufferedImage clothJump = currentCloth.getJumpFrame(clothJumpFrameIndex, jumpDirection);
                if (clothJump != null) {
                    g2.drawImage(clothJump, jumpDrawX, jumpDrawY, jumpDrawSize, jumpDrawSize, null);
                }
            }
            return;
        }

        int drawSize = gp.tileSize * 2;
        int drawX = screenX - gp.tileSize / 2;
        int drawY = screenY - (gp.tileSize / 3);

        if (isSprinting) {
            switch (direction) {
                case "down":  image = runFrontFrames[runFrameIndex];  break;
                case "up":    image = runBackFrames[runFrameIndex];   break;
                case "left":  image = runLeftFrames[runFrameIndex];   break;
                case "right": image = runRightFrames[runFrameIndex];  break;
            }
        } else if (isMoving) {
            switch (direction) {
                case "down":  image = walkFrontFrames[walkFrameIndex]; break;
                case "up":    image = walkBackFrames[walkFrameIndex];  break;
                case "left":  image = walkLeftFrames[walkFrameIndex];  break;
                case "right": image = walkRightFrames[walkFrameIndex]; break;
                case "emote": image = hi; break;
            }
        } else {
            switch (direction) {
                case "down":  image = idleFrontFrames[idleFrameIndex]; break;
                case "up":    image = idleBackFrames[idleFrameIndex];  break;
                case "left":  image = idleLeftFrames[idleFrameIndex];  break;
                case "right": image = idleRightFrames[idleFrameIndex]; break;
                default:      image = idleFrontFrames[idleFrameIndex]; break;
            }
        }

        drawStaminaBar(g2);

        int shadowWidth  = gp.tileSize - 20;
        int shadowHeight = gp.tileSize / 4;
        int shadowX = drawX + (drawSize - shadowWidth) / 2;
        int shadowY = (screenY + gp.tileSize) - (shadowHeight / 2);

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);

        g2.drawImage(image, drawX, drawY, drawSize, drawSize, null);

        if (currentArmor != null) {
            String armorState = isSprinting ? "run" : (isMoving ? "walk" : "idle");
            int armorFrame = isSprinting ? runFrameIndex : (isMoving ? walkFrameIndex : 0);
            BufferedImage armorBody = currentArmor.getBody(armorState, armorFrame, direction);
            if (armorBody != null) g2.drawImage(armorBody, drawX, drawY, drawSize, drawSize, null);
        } else if (currentCloth != null) {
            BufferedImage clothImage = null;
            if (isSprinting) {
                clothImage = currentCloth.getRunFrame(runFrameIndex, direction);
            } else if (isMoving) {
                clothImage = currentCloth.getWalkFrame(walkFrameIndex, direction);
            } else {
                clothImage = currentCloth.getIdleFrame(direction);
            }
            if (clothImage != null)
                g2.drawImage(clothImage, drawX, drawY, drawSize, drawSize, null);
        }

        if (currentArmor != null) {
            String armorState = isSprinting ? "run" : (isMoving ? "walk" : "idle");
            int armorFrame = isSprinting ? runFrameIndex : (isMoving ? walkFrameIndex : 0);
            BufferedImage armorHead = currentArmor.getHead(armorState, armorFrame, direction);
            if (armorHead != null) g2.drawImage(armorHead, drawX, drawY, drawSize, drawSize, null);
        }
    }

    private void checkWorldCollision(int dx, int dy) {
        if (gp.gameState == GamePanel.GameState.DOMAIN) {
            Rectangle next = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy,
                                           solidArea.width, solidArea.height);
            if (gp.domainManager != null && gp.domainManager.checkCollision(next)) {
                collisionOn = true;
            }
        } else {
        	checkWorldCollision(dx, 0);
        }
    }
    
    public boolean isNotPunching() {
        return attackState == AttackState.NONE;
    }

    // ===== SKILL V (pukulan bertubi-tubi) =====
    private final java.util.HashMap<String, BufferedImage> punchTintCache = new java.util.HashMap<>();

    /** SkillV cuma boleh mulai kalau player hidup & lagi gak lompat. */
    public boolean canUseSkill() {
        return !isDead && jumpState == JumpState.NONE;
    }

    /** Batalin serangan/combo yang lagi jalan biar skill mulai dari pose bersih. */
    public void cancelAttack() {
        attackState = AttackState.NONE;
        attackTimer = 0;
        attackTotalTimer = 0;
        attackFrameIndex = 0;
        attackType = 0;
        comboWindowTimer = 0;
        hasComboInput = false;
        attackHit = false;
        wooshPlayed = false;
    }

    /**
     * Gambar satu frame pukulan tangan kosong (attackUp/Down/Left/RightFrames, index 0-3) beserta baju / armor.
     * (drawX, drawY) = pojok kiri-atas sel player di layar, sama kayak (screenX, screenY).
     * alpha < 1 buat bayangan; ghostTint != null mewarnai siluetnya (afterimage).
     */
    public void drawPunchFrame(Graphics2D g2, int frameIndex, String dir, int drawX, int drawY, float alpha, Color ghostTint) {
        if (alpha <= 0f) return;
        BufferedImage[] frames = punchFramesFor(dir);
        int idx = Math.max(0, Math.min(frames.length - 1, frameIndex));
        int size = gp.tileSize * 2;
        int px = drawX - gp.tileSize / 2;
        int py = drawY - (gp.tileSize / 3);

        java.awt.Composite old = g2.getComposite();
        if (alpha < 1f) g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.min(1f, alpha)));

        if (ghostTint == null) {
            drawPunchLayers(g2, frames, idx, dir, px, py, size);
        } else {
            g2.drawImage(getTintedPunchFrame(frames, idx, dir, ghostTint, size), px, py, null);
        }
        g2.setComposite(old);
    }

    private BufferedImage[] punchFramesFor(String dir) {
        if ("up".equals(dir))    return attackUpFrames;
        if ("left".equals(dir))  return attackLeftFrames;
        if ("right".equals(dir)) return attackRightFrames;
        return attackDownFrames;
    }

    // Urutan layer sama kayak serangan biasa: badan -> baju/armor -> helm armor (senjata sengaja gak ikut, ini pukulan tangan kosong)
    private void drawPunchLayers(Graphics2D g, BufferedImage[] frames, int idx, String dir, int px, int py, int size) {
        if (frames[idx] != null) g.drawImage(frames[idx], px, py, size, size, null);
        if (currentArmor != null) {
            BufferedImage armorBody = currentArmor.getBody("atk1", idx, dir);
            if (armorBody != null) g.drawImage(armorBody, px, py, size, size, null);
            BufferedImage armorHead = currentArmor.getHead("atk1", idx, dir);
            if (armorHead != null) g.drawImage(armorHead, px, py, size, size, null);
        } else if (currentCloth != null) {
            BufferedImage cloth = currentCloth.getFrame(1, idx, dir);
            if (cloth != null) g.drawImage(cloth, px, py, size, size, null);
        }
    }

    // Siluet berwarna untuk afterimage. Di-cache per (arah, frame, baju, armor, warna) biar gak bikin gambar baru tiap frame.
    private BufferedImage getTintedPunchFrame(BufferedImage[] frames, int idx, String dir, Color tint, int size) {
        String key = dir + ":" + idx + ":" + System.identityHashCode(currentCloth) + ":"
                + System.identityHashCode(currentArmor) + ":" + tint.getRGB();
        BufferedImage cached = punchTintCache.get(key);
        if (cached != null) return cached;

        BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gb = buf.createGraphics();
        drawPunchLayers(gb, frames, idx, dir, 0, 0, size);
        gb.setComposite(java.awt.AlphaComposite.SrcAtop); // warnai cuma bagian yang ada gambarnya
        gb.setColor(tint);
        gb.fillRect(0, 0, size, size);
        gb.dispose();

        if (punchTintCache.size() > 64) punchTintCache.clear();
        punchTintCache.put(key, buf);
        return buf;
    }

    public void drawStaminaBar(Graphics2D g2) {
        if (!showStaminaBar) return;
        int barWidth = 3;
        int barHeight = 40;
        int x = screenX - 10;
        int y = screenY + gp.tileSize / 2 - barHeight / 2;
        
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x, y, barWidth, barHeight, 5, 5);
        int filledHeight = (int)((double) stamina / maxStamina * barHeight);
        int fy = y + barHeight - filledHeight;
        g2.setColor(stamina > 200 ? new Color(0, 220, 0) : new Color(220, 50, 50));
        g2.fillRoundRect(x, fy, barWidth, filledHeight, 5, 5);
    }
}