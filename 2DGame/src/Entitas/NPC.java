package Entitas;

import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import Main.GamePanel;
import pathfinding.Pathfinder;

public class NPC extends Entity{
	
	public NPC (GamePanel gp, String dialogId) {
		super(gp);
		 this.dialogId = dialogId;
		  	direction = "down";
	speed = 1;
	
	getImage();
	}
	public void die() {
	    isDead = true;
	    respawnTimer = 0;

	    // MATIKAN SEMUA
	    collisionOn = false;
	    isAngry = false;
	}

	@Override
	public void onDeath() {
	    npcState = NPCState.STAY;
	    collisionOn = true;
	}

	public void getImage() {
	    try {
	        front1     = ImageIO.read(getClass().getResourceAsStream("/Aset/Front-1.png"));
	        front2     = ImageIO.read(getClass().getResourceAsStream("/Aset/Front-2.png"));
	        stand      = ImageIO.read(getClass().getResourceAsStream("/Aset/Stand.png"));
	        stand2     = ImageIO.read(getClass().getResourceAsStream("/Aset/Stand2.png"));
	        back1      = ImageIO.read(getClass().getResourceAsStream("/Aset/Back2.png"));
	        back2      = ImageIO.read(getClass().getResourceAsStream("/Aset/Back1.png"));
	        standback  = ImageIO.read(getClass().getResourceAsStream("/Aset/Stand-Back.png"));
	        standback2 = ImageIO.read(getClass().getResourceAsStream("/Aset/Stand-Back2.png"));
	        left1      = ImageIO.read(getClass().getResourceAsStream("/Aset/left1.png"));
	        left2      = ImageIO.read(getClass().getResourceAsStream("/Aset/left2.png"));
	        standleft  = ImageIO.read(getClass().getResourceAsStream("/Aset/leftstand.png"));
	        standleft2 = ImageIO.read(getClass().getResourceAsStream("/Aset/leftstand2.png"));
	        right1     = ImageIO.read(getClass().getResourceAsStream("/Aset/right2.png"));
	        right2     = ImageIO.read(getClass().getResourceAsStream("/Aset/right1.png"));
	        standright = ImageIO.read(getClass().getResourceAsStream("/Aset/rightstand.png"));
	        standright2= ImageIO.read(getClass().getResourceAsStream("/Aset/rightstand2.png"));
	        hi         = ImageIO.read(getClass().getResourceAsStream("/Aset/Hi!!.png")); // ← yang udah ada

	        // ← TAMBAHIN DI SINI, tepat dibawah baris hi
	        punchUp    = ImageIO.read(getClass().getResourceAsStream("/Aset/rightpunch1.png"));
	        punchDown  = ImageIO.read(getClass().getResourceAsStream("/Aset/Punch1.png"));
	        punchLeft  = ImageIO.read(getClass().getResourceAsStream("/Aset/leftpunch1.png"));
	        punchRight = ImageIO.read(getClass().getResourceAsStream("/Aset/rightpunch1.png"));
	        punchDown2 = ImageIO.read(getClass().getResourceAsStream("/Aset/punch2.png"));
	        punchLeft2 = ImageIO.read(getClass().getResourceAsStream("/Aset/leftpunch2.png"));
	        punchRight2= ImageIO.read(getClass().getResourceAsStream("/Aset/rigtpunch2.png"));

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}



public void setAction() {

    actionLockCounter++;

    if (actionLockCounter >= 120) { // ganti arah tiap 2 detik
        Random random = new Random();
        int i = random.nextInt(100) + 1;

        if (i <= 25) direction = "up";
        else if (i <= 50) direction = "down";
        else if (i <= 75) direction = "right";
        else if (i <= 100) direction = "left";

        actionLockCounter = 0;
    }
}



	
}

