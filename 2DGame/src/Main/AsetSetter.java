package Main;

import Entitas.Entity;
import Entitas.NPC;
import Entitas.NPCLow;
import Entitas.monster;
import Object.ObjKey;

public class AsetSetter {
	GamePanel gp;
	
	public AsetSetter(GamePanel gp) {
		this.gp = gp;
	}

	public void setObject() {
		gp.obj[0] = new ObjKey();
		gp.obj[0].worldX = 23 * gp.tileSize;
		gp.obj[0].worldY = 7 * gp.tileSize;
		
		gp.obj[1] = new ObjKey();
		gp.obj[1].worldX = 23 * gp.tileSize;
		gp.obj[1].worldX = 40 * gp.tileSize;
	}
		public void setNPC() {
			gp.npc[0] = new NPC(gp, "guard");
			gp.npc[0].x = gp.tileSize * 15;
			gp.npc[0].y = gp.tileSize * 20;
			gp.npc[0].name = "NPC 1";

			gp.npc[1] = new NPC(gp, "mystic");
			gp.npc[1].x = gp.tileSize * 16;
			gp.npc[1].y = gp.tileSize * 20;
			gp.npc[1].name = "NPC 2";
			
			gp.npc[2] = new NPC(gp, "kid");
			gp.npc[2].x = gp.tileSize * 22;
			gp.npc[2].y = gp.tileSize * 21;
			gp.npc[2].name = "NPC 3";

			gp.npc[3] = new NPCLow(gp);
			gp.npc[3].x = gp.tileSize * 10;
			gp.npc[3].y = gp.tileSize * 12;
			gp.npc[3].name = "Kael";
			
		}
		
		public void setMonster() {
		    if (gp.gameState != GamePanel.GameState.OUTSIDE) return;
		    gp.monsters[0] = new monster(gp);
		    gp.monsters[0].x = gp.tileSize * 20;
		    gp.monsters[0].y = gp.tileSize * 58;
		    
		    gp.monsters[1] = new monster(gp);
		    gp.monsters[1].x = gp.tileSize * 25;
		    gp.monsters[1].y = gp.tileSize * 60;
		    
		    gp.monsters[2] = new monster(gp);
		    gp.monsters[2].x = gp.tileSize * 22;
		    gp.monsters[2].y = gp.tileSize * 65;
		    
		    gp.monsters[3] = new monster(gp);
		    gp.monsters[3].x = gp.tileSize * 40;
		    gp.monsters[3].y = gp.tileSize * 50;
		}
		
}
