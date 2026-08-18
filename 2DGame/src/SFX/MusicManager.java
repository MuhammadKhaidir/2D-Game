package SFX;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class MusicManager {

    private Clip clip;
    private FloatControl volumeControl;
    
 // TAMBAHKAN METHOD INI DI DALAM MUSICMANAGER
    public void playSE(String fileName) {
        try {
            // Ambil resource langsung (JANGAN panggil stop() biar BGM gak mati)
            URL url = getClass().getResource("/SFX/" + fileName);
            if (url == null) {
                System.err.println("[MusicManager] File SE tidak ditemukan: " + fileName);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip seClip = AudioSystem.getClip(); // Menggunakan variabel lokal agar tidak menabrak Clip BGM
            seClip.open(ais);

            // Set volume khusus SE (opsional)
            if (seClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) seClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f); // Turunin dikit biar gak budek
            }

            seClip.start(); // JALANKAN SEKALI SAJA (Jangan di-loop!)
            
        } catch (Exception e) {
            System.err.println("[MusicManager] Error muter SE: " + e.getMessage());
        }
    }
    public void playSE(String fileName, float volume) {
        try {
            URL url = getClass().getResource("/SFX/" + fileName);
            if (url == null) {
                System.err.println("[MusicManager] File SE tidak ditemukan: " + fileName);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip seClip = AudioSystem.getClip(); 
            seClip.open(ais);

            if (seClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) seClip.getControl(FloatControl.Type.MASTER_GAIN);
                
                // Batasi input agar tidak melampaui batas minimum/maksimum Java API
                if (volume > 6.0f) volume = 6.0f;      // 6.0f adalah volume paling keras di Java
                if (volume < -80.0f) volume = -80.0f;  // -80.0f artinya mute total
                
                gainControl.setValue(volume); 
            }

            seClip.start(); 
            
        } catch (Exception e) {
            System.err.println("[MusicManager] Error muter SE: " + e.getMessage());
        }
    }

    public void play(String fileName) {
        try {
            stop(); // Stop dulu kalau ada yang lagi main

            URL url = getClass().getResource("/SFX/" + fileName);
            if (url == null) {
                System.err.println("[MusicManager] File tidak ditemukan: " + fileName);
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(ais);

            // Set volume (opsional)
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                volumeControl.setValue(-10.0f); // -10 dB, bisa diubah
            }

            // LOOP TERUS MENERUS
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    public void setVolume(float db) {
        // db: -80.0f (mute) sampai 6.0f (max)
        if (volumeControl != null) {
            volumeControl.setValue(db);
        }
    }

    public boolean isPlaying() {
        return clip != null && clip.isRunning();
    }
}