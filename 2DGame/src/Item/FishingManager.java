package Item;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import Entitas.player;
import Item.FishingLoot.Kind;
import Item.FishingLoot.Rarity;
import Main.GamePanel;
import bahan.WaterCollision;

/**
 * MANCING.
 *
 * Cara main:
 *  1. Beli Fishing Rod di Armor Merchant (bayar Coin), taruh di hotbar.
 *  2. PEGANG pancingannya: tekan angka slotnya / klik slotnya (atau pakai dari backpack). Tekan lagi buat menyimpan.
 *     Selama dipegang, klik kiri gak nonjok lagi dan ada lingkaran bidik di air tempat mouse mengarah
 *     (hijau = bisa dilempar ke situ, merah = gak bisa).
 *  3. KLIK KIRI di air buat melempar pelampung ke titik itu. Syaratnya: paling jauh {@link #CAST_REACH_TILES} tile dari
 *     player dan paling dekat {@link #MIN_SHORE_DIST_TILES} tile dari daratan (gak boleh nempel pantai).
 *     Boleh mancing dari atas jembatan juga (dek jembatan itu bukan air dan bukan daratan).
 *  4. Tunggu pelampung. Kadang pelampung cuma "tuk" kecil (ikan nyenggol, belum nyambar). Bayangan ikan akan
 *     mendekat; pas ikan nyambar muncul tanda "!" dan pelampung tenggelam: KLIK lagi SECEPATNYA
 *     (cuma ada {@link #BITE_WINDOW_SEC} detik, ada lingkaran yang menyusut sebagai penunjuk). Kelamaan = ikannya kabur.
 *  5. Klik sebelum ada gigitan = tarik pancingan lagi (batal). Jalan / noleh / kena damage juga membatalkan.
 *
 * Apa yang ketarik ditentukan dari awal lempar (lihat {@link FishingLoot}), jadi ukuran dan kilau bayangan ikan
 * di air memang petunjuk hasilnya: bayangan gede / bersinar = barang langka.
 * Hasil tangkapan langsung masuk backpack, lalu muncul melompat dari air dengan efek sesuai kelangkaannya.
 *
 * Alurnya state machine: IDLE -> CASTING -> WAITING -> BITE -> REELING -> COOLDOWN -> IDLE.
 * Semua waktu dalam detik, dikali gp.FPS pas dipakai, jadi aman kalau FPS diubah.
 */
public class FishingManager {

    public enum State { IDLE, CASTING, WAITING, BITE, REELING, COOLDOWN }

    // ===== SETELAN (gampang diubah di sini) =====
    private static final double CAST_SEC         = 0.70;  // animasi lempar (ayun joran, pelampung terbang)
    private static final double WAIT_MIN_SEC     = 2.5;   // paling cepat ikan nyambar (setelah pelampung mendarat)
    private static final double WAIT_MAX_SEC     = 7.0;   // paling lama ikan nyambar
    private static final double RAIN_WAIT_FACTOR = 0.7;   // pas hujan ikan lebih agresif (waktu tunggu x0.7)
    private static final double APPROACH_SEC     = 1.7;   // bayangan ikan kelihatan segini lama sebelum nyambar
    private static final double BITE_WINDOW_SEC  = 1.2;   // waktu buat narik pancingan setelah ikan nyambar
    private static final double REEL_SEC         = 0.7;   // animasi narik ikan
    private static final double COOLDOWN_SEC     = 0.5;   // jeda sesudah selesai, biar klik gak langsung ngelempar lagi
    private static final double CATCH_FX_SEC     = 2.4;   // lama barang hasil tangkapan melayang di atas air
    private static final int    CAST_REACH_TILES = 7;     // sejauh apa pelampung bisa dilempar dari player (dalam tile)
    /**
     * Pelampung harus minimal segini jauh dari daratan mana pun (dalam tile, boleh pecahan). Dek jembatan GAK dihitung
     * daratan. Catatan: sungai kecil lebarnya cuma 3-4 tile, jadi kalau angka ini dinaikin ke 3 ke atas, sebagian besar
     * sungai gak bisa dipancing sama sekali.
     */
    private static final double MIN_SHORE_DIST_TILES = 2.0;

    /** Ukuran bayangan ikan (px) per kelangkaan. Urutan = Rarity.ordinal(): JUNK, COMMON, UNCOMMON, RARE, EPIC, LEGENDARY. */
    private static final int[] SHADOW_LENGTH = {13, 16, 20, 24, 30, 36};
    /** Jumlah percikan bintang pas barang muncul, per kelangkaan. */
    private static final int[] SPARKLES = {0, 0, 4, 10, 18, 30};

    private final GamePanel gp;
    private final Random rng = new Random();

    private State state = State.IDLE;
    private int timer = 0;          // sisa frame di state sekarang
    private int stateTick = 0;      // berapa frame sejak state sekarang mulai
    private int stateTotal = 1;     // panjang state sekarang (frame), buat progres animasi
    private int tick = 0;           // jam animasi (naik tiap update)

    // posisi pelampung target (dunia, pixel) + kondisi pas dilempar (buat deteksi pembatalan)
    private int bobberX, bobberY;
    private int castPX, castPY;
    private String castDir = "down";
    private int lastLife;
    private int dirX = 1, dirY = 0;          // arah lempar (satu dari 4 arah)

    // pancingan lagi DIPEGANG (dipilih lewat hotbar) + bidikan mouse
    private boolean rodReady = false;
    private int aimX, aimY;                  // titik bidik (dunia, px), sudah dijepit ke jangkauan lempar
    private boolean aimOk = false;           // titik bidik boleh dipakai buat melempar
    private String aimProblem = null;        // alasan kalau gak boleh

    // posisi pelampung SEKARANG (dunia). Beda dari target pas lagi terbang (CASTING) atau ditarik (REELING).
    private double bpx, bpy;
    private double releaseX, releaseY;       // titik ujung joran pas pelampung dilepas

    // ikan
    private FishingLoot.Entry pending;       // apa yang bakal ketarik (diundi pas lempar)
    private int waitTotal = 1;
    private int[] nibbleAt = new int[0];     // kapan (frame sejak mendarat) pelampung "tuk" kecil
    private int nibbleTick = -1;             // >=0 = lagi animasi "tuk"
    private double shadowAngle, shadowSpin;

    // efek
    private final List<Particle> particles = new ArrayList<>();
    private final List<Ripple> ripples = new ArrayList<>();
    private CatchFx catchFx;

    // geometri joran (dunia), dihitung ulang tiap frame di computeRod()
    private double handX, handY, tipX, tipY, ctlX, ctlY;

    public FishingManager(GamePanel gp) {
        this.gp = gp;
    }

    // ===================================================================
    // INPUT
    // ===================================================================

