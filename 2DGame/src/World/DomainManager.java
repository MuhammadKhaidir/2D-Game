package World;

import Main.GamePanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

import Entitas.DarkMerchant;
import Entitas.Skeleton;
import Entitas.Golemite;
import Entitas.SkeletonKing;
import Item.ItemManager;
import Item.ItemStack;

public class DomainManager {

    public boolean insideDomain = false;

    private final List<DomainData> domains = new ArrayList<>();
    private DomainData activeDomain = null;
    private ImageIcon bgImage = null;
    private final List<Rectangle> wallCollisions = new ArrayList<>();

    // ── Nested Room (misal: Boss Room di dalam domain) ──
    private RoomData activeRoom = null;
    private ImageIcon roomBgImage = null;
    private RoomData nearDoorRoom = null; // buat nampilin hint "[E] Masuk ..." pas deket pintu
    private boolean nearExitPoint = false; // true kalau player lagi di zona keluar domain (hint "[E] Keluar dungeon")
    private int returnWorldX = -1; // posisi player di world pas masuk domain (buat keluar di titik yang sama)
    private int returnWorldY = -1;

    // ── Debug: gambar kotak collision biar gampang nge-tune posisinya ──
    public boolean showCollisionDebug = false; // toggle: F3

    // ── Spawn Boss via Skeleton Key ──
    private final Rectangle bossSpawnAltar = new Rectangle(580 - 40, 190 - 40, 80, 80);
    private boolean bossSpawnCountdownActive = false;
    private int bossSpawnCountdownTimer = 0;
    private static final int GAME_FPS = 60; // sesuaikan kalau FPS game kamu beda
    private static final int BOSS_SPAWN_COUNTDOWN_FRAMES = 10 * GAME_FPS; // countdown dari 10 detik

    // ── Confirmation dialog sebelum spawn boss ──
    private boolean bossSpawnConfirmOpen = false;
    private int bossSpawnConfirmSelection = 0; // 0 = YES, 1 = NO
    private int confirmNavCooldown = 0;

    // ── Posisi UI countdown (digeser dikit ke kanan dari titik altar) ──
    private static final int COUNTDOWN_WORLD_X = 580 + 40;
    private static final int COUNTDOWN_WORLD_Y = 150;

    // ── Boss di dalam room ──
    private SkeletonKing bossSkeleton = null;
    private boolean bossDefeated = false; // sekali kalah, gak spawn lagi

    private int fadeAlpha = 0;
    private boolean fading = false;
    private boolean fadeIn = false;
    private Runnable onFadeComplete;

    public String getActiveDomainId() {
        return activeDomain != null ? activeDomain.id : null;
    }
    
    public DarkMerchant darkMerchant = null;
    
    // Pesan feedback pembelian (tampil sebentar di layar)
    private String shopFeedback = "";
    private int shopFeedbackTimer = 0;

    private final List<Skeleton> skeletons = new ArrayList<>();
    public List<Skeleton> getSkeletons() {
        // Skeleton Tomb gak ditampilin pas lagi di dalam room (misal boss room)
        return activeRoom != null ? java.util.Collections.emptyList() : skeletons;
    }
    
    private final List<Golemite> golemites = new ArrayList<>();
    public List<Golemite> getGolemites() {
        // sama kayak skeleton: gak ditampilin pas di dalam room
        return activeRoom != null ? java.util.Collections.emptyList() : golemites;
    }

    // skeleton summon an si king skeleton / pion bisa dibilang
    private final List<Skeleton> roomSkeletons = new ArrayList<>();

    /** Skeleton hasil summon boss, cuma keliatan pas lagi di dalam room */
    public List<Skeleton> getRoomSkeletons() {
        return activeRoom != null ? roomSkeletons : java.util.Collections.emptyList();
    }

    private static final int MAX_ROOM_SKELETONS = 5; // batas skeleton summonan yang boleh HIDUP bersamaan

    /** Jumlah skeleton summonan boss yang MASIH HIDUP sekarang (dipakai buat cek limit summon). */
    public int getAliveRoomSkeletonCount() {
        int count = 0;
        for (Skeleton sk : roomSkeletons) {
            if (sk != null && sk.alive) count++;
        }
        return count;
    }

    /** Dipanggil SkeletonKing buat nyummon Skeleton biasa di dalam boss room. */
    public void spawnSkeletonInRoom(int wx, int wy) {
        if (activeRoom == null) return; // safety: cuma valid kalau lagi di dalam room
        if (getAliveRoomSkeletonCount() >= MAX_ROOM_SKELETONS) return; // cegah numpuk/lag
        Skeleton sk = new Skeleton(gp);
        sk.x = wx; sk.y = wy;
        sk.spawnX = wx; sk.spawnY = wy;
        sk.noRespawn = true; // pion boss, sekali mati ya mati
        sk.noDrop = true;
        roomSkeletons.add(sk);
    }
    
