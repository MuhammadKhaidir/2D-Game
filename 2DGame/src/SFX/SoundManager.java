package SFX;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
public class SoundManager {
    // Clip dimuat sekali per file lalu dipakai bersama semua SoundManager.
    // Sebelumnya tiap Skeleton/UndeadMinion yang dibuat memuat 7 Clip baru (7 thread + memori audio)
    // yang tidak pernah ditutup.
    private static final Map<String, Clip> CACHE = new HashMap<>();

    // ===== VOLUME SUARA SKILL V (dB, 0 = sesuai file aslinya) =====
    // Kurang gede? Naikin angkanya (+6 dB = 2x lebih keras). Kalau mulai kasar / pecah, turunin lagi.
    private static final float ULTIMATE_START_GAIN_DB = 5f;
    private static final float MULTIPLE_PUNCH_GAIN_DB = 8f;
    private static final float LAST_PUNCH_GAIN_DB     = 10f;
    // ===== VOLUME SUARA SKILL LEVIATHAN FANG (dB, 0 = sesuai file aslinya) =====
    private static final float LEVIATHAN_ROAR_GAIN_DB = 0f;
    private static final float LEVIATHAN_BITE_GAIN_DB = 0f;
    // Sampel yang lewat batas ini (0..1 dari volume maksimum) dilembutkan pelan-pelan, bukan dipotong,
    // jadi suara yang dinaikkan banyak tetap keras tapi tidak pecah.
    private static final float LIMITER_KNEE = 0.7f;

    // ===== SOUND CLIPS =====
    private Clip wooshClip;
    private Clip punchClip;
    private Clip swingClip;
    private Clip kingAttackClip; 
    private Clip spawnSkillClip;  
    private Clip kingSpawnClip;
    private Clip kingSkill2Clip; // NEW: sound saat King muncul kembali dari skill Skeleton Hand
    private Clip ultimateStartClip;  // SkillV: pembukaan ultimate
    private Clip multiplePunchClip;  // SkillV: tiap player pindah tempat mukul
    private Clip lastPunchClip;      // SkillV: pukulan terakhir
    private Clip leviathanRoarClip;  // Leviathan Fang (R): raungan pas skill mulai (5 detik)
    private Clip leviathanBiteClip;  // Leviathan Fang (R): gigitan pas skill selesai
    public SoundManager() {
        wooshClip = loadClip("/SFX/ribhavagrawal-woosh-230554.wav");  // whoosh sfx
        punchClip = loadClip("/SFX/freesound_community-punch-2-37333.wav"); // punch sfx
        swingClip = loadClip("/SFX/SwordSfx1.wav");
        kingAttackClip = loadClip("/SFX/KingSkeletonAttack.wav");   
        spawnSkillClip = loadClip("/SFX/SkeletonSpawningSkill.wav"); 
        kingSpawnClip = loadClip("/SFX/KingSkeletonSpawning.wav");
        kingSkill2Clip = loadClip("/SFX/KingSkeletonSkill2.wav"); // NEW
        ultimateStartClip = loadClipLoud("/SFX/UltimateStart.wav", ULTIMATE_START_GAIN_DB);
        multiplePunchClip = loadClipLoud("/SFX/MultiplePunch.wav", MULTIPLE_PUNCH_GAIN_DB);
        lastPunchClip = loadClipLoud("/SFX/LastPunch.wav", LAST_PUNCH_GAIN_DB);
        leviathanRoarClip = loadClipLoud("/SFX/LeviathanRoar.wav", LEVIATHAN_ROAR_GAIN_DB);
        leviathanBiteClip = loadClipLoud("/SFX/LeviathanBite.wav", LEVIATHAN_BITE_GAIN_DB);
    }
    
    /**
     * Load file WAV dari resource path.
     * Taruh file .wav kamu di: src/Sound/woosh.wav dan src/Sound/punch.wav
     */
    private Clip loadClip(String path) {
        synchronized (CACHE) {
            if (CACHE.containsKey(path)) return CACHE.get(path);
            Clip c = loadClipUncached(path);
            CACHE.put(path, c); // null juga disimpan, supaya file yang gagal tidak dicoba terus
            return c;
        }
    }

