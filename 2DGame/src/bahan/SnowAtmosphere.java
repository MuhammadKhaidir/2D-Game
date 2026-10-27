package bahan;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.util.Random;

import Main.GamePanel;

/**
 * Suasana biome es, digambar di layar (bukan di dunia) di atas semua objek:
 *  - hujan salju (partikel, 3 lapis kedalaman, ikut bergeser waktu player jalan supaya terasa berjarak)
 *  - kabut tipis yang melayang pelan + vignette putih kebiruan di tepi layar
 *  - sapuan warna dingin (biru-putih) di seluruh layar
 *
 * Kekuatannya mengikuti seberapa "bersalju" posisi player (WorldGen.ice), dan berubah pelan-pelan, jadi waktu
 * player berjalan dari hutan ke salju suasananya beralih halus, bukan tiba-tiba. Di luar biome es semuanya mati
 * (tidak ada biaya gambar sama sekali).
 */
public final class SnowAtmosphere {

    private static final int N = 340;                 // jumlah maksimum serpihan
    private final GamePanel gp;
    private final int W, H;

    private float intensity = 0f;                     // 0..1, dihaluskan
    private float target = 0f;
    private boolean heavy = false;                    // badai (dipakai kalau lagi hujan di biome es)
    private float heavyMix = 0f;

    private final float[] bx = new float[N], by = new float[N], depth = new float[N], drift = new float[N], phase = new float[N];
    private float time = 0f;

    private BufferedImage vignette;                   // sapuan warna dingin + putih kebiruan di tepi layar (satu gambar, satu kali gambar)
    private BufferedImage fog;                        // gumpalan kabut lembut
    private final int[] order = new int[N];           // serpihan diurutkan per kedalaman: jauh -> dekat
    private final int[] classEnd = new int[3];        // batas kelompok (jauh / sedang / dekat) di order[]

    public SnowAtmosphere(GamePanel gp) {
        this.gp = gp;
        this.W = gp.screenWidth;
        this.H = gp.screenHeight;
        Random rnd = new Random(7);
        for (int i = 0; i < N; i++) {
            bx[i] = rnd.nextFloat() * W;
            by[i] = rnd.nextFloat() * H;
            depth[i] = 0.30f + 0.70f * rnd.nextFloat() * rnd.nextFloat() + 0.25f * rnd.nextFloat();
            if (depth[i] > 1f) depth[i] = 1f;
            drift[i] = (rnd.nextFloat() - 0.5f) * 2f;
            phase[i] = rnd.nextFloat() * 6.2832f;
        }
        // urutkan serpihan per kedalaman supaya bisa digambar per kelompok (alpha sama) tanpa ganti-ganti composite
        Integer[] idx = new Integer[N];
        for (int i = 0; i < N; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (x, y) -> Float.compare(depth[x], depth[y]));
        for (int i = 0; i < N; i++) order[i] = idx[i];
        int c0 = 0, c1 = 0;
        for (int i = 0; i < N; i++) { if (depth[order[i]] < 0.55f) c0 = i + 1; if (depth[order[i]] < 0.82f) c1 = i + 1; }
        classEnd[0] = c0; classEnd[1] = c1; classEnd[2] = N;
        buildSprites();
    }

    private void buildSprites() {
        vignette = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = vignette.createGraphics();
        g.setColor(new Color(150, 195, 240, 38));            // sapuan warna dingin di seluruh layar
        g.fillRect(0, 0, W, H);
        float r = (float) Math.hypot(W, H) * 0.55f;
        g.setPaint(new RadialGradientPaint(W / 2f, H / 2f, r, new float[] {0f, 0.55f, 1f},
                new Color[] {new Color(225, 238, 255, 0), new Color(225, 238, 255, 18), new Color(225, 238, 255, 150)}));
        g.fillRect(0, 0, W, H);
        g.dispose();

        int fw = 720, fh = 360;
        fog = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
        g = fog.createGraphics();
        g.setPaint(new RadialGradientPaint(fw / 2f, fh / 2f, fw / 2f, new float[] {0f, 1f},
                new Color[] {new Color(235, 245, 255, 120), new Color(235, 245, 255, 0)}));
        g.scale(1.0, fh / (double) fw);
        g.fillRect(0, 0, fw, fw);
        g.dispose();
    }

