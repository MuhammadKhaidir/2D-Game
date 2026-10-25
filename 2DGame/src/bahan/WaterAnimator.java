package bahan;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Animasi gelombang air untuk semua tile air (dan tepi pantainya), tanpa aset gambar baru.
 *
 * Cara kerja:
 *  1. Satu tekstur air "dunia" berukuran 6x6 tile dibuat sekali pas load, sebanyak F frame.
 *     Teksturnya nyambung mulus ke segala arah (seamless) dan nyambung dari frame terakhir ke frame
 *     pertama (loop), karena semua gelombangnya pakai bilangan gelombang bulat.
 *     Isinya: ayunan lebar (swell) pelan, garis-garis riak putus-putus yang geser ke samping,
 *     garis kilau yang lebih jarang, dan kilatan kecil.
 *  2. Tile air penuh (1, 2) cukup menggambar potongan 48x48 dari tekstur itu. Posisi potongan ditentukan
 *     dari kolom/baris tile, jadi danau yang luas kelihatan satu permukaan air yang sama, bukan tile berulang.
 *  3. Tile tepi pantai: air animasi digambar dulu, lalu "lapisan darat" di atasnya. Lapisan darat = gambar tile
 *     aslinya dengan piksel airnya dilubangi, ditambah buih putih yang maju-mundur di sepanjang garis pantai.
 *     Jadi bentuk pantai (rumput, tebing coklat, dst.) tidak berubah sama sekali, hanya airnya yang bergerak.
 *
 * Semua dihitung dalam "piksel seni" (1 tile = 16x16 piksel seni, 1 piksel seni = 3 piksel layar) supaya
 * gayanya sama dengan tile aslinya.
 *
 * Ubah angka-angka di bawah (kecepatan, warna, kepadatan riak) untuk menyetel tampilannya.
 */
public final class WaterAnimator {

    /** piksel seni per sisi tile */
    public static final int ART = 16;
    /** sisi tekstur dalam piksel seni (6 tile) */
    public static final int N = 96;
    /** jumlah frame satu putaran loop */
    public static final int F = 24;
    /** lama satu frame (ms). Satu putaran = F * FRAME_MS = 2,6 detik. Makin kecil = ombak makin cepat. */
    public static final int FRAME_MS = 110;

    private static final double TAU = Math.PI * 2.0;

    // warna (RGB). BASE harus sama dengan warna air di tile aslinya (59,143,202).
    static final int BASE    = 0x3B8FCA;
    static final int DEEP    = 0x2F80BE;
    static final int LIGHT   = 0x60AADC;
    static final int CREST   = 0x9CD4F2;
    static final int SPARK   = 0xEEF9FF;
    static final int SWELL_L = 0x4195CE;
    static final int SWELL_D = 0x3689C5;

    private final int tile;      // sisi tile dalam piksel layar (48)
    private final int px;        // piksel layar per piksel seni (3)
    private final int period;    // sisi tekstur dalam tile (6)
    private final BufferedImage[] frames = new BufferedImage[F];
    private final BufferedImage[][] overlayOf;           // per index tile; null = air penuh
    private final boolean[] handled;
    private int cur = 0;

    public WaterAnimator(BufferedImage[] tileImages, int tileSize) {
        this.px = Math.max(1, tileSize / ART);
        this.tile = px * ART;
        this.period = N / ART;
        this.handled = new boolean[tileImages.length];
        this.overlayOf = new BufferedImage[tileImages.length][];

        for (int t = 0; t < F; t++) frames[t] = renderTexture(t, px);

        Map<BufferedImage, BufferedImage[]> cache = new IdentityHashMap<>();
        for (int i = 0; i < tileImages.length; i++) {
            if (tileImages[i] == null || !WaterCollision.isWaterTile(i)) continue;
            handled[i] = true;
            BufferedImage src = tileImages[i];
            if (!cache.containsKey(src)) cache.put(src, buildOverlay(src, px));
            overlayOf[i] = cache.get(src);
        }
    }

    /** true kalau tile index ini digambar oleh WaterAnimator */
    public boolean handles(int tileIndex) {
        return tileIndex >= 0 && tileIndex < handled.length && handled[tileIndex];
    }

