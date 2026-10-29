package Item;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

import Entitas.PlayerBuffs;
import Entitas.PlayerBuffs.Buff;
import Main.GamePanel;
import Object.Weapon;

/**
 * TABEL HASIL MANCING. Semua yang bisa ketarik dari air ada di sini, jadi mau nambah / ngurangin barang,
 * ubah peluang, harga, durasi buff, atau damage senjata cukup edit baris di method {@link #buildTable()}.
 *
 * Jenis hasil ({@link Kind}):
 *   JUNK      sampah, gak ada gunanya (cuma koleksi / bisa dihapus dari backpack)
 *   FOOD      ikan / makanan: dimakan lewat hotbar buat nambah HP
 *   BUFF      barang buff: dipakai lewat hotbar, ngasih boost sementara (sama kayak boost potion, tapi lebih singkat)
 *   TREASURE  harta: langsung kebuka pas ditangkap, isinya Coin (peti kadang bonus potion)
 *   WEAPON    senjata: dipasang lewat backpack / hotbar. Gerakan serangnya pakai set animasi Sword / Axe, gambar senjata
 *             di tangannya sama kayak ikon (src/drop/fishing/held/&lt;slug&gt;.png), efek tebasannya diberi warna
 *
 * Kelangkaan ({@link Rarity}) dipakai buat: bayangan ikan di air, efek pas barangnya muncul, dan warna teks / efek.
 * Peluang tiap barang = bobotnya / total bobot semua barang (bobot gak harus dijumlah ke 100).
 *
 * Gambar: src/drop/fishing/&lt;slug&gt;.png (dibuat tools/import_fishing_sheet.py dari gambar sheet, boleh ditimpa gambar sendiri).
 */
public final class FishingLoot {

    // ===== SETELAN (gampang diubah) =====
    public static final int BUFF_ITEM_SECONDS   = 60;   // durasi buff dari barang buff murni
    public static final int HYBRID_BUFF_SECONDS = 45;   // durasi buff dari ikan "heal + buff" (Electric Eel, Firefin Tuna)
    /** Pengali peluang barang RARE ke atas saat hujan / malam (bisa numpuk). */
    public static final double RAIN_RARE_BOOST  = 1.4;
    public static final double NIGHT_RARE_BOOST = 1.25;
    /** Senjata kembar (udah punya) diganti Coin segini, tergantung kelangkaan. */
    private static final int[] DUPLICATE_WEAPON_COINS = {0, 0, 10, 25, 60, 120};   // index = Rarity.ordinal()

    public enum Rarity {
        JUNK("Junk", 0x9AA0A6),
        COMMON("Common", 0xE8E8E8),
        UNCOMMON("Uncommon", 0x62D26F),
        RARE("Rare", 0x4DA3FF),
        EPIC("Epic", 0xB36BFF),
        LEGENDARY("Legendary", 0xFFC84A);

        public final String label;
        public final int rgb;

        Rarity(String label, int rgb) {
            this.label = label;
            this.rgb = rgb;
        }

        public boolean isRareOrBetter() {
            return ordinal() >= RARE.ordinal();
        }
    }

    public enum Kind { JUNK, FOOD, BUFF, TREASURE, WEAPON }

    /** Satu baris di tabel loot. */
    public static final class Entry {
        public final Item item;
        public final Rarity rarity;
        public final Kind kind;
        public final int weight;
        public final String description;

        // senjata
        public String weaponType;     // "Sword" / "Axe" / "BoneSword" (pilih set animasi)
        public int weaponDamage;
        public int weaponTint;        // 0xRRGGBB, warna efek tebasan (gambar senjatanya sendiri gak diwarnai)
        public String heldSheet;      // lembar frame ayunan: /drop/fishing/held/<slug>.png (null / hilang = pakai frame bawaan yang diwarnai)

        // harta
        public int coinMin, coinMax;
        public boolean bonusPotion;

        Entry(Item item, Rarity rarity, Kind kind, int weight, String description) {
            this.item = item;
            this.rarity = rarity;
            this.kind = kind;
            this.weight = weight;
            this.description = description;
        }
    }

