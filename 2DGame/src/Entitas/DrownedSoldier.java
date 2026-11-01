package Entitas;

import Item.ItemManager;
import Main.GamePanel;
import Tamed.GolemMinion;
import Tamed.GolemiteMinion;
import Tamed.UndeadMinion;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Drowned Soldier — mantan prajurit penjaga kerajaan bawah laut (Sunken Kingdom), sekarang undead.
 * Regular mob tingkat menengah buat Water Domain: lebih tebal dan lebih sakit dari Skeleton (400 HP / 15 dmg),
 * tapi masih di bawah Golemite (1500 HP / 80 dmg).
 *
 * Perilaku:
 *   IDLE / WANDER  patroli kecil di sekitar titik spawn
 *   CHASE          melihat player (DETECTION_RANGE) -> mengejar, menyusuri dinding kalau mentok
 *   ATTACK         tusukan pedang lurus 4 frame (tarik -> melesat -> tusukan kena -> tarik balik); tusukan kena di
 *                  frame ke-3 (HIT_FRAME), hitbox memanjang sempit lurus di depan badan
 *   HURT           terhuyung sebentar kalau kena pukul (tidak bisa di-stunlock: ada jeda kebal-stagger)
 *   RETURN         kalau player kabur terlalu jauh, balik ke titik spawn sambil pulih
 *   DYING          animasi mati yang sama kayak player (3 frame, rebah), memudar, lalu respawn
 *
 * Sprite: gaya sprite PLAYER (sel 64 px, proporsi, palet, dan gerakan sama), tampilannya tetap Drowned Soldier.
 * Sheet src/Aset/DrownedSoldier/drowned_soldier_sheet.png dibuat tools/build_drowned_sheet_from_player.py dari pose
 * frame player (idle, walk, tusukan, mati) yang di-skin jadi Drowned Soldier. Digambar 96 px, persis ukuran player.
 * Tata letak sheet: kolom = nomor frame, baris = anim * 4 + arah. Lihat konstanta di bawah.
 *
 * Mob ini hanya hidup di Water Domain (DomainManager yang nge-spawn, nge-update, dan membersihkannya waktu keluar).
 * Targetnya cuma player: minion hasil Death Totem belum bisa melawan mob ini, jadi mob ini tidak mengincar mereka.
 */
public class DrownedSoldier extends Entity {

    // ═══════════ SPRITE SHEET ═══════════
    private static final String SHEET_PATH = "/Aset/DrownedSoldier/drowned_soldier_sheet.png";
    private static final int CELL  = 64;               // ukuran satu sel di sheet (px), sama kayak frame player
    private static final int DRAW  = 96;               // digambar 96 px di layar (= gp.tileSize * 2), persis ukuran player

    // indeks animasi (baris = ANIM * 4 + arah)
    private static final int A_IDLE = 0, A_WALK = 1, A_ATTACK = 2, A_HURT = 3, A_DEATH = 4;
    // idle 1 frame (player juga diam 1 frame), hurt = frame idle (player gak punya animasi hurt), death 3 frame kayak player
    private static final int[] FRAME_COUNT = {1, 6, 4, 2, 3};
    private static final int HIT_FRAME = 2;            // frame ke-3 attack = tusukan kena (tangan lurus penuh)

    private static final BufferedImage[][][] FRAMES = new BufferedImage[5][4][];   // [anim][arah][frame]
    private static boolean framesLoaded = false;

    // posisi sprite relatif ke (x, y) entity: sama persis kayak player (screenX - tileSize/2, screenY - tileSize/3),
    // jadi badan di tengah tile dan kaki sejajar dasar hitbox
    private static final int SPRITE_OFF_X = -24;
    private static final int SPRITE_OFF_Y = -16;
    private static final int FEET_PX = 64;             // garis tanah dari atas sprite (px layar), pusat bayangan (sama kayak bayangan player)
    private static final int HEAD_PX = 22;             // puncak kepala dari atas sprite (px layar), buat HP bar

    // ═══════════ STAT (gampang di-tune) ═══════════
    public static final int    MAX_LIFE        = 4000;
    public static final int    ATTACK_DAMAGE   = 150;
    public static final double MOVE_SPEED      = 1.4;    // px / frame saat ngejar
    private static final double WANDER_SPEED   = 0.7;
    public static final int    EXP_VALUE       = 150;
    public static final int    COIN_VALUE      = 6;

