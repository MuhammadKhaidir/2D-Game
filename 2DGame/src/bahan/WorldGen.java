package bahan;

import java.util.ArrayList;
import java.util.List;

/**
 * Generator dunia (murni fungsi dari koordinat + SEED, tidak menyimpan apa-apa).
 *
 * Peta lama (map01.txt, "core") tetap seperti aslinya. Generator ini cuma mengurus:
 *  - area BARU di luar peta lama (timur & selatan), termasuk laut pembatas di tepi dunia,
 *  - biome es: seberapa "bersalju" suatu titik (ice()), danau/sungai beku (depth()),
 *  - sebaran pohon / batu / kristal / semak per chunk (populate()).
 *
 * Karena semuanya murni fungsi, sebuah chunk bisa dibuang lalu dibuat lagi dan hasilnya sama persis,
 * dan hasilnya tidak tergantung urutan chunk dimuat. Koordinat di sini satuan TILE (boleh pecahan).
 */
public final class WorldGen {

    // ------------------------------------------------------------------ ukuran dunia (tile)
    /** Ukuran dunia setelah diperluas. Peta lama (243 x 321) ada di pojok kiri-atas. */
    public static final int WORLD_COLS = 340;
    public static final int WORLD_ROWS = 460;
    /** Lebar pita laut di tepi timur / selatan / utara area baru. */
    public static final int BORDER = 10;
    /** Baris pertama daratan di area utara-baru (timur peta lama). Sambung dengan garis pantai peta lama. */
    public static final int NORTH_COAST_ROW = 7;

    public static final long SEED = 20261026L;
    private static final int S = (int) (SEED ^ (SEED >>> 32));

    // ------------------------------------------------------------------ tile index
    /**
     * Varian rumput untuk area baru (dan pengganti tile salju lama). Gambarnya sama dengan tile 4 / 6 / 10 tapi
     * TIDAK ditandai collision (rumput lama 0/4/6/10 ditandai collision, dan itu mengganggu pathfinding monster).
     */
    public static final int[] GRASS = {76, 77, 78};
    /** Tile tepi pantai (air + daratan di sisinya). Versi saljunya = 140 + posisi di array ini. */
    public static final int[] SHORE_BASE = {14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 57, 59};
    public static final int SNOW_SHORE_FIRST = 140;
    /** Tile "permukaan" untuk minimap / pewarnaan rata-rata: salju dan es. */
    public static final int TILE_SNOW = 100;
    public static final int TILE_ICE = 131;

    // ------------------------------------------------------------------ jenis objek hasil populate()
    public static final int K_OAK = 0;          // pohon biasa (pohon tebang, rangka animasi yang sama dengan peta lama)
    public static final int K_OAK_FROST = 1;    // pohon biasa bersalju tipis
    public static final int K_PINE = 2;         // cemara bersalju
    public static final int K_DEAD = 3;         // pohon mati / beku
    public static final int K_ICE_ROCK = 4;     // batu es (bisa dihancurkan)
    public static final int K_CRYSTAL = 5;      // kristal es (bisa dihancurkan)
    public static final int K_ROCK = 6;         // batu biasa (hutan)
    public static final int K_BUSH = 7;         // semak hijau (hiasan)
    public static final int K_BUSH_SNOW = 8;    // semak bersalju (hiasan)
    public static final int K_GLOW = 9;         // cahaya lembut di bawah kristal (hiasan datar)
    public static final int K_TUFT = 10;        // rumpun rumput kering yang menyembul dari salju (hiasan)
    public static final int K_PEBBLE = 11;      // kerikil bersalju (hiasan)
    public static final int K_SHARD = 12;       // pecahan es kecil (hiasan)
    public static final int K_DRIFT = 13;       // gundukan salju rendah (hiasan)

    /** Satu objek hasil populate(): posisi kiri-atas dalam piksel dunia. */
    public static final class Spawn {
        public final int kind, x, y, w, h, hp, variant;
        Spawn(int kind, int x, int y, int w, int h, int hp, int variant) {
            this.kind = kind; this.x = x; this.y = y; this.w = w; this.h = h; this.hp = hp; this.variant = variant;
        }
    }

    // ------------------------------------------------------------------ parameter biome es
    /** Lebar pinggiran salju yang memudar ke hutan (tile). */
    private static final float FADE = 30f;
    private static final float WARP_AMP = 34f;
    private static final float CORNER_R = 22f;
    /** Salju dianggap "milik generator" (objek lama di sini dibuang) kalau ice() di atas ini. */
    public static final float OWN_ICE = 0.62f;
    /** Pohon lama dengan ice() di atas ini diberi salju tipis. */
    public static final float FROST_ICE = 0.20f;