    /** Panggil sekali per frame gambar (sebelum menggambar tile), supaya semua tile pakai frame yang sama. */
    public void beginFrame() {
        cur = (int) ((System.nanoTime() / 1_000_000L / FRAME_MS) % F);
    }

    /** Untuk tes: paksa frame tertentu. */
    public void setFrame(int f) {
        cur = ((f % F) + F) % F;
    }

    public int getFrame() {
        return cur;
    }

    /**
     * Gambar satu tile air.
     * @param col,row kolom/baris tile di peta (menentukan potongan tekstur dan fase buih)
     * @param sx,sy   posisi kiri-atas di layar
     * @param size    ukuran tile di layar
     */
    public void drawTile(Graphics2D g2, int tileIndex, int col, int row, int sx, int sy, int size) {
        int tx = Math.floorMod(col, period) * tile;
        int ty = Math.floorMod(row, period) * tile;
        g2.drawImage(frames[cur], sx, sy, sx + size, sy + size, tx, ty, tx + tile, ty + tile, null);

        BufferedImage[] ov = overlayOf[tileIndex];
        if (ov != null) {
            // fase buih bergeser per tile -> buih bergelombang menyusuri pantai, bukan kedip serentak
            int f = Math.floorMod(cur + 2 * (col + row), F);
            g2.drawImage(ov[f], sx, sy, size, size, null);
        }
    }

    // ------------------------------------------------------------------ tekstur air

