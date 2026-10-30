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
import java.util.ArrayList;
import java.util.List;

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
public WaterAnimator waterAnim;          // gelombang air + buih pantai (lihat WaterAnimator.java)
public FishJumpManager fishManager;      // ikan kuning yang sesekali meloncat (lihat FishJumpManager.java)

/**
 * Dek jembatan yang bisa diinjak (rectangle dunia, px). Diisi di loadObjectCollision(), ngikut posisi objek jembatan.
 * Tile di bawah dek tetap tile air, jadi kode yang nanya "ini air?" (mis. mancing) harus ngecek ini juga.
 */
public final List<Rectangle> bridgeDecks = new ArrayList<>();

/** True kalau titik dunia (px) ini ada di atas dek jembatan. */
public boolean isOnBridgeDeck(int wx, int wy) {
    for (Rectangle r : bridgeDecks) {
        if (r.contains(wx, wy)) return true;
    }
    return false;
}

// ==== DUNIA YANG DIPERLUAS + BIOME ES ====
// Peta lama (map01.txt, 243 x 321 tile) tetap utuh. Di luarnya dunia disambung sampai WorldGen.WORLD_COLS x WORLD_ROWS tile
// oleh generator (WorldGen), dan chunk-nya dimuat / dibuang mengikuti player oleh ChunkManager.
public WorldGen gen;                     // generator dunia: area baru, kekuatan salju (ice), danau beku, sebaran objek
public ChunkManager chunks;              // peta dinamis (lihat ChunkManager.java)
private IceSprites iceSprites;           // sprite cemara / pohon mati / batu es / kristal
private int poolBase;                    // slot pertama di breakables[] yang dipinjamkan ke ChunkManager
private static final int BREAKABLE_POOL = 1500;
// gambar pohon bulat + versi bekunya (dipakai pohon hutan lama di pinggiran salju dan pohon hutan area baru)
private BufferedImage[] oakIdle, oakHit, oakDown, frostIdle, frostHit, frostDown;

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
    prepareWorld();            // peta lama -> dunia yang diperluas (lihat WorldGen)
    loadObjectCollision();
    releaseGeneratedArea();    // sel area baru dikosongkan lagi; ChunkManager mengisinya waktu chunk dimuat

    iceSprites = new IceSprites(getClass());
    loadBreakableObjects();
    
    animObjManager = new AnimatedObjectManager(getClass());

    // Animasi air: semua tile air + tepi pantainya digambar oleh WaterAnimator (gambar tile aslinya tetap dipakai
    // sebagai bentuk pantai, cuma airnya yang bergerak). Harus SETELAH getTileImage().
    BufferedImage[] tileImgs = new BufferedImage[tile.length];
    for (int i = 0; i < tile.length; i++) if (tile[i] != null) tileImgs[i] = tile[i].image;
    waterAnim = new WaterAnimator(tileImgs, gp.tileSize);

    // Ikan kuning loncat. Tiap baris = {x, y, jenis}: titik tengah busur loncatan di permukaan air (piksel dunia),
    // jenis 0 = loncatan pendek, 1 = loncatan jauh. Titik-titiknya dipilih di tengah danau/laut yang cukup luas.
    // Mau nambah ikan? Tambah baris baru, pastikan sekitar titiknya air semua (270px lebar, 130px atas, 40px bawah).
    fishManager = new FishJumpManager(getClass(), new int[][] {
        {2208, 1440, 0},
        {1440, 1488, 1},
        {7344, 2016, 0},
        {6960, 2064, 1},
        {2496, 144, 0},
        {6144, 144, 1},
        {10464, 144, 0},
        {9888, 7296, 1},
        {9504, 7296, 0},
        {2592, 8688, 1},
        {7008, 12816, 0},
        {7392, 12816, 1}
    });

    chunks = new ChunkManager(gp, this, gen, iceSprites, poolBase, BREAKABLE_POOL,
            oakIdle, oakHit, oakDown, frostIdle, frostHit, frostDown);
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
			tile[73] = new Tile();tile[74] = new Tile();tile[75] = new Tile();
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
			// 73-75 = jembatan HORIZONTAL (kiri, tengah, kanan). Gambarnya sama dengan jembatan vertikal 25/26/27
			// (atas, tengah, bawah) yang diputar 90 derajat searah jarum jam, jadi tidak perlu aset baru.
			tile[73].image = rotateCW(tile[27].image);
			tile[74].image = rotateCW(tile[26].image);
			tile[75].image = rotateCW(tile[25].image);
			
			// dst. tiap rumah baru = tile baru
			
			tile[14].collision = true;
			tile[10].collision = true;	

			addIceTiles();
		}
		catch(IOException e) {
			e.printStackTrace();
		}}
	/**
	 * Tile baru untuk dunia yang diperluas:
	 *  76-78  = rumput (gambar sama dengan tile 4 / 6 / 10, tapi TIDAK collision) untuk area baru dan bekas tanah salju lama
	 *  100    = salju, 131 = es (polos; cuma dipakai minimap)
	 *  140-151 = tile tepi pantai versi salju (urutan sama dengan WorldGen.SHORE_BASE)
	 */
	private void addIceTiles() {
		int[] grassSrc = {4, 6, 10};
		for (int i = 0; i < 3; i++) {
			tile[WorldGen.GRASS[i]] = new Tile();
			tile[WorldGen.GRASS[i]].image = tile[grassSrc[i]].image;
		}
		tile[WorldGen.TILE_SNOW] = new Tile();
		tile[WorldGen.TILE_SNOW].image = IceSprites.flat(0xE8F1FA);
		tile[WorldGen.TILE_ICE] = new Tile();
		tile[WorldGen.TILE_ICE].image = IceSprites.flat(0x9ACFEF);
		for (int i = 0; i < WorldGen.SHORE_BASE.length; i++) {
			Tile base = tile[WorldGen.SHORE_BASE[i]];
			Tile t = new Tile();
			t.image = IceSprites.snowShore(base.image);
			t.collision = base.collision;
			tile[WorldGen.SNOW_SHORE_FIRST + i] = t;
		}
	}

	/**
	 * Sambungkan peta lama ke dunia yang diperluas:
	 *  - tanah salju lama (tile 67 / 68, putih polos) diganti rumput; salju & esnya sekarang digambar generator (IceGround)
	 *  - ujung timur laut utara dibereskan supaya nyambung ke laut area baru
	 *  - tile area baru diisi SEMENTARA supaya collision air laut pembatas ikut terhitung
	 */
	private void prepareWorld() {
		int coreC = loadedCols, coreR = loadedRows;
		for (int c = 0; c < coreC; c++) {
			for (int r = 0; r < coreR; r++) {
				int t = mapTileNum[c][r];
				if (t == 67 || t == 68) mapTileNum[c][r] = WorldGen.GRASS[(WorldGen.h2(c, r, 7) >>> 4) % 3];
			}
		}
		if (coreC == 243) { // kolom terakhir peta lama: tile 14 (air, darat di timur) -> air penuh, sambung ke laut baru
			for (int r = 0; r < 6; r++) mapTileNum[coreC - 1][r] = 2;
			mapTileNum[coreC - 1][6] = 17;
		}
		gen = new WorldGen(mapTileNum, coreC, coreR);
		for (int c = 0; c < WorldGen.WORLD_COLS; c++) {
			for (int r = 0; r < WorldGen.WORLD_ROWS; r++) {
				if (!gen.inCore(c, r)) mapTileNum[c][r] = gen.groundTile(c, r);
			}
		}
		loadedCols = WorldGen.WORLD_COLS;
		loadedRows = WorldGen.WORLD_ROWS;
	}

	/** Kosongkan lagi sel di luar peta lama (jadi "air" yang belum dimuat); ChunkManager mengisinya waktu chunk aktif. */
	private void releaseGeneratedArea() {
		for (int c = 0; c < gp.maxWorldCol; c++) {
			for (int r = 0; r < gp.maxWorldRow; r++) {
				if (!gen.inCore(c, r)) mapTileNum[c][r] = 2;
			}
		}
	}

	/** Peta tile untuk minimap: tidak tergantung chunk sudah dimuat atau belum. */
	public int minimapTile(int col, int row) {
		return gen.surfaceTile(col, row);
	}

	/** Putar gambar 90 derajat searah jarum jam (lebar dan tinggi tertukar). */
	static BufferedImage rotateCW(BufferedImage srcImg) {
	    int w = srcImg.getWidth(), h = srcImg.getHeight();
	    BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
	    for (int y = 0; y < h; y++) {
	        for (int x = 0; x < w; x++) {
	            dst.setRGB(h - 1 - y, x, srcImg.getRGB(x, y));
	        }
	    }
	    return dst;
	}

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

	/** true kalau kotak objek ini ada di layar atau dekat layar (margin 150px, supaya sort tidak kebanyakan). */
	private boolean nearView(int worldX, int worldY, int w, int h, int camX, int camY) {
	    int sx = worldX - camX + gp.player.screenX;
	    int sy = worldY - camY + gp.player.screenY;
	    return sx + w > gp.viewMinX() - 150 && sx < gp.viewMaxX() + 150
	        && sy + h > gp.viewMinY() - 150 && sy < gp.viewMaxY() + 150;
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
        	            {1375, 872, 120, 120, 71, 800, 0},{9704, 2821, 120, 120, 71, 800, 0},{1538, 1976, 120, 120, 71, 800, 0},{1476, 4593, 120, 120, 71, 800, 0},
        	            {8756, 5242, 120, 120, 71, 800, 0},{1918, 2798, 120, 120, 71, 800, 0},{11087, 4183, 120, 120, 71, 800, 0},{467, 2433, 120, 120, 71, 800, 0},
        	            {1729, 2392, 120, 120, 71, 800, 0},{11081, 4761, 120, 120, 71, 800, 0}// === BATU BARU BATCH 2 (30 buah, tersebar, jaga jarak dari objek lain + batch 1) ===
        	            ,{9518, 3796, 120, 120, 71, 800, 0},{11278, 4997, 120, 120, 71, 800, 0},{7281, 5090, 120, 120, 71, 800, 0},{5095, 2844, 120, 120, 71, 800, 0},
        	            {178, 2254, 120, 120, 71, 800, 0},{3765, 4091, 120, 120, 71, 800, 0},{144, 3733, 120, 120, 71, 800, 0},{11068, 4534, 120, 120, 71, 800, 0},
        	            {1655, 2927, 120, 120, 71, 800, 0},{7734, 5247, 120, 120, 71, 800, 0},{3337, 2810, 120, 120, 71, 800, 0},{6224, 2884, 120, 120, 71, 800, 0},
        	            {681, 11280, 120, 120, 71, 800, 0},{6913, 5140, 120, 120, 71, 800, 0},{1219, 525, 120, 120, 71, 800, 0},{4794, 2912, 120, 120, 71, 800, 0},
        	            {2449, 2891, 120, 120, 71, 800, 0},{1060, 4516, 120, 120, 71, 800, 0},{468, 3164, 120, 120, 71, 800, 0},{1476, 10318, 120, 120, 71, 800, 0},
        	            {4388, 4132, 120, 120, 71, 800, 0},{2184, 2934, 120, 120, 71, 800, 0},{1201, 1739, 120, 120, 71, 800, 0},{5617, 3071, 120, 120, 71, 800, 0},
        	            {712, 10678, 120, 120, 71, 800, 0},{4917, 3530, 120, 120, 71, 800, 0},{570, 1529, 120, 120, 71, 800, 0},{1757, 4305, 120, 120, 71, 800, 0},
        	            {5630, 5241, 120, 120, 71, 800, 0},{1585, 11864, 120, 120, 71, 800, 0}

        	            // ===== POHON (1129 buah) — hutan: ada rimba lebat (grove) + padang rumput terbuka =====
        	            // Dibuat dari peta baru: pohon cuma di darat, tidak di air / jalan tanah / jembatan / area rumah,
        	            // makin lebat di tengah rimba dan jarang di padang. Pinggir jalan dibuat agak rapat biar kayak jalan di hutan.
        	            // Format: {x, y, lebar, tinggi, 55 (pohon), hp}
        	            ,{3914, 423, 186, 186, 55, 800},{4130, 397, 173, 173, 55, 500},{4597, 431, 171, 171, 55, 500},{5266, 450, 184, 184, 55, 800},{5427, 444, 181, 181, 55, 800},{8952, 399, 193, 193, 55, 500}
        	            ,{9954, 356, 183, 183, 55, 500},{2848, 584, 192, 192, 55, 500},{3327, 643, 194, 194, 55, 800},{4441, 705, 173, 173, 55, 500},{5135, 924, 197, 197, 55, 500},{5330, 571, 197, 197, 55, 500}
        	            ,{5362, 786, 188, 188, 55, 500},{5509, 675, 195, 195, 55, 500},{5517, 892, 186, 186, 55, 800},{5545, 517, 193, 193, 55, 500},{5661, 828, 187, 187, 55, 800},{5681, 581, 188, 188, 55, 500}
        	            ,{5801, 827, 190, 190, 55, 500},{5939, 731, 194, 194, 55, 500},{6031, 950, 200, 200, 55, 800},{6279, 553, 177, 177, 55, 500},{7003, 505, 172, 172, 55, 500},{7478, 584, 184, 184, 55, 500}
        	            ,{7680, 934, 191, 191, 55, 500},{7959, 488, 175, 175, 55, 500},{8582, 736, 186, 186, 55, 500},{9408, 759, 178, 178, 55, 500},{9940, 779, 188, 188, 55, 500},{10492, 873, 200, 200, 55, 800}
        	            ,{10649, 538, 175, 175, 55, 500},{10675, 743, 174, 174, 55, 500},{10702, 886, 182, 182, 55, 500},{10822, 690, 186, 186, 55, 500},{10860, 816, 196, 196, 55, 500},{10952, 958, 179, 179, 55, 800}
        	            ,{10989, 504, 183, 183, 55, 500},{11085, 632, 185, 185, 55, 500},{11106, 789, 176, 176, 55, 800},{11123, 932, 170, 170, 55, 800},{11279, 683, 186, 186, 55, 500},{11284, 873, 180, 180, 55, 800}
        	            ,{2879, 1124, 193, 193, 55, 500},{3517, 1323, 189, 189, 55, 500},{3790, 963, 191, 191, 55, 500},{3994, 1404, 185, 185, 55, 500},{4415, 1245, 175, 175, 55, 500},{4887, 1038, 178, 178, 55, 500}
        	            ,{5030, 1264, 177, 177, 55, 500},{5473, 1173, 197, 197, 55, 500},{5708, 962, 199, 199, 55, 500},{5874, 1230, 176, 176, 55, 500},{6158, 1098, 188, 188, 55, 800},{6252, 1329, 193, 193, 55, 500}
        	            ,{6391, 1049, 171, 171, 55, 500},{6393, 1240, 194, 194, 55, 500},{6464, 1432, 170, 170, 55, 500},{6570, 1264, 182, 182, 55, 500},{6574, 1022, 176, 176, 55, 500},{6702, 1395, 199, 199, 55, 500}
        	            ,{6743, 1254, 185, 185, 55, 500},{6912, 1370, 178, 178, 55, 500},{7240, 1077, 171, 171, 55, 500},{7665, 1346, 176, 176, 55, 500},{8143, 1000, 191, 191, 55, 500},{8878, 1142, 187, 187, 55, 500}
        	            ,{9650, 1115, 184, 184, 55, 800},{10473, 1406, 195, 195, 55, 500},{10598, 1015, 180, 180, 55, 500},{10599, 1276, 170, 170, 55, 500},{10669, 1435, 178, 178, 55, 800},{10744, 1174, 191, 191, 55, 500}
        	            ,{10779, 1011, 197, 197, 55, 500},{10846, 1323, 181, 181, 55, 500},{10902, 1136, 183, 183, 55, 500},{10991, 1414, 194, 194, 55, 800},{11044, 1173, 187, 187, 55, 500},{11126, 1318, 180, 180, 55, 500}
        	            ,{11168, 1057, 185, 185, 55, 500},{11272, 1237, 176, 176, 55, 800},{1602, 1805, 178, 178, 55, 500},{2595, 1796, 195, 195, 55, 800},{2997, 1682, 192, 192, 55, 500},{3480, 1836, 194, 194, 55, 800}
        	            ,{4075, 1893, 180, 180, 55, 500},{5093, 1808, 181, 181, 55, 500},{5570, 1707, 199, 199, 55, 500},{5725, 1654, 186, 186, 55, 800},{5781, 1874, 191, 191, 55, 500},{5887, 1727, 178, 178, 55, 500}
        	            ,{6046, 1811, 198, 198, 55, 500},{6329, 1499, 194, 194, 55, 500},{6582, 1521, 182, 182, 55, 500},{6724, 1542, 185, 185, 55, 500},{6887, 1508, 195, 195, 55, 500},{7790, 1622, 172, 172, 55, 500}
        	            ,{7920, 1694, 178, 178, 55, 500},{8017, 1805, 186, 186, 55, 500},{8265, 1560, 185, 185, 55, 800},{8932, 1551, 200, 200, 55, 500},{9514, 1886, 199, 199, 55, 500},{9552, 1516, 170, 170, 55, 500}
        	            ,{10126, 1510, 200, 200, 55, 500},{10486, 1588, 184, 184, 55, 500},{10727, 1759, 183, 183, 55, 500},{10832, 1531, 194, 194, 55, 500},{10872, 1682, 199, 199, 55, 800},{11021, 1676, 186, 186, 55, 500}
        	            ,{11233, 1516, 174, 174, 55, 800},{1010, 2283, 188, 188, 55, 500},{1599, 2272, 171, 171, 55, 800},{1981, 2021, 176, 176, 55, 500},{3115, 2100, 174, 174, 55, 500},{3771, 2290, 178, 178, 55, 800}
        	            ,{4578, 2035, 183, 183, 55, 500},{5043, 2362, 173, 173, 55, 500},{5485, 2088, 197, 197, 55, 500},{5580, 2360, 198, 198, 55, 500},{5626, 1939, 171, 171, 55, 500},{5701, 2148, 180, 180, 55, 500}
        	            ,{5847, 2202, 181, 181, 55, 500},{5946, 2350, 188, 188, 55, 500},{5956, 2006, 179, 179, 55, 500},{6094, 2356, 171, 171, 55, 500},{6202, 1940, 179, 179, 55, 500},{6267, 2330, 179, 179, 55, 500}
        	            ,{6414, 2224, 197, 197, 55, 800},{6564, 2211, 185, 185, 55, 500},{6572, 2005, 179, 179, 55, 800},{6922, 2393, 196, 196, 55, 500},{8445, 2007, 199, 199, 55, 500},{9042, 1951, 171, 171, 55, 500}
        	            ,{9658, 2003, 191, 191, 55, 500},{9695, 2268, 195, 195, 55, 800},{9818, 2130, 171, 171, 55, 500},{9927, 2337, 190, 190, 55, 500},{10163, 2360, 180, 180, 55, 800},{10172, 1928, 178, 178, 55, 500}
        	            ,{10543, 1989, 198, 198, 55, 500},{10609, 2392, 175, 175, 55, 500},{11006, 1950, 175, 175, 55, 500},{11272, 1953, 194, 194, 55, 800},{1601, 2606, 196, 196, 55, 500},{2330, 2422, 200, 200, 55, 500}
        	            ,{2409, 2680, 177, 177, 55, 800},{2571, 2519, 171, 171, 55, 500},{2582, 2872, 200, 200, 55, 500},{2623, 2704, 180, 180, 55, 500},{2758, 2458, 178, 178, 55, 500},{2835, 2860, 191, 191, 55, 800}
        	            ,{3371, 2657, 178, 178, 55, 500},{4122, 2649, 196, 196, 55, 800},{4485, 2491, 170, 170, 55, 500},{4979, 2732, 184, 184, 55, 500},{5561, 2678, 170, 170, 55, 500},{5713, 2605, 171, 171, 55, 500}
        	            ,{5758, 2727, 194, 194, 55, 500},{5884, 2584, 186, 186, 55, 800},{5943, 2831, 174, 174, 55, 500},{6045, 2524, 174, 174, 55, 800},{6160, 2654, 181, 181, 55, 500},{6203, 2514, 187, 187, 55, 500}
        	            ,{6382, 2567, 172, 172, 55, 800},{6423, 2417, 193, 193, 55, 500},{6965, 2767, 193, 193, 55, 500},{6976, 2523, 192, 192, 55, 500},{7052, 2658, 171, 171, 55, 500},{7165, 2435, 171, 171, 55, 500}
        	            ,{7379, 2686, 183, 183, 55, 500},{7410, 2878, 190, 190, 55, 500},{7487, 2435, 195, 195, 55, 500},{7537, 2592, 186, 186, 55, 800},{7608, 2756, 190, 190, 55, 500},{7662, 2456, 198, 198, 55, 500}
        	            ,{7702, 2611, 192, 192, 55, 800},{7809, 2876, 178, 178, 55, 500},{8307, 2403, 186, 186, 55, 500},{8816, 2652, 180, 180, 55, 500},{9488, 2403, 200, 200, 55, 500},{9823, 2839, 185, 185, 55, 500}
        	            ,{9865, 2474, 191, 191, 55, 800},{9873, 2640, 181, 181, 55, 500},{10083, 2742, 192, 192, 55, 500},{10094, 2584, 178, 178, 55, 500},{10237, 2616, 189, 189, 55, 500},{10314, 2799, 194, 194, 55, 500}
        	            ,{10959, 2553, 182, 182, 55, 500},{1081, 2923, 192, 192, 55, 500},{1115, 3329, 195, 195, 55, 500},{1907, 3185, 182, 182, 55, 500},{2469, 2970, 171, 171, 55, 800},{2744, 2988, 184, 184, 55, 500}
        	            ,{2785, 3193, 194, 194, 55, 500},{3657, 3220, 197, 197, 55, 800},{4128, 3027, 196, 196, 55, 500},{4580, 3040, 196, 196, 55, 500},{4898, 3329, 196, 196, 55, 500},{5501, 3221, 192, 192, 55, 500}
        	            ,{5616, 2901, 194, 194, 55, 500},{5659, 3278, 179, 179, 55, 500},{5675, 3121, 173, 173, 55, 800},{5771, 2919, 182, 182, 55, 800},{5802, 3247, 190, 190, 55, 500},{5870, 3121, 192, 192, 55, 800}
        	            ,{5898, 2985, 173, 173, 55, 500},{6034, 2985, 174, 174, 55, 500},{6472, 3254, 171, 171, 55, 500},{6774, 2918, 173, 173, 55, 800},{6883, 3069, 189, 189, 55, 500},{6923, 2926, 179, 179, 55, 800}
        	            ,{7238, 3307, 172, 172, 55, 500},{7274, 2988, 197, 197, 55, 500},{7390, 3142, 199, 199, 55, 500},{7428, 3350, 182, 182, 55, 500},{7562, 2923, 178, 178, 55, 500},{7571, 3092, 198, 198, 55, 800}
        	            ,{8470, 2977, 173, 173, 55, 500},{9306, 2999, 170, 170, 55, 500},{9776, 3078, 195, 195, 55, 500},{9946, 2921, 198, 198, 55, 500},{10141, 2897, 176, 176, 55, 500},{10547, 3314, 179, 179, 55, 500}
        	            ,{10969, 2994, 195, 195, 55, 500},{11282, 3303, 174, 174, 55, 500},{1442, 3833, 184, 184, 55, 800},{1892, 3764, 179, 179, 55, 800},{2209, 3479, 189, 189, 55, 800},{2661, 3691, 175, 175, 55, 500}
        	            ,{3313, 3809, 180, 180, 55, 500},{3749, 3700, 179, 179, 55, 500},{4327, 3627, 195, 195, 55, 500},{5424, 3783, 188, 188, 55, 500},{6144, 3535, 190, 190, 55, 500},{6905, 3830, 186, 186, 55, 500}
        	            ,{7076, 3550, 179, 179, 55, 500},{7240, 3557, 175, 175, 55, 500},{7625, 3807, 175, 175, 55, 800},{7845, 3436, 189, 189, 55, 500},{8546, 3372, 184, 184, 55, 800},{9018, 3597, 174, 174, 55, 500}
        	            ,{9679, 3397, 199, 199, 55, 500},{9966, 3713, 198, 198, 55, 800},{10799, 3807, 183, 183, 55, 500},{11255, 3785, 194, 194, 55, 500},{546, 3959, 173, 173, 55, 800},{1007, 4299, 187, 187, 55, 500}
        	            ,{2001, 4271, 190, 190, 55, 500},{2424, 4210, 198, 198, 55, 500},{4257, 4262, 184, 184, 55, 500},{4417, 4303, 174, 174, 55, 500},{4431, 3994, 182, 182, 55, 500},{4455, 4179, 176, 176, 55, 500}
        	            ,{4545, 4083, 171, 171, 55, 500},{4551, 4282, 197, 197, 55, 500},{4678, 3957, 191, 191, 55, 500},{4721, 4168, 193, 193, 55, 800},{4903, 4023, 183, 183, 55, 800},{5003, 4158, 188, 188, 55, 500}
        	            ,{5182, 4110, 175, 175, 55, 800},{5186, 4235, 172, 172, 55, 500},{6029, 3955, 173, 173, 55, 500},{6387, 3860, 190, 190, 55, 800},{6895, 4234, 190, 190, 55, 500},{8027, 3874, 180, 180, 55, 500}
        	            ,{8314, 4304, 198, 198, 55, 500},{8336, 4132, 190, 190, 55, 500},{8703, 3848, 180, 180, 55, 800},{9020, 4094, 183, 183, 55, 500},{9443, 4007, 192, 192, 55, 500},{10026, 4111, 188, 188, 55, 500}
        	            ,{10398, 3864, 173, 173, 55, 500},{10680, 4211, 191, 191, 55, 800},{11280, 4250, 198, 198, 55, 500},{417, 4424, 194, 194, 55, 500},{911, 4640, 179, 179, 55, 800},{1320, 4531, 191, 191, 55, 500}
        	            ,{2686, 4636, 184, 184, 55, 800},{3535, 4787, 171, 171, 55, 500},{3866, 4393, 188, 188, 55, 500},{3993, 4347, 175, 175, 55, 500},{4023, 4483, 183, 183, 55, 800},{4059, 4612, 188, 188, 55, 500}
        	            ,{4120, 4383, 188, 188, 55, 500},{4622, 4484, 200, 200, 55, 500},{4705, 4353, 182, 182, 55, 500},{4786, 4642, 170, 170, 55, 800},{4886, 4325, 181, 181, 55, 500},{4933, 4494, 172, 172, 55, 500}
        	            ,{4938, 4656, 179, 179, 55, 500},{5039, 4392, 186, 186, 55, 500},{5482, 4725, 187, 187, 55, 500},{5876, 4375, 175, 175, 55, 500},{6553, 4477, 179, 179, 55, 800},{7318, 4338, 170, 170, 55, 500}
        	            ,{7595, 4655, 171, 171, 55, 800},{7935, 4477, 188, 188, 55, 500},{8436, 4724, 189, 189, 55, 500},{9286, 4530, 193, 193, 55, 500},{9632, 4729, 200, 200, 55, 500},{10122, 4682, 182, 182, 55, 500}
        	            ,{10903, 4592, 173, 173, 55, 500},{496, 4892, 183, 183, 55, 500},{1001, 5176, 183, 183, 55, 500},{1372, 5049, 183, 183, 55, 500},{2179, 4953, 193, 193, 55, 500},{2579, 4965, 196, 196, 55, 500}
        	            ,{3136, 4814, 192, 192, 55, 500},{3749, 5071, 195, 195, 55, 500},{4775, 4825, 170, 170, 55, 500},{5576, 4931, 182, 182, 55, 800},{5693, 5120, 200, 200, 55, 800},{5968, 4800, 183, 183, 55, 500}
        	            ,{6156, 5245, 181, 181, 55, 500},{6381, 4854, 192, 192, 55, 500},{6913, 4944, 170, 170, 55, 800},{7444, 5091, 200, 200, 55, 500},{7916, 4999, 182, 182, 55, 500},{8309, 5242, 170, 170, 55, 500}
        	            ,{8844, 5159, 174, 174, 55, 800},{9255, 5154, 186, 186, 55, 800},{9830, 5171, 174, 174, 55, 500},{10632, 4898, 187, 187, 55, 500},{11250, 4825, 170, 170, 55, 500},{424, 5623, 177, 177, 55, 500}
        	            ,{683, 5484, 183, 183, 55, 500},{1318, 5442, 175, 175, 55, 500},{2194, 5400, 196, 196, 55, 800},{2196, 5634, 179, 179, 55, 500},{2805, 5431, 175, 175, 55, 500},{3249, 5439, 180, 180, 55, 800}
        	            ,{3830, 5446, 190, 190, 55, 500},{4322, 5329, 200, 200, 55, 500},{4768, 5362, 192, 192, 55, 500},{4787, 5533, 180, 180, 55, 800},{4854, 5690, 187, 187, 55, 500},{4945, 5423, 173, 173, 55, 500}
        	            ,{4968, 5572, 174, 174, 55, 500},{5100, 5539, 180, 180, 55, 500},{5253, 5492, 174, 174, 55, 800},{5400, 5756, 175, 175, 55, 500},{6321, 5642, 199, 199, 55, 800},{6602, 5290, 192, 192, 55, 500}
        	            ,{7097, 5502, 198, 198, 55, 800},{7876, 5655, 190, 190, 55, 500},{8198, 5557, 174, 174, 55, 500},{8475, 5504, 180, 180, 55, 500},{8619, 5723, 177, 177, 55, 500},{8755, 5616, 190, 190, 55, 500}
        	            ,{9460, 5538, 175, 175, 55, 500},{10026, 5639, 180, 180, 55, 500},{10371, 5289, 197, 197, 55, 800},{10669, 5579, 190, 190, 55, 500},{10980, 5321, 197, 197, 55, 500},{634, 5838, 185, 185, 55, 500}
        	            ,{1001, 6172, 189, 189, 55, 800},{1440, 5845, 172, 172, 55, 500},{2056, 5834, 174, 174, 55, 500},{2240, 5773, 186, 186, 55, 500},{2303, 5931, 182, 182, 55, 500},{2533, 6043, 200, 200, 55, 500}
        	            ,{2709, 6177, 188, 188, 55, 500},{2930, 6172, 170, 170, 55, 500},{3149, 6134, 193, 193, 55, 500},{3340, 6218, 194, 194, 55, 500},{3389, 5872, 178, 178, 55, 500},{3527, 6095, 170, 170, 55, 500}
        	            ,{4142, 6067, 179, 179, 55, 500},{4429, 5766, 187, 187, 55, 500},{5068, 6071, 200, 200, 55, 500},{5120, 5947, 170, 170, 55, 500},{5251, 6099, 196, 196, 55, 500},{5259, 5911, 199, 199, 55, 500}
        	            ,{5430, 6175, 179, 179, 55, 500},{5811, 6211, 179, 179, 55, 500},{5936, 6005, 195, 195, 55, 800},{6726, 6119, 180, 180, 55, 500},{7171, 5973, 185, 185, 55, 500},{7550, 5998, 191, 191, 55, 500}
        	            ,{8236, 5980, 184, 184, 55, 500},{8704, 5863, 171, 171, 55, 500},{9067, 5857, 200, 200, 55, 500},{9598, 6103, 178, 178, 55, 800},{10207, 6093, 191, 191, 55, 500},{10697, 6161, 194, 194, 55, 800}
        	            ,{11281, 5884, 190, 190, 55, 500},{418, 6704, 176, 176, 55, 500},{448, 6292, 173, 173, 55, 500},{857, 6550, 198, 198, 55, 500},{1407, 6340, 173, 173, 55, 500},{2203, 6388, 176, 176, 55, 500}
        	            ,{2525, 6656, 193, 193, 55, 800},{2735, 6541, 191, 191, 55, 800},{2788, 6386, 173, 173, 55, 500},{2899, 6631, 195, 195, 55, 500},{2961, 6363, 177, 177, 55, 500},{3040, 6707, 171, 171, 55, 500}
        	            ,{3057, 6512, 199, 199, 55, 500},{3154, 6407, 176, 176, 55, 500},{3165, 6654, 186, 186, 55, 500},{3275, 6574, 184, 184, 55, 500},{3295, 6353, 187, 187, 55, 800},{3451, 6470, 189, 189, 55, 500}
        	            ,{3512, 6313, 170, 170, 55, 500},{3617, 6430, 174, 174, 55, 500},{3715, 6267, 196, 196, 55, 800},{4172, 6539, 171, 171, 55, 500},{4784, 6485, 183, 183, 55, 500},{5181, 6299, 198, 198, 55, 500}
        	            ,{5241, 6692, 184, 184, 55, 500},{5273, 6553, 173, 173, 55, 800},{5414, 6325, 182, 182, 55, 500},{5431, 6556, 200, 200, 55, 800},{5537, 6423, 192, 192, 55, 800},{5669, 6307, 190, 190, 55, 800}
        	            ,{5697, 6655, 191, 191, 55, 500},{5709, 6461, 190, 190, 55, 500},{5832, 6365, 198, 198, 55, 500},{5864, 6706, 170, 170, 55, 500},{5942, 6522, 171, 171, 55, 500},{6263, 6711, 190, 190, 55, 800}
        	            ,{6347, 6374, 175, 175, 55, 500},{6744, 6511, 191, 191, 55, 500},{7125, 6408, 190, 190, 55, 500},{7709, 6337, 196, 196, 55, 500},{8340, 6376, 192, 192, 55, 500},{8936, 6596, 173, 173, 55, 500}
        	            ,{9183, 6293, 187, 187, 55, 500},{9316, 6715, 175, 175, 55, 800},{10003, 6477, 177, 177, 55, 800},{10390, 6484, 189, 189, 55, 800},{11176, 6429, 173, 173, 55, 500},{594, 7191, 192, 192, 55, 500}
        	            ,{1001, 7063, 172, 172, 55, 500},{1513, 6742, 184, 184, 55, 500},{2119, 6816, 189, 189, 55, 500},{2518, 6865, 181, 181, 55, 500},{3142, 6798, 190, 190, 55, 500},{3572, 6829, 184, 184, 55, 500}
        	            ,{3750, 6753, 182, 182, 55, 800},{4013, 6972, 182, 182, 55, 800},{4624, 7123, 181, 181, 55, 500},{5317, 6886, 180, 180, 55, 500},{5380, 6762, 187, 187, 55, 500},{5498, 7008, 190, 190, 55, 500}
        	            ,{5524, 6817, 184, 184, 55, 500},{5662, 6889, 174, 174, 55, 500},{5841, 6882, 170, 170, 55, 500},{6984, 7025, 174, 174, 55, 500},{7432, 6935, 187, 187, 55, 500},{8175, 7058, 183, 183, 55, 500}
        	            ,{8628, 6734, 170, 170, 55, 500},{10662, 6789, 173, 173, 55, 500},{11137, 6911, 176, 176, 55, 500},{774, 7543, 182, 182, 55, 500},{1380, 7588, 185, 185, 55, 800},{1527, 7221, 187, 187, 55, 800}
        	            ,{2321, 7475, 195, 195, 55, 500},{2573, 7612, 174, 174, 55, 500},{2694, 7534, 174, 174, 55, 500},{2734, 7397, 181, 181, 55, 500},{2853, 7540, 198, 198, 55, 500},{2918, 7400, 176, 176, 55, 800}
        	            ,{2998, 7561, 189, 189, 55, 500},{3089, 7399, 198, 198, 55, 500},{3453, 7383, 178, 178, 55, 500},{4013, 7504, 176, 176, 55, 500},{4345, 7416, 181, 181, 55, 800},{4626, 7653, 185, 185, 55, 500}
        	            ,{4973, 7283, 185, 185, 55, 500},{5994, 7410, 198, 198, 55, 500},{6377, 7227, 192, 192, 55, 500},{6652, 7504, 183, 183, 55, 500},{7059, 7464, 195, 195, 55, 500},{7461, 7341, 181, 181, 55, 500}
        	            ,{7751, 7623, 199, 199, 55, 500},{8168, 7605, 190, 190, 55, 500},{8443, 7361, 171, 171, 55, 500},{8776, 7303, 178, 178, 55, 500},{10801, 7402, 182, 182, 55, 500},{11262, 7449, 198, 198, 55, 500}
        	            ,{415, 7709, 191, 191, 55, 500},{737, 8002, 171, 171, 55, 500},{1489, 7945, 198, 198, 55, 500},{2077, 8137, 197, 197, 55, 800},{2358, 7798, 181, 181, 55, 500},{2434, 7923, 196, 196, 55, 500}
        	            ,{2459, 8092, 190, 190, 55, 500},{2606, 7749, 198, 198, 55, 800},{2628, 8114, 176, 176, 55, 500},{2693, 7921, 194, 194, 55, 500},{2794, 7699, 177, 177, 55, 500},{2885, 7827, 173, 173, 55, 500}
        	            ,{3116, 8023, 189, 189, 55, 500},{3191, 7848, 199, 199, 55, 500},{3252, 8125, 174, 174, 55, 500},{3689, 7977, 184, 184, 55, 500},{4125, 7969, 174, 174, 55, 500},{5057, 7954, 200, 200, 55, 500}
        	            ,{5389, 7688, 184, 184, 55, 800},{5897, 7940, 185, 185, 55, 500},{6369, 7832, 182, 182, 55, 500},{6852, 8149, 196, 196, 55, 500},{7321, 8074, 189, 189, 55, 500},{8228, 7990, 175, 175, 55, 800}
        	            ,{8616, 7786, 195, 195, 55, 500},{9173, 7701, 176, 176, 55, 500},{9855, 7970, 193, 193, 55, 800},{10522, 7808, 187, 187, 55, 500},{11095, 8002, 177, 177, 55, 500},{390, 8280, 194, 194, 55, 500}
        	            ,{1054, 8333, 173, 173, 55, 500},{1453, 8422, 170, 170, 55, 500},{2902, 8251, 196, 196, 55, 500},{3048, 8321, 187, 187, 55, 500},{3055, 8174, 183, 183, 55, 500},{3409, 8637, 200, 200, 55, 500}
        	            ,{3907, 8515, 189, 189, 55, 800},{4429, 8382, 177, 177, 55, 500},{4846, 8463, 172, 172, 55, 500},{5237, 8638, 186, 186, 55, 800},{5624, 8360, 180, 180, 55, 500},{6158, 8559, 192, 192, 55, 800}
        	            ,{6477, 8305, 184, 184, 55, 500},{7510, 8573, 187, 187, 55, 800},{7695, 8161, 173, 173, 55, 500},{8139, 8349, 179, 179, 55, 500},{8451, 8611, 194, 194, 55, 800},{8718, 8256, 178, 178, 55, 500}
        	            ,{9236, 8187, 194, 194, 55, 500},{9369, 8426, 193, 193, 55, 500},{9619, 8564, 199, 199, 55, 500},{9909, 8585, 198, 198, 55, 800},{10092, 8631, 196, 196, 55, 500},{10098, 8408, 199, 199, 55, 800}
        	            ,{10281, 8510, 198, 198, 55, 500},{10402, 8393, 176, 176, 55, 500},{10494, 8589, 179, 179, 55, 500},{10570, 8344, 186, 186, 55, 500},{10690, 8481, 181, 181, 55, 500},{10720, 8635, 198, 198, 55, 500}
        	            ,{10950, 8475, 187, 187, 55, 800},{469, 8764, 192, 192, 55, 500},{881, 8851, 183, 183, 55, 800},{1311, 8746, 188, 188, 55, 800},{2092, 9010, 196, 196, 55, 500},{2537, 9051, 195, 195, 55, 500}
        	            ,{3151, 8923, 177, 177, 55, 500},{3666, 8996, 181, 181, 55, 500},{4131, 9086, 199, 199, 55, 500},{4521, 8758, 200, 200, 55, 500},{5229, 9018, 197, 197, 55, 500},{5491, 9037, 173, 173, 55, 500}
        	            ,{5621, 8961, 193, 193, 55, 800},{5812, 9025, 174, 174, 55, 500},{5929, 8907, 174, 174, 55, 500},{6064, 8966, 195, 195, 55, 500},{6280, 8996, 178, 178, 55, 800},{7031, 8799, 186, 186, 55, 500}
        	            ,{7416, 8928, 184, 184, 55, 800},{8076, 9050, 170, 170, 55, 500},{8464, 8921, 170, 170, 55, 500},{8535, 9059, 172, 172, 55, 500},{8571, 8806, 179, 179, 55, 800},{8642, 8959, 173, 173, 55, 500}
        	            ,{8775, 9084, 196, 196, 55, 500},{8789, 8883, 176, 176, 55, 500},{8858, 8655, 182, 182, 55, 800},{9826, 8896, 173, 173, 55, 500},{9826, 9072, 175, 175, 55, 500},{9964, 8770, 174, 174, 55, 500}
        	            ,{9978, 9001, 181, 181, 55, 500},{10082, 8900, 196, 196, 55, 500},{10171, 9078, 183, 183, 55, 500},{10212, 8806, 187, 187, 55, 500},{10268, 8950, 186, 186, 55, 500},{10354, 8685, 198, 198, 55, 500}
        	            ,{10494, 8874, 171, 171, 55, 500},{10554, 9034, 185, 185, 55, 500},{10587, 8705, 182, 182, 55, 500},{10723, 8986, 196, 196, 55, 500},{10755, 8825, 187, 187, 55, 500},{10898, 8764, 200, 200, 55, 500}
        	            ,{11257, 9014, 183, 183, 55, 500},{456, 9478, 200, 200, 55, 500},{616, 9448, 182, 182, 55, 800},{655, 9316, 175, 175, 55, 500},{729, 9580, 178, 178, 55, 500},{894, 9355, 184, 184, 55, 500}
        	            ,{1302, 9149, 195, 195, 55, 800},{1561, 9441, 171, 171, 55, 500},{2224, 9501, 185, 185, 55, 500},{3087, 9407, 196, 196, 55, 800},{3804, 9412, 181, 181, 55, 500},{4679, 9146, 175, 175, 55, 800}
        	            ,{5220, 9348, 192, 192, 55, 500},{5374, 9205, 173, 173, 55, 500},{5393, 9495, 182, 182, 55, 500},{5476, 9368, 186, 186, 55, 500},{5539, 9517, 186, 186, 55, 500},{5597, 9137, 192, 192, 55, 500}
        	            ,{5731, 9270, 193, 193, 55, 500},{5742, 9445, 192, 192, 55, 500},{5925, 9427, 197, 197, 55, 500},{6059, 9569, 186, 186, 55, 500},{6174, 9412, 200, 200, 55, 500},{6421, 9175, 180, 180, 55, 500}
        	            ,{6856, 9177, 170, 170, 55, 500},{7248, 9255, 185, 185, 55, 500},{7860, 9421, 178, 178, 55, 500},{8588, 9370, 181, 181, 55, 800},{8646, 9174, 183, 183, 55, 500},{8994, 9383, 199, 199, 55, 500}
        	            ,{9401, 9197, 186, 186, 55, 500},{9660, 9239, 181, 181, 55, 500},{9708, 9431, 186, 186, 55, 500},{9817, 9512, 200, 200, 55, 500},{9824, 9217, 178, 178, 55, 500},{9854, 9378, 197, 197, 55, 500}
        	            ,{10018, 9432, 187, 187, 55, 800},{10097, 9248, 172, 172, 55, 500},{10176, 9464, 171, 171, 55, 800},{10224, 9316, 191, 191, 55, 500},{10331, 9475, 172, 172, 55, 500},{10347, 9145, 190, 190, 55, 500}
        	            ,{10429, 9343, 176, 176, 55, 500},{10524, 9211, 175, 175, 55, 800},{10564, 9461, 180, 180, 55, 500},{10741, 9492, 177, 177, 55, 500},{10945, 9543, 172, 172, 55, 500},{11284, 9403, 189, 189, 55, 800}
        	            ,{476, 9853, 180, 180, 55, 500},{514, 9674, 200, 200, 55, 500},{663, 9752, 182, 182, 55, 500},{830, 9765, 170, 170, 55, 500},{1227, 10046, 199, 199, 55, 500},{1346, 9878, 185, 185, 55, 500}
        	            ,{1450, 10056, 174, 174, 55, 500},{1523, 9862, 200, 200, 55, 500},{2338, 9883, 178, 178, 55, 500},{2650, 9640, 175, 175, 55, 800},{3320, 9981, 171, 171, 55, 800},{4296, 9950, 186, 186, 55, 800}
        	            ,{4476, 10048, 171, 171, 55, 500},{4542, 9617, 177, 177, 55, 500},{4705, 10008, 183, 183, 55, 500},{4997, 9681, 182, 182, 55, 500},{5248, 9695, 177, 177, 55, 500},{5372, 9880, 192, 192, 55, 500}
        	            ,{5487, 9758, 184, 184, 55, 500},{5565, 9999, 178, 178, 55, 800},{5673, 9809, 200, 200, 55, 500},{5712, 10055, 187, 187, 55, 500},{5741, 9659, 176, 176, 55, 500},{5832, 9798, 182, 182, 55, 500}
        	            ,{5907, 9929, 184, 184, 55, 500},{5990, 9687, 191, 191, 55, 500},{6048, 9895, 186, 186, 55, 500},{6193, 9993, 170, 170, 55, 800},{6233, 9836, 194, 194, 55, 500},{6274, 9669, 171, 171, 55, 500}
        	            ,{6934, 9668, 181, 181, 55, 500},{7151, 10062, 177, 177, 55, 800},{7310, 9646, 187, 187, 55, 500},{7783, 9981, 184, 184, 55, 500},{8203, 9872, 188, 188, 55, 500},{8868, 10009, 183, 183, 55, 500}
        	            ,{9342, 10069, 183, 183, 55, 500},{9412, 9953, 170, 170, 55, 800},{9433, 9708, 188, 188, 55, 500},{9537, 10028, 184, 184, 55, 500},{9648, 9869, 195, 195, 55, 500},{9710, 10049, 183, 183, 55, 500}
        	            ,{9727, 9651, 185, 185, 55, 500},{9884, 9863, 175, 175, 55, 500},{9957, 10014, 185, 185, 55, 500},{10003, 9639, 182, 182, 55, 500},{10055, 9788, 178, 178, 55, 500},{10118, 9946, 189, 189, 55, 500}
        	            ,{10184, 9726, 171, 171, 55, 500},{10323, 9956, 178, 178, 55, 500},{10392, 9802, 187, 187, 55, 800},{10408, 9604, 184, 184, 55, 500},{10539, 9900, 172, 172, 55, 500},{10574, 9742, 195, 195, 55, 500}
        	            ,{10660, 9615, 173, 173, 55, 500},{10717, 9932, 178, 178, 55, 800},{10835, 9722, 196, 196, 55, 500},{10886, 9965, 174, 174, 55, 500},{10994, 9717, 196, 196, 55, 500},{11053, 10013, 191, 191, 55, 500}
        	            ,{11089, 9871, 179, 179, 55, 500},{11134, 9711, 200, 200, 55, 800},{585, 10445, 199, 199, 55, 500},{946, 10430, 184, 184, 55, 500},{1105, 10201, 175, 175, 55, 500},{1338, 10159, 194, 194, 55, 500}
        	            ,{1657, 10182, 184, 184, 55, 500},{2396, 10220, 186, 186, 55, 800},{2806, 10262, 186, 186, 55, 500},{3727, 10179, 189, 189, 55, 800},{4152, 10436, 181, 181, 55, 500},{4153, 10099, 191, 191, 55, 800}
        	            ,{4213, 10284, 186, 186, 55, 800},{4303, 10090, 200, 200, 55, 800},{4349, 10476, 197, 197, 55, 500},{4386, 10256, 197, 197, 55, 500},{4588, 10461, 187, 187, 55, 800},{4690, 10238, 173, 173, 55, 500}
        	            ,{4735, 10410, 182, 182, 55, 500},{4823, 10269, 190, 190, 55, 500},{5203, 10481, 178, 178, 55, 500},{5509, 10135, 189, 189, 55, 500},{5516, 10308, 189, 189, 55, 500},{5754, 10234, 184, 184, 55, 800}
        	            ,{5902, 10092, 186, 186, 55, 500},{5903, 10248, 192, 192, 55, 800},{6035, 10150, 186, 186, 55, 500},{6069, 10507, 173, 173, 55, 800},{6143, 10235, 199, 199, 55, 500},{6385, 10170, 182, 182, 55, 500}
        	            ,{6858, 10344, 179, 179, 55, 500},{7726, 10478, 195, 195, 55, 500},{8472, 10343, 194, 194, 55, 500},{8874, 10445, 172, 172, 55, 800},{9259, 10228, 179, 179, 55, 800},{9385, 10409, 196, 196, 55, 500}
        	            ,{9455, 10210, 180, 180, 55, 800},{9582, 10373, 182, 182, 55, 800},{9606, 10180, 177, 177, 55, 500},{9744, 10217, 181, 181, 55, 500},{9766, 10522, 189, 189, 55, 500},{9889, 10329, 183, 183, 55, 500}
        	            ,{9920, 10507, 175, 175, 55, 500},{9990, 10203, 170, 170, 55, 500},{10087, 10538, 170, 170, 55, 500},{10136, 10360, 194, 194, 55, 800},{10139, 10209, 192, 192, 55, 500},{10288, 10111, 198, 198, 55, 500}
        	            ,{10295, 10373, 182, 182, 55, 500},{10392, 10250, 183, 183, 55, 500},{10453, 10429, 200, 200, 55, 500},{10526, 10084, 172, 172, 55, 500},{10566, 10231, 181, 181, 55, 500},{10693, 10476, 176, 176, 55, 500}
        	            ,{10703, 10326, 175, 175, 55, 800},{10750, 10113, 182, 182, 55, 500},{10865, 10431, 200, 200, 55, 500},{10912, 10104, 199, 199, 55, 800},{10926, 10293, 171, 171, 55, 500},{11261, 10477, 182, 182, 55, 500}
        	            ,{598, 11018, 188, 188, 55, 500},{752, 10749, 193, 193, 55, 500},{1827, 10573, 193, 193, 55, 800},{2215, 10978, 171, 171, 55, 500},{2348, 10600, 177, 177, 55, 500},{2955, 10997, 192, 192, 55, 500}
        	            ,{3307, 10653, 184, 184, 55, 500},{4052, 10827, 197, 197, 55, 500},{4079, 10588, 188, 188, 55, 500},{4150, 10946, 194, 194, 55, 800},{4230, 10615, 187, 187, 55, 500},{4298, 10891, 181, 181, 55, 800}
        	            ,{4312, 10748, 191, 191, 55, 500},{4430, 10653, 173, 173, 55, 800},{4558, 10832, 187, 187, 55, 500},{4561, 11002, 175, 175, 55, 500},{4647, 10613, 181, 181, 55, 500},{4793, 10692, 193, 193, 55, 500}
        	            ,{4800, 10905, 176, 176, 55, 500},{5353, 10847, 177, 177, 55, 500},{6385, 10849, 198, 198, 55, 500},{7046, 10637, 194, 194, 55, 500},{7512, 10986, 186, 186, 55, 500},{7927, 10961, 199, 199, 55, 800}
        	            ,{8317, 10838, 192, 192, 55, 500},{8751, 10814, 189, 189, 55, 500},{9358, 10632, 184, 184, 55, 800},{9507, 10573, 184, 184, 55, 500},{9585, 10744, 190, 190, 55, 800},{9800, 10774, 176, 176, 55, 500}
        	            ,{9802, 10996, 176, 176, 55, 500},{9954, 10932, 174, 174, 55, 500},{9993, 10671, 180, 180, 55, 500},{10195, 10638, 172, 172, 55, 500},{10200, 10914, 197, 197, 55, 800},{10285, 10768, 183, 183, 55, 500}
        	            ,{10325, 10564, 170, 170, 55, 800},{10399, 10950, 173, 173, 55, 500},{10461, 10767, 176, 176, 55, 800},{10517, 10988, 199, 199, 55, 800},{10569, 10605, 191, 191, 55, 500},{11223, 10984, 196, 196, 55, 500}
        	            ,{1097, 11408, 171, 171, 55, 500},{1570, 11346, 200, 200, 55, 800},{2254, 11355, 195, 195, 55, 800},{2645, 11327, 194, 194, 55, 500},{3100, 11397, 193, 193, 55, 800},{3381, 11065, 174, 174, 55, 500}
        	            ,{3623, 11507, 198, 198, 55, 800},{3906, 11488, 172, 172, 55, 500},{3909, 11238, 200, 200, 55, 500},{3926, 11093, 173, 173, 55, 500},{4026, 11343, 182, 182, 55, 800},{4109, 11457, 186, 186, 55, 500}
        	            ,{4114, 11179, 188, 188, 55, 800},{4274, 11248, 188, 188, 55, 500},{4307, 11439, 188, 188, 55, 500},{4311, 11053, 200, 200, 55, 500},{4448, 11246, 198, 198, 55, 800},{4458, 11413, 171, 171, 55, 500}
        	            ,{4557, 11143, 183, 183, 55, 500},{4615, 11325, 186, 186, 55, 500},{4701, 11494, 183, 183, 55, 500},{4815, 11113, 183, 183, 55, 800},{4853, 11454, 176, 176, 55, 500},{5214, 11367, 190, 190, 55, 800}
        	            ,{5895, 11041, 180, 180, 55, 500},{5911, 11501, 185, 185, 55, 500},{6251, 11260, 191, 191, 55, 500},{6838, 11452, 184, 184, 55, 800},{6971, 11053, 171, 171, 55, 500},{7477, 11364, 173, 173, 55, 500}
        	            ,{8160, 11288, 172, 172, 55, 800},{8909, 11248, 183, 183, 55, 500},{9610, 11414, 188, 188, 55, 500},{9762, 11240, 176, 176, 55, 500},{9807, 11364, 185, 185, 55, 800},{9919, 11101, 182, 182, 55, 500}
        	            ,{10004, 11262, 187, 187, 55, 500},{10019, 11481, 186, 186, 55, 500},{10080, 11116, 178, 178, 55, 500},{10137, 11383, 183, 183, 55, 500},{10224, 11218, 170, 170, 55, 500},{10319, 11389, 170, 170, 55, 500}
        	            ,{10370, 11166, 183, 183, 55, 800},{10456, 11282, 197, 197, 55, 500},{10652, 11308, 173, 173, 55, 800},{10668, 11135, 195, 195, 55, 500},{10721, 11432, 197, 197, 55, 500},{11168, 11409, 172, 172, 55, 800}
        	            ,{509, 11607, 177, 177, 55, 800},{1093, 11799, 180, 180, 55, 500},{1159, 11623, 192, 192, 55, 500},{1415, 11702, 197, 197, 55, 500},{1545, 11581, 185, 185, 55, 500},{1872, 11774, 193, 193, 55, 800}
        	            ,{2876, 11786, 173, 173, 55, 500},{3213, 11967, 197, 197, 55, 500},{3867, 11918, 197, 197, 55, 500},{3874, 11738, 188, 188, 55, 800},{4057, 11783, 200, 200, 55, 500},{4086, 11636, 179, 179, 55, 800}
        	            ,{4170, 11936, 175, 175, 55, 500},{4239, 11683, 186, 186, 55, 500},{4336, 11855, 199, 199, 55, 500},{4460, 11728, 180, 180, 55, 800},{4533, 11528, 198, 198, 55, 500},{4545, 11866, 195, 195, 55, 500}
        	            ,{4718, 11639, 187, 187, 55, 800},{5495, 11749, 197, 197, 55, 500},{6251, 11887, 186, 186, 55, 500},{6744, 11857, 182, 182, 55, 500},{7368, 11962, 187, 187, 55, 500},{7798, 11573, 197, 197, 55, 500}
        	            ,{8473, 11621, 194, 194, 55, 500},{9180, 11688, 175, 175, 55, 500},{9666, 11555, 181, 181, 55, 800},{9717, 11827, 197, 197, 55, 500},{9756, 11672, 183, 183, 55, 500},{9837, 11538, 172, 172, 55, 500}
        	            ,{9865, 11928, 181, 181, 55, 500},{10017, 11882, 178, 178, 55, 500},{10042, 11711, 178, 178, 55, 800},{10172, 11921, 187, 187, 55, 800},{10293, 11741, 170, 170, 55, 500},{10297, 11542, 193, 193, 55, 500}
        	            ,{10416, 11822, 170, 170, 55, 500},{10441, 11604, 191, 191, 55, 500},{10562, 11954, 195, 195, 55, 500},{10573, 11527, 175, 175, 55, 800},{10590, 11689, 197, 197, 55, 500},{10790, 11579, 191, 191, 55, 500}
        	            ,{442, 12185, 187, 187, 55, 500},{962, 12235, 174, 174, 55, 500},{962, 12467, 189, 189, 55, 500},{1446, 12471, 183, 183, 55, 500},{1454, 12012, 173, 173, 55, 500},{1845, 12161, 196, 196, 55, 500}
        	            ,{2413, 12117, 187, 187, 55, 500},{3291, 12379, 186, 186, 55, 500},{3723, 12415, 176, 176, 55, 500},{4001, 12029, 197, 197, 55, 500},{4128, 12135, 188, 188, 55, 500},{4264, 12281, 191, 191, 55, 500}
        	            ,{4302, 12083, 181, 181, 55, 500},{4432, 12017, 186, 186, 55, 500},{4527, 12267, 194, 194, 55, 500},{4589, 12093, 170, 170, 55, 500},{4708, 12305, 192, 192, 55, 500},{4758, 12044, 178, 178, 55, 500}
        	            ,{4783, 12187, 171, 171, 55, 500},{4917, 12400, 174, 174, 55, 800},{4923, 12132, 194, 194, 55, 500},{5111, 12408, 184, 184, 55, 500},{5117, 12263, 176, 176, 55, 500},{5215, 12074, 187, 187, 55, 500}
        	            ,{5289, 12277, 172, 172, 55, 500},{5806, 12087, 194, 194, 55, 800},{7351, 12191, 196, 196, 55, 800},{8074, 12000, 198, 198, 55, 800},{8565, 12401, 188, 188, 55, 500},{8898, 12098, 198, 198, 55, 500}
        	            ,{9190, 12394, 193, 193, 55, 500},{9608, 12328, 176, 176, 55, 500},{9993, 12052, 178, 178, 55, 500},{10082, 12270, 187, 187, 55, 500},{10175, 12099, 186, 186, 55, 500},{10346, 12044, 195, 195, 55, 500}
        	            ,{10485, 12223, 194, 194, 55, 500},{11165, 12106, 174, 174, 55, 500},{506, 12922, 175, 175, 55, 500},{507, 12719, 184, 184, 55, 500},{626, 12562, 171, 171, 55, 500},{664, 12906, 187, 187, 55, 500}
        	            ,{760, 12798, 188, 188, 55, 500},{767, 12507, 170, 170, 55, 800},{859, 12606, 182, 182, 55, 500},{949, 12953, 193, 193, 55, 500},{1033, 12740, 193, 193, 55, 500},{1118, 12544, 196, 196, 55, 500}
        	            ,{1202, 12684, 188, 188, 55, 500},{1281, 12499, 183, 183, 55, 800},{1476, 12863, 179, 179, 55, 500},{1719, 12555, 172, 172, 55, 500},{2032, 12914, 193, 193, 55, 500},{2161, 12500, 186, 186, 55, 500}
        	            ,{2517, 12789, 176, 176, 55, 500},{2862, 12594, 180, 180, 55, 500},{3115, 12896, 185, 185, 55, 500},{3931, 12897, 191, 191, 55, 500},{4374, 12760, 181, 181, 55, 800},{5468, 12692, 186, 186, 55, 500}
        	            ,{5628, 12920, 199, 199, 55, 500},{5776, 12753, 186, 186, 55, 500},{5880, 12622, 170, 170, 55, 500},{6051, 12604, 185, 185, 55, 500},{6089, 12777, 180, 180, 55, 500},{6186, 12519, 179, 179, 55, 500}
        	            ,{6199, 12846, 200, 200, 55, 500},{6219, 12686, 178, 178, 55, 500},{6372, 12725, 194, 194, 55, 500},{6390, 12566, 189, 189, 55, 500},{7954, 12520, 200, 200, 55, 500},{8475, 12814, 194, 194, 55, 500}
        	            ,{9229, 12932, 182, 182, 55, 500},{9763, 12820, 175, 175, 55, 500},{10129, 12678, 173, 173, 55, 800},{10440, 12546, 184, 184, 55, 500},{10495, 12744, 175, 175, 55, 800},{10634, 12607, 190, 190, 55, 500}
        	            ,{10637, 12925, 200, 200, 55, 500},{10705, 12791, 183, 183, 55, 500},{10993, 12824, 173, 173, 55, 500},{11214, 12619, 192, 192, 55, 800},{406, 13028, 185, 185, 55, 500},{444, 13189, 200, 200, 55, 800}
        	            ,{584, 13077, 179, 179, 55, 500},{597, 13371, 185, 185, 55, 500},{699, 13159, 194, 194, 55, 500},{777, 12987, 198, 198, 55, 500},{806, 13307, 173, 173, 55, 500},{913, 13180, 174, 174, 55, 500}
        	            ,{1027, 13312, 195, 195, 55, 500},{1098, 13163, 173, 173, 55, 800},{1111, 12989, 179, 179, 55, 800},{2054, 13386, 191, 191, 55, 500},{2475, 13308, 197, 197, 55, 500},{2894, 13244, 198, 198, 55, 500}
        	            ,{3459, 13123, 196, 196, 55, 500},{4273, 13309, 170, 170, 55, 500},{4836, 13100, 172, 172, 55, 500},{5523, 13128, 191, 191, 55, 500},{5811, 13220, 175, 175, 55, 500},{5895, 12962, 183, 183, 55, 800}
        	            ,{5959, 13104, 182, 182, 55, 500},{6053, 13228, 192, 192, 55, 500},{6067, 12983, 183, 183, 55, 500},{6190, 13191, 185, 185, 55, 500},{6213, 13406, 181, 181, 55, 800},{6224, 13008, 174, 174, 55, 500}
        	            ,{6342, 13191, 193, 193, 55, 500},{6385, 13032, 174, 174, 55, 800},{6461, 13285, 198, 198, 55, 500},{6578, 13422, 180, 180, 55, 500},{6589, 13134, 200, 200, 55, 500},{6716, 13270, 200, 200, 55, 500}
        	            ,{6903, 13255, 197, 197, 55, 500},{6987, 13382, 188, 188, 55, 800},{7117, 13282, 181, 181, 55, 500},{7323, 13341, 176, 176, 55, 800},{7402, 13201, 188, 188, 55, 500},{7524, 13267, 194, 194, 55, 500}
        	            ,{7562, 13070, 187, 187, 55, 500},{7688, 13275, 177, 177, 55, 800},{7694, 13418, 175, 175, 55, 800},{8056, 13202, 183, 183, 55, 500},{9654, 13188, 174, 174, 55, 500},{10236, 13063, 194, 194, 55, 500}
        	            ,{10596, 13110, 177, 177, 55, 500},{10596, 13294, 182, 182, 55, 500},{10712, 13370, 200, 200, 55, 800},{10718, 13211, 186, 186, 55, 500},{10864, 12969, 173, 173, 55, 500},{10873, 13186, 182, 182, 55, 500}
        	            ,{10902, 13366, 197, 197, 55, 500},{11019, 13251, 199, 199, 55, 500},{11080, 13126, 170, 170, 55, 800},{11273, 13318, 175, 175, 55, 800},{402, 13607, 191, 191, 55, 500},{645, 13573, 183, 183, 55, 800}
        	            ,{754, 13726, 181, 181, 55, 500},{810, 13482, 193, 193, 55, 500},{1105, 13489, 180, 180, 55, 500},{1687, 13512, 197, 197, 55, 500},{2739, 13609, 186, 186, 55, 500},{3136, 13641, 181, 181, 55, 500}
        	            ,{3583, 13584, 181, 181, 55, 800},{4332, 13616, 198, 198, 55, 500},{4614, 13835, 179, 179, 55, 500},{4883, 13727, 198, 198, 55, 500},{5175, 13550, 197, 197, 55, 500},{5657, 13627, 182, 182, 55, 500}
        	            ,{6040, 13858, 172, 172, 55, 500},{6368, 13456, 196, 196, 55, 500},{6540, 13832, 172, 172, 55, 500},{6723, 13605, 170, 170, 55, 800},{6832, 13444, 190, 190, 55, 500},{7001, 13564, 170, 170, 55, 500}
        	            ,{7100, 13676, 170, 170, 55, 500},{7153, 13539, 186, 186, 55, 800},{7282, 13707, 196, 196, 55, 500},{7349, 13516, 177, 177, 55, 500},{7520, 13533, 174, 174, 55, 800},{7525, 13764, 197, 197, 55, 500}
        	            ,{7691, 13748, 174, 174, 55, 500},{7719, 13578, 187, 187, 55, 500},{8239, 13678, 199, 199, 55, 500},{8640, 13448, 182, 182, 55, 500},{9170, 13545, 173, 173, 55, 500},{9348, 13871, 195, 195, 55, 500}
        	            ,{9760, 13689, 195, 195, 55, 800},{10151, 13561, 200, 200, 55, 800},{10665, 13547, 185, 185, 55, 500},{10831, 13912, 195, 195, 55, 800},{10989, 13512, 192, 192, 55, 800},{11151, 13482, 182, 182, 55, 500}
        	            ,{11265, 13643, 178, 178, 55, 800},{432, 14069, 189, 189, 55, 500},{922, 14028, 193, 193, 55, 500},{1049, 14275, 189, 189, 55, 500},{1113, 13997, 175, 175, 55, 800},{1146, 14170, 179, 179, 55, 500}
        	            ,{1230, 14389, 189, 189, 55, 500},{1339, 14269, 190, 190, 55, 500},{1495, 14218, 171, 171, 55, 800},{1628, 14014, 198, 198, 55, 500},{1638, 14304, 200, 200, 55, 500},{1800, 14265, 200, 200, 55, 500}
        	            ,{2138, 14001, 181, 181, 55, 500},{2508, 14399, 186, 186, 55, 500},{2599, 13978, 192, 192, 55, 500},{3207, 14095, 173, 173, 55, 800},{3603, 14355, 198, 198, 55, 500},{3914, 14046, 189, 189, 55, 500}
        	            ,{4602, 14237, 180, 180, 55, 500},{5174, 14349, 170, 170, 55, 500},{5593, 14103, 183, 183, 55, 800},{6513, 14280, 187, 187, 55, 500},{6965, 14223, 181, 181, 55, 500},{7592, 13972, 180, 180, 55, 800}
        	            ,{7633, 14363, 179, 179, 55, 500},{8110, 14314, 197, 197, 55, 500},{8551, 13991, 195, 195, 55, 500},{8691, 14250, 190, 190, 55, 500},{8831, 14123, 179, 179, 55, 500},{8900, 14319, 190, 190, 55, 800}
        	            ,{8961, 13957, 188, 188, 55, 500},{9014, 14150, 186, 186, 55, 500},{9228, 14255, 171, 171, 55, 500},{9842, 14275, 197, 197, 55, 800},{10350, 13920, 195, 195, 55, 500},{10623, 14369, 175, 175, 55, 500}
        	            ,{11217, 14007, 177, 177, 55, 500},{543, 14518, 172, 172, 55, 500},{959, 14630, 174, 174, 55, 500},{1025, 14456, 199, 199, 55, 500},{1136, 14585, 183, 183, 55, 500},{1295, 14617, 182, 182, 55, 800}
        	            ,{1379, 14743, 194, 194, 55, 500},{1408, 14505, 195, 195, 55, 500},{1540, 14440, 198, 198, 55, 500},{1600, 14622, 200, 200, 55, 500},{1828, 14525, 192, 192, 55, 500},{1993, 14681, 175, 175, 55, 500}
        	            ,{2956, 14450, 198, 198, 55, 500},{3536, 14779, 188, 188, 55, 800},{4263, 14864, 183, 183, 55, 500},{4268, 14557, 192, 192, 55, 500},{5223, 14745, 178, 178, 55, 800},{5607, 14750, 200, 200, 55, 500}
        	            ,{6060, 14536, 173, 173, 55, 500},{6272, 14861, 186, 186, 55, 500},{6738, 14833, 177, 177, 55, 500},{7183, 14704, 198, 198, 55, 800},{7872, 14736, 177, 177, 55, 500},{8524, 14549, 192, 192, 55, 500}
        	            ,{8792, 14846, 196, 196, 55, 500},{9012, 14497, 187, 187, 55, 500},{9705, 14819, 185, 185, 55, 500},{10332, 14616, 197, 197, 55, 500},{10861, 14667, 185, 185, 55, 800},{11235, 14469, 179, 179, 55, 800}
        	            ,{449, 14926, 194, 194, 55, 800},{2640, 14932, 192, 192, 55, 500},{3095, 14920, 184, 184, 55, 800},{4861, 14882, 194, 194, 55, 800},{8405, 14929, 187, 187, 55, 500},{9311, 14937, 191, 191, 55, 500}
        	            ,{11281, 14894, 190, 190, 55, 500}
        	        };

        data = keepOutsideIce(data);
        poolBase = data.length;                       // slot sesudah ini dipinjamkan ke ChunkManager (objek hasil generator)
        breakables = new breakable[data.length + BREAKABLE_POOL];
        breakableTile = new int[data.length + BREAKABLE_POOL];
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
        oakIdle = treeIdleFrames; oakHit = treeHitFrames; oakDown = treeDownFrames;
        frostIdle = frostAll(treeIdleFrames); frostHit = frostAll(treeHitFrames); frostDown = frostAll(treeDownFrames);

        for (int i = 0; i < data.length; i++) {
            int tileIndex = data[i][4];
            int hp        = data[i][5];

            // Kolom ke-7 (index 6) itu OPSIONAL: 1 = ada bayangan, 0 = gak ada.
            // Kalau entry-nya cuma 6 angka (format lama), default tetap 1
            // (ada bayangan) biar semua data pohon lama gak perlu diubah.
            int shadowFlag = (data[i].length >= 7) ? data[i][6] : 1;

            // pohon di pinggiran salju: versi "kena embun beku" (puncak daun bersalju)
            boolean frosty = tileIndex == 55 && gen.ice((data[i][0] + data[i][2] / 2f) / gp.tileSize,
                    (data[i][1] + data[i][3]) / (float) gp.tileSize) > WorldGen.FROST_ICE;
            BufferedImage[] idleSet = frosty ? frostIdle : treeIdleFrames;
            BufferedImage[] hitSet  = frosty ? frostHit  : treeHitFrames;
            BufferedImage[] downSet = frosty ? frostDown : treeDownFrames;

            BufferedImage img;
            if (tileIndex == 55 && idleSet[0] != null) {
                img = idleSet[0];
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
                breakables[i].setIdleFrames(idleSet);
                breakables[i].setHitFrames(hitSet);
                breakables[i].setDownFrames(downSet); 
            }
        }
    }
	
	
	private static BufferedImage[] frostAll(BufferedImage[] in) {
		BufferedImage[] out = new BufferedImage[in.length];
		for (int i = 0; i < in.length; i++) out[i] = in[i] == null ? null : IceSprites.frost(in[i]);
		return out;
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

        if (waterAnim != null) waterAnim.beginFrame();   // semua tile air frame ini pakai frame animasi yang sama

        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
                // tile rumput yang tertutup penuh salju / es tidak perlu digambar (lihat ChunkManager.isCovered)
                if (chunks != null && chunks.isCovered(worldCol, worldRow)) continue;

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

                    if (waterAnim != null && waterAnim.handles(tileNum)) {
                        // air: gelombang animasi + buih pantai
                        waterAnim.drawTile(g2, tileNum, worldCol, worldRow, screenX, screenY, gp.tileSize);
                    } else if (tileAnimImage[tileNum] != null) {
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

        // Peta dinamis: gambar kulit salju / es di atas tile dasar
        if (chunks != null) chunks.drawGround(g2, camX, camY);
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
			
			// ── Ikan kuning loncat: di atas air, di bawah objek berdiri / player ──
			if (fishManager != null) {
			    fishManager.draw(g2, fCamX, fCamY, gp.player.screenX, gp.player.screenY,
			                     gp.viewMinX(), gp.viewMinY(), gp.viewMaxX(), gp.viewMaxY());
			}

			// ── PASS 2: Kumpulkan semua standing objects ke drawList ──
			java.util.List<int[]> drawList = new java.util.ArrayList<>();
			
			// WorldObjects yang standing (isFlat == 0)
			for (int i = 0; i < worldObjects.length; i++) {
			if (worldObjects[i][5] == 0
			        && nearView(worldObjects[i][0], worldObjects[i][1], worldObjects[i][2], worldObjects[i][3], fCamX, fCamY)) {
			int sortY = worldObjects[i][1] + worldObjects[i][3] - 40; // worldY + height = kaki objek
			drawList.add(new int[]{sortY, 0, i});
			}
			}
			
			// Breakables (pohon tebang, batu)
			if (breakables != null) {
			    for (int i = 0; i < breakables.length; i++) {
			        breakable bk = breakables[i];   // satu kali baca: slot kolam bisa dikosongkan thread game kapan saja
			        if (bk == null) continue;
			        
			        // UPDATE selalu dipanggil, termasuk saat destroyed (biar timer respawn jalan)
			        bk.update();
			        
			        // Tapi hanya masuk drawList kalau belum destroyed
			        if (bk.destroyed) continue;
			        // pohon sekarang lebih banyak: yang jauh di luar layar tidak usah ikut di-sort
			        if (!nearView(bk.worldX, bk.worldY, bk.width, bk.height, fCamX, fCamY)) continue;
			        
			        int sortY = bk.worldY + bk.height - 10;
			        drawList.add(new int[]{sortY, 1, i});
			    }
			}
			
			
			// Hiasan hasil generator (semak dll) di chunk yang aktif
			if (chunks != null) chunks.collectDecor(drawList, fCamX, fCamY, 150);
			
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
			    if (b == null) break;   // chunk-nya keburu dibuang
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
			case 7: { // hiasan chunk (semak hutan / semak salju)
			    chunks.drawDecor(g2, index, fCamX, fCamY);
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
	    // CATATAN PETA BARU: rectangle manual untuk air / tembok gaib yang lama sudah di-comment (tanda //AIR-LAMA).
	    // Collision air sekarang DIHITUNG OTOMATIS dari tile peta, lihat WaterCollision.build() di akhir method ini.
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
	    //AIR-LAMA gp.objCol.add(1170,1400,30,105);
	    //AIR-LAMA gp.objCol.add(1220,1255,5000,135);
	    
	    gp.objCol.add(1040,1100,75,310);
	    gp.objCol.add(500,1100,45,75);
	    gp.objCol.add(545,1175,65,80);
	    
	    //collision rock gede dpn rumh
	    gp.objCol.add(1140,1210, 60, 70);
	    
	    //collsion air dpn jembatan
	    //AIR-LAMA gp.objCol.add(690, 2025, 350,150);
	    //AIR-LAMA gp.objCol.add(930, 1975, 100,40);
	    //AIR-LAMA gp.objCol.add(830, 2190, 50,50);
	    //AIR-LAMA gp.objCol.add(880, 2240, 140,30);
	    //AIR-LAMA gp.objCol.add(990, 2160, 50,110);
	    
	    
	    // coliision monestry
	    gp.objCol.add(1700, 630, 150 , 80);
	    
	    // collision witch house
	    gp.objCol.add(4000, 3820, 310 , 140);
	    
	    
	    //objek di air
	   // gp.objCol.add();   
	    gp.objCol.add(1200, 920, 60,80);
	    //AIR-LAMA gp.objCol.add(340,900,20,300);
	    //AIR-LAMA gp.objCol.add(340,1100,120,20); //colisiion samping ruins pembatas aer
	    //AIR-LAMA gp.objCol.add(340,10,20,1000); // colisiion samping ruins pembatas aer
	    //AIR-LAMA gp.objCol.add(450,900,20,500);
	    //AIR-LAMA gp.objCol.add(10, 380, 30000, 20);
	    //AIR-LAMA gp.objCol.add(480,1400,30,20);
	    //AIR-LAMA gp.objCol.add(520,1450,30,20);
	    //AIR-LAMA gp.objCol.add(350,1490,255,265);
	    gp.objCol.add(350,1490,45,9065);
	    // lanjutan tembok barat, hanya untuk bagian dunia yang BARU (di bawah peta lama): area peta lama dibiarkan persis seperti sebelumnya
	    int westWallTop = (gen != null ? gen.coreRows() : 321) * gp.tileSize;
	    gp.objCol.add(350, westWallTop, 45, WorldGen.WORLD_ROWS * gp.tileSize - westWallTop);
	    //AIR-LAMA gp.objCol.add(695,1520,110,220);
	    //AIR-LAMA gp.objCol.add(900,1520,105,220);
	    //AIR-LAMA gp.objCol.add(1095,1530,5000,210);
	    //AIR-LAMA gp.objCol.add(2040,1210,30,30);
	    //AIR-LAMA gp.objCol.add(2080,1115,30,85);
	    //AIR-LAMA gp.objCol.add(2130,400,30,1000);
	    
	    //AIR-LAMA gp.objCol.add(1050, 100, 550, 80); // collision pohon di depan air
	    
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
	        {1160,2110,70,70,48,1, 0},
	        {1380,2200,70,70,46,1, 0},

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
	        
	        {1426,3156,100,100,66,0,1},{1446,3166,100,100,66,0,1},{1406,3146,100,100,66,0,1},{1436,3186,100,100,66,0,1},
	        {2974,3215,100,100,66,0,1},{2994,3225,100,100,66,0,1},{2954,3205,100,100,66,0,1},

	        {3152,3369,100,100,66,0,1},{3179,3318,100,100,66,0,1},{3097,3384,100,100,66,0,1},

	     // pohon (2765,4161) -> merengang (nyebar)

	     {2678,4165,100,100,66,0,1},{2849,4198,100,100,66,0,1},{2824,4057,100,100,66,0,1},

	     // pohon (3379,3158) -> huge bush, dempet gerombol gede

	     {3342,3125,100,100,66,0,1},{3330,3131,100,100,66,0,1},

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

	     

	     {3163,3355,100,100,66,0,1},{3167,3331,100,100,66,0,1},{3128,3385,100,100,66,0,1},

	     // pohon (2765,4161) -> merengang (nyebar)

	     {2675,4168,100,100,66,0,1},{2843,4200,100,100,66,0,1},{2810,4055,100,100,66,0,1},

	     // pohon (3379,3158) -> huge bush, dempet gerombol gede

	     {3335,3135,100,100,66,0,1},{3308,3158,100,100,66,0,1},{3327,3123,100,100,66,0,1},{3350,3102,100,100,66,0,1},

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
	        
	        

	        // ── PETA HUTAN BARU ──
	        // Jembatan baru (isFlat=1, digambar di bawah semua objek). Bagian dek-nya boleh diinjak, lihat deck di akhir method.
	        // horizontal (di atas sungai, jalan utama menuju hutan timur) dan vertikal (jalan ke dermaga utara)
	        {3186,7121,100,100,73,1,0},{3286,7121,100,100,74,1,0},{3386,7121,100,100,75,1,0},
	        {4678,1530,100,100,25,1,0},{4678,1630,100,100,26,1,0},{4678,1730,100,100,27,1,0},
	        // batu pipih di rumput
	        {2928,576,48,48,65,1,0},{10320,7776,48,48,65,1,0},{5040,5424,48,48,65,1,0},{10744,9160,64,64,65,1,0},{3792,6864,48,48,65,1,0},{10792,9592,64,64,65,1,0},
	        {6720,6720,48,48,65,1,0},{9840,3648,48,48,65,1,0},{2056,4888,64,64,65,1,0},{7392,9744,48,48,65,1,0},{2824,1288,64,64,65,1,0},{6476,1676,56,56,65,1,0},
	        {8632,5656,64,64,65,1,0},{7528,2536,64,64,65,1,0},{7624,7048,64,64,65,1,0},{672,4128,48,48,65,1,0},{4168,9544,64,64,65,1,0},{2152,9016,64,64,65,1,0},
	        {2200,6520,64,64,65,1,0},{2396,7388,56,56,65,1,0},{3308,5468,56,56,65,1,0},{9024,4704,48,48,65,1,0},{8392,6040,64,64,65,1,0},{8448,3216,48,48,65,1,0},
	        {7816,7768,64,64,65,1,0},{2780,7436,56,56,65,1,0},{5228,524,56,56,65,1,0},{7528,12376,64,64,65,1,0},{6140,1484,56,56,65,1,0},{4840,2824,64,64,65,1,0},
	        {1672,6952,64,64,65,1,0},{2584,4792,64,64,65,1,0},{4988,1052,56,56,65,1,0},{6192,9504,48,48,65,1,0},{6720,12336,48,48,65,1,0},{10176,3168,48,48,65,1,0},
	        {9260,7628,56,56,65,1,0},{5512,5416,64,64,65,1,0},{8064,2064,48,48,65,1,0},{4936,1912,64,64,65,1,0},{3840,9888,48,48,65,1,0},{7292,6620,56,56,65,1,0},
	        {8784,4128,48,48,65,1,0},{6472,2584,64,64,65,1,0},{2064,5232,48,48,65,1,0},{1196,3884,56,56,65,1,0},{4464,3168,48,48,65,1,0},{7728,8592,48,48,65,1,0},
	        {8876,6812,56,56,65,1,0},{4460,7388,56,56,65,1,0},{8300,7148,56,56,65,1,0},{1192,5896,64,64,65,1,0},{5232,6000,48,48,65,1,0},{7048,2872,64,64,65,1,0},
	        {1432,4744,64,64,65,1,0},{7872,9456,48,48,65,1,0},{8204,2636,56,56,65,1,0},{1680,7344,48,48,65,1,0},{6960,12096,48,48,65,1,0},{2352,1824,48,48,65,1,0},
	        {7340,3068,56,56,65,1,0},{4360,1864,64,64,65,1,0},{4416,6864,48,48,65,1,0},{7244,1532,56,56,65,1,0},{5128,4456,64,64,65,1,0},{9836,7868,56,56,65,1,0},
	        {4940,4940,56,56,65,1,0},{4272,2592,48,48,65,1,0},{10552,7528,64,64,65,1,0},{9692,6764,56,56,65,1,0},{9360,6768,48,48,65,1,0},{4176,4416,48,48,65,1,0},
	        {6472,2056,64,64,65,1,0},{7192,3448,64,64,65,1,0},{2160,7920,48,48,65,1,0},{7728,6528,48,48,65,1,0},{6476,4076,56,56,65,1,0},
	        // semak-semak (standing, ikut Y-sort). Rapat di pinggir jalan & tepi air, jarang di tengah padang
	        {3728,2581,88,88,66,0,1},{3692,2556,96,96,66,0,1},{3663,2501,102,102,66,0,1},{7205,6598,92,92,66,0,1},{7258,6551,107,107,66,0,1},{7217,6571,105,105,66,0,1},
	        {7266,6579,97,97,66,0,1},{7204,6605,97,97,66,0,1},{4543,6738,97,97,66,0,1},{4488,6741,97,97,66,0,1},{4521,6733,100,100,66,0,1},{2258,6890,90,90,66,0,1},
	        {2251,6874,96,96,66,0,1},{2209,6837,87,87,66,0,1},{2226,6844,100,100,66,0,1},{2235,6840,88,88,66,0,1},{7634,10103,98,98,66,0,1},{7691,10060,99,99,66,0,1},
	        {7698,10081,82,82,66,0,1},{7726,10068,82,82,66,0,1},{2045,5093,85,85,66,0,1},{2100,5157,107,107,66,0,1},{2062,5159,108,108,66,0,1},{2028,5164,90,90,66,0,1},
	        {2103,5108,97,97,66,0,1},{5107,4380,96,96,66,0,1},{5150,4389,93,93,66,0,1},{5115,4423,93,93,66,0,1},{5168,4382,97,97,66,0,1},{1551,7929,84,84,66,0,1},
	        {1500,7876,103,103,66,0,1},{1503,7938,94,94,66,0,1},{1513,7924,90,90,66,0,1},{1550,7929,89,89,66,0,1},{7729,7755,96,96,66,0,1},{7735,7676,92,92,66,0,1},
	        {7738,7687,96,96,66,0,1},{7727,7650,99,99,66,0,1},{7737,7723,91,91,66,0,1},{6933,11155,105,105,66,0,1},{6921,11086,96,96,66,0,1},{6919,11100,86,86,66,0,1},
	        {6929,11087,105,105,66,0,1},{6975,11060,99,99,66,0,1},{4445,1111,89,89,66,0,1},{4447,1133,85,85,66,0,1},{4481,1054,90,90,66,0,1},{4489,1078,82,82,66,0,1},
	        {4490,1020,102,102,66,0,1},{3852,4602,89,89,66,0,1},{3917,4585,92,92,66,0,1},{3943,4531,106,106,66,0,1},{3909,4634,105,105,66,0,1},{3957,4555,106,106,66,0,1},
	        {1562,2794,96,96,66,0,1},{1499,2797,88,88,66,0,1},{1557,2829,92,92,66,0,1},{1490,2758,94,94,66,0,1},{5998,7569,85,85,66,0,1},{5958,7534,102,102,66,0,1},
	        {5971,7561,100,100,66,0,1},{5900,7511,98,98,66,0,1},{5968,7465,98,98,66,0,1},{6011,3095,87,87,66,0,1},{6035,3127,96,96,66,0,1},{5990,3111,106,106,66,0,1},
	        {5954,3160,82,82,66,0,1},{2214,9525,99,99,66,0,1},{2234,9527,85,85,66,0,1},{8455,6724,101,101,66,0,1},{8503,6655,98,98,66,0,1},{8523,6669,102,102,66,0,1},
	        {8532,6756,84,84,66,0,1},{971,4397,85,85,66,0,1},{1103,4422,84,84,66,0,1},{979,4402,103,103,66,0,1},{1027,4444,104,104,66,0,1},{2280,10845,105,105,66,0,1},
	        {2237,10836,105,105,66,0,1},{2196,10846,102,102,66,0,1},{2194,10865,93,93,66,0,1},{2215,10808,91,91,66,0,1},{8044,8948,92,92,66,0,1},{8056,8945,95,95,66,0,1},
	        {8103,8932,86,86,66,0,1},{8062,8907,87,87,66,0,1},{2870,3721,92,92,66,0,1},{2844,3664,100,100,66,0,1},{2911,3677,84,84,66,0,1},{3587,7388,85,85,66,0,1},
	        {3599,7361,93,93,66,0,1},{3535,7382,100,100,66,0,1},{3938,1414,102,102,66,0,1},{4082,1414,83,83,66,0,1},{4038,1408,92,92,66,0,1},{4019,1422,94,94,66,0,1},
	        {4030,1412,92,92,66,0,1},{2127,8333,105,105,66,0,1},{2097,8344,93,93,66,0,1},{2126,8335,83,83,66,0,1},{2098,8333,101,101,66,0,1},{11240,9352,85,85,66,0,1},
	        {11254,9437,88,88,66,0,1},{11264,9392,85,85,66,0,1},{940,2436,83,83,66,0,1},{882,2391,95,95,66,0,1},{915,2367,94,94,66,0,1},{963,2376,91,91,66,0,1},
	        {984,2420,88,88,66,0,1},{4773,6274,106,106,66,0,1},{4722,6314,101,101,66,0,1},{4845,6308,84,84,66,0,1},{4795,6207,107,107,66,0,1},{7002,3854,104,104,66,0,1},
	        {7000,3872,98,98,66,0,1},{6990,3858,103,103,66,0,1},{6939,3856,82,82,66,0,1},{6874,3888,107,107,66,0,1},{1198,7102,85,85,66,0,1},{1136,7138,90,90,66,0,1},
	        {1140,7185,90,90,66,0,1},{5870,9010,84,84,66,0,1},{5926,9042,93,93,66,0,1},{5808,8994,107,107,66,0,1},{8231,3665,107,107,66,0,1},{8182,3663,91,91,66,0,1},
	        {8183,3639,106,106,66,0,1},{1398,5467,100,100,66,0,1},{1342,5434,106,106,66,0,1},{1340,5413,93,93,66,0,1},{9644,11383,90,90,66,0,1},{9589,11344,85,85,66,0,1},
	        {9647,11400,101,101,66,0,1},{9692,11342,88,88,66,0,1},{9610,11348,97,97,66,0,1},{6859,1556,96,96,66,0,1},{6727,1589,85,85,66,0,1},{6824,1582,103,103,66,0,1},
	        {6172,4574,99,99,66,0,1},{6126,4589,106,106,66,0,1},{6149,4656,94,94,66,0,1},{6217,4596,101,101,66,0,1},{6152,4608,85,85,66,0,1},{7192,13402,108,108,66,0,1},
	        {7247,13380,106,106,66,0,1},{7196,13315,102,102,66,0,1},{7162,13424,93,93,66,0,1},{3575,5123,83,83,66,0,1},{3542,5078,96,96,66,0,1},{3572,5140,87,87,66,0,1},
	        {3576,5084,106,106,66,0,1},{3534,5082,82,82,66,0,1},{8036,2154,102,102,66,0,1},{8072,2169,104,104,66,0,1},{8030,2158,94,94,66,0,1},{8092,2170,105,105,66,0,1},
	        {8034,2117,85,85,66,0,1},{8709,5719,107,107,66,0,1},{8647,5674,107,107,66,0,1},{8652,5727,102,102,66,0,1},{8748,5766,86,86,66,0,1},{8713,5788,93,93,66,0,1},
	        {9952,3649,98,98,66,0,1},{9939,3702,99,99,66,0,1},{10051,3659,82,82,66,0,1},{3013,514,82,82,66,0,1},{2971,486,91,91,66,0,1},{3015,498,82,82,66,0,1},
	        {3028,490,95,95,66,0,1},{4014,6743,89,89,66,0,1},{3931,6755,90,90,66,0,1},{3912,6785,89,89,66,0,1},{6613,12411,87,87,66,0,1},{6561,12470,102,102,66,0,1},
	        {6585,12419,97,97,66,0,1},{4913,1906,86,86,66,0,1},{5015,1799,93,93,66,0,1},{4923,1855,101,101,66,0,1},{5043,1823,82,82,66,0,1},{3823,9853,107,107,66,0,1},
	        {3811,9785,95,95,66,0,1},{3753,9837,103,103,66,0,1},{9568,6131,103,103,66,0,1},{9520,6169,96,96,66,0,1},{9561,6191,103,103,66,0,1},{9509,6175,92,92,66,0,1},
	        {9523,6126,94,94,66,0,1},{3084,7560,82,82,66,0,1},{3127,7561,105,105,66,0,1},{3085,7553,90,90,66,0,1},{3148,7552,106,106,66,0,1},{9329,7711,101,101,66,0,1},
	        {9372,7710,94,94,66,0,1},{9456,7773,93,93,66,0,1},{9324,7764,94,94,66,0,1},{9311,7719,102,102,66,0,1},{3108,1409,87,87,66,0,1},{3095,1352,89,89,66,0,1},
	        {3193,1366,99,99,66,0,1},{3181,1358,107,107,66,0,1},{3127,1419,108,108,66,0,1},{3013,8416,93,93,66,0,1},{2987,8399,85,85,66,0,1},{2996,8435,96,96,66,0,1},
	        {3023,8411,93,93,66,0,1},{5451,11493,90,90,66,0,1},{5330,11487,91,91,66,0,1},{5465,11531,98,98,66,0,1},{5396,11465,94,94,66,0,1},{5458,11470,83,83,66,0,1},
	        {10793,491,99,99,66,0,1},{10716,527,108,108,66,0,1},{10662,555,89,89,66,0,1},{10785,565,85,85,66,0,1},{10740,545,105,105,66,0,1},{1947,1891,82,82,66,0,1},
	        {1956,1851,86,86,66,0,1},{1938,1894,96,96,66,0,1},{2017,1842,94,94,66,0,1},{2006,1890,89,89,66,0,1},{7509,12341,82,82,66,0,1},{7473,12326,103,103,66,0,1},
	        {7555,12344,88,88,66,0,1},{7505,12303,100,100,66,0,1},{7550,12370,92,92,66,0,1},{3895,439,89,89,66,0,1},{3821,438,89,89,66,0,1},{3880,480,83,83,66,0,1},
	        {3833,451,93,93,66,0,1},{7879,9726,87,87,66,0,1},{7889,9745,93,93,66,0,1},{7937,9798,107,107,66,0,1},{7912,9800,101,101,66,0,1},{5983,5769,84,84,66,0,1},
	        {6017,5800,104,104,66,0,1},{6075,5770,94,94,66,0,1},{9116,4813,101,101,66,0,1},{9118,4805,89,89,66,0,1},{9037,4835,89,89,66,0,1},{9089,4728,90,90,66,0,1},
	        {4317,3028,96,96,66,0,1},{4219,2988,94,94,66,0,1},{4315,2981,103,103,66,0,1},{4257,2958,106,106,66,0,1},{7311,2990,108,108,66,0,1},{7341,2934,99,99,66,0,1},
	        {7369,2913,100,100,66,0,1},{4724,492,94,94,66,0,1},{4659,554,105,105,66,0,1},{4723,525,97,97,66,0,1},{4745,516,102,102,66,0,1},{4720,500,91,91,66,0,1},
	        {6006,501,91,91,66,0,1},{6092,469,102,102,66,0,1},{6026,441,93,93,66,0,1},{5961,511,90,90,66,0,1},{6010,476,82,82,66,0,1},

	        // ── BARREL ──
	        //{550,2280,30,40,64,0,     0},
	        //{470,1300,30,40,64,0,     0},
	    };

	    worldObjects = adaptWorldObjectsToIce(worldObjects);

	    // ── COLLISION AIR OTOMATIS (dari tile peta, lihat WaterCollision.java) ──
	    // Dek jembatan (objek flat tile 25/26/27 = vertikal, 73/74/75 = horizontal) boleh diinjak walau tile di
	    // bawahnya air. Dek-nya ngikut posisi objek jembatan di worldObjects, jadi kalau jembatan dipindah ya ikut.
	    java.util.List<Rectangle> deckJembatan = new java.util.ArrayList<>();
	    for (int[] o : worldObjects) {
	        if (o[5] != 1) continue;
	        if (o[4] >= 25 && o[4] <= 27)      deckJembatan.add(new Rectangle(o[0] + 8, o[1], o[2] - 16, o[3]));
	        else if (o[4] >= 73 && o[4] <= 75) deckJembatan.add(new Rectangle(o[0], o[1] + 8, o[2], o[3] - 16));
	    }
	    bridgeDecks.clear();
	    bridgeDecks.addAll(deckJembatan);   // dipakai FishingManager: boleh mancing dari atas jembatan
	    for (Rectangle r : WaterCollision.build(mapTileNum, loadedCols, loadedRows, gp.tileSize, deckJembatan)) {
	        gp.objCol.add(r.x, r.y, r.width, r.height);
	    }
	}

	/**
	 * Semak (tile 66) yang tumbuh di dalam area salju dibuang (diganti sebaran generator), dan yang di pinggiran salju
	 * diganti semak bersalju (tile 69). Objek lain tidak disentuh.
	 */
	private int[][] adaptWorldObjectsToIce(int[][] src) {
		List<int[]> out = new ArrayList<>(src.length);
		for (int[] o : src) {
			if (o[4] == 66 && o[5] == 0) {
				float ice = gen.ice((o[0] + o[2] / 2f) / gp.tileSize, (o[1] + o[3]) / (float) gp.tileSize);
				if (ice > WorldGen.OWN_ICE) continue;
				if (ice > WorldGen.FROST_ICE) { o = o.clone(); o[4] = 69; }
			}
			out.add(o);
		}
		return out.toArray(new int[0][]);
	}

	/** Pohon / batu lama yang ada di dalam area salju dibuang (diganti sebaran generator: cemara, pohon mati, batu es, kristal). */
	private int[][] keepOutsideIce(int[][] data) {
		List<int[]> kept = new ArrayList<>(data.length);
		for (int[] d : data) {
			float ice = gen.ice((d[0] + d[2] / 2f) / gp.tileSize, (d[1] + d[3]) / (float) gp.tileSize);
			if (ice > WorldGen.OWN_ICE) continue;
			kept.add(d);
		}
		return kept.toArray(new int[0][]);
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