    private static final int DETECTION_RANGE   = 270;    // jarak (pusat ke pusat) mulai mengejar
    private static final int LEASH_RANGE       = 480;    // lewat ini dia menyerah dan pulang
    private static final int SPAWN_LEASH       = 650;    // jarak maksimal dari titik spawn
    private static final int ATTACK_RANGE      = 62;     // jarak mulai menusuk
    private static final int ATTACK_REACH      = 56;     // panjang hitbox tusukan (tusukan menjangkau lebih jauh dari tebasan)
    private static final int ATTACK_SPREAD     = 18;     // setengah lebar hitbox tusukan (sempit: cuma kena yang tepat di depan)
    private static final int ATTACK_FRAME_TICKS = 7;     // 4 frame x 7 tick; tusukan kena ~14 tick (~0.23 dtk) setelah mulai
    private static final int ATTACK_COOLDOWN   = 70;
    private static final int NO_HIT_HEAL_SECONDS = 8;   // tanpa kena hit selama ini (detik, dikali gp.FPS) -> HP langsung full lagi
    private static final int HURT_TICKS        = 16;
    private static final int HURT_IMMUNITY     = 56;     // jeda sesudah terhuyung di mana pukulan gak bikin dia terhuyung lagi
    private static final int WANDER_RADIUS     = 200;
    private static final int WALK_TICKS_CHASE  = 8;      // tick per frame jalan saat ngejar / pulang (sama kayak player)
    private static final int WALK_TICKS_WANDER = 12;     // roaming speed-nya cuma 0.7 px/tick, jadi frame diganti lebih pelan biar langkah gak "ngesot"
    private static final int WALK_GRACE        = 3;      // toleransi tick tanpa gerak sebelum animasi jalan dianggap berhenti

    // ═══════════ DROP / RESPAWN ═══════════
    private static final double BONE_CHANCE    = 0.70;   // 1-4 Bone
    private static final double EMBLEM_CHANCE  = 0.35;   // Drowned Royal Emblem (1, kadang 2)
    private static final int    RESPAWN_DELAY  = 25 * 60;
    private static final int    RESPAWN_CLEAR  = 220;    // respawn ditunda kalau player masih sedekat ini dari titik spawn

    // ═══════════ STATE ═══════════
    private enum State { IDLE, WANDER, CHASE, ATTACK, HURT, RETURN, DYING }
    private State state = State.IDLE;

    private int dirIdx = 0;                              // 0 down, 1 up, 2 left, 3 right
    private int idleFrame = 0, idleTimer = 0;
    private int walkFrame = 0, walkTimer = 0;
    private int walkGrace = 0;                           // sisa tick "masih dianggap jalan" (anti reset animasi pas ada tick tanpa gerak)
    private int atkFrame = 0, atkTimer = 0, atkCooldown = 0;
    private boolean swingHitDone = false;
    private int hurtTimer = 0, hurtImmunity = 0;
    private int deathFrame = 0, deathTimer = 0, deathHold = 0, fadeTimer = 0;
    private static final int DEATH_FRAME_TICKS = 8, DEATH_HOLD_TICKS = 100, DEATH_FADE_TICKS = 40;   // 8 tick/frame = sama kayak animasi mati player

    private int wanderTimer = 0, wanderIdleTimer = 0;
    private int wanderDx = 0, wanderDy = 0;
    private int stuckTicks = 0, slideTicks = 0, slideSign = 1;
    private int returnTicks = 0;
    private int noDamageTimer = 0;
    private double accX = 0, accY = 0;                   // sisa pecahan langkah (speed 1.4 px/frame)
    private boolean movedThisTick = false;

    private boolean spawnSet = false, spawnChecked = false;
    private boolean active = true;                       // false = lagi menunggu respawn (gak digambar, gak bisa kena)
    private int respawnCounter = 0;

    private static final int HIT_FLASH_FRAMES = 5;
    private int whiteFlashTimer = 0;
    private static final Random RNG = new Random();

    public DrownedSoldier(GamePanel gp) {
        super(gp);
        this.name = "Drowned Soldier";
        this.maxLife = MAX_LIFE;
        this.life = maxLife;
        this.speed = MOVE_SPEED;
        this.direction = "down";
        this.solidArea = new Rectangle(14, 30, 20, 18);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.getExpValue = EXP_VALUE;
        this.coinDrop = COIN_VALUE;
        this.alive = true;
        loadFrames();
    }

    // ═══════════ STATUS (dipakai player, skill, DomainManager, GamePanel) ═══════════
    /** True selama masih bisa dipukul / mengincar player (tidak sedang mati atau menunggu respawn). */
    public boolean isAlive() { return active && state != State.DYING && life > 0; }

