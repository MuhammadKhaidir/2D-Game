package Item;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.QuadCurve2D;
import java.util.Random;

import Entitas.player;
import Main.GamePanel;
import Main.KeyHandler;
import bahan.WaterCollision;

/**
 * MANCING.
 *
 * Cara main:
 *  1. Beli Fishing Rod di Armor Merchant (bayar Coin).
 *  2. Berdiri di darat, hadap ke air (jarak maksimal 3 tile), tekan F. Atau masukin Fishing Rod ke hotbar
 *     lalu tekan angkanya / klik slotnya (rod gak habis, cuma jadi "tombol pancing").
 *  3. Tunggu pelampung. Pas ikan nyambar muncul tanda "!" di atas kepala dan pelampung tenggelam:
 *     tekan F lagi SECEPATNYA (cuma ada {@link #BITE_WINDOW_SEC} detik). Kelamaan = ikannya kabur.
 *  4. Tekan F sebelum ada gigitan = tarik pancingan lagi (batal). Jalan / noleh / kena damage juga membatalkan.
 *
 * Hasil tangkapan masuk backpack. Ikan bisa dimakan lewat hotbar buat nambah HP (lihat ItemManager.createFoodEffect).
 *
 * Alurnya state machine: IDLE -> WAITING -> BITE -> COOLDOWN -> IDLE.
 * Semua waktu dalam detik, dikali gp.FPS pas dipakai, jadi aman kalau FPS diubah.
 */
public class FishingManager {

    public enum State { IDLE, WAITING, BITE, COOLDOWN }

    // ===== SETELAN (gampang diubah di sini) =====
    private static final double WAIT_MIN_SEC     = 2.5;   // paling cepat ikan nyambar
    private static final double WAIT_MAX_SEC     = 7.0;   // paling lama ikan nyambar
    private static final double RAIN_WAIT_FACTOR = 0.7;   // pas hujan ikan lebih agresif (waktu tunggu x0.7)
    private static final double BITE_WINDOW_SEC  = 1.2;   // waktu buat narik pancingan setelah ikan nyambar
    private static final double COOLDOWN_SEC     = 0.5;   // jeda sesudah selesai, biar F gak langsung ngelempar lagi
    private static final int    CAST_REACH_TILES = 3;     // sejauh apa pelampung bisa dilempar (dalam tile)

    /** Peluang tangkapan (bobot). Total bebas, gak harus 100. */
    private static final int WEIGHT_SMALL  = 62;
    private static final int WEIGHT_BIG    = 30;
    private static final int WEIGHT_GOLDEN = 8;

    private final GamePanel gp;
    private final Random rng = new Random();

    private State state = State.IDLE;
    private int timer = 0;          // sisa frame di state sekarang
    private int tick = 0;           // jam animasi (naik tiap update)

    // posisi pelampung (dunia, pixel) + kondisi pas dilempar (buat deteksi pembatalan)
    private int bobberX, bobberY;
    private int castPX, castPY;
    private String castDir = "down";
    private int lastLife;

    public FishingManager(GamePanel gp) {
        this.gp = gp;
    }

    // ===================================================================
    // INPUT
    // ===================================================================

    /**
     * Dipanggil GamePanel tiap frame (sebelum cek dialog / toko). Tombol F SELALU dikonsumsi di sini,
     * supaya F yang ditekan pas dialog / toko / chat gak "nyangkut" lalu ngelempar pancingan sesudahnya.
     */
    public void handleKey(KeyHandler keys) {
        boolean pressed = keys.fPressed;
        keys.fPressed = false;
        if (!pressed) return;
        if (gp.hotbar != null && !gp.hotbar.canUse()) return; // dialog, menu, toko, mati, dll.

        if (!hasRod()) {
            // belum punya pancingan: kasih petunjuk, tapi cuma kalau lagi hadap air (biar F biasa gak berisik)
            if (state == State.IDLE && findCastTarget() == null) {
                say("I need a fishing rod. The Armor Merchant sells them.");
            }
            return;
        }
        useRod();
    }

    /** Aksi pancingan: lempar kalau lagi diam, tarik kalau ikan nyambar, batal kalau masih nunggu. */
    public void useRod() {
        switch (state) {
            case IDLE:
                cast();
                break;
            case WAITING:
                cancel("You reeled in too early.");
                break;
            case BITE:
                hook();
                break;
            case COOLDOWN:
            default:
                break;
        }
    }

    // ===================================================================
    // LOGIKA
    // ===================================================================

