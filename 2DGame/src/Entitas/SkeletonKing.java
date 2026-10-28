package Entitas;

import Item.ItemManager;
import SFX.SoundManager;
import Tamed.GolemMinion;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Paint;
import Main.GamePanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class SkeletonKing extends Entity {

    // ══════════════════════════════════════════════════════
    //  STATIC IMAGE CACHE — dimuat sekali, dipakai semua instance
    // ══════════════════════════════════════════════════════
    private static BufferedImage[] S_walkFront = new BufferedImage[6];
    private static BufferedImage[] S_walkBack  = new BufferedImage[6];
    private static BufferedImage[] S_walkLeft  = new BufferedImage[6];
    private static BufferedImage[] S_walkRight = new BufferedImage[6];

    private static BufferedImage[] S_idleFront = new BufferedImage[2];
    private static BufferedImage[] S_idleBack  = new BufferedImage[2];
    private static BufferedImage[] S_idleLeft  = new BufferedImage[2];
    private static BufferedImage[] S_idleRight = new BufferedImage[2];

    private static BufferedImage[] S_attackFront = new BufferedImage[7];
    private static BufferedImage[] S_attackBack  = new BufferedImage[7];
    private static BufferedImage[] S_attackLeft  = new BufferedImage[7];
    private static BufferedImage[] S_attackRight = new BufferedImage[7];
    
    private final SoundManager sfx = new SoundManager();

 // ── Skill: tiap 3x mukul berhasil, summon Skeleton ──
 private int hitStreak = 0;
 private final int SUMMON_HIT_THRESHOLD = 3;
 private final int SUMMON_COUNT = 2; // jumlah Skeleton yang muncul tiap trigger

    // ══════════════════════════════════════════════════════
    //  SKILL: SKELETON HAND — trigger tiap 10 detik
    // ══════════════════════════════════════════════════════
    private int handSkillTimer = 0;
    private final int HAND_SKILL_INTERVAL = 600; // 10 detik @ 60 FPS
    private final int HAND_SKILL_COUNT = 5;
    private final int HAND_SKILL_DAMAGE = 200;
    private final int HAND_SPAWN_INTERVAL = 25;  // jeda antar munculnya tiap tangan (~0.4 detik) — biar berurutan "ngejer"
    private final int HAND_SPAWN_JITTER = 50;    // NEW: diperbesar dari 20 -> 50, biar sebaran posisi tangan gak numpuk persis di player
    private final int HAND_RETURN_RADIUS = 150;  // radius King muncul lagi di sekitar player
    private final int HAND_RETURN_SAFE_BUFFER = 10; // NEW: jarak aman ekstra biar King gak overlap hitbox player pas muncul lagi

    private int handsSpawnedCount = 0;
    private int handSpawnSeqTimer = 0;

    private List<SkeletonHand> activeHands = new ArrayList<>();

    private int vanishFrameIndex = 0, vanishAnimTimer = 0; // menghilang: maju dari frame 0
    private int returnFrameIndex = 6, returnAnimTimer = 0; // muncul lagi: mundur dari frame 6
    // ── Semua delay animasi muncul/ngilang disamain & dipercepat (dulu semua 10, RETURN sempet 7) ──
    private final int RETURN_FRAME_DELAY = 5; // dipercepat dari 7 -> 5, animasi muncul lagi abis skill
    private final int SPAWN_FRAME_DELAY = 5;  // dipercepat dari 10 -> 5, animasi spawn awal boss
    private final int VANISH_FRAME_DELAY = 5; // dipercepat dari 10 -> 5, animasi ngilang buat skill
    private final int DEATH_FRAME_DELAY = 5;  // NEW: dipercepat dari 10 -> 5, animasi ngilang karna mati

    // Dead CUMA Front/Left/Right, gak ada Back
    private static BufferedImage[] S_deadFront = new BufferedImage[7];
    private static BufferedImage[] S_deadLeft  = new BufferedImage[7];
    private static BufferedImage[] S_deadRight = new BufferedImage[7];

    private static boolean imagesLoaded = false;

    public static synchronized void preloadImages() {
        if (imagesLoaded) return;
        try {
            Class<?> cls = SkeletonKing.class;
            for (int i = 0; i < 6; i++) {
                int f = i + 1;
                S_walkFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingFrontWalk (" + f + ").png"));
                S_walkBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingBackWalk ("  + f + ").png"));
                S_walkLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingLeftWalk ("  + f + ").png"));
                S_walkRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingRightWalk (" + f + ").png"));
            }
            for (int i = 0; i < 2; i++) {
                int f = i + 1;
                S_idleFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingFrontIdle (" + f + ").png"));
                S_idleBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingBackIdle ("  + f + ").png"));
                S_idleLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingLeftIdle ("  + f + ").png"));
                S_idleRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingRightIdle (" + f + ").png"));
            }
            for (int i = 0; i < 7; i++) {
                int f = i + 1;
                S_attackFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingFrontAttack (" + f + ").png"));
                S_attackBack[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingBackAttack ("  + f + ").png"));
                S_attackLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingLeftAttack ("  + f + ").png"));
                S_attackRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingRightAttack (" + f + ").png"));
            }
            for (int i = 0; i < 7; i++) {
                int f = i + 1;
                S_deadFront[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingFrontDead (" + f + ").png"));
                S_deadLeft[i]  = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingLeftDead ("  + f + ").png"));
                S_deadRight[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonKingRightDead (" + f + ").png"));
            }
            imagesLoaded = true;
            Main.Log.d("[SkeletonKing] Semua gambar berhasil di-preload.");

            // NEW: preload asset skill Skeleton Hand bareng-bareng juga
            SkeletonHand.preloadImages();
        } catch (Exception e) {
            System.out.println("[SkeletonKing] Gagal preload gambar: " + e.getMessage());
        }
    }

    // ── State machine ──
    private enum KingState { IDLE, WALK, ATTACK, DEATH, SPAWNING, HAND_VANISH, HAND_HIDDEN, HAND_RETURN }
    private KingState state = KingState.IDLE;

    private int walkFrameIndex = 0, walkAnimTimer = 0;
    private int idleFrameIndex = 0, idleAnimTimer = 0;
    private final int IDLE_FRAME_DELAY = 30; // 2 frame idle, ganti tiap 30 tick (~0.5 detik)
    private int attackFrameIndex = 0, attackTimer = 0, attackCooldown = 0;
    private final int ATTACK_FRAME_DELAY = 6; // dulu 6 (2x lebih lambat = lebih smooth)
    private boolean hasHitThisSwing = false;

    private int deathFrameIndex = 0, deathAnimTimer = 0;
    private boolean deathAnimDone = false;
    private boolean dropped = false;
    
    private int spawnFrameIndex = 6;
    private int spawnAnimTimer = 0;
    private boolean spawnAnimDone = false;

    // NOTE: ini nge-shadow Entity.alive, sengaja ngikutin pola Skeleton.java.
    // Kalau kelas lain mau cek boss masih hidup, WAJIB cast ke SkeletonKing dulu.
    public boolean alive = true;

    public int attackDamage = 100;
    public int detectionRange = 260;
    public int attackRange = 65;

    // ── Target lock: bisa Player, bisa juga GolemMinion (siapa yang paling deket) ──
    private Entity currentTarget = null;

    private final int drawScale = 5; // Boss digambar gede — tier boss

    public SkeletonKing(GamePanel gp) {
        super(gp);
        this.maxLife = 10000;
        this.life = maxLife;
        this.speed = 1.2;
        this.direction = "down";

        // ══════════════════════════════════════════════════════
        //  solidArea: lebar PERSIS 1 tileSize (biar center di anchor visual, offsetX = 0),
        //  tinggi ditaruh di area bawah sprite (kaki), bukan ngambang di tengah/atas.
        // ══════════════════════════════════════════════════════
        int hbWidth   = gp.tileSize;
        int hbHeight  = (int) (gp.tileSize * 1.2);
        int hbOffsetX = 0;                       // center: (tileSize/2) - (hbWidth/2) == 0 karena hbWidth == tileSize
        int hbOffsetY = (int) (gp.tileSize * 1.0); // taruh di area bawah sprite (kaki)
        this.solidArea = new Rectangle(hbOffsetX, hbOffsetY, hbWidth, hbHeight);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = 5000;
    }

    @Override
    public void update() {
    	 if (state == KingState.SPAWNING) { updateSpawnAnimation(); return; }
        if (state == KingState.DEATH) { updateDeathAnimation(); return; }

        // ── Skill Skeleton Hand: vanish -> hidden (tangan aktif) -> return ──
        if (state == KingState.HAND_VANISH) { updateActiveHands(); updateHandVanish(); return; }
        if (state == KingState.HAND_HIDDEN) { updateActiveHands(); updateHandSpawnSequence(); checkHandSkillDone(); return; }
        if (state == KingState.HAND_RETURN) { updateHandReturn(); return; }

        if (!alive) return;

        if (life <= 0) {
            alive = false;
            state = KingState.DEATH;
            deathFrameIndex = 0; deathAnimTimer = 0; deathAnimDone = false;
            this.solidArea.x = -9999; this.solidArea.y = -9999;
            activeHands.clear(); // NEW: bersih-bersih kalau boss mati pas lagi skill
            dropItems();
            gp.player.killMonster(this);
            return;
        }

        if (isHit) { hitFlashTimer--; if (hitFlashTimer <= 0) isHit = false; }
        if (attackCooldown > 0) attackCooldown--;

        // ── Timer skill Skeleton Hand: trigger tiap 10 detik ──
        handSkillTimer++;
        if (handSkillTimer >= HAND_SKILL_INTERVAL) {
            handSkillTimer = 0;
            triggerHandSkill();
            return;
        }

        int cx = x + solidArea.x + solidArea.width / 2;
        int cy = y + solidArea.y + solidArea.height / 2;

        // Cari target terdekat: Player ATAU GolemMinion yang lagi alive.
        Entity target = findNearestTarget(cx, cy);
        currentTarget = target;

        int tcx = target.x + target.solidArea.x + target.solidArea.width / 2;
        int tcy = target.y + target.solidArea.y + target.solidArea.height / 2;

        int distX = tcx - cx;
        int distY = tcy - cy;
        double dist = Math.sqrt((double) distX * distX + (double) distY * distY);

        if (dist <= attackRange && attackCooldown == 0) {
            faceTowards(distX, distY);
            if (state != KingState.ATTACK) {
                state = KingState.ATTACK;
                attackFrameIndex = 0;
                attackTimer = 0;
                hasHitThisSwing = false;
            }
        } else if (dist <= detectionRange) {
            faceTowards(distX, distY);

            int dx = 0, dy = 0;
            if (distX > 0) dx = 1; else if (distX < 0) dx = -1;
            if (distY > 0) dy = 1; else if (distY < 0) dy = -1;
            double moveSpeed = (dx != 0 && dy != 0) ? speed * 0.707 : speed;
            dx = (int) Math.round(dx * moveSpeed);
            dy = (int) Math.round(dy * moveSpeed);

            collisionOn = false;
            Rectangle nextBox = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
            boolean tileCollision = gp.domainManager.checkCollision(nextBox);
            if (gp.colision != null) gp.colision.checkPlayer(this);

            // Collision dua arah biar boss & GolemMinion gak saling tembus
            if (!collisionOn && gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
                for (Entity e : gp.deathTotemManager.getMinions()) {
                    if (e instanceof GolemMinion && e.alive) {
                        gp.colision.checkEntity(this, e, dx, dy);
                    }
                }
            }

            // FIX: cuma masuk state WALK kalau gerakannya beneran berhasil.
            // Kalau kehalang (tile/player/minion), diem di IDLE, jangan lanjut muter animasi jalan.
            boolean blocked = tileCollision || collisionOn;
            if (!blocked) {
                x += dx; y += dy;
                state = KingState.WALK;
            } else {
                state = KingState.IDLE;
            }
        } else {
            state = KingState.IDLE;
            currentTarget = null;
        }

        if (state == KingState.WALK) {
            walkAnimTimer++;
            if (walkAnimTimer >= 8) { walkFrameIndex = (walkFrameIndex + 1) % 6; walkAnimTimer = 0; }
        } else {
            walkFrameIndex = 0; walkAnimTimer = 0;
        }

        if (state == KingState.IDLE) {
            idleAnimTimer++;
            if (idleAnimTimer >= IDLE_FRAME_DELAY) { idleFrameIndex = (idleFrameIndex + 1) % 2; idleAnimTimer = 0; }
        } else {
            idleFrameIndex = 0; idleAnimTimer = 0;
        }

        if (state == KingState.ATTACK) {
            attackTimer++;
            if (attackTimer >= ATTACK_FRAME_DELAY) {
                attackTimer = 0;
                attackFrameIndex++;
                if (attackFrameIndex == 3 && !hasHitThisSwing) { // ~pertengahan animasi 7 frame
                    hitTarget();
                    hasHitThisSwing = true;
                    sfx.kingAttackSound();

                    hitStreak++;
                    if (hitStreak >= SUMMON_HIT_THRESHOLD) {
                        hitStreak = 0;
                        performSummonSkill();
                    }
                }
                if (attackFrameIndex >= 7) {
                    attackFrameIndex = 0;
                    state = KingState.IDLE;
                    attackCooldown = 75;
                    hasHitThisSwing = false;
                }
            }
        }
    }
    
    /** Dipanggil DomainManager tepat setelah boss di-instantiate & countdown kelar. */
    public void beginSpawnAnimation() {
        state = KingState.SPAWNING;
        spawnFrameIndex = 6;   // reverse death: mulai dari frame TERAKHIR
        spawnAnimTimer = 0;
        spawnAnimDone = false;
        alive = true;
        
        sfx.kingSpawnSound();
        sfx.spawnSkillSound();
    }
    
    private void updateSpawnAnimation() {
        if (spawnAnimDone) { state = KingState.IDLE; return; }
        spawnAnimTimer++;
        if (spawnAnimTimer >= SPAWN_FRAME_DELAY) { // FIX: dulu hardcode 10, konstantanya gak kepake
            spawnAnimTimer = 0;
            spawnFrameIndex--; // MUNDUR — kebalikan dari death yang maju
            if (spawnFrameIndex < 0) {
                spawnFrameIndex = 0;
                spawnAnimDone = true;
                state = KingState.IDLE; // spawn kelar, boss resmi aktif
            }
        }
    }

    // ══════════════════════════════════════════════════════
    //  SKILL: SKELETON HAND — muncul tiap 10 detik
    // ══════════════════════════════════════════════════════

    /** Trigger skill: boss mulai ngilang. Tangan baru mulai muncul begitu dia bener-bener ilang (HAND_HIDDEN). */
    private void triggerHandSkill() {
        state = KingState.HAND_VANISH;
        vanishFrameIndex = 0;
        vanishAnimTimer = 0;

        sfx.kingSpawnSound(); // "KingSkeletonSpawning.wav" — boss menghilang

        handsSpawnedCount = 0;
        handSpawnSeqTimer = 0;
        activeHands.clear();
    }

    /** Munculin 1 tangan di posisi player SAAT INI — dipanggil berurutan, jadi berasa "ngejer". */
    private void spawnNextHand() {
        if (handsSpawnedCount >= HAND_SKILL_COUNT) return;

        int jitterX = (int) (Math.random() * HAND_SPAWN_JITTER * 2 - HAND_SPAWN_JITTER);
        int jitterY = (int) (Math.random() * HAND_SPAWN_JITTER * 2 - HAND_SPAWN_JITTER);
        int hx = gp.player.x + jitterX;
        int hy = gp.player.y + jitterY;

        activeHands.add(new SkeletonHand(gp, hx, hy, HAND_SKILL_DAMAGE));
        handsSpawnedCount++;

        sfx.spawnSkillSound(); // "SkeletonSpawningSkill.wav" — tangan muncul dari tanah
    }

    /** Jalanin urutan spawn: tiap HAND_SPAWN_INTERVAL tick, munculin 1 tangan baru sampai kuota abis. */
    private void updateHandSpawnSequence() {
        if (handsSpawnedCount >= HAND_SKILL_COUNT) return;

        handSpawnSeqTimer++;
        if (handSpawnSeqTimer >= HAND_SPAWN_INTERVAL) {
            handSpawnSeqTimer = 0;
            spawnNextHand();
        }
    }

    private void updateActiveHands() {
        for (SkeletonHand hand : activeHands) {
            hand.update();
        }
        activeHands.removeIf(h -> h.finished);
    }

    private void updateHandVanish() {
        vanishAnimTimer++;
        if (vanishAnimTimer >= VANISH_FRAME_DELAY) { // FIX: dulu hardcode 10, konstantanya gak kepake
            vanishAnimTimer = 0;
            vanishFrameIndex++;
            if (vanishFrameIndex >= 7) {
                vanishFrameIndex = 6;
                // King resmi ngilang total: gak digambar, gak bisa nabrak/kena damage
                this.solidArea.x = -9999; this.solidArea.y = -9999;
                state = KingState.HAND_HIDDEN;

                // Tangan pertama langsung muncul begitu King ilang total
                spawnNextHand();
            }
        }
    }

    /** Baru boleh muncul lagi kalau kuota 5 tangan udah abis DAN semua animasinya udah kelar. */
    private void checkHandSkillDone() {
        if (handsSpawnedCount >= HAND_SKILL_COUNT && activeHands.isEmpty()) {
            beginHandReturn();
        }
    }

    private void beginHandReturn() {
        // Posisi muncul kembali: random di area dekat sekitar player
        double angle = Math.random() * Math.PI * 2;
        int dist = (int) (Math.random() * HAND_RETURN_RADIUS);
        this.x = gp.player.x + (int) (Math.cos(angle) * dist);
        this.y = gp.player.y + (int) (Math.sin(angle) * dist);

        this.solidArea.x = solidAreaDefaultX;
        this.solidArea.y = solidAreaDefaultY;

        state = KingState.HAND_RETURN;
        returnFrameIndex = 6; // reverse death: mulai dari frame terakhir
        returnAnimTimer = 0;

        sfx.kingSkill2Sound(); // "KingSkeletonSkill2" — boss muncul kembali
    }

    private void updateHandReturn() {
        returnAnimTimer++;
        if (returnAnimTimer >= RETURN_FRAME_DELAY) {
            returnAnimTimer = 0;
            returnFrameIndex--;
            if (returnFrameIndex < 0) {
                returnFrameIndex = 0;
                state = KingState.IDLE; // skill kelar total, boss balik aktif normal
            }
        }
    }
    
    private void drawBossUI(Graphics2D g2, int sx, int sy, int drawSize) {
        int barW = 140, barH = 14;
        int barX = sx + drawSize / 2 - barW / 2;
        int barY = sy - 30;

        // Nama boss
        g2.setFont(new Font("Serif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String name = "SKELETON KING";
        int nameW = fm.stringWidth(name);
        g2.setColor(Color.BLACK);
        g2.drawString(name, barX + barW / 2 - nameW / 2 + 1, barY - 5);
        g2.setColor(new Color(230, 210, 160));
        g2.drawString(name, barX + barW / 2 - nameW / 2, barY - 6);

        // Border luar gelap + border dalam perunggu (vibe boss)
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(barX - 3, barY - 3, barW + 6, barH + 6);
        g2.setColor(new Color(120, 90, 40));
        g2.drawRect(barX - 2, barY - 2, barW + 4, barH + 4);

        // Background bar kosong
        g2.setColor(new Color(40, 10, 10));
        g2.fillRect(barX, barY, barW, barH);

        // Fill HP pakai gradient merah
        double hpRatio = Math.max(0, (double) life / maxLife);
        int hpFill = (int) (barW * hpRatio);
        if (hpFill > 0) {
            GradientPaint hpGradient = new GradientPaint(
                barX, barY, new Color(255, 90, 60),
                barX, barY + barH, new Color(150, 20, 20)
            );
            Paint oldPaint = g2.getPaint();
            g2.setPaint(hpGradient);
            g2.fillRect(barX, barY, hpFill, barH);
            g2.setPaint(oldPaint);
        }

        // Tick pemisah tiap 25%
        g2.setColor(new Color(0, 0, 0, 120));
        for (int t = 1; t < 4; t++) {
            int tickX = barX + (barW * t / 4);
            g2.drawLine(tickX, barY, tickX, barY + barH);
        }

        // Highlight glossy tipis di atas
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRect(barX, barY, barW, barH / 3);

        // ── Progress bar skill summon (3 slot, di bawah HP bar) ──
        int skillBarW = barW, skillBarH = 7;
        int skillBarX = barX;
        int skillBarY = barY + barH + 5;

        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(skillBarX - 2, skillBarY - 2, skillBarW + 4, skillBarH + 4);
        g2.setColor(new Color(40, 40, 60));
        g2.fillRect(skillBarX, skillBarY, skillBarW, skillBarH);

        double skillRatio = (double) hitStreak / SUMMON_HIT_THRESHOLD;
        int skillFill = (int) (skillBarW * skillRatio);
        g2.setColor(new Color(80, 160, 255));
        g2.fillRect(skillBarX, skillBarY, skillFill, skillBarH);

        g2.setColor(new Color(0, 0, 0, 150));
        for (int t = 1; t < SUMMON_HIT_THRESHOLD; t++) {
            int tickX = skillBarX + (skillBarW * t / SUMMON_HIT_THRESHOLD);
            g2.drawLine(tickX, skillBarY, tickX, skillBarY + skillBarH);
        }
    }

    /**
     * Cari target paling deket buat boss ini: bisa Player, bisa juga GolemMinion.
     * Default balikin Player kalau gak ada golem yang lebih deket / gak ada golem sama sekali.
     */
    private Entity findNearestTarget(int cx, int cy) {
        Entity nearest = gp.player;
        int pcx = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2;
        int pcy = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        double minDist = Math.hypot(pcx - cx, pcy - cy);

        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity e : gp.deathTotemManager.getMinions()) {
                if (!(e instanceof GolemMinion) || !e.alive) continue;
                int ecx = e.x + e.solidArea.x + e.solidArea.width / 2;
                int ecy = e.y + e.solidArea.y + e.solidArea.height / 2;
                double d = Math.hypot(ecx - cx, ecy - cy);
                if (d < minDist) { minDist = d; nearest = e; }
            }
        }
        return nearest;
    }

    private void performSummonSkill() {
        sfx.spawnSkillSound();
        for (int i = 0; i < SUMMON_COUNT; i++) {
            int offsetX = (int) (Math.random() * 80 - 40);
            int offsetY = (int) (Math.random() * 80 - 40);
            gp.domainManager.spawnSkeletonInRoom(this.x + offsetX, this.y + offsetY);
        }
    }

    private void faceTowards(int distX, int distY) {
        if (Math.abs(distX) > Math.abs(distY)) direction = distX > 0 ? "right" : "left";
        else direction = distY > 0 ? "down" : "up";
    }

    private void updateDeathAnimation() {
        if (deathAnimDone) return; // boss gak respawn, tetep diem di frame terakhir
        deathAnimTimer++;
        if (deathAnimTimer >= DEATH_FRAME_DELAY) { // FIX: dulu hardcode 10
            deathAnimTimer = 0;
            deathFrameIndex++;
            if (deathFrameIndex >= 7) { deathFrameIndex = 6; deathAnimDone = true; }
        }
    }

    private void hitTarget() {
        if (currentTarget == null) return;
        Rectangle atk = getAttackBox();
        Rectangle tgt = new Rectangle(
            currentTarget.x + currentTarget.solidArea.x,
            currentTarget.y + currentTarget.solidArea.y,
            currentTarget.solidArea.width,
            currentTarget.solidArea.height
        );
        if (atk != null && atk.intersects(tgt)) {
            if (currentTarget instanceof player) {
                ((player) currentTarget).takeDamageFrom(attackDamage, this);
            } else if (currentTarget instanceof GolemMinion) {
                ((GolemMinion) currentTarget).takeDamage(attackDamage);
            }
        }
    }

    private Rectangle getAttackBox() {
        int cx = x + solidArea.x + solidArea.width / 2;
        int cy = y + solidArea.y + solidArea.height / 2;
        int reach = 65, spread = 30; // TODO: sesuaikan sama jangkauan senjata boss
        switch (direction) {
            case "up":    return new Rectangle(cx - spread, cy - solidArea.height / 2 - reach, spread * 2, reach);
            case "down":  return new Rectangle(cx - spread, cy + solidArea.height / 2, spread * 2, reach);
            case "left":  return new Rectangle(cx - solidArea.width / 2 - reach, cy - spread, reach, spread * 2);
            case "right": return new Rectangle(cx + solidArea.width / 2, cy - spread, reach, spread * 2);
        }
        return null;
    }

    /**
     * Drop item pas boss mati.
     * Guaranteed: Bone, Skull, dan Soul of Skeleton (trophy/memento boss).
     * Chance: Death Totem (rare drop).
     */
    private void dropItems() {
        if (dropped) return;
        dropped = true;
        ItemManager im = ItemManager.get();

        int boneAmount = 5 + (int) (Math.random() * 6);
        im.add(im.BONE, boneAmount);
        if (gp.speechBubble != null) gp.speechBubble.show("Obtained " + boneAmount + "x Bone");

        int skullAmount = 2 + (int) (Math.random() * 3);
        im.add(im.SKULL, skullAmount);
        if (gp.speechBubble != null) gp.speechBubble.show("Obtained " + skullAmount + "x Skull");

        // NEW: Soul of Skeleton — guaranteed 1x, trophy khusus dari Skeleton King
        im.add(im.SOUL_OF_SKELETON, 1);
        if (gp.speechBubble != null) gp.speechBubble.show("Obtained a Soul of Skeleton");

        if (Math.random() * 100 < 40) {
            im.add(im.DEATH_TOTEM, 1);
            if (gp.speechBubble != null) gp.speechBubble.show("Rare drop: Death Totem!");
        }
    }

    @Override
    public void takeDamage(int dmg) {
        if (!alive) return;
        if (state == KingState.DEATH || state == KingState.SPAWNING) return;
        // NEW: gak bisa kena damage selagi menghilang/lagi ngejalanin skill Skeleton Hand
        if (state == KingState.HAND_VANISH || state == KingState.HAND_HIDDEN || state == KingState.HAND_RETURN) return;
        life -= dmg;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);
        isHit = true;
        hitFlashTimer = 8;
    }
    
    public boolean isDeathAnimDone() {
        return deathAnimDone;
    }

    @Override
    public void draw(Graphics2D g2) {
        // NEW: gambar tangan-tangan skill duluan, terlepas dari state King
        // (biar tetep muncul & kelihatan walau King lagi ngilang total)
        for (SkeletonHand hand : activeHands) {
            hand.draw(g2);
        }

    	  if (state == KingState.DEATH && deathAnimDone) return;
        if (!alive && state != KingState.DEATH) return;
        if (state == KingState.HAND_HIDDEN) return; // King lagi ngilang total, gak digambar

        // Anchor mentah di layar (sebelum digeser buat centering sprite gede) —
        // dipake buat hitung posisi hitbox debug yang bener, JANGAN dicampur sama sx/sy di bawah.
        int screenX0 = x - gp.player.x + gp.player.screenX;
        int screenY0 = y - gp.player.y + gp.player.screenY;

        int drawSize = gp.tileSize * drawScale;
        int sx = screenX0 - drawSize / 2 + gp.tileSize / 2;
        int sy = screenY0 - drawSize / 2 + gp.tileSize / 3 + gp.tileSize; // TODO: makin gede nilainya, makin turun gambarnya
        if (sx + drawSize < gp.viewMinX() || sx > gp.viewMaxX() || sy + drawSize < gp.viewMinY() || sy > gp.viewMaxY()) return;

        BufferedImage img = null;

        if (state == KingState.DEATH) {
            switch (direction) {
                case "left":  img = S_deadLeft[deathFrameIndex];  break;
                case "right": img = S_deadRight[deathFrameIndex]; break;
                default:      img = S_deadFront[deathFrameIndex]; break; // "down"/"up" -> gak ada Back, fallback Front
            }
        } else if (state == KingState.SPAWNING) {         
            img = S_deadFront[spawnFrameIndex]; 
            
            
        } else if (state == KingState.HAND_VANISH) { // NEW
            switch (direction) {
                case "left":  img = S_deadLeft[vanishFrameIndex];  break;
                case "right": img = S_deadRight[vanishFrameIndex]; break;
                default:      img = S_deadFront[vanishFrameIndex]; break;
            }
        } else if (state == KingState.HAND_RETURN) { // NEW
            switch (direction) {
                case "left":  img = S_deadLeft[returnFrameIndex];  break;
                case "right": img = S_deadRight[returnFrameIndex]; break;
                default:      img = S_deadFront[returnFrameIndex]; break;
            }
        } else if (state == KingState.ATTACK) {
            switch (direction) {
                case "down":  img = S_attackFront[attackFrameIndex]; break;
                case "up":    img = S_attackBack[attackFrameIndex];  break;
                case "left":  img = S_attackLeft[attackFrameIndex];  break;
                case "right": img = S_attackRight[attackFrameIndex]; break;
            }
        } else if (state == KingState.WALK) {
            switch (direction) {
                case "down":  img = S_walkFront[walkFrameIndex]; break;
                case "up":    img = S_walkBack[walkFrameIndex];  break;
                case "left":  img = S_walkLeft[walkFrameIndex];  break;
                case "right": img = S_walkRight[walkFrameIndex]; break;
            }
        } else { // IDLE
            switch (direction) {
                case "down":  img = S_idleFront[idleFrameIndex]; break;
                case "up":    img = S_idleBack[idleFrameIndex];  break;
                case "left":  img = S_idleLeft[idleFrameIndex];  break;
                case "right": img = S_idleRight[idleFrameIndex]; break;
            }
        }

        // NEW: UI & shadow disembunyiin juga selama transisi HAND_VANISH/HAND_RETURN
        boolean showUiAndShadow = state != KingState.DEATH && state != KingState.SPAWNING
                && state != KingState.HAND_VANISH && state != KingState.HAND_RETURN;

        if (showUiAndShadow) {
            drawBossUI(g2, sx, sy, drawSize);
        }

        if (showUiAndShadow) {
            Composite oldComp = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(Color.BLACK);
            int shadowW = drawSize / 3, shadowH = shadowW / 3;
            g2.fillOval(sx + drawSize / 2 - shadowW / 2, sy + drawSize - shadowH - 55, shadowW, shadowH);
            g2.setComposite(oldComp);
        }

        if (img != null) {
            g2.drawImage(img, sx, sy, drawSize, drawSize, null);
            if (isHit && state != KingState.DEATH) {
                Composite oldComp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2.setColor(Color.RED);
                g2.fillRect(sx, sy, drawSize, drawSize);
                g2.setComposite(oldComp);
            }
        }

        g2.setColor(Color.RED);

        int hitboxX = screenX0 + solidArea.x;
        int hitboxY = screenY0 + solidArea.y;

        g2.drawRect(
            hitboxX,
            hitboxY,
            solidArea.width,
            solidArea.height
        );

        // Titik tengah hitbox
        g2.setColor(Color.YELLOW);
        int centerX = hitboxX + solidArea.width / 2;
        int centerY = hitboxY + solidArea.height / 2;
        g2.fillOval(centerX - 2, centerY - 2, 4, 4);

     // Attack Box (saat menyerang)
        if (state == KingState.ATTACK) {
            Rectangle atk = getAttackBox();

            if (atk != null) {
                int ax = atk.x - gp.player.x + gp.player.screenX;
                int ay = atk.y - gp.player.y + gp.player.screenY;

                g2.setColor(Color.GREEN);
                g2.drawRect(ax, ay, atk.width, atk.height);
            }
        
        }
    }
}