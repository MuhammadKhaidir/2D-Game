package bahan;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Entitas.Entity;
import Main.GamePanel;
import bahan.WaterDomainArt.Sprite;

/**
 * Peta "Water Domain": gua bawah tanah berisi reruntuhan kuil yang terendam, 112 x 84 tile (5376 x 4032 px, kira-kira
 * 11 kali luas Tomb). Semuanya dibuat prosedural di kode dengan seed tetap, jadi bentuknya selalu sama.
 *
 * Tata letak (koordinat tile):
 *  - Teluk Kedatangan (selatan, 56,72): peron batu + kolam keluar. Pemain muncul dari sini dan lompat ke daratan.
 *  - Jalan batu ke utara menuju Plaza Pasang-Surut (56,44) dengan lingkaran rune besar, 8 pilar, 4 obelisk.
 *  - Utara: Kuil Pasang-Surut (56,10), gerbang besar bersegel rune, dua patung penjaga, kolam air mancur.
 *  - Barat: Gua Karang (20,44) -> Taman Rumput Laut (22,21) dan Pantai Kerang (20,68).
 *  - Timur: Pelataran Pilar (92,44, digenangi air dangkal) -> Gua Kristal (92,20) dan Taman Terendam (92,68).
 *  - Danau dalam (tidak bisa dilewati) memisahkan semuanya; air dangkal bisa diseberangi.
 * Tile: lantai batu, pasir, air dangkal (bisa dilewati), air dalam (penghalang), tebing (penghalang).
 */
public final class WaterDomainMap {

    public static final int COLS = 112, ROWS = 84, T = WaterDomainArt.T;
    public static final int ROCK = 0, FLOOR = 1, SAND = 2, SHALLOW = 3, DEEP = 4, FACE = 5;

    /** Pusat kolam keluar (px dunia domain) dan jarak ke titik mendarat kaki pemain di depannya (selatan kolam). */
    public static final int POOL_X = 56 * T + T / 2, POOL_Y = 70 * T + T / 2, LAND_DY = 153;
    public final int poolX, poolY, landX, landY;
    public final int pxW = COLS * T, pxH = ROWS * T;

    private final GamePanel gp;
    private final WaterDomainArt.Tiles tiles;
    private final byte[] type = new byte[COLS * ROWS];
    private final byte[] var = new byte[COLS * ROWS];
    private final byte[] mask = new byte[COLS * ROWS];      // ROCK: sisi terbuka | FLOOR/SAND: sisi berair dalam | air: sisi darat
    private final byte[] mask2 = new byte[COLS * ROWS];     // FLOOR: sisi berair dangkal | SAND: sisi berair
    private final boolean[] occ = new boolean[COLS * ROWS];
    private final int[] landDist = new int[COLS * ROWS];

    private final List<Prop> props = new ArrayList<>();
    private final List<Rectangle>[] buckets;
    private final List<Halo> halos = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();
    private final List<Lily> lilies = new ArrayList<>();
    private final List<Fish> fish = new ArrayList<>();
    private final List<int[]> bubbleSpots = new ArrayList<>();
    private final List<int[]> jumpFishSpots = new ArrayList<>();
    private final Map<String, Sprite> cache = new HashMap<>();
    private FishJumpManager jumpFish;
    private BufferedImage vignette;

    private final int[] deepSX = new int[2600], deepSY = new int[2600], deepI = new int[2600];
    private final int[] shalSX = new int[2600], shalSY = new int[2600], shalI = new int[2600];

    // ================================================================== bangun

    @SuppressWarnings("unchecked")
    public WaterDomainMap(GamePanel gp) {
        this.gp = gp;
        this.buckets = (List<Rectangle>[]) new List[COLS * ROWS];
        this.tiles = WaterDomainArt.buildTiles();

        generateLayout();
        deriveTiles();

        poolX = POOL_X;
        poolY = POOL_Y;
        landX = poolX;
        landY = poolY + LAND_DY;

        buildPoolBlockers();
        buildProps();
        buildAmbient();
    }

    // ---- bentuk dasar ------------------------------------------------

    private int at(int x, int y) {
        return (x < 0 || y < 0 || x >= COLS || y >= ROWS) ? ROCK : type[y * COLS + x];
    }

    private void put(int x, int y, int v) {
        if (x >= 0 && y >= 0 && x < COLS && y < ROWS) type[y * COLS + x] = (byte) v;
    }

    private static double wob(double th, int s) {
        return 1.0 + 0.085 * Math.sin(3 * th + s * 1.3) + 0.055 * Math.sin(5 * th + s * 2.1) + 0.03 * Math.sin(8 * th + s * 0.7);
    }

    /** Bentuk bulat tak beraturan, dibuat dengan cara menggoyang jari-jari menurut sudut. */
    private void blob(int v, double cx, double cy, double rx, double ry, int seed) {
        for (int y = (int) Math.floor(cy - ry * 1.25) - 1; y <= (int) Math.ceil(cy + ry * 1.25) + 1; y++) {
            for (int x = (int) Math.floor(cx - rx * 1.25) - 1; x <= (int) Math.ceil(cx + rx * 1.25) + 1; x++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                double r = Math.sqrt(dx * dx + dy * dy);
                if (r <= wob(Math.atan2(dy, dx), seed)) put(x, y, v);
            }
        }
    }

