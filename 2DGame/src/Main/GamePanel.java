package Main;

import java.awt.*;
import java.awt.GradientPaint;
import java.awt.geom.AffineTransform;

import Item.DeathTotemManager;
import Item.FishingManager;
import Item.DreadGuardian;
import Item.ArmorSetEffects;
import Item.DropItem;
import java.util.ArrayList;

import java.util.List;
import java.util.ArrayList;

import World.DomainManager;

import Item.Item;
import Item.ItemManager;
import Item.ItemStack;
import Entitas.monster;
import World.WorldSystem;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import World.HouseManager;
import World.RainDrop;
import World.WorldSystem;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import Entitas.player;
import Object.Barang;
import SFX.MusicManager;
import UI.BackpackUI;
import UI.MobIndexUI;
import UI.PlayerStatUI;
import UI.UICommandButton;
import bahan.KeyHandlerObj;
import bahan.TileM;
import Entitas.ArmorMerchant;
import Entitas.DarkMerchant;
import Entitas.DialogNode;
import Entitas.Entity;
import Entitas.MonsterBatu;
import Entitas.NPC;
import Entitas.PotionMerchant;
import Main.AsetSetter;
import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import javax.swing.event.AncestorListener;

public class GamePanel extends JPanel implements Runnable{
	public boolean showDebug = true; // koordinat player (toggle: F3)
	public boolean dialogOn = false;
	public String dialogText = "";
	public Entity currentNPC = null;
	public Map<String, DialogNode> dialogMap = new HashMap<>();
	public boolean chatMode = false;
	public String chatInput = "";
	public int chatTimer = 0; // durasi tampil
	
	public DeathTotemManager deathTotemManager;
	public DreadGuardian dreadGuardian; // set effect Dread Armor: bayangan penyerang
	public ArmorSetEffects armorEffects; // set effect Iron Armor (Iron Will) & Umbral Armor (Ember Verdict)
	public DamagePopup damagePopups;    // angka damage melayang (musuh kuning, player merah, bayangan ungu)
	public Entitas.PlayerSpeechBubble speechBubble;
	
	// grafik warna
	// Sunset / golden hour atmosphere
	public boolean sunsetMode = true;
	public float sunsetIntensity = 0.2f; // 0.0 = off, 0.3 = kuat banget
	public UI.DeathScreenUI deathScreenUI;
	
	// drop item
	public ArrayList<DropItem> drops = new ArrayList<>();
	
	// npc
	public PotionMerchant potionMerchant;
	public ArmorMerchant armorMerchant;

	// toko potion + ikon buff aktif
	public UI.PotionShopUI potionShop;
	public UI.ArmorShopUI armorShop;
	public World.StorageChest chest;   // peti penyimpanan di (800, 1110)
	public UI.ChestUI chestUI;
	public UI.BuffBar buffBar;
	
	public HouseManager houseManager;
	
	public DomainManager domainManager;
	
	// music
	public MusicManager music = new MusicManager();
	private MusicState lastMusicState = null; // untuk detect perubahan state BGM

	// ===== BGM CONFIG =====
	// NEW: state musik terpisah dari GameState, biar bisa bedain DOMAIN biasa vs BOSS
	private enum MusicState { MENU, OUTSIDE, INSIDE, DOMAIN, BOSS }

	// NEW: ganti nama file ini sesuai file audio kamu (taro di folder /SFX/)
	private static final String MUSIC_MENU    = "Amber Road Glee.wav";
	private static final String MUSIC_OUTSIDE = "Amber Road Glee.wav";
	private static final String MUSIC_INSIDE  = "Amber Road Glee.wav";
	private static final String MUSIC_DOMAIN  = "Umbrae Surgunt.wav";
	
	//backpack
	public BackpackUI backpack;

	// hotbar: 5 slot akses cepat di bawah layar (tombol angka 1-5)
	public UI.Hotbar hotbar;

	// mancing: pegang Fishing Rod dari hotbar, lalu KLIK air (butuh Fishing Rod dari Armor Merchant)
	public FishingManager fishing;
	public int mouseX, mouseY;   // posisi mouse terakhir di layar (diupdate tiap gerak / drag), dipakai bidikan mancing

	// minimap di pojok kanan atas (cuma tampil di dunia luar)
	public UI.MiniMap miniMap;

	// Atmosper
	public boolean darkMode = true;
	public int darknessAlpha = 80; // 0–255 (semakin gede = makin gelap)
	
	public UI.MainMenu mainMenu;
	public PlayerStatUI statUI;
	
	public WorldSystem world;

	// ===================== ZOOM =====================
	// Scroll mouse / tombol  +  -  buat zoom, tombol 0 buat reset ke 100%.
	// Zoom berlaku di mana-mana: di luar, di dalam rumah, dan di dalam domain/dungeon.
	public static final float ZOOM_MIN  = 0.5f;   // 50%  (zoom out maksimal)
	public static final float ZOOM_MAX  = 2.0f;   // 200% (zoom in maksimal)
	public static final float ZOOM_STEP = 0.1f;   // tiap 1 notch scroll / 1 tekan tombol
	public float zoom = 1.0f;        // zoom yang dipakai buat render (bergerak halus)
	public float targetZoom = 1.0f;  // zoom tujuan (diubah scroll / tombol)
	private int zoomHintTimer = 0;   // tampilan "Zoom: 150%" sebentar di layar

	final int originalTileSize = 16;
	public final int scale = 3;
	public final int tileSize = originalTileSize * scale;
	public final int maxScreenColumn = 16;
	public final int maxScreenRow = 12;
	public final int screenWidth = tileSize * maxScreenColumn;
	public final int screenHeight = tileSize * maxScreenRow;
	// interior
	public final int maxWorldCol = 700;
	public final int maxWorldRow = 700;
	public final int worldWidth = tileSize * maxWorldCol;
	public final int worldHeight = tileSize * maxWorldRow;

	public boolean inspectOn = false;
	public Entity inspectedNPC = null;
	
	public boolean debugCollision = false; // toggle: F4
	RainDrop[] rain;
	
	public final int FPS = 40;
	public WorldObject objCol = new WorldObject();
	public KeyHandler KeyH = new KeyHandler();

	TerrainMusic.land land = new TerrainMusic.land();
	private static final String MUSIC_GOLEM_HIDE = "golemHideSound.wav";
	Thread gameThread;
	public Collision colision = new Collision(this);

	public AsetSetter AS = new AsetSetter(this);
	public player player = new player(this,KeyH);
	

	public Barang obj[]= new Barang[10];
	public Entity npc [] = new Entity[10];
	public Entity monsters [] = new Entity[50];
	
	//UI Field Death totem aka sung jin woo
	public UI.TotemUI totemUI;
	public TileM tileM = new TileM(this);
	public bahan.SnowAtmosphere snow = new bahan.SnowAtmosphere(this); // hujan salju + kabut + warna dingin di biome es
	
	public MobIndexUI mobIndexUI = new MobIndexUI(this);
	
	
	UICommandButton btnStay;
	UICommandButton btnFollow;
	UICommandButton btnCancel;
	
