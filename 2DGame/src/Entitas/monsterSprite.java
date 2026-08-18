package Entitas;

import java.awt.image.BufferedImage;
import Main.GamePanel;

public class monsterSprite {

    public BufferedImage
        mfront1, mfront2,
        mback1, mback2,
        mleft1, mleft2,
        mright1, mright2,
        mstand, mattack1, mattack2, mattack3, mattack4, mattack5, mattack6, mattack7;

    // 🔥 ATTACK 7 FRAME
    public BufferedImage[] mattack = new BufferedImage[7];

    public monsterSprite(GamePanel gp) {

        mfront1 = gp.setup("Sticklooter_carrot_walk1");
        mfront2 = gp.setup("Sticklooter_carrot_walk2");

        mback1  = gp.setup("Sticklooter_carrot_walk2");
        mback2  = gp.setup("Sticklooter_carrot_walk1");

        mleft1  = gp.setup("Sticklooter_carrot_walk1");
        mleft2  = gp.setup("Sticklooter_carrot_walk2");

        mright1 = gp.setup("Sticklooter_carrot_walk1");
        mright2 = gp.setup("Sticklooter_carrot_walk2");

        mstand  = gp.setup("Sticklooter_carrot_idle1");

        // ===== ATTACK ANIMATION =====
        mattack[0] = gp.setup("slimeatt1");
        mattack[1] = gp.setup("slimeatt2");
        mattack[2] = gp.setup("slimeatt3");
        mattack[3] = gp.setup("slimeatt4");
        mattack[4] = gp.setup("slimeatt5");
        mattack[5] = gp.setup("slimeatt6");
        mattack[6] = gp.setup("slimeatt7");
    }
}