package bahan;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import Entitas.player;
import Item.Item;
import Item.ItemManager;

import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import Main.GamePanel;
import Object.Barang;
import Object.breakable;
import World.AnimatedObjectManager;

public class TileM {
GamePanel gp;
public Tile []tile;
public int mapTileNum[][];
ImageIcon npcGif;
ImageIcon firecamp;
ImageIcon GrassGif, RockWater, RockWater1, RockWater2, herbShop;
int[][] grassGifs = {

	};

int npcWorldX = 250;
int npcWorldY = 250;
int[][] worldObjects;
public Barang[] interiorObj;
public boolean interiorLoaded = false;
public Barang obj[]= new Barang[300];
//INTERIOR MAP
public int interiorCol = 8;
public int interiorRow = 13;
public int[][] interiorMap;
public int interiorFloorTile = 12; // bebas, sesuaiin index tile lantai
//INTERIOR COLLISION
public Rectangle [] interiorCollision;
public breakable[] breakables;

ImageIcon interiorBg; // background ruangan interior

public AnimatedObjectManager animObjManager;

public BufferedImage setup(String imageName) {
    BufferedImage image = null;
    try {
        image = ImageIO.read(
            getClass().getResourceAsStream("/Aset/" + imageName + ".png") // ini dibuat biar pemanggilan cm pakai nama file
        );
    } catch (Exception e) {
        e.printStackTrace();
    }
    return image;
}

public TileM(GamePanel gp) {
    this.gp = gp;
    tile = new Tile[1000];
    mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];


    npcGif = new ImageIcon(
    	    getClass().getResource("/Entitas/__AttackCombo2hit.gif")
    	);

    	firecamp = new ImageIcon(
    	    getClass().getResource("/Aset/CampFire.gif")
    	);
    	
    	GrassGif = new ImageIcon (
    		getClass().getResource("/Aset/GrassGiff.gif")
    	);
    	RockWater = new ImageIcon (
    			getClass().getResource("/Aset/Rock7_water_shadow_dark2.gif"));
    	RockWater1 = new ImageIcon (
    			getClass().getResource("/Aset/Rock7_water_shadow_dark1.gif"));
    	RockWater2 = new ImageIcon (
    			getClass().getResource("/Aset/Rock7_water_shadow_dark3.gif"));
    	herbShop = new ImageIcon(
    			getClass().getResource("/Aset/mockup6.gif"));
    	interiorBg = new ImageIcon(
    		    getClass().getResource("/Aset/mockup6.gif")
    		);

    getTileImage();
    loadMap("/Aset/map01.txt");
    loadObjectCollision();

    loadBreakableObjects();
    