    public void spawnGolemiteInRoom(int wx, int wy) {
        if (activeRoom == null) return; // safety: cuma valid kalau lagi di dalam room
        if (getAliveRoomSkeletonCount() >= MAX_ROOM_SKELETONS) return; // cegah numpuk/lag
        Skeleton sk = new Skeleton(gp);
        sk.x = wx; sk.y = wy;
        sk.spawnX = wx; sk.spawnY = wy;
        sk.noRespawn = true; // pion boss, sekali mati ya mati
        sk.noDrop = true;
        roomSkeletons.add(sk);
    }

    /** Boss yang lagi aktif di dalam room (null kalau gak lagi di boss room / boss udah kalah) */
    public SkeletonKing getBossSkeleton() {
        return bossSkeleton;
    }

    private final GamePanel gp;

    public DomainManager(GamePanel gp) {
        this.gp = gp;
        registerAllDomains();
        new Thread(() -> Skeleton.preloadImages()).start();
        new Thread(() -> SkeletonKing.preloadImages()).start();
       // new Thread(() -> Golemite.preloadImages()).start();
    }

    private void registerAllDomains() {
        DomainData tomb = new DomainData(
            "Tomb",
            new Rectangle(1300, 2100, 200, 200),
            900, 1090,
            800, 600,
            "/Latar/TombDomainPixel.png")
        .setExit(900, 1090)
        // Titik keluar di dalam Tomb: deketin -> muncul hint, tekan E buat keluar dungeon
        .setExitPoint(990, 1090, 60)
        .addCollision(250,  650,  400, 190)
        .addCollision(400,  725,  360, 145)
        .addCollision(380,  650,  110, 320)
        .addCollision(320,  155,  110, 380)
        .addCollision(245,  355,   70, 165)
        .addCollision(550,  855,  200, 100)
        .addCollision(255,  620,  140,  30)
        .addCollision(435,  335,  220, 185)
        .addCollision(400,  335,  165, 220)
        .addCollision(600,  615,   50,  40)
        .addCollision(1070, 900,   60,  50)
        .addCollision(325, 1040,  160, 100)
        .addCollision(135, 1095,  200,  50)
        .addCollision(55,  355,  100, 900)
        .addCollision(1215, 190,   90, 250)
        .addCollision(1215, 365,  150, 160)
        .addCollision(1280, 510,  200, 100)
        .addCollision(280,  825,  100,  50)
        .addCollision(330,  860,   50, 100)
        .addCollision(65,   185,  300, 100)
        .addCollision(485,  200,  180,  30)
        .addCollision(0,      0, 1800, 200)
        .addCollision(700, 1100,  150,  40)
        .addCollision(700, 1050,   50,  40)
        .addCollision(990, 1100,  125,  40)
        .addCollision(1080,1050,  125,  40)
        .addCollision(125, 1140, 2000,  40)
        .addCollision(1165,1010,   95,  40)
        .addCollision(1270,1070,   40,  45)
        .addCollision(1500,1065,  125,  45)
        .addCollision(1580,1080,  125,  40)
        .addCollision(0,     0,    60, 600)
        .addCollision(1485, 365,   60, 410)
        .addCollision(1210, 620,  300,  40)
        .addCollision(1445, 675,   30,  30)
        .addCollision(1210, 620,   40, 100)
        .addCollision(1070, 725,  210, 190)
        .addCollision(1260, 665,  120, 120)
        .addCollision(1550, 650,   40, 600);
        domains.add(tomb);

        // ── Pintu ke Boss Room (di dalam Tomb) ──
        // TODO: cek posisi 895,195 gak nabrak collision Tomb di atas, geser kalau perlu
        // TODO: sesuaikan spawnX/spawnY (posisi player muncul di dalam BosSkeletonRoom.png)
        // TODO: sesuaikan width/height interior room sesuai ukuran gambar
        tomb.addRoom(new RoomData(
                "BossSkeletonRoom",
                new Rectangle(895, 195, 60, 60),
                590, 705,
                600, 400,
                "/Aset/BosSkeletonRoom.png")
            .setReturnPoint(895, 260)
            // TODO: koordinat di bawah cuma CONTOH — sesuaikan sama gambar BosSkeletonRoom.png,
            // pas gambar debug muncul, geser angkanya sampai pas nutup obstacle di background.
            .addCollision(0,   0,   1250, 250)   // dinding atas
            .addCollision(0,   0,   80,  850)    // dinding kiri
            .addCollision(1130, 0,   80,  850)   // dinding kanan
            .addCollision(0,   750, 1250, 40)    // dinding bawah
            .addCollision(350, 250, 45, 65)
            .addCollision(820, 250, 45, 65)
        );

        // ── Golem Hide ──
        final int GH_W = 800, GH_H = 600;              // ukuran golemHide.png (digambar 2x, sama kayak Tomb)
        final int GH_TRIG_X = 4420, GH_TRIG_Y = 4830;  // posisi bangunan golemMine di world

        DomainData golemHide = new DomainData(
            "GolemHide",
            // FIX: sebelumnya hardcode (3000, 200, ...) jadi trigger-nya gak nempel di bangunan golemMine
            new Rectangle(GH_TRIG_X, GH_TRIG_Y, 200, 200),  // area tekan [E] buat masuk
            730, 330,                                     // spawn player: tengah map
            GH_W, GH_H,
            // FIX: huruf besar "Latar" biar sama kayak Tomb (getResource itu case-sensitive di jar).
            // Ganti ke "/Aset/golemHide.png" kalau filenya ternyata di folder Aset.
            "/Latar/golemHide.png")
        // FIX: exit ikut posisi bangunan (pola sama kayak Tomb: trigger 1300,2100 -> exit 1375,2265)
        .setExit(GH_TRIG_X + 75, GH_TRIG_Y + 165)
        // Titik keluar DI DALAM domain: deketin koordinat 723,170 -> otomatis keluar (radius 60px)
        .setExitPoint(723, 170, 60)
        .addCollision(0, 0, GH_W * 2, 300)                   // dinding atas
        .addCollision(0, GH_H * 2 - 60, GH_W * 2, 400)       // dinding bawah
        .addCollision(0, 0, 60, GH_H * 2)                   // dinding kiri
        .addCollision(GH_W * 2 - 60, 0, 60, GH_H * 2)       // dinding kanan
        .addCollision(535,155,50*2,250);
        domains.add(golemHide);
    }

