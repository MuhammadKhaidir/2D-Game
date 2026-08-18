package Entitas;

import java.io.IOException;
import javax.imageio.ImageIO;
import Main.GamePanel;

public class NPCLow extends Entity {

    public NPCLow(GamePanel gp) {
        super(gp);

        name = "Kael";
        speed = 2;                  // ❗ DIAM
        npcState = NPCState.STAY;   // ❗ GAK GERAK
        direction = "down";
        imgNum = 1;

        try {
   		 KaelFront1 = ImageIO.read(getClass().getResource("/Aset/Kael-Front1.png"));//14
   		 KaelFront2 = ImageIO.read(getClass().getResource("/Aset/Kael-Front2.png"));//15
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAction() {
        // idle animation doang
        imgCount++;
        if (imgCount > 10) {
            imgNum = (imgNum == 1) ? 2 : 1;
            imgCount = 0;
        }
    }
}
