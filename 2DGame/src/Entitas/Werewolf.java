package Entitas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;

import Main.GamePanel;
import bahan.WorldGen;

/**
 * Werewolf — manusia serigala penghuni DARATAN ES (biome salju di barat daya dunia).
 *
 * Sifat:
 *  - Penjaga wilayah: berkeliaran pelan di salju di sekitar "sarang"-nya dan TIDAK PERNAH keluar dari biome es
 *    (langkah yang nyeberang ke luar salju ditolak). Player yang kabur ke hutan / pantai gak akan dikejar sampai situ.
 *  - Pemburu: kalau player masuk radius pandang dia ngejar, dan dari jarak menengah dia LONCAT menerkam (lunge) untuk
 *    nutup jarak, lalu mencakar. Cakaran gantian pakai gerakan serangan player ke-3 dan ke-4 (masing-masing 4 frame,
 *    damage masuk di frame ke-2); serangan player ke-1 dan ke-2 gak dipakai.
 *  - Ngamuk (enrage): HP di bawah 35% -> lebih cepat, jeda serangan lebih pendek, bayangan kakinya jadi merah.
 *  - Malam hari (jam game 18:00 - 05:59) lebih galak: radius pandang +20%, damage +25%.
 *  - Kuat: HP 220, exp 90, koin 6. Cakaran 16 damage (dikurangi DEF/armor player), jadi jangan diladeni sendirian di awal.
 *
 * Siklus hidup (sama gaya kayak Wolf, tapi gak tergantung jam):
 *  - Cuma "ada" kalau player lagi di biome es. Pas player masuk, tiap werewolf muncul (fade in) di daratan es
 *    600-1000 px dari player, di luar layar, dengan jeda acak biar gak muncul barengan.
 *  - Player keluar dari biome es dan werewolf jauh -> menghilang pelan-pelan. Masuk lagi -> muncul lagi dengan HP penuh.
 *  - Mati -> gak muncul lagi selama RESPAWN_TICKS (90 detik), baru boleh muncul lagi.
 *  - Kalau player jalan jauh di dalam biome es sampai werewolf ketinggalan (> RELOCATE_DIST), dia pindah ke dekat player lagi.
 *
 * Sprite dari sheet (lihat WerewolfSprite; dibuat lewat tools/werewolf_gen/player_sheet.py dari frame PLAYER, jadi
 * animasinya sama dengan player): idle napas 2 frame, jalan 6 frame, terkam = frame lari player 3 frame,
 * cakaran = serangan player ke-3 / ke-4 (4 frame), mati 3 frame. Empat arah beneran (kanan / kiri / atas / bawah).
 *
 * Catatan penting: pas gak ada werewolf berstatus ABSENT dan isDead = true, supaya semua kode lain (serang player, skill,
 * tabrakan, TileM) menganggapnya gak ada. Makanya GamePanel tetap harus manggil update() werewolf walau isDead
 * (lihat GamePanel, sama kayak Wolf / Golem / Golemite).
 *
 * Posisi: (x, y) = pojok kiri-atas "sel" setinggi 1 tile; telapak kaki di y + tileSize - 2 (jadi urutan gambar TileM yang
 * ngurutin monster pakai y + tileSize tetap benar). Gambar sprite digeser ke atas / samping dari titik itu.
 */
public class Werewolf extends Entity {

    public enum WerewolfState { ABSENT, IDLE, WANDER, CHASE, LUNGE, ATTACK, DYING, LEAVING }

    private WerewolfState state = WerewolfState.ABSENT;

    // ── Stat ──
    private static final int    MAX_LIFE        = 4000;
    private static final int    EXP_VALUE       = 400;
    private static final int    COIN_DROP       = 6;
    private static final double WANDER_SPEED    = 0.9;
    private static final double CHASE_SPEED     = 3.0;    // player jalan 2.5, lari 5.0
    private static final double ENRAGE_SPEED    = 3.7;
    private static final double LUNGE_SPEED     = 7.5;    // loncatan menerkam
    private static final int    AGGRO_RADIUS    = 420;    // mulai ngejar
    private static final int    LOSE_RADIUS     = 720;    // nyerah ngejar
    private static final int    ATTACK_RANGE    = 80;     // mulai mencakar (jarak antar titik tengah hitbox)
    private static final int    ATTACK_DAMAGE   = 300;
    private static final int    ATTACK_COOLDOWN = 90;     // 2,25 detik @40 FPS
    private static final int    ENRAGE_COOLDOWN = 60;
    private static final double ENRAGE_HP       = 0.35;   // sisa HP (rasio) buat mulai ngamuk
    private static final int    PROVOKED_TICKS  = 280;    // abis dipukul, tetap ngejar walau agak jauh
    private static final double NIGHT_AGGRO     = 1.20;
    private static final double NIGHT_DAMAGE    = 1.25;

