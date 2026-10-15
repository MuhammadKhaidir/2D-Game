package Item;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import Object.Weapon;
public class ItemManager {
    private static ItemManager instance;
    public static ItemManager get() {
        if (instance == null) instance = new ItemManager();
        return instance;
    }
    public static List<ItemStack> inventory = new ArrayList<>();
    // ITEMS EXISTING
    public Item LOG;
    public Item ORE;
    public Item SLIME_GEL;
    public Item SWORD;
    public Item AXE;
    // DROP SKELETON
    public Item BONE;
    public Item SKULL;
    public Item DEATH_TOTEM;
    public Item SKELETON_KEY;
    // DROP SKELETON KING (boss)
    public Item SOUL_OF_SKELETON;
    // ITEM DARI DARK MERCHANT
    public Item BONE_SWORD;
    // EQUIPMENT
    public Item HELM;
    // CURRENCY
    public Item COIN; // drop pasti dari setiap monster yang dikill, masuk ke backpack
    // CONSUMABLE (bisa dipakai lewat hotbar)
    public Item HEALTH_POTION;
    public static final int HEALTH_POTION_HEAL = 50;     // HP yang dipulihkan per potion
    public static final int STARTER_HEALTH_POTIONS = 5;  // jumlah potion di awal game (buat testing hotbar)
    // ===== PRELOAD WEAPON INSTANCES =====
    public Weapon weaponSwordInstance;
    public Weapon weaponAxeInstance;
    public Weapon weaponBoneSwordInstance; // preload BoneSword
    private ItemManager() {
        loadItems();
        // Item awal di inventory
        inventory.add(new ItemStack(SWORD, 1));
        inventory.add(new ItemStack(AXE, 1));
        inventory.add(new ItemStack(HELM, 1)); // biar bisa langsung dites equip/unequip
        inventory.add(new ItemStack(HEALTH_POTION, STARTER_HEALTH_POTIONS)); // biar bisa langsung dites hotbar
    }
    private BufferedImage safeLoad(String path, String fallbackPath) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[ItemManager] WARNING: " + path + " tidak ditemukan, pakai fallback.");
            is = getClass().getResourceAsStream(fallbackPath);
        }
        try {
            return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("[ItemManager] CRITICAL ERROR saat baca: " + path);
            return null;
        }
    }
    private void loadItems() {
        try {
            LOG       = new Item("Log",       ImageIO.read(getClass().getResourceAsStream("/drop/Log.png")));
            ORE       = new Item("Ore",       ImageIO.read(getClass().getResourceAsStream("/drop/Ore.png")));
            SLIME_GEL = new Item("Slime Gel", ImageIO.read(getClass().getResourceAsStream("/drop/Ore.png")));
            SWORD = new Item("Sword",     safeLoad("/Aset/BasicSword.png",    "/drop/Ore.png"));
            AXE   = new Item("Axe",       safeLoad("/Aset/BasicAxe.png",      "/Aset/BasicSword.png"));
            BONE         = new Item("Bone",         safeLoad("/drop/Bone.png",        "/drop/Ore.png"));
            SKULL        = new Item("Skull",         safeLoad("/drop/Skull.png",       "/drop/Ore.png"));
            DEATH_TOTEM  = new Item("Death Totem",   safeLoad("/drop/DeathTotem.png",  "/drop/Ore.png"));
            SKELETON_KEY = new Item("Skeleton Key",  safeLoad("/drop/SkeletonKey.png", "/drop/Ore.png"));
            // TODO: pastikan file "Soul of Skeleton.png" ada di folder /drop/ (resources)
            SOUL_OF_SKELETON = new Item("Soul of Skeleton", safeLoad("/drop/Soul of Skeleton.png", "/drop/Ore.png"));
            BONE_SWORD = new Item("Bone Sword", safeLoad("/Aset/BoneSword.png", "/Aset/BasicSword.png"));
            HELM       = new Item("Helm",       safeLoad("/Aset/IronHelmet.png",      "/Aset/IronHelmet.png"));
            COIN       = new Item("Coin",       safeLoad("/drop/Coin.png",      "/drop/Ore.png"));
            // Kalau nanti punya gambar sendiri, taruh di /drop/HealthPotion.png — otomatis kepake.
            // Kalau filenya gak ada, dipakai ikon gambaran bawaan (dibuat lewat kode).
            HEALTH_POTION = new Item("Health Potion", loadHealthPotionIcon());
            HEALTH_POTION.onUse = gp -> {
                if (gp.player == null || gp.player.isDead) return false;
                if (gp.player.life >= gp.player.maxLife) {
                    if (gp.speechBubble != null) gp.speechBubble.show("HP is already full.");
                    return false; // gak dipakai, potion gak berkurang
                }
                int before = gp.player.life;
                gp.player.life = Math.min(gp.player.maxLife, gp.player.life + HEALTH_POTION_HEAL);
                if (gp.speechBubble != null) gp.speechBubble.show("+" + (gp.player.life - before) + " HP");
                return true;
            };
            Main.Log.d("[ItemManager] Preloading weapon instances...");
            weaponSwordInstance    = new Weapon("Basic Sword", "Sword");
            weaponAxeInstance      = new Weapon("Basic Axe",   "Axe");
            weaponBoneSwordInstance = new Weapon("Bone Sword", "BoneSword");
        } catch (IOException e) {
            System.err.println("[ItemManager] Gagal memuat items.");
            e.printStackTrace();
        }
    }
    private BufferedImage loadHealthPotionIcon() {
        InputStream is = getClass().getResourceAsStream("/drop/HealthPotion.png");
        if (is != null) {
            try {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            } catch (Exception ignored) { /* jatuh ke ikon bawaan */ }
        }
        return createHealthPotionIcon();
    }

    /** Ikon botol merah 16x16 digambar lewat kode, di-scale 4x (nearest) jadi 64x64. */
    private BufferedImage createHealthPotionIcon() {
        String[] map = {
            "................",
            "......cccc......",
            "......cCCc......",
            ".....kkkkkk.....",
            "......kwwk......",
            "......kwwk......",
            ".....kwggwk.....",
            "....kwrrrrwk....",
            "...kwrlrrrrwk...",
            "...kwrlrrrRwk...",
            "...kwrrrrrRwk...",
            "...kwrrrrRRwk...",
            "...kwrrrRRRwk...",
            "....kwRRRRwk....",
            ".....kkkkkk.....",
            "................"
        };
        BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int argb;
                switch (map[y].charAt(x)) {
                    case 'k': argb = 0xFF2B1A14; break; // outline
                    case 'c': argb = 0xFFB07A44; break; // gabus
                    case 'C': argb = 0xFF7A5230; break; // gabus (bayangan)
                    case 'w': argb = 0x66C8DCEB; break; // kaca
                    case 'g': argb = 0xCCFFFFFF; break; // kilap kaca
                    case 'r': argb = 0xFFD7263D; break; // cairan merah
                    case 'R': argb = 0xFF8E1631; break; // cairan merah tua
                    case 'l': argb = 0xFFFF7A85; break; // kilap cairan
                    default:  argb = 0x00000000;        // transparan
                }
                small.setRGB(x, y, argb);
            }
        }
        BufferedImage big = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = big.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                           java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(small, 0, 0, 64, 64, null);
        g.dispose();
        return big;
    }

    public void add(Item item, int amount) {
        if (item == null || amount <= 0) return;
        for (ItemStack stack : inventory) {
            if (stack.isSame(item)) { stack.amount += amount; return; }
        }
        inventory.add(new ItemStack(item, amount));
    }
    public void cleanNull() {
        inventory.removeIf(s -> s == null || s.item == null);
    }
}