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
 * Suasana biome terkutuk, digambar di layar (bukan di dunia) di atas semua objek:
 *  - serpihan abu dan percikan ungu yang melayang naik pelan (3 lapis kedalaman, ikut bergeser waktu player jalan)
 *  - kabut ungu gelap yang melayang pelan + vignette gelap keunguan di tepi layar, berdenyut sangat pelan
 *  - sapuan warna ungu di seluruh layar
 *
 * Kekuatannya mengikuti seberapa "terkutuk" posisi player (WorldGen.curse) dan berubah pelan-pelan, jadi waktu player
 * berjalan dari hutan ke biome terkutuk suasananya beralih halus. Di luar biome semuanya mati (tidak ada biaya gambar).
 */
public final class CurseAtmosphere {

    private static final int N = 220;                 // jumlah maksimum serpihan
    private final GamePanel gp;
    private final int W, H;

    private float intensity = 0f;                     // 0..1, dihaluskan
    private float target = 0f;

    private final float[] bx = new float[N], by = new float[N], depth = new float[N], drift = new float[N], phase = new float[N];
    private final boolean[] reddish = new boolean[N];
    private float time = 0f;

    private BufferedImage vignette;                   // sapuan ungu + vignette gelap di tepi layar
    private BufferedImage fog;                        // gumpalan kabut lembut
    private final int[] order = new int[N];           // serpihan diurutkan per kedalaman: jauh -> dekat
    private final int[] classEnd = new int[3];

    public CurseAtmosphere(GamePanel gp) {
        this.gp = gp;
        this.W = gp.screenWidth;
        this.H = gp.screenHeight;
        Random rnd = new Random(13);
        for (int i = 0; i < N; i++) {
            bx[i] = rnd.nextFloat() * W;
            by[i] = rnd.nextFloat() * H;
            depth[i] = 0.30f + 0.70f * rnd.nextFloat() * rnd.nextFloat() + 0.25f * rnd.nextFloat();
            if (depth[i] > 1f) depth[i] = 1f;
            drift[i] = (rnd.nextFloat() - 0.5f) * 2f;
            phase[i] = rnd.nextFloat() * 6.2832f;
            reddish[i] = rnd.nextFloat() < 0.18f;
        }
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
        g.setColor(new Color(70, 20, 110, 40));              // sapuan ungu di seluruh layar
        g.fillRect(0, 0, W, H);
        float r = (float) Math.hypot(W, H) * 0.55f;
        g.setPaint(new RadialGradientPaint(W / 2f, H / 2f, r, new float[] {0f, 0.50f, 1f},
                new Color[] {new Color(20, 4, 34, 0), new Color(20, 4, 34, 40), new Color(14, 2, 26, 190)}));
        g.fillRect(0, 0, W, H);
        g.dispose();

        int fw = 720, fh = 360;
        fog = new BufferedImage(fw, fh, BufferedImage.TYPE_INT_ARGB);
        g = fog.createGraphics();
        g.setPaint(new RadialGradientPaint(fw / 2f, fh / 2f, fw / 2f, new float[] {0f, 1f},
                new Color[] {new Color(60, 22, 92, 110), new Color(60, 22, 92, 0)}));
        g.scale(1.0, fh / (double) fw);
        g.fillRect(0, 0, fw, fw);
        g.dispose();
    }

    /** kekuatan efek sekarang (0..1); dipakai juga untuk menggelapkan layar di malam hari */
    public float intensity() { return intensity; }

    /** Dipanggil tiap frame saat di dunia luar. */
    public void update() {
        WorldGen gen = gp.tileM.gen;
        if (gen != null && gp.player != null) {
            float cs = gen.curse((gp.player.x + gp.tileSize / 2f) / gp.tileSize, (gp.player.y + gp.tileSize / 2f) / gp.tileSize);
            // mulai terasa waktu pohon-pohon mulai layu, penuh di dalam biome
            float t = (cs - 0.08f) / 0.55f;
            target = t < 0f ? 0f : (t > 1f ? 1f : t * t * (3f - 2f * t));
        }
        // naik pelan (~3 detik), turun pelan (~4 detik)
        intensity += (target - intensity) * (target > intensity ? 0.012f : 0.009f);
        if (Math.abs(target - intensity) < 0.002f) intensity = target;
        time += 1f / gp.FPS;
    }

    /** Gambar di layar normal (sesudah transform zoom dunia dilepas). */
    public void draw(Graphics2D g2) {
        if (intensity < 0.01f) return;
        Composite old = g2.getComposite();
        float in = intensity;
        float pulse = 0.92f + 0.08f * (float) Math.sin(time * 0.7f);

        // 1) sapuan ungu + vignette gelap (satu gambar)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, in * pulse)));
        g2.drawImage(vignette, 0, 0, null);

        // 2) kabut melayang
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, 0.60f * in)));
        for (int i = 0; i < 2; i++) {
            float speed = 7f + 5f * i;
            int fw = fog.getWidth(), fh = fog.getHeight();
            float cycle = W + fw;
            float x = ((time * speed + i * 380f + gp.player.x * 0.05f) % cycle) - fw;
            int y = (int) (H * (0.25f + 0.40f * i) + Math.sin(time * 0.17f + i * 2.3f) * 24f - gp.player.y * 0.02f % 40f);
            g2.drawImage(fog, (int) x, y, null);
        }

        // 3) abu dan percikan ungu yang naik pelan, digambar per kelompok kedalaman
        int count = (int) (N * (0.30f + 0.70f * in));
        float camX = gp.player.x, camY = gp.player.y;
        float[] classAlpha = {0.45f, 0.70f, 0.95f};
        int[] classSize = {2, 3, 3};
        Color violet = new Color(196, 140, 255), ember = new Color(255, 96, 140), ash = new Color(170, 150, 190);
        int start = 0;
        for (int cls = 0; cls < 3; cls++) {
            int end = Math.min(classEnd[cls], N);
            float a = classAlpha[cls] * Math.min(1f, in * 1.2f);
            int size = classSize[cls];
            for (int k = start; k < end; k++) {
                int i = order[k];
                if ((i * 7919) % N >= count) continue;
                float d = depth[i];
                float sx = bx[i] + time * (drift[i] * 6f) + (float) Math.sin(time * 0.9f + phase[i]) * 11f * d - camX * 0.45f * d;
                float sy = by[i] - time * (10f + 22f * d) - camY * 0.45f * d;       // naik
                sx = ((sx % W) + W) % W;
                sy = ((sy % H) + H) % H;
                // berkedip pelan
                float tw = 0.65f + 0.35f * (float) Math.sin(time * 2.1f + phase[i] * 3f);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, a * tw)));
                g2.setColor(i % 5 == 0 ? ash : (reddish[i] ? ember : violet));
                g2.fillRect((int) sx, (int) sy, size, size);
            }
            start = end;
        }
        g2.setComposite(old);
    }
}