    /** Hasil sebuah tangkapan setelah diproses (barang sudah masuk backpack). */
    public static final class Result {
        public final Entry entry;
        public final String message;     // buat speech bubble
        public final String extraLine;   // baris kecil di bawah rarity pas barangnya muncul (boleh null)

        Result(Entry entry, String message, String extraLine) {
            this.entry = entry;
            this.message = message;
            this.extraLine = extraLine;
        }
    }

    private final ItemManager im;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Entry> byName = new HashMap<>();
    private final Map<String, Weapon> weapons = new HashMap<>();   // dibuat pas pertama dipakai (satu Weapon = puluhan gambar + 3 Clip)
    private int totalWeight;

    /** Dipanggil ItemManager di dalam constructor-nya, jadi JANGAN panggil ItemManager.get() di sini. */
    FishingLoot(ItemManager im) {
        this.im = im;
        buildTable();
        for (Entry e : entries) {
            totalWeight += e.weight;
            byName.put(e.item.name.toLowerCase(), e);
        }
    }

    // ===================================================================
    // TABEL
    // ===================================================================

    private void buildTable() {
        // ---------- SAMPAH ----------
        junk("Old Boot",        "old_boot",      40, "Soggy and smelly. Somebody lost this a very long time ago.");
        junk("Rusty Tin Can",   "tin_can",       40, "A dented can full of holes. Nobody wants this.");
        junk("Seaweed Clump",   "seaweed_clump", 40, "A slimy tangle of seaweed. Smells like low tide.");
        junk("Broken Bottle",   "broken_bottle", 30, "Sharp green shards. Watch your step.");
        junk("Soggy Plank",     "soggy_plank",   30, "A rotten plank from some wrecked boat.");
        junk("Tangled Net",     "tangled_net",   19, "A knotted old fishing net, far too tangled to use.");

        // ---------- MAKANAN (heal) ----------
        food("Sardine",    "sardine",    Rarity.COMMON,   130, 10, "A slim silver fish.");
        existingFood(im.FISH_SMALL,  Rarity.COMMON,   120, ItemManager.FISH_SMALL_HEAL,  "A small, silvery fish. Not much of a meal, but it will do.");
        food("Dried Kelp", "dried_kelp", Rarity.UNCOMMON,  60, 25, "Chewy sun-dried kelp. Salty and filling.");
        existingFood(im.FISH_BIG,    Rarity.UNCOMMON,  90, ItemManager.FISH_BIG_HEAL,    "A plump blue fish with firm flesh.");
        food("Crab",       "crab",       Rarity.UNCOMMON,  60, 40, "A feisty crab. Cracks open into sweet meat.");
        food("Salmon",     "salmon",     Rarity.UNCOMMON,  50, 55, "A thick, rosy fillet-to-be. Hearty.");
        food("Lobster",    "lobster",    Rarity.RARE,      30, 60, "A big red lobster. A real feast.");
        existingFood(im.FISH_GOLDEN, Rarity.EPIC,      15, ItemManager.FISH_GOLDEN_HEAL, "A rare fish with shimmering golden scales.");

        // ---------- BUFF ----------
        buff("Jellyfish Jelly", "jellyfish_jelly", Rarity.UNCOMMON, 25, Buff.ATTACK_SPEED, "Wobbly jelly that makes you swing faster.");
        buff("Storm Bottle",    "storm_bottle",    Rarity.UNCOMMON, 25, Buff.SPEED,        "A bottled thunderstorm. Your feet feel light.");
        buff("Kraken Ink",      "kraken_ink",      Rarity.RARE,     20, Buff.DAMAGE,       "Dark ink from the deep. Your strikes hit harder.");
        buff("Vitality Pearl",  "vitality_pearl",  Rarity.RARE,     20, Buff.MAX_HP,       "A glowing pearl. Swallow it for a burst of vigor.");
        hybrid("Electric Eel",  "electric_eel",    Rarity.RARE,     18, 25, Buff.SPEED,    "Crackling with energy. Tastes shocking.");
        hybrid("Firefin Tuna",  "firefin_tuna",    Rarity.RARE,     18, 30, Buff.DAMAGE,   "A tuna with a burning fin. Eat it hot.");

        // ---------- HARTA (langsung kebuka) ----------
        treasure("Coin Pouch",   "coin_pouch",   Rarity.UNCOMMON, 50,  5, 12, false, "A soggy pouch with some coins inside.");
        treasure("Sunken Chest", "sunken_chest", Rarity.RARE,     12, 25, 50, true,  "A barnacled chest from the seabed. Coins and a potion inside.");

        // ---------- SENJATA ----------   (nama, gambar, rarity, bobot, tipe animasi, damage, warna, deskripsi)
        weapon("Rusty Cutlass",     "rusty_cutlass",     Rarity.UNCOMMON,  14, "Sword",     35, 0xC8742C, "A pitted pirate blade. Still bites.");
        weapon("Driftwood Hatchet", "driftwood_hatchet", Rarity.UNCOMMON,  14, "Axe",       40, 0xC9A66B, "Sea-worn wood lashed with kelp rope.");
        weapon("Coral Cutlass",     "coral_cutlass",     Rarity.RARE,       9, "Sword",     70, 0xF29BB0, "A cutlass grown from living coral.");
        weapon("Sailor's Hatchet",  "sailors_hatchet",   Rarity.RARE,       9, "Axe",       80, 0x8FB0E0, "A salt-hardened steel hatchet.");
        weapon("Leviathan Fang",    "leviathan_fang",    Rarity.EPIC,       5, "Sword",     130, 0x2EC4B6, "A serrated fang from a deep sea beast.");   // animasi Sword: frame BoneSword bawaan nyatu sama efek glow-nya
        weapon("Kraken Cleaver",    "kraken_cleaver",    Rarity.EPIC,       4, "Axe",       150, 0x9B63E0, "A heavy cleaver studded with sucker barbs.");
        weapon("Tidebreaker",       "tidebreaker",       Rarity.LEGENDARY,  3, "Sword",     220, 0x4FD6F0, "A blade said to part the sea itself.");
    }

