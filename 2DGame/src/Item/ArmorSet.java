package Item;

/**
 * ArmorSet - data satu SET armor utuh (helm + badan + sarung tangan + celana + sepatu jadi SATU item).
 *
 * Satu set = satu Item di inventory, satu slot "ARMOR" di backpack, dan satu folder sprite di
 * /Aset/armor/&lt;assetFolder&gt;/ (di-render oleh {@link Object.PlayerArmor}).
 *
 * Nambah set baru: 1) bikin sprite-nya (lihat tools/generate_dread_armor.py),
 * 2) daftarin satu konstanta di sini + masukin ke ALL, 3) bikin Item-nya di ItemManager,
 * 4) jual lewat NPC (DarkMerchant.shopItems + DomainManager.handleShopBuy).
 */
public final class ArmorSet {

    /** Nama item di inventory / save file / shop. */
    public final String itemName;
    /** Folder sprite di src/Aset/armor/. */
    public final String assetFolder;
    /** Pengurang damage flat (ditambah ke stat DEF player). */
    public final int defense;
    /** Pengurang damage persen, dihitung setelah defense (0-100). */
    public final int damageReduction;
    public final String description;

    private ArmorSet(String itemName, String assetFolder, int defense, int damageReduction, String description) {
        this.itemName = itemName;
        this.assetFolder = assetFolder;
        this.defense = defense;
        this.damageReduction = damageReduction;
        this.description = description;
    }

    /** Set armor pertama: baja obsidian gelap dengan aksen ungu. Dijual Dark Merchant di Tomb Domain. */
    public static final ArmorSet DREAD = new ArmorSet(
            "Dread Armor", "dread", 5, 10,
            "A complete set of obsidian plate: horned helm, cuirass, "
          + "gauntlets and greaves in one piece. Violet runes glow "
          + "faintly in the dark.");

    /** Set armor kedua: besi polos (full iron). Dijual Armor Merchant di dekat rumah spawn, bayar pakai Coin. */
    public static final ArmorSet IRON = new ArmorSet(
            "Iron Armor", "iron", 3, 5,
            "A complete set of plain iron plate: closed helm, "
          + "cuirass, gauntlets and greaves in one piece. "
          + "Heavy, dependable and forged for the road.");

    private static final ArmorSet[] ALL = { DREAD, IRON };

    /** Cari set dari nama item (tidak peka huruf besar/kecil). null kalau item itu bukan armor. */
    public static ArmorSet byItemName(String name) {
        if (name == null) return null;
        for (ArmorSet set : ALL) {
            if (set.itemName.equalsIgnoreCase(name)) return set;
        }
        return null;
    }
}
