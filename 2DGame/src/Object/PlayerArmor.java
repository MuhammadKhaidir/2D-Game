package Object;

import Item.ArmorSet;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * PlayerArmor - sprite SATU SET armor utuh (bukan lagi per bagian helm/chest/legs).
 *
 * Tiap frame animasi punya 2 layer, sama kayak urutan gambar di Player.draw():
 *   body : badan armor. MENGGANTIKAN baju dasar (PlayerCloth) selama armor dipakai.
 *   head : helm. Digambar paling atas, setelah senjata.
 *
 * File: /Aset/armor/<assetFolder>/<layer>_<state>_<arah>_<nomor>.png  (nomor mulai dari 1)
 *   state : idle(1) walk(6) run(3) jump(2) atk1(4) atk2(4) atk3(4) atk4(4) dead(3)   (dalam kurung = jumlah frame)
 *   arah  : down up left right  (sama kayak player.direction)
 *   atkN  : N = attackType di Player (1 pukulan, 2 swing tangan kosong, 3-4 combo pakai senjata)
 *   jump  : nomor frame = index frame jump BODY (bukan index baju), jadi gak perlu dibalik lagi
 * Sprite dibuat dari sprite body/baju yang sudah ada, jadi selalu sejajar: lihat tools/generate_dread_armor.py.
 *
 * Asset yang gak ketemu cuma nge-print warning (nggak crash) dan layernya dilewati pas digambar.
 * Pakai PlayerArmor.of(set) supaya sprite satu set cuma dimuat sekali.
 */
public class PlayerArmor {

    public static final String BODY = "body";
    public static final String HEAD = "head";

    private static final String[] DIRECTIONS = { "down", "up", "left", "right" };
    private static final String[] LAYERS = { BODY, HEAD };
    private static final String[] STATES = { "idle", "walk", "run", "jump", "atk1", "atk2", "atk3", "atk4", "dead" };
    private static final int[] FRAME_COUNT = { 1, 6, 3, 2, 4, 4, 4, 4, 3 };

    private static final Map<ArmorSet, PlayerArmor> CACHE = new HashMap<>();

    /** Ambil (dan cache) sprite untuk satu set. */
    public static PlayerArmor of(ArmorSet set) {
        if (set == null) return null;
        PlayerArmor armor = CACHE.get(set);
        if (armor == null) {
            armor = new PlayerArmor(set);
            CACHE.put(set, armor);
        }
        return armor;
    }

    public final ArmorSet set;

    // key: layer + "_" + state + "_" + arah  ->  array frame
    private final Map<String, BufferedImage[]> frames = new HashMap<>();

    private PlayerArmor(ArmorSet set) {
        this.set = set;
        loadAll();
    }

    private void loadAll() {
        for (String layer : LAYERS) {
            for (int s = 0; s < STATES.length; s++) {
                for (String dir : DIRECTIONS) {
                    BufferedImage[] arr = new BufferedImage[FRAME_COUNT[s]];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = readSafely("/Aset/armor/" + set.assetFolder + "/"
                                + layer + "_" + STATES[s] + "_" + dir + "_" + (i + 1) + ".png");
                    }
                    frames.put(key(layer, STATES[s], dir), arr);
                }
            }
        }
    }

    private BufferedImage readSafely(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[PlayerArmor:" + set.itemName + "] Asset GA KETEMU: " + path);
            return null;
        }
        try {
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("[PlayerArmor:" + set.itemName + "] Gagal baca: " + path);
            return null;
        } finally {
            try { is.close(); } catch (IOException ignored) { }
        }
    }

    private static String key(String layer, String state, String direction) {
        return layer + "_" + state + "_" + direction;
    }

    // ================= GETTERS (semua null-safe) =================

    /**
     * @param layer     BODY atau HEAD
     * @param state     "idle" "walk" "run" "jump" "atk1".."atk4" "dead"
     * @param frameIndex index frame (di-modulo, jadi aman kalau kebanyakan)
     * @param direction "down" "up" "left" "right" (arah lain, mis. "emote", -> null)
     */
    public BufferedImage get(String layer, String state, int frameIndex, String direction) {
        if (direction == null || frameIndex < 0) return null;
        BufferedImage[] arr = frames.get(key(layer, state, direction));
        if (arr == null || arr.length == 0) return null;
        return arr[frameIndex % arr.length];
    }

    public BufferedImage getBody(String state, int frameIndex, String direction) {
        return get(BODY, state, frameIndex, direction);
    }

    public BufferedImage getHead(String state, int frameIndex, String direction) {
        return get(HEAD, state, frameIndex, direction);
    }

    /** attackType Player (1-4) -> nama state: "atk1".."atk4". */
    public static String attackState(int attackType) {
        return "atk" + attackType;
    }
}
