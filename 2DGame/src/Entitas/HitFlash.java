package Entitas;

import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Efek "kena pukul": bikin versi putih polos dari sebuah sprite (bentuk / transparansinya tetap, warnanya jadi putih semua).
 * Dipakai Golem & Golemite pas whiteFlashTimer-nya masih jalan. Hasilnya di-cache per gambar, jadi dibuat sekali saja.
 */
public final class HitFlash {

    private static final Map<BufferedImage, BufferedImage> CACHE = new IdentityHashMap<>();

    private HitFlash() {}

    public static synchronized BufferedImage whiteVersion(BufferedImage src) {
        BufferedImage w = CACHE.get(src);
        if (w != null) return w;
        int iw = src.getWidth(), ih = src.getHeight();
        w = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < ih; py++) {
            for (int px = 0; px < iw; px++) {
                int a = src.getRGB(px, py) >>> 24;
                if (a != 0) w.setRGB(px, py, (a << 24) | 0xFFFFFF);
            }
        }
        CACHE.put(src, w);
        return w;
    }
}
