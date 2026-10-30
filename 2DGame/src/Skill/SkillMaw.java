package Skill;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Entitas.Entity;
import Entitas.player;
import Main.GamePanel;

/**
 * Maw of the Forgotten — skill aktif senjata Leviathan Fang (tombol R, cuma jalan kalau Leviathan Fang dipegang).
 *
 *  1. Rahang raksasa Leviathan muncul mengelilingi player.
 *  2. Selama 3 detik, SEMUA serangan yang kena player diserap penuh (HP gak berkurang). Totalnya dicatat.
 *     Hook-nya ada di player.takeDamageFrom() -> absorb().
 *  3. Habis 3 detik rahang menutup dan menggigit semua monster di sekitar player dengan damage
 *     = total diserap x BITE_MULTIPLIER. Kalau gak ada yang diserap: gak ada gigitan, rahang cuma memudar.
 *
 * Semua efek digambar pakai Graphics2D (gak butuh aset). Timing dalam frame, FPS game = 40.
 */
public class SkillMaw extends Skill {

    public static final String WEAPON_NAME = "Leviathan Fang";

    // ── Timing (FPS 40) ──
    private static final int EMERGE_FRAMES = 20;    // rahang muncul (bagian dari 3 detik serap)
    private static final int ABSORB_FRAMES = 120;   // 3 detik serap
    private static final int CLOSE_FRAMES  = 14;    // rahang menutup / menggigit

    // ── Damage ──
    private static final float BITE_MULTIPLIER = 1.5f;   // damage gigitan = total diserap x ini (1.0 = persis sama)

    // ── Warna tema Leviathan (teal laut dalam, sama dengan tint Leviathan Fang) ──
    private static final Color TEAL      = new Color(0x2E, 0xC4, 0xB6);
    private static final Color TEAL_DEEP = new Color(0x0C, 0x3A, 0x42);
    private static final Color TEAL_LITE = new Color(0x9A, 0xF0, 0xE6);
    private static final Color BONE      = new Color(0xDD, 0xF3, 0xEE);

    // ── State ──
    private int absorbed = 0;
    private boolean bitten = false;
    private int biteDamage = 0;
    private float flash = 0f;            // kilau rahang tiap kali menyerap

    private static class Ring {
        final float start, max, speed, width;
        final Color color;
        float r;
        Ring(float start, float max, float speed, float width, Color color) {
            this.start = start; this.r = start; this.max = max; this.speed = speed; this.width = width; this.color = color;
        }
        float progress() { return Math.min(1f, (r - start) / (max - start)); }
        boolean done() { return r >= max; }
    }

    private static class Bubble {          // gelembung naik (burst = serpihan yang melesat keluar pas gigitan)
        float ox, oy, vx, vy, size;
        int life, maxLife;
        boolean burst;
    }

    private static class Pop {             // teks melayang
        final String text; final Color color; final int size, maxLife;
        float oy; int life;
        Pop(String text, Color color, int size, int maxLife, float oy) {
            this.text = text; this.color = color; this.size = size; this.maxLife = maxLife; this.oy = oy;
        }
    }

    private final List<Ring> rings = new ArrayList<>();
    private final List<Bubble> bubbles = new ArrayList<>();
    private final List<Pop> pops = new ArrayList<>();
    private final Random rng = new Random();

    public SkillMaw(GamePanel gp, player owner) {
        super(gp, owner);
        cooldownMax = 800;                                 // 20 detik (dihitung dari saat skill dipakai)
        activeDuration = ABSORB_FRAMES + CLOSE_FRAMES;
    }

    /** True kalau Leviathan Fang lagi dipegang. */
    public boolean isWielding() {
        return owner.isWeaponEquipped && owner.currentWeapon != null
            && WEAPON_NAME.equals(owner.currentWeapon.name);
    }

    private float jawRadius() { return gp.tileSize * 1.5f; }

    // ───────────────────────── Aktivasi ─────────────────────────

    @Override
    public void activate() {
        if (owner.isDead || !isWielding()) return;     // skill ini cuma buat Leviathan Fang
        super.activate();
    }

    @Override
    protected void onActivate() {
        absorbed = 0;
        bitten = false;
        biteDamage = 0;
        flash = 0f;
        rings.clear();
        bubbles.clear();
        pops.clear();

        float R = jawRadius();
        // efek aktivasi: dua riak air melebar dari tubuh player + gelembung naik
        rings.add(new Ring(R * 0.4f, R * 2.3f, R * 0.11f, 4f, TEAL));
        rings.add(new Ring(R * 0.2f, R * 1.8f, R * 0.085f, 2f, TEAL_LITE));
        for (int i = 0; i < 16; i++) spawnBubble(R);
    }