    /**
     * Dipakai item Fishing Rod (tekan angka slot hotbar / klik slotnya / pakai dari backpack):
     * pegang pancingan, atau simpan lagi kalau sudah dipegang. Rod-nya sendiri gak habis.
     */
    public void toggleRod() {
        if (rodReady) {
            if (state == State.REELING) return;                       // ikannya lagi ditarik, biar selesai dulu
            if (state != State.IDLE && state != State.COOLDOWN) cancel(null);
            rodReady = false;
            say("Rod put away.");
            return;
        }
        if (!hasRod()) {
            say("I need a fishing rod. The Armor Merchant sells them.");
            return;
        }
        if (gp.gameState != GamePanel.GameState.OUTSIDE) {
            say("I can't fish in here.");
            return;
        }
        rodReady = true;
        say("Rod ready. Click on the water to cast!");
    }

    /** True kalau pancingan lagi dipegang (klik kiri = mancing, bukan nonjok). Dipakai Hotbar buat nyalain slotnya. */
    public boolean isRodReady() {
        return rodReady;
    }

    /**
     * Klik kiri dari GamePanel (dipanggil sesudah semua UI lain gak nangkap kliknya).
     * Pancingan lagi diam = lempar ke titik yang diklik, nunggu = tarik lagi (batal), ikan nyambar = angkat.
     *
     * @return true kalau klik ini dipakai buat mancing, jadi GamePanel jangan lanjut nonjok.
     */
    public boolean handleClick(MouseEvent e) {
        if (!rodReady || e.getButton() != MouseEvent.BUTTON1) return false;
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return false;
        if (gp.hotbar != null && !gp.hotbar.canUse()) return false;   // dialog, menu, toko, mati, dll.

        switch (state) {
            case IDLE: {
                int[] w = screenToWorld(e.getX(), e.getY());
                cast(w[0], w[1]);
                break;
            }
            case WAITING:
                cancel("You reeled in too early.");
                break;
            case BITE:
                hook();
                break;
            case CASTING:
            case REELING:
            case COOLDOWN:
            default:
                break;
        }
        return true;
    }

    // ===================================================================
    // LOGIKA
    // ===================================================================

    /** Lempar pelampung ke titik dunia (mx, my) yang diklik. Gagal (dengan pesan) kalau titiknya gak memenuhi syarat. */
    private void cast(int mx, int my) {
        player p = gp.player;
        if (p == null || p.isDead) return;
        if (gp.gameState != GamePanel.GameState.OUTSIDE) {
            say("I can't fish in here.");
            return;
        }
        if (!p.isNotPunching()) return;

        evaluateAim(mx, my);
        if (!aimOk) {
            say(aimProblem);
            return;
        }
        bobberX = aimX;
        bobberY = aimY;

        // arah lempar = sumbu yang paling jauh dari pelampung ke player; player noleh ke arah itu
        int ts = gp.tileSize;
        int ddx = bobberX - (p.x + ts / 2);
        int ddy = bobberY - (p.y + ts / 2);
        if (Math.abs(ddx) < Math.abs(ddy)) { dirX = 0; dirY = Integer.signum(ddy); }
        else { dirX = Integer.signum(ddx); dirY = 0; }
        if (dirX == 0 && dirY == 0) dirX = 1;
        p.direction = dirY < 0 ? "up" : dirY > 0 ? "down" : dirX < 0 ? "left" : "right";

        castPX = p.x;
        castPY = p.y;
        castDir = p.direction;
        lastLife = p.life;

        pending = loot().roll(rng, rareBoost());   // apa yang bakal ketarik ditentukan dari sekarang (bayangan ikan = petunjuknya)
        nibbleTick = -1;
        enter(State.CASTING, CAST_SEC);
        updateBobberPos();                           // supaya frame pertama sudah punya posisi joran + pelampung
        FishingSfx.play("cast");
        say("Line cast. Click when something bites!");
    }

    /** Pelampung mendarat: mulai nunggu, jadwalkan "tuk" kecil dan arah datang bayangan ikan. */
    private void land() {
        bpx = bobberX;
        bpy = bobberY;
        splash(bpx, bpy, 12, 1.0, 0xDCEBFF);
        ring(bpx, bpy, 36, 28, 0);
        ring(bpx, bpy, 24, 22, 6);
        FishingSfx.play("splash");

        double wait = WAIT_MIN_SEC + rng.nextDouble() * (WAIT_MAX_SEC - WAIT_MIN_SEC);
        if (gp.world != null && gp.world.isRaining) wait *= RAIN_WAIT_FACTOR;
        enter(State.WAITING, wait);
        waitTotal = timer;

        int start = frames(0.7);
        int end = waitTotal - frames(APPROACH_SEC) - frames(0.3);
        int count = (end - start > frames(1.0)) ? 1 + rng.nextInt(2) : 0;
        nibbleAt = new int[count];
        for (int i = 0; i < count; i++) nibbleAt[i] = start + rng.nextInt(Math.max(1, end - start));
        pickShadowRoute();
    }

    /** Pilih arah datang bayangan ikan yang jalurnya paling banyak melewati air (jangan sampai bayangannya "jalan" di daratan). */
    private void pickShadowRoute() {
        int best = -1;
        double bestAngle = 0, bestSpin = 1;
        for (int i = 0; i < 16 && best < 9; i++) {
            shadowAngle = rng.nextDouble() * Math.PI * 2;
            shadowSpin = rng.nextBoolean() ? 1 : -1;
            int score = 0;
            for (int k = 0; k <= 8; k++) {
                double[] q = shadowPos(k / 8.0);
                if (isWaterAt((int) q[0], (int) q[1])) score++;
            }
            if (score > best) {
                best = score;
                bestAngle = shadowAngle;
                bestSpin = shadowSpin;
            }
        }
        shadowAngle = bestAngle;
        shadowSpin = bestSpin;
    }

    private void nibble() {
        nibbleTick = 0;
        ring(bpx, bpy, 15, 18, 0);
        FishingSfx.play("nibble");
    }

    private void bite() {
        splash(bpx, bpy, 16, 1.5, 0xDCEBFF);
        ring(bpx, bpy, 40, 26, 0);
        ring(bpx, bpy, 28, 22, 5);
        ring(bpx, bpy, 18, 18, 10);
        FishingSfx.play("bite");
        enter(State.BITE, BITE_WINDOW_SEC);
    }

    private void hook() {
        enter(State.REELING, REEL_SEC);
        splash(bpx, bpy, 14, 1.6, 0xDCEBFF);
        FishingSfx.play("reel");
    }

    /** Ikannya sampai di tepi: bagikan hasilnya, munculkan efek, lalu jeda. */
    private void landCatch() {
        FishingLoot loot = loot();
        FishingLoot.Entry e = pending != null ? pending : loot.roll(rng, rareBoost());
        pending = null;
        FishingLoot.Result r = loot.award(gp, e, rng);
        say(r.message);
        spawnCatchFx(r);
        FishingSfx.play(e.kind == Kind.JUNK ? "junk" : "catch_" + e.rarity.name().toLowerCase());
        startCooldown();
    }

