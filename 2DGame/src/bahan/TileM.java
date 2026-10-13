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

// ==== TILE GIF ANIMASI ====
// tileAnimImage[i] != null artinya tile index "i" itu GIF animasi (bukan gambar statis).
// Kalau null, tile itu tetap gambar statis biasa (ambil dari tile[i].image seperti sebelumnya).
public Image[] tileAnimImage;
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
    tileAnimImage = new Image[tile.length];
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

    // Daftarin tile yang berupa GIF animasi di sini (harus SEBELUM loadMap dipanggil).
    // index 62 masih kosong/belum kepake tile lain di getTileImage(), jadi aman dipakai.
    // Format: loadAnimatedTile(index, "/path/ke/file.gif");
    loadAnimatedTile(62, "/Aset/GrassGiff.gif");

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
			tile[66] = new Tile();tile[67] = new Tile();tile[68] = new Tile();tile[69] = new Tile();tile[70] = new Tile();
			tile[71] = new Tile();tile[72] = new Tile();
			tile[0].image = ImageIO.read(getClass().getResourceAsStream("/Aset/Grass (1).png"));//tile rumput
			tile[0].collision = true;
			tile[1].image = ImageIO.read(getClass().getResourceAsStream("/latar/water01.png"));//
			tile[1].collision = true;
			tile[2].image = ImageIO.read(getClass().getResourceAsStream("/latar/water00.png"));//aer
			tile[2].collision = true;
			tile[3].image = ImageIO.read(getClass().getResource("/latar/road16.png"));//batu
			tile[3].collision = true;
			tile[4].image = ImageIO.read(getClass().getResource("/Aset/Grass (2).png"));//reruntuhan
			tile[4].collision = true;
			tile[5].image = ImageIO.read(getClass().getResource("/Aset/Tree1.png"));//poshon
			tile[5].collision = true;
			tile[6].image = ImageIO.read(getClass().getResource("/Aset/Grass (3).png"));//pohon
			tile[6].collision = true;
			tile[7].image = ImageIO.read(getClass().getResource("/latar/road14.png"));
			tile[8].image = ImageIO.read(getClass().getResource("/latar/road01.png"));
			tile[9].image = ImageIO.read(getClass().getResource("/latar/road02.png"));
			tile[10].image = ImageIO.read(getClass().getResource("/Aset/Grass (4).png"));
			tile[11].image = ImageIO.read(getClass().getResource("/Aset/4.png"));
			tile[12].image = ImageIO.read(getClass().getResource("/Aset/Snowybush.png"));
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
			tile[66].image = ImageIO.read(getClass().getResource("/Aset/Bush.png"));
			tile[68].image = ImageIO.read(getClass().getResource("/Aset/snow1.png"));
			tile[67].image = ImageIO.read(getClass().getResource("/Aset/snow.png"));
			tile[69].image = ImageIO.read(getClass().getResource("/Aset/Snowybush.png"));
			tile[70].image = ImageIO.read(getClass().getResource("/Aset/JungleHouse.png"));
			tile[71].image = ImageIO.read(getClass().getResource("/Aset/Rock4_2.png"));
			tile[72].image = ImageIO.read(getClass().getResource("/latar/golemMine.png"));
			
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

	/**
	 * Daftarin sebuah GIF sebagai tile animasi.
	 *
	 * index   = index tile-nya (angka yang sama dipakai di map01.txt / mapTileNum)
	 * gifPath = path resource ke file .gif, contoh: "/Aset/GrassGiff.gif"
	 *
	 * PENTING kenapa sebelumnya macet/stuck di frame pertama:
	 * ImageIO.read(...) itu cuma ngedecode GIF jadi SATU BufferedImage statis
	 * (cuma nangkep frame pertama doang, animasinya ilang).
	 * Makanya di sini kita load GIF-nya pakai ImageIcon (lewat Toolkit), sama
	 * persis kayak npcGif/firecamp/RockWater di atas -- Toolkit yang otomatis
	 * jalanin animasi GIF-nya sendiri di background thread. Kita tinggal ambil
	 * Image-nya tiap kali digambar (bukan nyimpen 1 frame statis doang).
	 */
	public void loadAnimatedTile(int index, String gifPath) {
	    try {
	        java.net.URL url = getClass().getResource(gifPath);
	        if (url == null) {
	            System.out.println("ANIMATED TILE NOT FOUND: " + gifPath);
	            return;
	        }

	        ImageIcon icon = new ImageIcon(url); // constructor ini nunggu sampe gambar full ke-load
	        tileAnimImage[index] = icon.getImage();

	        // tile[index] tetep disiapin (buat collision & fallback frame statis),
	        // biar kode lama yang masih baca tile[index].image gak error/kosong.
	        if (tile[index] == null) tile[index] = new Tile();
	        BufferedImage fallback = new BufferedImage(
	                Math.max(icon.getIconWidth(), 1),
	                Math.max(icon.getIconHeight(), 1),
	                BufferedImage.TYPE_INT_ARGB);
	        Graphics2D g = fallback.createGraphics();
	        g.drawImage(icon.getImage(), 0, 0, null);
	        g.dispose();
	        tile[index].image = fallback;

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

	            int shadowOffsetX = 5;          // geser ke kanan
	            int shadowOffsetY = -30;           // geser ke bawah, TINGGAL ATUR INI
	            int shadowScaleH  = (int)(height * 0.30f); // tinggi bayangan = 25% tinggi objek

	            // Bayangan = oval gepeng di bawah kaki objek
	            g2.fillOval(
	                screenX + shadowOffsetX,
	                screenY + height - (shadowScaleH / 2) + shadowOffsetY,
	                (int)(width * 0.90f),
	                shadowScaleH
	            );
	            
	            g2.setComposite(old);
	        }

	        if (tileAnimImage[tileIndex] != null) {
	            g2.drawImage(tileAnimImage[tileIndex],
	                         screenX, screenY, width, height, null);
	        } else {
	            g2.drawImage(tile[tileIndex].image,
	                         screenX, screenY, width, height, null);
	        }
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
        	            ,{1140, 2510,200, 200, 55, 500},{1070, 2600, 150, 150, 55, 500},{950, 2590, 150, 150, 55, 500},{460, 2150,150, 150, 55, 500},{410, 2250, 150, 150, 55, 500},{350, 2440,150, 150, 55, 500},{430, 2620, 150, 150, 55, 500}
        	            ,{1540, 2600, 150, 150, 55, 500}
        	            ,{1460, 660, 70, 70, 40, 500}
        	            ,{440, 2920,150, 150, 55, 500} ,{490, 3000, 150, 150, 55, 500},{610, 2670, 150, 150, 55, 500},{500, 2740,150, 150, 55, 500},{930, 2900, 150, 150, 55, 500},{830, 3020,150, 150, 55, 500}
        	            ,{1500, 2770,150, 150, 55, 500},{1400, 2860, 150, 150, 55, 500},{1260, 2860, 150, 150, 55, 500},{820, 3290, 150, 150, 55, 500},{870, 3460, 150, 150, 55, 500},{1150, 3300,150, 150, 55, 500}
        	            ,{840, 3600, 150, 150, 55, 500},{900, 3750, 150, 150, 55, 500},{950, 3820,150, 150, 55, 500},{1150, 3950, 150, 150, 55, 500},{790, 3650,150, 150, 55, 500},{1300, 3940,150, 150, 55, 500}
        	            ,{400, 3600, 150, 150, 55, 500},{470, 3750,150, 150, 55, 500},{470, 3900, 150, 150, 55, 500},{530, 4050,150, 150, 55, 500},{650, 4150, 150, 150, 55, 500},{720, 4200,150, 150, 55, 500}
        	            ,{860, 4250, 150, 150, 55, 500},{1020, 4230, 150, 150, 55, 500}
        	            , {900, 1800, 120, 120, 40, 800}
        	            , {1000, 1850, 120, 120, 41, 800}
        	            
        	            , {1350, 3500, 120, 120, 71, 800, 0}// === BATU BARU (tersebar, jaga jarak dari objek lain) ===
        	            ,{834, 1557, 120, 120, 71, 800, 0},{9884, 5078, 120, 120, 71, 800, 0},{1173, 10409, 120, 120, 71, 800, 0},{8142, 3826, 120, 120, 71, 800, 0},
        	            {3728, 3892, 120, 120, 71, 800, 0},{369, 3349, 120, 120, 71, 800, 0},{1641, 450, 120, 120, 71, 800, 0},{7666, 3648, 120, 120, 71, 800, 0},
        	            {1375, 872, 120, 120, 71, 800, 0},{9704, 2821, 120, 120, 71, 800, 0},{1538, 1976, 120, 120, 71, 800, 0},{1536, 4489, 120, 120, 71, 800, 0},
        	            {8756, 5242, 120, 120, 71, 800, 0},{1918, 2798, 120, 120, 71, 800, 0},{11087, 4183, 120, 120, 71, 800, 0},{583, 2402, 120, 120, 71, 800, 0},
        	            {1729, 2392, 120, 120, 71, 800, 0},{11081, 4761, 120, 120, 71, 800, 0}// === BATU BARU BATCH 2 (30 buah, tersebar, jaga jarak dari objek lain + batch 1) ===
        	            ,{9518, 3796, 120, 120, 71, 800, 0},{11278, 4997, 120, 120, 71, 800, 0},{7281, 5090, 120, 120, 71, 800, 0},{5095, 2844, 120, 120, 71, 800, 0},
        	            {178, 2254, 120, 120, 71, 800, 0},{3813, 4174, 120, 120, 71, 800, 0},{144, 3733, 120, 120, 71, 800, 0},{11068, 4534, 120, 120, 71, 800, 0},
        	            {1655, 2927, 120, 120, 71, 800, 0},{7734, 5247, 120, 120, 71, 800, 0},{3337, 2810, 120, 120, 71, 800, 0},{6272, 2884, 120, 120, 71, 800, 0},
        	            {681, 11280, 120, 120, 71, 800, 0},{6913, 5140, 120, 120, 71, 800, 0},{1219, 525, 120, 120, 71, 800, 0},{4746, 2912, 120, 120, 71, 800, 0},
        	            {2449, 2891, 120, 120, 71, 800, 0},{1060, 4516, 120, 120, 71, 800, 0},{593, 3236, 120, 120, 71, 800, 0},{1476, 10318, 120, 120, 71, 800, 0},
        	            {4388, 4132, 120, 120, 71, 800, 0},{2184, 2934, 120, 120, 71, 800, 0},{1201, 1739, 120, 120, 71, 800, 0},{5617, 3071, 120, 120, 71, 800, 0},
        	            {712, 10678, 120, 120, 71, 800, 0},{4917, 3530, 120, 120, 71, 800, 0},{570, 1529, 120, 120, 71, 800, 0},{1757, 4305, 120, 120, 71, 800, 0},
        	            {5630, 5241, 120, 120, 71, 800, 0},{1585, 11864, 120, 120, 71, 800, 0}

        	            // === Hutan tambahan — ukuran 180-200 (persegi), jumlah dikurangi ===
        	            ,{3182, 3374, 200, 200, 55, 500},{3357, 3557, 198, 198, 55, 500},{2695, 3066, 200, 200, 55, 500},{3108, 3843, 197, 197, 55, 500}
        	            ,{1782, 3565, 191, 191, 55, 800},{1739, 3131, 182, 182, 55, 500},{3379, 3158, 182, 182, 55, 500},{2919, 3452, 188, 188, 55, 500}
        	            ,{2765, 4161, 200, 200, 55, 500},{1207, 3681, 187, 187, 55, 500},{2388, 3439, 199, 199, 55, 500},{3471, 4010, 192, 192, 55, 800}
        	            ,{2612, 3539, 181, 181, 55, 500},{1825, 4049, 180, 180, 55, 500},{1998, 3071, 186, 186, 55, 800},{2093, 4009, 182, 182, 55, 500}
        	            ,{2297, 4220, 195, 195, 55, 800},{3177, 4213, 197, 197, 55, 500},{1466, 4188, 189, 189, 55, 800},{1426, 3156, 198, 198, 55, 500}
        	            ,{2974, 3215, 199, 199, 55, 500},{1950, 3346, 196, 196, 55, 500},{3533, 3764, 198, 198, 55, 500},{1605, 3959, 200, 200, 55, 500}
        	            ,{2020, 3778, 200, 200, 55, 500},{2549, 3946, 191, 191, 55, 500},{1681, 3356, 184, 184, 55, 500},{1506, 3545, 183, 183, 55, 800}
        	            ,{2819, 3717, 196, 196, 55, 500},{3602, 3191, 188, 188, 55, 500},{2447, 3120, 195, 195, 55, 500},{3600, 3525, 194, 194, 55, 800}
        	            ,{2366, 3723, 181, 181, 55, 500},{3084, 3616, 181, 181, 55, 500},{2222, 3241, 195, 195, 55, 500},{2874, 3944, 189, 189, 55, 500}
        	            ,{2745, 3287, 193, 193, 55, 500},{1422, 3757, 182, 182, 55, 500},{1087, 3066, 196, 196, 55, 500},{3596, 4243, 194, 194, 55, 500}
        	            ,{2189, 3559, 184, 184, 55, 800},{2025, 4244, 185, 185, 55, 500},{1231, 4245, 198, 198, 55, 500},{3153, 3071, 181, 181, 55, 800}
        	            ,{1417, 11694, 183, 183, 55, 500},{1197, 11337, 188, 188, 55, 500},{2166, 11550, 193, 193, 55, 800},{2119, 10714, 180, 180, 55, 500}
        	            ,{1890, 11111, 198, 198, 55, 500},{1322, 11543, 181, 181, 55, 800},{2037, 11705, 187, 187, 55, 500},{1905, 11393, 196, 196, 55, 500}
        	            ,{1238, 11054, 181, 181, 55, 500},{1728, 10611, 180, 180, 55, 500},{1546, 10643, 193, 193, 55, 800},{2148, 11373, 195, 195, 55, 500}
        	            ,{1129, 10653, 194, 194, 55, 800},{1792, 11618, 196, 196, 55, 500},{2126, 11055, 182, 182, 55, 500},{1502, 10914, 200, 200, 55, 500}
        	            ,{1059, 11567, 189, 189, 55, 500},{1920, 10761, 186, 186, 55, 500},{971, 11181, 187, 187, 55, 800},{944, 10737, 197, 197, 55, 500}
        	            ,{994, 10984, 192, 192, 55, 500},{1572, 11317, 198, 198, 55, 500},{1416, 11196, 191, 191, 55, 500},{953, 11430, 200, 200, 55, 500}
        	            ,{1338, 10589, 193, 193, 55, 500},{1334, 10830, 200, 200, 55, 500},{2716, 3866, 184, 184, 55, 500},{1240, 3242, 192, 192, 55, 500}
        	            ,{2599, 4225, 190, 190, 55, 500},{2355, 3978, 196, 196, 55, 500},{1714, 10893, 182, 182, 55, 500},{2957, 4211, 196, 196, 55, 500}
        	            ,{1103, 3364, 183, 183, 55, 500},{3372, 4239, 181, 181, 55, 800},{8205, 4773, 186, 186, 55, 500},{9085, 4754, 187, 187, 55, 500}
        	            ,{10899, 3953, 181, 181, 55, 500},{8201, 4040, 189, 189, 55, 500},{9311, 3388, 190, 190, 55, 800},{6012, 4646, 188, 188, 55, 800}
        	            ,{8693, 3974, 184, 184, 55, 500},{9659, 4257, 194, 194, 55, 500},{10996, 3381, 185, 185, 55, 500},{5271, 3914, 189, 189, 55, 500}
        	            ,{6985, 3290, 186, 186, 55, 500},{5242, 4103, 187, 187, 55, 500},{10180, 4299, 189, 189, 55, 800},{4843, 4219, 191, 191, 55, 800}
        	            ,{7745, 4975, 196, 196, 55, 800},{8210, 4339, 198, 198, 55, 500},{10553, 4260, 189, 189, 55, 800},{9825, 3322, 185, 185, 55, 500}
        	            ,{9604, 3030, 188, 188, 55, 500},{8828, 3129, 182, 182, 55, 500},{4987, 3073, 182, 182, 55, 500},{6481, 4228, 181, 181, 55, 500}
        	            ,{4746, 4593, 198, 198, 55, 500},{8300, 3668, 188, 188, 55, 500},{6100, 4063, 189, 189, 55, 800},{6909, 4023, 186, 186, 55, 500}
        	            ,{4537, 4356, 188, 188, 55, 500},{9864, 3569, 180, 180, 55, 500},{7832, 4128, 184, 184, 55, 800},{5181, 4449, 181, 181, 55, 500}
        	            ,{6580, 3645, 198, 198, 55, 500},{10709, 3470, 195, 195, 55, 500},{8701, 3591, 182, 182, 55, 500},{4743, 3143, 200, 200, 55, 800}
        	            ,{9113, 4569, 190, 190, 55, 500},{5383, 4733, 196, 196, 55, 800},{6883, 3791, 200, 200, 55, 500},{5047, 4962, 196, 196, 55, 500}
        	            ,{4638, 4735, 197, 197, 55, 500},{10110, 3001, 185, 185, 55, 500},{6249, 3429, 193, 193, 55, 500},{4928, 3962, 194, 194, 55, 500}
        	            ,{7575, 4451, 188, 188, 55, 800},{7755, 3859, 187, 187, 55, 500},{9656, 3406, 198, 198, 55, 500},{10877, 4382, 188, 188, 55, 500}
        	            ,{5213, 3637, 181, 181, 55, 500},{7224, 3031, 185, 185, 55, 500},{7859, 4552, 199, 199, 55, 500},{5466, 3275, 195, 195, 55, 800}
        	            ,{6518, 4447, 196, 196, 55, 500},{5327, 3022, 188, 188, 55, 800},{8488, 3363, 180, 180, 55, 500},{10087, 4145, 185, 185, 55, 500}
        	            ,{8668, 3390, 187, 187, 55, 500},{10495, 4575, 181, 181, 55, 500},{5572, 3858, 181, 181, 55, 500},{9773, 3785, 199, 199, 55, 500}
        	            ,{7946, 3435, 188, 188, 55, 500},{4503, 3552, 195, 195, 55, 500},{9355, 3622, 196, 196, 55, 500},{4660, 3431, 188, 188, 55, 500}
        	            ,{9431, 4314, 195, 195, 55, 500},{9226, 3205, 199, 199, 55, 800},{4844, 3299, 181, 181, 55, 500},{6246, 3904, 194, 194, 55, 500}
        	            ,{6615, 3019, 186, 186, 55, 500},{7194, 4700, 199, 199, 55, 800},{5238, 3427, 196, 196, 55, 500},{7544, 4274, 197, 197, 55, 500}
        	            ,{8212, 3260, 193, 193, 55, 800},{9310, 3990, 181, 181, 55, 500},{7662, 3374, 199, 199, 55, 500},{7046, 4860, 188, 188, 55, 500}
        	            ,{6370, 4674, 187, 187, 55, 500},{9500, 3510, 182, 182, 55, 800},{10442, 3388, 191, 191, 55, 500},{5798, 4514, 199, 199, 55, 500}
        	            ,{9652, 4925, 185, 185, 55, 500},{9037, 3402, 200, 200, 55, 800},{10127, 4945, 190, 190, 55, 500},{7680, 4806, 198, 198, 55, 800}
        	            ,{8452, 4236, 185, 185, 55, 500},{8697, 4971, 186, 186, 55, 500},{5937, 4903, 186, 186, 55, 500},{10401, 3140, 189, 189, 55, 500}
        	            ,{5535, 3467, 195, 195, 55, 800},{9853, 4745, 185, 185, 55, 800},{6239, 4878, 193, 193, 55, 500},{6170, 4533, 188, 188, 55, 500}
        	            ,{6705, 3842, 188, 188, 55, 500},{8151, 3510, 196, 196, 55, 500},{5943, 3577, 188, 188, 55, 500},{7521, 4087, 198, 198, 55, 800}
        	            ,{9187, 4992, 186, 186, 55, 500},{7466, 3283, 191, 191, 55, 500},{9881, 4500, 187, 187, 55, 500},{5649, 4208, 183, 183, 55, 500}
        	            ,{4787, 4891, 190, 190, 55, 800},{4646, 3972, 194, 194, 55, 500},{5113, 3987, 196, 196, 55, 500},{7048, 3653, 187, 187, 55, 500}
        	            ,{8973, 3753, 186, 186, 55, 500},{10264, 3265, 186, 186, 55, 500},{10998, 4973, 193, 193, 55, 500},{7298, 3720, 184, 184, 55, 500}
        	            ,{8377, 4844, 197, 197, 55, 500},{5136, 4784, 194, 194, 55, 500},{7917, 4933, 184, 184, 55, 800},{10976, 3061, 188, 188, 55, 500}
        	            ,{8749, 4536, 200, 200, 55, 500},{7041, 4231, 189, 189, 55, 500},{8573, 4597, 182, 182, 55, 800},{4592, 3753, 191, 191, 55, 500}
        	            ,{7305, 4351, 191, 191, 55, 500},{7815, 4363, 200, 200, 55, 500},{10681, 4872, 195, 195, 55, 500},{6298, 4194, 189, 189, 55, 500}
        	            ,{7092, 3429, 188, 188, 55, 500},{8567, 3718, 192, 192, 55, 500},{6767, 4683, 187, 187, 55, 500},{5885, 3231, 190, 190, 55, 800}
        	            ,{6754, 4895, 184, 184, 55, 500},{9626, 4064, 180, 180, 55, 800},{7083, 4466, 182, 182, 55, 800},{10056, 4451, 190, 190, 55, 800}
        	            ,{6802, 4168, 182, 182, 55, 500},{9120, 3039, 188, 188, 55, 800},{6055, 3050, 185, 185, 55, 500},{10801, 4699, 192, 192, 55, 500}
        	            ,{5393, 4290, 186, 186, 55, 500},{8885, 4341, 200, 200, 55, 500},{7520, 4962, 197, 197, 55, 500},{5963, 4462, 182, 182, 55, 800}
        	            ,{7332, 4105, 188, 188, 55, 500},{9533, 4548, 191, 191, 55, 500},{5804, 3805, 180, 180, 55, 500},{6334, 3178, 188, 188, 55, 500}
        	            ,{10417, 3794, 187, 187, 55, 500},{9005, 4164, 180, 180, 55, 500},{10414, 4773, 190, 190, 55, 500},{7696, 3179, 183, 183, 55, 800}
        	            ,{8634, 3169, 186, 186, 55, 500},{10124, 4613, 193, 193, 55, 500},{10427, 4061, 190, 190, 55, 500},{8105, 4936, 196, 196, 55, 500}
        	            ,{9077, 3594, 183, 183, 55, 800},{5656, 4983, 195, 195, 55, 500},{5436, 4030, 199, 199, 55, 800},{8909, 4985, 188, 188, 55, 500}
        	            ,{8913, 3939, 186, 186, 55, 800},{6517, 3969, 184, 184, 55, 500},{6631, 3265, 193, 193, 55, 500},{10128, 3581, 184, 184, 55, 500}
        	            ,{7411, 3500, 199, 199, 55, 500},{8681, 4276, 180, 180, 55, 500},{8844, 4799, 184, 184, 55, 500},{8078, 3047, 188, 188, 55, 500}
        	            ,{8838, 3442, 200, 200, 55, 500},{9305, 4754, 192, 192, 55, 800},{7347, 4848, 184, 184, 55, 500},{5700, 3388, 192, 192, 55, 500}
        	            ,{10012, 3195, 186, 186, 55, 500},{10078, 3946, 184, 184, 55, 500},{7404, 3880, 183, 183, 55, 500},{9693, 4435, 188, 188, 55, 800}
        	            ,{6098, 4238, 188, 188, 55, 500},{5452, 4524, 187, 187, 55, 500},{8036, 4567, 189, 189, 55, 500},{6401, 3559, 186, 186, 55, 800}
        	            ,{9541, 3221, 199, 199, 55, 500},{4869, 3791, 180, 180, 55, 800},{6822, 4359, 194, 194, 55, 500},{6925, 3055, 191, 191, 55, 500}
        	            ,{10682, 4401, 196, 196, 55, 500},{9265, 4480, 187, 187, 55, 800},{6555, 4993, 195, 195, 55, 500},{5306, 4919, 197, 197, 55, 800}
        	            ,{7917, 3933, 183, 183, 55, 800},{6647, 4309, 199, 199, 55, 500},{6779, 3148, 186, 186, 55, 500},{10691, 3177, 186, 186, 55, 500}
        	            ,{8465, 3980, 186, 186, 55, 500},{9609, 4716, 183, 183, 55, 500},{9923, 4236, 197, 197, 55, 800},{7675, 4630, 187, 187, 55, 500}
        	            ,{5440, 3621, 194, 194, 55, 500},{8258, 3035, 192, 192, 55, 500},{5600, 4641, 190, 190, 55, 800},{4974, 4346, 199, 199, 55, 500}
        	            ,{8432, 4420, 185, 185, 55, 800},{5049, 3736, 183, 183, 55, 500},{8252, 4580, 191, 191, 55, 500},{10959, 3601, 185, 185, 55, 500}
        	            ,{10712, 4174, 200, 200, 55, 500},{7474, 4701, 200, 200, 55, 500},{10709, 3833, 186, 186, 55, 500},{8617, 4788, 199, 199, 55, 500}
        	            ,{10486, 4979, 182, 182, 55, 500},{6925, 3499, 191, 191, 55, 800},{7157, 4020, 182, 182, 55, 500}
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

            // Kolom ke-7 (index 6) itu OPSIONAL: 1 = ada bayangan, 0 = gak ada.
            // Kalau entry-nya cuma 6 angka (format lama), default tetap 1
            // (ada bayangan) biar semua data pohon lama gak perlu diubah.
            int shadowFlag = (data[i].length >= 7) ? data[i][6] : 1;

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
                breakables[i].hasShadow = (shadowFlag != 0);

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
        // Cuma telusuri tile yang kelihatan di layar (sekitar 17x13), bukan seluruh peta 700x700
        int leftWorld = camX - gp.player.screenX;
        int topWorld  = camY - gp.player.screenY;
        int startCol = Math.max(0, leftWorld / gp.tileSize);
        int endCol   = Math.min(gp.maxWorldCol - 1, (leftWorld + gp.screenWidth) / gp.tileSize + 1);
        int startRow = Math.max(0, topWorld / gp.tileSize);
        int endRow   = Math.min(gp.maxWorldRow - 1, (topWorld + gp.screenHeight) / gp.tileSize + 1);

        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
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

                    if (tileAnimImage[tileNum] != null) {
                        // tile GIF animasi -> ambil frame yang lagi "hidup" sekarang dari Toolkit
                        g2.drawImage(tileAnimImage[tileNum], screenX, screenY,
                                     gp.tileSize, gp.tileSize, null);
                    } else {
                        g2.drawImage(tile[tileNum].image, screenX, screenY,
                                     gp.tileSize, gp.tileSize, null);
                    }
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

			        // 🔥 Bayangan sekarang ngikutin b.hasShadow (diisi dari data[] pas load)
			        if (b.hasShadow) {
			            java.awt.Composite old = g2.getComposite();
			            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
			            g2.setColor(Color.BLACK);

			            int shadowOffsetX = 20;
			            int shadowScaleH  = (int)(b.height * 0.25f);

			            g2.fillOval(
			                sx + shadowOffsetX,
			                sy + b.height - (shadowScaleH / 2) - 10,
			                (int)(b.width * 0.75f),
			                shadowScaleH
			            );
			            g2.setComposite(old);
			        }

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
			
			drawObjectCollisionDebug(g2, fCamX, fCamY);
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
	    
	    // collision witch house
	    gp.objCol.add(4000, 3820, 310 , 140);
	    
	    
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
	        {3960, 3660, 380, 360, 70, 0, 0},
	        {4300, 4600, 300, 300, 72, 0, 0},

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
	      

	        // ── RUINS BESAR ──
	        {1300,2100,200,200,49,0,  0},
	        {1400,110000,200,200,50,0,  0},
	        
	        // semak/Bush
	        {1005,3175,100,100,66,0,  1},{1305,3110,100,100,66,0,  1},
	        {985,3185,100,100,66,0,  1},{1325,3035,100,100,66,0,  1},
	        {1020,3185,100,100,66,0,  1},{1350,3045,100,100,66,0,  1},
	        {1040,3170,100,100,66,0,  1},{1260,3200,100,100,66,0,  1},
	        {1240,3200,100,100,66,0,  1},
	        {1040,3170,100,100,66,0,  1},
	        {1100,3240,100,100,66,0,  1},
	        {1100,3200,100,100,66,0,  1},{1782,3565,100,100,66,0,1},{1802,3575,100,100,66,0,1},{1822,3560,100,100,66,0,1},{1762,3580,100,100,66,0,1},
	        {1739,3131,100,100,66,0,1},{1759,3141,100,100,66,0,1},{1719,3121,100,100,66,0,1},
	        {3379,3158,100,100,66,0,1},{3399,3168,100,100,66,0,1},{3359,3148,100,100,66,0,1},{3389,3188,100,100,66,0,1},
	        {2919,3452,100,100,66,0,1},{2939,3462,100,100,66,0,1},{2899,3442,100,100,66,0,1},
	        {2765,4161,100,100,66,0,1},{2785,4171,100,100,66,0,1},{2745,4151,100,100,66,0,1},{2775,4141,100,100,66,0,1},{2755,4181,100,100,66,0,1},
	        {1207,3681,100,100,66,0,1},{1227,3691,100,100,66,0,1},{1187,3671,100,100,66,0,1},
	        {2388,3439,100,100,66,0,1},{2408,3449,100,100,66,0,1},{2368,3429,100,100,66,0,1},
	        {3471,4010,100,100,66,0,1},{3491,4020,100,100,66,0,1},{3451,4000,100,100,66,0,1},{3481,3990,100,100,66,0,1},
	        {2612,3539,100,100,66,0,1},{2632,3549,100,100,66,0,1},{2592,3529,100,100,66,0,1},
	        {1825,4049,100,100,66,0,1},{1845,4059,100,100,66,0,1},{1805,4039,100,100,66,0,1},{1835,4079,100,100,66,0,1},
	        {1998,3071,100,100,66,0,1},{2018,3081,100,100,66,0,1},{1978,3061,100,100,66,0,1},
	        {2093,4009,100,100,66,0,1},{2113,4019,100,100,66,0,1},{2073,3999,100,100,66,0,1},
	        {2297,4220,100,100,66,0,1},{2317,4230,100,100,66,0,1},{2277,4210,100,100,66,0,1},{2307,4250,100,100,66,0,1},
	        {3177,4213,100,100,66,0,1},{3197,4223,100,100,66,0,1},{3157,4203,100,100,66,0,1},
	        {1466,4188,100,100,66,0,1},{1486,4198,100,100,66,0,1},{1446,4178,100,100,66,0,1},
	        {1426,3156,100,100,66,0,1},{1446,3166,100,100,66,0,1},{1406,3146,100,100,66,0,1},{1436,3186,100,100,66,0,1},
	        {2974,3215,100,100,66,0,1},{2994,3225,100,100,66,0,1},{2954,3205,100,100,66,0,1},

	        {3152,3369,100,100,66,0,1},{3179,3318,100,100,66,0,1},{3097,3384,100,100,66,0,1},

	     // pohon (2765,4161) -> merengang (nyebar)

	     {2678,4165,100,100,66,0,1},{2849,4198,100,100,66,0,1},{2824,4057,100,100,66,0,1},

	     // pohon (3379,3158) -> huge bush, dempet gerombol gede

	     {3342,3125,100,100,66,0,1},{3365,3121,100,100,66,0,1},{3420,3135,100,100,66,0,1},{3388,3180,100,100,66,0,1},{3372,3188,100,100,66,0,1},{3324,3176,100,100,66,0,1},{3317,3148,100,100,66,0,1},{3330,3131,100,100,66,0,1},{3359,3104,100,100,66,0,1},{3390,3163,100,100,66,0,1},{3352,3171,100,100,66,0,1},

	     // pohon (1998,3071) -> campuran, agak natural

	     {2010,3103,100,100,66,0,1},{1958,3072,100,100,66,0,1},{1985,3018,100,100,66,0,1},{2025,3061,100,100,66,0,1},{1979,3084,100,100,66,0,1},

	     // pohon (1825,4049) -> dempet banget (numpuk), random

	     {1853,4057,100,100,66,0,1},{1799,4047,100,100,66,0,1},{1841,4016,100,100,66,0,1},

	     // pohon (2297,4220) -> merengang (nyebar)

	     {2374,4143,100,100,66,0,1},{2268,4117,100,100,66,0,1},{2197,4168,100,100,66,0,1},

	     // pohon (1681,3356) -> huge bush, dempet gerombol gede

	     {1638,3336,100,100,66,0,1},{1701,3305,100,100,66,0,1},{1764,3333,100,100,66,0,1},{1756,3397,100,100,66,0,1},{1712,3434,100,100,66,0,1},{1668,3442,100,100,66,0,1},{1618,3395,100,100,66,0,1},{1607,3354,100,100,66,0,1},{1658,3307,100,100,66,0,1},{1678,3375,100,100,66,0,1},

	     // pohon (2874,3944) -> campuran, agak natural

	     {2854,3942,100,100,66,0,1},{2905,3934,100,100,66,0,1},{2852,3976,100,100,66,0,1},{2884,3959,100,100,66,0,1},{2871,3926,100,100,66,0,1},

	     // pohon (1426,3156) -> dempet banget (numpuk), random

	     {1412,3172,100,100,66,0,1},{1446,3150,100,100,66,0,1},{1423,3120,100,100,66,0,1},

	     // pohon (3533,3764) -> merengang (nyebar)

	     {3475,3878,100,100,66,0,1},{3595,3701,100,100,66,0,1},{3444,3649,100,100,66,0,1},

	     // pohon (1231,4245) -> dempet banget (numpuk), random

	     {1213,4268,100,100,66,0,1},{1254,4243,100,100,66,0,1},{1227,4205,100,100,66,0,1},{1238,4277,100,100,66,0,1},

	     {3163,3355,100,100,66,0,1},{3167,3331,100,100,66,0,1},{3128,3385,100,100,66,0,1},

	     // pohon (2765,4161) -> merengang (nyebar)

	     {2675,4168,100,100,66,0,1},{2843,4200,100,100,66,0,1},{2810,4055,100,100,66,0,1},

	     // pohon (3379,3158) -> huge bush, dempet gerombol gede

	     {3335,3135,100,100,66,0,1},{3375,3108,100,100,66,0,1},{3416,3145,100,100,66,0,1},{3404,3170,100,100,66,0,1},{3366,3197,100,100,66,0,1},{3328,3169,100,100,66,0,1},{3308,3158,100,100,66,0,1},{3327,3123,100,100,66,0,1},{3350,3102,100,100,66,0,1},{3386,3181,100,100,66,0,1},{3361,3163,100,100,66,0,1},

	     // pohon (1998,3071) -> campuran, agak natural

	     {2003,3112,100,100,66,0,1},{1961,3063,100,100,66,0,1},{1996,3015,100,100,66,0,1},{2024,3073,100,100,66,0,1},{1982,3086,100,100,66,0,1},

	     // pohon (1825,4049) -> dempet banget (numpuk), random

	     {1848,4068,100,100,66,0,1},{1801,4035,100,100,66,0,1},{1834,4014,100,100,66,0,1},

	     // pohon (2297,4220) -> merengang (nyebar)

	     {2385,4138,100,100,66,0,1},{2258,4118,100,100,66,0,1},{2187,4168,100,100,66,0,1},

	     // pohon (1681,3356) -> huge bush, dempet gerombol gede

	     {1627,3346,100,100,66,0,1},{1712,3295,100,100,66,0,1},{1761,3350,100,100,66,0,1},{1758,3406,100,100,66,0,1},{1722,3435,100,100,66,0,1},{1659,3432,100,100,66,0,1},{1610,3405,100,100,66,0,1},{1598,3341,100,100,66,0,1},{1647,3308,100,100,66,0,1},{1690,3374,100,100,66,0,1},

	     // pohon (2874,3944) -> campuran, agak natural

	     {2842,3932,100,100,66,0,1},{2918,3948,100,100,66,0,1},{2861,3975,100,100,66,0,1},{2897,3945,100,100,66,0,1},{2875,3923,100,100,66,0,1},

	     // pohon (1426,3156) -> dempet banget (numpuk), random

	     {1401,3180,100,100,66,0,1},{1455,3142,100,100,66,0,1},{1414,3121,100,100,66,0,1},

	     // pohon (3533,3764) -> merengang (nyebar)

	     {3486,3882,100,100,66,0,1},{3596,3690,100,100,66,0,1},{3446,3657,100,100,66,0,1},

	     // pohon (1231,4245) -> dempet banget (numpuk), random

	     {1201,4269,100,100,66,0,1},{1265,4232,100,100,66,0,1},{1215,4203,100,100,66,0,1},{1248,4285,100,100,66,0,1},
	        
	        
	        // Ice Biome
	        {1415,11005,100,100,66,0,  1},{1500,11005,100,100,66,0,  1},
            {1500,11105,100,100,66,0,  1},{1530,11045,100,100,66,0,  1},
            {1530,11105,100,100,66,0,  1},{1530,11005,100,100,66,0,  1},
            {1550,11055,100,100,66,0,  1},{1550,11035,100,100,66,0,  1},
            {1470,11025,100,100,66,0,  1},{1550,11005,100,100,66,0,  1},
	        // HUGE BUSH (numpuk jadi 1 massa gede)
	        {1660,11173,100,100,66,0,  1},{1695,11178,100,100,66,0,  1},{1757,11177,100,100,66,0,  1},
	        {1809,11173,100,100,66,0,  1},{1657,11227,100,100,66,0,  1},{1713,11238,100,100,66,0,  1},
	        {1751,11225,100,100,66,0,  1},{1807,11231,100,100,66,0,  1},{1647,11296,100,100,66,0,  1},
	        {1714,11280,100,100,66,0,  1},{1767,11286,100,100,66,0,  1},{1825,11297,100,100,66,0,  1},
	        // MEDIUM cluster A (kiri atas)
	        {1280,10870,100,100,66,0,  1},{1375,10880,100,100,66,0,  1},{1470,10855,100,100,66,0,  1},
	        {1340,10970,100,100,66,0,  1},{1440,10965,100,100,66,0,  1},
	        // MEDIUM cluster B (kanan)
	        {1780,10920,100,100,66,0,  1},{1880,10935,100,100,66,0,  1},{1980,10910,100,100,66,0,  1},
	        {1850,11030,100,100,66,0,  1},{1960,11020,100,100,66,0,  1},
	        // SOLO / sendiri-sendiri (jauh dari yang lain)
	        {1150,11150,100,100,66,0,  1},{1980,11260,100,100,66,0,  1},{1300,11480,100,100,66,0,  1},
	        {1650,10730,100,100,66,0,  1},{1870,11570,100,100,66,0,  1},{1080,10930,100,100,66,0,  1},
	        {2020,11020,100,100,66,0,  1},{1420,11620,100,100,66,0,  1},
	        
	        

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