    animObjManager = new AnimatedObjectManager(getClass());
}

	public void getTileImage() {
		try {
			tile[1] = new Tile();tile[0] = new Tile();tile[2] = new Tile();
			tile[3] = new Tile();tile[4] = new Tile();tile[5] = new Tile();
			tile[6] = new Tile();tile[7] = new Tile();tile[8] = new Tile();
			tile[9] = new Tile();tile[10] = new Tile();tile[11] = new Tile();
			tile[12] = new Tile();tile[13] = new Tile();tile[14] = new Tile();
			tile[15] = new Tile();tile[16] = new Tile();tile[17] = new Tile();
			tile[18] = new Tile();tile[19] = new Tile();tile[20] = new Tile();
			tile[21] = new Tile();tile[22] = new Tile();tile[23] = new Tile();
			tile[24] = new Tile();tile[25] = new Tile();tile[26] = new Tile();
			tile[27] = new Tile();tile[28] = new Tile();tile[29] = new Tile();
			tile[30] = new Tile();tile[31] = new Tile();tile[32] = new Tile();			
			tile[33] = new Tile();tile[34] = new Tile();tile[35] = new Tile();
			tile[36] = new Tile();tile[37] = new Tile();tile[38] = new Tile();
			tile[39] = new Tile();tile[40] = new Tile();tile[41] = new Tile();
			tile[42] = new Tile();tile[43] = new Tile();tile[44] = new Tile();
			tile[45] = new Tile();tile[46] = new Tile();tile[47] = new Tile();
			tile[48] = new Tile();tile[49] = new Tile();tile[50] = new Tile();
			tile[51] = new Tile();tile[52] = new Tile();tile[53] = new Tile();
			tile[54] = new Tile();tile[55] = new Tile();tile[56] = new Tile();
			tile[57] = new Tile();tile[58] = new Tile();tile[59] = new Tile();
			tile[60] = new Tile();tile[61] = new Tile();tile[62] = new Tile();tile[63] = new Tile();tile[64] = new Tile();tile[65] = new Tile();
			tile[66] = new Tile();tile[67] = new Tile();
			tile[0].image = ImageIO.read(getClass().getResourceAsStream("/latar/grass00.png"));//tile rumput
			tile[0].collision = true;
			tile[1].image = ImageIO.read(getClass().getResourceAsStream("/latar/water01.png"));//
			tile[1].collision = true;
			tile[2].image = ImageIO.read(getClass().getResourceAsStream("/latar/water00.png"));//aer
			tile[2].collision = true;
			tile[3].image = ImageIO.read(getClass().getResource("/latar/road16.png"));//batu
			tile[3].collision = true;
			tile[4].image = ImageIO.read(getClass().getResource("/latar/grass01.png"));//reruntuhan
			tile[4].collision = true;
			tile[5].image = ImageIO.read(getClass().getResource("/Aset/Tree1.png"));//poshon
			tile[5].collision = true;
			tile[6].image = ImageIO.read(getClass().getResource("/latar/Grassadv01.png"));//pohon
			tile[6].collision = true;
			tile[7].image = ImageIO.read(getClass().getResource("/latar/road14.png"));
			tile[8].image = ImageIO.read(getClass().getResource("/latar/road01.png"));
			tile[9].image = ImageIO.read(getClass().getResource("/latar/road02.png"));
			tile[10].image = ImageIO.read(getClass().getResource("/Aset/Fruit_tree1.png"));
			tile[11].image = ImageIO.read(getClass().getResource("/Aset/4.png"));
			tile[12].image = ImageIO.read(getClass().getResource("/Aset/LightWoodTile.jpg"));
			tile[13].image = ImageIO.read(getClass().getResource("/Aset/Tile2_02.png"));
			tile[14].image = ImageIO.read(getClass().getResource("/latar/water06.png"));  // air samping kanan
			tile[15].image = ImageIO.read(getClass().getResource("/latar/water04.png"));
			tile[16].image = ImageIO.read(getClass().getResource("/latar/water12.png"));
			tile[17].image = ImageIO.read(getClass().getResource("/latar/water08.png")); // air atas
			tile[18].image = ImageIO.read(getClass().getResource("/latar/water03.png")); // air bawah
			tile[19].image = ImageIO.read(getClass().getResource("/latar/water02.png"));
			tile[20].image = ImageIO.read(getClass().getResource("/latar/water13.png"));
			tile[21].image = ImageIO.read(getClass().getResource("/latar/water05.png"));
			tile[22].image = ImageIO.read(getClass().getResource("/latar/water11.png"));
			tile[23].image = ImageIO.read(getClass().getResource("/latar/water10.png"));
			tile[24].image = ImageIO.read(getClass().getResource("/latar/water12.png"));
			tile[25].image = ImageIO.read(getClass().getResource("/latar/bridge_D_top.png")); //
			tile[26].image = ImageIO.read(getClass().getResource("/latar/bridge_D_middle.png"));
			tile[27].image = ImageIO.read(getClass().getResource("/latar/bridge_D_bottom.png"));
			tile[28].image = ImageIO.read(getClass().getResource("/latar/water10.png"));
			tile[29].image = ImageIO.read(getClass().getResource("/latar/water12.png"));
			tile[30].image = ImageIO.read(getClass().getResource("/latar/road03.png"));
			tile[31].image = ImageIO.read(getClass().getResource("/latar/road04.png"));
			tile[32].image = ImageIO.read(getClass().getResource("/latar/road05.png"));
			tile[33].image = ImageIO.read(getClass().getResource("/latar/road06.png"));
			tile[34].image = ImageIO.read(getClass().getResource("/latar/road07.png"));
			tile[35].image = ImageIO.read(getClass().getResource("/latar/road08.png"));
			tile[36].image = ImageIO.read(getClass().getResource("/latar/road09.png"));
			tile[37].image = ImageIO.read(getClass().getResource("/latar/road10.png"));
			tile[38].image = ImageIO.read(getClass().getResource("/latar/road11.png"));
			tile[39].image = ImageIO.read(getClass().getResource("/latar/road12.png"));
			tile[40].image = ImageIO.read(getClass().getResource("/Aset/Rock5_grass_shadow_dark1.png"));
			tile[41].image = ImageIO.read(getClass().getResource("/Aset/Rock5_grass_shadow_dark2.png"));
			tile[42].image = ImageIO.read(getClass().getResource("/Aset/Rock5_grass_shadow_dark3.png"));
			tile[43].image = ImageIO.read(getClass().getResource("/Aset/Rock7_water_shadow_dark1.png"));
			tile[44].image = ImageIO.read(getClass().getResource("/Aset/Rock7_water_shadow_dark2.png"));
			tile[45].image = ImageIO.read(getClass().getResource("/Aset/Rock7_water_shadow_dark3.png"));
			tile[46].image = ImageIO.read(getClass().getResource("/Aset/Rock6_grass_shadow1.png"));
			tile[47].image = ImageIO.read(getClass().getResource("/Aset/Rock6_grass_shadow2.png"));
			tile[48].image = ImageIO.read(getClass().getResource("/Aset/Rock6_grass_shadow3.png"));
			tile[48].image = ImageIO.read(getClass().getResource("/Aset/Rock6_grass_shadow3.png"));
			tile[49].image = ImageIO.read(getClass().getResource("/Aset/Blue-gray_ruins3.png"));
			tile[50].image = ImageIO.read(getClass().getResource("/Aset/Blue-gray_ruins2.png"));
			tile[51].image = ImageIO.read(getClass().getResource("/latar/highland1.png"));
			tile[52].image = ImageIO.read(getClass().getResource("/Aset/Blue-gray_ruins3.png"));
			tile[53].image = ImageIO.read(getClass().getResource("/Aset/PohonPinus2.png"));
			tile[54].image = ImageIO.read(getClass().getResource("/Aset/PohonPinus.png"));
			tile[55].image = ImageIO.read(getClass().getResource("/Aset/Tree2.png"));
			tile[56].image = ImageIO.read(getClass().getResource("/Aset/Tree3.png"));
			tile[57].image = ImageIO.read(getClass().getResource("/latar/water09.png"));
			tile[58].image = ImageIO.read(getClass().getResource("/Aset/Box.png"));
			tile[59].image = ImageIO.read(getClass().getResource("/latar/water07.png"));
			tile[60].image = ImageIO.read(getClass().getResource("/Aset/Anvil.png"));
			tile[61].image = ImageIO.read(getClass().getResource("/Aset/Barracks.png"));
			tile[61].image = ImageIO.read(getClass().getResource("/Aset/Castle.png"));
			tile[63].image = ImageIO.read(getClass().getResource("/Aset/Monastery.png"));
			tile[64].image = ImageIO.read(getClass().getResource("/latar/Barrel Sprite.png"));
			tile[64].image = ImageIO.read(getClass().getResource("/latar/Road13.png"));
			tile[65].image = ImageIO.read(getClass().getResource("/Aset/Rock1_grass_shadow_dark2.png"));
			// dst. tiap rumah baru = tile baru
			
			tile[14].collision = true;
			tile[10].collision = true;	
		}
		catch(IOException e) {
			e.printStackTrace();
		}}
	public void loadMap(String filePath) {

	    try {
	        InputStream is = getClass().getResourceAsStream(filePath);
	        if (is == null) {
	            System.out.println("MAP NOT FOUND");
	            return;
	        }

	        BufferedReader br = new BufferedReader(new InputStreamReader(is));

	        int row = 0;

	        while (row < gp.maxWorldRow) {
	            String line = br.readLine();
	            if (line == null) break;

	            String numbers[] = line.trim().split("\\s+");

	            for (int col = 0; col < numbers.length && col < gp.maxWorldCol; col++) {
	                mapTileNum[col][row] = Integer.parseInt(numbers[col]);
	            }


	            row++;
	        }

	        br.close();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	public void drawObject(Graphics2D g2,
	        int worldX, int worldY, int width, int height, int tileIndex,
	        int camX, int camY, int shadow) {

	    int screenX = worldX - camX + gp.player.screenX;
	    int screenY = worldY - camY + gp.player.screenY;

	    if (screenX + width > 0 && screenX < gp.screenWidth &&
	        screenY + height > 0 && screenY < gp.screenHeight) {

	        // Gambar bayangan dulu sebelum objeknya
	        if (shadow == 1) {
	            java.awt.Composite old = g2.getComposite();
	            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
	            g2.setColor(Color.BLACK);

	            int shadowOffsetX = 12;          // geser ke kanan
	            int shadowScaleH  = (int)(height * 0.25f); // tinggi bayangan = 25% tinggi objek

	            // Bayangan = oval gepeng di bawah kaki objek
	            g2.fillOval(
	                screenX + shadowOffsetX,
	                screenY + height - shadowScaleH / 2,
	                (int)(width * 0.75f),
	                shadowScaleH
	            );

	            g2.setComposite(old);
	        }

	        g2.drawImage(tile[tileIndex].image,
	                     screenX, screenY, width, height, null);
	    }
	}
	
	public void drawNPC(Graphics2D g2, Image img, int worldX, int worldY, int w, int h, int camX, int camY) {

	    int screenX = worldX - camX + gp.player.screenX;
	    int screenY = worldY - camY + gp.player.screenY;

	    if (screenX + w > 0 && screenX < gp.screenWidth &&
	        screenY + h > 0 && screenY < gp.screenHeight) {

	        g2.drawImage(img, screenX, screenY, w, h, null);
	    }
	}
			
	//buat load objek yang bisa diancurin

	public void loadBreakableObjects() {
        int[][] data = {
            // data koordinat pohon kamu tetap aman di sini...
            {440, 1790, 150, 150, 55, 500},{560, 2010, 150, 150, 55, 500},{380, 2060, 150, 150, 55, 500},{785, 1840,150, 150, 55, 500},{220, 1850, 150, 150, 55, 500},{1530, 2250,150, 150, 55, 500},{1100, 2140,150, 150, 55, 500}
            ,{1140, 2510,150, 150, 55, 500},{1070, 2600, 150, 150, 55, 500},{950, 2590, 150, 150, 55, 500},{460, 2150,150, 150, 55, 500},{410, 2250, 150, 150, 55, 500},{350, 2440,150, 150, 55, 500},{430, 2620, 150, 150, 55, 500}
            ,{1540, 2600, 150, 150, 55, 500}
            ,{1460, 660, 70, 70, 40, 500}
            ,{440, 2920,150, 150, 55, 500} ,{490, 3000, 150, 150, 55, 500},{610, 2670, 150, 150, 55, 500},{500, 2740,150, 150, 55, 500},{930, 2900, 150, 150, 55, 500},{830, 3020,150, 150, 55, 500}
            ,{1500, 2770,150, 150, 55, 500},{1400, 2860, 150, 150, 55, 500},{1260, 2860, 150, 150, 55, 500},{820, 3290, 150, 150, 55, 500},{870, 3460, 150, 150, 55, 500},{1150, 3300,150, 150, 55, 500}
            ,{840, 3600, 150, 150, 55, 500},{900, 3750, 150, 150, 55, 500},{950, 3820,150, 150, 55, 500},{1150, 3950, 150, 150, 55, 500},{790, 3650,150, 150, 55, 500},{1300, 3940,150, 150, 55, 500}
            ,{400, 3600, 150, 150, 55, 500},{470, 3750,150, 150, 55, 500},{470, 3900, 150, 150, 55, 500},{530, 4050,150, 150, 55, 500},{650, 4150, 150, 150, 55, 500},{720, 4200,150, 150, 55, 500}
            ,{860, 4250, 150, 150, 55, 500},{1020, 4230, 150, 150, 55, 500}
            , {900, 1800, 120, 120, 40, 800}
            , {1000, 1850, 120, 120, 41, 800}
        };

        breakables = new breakable[data.length];

        BufferedImage[] treeIdleFrames = new BufferedImage[2];
        BufferedImage[] treeHitFrames = new BufferedImage[4];
        BufferedImage[] treeDownFrames = new BufferedImage[6];

        try {
            treeIdleFrames[0] = ImageIO.read(getClass().getResourceAsStream("/Aset/Tree (1).png"));
            treeIdleFrames[1] = ImageIO.read(getClass().getResourceAsStream("/Aset/Tree (2).png"));

            treeHitFrames[0] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeHit0.png"));
            treeHitFrames[1] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeHit1.png"));
            treeHitFrames[2] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeHit2.png"));
            treeHitFrames[3] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeHit3.png"));
            
            treeDownFrames[0] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown0.png"));
            treeDownFrames[1] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown1.png"));
            treeDownFrames[2] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown2.png"));
            treeDownFrames[3] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown3.png"));
            treeDownFrames[4] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown4.png"));
            treeDownFrames[5] = ImageIO.read(getClass().getResourceAsStream("/Aset/TreeDown5.png"));
        } catch (Exception e) {
            System.out.println("Gagal me-load gambar aset Pohon! Periksa folder res/Aset/");
            e.printStackTrace();
        }

        for (int i = 0; i < data.length; i++) {
            int tileIndex = data[i][4];
            int hp        = data[i][5];
            
            // 🔥 MODIFIKASI FORMAT KODE LAMA:
            // Jika dia pohon (55), langsung gunakan frame pertama animasi idle, bukan gambar statis tile[55]
            BufferedImage img;
            if (tileIndex == 55 && treeIdleFrames[0] != null) {
                img = treeIdleFrames[0];
            } else {
                img = tile[tileIndex].image;
            }

            Item dropItem;
            if (tileIndex == 55) {
                dropItem = ItemManager.get().LOG;
            } else {
                dropItem = ItemManager.get().ORE;
            }

            breakables[i] = new breakable(
                data[i][0], data[i][1], data[i][2], data[i][3],
                img, hp, dropItem, gp
            );

            if (tileIndex == 55) {
                breakables[i].setIdleFrames(treeIdleFrames);
                breakables[i].setHitFrames(treeHitFrames);
                breakables[i].setDownFrames(treeDownFrames); 
            }
        }
    }
	
	
	public void draw(Graphics2D g2) {
        int worldWidth  = gp.maxWorldCol * gp.tileSize;
        int worldHeight = gp.maxWorldRow * gp.tileSize;
        int camX = gp.player.x;
        int camY = gp.player.y;
        if (camX < gp.player.screenX) camX = gp.player.screenX;
        if (camX > worldWidth  - gp.player.screenX) camX = worldWidth  - gp.player.screenX;
        if (camY < gp.player.screenY)  camY = gp.player.screenY;
        if (camY > worldHeight - gp.player.screenY) camY = worldHeight - gp.player.screenY;

        // Hanya gambar tile dasar (ground layer)
        for (int worldRow = 0; worldRow < gp.maxWorldRow; worldRow++) {
            for (int worldCol = 0; worldCol < gp.maxWorldCol; worldCol++) {
                int tileNum = mapTileNum[worldCol][worldRow];
                
                // 🔥 BARU: Jika tile map mendeteksi objek breakable (pohon 55, batu 40, 41)
                // Timpa otomatis layer dasarnya dengan rumput (tile 0) agar tidak double render/saling timpa!
                if (tileNum == 55 || tileNum == 40 || tileNum == 41) {
                    tileNum = 0; 
                }

                int worldX  = worldCol * gp.tileSize;
                int worldY  = worldRow * gp.tileSize;
                int screenX = worldX - camX + gp.player.screenX;
                int screenY = worldY - camY + gp.player.screenY;

                if (screenX + gp.tileSize > 0 && screenX < gp.screenWidth &&
                    screenY + gp.tileSize > 0 && screenY < gp.screenHeight) {
                    g2.drawImage(tile[tileNum].image, screenX, screenY,
                                 gp.tileSize, gp.tileSize, null);
                }
            }
        }
    }
	
	
	
		
				public void drawYSorted(Graphics2D g2, player p,
			            Entitas.Entity[] npcs,
			            Entitas.Entity[] monsters) {
			
			int worldWidth  = gp.maxWorldCol * gp.tileSize;
			int worldHeight = gp.maxWorldRow * gp.tileSize;
			int camX = gp.player.x;
			int camY = gp.player.y;
			if (camX < p.screenX) camX = p.screenX;
			if (camX > worldWidth  - p.screenX) camX = worldWidth  - p.screenX;
			if (camY < p.screenY)  camY = p.screenY;
			if (camY > worldHeight - p.screenY) camY = worldHeight - p.screenY;
			
			final int fCamX = camX, fCamY = camY;
			
			
			// ── GIF & animated objects — selalu di atas ──
			animObjManager.update();
			animObjManager.draw(g2, fCamX, fCamY,
			            gp.player.screenX, gp.player.screenY,
			            gp.screenWidth, gp.screenHeight);
			
			// ── PASS 1: Gambar semua flat objects dulu (tidak ikut sort) ──
			for (int[] obj : worldObjects) {
			if (obj[5] == 1) { // isFlat
				// BARU — ambil flag shadow dari index 6
				drawObject(g2, obj[0], obj[1], obj[2], obj[3], obj[4], fCamX, fCamY, obj[6]);}
			}
			
			// ── PASS 2: Kumpulkan semua standing objects ke drawList ──
			java.util.List<int[]> drawList = new java.util.ArrayList<>();
			
			// WorldObjects yang standing (isFlat == 0)
			for (int i = 0; i < worldObjects.length; i++) {
			if (worldObjects[i][5] == 0) {
			int sortY = worldObjects[i][1] + worldObjects[i][3] - 40; // worldY + height = kaki objek
			drawList.add(new int[]{sortY, 0, i});
			}
			}
			
			// Breakables (pohon tebang, batu)
			if (breakables != null) {
			    for (int i = 0; i < breakables.length; i++) {
			        if (breakables[i] == null) continue;
			        
			        // UPDATE selalu dipanggil, termasuk saat destroyed (biar timer respawn jalan)
			        breakables[i].update();
			        
			        // Tapi hanya masuk drawList kalau belum destroyed
			        if (breakables[i].destroyed) continue;
			        
			        int sortY = breakables[i].worldY + breakables[i].height - 10;
			        drawList.add(new int[]{sortY, 1, i});
			    }
			}
			
			
			// NPC
			for (int i = 0; i < npcs.length; i++) {
			if (npcs[i] != null && !npcs[i].isDead) {
			drawList.add(new int[]{npcs[i].y + gp.tileSize, 2, i});
			}
			}
			
			// Monster
			for (int i = 0; i < monsters.length; i++) {
			if (monsters[i] != null && !monsters[i].isDead) {
			drawList.add(new int[]{monsters[i].y + gp.tileSize, 3, i});
			}
			}
			
			// Golem
			java.util.List<Entitas.Entity> minionList = new java.util.ArrayList<>();
			if (gp.deathTotemManager != null) {
			    for (Entitas.Entity m : gp.deathTotemManager.getMinions()) {
			        if (m != null) minionList.add(m);
			    }
			    for (int i = 0; i < minionList.size(); i++) {
			        drawList.add(new int[]{minionList.get(i).getSortY(), 5, i});   // ← INI YANG HILANG
			    }
			}
			
			
			// Player
			drawList.add(new int[]{p.y + gp.tileSize, 4, 0});
			
			// ── Sort by Y ascending ──
			drawList.sort((a, b) -> Integer.compare(a[0], b[0]));
			
			// ── PASS 3: Draw sesuai urutan Y ──
			for (int[] entry : drawList) {
			int type  = entry[1];
			int index = entry[2];
			
			switch (type) {
			case 0: { // worldObject standing
			    int[] obj = worldObjects[index];
			    drawObject(g2, obj[0], obj[1], obj[2], obj[3], obj[4], fCamX, fCamY, obj[6]);
			    break;
			}
			case 1: { // breakable
			    breakable b = breakables[index];
			    int sx = b.worldX - fCamX + gp.player.screenX;
			    int sy = b.worldY - fCamY + gp.player.screenY;
			    if (sx + b.width >= 0 && sx <= gp.screenWidth &&
			        sy + b.height >= 0 && sy <= gp.screenHeight) {
			        
			        // 🔥 TAMBAHAN: Menggambar bayangan oval transparan di bawah pohon
			        java.awt.Composite old = g2.getComposite();
			        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f)); // Alpha 35%
			        g2.setColor(Color.BLACK);

			        int shadowOffsetX = 12;          // Geser sedikit ke kanan agar pas di tengah batang
			        int shadowScaleH  = (int)(b.height * 0.20f); // Tinggi bayangan 20% dari tinggi pohon

			        g2.fillOval(
			            sx + shadowOffsetX,
			            sy + b.height - (shadowScaleH / 2) - 10, // Posisi di area kaki/bawah pohon
			            (int)(b.width * 0.75f),                  // Lebar bayangan 75% dari lebar pohon
			            shadowScaleH
			        );
			        g2.setComposite(old); // Kembalikan opacity normal graphics

			        // Gambar pohon asli di atas bayangan
			        b.draw(g2, sx, sy);
			    }
			    break;
			}
			case 2: { // NPC
			    gp.npc[index].draw(g2);
			    break;
			}
			case 3: { // monster
			    gp.monsters[index].draw(g2);
			    break;
			}
			case 4: { // player
			    p.draw(g2);
			    break;
			}
			case 5: { // minion (golem/skeleton pion)
			    minionList.get(index).draw(g2);
			    break;
			}
			}
			}
			
			drawGif(g2, RockWater,  1380, 1380, fCamX, fCamY, 150);
			drawGif(g2, RockWater1, 240,  1280, fCamX, fCamY, 100);
			drawGif(g2, RockWater2, 240,  1350, fCamX, fCamY, 100);
			
			//drawObjectCollisionDebug(g2, fCamX, fCamY);
			}
		
	
	public void loadObjectCollision() {
//base player
	    // PLING ATAS RUIINS
		gp.objCol.add(590, 675, 35, 26);
	    gp.objCol.add(530, 725, 140, 110);//col ruins
	    gp.objCol.add(740, 855, 140, 50);
	    gp.objCol.add(830, 825, 266, 170); // colisin pohon depan rumah belah kiri
	    gp.objCol.add(1040,1100,552,75); //col pohon depan rumah belah kanan
	    gp.objCol.add(680, 840, 60, 50);
	    gp.objCol.add(480, 900, 40, 300);
	    gp.objCol.add(540, 880, 60, 80);
	    gp.objCol.add(990, 850,240,70);//col rumah
	    gp.objCol.add(1160,930,10,45);//collision batu dpn rumah
	    gp.objCol.add(1170,1400,30,105);
	    gp.objCol.add(1220,1255,5000,135);
	    
	    gp.objCol.add(1040,1100,75,310);
	    gp.objCol.add(500,1100,45,75);
	    gp.objCol.add(545,1175,65,80);
	    
	    //collision rock gede dpn rumh
	    gp.objCol.add(1140,1210, 60, 70);
	    
	    //collsion air dpn jembatan
	    gp.objCol.add(690, 2025, 350,150);
	    gp.objCol.add(930, 1975, 100,40);
	    gp.objCol.add(830, 2190, 50,50);
	    gp.objCol.add(880, 2240, 140,30);
	    gp.objCol.add(990, 2160, 50,110);
	    
	    
	    // coliision monestry
	    gp.objCol.add(1700, 630, 150 , 80);
	    
	    
	    //objek di air
	   // gp.objCol.add();   
	    gp.objCol.add(1200, 920, 60,80);
	    gp.objCol.add(340,900,20,300);
	    gp.objCol.add(340,1100,120,20); //colisiion samping ruins pembatas aer
	    gp.objCol.add(340,10,20,1000); // colisiion samping ruins pembatas aer
	    gp.objCol.add(450,900,20,500);
	    gp.objCol.add(10, 380, 30000, 20);
	    gp.objCol.add(480,1400,30,20);
	    gp.objCol.add(520,1450,30,20);
	    gp.objCol.add(350,1490,255,265);
	    gp.objCol.add(350,1490,45,9065);
	    gp.objCol.add(695,1520,110,220);
	    gp.objCol.add(900,1520,105,220);
	    gp.objCol.add(1095,1530,5000,210);
	    gp.objCol.add(2040,1210,30,30);
	    gp.objCol.add(2080,1115,30,85);
	    gp.objCol.add(2130,400,30,1000);
	    
	    gp.objCol.add(1050, 100, 550, 80); // collision pohon di depan air
	    
	    // semakin kecil semkin kiri, semakin besar smkin ke kanan
	    // collision di luar base player
	    gp.objCol.add(1160, 1990, 130, 100); gp.objCol.add(1300, 1930, 105, 110); gp.objCol.add(1160, 2090, 70, 65);
	    gp.objCol.add(720, 1910, 80, 80); gp.objCol.add(795, 1910, 20, 20);
	    gp.objCol.add(1570, 2155, 200, 100); gp.objCol.add(1340, 2170, 50 , 40 );
	    gp.objCol.add(1400, 2200, 40 , 40 ); gp.objCol.add(1510, 2035, 100 , 100 );
	    

	    worldObjects = new int[][] {
	        // FORMAT: {x, y, width, height, tileIndex, isFlat, shadow}
	        // shadow: 1 = ada bayangan oval di bawah, 0 = tidak

	        // ── FLAT OBJECTS ──
	        {475,1200,100,100,58,1, 0},
	        {535,1390,60,60,47,1,  0},

	        // ── BATU KECIL (flat) ──
	        {910,1330,60,60,65,1,   0},
	        {710,830,60,60,65,1,    0},
	        {1060,930,60,60,65,1,   0},
	        {1060,960,60,60,65,1,   0},
	        {1135,930,60,60,65,1,   0},
	        {1135,960,60,60,65,1,   0},
	        {680,830,60,60,65,1,    0},
	        {660,857,60,60,65,1,    0},
	        {695,857,60,60,65,1,    0},
	        {710,830,140,140,65,1,  0},
	        {1100,1180,140,140,65,1,0},

	        // ── RERUNTUHAN (flat) ──
	        {510,680,200,200,50,1,  0},

	        // ── JEMBATAN (flat) ──
	        {800,1500,100,100,25,1, 0},{800,1600,100,100,26,1, 0},{800,1700,100,100,27,1, 0},
	        {600,1500,100,100,25,1, 0},{600,1600,100,100,26,1, 0},{600,1700,100,100,27,1, 0},
	        {1000,1500,100,100,25,1,0},{1000,1600,100,100,26,1,0},{1000,1700,100,100,27,1,0},

	        // ── BATU PUTIH KECIL (flat) ──
	        {1200,2000,100,100,40,1,0},{1130,1980,100,100,41,1,0},{1170,2040,50,50,40,1,0},
	        {780,1910,50,50,40,1,   0},{700,1910,100,100,40,1, 0},

	        // ── BATU KREM (flat/medium) ──
	        {1260,2410,90,90,46,1, 0},{1045,2100,70,70,47,1, 0},{1160,2110,70,70,48,1, 0},
	        {1380,2200,70,70,46,1, 0},{1330,2170,70,70,48,1, 0},

	        // ── STANDING OBJECTS — ikut Y-sort ──

	        // ── RUMAH-RUMAH (tidak perlu bayangan oval, punya shadow sendiri) ──
	        {995, 700, 250, 250, 11, 0,  0},
	        {1205, 800, 200, 200, 62, 0, 0},
	        {1680, 360, 200, 400, 63, 0, 0},

	        // ── POHON-POHON (bayangan aktif) ──
	        {740,780,240,240,10,0,    0},
	        {780,855,200,200,5,0,     0},{845,863,200,200,5,0,  0},{930,855,200,200,5,0,  0},{1130,863,200,200,5,0,0},
	        {470,840,200,200,5,0,     0},{390,850,200,200,5,0,  0},{390,900,200,200,5,0,  0},{390,950,200,200,5,0, 0},
	        {390,1000,200,200,5,0,    0},{410,1050,200,200,5,0, 0},
	        {480,1130,200,200,5,0,    0},
	        {980,1050,200,200,5,0,    0},{1060,1050,200,200,5,0,0},{1140,1050,200,200,5,0,0},
	        {1220,1050,200,200,5,0,   0},{1300,1050,200,200,5,0,0},{1380,1050,200,200,5,0,0},{1460,1050,200,200,5,0,0},
	        {1680,1050,200,200,5,0,   0},
	        {980,1130,200,200,5,0,    0},{980,1210,200,200,5,0, 0},{980,1290,200,200,5,0, 0},

	        // ── POHON DI LUAR BASE ──
	        {1240,1880,240,240,10,0,  0},{1440,1980,240,240,10,0,0},{1130,2300,240,240,10,0,0},
	        {1500,2100,240,240,10,0,  0},{1420,2350,240,240,10,0,0},
	        {1600,2100,240,240,10,0,  0},

	        // ── RUINS BESAR ──
	        {1300,2100,200,200,49,0,  0},

	        // ── BARREL ──
	        //{550,2280,30,40,64,0,     0},
	        //{470,1300,30,40,64,0,     0},
	    };
	}

	public void checkHouseInteraction() {

	    if (!gp.KeyH.interactPressed) return;

	    Rectangle playerArea = gp.player.getInteractArea();
	    for (int i = 0; i < worldObjects.length; i++) {

	        int wx   = worldObjects[i][0];
	        int wy   = worldObjects[i][1];
	        int size = worldObjects[i][2];
	        int tileIndex = worldObjects[i][3];

	        if (tileIndex == 11) {

	            Rectangle houseArea = new Rectangle(wx, wy, size, size);

	            if (playerArea.intersects(houseArea)) {
	                enterHouse();
	                gp.KeyH.interactPressed = false; // anti spam
	                break;
	            }
	            
	        }
	    }
	}
	public void drawGif(Graphics2D g2, ImageIcon gif,
	        int worldX, int worldY,
	        int camX, int camY, int size) {

	    int w = size;
	    int h = size;

	    int screenX = worldX - camX + gp.player.screenX;
	    int screenY = worldY - camY + gp.player.screenY;

	    if (screenX + w > 0 &&
	        screenX < gp.screenWidth &&
	        screenY + h > 0 &&
	        screenY < gp.screenHeight) {

	        g2.drawImage(gif.getImage(), screenX, screenY, w, h, null);
	    }
	}

	public void enterHouse() {

	    gp.gameState = GamePanel.GameState.INSIDE;

	    if (!interiorLoaded) {
	        loadInteriorFloor();
	        loadInteriorObjects();
	        interiorLoaded = true;
	    }

	    // Spawn di tengah bawah ruangan
	    gp.player.x = 200;
	    gp.player.y = 500;
	}



	public boolean checkInteriorCollision(Rectangle playerBox) {

	    if (interiorCollision == null) return false;

	    for (Rectangle r : interiorCollision) {
	        if (playerBox.intersects(r)) {
	            return true;
	        }
	    }
	    return false;
	}

	public void loadInteriorObjects() {

	    interiorObj = new Barang[0]; // tidak ada objek tambahan dulu

	    // Collision disesuaikan sama layout mockup4.gif
	    // Format: Rectangle(x, y, width, height) — koordinat world interior
	    interiorCollision = new Rectangle[] {

	        // ── TEMBOK ATAS ──
	        new Rectangle(0, -20, 480, 30),

	        // ── TEMBOK KIRI ──
	        new Rectangle(-20, 0, 30, 640),

	        // ── TEMBOK KANAN ──
	        new Rectangle(470, 0, 30, 640),

	        // ── TEMBOK BAWAH ──
	        new Rectangle(0, 620, 480, 30),
	    };

	    interiorLoaded = true;
	}

	
	public void loadInteriorFloor() {

	    interiorMap = new int[interiorCol][interiorRow];

	    for (int col = 0; col < interiorCol; col++) {
	        for (int row = 0; row < interiorRow; row++) {

	            interiorMap[col][row] = interiorFloorTile;
	        }
	    }
	}
	
	public void drawObjectCollisionDebug(Graphics2D g2, int camX, int camY) {

	    if (!gp.debugCollision) return;

	    g2.setColor(new Color(255, 0, 0, 120));

	    for (Rectangle r : gp.objCol.objects) {

	        int screenX = r.x - camX + gp.player.screenX;
	        int screenY = r.y - camY + gp.player.screenY;

	        // 🔥 SKIP YANG DI LUAR LAYAR
	        if (screenX + r.width < 0 || screenX > gp.screenWidth ||
	            screenY + r.height < 0 || screenY > gp.screenHeight) {
	            continue;
	        }

	        g2.drawRect(screenX, screenY, r.width, r.height);
	    }
	}


	
	public void drawInteriorFloor(Graphics2D g2) {

	    // Ukuran ruangan — sesuaikan kalau perlu
	    int roomW = 480;
	    int roomH = 640;

	    // Posisi room selalu relatif ke player
	    int screenX = 0 - gp.player.x + gp.player.screenX;
	    int screenY = 0 - gp.player.y + gp.player.screenY;

	    g2.drawImage(interiorBg.getImage(), screenX, screenY, roomW, roomH, null);
	}

}