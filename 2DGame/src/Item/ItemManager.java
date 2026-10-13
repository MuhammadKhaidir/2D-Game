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
    public Item GOLD;
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
            GOLD       = new Item("Gold",       safeLoad("/drop/Gold.png",      "/drop/Ore.png"));
            Main.Log.d("[ItemManager] Preloading weapon instances...");
            weaponSwordInstance    = new Weapon("Basic Sword", "Sword");
            weaponAxeInstance      = new Weapon("Basic Axe",   "Axe");
            weaponBoneSwordInstance = new Weapon("Bone Sword", "BoneSword");
        } catch (IOException e) {
            System.err.println("[ItemManager] Gagal memuat items.");
            e.printStackTrace();
        }
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