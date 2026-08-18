package Entitas;

import Main.GamePanel;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * OrcSprite — memuat semua frame animasi Orc dari folder /Aset/Orc/
 *
 * Struktur file yang dibutuhkan (nama bebas di-adjust sesuai asset lo):
 *
 *   IDLE   (4  frame): orc_idle_1.png  – orc_idle_4.png
 *   WALK   (6  frame): orc_walk_1.png  – orc_walk_6.png
 *   ATTACK (7  frame): orc_attack_1.png – orc_attack_7.png
 *   DEAD   (1+ frame): orc_dead_1.png
 *
 * Semua asset disimpan di /Aset/Orc/
 */
public class OrcSprite {

    // ---------- IDLE ----------
    public BufferedImage idle[] = new BufferedImage[4];

    // ---------- WALK ----------
    public BufferedImage walkDown[]  = new BufferedImage[6];
    public BufferedImage walkUp[]    = new BufferedImage[6];
    public BufferedImage walkLeft[]  = new BufferedImage[6];
    public BufferedImage walkRight[] = new BufferedImage[6];

    // ---------- ATTACK ----------
    public BufferedImage attack[] = new BufferedImage[7];

    // ---------- DEAD ----------
    public BufferedImage dead[] = new BufferedImage[1];

    private GamePanel gp;

    public OrcSprite(GamePanel gp) {
        this.gp = gp;
        load();
    }

    private BufferedImage load(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("[OrcSprite] Gagal load: " + path);
            return null;
        }
    }

    private void load() {

        // ===== IDLE =====
        for (int i = 0; i < idle.length; i++) {
            idle[i] = load("/Aset/ogre-idle" + (i + 1) + ".png");
        }

        // ===== WALK =====
        // Asumsi sprite sheet walk sama untuk semua arah;
        // kalau punya sprite per-arah, ganti path-nya.
        for (int i = 0; i < walkDown.length; i++) {
            walkDown[i]  = load("/Aset/ogre-walk"  + (i + 1) + ".png");
            walkUp[i]    = load("/Aset/ogre-walk"    + (i + 1) + ".png");
            walkLeft[i]  = load("/Aset/ogre-walk"  + (i + 1) + ".png");
            walkRight[i] = load("/Aset/ogre-walk" + (i + 1) + ".png");
        }

        // ===== ATTACK =====
        for (int i = 0; i < attack.length; i++) {
            attack[i] = load("/Aset/ogre-attack" + (i + 1) + ".png");
        }

        // ===== DEAD =====
        for (int i = 0; i < dead.length; i++) {
            dead[i] = load("/Aset/Orc/orc_dead_" + (i + 1) + ".png");
        }
    }
}