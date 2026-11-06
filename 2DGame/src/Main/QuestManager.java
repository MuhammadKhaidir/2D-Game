package Main;

import Entitas.Entity;
import Item.ItemManager;

/**
 * Quest & Bounty Board (versi sederhana).
 *
 * - Ada 3 bounty tetap (lihat QUESTS). Pemain cuma boleh pegang SATU misi dalam satu waktu.
 * - Progress nambah otomatis lewat onKill(), yang dipanggil dari player.giveCoinReward(), titik yang SAMA dengan koin.
 *   Jadi semua kill monster yang ngasih koin ikut kehitung, termasuk yang dibunuh minion (Golem / Golemite / pion
 *   Skeleton). Pion summonan boss (noDrop) sudah disaring di giveCoinReward, jadi gak bisa di-farm.
 * - Misi selesai kalau progress == goal. Reward (Coin) diklaim lewat BountyBoardUI, setelah itu misi kosong lagi dan
 *   boleh diambil ulang (misi yang sama atau yang lain).
 * - Disimpan lewat SaveData / SaveManager (questActive + questProgress).
 */
public class QuestManager {

    /** Satu bounty: apa yang diburu, berapa ekor, dan hadiahnya. */
    public static final class Quest {
        public final String targetName;                      // nama monster buat ditampilkan
        public final Class<? extends Entity> targetClass;    // kelas monster yang dihitung
        public final int goal;                               // jumlah kill yang dibutuhkan
        public final int reward;                             // hadiah dalam Coin

        Quest(String targetName, Class<? extends Entity> targetClass, int goal, int reward) {
            this.targetName = targetName;
            this.targetClass = targetClass;
            this.goal = goal;
            this.reward = reward;
        }

        public String title() {
            return "Kill " + goal + " " + targetName;
        }
    }

    /** Daftar bounty. Mau ganti target / jumlah / hadiah? Cukup ubah di sini. */
    public static final Quest[] QUESTS = {
        new Quest("Slime",    Entitas.monster.class,  10, 30),
        new Quest("Skeleton", Entitas.Skeleton.class,  5, 50),
        new Quest("Wolf",     Entitas.Wolf.class,      3, 40),
    };

    /** Index misi yang lagi diambil, -1 = gak ada misi aktif. */
    private int active = -1;
    private int progress = 0;

    public int getActive()   { return active; }
    public int getProgress() { return progress; }

    public boolean hasActive() { return active >= 0; }

    /** Misi yang lagi diambil sudah selesai dan hadiahnya belum diklaim? */
    public boolean isReady() {
        return active >= 0 && progress >= QUESTS[active].goal;
    }

    /** Ambil misi. Gagal (false) kalau masih ada misi aktif atau index salah. */
    public boolean accept(int index) {
        if (active >= 0) return false;
        if (index < 0 || index >= QUESTS.length) return false;
        active = index;
        progress = 0;
        return true;
    }

    /** Klaim hadiah misi yang sudah selesai. Return jumlah Coin yang didapat, 0 kalau belum bisa diklaim. */
    public int claim() {
        if (!isReady()) return 0;
        int reward = QUESTS[active].reward;
        ItemManager im = ItemManager.get();
        im.add(im.COIN, reward);
        active = -1;      // misi kosong lagi -> boleh diambil ulang
        progress = 0;
        return reward;
    }

    /** Dipanggil player.giveCoinReward() tiap monster mati. Progress cuma nambah kalau targetnya cocok. */
    public void onKill(Entity killed) {
        if (active < 0 || killed == null) return;
        Quest q = QUESTS[active];
        if (progress >= q.goal) return;               // sudah selesai, tinggal klaim
        if (q.targetClass.isInstance(killed)) progress++;
    }

    // ── Simpan / muat (dipakai SaveManager) ──

    /** Pulihkan misi dari save. Index di luar daftar (misal -1 dari save lama) = gak ada misi aktif. Progress dijaga 0..goal. */
    public void restore(int savedActive, int savedProgress) {
        if (savedActive < 0 || savedActive >= QUESTS.length) {
            active = -1;
            progress = 0;
            return;
        }
        active = savedActive;
        progress = Math.max(0, Math.min(savedProgress, QUESTS[savedActive].goal));
    }
}
