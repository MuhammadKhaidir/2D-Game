package Item;

import Item.DeathTotem.CapturedType;

/**
 * Mewakili satu mob yang tersimpan di dalam Death Totem.
 * Nyimpen tipe, HP saat ini, level, dan akumulasi EXP.
 */
public class CapturedMob {

    public CapturedType type;
    public int storedHP;
    public int maxHP;
    public int level    = 1;
    public int currentExp = 0;

    // Base stats (dari level 1, sebelum scaling)
    public final int baseMaxHP;
    public final int baseAttack;

    // EXP yang dibutuhkan untuk naik dari level ini ke level+1
    // Skeleton: +30/level, Golemite: +400/level, Golem: +1000/level
    public final int expPerLevel;

    private static final int MAX_LEVEL = 100;

    public CapturedMob(CapturedType type) {
        this.type = type;
        switch (type) {
            case GOLEM:
                this.baseMaxHP    = 4000;
                this.baseAttack   = 200;
                this.expPerLevel  = 1000;
                break;
            case GOLEMITE:
                this.baseMaxHP    = Tamed.GolemiteMinion.MAX_LIFE;
                this.baseAttack   = 80;
                this.expPerLevel  = 400;
                break;
            case SKELETON:
            default:
                this.baseMaxHP    = Tamed.UndeadMinion.MAX_LIFE;
                this.baseAttack   = 15;
                this.expPerLevel  = 30;
                break;
        }
        this.maxHP    = baseMaxHP;
        this.storedHP = baseMaxHP;
    }

    /** Terima EXP, naik level kalau cukup. Return true kalau naik level. */
    public boolean addExp(int exp) {
        if (level >= MAX_LEVEL) return false;
        currentExp += exp;
        boolean leveled = false;
        while (level < MAX_LEVEL && currentExp >= expNeededForNextLevel()) {
            currentExp -= expNeededForNextLevel();
            level++;
            leveled = true;
            applyLevelStats();
        }
        return leveled;
    }

    /** EXP yang dibutuhkan untuk naik ke level berikutnya dari level sekarang. */
    public int expNeededForNextLevel() {
        return expPerLevel * level; // level 1→2: 1x, level 2→3: 2x, dst
    }

    /** Terapkan scaling stat berdasarkan level saat ini. */
    private void applyLevelStats() {
        // HP: +1% dari baseMaxHP per level di atas 1
        maxHP = (int)(baseMaxHP * (1.0 + 0.01 * (level - 1)));
        // HP saat ini ikut naik proporsional (biar ga tiba-tiba "sakit" pas level up)
        storedHP = Math.min(storedHP + (int)(baseMaxHP * 0.01), maxHP);
    }

    public int getCurrentAttack() {
        // ATK: +5% dari baseAttack per level di atas 1
        return (int)(baseAttack * (1.0 + 0.05 * (level - 1)));
    }

    public int getMaxLevel() { return MAX_LEVEL; }

    public String getTypeName() {
        switch (type) {
            case GOLEM:    return "Golem";
            case GOLEMITE: return "Golemite";
            case SKELETON: return "Skeleton";
            default:       return "Unknown";
        }
    }
}