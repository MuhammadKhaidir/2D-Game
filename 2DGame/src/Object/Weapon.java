package Object;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Weapon {
    public String name;
    public String type; // "Sword", "Axe", atau "BoneSword"
    
    public int damage;

    // State 1 (4 frame per arah)
    public BufferedImage[] wpState1Up    = new BufferedImage[4];
    public BufferedImage[] wpState1Down  = new BufferedImage[4];
    public BufferedImage[] wpState1Left  = new BufferedImage[4];
    public BufferedImage[] wpState1Right = new BufferedImage[4];

    // State 2 (4 frame per arah)
    public BufferedImage[] wpState2Up    = new BufferedImage[4];
    public BufferedImage[] wpState2Down  = new BufferedImage[4];
    public BufferedImage[] wpState2Left  = new BufferedImage[4];
    public BufferedImage[] wpState2Right = new BufferedImage[4];

    // State 3 (4 frame per arah)
    public BufferedImage[] wpState3Up    = new BufferedImage[4];
    public BufferedImage[] wpState3Down  = new BufferedImage[4];
    public BufferedImage[] wpState3Left  = new BufferedImage[4];
    public BufferedImage[] wpState3Right = new BufferedImage[4];

    // State 4 (4 frame per arah)
    public BufferedImage[] wpState4Up    = new BufferedImage[4];
    public BufferedImage[] wpState4Down  = new BufferedImage[4];
    public BufferedImage[] wpState4Left  = new BufferedImage[4];
    public BufferedImage[] wpState4Right = new BufferedImage[4];

    private Clip swordSfx1;
    private Clip swordSfx2;
    private Clip punchSfx;

    // Konstruktor lama (tetap ada untuk kompatibilitas)
    public Weapon(String name, String type) {
        this.name = name;
        this.type = type;
        
        // ===== SET DAMAGE BERDASARKAN TIPE SENJATA =====
        if (type.equalsIgnoreCase("BoneSword")) {
            this.damage = 100;
        } else if (type.equalsIgnoreCase("Sword")) {
            this.damage = 2500;
        } else if (type.equalsIgnoreCase("Axe") || type.equalsIgnoreCase("Kapak")) {
            this.damage = 20;
        } else {
            this.damage = 0; // Jaga-jaga jika tangan kosong / tipe tidak dikenal
        }
        
        loadWeaponImages();
        preloadSounds();
    }
    
    private BufferedImage readImageSafely(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[Weapon Error] Asset Hilang/Salah Nama: " + path);
            return null;
        }
        try {
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadWeaponImages() {
        System.out.println("[Weapon System] Memuat aset untuk: " + name + " (" + type + ")");

        for (int i = 0; i < 4; i++) {
            int frameNum = i + 1;

            if (type.equalsIgnoreCase("Sword")) {
                wpState1Up[i]    = readImageSafely("/Aset/SwordBack (" + frameNum + ").png");
                wpState1Down[i]  = readImageSafely("/Aset/SwordFront (" + frameNum + ").png");
                wpState1Left[i]  = readImageSafely("/Aset/SwordLeft (" + frameNum + ").png");
                wpState1Right[i] = readImageSafely("/Aset/SwordRight (" + frameNum + ").png");

                wpState2Up[i]    = readImageSafely("/Aset/SwingBack (" + frameNum + ").png");
                wpState2Down[i]  = readImageSafely("/Aset/SwingFront (" + frameNum + ").png");
                wpState2Left[i]  = readImageSafely("/Aset/SwingLeft (" + frameNum + ").png");
                wpState2Right[i] = readImageSafely("/Aset/SwingRight (" + frameNum + ").png");

                wpState3Up[i]    = readImageSafely("/Aset/BackSw (" + frameNum + ").png");
                wpState3Down[i]  = readImageSafely("/Aset/FrontSw (" + frameNum + ").png");
                wpState3Left[i]  = readImageSafely("/Aset/LeftSw (" + frameNum + ").png");
                wpState3Right[i] = readImageSafely("/Aset/RightSw (" + frameNum + ").png");

                wpState4Up[i]    = readImageSafely("/Aset/BackSw (" + (i + 5) + ").png");
                wpState4Down[i]  = readImageSafely("/Aset/FrontSw (" + (i + 5) + ").png");
                wpState4Left[i]  = readImageSafely("/Aset/LeftSw (" + (i + 5) + ").png");
                wpState4Right[i] = readImageSafely("/Aset/RightSw (" + (i + 5) + ").png");

            } else if (type.equalsIgnoreCase("Axe") || type.equalsIgnoreCase("Kapak")) {
                wpState1Up[i]    = readImageSafely("/Aset/BackAxeThrust (" + frameNum + ").png");
                wpState1Down[i]  = readImageSafely("/Aset/FrontAxeThrust (" + frameNum + ").png");
                wpState1Left[i]  = readImageSafely("/Aset/LeftAxeThrust (" + frameNum + ").png");
                wpState1Right[i] = readImageSafely("/Aset/RightAxeThrust (" + frameNum + ").png");

                wpState2Up[i]    = readImageSafely("/Aset/BackAxeBash (" + frameNum + ").png");
                wpState2Down[i]  = readImageSafely("/Aset/FrontAxeBash (" + frameNum + ").png");
                wpState2Left[i]  = readImageSafely("/Aset/LeftAxeBash (" + frameNum + ").png");
                wpState2Right[i] = readImageSafely("/Aset/RightAxeBash (" + frameNum + ").png");

                wpState3Up[i]    = readImageSafely("/Aset/BackAxeSwing (" + frameNum + ").png");
                wpState3Down[i]  = readImageSafely("/Aset/FrontAxeSwing (" + frameNum + ").png");
                wpState3Left[i]  = readImageSafely("/Aset/LeftAxeSwing (" + frameNum + ").png");
                wpState3Right[i] = readImageSafely("/Aset/RightAxeSwing (" + frameNum + ").png");

                wpState4Up[i]    = readImageSafely("/Aset/BackAxeSwing (" + (i + 5) + ").png");
                wpState4Down[i]  = readImageSafely("/Aset/FrontAxeSwing (" + (i + 5) + ").png");
                wpState4Left[i]  = readImageSafely("/Aset/LeftAxeSwing (" + (i + 5) + ").png");
                wpState4Right[i] = readImageSafely("/Aset/RightAxeSwing (" + (i + 5) + ").png");

            } else if (type.equalsIgnoreCase("BoneSword")) {
                // State 1: Thrust  — SkullSwFront/Back/Left/RightThrust (1)-(4)
                wpState1Up[i]    = readImageSafely("/Aset/SkullSwBackThrust (" + frameNum + ").png");
                wpState1Down[i]  = readImageSafely("/Aset/SkullSwFrontThrust (" + frameNum + ").png");
                wpState1Left[i]  = readImageSafely("/Aset/SkullSwLeftThrust (" + frameNum + ").png");
                wpState1Right[i] = readImageSafely("/Aset/SkullSwRightThrust (" + frameNum + ").png");

                // State 2: Bash   — SkullSwFront/Back/Left/RightBash (1)-(4)
                wpState2Up[i]    = readImageSafely("/Aset/SkullSwBackBash (" + frameNum + ").png");
                wpState2Down[i]  = readImageSafely("/Aset/SkullSwFrontBash (" + frameNum + ").png");
                wpState2Left[i]  = readImageSafely("/Aset/SkullSwLeftBash (" + frameNum + ").png");
                wpState2Right[i] = readImageSafely("/Aset/SkullSwRightBash (" + frameNum + ").png");

                // State 3: Swing  — SkullSwFront/Back/Left/RightSwing (1)-(4)
                wpState3Up[i]    = readImageSafely("/Aset/SkullSwBackSwing (" + frameNum + ").png");
                wpState3Down[i]  = readImageSafely("/Aset/SkullSwFrontSwing (" + frameNum + ").png");
                wpState3Left[i]  = readImageSafely("/Aset/SkullSwLeftSwing (" + frameNum + ").png");
                wpState3Right[i] = readImageSafely("/Aset/SkullSwRightSwing (" + frameNum + ").png");

                // State 4: Swing  — SkullSwFront/Back/Left/RightSwing (5)-(8)
                wpState4Up[i]    = readImageSafely("/Aset/SkullSwBackSwing (" + (i + 5) + ").png");
                wpState4Down[i]  = readImageSafely("/Aset/SkullSwFrontSwing (" + (i + 5) + ").png");
                wpState4Left[i]  = readImageSafely("/Aset/SkullSwLeftSwing (" + (i + 5) + ").png");
                wpState4Right[i] = readImageSafely("/Aset/SkullSwRightSwing (" + (i + 5) + ").png");
            }
        }
        System.out.println("[Weapon System] Selesai memuat tipe: " + type);
    }

    private void preloadSounds() {
        swordSfx1 = loadClip("/SFX/SwordSfx1.wav");
        swordSfx2 = loadClip("/SFX/SwordSfx2.wav");
        punchSfx  = loadClip("/SFX/freesound_community-punch-2-37333.wav");
    }

    private Clip loadClip(String path) {
        try {
            URL url = getClass().getResource(path);
            if (url == null) return null;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }

    private void playClip(Clip clip) {
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void playAttackSound(int attackType, boolean isWeaponEquipped) {
        if (!isWeaponEquipped) { playClip(punchSfx); return; }
        switch (attackType) {
            case 1: playClip(swordSfx1); break;
            case 2: playClip(swordSfx2); break;
            case 3:
            case 4: playClip(Math.random() < 0.5 ? swordSfx1 : swordSfx2); break;
        }
    }
}