    /** kekuatan efek sekarang (0..1); dipakai juga untuk menyembunyikan hujan biasa di biome es */
    public float intensity() { return intensity; }

    public void setHeavy(boolean heavy) { this.heavy = heavy; }

    /** Dipanggil tiap frame saat di dunia luar. */
    public void update() {
        WorldGen gen = gp.tileM.gen;
        if (gen != null && gp.player != null) {
            float ice = gen.ice((gp.player.x + gp.tileSize / 2f) / gp.tileSize, (gp.player.y + gp.tileSize / 2f) / gp.tileSize);
            // sama dengan kapan tanah mulai ditutupi salju: mulai terasa di pinggir, penuh di dalam
            float t = (ice - 0.10f) / 0.60f;
            target = t < 0f ? 0f : (t > 1f ? 1f : t * t * (3f - 2f * t));
        }
        // naik pelan (~3 detik), turun pelan (~4 detik)
        intensity += (target - intensity) * (target > intensity ? 0.012f : 0.009f);
        if (Math.abs(target - intensity) < 0.002f) intensity = target;
        heavyMix += ((heavy ? 1f : 0f) - heavyMix) * 0.02f;
        time += 1f / gp.FPS;
    }

    /** Gambar di layar normal (sesudah transform zoom dunia dilepas). */
    public void draw(Graphics2D g2) {
        if (intensity < 0.01f) return;
        Composite old = g2.getComposite();
        float in = intensity;

        // 1) sapuan warna dingin + vignette putih kebiruan (satu gambar)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, in * (0.88f + 0.12f * heavyMix))));
        g2.drawImage(vignette, 0, 0, null);

        // 2) kabut melayang
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, (0.55f + 0.30f * heavyMix) * in)));
        for (int i = 0; i < 2; i++) {
            float speed = 9f + 6f * i;
            int fw = fog.getWidth(), fh = fog.getHeight();
            float cycle = W + fw;
            float x = ((time * speed + i * 420f + gp.player.x * 0.05f) % cycle) - fw;
            int y = (int) (H * (0.20f + 0.40f * i) + Math.sin(time * 0.15f + i * 2f) * 22f - gp.player.y * 0.02f % 40f);
            g2.drawImage(fog, (int) x, y, null);
        }

        // 3) serpihan salju, digambar per kelompok kedalaman
        int count = (int) (N * (0.30f + 0.70f * in) * (0.65f + 0.35f * heavyMix));
        float camX = gp.player.x, camY = gp.player.y;
        float wind = 22f + 38f * heavyMix;
        float fall = 36f + 40f * heavyMix;
        float[] classAlpha = {0.50f, 0.72f, 0.92f};
        int[] classSize = {2, 3, 4};
        int start = 0;
        for (int cls = 0; cls < 3; cls++) {
            int end = Math.min(classEnd[cls], N);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, classAlpha[cls] * Math.min(1f, in * 1.2f)));
            g2.setColor(cls == 2 ? Color.WHITE : new Color(238, 246, 255));
            int size = classSize[cls];
            for (int k = start; k < end; k++) {
                int i = order[k];
                if ((i * 7919) % N >= count) continue;               // sebagian serpihan dimatikan kalau lebih tenang
                float d = depth[i];
                float sx = bx[i] + time * (wind * d * 0.55f + drift[i] * 8f) + (float) Math.sin(time * 1.3f + phase[i]) * 9f * d - camX * 0.45f * d;
                float sy = by[i] + time * fall * (0.35f + 0.65f * d) - camY * 0.45f * d;
                sx = ((sx % W) + W) % W;
                sy = ((sy % H) + H) % H;
                g2.fillRect((int) sx, (int) sy, size, size);
            }
            start = end;
        }
        g2.setComposite(old);
    }
}