    private void junk(String name, String slug, int weight, String desc) {
        add(new Entry(icon(name, slug), Rarity.JUNK, Kind.JUNK, weight, desc));
    }

    private void food(String name, String slug, Rarity r, int weight, int heal, String flavor) {
        Item item = new Item(name, load(slug));
        item.onUse = eat(heal, null, 0);
        add(new Entry(item, r, Kind.FOOD, weight, flavor + " Restores " + heal + " HP."));
    }

    /** Ikan lama (Small / Big / Golden Fish): item-nya sudah dibuat ItemManager, di sini cuma didaftarkan ke tabel. */
    private void existingFood(Item item, Rarity r, int weight, int heal, String flavor) {
        add(new Entry(item, r, Kind.FOOD, weight, flavor + " Restores " + heal + " HP."));
    }

    private void buff(String name, String slug, Rarity r, int weight, Buff buff, String flavor) {
        Item item = new Item(name, load(slug));
        item.onUse = eat(0, buff, BUFF_ITEM_SECONDS);
        add(new Entry(item, r, Kind.BUFF, weight,
                flavor + " " + buff.effectText() + " for " + PlayerBuffs.formatTime(BUFF_ITEM_SECONDS) + "."));
    }

    private void hybrid(String name, String slug, Rarity r, int weight, int heal, Buff buff, String flavor) {
        Item item = new Item(name, load(slug));
        item.onUse = eat(heal, buff, HYBRID_BUFF_SECONDS);
        add(new Entry(item, r, Kind.FOOD, weight,
                flavor + " +" + heal + " HP and " + buff.effectText() + " for "
                        + PlayerBuffs.formatTime(HYBRID_BUFF_SECONDS) + "."));
    }

    private void treasure(String name, String slug, Rarity r, int weight, int coinMin, int coinMax, boolean potion, String desc) {
        Entry e = new Entry(icon(name, slug), r, Kind.TREASURE, weight, desc);
        e.coinMin = coinMin;
        e.coinMax = coinMax;
        e.bonusPotion = potion;
        add(e);
    }

