package Item;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Color;
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
    // EQUIPMENT (armor = satu set utuh per item, data-nya di ArmorSet)
    public Item DREAD_ARMOR;      // dijual Dark Merchant (Tomb Domain)
    public Item IRON_ARMOR;       // dijual Armor Merchant (dekat rumah spawn), bayar Coin
    public Item UMBRAL_ARMOR;     // dijual Dark Merchant (Tomb Domain)
    // CURRENCY
    public Item COIN; // drop pasti dari setiap monster yang dikill, masuk ke backpack
    // MANCING (lihat FishingManager)
    public Item FISHING_ROD;   // dijual Armor Merchant, bayar Coin. Gak habis dipakai.
    public Item FISH_SMALL;    // tangkapan umum,  makan = +HP kecil
    public Item FISH_BIG;      // tangkapan lumayan, makan = +HP sedang
    public Item FISH_GOLDEN;   // tangkapan langka, makan = +HP besar
    public static final int FISH_SMALL_HEAL  = 15;
    public static final int FISH_BIG_HEAL    = 35;
    public static final int FISH_GOLDEN_HEAL = 80;
    public FishingLoot fishingLoot;   // semua barang lain hasil mancing (dibuat di loadItems)
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
            DREAD_ARMOR = new Item(ArmorSet.DREAD.itemName, safeLoad("/Aset/DreadArmor.png", "/drop/Ore.png"));
            DREAD_ARMOR.maxStack = 1; // armor gak numpuk
            IRON_ARMOR = new Item(ArmorSet.IRON.itemName, safeLoad("/Aset/IronArmor.png", "/drop/Ore.png"));
            IRON_ARMOR.maxStack = 1;
            UMBRAL_ARMOR = new Item(ArmorSet.UMBRAL.itemName, safeLoad("/Aset/UmbralArmor.png", "/drop/Ore.png"));
            UMBRAL_ARMOR.maxStack = 1;
            COIN       = new Item("Coin",       safeLoad("/drop/Coin.png",      "/drop/Ore.png"));
            // MANCING: ikonnya digambar lewat kode (gak butuh file gambar). Mau pakai gambar sendiri? ganti icon-nya di sini.
            FISHING_ROD = new Item("Fishing Rod", createRodIcon());
            FISHING_ROD.maxStack = 1;
            FISHING_ROD.onUse = gp -> {                 // pakai rod (hotbar / backpack) = pegang / simpan pancingan, lalu klik air buat melempar
                if (gp.fishing != null) gp.fishing.toggleRod();
                return false;                           // rod-nya sendiri gak habis
            };
            FISH_SMALL  = new Item("Small Fish",  createFishIcon(0xB8C4CC, 0xE6EDF0, 0x8C9AA4, false, false));
            FISH_BIG    = new Item("Big Fish",    createFishIcon(0x5E86B0, 0xC9D8E6, 0x3F5F85, true,  false));
            FISH_GOLDEN = new Item("Golden Fish", createFishIcon(0xF2B930, 0xFFE9A0, 0xD98A1C, true,  true));
            FISH_SMALL.onUse  = createFoodEffect(FISH_SMALL_HEAL);
            FISH_BIG.onUse    = createFoodEffect(FISH_BIG_HEAL);
            FISH_GOLDEN.onUse = createFoodEffect(FISH_GOLDEN_HEAL);
            fishingLoot = new FishingLoot(this);   // tabel hasil mancing (sampah, makanan, buff, harta, senjata) — lihat FishingLoot
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

    // ===================================================================
    // DROP POTION DARI MONSTER
    // ===================================================================

    /** Peluang tiap monster yang mati ngejatuhin Small HP Potion (0.0 - 1.0). */
    public static final double SMALL_HP_DROP_CHANCE  = 0.50; // 50%
    /** Peluang Medium HP Potion. Dibuat lebih langka dari small karena healnya 3x lebih gede. */
    public static final double MEDIUM_HP_DROP_CHANCE = 0.20; // 20%

    private final java.util.Random potionDropRng = new java.util.Random();

    /**
     * Dipanggil sekali tiap monster mati. Small & medium di-roll terpisah, jadi keduanya
     * bisa keluar sekaligus. Potion langsung masuk backpack (sama kayak drop Skeleton).
     */
    public void rollPotionDrops(Main.GamePanel gp) {
        rollPotion(gp, PotionType.SMALL_HP,  SMALL_HP_DROP_CHANCE);
        rollPotion(gp, PotionType.MEDIUM_HP, MEDIUM_HP_DROP_CHANCE);
    }

    private void rollPotion(Main.GamePanel gp, PotionType type, double chance) {
        if (potionDropRng.nextDouble() >= chance) return;
        add(getPotion(type), 1);
        say(gp, "Obtained 1x " + type.itemName);
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
    // MANCING: efek makan ikan + ikon
    // ===================================================================

    /** Efek makan ikan lewat hotbar: nambah HP (maksimal sampai penuh). Kalau HP udah penuh, ikan gak dimakan. */
    private Item.UseEffect createFoodEffect(int heal) {
        return gp -> {
            Entitas.player p = gp.player;
            if (p == null || p.isDead) return false;
            if (p.life >= p.maxLife) {
                say(gp, "HP is already full.");
                return false;
            }
            int before = p.life;
            p.life = Math.min(p.maxLife, p.life + heal);
            say(gp, "+" + (p.life - before) + " HP");
            return true;
        };
    }

    /** Ikon ikan 16x16 digambar lewat kode, di-scale 4x (nearest) jadi 64x64. big = badan lebih gede, sparkle = kilau (ikan emas). */
    private BufferedImage createFishIcon(int bodyRgb, int bellyRgb, int finRgb, boolean big, boolean sparkle) {
        Color body  = new Color(bodyRgb);
        Color belly = new Color(bellyRgb);
        Color fin   = new Color(finRgb);
        Color line  = new Color(scaleColor(bodyRgb, 0.40));

        BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = small.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);

        int bw = big ? 11 : 9;      // lebar badan
        int bh = big ? 8 : 6;       // tinggi badan
        int bx = 1;                 // badan mulai dari kiri (kepala di kiri, ekor di kanan)
        int by = 8 - bh / 2;
        int tailX = bx + bw - 1;

        // ekor
        g.setColor(fin);
        g.fillPolygon(new int[] {tailX, 15, 15}, new int[] {8, 8 - (big ? 4 : 3), 8 + (big ? 4 : 3)}, 3);
        // sirip punggung
        g.fillPolygon(new int[] {bx + bw / 2 - 2, bx + bw / 2 + 1, bx + bw / 2 + 2}, new int[] {by, by - 3, by}, 3);
        // badan
        g.setColor(body);
        g.fillOval(bx, by, bw, bh);
        // perut (setengah bawah lebih terang)
        g.setClip(0, 8, 16, 8);
        g.setColor(belly);
        g.fillOval(bx, by, bw, bh);
        g.setClip(null);
        // garis tepi
        g.setColor(line);
        g.drawOval(bx, by, bw - 1, bh - 1);
        // insang + mata
        g.drawLine(bx + 3, by + 2, bx + 3, by + bh - 3);
        small.setRGB(bx + 1, by + 2, 0xFFFFFFFF);
        small.setRGB(bx + 1, by + 3, 0xFF1A1A1A);
        if (sparkle) {
            small.setRGB(2, 2, 0xFFFFFFFF);
            small.setRGB(1, 3, 0xCCFFFFFF);
            small.setRGB(3, 3, 0xCCFFFFFF);
            small.setRGB(2, 4, 0xCCFFFFFF);
            small.setRGB(13, 12, 0xFFFFFFFF);
        }
        g.dispose();
        return scaleUp(small);
    }

    /** Ikon Fishing Rod 16x16: joran miring, gulungan, tali + pelampung merah-putih. */
    private BufferedImage createRodIcon() {
        BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // joran: dari pojok kiri bawah (gagang, lebih gelap) ke kanan atas (ujung, lebih terang)
        for (int i = 0; i < 12; i++) {
            int x = 1 + i;
            int y = 14 - i;
            int c = i < 3 ? 0xFF4A2E18 : (i < 8 ? 0xFF7A5230 : 0xFFA9743F);
            small.setRGB(x, y, c);
            if (i < 3) small.setRGB(x, y - 1, c);          // gagang agak tebal
        }
        // gulungan
        small.setRGB(4, 10, 0xFFB0B7BD);
        small.setRGB(5, 10, 0xFF7C858C);
        small.setRGB(4, 11, 0xFF7C858C);
        // tali dari ujung joran ke bawah
        for (int y = 3; y <= 9; y++) small.setRGB(13, y, 0xFFE8E8E8);
        // pelampung
        small.setRGB(13, 10, 0xFFD23A2E);
        small.setRGB(12, 10, 0xFFD23A2E);
        small.setRGB(14, 10, 0xFFD23A2E);
        small.setRGB(13, 11, 0xFFFFFFFF);
        small.setRGB(12, 11, 0xFFFFFFFF);
        small.setRGB(14, 11, 0xFFFFFFFF);
        small.setRGB(13, 12, 0xFF2B1A14);
        return scaleUp(small);
    }

    private static BufferedImage scaleUp(BufferedImage small) {
        BufferedImage big = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = big.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                           java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(small, 0, 0, 64, 64, null);
        g.dispose();
        return big;
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