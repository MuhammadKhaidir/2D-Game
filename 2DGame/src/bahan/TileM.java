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
// index tile tiap breakable (sejajar dengan breakables[]): 55 = pohon, 40/41/71 = batu. Dipakai MiniMap.
public int[] breakableTile;

// Ukuran peta yang BENAR-BENAR terisi di map01.txt (bukan 700x700 yang cuma kapasitas array):
// loadedCols = panjang baris yang paling umum, loadedRows = jumlah baris.
// Dipakai MiniMap supaya yang ditampilkan cuma area dunia yang nyata.
public int loadedCols = 0;
public int loadedRows = 0;

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
	        java.util.Map<Integer, Integer> rowLengths = new java.util.HashMap<>();

	        while (row < gp.maxWorldRow) {
	            String line = br.readLine();
	            if (line == null) break;

	            String numbers[] = line.trim().split("\\s+");

	            for (int col = 0; col < numbers.length && col < gp.maxWorldCol; col++) {
	                mapTileNum[col][row] = Integer.parseInt(numbers[col]);
	            }

	            int len = Math.min(numbers.length, gp.maxWorldCol);
	            rowLengths.merge(len, 1, Integer::sum);

	            row++;
	        }
	        loadedRows = row;

	        // Lebar peta = panjang baris yang paling umum. Beberapa baris di map01.txt kelebihan kolom
	        // (ada yang sampai 480), jadi memakai "yang terpanjang" bikin minimap melebar dan setengahnya kosong.
	        int bestLen = 0, bestCount = -1;
	        for (java.util.Map.Entry<Integer, Integer> e : rowLengths.entrySet()) {
	            if (e.getValue() > bestCount || (e.getValue() == bestCount && e.getKey() > bestLen)) {
	                bestLen = e.getKey();
	                bestCount = e.getValue();
	            }
	        }
	        loadedCols = bestLen;

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

	    if (screenX + width > gp.viewMinX() && screenX < gp.viewMaxX() &&
	        screenY + height > gp.viewMinY() && screenY < gp.viewMaxY()) {

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

	    if (screenX + w > gp.viewMinX() && screenX < gp.viewMaxX() &&
	        screenY + h > gp.viewMinY() && screenY < gp.viewMaxY()) {

	        g2.drawImage(img, screenX, screenY, w, h, null);
	    }
	}
			
	//buat load objek yang bisa diancurin

	public void loadBreakableObjects() {
        int[][] data = {
        	            // ===== BATU (tidak diubah) =====
        	            {1460, 660, 70, 70, 40, 500}
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

        	            // ===== POHON (703 buah) — disebar merata di seluruh area darat =====
        	            // Posisi udah dicek satu-satu: gak ada yang di air / jalan tanah / rumah & area pintunya / ruins /
        	            // batu / semak / tembok collision / titik spawn monster, dan jarak antar pohon min ~250px biar gak numpuk.
        	            // Format: {x, y, lebar, tinggi, 55 (pohon), hp}
        	            ,{2971, 510, 190, 190, 55, 500},{3335, 663, 184, 184, 55, 500},{3825, 657, 184, 184, 55, 500},{4284, 649, 183, 183, 55, 500},{4580, 540, 172, 172, 55, 500},{4973, 625, 172, 172, 55, 500}
        	            ,{5383, 621, 196, 196, 55, 500},{6320, 522, 183, 183, 55, 500},{6952, 582, 178, 178, 55, 800},{7562, 689, 200, 200, 55, 500},{8003, 492, 175, 175, 55, 800},{8567, 480, 189, 189, 55, 500}
        	            ,{9049, 495, 197, 197, 55, 800},{9480, 546, 174, 174, 55, 500},{9795, 696, 177, 177, 55, 500},{10248, 521, 175, 175, 55, 500},{10743, 487, 182, 182, 55, 800},{4059, 1033, 179, 179, 55, 500}
        	            ,{4351, 991, 173, 173, 55, 500},{4664, 932, 183, 183, 55, 500},{5222, 976, 177, 177, 55, 500},{5792, 793, 173, 173, 55, 500},{6226, 1391, 191, 191, 55, 500},{6256, 1009, 189, 189, 55, 500}
        	            ,{6744, 1159, 180, 180, 55, 500},{7138, 1111, 189, 189, 55, 500},{7643, 1084, 174, 174, 55, 500},{8371, 1124, 188, 188, 55, 500},{8409, 725, 190, 190, 55, 500},{8921, 1192, 179, 179, 55, 800}
        	            ,{9565, 1018, 190, 190, 55, 800},{9896, 1106, 191, 191, 55, 500},{10593, 915, 175, 175, 55, 800},{11007, 831, 178, 178, 55, 500},{1786, 1990, 175, 175, 55, 500},{2340, 1940, 194, 194, 55, 500}
        	            ,{3531, 1872, 172, 172, 55, 800},{4018, 1976, 170, 170, 55, 800},{4411, 1703, 195, 195, 55, 500},{4986, 1901, 182, 182, 55, 800},{5363, 1722, 180, 180, 55, 500},{5688, 1977, 188, 188, 55, 800}
        	            ,{6102, 1717, 196, 196, 55, 800},{6456, 1794, 172, 172, 55, 500},{6490, 1465, 200, 200, 55, 500},{6879, 1462, 200, 200, 55, 500},{6994, 1949, 189, 189, 55, 500},{7393, 1539, 177, 177, 55, 500}
        	            ,{7545, 2047, 183, 183, 55, 500},{7936, 1435, 180, 180, 55, 800},{8142, 2019, 176, 176, 55, 500},{8623, 1695, 170, 170, 55, 500},{8925, 1752, 188, 188, 55, 800},{9031, 1522, 174, 174, 55, 500}
        	            ,{9297, 1415, 185, 185, 55, 500},{9673, 1463, 184, 184, 55, 500},{9813, 2050, 173, 173, 55, 500},{10132, 1452, 200, 200, 55, 500},{10613, 1963, 194, 194, 55, 800},{11080, 1968, 199, 199, 55, 500}
        	            ,{521, 2573, 171, 171, 55, 800},{1677, 2673, 184, 184, 55, 500},{2177, 2306, 178, 178, 55, 500},{2407, 2607, 188, 188, 55, 500},{2851, 2243, 181, 181, 55, 500},{3082, 2706, 197, 197, 55, 500}
        	            ,{3160, 2443, 200, 200, 55, 500},{3325, 2188, 193, 193, 55, 500},{3528, 2490, 179, 179, 55, 500},{3726, 2228, 176, 176, 55, 500},{3971, 2574, 197, 197, 55, 500},{4422, 2206, 187, 187, 55, 500}
        	            ,{4471, 2661, 179, 179, 55, 500},{4851, 2375, 192, 192, 55, 500},{5367, 2333, 197, 197, 55, 500},{5474, 2711, 170, 170, 55, 500},{5667, 2499, 194, 194, 55, 500},{5958, 2525, 179, 179, 55, 500}
        	            ,{6002, 2184, 187, 187, 55, 500},{6270, 2504, 179, 179, 55, 500},{6439, 2104, 200, 200, 55, 500},{6773, 2521, 193, 193, 55, 500},{7317, 2720, 179, 179, 55, 500},{8026, 2551, 188, 188, 55, 500}
        	            ,{8454, 2357, 179, 179, 55, 500},{8577, 2649, 187, 187, 55, 500},{8781, 2452, 199, 199, 55, 500},{8824, 2166, 172, 172, 55, 500},{9048, 2708, 176, 176, 55, 500},{9279, 2216, 182, 182, 55, 500}
        	            ,{9450, 2630, 189, 189, 55, 500},{9779, 2376, 171, 171, 55, 500},{10134, 2570, 174, 174, 55, 500},{10214, 2162, 180, 180, 55, 500},{10783, 2354, 182, 182, 55, 500},{10948, 2731, 175, 175, 55, 800}
        	            ,{11210, 2352, 180, 180, 55, 500},{891, 3422, 179, 179, 55, 500},{1869, 3320, 175, 175, 55, 500},{2428, 3002, 199, 199, 55, 800},{2727, 3313, 172, 172, 55, 500},{2783, 2830, 193, 193, 55, 500}
        	            ,{3091, 3050, 197, 197, 55, 500},{3352, 3362, 197, 197, 55, 500},{3576, 2881, 187, 187, 55, 500},{4090, 3215, 181, 181, 55, 500},{4645, 3230, 179, 179, 55, 500},{5201, 3357, 175, 175, 55, 500}
        	            ,{5294, 2944, 195, 195, 55, 500},{5617, 3224, 181, 181, 55, 500},{5776, 2907, 180, 180, 55, 500},{6249, 3001, 171, 171, 55, 500},{6651, 3172, 189, 189, 55, 800},{6825, 3460, 176, 176, 55, 500}
        	            ,{7062, 3100, 197, 197, 55, 500},{7344, 3431, 196, 196, 55, 800},{7672, 3170, 193, 193, 55, 800},{7846, 2965, 171, 171, 55, 800},{8011, 3255, 180, 180, 55, 500},{8143, 2939, 183, 183, 55, 500}
        	            ,{8659, 3026, 180, 180, 55, 500},{9232, 3096, 190, 190, 55, 500},{9698, 3084, 194, 194, 55, 500},{10127, 3216, 176, 176, 55, 500},{10445, 2865, 199, 199, 55, 500},{10880, 3055, 179, 179, 55, 500}
        	            ,{11163, 3303, 176, 176, 55, 800},{473, 4058, 174, 174, 55, 500},{1073, 3759, 194, 194, 55, 500},{1597, 3868, 187, 187, 55, 500},{1957, 4164, 199, 199, 55, 500},{2042, 3737, 178, 178, 55, 500}
        	            ,{2407, 3723, 190, 190, 55, 800},{2928, 3712, 174, 174, 55, 500},{3468, 4147, 174, 174, 55, 800},{4560, 4168, 178, 178, 55, 500},{4711, 3735, 196, 196, 55, 500},{4844, 4126, 200, 200, 55, 500}
        	            ,{5160, 3972, 179, 179, 55, 500},{5576, 3589, 197, 197, 55, 500},{5654, 4024, 197, 197, 55, 500},{6178, 3966, 191, 191, 55, 500},{6304, 3550, 190, 190, 55, 500},{6748, 3802, 176, 176, 55, 500}
        	            ,{6776, 4128, 190, 190, 55, 500},{7230, 3963, 172, 172, 55, 500},{7775, 3957, 180, 180, 55, 500},{7861, 3611, 171, 171, 55, 500},{8458, 3732, 198, 198, 55, 500},{8998, 3982, 187, 187, 55, 500}
        	            ,{9166, 3533, 189, 189, 55, 500},{9293, 3874, 177, 177, 55, 500},{9570, 3539, 176, 176, 55, 800},{9806, 3898, 175, 175, 55, 500},{10101, 3519, 175, 175, 55, 500},{10174, 3913, 184, 184, 55, 800}
        	            ,{10693, 3549, 190, 190, 55, 500},{11171, 3924, 191, 191, 55, 500},{507, 4801, 179, 179, 55, 800},{541, 4457, 181, 181, 55, 500},{1268, 4462, 177, 177, 55, 500},{2259, 4582, 187, 187, 55, 500}
        	            ,{2525, 4255, 182, 182, 55, 500},{2793, 4779, 176, 176, 55, 800},{3022, 4270, 192, 192, 55, 500},{3262, 4645, 191, 191, 55, 500},{3508, 4791, 186, 186, 55, 500},{3521, 4437, 199, 199, 55, 800}
        	            ,{3826, 4635, 175, 175, 55, 500},{5256, 4531, 191, 191, 55, 500},{5808, 4752, 190, 190, 55, 500},{5875, 4381, 194, 194, 55, 500},{6369, 4346, 199, 199, 55, 500},{6635, 4773, 177, 177, 55, 800}
        	            ,{6975, 4477, 199, 199, 55, 500},{7011, 4820, 181, 181, 55, 500},{7319, 4627, 183, 183, 55, 500},{7706, 4464, 180, 180, 55, 500},{8086, 4218, 186, 186, 55, 500},{8368, 4585, 176, 176, 55, 500}
        	            ,{8741, 4248, 171, 171, 55, 500},{8987, 4505, 189, 189, 55, 500},{9295, 4249, 200, 200, 55, 800},{9720, 4649, 182, 182, 55, 500},{9998, 4322, 175, 175, 55, 500},{10081, 4627, 180, 180, 55, 500}
        	            ,{10471, 4269, 197, 197, 55, 500},{10842, 4497, 197, 197, 55, 500},{11031, 4899, 170, 170, 55, 500},{11224, 4366, 171, 171, 55, 500},{500, 5348, 199, 199, 55, 500},{1057, 4947, 184, 184, 55, 500}
        	            ,{1217, 5509, 182, 182, 55, 500},{1400, 5240, 192, 192, 55, 800},{2072, 5324, 172, 172, 55, 500},{2455, 5402, 190, 190, 55, 500},{2459, 4999, 176, 176, 55, 500},{2792, 5173, 170, 170, 55, 500}
        	            ,{3112, 4936, 180, 180, 55, 500},{3223, 5339, 178, 178, 55, 500},{3667, 5171, 188, 188, 55, 500},{4077, 5536, 184, 184, 55, 800},{4423, 5382, 184, 184, 55, 500},{4747, 5544, 195, 195, 55, 800}
        	            ,{4994, 5158, 182, 182, 55, 800},{5470, 4961, 184, 184, 55, 500},{5484, 5414, 177, 177, 55, 500},{5902, 5492, 194, 194, 55, 500},{6189, 5116, 185, 185, 55, 500},{6388, 5569, 174, 174, 55, 500}
        	            ,{6683, 5173, 170, 170, 55, 500},{7053, 5541, 181, 181, 55, 500},{7441, 5122, 170, 170, 55, 500},{7785, 5367, 172, 172, 55, 500},{7961, 5026, 184, 184, 55, 500},{8040, 5414, 194, 194, 55, 500}
        	            ,{8350, 5339, 173, 173, 55, 500},{8369, 5018, 183, 183, 55, 800},{8697, 4977, 184, 184, 55, 500},{9145, 5330, 194, 194, 55, 500},{9199, 4907, 196, 196, 55, 500},{9590, 5079, 199, 199, 55, 500}
        	            ,{10027, 5521, 185, 185, 55, 500},{10406, 4974, 194, 194, 55, 500},{10428, 5443, 173, 173, 55, 500},{10842, 5230, 197, 197, 55, 500},{10933, 5571, 187, 187, 55, 500},{11151, 5184, 177, 177, 55, 500}
        	            ,{707, 5815, 178, 178, 55, 500},{910, 6235, 171, 171, 55, 500},{1122, 5866, 195, 195, 55, 500},{1422, 5848, 187, 187, 55, 500},{1462, 6142, 192, 192, 55, 500},{2086, 5781, 171, 171, 55, 800}
        	            ,{2580, 6058, 183, 183, 55, 500},{2780, 5685, 190, 190, 55, 500},{3434, 6222, 197, 197, 55, 500},{3532, 5895, 172, 172, 55, 500},{3795, 5682, 185, 185, 55, 500},{3838, 5993, 187, 187, 55, 500}
        	            ,{4257, 5927, 177, 177, 55, 500},{4623, 6255, 197, 197, 55, 800},{4998, 6047, 193, 193, 55, 500},{5148, 5684, 177, 177, 55, 800},{5631, 5943, 197, 197, 55, 800},{5940, 6113, 199, 199, 55, 800}
        	            ,{6171, 5909, 193, 193, 55, 500},{6575, 6289, 179, 179, 55, 800},{6797, 5862, 198, 198, 55, 500},{7342, 6034, 194, 194, 55, 500},{7711, 5638, 182, 182, 55, 500},{7836, 6284, 177, 177, 55, 500}
        	            ,{8007, 5699, 170, 170, 55, 500},{8357, 5860, 181, 181, 55, 500},{8693, 6248, 194, 194, 55, 500},{8838, 5722, 196, 196, 55, 500},{9193, 6080, 192, 192, 55, 500},{9563, 5870, 182, 182, 55, 500}
        	            ,{9948, 6170, 178, 178, 55, 500},{10220, 5909, 172, 172, 55, 500},{10350, 6215, 186, 186, 55, 800},{10434, 5704, 200, 200, 55, 500},{10752, 5920, 183, 183, 55, 800},{11178, 5870, 199, 199, 55, 500}
        	            ,{482, 6929, 184, 184, 55, 800},{504, 6456, 184, 184, 55, 500},{670, 6753, 171, 171, 55, 800},{988, 6795, 175, 175, 55, 500},{1223, 6391, 188, 188, 55, 500},{1364, 6902, 197, 197, 55, 500}
        	            ,{1570, 6630, 173, 173, 55, 500},{2243, 6534, 177, 177, 55, 500},{2983, 6315, 173, 173, 55, 500},{3501, 6720, 171, 171, 55, 500},{3799, 6381, 194, 194, 55, 500},{4009, 6826, 176, 176, 55, 500}
        	            ,{4300, 6507, 176, 176, 55, 500},{4548, 6829, 186, 186, 55, 500},{4690, 6550, 191, 191, 55, 500},{4907, 6375, 179, 179, 55, 500},{4953, 6685, 170, 170, 55, 500},{5345, 6430, 173, 173, 55, 500}
        	            ,{5490, 6902, 172, 172, 55, 800},{5783, 6532, 172, 172, 55, 800},{6122, 6408, 185, 185, 55, 800},{6275, 6911, 195, 195, 55, 800},{6923, 6699, 196, 196, 55, 500},{7344, 6959, 189, 189, 55, 500}
        	            ,{7510, 6605, 183, 183, 55, 500},{8188, 6648, 171, 171, 55, 500},{8322, 6900, 179, 179, 55, 500},{8349, 6379, 181, 181, 55, 800},{8570, 6607, 199, 199, 55, 500},{9049, 6436, 198, 198, 55, 800}
        	            ,{9500, 6667, 191, 191, 55, 500},{9799, 6957, 186, 186, 55, 500},{10106, 6663, 170, 170, 55, 500},{10530, 6618, 194, 194, 55, 800},{10955, 6968, 185, 185, 55, 500},{11026, 6305, 193, 193, 55, 500}
        	            ,{626, 7333, 190, 190, 55, 500},{1139, 7379, 194, 194, 55, 800},{1442, 7684, 179, 179, 55, 500},{1653, 7175, 191, 191, 55, 800},{2249, 7443, 174, 174, 55, 500},{2587, 7562, 196, 196, 55, 500}
        	            ,{2653, 7257, 199, 199, 55, 500},{2876, 7664, 180, 180, 55, 800},{2934, 7247, 190, 190, 55, 500},{3253, 7464, 190, 190, 55, 800},{3755, 7279, 186, 186, 55, 500},{4201, 7214, 176, 176, 55, 500}
        	            ,{4477, 7609, 175, 175, 55, 500},{4810, 7189, 180, 180, 55, 800},{4916, 7605, 189, 189, 55, 800},{5208, 7452, 177, 177, 55, 500},{5338, 7683, 194, 194, 55, 500},{5662, 7420, 170, 170, 55, 500}
        	            ,{5856, 7091, 178, 178, 55, 800},{5978, 7599, 192, 192, 55, 500},{6392, 7413, 182, 182, 55, 800},{6841, 7391, 200, 200, 55, 500},{7021, 7063, 183, 183, 55, 500},{7607, 7541, 182, 182, 55, 800}
        	            ,{7762, 7080, 192, 192, 55, 500},{8019, 7438, 178, 178, 55, 500},{8149, 7173, 185, 185, 55, 500},{8392, 7582, 197, 197, 55, 500},{8458, 7258, 171, 171, 55, 500},{8757, 7001, 173, 173, 55, 800}
        	            ,{8858, 7497, 179, 179, 55, 500},{9115, 7139, 199, 199, 55, 800},{9328, 7677, 198, 198, 55, 500},{9508, 7198, 183, 183, 55, 500},{9905, 7637, 180, 180, 55, 500},{10232, 7160, 175, 175, 55, 500}
        	            ,{10677, 7365, 180, 180, 55, 500},{11194, 7367, 199, 199, 55, 500},{495, 7833, 190, 190, 55, 500},{858, 8305, 179, 179, 55, 500},{932, 7881, 176, 176, 55, 500},{1353, 8391, 182, 182, 55, 800}
        	            ,{2300, 8143, 176, 176, 55, 800},{2368, 7775, 188, 188, 55, 500},{2992, 8105, 192, 192, 55, 500},{3670, 8254, 195, 195, 55, 800},{3990, 7736, 170, 170, 55, 500},{4294, 8058, 193, 193, 55, 500}
        	            ,{4780, 8151, 180, 180, 55, 500},{4886, 7891, 174, 174, 55, 500},{5112, 8063, 175, 175, 55, 500},{5503, 8391, 187, 187, 55, 500},{5793, 8022, 192, 192, 55, 500},{6228, 8016, 175, 175, 55, 800}
        	            ,{6451, 8301, 189, 189, 55, 800},{6636, 7879, 176, 176, 55, 500},{6887, 8291, 180, 180, 55, 500},{7170, 7932, 185, 185, 55, 500},{7497, 8335, 180, 180, 55, 500},{7675, 7942, 176, 176, 55, 500}
        	            ,{8135, 7760, 177, 177, 55, 500},{8332, 8266, 180, 180, 55, 500},{8658, 7910, 192, 192, 55, 500},{9115, 8045, 190, 190, 55, 500},{9563, 8161, 176, 176, 55, 500},{9966, 8233, 179, 179, 55, 800}
        	            ,{10316, 8125, 187, 187, 55, 500},{10368, 7750, 182, 182, 55, 800},{10703, 8301, 176, 176, 55, 500},{10994, 7788, 179, 179, 55, 500},{11159, 8299, 179, 179, 55, 500},{538, 8589, 188, 188, 55, 500}
        	            ,{1221, 8882, 192, 192, 55, 500},{2071, 8661, 171, 171, 55, 500},{2229, 9034, 199, 199, 55, 500},{2522, 8588, 174, 174, 55, 500},{2800, 8922, 173, 173, 55, 500},{3398, 8668, 188, 188, 55, 800}
        	            ,{4018, 8870, 179, 179, 55, 500},{4302, 9049, 182, 182, 55, 500},{4340, 8505, 199, 199, 55, 500},{4599, 8947, 171, 171, 55, 500},{4706, 8627, 196, 196, 55, 500},{4965, 8847, 179, 179, 55, 500}
        	            ,{4970, 8514, 175, 175, 55, 500},{5362, 8970, 185, 185, 55, 500},{5825, 8771, 171, 171, 55, 800},{5981, 8415, 192, 192, 55, 500},{6180, 8708, 175, 175, 55, 500},{6679, 9023, 198, 198, 55, 500}
        	            ,{7183, 8681, 176, 176, 55, 800},{7814, 8813, 183, 183, 55, 500},{8201, 8965, 185, 185, 55, 500},{8433, 8601, 173, 173, 55, 800},{8682, 8998, 188, 188, 55, 500},{8848, 8569, 178, 178, 55, 500}
        	            ,{9341, 8579, 175, 175, 55, 500},{9538, 8882, 182, 182, 55, 500},{9690, 8567, 174, 174, 55, 500},{10288, 8632, 191, 191, 55, 800},{10579, 9087, 188, 188, 55, 500},{10772, 8768, 187, 187, 55, 500}
        	            ,{11205, 8913, 177, 177, 55, 500},{504, 9605, 181, 181, 55, 800},{760, 9197, 183, 183, 55, 500},{1241, 9358, 192, 192, 55, 500},{1578, 9674, 188, 188, 55, 500},{2269, 9618, 189, 189, 55, 500}
        	            ,{2622, 9743, 195, 195, 55, 500},{2700, 9443, 174, 174, 55, 500},{3076, 9541, 176, 176, 55, 500},{3285, 9230, 199, 199, 55, 500},{3354, 9765, 178, 178, 55, 500},{3650, 9442, 192, 192, 55, 800}
        	            ,{3981, 9275, 194, 194, 55, 500},{4391, 9487, 192, 192, 55, 500},{4901, 9660, 176, 176, 55, 500},{5440, 9402, 190, 190, 55, 500},{5767, 9663, 198, 198, 55, 500},{6013, 9271, 176, 176, 55, 800}
        	            ,{6278, 9594, 186, 186, 55, 500},{6459, 9384, 179, 179, 55, 500},{6749, 9572, 183, 183, 55, 800},{7121, 9223, 187, 187, 55, 500},{7595, 9728, 192, 192, 55, 800},{7611, 9257, 172, 172, 55, 500}
        	            ,{8088, 9757, 199, 199, 55, 500},{8341, 9247, 197, 197, 55, 500},{8483, 9742, 195, 195, 55, 500},{8887, 9605, 170, 170, 55, 800},{9094, 9144, 173, 173, 55, 500},{9428, 9707, 174, 174, 55, 500}
        	            ,{9611, 9367, 180, 180, 55, 500},{10033, 9235, 188, 188, 55, 800},{10816, 9387, 195, 195, 55, 500},{593, 10321, 175, 175, 55, 500},{881, 9851, 189, 189, 55, 500},{944, 10217, 178, 178, 55, 500}
        	            ,{1253, 10006, 182, 182, 55, 500},{1632, 10393, 170, 170, 55, 500},{2330, 10020, 175, 175, 55, 500},{2822, 10126, 186, 186, 55, 500},{2977, 9825, 171, 171, 55, 500},{3211, 10460, 191, 191, 55, 500}
        	            ,{3591, 10176, 179, 179, 55, 500},{3938, 10489, 197, 197, 55, 800},{3982, 9838, 192, 192, 55, 500},{4319, 10196, 180, 180, 55, 500},{4833, 10097, 185, 185, 55, 500},{4982, 10433, 182, 182, 55, 800}
        	            ,{5191, 10177, 193, 193, 55, 500},{5371, 9869, 184, 184, 55, 800},{5769, 10195, 183, 183, 55, 500},{6135, 9977, 188, 188, 55, 500},{6283, 10250, 193, 193, 55, 500},{6451, 9910, 189, 189, 55, 500}
        	            ,{6690, 10299, 194, 194, 55, 500},{7099, 9921, 188, 188, 55, 800},{7652, 10132, 186, 186, 55, 500},{8129, 10437, 175, 175, 55, 500},{8561, 10295, 194, 194, 55, 500},{8829, 9965, 179, 179, 55, 500}
        	            ,{9312, 10253, 185, 185, 55, 500},{9808, 10388, 195, 195, 55, 500},{10027, 9818, 196, 196, 55, 500},{10085, 10295, 197, 197, 55, 500},{10530, 10235, 188, 188, 55, 500},{10538, 9829, 193, 193, 55, 500}
        	            ,{10868, 10375, 183, 183, 55, 500},{11064, 9938, 192, 192, 55, 500},{495, 10670, 177, 177, 55, 800},{865, 10787, 198, 198, 55, 500},{1654, 10963, 176, 176, 55, 500},{2124, 10819, 172, 172, 55, 500}
        	            ,{2164, 11149, 183, 183, 55, 500},{2429, 10936, 188, 188, 55, 500},{2562, 10550, 185, 185, 55, 500},{2799, 10894, 193, 193, 55, 500},{3025, 10690, 181, 181, 55, 500},{3177, 10951, 179, 179, 55, 800}
        	            ,{3673, 10988, 199, 199, 55, 500},{4295, 10966, 197, 197, 55, 500},{4647, 10835, 172, 172, 55, 800},{4990, 11008, 190, 190, 55, 500},{5492, 10585, 194, 194, 55, 500},{5516, 10944, 196, 196, 55, 800}
        	            ,{6005, 10949, 197, 197, 55, 500},{6051, 10506, 186, 186, 55, 500},{6589, 10985, 173, 173, 55, 500},{7046, 10685, 196, 196, 55, 800},{7107, 11138, 190, 190, 55, 500},{7400, 10522, 194, 194, 55, 500}
        	            ,{7794, 10827, 194, 194, 55, 500},{8419, 10864, 172, 172, 55, 500},{8795, 10914, 200, 200, 55, 800},{8926, 10549, 186, 186, 55, 500},{9235, 10956, 195, 195, 55, 500},{9540, 10644, 194, 194, 55, 500}
        	            ,{9629, 10964, 191, 191, 55, 500},{9842, 10763, 182, 182, 55, 500},{10178, 10642, 184, 184, 55, 800},{10844, 10978, 185, 185, 55, 500},{11188, 10626, 195, 195, 55, 500},{501, 11488, 187, 187, 55, 500}
        	            ,{885, 11763, 170, 170, 55, 500},{1112, 11388, 185, 185, 55, 500},{1320, 11742, 187, 187, 55, 500},{1708, 11344, 182, 182, 55, 800},{1861, 11704, 197, 197, 55, 500},{2241, 11463, 195, 195, 55, 800}
        	            ,{2317, 11848, 172, 172, 55, 500},{2694, 11344, 170, 170, 55, 500},{2767, 11709, 199, 199, 55, 500},{3142, 11404, 194, 194, 55, 500},{3580, 11655, 187, 187, 55, 500},{4084, 11613, 174, 174, 55, 800}
        	            ,{4141, 11330, 178, 178, 55, 500},{4407, 11272, 175, 175, 55, 500},{4412, 11568, 171, 171, 55, 500},{4834, 11653, 188, 188, 55, 500},{5353, 11370, 173, 173, 55, 500},{5864, 11506, 199, 199, 55, 500}
        	            ,{6275, 11846, 197, 197, 55, 500},{6551, 11444, 195, 195, 55, 500},{6635, 11734, 195, 195, 55, 500},{6853, 11540, 173, 173, 55, 800},{7264, 11795, 178, 178, 55, 800},{7521, 11353, 188, 188, 55, 800}
        	            ,{7681, 11873, 199, 199, 55, 800},{7849, 11509, 188, 188, 55, 500},{8113, 11248, 195, 195, 55, 500},{8703, 11278, 195, 195, 55, 500},{8755, 11889, 176, 176, 55, 500},{9129, 11822, 197, 197, 55, 500}
        	            ,{9381, 11440, 200, 200, 55, 800},{9752, 11709, 180, 180, 55, 500},{9892, 11263, 180, 180, 55, 500},{10283, 11456, 191, 191, 55, 500},{10713, 11736, 173, 173, 55, 500},{10714, 11353, 173, 173, 55, 500}
        	            ,{11154, 11881, 176, 176, 55, 500},{11192, 11441, 171, 171, 55, 500},{701, 12167, 184, 184, 55, 500},{1020, 12474, 176, 176, 55, 500},{1099, 12190, 180, 180, 55, 500},{1330, 12425, 198, 198, 55, 800}
        	            ,{1876, 12148, 178, 178, 55, 500},{2256, 12416, 177, 177, 55, 500},{2534, 12097, 189, 189, 55, 500},{2920, 12367, 181, 181, 55, 500},{3125, 11912, 185, 185, 55, 500},{3505, 12045, 177, 177, 55, 500}
        	            ,{3870, 12366, 189, 189, 55, 500},{4211, 12017, 193, 193, 55, 500},{4401, 12481, 186, 186, 55, 800},{4656, 12158, 199, 199, 55, 500},{5036, 12543, 179, 179, 55, 800},{5184, 12117, 195, 195, 55, 800}
        	            ,{5551, 11976, 191, 191, 55, 500},{5727, 12326, 174, 174, 55, 500},{6221, 12143, 171, 171, 55, 500},{6447, 12564, 185, 185, 55, 500},{6581, 12210, 189, 189, 55, 500},{7016, 12289, 181, 181, 55, 500}
        	            ,{7713, 12357, 180, 180, 55, 500},{8178, 11909, 176, 176, 55, 500},{8193, 12492, 175, 175, 55, 500},{8546, 12199, 173, 173, 55, 500},{8627, 12550, 177, 177, 55, 500},{8841, 12347, 176, 176, 55, 500}
        	            ,{9282, 12191, 194, 194, 55, 800},{9933, 12090, 189, 189, 55, 500},{10501, 12292, 177, 177, 55, 500},{10972, 12365, 192, 192, 55, 500},{11200, 12206, 192, 192, 55, 500},{577, 12913, 193, 193, 55, 500}
        	            ,{768, 12609, 185, 185, 55, 800},{1250, 12819, 191, 191, 55, 500},{1623, 13167, 189, 189, 55, 500},{1948, 12766, 190, 190, 55, 800},{2056, 13200, 197, 197, 55, 500},{2585, 12812, 194, 194, 55, 800}
        	            ,{3147, 12982, 176, 176, 55, 500},{3351, 12604, 174, 174, 55, 500},{3475, 13222, 171, 171, 55, 500},{3605, 12872, 171, 171, 55, 800},{4154, 13097, 194, 194, 55, 500},{4616, 12807, 186, 186, 55, 800}
        	            ,{4676, 13278, 174, 174, 55, 500},{5095, 13074, 170, 170, 55, 500},{5461, 12656, 175, 175, 55, 500},{5485, 13206, 189, 189, 55, 500},{5737, 12947, 172, 172, 55, 500},{5766, 13225, 182, 182, 55, 500}
        	            ,{6017, 12744, 181, 181, 55, 500},{6251, 13163, 171, 171, 55, 800},{6839, 12723, 200, 200, 55, 500},{7276, 13083, 177, 177, 55, 500},{7712, 12737, 180, 180, 55, 500},{7748, 13212, 188, 188, 55, 500}
        	            ,{8175, 13036, 199, 199, 55, 500},{8620, 12889, 195, 195, 55, 800},{8859, 13247, 180, 180, 55, 500},{9027, 12644, 183, 183, 55, 800},{9217, 13050, 183, 183, 55, 500},{9636, 13104, 186, 186, 55, 800}
        	            ,{9796, 12786, 188, 188, 55, 500},{10051, 12615, 198, 198, 55, 500},{10051, 13069, 187, 187, 55, 500},{10609, 12960, 183, 183, 55, 500},{10848, 12615, 189, 189, 55, 500},{11136, 12716, 195, 195, 55, 500}
        	            ,{11201, 13200, 182, 182, 55, 500},{501, 13447, 193, 193, 55, 800},{787, 13902, 188, 188, 55, 800},{1055, 13324, 178, 178, 55, 500},{1116, 13698, 191, 191, 55, 500},{1240, 13984, 187, 187, 55, 500}
        	            ,{1506, 13632, 191, 191, 55, 500},{1572, 13970, 195, 195, 55, 800},{1965, 13754, 171, 171, 55, 500},{2362, 13791, 191, 191, 55, 500},{2435, 13358, 194, 194, 55, 500},{2983, 13777, 187, 187, 55, 500}
        	            ,{3047, 13308, 189, 189, 55, 500},{3513, 13658, 183, 183, 55, 500},{3960, 13791, 192, 192, 55, 500},{4267, 13561, 191, 191, 55, 500},{4633, 13816, 190, 190, 55, 800},{5141, 13695, 185, 185, 55, 800}
        	            ,{5570, 13584, 187, 187, 55, 500},{6019, 13638, 173, 173, 55, 500},{6398, 13936, 185, 185, 55, 500},{6694, 13329, 170, 170, 55, 500},{6795, 13750, 171, 171, 55, 500},{7320, 13773, 185, 185, 55, 500}
        	            ,{7619, 13502, 195, 195, 55, 800},{7688, 13843, 175, 175, 55, 500},{7958, 13977, 170, 170, 55, 500},{8039, 13353, 185, 185, 55, 800},{8109, 13631, 179, 179, 55, 500},{8496, 13548, 180, 180, 55, 500}
        	            ,{8921, 13870, 175, 175, 55, 500},{9306, 13560, 178, 178, 55, 500},{9925, 13524, 172, 172, 55, 500},{10279, 13738, 174, 174, 55, 500},{10454, 13391, 198, 198, 55, 500},{10587, 13826, 194, 194, 55, 500}
        	            ,{10904, 13431, 177, 177, 55, 500},{11106, 13781, 175, 175, 55, 500},{639, 14297, 171, 171, 55, 500},{958, 14626, 172, 172, 55, 800},{1000, 14200, 176, 176, 55, 500},{1297, 14280, 198, 198, 55, 800}
        	            ,{1882, 14476, 176, 176, 55, 800},{2303, 14159, 200, 200, 55, 800},{2512, 14666, 180, 180, 55, 500},{2928, 14363, 199, 199, 55, 800},{3350, 14096, 191, 191, 55, 500},{3434, 14601, 198, 198, 55, 800}
        	            ,{3922, 14266, 197, 197, 55, 500},{4551, 14605, 179, 179, 55, 800},{4583, 14286, 191, 191, 55, 800},{4864, 14492, 196, 196, 55, 500},{5113, 14194, 193, 193, 55, 500},{5438, 14503, 197, 197, 55, 500}
        	            ,{5837, 14518, 178, 178, 55, 500},{5855, 14101, 188, 188, 55, 500},{6346, 14400, 196, 196, 55, 500},{6727, 14123, 195, 195, 55, 500},{6858, 14547, 170, 170, 55, 500},{7111, 14095, 174, 174, 55, 500}
        	            ,{7482, 14114, 170, 170, 55, 500},{7625, 14574, 189, 189, 55, 500},{8401, 14209, 180, 180, 55, 500},{8816, 14359, 191, 191, 55, 800},{9188, 14365, 200, 200, 55, 500},{9552, 14293, 190, 190, 55, 500}
        	            ,{10045, 14372, 179, 179, 55, 800},{10055, 14003, 171, 171, 55, 500},{10324, 14014, 182, 182, 55, 500},{10575, 14317, 193, 193, 55, 500},{11067, 14366, 197, 197, 55, 500},{552, 14750, 198, 198, 55, 500}
        	            ,{1236, 14914, 174, 174, 55, 500},{1601, 14799, 195, 195, 55, 500},{2152, 14894, 176, 176, 55, 500},{3071, 14914, 179, 179, 55, 500},{3970, 14911, 191, 191, 55, 500},{4471, 14845, 196, 196, 55, 500}
        	            ,{4798, 14801, 176, 176, 55, 500},{5171, 14864, 200, 200, 55, 800},{5977, 14858, 178, 178, 55, 500},{6558, 14896, 195, 195, 55, 500},{7170, 14785, 178, 178, 55, 500},{7913, 14906, 174, 174, 55, 500}
        	            ,{8295, 14888, 197, 197, 55, 500},{8582, 14709, 177, 177, 55, 500},{8915, 14869, 173, 173, 55, 500},{9306, 14801, 173, 173, 55, 500},{10078, 14823, 182, 182, 55, 500},{10878, 14906, 172, 172, 55, 500}
        	            ,{11164, 14707, 179, 179, 55, 500}
        	        };

        breakables = new breakable[data.length];
        breakableTile = new int[data.length];
        for (int i = 0; i < data.length; i++) breakableTile[i] = data[i][4];

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
        // viewMin/viewMax melebar pas zoom out, jadi tile di pinggir tetap kegambar
        int startCol = Math.max(0, (leftWorld + gp.viewMinX()) / gp.tileSize - 1);
        int endCol   = Math.min(gp.maxWorldCol - 1, (leftWorld + gp.viewMaxX()) / gp.tileSize + 1);
        int startRow = Math.max(0, (topWorld + gp.viewMinY()) / gp.tileSize - 1);
        int endRow   = Math.min(gp.maxWorldRow - 1, (topWorld + gp.viewMaxY()) / gp.tileSize + 1);

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

                if (screenX + gp.tileSize > gp.viewMinX() && screenX < gp.viewMaxX() &&
                    screenY + gp.tileSize > gp.viewMinY() && screenY < gp.viewMaxY()) {

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
			            gp.viewMinX(), gp.viewMinY(),
			            gp.viewMaxX(), gp.viewMaxY());
			
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
			if (monsters[i] != null && (!monsters[i].isDead
			        || (monsters[i] instanceof Entitas.monster && ((Entitas.monster) monsters[i]).isDeathAnimPlaying()))) {
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
			
			
			// Peti penyimpanan
			if (gp.chest != null) {
			    drawList.add(new int[]{gp.chest.getSortY(), 6, 0});
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
			    if (sx + b.width >= gp.viewMinX() && sx <= gp.viewMaxX() &&
			        sy + b.height >= gp.viewMinY() && sy <= gp.viewMaxY()) {

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
			case 6: { // peti penyimpanan
			    gp.chest.draw(g2, fCamX, fCamY);
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

	/** Data objek dunia {x, y, w, h, tileIndex, isFlat, shadow} — dibaca MiniMap (read-only). */
	public int[][] getWorldObjects() {
	    return worldObjects;
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

	    if (screenX + w > gp.viewMinX() &&
	        screenX < gp.viewMaxX() &&
	        screenY + h > gp.viewMinY() &&
	        screenY < gp.viewMaxY()) {

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
	        if (screenX + r.width < gp.viewMinX() || screenX > gp.viewMaxX() ||
	            screenY + r.height < gp.viewMinY() || screenY > gp.viewMaxY()) {
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