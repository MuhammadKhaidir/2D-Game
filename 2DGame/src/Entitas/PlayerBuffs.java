package Entitas;

/**
 * Buff sementara buat player (dari boost potion).
 *
 * Kekuatan & durasi semua buff diatur di konstanta paling atas — ubah di situ kalau mau di-balance.
 * Timer jalan per frame di player.update(), jadi otomatis ikut berhenti kalau game lagi
 * di-pause (dialog, shop, dll). Minum potion yang sama pas buff-nya masih aktif = timer direset ke penuh
 * (efeknya tidak menumpuk).
 */
public class PlayerBuffs {

    // ===== KEKUATAN BUFF =====
    public static final int    DURATION_SECONDS        = 120;  // 2 menit, sama buat semua boost potion
    public static final double DAMAGE_MULTIPLIER       = 1.5;  // +50% damage
    public static final double SPEED_MULTIPLIER        = 1.3;  // +30% kecepatan jalan (& lari)
    public static final double ATTACK_SPEED_MULTIPLIER = 1.5;  // +50% kecepatan animasi serangan
    public static final int    MAX_HP_BONUS            = 50;   // +50 max HP (HP sekarang ikut naik segitu)

    public enum Buff {
        DAMAGE("Damage Boost"),
        SPEED("Speed Boost"),
        ATTACK_SPEED("Attack Speed Boost"),
        MAX_HP("Max HP Boost");

        public final String label;

        Buff(String label) {
            this.label = label;
        }

        /** Teks efek singkat buat deskripsi item, mis. "+50% damage". Ikut berubah kalau konstanta di atas diubah. */
        public String effectText() {
            switch (this) {
                case DAMAGE:       return "+" + percent(DAMAGE_MULTIPLIER) + "% damage";
                case SPEED:        return "+" + percent(SPEED_MULTIPLIER) + "% move speed";
                case ATTACK_SPEED: return "+" + percent(ATTACK_SPEED_MULTIPLIER) + "% attack speed";
                default:           return "+" + MAX_HP_BONUS + " max HP";
            }
        }

        private static long percent(double multiplier) {
            return Math.round((multiplier - 1.0) * 100);
        }
    }

    private final player owner;
    private final int[] framesLeft = new int[Buff.values().length];

    private int appliedMaxHp = 0;            // bonus max HP yang SAAT INI lagi nempel di owner.maxLife
    private double attackTickBank = 0;       // penampung "tick bonus" animasi serangan (buat buff attack speed)

    public PlayerBuffs(player owner) {
        this.owner = owner;
    }

    // ===================================================================
    // AKTIFKAN / UPDATE / HAPUS
    // ===================================================================

    public void activate(Buff buff, int frames) {
        framesLeft[buff.ordinal()] = frames;

        if (buff == Buff.MAX_HP && appliedMaxHp == 0) {
            appliedMaxHp = MAX_HP_BONUS;
            owner.maxLife += appliedMaxHp;
            owner.life += appliedMaxHp;      // HP ikut naik, biar bonusnya kerasa langsung
        }
    }

    /** Dipanggil player.update() tiap frame. */
    public void update() {
        for (Buff buff : Buff.values()) {
            int i = buff.ordinal();
            if (framesLeft[i] > 0 && --framesLeft[i] == 0) {
                onExpire(buff);
            }
        }
    }

    /** Matikan semua buff sekarang (dipanggil pas mati / load save). */
    public void clear() {
        for (Buff buff : Buff.values()) {
            if (framesLeft[buff.ordinal()] > 0) {
                framesLeft[buff.ordinal()] = 0;
                onExpire(buff);
            }
        }
        removeMaxHpBonus();
        attackTickBank = 0;
    }

    private void onExpire(Buff buff) {
        if (buff == Buff.MAX_HP) removeMaxHpBonus();
        if (buff == Buff.ATTACK_SPEED) attackTickBank = 0;
    }

    private void removeMaxHpBonus() {
        if (appliedMaxHp == 0) return;
        owner.maxLife -= appliedMaxHp;
        appliedMaxHp = 0;
        owner.life = Math.min(owner.life, owner.maxLife);
    }

    // ===================================================================
    // STATUS
    // ===================================================================

    public boolean isActive(Buff buff) {
        return framesLeft[buff.ordinal()] > 0;
    }

    public int secondsLeft(Buff buff) {
        return (int) Math.ceil(framesLeft[buff.ordinal()] / (double) owner.gp.FPS);
    }

    /** Bonus max HP yang lagi aktif (0 kalau tidak ada). Dipakai save & stat UI supaya buff tidak ikut "permanen". */
    public int getMaxHpBonus() {
        return appliedMaxHp;
    }

    /** "2:00", "0:45", dst. */
    public static String formatTime(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // ===================================================================
    // EFEK KE STAT PLAYER
    // ===================================================================

    public int scaleDamage(int damage) {
        return isActive(Buff.DAMAGE) ? (int) Math.round(damage * DAMAGE_MULTIPLIER) : damage;
    }

    public double scaleSpeed(double speed) {
        return isActive(Buff.SPEED) ? speed * SPEED_MULTIPLIER : speed;
    }

    /**
     * Dipanggil tiap frame selama animasi serangan. True = frame ini animasinya dapat 1 tick ekstra
     * (jadi serangan selesai lebih cepat). Pecahan multiplier dikumpulin, jadi 1.5x = tick ekstra tiap 2 frame.
     */
    public boolean consumeExtraAttackTick() {
        if (!isActive(Buff.ATTACK_SPEED)) return false;
        attackTickBank += ATTACK_SPEED_MULTIPLIER - 1.0;
        if (attackTickBank >= 1.0) {
            attackTickBank -= 1.0;
            return true;
        }
        return false;
    }
}