    private void weapon(String name, String slug, Rarity r, int weight, String type, int damage, int tint, String flavor) {
        Item item = icon(name, slug);
        item.maxStack = 1;
        Entry e = new Entry(item, r, Kind.WEAPON, weight, flavor + " Swings like a " + prettyType(type) + ".");
        e.weaponType = type;
        e.weaponDamage = damage;
        e.weaponTint = tint;
        e.heldSheet = "/drop/fishing/held/" + slug + ".png";
        add(e);
    }

    private static String prettyType(String type) {
        return type.equalsIgnoreCase("Axe") ? "axe" : (type.equalsIgnoreCase("BoneSword") ? "bone sword" : "sword");
    }

    private void add(Entry e) {
        entries.add(e);
    }

    private Item icon(String name, String slug) {
        return new Item(name, load(slug));
    }

    /** Muat src/drop/fishing/&lt;slug&gt;.png. Kalau gambarnya hilang, pakai Ore.png supaya game tetap jalan. */
    private BufferedImage load(String slug) {
        String[] paths = {"/drop/fishing/" + slug + ".png", "/drop/Ore.png"};
        for (String path : paths) {
            try (InputStream is = FishingLoot.class.getResourceAsStream(path)) {
                if (is == null) continue;
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            } catch (Exception ignored) { /* coba berikutnya */ }
        }
        System.err.println("[FishingLoot] Gambar hilang: " + slug);
        return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    }

    // ===================================================================
    // EFEK PAS DIPAKAI (hotbar)
    // ===================================================================

    /**
     * Makan / pakai: heal (kalau heal &gt; 0) dan / atau buff (kalau buff != null). Return false = gak ada efeknya
     * (HP penuh dan buff masih lebih lama), jadi barangnya TIDAK berkurang. Buff yang lagi jalan cuma diganti
     * kalau durasi barunya lebih lama (PlayerBuffs.activate nimpa sisa waktu, jadi dicek dulu di sini).
     */
    private static Item.UseEffect eat(int heal, Buff buff, int buffSeconds) {
        return gp -> {
            Entitas.player p = gp.player;
            if (p == null || p.isDead) return false;

            int healed = 0;
            if (heal > 0) {
                int before = p.life;
                p.life = Math.min(p.maxLife, p.life + heal);
                healed = p.life - before;
            }

            boolean buffed = false;
            if (buff != null) {
                int frames = buffSeconds * gp.FPS;
                int remaining = p.buffs.secondsLeft(buff) * gp.FPS;
                if (frames > remaining) {
                    p.buffs.activate(buff, frames);
                    buffed = true;
                }
            }

            if (healed == 0 && !buffed) {
                say(gp, buff == null ? "HP is already full."
                        : (heal > 0 ? "Nothing happens right now." : buff.label + " is already active."));
                return false;
            }

            StringBuilder msg = new StringBuilder();
            if (healed > 0) msg.append("+").append(healed).append(" HP");
            if (buffed) {
                if (msg.length() > 0) msg.append("  ");
                msg.append(buff.label).append("! ").append(PlayerBuffs.formatTime(buffSeconds));
            }
            say(gp, msg.toString());
            return true;
        };
    }

    private static void say(GamePanel gp, String msg) {
        if (gp.speechBubble != null) gp.speechBubble.show(msg);
    }

    // ===================================================================
    // GACHA
    // ===================================================================

    /**
     * Tarik satu hasil tangkapan. rareBoost = pengali bobot barang RARE ke atas (1.0 = normal).
     */
    public Entry roll(Random rng, double rareBoost) {
        double total = 0;
        for (Entry e : entries) total += effectiveWeight(e, rareBoost);
        double pick = rng.nextDouble() * total;
        for (Entry e : entries) {
            pick -= effectiveWeight(e, rareBoost);
            if (pick < 0) return e;
        }
        return entries.get(0);
    }

    private static double effectiveWeight(Entry e, double rareBoost) {
        return e.rarity.isRareOrBetter() ? e.weight * rareBoost : e.weight;
    }

