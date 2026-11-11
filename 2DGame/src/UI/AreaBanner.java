package UI;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import Main.GamePanel;
import bahan.WorldGen;
import World.DomainManager;

/**
 * Banner nama tempat di bawah layar (di atas hotbar) tiap player pindah ke tempat baru:
 * dataran, laut, daratan es, tanah terkutuk (tepi dunia), dan domain (Tomb, Golem Hide, Water Domain).
 *
 * Sekaligus jadi sumber "lagi di area luar mana" buat musik: GamePanel.updateMusic() menanyakan {@link #outdoorArea()}.
 *
 * Aturan pindah area (biar banner gak berkedip waktu player nyusur pantai / batas salju):
 *  - Area luar baru harus terdeteksi 4 kali cek berturut-turut (cek 4x per detik, jadi sekitar 1 detik) baru diumumkan.
 *  - Es punya histeresis: masuk di ice() >= ICE_ENTER, baru dianggap keluar kalau turun di bawah ICE_LEAVE.
 *  - Laut = titik itu air DAN sekelilingnya (jendela 9 x 9 tile) hampir semuanya air, jadi kolam kecil / sungai
 *    tidak dihitung "Open Sea".
 *  - Masuk / keluar domain langsung diumumkan (itu transisi yang disengaja). Masuk rumah tidak mengubah apa-apa.
 * Nama dan warna tempat ada di {@link #describe(String)}.
 */
public class AreaBanner {

    /** Area di dunia luar. Dipakai juga buat memilih musik. */
    public enum Area { PLAINS, SEA, ICE, CURSED }

    // ── Deteksi ──
    private static final int   CHECK_EVERY   = 10;      // game jalan 40 FPS -> cek posisi 4x per detik
    private static final int   SETTLE_CHECKS = 4;       // calon area harus bertahan segini kali cek berturut-turut
    private static final float ICE_ENTER     = 0.50f;   // sama dengan batas "bersalju" di WorldGen.surfaceTile / minimap
    private static final float ICE_LEAVE     = 0.35f;
    private static final float CURSE_ENTER   = 0.55f;   // WorldGen.curse(): sudah masuk dalam biome terkutuk (pohon layu mulai dari 0.10)
    private static final float CURSE_LEAVE   = 0.40f;
    private static final float SEA_ENTER     = 0.70f;   // porsi air di jendela sekitar player buat dianggap laut lepas
    private static final float SEA_LEAVE     = 0.50f;
    private static final int   SEA_WINDOW    = 4;       // jendela sampling: +-4 tile, tiap 2 tile -> 25 titik

    // ── Tampilan (ms) ──
    private static final long FADE_IN  = 450;
    private static final long HOLD     = 2600;
    private static final long FADE_OUT = 900;

    private final Font fCaption = new Font("Monospaced", Font.BOLD, 11);
    private final Font fName    = new Font("Monospaced", Font.BOLD, 24);

    private final GamePanel gp;

    private Area outdoor = null;                 // area luar terakhir yang sudah diumumkan
    private String shownKey = null;              // tempat terakhir yang diumumkan ("PLAINS", "DOMAIN:Tomb", ...)
    private String candidateKey = null;          // calon tempat baru yang masih menunggu bertahan
    private int candidateChecks = 0;
    private int tick = 0;
    private GamePanel.GameState lastState = null;

    private String text = "";
    private Color accent = Color.WHITE;
    private long shownAt = -1;

    public AreaBanner(GamePanel gp) {
        this.gp = gp;
    }

    /** Area luar yang terakhir diumumkan (PLAINS kalau belum ada). Dipakai buat memilih musik. */
    public Area outdoorArea() {
        return outdoor == null ? Area.PLAINS : outdoor;
    }

    // ───────────────────────── Update (panggil tiap tick) ─────────────────────────

    public void update() {
        if (gp.player == null || gp.tileM == null || gp.tileM.gen == null) return;

        GamePanel.GameState st = gp.gameState;
        if (st != lastState) {            // ganti state (keluar rumah / domain, dll): cek langsung di tick ini
            lastState = st;
            tick = CHECK_EVERY - 1;
        }
        if (st != GamePanel.GameState.OUTSIDE && st != GamePanel.GameState.DOMAIN) return;   // rumah / dialog: area tetap
        if (++tick % CHECK_EVERY != 0) return;

        String key;
        if (st == GamePanel.GameState.DOMAIN) {
            String id = (gp.domainManager != null) ? gp.domainManager.getActiveDomainId() : null;
            if (id == null) return;
            key = "DOMAIN:" + id;
        } else {
            key = detectOutdoor().name();
        }

        if (key.equals(shownKey)) {       // masih di tempat yang sama
            candidateKey = null;
            candidateChecks = 0;
            return;
        }

        boolean immediate = shownKey == null || shownKey.startsWith("DOMAIN:") || key.startsWith("DOMAIN:");
        if (!immediate) {
            if (!key.equals(candidateKey)) {
                candidateKey = key;
                candidateChecks = 1;
                return;
            }
            if (++candidateChecks < SETTLE_CHECKS) return;
        }
        announce(key);
    }

    private void announce(String key) {
        shownKey = key;
        candidateKey = null;
        candidateChecks = 0;
        if (!key.startsWith("DOMAIN:")) outdoor = Area.valueOf(key);

        Object[] info = describe(key);
        text = (String) info[0];
        accent = (Color) info[1];
        shownAt = System.currentTimeMillis();
    }