    /** True selama masih perlu digambar (termasuk animasi mati dan fade-out). */
    public boolean isVisible() { return active; }

    @Override
    public int getSortY() {
        // pakai nilai default: solidArea dipindah ke -9999 pas mati, jangan sampai urutan gambarnya loncat
        return y + solidAreaDefaultY + solidArea.height;
    }

    // ═══════════ LOAD SPRITE ═══════════
    public static synchronized void preloadImages() { loadFrames(); }

    /** Frame idle menghadap depan (ukuran asli 64 px), buat ikon Mob Index. */
    public static BufferedImage getIconFrame() {
        loadFrames();
        return FRAMES[A_IDLE][0][0];
    }

    private static synchronized void loadFrames() {
        if (framesLoaded) return;
        BufferedImage sheet = null;
        try (InputStream is = DrownedSoldier.class.getResourceAsStream(SHEET_PATH)) {
            if (is == null) System.out.println("[DrownedSoldier] Sprite sheet tidak ketemu: " + SHEET_PATH);
            else sheet = ImageIO.read(is);
        } catch (Exception e) {
            System.out.println("[DrownedSoldier] Gagal load sprite sheet: " + e.getMessage());
        }
        for (int a = 0; a < 5; a++) {
            for (int d = 0; d < 4; d++) {
                FRAMES[a][d] = new BufferedImage[FRAME_COUNT[a]];
                for (int f = 0; f < FRAME_COUNT[a]; f++) {
                    FRAMES[a][d][f] = sheet != null && sheet.getHeight() >= (a * 4 + d + 1) * CELL
                            ? copyCell(sheet.getSubimage(f * CELL, (a * 4 + d) * CELL, CELL, CELL))
                            : placeholder();
                }
            }
        }
        framesLoaded = true;
        Main.Log.d("[DrownedSoldier] Selesai load sprite sheet.");
    }

