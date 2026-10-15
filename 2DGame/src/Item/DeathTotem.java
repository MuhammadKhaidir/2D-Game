package Item;

import Item.DeathTotem.CapturedType;
import Main.GamePanel;
import Tamed.GolemMinion;
import Tamed.GolemiteMinion;
import Tamed.UndeadMinion;

import java.util.ArrayList;
import java.util.List;

/**
 * DeathTotem — multi-slot, tiap slot nyimpen CapturedMob dengan level & EXP sendiri.
 */
public class DeathTotem extends SuperItem {

    public enum CapturedType { EMPTY, SKELETON, GOLEM, GOLEMITE }

    // ── Kapasitas & slot ─────────────────────────────────────────────────────
    private int capacity = 1; // default 1, bisa di-upgrade
    private static final int MAX_CAPACITY = 6;

    private final List<CapturedMob> slots = new ArrayList<>();

    // ── Pion aktif (masih satu pion aktif sekaligus, tapi bisa dari slot manapun) ──
    private int       activePawnSlot    = -1; // index slot yang lagi di-spawn
    private UndeadMinion activePawn     = null;
    private GolemMinion  activeGolemPawn = null;
    private GolemiteMinion activeGolemitePawn = null;

    // ── Biaya upgrade kapasitas per tier ─────────────────────────────────────
    // Index = kapasitas setelah upgrade (2..6)
    // { gold, bone, skull, skeletonKey }
    private static final int[][] UPGRADE_COSTS = {
        {},                          // index 0 — unused
        {},                          // index 1 — kapasitas 1, default, ga perlu beli
        { 500,  20,  5, 1 },         // upgrade ke kapasitas 2
        { 1500, 50, 15, 2 },         // upgrade ke kapasitas 3
        { 3000, 100, 30, 3 },        // upgrade ke kapasitas 4
        { 6000, 200, 60, 5 },        // upgrade ke kapasitas 5
        { 12000, 400, 120, 8 },      // upgrade ke kapasitas 6
    };

    public DeathTotem() {
        super("Death Totem",
              "Capture enemy souls and revive them as your minions.",
              "/Aset/DeathTotem.png");
    }

    // ── Kapasitas ─────────────────────────────────────────────────────────────

    public int getCapacity()    { return capacity; }
    public int getSlotCount()   { return slots.size(); }
    public int getMaxCapacity() { return MAX_CAPACITY; }
    public boolean isFull()     { return slots.size() >= capacity; }
    public boolean isEmpty()    { return slots.isEmpty(); }
    public List<CapturedMob> getSlots() { return slots; }

    /**
     * Cek apakah bisa upgrade & cukup resource.
     * Return array biaya { gold, bone, skull, skeletonKey } untuk upgrade tier berikutnya,
     * atau null kalau sudah max / belum bisa.
     */
    public int[] getNextUpgradeCost() {
        int next = capacity + 1;
        if (next > MAX_CAPACITY) return null;
        return UPGRADE_COSTS[next];
    }

    /**
     * Lakukan upgrade kapasitas. Kurangi resource dari inventory.
     * Return true kalau berhasil.
     */
    public boolean tryUpgrade(GamePanel gp) {
        int[] cost = getNextUpgradeCost();
        if (cost == null) return false; // sudah max

        ItemManager im = ItemManager.get();
        int goldAmt  = getInventoryAmount(im.COIN);
        int boneAmt  = getInventoryAmount(im.BONE);
        int skullAmt = getInventoryAmount(im.SKULL);
        int keyAmt   = getInventoryAmount(im.SKELETON_KEY);

        if (goldAmt < cost[0] || boneAmt < cost[1] || skullAmt < cost[2] || keyAmt < cost[3]) {
            return false; // resource kurang
        }

        // Kurangi resource
        removeFromInventory(im.COIN,         cost[0]);
        removeFromInventory(im.BONE,         cost[1]);
        removeFromInventory(im.SKULL,        cost[2]);
        removeFromInventory(im.SKELETON_KEY, cost[3]);

        capacity++;
        return true;
    }

    private int getInventoryAmount(Item item) {
        for (ItemStack s : ItemManager.inventory) {
            if (s != null && s.item == item) return s.amount;
        }
        return 0;
    }

