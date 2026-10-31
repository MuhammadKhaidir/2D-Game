package Skill;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

import Entitas.Entity;
import Entitas.player;
import Main.GamePanel;

public class SkillX extends Skill {

    private static class AuraWave {
        float radius;
        float maxRadius;
        float alpha;
        int damage;
        List<Entity> hitTargets = new ArrayList<>();

        AuraWave(float startRadius, float maxRadius, float alpha, int damage) {
            this.radius = startRadius;
            this.maxRadius = maxRadius;
            this.alpha = alpha;
            this.damage = damage;
        }

        boolean isDone() { return radius >= maxRadius || alpha <= 0; }
    }

    private List<AuraWave> waves = new ArrayList<>();
    
    // Setting untuk gelombang tunggal
    private float waveMaxRadius = 180f;   
    private float waveSpeed = 8.0f;       
    private int skillDamage = 25;
    private int knockbackForce = 15;      

    // Warna Sonic
    private Color sonicColor = new Color(255, 255, 255);
    private Color distortionColor = new Color(220, 220, 220, 80);

    public SkillX(GamePanel gp, player owner) {
        super(gp, owner);
        cooldownMax = 120; 
        activeDuration = 30;
    }

    @Override
    protected void onActivate() {
        waves.clear();
        // Spawn satu gelombang utama
        waves.add(new AuraWave(10f, waveMaxRadius, 1.0f, skillDamage));
    }

    @Override
    protected void onActiveUpdate() {
        // Logika update gelombang dipindah ke sini sepenuhnya
        List<AuraWave> toRemove = new ArrayList<>();
        
        for (AuraWave w : waves) {
            w.radius += waveSpeed;
            w.alpha -= 0.035f; 

            checkHit(w);
            if (w.isDone()) toRemove.add(w);
        }
        waves.removeAll(toRemove);
        
        // Matikan skill jika gelombang sudah habis
        if (waves.isEmpty()) {
            isActive = false;
        }
    }

    private void checkHit(AuraWave w) {
        if (gp.outsideMonsters() == null) return;

        int px = owner.x + owner.solidArea.x + owner.solidArea.width / 2;
        int py = owner.y + owner.solidArea.y + owner.solidArea.height / 2;

        for (Entity m : gp.outsideMonsters()) {
            if (m == null || m.isDead) continue;
            if (w.hitTargets.contains(m)) continue;

            int mx = m.x + m.solidArea.x + m.solidArea.width / 2;
            int my = m.y + m.solidArea.y + m.solidArea.height / 2;

            double dist = Math.hypot(mx - px, my - py);

            if (dist <= w.radius && dist >= w.radius - (waveSpeed + 15)) {
                m.takeDamage(w.damage, owner);
                applyRadialKnockback(m, px, py, mx, my);
                w.hitTargets.add(m);
            }
        }
    }

    private void applyRadialKnockback(Entity m, int px, int py, int mx, int my) {
        double diffX = mx - px;
        double diffY = my - py;
        double distance = Math.hypot(diffX, diffY);

        if (distance > 0) {
            m.x += (int) ((diffX / distance) * knockbackForce * 4);
            m.y += (int) ((diffY / distance) * knockbackForce * 4);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        // Tambahkan ini agar cooldown bar tetap muncul meski wave sudah hilang
        drawCooldownBar(g2);
        
        if (waves.isEmpty()) return;
        onDraw(g2);
    }

    @Override
    protected void onDraw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int px = owner.screenX + gp.tileSize / 2;
        int py = owner.screenY + gp.tileSize / 2;

        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        for (AuraWave w : waves) {
            float a = Math.max(0, w.alpha);
            int r = (int) w.radius;

            // 1. Distortion Shadow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.2f));
            g2.setColor(distortionColor);
            g2.setStroke(new BasicStroke(15f));
            g2.drawOval(px - r, py - r, r * 2, r * 2);

            // 2. Ring Sonic Utama
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.setColor(sonicColor);
            g2.setStroke(new BasicStroke(4f));
            g2.drawOval(px - r, py - r, r * 2, r * 2);

            // 3. Ring Dalam
            int rIn = r - 20;
            if (rIn > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.4f));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(px - rIn, py - rIn, rIn * 2, rIn * 2);
            }
        }

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
    }

    private void drawCooldownBar(Graphics2D g2) {
        int barW = gp.tileSize;
        int barH = 3;
        int bx = owner.screenX;
        int by = owner.screenY + gp.tileSize + 12;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(bx, by, barW, barH);

        float pct = getCooldownPercent();
        g2.setColor(Color.WHITE);
        g2.fillRect(bx, by, (int)(barW * pct), barH);
    }

    @Override protected void onEnd() {
        waves.clear();
    }
}