package bahan;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Item.Item;
import Item.ItemManager;
import Main.GamePanel;
import Object.breakable;

/**
 * Peta dinamis: dunia dibagi jadi chunk {@link #CH} x {@link #CH} tile. Chunk di dekat player dimuat, chunk yang
 * jauh dibuang, jadi jumlah objek / gambar yang aktif tidak ikut membesar walau dunianya luas.
 *
 * Isi sebuah chunk (semuanya murni fungsi dari koordinat, lihat {@link WorldGen}, jadi dibuang lalu dimuat lagi
 * hasilnya sama):
 *  - tile dasar area baru (di luar peta lama) -> ditulis ke mapTileNum selama chunk aktif
 *  - kulit salju / es (gambar RGBA hasil {@link IceGround}), di-bake di thread terpisah supaya game tidak patah-patah
 *  - pohon / batu / kristal yang bisa dihancurkan: dipinjam dari "kolam" slot di TileM.breakables, dan collision-nya
 *    dipasang / dilepas ikut chunk
 *  - hiasan tanpa collision (semak, cahaya kristal)
 *
 * Semua perubahan state game (mapTileNum, breakables, objCol) hanya terjadi di thread game; thread pembuat gambar
 * cuma menghasilkan gambar.
 */
public final class ChunkManager {

    public static final int CH = 32;
    /** chunk yang jaraknya (tile, dari player) kurang dari ini dimuat */
    public static final int LOAD_TILES = 40;
    /** chunk yang jaraknya lebih dari ini dibuang (lebih besar dari LOAD supaya tidak bolak-balik muat-buang) */
    public static final int UNLOAD_TILES = 64;
    /** paling banyak chunk yang diaktifkan per frame (supaya tidak ada frame yang berat) */
    private static final int MAX_ACTIVATE_PER_FRAME = 2;

    private final GamePanel gp;
    private final TileM tm;
    private final WorldGen gen;
    private final IceSprites spr;
    private final int ts;
    private final int ncx, ncy;
    private final Chunk[] chunks;
    private final List<Chunk> active = new ArrayList<>();         // hanya disentuh thread game
    private volatile Chunk[] view = new Chunk[0];                  // salinan 'active' untuk thread gambar (tidak pernah diubah, hanya diganti)
    private boolean viewDirty = false;
    private final ExecutorService baker;
    /** chunk yang jaraknya (tile) kurang dari ini dari player pasti dimuat + gambarnya siap SEBELUM frame berikutnya digambar (isi layar) */
    private final int forceTiles;

    // kolam slot breakables
    private final int poolBase;
    private final int[] freeSlots;
    private int nFree;

    // gambar & animasi pohon biasa (dipakai pohon hutan di area baru)
    private final BufferedImage[] oakIdle, oakHit, oakDown, frostIdle, frostHit, frostDown;
    private final BufferedImage rockImg, bushImg, snowBushImg;

    /** statistik untuk tes / debug */
    public int activations = 0, deactivations = 0, syncBakes = 0;

    /** hiasan tanpa collision di satu chunk: tidak pernah diubah sesudah dibuat, jadi aman dibaca thread gambar */
    private static final class Decor {
        static final Decor EMPTY = new Decor(0);
        final int n;
        final int[] x, y, w, h;
        final byte[] kind;
        final BufferedImage[] img;
        Decor(int n) {
            this.n = n;
            x = new int[n]; y = new int[n]; w = new int[n]; h = new int[n];
            kind = new byte[n]; img = new BufferedImage[n];
        }
    }

    /*
     * Pembagian thread: update() (muat / buang / ubah mapTileNum / breakables / collision) hanya jalan di thread game.
     * Thread gambar (EDT) cuma MEMBACA: daftar chunk lewat 'view', gambar lewat 'baked', hiasan lewat 'decor' --
     * ketiganya diganti utuh (volatile), tidak pernah diubah di tempat.
     */
    private static final class Chunk {
        final int cx, cy, col0, row0;
        boolean active;                      // thread game saja
        Future<IceGround.Result> future;     // thread game saja
        volatile IceGround.Result baked;     // gambar salju/es + penanda tile tertutup; null = belum siap / tidak ada
        boolean bakeDone;                    // thread game saja
        int[] slots = new int[0];            // thread game saja
        volatile Decor decor = Decor.EMPTY;

