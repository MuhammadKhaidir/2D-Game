package Item;
import java.util.ArrayList;
import java.util.EnumMap;
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
    // CONSUMABLE (bisa dipakai lewat hotbar) — semua potion didefinisikan di PotionType
    private final EnumMap<PotionType, Item> potions = new EnumMap<>(PotionType.class);
    public static final int STARTER_SMALL_HP_POTIONS = 5;  // jumlah potion di awal game (buat testing hotbar)
    // ===== PRELOAD WEAPON INSTANCES =====
    public Weapon weaponSwordInstance;
    public Weapon weaponAxeInstance;
    public Weapon weaponBoneSwordInstance; // preload BoneSword
    private ItemManager() {
        loadItems();
        loadPotions();
        // Item awal di inventory
        inventory.add(new ItemStack(SWORD, 1));
        inventory.add(new ItemStack(AXE, 1));
        inventory.add(new ItemStack(HELM, 1)); // biar bisa langsung dites equip/unequip
        inventory.add(new ItemStack(getPotion(PotionType.SMALL_HP), STARTER_SMALL_HP_POTIONS)); // biar bisa langsung dites hotbar
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
            Main.Log.d("[ItemManager] Preloading weapon instances...");
            weaponSwordInstance    = new Weapon("Basic Sword", "Sword");
            weaponAxeInstance      = new Weapon("Basic Axe",   "Axe");
            weaponBoneSwordInstance = new Weapon("Bone Sword", "BoneSword");
        } catch (IOException e) {
            System.err.println("[ItemManager] Gagal memuat items.");
            e.printStackTrace();
        }
    }

    // ===================================================================
    // POTION
    // ===================================================================

    private void loadPotions() {
        for (PotionType type : PotionType.values()) {
            Item potion = new Item(type.itemName, loadPotionIcon(type));
            potion.onUse = createPotionEffect(type);
            potions.put(type, potion);
        }
    }

    /** Efek pas potion dipakai lewat hotbar. Return false = gak dipakai (potion gak berkurang). */
    private Item.UseEffect createPotionEffect(PotionType type) {
        return gp -> {
            Entitas.player p = gp.player;
            if (p == null || p.isDead) return false;

            if (type.isHeal()) {
                if (p.life >= p.maxLife) {
                    say(gp, "HP is already full.");
                    return false;
                }
                int before = p.life;
                p.life = Math.min(p.maxLife, p.life + type.heal);
                say(gp, "+" + (p.life - before) + " HP");
            } else {
                p.buffs.activate(type.buff, Entitas.PlayerBuffs.DURATION_SECONDS * gp.FPS);
                say(gp, type.buff.label + "! " + Entitas.PlayerBuffs.formatTime(Entitas.PlayerBuffs.DURATION_SECONDS));
            }
            return true;
        };
    }

    private static void say(Main.GamePanel gp, String message) {
        if (gp.speechBubble != null) gp.speechBubble.show(message);
    }

    public Item getPotion(PotionType type) {
        return potions.get(type);
    }

    /** Cari potion lewat nama item (buat load save). null kalau bukan potion. */
    public Item getPotionByName(String name) {
        PotionType type = PotionType.fromItemName(name);
        return type != null ? potions.get(type) : null;
    }

    /** Pakai gambar src/drop/<imageFile> kalau ada; kalau belum ada, pakai ikon botol bawaan. */
    private BufferedImage loadPotionIcon(PotionType type) {
        try (InputStream is = getClass().getResourceAsStream("/drop/" + type.imageFile)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            }
        } catch (Exception ignored) { /* jatuh ke ikon bawaan */ }
        return createPotionIcon(type.liquidColor);
    }

    /** Ikon botol 16x16 digambar lewat kode (warna cairan = liquidColor), di-scale 4x (nearest) jadi 64x64. */
    private BufferedImage createPotionIcon(int liquidColor) {
        int liquid      = 0xFF000000 | liquidColor;
        int liquidDark  = scaleColor(liquidColor, 0.6);
        int liquidLight = blendWithWhite(liquidColor, 0.45);
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
                    case 'r': argb = liquid;      break; // cairan
                    case 'R': argb = liquidDark;  break; // cairan (bayangan)
                    case 'l': argb = liquidLight; break; // kilap cairan
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

    private static int scaleColor(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int blendWithWhite(int rgb, double amount) {
        int r = (int) (((rgb >> 16) & 0xFF) * (1 - amount) + 255 * amount);
        int g = (int) (((rgb >> 8) & 0xFF) * (1 - amount) + 255 * amount);
        int b = (int) ((rgb & 0xFF) * (1 - amount) + 255 * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ===================================================================
    // INVENTORY
    // ===================================================================

    public void add(Item item, int amount) {
        if (item == null || amount <= 0) return;
        for (ItemStack stack : inventory) {
            if (stack.isSame(item)) { stack.amount += amount; return; }
        }
        inventory.add(new ItemStack(item, amount));
    }

    /** Total jumlah item ini di inventory. */
    public int count(Item item) {
        int total = 0;
        for (ItemStack stack : inventory) {
            if (stack != null && stack.isSame(item)) total += stack.amount;
        }
        return total;
    }

    /** Kurangi item dari inventory. Return false (dan tidak mengurangi apa pun) kalau jumlahnya kurang. */
    public boolean remove(Item item, int amount) {
        if (amount <= 0) return true;
        if (count(item) < amount) return false;
        int left = amount;
        for (ItemStack stack : new ArrayList<>(inventory)) {
            if (left <= 0) break;
            if (stack == null || !stack.isSame(item)) continue;
            int taken = Math.min(stack.amount, left);
            stack.amount -= taken;
            left -= taken;
            if (stack.amount <= 0) inventory.remove(stack);
        }
        return true;
    }
    public void cleanNull() {
        inventory.removeIf(s -> s == null || s.item == null);
    }
}