    private void cast() {
        player p = gp.player;
        if (p == null || p.isDead) return;
        if (gp.gameState != GamePanel.GameState.OUTSIDE) {
            say("I can't fish in here.");
            return;
        }
        if (!p.isNotPunching()) return;

        String problem = findCastTarget();
        if (problem != null) {
            say(problem);
            return;
        }

        castPX = p.x;
        castPY = p.y;
        castDir = p.direction;
        lastLife = p.life;

        double wait = WAIT_MIN_SEC + rng.nextDouble() * (WAIT_MAX_SEC - WAIT_MIN_SEC);
        if (gp.world != null && gp.world.isRaining) wait *= RAIN_WAIT_FACTOR;
        timer = frames(wait);
        state = State.WAITING;
        say("Line cast. Press F when something bites!");
    }

    private void hook() {
        Item caught = rollCatch();
        ItemManager.get().add(caught, 1);
        say("Caught 1x " + caught.name + "!");
        startCooldown();
    }

    /** Batalin pancingan (pelampung ditarik balik). Pesan boleh null = diam saja. */
    public void cancel(String message) {
        if (state == State.IDLE) return;
        if (message != null) say(message);
        startCooldown();
    }

    /** Batal tanpa pesan & tanpa jeda (dipakai kalau game di-load / respawn). */
    public void reset() {
        state = State.IDLE;
        timer = 0;
    }

    public boolean isFishing() {
        return state == State.WAITING || state == State.BITE;
    }

    public State getState() {
        return state;
    }

    private void startCooldown() {
        state = State.COOLDOWN;
        timer = frames(COOLDOWN_SEC);
    }

    /** Dipanggil GamePanel.update() tiap frame di jalur update normal (dunia berhenti pas dialog / toko). */
    public void update() {
        tick++;
        if (state == State.IDLE) return;

        if (state != State.COOLDOWN) {
            String why = interruptReason();
            if (why != null) {
                cancel(why.isEmpty() ? null : why);
                return;
            }
        }

        timer--;
        if (timer > 0) return;

        switch (state) {
            case WAITING:
                state = State.BITE;
                timer = frames(BITE_WINDOW_SEC);
                break;
            case BITE:
                say("It got away...");
                startCooldown();
                break;
            case COOLDOWN:
                state = State.IDLE;
                break;
            default:
                break;
        }
    }

    /** null = aman lanjut. "" = batal diam-diam. Selain itu = batal + pesan. */
    private String interruptReason() {
        player p = gp.player;
        if (p == null || p.isDead) return "";
        if (gp.gameState != GamePanel.GameState.OUTSIDE) return "";
        if (!hasRod()) return "";
        if (p.x != castPX || p.y != castPY) return "You walked away from your line.";
        if (p.direction != null && !p.direction.equals(castDir)) return "You turned away from your line.";
        if (p.life < lastLife) return "Fishing interrupted!";
        lastLife = p.life;
        return null;
    }

    private Item rollCatch() {
        ItemManager im = ItemManager.get();
        int total = WEIGHT_SMALL + WEIGHT_BIG + WEIGHT_GOLDEN;
        int roll = rng.nextInt(total);
        if (roll < WEIGHT_SMALL) return im.FISH_SMALL;
        if (roll < WEIGHT_SMALL + WEIGHT_BIG) return im.FISH_BIG;
        return im.FISH_GOLDEN;
    }

    private boolean hasRod() {
        ItemManager im = ItemManager.get();
        return im.FISHING_ROD != null && im.count(im.FISHING_ROD) > 0;
    }

    private int frames(double seconds) {
        return Math.max(1, (int) Math.round(seconds * gp.FPS));
    }

    private void say(String msg) {
        if (gp.speechBubble != null) gp.speechBubble.show(msg);
    }

    // ===================================================================
    // CARI AIR
    // ===================================================================

    /**
     * Cari titik air di depan player (sampai CAST_REACH_TILES tile) dan simpan di bobberX/bobberY.
     * @return null kalau ketemu, selain itu pesan kenapa gak bisa.
     */
    private String findCastTarget() {
        player p = gp.player;
        int ts = gp.tileSize;
        int cx = p.x + ts / 2;
        int cy = p.y + ts / 2;

        if (isWaterAt(cx, cy)) return "I need solid ground to fish from.";

        int dx = 0, dy = 0;
        String dir = p.direction == null ? "down" : p.direction;
        switch (dir) {
            case "up":    dy = -1; break;
            case "down":  dy =  1; break;
            case "left":  dx = -1; break;
            default:      dx =  1; break;
        }

        int step = Math.max(2, ts / WaterCollision.SUB);   // sekecil sel collision air (8px)
        int reach = ts * CAST_REACH_TILES;
        for (int d = ts / 2; d <= reach; d += step) {
            int px = cx + dx * d;
            int py = cy + dy * d;
            if (!isWaterAt(px, py)) continue;

            // geser sedikit ke tengah air, biar pelampung gak nempel di bibir pantai
            int bx = px + dx * (ts / 3);
            int by = py + dy * (ts / 3);
            if (!isWaterAt(bx, by)) { bx = px; by = py; }
            bobberX = bx;
            bobberY = by;
            return null;
        }
        return "There's no water in front of me.";
    }