    public void forceEnterDomain(String domainId) {
        for (DomainData d : domains) {
            if (d.id.equals(domainId)) {
                buildInteriorFor(d);
                activeDomain = d;
                insideDomain = true;
                gp.gameState = GamePanel.GameState.DOMAIN;
                gp.player.x = d.spawnX;
                gp.player.y = d.spawnY;
                playDomainBGM(d.id);
                return;
            }
        }
        System.out.println("[DomainManager] Domain tidak ketemu: " + domainId);
    }

    private void drawCollisionDebug(Graphics2D g2) {
        if (!showCollisionDebug) return;
        int camX = gp.player.x, camY = gp.player.y;
        int psx = gp.player.screenX, psy = gp.player.screenY;

        for (Rectangle r : wallCollisions) {
            int sx = r.x - camX + psx;
            int sy = r.y - camY + psy;
            if (sx + r.width < gp.viewMinX() || sx > gp.viewMaxX() || sy + r.height < gp.viewMinY() || sy > gp.viewMaxY()) continue;

            g2.setColor(new Color(255, 0, 0, 90));
            g2.fillRect(sx, sy, r.width, r.height);
            g2.setColor(Color.RED);
            g2.drawRect(sx, sy, r.width, r.height);
        }

        // Debug titik keluar domain (lingkaran hijau), cuma di interior domain utama
        if (activeRoom == null && activeDomain != null && activeDomain.hasExitPoint) {
            int rad = activeDomain.exitPointRadius;
            int ex = activeDomain.exitPointX - camX + psx - rad;
            int ey = activeDomain.exitPointY - camY + psy - rad;
            g2.setColor(new Color(0, 255, 0, 70));
            g2.fillOval(ex, ey, rad * 2, rad * 2);
            g2.setColor(Color.GREEN);
            g2.drawOval(ex, ey, rad * 2, rad * 2);
        }
    }

    public void update() {
        handleFade();

        if (!insideDomain) {
            checkEnterDomain();
            return;
        }

        if (activeRoom == null) {
            // Lagi di interior domain utama (Tomb) — bisa masuk room lewat pintu
            checkEnterRoom();
            checkExitPoint();
            for (Skeleton sk : skeletons) sk.update();
            for (Golemite g : golemites) g.update();
            if (darkMerchant != null) {
                darkMerchant.update();
                checkMerchantInteract();
            }
            if (shopFeedbackTimer > 0) shopFeedbackTimer--;
        } else {
            checkBossSpawnAltar();
            checkBossSpawnConfirm();

            if (bossSpawnCountdownActive) {
                bossSpawnCountdownTimer--;
                if (bossSpawnCountdownTimer <= 0) {
                    bossSpawnCountdownActive = false;
                    bossSkeleton = new SkeletonKing(gp);
                    bossSkeleton.x = 580;
                    bossSkeleton.y = 250;
                    bossSkeleton.beginSpawnAnimation();
                }
            }

            if (bossSkeleton != null) {
                bossSkeleton.update();
                if (!bossSkeleton.alive && bossSkeleton.isDeathAnimDone()) {
                    bossSkeleton = null; // altar otomatis kepake lagi setelah ini
                }
            }
            for (Skeleton sk : roomSkeletons) sk.update();
            
        }
    }