    private void removeFromInventory(Item item, int amount) {
        for (ItemStack s : ItemManager.inventory) {
            if (s != null && s.item == item) {
                s.amount -= amount;
                if (s.amount <= 0) ItemManager.inventory.remove(s);
                return;
            }
        }
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    public boolean canCapture() {
        return slots.size() < capacity;
    }

    /**
     * Capture mob baru ke slot kosong.
     * Return true kalau berhasil.
     */
    public boolean capture(CapturedType type) {
        if (!canCapture()) return false;
        if (type == CapturedType.EMPTY) return false;
        slots.add(new CapturedMob(type));
        return true;
    }

    // ── Pion aktif ────────────────────────────────────────────────────────────

    public boolean hasPawnAlive() {
        if (activePawn     != null && activePawn.isAlive())     return true;
        if (activeGolemPawn != null && activeGolemPawn.isAlive()) return true;
        if (activeGolemitePawn != null && activeGolemitePawn.isAlive()) return true;
        return false;
    }

    public int getActivePawnSlot() { return activePawnSlot; }

    /**
     * Spawn pion dari slot tertentu.
     */
    public Object spawnFromSlot(int slotIndex, GamePanel gp, int spawnX, int spawnY) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return null;
        if (hasPawnAlive()) return null;

        CapturedMob mob = slots.get(slotIndex);

        // Kalau HP simpanan 0, spawn dengan HP minimal 1 (biar animasi spawn keliatan)
        // tapi ini seharusnya ga terjadi lagi setelah releasePawn() hapus slot
        if (mob.storedHP <= 0) mob.storedHP = 1;

        activePawnSlot = slotIndex;

        if (mob.type == CapturedType.GOLEM) {
            GolemMinion pawn = new GolemMinion(gp, this);
            pawn.x = spawnX; pawn.y = spawnY;
            pawn.spawnX = spawnX; pawn.spawnY = spawnY;
            pawn.maxLife = mob.maxHP;
            pawn.life    = mob.storedHP;
            activeGolemPawn = pawn;
            return pawn;
        } else if (mob.type == CapturedType.GOLEMITE) {
            GolemiteMinion pawn = new GolemiteMinion(gp, this);
            pawn.x = spawnX; pawn.y = spawnY;
            pawn.spawnX = spawnX; pawn.spawnY = spawnY;
            pawn.maxLife = mob.maxHP;
            pawn.life    = mob.storedHP;
            activeGolemitePawn = pawn;
            return pawn;
        } else {
            UndeadMinion pawn = new UndeadMinion(gp, mob.type, this);
            pawn.x = spawnX; pawn.y = spawnY;
            pawn.spawnX = spawnX; pawn.spawnY = spawnY;
            pawn.maxLife = mob.maxHP;
            pawn.life    = mob.storedHP;
            activePawn = pawn;
            return pawn;
        }
    }

   /*
     * Simpan HP pion sebelum di-recall/mati.
     */
    public void saveActivePawnHP(int currentHP) {
        if (activePawnSlot >= 0 && activePawnSlot < slots.size()) {
            CapturedMob mob = slots.get(activePawnSlot);
            mob.storedHP = Math.max(0, Math.min(currentHP, mob.maxHP));
        }
    }

    /**
     * Distribusi EXP ke SEMUA mob tersimpan di totem (bukan cuma yang aktif).
     * Dipanggil tiap kali pion aktif bunuh musuh.
     * Return list nama mob yang naik level (buat notif).
     */
    public List<String> distributeExp(int exp) {
        List<String> leveledUp = new ArrayList<>();
        for (CapturedMob mob : slots) {
            boolean leveled = mob.addExp(exp);
            if (leveled) leveledUp.add(mob.getTypeName() + " (Lv." + mob.level + ")");
        }
        return leveledUp;
    }

    /**
     * Recall pion — lepas referensi pion aktif, HP tersimpan.
     */
    public void recallPawn(int currentHP) {
        saveActivePawnHP(currentHP);
        activePawn      = null;
        activeGolemPawn = null;
        activeGolemitePawn = null;
        activePawnSlot  = -1;
    }

    /**
     * Pion mati — slot tetap ada (jiwa masih tersimpan, HP = 0 / min).
     */
    public void releasePawn() {
        // Pion mati permanen → hapus slot dari totem, harus capture ulang
        if (activePawnSlot >= 0 && activePawnSlot < slots.size()) {
            slots.remove(activePawnSlot);
        }
        activePawn      = null;
        activeGolemPawn = null;
        activeGolemitePawn = null;
        activePawnSlot  = -1;
    }

    public UndeadMinion getActivePawn()     { return activePawn; }
    public GolemMinion  getActiveGolemPawn(){ return activeGolemPawn; }
    public GolemiteMinion getActiveGolemitePawn(){ return activeGolemitePawn; }

    // ── Restore (load game) ───────────────────────────────────────────────────

    public void restoreState(List<CapturedMob> savedSlots, int savedCapacity) {
        slots.clear();
        slots.addAll(savedSlots);
        this.capacity    = Math.max(1, Math.min(savedCapacity, MAX_CAPACITY));
        activePawn       = null;
        activeGolemPawn  = null;
        activeGolemitePawn = null;
        activePawnSlot   = -1;
    }
    public void removeSlot(int index) {
        if (index < 0 || index >= slots.size()) return;
        // Kalau yang dihapus adalah active pawn, recall dulu
        if (activePawnSlot == index) {
            recallPawn(index);       // ganti dengan nama method recall milikmu
            activePawnSlot = -1;
        } else if (activePawnSlot > index) {
            activePawnSlot--;   // shift karena list bergeser
        }
        slots.remove(index);
    }
    @Override
    public String getStatusDescription() {
        if (isEmpty())      return "Empty — defeat enemies to fill";
        if (hasPawnAlive()) return "Active pawn: slot " + (activePawnSlot+1);
        return "Souls stored: " + slots.size() + "/" + capacity;
    }
}