	public BufferedImage setup(String imageName) {
	    BufferedImage image = null;
	    try {
	        image = ImageIO.read(
	            getClass().getResourceAsStream("/Aset/" + imageName + ".png")
	        );
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return image;
	}

	int pX = 100;
	int pY = 100;
	int pSpeed = 2;
	
	public GamePanel () throws IOException {
		
		ItemManager.get(); // PAKSA LOAD ITEM
		this.setFocusable(true);
		this.setPreferredSize(new Dimension(screenWidth, screenHeight));
		this.setDoubleBuffered(true);
		this.addKeyListener(KeyH);
		this.setFocusable(true);
		this.setFocusTraversalKeysEnabled(false);
		this.player = new player(this, KeyH); // Player lahir
		this.deathScreenUI = new UI.DeathScreenUI(this, player);
	    this.statUI = new PlayerStatUI(this, player);
	    this.deathTotemManager = new DeathTotemManager(this);
	    this.dreadGuardian = new DreadGuardian(this);
	    this.armorEffects = new ArmorSetEffects(this);
	    this.damagePopups = new DamagePopup(this);
		world = new WorldSystem();
		world.hour = 6; // sore ke malam
		mainMenu = new UI.MainMenu(this); 
		houseManager = new HouseManager(this);
		mainMenu.active = true;
		domainManager = new DomainManager(this);
		speechBubble = new Entitas.PlayerSpeechBubble(this);
		totemUI = new UI.TotemUI(this);
		SaveManager.init();
		

		
		backpack = new BackpackUI(ItemManager.inventory);
		hotbar = new UI.Hotbar(this);
		fishing = new FishingManager(this);
		miniMap = new UI.MiniMap(this);
		potionShop = new UI.PotionShopUI(this);
		armorShop = new UI.ArmorShopUI(this);
		chest = new World.StorageChest(this);
		chestUI = new UI.ChestUI(this, chest);
		buffBar = new UI.BuffBar(this);

		this.addMouseWheelListener(backpack);
		this.addMouseWheelListener(e -> {
		    if (chestUI != null && chestUI.visible) chestUI.handleMouseWheel(e.getWheelRotation());
		});

		// ZOOM pakai scroll mouse (scroll naik = zoom in, scroll turun = zoom out).
		// Cuma jalan kalau gak ada UI yang kebuka, jadi gak bentrok sama scroll backpack/shop.
		this.addMouseWheelListener(e -> {
		    if (canZoom()) {
		        zoomBy(-e.getWheelRotation() * ZOOM_STEP);
		    }
		});

		this.addMouseWheelListener(e -> {
		    if (domainManager != null) {
		        domainManager.handleMerchantMouseWheel(
		            e.getWheelRotation()
		        );
		    }
		});
		
		
		rain = new RainDrop[100];
		for (int i = 0; i < rain.length; i++) {
		    rain[i] = new RainDrop(screenWidth, screenHeight);
		}

		// ===== SPAWN MONSTER =====
		spawnSlime(0, 1000, 2300);

		
        spawnOrc(5,  1400, 2500);
        // Semua Golem & Golemite sekarang tinggal di domain GolemHide (lihat DomainManager.GOLEM_SPAWNS / GOLEMITE_SPAWNS)

        // 30 slime acak di area (700,2500)-(5000,5000), di luar collision.
        // Slime pakai index 15..44 (array monsters ukurannya 50).
        spawnSlimesRandom(15, 30, 700, 2500, 5000, 5000);

        // 3 serigala (slot 45-47, slime pakai 15..44). Mereka cuma ada pas malam; posisinya dicari sendiri
        // di dekat player tiap malam tiba, jadi gak perlu koordinat di sini.
        spawnWolves(45, 3);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
 
                // ── DEATH SCREEN ──
                // Harus paling atas supaya klik Respawn benar-benar diterima
                if (deathScreenUI != null && deathScreenUI.visible) {
                    deathScreenUI.handleClick(e);
                    return;
                }
 
                // ── DEATH TOTEM UI ── (SEBELUM UI lain)
                if (totemUI != null && totemUI.visible) {
                    totemUI.handleMouseClick(e);
                    return;
                }
 
                // ── PETI PENYIMPANAN ──
                if (chestUI != null && chestUI.visible) {
                    chestUI.handleMouseClick(e);
                    return;
                }

                // ── POTION MERCHANT SHOP ──
                if (potionShop != null && potionShop.visible) {
                    potionShop.handleMouseClick(e);
                    return;
                }

                // ── ARMOR MERCHANT SHOP ──
                if (armorShop != null && armorShop.visible) {
                    armorShop.handleMouseClick(e);
                    return;
                }

                // ── DARK MERCHANT SHOP ──
                if (domainManager != null
                        && domainManager.darkMerchant != null
                        && domainManager.darkMerchant.isShopOpen()) {

                    domainManager.handleMerchantMouseClick(e);
                    return;
                }

                // ── HOTBAR ── (klik slot = pakai item, klik kanan = kosongin; juga biar gak nonjok)
                if (hotbar != null && hotbar.handleMousePressed(e)) {
                    return;
                }
 
                // ── PLAYER STAT UI ──
                if (statUI != null && statUI.visible) {
                    statUI.handleClick(e);
                    return;
                }
 
                // ── MOB INDEX UI ──
                if (mobIndexUI != null && mobIndexUI.visible) {
                    mobIndexUI.handleMouseClick(e, GamePanel.this);
                    return;
                }
 
                // ── BACKPACK ──
                if (backpack != null && backpack.visible) {
                    backpack.handleMouseClick(e, GamePanel.this);
                    return;
                }
 
                // ── MANCING ── (pancingan lagi dipegang: klik = lempar / tarik, bukan nonjok)
                if (fishing != null && fishing.handleClick(e)) {
                    return;
                }

                // ── PUNCH ──
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (player.isNotPunching()) {
                        player.startPunch();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // lepas klik: selesaiin drag & drop ke hotbar (kalau lagi ada)
                // drag & drop antar kotak backpack: harus SEBELUM hotbar, soalnya hotbar mereset status drag
                if (backpack != null) backpack.handleMouseReleased(e, GamePanel.this);
                if (hotbar != null) hotbar.handleMouseReleased(e);
            }
        });