    /** Kalau player deket titik keluar domain: tampil hint, dan keluar kalau tekan E. */
    private void checkExitPoint() {
        nearExitPoint = false;
        if (fading) return; // lagi transisi fade, jangan trigger dobel
        if (activeDomain == null || !activeDomain.hasExitPoint) return;

        double dist = Math.hypot(
            gp.player.x - activeDomain.exitPointX,
            gp.player.y - activeDomain.exitPointY
        );
        if (dist > activeDomain.exitPointRadius) return;

        nearExitPoint = true; // buat hint "[E] Keluar dungeon"
        if (gp.KeyH.interactPressed) {
            gp.KeyH.interactPressed = false;
            exitDomain();
        }
    }

    private void checkEnterDomain() {
        if (insideDomain) return;
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return;
        if (gp.houseManager.isInsideAnyHouse()) return;
        if (!gp.KeyH.interactPressed) return;
        Rectangle playerArea = gp.player.getInteractArea();
        for (DomainData d : domains) {
            if (playerArea.intersects(d.triggerArea)) {
                gp.KeyH.interactPressed = false;
                Main.Log.d("[DomainManager] Masuk domain: " + d.id + " bg=" + d.backgroundPath); // debug
                startEnter(d);
                return;
            }
        }
    }

    private void startEnter(DomainData d) {
        // simpan posisi player pas mau masuk, biar keluarnya di titik yang sama
        returnWorldX = gp.player.x;
        returnWorldY = gp.player.y;
        startFade(false, () -> {
            buildInteriorFor(d);
            activeDomain = d;
            insideDomain = true;
            gp.gameState = GamePanel.GameState.DOMAIN;
            gp.player.x = d.spawnX;
            gp.player.y = d.spawnY;
            playDomainBGM(d.id);
            startFade(true, null);
        });
    }

    /** Cek apakah player deket pintu room & tekan E buat masuk */
    private void checkEnterRoom() {
        nearDoorRoom = null;
        if (activeDomain == null) return;
        Rectangle playerArea = gp.player.getInteractArea();
        for (RoomData r : activeDomain.rooms) {
            if (playerArea.intersects(r.doorTriggerArea)) {
                nearDoorRoom = r; // buat kasih hint visual "[E] Masuk ..."
                if (gp.KeyH.interactPressed) {
                    gp.KeyH.interactPressed = false;
                    startEnterRoom(r);
                }
                return;
            }
        }
    }

    private void startEnterRoom(RoomData r) {
        startFade(false, () -> {
            buildRoomInterior(r);
            activeRoom = r;
            gp.player.x = r.spawnX;
            gp.player.y = r.spawnY;
            startFade(true, null);
        });
    }

    private void buildRoomInterior(RoomData r) {
        wallCollisions.clear();
        roomSkeletons.clear();
        try {
            java.net.URL url = getClass().getResource(r.backgroundPath);
            roomBgImage = url != null ? new ImageIcon(url) : null;
            if (url == null) System.out.println("[DomainManager] Room background tidak ketemu: " + r.backgroundPath);
        } catch (Exception e) { roomBgImage = null; e.printStackTrace(); }
        for (Rectangle rect : r.collisions) wallCollisions.add(rect);

        // Boss TIDAK auto-spawn lagi — harus dibangkitkan manual pakai Skeleton Key di altar
        bossSkeleton = null;
        bossSpawnCountdownActive = false;
        bossSpawnCountdownTimer = 0;
    }

    /** Keluar dari room, balik ke interior domain induk (dipanggil dari exitDomain()) */
    private void exitRoom() {
        if (activeRoom == null) return;
        final int returnX = activeRoom.returnX;
        final int returnY = activeRoom.returnY;
        startFade(false, () -> {
            activeRoom = null;
            roomBgImage = null;
            bossSkeleton = null;
            bossSpawnCountdownActive = false;
            bossSpawnCountdownTimer = 0;
            wallCollisions.clear();
            roomSkeletons.clear();
            if (activeDomain != null) {
                for (Rectangle rect : activeDomain.collisions) wallCollisions.add(rect);
            }
            gp.player.x = returnX;
            gp.player.y = returnY;
            startFade(true, null);
        });
    }

    /** Render sprite skeleton hasil summon boss (dipanggil GamePanel saat Y-sort) */
    public void drawRoomSkeletons(Graphics2D g2) {
        if (activeRoom == null) return;
        for (Skeleton sk : roomSkeletons) sk.draw(g2);
    }

    public boolean isInRoom() { return activeRoom != null; }

    /** True kalau dialog konfirmasi "bangunin Skeleton King" lagi kebuka (zoom dikunci). */
    public boolean isBossConfirmOpen() { return bossSpawnConfirmOpen; }