    // ── Terkam ──
    private static final int LUNGE_MIN       = 150;       // jarak minimal buat meloncat
    private static final int LUNGE_MAX       = 260;
    private static final int LUNGE_TICKS     = 9;         // 9 tick * 7,5 px = ~67 px
    private static final int LUNGE_COOLDOWN  = 170;

    // ── Wilayah (biome es) ──
    /** Titik hanya boleh diinjak kalau WorldGen.ice() di titik itu minimal segini (0 = hutan, 1 = tengah salju). */
    private static final float TERRITORY_ICE = 0.55f;
    /** Tempat muncul harus lebih dalam lagi ke tengah salju. */
    private static final float SPAWN_ICE     = 0.72f;
    /** Player dianggap "ada di biome es" (werewolf boleh muncul) kalau ice() di kakinya di atas ini. */
    private static final float ACTIVATE_ICE  = 0.35f;
    /** Player dianggap sudah keluar dari biome es kalau ice() di kakinya di bawah ini (hysteresis dari ACTIVATE_ICE). */
    private static final float LEAVE_ICE     = 0.12f;
    private static final int   HOME_RADIUS   = 520;      // jalan-jalan santai di sekitar sarang sejauh ini

    // ── Muncul / hilang ──
    private static final int APPEAR_DELAY_MAX = 1200;     // tiap werewolf muncul acak 0-30 detik setelah player masuk biome es
    private static final int SPAWN_MIN_DIST   = 600;      // muncul minimal sejauh ini dari player (di luar layar)
    private static final int SPAWN_MAX_DIST   = 1000;
    private static final int SPAWN_APART      = 450;      // jarak minimal ke werewolf lain waktu muncul
    private static final int RELOCATE_DIST    = 1500;     // ketinggalan jauh -> pindah ke dekat player lagi
    private static final int DESPAWN_DIST     = 1100;     // player di luar biome es & werewolf sejauh ini -> menghilang
    private static final int RESPAWN_TICKS    = 40 * 90;  // 90 detik setelah mati
    private static final int FADE_IN_TICKS    = 40;
    private static final int LEAVE_TICKS      = 60;
    private static final int DEAD_LIE_TICKS   = 90;
    private static final int DEAD_FADE_TICKS  = 50;

    // ── Efek kena pukul: badan putih sekejap ──
    private static final int HIT_FLASH_FRAMES = 5;
    private int whiteFlashTimer = 0;

    // ── Animasi ──
    private static final int WALK_FRAME_TICKS   = 6;      // 6 frame x 6 tick = 1 siklus langkah 0,9 detik @40 FPS (player: 8 tick/frame)
    private static final int IDLE_FRAME_TICKS   = 20;     // 2 frame x 20 tick = 1 tarikan napas 1 detik
    private static final double ATTACK_SPEED   = 40.6;    // kecepatan serangan: 1.0 = pelan (kayak dulu), 2.0 = 2x lebih cepat
    private static final int ATTACK_FRAME_TICKS = Math.max(1, (int) Math.round(5 / ATTACK_SPEED));    // 5 tick @1.0x -> 3 tick @1.6x per frame serangan
    private static final int ATTACK_WINDUP_TICKS = Math.max(1, (int) Math.round(10 / ATTACK_SPEED));  // 10 tick @1.0x -> 6 tick @1.6x; frame pertama ditahan biar kelihatan mau nyerang
    private static final int ATTACK_FRAMES      = 4;      // serangan player ke-3 dan ke-4 sama-sama 4 frame
    private static final int STRIKE_FRAME       = 1;      // damage masuk begitu masuk frame ke-2 (player juga gitu)

    private final WerewolfSprite sprite;
    private final int drawW, drawH;
    private final int drawOffX, drawOffY;   // geser gambar dari (x, y) entity
    private final int footLocalY;           // y telapak kaki relatif ke y entity

