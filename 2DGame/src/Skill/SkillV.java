package Skill;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import Entitas.Entity;
import Entitas.Golemite;
import Entitas.Skeleton;
import Entitas.SkeletonKing;
import Entitas.player;
import Main.GamePanel;
import SFX.SoundManager;

/**
 * SkillV — "barrage". Dunia di-freeze, kamera zoom in, atmosfer jadi gelap-oranye kemerahan,
 * player teleport ke musuh terdekat lalu meninju bertubi-tubi dari berbagai sisi
 * (pindah sisi tiap 2-5 pukulan tanpa jeda), ditutup pukulan terakhir yang kuat + knockback,
 * lalu teleport balik ke posisi awal dan semuanya normal pelan-pelan.
 *
 * Animasi pukulan = loop frame attack player yang sudah ada (cuma diputar cepat),
 * jadi gak ada aset baru. Player kebal dan input dikunci selama skill (diatur di player.update()).
 *
 * Jalan di dunia luar (musuh dari gp.monsters) dan di dalam domain (skeleton, golemite,
 * skeleton boss room, dan Skeleton King dari DomainManager).
 */
public class SkillV extends Skill {

    // ===================================================================
    //  KONSTANTA (semua angka di sini, 1 tick = 1/40 detik)
    // ===================================================================

    // ----- WAKTU -----
    private static final int SAFETY_MAX_TICKS        = 1200; // batas aman, skill pasti kelar sebelum ini
    private static final int COOLDOWN_TICKS          = 600;  // 15 detik, mulai dihitung setelah skill kelar
    private static final int FREEZE_ZOOM_TICKS       = 14;   // freeze + zoom in
    private static final int ATMOSPHERE_TICKS        = 22;   // atmosfer berubah (gelap, tint, vignette)
    private static final int FINISHER_WINDUP_TICKS   = 8;    // ancang-ancang pukulan terakhir
    private static final int FINISHER_HITSTOP_TICKS  = 6;    // dunia "nahan napas" sesaat setelah pukulan terakhir kena
    private static final int KNOCKBACK_TICKS         = 10;   // lama musuh terpental
    private static final int FINISHER_SETTLE_TICKS   = 6;    // jeda singkat sebelum teleport balik
    private static final int RETURN_TICKS            = 50;   // zoom + atmosfer balik normal (pelan-pelan)

    // ----- ZOOM -----
    private static final float ZOOM_IN_TARGET        = 1.6f; // kalau zoom awal sudah lebih besar, dipakai yang awal

    // ----- TARGET & POSISI TELEPORT -----
    private static final int   TARGET_RANGE_TILES    = 10;   // musuh terdekat harus dalam jarak ini
    private static final int   STAND_GAP             = 6;    // jarak hitbox player ke hitbox musuh (px)
    private static final int[] SIDE_SHIFT_PX         = { 0, 12, -12, 24, -24 }; // geser menyamping kalau posisi pas kena collision
    private static final int   KNOCKBACK_ROOM_CHECK  = 160;  // cek ruang dorong (px) buat milih sisi pukulan terakhir
    private static final int   RETURN_SEARCH_RADIUS  = 144;  // cari titik aman terdekat kalau posisi awal ternyata kena collision (px)
    private static final int   RETURN_SEARCH_STEP    = 8;

    // ----- BARRAGE -----
    private static final int   HIT_COUNT_MIN         = 30;   // total pukulan (termasuk pukulan terakhir)
    private static final int   HIT_COUNT_MAX         = 40;
    private static final int   SEGMENT_HITS_MIN      = 2;    // teleport ke sisi lain tiap 2-5 pukulan
    private static final int   SEGMENT_HITS_MAX      = 5;
    private static final int   ANIM_FRAME_TICKS      = 2;    // ganti frame tiap 2 tick
    // Urutan frame attack yang di-loop (index 0-3 dari attackXFrames): 0,1 = tangan di belakang, 2,3 = tangan lurus.
    // Urutan selang-seling ini bikin tiap 2 frame ada 1 pukulan kena (tiap 4 tick = 10 pukulan/detik).
    private static final int[] PUNCH_FRAME_SEQUENCE  = { 0, 2, 1, 3 };
    private static final int   HIT_FRAME_MIN_INDEX   = 2;    // pukulan "kena" tiap frame berganti ke index >= ini
    private static final int   FINISHER_WINDUP_FRAME = 1;
    private static final int   FINISHER_HIT_FRAME    = 3;
    private static final float HIT_DAMAGE_MULT       = 0.5f; // damage tiap pukulan = totalDamage player * ini (min 1)
    private static final float FINISHER_DAMAGE_MULT  = 4f;   // damage pukulan terakhir
    private static final float KNOCKBACK_START_SPEED = 22f;  // px/tick di awal, turun linear sampai 0

    // ----- AFTERIMAGE -----
    private static final int   GHOST_MAX             = 60;
    private static final int   GHOST_LIFE            = 6;    // bayangan tiap tick pas nonjok
    private static final float GHOST_ALPHA           = 0.38f;
    private static final int   GHOST_JITTER_PX       = 9;    // bayangan sedikit geser acak -> kesan banyak tangan
    private static final int   TELEPORT_GHOST_LIFE   = 12;   // bayangan di posisi lama pas teleport
    private static final float TELEPORT_GHOST_ALPHA  = 0.75f;
    private static final int   TELEPORT_TRAIL_GHOSTS = 3;    // bayangan di sepanjang jalur teleport
    private static final int   STREAK_LIFE           = 5;    // garis kilat jalur teleport
    private static final Color GHOST_TINT            = new Color(255, 110, 50, 190);

    // ----- ATMOSFER (overlay layar penuh) -----
    private static final float DARK_ALPHA            = 0.30f; // layar gelap merata
    private static final float TINT_ALPHA            = 0.38f; // tint oranye kemerahan
    private static final Color TINT_TOP              = new Color(255, 120, 40);
    private static final Color TINT_BOTTOM           = new Color(190, 30, 15);
    private static final Color VIGNETTE_MID          = new Color(60, 10, 0, 110);
    private static final Color VIGNETTE_EDGE         = new Color(15, 0, 0, 235);

