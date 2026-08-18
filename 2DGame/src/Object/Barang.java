package Object;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import Main.GamePanel;

public class Barang {

    public BufferedImage image;
    public int worldX, worldY;
    public int width, height; // 🔥 ukuran custom
    public Rectangle solidArea;

    public boolean alive = true;

    public int spawnX, spawnY;
    public long deadTime = 0;
    public int respawnDelay = 8000; // ms

    public void draw(Graphics2D g2, GamePanel gp) {

        int screenX = worldX - gp.player.x + gp.player.screenX;
        int screenY = worldY - gp.player.y + gp.player.screenY;

        if (worldX + gp.tileSize > gp.player.x - gp.player.screenX &&
            worldX - gp.tileSize < gp.player.x + gp.player.screenX &&
            worldY + gp.tileSize > gp.player.y - gp.player.screenY &&
            worldY - gp.tileSize < gp.player.y + gp.player.screenY) {

            g2.drawImage(image, screenX,screenY, gp.tileSize, gp.tileSize, null);
        }
    }
    
    public void drawInterior(Graphics2D g2, GamePanel gp) {

        int screenX = worldX - gp.player.x + gp.player.screenX;
        int screenY = worldY - gp.player.y + gp.player.screenY;

        g2.drawImage(
            image,
            screenX,
            screenY,
            width,
            height,
            null
        );
    }



}