    /** Salinan mandiri satu sel (ukuran asli 64 px). Pembesaran ke 96 px dilakukan pas digambar, sama kayak player. */
    private static BufferedImage copyCell(BufferedImage src) {
        BufferedImage out = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Kotak teal polos kalau sprite sheet hilang, supaya game tetap jalan (dan mob tetap kelihatan). */
    private static BufferedImage placeholder() {
        BufferedImage img = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 140, 130, 200));
        g.fillRect(CELL / 2 - 8, 12, 16, 34);
        g.dispose();
        return img;
    }

    // ═══════════ GEOMETRI ═══════════
    private int centerX() { return x + solidArea.x + solidArea.width / 2; }
    private int centerY() { return y + solidArea.y + solidArea.height / 2; }

    private static int cx(Entity e) { return e.x + e.solidArea.x + e.solidArea.width / 2; }
    private static int cy(Entity e) { return e.y + e.solidArea.y + e.solidArea.height / 2; }

    private static Rectangle box(Entity e) {
        return new Rectangle(e.x + e.solidArea.x, e.y + e.solidArea.y, e.solidArea.width, e.solidArea.height);
    }

    private void faceToward(int dx, int dy) {
        // hysteresis kecil supaya hadapnya gak berkedip kalau |dx| ~ |dy|
        int ax = Math.abs(dx), ay = Math.abs(dy);
        boolean horizontal = (dirIdx >= 2) ? ax + 6 >= ay : ax > ay + 6;
        if (horizontal) dirIdx = dx >= 0 ? 3 : 2;
        else dirIdx = dy >= 0 ? 0 : 1;
        direction = new String[]{"down", "up", "left", "right"}[dirIdx];
    }

    /**
     * Hadap lurus ke arah jalan (dx, dy cuma -1 / 0 / 1). Dipakai pas roaming. JANGAN pakai faceToward() buat ini:
     * faceToward() ngitung selisih PIXEL dengan margin 6 px, jadi kalau dikasih vektor -1/0/1 dia gak pernah milih
     * kiri/kanan dari posisi hadap depan/belakang (dan sebaliknya), akibatnya sprite jalan menyamping tapi hadap depan.
     */
    private void faceAxis(int dx, int dy) {
        if (dx != 0) dirIdx = dx > 0 ? 3 : 2;
        else dirIdx = dy >= 0 ? 0 : 1;
        direction = new String[]{"down", "up", "left", "right"}[dirIdx];
    }

    // ═══════════ COLLISION ═══════════
    /** Apakah langkah (dx, dy) terhalang: dinding / air dalam / pilar domain, player, atau prajurit lain. */
    private boolean blockedBy(int dx, int dy) {
        Rectangle cur = box(this);
        Rectangle next = new Rectangle(cur.x + dx, cur.y + dy, cur.width, cur.height);
        if (gp.domainManager != null && gp.domainManager.checkCollision(next)) return true;
        // tidak boleh masuk ke badan player / prajurit lain (kalau sudah terlanjur numpuk, tetap boleh menjauh)
        Rectangle pb = box(gp.player);
        if (next.intersects(pb) && !cur.intersects(pb)) return true;
        if (gp.domainManager != null) {
            for (DrownedSoldier o : gp.domainManager.getDrownedSoldiers()) {
                if (o == this || !o.isAlive()) continue;
                Rectangle ob = box(o);
                if (next.intersects(ob) && !cur.intersects(ob)) return true;
            }
        }
        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity m : gp.deathTotemManager.getMinions()) {
                if (!minionAlive(m)) continue;
                Rectangle mb = box(m);
                if (next.intersects(mb) && !cur.intersects(mb)) return true;
            }
        }
        return false;
    }

    /** Minion Death Totem menyembunyikan field `alive` milik Entity, jadi dicek per tipe (sama kayak Skeleton). */
    private static boolean minionAlive(Entity m) {
        if (m == null) return false;
        if (m instanceof UndeadMinion)   return ((UndeadMinion) m).alive;
        if (m instanceof GolemiteMinion) return m.alive;
        if (m instanceof GolemMinion)    return ((GolemMinion) m).alive;
        return false;
    }

    private boolean tryMove(int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        if (blockedBy(dx, dy)) return false;
        x += dx;
        y += dy;
        return true;
    }

    /** Pecahan langkah: speed 1.4 px/frame jadi 1 atau 2 px per frame secara bergantian. */
    private int takeStep(double v, boolean horizontal) {
        double acc = (horizontal ? accX : accY) + v;
        int s = (int) acc;               // dibulatkan ke arah nol
        acc -= s;
        if (horizontal) accX = acc; else accY = acc;
        return s;
    }

    /**
     * Gerak satu langkah ke titik (tx, ty). Kalau mentok dinding, merambat menyamping (slide) ke arah acak
     * selama ~30 frame; kalau macet lama, return false terus supaya pemanggil bisa menyerah.
     */
    private boolean steerTowards(int tx, int ty, double spd) {
        int distX = tx - centerX(), distY = ty - centerY();
        double vx = Math.abs(distX) < 3 ? 0 : Integer.signum(distX);
        double vy = Math.abs(distY) < 3 ? 0 : Integer.signum(distY);
        if (vx == 0 && vy == 0) return false;
        if (vx != 0 && vy != 0) { vx *= 0.707; vy *= 0.707; }

        boolean moved = false;
        if (slideTicks > 0) {
            slideTicks--;
            // merambat tegak lurus arah utama yang tadi mentok
            boolean mainHorizontal = Math.abs(distX) >= Math.abs(distY);
            int s = (int) Math.round(spd) * slideSign;
            moved = mainHorizontal ? tryMove(0, s) : tryMove(s, 0);
            if (moved) { stuckTicks = 0; return true; }
            slideSign = -slideSign;
        }

        int ix = takeStep(vx * spd, true), iy = takeStep(vy * spd, false);
        if (ix == 0 && iy == 0) return true;   // masih ngumpulin pecahan langkah (diagonal ~0.99 px): tetap jalan, bukan macet
        boolean mx = ix != 0 && tryMove(ix, 0);
        boolean my = iy != 0 && tryMove(0, iy);
        moved = mx || my;
        if (moved) {
            stuckTicks = 0;
        } else if (ix != 0 || iy != 0) {
            stuckTicks++;
            if (stuckTicks >= 6 && slideTicks == 0) {
                slideTicks = 30;
                slideSign = RNG.nextBoolean() ? 1 : -1;
            }
        }
        return moved;
    }

    // ═══════════ UPDATE ═══════════
    @Override
    public void update() {
        if (!spawnSet) { spawnX = x; spawnY = y; spawnSet = true; }
        movedThisTick = false;
        if (whiteFlashTimer > 0) whiteFlashTimer--;
        if (hurtImmunity > 0) hurtImmunity--;

        // ── menunggu respawn ──
        if (!active) {
            respawnCounter++;
            if (respawnCounter >= RESPAWN_DELAY && playerFarFromSpawn()) respawn();
            return;
        }

        // ── jaga-jaga: posisi spawn ternyata nyangkut di tembok / air dalam -> geser ke tempat bebas terdekat ──
        if (!spawnChecked) {
            spawnChecked = true;
            relocateIfBlocked();
        }

        if (state == State.DYING) { updateDying(); return; }
        if (life <= 0) { startDying(); return; }       // mati lewat jalur selain takeDamage()

        updateRegen();

        // ── terhuyung ──
        if (state == State.HURT) {
            if (isKnockback) updateKnockback();
            hurtTimer--;
            if (hurtTimer <= 0) state = State.CHASE;
            return;
        }
        if (isKnockback) updateKnockback();

        if (atkCooldown > 0) atkCooldown--;

        Entity target = gp.player;
        boolean targetValid = !gp.player.isDead;
        int tx = cx(target), ty = cy(target);
        double dist = Math.hypot(tx - centerX(), ty - centerY());
        double spawnDist = Math.hypot(spawnX + 24 - centerX(), spawnY + 39 - centerY());

        switch (state) {
            case IDLE:
                idleAnim();
                if (targetValid && dist <= DETECTION_RANGE && spawnDist <= SPAWN_LEASH) { state = State.CHASE; break; }
                wanderIdleTimer++;
                if (wanderIdleTimer >= 90 + RNG.nextInt(90)) { wanderIdleTimer = 0; pickWander(); state = State.WANDER; doWander(); }   // doWander() langsung: tick pertama gak boleh nampilin pose idle
                break;

            case WANDER:
                if (targetValid && dist <= DETECTION_RANGE && spawnDist <= SPAWN_LEASH) { state = State.CHASE; break; }
                wanderTimer--;
                if (wanderTimer <= 0) { state = State.IDLE; wanderIdleTimer = 0; break; }
                doWander();
                break;

            case CHASE:
                if (!targetValid || dist > LEASH_RANGE || spawnDist > SPAWN_LEASH || stuckTicks > 150) {
                    state = State.RETURN; returnTicks = 0; stuckTicks = 0;
                    break;
                }
                faceToward(tx - centerX(), ty - centerY());
                if (dist <= ATTACK_RANGE && atkCooldown == 0) {
                    state = State.ATTACK; atkFrame = 0; atkTimer = 0; swingHitDone = false; walkGrace = 0;
                    break;
                }
                if (dist > ATTACK_RANGE * 0.8) movedThisTick = steerTowards(tx, ty, speed);
                break;

            case ATTACK:
                updateAttack(target, targetValid);
                break;

            case RETURN:
                returnTicks++;
                if (targetValid && dist <= DETECTION_RANGE * 0.7 && spawnDist <= SPAWN_LEASH * 0.8) { state = State.CHASE; break; }
                if (spawnDist <= 24 || returnTicks > 600) {
                    if (returnTicks > 600) { x = spawnX; y = spawnY; }   // macet di jalan pulang: langsung dikembalikan
                    state = State.IDLE; wanderIdleTimer = 0; stuckTicks = 0;
                    break;
                }
                faceToward(spawnX + 24 - centerX(), spawnY + 39 - centerY());
                movedThisTick = steerTowards(spawnX + 24, spawnY + 39, speed * 1.2);
                life = Math.min(maxLife, life + 1);                       // pulih pelan selama pulang
                break;

            case HURT:
            case DYING:
                break;
        }

        // Animasi jalan jangan digantung ke "geser >= 1 px tick ini": speed pecahan (roaming 0.7 px/tick) bikin tiap
        // beberapa tick langkahnya 0 px, dan dulu itu langsung mereset walkFrame/walkTimer ke 0 -> timer gak pernah
        // nyampe batas, frame cuma 0 terus dan sprite kedip antara pose jalan-0 dan idle (kelihatan patah-patah).
        if (movedThisTick) {
            walkGrace = WALK_GRACE;
            walkAnim();
        } else if (state != State.ATTACK) {
            if (walkGrace > 0) walkGrace--;
            if (walkGrace > 0) walkAnim();            // hiccup sesaat (belok / geser): lanjutkan siklus
            else { walkFrame = 0; walkTimer = 0; }    // benar-benar berhenti
        }
    }

    private void idleAnim() {
        idleTimer++;
        if (idleTimer >= 14) { idleTimer = 0; idleFrame = (idleFrame + 1) % FRAME_COUNT[A_IDLE]; }
    }

    private void walkAnim() {
        walkTimer++;
        int len = state == State.WANDER ? WALK_TICKS_WANDER : WALK_TICKS_CHASE;
        if (walkTimer >= len) { walkTimer = 0; walkFrame = (walkFrame + 1) % FRAME_COUNT[A_WALK]; }
    }

    private void pickWander() {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        // kalau sudah jauh dari spawn, jalan balik ke arah spawn
        int bx = spawnX + 24 - centerX(), by = spawnY + 39 - centerY();
        if (Math.hypot(bx, by) > WANDER_RADIUS) {
            if (Math.abs(bx) > Math.abs(by)) { wanderDx = Integer.signum(bx); wanderDy = 0; }
            else { wanderDx = 0; wanderDy = Integer.signum(by); }
        } else {
            int[] d = dirs[RNG.nextInt(4)];
            wanderDx = d[0]; wanderDy = d[1];
        }
        wanderTimer = 40 + RNG.nextInt(70);
        accX = 0; accY = 0;               // buang sisa pecahan dari arah sebelumnya (bisa bernilai negatif -> langkah pertama meleset)
        faceAxis(wanderDx, wanderDy);
    }

    private void doWander() {
        int ix = takeStep(wanderDx * WANDER_SPEED, true), iy = takeStep(wanderDy * WANDER_SPEED, false);
        if (ix == 0 && iy == 0) { movedThisTick = true; return; }   // tick tanpa pergeseran px (0.7 px/tick): tetap jalan, jangan dihitung diam
        boolean moved = (ix != 0 && tryMove(ix, 0)) | (iy != 0 && tryMove(0, iy));
        if (moved) movedThisTick = true;
        else if (ix != 0 || iy != 0) { state = State.IDLE; wanderIdleTimer = 0; }   // mentok: berhenti dulu
    }

    private void updateAttack(Entity target, boolean targetValid) {
        atkTimer++;
        if (atkTimer < ATTACK_FRAME_TICKS) return;
        atkTimer = 0;
        atkFrame++;
        if (atkFrame == HIT_FRAME && !swingHitDone) {
            swingHitDone = true;
            gp.playSE("SwordSfx2.wav");
            if (targetValid) hitTarget(target);
        }
        if (atkFrame >= FRAME_COUNT[A_ATTACK]) {
            atkFrame = 0;
            atkCooldown = ATTACK_COOLDOWN;
            state = State.CHASE;
        }
    }

    /** Hitbox tusukan di depan badan (searah hadap): panjang dan sempit. */
    private Rectangle attackBox() {
        int cx = centerX(), cy = centerY();
        int hw = solidArea.width / 2, hh = solidArea.height / 2;
        switch (dirIdx) {
            case 1:  return new Rectangle(cx - ATTACK_SPREAD, cy - hh - ATTACK_REACH, ATTACK_SPREAD * 2, ATTACK_REACH);
            case 2:  return new Rectangle(cx - hw - ATTACK_REACH, cy - ATTACK_SPREAD, ATTACK_REACH, ATTACK_SPREAD * 2);
            case 3:  return new Rectangle(cx + hw, cy - ATTACK_SPREAD, ATTACK_REACH, ATTACK_SPREAD * 2);
            default: return new Rectangle(cx - ATTACK_SPREAD, cy + hh, ATTACK_SPREAD * 2, ATTACK_REACH);
        }
    }

    private void hitTarget(Entity target) {
        if (!attackBox().intersects(box(target))) return;
        if (target instanceof player) ((player) target).takeDamageFrom(ATTACK_DAMAGE, this);
        else target.takeDamage(ATTACK_DAMAGE);
    }

    private void updateRegen() {
        if (life >= maxLife) { noDamageTimer = 0; return; }
        noDamageTimer++;
        if (noDamageTimer >= NO_HIT_HEAL_SECONDS * gp.FPS) {   // tidak dipukul NO_HIT_HEAL_SECONDS detik -> HP langsung penuh
            life = maxLife;
            noDamageTimer = 0;
        }
    }

    // ═══════════ DAMAGE ═══════════
    @Override
    public void takeDamage(int dmg) {
        if (!isAlive()) return;
        noDamageTimer = 0;
        life -= dmg;
        whiteFlashTimer = HIT_FLASH_FRAMES;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);
        if (life <= 0) { startDying(); return; }

        // sedang menusuk: jangan diinterupsi (animasinya dibiarkan selesai)
        if (state == State.ATTACK) return;
        if (hurtImmunity == 0) {
            state = State.HURT;
            hurtTimer = HURT_TICKS;
            hurtImmunity = HURT_TICKS + HURT_IMMUNITY;
            atkFrame = 0; atkTimer = 0; walkGrace = 0;
        } else if (state != State.HURT) {
            state = State.CHASE;        // tetap marah, cuma gak terhuyung lagi
        }
    }

    @Override
    public void takeDamage(int dmg, player attacker) {
        takeDamage(dmg);
    }

    // ── Knockback: kedorong sedikit lalu balik ke titik tempat dia kena (sama kayak Skeleton / Golemite) ──
    private static final int KB_DISTANCE = 6, KB_OUT_STEP = 3, KB_BACK_STEP = 2;
    private int kbOriginX, kbOriginY, kbTargetX, kbTargetY;
    private boolean kbReturning = false;

    /** Dipanggil SETELAH takeDamage() oleh yang memukul. Arah dorong dari posisi relatif attacker. */
    public void knockbackFrom(Entity attacker) {
        if (attacker == null || !isAlive()) return;
        int dx = centerX() - cx(attacker), dy = centerY() - cy(attacker);
        String pushDir;
        if (dx == 0 && dy == 0) pushDir = attacker.direction != null ? attacker.direction : "down";
        else if (Math.abs(dx) > Math.abs(dy)) pushDir = dx > 0 ? "right" : "left";
        else pushDir = dy > 0 ? "down" : "up";

        if (!isKnockback) { kbOriginX = x; kbOriginY = y; }
        applyKnockback(pushDir);
        kbTargetX = kbOriginX + knockbackDirectionX * KB_DISTANCE;
        kbTargetY = kbOriginY + knockbackDirectionY * KB_DISTANCE;
        kbReturning = false;
    }

    private void updateKnockback() {
        int goalX = kbReturning ? kbOriginX : kbTargetX, goalY = kbReturning ? kbOriginY : kbTargetY;
        int step = kbReturning ? KB_BACK_STEP : KB_OUT_STEP;
        int dx = Integer.compare(goalX, x) * Math.min(step, Math.abs(goalX - x));
        int dy = Integer.compare(goalY, y) * Math.min(step, Math.abs(goalY - y));
        if (dx != 0 || dy != 0) {
            Rectangle next = new Rectangle(x + solidArea.x + dx, y + solidArea.y + dy, solidArea.width, solidArea.height);
            if (gp.domainManager != null && gp.domainManager.checkCollision(next)) {
                if (kbReturning) endKnockback(); else kbReturning = true;   // mentok: balik / berhenti
                return;
            }
            x += dx;
            y += dy;
        }
        if (x == goalX && y == goalY) {
            if (kbReturning) endKnockback(); else kbReturning = true;
        }
    }

    private void endKnockback() {
        isKnockback = false;
        knockbackCounter = 0;
        kbReturning = false;
    }

    // ═══════════ MATI / RESPAWN ═══════════
    private void startDying() {
        state = State.DYING;
        alive = false;
        isKnockback = false;
        kbReturning = false;
        deathFrame = 0; deathTimer = 0; deathHold = 0; fadeTimer = 0;
        solidArea.x = -9999;                 // gak bisa dipukul / ditabrak lagi
        solidArea.y = -9999;
        dropItems();
        gp.player.killMonster(this);         // EXP + Coin (coinDrop) + peluang potion (+ capture Death Totem kalau ada tipenya)
    }

    private void updateDying() {
        if (deathFrame < FRAME_COUNT[A_DEATH] - 1) {
            deathTimer++;
            if (deathTimer >= DEATH_FRAME_TICKS) { deathTimer = 0; deathFrame++; }
        } else if (deathHold < DEATH_HOLD_TICKS) {
            deathHold++;
        } else {
            fadeTimer++;
            if (fadeTimer >= DEATH_FADE_TICKS) {
                active = false;              // hilang sampai respawn
                respawnCounter = 0;
            }
        }
    }

    private boolean playerFarFromSpawn() {
        return Math.hypot(cx(gp.player) - (spawnX + 24), cy(gp.player) - (spawnY + 39)) >= RESPAWN_CLEAR;
    }

    /** Bangkit lagi di titik spawn dengan HP penuh. */
    public void respawn() {
        active = true;
        alive = true;
        life = maxLife;
        x = spawnX;
        y = spawnY;
        state = State.IDLE;
        dirIdx = 0;
        direction = "down";
        idleFrame = 0; idleTimer = 0; walkFrame = 0; walkTimer = 0; walkGrace = 0;
        atkFrame = 0; atkTimer = 0; atkCooldown = 0; swingHitDone = false;
        hurtTimer = 0; hurtImmunity = 0;
        deathFrame = 0; deathTimer = 0; deathHold = 0; fadeTimer = 0;
        wanderTimer = 0; wanderIdleTimer = 0; stuckTicks = 0; slideTicks = 0; returnTicks = 0;
        noDamageTimer = 0; accX = 0; accY = 0;
        isKnockback = false; kbReturning = false; isDead = false;
        whiteFlashTimer = 0;
        solidArea.x = solidAreaDefaultX;
        solidArea.y = solidAreaDefaultY;
        respawnCounter = 0;
    }

    /** Kalau titik spawn terblokir (peta berubah / salah koordinat), cari titik bebas terdekat (spiral, langkah 16 px). */
    private void relocateIfBlocked() {
        if (gp.domainManager == null || !blockedAtOrigin()) return;
        for (int r = 16; r <= 192; r += 16) {
            for (int dy = -r; dy <= r; dy += 16) {
                for (int dx = -r; dx <= r; dx += 16) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    Rectangle b = new Rectangle(x + dx + solidArea.x, y + dy + solidArea.y, solidArea.width, solidArea.height);
                    if (!gp.domainManager.checkCollision(b)) {
                        x += dx; y += dy;
                        spawnX = x; spawnY = y;
                        return;
                    }
                }
            }
        }
    }

    private boolean blockedAtOrigin() {
        return gp.domainManager.checkCollision(box(this));
    }

    // ═══════════ DROP ═══════════
    private void dropItems() {
        ItemManager im = ItemManager.get();
        if (RNG.nextDouble() < BONE_CHANCE) {
            int n = 1 + RNG.nextInt(4);
            im.add(im.BONE, n);
            say("Obtained " + n + "x Bone");
        }
        if (RNG.nextDouble() < EMBLEM_CHANCE && im.DROWNED_EMBLEM != null) {
            int n = RNG.nextDouble() < 0.15 ? 2 : 1;
            im.add(im.DROWNED_EMBLEM, n);
            say("Obtained " + n + "x Drowned Royal Emblem");
        }
        // Coin (coinDrop), EXP, dan peluang potion diurus player.killMonster() di startDying()
    }

    private void say(String msg) {
        if (gp.speechBubble != null) gp.speechBubble.show(msg);
    }

    // ═══════════ GAMBAR ═══════════
    private BufferedImage currentFrame() {
        switch (state) {
            case DYING:  return FRAMES[A_DEATH][dirIdx][Math.min(deathFrame, FRAME_COUNT[A_DEATH] - 1)];
            case HURT:   return FRAMES[A_HURT][dirIdx][hurtTimer > HURT_TICKS / 2 ? 0 : 1];
            case ATTACK: return FRAMES[A_ATTACK][dirIdx][Math.min(atkFrame, FRAME_COUNT[A_ATTACK] - 1)];
            default:
                if (movedThisTick || walkGrace > 0) return FRAMES[A_WALK][dirIdx][walkFrame % FRAME_COUNT[A_WALK]];
                return FRAMES[A_IDLE][dirIdx][idleFrame % FRAME_COUNT[A_IDLE]];
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (!active) return;

        int sx = x - gp.player.x + gp.player.screenX + SPRITE_OFF_X;
        int sy = y - gp.player.y + gp.player.screenY + SPRITE_OFF_Y;
        if (sx + DRAW < gp.viewMinX() || sx > gp.viewMaxX() || sy + DRAW < gp.viewMinY() || sy > gp.viewMaxY()) return;

        boolean dying = state == State.DYING;
        Composite old = g2.getComposite();

        // bayangan di kaki (gak dipakai pas mati: tumpukan sisanya sudah punya genangan sendiri)
        if (!dying) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(Color.BLACK);
            int shadowW = 28, shadowH = 12;        // ukuran bayangan player
            g2.fillOval(sx + DRAW / 2 - shadowW / 2, sy + FEET_PX - shadowH / 2, shadowW, shadowH);
            g2.setComposite(old);
        }

        // HP bar di atas kepala, cuma muncul kalau sudah terluka
        if (!dying && life < maxLife) {
            int barW = 44, barH = 5;
            int fill = (int) ((double) Math.max(0, life) / maxLife * barW);
            int bx = sx + DRAW / 2 - barW / 2, by = sy + HEAD_PX - 10;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(bx, by, barW, barH);
            g2.setColor(new Color(200, 50, 50));
            g2.fillRect(bx, by, fill, barH);
        }

        // memudar di akhir animasi mati
        if (dying && fadeTimer > 0) {
            float a = Math.max(0f, 1f - (float) fadeTimer / DEATH_FADE_TICKS);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        }

        BufferedImage img = currentFrame();
        if (img != null) {
            g2.drawImage(whiteFlashTimer > 0 ? HitFlash.whiteVersion(img) : img, sx, sy, DRAW, DRAW, null);
        }
        g2.setComposite(old);
    }
}