    // ----- PARTIKEL -----
    private static final int   PARTICLE_MAX          = 140;
    private static final int   PARTICLE_SPAWN_PER_TICK = 3;
    private static final Color[] EMBER_COLORS = {
        new Color(255, 190, 80), new Color(255, 120, 40), new Color(255, 235, 200), new Color(230, 70, 30)
    };

    // ----- SPEED LINES -----
    private static final int   SPEED_LINE_COUNT      = 30;
    private static final int   SPEED_LINE_REFRESH_TICKS = 2;   // posisi garis diacak ulang tiap N tick (efek manga)
    private static final float SPEED_LINE_INNER_MIN  = 0.30f;  // jarak mulai garis dari fokus (fraksi setengah diagonal layar)
    private static final float SPEED_LINE_INNER_MAX  = 0.62f;
    private static final float SPEED_LINE_LEN_MIN    = 0.18f;
    private static final float SPEED_LINE_LEN_MAX    = 0.55f;
    private static final float SPEED_LINE_HALF_ANGLE_MIN = 0.006f; // lebar garis (radian)
    private static final float SPEED_LINE_HALF_ANGLE_MAX = 0.020f;
    private static final float SPEED_LINE_ALPHA      = 0.55f;
    private static final Color SPEED_LINE_COLOR      = new Color(255, 235, 210);
    private static final Color SPEED_LINE_HOT        = new Color(255, 150, 70);

    // ----- KILATAN / SPARK -----
    private static final int   FIRST_FLASH_TICKS     = 8;     // impact frame putih pas teleport pertama
    private static final float FIRST_FLASH_STRENGTH  = 1.0f;
    private static final int   HIT_FLASH_TICKS       = 2;     // kilat tipis tiap pukulan (set strength 0 buat matiin)
    private static final float HIT_FLASH_STRENGTH    = 0.12f;
    private static final int   FINISH_FLASH_TICKS    = 10;
    private static final float FINISH_FLASH_STRENGTH = 1.0f;
    private static final int   RETURN_FLASH_TICKS    = 6;
    private static final float RETURN_FLASH_STRENGTH = 0.6f;
    private static final int   SPARK_MAX             = 40;
    private static final int   SPARK_LIFE            = 5;
    private static final float SPARK_SIZE            = 26f;
    private static final float SPARK_SIZE_BIG        = 70f;
    private static final int   SHOCK_RING_TICKS      = 12;    // cincin kejut pukulan terakhir
    private static final float SHOCK_RING_MAX_RADIUS = 150f;
    private static final float CHARGE_RING_START     = 90f;   // cincin ancang-ancang mengecil ke player
    private static final float CHARGE_RING_END       = 12f;
    private static final Color SPARK_COLOR           = new Color(255, 250, 220);
    private static final Color SPARK_COLOR_HOT       = new Color(255, 170, 70);

    // ===================================================================
    //  TIPE BANTU
    // ===================================================================

    private enum Phase { FREEZE_ZOOM, ATMOSPHERE, BARRAGE, FINISH_WINDUP, FINISH_IMPACT, RETURN }

    // Sisi musuh tempat player berdiri. facing = arah hadap player; (sx, sy) = arah dari musuh ke player.
    private enum Side {
        UP("down", 0, -1), DOWN("up", 0, 1), LEFT("right", -1, 0), RIGHT("left", 1, 0);

        final String facing;
        final int sx, sy;

        Side(String facing, int sx, int sy) {
            this.facing = facing;
            this.sx = sx;
            this.sy = sy;
        }
    }

    private static class Placement {
        final int x, y;   // posisi player (owner.x, owner.y)
        final Side side;

        Placement(int x, int y, Side side) {
            this.x = x;
            this.y = y;
            this.side = side;
        }
    }

    private static class Ghost {
        int x, y;         // posisi dunia (owner.x, owner.y) waktu bayangan dibuat
        String dir;
        int frame;
        float alpha;
        int life, maxLife;

        Ghost(int x, int y, String dir, int frame, float alpha, int life) {
            this.x = x;
            this.y = y;
            this.dir = dir;
            this.frame = frame;
            this.alpha = alpha;
            this.life = life;
            this.maxLife = life;
        }
    }

    private static class Particle {
        float x, y, vx, vy;   // koordinat layar
        float size;
        int life, maxLife;
        Color color;
    }

    private static class Spark {
        int wx, wy;           // koordinat dunia titik pukulan kena
        int life, maxLife;
        float size, rot;
        boolean big;
    }

    private static class Streak {
        int x0, y0, x1, y1;   // koordinat dunia
        int life, maxLife;
    }

    // ===================================================================
    //  STATE
    // ===================================================================

    private final Random rng = new Random();
    private final SoundManager sound = new SoundManager();

    private Phase phase = Phase.FREEZE_ZOOM;
    private int phaseTimer = 0;
    private int fxTick = 0;
    private boolean started = false;   // true selama skill beneran jalan (biar cleanup aman dipanggil berulang)
    private boolean frozen = false;    // true = dunia (monster, NPC, minion) berhenti

    private Entity target;
    private Placement firstPlacement;
    private Side currentSide;

    // Dunia tempat skill dimulai (OUTSIDE / DOMAIN, di domain juga dicatat lagi di boss room atau nggak)
    private GamePanel.GameState startState;
    private boolean startInRoom;

    // Posisi & kondisi awal, dikembalikan di akhir skill
    private int originX, originY;
    private String originDirection;
    private float savedZoom = 1f;
    private float zoomInTarget = 1f;
    private float lastZoomSet = -1f;

    // Barrage
    private int totalHits, hitsDone, segmentHitsLeft;
    private int animTick, seqIndex, punchFrame;
    private int knockDx, knockDy;

    // Efek layar
    private float atmo = 0f;           // intensitas atmosfer 0..1
    private int flashTimer, flashTotal;
    private float flashStrength;
    private int shockTimer, shockX, shockY;

    private final List<Ghost> ghosts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Streak> streaks = new ArrayList<>();