    /** Batalin pancingan (pelampung ditarik balik). Pesan boleh null = diam saja. */
    public void cancel(String message) {
        if (state == State.IDLE) return;
        if (message != null) say(message);
        if (state == State.WAITING || state == State.BITE) {
            splash(bpx, bpy, 8, 0.8, 0xDCEBFF);
            ring(bpx, bpy, 20, 16, 0);
        }
        pending = null;
        startCooldown();
    }

    /** Batal tanpa pesan & tanpa jeda (dipakai kalau game di-load / respawn). */
    public void reset() {
        state = State.IDLE;
        rodReady = false;
        timer = 0;
        pending = null;
        nibbleTick = -1;
        particles.clear();
        ripples.clear();
        catchFx = null;
    }

    /** True selama tali lagi di air / di udara (dari lempar sampai ikan selesai ditarik). */
    public boolean isFishing() {
        return state == State.CASTING || state == State.WAITING || state == State.BITE || state == State.REELING;
    }

    public State getState() {
        return state;
    }

    private void enter(State s, double seconds) {
        state = s;
        timer = frames(seconds);
        stateTotal = timer;
        stateTick = 0;
    }

    private void startCooldown() {
        enter(State.COOLDOWN, COOLDOWN_SEC);
    }

    /** Dipanggil GamePanel.update() tiap frame di jalur update normal (dunia berhenti pas dialog / toko). */
    public void update() {
        tick++;
        updateEffects();
        if (rodReady && !canKeepRod()) rodReady = false;   // masuk rumah / domain, rod hilang, mati: pancingan disimpan sendiri
        if (state == State.IDLE) {
            if (rodReady) updateAim();
            return;
        }
        stateTick++;

        if (state == State.CASTING || state == State.WAITING || state == State.BITE) {
            String why = interruptReason();
            if (why != null) {
                cancel(why.isEmpty() ? null : why);
                return;
            }
        }

        if (state == State.WAITING) {
            int elapsed = waitTotal - timer;
            for (int at : nibbleAt) if (elapsed == at) nibble();
        }
        if (nibbleTick >= 0 && ++nibbleTick > 16) nibbleTick = -1;

        if (state == State.REELING && stateTick % 3 == 0) {
            splash(bpx, bpy, 3, 0.9, 0xDCEBFF);
        }

        updateBobberPos();

        timer--;
        if (timer > 0) return;

        switch (state) {
            case CASTING:
                land();
                break;
            case WAITING:
                bite();
                break;
            case BITE:
                say("It got away...");
                FishingSfx.play("miss");
                ring(bpx, bpy, 24, 20, 0);
                pending = null;
                startCooldown();
                break;
            case REELING:
                landCatch();
                break;
            case COOLDOWN:
                state = State.IDLE;
                break;
            default:
                break;
        }
    }

    /** null = aman lanjut. "" = batal diam-diam. Selain itu = batal + pesan. */
    private String interruptReason() {
        player p = gp.player;
        if (p == null || p.isDead) return "";
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return "";
        if (!hasRod() || !rodReady) return "";
        if (p.x != castPX || p.y != castPY) return "You walked away from your line.";
        if (p.direction != null && !p.direction.equals(castDir)) return "You turned away from your line.";
        if (p.life < lastLife) return "Fishing interrupted!";
        lastLife = p.life;
        return null;
    }

    private FishingLoot loot() {
        return ItemManager.get().fishingLoot;
    }

    /** Pengali peluang barang langka: hujan dan malam hari bikin hasil lebih bagus. */
    private double rareBoost() {
        double boost = 1.0;
        if (gp.world != null) {
            if (gp.world.isRaining) boost *= FishingLoot.RAIN_RARE_BOOST;
            if (gp.world.hour >= 20 || gp.world.hour < 5) boost *= FishingLoot.NIGHT_RARE_BOOST;
        }
        return boost;
    }

    private boolean hasRod() {
        ItemManager im = ItemManager.get();
        return im.FISHING_ROD != null && im.count(im.FISHING_ROD) > 0;
    }

    private int frames(double seconds) {
        return Math.max(1, (int) Math.round(seconds * gp.FPS));
    }

    private void say(String msg) {
        if (gp.speechBubble != null) gp.speechBubble.show(msg);
    }

    // ===================================================================
    // BIDIK (MOUSE) + CARI AIR
    // ===================================================================

    private boolean canKeepRod() {
        player p = gp.player;
        return p != null && !p.isDead && gp.gameState == GamePanel.GameState.OUTSIDE && hasRod();
    }

    /** Posisi mouse di layar -> posisi dunia (px). Ikut zoom (pivotnya tengah layar, lihat GamePanel.applyWorldZoom). */
    private int[] screenToWorld(int mx, int my) {
        player p = gp.player;
        double z = gp.renderZoom();
        double cx = gp.screenWidth / 2.0, cy = gp.screenHeight / 2.0;
        double ux = cx + (mx - cx) / z;           // koordinat layar sebelum di-zoom
        double uy = cy + (my - cy) / z;
        return new int[] { (int) Math.round(ux - p.screenX + p.x), (int) Math.round(uy - p.screenY + p.y) };
    }

    /** Tiap frame pas pancingan dipegang & belum dilempar: joran ngikut hadap player, bidikan ngikut mouse. */
    private void updateAim() {
        player p = gp.player;
        int[] w = screenToWorld(gp.mouseX, gp.mouseY);
        evaluateAim(w[0], w[1]);

        String dir = p.direction == null ? "down" : p.direction;
        dirX = dir.equals("left") ? -1 : dir.equals("right") ? 1 : 0;
        dirY = dir.equals("up") ? -1 : dir.equals("down") ? 1 : 0;
        if (dirX == 0 && dirY == 0) dirX = 1;
        computeRod();
    }

    /**
     * Cek titik bidik (dunia): dijepit ke jangkauan lempar, lalu harus air dan harus cukup jauh dari daratan.
     * Hasilnya di aimX / aimY / aimOk / aimProblem.
     */
    private void evaluateAim(int tx, int ty) {
        player p = gp.player;
        int ts = gp.tileSize;
        double cx = p.x + ts / 2.0, cy = p.y + ts / 2.0;
        double dx = tx - cx, dy = ty - cy;
        double reach = (double) ts * CAST_REACH_TILES;
        double dist = Math.hypot(dx, dy);
        boolean tooFar = dist > reach;
        if (tooFar) { dx *= reach / dist; dy *= reach / dist; }   // mouse kejauhan: bidik sejauh-jauhnya ke arah itu
        aimX = (int) Math.round(cx + dx);
        aimY = (int) Math.round(cy + dy);

        // titik pijakan = tengah kotak kaki player (sama kayak yang dipakai collision)
        int footX = p.x + p.solidArea.x + p.solidArea.width / 2;
        int footY = p.y + p.solidArea.y + p.solidArea.height / 2;

        aimOk = false;
        if (isWaterAt(footX, footY)) {
            aimProblem = "I need solid ground to fish from.";
        } else if (!isWaterAt(aimX, aimY)) {
            aimProblem = tooFar ? "That's out of my reach." : "I can only cast onto open water.";
        } else if (!farFromLand(aimX, aimY)) {
            aimProblem = "Too close to the shore. Cast at least " + tilesText(MIN_SHORE_DIST_TILES) + " tiles out.";
        } else {
            aimOk = true;
            aimProblem = null;
        }
    }