    private int dir = WerewolfSprite.DOWN;  // arah hadap: WerewolfSprite.RIGHT / LEFT / UP / DOWN
    private boolean moving = false;
    private int walkFrame = 0, walkTick = 0;
    private int idleFrame = 0, idleTick = 0;
    private int attackFrame = 0, attackTick = 0;
    private int attackVariant = 4;          // gerakan serangan player yang lagi dipakai: 3 atau 4 (gantian tiap nyerang)
    private boolean strikeDone = false;

    // ── Status ──
    private int respawnTimer = 0;
    private int appearCountdown = -1;       // -1 = belum diundi
    private int fadeIn = 0;
    private int leaveTimer = 0;
    private int dyingTimer = 0;
    private int attackCd = 0;
    private int lungeCd = 0;
    private int lungeTicks = 0;
    private double lungeDX = 0, lungeDY = 0;
    private int provokedTimer = 0;
    private int stuckTicks = 0;
    private boolean enraged = false;

    // ── Gerak ──
    private double accX = 0, accY = 0;      // sisa gerak pecahan (speed bukan bilangan bulat)
    private double wanderDX = 0, wanderDY = 1;
    private int wanderTimer = 0, pauseTimer = 0;
    private int homeX, homeY;               // titik tengah "sarang"

    private final Random rand = new Random();

    public Werewolf(GamePanel gp) {
        super(gp);

        name        = "werewolf";
        showName    = true;
        maxLife     = MAX_LIFE;
        life        = maxLife;
        getExpValue = EXP_VALUE;
        coinDrop    = COIN_DROP;
        speed       = WANDER_SPEED;
        attackRange = ATTACK_RANGE;
        direction   = "down";

        double scale = gp.tileSize / 32.0;      // tileSize 48 -> 1.5 (sama kayak sprite player)
        sprite = WerewolfSprite.get(scale);
        drawW  = sprite.width;
        drawH  = sprite.height;

        // hitbox di kaki (sama kayak player / monster lain), simetris di tengah badan
        int boxW = (int) Math.round(40 * scale / 1.5);
        int boxH = (int) Math.round(30 * scale / 1.5);
        footLocalY = gp.tileSize - 2;
        solidArea = new Rectangle(4, footLocalY - boxH, boxW, boxH);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        drawOffX = (solidArea.x + boxW / 2) - sprite.centerX;
        drawOffY = footLocalY - sprite.footY;

        becomeAbsent();   // awalnya belum ada
    }

    // ───────────────────────── Status publik ─────────────────────────

    /** True kalau werewolf lagi ada dan masih bisa dipukul / ditarget skill. */
    public boolean isAlive() {
        return state == WerewolfState.IDLE || state == WerewolfState.WANDER || state == WerewolfState.CHASE
            || state == WerewolfState.LUNGE || state == WerewolfState.ATTACK;
    }

    private boolean isNight() {
        int h = gp.world.hour;
        return h >= 18 || h < 6;
    }

    // ───────────────────────── Biome es ─────────────────────────

    /** Seberapa bersalju titik dunia (wx, wy) piksel: 0 = bukan salju, 1 = tengah salju. */
    private float iceAt(double wx, double wy) {
        WorldGen g = (gp.tileM != null) ? gp.tileM.gen : null;
        if (g == null) return 0f;
        return g.ice((float) (wx / gp.tileSize), (float) (wy / gp.tileSize));
    }

    private float iceAtPlayer() {
        return iceAt(playerCX(), playerCY());
    }

    /** Boleh menginjak posisi entity (nx, ny)? Titik tengah hitbox harus di dalam biome es (atau minimal gak makin keluar). */
    private boolean territoryOk(int nx, int ny) {
        float there = iceAt(nx + solidArea.x + solidArea.width / 2.0, ny + solidArea.y + solidArea.height / 2.0);
        if (there >= TERRITORY_ICE) return true;
        float here = iceAt(centerX(), centerY());
        return here < TERRITORY_ICE && there >= here;   // lagi nyasar di luar: boleh asal makin masuk ke salju
    }

    // ───────────────────────── Update utama ─────────────────────────

