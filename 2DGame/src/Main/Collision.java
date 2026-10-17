package Main;

import java.awt.Rectangle;

import Entitas.Entity;

public class Collision {

    GamePanel gp;

    public Collision(GamePanel gp) {
        this.gp = gp;
    }
    public void checkTile(Entity entity) {
        // logic collision tile
    }
    public void checkObject(Entity entity) {
        // collision doang, tanpa return index
    }

    public boolean checkMonster(Entity a, Entity b, int dx, int dy) {
    	
    	
    	
		return false;
    	
    }
    
 // Taruh kode ini di dalam class Collision kamu
    public void checkPlayer(Entity entity) {
        // Dapatkan area solid dari entity yang mengecek (misal: Skeleton)
        entity.solidArea.x = entity.x + entity.solidArea.x;
        entity.solidArea.y = entity.y + entity.solidArea.y;

        // Dapatkan area solid dari Player
        gp.player.solidArea.x = gp.player.x + gp.player.solidArea.x;
        gp.player.solidArea.y = gp.player.y + gp.player.solidArea.y;

        // Cek arah pergerakan entity untuk memprediksi tabrakan
        switch(entity.direction) {
            case "up":
                entity.solidArea.y -= entity.speed;
                if(entity.solidArea.intersects(gp.player.solidArea)) {
                    entity.collisionOn = true;
                }
                break;
            case "down":
                entity.solidArea.y += entity.speed;
                if(entity.solidArea.intersects(gp.player.solidArea)) {
                    entity.collisionOn = true;
                }
                break;
            case "left":
                entity.solidArea.x -= entity.speed;
                if(entity.solidArea.intersects(gp.player.solidArea)) {
                    entity.collisionOn = true;
                }
                break;
            case "right":
                entity.solidArea.x += entity.speed;
                if(entity.solidArea.intersects(gp.player.solidArea)) {
                    entity.collisionOn = true;
                }
                break;
        }

        // Reset kembali ke posisi default area solid masing-masing
        entity.solidArea.x = entity.solidAreaDefaultX;
        entity.solidArea.y = entity.solidAreaDefaultY;
        gp.player.solidArea.x = gp.player.solidAreaDefaultX;
        gp.player.solidArea.y = gp.player.solidAreaDefaultY;
    }
    
    //collision biar player sm npc dak tabrakan
    public boolean checkEntity(Entity a, Entity b, int dx, int dy) {

        Rectangle aBox = new Rectangle(
            a.x + a.solidArea.x + dx,
            a.y + a.solidArea.y + dy,
            a.solidArea.width,
            a.solidArea.height
        );

        Rectangle bBox = new Rectangle(
            b.x + b.solidArea.x,
            b.y + b.solidArea.y,
            b.solidArea.width,
            b.solidArea.height
        );

        if (aBox.intersects(bBox)) {
            a.collisionOn = true;
            return true; // 🔥 PENTING
        }

        return false;
    }


    /**
     * True kalau hitbox entity, andai berdiri di (worldX, worldY), nabrak collision DUNIA
     * (object di luar / tembok rumah / tembok domain, sesuai gameState saat ini).
     * Aturannya sama kayak cek jalan biasa di player.update(). Entity (monster/NPC) sengaja
     * gak ikut dicek di sini. Dipakai buat skill gerak cepat kayak dash Z.
     */
    public boolean isBlockedByWorld(Entity e, int worldX, int worldY) {
        Rectangle box = new Rectangle(
            worldX + e.solidArea.x,
            worldY + e.solidArea.y,
            e.solidArea.width,
            e.solidArea.height
        );

        switch (gp.gameState) {
            case INSIDE:
                return gp.houseManager != null && gp.houseManager.checkCollision(box);
            case DOMAIN:
                return gp.domainManager != null && gp.domainManager.checkCollision(box);
            default: // OUTSIDE (dan DIALOG)
                for (Rectangle obj : gp.objCol.objects) {
                    if (box.intersects(obj)) return true;
                }
                return false;
        }
    }

    public void checkObject(Entity e, int dx, int dy) {

        Rectangle futureArea = new Rectangle(
            e.x + dx + e.solidArea.x,
            e.y + dy + e.solidArea.y,
            e.solidArea.width,
            e.solidArea.height
        );

        for (Rectangle obj : gp.objCol.objects) {
            if (futureArea.intersects(obj)) {
                e.collisionOn = true;
                return;
            }
        }
    }
    /*
    public int checkObj2(Entity entity, boolean player) {
    	
    	int index = 999;
    	
    	for (int i = 0; i < gp.obj.length; i++) {
    		if(gp.obj[i] != null) {
    			entity.solidArea.x = entity.x + entity.solidArea.x;
    			entity.solidArea.y = entity.y + entity.solidArea.y;
    			
    			gp.obj[i].solidArea.x = gp.obj[i].worldX + gp.obj[i].solidArea.x;
    			gp.obj[i].solidArea.y = gp.obj[i].worldY + gp.obj[i].solidArea.y;
    			
    			switch(entity.direction) {
    			case "up":
    				entity.solidArea.y -= entity.speed;
    				if(entity.solidArea.intersects(gp.obj[i].solidArea)) {
    					
    				}
    				break;
    			case "down":
    				entity.solidArea.y += entity.speed;
    				break;
    			case "left":
    				entity.solidArea.x -= entity.speed;
    				break;
    			case "right":
    				entity.solidArea.x += entity.speed;
    				break;
    			}
    		}
    	}
    	return index;
    }
*/
}