    /** Satu frame tekstur air, N*px x N*px piksel layar. */
    static BufferedImage renderTexture(int t, int px) {
        int[] art = new int[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) art[y * N + x] = artPixel(x, y, t);
        }
        int size = N * px;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int[] data = new int[size * size];
        for (int y = 0; y < size; y++) {
            int ay = y / px;
            for (int x = 0; x < size; x++) data[y * size + x] = art[ay * N + x / px];
        }
        img.setRGB(0, 0, size, size, data, 0, size);
        return img;
    }

    /** Warna satu piksel seni (x,y) pada frame t. Rumusnya sengaja dibuat periodik di x, y (N) dan t (F). */
    static int artPixel(int xi, int yi, int ti) {
        double x = xi, y = yi, t = ti;
        int c = BASE;

        // ayunan lebar & pelan (dua warna tipis)
        double sw = Math.sin(TAU * (1 * x / N + 2 * y / N - t / F) + 0.4)
                  + 0.8 * Math.sin(TAU * (-2 * x / N + 1 * y / N - t / F) + 2.0)
                  + 0.5 * Math.sin(TAU * (3 * x / N - 1 * y / N + 2 * t / F));
        if (sw > 1.05) c = SWELL_L;
        if (sw < -1.15) c = SWELL_D;

        // garis riak putus-putus: baris bergelombang yang geser ke samping
        double wob = 0.55 * Math.sin(TAU * (2 * x / N - t / F))
                   + 0.35 * Math.sin(TAU * (5 * x / N + 1.0 * t / F) + 1.3)
                   + 0.25 * Math.sin(TAU * (3 * y / N));
        double s = 10 * y / N + wob / 8.0;
        double fr = s - Math.floor(s);
        double dash = Math.sin(TAU * (3 * x / N - t / F) + 2.2 * Math.sin(TAU * (y / N)))
                    + 0.6 * Math.sin(TAU * (7 * x / N + 2 * t / F) + 0.7 * Math.sin(TAU * 2 * y / N));
        boolean on = dash > 0.30;
        if (on && fr >= 0.105 && fr < 0.21) c = DEEP;     // bayangan tipis di bawah riak
        if (on && fr > 0.0 && fr < 0.105) c = LIGHT;       // riak

        // garis kilau lebih terang, lebih jarang, geser ke arah lain
        double wob2 = 0.5 * Math.sin(TAU * (3 * x / N + t / F) + 0.8)
                    + 0.3 * Math.sin(TAU * (4 * x / N - 2 * t / F));
        double s2 = 7 * y / N + wob2 / 8.0 + 0.37;
        double fr2 = s2 - Math.floor(s2);
        double dash2 = Math.sin(TAU * (2 * x / N + t / F) + 1.0 + 1.5 * Math.sin(TAU * (2 * y / N)));
        if (dash2 > 0.88 && fr2 > 0.0 && fr2 < 0.145) c = CREST;

        // kilatan kecil
        double sp = Math.sin(TAU * (11 * x / N + 7 * y / N - 3 * t / F))
                  + Math.sin(TAU * (-9 * x / N + 13 * y / N + 2 * t / F) + 1.0);
        if (sp > 1.985) c = SPARK;
        return c;
    }

    // ------------------------------------------------------------------ lapisan darat + buih

    /**
     * Bikin F frame lapisan darat untuk satu gambar tile tepi pantai.
     * Piksel yang warnanya persis warna air dilubangi (transparan), sisanya (rumput, tebing, dll.) tetap.
     * Piksel air yang dekat darat diwarnai buih, lebarnya naik-turun mengikuti frame.
     * @return null kalau gambarnya air penuh (tidak ada darat), tidak perlu lapisan.
     */
    static BufferedImage[] buildOverlay(BufferedImage src, int px) {
        int w = src.getWidth(), h = src.getHeight();
        int bw = Math.max(1, w / ART), bh = Math.max(1, h / ART);
        int[] rgb = new int[ART * ART];
        boolean[] land = new boolean[ART * ART];
        int landCount = 0;
        for (int ay = 0; ay < ART; ay++) {
            for (int ax = 0; ax < ART; ax++) {
                int p = src.getRGB(Math.min(w - 1, ax * bw + bw / 2), Math.min(h - 1, ay * bh + bh / 2));
                rgb[ay * ART + ax] = p & 0xFFFFFF;
                boolean transparent = ((p >>> 24) & 0xFF) < 128;
                boolean water = !transparent && (p & 0xFFFFFF) == BASE;
                land[ay * ART + ax] = !water && !transparent;
                if (land[ay * ART + ax]) landCount++;
            }
        }
        if (landCount == 0) return null;

        // jarak tiap piksel ke piksel darat terdekat (satuan piksel seni)
        double[] dist = new double[ART * ART];
        for (int ay = 0; ay < ART; ay++) {
            for (int ax = 0; ax < ART; ax++) {
                double best = 99;
                for (int by = 0; by < ART; by++) {
                    for (int bx = 0; bx < ART; bx++) {
                        if (!land[by * ART + bx]) continue;
                        double dx = ax - bx, dy = ay - by;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if (d < best) best = d;
                    }
                }
                dist[ay * ART + ax] = best;
            }
        }

        int size = ART * px;
        BufferedImage[] out = new BufferedImage[F];
        for (int f = 0; f < F; f++) {
            double phi = (double) f / F;
            double swell = 0.5 - 0.5 * Math.cos(TAU * phi);       // 0..1, ombak maju lalu mundur
            double limit = 1.05 + 1.5 * swell;                     // lebar buih (piksel seni)
            int[] data = new int[size * size];
            for (int ay = 0; ay < ART; ay++) {
                for (int ax = 0; ax < ART; ax++) {
                    int i = ay * ART + ax;
                    int argb;
                    if (land[i]) {
                        argb = 0xFF000000 | rgb[i];
                    } else if (dist[i] <= limit) {
                        double brk = Math.sin(TAU * ((2.0 * ax + 3.0 * ay) / ART - 2.0 * phi));
                        if (dist[i] <= 1.05) {
                            // bibir buih: putus-putus kalau ombak lagi surut, nyaris penuh kalau lagi pasang
                            argb = (brk > 0.9 - 1.5 * swell) ? (0xFF000000 | SPARK) : 0;
                        } else {
                            argb = (brk > -0.2) ? (0xFF000000 | CREST) : 0;
                        }
                    } else {
                        argb = 0;
                    }
                    if (argb == 0) continue;
                    for (int yy = 0; yy < px; yy++) {
                        int row = (ay * px + yy) * size + ax * px;
                        for (int xx = 0; xx < px; xx++) data[row + xx] = argb;
                    }
                }
            }
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, size, size, data, 0, size);
            out[f] = img;
        }
        return out;
    }
}