    /**
     * Keluar dari konteks sekarang:
     * - Kalau lagi di dalam room (boss room) → balik ke interior domain induk.
     * - Kalau lagi di interior domain → balik ke OUTSIDE (world biasa).
     * Dipanggil dari GamePanel saat ESC, gak perlu ubah GamePanel sama sekali.
     */
    public void exitDomain() {
        if (activeRoom != null) {
            exitRoom();
            return;
        }
        if (activeDomain == null) return;
        // balik ke posisi pas masuk; kalau gak ada (belum pernah masuk lewat startEnter) pakai titik exit bawaan domain
        final int exitX = returnWorldX >= 0 ? returnWorldX : activeDomain.exitWorldX;
        final int exitY = returnWorldY >= 0 ? returnWorldY : activeDomain.exitWorldY;
        startFade(false, () -> {
            gp.music.stop();
            insideDomain = false;
            activeDomain = null;
            gp.gameState = GamePanel.GameState.OUTSIDE;
            // bersihin sisa isi domain biar gak "bocor" ke dunia luar
            wallCollisions.clear();
            skeletons.clear();
            golemites.clear();
            roomSkeletons.clear();
            bossSkeleton = null;
            gp.player.x = exitX;
            gp.player.y = exitY;
            darkMerchant = null;
            startFade(true, null);
        });
    }

    private void playDomainBGM(String domainId) {
        switch (domainId) {
            case "Tomb":      gp.music.play("Umbrae Surgunt.wav"); break;
            case "GolemHide": gp.music.play("golemHideSound.wav"); break;
            default:          gp.music.stop();
        }
    }
    


    private void spawnSkeleton(int wx, int wy) {
        Skeleton sk = new Skeleton(gp);
        sk.x = wx; sk.y = wy;
        sk.spawnX = wx; sk.spawnY = wy;
        skeletons.add(sk);
    }
    
    private void spawnGolemite(int wx, int wy) {
        Golemite g = new Golemite(gp);
        g.x = wx; g.y = wy;
        g.spawnX = wx; g.spawnY = wy;
        golemites.add(g);
    }

    private void buildInteriorFor(DomainData d) {
        wallCollisions.clear();
        skeletons.clear();
        golemites.clear();
        activeRoom = null;
        roomBgImage = null;
        nearDoorRoom = null;
        bossSkeleton = null;
        darkMerchant = null;

        if (d.id.equals("Tomb")) {
            // FIX: Dark Merchant sekarang cuma muncul di Tomb (sebelumnya muncul di semua domain)
            darkMerchant = new DarkMerchant(gp);
            darkMerchant.x = 410;
            darkMerchant.y = 150;

            spawnSkeleton(800,  330);
            spawnSkeleton(880,  350);
            spawnSkeleton(985,  260);
            spawnSkeleton(1080, 285);
            spawnSkeleton(1080, 380);
            spawnSkeleton(785,  520);
            spawnSkeleton(730,  500);
            spawnSkeleton(700,  370);
            spawnSkeleton(940,  600);
            spawnSkeleton(1000, 640);
            spawnSkeleton(110,  290);
            spawnSkeleton(230,  290);
            spawnSkeleton(200,  430);
            spawnSkeleton(200,  600);
            spawnSkeleton(270,  900);
            spawnSkeleton(200,  970);
            spawnSkeleton(300,  970);
            spawnSkeleton(580, 1040);
            spawnSkeleton(610,  990);
            spawnSkeleton(1370, 890);
            spawnSkeleton(1430, 980);
        }
        
        if (d.id.equals("GolemHide")) {
            // Cuma golemite, tanpa skeleton dan tanpa merchant
            spawnGolemite(300,  500);
            spawnGolemite(520,  720);
            spawnGolemite(900,  550);
            spawnGolemite(1100, 450);
            spawnGolemite(1300, 720);
            spawnGolemite(700,  920);
            spawnGolemite(1000, 960);
            spawnGolemite(400,  1000);
        }

        try {
            java.net.URL url = getClass().getResource(d.backgroundPath);
            bgImage = url != null ? new ImageIcon(url) : null;
            if (url == null) System.out.println("[DomainManager] Background tidak ketemu: " + d.backgroundPath);
        } catch (Exception e) { bgImage = null; e.printStackTrace(); }

        for (Rectangle r : d.collisions) wallCollisions.add(r);
    }
    
    
    

    public void draw(Graphics2D g2) {
        drawBackground(g2);
    }