    @Override
    public void update() {

        if (whiteFlashTimer > 0) whiteFlashTimer--;

        switch (state) {
            case ABSENT:
                updateAbsent();
                return;

            case DYING:
                dyingTimer++;
                if (dyingTimer >= DEAD_LIE_TICKS + DEAD_FADE_TICKS) becomeAbsent();
                return;

            case LEAVING:
                leaveTimer++;
                if (leaveTimer >= LEAVE_TICKS) becomeAbsent();
                return;

            default:
                break;
        }

        // ── Hidup ──
        moving = false;
        if (fadeIn < FADE_IN_TICKS) fadeIn++;
        if (attackCd > 0) attackCd--;
        if (lungeCd > 0) lungeCd--;
        if (provokedTimer > 0) provokedTimer--;

        boolean hunting = !gp.player.isDead;
        double dist = hunting ? distanceToPlayer() : Double.MAX_VALUE;
        boolean playerInIce = iceAtPlayer() > LEAVE_ICE;

        // player udah ninggalin biome es dan jauh: menghilang
        if (!playerInIce && dist > DESPAWN_DIST && state != WerewolfState.ATTACK) {
            startLeaving();
            return;
        }

        double aggro = AGGRO_RADIUS * (isNight() ? NIGHT_AGGRO : 1.0);

        switch (state) {
            case IDLE:
            case WANDER:
                wander();
                if (hunting && dist < aggro && iceAtPlayer() > LEAVE_ICE) {
                    state = WerewolfState.CHASE;
                    stuckTicks = 0;
                } else if (hunting && dist > RELOCATE_DIST && iceAtPlayer() > ACTIVATE_ICE) {
                    relocate();
                }
                break;

            case CHASE:
                if (!hunting || (dist > LOSE_RADIUS && provokedTimer == 0)) {
                    state = WerewolfState.WANDER;
                    break;
                }
                faceTowardPlayer();
                if (dist <= ATTACK_RANGE) {
                    stuckTicks = 0;
                    if (attackCd == 0) startAttack();
                    // kalau masih cooldown: diam di tempat, nunggu giliran mencakar lagi
                } else if (lungeCd == 0 && dist >= LUNGE_MIN && dist <= LUNGE_MAX) {
                    startLunge();
                } else {
                    double[] v = dirToPlayer();
                    double spd = enraged ? ENRAGE_SPEED : CHASE_SPEED;
                    if (step(v[0], v[1], spd)) {
                        moving = true;
                        stuckTicks = 0;
                    } else if (++stuckTicks > 90) {       // mentok (tembok / tepi salju): nyerah, jalan-jalan lagi
                        stuckTicks = 0;
                        provokedTimer = 0;
                        state = WerewolfState.WANDER;
                        pauseTimer = 30;
                    }
                }
                break;

            case LUNGE:
                updateLunge();
                break;

            case ATTACK:
                updateAttack();
                break;

            default:
                break;
        }

        tickAnimation();
    }

    private void updateAbsent() {
        if (respawnTimer > 0) {
            respawnTimer--;
            return;
        }
        if (gp.player.isDead || iceAtPlayer() < ACTIVATE_ICE) {
            appearCountdown = -1;       // player belum di biome es: undian jeda muncul diulang tiap masuk
            return;
        }
        if (appearCountdown < 0) appearCountdown = rand.nextInt(APPEAR_DELAY_MAX);
        if (appearCountdown > 0) {
            appearCountdown--;
            return;
        }
        if (placeNearPlayer()) {
            appearCountdown = -1;
            enterWorld();
        } else {
            appearCountdown = 40;       // belum ketemu tempat yang cocok: coba lagi sebentar lagi
        }
    }

    // ───────────────────────── Muncul / hilang ─────────────────────────

    /** Cari tempat muncul di daratan es sekitar player (di luar layar, gak nabrak collision). True kalau ketemu. */
    private boolean placeNearPlayer() {
        int pcx = playerCX();
        int pcy = playerCY();
        int margin = gp.tileSize * 2;

        for (int i = 0; i < 60; i++) {
            double ang  = rand.nextDouble() * Math.PI * 2;
            double dist = SPAWN_MIN_DIST + rand.nextDouble() * (SPAWN_MAX_DIST - SPAWN_MIN_DIST);
            int cx = pcx + (int) (Math.cos(ang) * dist);   // titik tengah hitbox
            int cy = pcy + (int) (Math.sin(ang) * dist);
            int nx = cx - solidAreaDefaultX - solidArea.width  / 2;
            int ny = cy - solidAreaDefaultY - solidArea.height / 2;

            if (cx < margin || cy < margin || cx > gp.worldWidth - margin || cy > gp.worldHeight - margin) continue;
            if (iceAt(cx, cy) < SPAWN_ICE) continue;                                    // harus di dalam daratan es
            if (blockedAt(nx, ny, solidAreaDefaultX, solidAreaDefaultY, gp.tileSize / 2)) continue;
            if (tooCloseToOtherWerewolf(cx, cy)) continue;

            x = nx;
            y = ny;
            spawnX = nx;
            spawnY = ny;
            homeX = cx;
            homeY = cy;
            return true;
        }
        return false;
    }