    private final float[] slAngle = new float[SPEED_LINE_COUNT];
    private final float[] slInner = new float[SPEED_LINE_COUNT];
    private final float[] slLen   = new float[SPEED_LINE_COUNT];
    private final float[] slHalf  = new float[SPEED_LINE_COUNT];

    private Paint tintPaint;
    private Paint vignettePaint;

    public SkillV(GamePanel gp, player owner) {
        super(gp, owner);

        cooldownMax = COOLDOWN_TICKS;
        activeDuration = SAFETY_MAX_TICKS; // skill berhenti sendiri lewat finishSkill(), ini cuma pengaman

        // Tanpa aset baru: semua efek digambar lewat kode
        imagePath = "";
    }

    // ===================================================================
    //  AKTIVASI
    // ===================================================================

    @Override
    public void activate() {
        if (cooldown > 0 || isActive) return;
        // Musuh ada di dunia luar (gp.monsters) dan di dalam domain (DomainManager). Rumah gak ada musuh.
        if (gp.gameState != GamePanel.GameState.OUTSIDE && gp.gameState != GamePanel.GameState.DOMAIN) return;
        if (!gp.canZoom()) return; // ada UI/dialog/shop/transisi yang lagi kebuka
        if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null && gp.domainManager.isFading()) return;
        if (!owner.canUseSkill()) return;

        // Cari musuh terdekat yang punya sisi pukul valid. Gak ada -> skill gak jalan & cooldown gak kepakai.
        if (!pickTarget()) {
            Main.Log.d("[SkillV] No Enemies Arround.");
            return;
        }

