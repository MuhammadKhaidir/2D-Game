package Item;

import Entitas.PlayerBuffs;
import Entitas.PlayerBuffs.Buff;

/**
 * Semua potion di game dalam satu tabel. Mau ubah harga, jumlah heal, nama, atau nama file gambar?
 * Cukup edit baris di sini. Kekuatan & durasi boost-nya ada di PlayerBuffs.
 *
 * GAMBAR: ditaruh di  src/drop/<imageFile>  (mis. src/drop/smallHpPotion.png).
 * Kalau filenya belum ada, game otomatis pakai ikon botol bawaan (digambar lewat kode,
 * warnanya ikut liquidColor), jadi aman dites dulu sebelum gambarnya jadi.
 *
 * HARGA: dalam Coin. Saat ini setiap monster cuma ngasih 1 coin (Entity.coinDrop),
 * jadi harga di bawah = kira-kira jumlah monster yang harus dikalahkan.
 */
public enum PotionType {

    //             nama item            file gambar          harga  heal  boost                  warna cairan
    SMALL_HP     ("Small HP Potion",    "smallHpPotion.png",    5,   50, null,                  0xD7263D),
    MEDIUM_HP    ("Medium HP Potion",   "mediumHpPotion.png",  12,  150, null,                  0xE0457B),
    DAMAGE       ("Damage Potion",      "dmgPotion.png",       25,    0, Buff.DAMAGE,           0xF28C28),
    SPEED        ("Speed Potion",       "speedPotion.png",     20,    0, Buff.SPEED,            0x35C7E8),
    ATTACK_SPEED ("Attack Speed Potion","attSpdPotion.png",    25,    0, Buff.ATTACK_SPEED,     0xB46CF0),
    MAX_HP       ("Max HP Potion",      "hpPotion.png",        30,    0, Buff.MAX_HP,           0x3DCB6B);

    public final String itemName;
    public final String imageFile;
    public final int price;
    public final int heal;         // HP yang dipulihkan (potion HP biasa)
    public final Buff buff;        // null = potion HP biasa, selain itu = boost potion
    public final int liquidColor;  // 0xRRGGBB, buat ikon bawaan

    PotionType(String itemName, String imageFile, int price, int heal, Buff buff, int liquidColor) {
        this.itemName = itemName;
        this.imageFile = imageFile;
        this.price = price;
        this.heal = heal;
        this.buff = buff;
        this.liquidColor = liquidColor;
    }

    public boolean isHeal() {
        return buff == null;
    }

    /** Satu baris singkat buat toko, mis. "Restores 50 HP" / "+50% damage for 2:00". */
    public String shortDescription() {
        if (isHeal()) return "Restores " + heal + " HP";
        return buff.effectText() + " for " + PlayerBuffs.formatTime(PlayerBuffs.DURATION_SECONDS);
    }

    /** Deskripsi panjang buat panel detail backpack. */
    public String longDescription() {
        String effect = isHeal()
            ? "restores " + heal + " HP"
            : "grants " + buff.effectText() + " for " + (PlayerBuffs.DURATION_SECONDS / 60) + " minutes";
        return "A potion that " + effect + ". Put it in the hotbar and press its number key to drink it.";
    }

    public static PotionType fromItemName(String name) {
        for (PotionType type : values()) {
            if (type.itemName.equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}