    private final int[][] core;
    private final int coreCols, coreRows;

    /**
     * @param core tile peta lama [kolom][baris] (sesudah dibereskan, lihat TileM)
     */
    public WorldGen(int[][] core, int coreCols, int coreRows) {
        this.core = core;
        this.coreCols = coreCols;
        this.coreRows = coreRows;
    }

    public int coreCols() { return coreCols; }
    public int coreRows() { return coreRows; }

    // ================================================================== noise

    static int h2(int x, int y, int s) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ s * 0x9E3779B1;
        h ^= h >>> 15; h *= 0x85ebca6b;
        h ^= h >>> 13; h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    /** angka acak 0..1 untuk satu titik kisi */
    static float rnd(int x, int y, int s) {
        return (h2(x, y, s) >>> 8) / 16777216f;
    }

    static float vnoise(float x, float y, int s) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = x - xi, fy = y - yi;
        float u = fx * fx * fx * (fx * (fx * 6f - 15f) + 10f);
        float v = fy * fy * fy * (fy * (fy * 6f - 15f) + 10f);
        float a = rnd(xi, yi, s), b = rnd(xi + 1, yi, s), c = rnd(xi, yi + 1, s), d = rnd(xi + 1, yi + 1, s);
        float top = a + (b - a) * u, bot = c + (d - c) * u;
        return top + (bot - top) * v;
    }

    static float fbm(float x, float y, int s, int oct) {
        float sum = 0f, amp = 0.5f, norm = 0f;
        for (int i = 0; i < oct; i++) {
            sum += amp * vnoise(x, y, s + i * 101);
            norm += amp;
            x = x * 2.03f + 17.1f;
            y = y * 2.03f + 9.7f;
            amp *= 0.5f;
        }
        return sum / norm;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    static float smooth(float t) { t = clamp01(t); return t * t * (3f - 2f * t); }

    private static float sdRoundBox(float x, float y, float x0, float y0, float x1, float y1, float r) {
        float cx = (x0 + x1) * 0.5f, cy = (y0 + y1) * 0.5f;
        float hx = (x1 - x0) * 0.5f - r, hy = (y1 - y0) * 0.5f - r;
        float qx = Math.abs(x - cx) - hx, qy = Math.abs(y - cy) - hy;
        float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
        return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, qy), 0f) - r;
    }

    // ================================================================== biome es

    /**
     * Seberapa bersalju titik (tx, ty) (satuan tile): 0 = hutan biasa, 1 = es penuh.
     * Bentuknya dua kotak membulat (pojok lama + daratan es di selatan) yang tepinya digoyang noise,
     * lalu memudar halus selebar FADE tile, jadi tidak ada garis batas yang tajam.
     */
    public float ice(float tx, float ty) {
        float wx = tx + WARP_AMP * (fbm(tx * 0.017f, ty * 0.017f, S + 1, 3) - 0.5f) * 2f;
        float wy = ty + WARP_AMP * (fbm(tx * 0.017f + 41.3f, ty * 0.017f + 7.9f, S + 2, 3) - 0.5f) * 2f;
        float d1 = sdRoundBox(wx, wy, -80f, 218f, 98f, 365f, CORNER_R);   // pojok lama (diperlebar ke timur)
        float d2 = sdRoundBox(wx, wy, -80f, 345f, 178f, 640f, CORNER_R);  // dataran es selatan
        float d = Math.min(d1, d2);
        return smooth((FADE - d) / FADE);
    }

    /**
     * Kedalaman es beku (danau + sungai): > 0 berarti di dalam danau/sungai beku, makin besar makin ke tengah.
     * Cuma bermakna di tengah biome es; di pinggiran dikecilkan supaya danau tidak terpotong tiba-tiba.
     */
    public float depth(float tx, float ty, float iceHere) {
        float lake = fbm(tx * 0.050f + 100f, ty * 0.050f + 100f, S + 11, 4);
        float lakeD = (lake - 0.605f) * 7f;
        float rv = fbm(tx * 0.027f + 300f, ty * 0.027f + 300f, S + 12, 3);
        float riverD = (0.020f - Math.abs(rv - 0.5f)) * 17f;
        float d = Math.max(lakeD, riverD);
        return d - 6f * Math.max(0f, 0.93f - iceHere);
    }

    public float depth(float tx, float ty) {
        return depth(tx, ty, ice(tx, ty));
    }

    // ================================================================== daratan / laut

    public boolean inCore(int c, int r) {
        return c >= 0 && r >= 0 && c < coreCols && r < coreRows;
    }

    /** true kalau sel (c, r) daratan (bukan air). Dipakai buat memilih tile tepi pantai. */
    public boolean isLand(int c, int r) {
        if (inCore(c, r)) return !WaterCollision.isWaterTile(core[c][r]);
        if (c < 0 || r < 0) return true;                          // di luar kiri/atas: tidak pernah terlihat
        if (c >= WORLD_COLS - BORDER || r >= WORLD_ROWS - BORDER) return false;
        if (c >= coreCols && r < NORTH_COAST_ROW) return false;   // laut utara di timur peta lama
        return true;
    }

    /** Tile dasar sel (c, r): peta lama kalau di dalam core, selain itu hasil generator. */
    public int baseTile(int c, int r) {
        return inCore(c, r) ? core[c][r] : groundTile(c, r);
    }

    /** Tile yang boleh ditutupi salju (rumput biasa). Jalan, air, rumah, dll tidak ikut tertutup. */
    public static boolean isGrassLike(int t) {
        return t == 0 || t == 4 || t == 6 || t == 10 || t == 62 || t == 67 || t == 68 || (t >= 76 && t <= 78);
    }

    /** true kalau sel ini bagian dari laut pembatas (area baru saja). */
    public boolean isSea(int c, int r) {
        return !inCore(c, r) && !isLand(c, r);
    }

    /** Tile dasar sel di luar peta lama: rumput (varian acak) atau air + tepi pantainya. */
    public int groundTile(int c, int r) {
        if (c >= WORLD_COLS || r >= WORLD_ROWS) return 2;
        if (isLand(c, r)) return GRASS[(h2(c, r, 7) >>> 4) % 3];
        return waterTile(c, r);
    }

    private int waterTile(int c, int r) {
        boolean n = isLand(c, r - 1), s = isLand(c, r + 1), w = isLand(c - 1, r), e = isLand(c + 1, r);
        int cnt = (n ? 1 : 0) + (s ? 1 : 0) + (w ? 1 : 0) + (e ? 1 : 0);
        int t;
        if (cnt == 0) {
            if (isLand(c + 1, r - 1)) t = 16;
            else if (isLand(c - 1, r - 1)) t = 20;
            else if (isLand(c - 1, r + 1)) t = 22;
            else if (isLand(c + 1, r + 1)) t = 23;
            else t = (((h2(c, r, 9) >>> 5) % 100) < 6) ? 1 : 2;
        } else if (cnt == 1) {
            t = n ? 18 : (s ? 17 : (w ? 21 : 14));
        } else if (cnt == 2 && n && e) t = 15;
        else if (cnt == 2 && n && w) t = 19;
        else if (cnt == 2 && s && e) t = 57;
        else if (cnt == 2 && s && w) t = 59;
        else t = n ? 18 : (s ? 17 : (w ? 21 : 14));     // kombinasi yang tidak ada tile-nya: ambil satu sisi
        return coldShore(c, r, t);
    }

    /** Di biome es, tepi pantai memakai tile versi salju (darat di tepinya putih, bukan hijau). */
    private int coldShore(int c, int r, int t) {
        if (t == 1 || t == 2) return t;
        if (ice(c + 0.5f, r + 0.5f) <= 0.5f) return t;
        for (int i = 0; i < SHORE_BASE.length; i++) if (SHORE_BASE[i] == t) return SNOW_SHORE_FIRST + i;
        return t;
    }

    /** Tile semantik sel (c, r) untuk minimap: rumput / air / salju / es. Tidak tergantung chunk sudah dimuat atau belum. */
    public int surfaceTile(int c, int r) {
        int base = inCore(c, r) ? core[c][r] : groundTile(c, r);
        if (WaterCollision.isWaterTile(base) || base >= SNOW_SHORE_FIRST) return base;
        boolean grassLike = isGrassLike(base);
        if (!grassLike) return base;
        float i = ice(c + 0.5f, r + 0.5f);
        if (i > 0.5f) return depth(c + 0.5f, r + 0.5f, i) > 0f && i > 0.82f ? TILE_ICE : TILE_SNOW;
        return (base == 67 || base == 68) ? GRASS[(h2(c, r, 7) >>> 4) % 3] : base;
    }

    // ================================================================== sebaran objek

    private static final int SLOT = 4;   // satu objek paling banyak per kotak 4 x 4 tile

    /**
     * Objek di satu chunk (kolom col0.., baris row0.., ukuran size tile).
     * Kotak-kotak 4x4 tile dibagi global (bukan per chunk), jadi hasilnya sama di mana pun batas chunk-nya.
     */
    public List<Spawn> populate(int col0, int row0, int size, int tileSize) {
        List<Spawn> out = new ArrayList<>();
        int sx0 = Math.floorDiv(col0, SLOT), sy0 = Math.floorDiv(row0, SLOT);
        int sx1 = Math.floorDiv(col0 + size - 1, SLOT), sy1 = Math.floorDiv(row0 + size - 1, SLOT);
        for (int sy = sy0; sy <= sy1; sy++) {
            for (int sx = sx0; sx <= sx1; sx++) {
                float fx = (sx * SLOT + 0.5f + rnd(sx, sy, S + 21) * (SLOT - 1f));
                float fy = (sy * SLOT + 0.5f + rnd(sx, sy, S + 22) * (SLOT - 1f));
                int c = (int) fx, r = (int) fy;
                if (c < col0 || c >= col0 + size || r < row0 || r >= row0 + size) continue;   // dimiliki chunk tetangga
                placeSlot(out, sx, sy, fx, fy, c, r, tileSize);
            }
        }
        return out;
    }

    private boolean buildableLand(int c, int r) {
        if (c < 3 || r < 3) return false;
        if (c >= WORLD_COLS - BORDER - 2 || r >= WORLD_ROWS - BORDER - 2) return false;
        if (c >= coreCols && r < NORTH_COAST_ROW + 2) return false;
        // sel di sekitar harus darat (jangan numpuk di pinggir laut)
        return isLand(c, r) && isLand(c - 1, r) && isLand(c + 1, r) && isLand(c, r - 1) && isLand(c, r + 1);
    }

    private void placeSlot(List<Spawn> out, int sx, int sy, float fx, float fy, int c, int r, int ts) {
        if (!buildableLand(c, r)) return;
        float ice = ice(fx, fy);
        if (inCore(c, r)) {
            // peta lama: di luar area es objeknya milik peta lama; di dalam area es, jalan tanah / jembatan dll dibiarkan
            if (ice <= OWN_ICE) return;
            int t = core[c][r];
            if (!isGrassLike(t) || t == 62) return;
        }
        float dep = depth(fx, fy, ice);
        if (ice > 0.82f && dep > -0.35f) return;            // jangan di atas / dekat danau & sungai beku

        float roll = rnd(sx, sy, S + 23);
        float roll2 = rnd(sx, sy, S + 24);
        float roll3 = rnd(sx, sy, S + 25);
        int px = (int) (fx * ts), py = (int) (fy * ts);

        float grove = fbm(fx * 0.06f, fy * 0.06f, S + 31, 3);

        // ---- kristal: bergerombol di "ladang kristal" (cuma di tengah biome es)
        if (ice > 0.9f) {
            float cf = fbm(fx * 0.035f + 55f, fy * 0.035f + 55f, S + 32, 3);
            if (cf > 0.60f && roll < 0.38f) {
                int n = 1 + (int) (roll2 * 3.2f);                    // 1..4 kristal
                for (int i = 0; i < n; i++) {
                    float a = (float) (i * 2.4 + roll3 * 6.0);
                    int w = 78 + (int) (rnd(sx + i, sy, S + 33) * 52);
                    int cx = px + (int) (Math.cos(a) * (i == 0 ? 0 : 54));
                    int cy = py + (int) (Math.sin(a) * (i == 0 ? 0 : 36));
                    int v = (int) (rnd(sx, sy + i, S + 34) * 3f);
                    out.add(new Spawn(K_GLOW, cx - 60, cy - 36, 160, 110, 0, 0));
                    out.add(new Spawn(K_CRYSTAL, cx - w / 2, cy - w + 20, w, (int) (w * 1.25f), 700, v));
                }
                return;
            }
        }

        // ---- batu
        float rockP = 0.028f + 0.060f * smoothstep(0.58f, 0.74f, fbm(fx * 0.05f + 9f, fy * 0.05f + 9f, S + 35, 2));
        if (roll < rockP) {
            int w = 92 + (int) (roll2 * 46);
            if (ice > 0.45f) out.add(new Spawn(K_ICE_ROCK, px - w / 2, py - (int) (w * 0.85f) / 2, w, (int) (w * 0.846f), 900, (int) (roll3 * 3f)));
            else out.add(new Spawn(K_ROCK, px - 60, py - 60, 120, 120, 800, 0));
            return;
        }

        // ---- pohon: rapat di rimba, jarang di padang; di es lebih jarang lagi (tundra luas)
        float forestP = 0.15f + 0.50f * smoothstep(0.40f, 0.64f, grove);
        float iceP = 0.13f + 0.50f * smoothstep(0.46f, 0.68f, grove);
        float treeP = forestP + (iceP - forestP) * smooth((ice - 0.25f) / 0.5f);
        if (roll < rockP + treeP) {
            int w = 170 + (int) (roll2 * 30);
            int hp = roll3 < 0.2f ? 800 : 500;
            if (ice < 0.12f) {
                out.add(new Spawn(K_OAK, px - w / 2, py - w + 40, w, w, hp, 0));
            } else if (ice < 0.55f) {
                float pFrost = smooth((ice - 0.12f) / 0.30f);
                float k = rnd(sx, sy, S + 26);
                if (ice > 0.30f && k < 0.30f * smooth((ice - 0.30f) / 0.25f)) addPine(out, px, py, roll2, roll3);
                else if (k < 0.30f + 0.70f * pFrost) out.add(new Spawn(K_OAK_FROST, px - w / 2, py - w + 40, w, w, hp, 0));
                else out.add(new Spawn(K_OAK, px - w / 2, py - w + 40, w, w, hp, 0));
            } else {
                float k = rnd(sx, sy, S + 27);
                float nearLake = ice > 0.8f && dep > -1.2f ? 0.18f : 0f;
                if (k < 0.12f + nearLake) {
                    int dw = 140 + (int) (roll2 * 40);
                    int dh = (int) (dw * 1.238f);
                    out.add(new Spawn(K_DEAD, px - dw / 2, py - dh + 38, dw, dh, 400, (int) (roll3 * 2f)));
                } else if (k < 0.20f + nearLake && ice < 0.75f) {
                    out.add(new Spawn(K_OAK_FROST, px - w / 2, py - w + 40, w, w, hp, 0));
                } else addPine(out, px, py, roll2, roll3);
            }
            return;
        }

        // ---- semak (hiasan, tanpa collision)
        float bushP = 0.10f + 0.12f * smooth((ice - 0.4f) / 0.4f);
        if (roll < rockP + treeP + bushP) {
            int w = 82 + (int) (roll2 * 26);
            out.add(new Spawn(ice > 0.4f ? K_BUSH_SNOW : K_BUSH, px - w / 2, py - w / 2, w, w, 0, 0));
            return;
        }

        // ---- hiasan tanah di biome es: gundukan salju, kerikil, pecahan es, rumput kering
        if (ice > 0.45f) {
            float dRoll = rnd(sx, sy, S + 28);
            if (dRoll < 0.34f * smooth((ice - 0.45f) / 0.25f)) {
                float pick = rnd(sx, sy, S + 29);
                int v = (int) (roll3 * 2f) & 1;
                if (pick < 0.38f) { int w = 96 + (int) (roll2 * 50); out.add(new Spawn(K_DRIFT, px - w / 2, py - w / 4, w, (int) (w * 0.5f), 0, v)); }
                else if (pick < 0.62f) { int w = 46 + (int) (roll2 * 22); out.add(new Spawn(K_PEBBLE, px - w / 2, py - (int) (w * 0.65f) / 2, w, (int) (w * 0.65f), 0, v)); }
                else if (pick < 0.80f) { int w = 44 + (int) (roll2 * 26); out.add(new Spawn(K_SHARD, px - w / 2, py - (int) (w * 1.22f) + 12, w, (int) (w * 1.22f), 0, v)); }
                else { int w = 48 + (int) (roll2 * 24); out.add(new Spawn(K_TUFT, px - w / 2, py - (int) (w * 0.94f) + 10, w, (int) (w * 0.94f), 0, v)); }
            }
        }
    }

    private void addPine(List<Spawn> out, int px, int py, float rA, float rB) {
        int v = (int) (rB * 3f);
        int w = 150 + (int) (rA * 46);
        int h = (int) (w * 1.3125f);
        out.add(new Spawn(K_PINE, px - w / 2, py - h + 30, w, h, 600, v));
    }

    private static float smoothstep(float a, float b, float x) {
        return smooth((x - a) / (b - a));
    }
}