    private static String tilesText(double tiles) {
        return tiles == Math.floor(tiles) ? String.valueOf((int) tiles) : String.valueOf(tiles);
    }

    /** True kalau gak ada daratan dalam radius MIN_SHORE_DIST_TILES dari titik dunia (wx, wy). */
    private boolean farFromLand(int wx, int wy) {
        int cell = Math.max(2, gp.tileSize / WaterCollision.SUB);   // sekecil sel collision air (8px)
        double r = MIN_SHORE_DIST_TILES * gp.tileSize;
        int gx0 = Math.floorDiv((int) Math.floor(wx - r), cell), gx1 = Math.floorDiv((int) Math.ceil(wx + r), cell);
        int gy0 = Math.floorDiv((int) Math.floor(wy - r), cell), gy1 = Math.floorDiv((int) Math.ceil(wy + r), cell);
        for (int gy = gy0; gy <= gy1; gy++) {
            for (int gx = gx0; gx <= gx1; gx++) {
                int px = gx * cell + cell / 2, py = gy * cell + cell / 2;
                double ddx = px - wx, ddy = py - wy;
                if (ddx * ddx + ddy * ddy > r * r) continue;
                if (isLandAt(px, py)) return false;
            }
        }
        return true;
    }

    /** Tile dasar di titik dunia ini, atau -1 kalau di luar dunia. Baca dari peta dasar, jadi gak tergantung chunk salju / es yang lagi dimuat. */
    private int baseTileAt(int wx, int wy) {
        if (wx < 0 || wy < 0) return -1;
        int ts = gp.tileSize;
        int col = wx / ts, row = wy / ts;
        if (col >= gp.maxWorldCol || row >= gp.maxWorldRow) return -1;
        bahan.TileM tm = gp.tileM;
        return (tm.gen != null) ? tm.gen.baseTile(col, row) : tm.mapTileNum[col][row];
    }

    /** Titik ini air menurut peta dasar? (Dek jembatan masih dihitung air di sini, tile di bawahnya kan air.) */
    private boolean isWaterTileAt(int wx, int wy) {
        int t = baseTileAt(wx, wy);
        if (t < 0) return false;
        int ts = gp.tileSize;
        int col = wx / ts, row = wy / ts;
        return WaterCollision.isWaterAt(t, (wx - col * ts) / (float) ts, (wy - row * ts) / (float) ts);
    }

    private boolean onBridge(int wx, int wy) {
        return gp.tileM != null && gp.tileM.isOnBridgeDeck(wx, wy);
    }

    /** Air yang boleh dilempari pelampung / dilewati bayangan ikan. Dek jembatan BUKAN air. */
    private boolean isWaterAt(int wx, int wy) {
        return isWaterTileAt(wx, wy) && !onBridge(wx, wy);
    }

    /** Daratan: bukan air dan bukan dek jembatan. Di luar dunia dianggap daratan. */
    private boolean isLandAt(int wx, int wy) {
        return !isWaterTileAt(wx, wy) && !onBridge(wx, wy);
    }

    // ===================================================================
    // EFEK: percikan, riak, barang melompat
    // ===================================================================

    private static final class Particle {
        double x, y, z, vx, vy, vz;   // x,y = posisi di air (dunia); z = tinggi di atas air (px)
        int life, max;
        float size;
        int rgb;
        boolean star;                 // bintang kilau (tanpa gravitasi) / tetes air (jatuh)
    }

    private static final class Ripple {
        double x, y;
        int age, life;                // age negatif = belum mulai (jeda)
        float maxR;
    }

    private static final class CatchFx {
        FishingLoot.Entry entry;
        String line3;                 // mis. "+7 Coin"
        double wx, wy;                // titik air tempat barang muncul
        int age, life;
    }

    private static final int CATCH_FLIGHT_FRAMES = 16;
    private static final int HOVER_HEIGHT = 46;      // setinggi apa barang hasil tangkapan melayang di atas air (px)

    private void splash(double wx, double wy, int n, double power, int rgb) {
        for (int i = 0; i < n; i++) {
            Particle q = new Particle();
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = (0.4 + rng.nextDouble() * 1.2) * power;
            q.x = wx + Math.cos(a) * 2;
            q.y = wy + Math.sin(a) * 1;
            q.vx = Math.cos(a) * sp * 0.8;
            q.vy = Math.sin(a) * sp * 0.35;
            q.z = 1;
            q.vz = (2.0 + rng.nextDouble() * 2.6) * power;
            q.max = q.life = 22 + rng.nextInt(12);
            q.size = 2f + rng.nextFloat() * 1.5f;
            q.rgb = rgb;
            particles.add(q);
        }
    }

    private void sparkles(double wx, double wy, double height, int n, int rgb) {
        for (int i = 0; i < n; i++) {
            Particle q = new Particle();
            double a = rng.nextDouble() * Math.PI * 2;
            double sp = 0.8 + rng.nextDouble() * 2.2;
            q.x = wx;
            q.y = wy;
            q.z = height;
            q.vx = Math.cos(a) * sp;
            q.vz = Math.sin(a) * sp;
            q.max = q.life = 26 + rng.nextInt(20);
            q.size = 3f + rng.nextFloat() * 3f;
            q.rgb = rgb;
            q.star = true;
            particles.add(q);
        }
    }

    private void ring(double wx, double wy, float maxR, int life, int delay) {
        Ripple r = new Ripple();
        r.x = wx;
        r.y = wy;
        r.maxR = maxR;
        r.life = life;
        r.age = -delay;
        ripples.add(r);
    }

    private void spawnCatchFx(FishingLoot.Result r) {
        CatchFx fx = new CatchFx();
        fx.entry = r.entry;
        fx.line3 = r.extraLine;
        fx.wx = bpx;
        fx.wy = bpy;
        fx.life = frames(CATCH_FX_SEC);
        catchFx = fx;

        Rarity rar = r.entry.rarity;
        splash(bpx, bpy, 16, 1.7, 0xDCEBFF);
        ring(bpx, bpy, 44, 28, 0);
        ring(bpx, bpy, 30, 22, 5);
        if (rar.isRareOrBetter()) {
            ring(bpx, bpy, 60 + 10 * (rar.ordinal() - Rarity.RARE.ordinal()), 36, 3);
        }
        sparkles(bpx, bpy, HOVER_HEIGHT - 4, SPARKLES[rar.ordinal()], rar.rgb);
    }