    public void drawBackground(Graphics2D g2) {
        if (activeDomain == null) return;
        g2.setColor(new Color(5, 5, 8));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int psx = gp.player.screenX, psy = gp.player.screenY;
        int camX = gp.player.x, camY = gp.player.y;

        // ── BAGIAN DUNIA (ikut ke-zoom): gambar background, teks countdown boss, debug collision ──
        java.awt.geom.AffineTransform baseTx = gp.applyWorldZoom(g2);
        if (activeRoom != null) {
            if (roomBgImage != null)
                g2.drawImage(roomBgImage.getImage(), 0-camX+psx, 0-camY+psy,
                    activeRoom.width*2, activeRoom.height*2, gp);
            drawBossSpawnCountdown(g2);
            drawCollisionDebug(g2);
        } else {
            if (bgImage != null)
                g2.drawImage(bgImage.getImage(), 0-camX+psx, 0-camY+psy,
                    activeDomain.width*2, activeDomain.height*2, gp);
            drawCollisionDebug(g2);
        }
        g2.setTransform(baseTx);

        // ── BAGIAN UI (layar normal, gak ke-zoom): hint, dialog konfirmasi, fade ──
        if (activeRoom != null) {
            drawRoomHint(g2, activeRoom);
            drawBossAltarHintIfNear(g2);
            drawBossSpawnConfirmDialog(g2);
        } else {
            drawHint(g2, activeDomain);
            drawDoorHintIfNear(g2);
            drawExitHintIfNear(g2);
        }
        drawFadeOverlay(g2);
    }

    public void drawEntities(Graphics2D g2) { /* Y-sort dihandle GamePanel */ }

    /** Render sprite merchant (dipanggil GamePanel saat Y-sort) */
    public void drawMerchant(Graphics2D g2) {
        if (darkMerchant == null || activeRoom != null) return;
        darkMerchant.draw(g2);
    }

    /** Render sprite boss (dipanggil GamePanel saat Y-sort, pas lagi di dalam room) */
    public void drawBoss(Graphics2D g2) {
        if (bossSkeleton == null || activeRoom == null) return;
        bossSkeleton.draw(g2);
    }

