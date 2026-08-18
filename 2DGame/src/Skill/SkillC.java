package Skill;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Entitas.Entity;
import Entitas.player;
import Main.GamePanel;

public class SkillC extends Skill {

    // ===== TOGGLE & STATE =====
    public boolean auraOn = false;
    private final Random rand = new Random();

    // ===== ENERGY =====
    public int energy = 200;
    public int maxEnergy = 200;
    private final int energyDrain = 1;
    private final int energyRegen = 2;

    // ===== DAMAGE TICK =====
    private int damageTickTimer = 0;
    private final int damageTickInterval = 20; // Sedikit lebih cepat
    private final int auraDamage = 8;
    private final float auraRadius = 90f;

    // ===== FLAME PARTICLE =====
    private static class Flame {
        float x, y, vx, vy;
        float width, height;
        float alpha;
        int life, maxLife;
        int colorType; // 0: Deep, 1: Mid, 2: Light/White

        Flame(float startX, float startY, Random r) {
            // Posisi awal di sekitar kaki dengan sedikit variasi
            x = startX + (r.nextFloat() - 0.5f) * 30f;
            y = startY - r.nextFloat() * 5f;
            
            // Kecepatan: naik ke atas dengan sedikit goyangan horizontal
            vy = -(1.2f + r.nextFloat() * 2.0f);
            vx = (r.nextFloat() - 0.5f) * 0.8f;
            
            width = 8f + r.nextFloat() * 12f;
            height = 15f + r.nextFloat() * 25f;
            
            maxLife = 20 + r.nextInt(15);
            life = 0;
            alpha = 0.6f + r.nextFloat() * 0.4f;
            
            // Tentukan warna berdasarkan ukuran (yang kecil biasanya lebih panas/putih)
            if (width < 10f) colorType = 2;
            else if (width < 15f) colorType = 1;
            else colorType = 0;
        }

        void update() {
            x += vx;
            y += vy;
            // Tambahkan efek "angin" atau goyangan api
            vx += (Math.random() - 0.5) * 0.15f;
            
            life++;
            float progress = (float) life / maxLife;
            
            // Api mengecil saat mati
            width *= 0.96f;
            height *= 0.94f;
            alpha = Math.max(0, 1.0f - progress);
        }

        boolean isDead() { return life >= maxLife || alpha <= 0.05f || width < 1f; }
    }

    // ===== SIDE WISP (Percikan Api Kecil) =====
    private static class Wisp {
        float x, y, vx, vy, alpha, size;
        int life, maxLife;

        Wisp(float cx, float cy, Random r) {
            x = cx + (r.nextFloat() - 0.5f) * 40f;
            y = cy - r.nextFloat() * 20f;
            vx = (r.nextFloat() - 0.5f) * 1.5f;
            vy = -(0.5f + r.nextFloat() * 1.0f);
            size = 3f + r.nextFloat() * 5f;
            maxLife = 20 + r.nextInt(20);
            life = 0;
            alpha = 0.8f;
        }

        void update() {
            x += vx; y += vy;
            life++;
            alpha = 1f - (float) life / maxLife;
            size *= 0.97f;
        }

        boolean isDead() { return life >= maxLife; }
    }

    private final List<Flame> flames = new ArrayList<>();
    private final List<Wisp> wisps = new ArrayList<>();
    private int flameSpawnTimer = 0;
    private int wispSpawnTimer = 0;

    // Warna Api Biru
    private final Color blueDeep  = new Color(0, 50, 200);
    private final Color blueMid   = new Color(50, 130, 255);
    private final Color blueLight = new Color(130, 210, 255);
    private final Color whiteCore = new Color(220, 240, 255);

    private float flickerAlpha = 0.15f;
    private float flickerDir = 0.015f;

    public SkillC(GamePanel gp, player owner) {
        super(gp, owner);
        cooldownMax = 20;
    }

    public void toggle() {
        if (auraOn) turnOff(); else turnOn();
    }

    private void turnOn() {
        if (cooldown > 0 || energy <= 0) return;
        auraOn = true; isActive = true; cooldown = cooldownMax;
    }

    private void turnOff() {
        auraOn = false; isActive = false; cooldown = cooldownMax;
    }