    /** {nama tampil, warna aksen} untuk sebuah tempat. Ganti teks / warna di sini. */
    private static Object[] describe(String key) {
        switch (key) {
            case "PLAINS":           return new Object[] { "GREEN PLAINS", new Color(160, 220, 90) };
            case "SEA":              return new Object[] { "OPEN SEA",     new Color(90, 190, 240) };
            case "ICE":              return new Object[] { "ICE LAND",     new Color(200, 238, 255) };
            case "CURSED":           return new Object[] { "CURSED LAND",  new Color(190, 90, 255) };
            case "DOMAIN:Tomb":      return new Object[] { "THE TOMB",     new Color(190, 130, 255) };
            case "DOMAIN:GolemHide": return new Object[] { "GOLEM HIDE",   new Color(235, 170, 80) };
            default:
                if (key.equals("DOMAIN:" + DomainManager.WATER_DOMAIN_ID)) {
                    return new Object[] { "WATER DOMAIN", new Color(80, 210, 200) };
                }
                return new Object[] { key.substring(key.indexOf(':') + 1).toUpperCase(), new Color(240, 220, 160) };
        }
    }

    // ───────────────────────── Deteksi area luar ─────────────────────────

    private Area detectOutdoor() {
        WorldGen g = gp.tileM.gen;
        // titik tengah hitbox player (di kaki), satuan tile
        double cx = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2.0;
        double cy = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2.0;
        float tx = (float) (cx / gp.tileSize), ty = (float) (cy / gp.tileSize);
        int col = (int) Math.floor(tx), row = (int) Math.floor(ty);

        // biome terkutuk (tepi dunia) diperiksa paling dulu: di sana tidak ada es / laut yang perlu dibedakan
        float curseLimit = (outdoor == Area.CURSED) ? CURSE_LEAVE : CURSE_ENTER;
        if (g.isLand(col, row) && g.curse(tx, ty) >= curseLimit) return Area.CURSED;

        if (!g.isLand(col, row)) {
            float need = (outdoor == Area.SEA) ? SEA_LEAVE : SEA_ENTER;
            if (waterFraction(g, col, row) >= need) return Area.SEA;
        }
        float limit = (outdoor == Area.ICE) ? ICE_LEAVE : ICE_ENTER;
        return g.ice(tx, ty) >= limit ? Area.ICE : Area.PLAINS;
    }

    /** Porsi titik air di jendela (2 * SEA_WINDOW + 1) tile di sekitar (col, row), disampling tiap 2 tile. */
    private float waterFraction(WorldGen g, int col, int row) {
        int water = 0, total = 0;
        for (int dy = -SEA_WINDOW; dy <= SEA_WINDOW; dy += 2) {
            for (int dx = -SEA_WINDOW; dx <= SEA_WINDOW; dx += 2) {
                total++;
                if (!g.isLand(col + dx, row + dy)) water++;
            }
        }
        return water / (float) total;
    }

    // ───────────────────────── Gambar ─────────────────────────

    public void draw(Graphics2D g2) {
        if (shownAt < 0 || text.isEmpty()) return;

        long t = System.currentTimeMillis() - shownAt;
        if (t >= FADE_IN + HOLD + FADE_OUT) {
            shownAt = -1;
            return;
        }
        float a = (t < FADE_IN) ? t / (float) FADE_IN
                : (t < FADE_IN + HOLD) ? 1f
                : 1f - (t - FADE_IN - HOLD) / (float) FADE_OUT;
        a = a * a * (3f - 2f * a);                         // ease in / out

        Composite oldComp = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        Font oldFont = g2.getFont();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));

        g2.setFont(fName);
        FontMetrics nfm = g2.getFontMetrics();
        int nameW = nfm.stringWidth(text);
        int w = Math.max(nameW + 110, 250);
        int h = 54;
        int x = (gp.screenWidth - w) / 2;
        int y = gp.screenHeight - 170 + Math.round((1f - a) * 10f);   // naik pelan dari bawah waktu muncul

        // pelat gelap
        g2.setColor(new Color(10, 10, 14, 170));
        g2.fillRect(x, y, w, h);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(withAlpha(accent, 200));
        g2.drawRect(x, y, w, h);
        g2.setColor(withAlpha(accent, 90));
        g2.drawRect(x + 3, y + 3, w - 6, h - 6);

        // diamond kiri-kanan
        g2.setColor(accent);
        diamond(g2, x + 16, y + h / 2, 4);
        diamond(g2, x + w - 16, y + h / 2, 4);

        // keterangan kecil
        g2.setFont(fCaption);
        FontMetrics cfm = g2.getFontMetrics();
        String caption = "- NOW ENTERING -";
        g2.setColor(withAlpha(accent, 190));
        g2.drawString(caption, x + (w - cfm.stringWidth(caption)) / 2, y + 17);

        // nama tempat (bayangan hitam + warna aksen)
        g2.setFont(fName);
        int nx = x + (w - nameW) / 2, ny = y + 43;
        g2.setColor(Color.BLACK);
        g2.drawString(text, nx + 2, ny + 2);
        g2.setColor(accent);
        g2.drawString(text, nx, ny);

        g2.setFont(oldFont);
        g2.setStroke(oldStroke);
        g2.setComposite(oldComp);
    }

    private static void diamond(Graphics2D g2, int cx, int cy, int r) {
        g2.fillPolygon(new int[] { cx, cx + r, cx, cx - r }, new int[] { cy - r, cy, cy + r, cy }, 4);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