		// NEW: Mouse hover — SEBELUMNYA GAK PERNAH DI-REGISTER, makanya hover buat
		// detail item BackpackUI, hover TotemUI, dan hover tombol inspect NPC gak pernah jalan.
		addMouseMotionListener(new MouseAdapter() {
		    @Override
		    public void mouseMoved(MouseEvent e) {
		        GamePanel.this.mouseX = e.getX();
		        GamePanel.this.mouseY = e.getY();
		        GamePanel.this.mouseMoved(e);
		    }

		    @Override
		    public void mouseDragged(MouseEvent e) {
		        GamePanel.this.mouseX = e.getX();
		        GamePanel.this.mouseY = e.getY();
		        // seret item dari backpack / antar slot hotbar
		        if (hotbar != null) hotbar.handleMouseDragged(e);
		    }
		});

		
		KeyH.gp  = this;
	}

	
	// Serigala: cuma muncul pas malam (jam 18:00-05:59). Lihat Entitas/Wolf.java.
	private void spawnWolves(int startIndex, int count) {
	    for (int i = 0; i < count; i++) {
	        monsters[startIndex + i] = new Entitas.Wolf(this);
	    }
	}

	
	
	public enum GameState {
	    OUTSIDE,
	    INSIDE,
	    DIALOG,
	    DOMAIN
	    }

	public GameState gameState = GameState.OUTSIDE;

	public void setupGame() {
	    AS.setObject();

	    setupDialogs();

	    AS.setNPC();

	    potionMerchant = new PotionMerchant(this);
	    potionMerchant.initDialog();
	    npc[3] = potionMerchant;

	    armorMerchant = new ArmorMerchant(this);
	    armorMerchant.initDialog();
	    npc[4] = armorMerchant;

	    mainMenu.active = true;                        // matiin main menu
	   // domainManager.forceEnterDomain("Tomb");         // langsung masuk domain
	}
	
	// ===================== ZOOM: logic =====================

	/** Zoom boleh dipakai cuma pas lagi main di luar & gak ada UI/dialog yang kebuka. */
	public boolean canZoom() {
	    if (mainMenu != null && mainMenu.active) return false;
	    if (isSkillVActive()) return false; // zoom lagi dikendalikan skill
	    if (chatMode || dialogOn || inspectOn) return false;
	    if (backpack != null && backpack.visible) return false;
	    if (statUI != null && statUI.visible) return false;
	    if (mobIndexUI != null && mobIndexUI.visible) return false;
	    if (totemUI != null && totemUI.visible) return false;
	    if (potionShop != null && potionShop.visible) return false;
	    if (armorShop != null && armorShop.visible) return false;
	    if (chestUI != null && chestUI.visible) return false;
	    if (deathScreenUI != null && deathScreenUI.visible) return false;
	    // di dalam domain: jangan zoom kalau lagi buka shop / dialog merchant / konfirmasi boss
	    if (gameState == GameState.DOMAIN && domainManager != null) {
	        DarkMerchant dm = domainManager.darkMerchant;
	        if (dm != null && (dm.isShopOpen() || dm.isInDialog())) return false;
	        if (domainManager.isBossConfirmOpen()) return false;
	    }
	    return true;
	}

	/** Ubah zoom tujuan (delta positif = zoom in, negatif = zoom out). */
	public void zoomBy(float delta) {
	    setTargetZoom(targetZoom + delta);
	}

	public void setTargetZoom(float z) {
	    z = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, z));
	    targetZoom = Math.round(z * 100f) / 100f;   // rapihin biar gak ada 1.0999999
	    zoomHintTimer = 60;                          // ~1.5 detik (FPS 40)
	}

	public void resetZoom() {
	    setTargetZoom(1.0f);
	}

	/** Sama kayak setTargetZoom, tapi tanpa munculin hint "Zoom: x%" (dipakai skill sinematik, mis. SkillV). */
	public void setTargetZoomSilent(float z) {
	    setTargetZoom(z);
	    zoomHintTimer = 0;
	}

	/** SkillV lagi jalan (input & interaksi dikunci). */
	public boolean isSkillVActive() {
	    return player != null && player.skillManager != null && player.skillManager.isSkillVActive();
	}

	/** SkillV lagi nge-freeze dunia: monster, NPC, dan minion berhenti update. */
	public boolean isWorldFrozen() {
	    return player != null && player.skillManager != null && player.skillManager.isWorldFrozen();
	}

	/** Dipanggil tiap frame: zoom jalan pelan-pelan ke targetZoom (smooth). */
	private void updateZoom() {
	    float diff = targetZoom - zoom;
	    if (Math.abs(diff) < 0.002f) zoom = targetZoom;
	    else zoom += diff * 0.25f;
	    if (zoomHintTimer > 0) zoomHintTimer--;
	}

	/** Zoom yang beneran dipakai render (sama di luar, rumah, dan domain). */
	public float renderZoom() {
	    return zoom;
	}

	// Batas area yang kelihatan (dalam koordinat layar SEBELUM di-zoom).
	// Kalau zoom out (<1), area yang kelihatan lebih luas dari layar asli,
	// jadi pengecekan "di luar layar → skip gambar" harus pakai batas ini,
	// kalau nggak, objek di pinggir bakal ilang pas zoom out.
	// Pas zoom in (>=1) hasilnya sama persis kayak sebelumnya (0 .. screenWidth).
	public int viewMinX() {
	    return Math.min(0, (int) Math.floor(screenWidth / 2.0 - screenWidth / (2.0 * renderZoom())));
	}
	public int viewMaxX() {
	    return Math.max(screenWidth, (int) Math.ceil(screenWidth / 2.0 + screenWidth / (2.0 * renderZoom())));
	}
	public int viewMinY() {
	    return Math.min(0, (int) Math.floor(screenHeight / 2.0 - screenHeight / (2.0 * renderZoom())));
	}
	public int viewMaxY() {
	    return Math.max(screenHeight, (int) Math.ceil(screenHeight / 2.0 + screenHeight / (2.0 * renderZoom())));
	}

	/**
	 * Pasang transform zoom (pivot = tengah layar = posisi player).
	 * Balikin transform lama; setelah selesai gambar dunia, panggil g2.setTransform(hasilnya)
	 * supaya UI digambar di layar normal (gak ikut ke-zoom).
	 */
	public AffineTransform applyWorldZoom(Graphics2D g2) {
	    AffineTransform previous = g2.getTransform();
	    float z = renderZoom();
	    if (z != 1.0f) {
	        double cx = screenWidth / 2.0;
	        double cy = screenHeight / 2.0;
	        g2.translate(cx, cy);
	        g2.scale(z, z);
	        g2.translate(-cx, -cy);
	    }
	    return previous;
	}

	/** Indikator "Zoom: 150%" di sisi kiri (di bawah HUD kiri), muncul sebentar tiap zoom diubah. */
	private void drawZoomHint(Graphics2D g2) {
	    if (zoomHintTimer <= 0) return;
	    String txt = "Zoom: " + Math.round(targetZoom * 100f) + "%";
	    g2.setFont(new Font("Arial", Font.BOLD, 13));
	    int w = g2.getFontMetrics().stringWidth(txt);
	    int x = LEFT_HUD_X;
	    int y = leftHudBottom + 8;
	    g2.setColor(new Color(0, 0, 0, 150));
	    g2.fillRoundRect(x - 8, y - 15, w + 16, 22, 10, 10);
	    g2.setColor(Color.WHITE);
	    g2.drawString(txt, x, y);
	}

	public void drawSunsetOverlay(Graphics2D g2) {
	    if (!sunsetMode) return;
	    // di biome es cahaya senja oranye diredam (suasananya biru-putih, bukan oranye)
	    float si = sunsetIntensity * (1f - 0.8f * snow.intensity());

	    // Base warm amber overlay
	    Composite old = g2.getComposite();
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, si));
	    g2.setColor(new Color(180, 90, 10)); // amber-oranye
	    g2.fillRect(0, 0, screenWidth, screenHeight);

	    // Top sky glow (lebih oranye di atas)
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, si * 0.6f));
	    GradientPaint skyGlow = new GradientPaint(
	        0, 0, new Color(210, 100, 20, 200),
	        0, screenHeight / 2, new Color(160, 70, 0, 0)
	    );
	    g2.setPaint(skyGlow);
	    g2.fillRect(0, 0, screenWidth, screenHeight / 2);

	    // Bottom shadow (bawah lebih gelap coklat)
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, si * 0.4f));
	    GradientPaint groundShadow = new GradientPaint(
	        0, screenHeight * 0.6f, new Color(80, 30, 0, 0),
	        0, screenHeight, new Color(60, 20, 0, 180)
	    );
	    g2.setPaint(groundShadow);
	    g2.fillRect(0, 0, screenWidth, screenHeight);

	    g2.setComposite(old);
	}
	
	
	
	private void spawnSlime(int index, int worldX, int worldY) {
	    Entitas.monster slime = new Entitas.monster(this);

	    slime.x = worldX;
	    slime.y = worldY;

	    slime.spawnX = worldX;
	    slime.spawnY = worldY;

	    // 🔥 PERBAIKI & BESARIN AREA
	    int range = 400; // sebelumnya 200 → sekarang lebih luas

	    slime.wanderArea = new Rectangle(
	        worldX - range,
	        worldY - range,
	        range * 2,
	        range * 2
	    );

	    monsters[index] = slime;
	}
	
	// Spawn 'count' slime di posisi acak dalam area (minX,minY)-(maxX,maxY), mulai dari monsters[startIndex].
	// Posisi ditolak kalau hitbox slime (+ jarak aman) nabrak collision dunia (objCol) atau monster lain.
	private void spawnSlimesRandom(int startIndex, int count, int minX, int minY, int maxX, int maxY) {
	    java.util.Random rnd = new java.util.Random();
	    Entitas.monster probe = new Entitas.monster(this); // cuma buat ambil ukuran hitbox slime
	    int margin = tileSize / 2; // jarak aman dari collision & monster lain

	    int spawned = 0;
	    int attempts = 0;
	    while (spawned < count && attempts < count * 200) {
	        attempts++;

	        int x = minX + rnd.nextInt(maxX - minX + 1);
	        int y = minY + rnd.nextInt(maxY - minY + 1);

	        Rectangle box = new Rectangle(
	            x + probe.solidArea.x - margin,
	            y + probe.solidArea.y - margin,
	            probe.solidArea.width + margin * 2,
	            probe.solidArea.height + margin * 2
	        );

	        boolean blocked = false;

	        for (Rectangle r : objCol.objects) {
	            if (box.intersects(r)) { blocked = true; break; }
	        }

	        if (!blocked) {
	            for (Entity e : monsters) {
	                if (e == null || e.solidArea == null) continue;
	                Rectangle other = new Rectangle(
	                    e.x + e.solidArea.x,
	                    e.y + e.solidArea.y,
	                    e.solidArea.width,
	                    e.solidArea.height
	                );
	                if (box.intersects(other)) { blocked = true; break; }
	            }
	        }

	        if (blocked) continue;

	        spawnSlime(startIndex + spawned, x, y);
	        spawned++;
	    }
	}

	 private void spawnOrc(int index, int worldX, int worldY) {
	        Entitas.Orc orc = new Entitas.Orc(this);
	 
	        orc.x = worldX;
	        orc.y = worldY;
	 
	        int range = 350;
	        orc.wanderArea = new java.awt.Rectangle(
	            worldX - range,
	            worldY - range,
	            range * 2,
	            range * 2
	        );
	 
	        monsters[index] = orc;
	    }
	
	// ===== HUD KIRI =====
	// Semua teks info ditaruh di kiri atas supaya sisi kanan bebas buat minimap.
	// Urutan (atas -> bawah): koordinat player (F3), jam, cuaca, lalu indikator zoom.
	private static final int LEFT_HUD_X = 20;
	private int leftHudBottom = 30; // batas bawah HUD kiri frame ini (dipakai drawZoomHint)

	/**
	 * @param showClock true = ikut gambar jam & cuaca (dunia luar & dalam rumah)
	 */
	public void drawLeftHud(Graphics2D g2, boolean showClock) {
	    int x = LEFT_HUD_X;
	    int y = 30;

	    if (showDebug) {
	        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
	        g2.setColor(Color.WHITE);
	        g2.drawString("PLAYER WORLD COORD", x, y);
	        y += 18;
	        g2.drawString("X : " + player.x, x, y);
	        y += 18;
	        g2.drawString("Y : " + player.y, x, y);
	        y += 24; // jarak ke blok jam
	    }

	    if (showClock) {
	        g2.setFont(new Font("Arial", Font.BOLD, 13));
	        g2.setColor(Color.RED);
	        g2.drawString(world.getTimeString(), x, y);
	        y += 18;
	        g2.drawString("Weather: " + (world.isRaining ? "RAIN" : "CLEAR"), x, y);
	        y += 18;
	    }

	    leftHudBottom = y;
	}

	/** Kompatibilitas: koordinat debug saja (tanpa jam/cuaca). */
	public void drawDebugInfo(Graphics2D g2) {
	    drawLeftHud(g2, false);
	}
	
	
	public void initInspectButtons() {

	    int w = 180;
	    int h = 35;
	    int x = screenWidth / 2 - w / 2;
	    int y = screenHeight / 2 - 50;

	    btnStay   = new UICommandButton(x, y, w, h, "Stay here");
	    btnFollow = new UICommandButton(x, y + 45, w, h, "Follow me");
	    btnCancel = new UICommandButton(x, y + 90, w, h, "Cancel follow");
	}

	
	public void startThread () {
		this.requestFocusInWindow();
		gameThread = new Thread(this);
		gameThread.start();
	}

	// Dipakai untuk mencegah log error yang sama tercetak 40x per detik
	private String lastErrorKey = null;

	@Override
	public void run() {
		double drawInterval = 1000000000.0 / FPS;
		double delta = 0;
		long lastTime = System.nanoTime();

		while (gameThread != null) {
			long currentTime = System.nanoTime();
			delta += (currentTime - lastTime) / drawInterval;
			lastTime = currentTime;

			if (delta >= 1) {
				try {
					// update() dijalankan di thread UI (EDT) supaya tidak balapan dengan
					// paintComponent() dan event keyboard/mouse yang juga jalan di EDT.
					SwingUtilities.invokeAndWait(this::update);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (InvocationTargetException e) {
					Throwable t = e.getCause() != null ? e.getCause() : e;
					String key = t.getClass().getName() + ":" + t.getMessage();
					if (!key.equals(lastErrorKey)) {
						lastErrorKey = key;
						t.printStackTrace();
					}
				}
				repaint();
				delta--;
				// kalau sempat ketinggalan jauh (mis. layar freeze), jangan dikejar sekaligus
				if (delta > 5) delta = 0;
			} else {
				// tidur sebentar supaya tidak makan 100% satu core CPU
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	/** Nyala/matikan tampilan koordinat player (tombol F3). */
	public void toggleDebug() {
		showDebug = !showDebug;
	}

	/** Nyala/matikan kotak collision + log di konsol (tombol F4). */
	public void toggleCollisionDebug() {
		boolean on = !debugCollision;
		debugCollision = on;
		Log.enabled = on;
		if (domainManager != null) domainManager.showCollisionDebug = on;
	}

	public void play() throws LineUnavailableException {
	//	playMusic(0);
		
	}
	
	public void playSE(String fileName) {
	    if (music != null) {
	        music.playSE(fileName); 
	    }
	}
	public void playSE(String fileName, float volume) {
        if (music != null) {
            music.playSE(fileName, volume); 
        }
    }

	// ===== NEW: BGM state machine =====
	// Cek state musik saat ini, kalau beda dari state terakhir, ganti track.
	private String lastTrack = null;

	private void updateMusic() {
	    String track;

	    if (mainMenu.active) {
	        track = MUSIC_MENU;
	    } else if (gameState == GameState.DOMAIN) {
	        if ("GolemHide".equals(domainManager.getActiveDomainId())) {
	            track = MUSIC_GOLEM_HIDE;
	        } else {
	            track = MUSIC_DOMAIN; // Tomb
	        }
	    } else if (gameState == GameState.INSIDE) {
	        track = MUSIC_INSIDE;
	    } else {
	        track = MUSIC_OUTSIDE;
	    }

	    if (!track.equals(lastTrack)) {
	        music.play(track);
	        lastTrack = track;
	    }
	}
	
	public void drawDarkOverlay(Graphics2D g2) {

	    if (!darkMode) return;

	    Composite old = g2.getComposite();

	    g2.setComposite(
	        AlphaComposite.getInstance(
	            AlphaComposite.SRC_OVER,
	            darknessAlpha / 255f
	        )
	    );

	    // malam di biome es: gelapnya condong ke biru tua, bukan hitam polos
	    float sk = snow.intensity();
	    g2.setColor(sk < 0.01f ? Color.BLACK : new Color((int) (6 * sk), (int) (16 * sk), (int) (46 * sk)));
	    g2.fillRect(0, 0, screenWidth, screenHeight);

	    g2.setComposite(old);
	}

	public void drawDialog(Graphics2D g2) {
		 
	    // ← TAMBAHAN: guard null supaya tidak crash
	    if (currentNPC == null || currentNPC.currentDialog == null) {
	        dialogOn = false;
	        return;
	    }
	 
	    int x = 30;
	    int y = screenHeight - 180;
	    int width = screenWidth - 60;
	    int height = 150;
	 
	    g2.setColor(new java.awt.Color(0, 0, 0, 220));
	    g2.fillRoundRect(x, y, width, height, 30, 30);
	 
	    g2.setColor(java.awt.Color.WHITE);
	    g2.setStroke(new java.awt.BasicStroke(8));
	    g2.drawRoundRect(x, y, width, height, 30, 30);
	 
	    g2.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 15));
	    g2.setColor(java.awt.Color.WHITE);
	 
	    int textX = x + 25;
	    int textY = y + 45;
	 
	    drawWrappedText(g2, currentNPC.currentDialog.text, textX, textY, width - 50);
	 
	    g2.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 13));
	    int optionY = y + 100;
	    for (int i = 0; i < currentNPC.currentDialog.options.size(); i++) {
	        g2.drawString(
	            (i + 1) + ". " + currentNPC.currentDialog.options.get(i).text,
	            textX, optionY
	        );
	        optionY += 24;
	    }
	}
	
	
	private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth) {

	    FontMetrics fm = g2.getFontMetrics();
	    String[] words = text.split(" ");
	    String line = "";
	    int lineHeight = fm.getHeight();

	    for (String word : words) {
	        if (fm.stringWidth(line + word) > maxWidth) {
	            g2.drawString(line, x, y);
	            line = word + " ";
	            y += lineHeight;
	        } else {
	            line += word + " ";
	        }}

	    if (!line.isEmpty()) {
	        g2.drawString(line, x, y);
	    }}
	private void resetDialogKeys() {
	    KeyH.onePressed = false;
	    KeyH.twoPressed = false;
	    KeyH.threePressed = false;
	    KeyH.interactPressed = false;
	}

	private void setupDialogs() {

	    // NPC 1 : Penjaga Desa
	    DialogNode guardRoot = new DialogNode("yoo dawg, im just a freaky npc here, youre so lucky to be a player :3");
	    DialogNode guardWho = new DialogNode("Aku penjaga desa ini.");
	    DialogNode guardJob = new DialogNode("Aku jaga supaya desa tetap aman.");
	    


	    dialogMap.put("guard", guardRoot);


	    // NPC 2 : Orang Misterius 
	    DialogNode mystRoot = new DialogNode("Keep humble...");
	    DialogNode mystTruth = new DialogNode("He's our creator...");

	    mystRoot.addOption("who's Lord K?", mystTruth);
	    mystTruth.addOption("OHH...", null);

	    dialogMap.put("mystic", mystRoot);


	    // 
	    DialogNode kidRoot = new DialogNode("This world belong to lord K");


	    dialogMap.put("kid", kidRoot);
	    
	    // potion merchant (dialog-nya diatur di PotionMerchant)
	    PotionMerchant.registerDialog(this);

	    // armor merchant (dialog-nya diatur di ArmorMerchant)
	    ArmorMerchant.registerDialog(this);
	}



	public void update() {
		updateMusic(); // NEW: cek & ganti BGM sesuai state, jalan tiap frame

		if (deathScreenUI != null && player.isDeathAnimDone()) {
		    deathScreenUI.visible = true;
		}
		if (mainMenu.active) { mainMenu.update(); return; }

		updateZoom(); // smooth zoom in/out
		if (gameState == GameState.OUTSIDE) {
		    tileM.chunks.update();                 // muat / buang chunk peta di sekitar player
		    snow.setHeavy(world.isRaining);        // hujan di biome es = badai salju
		    snow.update();
		}
		if (hotbar != null) hotbar.update(); // efek kedip slot hotbar

		// SkillV lagi jalan: dialog / inspect NPC / buka peti / pintu & keluar domain (E, ESC) dikunci biar skill gak ke-pause
		if (isSkillVActive()) {
		    KeyH.interactPressed = false;
		    KeyH.inspectPressed = false;
		    KeyH.escPressed = false;
		}

		// TOKO POTION KEBUKA → dunia berhenti (buff juga ikut berhenti), cuma ESC yang diproses
		if (potionShop != null && potionShop.visible) {
		    potionShop.update();
		    // panel lain gak boleh kebuka bareng toko
		    KeyH.yPressed = false;
		    KeyH.tabPressed = false;
		    KeyH.mPressed = false;
		    return;
		}

		// TOKO ARMOR KEBUKA → sama: dunia berhenti, cuma ESC yang diproses
		if (armorShop != null && armorShop.visible) {
		    armorShop.update();
		    KeyH.yPressed = false;
		    KeyH.tabPressed = false;
		    KeyH.mPressed = false;
		    return;
		}

		// PETI KEBUKA → dunia berhenti, cuma ESC / klik yang diproses
		if (chestUI != null && chestUI.visible) {
		    chest.update();
		    chestUI.update();
		    KeyH.yPressed = false;
		    KeyH.tabPressed = false;
		    KeyH.mPressed = false;
		    return;
		}

		world.update();
		// Di bagian update(), setelah world.update()
		int hour = world.hour;

		float targetSunset;
		if (hour >= 6 && hour <= 19) {
		    targetSunset = 0.2f;
		} else {
		    targetSunset = 0.06f; // malam tetap ada sedikit warm tone, gak ilang total
		}

		// Transisi pelan-pelan, mirip darknessAlpha
		float step = 0.001f; // makin kecil = makin lambat transisinya
		if (sunsetIntensity < targetSunset) {
		    sunsetIntensity = Math.min(sunsetIntensity + step, targetSunset);
		} else if (sunsetIntensity > targetSunset) {
		    sunsetIntensity = Math.max(sunsetIntensity - step, targetSunset);
		}

		sunsetMode = sunsetIntensity > 0.01f;
		
		// UPDATE MONSTER (HANYA SAAT OUTSIDE, dan gak lagi di-freeze SkillV)
		if (gameState == GameState.OUTSIDE && !isWorldFrozen()) {
		    for (int i = 0; i < monsters.length; i++) {
		    	if (monsters[i] != null) {
		    	    if (!monsters[i].isDead
		    	            || monsters[i] instanceof Entitas.Golem
		    	            || monsters[i] instanceof Entitas.Golemite
		    	            || monsters[i] instanceof Entitas.Wolf
		    	            || (monsters[i] instanceof Entitas.monster && ((Entitas.monster) monsters[i]).isDeathAnimPlaying())) {
		    	        monsters[i].update();
		    	    }
		    	}
		    }
		}

 
		int targetAlpha = world.getTargetDarkness();

		if (KeyH.yPressed) {
		    backpack.visible = !backpack.visible;
		    KeyH.yPressed = false;
		}
		// ── ESC: TotemUI prioritas pertama ──
        if (KeyH.escPressed && totemUI != null && totemUI.visible) {
            totemUI.handleEsc();
            KeyH.escPressed = false;
            return; // stop update frame ini
        }
		if (KeyH.tabPressed) { 
		    statUI.visible = !statUI.visible; // Switch true ke false atau sebaliknya
		    KeyH.tabPressed = false; // Reset agar tidak kedap-kedip
		}
		if (KeyH.mPressed) { 
		    mobIndexUI.visible = !mobIndexUI.visible;
		    KeyH.mPressed = false; // Reset agar tidak kedap-kedip
		}

		// TRANSISI HALUS
		if (darknessAlpha < targetAlpha) darknessAlpha++;
		else if (darknessAlpha > targetAlpha) darknessAlpha--;

		darkMode = darknessAlpha > 0;

		// AKTIFKAN DARK MODE SAAT ADA GELAP
		darkMode = darknessAlpha > 0;


		if (world.isRaining) {
		    for (RainDrop r : rain) {
		        r.update();
		    }
		}

		if (KeyH.chatPressed) {
		    chatMode = true;
		    chatInput = "";
		    KeyH.chatPressed = false;
		}

		// CHAT MODE
		if (chatMode) {

		    // BACKSPACE
		    if (KeyH.backspacePressed && chatInput.length() > 0) {
		        chatInput = chatInput.substring(0, chatInput.length() - 1);
		        KeyH.backspacePressed = false;
		    }

		    // ENTER, KIRIM CHAT
		    if (KeyH.enterPressed) {
		        player.chatText = chatInput;
		        player.chatTimer = 120; // 3 detik (FPS 40)

		        chatInput = "";
		        chatMode = false;

		        KeyH.enterPressed = false;
		        return;
		    }

		    // INPUT KARAKTER
		    char c = KeyH.typedChar;
		    if (c >= 32 && c <= 126 && chatInput.length() < 40) {
		        chatInput += c;
		    }
		    KeyH.typedChar = 0;

		    return; // STOP GAME SAAT CHAT
		}
		
		// Di KeyHandler, tambah key F5
		// Di GamePanel.update(), tambah:
		if (KeyH.f5Pressed) {
		    SaveManager.save(this, 1); // save ke slot 1
		    KeyH.f5Pressed = false;
		}

	    //
	    // 1. SAAT DIALOG AKTIF
		if (dialogOn && currentNPC != null) {

		    //ESC SELALU BISA KELUAR
		    if (KeyH.escPressed) {
		        dialogOn = false;

		        currentNPC.isTalking = false;
		        currentNPC.talkCooldown = 80; // 2 detik (FPS 40)
		        currentNPC = null;

		        KeyH.escPressed = false;
		        resetDialogKeys();
		        return;
		    }

		    int choice = -1;
		    if (KeyH.onePressed)   choice = 0;
		    if (KeyH.twoPressed)   choice = 1;
		    if (KeyH.threePressed) choice = 2;

		    //JIKA PLAYER NEKAN ANGKA
		    if (choice != -1) {

		        //OPSI TIDAK ADA → ABAIKAN
		        if (choice >= currentNPC.currentDialog.options.size()) {
		            resetDialogKeys();
		            return;
		        }

		        //OPSI VALID
		        Entitas.DialogOption picked = currentNPC.currentDialog.options.get(choice);
		        DialogNode next = picked.next;

		        // aksi opsional dari opsi ini (mis. buka toko potion)
		        if (picked.action != null) picked.action.run();

		        if (next == null) {
		            dialogOn = false;

		            currentNPC.isTalking = false;
		            currentNPC.talkCooldown = 80; // delay jalan lagi
		            currentNPC = null;
		        } else {
		            currentNPC.currentDialog = next;
		        }

		        resetDialogKeys();
		    }

		    return; // STOP update saat dialog
		}
		//
		// INSPECT NPC (KEY I)
		if (KeyH.inspectPressed && gameState != GameState.OUTSIDE) KeyH.inspectPressed = false; // NPC cuma ada di dunia luar
		if (KeyH.inspectPressed && !dialogOn) {

		    Rectangle playerArea = player.getInteractArea();

		    for (int i = 0; i < npc.length; i++) {
		        if (npc[i] != null) {

		            if (playerArea.intersects(npc[i].getInteractArea())) {

		                inspectOn = true;
		                inspectedNPC = npc[i];

		                initInspectButtons();

		                KeyH.inspectPressed = false;
		                break;
		            }
		        }
		    }
		
		}
		if (inspectOn && inspectedNPC != null) {

		    // 1. STAY
		    if (KeyH.onePressed) {
		        inspectedNPC.npcState = Entity.NPCState.STAY;
		        inspectedNPC.followTarget = null;
		        inspectOn = false;
		    }

		    // 2. FOLLOW
		    if (KeyH.twoPressed) {
		        inspectedNPC.npcState = Entity.NPCState.FOLLOW;
		        inspectedNPC.followTarget = player;

		        // INIT TRACKING
		        inspectedNPC.lastTargetX = player.x;
		        inspectedNPC.lastTargetY = player.y;
		        inspectedNPC.followIdleTimer = 0;
		        inspectedNPC.alreadyComplained = false;


		        inspectOn = false;
		    }


		    // 3. CANCEL FOLLOW (BALIK KE IDLE)
		    if (KeyH.threePressed) {
		        inspectedNPC.npcState = Entity.NPCState.IDLE;
		        inspectedNPC.followTarget = null;
		        inspectOn = false;
		    }

		    // ESC → TUTUP PANEL
		    if (KeyH.escPressed) {
		        inspectOn = false;
		    }

		    resetDialogKeys();
		    return;
		}
	    // 2. UPDATE NORMAL
	    if (fishing != null) fishing.update(); // timer gigitan ikan (berhenti sendiri pas dialog / toko, karena return di atas)
	    player.update();

	    if (!isWorldFrozen()) { // SkillV nge-freeze NPC juga
	        for (int i = 0; i < npc.length; i++) {
	            if (npc[i] != null) {
	                npc[i].update();
	            }
	        }
	    }

	    //
	    // 2b. PETI PENYIMPANAN: animasi + interaksi [E]
	    //
	    if (gameState == GameState.OUTSIDE && chest != null) {
		    chest.update();
		    if (chest.consumeOpenedEvent()) chestUI.open(); // animasi buka selesai -> tampilkan panel
		    if (KeyH.interactPressed && !player.isDead
		            && !backpack.visible && !statUI.visible && !mobIndexUI.visible
		            && chest.tryOpen(player)) {
		        KeyH.interactPressed = false;
		    }
	    }

	    //
	    // 3. INTERAKSI NPC
	    //
	    if (KeyH.interactPressed && gameState == GameState.OUTSIDE) { // NPC cuma ada di dunia luar

	        Rectangle playerArea = player.getInteractArea();

	        for (int i = 0; i < npc.length; i++) {
	            if (npc[i] != null) {

	                Rectangle npcArea = npc[i].getInteractArea();

	                if (playerArea.intersects(npcArea)) {

	                    dialogOn = true;

	                    // beda dialog per NPC (opsional)
	                    if (i == 0) {
	                        dialogText = "yoo dawg, im just a freaky npc here, youre so lucky to be a player :3";
	                    } else if (i == 1) {
	                        dialogText = "hey Kid, keep humble...";
	                    }
	                    else if (i == 2) {
	                    	dialogText = "this world belong to Lord K";
	                    }

	                    currentNPC = npc[i];

	                    //INI YANG BIKIN NOLEH
	                    currentNPC.facePlayer(player);
	                    currentNPC.currentDialog = currentNPC.dialogRoot;
	                    currentNPC.dialogRoot = dialogMap.get(currentNPC.dialogId);
	                    currentNPC.currentDialog = currentNPC.dialogRoot;
	                    
	                    currentNPC.isTalking = true;
	                    currentNPC.imgNum = 3; // STAND
	                    currentNPC.animIndex = 0;
	                    currentNPC.imgCount = 0;


	                    KeyH.interactPressed = false;
	                    break;
	                }
	            }
	        }
	    }

	    houseManager.update();
	    domainManager.update(); 
	    
	    if (deathTotemManager != null && !isWorldFrozen()) { // minion ikut berhenti pas SkillV nge-freeze dunia
            deathTotemManager.update();
        }
	    if (dreadGuardian != null && !isWorldFrozen()) {
	        dreadGuardian.update();
	    }
	    if (armorEffects != null && !isWorldFrozen()) {
	        armorEffects.update();
	    }
	    if (damagePopups != null && !isWorldFrozen()) {
	        damagePopups.update();
	    }
	    if (speechBubble != null) {
            speechBubble.update();
        }
	    if (totemUI != null) totemUI.update();

	    if (gameState == GameState.DOMAIN && KeyH.escPressed) {
	        // Kalau shop merchant lagi buka, ESC ditanganin checkMerchantInteract()
	        // bukan di sini — jadi cek dulu
	    	// ── ESC: TotemUI prioritas pertama ──
	        if (KeyH.escPressed && totemUI != null && totemUI.visible) {
	            totemUI.handleEsc();
	            KeyH.escPressed = false;
	            return; // stop update frame ini
	        }
	        DarkMerchant dm = domainManager.darkMerchant;
	        boolean merchantBusy = dm != null && (dm.isShopOpen() || dm.isInDialog());
	        if (!merchantBusy) {
	            if (domainManager.isInRoom()) domainManager.exitDomain(); // ESC cuma buat keluar boss room; domain utama keluar lewat [E]
	            KeyH.escPressed = false;
	        }
	    }
	    
	    
	    if (gameState == GameState.DOMAIN) {
	        darknessAlpha = 0;
	        darkMode = false;
	    }
	    
	     // ── TAMBAHAN 2: Trigger spawn pion saat player tekan tombol T ──
            if (KeyH.tPressed) {
                if (deathTotemManager != null) {
                    deathTotemManager.trySpawn();
                
                KeyH.tPressed = false; // reset tombol agar tidak ke-spawn berkali-kali
            }
	    }
	    // 4. LOGIKA RUMAH (TETAP)
	    if (gameState == GameState.OUTSIDE) {
	        tileM.checkHouseInteraction();
	    }

	    if (gameState == GameState.INSIDE && KeyH.escPressed) {
	        houseManager.exitHouse();   // ← pakai ini, bukan exitHouse() lama
	        KeyH.escPressed = false;
	     // ── ESC: TotemUI prioritas pertama ──
	        if (KeyH.escPressed && totemUI != null && totemUI.visible) {
	            totemUI.handleEsc();
	            KeyH.escPressed = false;
	            return; // stop update frame ini
	        }
	    }
	 // Saat INSIDE, matikan dark overlay
	    if (gameState == GameState.INSIDE) {
	        darknessAlpha = 0;
	        darkMode = false;
	    }
	    
	}


	public void enterHouse() {
	    gameState = GameState.INSIDE;

	    if (!tileM.interiorLoaded) {
	        tileM.loadInteriorFloor();
	        tileM.loadInteriorObjects();
	        tileM.interiorLoaded = true;
	    }

	    
	    player.x = tileSize * 8;
	    player.y = tileSize * 6;
	}


