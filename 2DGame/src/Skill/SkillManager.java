package Skill;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import Entitas.player;
import Main.GamePanel;

public class SkillManager {

    private GamePanel gp;
    private player owner;

    // ===== DAFTAR SKILL =====
    public List<Skill> skills = new ArrayList<>();

    // Shortcut langsung ke skill spesifik
    public SkillZ skillZ;
    public SkillX skillX;
    public SkillC skillC;
    public SkillV skillV;
    public SkillMaw skillMaw; // Maw of the Forgotten (Leviathan Fang)

    public SkillManager(GamePanel gp, player owner) {
        this.gp = gp;
        this.owner = owner;

        skillZ = new SkillZ(gp, owner);
        skillX = new SkillX(gp, owner);
        skillC = new SkillC(gp, owner);
        skillV = new SkillV(gp, owner);
        skillMaw = new SkillMaw(gp, owner);

        skills.add(skillZ);
        skills.add(skillX);
        skills.add(skillC);
        skills.add(skillV);
        skills.add(skillMaw);
    }

    // ===== UPDATE SEMUA SKILL =====
    public void update() {
        skillZ.update();
        skillX.update();
        skillC.update(); // SkillC punya update sendiri (toggle logic)
        skillV.update();
        skillMaw.update();
    }

    // ===== DRAW SEMUA SKILL =====
    public void draw(Graphics2D g2) {
        for (Skill s : skills) {
            s.draw(g2);
        }
    }

    // ===== OVERLAY LAYAR PENUH (dipanggil GamePanel.paintComponent, dekat drawDarkOverlay) =====
    public void drawOverlay(Graphics2D g2) {
        skillV.drawOverlay(g2);
    }

    // SkillV lagi jalan? (input & interaksi dikunci)
    public boolean isSkillVActive() { return skillV.isActive; }

    // SkillV lagi nge-freeze dunia? (monster, NPC, minion berhenti)
    public boolean isWorldFrozen()  { return skillV.isWorldFrozen(); }

    public void activateZ() { skillZ.activate(); }
    public void activateX() { skillX.activate(); }
    public void toggleC()   { skillC.toggle(); }
    public void activateV() { skillV.activate(); }
    public void activateMaw() { skillMaw.activate(); } // cuma jalan kalau Leviathan Fang dipegang
}