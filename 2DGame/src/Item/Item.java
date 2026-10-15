package Item;

import java.awt.image.BufferedImage;

public class Item {
    public String name;
    public BufferedImage icon;
    public int maxStack = 99;

    /**
     * Efek pas item dipakai (lewat hotbar). Kalau null, berarti item ini bukan consumable.
     * Return true = berhasil dipakai (jumlah item berkurang 1),
     * return false = gagal / gak perlu dipakai (jumlah item TIDAK berkurang).
     * Pesan ke player (mis. "HP is already full.") ditampilkan sendiri oleh efeknya.
     */
    public interface UseEffect {
        boolean use(Main.GamePanel gp);
    }
    public UseEffect onUse = null;

    public Item(String name, BufferedImage icon) {
        this.name = name;
        this.icon = icon;
    }
}
