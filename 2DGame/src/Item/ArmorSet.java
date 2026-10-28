package Item;

/**
 * ArmorSet - data satu SET armor utuh (helm + badan + sarung tangan + celana + sepatu jadi SATU item).
 *
 * Satu set = satu Item di inventory, satu slot "ARMOR" di backpack, dan satu folder sprite di
 * /Aset/armor/&lt;assetFolder&gt;/ (di-render oleh {@link Object.PlayerArmor}).
 *
 * Tiap set punya RARITY (COMMON / RARE) dan satu SET EFFECT (nama + deskripsi buat UI). Logika efeknya sendiri ada di
 * {@link DreadGuardian} (Dread), {@link ArmorSetEffects} (Iron & Umbral).
 *
 * Nambah set baru: 1) bikin sprite-nya (lihat tools/generate_dread_armor.py),
 * 2) daftarin satu konstanta di sini (rarity + efek) + masukin ke ALL, 3) bikin Item-nya di ItemManager,
 * 4) jual lewat NPC (DarkMerchant.shopItems + DomainManager.handleShopBuy), 5) bikin efeknya.
 */
public final class ArmorSet {

    /** Tier armor. COMMON = efek sederhana, RARE = efek kuat (set dari Dark Merchant). */
    public enum Rarity {
        COMMON("COMMON"),
        RARE("RARE");

        public final String label;

        Rarity(String label) {
            this.label = label;
        }
    }

    /** Nama item di inventory / save file / shop. */
    public final String itemName;
    /** Folder sprite di src/Aset/armor/. */
    public final String assetFolder;
    /** Pengurang damage flat (ditambah ke stat DEF player). */
    public final int defense;
    /** Pengurang damage persen, dihitung setelah defense (0-100). */
    public final int damageReduction;
    public final Rarity rarity;
    /** Nama set effect (mis. "Iron Will"). */
    public final String effectName;
    /** Penjelasan singkat set effect buat UI. */
    public final String effectDescription;
    public final String description;

    private ArmorSet(String itemName, String assetFolder, int defense, int damageReduction,
                     Rarity rarity, String effectName, String effectDescription, String description) {
        this.itemName = itemName;
        this.assetFolder = assetFolder;
        this.defense = defense;
        this.damageReduction = damageReduction;
        this.rarity = rarity;
        this.effectName = effectName;
        this.effectDescription = effectDescription;
        this.description = description;
    }

    /** Set armor pertama: baja obsidian gelap dengan aksen ungu. Dijual Dark Merchant di Tomb Domain. */
    public static final ArmorSet DREAD = new ArmorSet(
            "Dread Armor", "dread", 5, 10,
            Rarity.RARE, "Dread Guardian",
            "Hits taken summon a shadow (max " + DreadGuardian.MAX_SHADOWS
          + ") that strikes for 50% of your attack.",
            "A complete set of obsidian plate: horned helm, cuirass, "
          + "gauntlets and greaves in one piece. Violet runes glow "
          + "faintly in the dark.");

    /** Set armor kedua: besi polos (full iron). Dijual Armor Merchant di dekat rumah spawn, bayar pakai Coin. */
    public static final ArmorSet IRON = new ArmorSet(
            "Iron Armor", "iron", 3, 5,
            Rarity.COMMON, "Iron Will", ArmorSetEffects.ironWillText(),
            "A complete set of plain iron plate: closed helm, "
          + "cuirass, gauntlets and greaves in one piece. "
          + "Heavy, dependable and forged for the road.");

    /** Set armor ketiga: baja arang hitam, mantel + jambul merah tua, mata merah menyala. Dijual Dark Merchant di Tomb Domain. */
    public static final ArmorSet UMBRAL = new ArmorSet(
            "Umbral Armor", "umbral", 7, 12,
            Rarity.RARE, "Ember Verdict", ArmorSetEffects.emberVerdictText(),
            "A complete set of blackened steel plate: plumed helm, "
          + "cuirass, gauntlets and greaves in one piece. Red "
          + "embers glow behind the visor.");

    private static final ArmorSet[] ALL = { DREAD, IRON, UMBRAL };

    /** Cari set dari nama item (tidak peka huruf besar/kecil). null kalau item itu bukan armor. */
    public static ArmorSet byItemName(String name) {
        if (name == null) return null;
        for (ArmorSet set : ALL) {
            if (set.itemName.equalsIgnoreCase(name)) return set;
        }
        return null;
    }
}