    /** Titik dunia (pixel) ini air? Baca dari peta dasar, jadi gak tergantung chunk salju / es yang lagi dimuat. */
    private boolean isWaterAt(int wx, int wy) {
        if (wx < 0 || wy < 0) return false;
        int ts = gp.tileSize;
        int col = wx / ts, row = wy / ts;
        if (col >= gp.maxWorldCol || row >= gp.maxWorldRow) return false;
        bahan.TileM tm = gp.tileM;
        int tile = (tm.gen != null) ? tm.gen.baseTile(col, row) : tm.mapTileNum[col][row];
        return WaterCollision.isWaterAt(tile, (wx - col * ts) / (float) ts, (wy - row * ts) / (float) ts);
    }

    // ===================================================================
    // GAMBAR (dipanggil di dalam transform zoom dunia, sesudah player)
    // ===================================================================

    public void draw(Graphics2D g2) {
        if (!isFishing()) return;
        player p = gp.player;
        if (p == null) return;

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        java.awt.Stroke oldStroke = g2.getStroke();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ts = gp.tileSize;
        int bx = bobberX - p.x + p.screenX;
        int by = bobberY - p.y + p.screenY;

        // ujung joran: dari tangan player ke arah air
        int dx = Integer.signum(bobberX - (p.x + ts / 2));
        int dy = Integer.signum(bobberY - (p.y + ts / 2));
        if (Math.abs(bobberX - (p.x + ts / 2)) < Math.abs(bobberY - (p.y + ts / 2))) dx = 0; else dy = 0;
        int handX = p.screenX + ts / 2 + dx * 8;
        int handY = p.screenY + ts / 2 + 4;
        int tipX = handX + dx * 24;
        int tipY = handY + dy * 24 - 12;

        boolean biting = state == State.BITE;
        // pelampung naik-turun pelan pas nunggu, nyelam + goyang pas ikan nyambar
        double bob = biting ? 4 + Math.sin(tick * 1.4) * 2.0 : Math.sin(tick * 0.12) * 1.5;
        int fy = (int) Math.round(by + bob);

        // riak di air
        drawRipples(g2, bx, by + 3, biting);

        // joran
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(70, 45, 25));
        g2.drawLine(handX, handY, tipX, tipY);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 85, 50));
        g2.drawLine(handX + dx * 12, handY + dy * 12 - 6, tipX, tipY);

        // tali (melengkung turun) ke pelampung
        float sag = biting ? 4f : 14f;
        QuadCurve2D line = new QuadCurve2D.Float(tipX, tipY, (tipX + bx) / 2f, Math.max(tipY, fy) + sag, bx, fy - 4);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(235, 235, 235, 200));
        g2.draw(line);

        // pelampung (merah-putih)
        g2.setColor(Color.WHITE);
        g2.fillOval(bx - 5, fy - 5, 10, 10);
        g2.setColor(new Color(210, 50, 45));
        g2.fillArc(bx - 5, fy - 5, 10, 10, 0, 180);
        g2.setColor(new Color(40, 25, 20));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(bx - 5, fy - 5, 10, 10);

        if (biting) drawBiteAlert(g2, bx, fy);

        g2.setStroke(oldStroke);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    private void drawRipples(Graphics2D g2, int cx, int cy, boolean biting) {
        double speed = biting ? 0.22 : 0.05;
        for (int i = 0; i < 2; i++) {
            double ph = (tick * speed + i * 0.5) % 1.0;
            int w = (int) (10 + ph * (biting ? 34 : 22));
            int h = w / 3;
            int alpha = (int) (150 * (1.0 - ph));
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(235, 245, 255, Math.max(0, alpha)));
            g2.drawOval(cx - w / 2, cy - h / 2, w, h);
        }
    }

    /** Tanda "!" + "[F]" di atas pelampung pas ikan nyambar (di sini, bukan di kepala player, biar gak nimpa HP bar & speech bubble). */
    private void drawBiteAlert(Graphics2D g2, int bx, int by) {
        int top = by - 62 - (int) Math.round(Math.abs(Math.sin(tick * 0.35)) * 4);

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(bx - 17, top - 4, 34, 42, 12, 12);
        g2.setColor(new Color(255, 210, 60));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bx - 17, top - 4, 34, 42, 12, 12);

        g2.setFont(new Font("Dialog", Font.BOLD, 24));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(255, 80, 60));
        g2.drawString("!", bx - fm.stringWidth("!") / 2, top + 22);

        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString("[F]", bx - fm.stringWidth("[F]") / 2, top + 35);
    }
}