    /** Peluang (persen) sebuah barang dengan boost 1.0. Dipakai buat dokumentasi / debug. */
    public double chancePercent(Entry e) {
        return 100.0 * e.weight / Math.max(1, totalWeight);
    }

    /**
     * Berikan hasil tangkapan ke player: masukin ke backpack (atau buka harta / tukar senjata kembar jadi Coin).
     * Dipanggil sekali pas ikannya selesai ditarik.
     */
    public Result award(GamePanel gp, Entry e, Random rng) {
        switch (e.kind) {
            case TREASURE: {
                int coins = e.coinMin + rng.nextInt(e.coinMax - e.coinMin + 1);
                im.add(im.COIN, coins);
                String extra = "+" + coins + " Coin";
                String msg = "Found a " + e.item.name + "! +" + coins + " Coin";
                if (e.bonusPotion) {
                    // sengaja pakai if, bukan ternary enum: compiler Eclipse (ECJ) bisa salah ngetik hasil ternary
                    // jadi Object di bytecode -> java.lang.VerifyError pas FishingLoot di-load
                    PotionType type = PotionType.SMALL_HP;
                    if (rng.nextInt(100) < 35) type = PotionType.MEDIUM_HP;
                    im.add(im.getPotion(type), 1);
                    extra += "  +1 " + type.itemName;
                    msg += " and a " + type.itemName + "!";
                }
                return new Result(e, msg, extra);
            }
            case WEAPON: {
                if (im.count(e.item) > 0) {            // senjata gak numpuk (maxStack 1), jadi yang kembar dijual otomatis
                    int coins = DUPLICATE_WEAPON_COINS[e.rarity.ordinal()];
                    im.add(im.COIN, coins);
                    return new Result(e, "Another " + e.item.name + "... sold it for " + coins + " Coin.",
                            "Duplicate! +" + coins + " Coin");
                }
                im.add(e.item, 1);
                return new Result(e, "Wow! You fished up a " + e.item.name + "!", "DMG " + e.weaponDamage);
            }
            case JUNK:
                im.add(e.item, 1);
                return new Result(e, "Ugh... just a " + e.item.name + ".", null);
            default:
                im.add(e.item, 1);
                return new Result(e, e.rarity.isRareOrBetter()
                        ? "Nice catch! 1x " + e.item.name + "!"
                        : "Caught 1x " + e.item.name + "!", null);
        }
    }

    // ===================================================================
    // LOOKUP (backpack, save / load)
    // ===================================================================

    public Entry entryFor(String itemName) {
        return itemName == null ? null : byName.get(itemName.toLowerCase());
    }

    /** Item hasil mancing lewat nama (buat load save). null kalau bukan barang dari tabel ini. */
    public Item itemByName(String name) {
        Entry e = entryFor(name);
        return e != null ? e.item : null;
    }

    public String describe(String itemName) {
        Entry e = entryFor(itemName);
        return e == null ? null : "(" + e.rarity.label + ") " + e.description;
    }

    public boolean isWeapon(String itemName) {
        Entry e = entryFor(itemName);
        return e != null && e.kind == Kind.WEAPON;
    }

    /** Damage senjata dari mancing, null kalau bukan senjata. Dipakai panel stat backpack. */
    public Integer weaponDamage(String itemName) {
        Entry e = entryFor(itemName);
        return (e != null && e.kind == Kind.WEAPON) ? Integer.valueOf(e.weaponDamage) : null;
    }

    /** Objek Weapon (animasi + damage) buat senjata mancing ini. Dibuat sekali, dipakai ulang. null kalau bukan senjata. */
    public Weapon weaponFor(String itemName) {
        Entry e = entryFor(itemName);
        if (e == null || e.kind != Kind.WEAPON) return null;
        return weapons.computeIfAbsent(e.item.name,
                n -> new Weapon(e.item.name, e.weaponType, e.weaponDamage, e.weaponTint, e.heldSheet));
    }

    public List<Entry> all() {
        return entries;
    }
}