        Chunk(int cx, int cy) {
            this.cx = cx; this.cy = cy;
            this.col0 = cx * CH; this.row0 = cy * CH;
        }
    }

    public ChunkManager(GamePanel gp, TileM tm, WorldGen gen, IceSprites spr, int poolBase, int poolSize,
                        BufferedImage[] oakIdle, BufferedImage[] oakHit, BufferedImage[] oakDown,
                        BufferedImage[] frostIdle, BufferedImage[] frostHit, BufferedImage[] frostDown) {
        this.gp = gp;
        this.tm = tm;
        this.gen = gen;
        this.spr = spr;
        this.ts = gp.tileSize;
        this.ncx = (WorldGen.WORLD_COLS + CH - 1) / CH;
        this.ncy = (WorldGen.WORLD_ROWS + CH - 1) / CH;
        this.chunks = new Chunk[ncx * ncy];
        for (int cy = 0; cy < ncy; cy++) for (int cx = 0; cx < ncx; cx++) chunks[cy * ncx + cx] = new Chunk(cx, cy);

        this.poolBase = poolBase;
        this.freeSlots = new int[poolSize];
        for (int i = 0; i < poolSize; i++) freeSlots[i] = poolBase + poolSize - 1 - i;   // slot pertama dipakai duluan
        this.nFree = poolSize;

        this.oakIdle = oakIdle; this.oakHit = oakHit; this.oakDown = oakDown;
        this.frostIdle = frostIdle; this.frostHit = frostHit; this.frostDown = frostDown;
        this.rockImg = tm.tile[71] != null ? tm.tile[71].image : null;
        this.bushImg = tm.tile[66] != null ? tm.tile[66].image : null;
        this.snowBushImg = tm.tile[69] != null ? tm.tile[69].image : null;

        // layar paling lebar = setengah lebar layar / zoom minimum; ditambah cadangan
        double halfTiles = Math.max(gp.screenWidth, gp.screenHeight) / (2.0 * ts * GamePanel.ZOOM_MIN);
        this.forceTiles = (int) Math.ceil(halfTiles) + 4;

        this.baker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "chunk-baker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    public WorldGen gen() { return gen; }
    public IceSprites sprites() { return spr; }
    public int activeCount() { return active.size(); }
    public int usedSlots() { return freeSlots.length - nFree; }

    // ================================================================== siklus muat / buang

    /** Dipanggil tiap frame (saat di dunia luar): muat chunk yang mendekat, buang yang menjauh, ambil gambar yang selesai di-bake. */
    public void update() {
        int ptx = Math.floorDiv(gp.player.x, ts), pty = Math.floorDiv(gp.player.y, ts);

        // buang yang jauh
        for (int i = active.size() - 1; i >= 0; i--) {
            Chunk c = active.get(i);
            if (distance(c, ptx, pty) > UNLOAD_TILES) deactivate(c);
        }
        // yang dekat sekali (isi layar) harus siap sekarang juga, tanpa batas per frame (biasanya sudah dimuat lebih dulu)
        for (Chunk c : chunks) {
            if (distance(c, ptx, pty) > forceTiles) continue;
            if (!c.active) activate(c);
            if (!c.bakeDone) { pollBake(c, true); syncBakes++; }
        }
        // muat yang dekat, yang paling dekat dulu
        int made = 0;
        while (made < MAX_ACTIVATE_PER_FRAME) {
            Chunk best = null;
            int bestD = Integer.MAX_VALUE;
            for (Chunk c : chunks) {
                if (c.active) continue;
                int d = distance(c, ptx, pty);
                if (d <= LOAD_TILES && d < bestD) { best = c; bestD = d; }
            }
            if (best == null) break;
            activate(best);
            made++;
        }
        // gambar yang sudah selesai
        for (Chunk c : active) pollBake(c, false);
        // kasih tahu thread gambar kalau daftar chunk aktif berubah
        if (viewDirty) { view = active.toArray(new Chunk[0]); viewDirty = false; }
    }

    private int distance(Chunk c, int ptx, int pty) {
        int dx = Math.max(Math.max(c.col0 - ptx, ptx - (c.col0 + CH - 1)), 0);
        int dy = Math.max(Math.max(c.row0 - pty, pty - (c.row0 + CH - 1)), 0);
        return Math.max(dx, dy);
    }

    private void activate(Chunk c) {
        c.active = true;
        c.bakeDone = false;
        c.baked = null;
        active.add(c);
        viewDirty = true;
        activations++;

        // 1) tile dasar area baru
        int cEnd = Math.min(c.col0 + CH, WorldGen.WORLD_COLS), rEnd = Math.min(c.row0 + CH, WorldGen.WORLD_ROWS);
        for (int col = c.col0; col < cEnd; col++) {
            for (int row = c.row0; row < rEnd; row++) {
                if (!gen.inCore(col, row)) tm.mapTileNum[col][row] = gen.groundTile(col, row);
            }
        }

        // 2) gambar salju / es: di thread lain
        final int col0 = c.col0, row0 = c.row0;
        c.future = baker.submit(() -> {
            try {
                return IceGround.bake(gen, col0, row0, CH);
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        });

        // 3) objek
        List<WorldGen.Spawn> spawns = gen.populate(c.col0, c.row0, CH, ts);
        int nBreak = 0, nDecor = 0;
        for (WorldGen.Spawn s : spawns) { if (isDecor(s.kind)) nDecor++; else nBreak++; }
        c.slots = new int[nBreak];
        Decor d = new Decor(nDecor);
        int usedDecor = 0;
        int usedBreak = 0;
        for (WorldGen.Spawn s : spawns) {
            if (isDecor(s.kind)) {
                BufferedImage img = decorImage(s);
                if (img == null) continue;
                int i = usedDecor++;
                d.x[i] = s.x; d.y[i] = s.y; d.w[i] = s.w; d.h[i] = s.h; d.kind[i] = (byte) s.kind; d.img[i] = img;
            } else {
                if (nFree == 0) continue;               // kolam habis: lewati objek ini (tidak crash)
                int slot = freeSlots[--nFree];
                breakable b = create(s);
                if (b == null) { freeSlots[nFree++] = slot; continue; }
                tm.breakables[slot] = b;
                tm.breakableTile[slot] = (s.kind == WorldGen.K_ICE_ROCK || s.kind == WorldGen.K_CRYSTAL || s.kind == WorldGen.K_ROCK) ? 71 : 55;
                c.slots[usedBreak++] = slot;
            }
        }
        if (usedBreak < c.slots.length) c.slots = java.util.Arrays.copyOf(c.slots, usedBreak);
        // hiasan yang gagal dibuat (gambar null) dibuang dari ujung: ukuran sebenarnya = usedDecor
        c.decor = usedDecor == nDecor ? d : trim(d, usedDecor);
    }

    private static Decor trim(Decor d, int n) {
        Decor t = new Decor(n);
        System.arraycopy(d.x, 0, t.x, 0, n); System.arraycopy(d.y, 0, t.y, 0, n);
        System.arraycopy(d.w, 0, t.w, 0, n); System.arraycopy(d.h, 0, t.h, 0, n);
        System.arraycopy(d.kind, 0, t.kind, 0, n); System.arraycopy(d.img, 0, t.img, 0, n);
        return t;
    }

    private void deactivate(Chunk c) {
        c.active = false;
        active.remove(c);
        viewDirty = true;
        deactivations++;
        for (int slot : c.slots) {
            breakable b = tm.breakables[slot];
            if (b != null) b.unregister();
            tm.breakables[slot] = null;
            freeSlots[nFree++] = slot;
        }
        c.slots = new int[0];
        c.decor = Decor.EMPTY;
        c.baked = null;
        if (c.future != null) c.future.cancel(false);
        c.future = null;
        c.bakeDone = false;
        // sel area baru yang tidak termuat dianggap air (tidak bisa dilewati pathfinding)
        int cEnd = Math.min(c.col0 + CH, WorldGen.WORLD_COLS), rEnd = Math.min(c.row0 + CH, WorldGen.WORLD_ROWS);
        for (int col = c.col0; col < cEnd; col++) {
            for (int row = c.row0; row < rEnd; row++) {
                if (!gen.inCore(col, row)) tm.mapTileNum[col][row] = 2;
            }
        }
    }

    private void pollBake(Chunk c, boolean wait) {
        if (c.bakeDone || c.future == null) return;
        if (!wait && !c.future.isDone()) return;
        try {
            c.baked = c.future.get();
        } catch (InterruptedException | ExecutionException | java.util.concurrent.CancellationException e) {
            c.baked = null;
        }
        c.bakeDone = true;
    }

    // ================================================================== bikin objek

    private static boolean isDecor(int kind) {
        return kind == WorldGen.K_BUSH || kind == WorldGen.K_BUSH_SNOW || kind == WorldGen.K_GLOW
            || (kind >= WorldGen.K_TUFT && kind <= WorldGen.K_DRIFT);
    }

    private BufferedImage decorImage(WorldGen.Spawn s) {
        switch (s.kind) {
            case WorldGen.K_BUSH: return bushImg;
            case WorldGen.K_BUSH_SNOW: return snowBushImg;
            case WorldGen.K_GLOW: return spr.glow;
            case WorldGen.K_TUFT: return spr.tuft[s.variant & 1];
            case WorldGen.K_PEBBLE: return spr.pebble[s.variant & 1];
            case WorldGen.K_SHARD: return spr.shard[s.variant & 1];
            case WorldGen.K_DRIFT: return spr.drift[s.variant & 1];
            default: return null;
        }
    }

    private breakable create(WorldGen.Spawn s) {
        Item drop = ItemManager.get().ORE;
        BufferedImage img;
        BufferedImage[] idle = null, hit = null, down = null;
        boolean shadow = false;
        double[] col = null;
        int v = s.variant;
        switch (s.kind) {
            case WorldGen.K_OAK:
                img = oakIdle[0]; idle = oakIdle; hit = oakHit; down = oakDown;
                drop = ItemManager.get().LOG; shadow = true; break;
            case WorldGen.K_OAK_FROST:
                img = frostIdle[0]; idle = frostIdle; hit = frostHit; down = frostDown;
                drop = ItemManager.get().LOG; shadow = true; break;
            case WorldGen.K_PINE:
                img = spr.pine[v % spr.pine.length]; hit = spr.pineHit[v % spr.pine.length]; down = spr.pineDown[v % spr.pine.length];
                drop = ItemManager.get().LOG; col = new double[] {0.38, 0.72, 0.24, 0.16}; break;
            case WorldGen.K_DEAD:
                img = spr.dead[v % spr.dead.length]; hit = spr.deadHit[v % spr.dead.length]; down = spr.deadDown[v % spr.dead.length];
                drop = ItemManager.get().LOG; col = new double[] {0.37, 0.80, 0.26, 0.12}; break;
            case WorldGen.K_ICE_ROCK:
                img = spr.rock[v % spr.rock.length]; col = new double[] {0.14, 0.46, 0.72, 0.32}; break;
            case WorldGen.K_CRYSTAL:
                img = spr.crystal[v % spr.crystal.length]; col = new double[] {0.18, 0.62, 0.64, 0.26}; break;
            case WorldGen.K_ROCK:
                img = rockImg; break;
            default:
                return null;
        }
        if (img == null) return null;
        breakable b = new breakable(s.x, s.y, s.w, s.h, img, s.hp, drop, gp);
        b.hasShadow = shadow;
        if (col != null) b.setCollisionFractions(col[0], col[1], col[2], col[3]);
        if (idle != null) b.setIdleFrames(idle);
        if (hit != null) b.setHitFrames(hit);
        if (down != null) b.setDownFrames(down);
        return b;
    }

    // ================================================================== gambar

    /**
     * true kalau tile (col, row) tertutup penuh oleh gambar salju / es yang sudah siap, jadi tile dasarnya tidak perlu
     * digambar (hasil di layar sama persis, tapi jauh lebih ringan di tengah biome es).
     */
    public boolean isCovered(int col, int row) {
        if (col < 0 || row < 0) return false;
        int cx = col / CH, cy = row / CH;
        if (cx >= ncx || cy >= ncy) return false;
        Chunk c = chunks[cy * ncx + cx];
        IceGround.Result r = c.baked;
        return r != null && r.covered[(row - c.row0) * CH + (col - c.col0)];
    }

    /** Salju / es dan cahaya kristal: digambar tepat di atas tile dasar, di bawah semua objek. */
    public void drawGround(Graphics2D g2, int camX, int camY) {
        int sxOff = gp.player.screenX - camX, syOff = gp.player.screenY - camY;
        int size = CH * ts;
        int vx0 = gp.viewMinX(), vx1 = gp.viewMaxX(), vy0 = gp.viewMinY(), vy1 = gp.viewMaxY();
        Chunk[] vs = view;
        for (Chunk c : vs) {
            int sx = c.col0 * ts + sxOff, sy = c.row0 * ts + syOff;
            if (sx + size < vx0 || sx > vx1 || sy + size < vy0 || sy > vy1) continue;
            IceGround.Result r = c.baked;
            if (r != null) g2.drawImage(r.image, sx, sy, size, size, null);
        }
        // cahaya kristal berdenyut pelan
        Composite old = null;
        float pulse = 0.70f + 0.30f * (float) Math.sin(System.nanoTime() / 1.1e9);
        for (Chunk c : vs) {
            Decor d = c.decor;
            for (int i = 0; i < d.n; i++) {
                if (d.kind[i] != WorldGen.K_GLOW) continue;
                int sx = d.x[i] + sxOff, sy = d.y[i] + syOff;
                if (sx + d.w[i] < vx0 || sx > vx1 || sy + d.h[i] < vy0 || sy > vy1) continue;
                if (old == null) old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
                g2.drawImage(d.img[i], sx, sy, d.w[i], d.h[i], null);
            }
        }
        if (old != null) g2.setComposite(old);
    }

    /** Tambahkan hiasan berdiri (semak) yang terlihat ke daftar urut-Y. Format entri: {sortY, 7, chunkIndex * 4096 + nomor}. */
    public void collectDecor(List<int[]> drawList, int camX, int camY, int margin) {
        int sxOff = gp.player.screenX - camX, syOff = gp.player.screenY - camY;
        int vx0 = gp.viewMinX() - margin, vx1 = gp.viewMaxX() + margin, vy0 = gp.viewMinY() - margin, vy1 = gp.viewMaxY() + margin;
        for (Chunk c : view) {
            int ci = c.cy * ncx + c.cx;
            Decor d = c.decor;
            for (int i = 0; i < d.n; i++) {
                if (d.kind[i] == WorldGen.K_GLOW) continue;
                int sx = d.x[i] + sxOff, sy = d.y[i] + syOff;
                if (sx + d.w[i] < vx0 || sx > vx1 || sy + d.h[i] < vy0 || sy > vy1) continue;
                int k = d.kind[i];
                int sortY = (k == WorldGen.K_BUSH || k == WorldGen.K_BUSH_SNOW) ? d.y[i] + d.h[i] - 40
                          : (k == WorldGen.K_DRIFT ? d.y[i] + d.h[i] - 400 : d.y[i] + d.h[i]);   // gundukan salju selalu di bawah objek berdiri
                drawList.add(new int[] {sortY, 7, ci * 4096 + i});
            }
        }
    }

    public void drawDecor(Graphics2D g2, int packed, int camX, int camY) {
        Chunk c = chunks[packed / 4096];
        int i = packed % 4096;
        Decor d = c.decor;                       // satu kali baca: kalau chunk keburu dibuang, d = EMPTY dan kita berhenti
        if (i >= d.n) return;
        int kind = d.kind[i];
        if (kind == WorldGen.K_BUSH || kind == WorldGen.K_BUSH_SNOW) {
            // sama dengan semak lama di worldObjects: gambar tile 66 / 69 dengan bayangan
            tm.drawObject(g2, d.x[i], d.y[i], d.w[i], d.h[i], kind == WorldGen.K_BUSH ? 66 : 69, camX, camY, 1);
            return;
        }
        int sx = d.x[i] - camX + gp.player.screenX, sy = d.y[i] - camY + gp.player.screenY;
        if (sx + d.w[i] < gp.viewMinX() || sx > gp.viewMaxX() || sy + d.h[i] < gp.viewMinY() || sy > gp.viewMaxY()) return;
        g2.drawImage(d.img[i], sx, sy, d.w[i], d.h[i], null);
    }

    // ================================================================== tes / debug

    /** daftar chunk aktif: {cx, cy, adaOverlay(0/1), jumlahObjek} */
    public int[][] describeActive() {
        int[][] out = new int[active.size()][];
        for (int i = 0; i < out.length; i++) {
            Chunk c = active.get(i);
            out[i] = new int[] {c.cx, c.cy, c.baked != null ? 1 : 0, c.slots.length + c.decor.n};
        }
        return out;
    }

    public void shutdown() { baker.shutdownNow(); }
}
