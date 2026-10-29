package Item;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Efek suara mancing. File WAV ada di src/SFX/fishing/ (dibuat tools/generate_fishing_assets.py, boleh diganti
 * file sendiri dengan nama sama). Clip dimuat sekali per nama lalu dipakai ulang.
 * Kalau file / perangkat audio gak ada, {@link #play} diam saja (game tetap jalan).
 */
final class FishingSfx {

    private static final Map<String, Clip> CACHE = new HashMap<>();

    private FishingSfx() {}

    static void play(String name) {
        Clip clip = get(name);
        if (clip == null) return;
        try {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception ignored) { /* audio bermasalah: lewati saja */ }
    }

    private static synchronized Clip get(String name) {
        if (CACHE.containsKey(name)) return CACHE.get(name);
        Clip clip = null;
        try {
            URL url = FishingSfx.class.getResource("/SFX/fishing/" + name + ".wav");
            if (url != null) {
                AudioInputStream in = AudioSystem.getAudioInputStream(url);
                clip = AudioSystem.getClip();
                clip.open(in);
            } else {
                System.err.println("[FishingSfx] File tidak ditemukan: " + name);
            }
        } catch (Throwable t) {
            clip = null;     // gak ada perangkat audio / format gak didukung
        }
        CACHE.put(name, clip);
        return clip;
    }
}