        owner.cancelAttack();
        super.activate();
    }

    @Override
    protected void onActivate() {
        started = true;
        frozen = true;
        fxTick = 0;
        atmo = 0f;
        flashTimer = 0;
        shockTimer = 0;
        ghosts.clear();
        particles.clear();
        sparks.clear();
        streaks.clear();
        reseedSpeedLines();

        originX = owner.x;
        originY = owner.y;
        originDirection = owner.direction;
        currentSide = null;
        startState = gp.gameState;
        startInRoom = isInDomainRoom();

        // Simpan zoom awal (targetZoom, bukan zoom yang lagi bergerak) buat dikembalikan nanti
        savedZoom = gp.targetZoom;
        zoomInTarget = Math.max(GamePanel.ZOOM_MIN, Math.min(GamePanel.ZOOM_MAX, Math.max(savedZoom, ZOOM_IN_TARGET)));
        lastZoomSet = -1f;

        totalHits = HIT_COUNT_MIN + rng.nextInt(HIT_COUNT_MAX - HIT_COUNT_MIN + 1);
        hitsDone = 0;
        punchFrame = PUNCH_FRAME_SEQUENCE[0];

        changePhase(Phase.FREEZE_ZOOM);
        sound.playUltimateStart(); // pembukaan ultimate
        Main.Log.d("[SkillV] Activated! Total pukulan: " + totalHits);
    }

    // ===================================================================
    //  UPDATE
    // ===================================================================

    @Override
    protected void onActiveUpdate() {
        // Jaga-jaga: kalau dunia berubah di tengah skill (masuk rumah, keluar domain/boss room), batalin tanpa teleport balik
        if (gp.gameState != startState || isInDomainRoom() != startInRoom || owner.isDead) {
            abort();
            return;
        }

        cooldown = cooldownMax; // cooldown baru mulai jalan setelah skill kelar
        phaseTimer++;
        updateEffects();

        switch (phase) {
            case FREEZE_ZOOM:   updateFreezeZoom();      break;
            case ATMOSPHERE:    updateAtmosphere();      break;
            case BARRAGE:       updateBarrage();         break;
            case FINISH_WINDUP: updateFinisherWindup();  break;
            case FINISH_IMPACT: updateFinisherImpact();  break;
            case RETURN:        updateReturn();          break;
        }
    }

    @Override
    protected void onEnd() {
        // Cuma kepanggil kalau batas aman kelewat
        sound.stopSkillVSounds();
        cleanup();
        cooldown = cooldownMax;
        Main.Log.d("[SkillV] Ended (batas aman).");
    }

    private void changePhase(Phase next) {
        phase = next;
        phaseTimer = 0;
    }

    // ----- 1. Freeze + zoom in -----
    private void updateFreezeZoom() {
        float t = Math.min(1f, phaseTimer / (float) FREEZE_ZOOM_TICKS);
        setZoom(lerp(savedZoom, zoomInTarget, easeOut(t)));
        if (phaseTimer >= FREEZE_ZOOM_TICKS) changePhase(Phase.ATMOSPHERE);
    }

    // ----- 2. Atmosfer berubah -----
    private void updateAtmosphere() {
        float t = Math.min(1f, phaseTimer / (float) ATMOSPHERE_TICKS);
        atmo = smooth(t);
        setZoom(zoomInTarget);
        if (phaseTimer >= ATMOSPHERE_TICKS) startBarrage();
    }

    // ----- 3. Teleport ke musuh (impact frame putih) -----
    private void startBarrage() {
        if (!isTargetable(target)) { beginReturn(); return; }

        Placement p = pickFirstPlacement(target);
        if (p == null) p = firstPlacement;
        if (p == null) { beginReturn(); return; }

        teleportTo(p);
        triggerFlash(FIRST_FLASH_TICKS, FIRST_FLASH_STRENGTH);
        sound.playMultiplePunch(); // sampai di tempat mukul yang pertama

        hitsDone = 0;
        segmentHitsLeft = randInt(SEGMENT_HITS_MIN, SEGMENT_HITS_MAX);
        animTick = 0;
        seqIndex = 0;
        punchFrame = PUNCH_FRAME_SEQUENCE[0];
        changePhase(Phase.BARRAGE);
    }

    // ----- 4. Barrage: loop frame attack cepat + teleport antar sisi tanpa jeda -----
    private void updateBarrage() {
        if (!isTargetable(target)) { // musuh keburu mati -> langsung tutup skill
            bigImpactFx();
            beginReturn();
            return;
        }

        spawnPunchGhost(GHOST_ALPHA);

        animTick++;
        if (animTick >= ANIM_FRAME_TICKS) {
            animTick = 0;
            seqIndex = (seqIndex + 1) % PUNCH_FRAME_SEQUENCE.length;
            punchFrame = PUNCH_FRAME_SEQUENCE[seqIndex];
            if (punchFrame >= HIT_FRAME_MIN_INDEX) landPunch();
        }
    }

    private void landPunch() {
        hitsDone++;

        int dmg = Math.max(1, Math.round(owner.getTotalDamage() * HIT_DAMAGE_MULT));
        target.takeDamage(dmg);

        spawnSpark(false);
        triggerFlash(HIT_FLASH_TICKS, HIT_FLASH_STRENGTH);

        // Pukulan terakhir (yang kuat) dipisah, ini masih pukulan biasa
        if (hitsDone >= totalHits - 1) {
            beginFinisher();
            return;
        }

        // Tiap 2-5 pukulan pindah ke sisi lain. Animasi gak di-reset -> lanjut mukul tanpa jeda.
        segmentHitsLeft--;
        if (segmentHitsLeft <= 0) {
            Placement p = chooseNextSide(false);
            if (p != null) {
                teleportTo(p);
                sound.playMultiplePunch(); // pindah ke tempat mukul yang baru
            }
            segmentHitsLeft = randInt(SEGMENT_HITS_MIN, SEGMENT_HITS_MAX);
        }
    }

    // ----- 5. Pukulan terakhir: ancang-ancang, kena, hit-stop, musuh terpental -----
    private void beginFinisher() {
        // Pilih sisi yang di belakang musuhnya paling lega supaya knockback kelihatan
        Placement p = chooseNextSide(true);
        if (p != null && p.side != currentSide) teleportTo(p);

        punchFrame = FINISHER_WINDUP_FRAME;
        changePhase(Phase.FINISH_WINDUP);
    }

    private void updateFinisherWindup() {
        if (!isTargetable(target)) {
            bigImpactFx();
            beginReturn();
            return;
        }

        spawnPunchGhost(GHOST_ALPHA + 0.15f);

        if (phaseTimer >= FINISHER_WINDUP_TICKS) {
            punchFrame = FINISHER_HIT_FRAME;
            dealFinisher();
            changePhase(Phase.FINISH_IMPACT);
        }
    }

    private void dealFinisher() {
        int dmg = Math.max(1, Math.round(owner.getTotalDamage() * FINISHER_DAMAGE_MULT));
        target.takeDamage(dmg);
        hitsDone++;

        // Arah dorong = arah hadap player (boss gak ikut terpental, sama kayak pukulan biasa)
        boolean pushable = !(target instanceof SkeletonKing);
        knockDx = pushable ? -currentSide.sx : 0;
        knockDy = pushable ? -currentSide.sy : 0;

        bigImpactFx();
        Main.Log.d("[SkillV] Pukulan terakhir! dmg=" + dmg);
    }

    private void bigImpactFx() {
        spawnSpark(true);
        triggerFlash(FINISH_FLASH_TICKS, FINISH_FLASH_STRENGTH);
        sound.playLastPunch();

        if (target != null) {
            shockX = target.x + target.solidArea.x + target.solidArea.width / 2;
            shockY = target.y + target.solidArea.y + target.solidArea.height / 2;
            shockTimer = SHOCK_RING_TICKS;
        }
    }

    private void updateFinisherImpact() {
        // Hit-stop dulu, baru musuh terpental
        if (target != null && phaseTimer > FINISHER_HITSTOP_TICKS && phaseTimer <= FINISHER_HITSTOP_TICKS + KNOCKBACK_TICKS) {
            int k = phaseTimer - FINISHER_HITSTOP_TICKS - 1; // 0..KNOCKBACK_TICKS-1
            float speed = KNOCKBACK_START_SPEED * (1f - k / (float) KNOCKBACK_TICKS);
            slideTarget(Math.round(speed));
        }

        if (phaseTimer >= FINISHER_HITSTOP_TICKS + KNOCKBACK_TICKS + FINISHER_SETTLE_TICKS) beginReturn();
    }

    // Geser musuh `px` pixel searah knockback, berhenti kalau nabrak collision dunia
    private void slideTarget(int px) {
        for (int i = 0; i < px; i++) {
            int nx = target.x + knockDx;
            int ny = target.y + knockDy;
            if (gp.colision.isBlockedByWorld(target, nx, ny)) break;
            target.x = nx;
            target.y = ny;
        }
    }

    // ----- 6. Teleport balik, zoom & atmosfer normal pelan-pelan -----
    private void beginReturn() {
        int[] back = findReturnPosition();

        spawnTeleportTrail(owner.x, owner.y, back[0], back[1], owner.direction, punchFrame);
        owner.x = back[0];
        owner.y = back[1];
        if (originDirection != null) owner.direction = originDirection;

        sound.playWoosh();
        triggerFlash(RETURN_FLASH_TICKS, RETURN_FLASH_STRENGTH);

        frozen = false; // waktu jalan lagi
        changePhase(Phase.RETURN);
    }

    private void updateReturn() {
        float t = Math.min(1f, phaseTimer / (float) RETURN_TICKS);
        atmo = 1f - smooth(t);
        setZoom(lerp(zoomInTarget, savedZoom, smooth(t)));

        if (phaseTimer >= RETURN_TICKS) finishSkill();
    }

    private void finishSkill() {
        cleanup();
        cooldown = cooldownMax;
        isActive = false;
        activeTimer = 0;
        Main.Log.d("[SkillV] Selesai.");
    }

    private void abort() {
        sound.stopSkillVSounds();
        cleanup();
        cooldown = cooldownMax;
        isActive = false;
        activeTimer = 0;
        Main.Log.d("[SkillV] Dibatalin (state dunia berubah).");
    }

    private void cleanup() {
        if (!started) return;
        started = false;
        frozen = false;
        atmo = 0f;
        flashTimer = 0;
        shockTimer = 0;
        ghosts.clear();
        particles.clear();
        sparks.clear();
        streaks.clear();
        target = null;
        firstPlacement = null;
        gp.setTargetZoomSilent(savedZoom); // balikin zoom awal, tanpa hint "Zoom: x%"
    }

    private void setZoom(float z) {
        if (Math.abs(z - lastZoomSet) < 0.001f) return;
        lastZoomSet = z;
        gp.setTargetZoomSilent(z);
    }

    // ===================================================================
    //  TARGET & POSISI
    // ===================================================================

    // Musuh dianggap hidup kalau life > 0. Skeleton & SkeletonKing punya field `alive` sendiri (menimpa Entity.alive),
    // Golemite punya isAlive(), dan Golem sengaja gak men-set isDead pas mati.
    private boolean isTargetable(Entity m) {
        if (m == null || m.isDead || m.life <= 0) return false;
        if (m instanceof Golemite)     return ((Golemite) m).isAlive();
        if (m instanceof Entitas.Golem) return ((Entitas.Golem) m).isAlive();
        if (m instanceof Entitas.Wolf) return ((Entitas.Wolf) m).isAlive();
        if (m instanceof Skeleton)     return ((Skeleton) m).alive;
        if (m instanceof SkeletonKing) return ((SkeletonKing) m).alive;
        return true;
    }

    private boolean isInDomainRoom() {
        return gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null && gp.domainManager.isInRoom();
    }

    // Semua musuh yang lagi "ada" di dunia sekarang: gp.monsters di luar, atau isi DomainManager di dalam domain
    private List<Entity> collectEnemies() {
        List<Entity> list = new ArrayList<>();
        if (gp.gameState == GamePanel.GameState.OUTSIDE) {
            if (gp.monsters != null) {
                for (Entity m : gp.monsters) if (m != null) list.add(m);
            }
        } else if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null) {
            list.addAll(gp.domainManager.getSkeletons());      // kosong kalau lagi di dalam room
            list.addAll(gp.domainManager.getGolemites());      // kosong kalau lagi di dalam room
            list.addAll(gp.domainManager.getGolems());         // kosong kalau lagi di dalam room
            list.addAll(gp.domainManager.getRoomSkeletons());  // cuma ada di dalam room
            if (gp.domainManager.getBossSkeleton() != null) list.add(gp.domainManager.getBossSkeleton());
        }
        return list;
    }

    // Pilih musuh terdekat (dalam jangkauan) yang punya setidaknya satu sisi pukul valid
    private boolean pickTarget() {
        target = null;
        firstPlacement = null;

        int px = owner.x + owner.solidArea.x + owner.solidArea.width / 2;
        int py = owner.y + owner.solidArea.y + owner.solidArea.height / 2;
        double range = TARGET_RANGE_TILES * gp.tileSize;

        List<Entity> candidates = new ArrayList<>();
        for (Entity m : collectEnemies()) {
            if (!isTargetable(m)) continue;
            if (centerDist(m, px, py) <= range) candidates.add(m);
        }
        final int fpx = px, fpy = py;
        Collections.sort(candidates, (a, b) -> Double.compare(centerDist(a, fpx, fpy), centerDist(b, fpx, fpy)));

        for (Entity m : candidates) {
            Placement p = pickFirstPlacement(m);
            if (p != null) {
                target = m;
                firstPlacement = p;
                return true;
            }
        }
        return false;
    }

    private double centerDist(Entity m, int px, int py) {
        int mx = m.x + m.solidArea.x + m.solidArea.width / 2;
        int my = m.y + m.solidArea.y + m.solidArea.height / 2;
        return Math.hypot(mx - px, my - py);
    }

    // Sisi pertama = sisi valid yang paling dekat dari posisi player sekarang
    private Placement pickFirstPlacement(Entity m) {
        Placement best = null;
        double bestDist = Double.MAX_VALUE;
        for (Side s : Side.values()) {
            Placement p = placementFor(m, s);
            if (p == null) continue;
            double d = Math.hypot(p.x - owner.x, p.y - owner.y);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    // Sisi lain (bukan sisi sekarang). Kalau forFinisher: pilih sisi dengan ruang dorong paling lega.
    private Placement chooseNextSide(boolean forFinisher) {
        List<Side> order = new ArrayList<>();
        for (Side s : Side.values()) {
            if (forFinisher || s != currentSide) order.add(s);
        }
        Collections.shuffle(order, rng);

        Placement best = null;
        int bestRoom = -1;
        for (Side s : order) {
            Placement p = placementFor(target, s);
            if (p == null) continue;
            if (!forFinisher) return p;

            int room = freeSlideRoom(target, -s.sx, -s.sy);
            if (room > bestRoom) {
                bestRoom = room;
                best = p;
            }
        }
        return best;
    }

    // Posisi player (owner.x, owner.y) di `side` musuh, nempel dengan jarak STAND_GAP. null kalau kena collision dunia.
    private Placement placementFor(Entity m, Side side) {
        java.awt.Rectangle ps = owner.solidArea;
        int mL = m.x + m.solidArea.x;
        int mT = m.y + m.solidArea.y;
        int mR = mL + m.solidArea.width;
        int mB = mT + m.solidArea.height;
        int mcx = (mL + mR) / 2;
        int mcy = (mT + mB) / 2;

        int px, py;
        switch (side) {
            case LEFT:
                px = mL - STAND_GAP - (ps.x + ps.width);
                py = mcy - (ps.y + ps.height / 2);
                break;
            case RIGHT:
                px = mR + STAND_GAP - ps.x;
                py = mcy - (ps.y + ps.height / 2);
                break;
            case UP:
                px = mcx - (ps.x + ps.width / 2);
                py = mT - STAND_GAP - (ps.y + ps.height);
                break;
            default: // DOWN
                px = mcx - (ps.x + ps.width / 2);
                py = mB + STAND_GAP - ps.y;
                break;
        }

        // Kalau pas kena collision, coba geser menyamping sedikit di sisi yang sama
        for (int shift : SIDE_SHIFT_PX) {
            int tx = px + (side.sx == 0 ? shift : 0);
            int ty = py + (side.sy == 0 ? shift : 0);
            if (!gp.colision.isBlockedByWorld(owner, tx, ty)) return new Placement(tx, ty, side);
        }
        return null;
    }

    // Seberapa jauh (px) musuh bisa didorong ke arah (dx, dy) sebelum nabrak collision dunia
    private int freeSlideRoom(Entity m, int dx, int dy) {
        int d = 0;
        while (d < KNOCKBACK_ROOM_CHECK && !gp.colision.isBlockedByWorld(m, m.x + dx * (d + 4), m.y + dy * (d + 4))) {
            d += 4;
        }
        return d;
    }

    // Posisi awal kalau masih aman; kalau nggak, titik aman terdekat; kalau gak ada, tetap di tempat
    private int[] findReturnPosition() {
        if (!gp.colision.isBlockedByWorld(owner, originX, originY)) return new int[] { originX, originY };

        for (int r = RETURN_SEARCH_STEP; r <= RETURN_SEARCH_RADIUS; r += RETURN_SEARCH_STEP) {
            for (int dx = -r; dx <= r; dx += RETURN_SEARCH_STEP) {
                for (int dy = -r; dy <= r; dy += RETURN_SEARCH_STEP) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // cuma tepi kotak
                    if (!gp.colision.isBlockedByWorld(owner, originX + dx, originY + dy)) {
                        return new int[] { originX + dx, originY + dy };
                    }
                }
            }
        }
        return new int[] { owner.x, owner.y };
    }

    // Teleport: bayangan di posisi lama + jalur kilat, lalu pindah & menghadap musuh
    private void teleportTo(Placement p) {
        spawnTeleportTrail(owner.x, owner.y, p.x, p.y, owner.direction, punchFrame);
        owner.x = p.x;
        owner.y = p.y;
        owner.direction = p.side.facing;
        currentSide = p.side;
        sound.playWoosh();
    }

    // ===================================================================
    //  EFEK: spawn & update
    // ===================================================================

    private void spawnPunchGhost(float alpha) {
        if (ghosts.size() >= GHOST_MAX) return;
        int jx = rng.nextInt(GHOST_JITTER_PX * 2 + 1) - GHOST_JITTER_PX;
        int jy = rng.nextInt(GHOST_JITTER_PX * 2 + 1) - GHOST_JITTER_PX;
        ghosts.add(new Ghost(owner.x + jx, owner.y + jy, owner.direction, punchFrame, alpha, GHOST_LIFE));
    }

    private void spawnTeleportTrail(int fromX, int fromY, int toX, int toY, String dir, int frame) {
        if (dir == null) dir = "down";

        // Bayangan di posisi lama
        addGhost(new Ghost(fromX, fromY, dir, frame, TELEPORT_GHOST_ALPHA, TELEPORT_GHOST_LIFE));

        // Bayangan di sepanjang jalur
        for (int i = 1; i <= TELEPORT_TRAIL_GHOSTS; i++) {
            float t = i / (float) (TELEPORT_TRAIL_GHOSTS + 1);
            int gx = Math.round(lerp(fromX, toX, t));
            int gy = Math.round(lerp(fromY, toY, t));
            float a = TELEPORT_GHOST_ALPHA * (1f - t) * 0.7f;
            addGhost(new Ghost(gx, gy, dir, frame, a, TELEPORT_GHOST_LIFE - 2));
        }

        // Garis kilat dari posisi lama ke posisi baru
        Streak s = new Streak();
        s.x0 = fromX;
        s.y0 = fromY;
        s.x1 = toX;
        s.y1 = toY;
        s.life = STREAK_LIFE;
        s.maxLife = STREAK_LIFE;
        streaks.add(s);
    }

    private void addGhost(Ghost g) {
        if (ghosts.size() < GHOST_MAX) ghosts.add(g);
    }

    private void spawnSpark(boolean big) {
        if (target == null || currentSide == null || sparks.size() >= SPARK_MAX) return;

        // Titik kena = tengah sisi hitbox musuh yang menghadap player
        int mcx = target.x + target.solidArea.x + target.solidArea.width / 2;
        int mcy = target.y + target.solidArea.y + target.solidArea.height / 2;
        int hx = mcx + currentSide.sx * (target.solidArea.width / 2);
        int hy = mcy + currentSide.sy * (target.solidArea.height / 2);

        Spark s = new Spark();
        s.wx = hx + rng.nextInt(13) - 6;
        s.wy = hy + rng.nextInt(13) - 6;
        s.life = big ? SPARK_LIFE * 3 : SPARK_LIFE;
        s.maxLife = s.life;
        s.size = big ? SPARK_SIZE_BIG : SPARK_SIZE * (0.7f + rng.nextFloat() * 0.6f);
        s.rot = rng.nextFloat() * (float) Math.PI;
        s.big = big;
        sparks.add(s);
    }

    private void triggerFlash(int ticks, float strength) {
        if (strength <= 0f || ticks <= 0) return;
        flashTotal = ticks;
        flashTimer = ticks;
        flashStrength = strength;
    }

    private void updateEffects() {
        fxTick++;

        for (int i = ghosts.size() - 1; i >= 0; i--) {
            if (--ghosts.get(i).life <= 0) ghosts.remove(i);
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            if (--sparks.get(i).life <= 0) sparks.remove(i);
        }
        for (int i = streaks.size() - 1; i >= 0; i--) {
            if (--streaks.get(i).life <= 0) streaks.remove(i);
        }

        // Partikel (koordinat layar): melayang ke atas
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.vx += (rng.nextFloat() - 0.5f) * 0.25f;
            if (--p.life <= 0 || p.y < -10) particles.remove(i);
        }
        if (atmo > 0.05f) {
            for (int k = 0; k < PARTICLE_SPAWN_PER_TICK; k++) {
                if (particles.size() < PARTICLE_MAX && rng.nextFloat() < atmo) particles.add(newParticle());
            }
            if (fxTick % SPEED_LINE_REFRESH_TICKS == 0) reseedSpeedLines();
        }

        if (flashTimer > 0) flashTimer--;
        if (shockTimer > 0) shockTimer--;
    }

    private Particle newParticle() {
        Particle p = new Particle();
        p.x = rng.nextFloat() * gp.screenWidth;
        p.y = gp.screenHeight * (0.35f + rng.nextFloat() * 0.75f);
        p.vx = (rng.nextFloat() - 0.5f) * 1.2f;
        p.vy = -(0.6f + rng.nextFloat() * 2.0f);
        p.size = 2f + rng.nextFloat() * 3f;
        p.maxLife = 24 + rng.nextInt(24);
        p.life = p.maxLife;
        p.color = EMBER_COLORS[rng.nextInt(EMBER_COLORS.length)];
        return p;
    }

    private void reseedSpeedLines() {
        for (int i = 0; i < SPEED_LINE_COUNT; i++) {
            slAngle[i] = rng.nextFloat() * (float) (Math.PI * 2);
            slInner[i] = SPEED_LINE_INNER_MIN + rng.nextFloat() * (SPEED_LINE_INNER_MAX - SPEED_LINE_INNER_MIN);
            slLen[i]   = SPEED_LINE_LEN_MIN + rng.nextFloat() * (SPEED_LINE_LEN_MAX - SPEED_LINE_LEN_MIN);
            slHalf[i]  = SPEED_LINE_HALF_ANGLE_MIN + rng.nextFloat() * (SPEED_LINE_HALF_ANGLE_MAX - SPEED_LINE_HALF_ANGLE_MIN);
        }
    }

    // ===================================================================
    //  DRAW
    // ===================================================================

    // Cooldown bar tetap muncul selama cooldown (kayak SkillX), efek skill cuma pas aktif
    @Override
    public void draw(Graphics2D g2) {
        if (cooldown > 0) drawCooldownBar(g2);
        if (!isActive) return;
        onDraw(g2);
    }

    // Dunia (dipanggil dari player.draw lewat skillManager.draw): afterimage di belakang player
    @Override
    protected void onDraw(Graphics2D g2) {
        for (Ghost g : ghosts) {
            float a = g.alpha * (g.life / (float) g.maxLife);
            int sx = g.x - owner.x + owner.screenX;
            int sy = g.y - owner.y + owner.screenY;
            owner.drawPunchFrame(g2, g.frame, g.dir, sx, sy, a, GHOST_TINT);
        }
    }

    /**
     * Overlay layar penuh (gelap, tint, vignette, speed lines, partikel, kilatan).
     * Dipanggil dari GamePanel.paintComponent, di bawah UI. Koordinat layar biasa (di luar transform zoom).
     */
    public void drawOverlay(Graphics2D g2) {
        if (!isActive) return;

        int w = gp.screenWidth;
        int h = gp.screenHeight;
        buildPaints(w, h);

        Composite oldComp = g2.getComposite();
        Paint oldPaint = g2.getPaint();
        Stroke oldStroke = g2.getStroke();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (atmo > 0f) {
            // 1. Layar gelap
            setAlpha(g2, DARK_ALPHA * atmo);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, w, h);

            // 2. Tint oranye kemerahan
            setAlpha(g2, TINT_ALPHA * atmo);
            g2.setPaint(tintPaint);
            g2.fillRect(0, 0, w, h);

            // 3. Vignette
            setAlpha(g2, atmo);
            g2.setPaint(vignettePaint);
            g2.fillRect(0, 0, w, h);

            // 4. Speed lines + partikel
            drawSpeedLines(g2, w, h);
            drawParticles(g2);
        }

        drawStreaks(g2);
        drawChargeRing(g2, w, h);
        drawShockRing(g2);
        drawSparks(g2);

        // 5. Kilatan putih (impact frame)
        if (flashTimer > 0) {
            setAlpha(g2, flashStrength * (flashTimer / (float) flashTotal));
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
        }

        g2.setComposite(oldComp);
        g2.setPaint(oldPaint);
        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void buildPaints(int w, int h) {
        if (tintPaint == null) {
            tintPaint = new GradientPaint(0, 0, TINT_TOP, 0, h, TINT_BOTTOM);
        }
        if (vignettePaint == null) {
            float radius = (float) (Math.hypot(w, h) / 2.0);
            vignettePaint = new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f), radius,
                new float[] { 0f, 0.45f, 1f },
                new Color[] { new Color(0, 0, 0, 0), VIGNETTE_MID, VIGNETTE_EDGE });
        }
    }

    // Titik fokus speed lines: musuh yang lagi dipukul, atau tengah layar
    private float[] focusPoint() {
        if (target != null && (phase == Phase.BARRAGE || phase == Phase.FINISH_WINDUP || phase == Phase.FINISH_IMPACT)) {
            int mcx = target.x + target.solidArea.x + target.solidArea.width / 2;
            int mcy = target.y + target.solidArea.y + target.solidArea.height / 2;
            return new float[] { toScreenX(mcx), toScreenY(mcy) };
        }
        return new float[] { gp.screenWidth / 2f, gp.screenHeight / 2f };
    }

    private void drawSpeedLines(Graphics2D g2, int w, int h) {
        float[] f = focusPoint();
        double maxR = Math.hypot(w, h) / 2.0;

        setAlpha(g2, SPEED_LINE_ALPHA * atmo);
        int[] xs = new int[3];
        int[] ys = new int[3];
        for (int i = 0; i < SPEED_LINE_COUNT; i++) {
            double a = slAngle[i];
            double r0 = slInner[i] * maxR;
            double r1 = r0 + slLen[i] * maxR;
            double half = slHalf[i];

            // Segitiga tipis: ujung runcing di dalam, melebar ke luar (gaya manga)
            xs[0] = (int) (f[0] + Math.cos(a) * r0);
            ys[0] = (int) (f[1] + Math.sin(a) * r0);
            xs[1] = (int) (f[0] + Math.cos(a - half) * r1);
            ys[1] = (int) (f[1] + Math.sin(a - half) * r1);
            xs[2] = (int) (f[0] + Math.cos(a + half) * r1);
            ys[2] = (int) (f[1] + Math.sin(a + half) * r1);

            g2.setColor(i % 3 == 0 ? SPEED_LINE_HOT : SPEED_LINE_COLOR);
            g2.fillPolygon(xs, ys, 3);
        }
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            float life = p.life / (float) p.maxLife;
            setAlpha(g2, Math.min(1f, life * 1.6f) * atmo);
            g2.setColor(p.color);
            int s = Math.max(1, Math.round(p.size));
            g2.fillOval(Math.round(p.x - s / 2f), Math.round(p.y - s / 2f), s, s);
        }
    }

    private void drawStreaks(Graphics2D g2) {
        for (Streak s : streaks) {
            float life = s.life / (float) s.maxLife;
            int x0 = Math.round(toScreenX(s.x0 + owner.solidArea.x + owner.solidArea.width / 2));
            int y0 = Math.round(toScreenY(s.y0 + owner.solidArea.y + owner.solidArea.height / 2));
            int x1 = Math.round(toScreenX(s.x1 + owner.solidArea.x + owner.solidArea.width / 2));
            int y1 = Math.round(toScreenY(s.y1 + owner.solidArea.y + owner.solidArea.height / 2));

            setAlpha(g2, life * 0.5f);
            g2.setColor(SPARK_COLOR_HOT);
            g2.setStroke(new BasicStroke(10f * life + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x0, y0, x1, y1);

            setAlpha(g2, life);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f * life + 1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x0, y0, x1, y1);
        }
    }

    // Cincin ancang-ancang yang menyusut ke arah player sebelum pukulan terakhir
    private void drawChargeRing(Graphics2D g2, int w, int h) {
        if (phase != Phase.FINISH_WINDUP) return;
        float t = Math.min(1f, phaseTimer / (float) FINISHER_WINDUP_TICKS);
        float r = lerp(CHARGE_RING_START, CHARGE_RING_END, t) * gp.renderZoom();
        float cx = w / 2f;
        float cy = h / 2f;

        setAlpha(g2, 0.35f + 0.65f * t);
        g2.setColor(SPARK_COLOR_HOT);
        g2.setStroke(new BasicStroke(3f));
        g2.drawOval(Math.round(cx - r), Math.round(cy - r), Math.round(r * 2), Math.round(r * 2));
    }

    private void drawShockRing(Graphics2D g2) {
        if (shockTimer <= 0) return;
        float t = 1f - shockTimer / (float) SHOCK_RING_TICKS; // 0 -> 1
        float r = lerp(10f, SHOCK_RING_MAX_RADIUS, easeOut(t)) * gp.renderZoom();
        float sx = toScreenX(shockX);
        float sy = toScreenY(shockY);

        setAlpha(g2, 1f - t);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5f * (1f - t) + 1f));
        g2.drawOval(Math.round(sx - r), Math.round(sy - r), Math.round(r * 2), Math.round(r * 2));
    }

    private void drawSparks(Graphics2D g2) {
        float z = gp.renderZoom();
        for (Spark s : sparks) {
            float life = s.life / (float) s.maxLife;          // 1 -> 0
            float r = s.size * z * (0.45f + (1f - life));      // membesar sambil memudar
            float sx = toScreenX(s.wx);
            float sy = toScreenY(s.wy);
            int rays = s.big ? 12 : 8;

            setAlpha(g2, life);
            g2.setStroke(new BasicStroke(s.big ? 3.5f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < rays; i++) {
                double a = s.rot + i * (Math.PI * 2 / rays);
                float inner = r * 0.25f;
                float outer = r * ((i % 2 == 0) ? 1f : 0.6f);
                g2.setColor(i % 2 == 0 ? SPARK_COLOR : SPARK_COLOR_HOT);
                g2.drawLine(Math.round(sx + (float) Math.cos(a) * inner), Math.round(sy + (float) Math.sin(a) * inner),
                            Math.round(sx + (float) Math.cos(a) * outer), Math.round(sy + (float) Math.sin(a) * outer));
            }

            g2.setColor(SPARK_COLOR);
            int core = Math.max(2, Math.round(r * 0.35f));
            g2.fillOval(Math.round(sx - core), Math.round(sy - core), core * 2, core * 2);
        }
    }

    // Bar kecil di bawah player menunjukkan sisa cooldown
    private void drawCooldownBar(Graphics2D g2) {
        int barW = gp.tileSize;
        int barH = 3;
        int bx = owner.screenX;
        int by = owner.screenY + gp.tileSize + 20;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(bx, by, barW, barH);

        float pct = getCooldownPercent();
        g2.setColor(new Color(255, 110, 40));
        g2.fillRect(bx, by, (int) (barW * pct), barH);
    }

    // ===================================================================
    //  DIPAKAI PLAYER / GAMEPANEL
    // ===================================================================

    /** True selama player lagi nonjok (player.draw() menggambar frame dari getPunchFrame()). */
    public boolean isPunching() {
        return isActive && (phase == Phase.BARRAGE || phase == Phase.FINISH_WINDUP || phase == Phase.FINISH_IMPACT);
    }

    /** Index frame attack (0-3) yang lagi ditampilkan. */
    public int getPunchFrame() {
        return punchFrame;
    }

    /** True selama dunia di-freeze (monster, NPC, minion berhenti). */
    public boolean isWorldFrozen() {
        return isActive && frozen;
    }

    // ===================================================================
    //  UTIL
    // ===================================================================

    // Koordinat dunia -> koordinat layar (sudah termasuk zoom, pivot di tengah layar kayak GamePanel.applyWorldZoom)
    private float toScreenX(float worldX) {
        float sx = worldX - owner.x + owner.screenX;
        float cx = gp.screenWidth / 2f;
        return cx + (sx - cx) * gp.renderZoom();
    }

    private float toScreenY(float worldY) {
        float sy = worldY - owner.y + owner.screenY;
        float cy = gp.screenHeight / 2f;
        return cy + (sy - cy) * gp.renderZoom();
    }

    private static void setAlpha(Graphics2D g2, float a) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
    }

    private int randInt(int min, int max) {
        return min + rng.nextInt(max - min + 1);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }
}