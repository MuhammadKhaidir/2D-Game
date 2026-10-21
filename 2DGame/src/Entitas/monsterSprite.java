package Entitas;

import java.awt.image.BufferedImage;
import Main.GamePanel;

public class monsterSprite {

    // jumlah frame tiap animasi (per arah)
    public static final int ATTACK_FRAMES = 12;
    public static final int WALK_FRAMES   = 6;
    public static final int HIT_FRAMES    = 7;
    public static final int DEATH_FRAMES  = 10;

    private static final String[] DIRS = { "up", "down", "left", "right" };

    // [arah][frame] - static: dimuat sekali, dipakai semua slime
    private static BufferedImage[][] attackImgs, walkImgs, hitImgs, deathImgs;

    public monsterSprite(GamePanel gp) {
        if (attackImgs != null) return;

        attackImgs = load(gp, "attack", ATTACK_FRAMES);
        walkImgs   = load(gp, "walk",   WALK_FRAMES);
        hitImgs    = load(gp, "hit",    HIT_FRAMES);
        deathImgs  = load(gp, "death",  DEATH_FRAMES);
    }

    // nama file: slime_<aksi>_<arah>_<nomor>.png, contoh slime_attack_up_1.png
    private static BufferedImage[][] load(GamePanel gp, String action, int frames) {
        BufferedImage[][] set = new BufferedImage[DIRS.length][frames];
        for (int d = 0; d < DIRS.length; d++) {
            for (int f = 0; f < frames; f++) {
                set[d][f] = gp.setup("slime_" + action + "_" + DIRS[d] + "_" + (f + 1));
            }
        }
        return set;
    }

    private static int dirIndex(String dir) {
        switch (dir) {
            case "up":    return 0;
            case "left":  return 2;
            case "right": return 3;
            default:      return 1; // down
        }
    }

    public BufferedImage getAttack(String dir, int frame) { return attackImgs[dirIndex(dir)][frame]; }
    public BufferedImage getWalk(String dir, int frame)   { return walkImgs[dirIndex(dir)][frame]; }
    public BufferedImage getHit(String dir, int frame)    { return hitImgs[dirIndex(dir)][frame]; }
    public BufferedImage getDeath(String dir, int frame)  { return deathImgs[dirIndex(dir)][frame]; }
}