    private boolean tooCloseToOtherWerewolf(int cx, int cy) {
        if (gp.monsters == null) return false;
        for (Entity m : gp.monsters) {
            if (!(m instanceof Werewolf) || m == this) continue;
            Werewolf w = (Werewolf) m;
            if (!w.isAlive() && w.state != WerewolfState.LEAVING) continue;
            if (Math.hypot(w.centerX() - cx, w.centerY() - cy) < SPAWN_APART) return true;
        }
        return false;
    }

    /** Dipanggil setelah tempat muncul ketemu: hidupkan werewolf dengan HP penuh. */
    private void enterWorld() {
        life = maxLife;
        isDead = false;
        isKnockback = false;
        enraged = false;
        showHitbox();
        state = WerewolfState.IDLE;
        fadeIn = 0;
        attackCd = 60;                  // jangan langsung mencakar pas muncul
        lungeCd = 0;
        provokedTimer = 0;
        stuckTicks = 0;
        accX = accY = 0;
        pauseTimer = rand.nextInt(80);
        wanderTimer = 0;
        setDir(rand.nextInt(4));
    }

    /** Ketinggalan jauh dari player (tapi player masih di biome es): pindah ke dekat player lagi (fade in ulang). */
    private void relocate() {
        if (placeNearPlayer()) {
            fadeIn = 0;
            state = WerewolfState.IDLE;
            wanderTimer = 0;
            pauseTimer = rand.nextInt(60);
        }
    }

    /** Player pergi: kalau lagi ada & hidup, menghilang pelan-pelan. */
    private void startLeaving() {
        if (state == WerewolfState.ABSENT || state == WerewolfState.DYING || state == WerewolfState.LEAVING) return;
        state = WerewolfState.LEAVING;
        leaveTimer = 0;
        hideHitbox();      // gak bisa dipukul / dihalangi lagi selama menghilang (isDead tetap false biar masih digambar)
    }

    /** Beneran gak ada di dunia: semua kode lain menganggap werewolf ini gak ada. */
    private void becomeAbsent() {
        state = WerewolfState.ABSENT;
        isDead = true;
        hideHitbox();
    }

    private void hideHitbox() { solidArea.x = -9999; solidArea.y = -9999; }
    private void showHitbox() { solidArea.x = solidAreaDefaultX; solidArea.y = solidAreaDefaultY; }

    // ───────────────────────── Gerak ─────────────────────────

    private void wander() {
        if (pauseTimer > 0) {              // lagi diam (IDLE)
            pauseTimer--;
            state = WerewolfState.IDLE;
            return;
        }
        state = WerewolfState.WANDER;

        wanderTimer--;
        if (wanderTimer <= 0) {
            wanderTimer = 70 + rand.nextInt(110);
            if (rand.nextInt(3) == 0) {    // kadang berhenti dulu, ngendus-ngendus
                pauseTimer = 50 + rand.nextInt(100);
                return;
            }
            pickWanderDirection();
        }

        if (step(wanderDX, wanderDY, WANDER_SPEED)) {
            moving = true;
            faceVector(wanderDX, wanderDY);
        } else {
            wanderTimer = 0;               // mentok (tembok / tepi salju): cari arah baru
        }
    }

    private void pickWanderDirection() {
        double a;
        double toHome = Math.hypot(homeX - centerX(), homeY - centerY());
        if (toHome > HOME_RADIUS) {
            // kejauhan dari sarang: balik ke arah sarang, melenceng sedikit
            a = Math.atan2(homeY - centerY(), homeX - centerX()) + (rand.nextDouble() - 0.5) * 1.2;
        } else {
            a = rand.nextDouble() * Math.PI * 2;
        }
        wanderDX = Math.cos(a);
        wanderDY = Math.sin(a);
    }

