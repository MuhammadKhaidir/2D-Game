package Main;

import Entitas.Entity;
import Entitas.NPC;
import Object.ObjKey;

public class AsetSetter2 {
	GamePanel gp;
	
	public AsetSetter2(GamePanel gp) {
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
			gp.npc[1].y = gp.tileSize * 22;
			gp.npc[1].name = "NPC 2";

			gp.npc[2] = new NPC(gp, "kid");
			gp.npc[2].x = gp.tileSize * 18;
			gp.npc[2].y = gp.tileSize * 24;
			gp.npc[2].name = "NPC 3";
			
			

		}
		
		public void setNPCDARK() {
			gp.npc[3] = new NPC(gp, "Corrupt NPC");
			gp.npc[3].x = gp.tileSize * 20;
			gp.npc[3].y = gp.tileSize * 21;
			gp.npc[3].name = "Kael";
		}

}