    /** Render shop UI + dialog box + feedback (dipanggil GamePanel setelah semua entity) */
    public void drawShopOverlay(Graphics2D g2) {
        if (darkMerchant == null || activeRoom != null) return;

        // Dialog box
        if (darkMerchant.isInDialog()) {
            darkMerchant.drawDialogBox(g2);
        }

        // Shop UI
        if (darkMerchant.isShopOpen()) {
            darkMerchant.drawShopUI(g2);
        }

        // Feedback pesan beli
        if (shopFeedbackTimer > 0 && !shopFeedback.isEmpty()) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(shopFeedback);
            int bx = gp.screenWidth / 2 - tw / 2 - 12;
            int by = gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 80;
            // warna berdasarkan isi pesan
            Color bgCol  = shopFeedback.startsWith("✓")
                ? new Color(0, 60, 0, 200)
                : new Color(80, 0, 0, 200);
            Color txtCol = shopFeedback.startsWith("✓")
                ? new Color(100, 255, 100)
                : new Color(255, 100, 100);
            g2.setColor(bgCol);
            g2.fillRoundRect(bx, by, tw + 24, 24, 8, 8);
            g2.setColor(txtCol);
            g2.drawString(shopFeedback, bx + 12, by + 17);
        }
    }

    private void checkBossSpawnAltar() {
        if (bossSpawnConfirmOpen) return; // lagi nunggu jawaban player, jangan re-trigger

        Rectangle playerArea = gp.player.getInteractArea();
        if (!playerArea.intersects(bossSpawnAltar)) return;
        if (!gp.KeyH.interactPressed) return;
        gp.KeyH.interactPressed = false;

        if (bossSkeleton != null) {
            if (gp.speechBubble != null) gp.speechBubble.show("The Skeleton King still stands!");
            return;
        }
        if (bossSpawnCountdownActive) {
            if (gp.speechBubble != null) gp.speechBubble.show("The ritual is already in progress!");
            return;
        }

        ItemManager im = ItemManager.get();
        int keyCount = getItemCount(im.SKELETON_KEY);
        if (keyCount <= 0) {
            if (gp.speechBubble != null) gp.speechBubble.show("A Skeleton Key is required to awaken the Boss!");
            return;
        }

        // Semua syarat lolos -> buka dialog konfirmasi
        bossSpawnConfirmOpen = true;
        bossSpawnConfirmSelection = 0; // default YES
        confirmNavCooldown = 0;
    }

    private void checkBossSpawnConfirm() {
        if (!bossSpawnConfirmOpen) return;

        if (confirmNavCooldown > 0) confirmNavCooldown--;

        if (confirmNavCooldown == 0 &&
            (gp.KeyH.leftPressed || gp.KeyH.rightPressed || gp.KeyH.upPressed || gp.KeyH.downPressed)) {
            bossSpawnConfirmSelection = 1 - bossSpawnConfirmSelection; // toggle YES <-> NO
            confirmNavCooldown = 12;
        }

        if (gp.KeyH.escPressed) {
            gp.KeyH.escPressed = false;
            bossSpawnConfirmOpen = false;
            return;
        }

        if (gp.KeyH.interactPressed) {
            gp.KeyH.interactPressed = false;
            bossSpawnConfirmOpen = false;

            if (bossSpawnConfirmSelection == 0) { // YES
                ItemManager im = ItemManager.get();
                removeItem(im.SKELETON_KEY, 1);
                bossSpawnCountdownActive = true;
                bossSpawnCountdownTimer = BOSS_SPAWN_COUNTDOWN_FRAMES;
                if (gp.speechBubble != null) gp.speechBubble.show("Skeleton Key used... The Boss will rise!");
            }
            // NO -> dialog ditutup, gak ada yang kejadian
        }
    }

    private void drawBossSpawnConfirmDialog(Graphics2D g2) {
        if (!bossSpawnConfirmOpen) return;

        String title = "Awaken the Skeleton King?";
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        int titleW = g2.getFontMetrics().stringWidth(title);

        int boxW = Math.max(titleW + 50, 220);
        int boxH = 80;
        int boxX = gp.screenWidth / 2 - boxW / 2;
        int boxY = 60;

        g2.setColor(new Color(15, 15, 15, 235));
        g2.fillRect(boxX, boxY, boxW, boxH);
        g2.setColor(Color.WHITE);
        g2.drawRect(boxX, boxY, boxW, boxH);
        g2.drawString(title, gp.screenWidth / 2 - titleW / 2, boxY + 26);

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        int centerX = gp.screenWidth / 2;
        int optY = boxY + 58;

        g2.setColor(bossSpawnConfirmSelection == 0 ? Color.WHITE : new Color(130, 130, 130));
        g2.drawString(bossSpawnConfirmSelection == 0 ? "> YES" : "  YES", centerX - 70, optY);

        g2.setColor(bossSpawnConfirmSelection == 1 ? Color.WHITE : new Color(130, 130, 130));
        g2.drawString(bossSpawnConfirmSelection == 1 ? "> NO" : "  NO", centerX + 20, optY);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOGIKA INTERAKSI & PEMBELIAN
    // ─────────────────────────────────────────────────────────────────────────

    private void checkMerchantInteract() {
        if (darkMerchant == null) return;

        // Shop terbuka:
        // navigasi = mouse wheel
        // beli = klik tombol BUY
        // tutup = ESC
        if (darkMerchant.isShopOpen()) {

            if (gp.KeyH.escPressed) {
                darkMerchant.closeShop();
                gp.KeyH.escPressed = false;
            }

            return;
        }

        // Dialog sedang berjalan
        if (darkMerchant.isInDialog()) {
            if (gp.KeyH.interactPressed) {
                darkMerchant.onInteract();
                gp.KeyH.interactPressed = false;
            }
            return;
        }

        // Belum berinteraksi — cek jarak
        if (!gp.KeyH.interactPressed) return;

        int dist = (int) Math.hypot(
            darkMerchant.x - gp.player.x,
            darkMerchant.y - gp.player.y
        );

        if (dist < gp.tileSize * 2) {
            darkMerchant.onInteract();
            gp.KeyH.interactPressed = false;
        }
    }

    public void handleMerchantMouseClick(java.awt.event.MouseEvent e) {
        if (darkMerchant == null) return;
        if (!darkMerchant.isShopOpen()) return;

        if (darkMerchant.handleMouseClick(e)) {
            handleShopBuy();
        }
    }

    public void handleMerchantMouseMove(java.awt.event.MouseEvent e) {
        if (darkMerchant == null) return;
        if (!darkMerchant.isShopOpen()) return;

        darkMerchant.handleMouseMove(e);
    }

    public void handleMerchantMouseWheel(int rotation) {
        if (darkMerchant == null) return;
        if (!darkMerchant.isShopOpen()) return;

        darkMerchant.handleMouseWheel(rotation);
    }

    /**
     * Cek stok Bone & Skull, kurangi jika cukup, tambahkan item ke inventory.
     */
    private void handleShopBuy() {
        DarkMerchant.ShopItem item = darkMerchant.shopItems[darkMerchant.selectedIndex];
        ItemManager im = ItemManager.get();

        // Hitung stok Bone dan Skull yang dimiliki player
        int playerBone  = getItemCount(im.BONE);
        int playerSkull = getItemCount(im.SKULL);

        // Validasi stok
        if (playerBone < item.boneCost) {
            shopFeedback = "✗ Bone tidak cukup! (perlu " + item.boneCost + ")";
            shopFeedbackTimer = 100;
            return;
        }
        if (item.skullCost > 0 && playerSkull < item.skullCost) {
            shopFeedback = "✗ Skull tidak cukup! (perlu " + item.skullCost + ")";
            shopFeedbackTimer = 100;
            return;
        }

        // Kurangi Bone
        removeItem(im.BONE, item.boneCost);

        // Kurangi Skull (kalau ada biaya)
        if (item.skullCost > 0) {
            removeItem(im.SKULL, item.skullCost);
        }

        // Tambah item yang dibeli ke inventory
        switch (item.name) {
            case "Bone Sword":
                im.add(im.BONE_SWORD, 1);
                break;
            case "Skeleton Key":
                im.add(im.SKELETON_KEY, 1);
                break;
            default:
                System.out.println("[Shop] Item tidak dikenal: " + item.name);
        }

        shopFeedback = "✓ " + item.name + " berhasil dibeli!";
        shopFeedbackTimer = 120;
        Main.Log.d("[Shop] Beli: " + item.name);

        darkMerchant.closeShop();
    }

    /** Hitung jumlah item tertentu di inventory */
    private int getItemCount(Item.Item target) {
        for (ItemStack stack : ItemManager.inventory) {
            if (stack != null && stack.item == target) return stack.amount;
        }
        return 0;
    }

    /** Kurangi jumlah item di inventory, hapus stack jika habis */
    private void removeItem(Item.Item target, int amount) {
        for (ItemStack stack : ItemManager.inventory) {
            if (stack != null && stack.item == target) {
                stack.amount -= amount;
                if (stack.amount <= 0) {
                    ItemManager.inventory.remove(stack);
                }
                return;
            }
        }
    }

    private void drawHint(Graphics2D g2, DomainData d) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 220, 120, 200));
        String hint = "[ " + d.id + " ]";
        g2.drawString(hint, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(hint)/2, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 16);
    }

    private void drawRoomHint(Graphics2D g2, RoomData r) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 220, 120, 200));
        String hint = "[ " + r.id + " ]  |  ESC = Keluar";
        g2.drawString(hint, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(hint)/2, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 16);
    }

    private void drawBossSpawnCountdown(Graphics2D g2) {
        if (!bossSpawnCountdownActive) return;
        int secondsLeft = (bossSpawnCountdownTimer / GAME_FPS) + 1;
        if (secondsLeft > 10) secondsLeft = 10;
        if (secondsLeft < 1) secondsLeft = 1;

        int sx = COUNTDOWN_WORLD_X - gp.player.x + gp.player.screenX;
        int sy = COUNTDOWN_WORLD_Y - gp.player.y + gp.player.screenY;

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        String label = "THE SKELETON KING RISES...";
        int lw = g2.getFontMetrics().stringWidth(label);
        g2.setColor(Color.WHITE);
        g2.drawString(label, sx - lw / 2, sy - 55);

        g2.setFont(new Font("Serif", Font.BOLD, 42));
        String txt = String.valueOf(secondsLeft);
        int tw = g2.getFontMetrics().stringWidth(txt);
        g2.setColor(Color.WHITE);
        g2.drawString(txt, sx - tw / 2, sy);
    }

    private void drawBossAltarHintIfNear(Graphics2D g2) {
        if (bossSkeleton != null || bossSpawnCountdownActive) return;
        Rectangle playerArea = gp.player.getInteractArea();
        if (!playerArea.intersects(bossSpawnAltar)) return;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 255, 150, 220));
        String hint = "[E] Awake the Skeleton King (need a Skeleton Key)";
        g2.drawString(hint, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(hint) / 2, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 60);
    }

    private void drawDoorHintIfNear(Graphics2D g2) {
        if (nearDoorRoom == null) return;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 255, 150, 220));
        String hint = "[E] Enter " + nearDoorRoom.id;
        g2.drawString(hint, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(hint)/2, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 40);
    }

    private void drawExitHintIfNear(Graphics2D g2) {
        if (!nearExitPoint) return;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 255, 150, 220));
        String hint = "Tekan [E] untuk keluar dungeon";
        g2.drawString(hint, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(hint)/2, gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 40);
    }

    private void drawFadeOverlay(Graphics2D g2) {
        if (fadeAlpha <= 0) return;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha/255f));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(old);
    }

    private void startFade(boolean in, Runnable onComplete) {
        fadeIn = in; fadeAlpha = in ? 255 : 0; fading = true; onFadeComplete = onComplete;
    }

    private void handleFade() {
        if (!fading) return;
        int speed = 12;
        if (fadeIn) {
            fadeAlpha -= speed;
            if (fadeAlpha <= 0) { fadeAlpha = 0; fading = false; }
        } else {
            fadeAlpha += speed;
            if (fadeAlpha >= 255) {
                fadeAlpha = 255; fading = false;
                if (onFadeComplete != null) {
                    onFadeComplete.run(); onFadeComplete = null;
                    fading = true; fadeIn = true;
                }
            }
        }
    }

    public boolean checkCollision(Rectangle playerBox) {
        if (!insideDomain) return false; // di luar domain, tembok domain gak boleh ngaruh
        for (Rectangle r : wallCollisions)
            if (playerBox.intersects(r)) return true;
        return false;
    }
}