public void exitHouse() {
    gameState = GameState.OUTSIDE;

    // spawn di depan pintu rumah
    player.x = 1100;
    player.y = 900;
}

public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    if (mainMenu.active) { mainMenu.draw(g2); g2.dispose(); return; }

    if (gameState == GameState.INSIDE) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);
        houseManager.draw(g2);   // zoom dunia rumah di-handle di dalam HouseManager.draw
        AffineTransform insideTx = applyWorldZoom(g2);
        player.draw(g2);
        if (speechBubble != null) speechBubble.draw(g2);
        g2.setTransform(insideTx);
        if (totemUI != null && totemUI.visible) totemUI.draw(g2);
        

        // UI saat INSIDE
        if (hotbar != null) hotbar.draw(g2);
        if (buffBar != null) buffBar.draw(g2);
        if (dialogOn) drawDialog(g2);
        if (backpack.visible) backpack.draw(g2, screenWidth, screenHeight, this);
        if (statUI != null && statUI.visible) statUI.draw(g2);
        if (mobIndexUI != null && mobIndexUI.visible) mobIndexUI.draw(g2, screenWidth, screenHeight, this);
        drawLeftHud(g2, true);   // koordinat + jam + cuaca, semuanya di kiri
        drawZoomHint(g2);

    } else if (gameState == GameState.DOMAIN) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        domainManager.drawBackground(g2);   // zoom background domain di-handle di dalam DomainManager

        // Y-sort semua entity
        List<Entity> sortList = new ArrayList<>();
        sortList.add(player);
        for (Entitas.Skeleton sk : domainManager.getSkeletons()) {
            if (sk != null) sortList.add(sk);
        }
        for (Entitas.Skeleton sk : domainManager.getRoomSkeletons()) {   // NEW
            if (sk != null) sortList.add(sk);
        }
        for (Entitas.Golemite gl : domainManager.getGolemites()) {
            if (gl != null && gl.isAlive()) sortList.add(gl);
        }
        for (Entitas.Golem gl : domainManager.getGolems()) {
            if (gl != null && gl.isAlive()) sortList.add(gl);
        }
        if (domainManager.darkMerchant != null) {
            sortList.add(domainManager.darkMerchant);
        }
        if (domainManager.getBossSkeleton() != null) {
            sortList.add(domainManager.getBossSkeleton());
        }

        // ── TAMBAHAN: Masukkan minion ke list Y-Sort agar digambar di layar ──
        if (deathTotemManager != null) {
            for (Entity minion : deathTotemManager.getMinions()) {
                if (minion != null) sortList.add(minion);
            }
        }
        

        sortList.sort((a, b) -> a.getSortY() - b.getSortY());
        AffineTransform domainTx = applyWorldZoom(g2);
        for (Entity e : sortList) {
            e.draw(g2);
        }
        if (dreadGuardian != null) dreadGuardian.draw(g2); // bayangan Dread Guardian (domain)
        if (armorEffects != null) armorEffects.draw(g2);   // Ember Umbral / cincin Iron Will (domain)
        if (damagePopups != null) damagePopups.draw(g2);   // angka damage (domain)
        if (speechBubble != null) speechBubble.draw(g2);
        g2.setTransform(domainTx);
        if (totemUI != null && totemUI.visible) totemUI.draw(g2);
        if (player.skillManager != null) player.skillManager.drawOverlay(g2); // atmosfer SkillV (di bawah UI)

        if (hotbar != null) hotbar.draw(g2);
        if (buffBar != null) buffBar.draw(g2);

        // UI merchant — setelah entity, biar overlay di atas segalanya
        DarkMerchant dm = domainManager.darkMerchant;
        if (dm != null) {
            dm.drawDialogBox(g2);
            dm.drawShopUI(g2);
        }

        // UI lain
        if (dialogOn) drawDialog(g2);
        if (backpack.visible) backpack.draw(g2, screenWidth, screenHeight, this);
        if (statUI != null && statUI.visible) statUI.draw(g2);
        if (mobIndexUI != null && mobIndexUI.visible) mobIndexUI.draw(g2, screenWidth, screenHeight, this);
        drawDebugInfo(g2);
        drawZoomHint(g2);
    
    } else if (gameState == GameState.OUTSIDE) {
        // Semua yang "dunia" (tile, objek, entity) digambar di dalam transform zoom.
        // Hujan, overlay gelap/sunset, dan semua UI digambar di luar transform (layar normal).
        AffineTransform baseTx = g2.getTransform();

        applyWorldZoom(g2);
        tileM.draw(g2);
        g2.setTransform(baseTx);
        
        if (world.isRaining && snow.intensity() < 0.5f) { // di tengah biome es hujan diganti badai salju
            for (RainDrop r : rain) r.draw(g2);
        }

        applyWorldZoom(g2);
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] != null) obj[i].draw(g2, this);
        }
        tileM.drawYSorted(g2, player, npc, monsters);
        if (dreadGuardian != null) dreadGuardian.draw(g2); // bayangan Dread Guardian (dunia luar)
        if (armorEffects != null) armorEffects.draw(g2);   // Ember Umbral / cincin Iron Will (dunia luar)
        if (damagePopups != null) damagePopups.draw(g2);   // angka damage (dunia luar)
        
        if (deathTotemManager != null) {

        }
        
        for (Entity n : npc) {
            if (n != null) n.drawOverhead(g2);
        }
        
        player.drawOverhead(g2);
        if (fishing != null) fishing.draw(g2); // joran + pelampung + tanda "!" pas ikan nyambar
        if (speechBubble != null) speechBubble.draw(g2);
        g2.setTransform(baseTx);

        if (totemUI != null && totemUI.visible) totemUI.draw(g2);
        snow.draw(g2);
        drawDarkOverlay(g2);
        drawSunsetOverlay(g2);
        if (player.skillManager != null) player.skillManager.drawOverlay(g2); // atmosfer SkillV (di bawah UI)

        // UI saat OUTSIDE
        if (hotbar != null) hotbar.draw(g2);
        if (buffBar != null) buffBar.draw(g2);
        if (dialogOn) drawDialog(g2);
        if (inspectOn) drawInspectPanel(g2);
        if (backpack.visible) backpack.draw(g2, screenWidth, screenHeight, this);
        if (statUI != null && statUI.visible) statUI.draw(g2);
        if (mobIndexUI != null && mobIndexUI.visible) mobIndexUI.draw(g2, screenWidth, screenHeight, this);

        AffineTransform dropsTx = g2.getTransform();
        applyWorldZoom(g2);
        for (int i = 0; i < drops.size(); i++) {
            DropItem d = drops.get(i);
            int screenX = d.x - player.x + player.screenX;
            int screenY = d.y - player.y + player.screenY;
            g2.drawImage(d.item.icon, screenX, screenY, 32, 32, null);
        }
        g2.setTransform(dropsTx);

        // Minimap di kanan atas. Digambar SETELAH drop item supaya tidak tertimpa,
        // dan disembunyikan saat panel besar (tas, stat, mob index, totem) terbuka.
        if (miniMap != null && !isMinimapHidden()) miniMap.draw(g2);

        drawLeftHud(g2, true);   // koordinat + jam + cuaca, semuanya di kiri (kanan khusus minimap)
        drawZoomHint(g2);
        if (chestUI != null) chestUI.draw(g2);       // panel peti
        if (potionShop != null) potionShop.draw(g2); // paling atas, nutup UI & drop di belakangnya
        if (armorShop != null) armorShop.draw(g2);
    }
    if (hotbar != null) hotbar.drawOverlay(g2); // tooltip + ikon yang lagi diseret, di atas semua UI
    if (deathScreenUI != null) {
        deathScreenUI.draw(g2);
    }
    g2.dispose();
}
	/** Minimap disembunyikan selagi panel besar terbuka (panelnya sudah menutupi sisi kanan layar). */
	private boolean isMinimapHidden() {
	    return (backpack != null && backpack.visible)
	        || (statUI != null && statUI.visible)
	        || (mobIndexUI != null && mobIndexUI.visible)
	        || (totemUI != null && totemUI.visible);
	}

	public void drawInspectPanel(Graphics2D g2) {

	    int width = 260;
	    int height = 200;
	    int x = screenWidth / 2 - width / 2;
	    int y = screenHeight / 2 - height / 2;

	    // panel bg
	    g2.setColor(new Color(0, 0, 0, 220));
	    g2.fillRoundRect(x, y, width, height, 20, 20);

	    // border
	    g2.setColor(Color.WHITE);
	    g2.drawRoundRect(x, y, width, height, 20, 20);

	    g2.setFont(new Font("Dialog", Font.BOLD, 15));
	    g2.drawString("NPC Command", x + 70, y + 30);

	    g2.setFont(new Font("Dialog", Font.PLAIN, 13));

	    btnStay.draw(g2);
	    btnFollow.draw(g2);
	    btnCancel.draw(g2);
	}
	
	
	public void mouseMoved(MouseEvent e) {

	    // hover hotbar (buat highlight + tooltip nama item)
	    if (hotbar != null) hotbar.handleMouseMoved(e);

	    // ── PETI PENYIMPANAN HOVER ──
	    if (chestUI != null && chestUI.visible) {
	        chestUI.handleMouseMove(e);
	        return;
	    }

	    // ── POTION MERCHANT SHOP HOVER ──
	    if (potionShop != null && potionShop.visible) {
	        potionShop.handleMouseMove(e);
	        return;
	    }

	    // ── ARMOR MERCHANT SHOP HOVER ──
	    if (armorShop != null && armorShop.visible) {
	        armorShop.handleMouseMove(e);
	        return;
	    }

	    // ── DARK MERCHANT SHOP HOVER ──
	    if (domainManager != null
	            && domainManager.darkMerchant != null
	            && domainManager.darkMerchant.isShopOpen()) {

	        domainManager.handleMerchantMouseMove(e);
	        return;
	    }

	    // TotemUI hover — jalan selalu, ga perlu inspectOn
	    if (totemUI != null && totemUI.visible) {
	        totemUI.handleMouseMove(e);
	    }

	    // NEW: Backpack hover — detail item otomatis muncul pas mouse masuk area item,
	    // dan ilang lagi pas mouse keluar dari area itemnya.
	    if (backpack != null && backpack.visible) {
	        backpack.handleMouseMove(e, this);
	    }

	    if (!inspectOn) return;

	    int mx = e.getX();
	    int my = e.getY();
	    btnStay.hovered   = btnStay.contains(mx, my);
	    btnFollow.hovered = btnFollow.contains(mx, my);
	    btnCancel.hovered = btnCancel.contains(mx, my);
	}
	
	
	public void mousePressed(MouseEvent e) {
	    // ── TotemUI: prioritas pertama, ga perlu inspectOn ──
	    if (totemUI != null && totemUI.visible) {
	        totemUI.handleMouseClick(e);
	        return;
	    }

	    // ── BackpackUI ──
	    if (backpack != null && backpack.visible) {
	        backpack.handleMouseClick(e, this);
	        return;
	    }

	    if (!inspectOn || inspectedNPC == null) return;

	    int mx = e.getX();
	    int my = e.getY();
	    if (btnStay.contains(mx, my)) {
	        inspectedNPC.npcState = Entity.NPCState.STAY;}
	    if (btnFollow.contains(mx, my)) {
	        inspectedNPC.npcState = Entity.NPCState.FOLLOW;
	        inspectedNPC.followTarget = player;
	   }
	    if (statUI != null && statUI.visible) {
	        statUI.handleClick(e);
	    }

	    if (btnCancel.contains(mx, my)) {
	        inspectedNPC.npcState = Entity.NPCState.IDLE;
	        inspectedNPC.followTarget = null;
	 }
	    if (statUI.visible && player.attributePoints > 0) {
	        //cek area klik STR (contoh angka kasar, sesuaikan dengan koordinat draw mu)
	        if(mx >= 130 && mx <= 160 && my >= 150 && my <= 170) {
	             player.strength++;
	             player.attributePoints--;
	        }
	        //cek area klik DEX
	        if(mx >= 130 && mx <= 160 && my >= 180 && my <= 200) {
	             player.dexterity++;
	             player.attributePoints--;
	        }
	    }

	    inspectOn = false;
	}
	
}