    private void rect(int v, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) put(x, y, v);
    }

    private void roundRect(int v, int x0, int y0, int x1, int y1, int rad) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int ddx = Math.max(0, Math.max(x0 + rad - x, x - (x1 - rad)));
                int ddy = Math.max(0, Math.max(y0 + rad - y, y - (y1 - rad)));
                if (ddx * ddx + ddy * ddy <= rad * rad) put(x, y, v);
            }
        }
    }

    private boolean isLand(int v) { return v == FLOOR || v == SAND; }
    private boolean isWater(int v) { return v == SHALLOW || v == DEEP; }
    public static boolean walkable(int v) { return v == FLOOR || v == SAND || v == SHALLOW; }

    private void generateLayout() {
        Arrays.fill(type, (byte) ROCK);
        blob(DEEP, 56, 42, 53, 39, 1);                       // seluruh gua: danau dalam

        // jalur air dangkal di atas danau (digambar dulu, daratan menimpanya)
        rect(SHALLOW, 30, 42, 46, 46);
        rect(SHALLOW, 66, 42, 82, 46);

        // Teluk Kedatangan + jalan utara + plaza + kuil
        blob(SAND, 56, 72, 17.5, 10.5, 2);
        blob(FLOOR, 56, 72, 15, 8.5, 3);
        rect(FLOOR, 52, 48, 60, 66);
        blob(FLOOR, 56, 44, 12.5, 10.5, 4);
        rect(FLOOR, 52, 19, 60, 34);
        roundRect(FLOOR, 42, 6, 70, 20, 3);

        // barat
        blob(SAND, 20, 44, 13.5, 11.5, 5);
        blob(FLOOR, 22, 40, 6.5, 4.8, 6);
        blob(SAND, 22, 21, 12.5, 9.5, 8);
        rect(SAND, 18, 29, 23, 34);
        blob(SAND, 20, 68, 13.5, 9.5, 10);
        rect(SAND, 18, 54, 23, 60);
        rect(SAND, 33, 69, 42, 74);

        // timur
        roundRect(FLOOR, 80, 36, 104, 52, 3);
        blob(SAND, 92, 20, 14, 11, 12);
        blob(FLOOR, 92, 20, 12, 9, 11);
        rect(FLOOR, 90, 28, 94, 36);
        blob(SAND, 92, 68, 14.5, 10, 13);
        blob(FLOOR, 92, 68, 8.5, 6, 14);
        rect(FLOOR, 90, 52, 94, 60);
        rect(SAND, 70, 69, 80, 74);

        // batu loncatan di jalur air dangkal
        for (int x = 32; x <= 44; x += 4) rect(FLOOR, x, 43, x + 1, 44);
        for (int x = 68; x <= 80; x += 4) rect(FLOOR, x, 43, x + 1, 44);

        // air di dalam daratan: kolam rumput laut, laguna, genangan pelataran
        blob(SHALLOW, 22, 22, 7, 4.5, 9);
        blob(SHALLOW, 15, 49, 5, 3.5, 7);
        rect(SHALLOW, 83, 42, 101, 46);

        // cincin air dangkal di sekeliling daratan (transisi ke air dalam)
        List<int[]> ring = new ArrayList<>();
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (!isLand(at(x, y))) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        if (dx * dx + dy * dy <= 5 && at(x + dx, y + dy) == DEEP) ring.add(new int[] {x + dx, y + dy});
                    }
                }
            }
        }
        for (int[] p : ring) put(p[0], p[1], SHALLOW);

        // air mancur di depan kuil (air dalam, dikelilingi lantai)
        blob(DEEP, 56, 16, 4.6, 2.5, 15);
    }

    private void deriveTiles() {
        // tebing: batu yang di selatannya ada ruang terbuka menjadi dinding muka
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                if (type[y * COLS + x] == ROCK && y + 1 < ROWS && at(x, y + 1) != ROCK && at(x, y + 1) != FACE) {
                    type[y * COLS + x] = (byte) FACE;
                }
            }
        }
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int i = y * COLS + x;
                int t = type[i];
                switch (t) {
                    case ROCK: {
                        int m = 0;
                        if (open(x, y - 1)) m |= WaterDomainArt.N;
                        if (open(x + 1, y)) m |= WaterDomainArt.E;
                        if (open(x - 1, y)) m |= WaterDomainArt.W;
                        mask[i] = (byte) m;
                        var[i] = (byte) (hash3(x, y, 1) % 4);
                        break;
                    }
                    case FACE: var[i] = (byte) (hash3(x, y, 2) % 3); break;
                    case FLOOR: {
                        int deep = 0, shal = 0;
                        int[][] nb = {{0, -1, WaterDomainArt.N}, {1, 0, WaterDomainArt.E}, {0, 1, WaterDomainArt.S}, {-1, 0, WaterDomainArt.W}};
                        for (int[] n : nb) {
                            int a = at(x + n[0], y + n[1]);
                            if (a == DEEP) deep |= n[2];
                            else if (a == SHALLOW) shal |= n[2];
                        }
                        mask[i] = (byte) deep;
                        mask2[i] = (byte) shal;
                        var[i] = (byte) (hash3(x, y, 3) % 4);
                        break;
                    }
                    case SAND: {
                        int w = 0;
                        int[][] nb = {{0, -1, WaterDomainArt.N}, {1, 0, WaterDomainArt.E}, {0, 1, WaterDomainArt.S}, {-1, 0, WaterDomainArt.W}};
                        for (int[] n : nb) if (isWater(at(x + n[0], y + n[1]))) w |= n[2];
                        mask2[i] = (byte) w;
                        var[i] = (byte) (hash3(x, y, 4) % 3);
                        break;
                    }
                    default: {   // SHALLOW / DEEP: sisi yang berbatasan dengan bukan-air
                        int m = 0;
                        int[][] nb = {{0, -1, WaterDomainArt.N}, {1, 0, WaterDomainArt.E}, {0, 1, WaterDomainArt.S}, {-1, 0, WaterDomainArt.W}};
                        for (int[] n : nb) if (!isWater(at(x + n[0], y + n[1]))) m |= n[2];
                        mask[i] = (byte) m;
                        break;
                    }
                }
            }
        }
        // jarak ke daratan terdekat lewat air (untuk menaruh teratai di tengah danau)
        Arrays.fill(landDist, 99);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < type.length; i++) if (isLand(type[i])) { landDist[i] = 0; q.add(i); }
        while (!q.isEmpty()) {
            int i = q.poll();
            int x = i % COLS, y = i / COLS;
            int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] n : nb) {
                int nx = x + n[0], ny = y + n[1];
                if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS) continue;
                int j = ny * COLS + nx;
                if (landDist[j] != 99 || !isWater(type[j])) continue;
                landDist[j] = landDist[i] + 1;
                q.add(j);
            }
        }
    }

    private boolean open(int x, int y) {
        int v = at(x, y);
        return v != ROCK && v != FACE;
    }

    private static int hash3(int x, int y, int s) {
        return Math.abs(WaterDomainArt.hash(x, y, s));
    }

    // ---- tabrakan ----------------------------------------------------

    private void addBlocker(Rectangle r) {
        int x0 = Math.max(0, Math.floorDiv(r.x, T)), x1 = Math.min(COLS - 1, Math.floorDiv(r.x + r.width - 1, T));
        int y0 = Math.max(0, Math.floorDiv(r.y, T)), y1 = Math.min(ROWS - 1, Math.floorDiv(r.y + r.height - 1, T));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int i = y * COLS + x;
                if (buckets[i] == null) buckets[i] = new ArrayList<>(2);
                buckets[i].add(r);
            }
        }
    }

    /** Kolam keluar: air tidak bisa diinjak. Dibuat dari pita-pita persegi mengikuti bentuk elips air (menyusut 2 piksel seni). */
    private void buildPoolBlockers() {
        for (Rectangle r : WaterPortalArt.blockerStrips(poolX, poolY)) addBlocker(r);
    }

    /** Jarak bertanda (px dunia) dari titik ke tepi air kolam keluar: negatif = di dalam air. */
    public double poolSignedDist(double wx, double wy) {
        return WaterPortalArt.signedDist((wx - poolX) / 3.0, (wy - poolY) / 3.0) * 3.0;
    }

    public int tileAt(int tx, int ty) { return at(tx, ty); }

    /**
     * True kalau hitbox (koordinat dunia domain) nabrak penghalang. Yang dicek cuma separuh bawah hitbox (kaki),
     * sama seperti WalkMask, supaya kepala karakter boleh menimpa dinding tebing di depannya.
     */
    public boolean isBlocked(Rectangle b) {
        int footH = Math.max(1, b.height / 2);
        int top = b.y + b.height - footH, bottom = b.y + b.height;
        int x0 = Math.floorDiv(b.x, T), x1 = Math.floorDiv(b.x + b.width - 1, T);
        int y0 = Math.floorDiv(top, T), y1 = Math.floorDiv(bottom - 1, T);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (!walkable(at(tx, ty))) return true;
            }
        }
        Rectangle foot = new Rectangle(b.x, top, b.width, footH);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                List<Rectangle> l = buckets[ty * COLS + tx];
                if (l == null) continue;
                for (Rectangle r : l) if (foot.intersects(r)) return true;
            }
        }
        return false;
    }

    // ================================================================== properti

    private static final int[] FOOT_PILLAR = WaterDomainArt.pillarFoot();
    private static final int[] FOOT_OBELISK = {-18, -16, 36, 13};
    private static final int[] FOOT_STATUE = {-30, -16, 60, 14};
    private static final int[] FOOT_GATE = {-84, -45, 168, 42};

    private static final class Halo {
        final Sprite s; final int px, py; final double phase;
        Halo(Sprite s, int px, int py, double phase) { this.s = s; this.px = px; this.py = py; this.phase = phase; }
    }

    private static final class Circle {
        final Sprite base, glow; final int px, py;
        Circle(Sprite[] s, int px, int py) { this.base = s[0]; this.glow = s[1]; this.px = px; this.py = py; }
    }

    private static final class Lily {
        final Sprite s; final int px, py; final double phase;
        Lily(Sprite s, int px, int py, double phase) { this.s = s; this.px = px; this.py = py; this.phase = phase; }
    }

    private static final class Fish {
        final double cx, cy, rx, ry, speed, phase;
        final int size;
        Fish(double cx, double cy, double rx, double ry, double speed, double phase, int size) {
            this.cx = cx; this.cy = cy; this.rx = rx; this.ry = ry; this.speed = speed; this.phase = phase; this.size = size;
        }
    }

    /** Properti berdiri (urut Y bersama pemain). */
    private static final class Prop extends Entity {
        static final int SPRITE = 0, KELP = 1;
        final GamePanel g;
        final Sprite spr, glow;
        final int wx, wy, kind;
        final double phase;
        final double glowRate;
        int kelpH;

        Prop(GamePanel g, Sprite spr, Sprite glow, int wx, int wy, int kind, double phase, double glowRate) {
            super(g);
            this.g = g; this.spr = spr; this.glow = glow; this.wx = wx; this.wy = wy; this.kind = kind;
            this.phase = phase; this.glowRate = glowRate;
            this.x = wx; this.y = wy;
        }

        @Override public int getSortY() { return wy; }

        @Override public void draw(Graphics2D g2) {
            int sx = wx - g.player.x + g.player.screenX;
            int sy = wy - g.player.y + g.player.screenY;
            if (kind == KELP) { drawKelp(g2, sx, sy); return; }
            int dx = sx - spr.footX, dy = sy - spr.footY;
            if (dx > g.viewMaxX() || dx + spr.w < g.viewMinX() || dy > g.viewMaxY() || dy + spr.h < g.viewMinY()) return;
            g2.drawImage(spr.img, dx, dy, null);
            if (glow != null) {
                double t = System.nanoTime() / 1e9;
                double pulse = 0.5 + 0.5 * Math.sin(t * glowRate + phase);
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.35 + 0.65 * pulse)));
                g2.drawImage(glow.img, dx, dy, null);
                g2.setComposite(old);
            }
        }

        private void drawKelp(Graphics2D g2, int sx, int sy) {
            if (sx > g.viewMaxX() + 60 || sx < g.viewMinX() - 60 || sy > g.viewMaxY() + 20 || sy < g.viewMinY() - kelpH * 3 - 20) return;
            double t = System.nanoTime() / 1e9;
            int[] cols = {0x1F6B45, 0x2E8F5C, 0x49B873};
            for (int s = 0; s < 3; s++) {
                int bx = sx + (s - 1) * 9;
                int h = kelpH - s * 3 + (s == 1 ? 4 : 0);
                for (int i = 0; i < h; i++) {
                    double tt = i / (double) h;
                    double sway = Math.sin(t * 1.7 + phase + s * 1.9 + i * 0.23) * (0.8 + 3.0 * tt);
                    int x = bx + (int) Math.round(sway) * 3;
                    int y = sy - i * 3;
                    int w = tt < 0.7 ? 6 : 3;
                    g2.setColor(new Color(cols[0]));
                    g2.fillRect(x - w / 2, y - 3, w, 3);
                    g2.setColor(new Color(cols[(i + s) % 7 == 0 ? 2 : 1]));
                    g2.fillRect(x - w / 2, y - 3, Math.max(3, w / 2), 3);
                }
            }
        }
    }

    private Sprite cached(String key, java.util.function.Supplier<Sprite> make) {
        Sprite s = cache.get(key);
        if (s == null) { s = make.get(); cache.put(key, s); }
        return s;
    }

    private Prop addSprite(Sprite s, Sprite glow, double tx, double ty, int[] foot, double glowRate) {
        int wx = (int) Math.round(tx * T), wy = (int) Math.round(ty * T);
        Prop p = new Prop(gp, s, glow, wx, wy, Prop.SPRITE, (wx * 0.013 + wy * 0.007), glowRate);
        props.add(p);
        if (foot != null) addBlocker(new Rectangle(wx + foot[0], wy + foot[1], foot[2], foot[3]));
        return p;
    }

    private void occupy(double tx, double ty, int r) {
        int cx = (int) Math.floor(tx), cy = (int) Math.floor(ty);
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++)
            if (x >= 0 && y >= 0 && x < COLS && y < ROWS) occ[y * COLS + x] = true;
    }

    private void pillarAt(double tx, double ty, boolean broken, int seed) {
        final int sd = seed;
        Sprite s = cached("pil" + broken + (seed % 3), () -> WaterDomainArt.pillar(sd % 3 + 1, broken));
        addSprite(s, null, tx, ty, FOOT_PILLAR, 0);
        occupy(tx, ty, 0);
    }

    private void obeliskAt(double tx, double ty, int seed) {
        final int sd = seed % 3;
        Sprite[] o = {null, null};
        cachedPair("obe" + sd, () -> WaterDomainArt.obelisk(sd), o);
        addSprite(o[0], o[1], tx, ty, FOOT_OBELISK, 1.7);
        halo(WaterDomainArt.AQUA, 8, tx, ty - 0.25, 0.5 * seed);
        occupy(tx, ty, 0);
    }

    private void cachedPair(String key, java.util.function.Supplier<Sprite[]> make, Sprite[] out) {
        Sprite a = cache.get(key + "a"), b = cache.get(key + "b");
        if (a == null) { Sprite[] p = make.get(); a = p[0]; b = p[1]; cache.put(key + "a", a); cache.put(key + "b", b); }
        out[0] = a; out[1] = b;
    }

    private void statueAt(double tx, double ty) {
        Sprite[] o = {null, null};
        cachedPair("statue", () -> WaterDomainArt.statue(5), o);
        addSprite(o[0], o[1], tx, ty, FOOT_STATUE, 1.3);
        occupy(tx, ty, 1);
    }

    private void boulderAt(double tx, double ty, int seed) {
        final int sd = seed;
        int wArt = 18 + (seed % 3) * 4, hArt = 12 + (seed % 3) * 2;
        Sprite s = cached("bld" + (seed % 6), () -> WaterDomainArt.boulder(sd, wArt, hArt));
        int fw = wArt * 3 - 12, fh = hArt * 3 / 2;
        addSprite(s, null, tx, ty, new int[] {-fw / 2, -fh, fw, fh}, 0);
        occupy(tx, ty, 0);
    }

    private void coralAt(double tx, double ty, int kind, int pal, int seed) {
        final int sd = seed % 3;
        Sprite s = cached("cor" + kind + "_" + pal + "_" + sd, () -> WaterDomainArt.coral(kind, pal, sd));
        addSprite(s, null, tx, ty, null, 0);
        occupy(tx, ty, 0);
    }

    private void crystalAt(double tx, double ty, int pal, int seed) {
        final int sd = seed % 4;
        Sprite s = cached("cry" + pal + "_" + sd, () -> WaterDomainArt.crystal(pal, sd));
        addSprite(s, null, tx, ty, null, 0);
        int[] rgb = {0x59F5E3, 0xA98BFF, 0xFF8CC4};
        halo(rgb[pal % 3], 9, tx, ty - 0.3, seed * 0.9);
        occupy(tx, ty, 0);
    }

    private void shellAt(double tx, double ty, int seed) {
        final int sd = seed % 6;
        Sprite s = cached("shl" + sd, () -> WaterDomainArt.shell(sd));
        addSprite(s, null, tx, ty, null, 0);
        occupy(tx, ty, 0);
    }

    private void kelpAt(double tx, double ty, int seed) {
        int wx = (int) Math.round(tx * T), wy = (int) Math.round(ty * T);
        Sprite dummy = cached("shl0", () -> WaterDomainArt.shell(0));
        Prop p = new Prop(gp, dummy, null, wx, wy, Prop.KELP, seed * 1.7, 0);
        p.kelpH = 11 + (seed % 5) * 3;
        props.add(p);
        occupy(tx, ty, 0);
    }

    private void lilyAt(double tx, double ty, int seed) {
        final int sd = seed;
        boolean flower = seed % 4 == 0;
        Sprite s = cached("lil" + (seed % 8) + flower, () -> WaterDomainArt.lilyPad(sd, flower));
        lilies.add(new Lily(s, (int) Math.round(tx * T), (int) Math.round(ty * T), seed * 0.77));
        occupy(tx, ty, 0);
    }

    private void halo(int rgb, int radius, double tx, double ty, double phase) {
        final int fr = rgb, rd = radius;
        Sprite s = cached("halo" + Integer.toHexString(rgb) + radius, () -> WaterDomainArt.glow(fr, rd));
        halos.add(new Halo(s, (int) Math.round(tx * T), (int) Math.round(ty * T), phase));
    }

    private void circleAt(int R, double tx, double ty) {
        circles.add(new Circle(WaterDomainArt.runeCircle(R), (int) Math.round(tx * T), (int) Math.round(ty * T)));
    }

    private void buildProps() {
        Random rnd = new Random(1031L);

        // ---- Teluk Kedatangan: tiang gerbang di mulut jalan utara, kristal di ujung peron
        pillarAt(51.2, 64.8, false, 0);
        pillarAt(61.8, 64.8, false, 1);
        crystalAt(45.5, 76.0, 0, 1);
        crystalAt(66.8, 76.4, 0, 2);
        crystalAt(50.5, 79.0, 1, 3);
        crystalAt(62.0, 79.2, 2, 4);

        // ---- Plaza Pasang-Surut
        circleAt(120, 56.5, 44.5);
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            pillarAt(56.5 + 9.0 * Math.cos(a), 44.5 + 8.0 * Math.sin(a) + 0.4, k % 2 == 1, k);
        }
        obeliskAt(52.0, 41.8, 0);
        obeliskAt(61.0, 41.8, 1);
        obeliskAt(52.0, 47.8, 2);
        obeliskAt(61.0, 47.8, 3);

        // ---- Kuil Pasang-Surut (utara)
        {
            Sprite[] gate = {null, null};
            cachedPair("gate", WaterDomainArt::templeGate, gate);
            addSprite(gate[0], gate[1], 56.5, 8.2, FOOT_GATE, 1.1);
            halo(WaterDomainArt.AQUA, 26, 56.5, 6.0, 0.0);
        }
        statueAt(46.0, 10.8);
        statueAt(67.0, 10.8);
        circleAt(40, 56.5, 10.9);
        pillarAt(45.0, 15.5, false, 0);
        pillarAt(45.0, 19.5, true, 1);
        pillarAt(68.0, 15.5, true, 2);
        pillarAt(68.0, 19.5, false, 0);
        pillarAt(49.5, 7.6, true, 2);
        pillarAt(63.5, 7.6, false, 1);
        crystalAt(50.5, 12.5, 0, 1);
        crystalAt(62.5, 12.5, 0, 2);

        // ---- Pelataran Pilar (timur), digenangi air dangkal di tengah
        for (int i = 0; i < 5; i++) {
            double x = 84.5 + i * 4.0;
            pillarAt(x, 40.2, (i == 1 || i == 3), i);
            pillarAt(x, 49.6, (i == 0 || i == 2), i + 1);
        }
        statueAt(102.2, 44.8);

        // ---- Gua Kristal (timur laut)
        for (int k = 0; k < 15; k++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 0.25 + rnd.nextDouble() * 0.6;
            double x = 92.5 + Math.cos(a) * r * 10.5, y = 20.5 + Math.sin(a) * r * 7.5;
            if (at((int) x, (int) y) == FLOOR && !occ[(int) y * COLS + (int) x]) crystalAt(x, y + 0.5, k % 3 == 2 ? 1 : (k % 7 == 0 ? 2 : 0), k);
        }
        boulderAt(83.0, 22.5, 3);
        boulderAt(101.5, 19.0, 4);
        boulderAt(95.5, 13.0, 5);

        // ---- Taman Terendam (tenggara): lingkaran obelisk
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60 + 15);
            obeliskAt(92.5 + 6.3 * Math.cos(a), 68.8 + 4.2 * Math.sin(a), k);
        }
        circleAt(56, 92.5, 68.7);
        boulderAt(104.0, 70.5, 1);
        boulderAt(80.5, 64.0, 2);

        // ---- Gua Karang (barat): reruntuhan melingkar
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(k * 60 + 30);
            pillarAt(22.5 + 5.0 * Math.cos(a), 40.5 + 3.3 * Math.sin(a) + 0.5, k % 3 != 0, k);
        }
        obeliskAt(22.5, 40.8, 1);
        boulderAt(10.5, 40.0, 0);
        boulderAt(12.0, 52.5, 3);
        boulderAt(30.5, 51.0, 4);

        // ---- Pantai Kerang (barat daya)
        boulderAt(12.0, 64.0, 0);
        boulderAt(14.0, 73.0, 1);
        boulderAt(25.5, 74.0, 2);
        boulderAt(9.5, 69.0, 3);
        boulderAt(27.0, 63.0, 4);
        boulderAt(18.0, 76.0, 5);

        // ---- Taman Rumput Laut (barat laut)
        crystalAt(11.5, 20.5, 0, 1);
        crystalAt(32.5, 20.0, 1, 2);
        crystalAt(18.0, 12.8, 0, 3);
        crystalAt(26.0, 12.5, 2, 0);

        // ---- Sebaran: karang, kerang, rumput laut, teratai, kristal dinding
        for (int y = 1; y < ROWS - 1; y++) {
            for (int x = 1; x < COLS - 1; x++) {
                int i = y * COLS + x;
                if (occ[i]) continue;
                double h1 = WaterDomainArt.rnd01(x, y, 901), h2 = WaterDomainArt.rnd01(x, y, 902), h3 = WaterDomainArt.rnd01(x, y, 903);
                int t = type[i];
                double px = x + 0.2 + 0.6 * h2, py = y + 0.55 + 0.4 * h3;
                if (t == SAND) {
                    boolean nearWater = mask2[i] != 0 || waterNear(x, y);
                    if (nearWater && h1 < 0.20) {
                        coralAt(px, py, (int) (h2 * 4) % 4, (int) (h3 * 4) % 4, hash3(x, y, 7));
                    } else if (h1 > 0.93) {
                        shellAt(px, py, hash3(x, y, 8));
                    } else if (nearWater && h1 > 0.88) {
                        shellAt(px, py, hash3(x, y, 9));
                    }
                } else if (t == SHALLOW) {
                    boolean nearDeep = false;
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (at(x + dx, y + dy) == DEEP) nearDeep = true;
                    if (nearDeep && h1 < 0.07 && landDist[i] <= 3) kelpAt(px, py, hash3(x, y, 10));
                    else if (h1 > 0.985) lilyAt(px, py, hash3(x, y, 11));
                } else if (t == DEEP) {
                    if (landDist[i] >= 3 && h1 < 0.022) lilyAt(px, py, hash3(x, y, 12));
                } else if (t == FLOOR) {
                    boolean nearWall = at(x, y - 1) == FACE || at(x - 1, y) == ROCK || at(x + 1, y) == ROCK;
                    if (nearWall && h1 < 0.035) crystalAt(px, py, h2 < 0.7 ? 0 : 1, hash3(x, y, 13));
                }
            }
        }
        // kolam rumput laut & laguna: rumput laut lebih rapat
        denseKelp(22.5, 22.5, 6.5, 4.0, 14, rnd);
        denseKelp(15.5, 49.5, 4.5, 3.0, 7, rnd);
    }

    private boolean waterNear(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (isWater(at(x + dx, y + dy))) return true;
        return false;
    }

    private void denseKelp(double cx, double cy, double rx, double ry, int n, Random rnd) {
        for (int k = 0; k < n; k++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * 0.85;
            double x = cx + Math.cos(a) * rx * r, y = cy + Math.sin(a) * ry * r;
            if (at((int) x, (int) y) == SHALLOW && !occ[(int) y * COLS + (int) x]) kelpAt(x, y + 0.5, rnd.nextInt(1000));
        }
    }

    // ================================================================== suasana

    private void buildAmbient() {
        Random rnd = new Random(77L);

        // siluet ikan: berputar di area air dalam yang luas
        for (int tries = 0; tries < 900 && fish.size() < 34; tries++) {
            int x = 4 + rnd.nextInt(COLS - 8), y = 4 + rnd.nextInt(ROWS - 8);
            if (!allWater(x - 3, y - 2, x + 3, y + 2, DEEP)) continue;
            double rx = 1.4 + rnd.nextDouble() * 1.5, ry = 0.7 + rnd.nextDouble() * 0.9;
            fish.add(new Fish((x + 0.5) * T, (y + 0.5) * T, rx * T, ry * T,
                (0.35 + rnd.nextDouble() * 0.45) * (rnd.nextBoolean() ? 1 : -1), rnd.nextDouble() * Math.PI * 2, 1 + rnd.nextInt(2)));
        }
        // gelembung
        for (int tries = 0; tries < 1200 && bubbleSpots.size() < 90; tries++) {
            int x = 2 + rnd.nextInt(COLS - 4), y = 2 + rnd.nextInt(ROWS - 4);
            if (at(x, y) == DEEP) bubbleSpots.add(new int[] {x * T + rnd.nextInt(T), y * T + rnd.nextInt(T), rnd.nextInt(1000)});
        }
        // ikan kuning yang meloncat (aset yang sama dengan danau dunia luar): butuh air dalam 7 x 5 tile
        for (int y = 5; y < ROWS - 3 && jumpFishSpots.size() < 12; y += 2) {
            for (int x = 6; x < COLS - 6 && jumpFishSpots.size() < 12; x += 2) {
                if (!allWater(x - 3, y - 3, x + 3, y + 1, DEEP)) continue;
                boolean far = true;
                for (int[] s : jumpFishSpots) if (Math.abs(s[0] / T - x) < 12 && Math.abs(s[1] / T - y) < 9) { far = false; break; }
                if (far) jumpFishSpots.add(new int[] {x * T + T / 2, y * T + T / 2, jumpFishSpots.size() % 2});
            }
        }
        jumpFish = new FishJumpManager(getClass(), jumpFishSpots.toArray(new int[0][]));

        // vinyet gelap kebiruan di tepi layar
        int w = gp.screenWidth, h = gp.screenHeight;
        vignette = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] data = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double nx = (x - w / 2.0) / (w / 2.0), ny = (y - h / 2.0) / (h / 2.0);
                double d = Math.sqrt(nx * nx * 0.8 + ny * ny);
                double a = Math.max(0, Math.min(1, (d - 0.55) / 0.85));
                int alpha = (int) Math.round(a * a * 150);
                data[y * w + x] = (alpha << 24) | 0x040C18;
            }
        }
        vignette.setRGB(0, 0, w, h, data, 0, w);
    }

    private boolean allWater(int x0, int y0, int x1, int y1, int kind) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) if (at(x, y) != kind) return false;
        return true;
    }

    // ================================================================== gambar

    private static final Color DEEP_TINT = new Color(0x06, 0x1E, 0x3A);
    private static final Color SHALLOW_TINT = new Color(0xC4, 0xF4, 0xFF);

    /** Lantai, air bergelombang, tepian, lalu dekorasi rata (lingkaran rune, teratai, pendar, ikan, gelembung). */
    public void drawGround(Graphics2D g2, WaterAnimator wa) {
        int camX = gp.player.x, camY = gp.player.y, psx = gp.player.screenX, psy = gp.player.screenY;
        int left = camX - psx, top = camY - psy;
        int vminX = gp.viewMinX(), vmaxX = gp.viewMaxX(), vminY = gp.viewMinY(), vmaxY = gp.viewMaxY();
        int c0 = Math.max(0, Math.floorDiv(left + vminX, T) - 1), c1 = Math.min(COLS - 1, Math.floorDiv(left + vmaxX, T) + 1);
        int r0 = Math.max(0, Math.floorDiv(top + vminY, T) - 1), r1 = Math.min(ROWS - 1, Math.floorDiv(top + vmaxY, T) + 1);
        double t = System.nanoTime() / 1e9;
        int foamFrame = (int) ((System.nanoTime() / 1_000_000L / 160) % WaterDomainArt.FOAM_FRAMES);
        if (wa != null) wa.beginFrame();

        int nd = 0, ns = 0;
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int i = r * COLS + c;
                int sx = c * T - left, sy = r * T - top;
                switch (type[i]) {
                    case ROCK:
                        g2.drawImage(tiles.rock[var[i]], sx, sy, null);
                        if (mask[i] != 0) g2.drawImage(tiles.rockEdge[mask[i]], sx, sy, null);
                        break;
                    case FACE:
                        g2.drawImage(tiles.face[var[i]], sx, sy, null);
                        break;
                    case FLOOR:
                        g2.drawImage(tiles.floor[var[i]], sx, sy, null);
                        if (mask2[i] != 0) g2.drawImage(tiles.floorEdge[mask2[i]], sx, sy, null);
                        if (mask[i] != 0) g2.drawImage(tiles.floorLip[mask[i]], sx, sy, null);
                        break;
                    case SAND:
                        g2.drawImage(tiles.sand[var[i]], sx, sy, null);
                        if (mask2[i] != 0) g2.drawImage(tiles.sandWet[mask2[i]], sx, sy, null);
                        break;
                    case SHALLOW:
                        if (wa != null) wa.drawTile(g2, 1, c, r, sx, sy, T);
                        shalSX[ns] = sx; shalSY[ns] = sy; shalI[ns++] = i;
                        break;
                    default:
                        if (wa != null) wa.drawTile(g2, 1, c, r, sx, sy, T);
                        deepSX[nd] = sx; deepSY[nd] = sy; deepI[nd++] = i;
                        break;
                }
            }
        }
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.44f));
        g2.setColor(DEEP_TINT);
        for (int k = 0; k < nd; k++) g2.fillRect(deepSX[k], deepSY[k], T, T);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.17f));
        g2.setColor(SHALLOW_TINT);
        for (int k = 0; k < ns; k++) g2.fillRect(shalSX[k], shalSY[k], T, T);
        g2.setComposite(old);
        for (int k = 0; k < nd; k++) if (mask[deepI[k]] != 0) g2.drawImage(tiles.foam[mask[deepI[k]]][foamFrame], deepSX[k], deepSY[k], null);
        for (int k = 0; k < ns; k++) if (mask[shalI[k]] != 0) g2.drawImage(tiles.foam[mask[shalI[k]]][foamFrame], shalSX[k], shalSY[k], null);

        // ---- dekorasi rata
        for (Circle ci : circles) {
            int sx = ci.px - ci.base.footX - left, sy = ci.py - ci.base.footY - top;
            if (sx > vmaxX || sx + ci.base.w < vminX || sy > vmaxY || sy + ci.base.h < vminY) continue;
            g2.drawImage(ci.base.img, sx, sy, null);
            double pulse = 0.5 + 0.5 * Math.sin(t * 1.1 + ci.px * 0.001);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.15 + 0.7 * pulse * pulse)));
            g2.drawImage(ci.glow.img, sx, sy, null);
            g2.setComposite(old);
        }
        for (Halo h : halos) {
            int sx = h.px - h.s.footX - left, sy = h.py - h.s.footY - top;
            if (sx > vmaxX || sx + h.s.w < vminX || sy > vmaxY || sy + h.s.h < vminY) continue;
            double pulse = 0.5 + 0.5 * Math.sin(t * 1.6 + h.phase);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.45 + 0.4 * pulse)));
            g2.drawImage(h.s.img, sx, sy, null);
        }
        g2.setComposite(old);
        for (Lily l : lilies) {
            int sx = l.px - l.s.footX - left, sy = l.py - l.s.footY - top + (int) Math.round(Math.sin(t * 1.3 + l.phase) * 1.4);
            if (sx > vmaxX || sx + l.s.w < vminX || sy > vmaxY || sy + l.s.h < vminY) continue;
            g2.drawImage(l.s.img, sx, sy, null);
        }

        // ---- siluet ikan di bawah permukaan
        for (Fish f : fish) {
            double a = f.speed * t + f.phase;
            double wx = f.cx + Math.cos(a) * f.rx, wy = f.cy + Math.sin(a) * f.ry;
            int sx = (int) Math.round(wx) - left, sy = (int) Math.round(wy) - top;
            if (sx > vmaxX + 20 || sx < vminX - 20 || sy > vmaxY + 20 || sy < vminY - 20) continue;
            boolean right = (-Math.sin(a) * f.speed) > 0;
            int dir = right ? 1 : -1;
            int u = 3 * f.size;
            g2.setColor(new Color(0x04, 0x22, 0x3C, 120));
            g2.fillRect(sx - 3 * u / 3, sy - u / 2, 4 * u, u);
            g2.fillRect(sx + dir * 2 * u - (dir < 0 ? u : 0), sy - u, u, 2 * u);
            int wag = (int) Math.round(Math.sin(t * 7 + f.phase) * u / 2.0);
            g2.fillRect(sx - dir * 2 * u - (dir > 0 ? u : 0), sy - u / 2 + wag - u / 2, u, 2 * u);
            g2.setColor(new Color(0x8A, 0xE8, 0xFF, 70));
            g2.fillRect(sx + dir * u, sy - u / 2, u, u / 2 + 1);
        }

        // ---- gelembung
        for (int[] b : bubbleSpots) {
            int sx = b[0] - left, sy = b[1] - top;
            if (sx > vmaxX + 10 || sx < vminX - 10 || sy > vmaxY + 70 || sy < vminY - 10) continue;
            for (int k = 0; k < 3; k++) {
                double p = (t * 0.45 + b[2] * 0.37 + k / 3.0) % 1.0;
                int x = sx + (int) Math.round(Math.sin(p * 9 + b[2]) * 4);
                int y = sy - (int) Math.round(p * 70);
                int alpha = (int) Math.round(Math.sin(Math.PI * p) * 170);
                int sz = k == 0 ? 6 : 3;
                g2.setColor(new Color(0xD6, 0xF6, 0xFF, Math.max(0, Math.min(255, alpha))));
                g2.fillRect(x, y, sz, sz);
                if (sz == 6) { g2.setColor(new Color(0x4F, 0xA8, 0xD6, Math.max(0, Math.min(255, alpha / 2)))); g2.fillRect(x + 3, y + 3, 3, 3); }
            }
        }

        // ---- ikan kuning loncat
        if (jumpFish != null) jumpFish.draw(g2, camX, camY, psx, psy, vminX, vminY, vmaxX, vmaxY);

        // ---- riak di kaki saat berdiri di air dangkal
        int ftx = Math.floorDiv(camX + 24, T), fty = Math.floorDiv(camY + 45, T);
        if (!gp.player.scripted && at(ftx, fty) == SHALLOW) {
            g2.setStroke(new BasicStroke(3));
            for (int k = 0; k < 2; k++) {
                double p = (t * 0.9 + k * 0.5) % 1.0;
                int rad = (int) Math.round(7 + 20 * p);
                g2.setColor(new Color(0xD6, 0xF6, 0xFF, (int) Math.round((1 - p) * 140)));
                g2.drawOval(psx + 24 - rad, psy + 45 - (int) (rad * 0.42), rad * 2, (int) (rad * 0.84));
            }
            g2.setStroke(new BasicStroke(1));
        }
    }

    /** Masukkan properti yang kelihatan ke daftar urut-Y milik GamePanel. */
    public void collectProps(List<Entity> out) {
        int camX = gp.player.x, camY = gp.player.y, psx = gp.player.screenX, psy = gp.player.screenY;
        int minX = gp.viewMinX() - 260, maxX = gp.viewMaxX() + 260, minY = gp.viewMinY() - 120, maxY = gp.viewMaxY() + 320;
        for (Prop p : props) {
            int sx = p.wx - camX + psx, sy = p.wy - camY + psy;
            if (sx < minX || sx > maxX || sy < minY || sy > maxY) continue;
            out.add(p);
        }
    }

    /** Cahaya dari atas, partikel pendar dan vinyet. Digambar di koordinat layar (tidak ikut zoom). */
    public void drawScreenFx(Graphics2D g2) {
        int w = gp.screenWidth, h = gp.screenHeight;
        double t = System.nanoTime() / 1e9;

        // tiang cahaya dari celah di langit-langit
        for (int k = 0; k < 5; k++) {
            double x0 = ((k * 0.23 + 0.06 + 0.012 * Math.sin(t * 0.25 + k)) % 1.0) * (w + 200) - 160;
            int bw = 70 + (k * 37) % 60;
            int skew = (int) (h * 0.55);
            float a = (float) (0.075 + 0.045 * Math.sin(t * 0.45 + k * 1.7));
            Polygon poly = new Polygon(
                new int[] {(int) x0, (int) x0 + bw, (int) x0 + bw + skew, (int) x0 + skew},
                new int[] {0, 0, h, h}, 4);
            g2.setPaint(new GradientPaint(0, 0, new Color(0.62f, 0.95f, 1f, Math.max(0f, a)), 0, h * 0.9f, new Color(0.62f, 0.95f, 1f, 0f)));
            g2.fill(poly);
        }
        g2.setPaint(null);

        // partikel pendar yang melayang pelan (parallax terhadap kamera)
        int camX = gp.player.x, camY = gp.player.y;
        for (int i = 0; i < 38; i++) {
            double bx = WaterDomainArt.rnd01(i, 1, 55) * (w + 80), by = WaterDomainArt.rnd01(i, 2, 55) * (h + 80);
            double sx = ((bx + t * (6 + (i % 5) * 3) - camX * 0.7) % (w + 80) + (w + 80)) % (w + 80) - 40;
            double sy = ((by - t * (8 + (i % 4) * 4) - camY * 0.7) % (h + 80) + (h + 80)) % (h + 80) - 40;
            double pulse = 0.5 + 0.5 * Math.sin(t * (0.8 + (i % 4) * 0.4) + i);
            int alpha = (int) Math.round(40 + 150 * pulse);
            g2.setColor(i % 3 == 0 ? new Color(0xFF, 0xFF, 0xFF, alpha) : new Color(0x7D, 0xFC, 0xF0, alpha));
            int sz = (i % 4 == 0) ? 6 : 3;
            g2.fillRect((int) sx, (int) sy, sz, sz);
        }
        g2.drawImage(vignette, 0, 0, null);
    }

    // ================================================================== untuk pengujian / tooling

    /** Salinan tipe tile per sel (indeks y * COLS + x), dipakai alat uji untuk mengecek keterhubungan jalur. */
    public byte[] debugTypes() { return type.clone(); }

    public int propCount() { return props.size(); }
}