    private void updateEffects() {
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle q = it.next();
            q.x += q.vx;
            q.y += q.vy;
            q.z += q.vz;
            if (q.star) {
                q.vx *= 0.94;
                q.vz = q.vz * 0.94 + 0.02;
            } else {
                q.vz -= 0.28;
                if (q.z <= 0 && q.vz < 0) q.life = 0;     // tetes air jatuh balik ke air
            }
            if (--q.life <= 0) it.remove();
        }
        for (Iterator<Ripple> it = ripples.iterator(); it.hasNext(); ) {
            Ripple r = it.next();
            if (++r.age >= r.life) it.remove();
        }
        if (catchFx != null && ++catchFx.age >= catchFx.life) catchFx = null;
        if (catchFx != null && catchFx.age == CATCH_FLIGHT_FRAMES) {
            // barang sampai di atas: kilau kedua (cuma barang langka)
            Rarity rar = catchFx.entry.rarity;
            if (rar.isRareOrBetter()) sparkles(catchFx.wx, catchFx.wy, HOVER_HEIGHT, SPARKLES[rar.ordinal()] / 2 + 2, rar.rgb);
        }
    }

    // ===================================================================
    // GEOMETRI JORAN + POSISI PELAMPUNG (dunia)
    // ===================================================================

    private static final double ROD_LEN = 34;
    private static final double PITCH_REST = Math.toRadians(30);

    private static double ease(double t) {            // easeOutCubic
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Sudut joran (dari mendatar) di state sekarang. Ayun belakang -> sentak depan -> turun lagi pas lempar. */
    private double pitch() {
        switch (state) {
            case CASTING: {
                double u = Math.min(1.0, stateTick / (double) stateTotal);
                if (u < 0.40) return lerp(PITCH_REST, Math.toRadians(88), ease(u / 0.40));
                if (u < 0.55) { double k = (u - 0.40) / 0.15; return lerp(Math.toRadians(88), Math.toRadians(5), k * k); }
                return lerp(Math.toRadians(5), PITCH_REST, ease((u - 0.55) / 0.45));
            }
            case BITE:
                return Math.toRadians(36) + Math.sin(tick * 1.6) * Math.toRadians(5);
            case REELING:
                return Math.toRadians(42) + Math.sin(stateTick * 0.45) * Math.toRadians(22);
            case COOLDOWN: {
                double k = 1.0 - timer / (double) Math.max(1, stateTotal);
                return lerp(PITCH_REST, Math.toRadians(85), ease(k));
            }
            default:
                return PITCH_REST + Math.sin(tick * 0.05) * Math.toRadians(1.5);
        }
    }

    private double bend() {
        switch (state) {
            case CASTING: {
                double u = stateTick / (double) stateTotal;
                return (u >= 0.40 && u < 0.58) ? -6 : 2;
            }
            case WAITING: return nibbleTick >= 0 ? 5 : 3;
            case BITE:    return 8 + Math.sin(tick * 1.9) * 2.5;
            case REELING: return 12;
            default:      return 2;
        }
    }

    /** Hitung tangan, ujung joran, dan titik kontrol lengkungnya (dunia). */
    private void computeRod() {
        player p = gp.player;
        int ts = gp.tileSize;
        double pitch = pitch();
        handX = p.x + ts / 2.0 + dirX * 8 + (dirY != 0 ? 7 : 0);       // joran dipegang tangan kanan
        handY = p.y + ts / 2.0 + 4;
        double c = Math.cos(pitch), s = Math.sin(pitch);
        tipX = handX + ROD_LEN * c * dirX + (dirY != 0 ? 6 * c : 0);
        tipY = handY + ROD_LEN * (dirY * 0.75 * c - s);

        double vx = tipX - handX, vy = tipY - handY;
        double len = Math.max(1, Math.hypot(vx, vy));
        double nx = -vy / len, ny = vx / len;
        if (ny < 0) { nx = -nx; ny = -ny; }                              // lengkung ke bawah
        double b = bend();
        ctlX = (handX + tipX) / 2 + nx * b;
        ctlY = (handY + tipY) / 2 + ny * b;
    }

    /** Posisi pelampung tiap frame sesuai state: menggantung, terbang melengkung, mengambang, atau diseret ke tepi. */
    private void updateBobberPos() {
        computeRod();
        switch (state) {
            case CASTING: {
                double u = Math.min(1.0, stateTick / (double) stateTotal);
                if (u < 0.5) {                                   // masih di ujung joran, menggantung
                    bpx = tipX;
                    bpy = tipY + 9;
                    releaseX = bpx;
                    releaseY = bpy;
                } else {
                    double s = Math.min(1.0, (u - 0.5) / 0.5);
                    double dist = Math.hypot(bobberX - releaseX, bobberY - releaseY);
                    bpx = lerp(releaseX, bobberX, s);
                    bpy = lerp(releaseY, bobberY, s) - 4 * (26 + dist * 0.22) * s * (1 - s);
                }
                break;
            }
            case WAITING:
            case BITE:
                bpx = bobberX;
                bpy = bobberY;
                break;
            case REELING: {
                double k = ease(stateTick / (double) stateTotal) * 0.45;
                double homeX = gp.player.x + gp.tileSize / 2.0 + dirX * 30;
                double homeY = gp.player.y + gp.tileSize / 2.0 + dirY * 30;
                bpx = lerp(bobberX, homeX, k) + Math.sin(stateTick * 1.3) * 3;
                bpy = lerp(bobberY, homeY, k) + Math.cos(stateTick * 1.7) * 2;
                break;
            }
            default:
                break;
        }
    }

    // ===================================================================
    // GAMBAR (dipanggil di dalam transform zoom dunia, sesudah player)
    // ===================================================================

    public void draw(Graphics2D g2) {
        player p = gp.player;
        if (p == null) return;
        boolean line = state != State.IDLE;
        boolean holding = rodReady && state == State.IDLE;        // pancingan dipegang, belum dilempar
        if (!line && !holding && particles.isEmpty() && ripples.isEmpty() && catchFx == null) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        java.awt.Stroke oldStroke = g2.getStroke();
        java.awt.Composite oldComp = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ox = p.screenX - p.x, oy = p.screenY - p.y;       // dunia -> layar

        drawRipples(g2, ox, oy);
        if (holding) drawAim(g2, ox, oy);
        if (state == State.WAITING) drawFishShadow(g2, ox, oy);
        if (line) drawLineAndRod(g2, ox, oy);
        else if (holding) drawHeldRod(g2, ox, oy);
        drawParticles(g2, ox, oy);
        if (state == State.BITE) drawBiteAlert(g2, bpx + ox, bpy + oy);       // di atas percikan, biar tetap kebaca
        if (catchFx != null) drawCatchFx(g2, ox, oy);

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    // ---------- bidikan mouse ----------

    /** Lingkaran bidik di air tempat mouse mengarah: hijau = boleh dilempar ke sini, merah = gak boleh. */
    private void drawAim(Graphics2D g2, int ox, int oy) {
        double x = aimX + ox, y = aimY + oy;
        double pulse = 0.5 + 0.5 * Math.sin(tick * 0.15);
        double w = 24 + pulse * 4, h = w * 0.42;
        Color c = aimOk ? new Color(120, 255, 150) : new Color(255, 90, 80);

        g2.setStroke(new BasicStroke(3f));
        g2.setColor(new Color(0, 0, 0, 90));
        g2.draw(new Ellipse2D.Double(x - w / 2, y - h / 2 + 1, w, h));
        g2.setStroke(new BasicStroke(1.8f));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 230));
        g2.draw(new Ellipse2D.Double(x - w / 2, y - h / 2, w, h));
        if (aimOk) {
            g2.fill(new Ellipse2D.Double(x - 1.5, y - 1, 3, 2));
        } else {                                                  // tanda silang kecil
            g2.draw(new java.awt.geom.Line2D.Double(x - 4, y - 2, x + 4, y + 2));
            g2.draw(new java.awt.geom.Line2D.Double(x - 4, y + 2, x + 4, y - 2));
        }
    }

    /** Joran dipegang tapi belum dilempar: cuma joran, tanpa tali dan pelampung. */
    private void drawHeldRod(Graphics2D g2, int ox, int oy) {
        drawRodBody(g2, handX + ox, handY + oy, tipX + ox, tipY + oy, ctlX + ox, ctlY + oy);
    }

    // ---------- riak ----------

    private void drawRipples(Graphics2D g2, int ox, int oy) {
        g2.setStroke(new BasicStroke(1.5f));
        // riak terus-menerus di sekitar pelampung (lebih cepat pas ikan nyambar)
        if (state == State.WAITING || state == State.BITE) {
            boolean biting = state == State.BITE;
            double speed = biting ? 0.22 : 0.05;
            int cx = (int) Math.round(bpx + ox), cy = (int) Math.round(bpy + oy) + 3;
            for (int i = 0; i < 2; i++) {
                double ph = (tick * speed + i * 0.5) % 1.0;
                int w = (int) (10 + ph * (biting ? 34 : 22));
                int h = w / 3;
                int alpha = (int) (150 * (1.0 - ph));
                g2.setColor(new Color(235, 245, 255, Math.max(0, alpha)));
                g2.drawOval(cx - w / 2, cy - h / 2, w, h);
            }
        }
        // riak sesaat (mendarat, nyambar, nyemplung)
        for (Ripple r : ripples) {
            if (r.age < 0) continue;
            double t = r.age / (double) r.life;
            double w = r.maxR * 2 * ease(t);
            double h = w * 0.42;
            int alpha = (int) (190 * (1 - t));
            g2.setColor(new Color(240, 248, 255, Math.max(0, Math.min(255, alpha))));
            g2.draw(new Ellipse2D.Double(r.x + ox - w / 2, r.y + oy - h / 2, w, h));
        }
    }

    // ---------- bayangan ikan ----------

    /** Posisi bayangan ikan (dunia) di progres u (0..1) pendekatan: dari jauh, melingkar, makin dekat ke pelampung. */
    private double[] shadowPos(double u) {
        double e = Math.pow(Math.max(0, Math.min(1, u)), 0.7);
        double r = lerp(62, 11, e);
        double a = shadowAngle + shadowSpin * u * 2.6;
        return new double[] {bobberX + Math.cos(a) * r, bobberY + Math.sin(a) * r * 0.55};
    }

    private void drawFishShadow(Graphics2D g2, int ox, int oy) {
        if (pending == null) return;
        int approach = frames(APPROACH_SEC);
        if (timer > approach) return;
        double u = 1.0 - timer / (double) approach;

        boolean junk = pending.kind == Kind.JUNK;
        double[] pos = shadowPos(u);
        double[] pre = shadowPos(Math.max(0, u - 0.04));
        double heading = Math.atan2((pos[1] - pre[1]) / 0.62, pos[0] - pre[0]);
        if (!isWaterAt((int) pos[0], (int) pos[1])) return;          // jangan gambar bayangan di atas daratan

        double fade = Math.min(1.0, u * 4);
        Rarity rar = pending.rarity;
        int len = SHADOW_LENGTH[rar.ordinal()];

        AffineTransform old = g2.getTransform();
        g2.translate(pos[0] + ox, pos[1] + oy);
        g2.scale(1, 0.62);                                           // perspektif: air dilihat miring dari atas
        g2.rotate(junk ? 0 : heading);

        if (rar.isRareOrBetter()) {                                 // kilau berdenyut di bawah bayangan langka
            double pulse = 0.65 + 0.35 * Math.sin(tick * 0.2);
            Color c = new Color(rar.rgb);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (70 * fade * pulse)));
            g2.fill(new Ellipse2D.Double(-len * 0.95, -len * 0.7, len * 1.9, len * 1.4));
        }

        g2.setColor(new Color(6, 22, 38, (int) (150 * fade)));
        switch (pending.kind) {
            case JUNK:
                g2.fill(new Ellipse2D.Double(-8, -3, 11, 7));
                g2.fill(new Ellipse2D.Double(-2, -5, 10, 9));
                g2.fill(new Ellipse2D.Double(1, -1, 8, 6));
                break;
            case TREASURE:
                g2.fill(new RoundRectangle2D.Double(-len * 0.45, -len * 0.3, len * 0.9, len * 0.6, 5, 5));
                break;
            case WEAPON:
                g2.fill(new Ellipse2D.Double(-len * 0.65, -2.2, len * 1.3, 4.4));
                g2.fill(new Ellipse2D.Double(-len * 0.38, -len * 0.3, 4, len * 0.6));     // gagang silang
                break;
            default: {                                               // ikan: badan + ekor yang bergoyang
                g2.fill(new Ellipse2D.Double(-len / 2.0, -len * 0.19, len, len * 0.38));
                double wig = Math.sin(tick * 0.55) * 0.45;
                AffineTransform t0 = g2.getTransform();
                g2.translate(-len / 2.0 + 1, 0);
                g2.rotate(wig);
                Path2D tail = new Path2D.Double();
                tail.moveTo(0, 0);
                tail.lineTo(-len * 0.34, -len * 0.2);
                tail.lineTo(-len * 0.34, len * 0.2);
                tail.closePath();
                g2.fill(tail);
                g2.setTransform(t0);
                Path2D fin = new Path2D.Double();
                fin.moveTo(-2, -len * 0.18);
                fin.lineTo(len * 0.08, -len * 0.36);
                fin.lineTo(len * 0.18, -len * 0.18);
                fin.closePath();
                g2.fill(fin);
                break;
            }
        }
        g2.setTransform(old);

        // kilau di barang bagus (peti, senjata): sesekali berkelip di atas bayangan
        if ((pending.kind == Kind.TREASURE || pending.kind == Kind.WEAPON) && (tick / 6) % 4 == 0) {
            drawStar(g2, pos[0] + ox, pos[1] + oy - 2, 4, new Color(255, 255, 255, (int) (230 * fade)));
        }
    }

    // ---------- tali, joran, pelampung ----------

    private void drawLineAndRod(Graphics2D g2, int ox, int oy) {
        boolean cooldown = state == State.COOLDOWN;
        float alpha = cooldown ? Math.max(0f, Math.min(1f, timer / (float) Math.max(1, stateTotal))) : 1f;
        java.awt.Composite old = g2.getComposite();
        if (alpha < 1f) g2.setComposite(AlphaComposite.SrcOver.derive(alpha));

        double hx = handX + ox, hy = handY + oy, tx = tipX + ox, ty = tipY + oy;
        double cx = ctlX + ox, cy = ctlY + oy;

        if (!cooldown) {
            double bx = bpx + ox, by = bpy + oy;
            if (state == State.CASTING) drawLandingMarker(g2, ox, oy);

            // tali: dari ujung joran ke pelampung (kendor pas nunggu, tegang pas ikan nyambar / ditarik)
            double sag;
            switch (state) {
                case CASTING: sag = (stateTick / (double) stateTotal < 0.5) ? 4 : 10; break;
                case BITE:    sag = 2; break;
                case REELING: sag = 1; break;
                default:      sag = nibbleTick >= 0 ? 8 : 14; break;
            }
            double lineEndY = by - 3 + bobDepth();
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(235, 235, 235, 200));
            g2.draw(new QuadCurve2D.Double(tx, ty, (tx + bx) / 2, Math.max(ty, lineEndY) + sag, bx, lineEndY));

            drawRodBody(g2, hx, hy, tx, ty, cx, cy);
            drawBobber(g2, bx, by);
        } else {
            drawRodBody(g2, hx, hy, tx, ty, cx, cy);
        }
        g2.setComposite(old);
    }

    /** Bayangan kecil di air tempat pelampung bakal mendarat (mengecil / menggelap saat pelampung makin dekat). */
    private void drawLandingMarker(Graphics2D g2, int ox, int oy) {
        double u = stateTick / (double) stateTotal;
        if (u < 0.5) return;
        double s = (u - 0.5) / 0.5;
        double w = lerp(7, 15, s);
        g2.setColor(new Color(10, 30, 50, (int) (40 + 60 * s)));
        g2.fill(new Ellipse2D.Double(bobberX + ox - w / 2, bobberY + oy - w / 5, w, w * 0.42));
    }

    /** Seberapa dalam pelampung tenggelam (px, positif = turun). */
    private double bobDepth() {
        switch (state) {
            case WAITING: {
                double d = Math.sin(tick * 0.12) * 1.3;
                if (nibbleTick >= 0) d += Math.sin(nibbleTick / 16.0 * Math.PI) * 3.5 + Math.sin(nibbleTick * 1.2) * 0.8;
                return d;
            }
            case BITE:
                return Math.min(7.5, stateTick * 1.4) + Math.sin(tick * 1.4) * 1.3;
            case REELING:
                return 1 + Math.sin(stateTick * 1.1) * 1.5;
            default:
                return 0;
        }
    }

    private void drawRodBody(Graphics2D g2, double hx, double hy, double tx, double ty, double cx, double cy) {
        int n = 12;
        double[] xs = new double[n + 1], ys = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            xs[i] = (1 - t) * (1 - t) * hx + 2 * (1 - t) * t * cx + t * t * tx;
            ys[i] = (1 - t) * (1 - t) * hy + 2 * (1 - t) * t * cy + t * t * ty;
        }
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            float w = (float) lerp(4.6, 1.5, t);
            Color col;
            if (t < 0.12) col = new Color(176, 132, 80);                          // gabus pegangan
            else if (t < 0.30) col = new Color(60, 38, 22);                       // gagang
            else col = new Color((int) lerp(96, 150, t), (int) lerp(64, 108, t), (int) lerp(38, 64, t));
            g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(col);
            g2.draw(new java.awt.geom.Line2D.Double(xs[i], ys[i], xs[i + 1], ys[i + 1]));
        }
        // gulungan (reel) + engkolnya (berputar pas narik)
        int ri = 4;
        double rx = xs[ri], ry = ys[ri] + 3.5;
        g2.setColor(new Color(40, 40, 46));
        g2.fill(new Ellipse2D.Double(rx - 4, ry - 4, 8, 8));
        g2.setColor(new Color(178, 186, 194));
        g2.fill(new Ellipse2D.Double(rx - 3, ry - 3, 6, 6));
        double ang = state == State.REELING ? stateTick * 0.9 : 0.6;
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(40, 40, 46));
        g2.draw(new java.awt.geom.Line2D.Double(rx, ry, rx + Math.cos(ang) * 5.5, ry + Math.sin(ang) * 5.5));
        // ujung joran
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(tx - 1.5, ty - 1.5, 3, 3));
    }

    private void drawBobber(Graphics2D g2, double bx, double by) {
        boolean inAir = state == State.CASTING;
        if (inAir) {
            paintBobber(g2, bx, by - 2);
            return;
        }
        double depth = bobDepth();
        double cy = by - 2 + depth;
        Shape oldClip = g2.getClip();
        java.awt.Composite oldComp = g2.getComposite();

        // bagian di atas air: normal
        g2.clipRect((int) bx - 14, (int) by - 40, 28, 40 + 1);
        paintBobber(g2, bx, cy);
        g2.setClip(oldClip);

        // bagian di bawah air: buram kebiruan
        g2.clipRect((int) bx - 14, (int) by + 1, 28, 30);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.38f));
        paintBobber(g2, bx, cy);
        g2.setComposite(oldComp);
        g2.setClip(oldClip);

        // garis air melingkari pelampung
        g2.setStroke(new BasicStroke(1.2f));
        g2.setColor(new Color(235, 245, 255, 190));
        g2.draw(new Ellipse2D.Double(bx - 8, by - 1, 16, 5));
    }

    private void paintBobber(Graphics2D g2, double bx, double cy) {
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(235, 235, 235));
        g2.draw(new java.awt.geom.Line2D.Double(bx, cy - 5, bx, cy - 10));          // antena
        g2.setColor(new Color(220, 50, 40));
        g2.fill(new Ellipse2D.Double(bx - 1.5, cy - 11.5, 3, 3));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(bx - 5, cy - 5, 10, 10));
        g2.setColor(new Color(210, 50, 45));
        g2.fill(new Arc2D.Double(bx - 5, cy - 5, 10, 10, 0, 180, Arc2D.PIE));
        g2.setColor(new Color(40, 25, 20));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new Ellipse2D.Double(bx - 5, cy - 5, 10, 10));
    }

    /** Tanda "!" + "CLICK" di atas pelampung, plus lingkaran yang menyusut = sisa waktu buat menarik. */
    private void drawBiteAlert(Graphics2D g2, double bx, double by) {
        int top = (int) by - 62 - (int) Math.round(Math.abs(Math.sin(tick * 0.35)) * 4);
        int cx = (int) bx;
        int midY = top + 17;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(cx - 17, top - 4, 34, 42, 12, 12);
        g2.setColor(new Color(255, 210, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(cx - 17, top - 4, 34, 42, 12, 12);

        // sisa waktu: lingkaran luar menyusut, hijau -> merah
        double frac = Math.max(0, Math.min(1, timer / (double) Math.max(1, stateTotal)));
        Color c = new Color((int) lerp(235, 80, frac), (int) lerp(60, 220, frac), (int) lerp(50, 90, frac));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0, 0, 0, 120));
        g2.draw(new Ellipse2D.Double(cx - 28, midY - 28, 56, 56));
        g2.setColor(c);
        g2.draw(new Arc2D.Double(cx - 28, midY - 28, 56, 56, 90, 360 * frac, Arc2D.OPEN));

        g2.setFont(new Font("Dialog", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(255, 80, 60));
        g2.drawString("!", cx - fm.stringWidth("!") / 2, top + 22);

        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString("CLICK", cx - fm.stringWidth("CLICK") / 2, top + 35);
    }

    // ---------- partikel ----------

    private void drawParticles(Graphics2D g2, int ox, int oy) {
        for (Particle q : particles) {
            double a = Math.max(0, Math.min(1, q.life / (double) q.max));
            double sx = q.x + ox, sy = q.y + oy - q.z;
            if (q.star) {
                Color c = new Color(q.rgb);
                drawStar(g2, sx, sy, q.size * (0.5 + 0.5 * a), new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * Math.min(1, a * 1.6))));
            } else {
                g2.setColor(new Color(220, 238, 255, (int) (220 * Math.min(1, a * 1.5))));
                g2.fill(new Ellipse2D.Double(sx - q.size / 2, sy - q.size / 2, q.size, q.size * 1.2));
            }
        }
    }

    /** Bintang kilau empat sudut. */
    private void drawStar(Graphics2D g2, double x, double y, double r, Color color) {
        Path2D star = new Path2D.Double();
        double in = r * 0.28;
        star.moveTo(x, y - r);
        star.lineTo(x + in, y - in);
        star.lineTo(x + r, y);
        star.lineTo(x + in, y + in);
        star.lineTo(x, y + r);
        star.lineTo(x - in, y + in);
        star.lineTo(x - r, y);
        star.lineTo(x - in, y - in);
        star.closePath();
        g2.setColor(color);
        g2.fill(star);
    }

    // ---------- barang hasil tangkapan melompat dari air ----------

    private static double easeOutBack(double t) {
        t = Math.max(0, Math.min(1, t));
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }

    private void drawCatchFx(Graphics2D g2, int ox, int oy) {
        CatchFx fx = catchFx;
        FishingLoot.Entry e = fx.entry;
        Rarity rar = e.rarity;
        Color rc = new Color(rar.rgb);
        int tier = rar.ordinal();

        double s = Math.min(1.0, fx.age / (double) CATCH_FLIGHT_FRAMES);
        double baseX = fx.wx + ox, baseY = fx.wy + oy;
        double hoverY = baseY - HOVER_HEIGHT;
        double x = baseX;
        double y = lerp(baseY, hoverY, easeOutBack(s));
        if (fx.age >= CATCH_FLIGHT_FRAMES) y += Math.sin((fx.age - CATCH_FLIGHT_FRAMES) * 0.12) * 2.5;

        int fadeFrames = Math.min(24, fx.life / 3);
        double alpha = 1.0;
        int left = fx.life - fx.age;
        if (left < fadeFrames) {
            alpha = Math.max(0, left / (double) fadeFrames);
            y -= (fadeFrames - left) * 0.6;
        }
        double scale = lerp(0.35, 1.0, easeOutBack(s));
        double spin = (1 - s) * (rar.isRareOrBetter() ? 6.3 : 3.1);

        java.awt.Composite oldComp = g2.getComposite();
        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);

        // 1) kilau di belakang barang (mulai dari UNCOMMON)
        if (tier >= Rarity.UNCOMMON.ordinal()) {
            float r = (float) (26 + tier * 4 + Math.sin(fx.age * 0.15) * 3);
            int a0 = (int) ((70 + tier * 28) * alpha * Math.min(1, s * 1.5));
            RadialGradientPaint glow = new RadialGradientPaint((float) x, (float) y, r,
                    new float[] {0f, 1f},
                    new Color[] {new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), Math.min(255, a0)),
                                 new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 0)});
            g2.setPaint(glow);
            g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        }

        // 2) sinar berputar (RARE ke atas)
        if (rar.isRareOrBetter() && fx.age >= CATCH_FLIGHT_FRAMES / 2) {
            int rays = 6 + (tier - Rarity.RARE.ordinal()) * 2;
            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(fx.age * 0.03);
            g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), (int) (60 * alpha)));
            for (int i = 0; i < rays; i++) {
                g2.rotate(Math.PI * 2 / rays);
                Path2D ray = new Path2D.Double();
                ray.moveTo(0, -8);
                ray.lineTo(-3, -34);
                ray.lineTo(3, -34);
                ray.closePath();
                g2.fill(ray);
            }
            g2.setTransform(old);
        }

        // 3) ikon barang
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(spin);
        g2.scale(scale, scale);
        g2.setComposite(AlphaComposite.SrcOver.derive((float) alpha));
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (e.item.icon != null) g2.drawImage(e.item.icon, -21, -21, 42, 42, null);
        g2.setTransform(old);
        if (oldInterp != null) g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);

        // 4) bintang yang mengorbit (EPIC / LEGENDARY)
        if (tier >= Rarity.EPIC.ordinal() && fx.age >= CATCH_FLIGHT_FRAMES) {
            int orbit = tier == Rarity.LEGENDARY.ordinal() ? 5 : 3;
            for (int i = 0; i < orbit; i++) {
                double a = fx.age * 0.09 + i * Math.PI * 2 / orbit;
                drawStar(g2, x + Math.cos(a) * 27, y + Math.sin(a) * 15,
                        3.5 + Math.sin(fx.age * 0.3 + i) * 1.2,
                        new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), (int) (230 * alpha)));
            }
        }

        // 5) teks di bawah ikon (di atasnya ada speech bubble pemain): nama, kelangkaan, info tambahan (Coin / damage)
        double textIn = Math.max(0, Math.min(1, (fx.age - CATCH_FLIGHT_FRAMES + 4) / 6.0));
        if (textIn > 0) {
            g2.setComposite(AlphaComposite.SrcOver.derive((float) (alpha * textIn)));
            g2.setFont(new Font("Dialog", Font.BOLD, 13));
            outlinedCentered(g2, e.item.name, x, y + 36, Color.WHITE);
            g2.setFont(new Font("Dialog", Font.BOLD, 10));
            outlinedCentered(g2, rar.label.toUpperCase() + (e.kind == Kind.WEAPON ? "  WEAPON" : ""), x, y + 48, rc);
            if (fx.line3 != null) {
                g2.setFont(new Font("Dialog", Font.BOLD, 11));
                outlinedCentered(g2, fx.line3, x, y + 61, new Color(255, 214, 90));
            }
        }
        g2.setComposite(oldComp);
    }

    private void outlinedCentered(Graphics2D g2, String text, double cx, double baseline, Color color) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (int) Math.round(cx - fm.stringWidth(text) / 2.0);
        int y = (int) Math.round(baseline);
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(text, x - 1, y);
        g2.drawString(text, x + 1, y);
        g2.drawString(text, x, y - 1);
        g2.drawString(text, x, y + 1);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }
}