    /** Jalan satu langkah ke arah (ux,uy) dengan kecepatan spd; geser nyusur tembok kalau mentok. */
    private boolean step(double ux, double uy, double spd) {
        accX += ux * spd;
        accY += uy * spd;
        int dx = (int) accX, dy = (int) accY;   // dibulatkan ke 0, sisanya disimpan buat frame berikutnya
        accX -= dx;
        accY -= dy;
        if (dx == 0 && dy == 0) return true;    // langkah terlalu kecil buat 1 piksel, tapi tetap dianggap jalan

        boolean moved = false;
        if (canStand(x + dx, y + dy)) {
            x += dx; y += dy; moved = true;
        } else {
            if (dx != 0 && canStand(x + dx, y)) { x += dx; moved = true; }
            if (dy != 0 && canStand(x, y + dy)) { y += dy; moved = true; }
        }
        return moved;
    }

    private boolean canStand(int nx, int ny) {
        return !blockedAt(nx, ny, solidArea.x, solidArea.y, 0) && territoryOk(nx, ny);
    }

    /** True kalau hitbox werewolf di posisi (nx,ny) nabrak dunia, player, atau monster lain. */
    private boolean blockedAt(int nx, int ny, int hbX, int hbY, int pad) {
        Rectangle box = new Rectangle(nx + hbX - pad, ny + hbY - pad, solidArea.width + pad * 2, solidArea.height + pad * 2);

        for (Rectangle r : gp.objCol.objects) {
            if (box.intersects(r)) return true;
        }

        if (!gp.player.isDead) {
            Rectangle pb = new Rectangle(gp.player.x + gp.player.solidArea.x, gp.player.y + gp.player.solidArea.y,
                                         gp.player.solidArea.width, gp.player.solidArea.height);
            if (box.intersects(pb)) return true;
        }

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m == this || m.isDead || m.solidArea == null) continue;
                Rectangle mb = new Rectangle(m.x + m.solidArea.x, m.y + m.solidArea.y, m.solidArea.width, m.solidArea.height);
                if (box.intersects(mb)) return true;
            }
        }
        return false;
    }

    // ───────────────────────── Jarak & arah ke player ─────────────────────────

    private int centerX() { return x + solidArea.x + solidArea.width  / 2; }
    private int centerY() { return y + solidArea.y + solidArea.height / 2; }

    private int playerCX() { return gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2; }
    private int playerCY() { return gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2; }

    private double distanceToPlayer() {
        return Math.hypot(playerCX() - centerX(), playerCY() - centerY());
    }

    private double[] dirToPlayer() {
        double dx = playerCX() - centerX(), dy = playerCY() - centerY();
        double len = Math.hypot(dx, dy);
        if (len == 0) return new double[] { 0, 0 };
        return new double[] { dx / len, dy / len };
    }

    private void faceTowardPlayer() {
        int dx = playerCX() - centerX(), dy = playerCY() - centerY();
        if (Math.abs(dx) + Math.abs(dy) > 8) faceVector(dx, dy);   // zona mati 8px biar gak kedip-kedip
    }

    private void setDir(int d) {
        dir = d;
        direction = WerewolfSprite.dirName(d);   // Entity.direction ikut disamakan
    }

    /**
     * Pilih arah hadap (kanan/kiri/atas/bawah) dari sebuah vektor (gerak atau arah ke target).
     * Sumbu yang sedang dipakai dipertahankan sampai sumbu lainnya jelas lebih dominan (1,5x),
     * supaya werewolf gak kedip-kedip atas<->kanan pas arahnya nyaris diagonal.
     */
    private void faceVector(double dx, double dy) {
        double ax = Math.abs(dx), ay = Math.abs(dy);
        if (ax + ay < 1e-6) return;
        boolean horizontal = (dir == WerewolfSprite.RIGHT || dir == WerewolfSprite.LEFT);
        boolean switchAxis = horizontal ? ay > ax * 1.5 : ax > ay * 1.5;
        boolean useHorizontal = switchAxis ? !horizontal : horizontal;
        if (useHorizontal) setDir(dx >= 0 ? WerewolfSprite.RIGHT : WerewolfSprite.LEFT);
        else               setDir(dy >= 0 ? WerewolfSprite.DOWN  : WerewolfSprite.UP);
    }

    // ───────────────────────── Terkam ─────────────────────────

    private void startLunge() {
        double[] v = dirToPlayer();
        lungeDX = v[0];
        lungeDY = v[1];
        lungeTicks = LUNGE_TICKS;
        state = WerewolfState.LUNGE;
        faceTowardPlayer();
    }

    private void updateLunge() {
        moving = true;
        boolean moved = step(lungeDX, lungeDY, LUNGE_SPEED);
        lungeTicks--;
        if (!moved || lungeTicks <= 0 || distanceToPlayer() <= ATTACK_RANGE) {
            lungeCd = LUNGE_COOLDOWN;
            state = WerewolfState.CHASE;
        }
    }

    // ───────────────────────── Serang ─────────────────────────

    private void startAttack() {
        state = WerewolfState.ATTACK;
        attackFrame = 0;
        attackTick = 0;
        strikeDone = false;
        attackVariant = (attackVariant == 3) ? 4 : 3;   // gantian: serangan player ke-3, ke-4, ke-3, ...
        faceTowardPlayer();
    }

    private void updateAttack() {
        attackTick++;
        int need = (attackFrame == 0) ? ATTACK_WINDUP_TICKS : ATTACK_FRAME_TICKS;
        if (attackTick < need) return;
        attackTick = 0;
        attackFrame++;

        // frame cakaran: damage masuk SEKALI, cuma kalau player masih dalam jangkauan
        if (attackFrame == STRIKE_FRAME && !strikeDone) {
            strikeDone = true;
            if (!gp.player.isDead && distanceToPlayer() <= ATTACK_RANGE + 34) {
                int dmg = isNight() ? (int) Math.round(ATTACK_DAMAGE * NIGHT_DAMAGE) : ATTACK_DAMAGE;
                gp.player.takeDamageFrom(dmg, this);
            }
        }

        if (attackFrame >= ATTACK_FRAMES) {
            attackFrame = 0;
            attackCd = enraged ? ENRAGE_COOLDOWN : ATTACK_COOLDOWN;
            state = WerewolfState.CHASE;
        }
    }

    // ───────────────────────── Kena damage & mati ─────────────────────────

    @Override
    public void takeDamage(int dmg, player attacker) {
        if (!isAlive()) return;     // lagi muncul/hilang/mati: gak bisa dipukul

        life -= dmg;
        whiteFlashTimer = HIT_FLASH_FRAMES;
        if (gp.damagePopups != null) gp.damagePopups.spawn(this, dmg, Main.DamagePopup.Type.ENEMY);

        if (life <= 0) {
            life = 0;
            die();
            return;
        }

        if (!enraged && life <= maxLife * ENRAGE_HP) enraged = true;

        provokedTimer = PROVOKED_TICKS;
        // lagi mencakar / menerkam: biarin animasinya selesai, jangan diinterupsi
        if (state != WerewolfState.ATTACK && state != WerewolfState.LUNGE) state = WerewolfState.CHASE;
    }

    private void die() {
        state = WerewolfState.DYING;
        dyingTimer = 0;
        respawnTimer = RESPAWN_TICKS;
        hideHitbox();
        gp.player.killMonster(this);   // exp, level up, koin, potion, bounty
    }

    // ───────────────────────── Animasi ─────────────────────────

    private void tickAnimation() {
        if (state == WerewolfState.ATTACK) return;
        if (moving) {
            // ngejar / menerkam = kaki gerak lebih cepat
            walkTick += (state == WerewolfState.CHASE || state == WerewolfState.LUNGE) ? 2 : 1;
            if (walkTick >= WALK_FRAME_TICKS) {
                walkTick = 0;
                walkFrame = (walkFrame + 1) % WerewolfSprite.WALK_FRAMES;
            }
            idleTick = 0;
        } else {
            idleTick++;
            if (idleTick >= IDLE_FRAME_TICKS) {
                idleTick = 0;
                idleFrame = (idleFrame + 1) % WerewolfSprite.IDLE_FRAMES;
            }
            walkTick = 0;
        }
    }

    private BufferedImage currentFrame() {
        int r = dir;    // baris sheet = arah hadap (kanan / kiri / atas / bawah)
        switch (state) {
            case DYING:  return sprite.dead[r][deadFrame()];
            case ATTACK:
                // (sengaja if/else, bukan ternary bertipe referensi: lihat catatan Eclipse di bawah)
                if (attackVariant == 3) return sprite.attack3[r][Math.min(attackFrame, ATTACK_FRAMES - 1)];
                return sprite.attack4[r][Math.min(attackFrame, ATTACK_FRAMES - 1)];
            case LUNGE:  return sprite.run[r][lungeFrame()];
            default:     break;
        }
        // (sengaja bukan ternary: compiler Eclipse pernah salah ngetik ternary bertipe referensi jadi Object -> VerifyError)
        if (moving) return sprite.walk[r][walkFrame % WerewolfSprite.WALK_FRAMES];
        return sprite.idle[r][idleFrame % WerewolfSprite.IDLE_FRAMES];
    }

    /** Frame terkam (frame lari player) dari progres loncatan: 3 frame dibagi rata sepanjang loncatan. */
    private int lungeFrame() {
        int elapsed = LUNGE_TICKS - lungeTicks;                       // 0 .. LUNGE_TICKS
        int f = elapsed * WerewolfSprite.RUN_FRAMES / (LUNGE_TICKS + 1);
        return Math.max(0, Math.min(WerewolfSprite.RUN_FRAMES - 1, f));
    }

    /** Frame mati dari timer: sempoyongan -> jatuh -> tergeletak (diam di frame terakhir). */
    private int deadFrame() {
        if (dyingTimer < 8)  return 0;
        if (dyingTimer < 16) return 1;
        return WerewolfSprite.DEAD_FRAMES - 1;
    }

    private float alpha() {
        switch (state) {
            case ABSENT:  return 0f;
            case LEAVING: return 1f - leaveTimer / (float) LEAVE_TICKS;
            case DYING:   return dyingTimer <= DEAD_LIE_TICKS ? 1f
                                : 1f - (dyingTimer - DEAD_LIE_TICKS) / (float) DEAD_FADE_TICKS;
            default:      return Math.min(1f, fadeIn / (float) FADE_IN_TICKS);
        }
    }

    // ───────────────────────── Gambar ─────────────────────────

    @Override
    public void draw(Graphics2D g2) {
        if (state == WerewolfState.ABSENT) return;

        float a = Math.max(0f, Math.min(1f, alpha()));
        if (a <= 0f) return;

        int sx = x - gp.player.x + gp.player.screenX;
        int sy = y - gp.player.y + gp.player.screenY;
        int ix = sx + drawOffX;
        int iy = sy + drawOffY;

        // di luar layar (ikut area pandang saat zoom): gak usah digambar
        if (ix + drawW < gp.viewMinX() - 50 || iy + drawH < gp.viewMinY() - 50
                || ix > gp.viewMaxX() + 50 || iy > gp.viewMaxY() + 50) return;

        Composite old = g2.getComposite();

        // bayangan di bawah kaki (merah kalau lagi ngamuk)
        if (state != WerewolfState.DYING) {
            int sw = (int) Math.round(sprite.scale * 30), sh = (int) Math.round(sprite.scale * 9);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (enraged ? 0.42f : 0.30f) * a));
            g2.setColor(enraged ? new Color(120, 0, 0) : Color.BLACK);
            g2.fillOval(sx + solidArea.x + solidArea.width / 2 - sw / 2, sy + footLocalY - sh / 2 - 1, sw, sh);
        }

        BufferedImage img = currentFrame();
        if (whiteFlashTimer > 0) img = HitFlash.whiteVersion(img);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g2.drawImage(img, ix, iy, null);
        g2.setComposite(old);

        // HP bar + nama (cuma pas hidup & sudah muncul penuh)
        if (isAlive() && fadeIn >= FADE_IN_TICKS) {
            int bw = 56, bh = 5;
            int bx = ix + (drawW - bw) / 2;
            int by = iy + sprite.headTopY - (int) Math.round(6 * sprite.scale);   // tepat di atas telinga
            if (life < maxLife) {
                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(bx, by, bw, bh);
                g2.setColor(enraged ? new Color(255, 70, 40) : Color.RED);
                g2.fillRect(bx, by, (int) ((double) life / maxLife * bw), bh);
            }
            if (showName) {
                g2.setFont(new Font("Dialog", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(name, bx + 4, by - 3);
            }
        }
    }
}
