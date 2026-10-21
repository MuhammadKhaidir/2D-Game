package Entitas;

import java.awt.image.BufferedImage;
import Main.GamePanel;

public class monsterSprite {

    public static final int BASE_SIZE = 32;

    public static final int IDLE_FRAMES   = 8;
    public static final int ATTACK_FRAMES = 12;
    public static final int WALK_FRAMES   = 6;
    public static final int HIT_FRAMES    = 7;
    public static final int DEATH_FRAMES  = 10;

    private static final String[] DIRS = {
        "up", "down", "left", "right"
    };

    private static BufferedImage[][] idleImgs;
    private static BufferedImage[][] attackImgs;
    private static BufferedImage[][] walkImgs;
    private static BufferedImage[][] hitImgs;
    private static BufferedImage[][] deathImgs;

    public monsterSprite(GamePanel gp) {

        // Semua animasi sudah dimuat? Tidak perlu memuat ulang.
        if (idleImgs != null &&
            attackImgs != null &&
            walkImgs != null &&
            hitImgs != null &&
            deathImgs != null) {
            return;
        }

        idleImgs   = load(gp, "idle",   IDLE_FRAMES);
        attackImgs = load(gp, "attack", ATTACK_FRAMES);
        walkImgs   = load(gp, "walk",   WALK_FRAMES);
        hitImgs    = load(gp, "hit",    HIT_FRAMES);
        deathImgs  = load(gp, "death",  DEATH_FRAMES);
    }

    // Format:
    // slime_idle_down_1.png sampai slime_idle_down_8.png
    // Berlaku juga untuk up, left, right, dan animasi lainnya.
    private static BufferedImage[][] load(
        GamePanel gp,
        String action,
        int frames
    ) {
        BufferedImage[][] set =
            new BufferedImage[DIRS.length][frames];

        for (int d = 0; d < DIRS.length; d++) {
            for (int f = 0; f < frames; f++) {

                String fileName =
                    "slime_" + action + "_" +
                    DIRS[d] + "_" + (f + 1);

                set[d][f] = gp.setup(fileName);
            }
        }

        return set;
    }

    private static int dirIndex(String dir) {
        switch (dir) {
            case "up":
                return 0;

            case "down":
                return 1;

            case "left":
                return 2;

            case "right":
                return 3;

            default:
                return 1;
        }
    }

    public BufferedImage getIdle(String dir, int frame) {
        return idleImgs[dirIndex(dir)][frame];
    }

    public BufferedImage getAttack(String dir, int frame) {
        return attackImgs[dirIndex(dir)][frame];
    }

    public BufferedImage getWalk(String dir, int frame) {
        return walkImgs[dirIndex(dir)][frame];
    }

    public BufferedImage getHit(String dir, int frame) {
        return hitImgs[dirIndex(dir)][frame];
    }

    public BufferedImage getDeath(String dir, int frame) {
        return deathImgs[dirIndex(dir)][frame];
    }
}