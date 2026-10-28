package Main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import Entitas.Entity;

/**
 * Angka damage melayang: muncul di atas target tiap kali player ngehit sesuatu atau player kena hit.
 *
 * - ENEMY  (kuning) : damage yang masuk ke musuh / NPC / breakable
 * - PLAYER (merah)  : damage yang beneran masuk ke player (setelah defense armor)
 * - SHADOW (ungu)   : damage dari bayangan Dread Guardian
 *
 * Dipanggil dari tiap takeDamage(...) lewat gp.damagePopups.spawn(...).
 * update() dan draw() dipanggil dari GamePanel (sama-sama jalan di EDT, jadi gak perlu sinkronisasi).
 * Digambar di dalam transform zoom dunia, jadi ikut ke-zoom bareng entity.
 */
public class DamagePopup {

    public enum Type { ENEMY, PLAYER, SHADOW }

    // ── Konfigurasi (FPS game = 40) ──
    private static final int    FONT_SIZE     = 12;    // ukuran angka (px) — naikin kalau kekecilan
    private static final float  POP_SCALE     = 0.3f;  // seberapa besar angka pas baru muncul (0.3 = 1.3x lalu mengecil)
    private static final int    LIFETIME      = 40;    // ~1 detik
    private static final int    FADE_START    = 24;    // mulai fade di tick ini
    private static final double RISE_TOTAL    = 26;    // total px naik ke atas selama hidup
    private static final int    JITTER_X      = 10;    // geser acak kiri/kanan biar hit beruntun gak numpuk
    private static final int    MAX_POPUPS    = 60;    // batas aman (AoE / banyak hit sekaligus)
    private static final int    POP_TICKS     = 6;     // durasi efek "pop" (angka membesar lalu mengecil)

    private static final Color COLOR_ENEMY  = new Color(255, 226, 80);
    private static final Color COLOR_PLAYER = new Color(255, 70, 70);
    private static final Color COLOR_SHADOW = new Color(190, 110, 255);

    private static class Popup {
        double wx, wy;          // posisi dunia (tengah atas target)
        int dmg;
        Type type;
        int age;
        GamePanel.GameState state; // popup cuma digambar di state tempat dia muncul
    }

    private final GamePanel gp;
    private final List<Popup> popups = new ArrayList<>();
    private final Random rng = new Random();
    private Type sourceOverride = null;
    private Font baseFont;

    public DamagePopup(GamePanel gp) {
        this.gp = gp;
        this.baseFont = new Font("Arial", Font.BOLD, FONT_SIZE);
    }

    // =====================
    // SPAWN
    // =====================

    /** Munculin angka di atas entity (player, musuh, NPC). Damage <= 0 diabaikan. */
    public void spawn(Entity target, int dmg, Type type) {
        if (target == null || dmg <= 0) return;
        int cx = target.x + target.solidArea.x + target.solidArea.width / 2;
        int top = target.y + target.solidArea.y;
        spawnAt(cx, top, dmg, type);
    }

    /** Munculin angka di koordinat dunia tertentu (dipakai breakable yang punya solidArea sendiri). */
    public void spawnAt(int worldX, int worldY, int dmg, Type type) {
        if (dmg <= 0) return;
        Popup p = new Popup();
        p.wx = worldX + rng.nextInt(JITTER_X * 2 + 1) - JITTER_X;
        p.wy = worldY - 4;
        p.dmg = dmg;
        // Kalau lagi di dalam blok beginSource() (mis. damage bayangan), warna ENEMY diganti sumbernya
        p.type = (type == Type.ENEMY && sourceOverride != null) ? sourceOverride : type;
        p.state = gp.gameState;
        popups.add(p);
        if (popups.size() > MAX_POPUPS) popups.remove(0);
    }

    /**
     * Tandai damage berikutnya berasal dari sumber khusus (mis. SHADOW) sampai endSource().
     * Dipakai DreadGuardian supaya angka damage bayangan warnanya beda.
     */
    public void beginSource(Type source) { sourceOverride = source; }
    public void endSource()              { sourceOverride = null; }

    // =====================
    // UPDATE / DRAW
    // =====================

    public void update() {
        for (Iterator<Popup> it = popups.iterator(); it.hasNext(); ) {
            Popup p = it.next();
            p.age++;
            if (p.age >= LIFETIME) it.remove();
        }
    }

    /** Bersihin semua angka (mis. pas load game / pindah dunia). */
    public void clear() { popups.clear(); }

    /** Panggil di dalam transform zoom dunia (sama kayak dreadGuardian.draw). */
    public void draw(Graphics2D g2) {
        if (popups.isEmpty()) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Composite oldComposite = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (Popup p : popups) {
            if (p.state != gp.gameState) continue;

            double t = p.age / (double) LIFETIME;
            double rise = RISE_TOTAL * (1.0 - (1.0 - t) * (1.0 - t));   // ease-out: cepat di awal, melambat

            int sx = (int) Math.round(p.wx - gp.player.x + gp.player.screenX);
            int sy = (int) Math.round(p.wy - gp.player.y + gp.player.screenY - rise);

            // skip kalau di luar layar
            if (sx < gp.viewMinX() - 40 || sx > gp.viewMaxX() + 40
             || sy < gp.viewMinY() - 40 || sy > gp.viewMaxY() + 40) continue;

            // efek pop: mulai (1 + POP_SCALE)x lalu mengecil ke 1.0x
            float scale = 1.0f;
            if (p.age < POP_TICKS) scale = 1.0f + POP_SCALE * (1.0f - p.age / (float) POP_TICKS);
            Font f = baseFont.deriveFont(baseFont.getSize2D() * scale);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();

            String text = String.valueOf(p.dmg);
            int tx = sx - fm.stringWidth(text) / 2;

            float alpha = 1.0f;
            if (p.age > FADE_START) alpha = Math.max(0f, 1.0f - (p.age - FADE_START) / (float) (LIFETIME - FADE_START));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            // outline hitam biar kebaca di tile apa pun
            g2.setColor(Color.BLACK);
            g2.drawString(text, tx - 1, sy);
            g2.drawString(text, tx + 1, sy);
            g2.drawString(text, tx, sy - 1);
            g2.drawString(text, tx, sy + 1);

            g2.setColor(colorOf(p.type));
            g2.drawString(text, tx, sy);
        }

        g2.setComposite(oldComposite);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }

    private Color colorOf(Type type) {
        switch (type) {
            case PLAYER: return COLOR_PLAYER;
            case SHADOW: return COLOR_SHADOW;
            default:     return COLOR_ENEMY;
        }
    }
}
