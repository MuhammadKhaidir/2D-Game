package World;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Peta "lantai / tembok" berbasis gambar hitam-putih buat domain yang bentuknya gak kotak-kotak
 * (contoh: GolemHide, gua dengan tembok batu yang berkelok-kelok).
 *
 *   putih = lantai, boleh dilewati
 *   hitam / transparan / di luar gambar = tembok (player & monster gak bisa nembus)
 *
 * Ukuran mask bebas, yang penting proporsinya sama kayak background domain. 1 piksel mask = (ukuran dunia / ukuran mask) px dunia
 * (GolemHide: mask 800x600, dunia 1600x1200 -> 1 piksel = 2 px dunia).
 * Mask dibuat otomatis dari gambar pakai tools/generate_golem_mask.py, tapi bisa juga di-edit manual pakai editor gambar.
 */
public class WalkMask {

    /**
     * Yang dicek ke tembok cuma bagian KAKI hitbox (separuh bawah), bukan seluruh kotak.
     * Kalau seluruh hitbox dipakai, kaki karakter berhenti 1 hitbox penuh dari tembok atas, jadi kelihatan melayang.
     * Dengan separuh bawah, kaki bisa nempel ke dasar tembok dan kepala numpuk di muka tembok (kayak biasa di game 3/4 top-down).
     */
    private static final double FOOT_FRACTION = 0.5;

    private final int w, h;
    private final double sx, sy;   // piksel mask per px dunia
    private final int[] blockedSum; // summed-area table jumlah piksel tembok, biar cek 1 kotak cuma O(1)
    private BufferedImage debugImage = null;

    private WalkMask(BufferedImage img, int worldW, int worldH) {
        this.w = img.getWidth();
        this.h = img.getHeight();
        this.sx = w / (double) worldW;
        this.sy = h / (double) worldH;

        blockedSum = new int[(w + 1) * (h + 1)];
        for (int y = 0; y < h; y++) {
            int rowSum = 0;
            for (int x = 0; x < w; x++) {
                if (isWall(img.getRGB(x, y))) rowSum++;
                blockedSum[(y + 1) * (w + 1) + (x + 1)] = blockedSum[y * (w + 1) + (x + 1)] + rowSum;
            }
        }
    }

    /** Putih & gak transparan = lantai. Selain itu tembok. */
    private static boolean isWall(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return a < 128 || (r + g + b) / 3 < 128;
    }

    /** @return mask hasil load, atau null kalau filenya gak ketemu / gagal dibaca. */
    public static WalkMask load(String resourcePath, int worldW, int worldH) {
        try {
            URL url = WalkMask.class.getResource(resourcePath);
            if (url == null) return null;
            BufferedImage img = ImageIO.read(url);
            return img != null ? new WalkMask(img, worldW, worldH) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** True kalau hitbox (koordinat dunia) nabrak tembok. Keluar dari area mask juga dianggap tembok. */
    public boolean isBlocked(Rectangle box) {
        int footH = Math.max(1, (int) (box.height * FOOT_FRACTION));
        int top = box.y + box.height - footH;
        int bottom = box.y + box.height;

        int x0 = (int) Math.floor(box.x * sx);
        int x1 = (int) Math.ceil((box.x + box.width) * sx);
        int y0 = (int) Math.floor(top * sy);
        int y1 = (int) Math.ceil(bottom * sy);

        if (x0 < 0 || y0 < 0 || x1 > w || y1 > h) return true;

        int stride = w + 1;
        int sum = blockedSum[y1 * stride + x1] - blockedSum[y0 * stride + x1]
                - blockedSum[y1 * stride + x0] + blockedSum[y0 * stride + x0];
        return sum > 0;
    }

    /** Overlay merah transparan buat tembok (dipakai mode debug F3), digambar sebesar background domain. */
    public BufferedImage getDebugImage() {
        if (debugImage == null) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            int stride = w + 1;
            int red = new java.awt.Color(255, 0, 0, 90).getRGB();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int cell = blockedSum[(y + 1) * stride + (x + 1)] - blockedSum[y * stride + (x + 1)]
                             - blockedSum[(y + 1) * stride + x] + blockedSum[y * stride + x];
                    if (cell > 0) img.setRGB(x, y, red);
                }
            }
            debugImage = img;
        }
        return debugImage;
    }
}
