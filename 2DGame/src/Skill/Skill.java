package Skill;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import Entitas.player;
import Main.GamePanel;

public abstract class Skill {

    protected GamePanel gp;
    protected player owner;

    // ===== COOLDOWN =====
    public int cooldown = 0;
    public int cooldownMax = 60; // default 1.5 detik di 40 FPS

    // ===== DURASI AKTIF =====
    public int activeTimer = 0;
    public int activeDuration = 20; // berapa frame skill aktif

    public boolean isActive = false;

    // ===== GAMBAR SKILL =====
    public BufferedImage skillImage = null; // gambar besar yang muncul saat skill aktif
    public String imagePath = "";           // path ke aset, diisi di subclass

    public Skill(GamePanel gp, player owner) {
        this.gp = gp;
        this.owner = owner;
        loadImage();
    }

    // Load gambar skill — override di subclass kalau path berbeda
    protected void loadImage() {
        if (imagePath == null || imagePath.isEmpty()) return;
        try {
            skillImage = ImageIO.read(getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            System.out.println("Skill image not found: " + imagePath);
        }
    }

    // ===== DIPANGGIL TIAP FRAME =====
    public void update() {
        if (cooldown > 0) cooldown--;

        if (isActive) {
            activeTimer++;
            onActiveUpdate();

            if (activeTimer >= activeDuration) {
                isActive = false;
                activeTimer = 0;
                onEnd();
            }
        }
    }

    // ===== TRIGGER SKILL =====
    public void activate() {
        if (cooldown > 0 || isActive) return;

        isActive = true;
        activeTimer = 0;
        cooldown = cooldownMax;

        onActivate();
    }

    // ===== DRAW SKILL (efek visual) =====
    public void draw(Graphics2D g2) {
        if (!isActive) return;
        onDraw(g2);
    }

    // ===== HOOK METHODS — override di subclass =====
    protected abstract void onActivate();      // logika saat skill pertama aktif
    protected abstract void onActiveUpdate();  // logika tiap frame selama aktif
    protected abstract void onEnd();           // logika saat skill selesai
    protected abstract void onDraw(Graphics2D g2); // gambar efek skill

    // Helper: cek apakah sedang cooldown
    public boolean isReady() {
        return cooldown <= 0 && !isActive;
    }

    // Helper: persentase cooldown (untuk UI bar)
    public float getCooldownPercent() {
        if (cooldownMax == 0) return 1f;
        return 1f - ((float) cooldown / cooldownMax);
    }
}