    private void spawnBubble(float R) {
        double a = rng.nextDouble() * Math.PI * 2;
        Bubble b = new Bubble();
        b.ox = (float) Math.cos(a) * R * (0.7f + rng.nextFloat() * 0.5f);
        b.oy = (float) Math.sin(a) * R * (0.7f + rng.nextFloat() * 0.5f);
        b.vy = -(0.5f + rng.nextFloat() * 0.9f);
        b.size = 2 + rng.nextInt(4);
        b.maxLife = b.life = 30 + rng.nextInt(26);
        bubbles.add(b);
    }

    // ───────────────────────── Penyerapan (dipanggil player.takeDamageFrom) ─────────────────────────

    /** Coba serap serangan. True = diserap penuh (pemanggil harus langsung return, HP gak berkurang). */
    public boolean absorb(int dmg) {
        if (!isActive || activeTimer > ABSORB_FRAMES || dmg <= 0) return false;
        absorbed += dmg;
        flash = 1f;
        float R = jawRadius();
        rings.add(new Ring(R * 0.95f, R * 1.6f, R * 0.08f, 3f, TEAL_LITE));   // riak penyerapan
        pops.add(new Pop("+" + dmg, TEAL_LITE, 13, 34, -R - 6f));
        return true;
    }

    // ───────────────────────── Update ─────────────────────────

    @Override
    protected void onActiveUpdate() {
        // mati atau ganti senjata di tengah skill -> batal, tanpa gigitan
        if (owner.isDead || !isWielding()) {
            cancel();
            return;
        }

        if (activeTimer == ABSORB_FRAMES + 1 && absorbed > 0) bite();

        if (activeTimer <= ABSORB_FRAMES && activeTimer % 5 == 0) spawnBubble(jawRadius());
        if (flash > 0f) flash = Math.max(0f, flash - 0.12f);

        for (Ring r : rings) r.r += r.speed;
        rings.removeIf(Ring::done);

        for (Bubble b : bubbles) {
            b.ox += b.vx; b.oy += b.vy;
            if (b.burst) { b.vx *= 0.88f; b.vy *= 0.88f; }
            b.life--;
        }
        bubbles.removeIf(b -> b.life <= 0);

        for (Pop p : pops) { p.life++; p.oy -= 0.9f; }
        pops.removeIf(p -> p.life >= p.maxLife);
    }

    private void cancel() {
        isActive = false;
        activeTimer = 0;
        absorbed = 0;
        rings.clear();
        bubbles.clear();
        pops.clear();
    }

    /** Gigitan akhir: damage ke semua monster dalam jangkauan rahang. */
    private void bite() {
        bitten = true;
        biteDamage = Math.max(1, Math.round(absorbed * BITE_MULTIPLIER));

        float R = jawRadius();
        int px = owner.x + owner.solidArea.x + owner.solidArea.width / 2;
        int py = owner.y + owner.solidArea.y + owner.solidArea.height / 2;
        double reach = R * 2.0;

        if (gp.monsters != null) {
            for (Entity m : gp.monsters) {
                if (m == null || m.isDead || m.life <= 0 || m.solidArea == null) continue;
                if (!isTargetable(m)) continue;
                int mx = m.x + m.solidArea.x + m.solidArea.width / 2;
                int my = m.y + m.solidArea.y + m.solidArea.height / 2;
                if (Math.hypot(mx - px, my - py) <= reach) m.takeDamage(biteDamage, owner);
            }
        }

        // efek gigitan: gelombang kejut + serpihan + teks
        rings.add(new Ring(R * 0.5f, R * 2.2f, R * 0.30f, 6f, Color.WHITE));
        rings.add(new Ring(R * 0.3f, R * 2.0f, R * 0.22f, 3f, TEAL));
        for (int i = 0; i < 22; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            float sp = 3f + rng.nextFloat() * 4f;
            Bubble b = new Bubble();
            b.ox = (float) Math.cos(a) * R * 0.6f;
            b.oy = (float) Math.sin(a) * R * 0.6f;
            b.vx = (float) Math.cos(a) * sp;
            b.vy = (float) Math.sin(a) * sp;
            b.size = 3 + rng.nextInt(3);
            b.maxLife = b.life = 12 + rng.nextInt(8);
            b.burst = true;
            bubbles.add(b);
        }
        pops.add(new Pop("BITE  " + biteDamage, BONE, 20, 50, -R - 14f));
    }

