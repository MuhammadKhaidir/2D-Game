package Main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SaveData implements Serializable {
	private static final long serialVersionUID = 4L;

    public String saveName;
    public long saveTime;

    public int playerX, playerY;
    public int playerLife, playerMaxLife;
    public int playerLevel, playerExp, playerNextLevelExp;
    public int playerStamina;
    public int attributePoints, strength, dexterity;
    public int playerMaxStamina;
    public int punchDamage;
    public double baseSpeed; // samakan tipe datanya dengan yang ada di kelas player
    public int dodge;
    public int def;
 // ── DeathTotem state ──
 // ── DeathTotem state (multi-slot) ──
    public int totemCapacity = 1;
    // Tiap mob disimpan flat: paralel list biar tetap Serializable tanpa dependency ke CapturedMob
    public List<String> totemSlotTypes    = new ArrayList<>(); // "SKELETON" / "GOLEM"
    public List<Integer> totemSlotHP      = new ArrayList<>();
    public List<Integer> totemSlotMaxHP   = new ArrayList<>();
    public List<Integer> totemSlotLevel   = new ArrayList<>();
    public List<Integer> totemSlotExp     = new ArrayList<>();
    public List<Integer> totemSlotBaseHP  = new ArrayList<>();
    public List<Integer> totemSlotBaseAtk = new ArrayList<>();
    public String gameState;
    public String activeDomainId; // domain yang sedang aktif saat save (null kalau di luar domain)

    public List<Integer> removedObjectIndexes = new ArrayList<>();

    // Inventory: simpan nama dan jumlah saja
    public List<String> inventoryNames   = new ArrayList<>();
    public List<Integer> inventoryAmounts = new ArrayList<>();
}