    private Clip loadClipUncached(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.err.println("[SoundManager] File tidak ditemukan: " + path);
                return null;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (IllegalArgumentException e) {
            // Tidak ada perangkat audio (audio dimatikan / remote desktop / dll): game tetap jalan tanpa suara
            System.err.println("[SoundManager] Perangkat audio tidak tersedia, lewati: " + path);
            return null;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[SoundManager] Gagal load: " + path);
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Sama kayak loadClip, tapi volumenya dinaikkan gainDb (lewat data suaranya langsung, jadi bisa lebih keras
     * dari batas kontrol volume Java yang cuma +6 dB). gainDb <= 0 -> sama persis dengan loadClip.
     * Sekalian mengubah file WAV 8/24/32-bit atau float jadi 16-bit supaya tetap bisa dimainkan.
     */
    private Clip loadClipLoud(String path, float gainDb) {
        if (gainDb <= 0f) return loadClip(path);
        String key = path + "|+" + gainDb + "dB";
        synchronized (CACHE) {
            if (CACHE.containsKey(key)) return CACHE.get(key);
            Clip c = loadClipLoudUncached(path, gainDb);
            CACHE.put(key, c);
            return c;
        }
    }

    private Clip loadClipLoudUncached(String path, float gainDb) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("[SoundManager] File tidak ditemukan: " + path);
            return null;
        }
        AudioFormat pcm;
        byte[] data;
        try (AudioInputStream src = AudioSystem.getAudioInputStream(url)) {
            AudioFormat sf = src.getFormat();
            pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sf.getSampleRate(), 16,
                    sf.getChannels(), sf.getChannels() * 2, sf.getSampleRate(), false);
            try (AudioInputStream in = AudioSystem.getAudioInputStream(pcm, src)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                data = out.toByteArray();
            }
        } catch (IllegalArgumentException | UnsupportedAudioFileException | IOException e) {
            System.err.println("[SoundManager] Gagal load (format tidak didukung?): " + path);
            e.printStackTrace();
            return null;
        }
        boostPcm16(data, gainDb);
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(pcm, data, 0, data.length);
            return clip;
        } catch (IllegalArgumentException e) {
            // Tidak ada perangkat audio: game tetap jalan tanpa suara
            System.err.println("[SoundManager] Perangkat audio tidak tersedia, lewati: " + path);
            return null;
        } catch (LineUnavailableException e) {
            System.err.println("[SoundManager] Gagal load: " + path);
            e.printStackTrace();
            return null;
        }
    }

    /** Naikkan volume data PCM 16-bit little-endian sebesar gainDb. Sampel yang kelewat keras dilembutkan (soft clip). */
    static void boostPcm16(byte[] data, float gainDb) {
        float gain = (float) Math.pow(10.0, gainDb / 20.0);
        float knee = LIMITER_KNEE;
        for (int i = 0; i + 1 < data.length; i += 2) {
            short s = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
            float a = Math.abs(s / 32768f * gain);
            if (a > knee) a = knee + (1f - knee) * (float) Math.tanh((a - knee) / (1f - knee));
            int y = Math.round(Math.copySign(a, s) * 32767f);
            data[i] = (byte) y;
            data[i + 1] = (byte) (y >> 8);
        }
    }

    /** Mainkan suara woosh (ayunan tanpa kena) */
    public void playWoosh() {
        playClip(wooshClip);
    }
    
    public void swingSound() {
        playClip(swingClip);
    }
    public void kingAttackSound() {
        playClip(kingAttackClip);
    }
    public void spawnSkillSound() {
        playClip(spawnSkillClip);
    }
    public void kingSpawnSound() {
        playClip(kingSpawnClip);
    }
    /** NEW: sound saat Skeleton King muncul kembali setelah skill Skeleton Hand selesai */
    public void kingSkill2Sound() {
        playClip(kingSkill2Clip);
    }
    /** Mainkan suara punch (pukulan kena target) */
    public void playPunch() {
        playClip(punchClip);
    }
    /** SkillV: suara pembukaan ultimate (dipanggil sekali pas skill mulai) */
    public void playUltimateStart() {
        playClip(ultimateStartClip);
    }
    /** SkillV: dipanggil tiap player pindah ke tempat mukul yang baru. Kalau suara sebelumnya belum habis, diulang dari awal. */
    public void playMultiplePunch() {
        playClip(multiplePunchClip);
    }
    /** SkillV: pukulan terakhir. Suara MultiplePunch yang masih jalan dipotong biar pukulan terakhirnya jelas. */
    public void playLastPunch() {
        stopClip(multiplePunchClip);
        playClip(lastPunchClip);
    }
    /** SkillV: matikan semua suara skill (dipanggil kalau skill dibatalin di tengah jalan) */
    public void stopSkillVSounds() {
        stopClip(ultimateStartClip);
        stopClip(multiplePunchClip);
        stopClip(lastPunchClip);
    }
    /** Leviathan Fang: raungan pas skill R mulai (durasi file 5 detik = durasi fase serap skill) */
    public void playLeviathanRoar() {
        playClip(leviathanRoarClip);
    }
    /** Leviathan Fang: gigitan pas fase serap habis. Sisa raungan (kalau masih ada) dipotong dulu. */
    public void playLeviathanBite() {
        stopClip(leviathanRoarClip);
        playClip(leviathanBiteClip);
    }
    /** Leviathan Fang: matikan raungan & gigitan (dipanggil kalau skill dibatalin di tengah jalan) */
    public void stopLeviathanSounds() {
        stopClip(leviathanRoarClip);
        stopClip(leviathanBiteClip);
    }
    private void stopClip(Clip clip) {
        if (clip != null && clip.isRunning()) clip.stop();
    }
    /** Helper: reset ke awal lalu play */
    private void playClip(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.flush();           // NEW: bersihin buffer biar restart-nya konsisten
        clip.setFramePosition(0);
        clip.start();
    }
}