    // Aturan target sama kayak SkillV: monster yang lagi muncul/hilang/mati gak ikut kena
    private boolean isTargetable(Entity m) {
        if (m instanceof Entitas.Golemite)     return ((Entitas.Golemite) m).isAlive();
        if (m instanceof Entitas.Wolf)         return ((Entitas.Wolf) m).isAlive();
        if (m instanceof Entitas.Skeleton)     return ((Entitas.Skeleton) m).alive;
        if (m instanceof Entitas.SkeletonKing) return ((Entitas.SkeletonKing) m).alive;
        return true;
    }

    @Override
    protected void onEnd() {
        rings.clear();
        bubbles.clear();
        pops.clear();
        absorbed = 0;
        bitten = false;
        flash = 0f;
    }

    // ───────────────────────── Gambar ─────────────────────────

    @Override
    public void draw(Graphics2D g2) {
        if (isWielding()) drawCooldownBar(g2);   // bar cooldown cuma muncul kalau Leviathan Fang dipegang
        if (!isActive) return;
        onDraw(g2);
    }

    @Override
    protected void onDraw(Graphics2D g2) {
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Font oldFont = g2.getFont();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = owner.screenX + gp.tileSize / 2;
        int cy = owner.screenY + gp.tileSize / 2;
        float R = jawRadius();
        int t = activeTimer;

        // ── Pose rahang ──
        float emerge = ease(t / (float) EMERGE_FRAMES);
        float alpha = emerge;
        float radius = R * (1.8f - 0.8f * emerge);     // muncul dari luar lalu merapat
        float gap = R * 0.45f;                         // jarak rahang atas-bawah dari pusat
        float snap = 0f;                               // 0 = terbuka, 1 = tertutup

        if (t <= ABSORB_FRAMES) {
            gap *= 1f + 0.05f * (float) Math.sin(t * 0.15);   // "bernapas" pelan
        } else {
            int ct = t - ABSORB_FRAMES;
            if (absorbed > 0) {
                float s = Math.min(1f, ct / (CLOSE_FRAMES * 0.5f));
                snap = s * s;                                            // makin cepat = menyentak
                gap *= 1f - snap;
                radius = R * (1f - 0.38f * snap);
                alpha = 1f - Math.max(0f, Math.min(1f, (ct - (CLOSE_FRAMES - 5)) / 5f));
            } else {
                float p = Math.min(1f, ct / (float) CLOSE_FRAMES);       // gak ada yang diserap: cuma memudar
                radius = R * (1f + 0.3f * p);
                alpha = 1f - p;
            }
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        // ── Air di dalam rahang ──
        setAlpha(g2, alpha * 0.14f);
        g2.setColor(TEAL);
        int dr = (int) (radius * 0.95f);
        g2.fillOval(cx - dr, cy - dr, dr * 2, dr * 2);

        // ── Riak / gelombang kejut ──
        for (Ring r : rings) {
            setAlpha(g2, (1f - r.progress()) * 0.9f);
            g2.setColor(r.color);
            g2.setStroke(new BasicStroke(r.width * (1f - 0.6f * r.progress())));
            int rr = (int) r.r;
            g2.drawOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }

        // ── Rahang bawah dulu, baru atas ──
        drawJaw(g2, cx, cy + gap, radius, false, snap, alpha);
        drawJaw(g2, cx, cy - gap, radius, true, snap, alpha);

        // ── Kilatan putih pas gigitan ──
        if (bitten && t > ABSORB_FRAMES) {
            float f = 1f - Math.max(0f, Math.min(1f, (t - ABSORB_FRAMES - 1) / 8f));
            setAlpha(g2, f * 0.55f);
            g2.setColor(Color.WHITE);
            int fr = (int) (R * 1.15f);
            g2.fillOval(cx - fr, cy - fr, fr * 2, fr * 2);
        }

        // ── Gelembung & serpihan ──
        for (Bubble b : bubbles) {
            float a = Math.max(0f, b.life / (float) b.maxLife);
            int bx = (int) (cx + b.ox), by = (int) (cy + b.oy), s = (int) b.size;
            if (b.burst) {
                setAlpha(g2, a);
                g2.setColor(a > 0.5f ? Color.WHITE : TEAL_LITE);
                g2.fillRect(bx - s / 2, by - s / 2, s, s);
            } else {
                setAlpha(g2, a * 0.8f * emerge);
                g2.setColor(TEAL_LITE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(bx - s, by - s, s * 2, s * 2);
            }
        }

        // ── Teks: total diserap + sisa waktu ──
        g2.setComposite(oldComp);
        if (t <= ABSORB_FRAMES) {
            int ty = (int) (cy - R - gap - 14);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String label = "ABSORBED " + absorbed;
            int tx = cx - fm.stringWidth(label) / 2;
            g2.setColor(Color.BLACK);
            g2.drawString(label, tx + 1, ty + 1);
            g2.setColor(absorbed > 0 ? TEAL_LITE : new Color(160, 200, 196));
            g2.drawString(label, tx, ty);

            int bw = 60, bx = cx - bw / 2, by = ty + 5;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(bx, by, bw, 3);
            g2.setColor(TEAL);
            g2.fillRect(bx, by, (int) (bw * (1f - t / (float) ABSORB_FRAMES)), 3);
        }
        for (Pop p : pops) {
            float a = 1f - p.life / (float) p.maxLife;
            g2.setFont(new Font("Monospaced", Font.BOLD, p.size));
            FontMetrics fm = g2.getFontMetrics();
            int tx = cx - fm.stringWidth(p.text) / 2;
            int ty = (int) (cy + p.oy);
            setAlpha(g2, a);
            g2.setColor(Color.BLACK);
            g2.drawString(p.text, tx + 1, ty + 1);
            g2.setColor(p.color);
            g2.drawString(p.text, tx, ty);
        }

        g2.setComposite(oldComp);
        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /**
     * Satu rahang = busur tebal + deretan gigi segitiga yang menghadap ke dalam.
     * (jx,jy) pusat busur; upper=true busur di atas, false di bawah. snap 0..1 melebarkan busur sampai hampir menyatu.
     */
    private void drawJaw(Graphics2D g2, float jx, float jy, float r, boolean upper, float snap, float alpha) {
        float a0 = (upper ? 25f : 205f) - snap * 20f;     // derajat, konvensi drawArc (0 = jam 3, naik = berlawanan jarum jam)
        float a1 = (upper ? 155f : 335f) + snap * 20f;
        float bone = r * 0.17f;

        // glow di belakang tulang
        setAlpha(g2, alpha * (0.22f + 0.4f * flash));
        g2.setColor(TEAL);
        g2.setStroke(new BasicStroke(bone * 1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc((int) (jx - r), (int) (jy - r), (int) (r * 2), (int) (r * 2), (int) a0, (int) (a1 - a0));

        // tulang rahang + sorot terang di sisi dalam
        setAlpha(g2, alpha);
        g2.setColor(lerp(TEAL_DEEP, TEAL_LITE, flash * 0.7f));
        g2.setStroke(new BasicStroke(bone, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc((int) (jx - r), (int) (jy - r), (int) (r * 2), (int) (r * 2), (int) a0, (int) (a1 - a0));
        g2.setColor(lerp(TEAL, Color.WHITE, flash));
        g2.setStroke(new BasicStroke(Math.max(1f, bone * 0.22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float rh = r - bone * 0.25f;
        g2.drawArc((int) (jx - rh), (int) (jy - rh), (int) (rh * 2), (int) (rh * 2), (int) a0, (int) (a1 - a0));

        // gigi
        int n = 11;
        float step = (a1 - a0) / n;
        double dth = Math.toRadians(step * 0.42f);
        float baseR = r - bone * 0.45f;
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < n; i++) {
            double th = Math.toRadians(a0 + (i + 0.5f) * step);
            float len = r * 0.30f * (0.65f + 0.35f * (float) Math.sin(Math.PI * (i + 0.5) / n));   // gigi tengah paling panjang
            int[] xs = {
                (int) (jx + Math.cos(th - dth) * baseR),
                (int) (jx + Math.cos(th + dth) * baseR),
                (int) (jx + Math.cos(th) * (baseR - len)) };
            int[] ys = {
                (int) (jy - Math.sin(th - dth) * baseR),
                (int) (jy - Math.sin(th + dth) * baseR),
                (int) (jy - Math.sin(th) * (baseR - len)) };
            g2.setColor(lerp(BONE, Color.WHITE, flash));
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(TEAL_DEEP);
            g2.drawPolygon(xs, ys, 3);
        }
    }

    private void drawCooldownBar(Graphics2D g2) {
        int barW = gp.tileSize;
        int bx = owner.screenX;
        int by = owner.screenY + gp.tileSize + 20;      // di bawah bar skill Z (+4) dan X/C (+12)

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(bx, by, barW, 3);
        g2.setColor(TEAL);
        g2.fillRect(bx, by, (int) (barW * getCooldownPercent()), 3);
    }

    // ───────────────────────── Util ─────────────────────────

    private static void setAlpha(Graphics2D g2, float a) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
    }

    private static float ease(float t) {      // ease-out cubic
        t = Math.max(0f, Math.min(1f, t));
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }
}
