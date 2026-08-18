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

    public SkillManager(GamePanel gp, player owner) {
        this.gp = gp;
        this.owner = owner;

        skillZ = new SkillZ(gp, owner);
        skillX = new SkillX(gp, owner);
        skillC = new SkillC(gp, owner);

        skills.add(skillZ);
        skills.add(skillX);
        skills.add(skillC);
    }

    // ===== UPDATE SEMUA SKILL =====
    public void update() {
        skillZ.update();
        skillX.update();
        skillC.update(); // SkillC punya update sendiri (toggle logic)
    }

    // ===== DRAW SEMUA SKILL =====
    public void draw(Graphics2D g2) {
        for (Skill s : skills) {
            s.draw(g2);
        }
    }

    public void activateZ() { skillZ.activate(); }
    public void activateX() { skillX.activate(); }
    public void toggleC()   { skillC.toggle(); }
}