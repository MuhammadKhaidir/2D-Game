package Item;

import Entitas.Entity;
import Main.GamePanel;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * ArmorSetEffects - set effect Iron Armor (COMMON) dan Umbral Armor (RARE).
 * (Dread Armor punya efek sendiri: lihat {@link DreadGuardian}.)
 *
 * <b>Umbral Armor - Ember Verdict</b> (RARE, armor ofensif: nyala dari kill beruntun)
 *  - Tiap monster mati = +1 Ember (maks {@link #EMBER_MAX}). Semua Ember bertahan {@link #EMBER_SECONDS} detik sejak
 *    kill terakhir (timer direset penuh tiap kill), habis waktu = semua Ember padam.
 *  - Tiap Ember: +5% damage (semua damage player: pukulan, skill, bayangan). Maks +25%.
 *  - Pas Ember penuh (Verdict): +10% kecepatan gerak.
 *  - Ember dihitung dari titik yang sama dengan koin (player.giveCoinReward), jadi cuma kill "beneran":
 *    pion summonan boss (noDrop) gak dihitung. Timer berhenti selama di dalam rumah / world di-freeze.
 *
 * <b>Iron Armor - Iron Will</b> (COMMON, efek sederhana, tahan banting)
 *  - Pas HP player sudah di bawah {@link #IRON_WILL_HP_THRESHOLD} (30%) max HP, damage yang masuk
 *    dikurangi {@link #IRON_WILL_REDUCTION} (15%) lagi, dihitung setelah DEF dan pengurang persen armor.
 *  - Yang dicek HP SEBELUM kena hit, jadi hit yang menjatuhkan HP ke bawah 30% belum dikurangi.
 *
 * Semua efek cuma aktif selama set-nya dipakai, dan state-nya otomatis hilang kalau armor dilepas / player mati.
 * Efek ini BUKAN Entity dan gak nyentuh collision / AI musuh: cuma mengubah angka lewat hook kecil di
 * player (getTotalDamage, kecepatan, takeDamageFrom, giveCoinReward) + gambar kecil di GamePanel.
 * Angka di bawah tinggal diubah buat balancing, teks deskripsi di UI ikut berubah otomatis.
 */
public class ArmorSetEffects {

    // ===== UMBRAL: EMBER VERDICT =====
    public static final int    EMBER_MAX              = 5;
    public static final int    EMBER_SECONDS          = 12;
    public static final double EMBER_DAMAGE_PER_STACK = 0.05;  // +5% damage per Ember
    public static final double EMBER_FULL_SPEED_BONUS = 0.10;  // +10% speed pas Ember penuh

    // ===== IRON: IRON WILL =====
    public static final double IRON_WILL_HP_THRESHOLD = 0.30;  // aktif kalau HP <= 30% max HP
    public static final double IRON_WILL_REDUCTION    = 0.15;  // damage masuk -15%

    private static final int FLASH_TICKS = 12;                 // ember baru nyala lebih terang sebentar

    private final GamePanel gp;

    private int embers = 0;        // Ember yang lagi nyala
    private int emberTicks = 0;    // sisa waktu (tick) sebelum semua Ember padam
    private int gainFlash = 0;     // > 0 = baru dapat Ember
    private int animTick = 0;      // buat animasi

    public ArmorSetEffects(GamePanel gp) {
        this.gp = gp;
    }

    // ================= TEKS DESKRIPSI (dipakai ArmorSet) =================

    public static String emberVerdictText() {
        return "Kills light Embers (max " + EMBER_MAX + ", " + EMBER_SECONDS + "s): +"
             + percent(EMBER_DAMAGE_PER_STACK) + "% damage each, +"
             + percent(EMBER_FULL_SPEED_BONUS) + "% speed at " + EMBER_MAX + ".";
    }

    public static String ironWillText() {
        return "Below " + percent(IRON_WILL_HP_THRESHOLD) + "% HP, take "
             + percent(IRON_WILL_REDUCTION) + "% less damage.";
    }

    private static long percent(double ratio) {
        return Math.round(ratio * 100);
    }

    // ================= HOOK DARI PLAYER =================

    /** Dipanggil player.giveCoinReward(): tiap kill monster beneran (pion boss noDrop sudah disaring di sana). */
    public void onKill(Entity target) {
        if (!isWearing(ArmorSet.UMBRAL) || gp.player.isDead) return;
        embers = Math.min(EMBER_MAX, embers + 1);
        emberTicks = EMBER_SECONDS * gp.FPS;
        gainFlash = FLASH_TICKS;
    }

    /** Pengali damage player dari set yang dipakai (1.0 = gak ada bonus). Dipakai player.getTotalDamage(). */
    public double getDamageMultiplier() {
        if (!isWearing(ArmorSet.UMBRAL)) return 1.0;
        return 1.0 + embers * EMBER_DAMAGE_PER_STACK;
    }

    /** Pengali kecepatan jalan player dari set yang dipakai. Dipakai player.update(). */
    public double getSpeedMultiplier() {
        if (isWearing(ArmorSet.UMBRAL) && embers >= EMBER_MAX) return 1.0 + EMBER_FULL_SPEED_BONUS;
        return 1.0;
    }

    /** Pengali damage yang MASUK ke player dari set yang dipakai (1.0 = normal). Dipakai player.takeDamageFrom(). */
    public double getIncomingDamageMultiplier() {
        if (isWearing(ArmorSet.IRON) && isIronWillActive()) return 1.0 - IRON_WILL_REDUCTION;
        return 1.0;
    }

    public boolean isIronWillActive() {
        return gp.player.life > 0 && gp.player.life <= gp.player.maxLife * IRON_WILL_HP_THRESHOLD;
    }

    public int getEmbers() {
        return isWearing(ArmorSet.UMBRAL) ? embers : 0;
    }

    public void clear() {
        embers = 0;
        emberTicks = 0;
        gainFlash = 0;
    }

    // ================= UPDATE =================

    /** Dipanggil GamePanel.update() tiap frame (berhenti kalau world di-freeze, sama kayak Dread Guardian). */
    public void update() {
        animTick++;
        if (gainFlash > 0) gainFlash--;

        if (gp.player == null || gp.player.isDead || !isWearing(ArmorSet.UMBRAL)) {
            if (embers != 0 || emberTicks != 0) clear();   // armor dilepas / ganti / mati: Ember padam
            return;
        }
        if (embers > 0 && inCombatArea()) {
            if (--emberTicks <= 0) clear();
        }
    }

    // ================= STATE =================

    private boolean isWearing(ArmorSet set) {
        return gp.player != null && gp.player.getArmorSet() == set;
    }

    private boolean inCombatArea() {
        return gp.gameState == GamePanel.GameState.OUTSIDE || gp.gameState == GamePanel.GameState.DOMAIN;
    }

    // ================= DRAW =================

    /** Dipanggil GamePanel di dalam transform zoom dunia (sama kayak dreadGuardian.draw). */
    public void draw(Graphics2D g2) {
        if (gp.player == null || gp.player.isDead || !inCombatArea()) return;

        if (isWearing(ArmorSet.UMBRAL)) {
            if (embers > 0) drawEmbers(g2);
        } else if (isWearing(ArmorSet.IRON)) {
            if (isIronWillActive()) drawIronWill(g2);
        }
    }

    /** Ember orbit pelan di sekeliling badan player, jumlahnya = Ember yang nyala. Kedip di 2 detik terakhir. */
    private void drawEmbers(Graphics2D g2) {
        int tile = gp.tileSize;
        int cx = gp.player.screenX + tile / 2;      // pusat badan player di layar
        int cy = gp.player.screenY + tile * 2 / 3;
        int feetY = gp.player.screenY + tile;

        Composite oldComposite = g2.getComposite();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float fade = 1f;
        if (emberTicks <= 2 * gp.FPS) fade = ((animTick / 4) % 2 == 0) ? 1f : 0.35f;   // mau padam: kedip

        // bara di bawah kaki pas Ember penuh (Verdict)
        if (embers >= EMBER_MAX) {
            double pulse = 0.5 + 0.5 * Math.sin(animTick * 0.12);
            setAlpha(g2, (float) ((0.16 + 0.12 * pulse) * fade));
            g2.setColor(new Color(220, 50, 30));
            int w = (int) (tile * 1.15), h = (int) (tile * 0.40);
            g2.fillOval(cx - w / 2, feetY - h / 2, w, h);
        }

        for (int i = 0; i < embers; i++) {
            double ang = animTick * 0.06 + i * (2 * Math.PI / EMBER_MAX);
            double depth = Math.sin(ang);                                   // > 0 = di depan badan
            double bob = Math.sin(animTick * 0.15 + i * 1.7) * 2.0;
            int ex = (int) Math.round(cx + Math.cos(ang) * tile * 0.62);
            int ey = (int) Math.round(cy - tile * 0.10 + depth * tile * 0.26 + bob);
            double scale = (0.85 + 0.15 * depth) * (gainFlash > 0 ? 1.0 + 0.5 * gainFlash / FLASH_TICKS : 1.0);

            drawEmberDot(g2, ex, ey, 7 * scale, new Color(255, 80, 30), 0.30f * fade);
            drawEmberDot(g2, ex, ey, 4.5 * scale, new Color(255, 140, 45), 0.80f * fade);
            drawEmberDot(g2, ex, ey, 2.2 * scale, new Color(255, 235, 170), 1.0f * fade);
        }

        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawEmberDot(Graphics2D g2, int x, int y, double radius, Color color, float alpha) {
        setAlpha(g2, alpha);
        g2.setColor(color);
        int r = Math.max(1, (int) Math.round(radius));
        g2.fillOval(x - r, y - r, r * 2, r * 2);
    }

    /** Cincin baja berdenyut di kaki player selama Iron Will aktif (HP rendah). */
    private void drawIronWill(Graphics2D g2) {
        int tile = gp.tileSize;
        int cx = gp.player.screenX + tile / 2;
        int feetY = gp.player.screenY + tile;
        int w = (int) (tile * 1.05), h = (int) (tile * 0.38);

        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pulse = 0.5 + 0.5 * Math.sin(animTick * 0.10);
        setAlpha(g2, (float) (0.10 + 0.08 * pulse));
        g2.setColor(new Color(150, 185, 225));
        g2.fillOval(cx - w / 2, feetY - h / 2, w, h);

        setAlpha(g2, (float) (0.45 + 0.30 * pulse));
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(190, 215, 240));
        g2.drawOval(cx - w / 2, feetY - h / 2, w, h);

        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private static void setAlpha(Graphics2D g2, float alpha) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
    }
}