    @Override
    public void update() {
        if (cooldown > 0) cooldown--;

        if (auraOn) {
            energy -= energyDrain;
            if (energy <= 0) { energy = 0; turnOff(); return; }

            // Efek denyut aura (flicker)
            flickerAlpha += flickerDir;
            if (flickerAlpha > 0.4f || flickerAlpha < 0.15f) flickerDir = -flickerDir;

            // Spawn partikel
            flameSpawnTimer++;
            if (flameSpawnTimer >= 2) {
                int feetX = owner.screenX + gp.tileSize / 2;
                int feetY = owner.screenY + gp.tileSize - 5;
                flames.add(new Flame(feetX, feetY, rand));
                flameSpawnTimer = 0;
            }

            wispSpawnTimer++;
            if (wispSpawnTimer >= 5) {
                int centerX = owner.screenX + gp.tileSize / 2;
                int centerY = owner.screenY + gp.tileSize / 2;
                wisps.add(new Wisp(centerX, centerY, rand));
                wispSpawnTimer = 0;
            }

            damageTickTimer++;
            if (damageTickTimer >= damageTickInterval) {
                applyDamage();
                damageTickTimer = 0;
            }
        } else {
            if (energy < maxEnergy) energy = Math.min(maxEnergy, energy + energyRegen);
            flickerAlpha = 0f;
        }

        // Update list
        flames.removeIf(Flame::isDead);
        wisps.removeIf(Wisp::isDead);
        flames.forEach(Flame::update);
        wisps.forEach(Wisp::update);
    }

    private void applyDamage() {
        if (gp.monsters == null) return;
        int cx = owner.x + owner.solidArea.x + owner.solidArea.width / 2;
        int cy = owner.y + owner.solidArea.y + owner.solidArea.height / 2;
        for (Entity m : gp.monsters) {
            if (m == null || m.isDead) continue;
            int mx = m.x + m.solidArea.x + m.solidArea.width / 2;
            int my = m.y + m.solidArea.y + m.solidArea.height / 2;
            if (Math.hypot(mx - cx, my - cy) <= auraRadius) {
                m.takeDamage(auraDamage, owner);
            }
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        drawEnergyBar(g2);
        if (!auraOn && flames.isEmpty() && wisps.isEmpty()) return;
        onDraw(g2);
    }

    @Override
    protected void onDraw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Composite oldComp = g2.getComposite();

        int px = owner.screenX + gp.tileSize / 2;
        int py = owner.screenY + gp.tileSize / 2;

        // 1. BASE GLOW (Cahaya di lantai & sekitar karakter)
        if (auraOn) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flickerAlpha));
            g2.setColor(blueMid);
            g2.fillOval(px - 40, py - 40, 80, 80); // Glow luar
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flickerAlpha * 1.5f));
            g2.fillOval(px - 20, owner.screenY + gp.tileSize - 10, 40, 15); // Cahaya di kaki
        }

        // 2. DRAW FLAMES (Lidah Api)
        for (Flame f : flames) {
            // Kita gambar 2 lapis per partikel untuk efek "Soft Glow"
            // Lapis 1: Cahaya Luar (Lebih besar, lebih transparan)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f.alpha * 0.4f));
            g2.setColor(f.colorType == 0 ? blueDeep : blueMid);
            g2.fillRoundRect((int)(f.x - f.width), (int)(f.y - f.height), (int)f.width * 2, (int)f.height, (int)f.width, (int)f.width);

            // Lapis 2: Core/Inti (Lebih kecil, lebih terang)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f.alpha));
            Color coreCol;
            switch(f.colorType) {
                case 2: coreCol = whiteCore; break;
                case 1: coreCol = blueLight; break;
                default: coreCol = blueMid; break;
            }
            g2.setColor(coreCol);
            int coreW = (int)(f.width * 0.6f);
            int coreH = (int)(f.height * 0.8f);
            g2.fillRoundRect((int)(f.x - coreW/2), (int)(f.y - coreH), coreW, coreH, coreW, coreW);
        }

        // 3. DRAW WISPS (Percikan)
        for (Wisp w : wisps) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, w.alpha));
            g2.setColor(whiteCore);
            g2.fillOval((int)w.x, (int)w.y, (int)w.size, (int)w.size);
        }

        g2.setComposite(oldComp);
    }

    private void drawEnergyBar(Graphics2D g2) {
        int barW = gp.tileSize, barH = 4;
        int bx = owner.screenX;
        int by = owner.screenY + gp.tileSize + 12;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(bx, by, barW, barH);

        float pct = (float) energy / maxEnergy;
        g2.setColor(auraOn ? blueLight : blueDeep);
        g2.fillRect(bx, by, (int)(barW * pct), barH);
    }

    // Metode kosong sesuai struktur superclass
    @Override protected void onActivate() {}
    @Override protected void onActiveUpdate() {}
    @Override protected void onEnd() {}
}