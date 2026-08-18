package SFX;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
public class SoundManager {
    // ===== SOUND CLIPS =====
    private Clip wooshClip;
    private Clip punchClip;
    private Clip swingClip;
    private Clip kingAttackClip; 
    private Clip spawnSkillClip;  
    private Clip kingSpawnClip;
    private Clip kingSkill2Clip; // NEW: sound saat King muncul kembali dari skill Skeleton Hand
    public SoundManager() {
        wooshClip = loadClip("/SFX/ribhavagrawal-woosh-230554.wav");  // whoosh sfx
        punchClip = loadClip("/SFX/freesound_community-punch-2-37333.wav"); // punch sfx
        swingClip = loadClip("/SFX/SwordSfx1.wav");
        kingAttackClip = loadClip("/SFX/KingSkeletonAttack.wav");   
        spawnSkillClip = loadClip("/SFX/SkeletonSpawningSkill.wav"); 
        kingSpawnClip = loadClip("/SFX/KingSkeletonSpawning.wav");
        kingSkill2Clip = loadClip("/SFX/KingSkeletonSkill2.wav"); // NEW
    }
    
    /**
     * Load file WAV dari resource path.
     * Taruh file .wav kamu di: src/Sound/woosh.wav dan src/Sound/punch.wav
     */
    private Clip loadClip(String path) {
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
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[SoundManager] Gagal load: " + path);
            e.printStackTrace();
            return null;
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