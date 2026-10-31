package World;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Entitas.player;
import Main.GamePanel;
import bahan.WaterAnimator;
import bahan.WaterDomainMap;
import bahan.WaterPortalArt;

/**
 * Portal Water Domain: dua kolam air bergelombang + animasi lompat masuk / keluar.
 *
 *  - Kolam dunia luar ada di padang rumput timur laut dari titik awal ({@link #OUT_X}, {@link #OUT_Y}).
 *    Air tidak bisa diinjak. MASUK = melompat ke air: dorong karakter ke arah air sebentar, atau tekan SPASI (lompat)
 *    menuju air. Animasi lompat diputar otomatis: jongkok, melayang lewat busur ke tengah kolam, nyebur (percikan +
 *    riak), tenggelam, layar gelap, lalu Water Domain tampil.
 *  - Di Water Domain pemain muncul dari kolam keluar di Teluk Kedatangan dan melompat ke daratan di depannya.
 *  - KELUAR = dekati kolam keluar, tekan [E]: pemain melompat ke air, layar gelap, lalu muncul dari kolam dunia luar
 *    dan melompat dari air kembali ke daratan (di titik tempat dia tadi melompat masuk).
 *
 * Selama animasi, player.scripted = true (input dikunci, kebal) dan posisi / pose player diatur dari sini tiap frame
 * (lihat player.beginScripted). Transisi layar gelap digambar sendiri oleh kelas ini (drawFade).
 */
public class WaterPortal {

    /** Pusat kolam dunia luar (px dunia): tile (55, 45), padang rumput kosong di timur laut Tomb. */
    public static final int OUT_X = 55 * 48 + 24, OUT_Y = 45 * 48 + 24;
    public static final String DOMAIN_ID = "WaterDomain";

    private static final int FOOT_X = 24, FOOT_Y = 45;   // titik pijak kaki relatif ke player.x / player.y
    private static final int SCALE = WaterPortalArt.SCALE;
    private static final int SINK_MAX = 72;              // kedalaman tenggelam sampai kepala hilang (px layar)

    private enum St { IDLE, PREP, AIR, SINK, FADE_OUT, RISE, HOP, LAND }

    private final GamePanel gp;
    private volatile BufferedImage[] outFrames, inFrames;   // dibuat di thread latar (agak berat)

    private St st = St.IDLE;
    private boolean sinkInOutside;   // kolam yang sedang dipakai: true = kolam dunia luar
    private int poolCx, poolCy;      // pusat kolam yang sedang dipakai (px dunia)
    private double sFx, sFy, tFx, tFy;   // kaki: titik awal dan tujuan busur
    // tempat melompat masuk dari dunia luar = tempat mendarat saat kembali (bawaan: depan kolam, mis. setelah load game di domain)
    private double entryFx = OUT_X, entryFy = OUT_Y + 150;
    private String dir = "down";
    private int tick, airN, fade, cooldown, pushTimer;
    private double peak = 60;

    // ── percikan & riak ──
    private static final class Pt { double x, y, vx, vy; int life, max, col, size; }
    private static final class Ring { double x, y; int age, delay, max; }
    private final List<Pt> pts = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final Random rnd = new Random();

    public WaterPortal(GamePanel gp) {
        this.gp = gp;
        // air kolam dunia luar tidak bisa diinjak (monster juga ikut menghindar karena memakai objCol yang sama)
        for (Rectangle r : WaterPortalArt.blockerStrips(OUT_X, OUT_Y)) gp.objCol.add(r.x, r.y, r.width, r.height);
        Thread th = new Thread(() -> {
            outFrames = WaterPortalArt.build(WaterPortalArt.Style.PORTAL);
            inFrames = WaterPortalArt.build(WaterPortalArt.Style.EXIT);
        }, "water-portal-art");
        th.setDaemon(true);
        th.start();
    }

    // ================================================================== status

    /** True selama animasi lompat masuk / keluar berjalan. */
    public boolean isBusy() { return st != St.IDLE; }

    /** Batalkan semuanya (dipanggil saat load game, mati, dll). */
    public void reset() {
        if (st != St.IDLE) gp.player.endScripted();
        st = St.IDLE;
        fade = 0;
        pushTimer = 0;
        pts.clear();
        rings.clear();
    }

    // ================================================================== update

    public void update() {
        updateFx();
        if (st == St.IDLE) {
            if (cooldown > 0) cooldown--;
            checkTriggers();
            return;
        }
        player p = gp.player;
        if (p.isDead) { reset(); return; }

        switch (st) {
            case PREP:
                p.scriptAir = false;
                p.scriptLift = 0;
                if (++tick >= 8) beginAir(sFx, sFy, tFx, tFy, 58);
                break;
            case AIR:
            case HOP: {
                tick++;
                double pr = Math.min(1.0, tick / (double) airN);
                setFeet(sFx + (tFx - sFx) * pr, sFy + (tFy - sFy) * pr);
                p.scriptLift = (int) Math.round(peak * 4 * pr * (1 - pr));
                p.scriptAir = pr > 0.10 && pr < 0.90;
                p.scriptShadow = true;
                if (tick >= airN) {
                    if (st == St.AIR) onImpact(); else { st = St.LAND; tick = 0; p.scriptLift = 0; p.scriptAir = false; }
                }
                break;
            }
            case SINK: {
                tick++;
                double pr = Math.min(1.0, tick / 16.0);
                p.scriptLift = 0;
                p.scriptAir = true;
                p.scriptShadow = false;
                p.scriptWaterLine = FOOT_Y + 3;
                p.scriptSink = (int) Math.round(SINK_MAX * Math.pow(pr, 0.85));
                if (tick >= 16) { st = St.FADE_OUT; tick = 0; }
                break;
            }
            case FADE_OUT:
                p.scriptSink = SINK_MAX;
                fade = Math.min(255, fade + 15);
                if (fade >= 255) doSwitch();
                break;
            case RISE: {
                tick++;
                fade = Math.max(0, fade - 17);
                double pr = Math.min(1.0, tick / 14.0);
                p.scriptSink = (int) Math.round(SINK_MAX * Math.pow(1.0 - pr, 1.2));
                p.scriptAir = true;
                p.scriptShadow = false;
                if (tick >= 14) {
                    p.scriptSink = 0;
                    p.scriptShadow = true;
                    splash(poolCx, poolCy + 8, 10, 0.75);
                    gp.playSE("fishing/splash.wav");
                    beginAir(poolCx, poolCy + 6, tFx, tFy, 72);
                    st = St.HOP;   // lompatan keluar dari air ke daratan (pendaratan tidak memicu nyebur)
                }
                break;
            }
            case LAND:
                tick++;
                fade = Math.max(0, fade - 17);
                p.scriptAir = false;
                p.scriptLift = 0;
                setFeet(tFx, tFy);
                if (tick >= 9) finish();
                break;
            default:
                break;
        }
    }

    private void finish() {
        player p = gp.player;
        p.direction = dir;
        p.endScripted();
        st = St.IDLE;
        fade = 0;
        cooldown = 40;
        pushTimer = 0;
    }

    private void setFeet(double fx, double fy) {
        gp.player.x = (int) Math.round(fx) - FOOT_X;
        gp.player.y = (int) Math.round(fy) - FOOT_Y;
    }

    private void beginAir(double fromX, double fromY, double toX, double toY, double peakPx) {
        st = St.AIR;
        sFx = fromX; sFy = fromY; tFx = toX; tFy = toY;
        peak = peakPx;
        tick = 0;
        airN = (int) Math.max(18, Math.min(34, Math.hypot(toX - fromX, toY - fromY) / 6.5));
        gp.playSE("ribhavagrawal-woosh-230554.wav");
    }

    /** Mulai menyelam ke kolam (masuk dari dunia luar, atau keluar dari Water Domain). */
    private void startDive(boolean outsidePool, double fx, double fy, boolean airborne) {
        player p = gp.player;
        sinkInOutside = outsidePool;
        if (outsidePool) { poolCx = OUT_X; poolCy = OUT_Y; entryFx = fx; entryFy = fy; }
        else { WaterDomainMap m = gp.domainManager.getWaterMap(); poolCx = m.poolX; poolCy = m.poolY; }
        double toX = poolCx, toY = poolCy + 6;
        dir = dominant(toX - fx, toY - fy);
        p.beginScripted(dir);
        sFx = fx; sFy = fy; tFx = toX; tFy = toY;
        pts.clear(); rings.clear();
        tick = 0;
        if (airborne) beginAir(fx, fy, toX, toY, 58); else st = St.PREP;
        pushTimer = 0;
    }

    private void onImpact() {
        st = St.SINK;
        tick = 0;
        splash(poolCx, poolCy + 8, 22, 1.0);
        gp.playSE("fishing/splash.wav");
    }

    /** Layar sudah gelap penuh: pindah dunia (luar <-> domain), lalu pemain muncul dari kolam di sisi seberang. */
    private void doSwitch() {
        player p = gp.player;
        pts.clear();
        rings.clear();
        if (sinkInOutside) {
            gp.domainManager.forceEnterDomain(DOMAIN_ID);
            WaterDomainMap m = gp.domainManager.getWaterMap();
            poolCx = m.poolX; poolCy = m.poolY;
            tFx = m.landX; tFy = m.landY;
            sinkInOutside = false;
        } else {
            gp.domainManager.exitDomainNow(OUT_X - FOOT_X, OUT_Y - FOOT_Y);
            poolCx = OUT_X; poolCy = OUT_Y;
            double[] land = safeLanding(entryFx, entryFy);
            tFx = land[0]; tFy = land[1];
            sinkInOutside = true;
        }
        fade = 255;
        setFeet(poolCx, poolCy + 6);
        dir = dominant(tFx - poolCx, tFy - (poolCy + 6));
        p.scriptDir = dir;
        p.direction = dir;
        p.scriptSink = SINK_MAX;
        p.scriptWaterLine = FOOT_Y + 3;
        p.scriptAir = true;
        p.scriptLift = 0;
        p.scriptShadow = false;
        st = St.RISE;
        tick = 0;
        splash(poolCx, poolCy + 8, 14, 0.85);
        gp.playSE("fishing/splash.wav");
    }

    /** Titik mendarat di dunia luar: tempat melompat masuk tadi, kecuali di situ sekarang tertutup (mis. monster / objek). */
    private double[] safeLanding(double fx, double fy) {
        Rectangle box = new Rectangle((int) Math.round(fx) - FOOT_X + 8, (int) Math.round(fy) - FOOT_Y + 16, 32, 32);
        boolean blocked = false;
        for (Rectangle r : gp.objCol.objects) if (box.intersects(r)) { blocked = true; break; }
        if (!blocked) return new double[] {fx, fy};
        return new double[] {OUT_X, OUT_Y + 150};   // depan kolam (selatan), pasti kosong
    }

    private static String dominant(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) return dx > 0 ? "right" : "left";
        return dy > 0 ? "down" : "up";
    }

    // ================================================================== pemicu

    private double sdAt(double px, double py, int cx, int cy) {
        return WaterPortalArt.signedDist((px - cx) / SCALE, (py - cy) / SCALE) * SCALE;
    }

    /** Jarak terdekat (px) dari hitbox pemain ke tepi air kolam; negatif kalau ada bagian hitbox yang sudah di dalam air. */
    private double boxDist(int cx, int cy) {
        player p = gp.player;
        int x0 = p.x + 8, y0 = p.y + 16, x1 = x0 + 32, y1 = y0 + 32, xm = (x0 + x1) / 2, ym = (y0 + y1) / 2;
        int[][] pt = {{x0, y0}, {x1, y0}, {x0, y1}, {x1, y1}, {xm, y0}, {xm, y1}, {x0, ym}, {x1, ym}, {xm, ym}};
        double best = Double.MAX_VALUE;
        for (int[] q : pt) best = Math.min(best, sdAt(q[0], q[1], cx, cy));
        return best;
    }

    private boolean canStartCutscene() {
        player p = gp.player;
        return !p.isDead && !p.scripted && p.isNotPunching() && !gp.dialogOn && !gp.inspectOn && !gp.chatMode
            && !gp.isSkillVActive() && !gp.domainManager.isFading();
    }

    private void checkTriggers() {
        if (cooldown > 0 || !canStartCutscene()) { pushTimer = 0; return; }
        player p = gp.player;
        double fx = p.x + FOOT_X, fy = p.y + FOOT_Y;

        if (gp.gameState == GamePanel.GameState.OUTSIDE) {
            double d = boxDist(OUT_X, OUT_Y);
            if (d > 400) { pushTimer = 0; return; }
            gp.domainManager.prepareWaterDomain();   // peta domain dibangun di thread latar selagi pemain mendekat
            double bx = p.x + 24, by = p.y + 32;      // pusat hitbox
            double tx = OUT_X - bx, ty = OUT_Y - by, tl = Math.max(1, Math.hypot(tx, ty));

            // A. melompat (SPASI) ke arah air
            if (p.isJumping()) {
                String jd = p.getJumpDirection();
                double jx = jd.equals("left") ? -1 : jd.equals("right") ? 1 : 0, jy = jd.equals("up") ? -1 : jd.equals("down") ? 1 : 0;
                if (d < 44 && (jx * tx + jy * ty) / tl > 0.3) { startDive(true, fx, fy, p.isJumpAirborne()); return; }
                pushTimer = 0;
                return;
            }
            // B. mendorong karakter ke arah air sampai tertahan di tepinya
            double mx = (gp.KeyH.rightPressed ? 1 : 0) - (gp.KeyH.leftPressed ? 1 : 0);
            double my = (gp.KeyH.downPressed ? 1 : 0) - (gp.KeyH.upPressed ? 1 : 0);
            double ml = Math.hypot(mx, my);
            if (ml > 0 && d < 3 && (mx * tx + my * ty) / (ml * tl) > 0.4) {
                if (++pushTimer >= 6) { startDive(true, fx, fy, false); return; }
            } else {
                pushTimer = 0;
            }
        } else if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager.isWaterDomain()) {
            WaterDomainMap m = gp.domainManager.getWaterMap();
            if (m == null) return;
            double d = boxDist(m.poolX, m.poolY);
            if (d < 72 && gp.KeyH.interactPressed) {
                gp.KeyH.interactPressed = false;
                startDive(false, fx, fy, false);
            }
        }
    }

    // ================================================================== percikan

    private void splash(double wx, double wy, int count, double power) {
        int[] cols = {0xFFFFFF, 0xDCF3FF, 0x9CD4F2, 0x60AADC};
        for (int i = 0; i < count; i++) {
            Pt q = new Pt();
            double a = Math.toRadians(-90 + (rnd.nextDouble() - 0.5) * 130);
            double sp = (3.2 + rnd.nextDouble() * 4.6) * power;
            q.x = wx + (rnd.nextDouble() - 0.5) * 40;
            q.y = wy + (rnd.nextDouble() - 0.5) * 10;
            q.vx = Math.cos(a) * sp * 0.8;
            q.vy = Math.sin(a) * sp * 1.25;
            q.max = q.life = 22 + rnd.nextInt(16);
            q.col = cols[rnd.nextInt(cols.length)];
            q.size = rnd.nextInt(3) == 0 ? 6 : 3;
            pts.add(q);
        }
        for (int k = 0; k < 3; k++) {
            Ring r = new Ring();
            r.x = wx; r.y = wy; r.delay = k * 5; r.max = 30 + k * 4;
            rings.add(r);
        }
    }

    private void updateFx() {
        for (int i = pts.size() - 1; i >= 0; i--) {
            Pt q = pts.get(i);
            q.x += q.vx; q.y += q.vy; q.vy += 0.42; q.vx *= 0.985;
            if (--q.life <= 0) pts.remove(i);
        }
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring r = rings.get(i);
            if (r.delay > 0) { r.delay--; continue; }
            if (++r.age >= r.max) rings.remove(i);
        }
    }

    // ================================================================== gambar

    private void drawPoolAt(Graphics2D g2, BufferedImage[] frames, int cx, int cy) {
        if (frames == null) return;
        int sx = cx - gp.player.x + gp.player.screenX - WaterPortalArt.CX * SCALE;
        int sy = cy - gp.player.y + gp.player.screenY - WaterPortalArt.CY * SCALE;
        int w = WaterPortalArt.W * SCALE, h = WaterPortalArt.H * SCALE;
        if (sx > gp.viewMaxX() || sx + w < gp.viewMinX() || sy > gp.viewMaxY() || sy + h < gp.viewMinY()) return;
        int f = (int) ((System.nanoTime() / 1_000_000L / WaterAnimator.FRAME_MS) % WaterPortalArt.F);
        g2.drawImage(frames[f], sx, sy, w, h, null);
    }

    /** Kolam dunia luar (dipanggil di dalam transform zoom, sebelum objek berdiri). */
    public void drawOutsidePool(Graphics2D g2) { drawPoolAt(g2, outFrames, OUT_X, OUT_Y); }

    /** Kolam keluar di Water Domain (dipanggil di dalam transform zoom, setelah lantai). */
    public void drawDomainPool(Graphics2D g2) {
        WaterDomainMap m = gp.domainManager.getWaterMap();
        if (m != null) drawPoolAt(g2, inFrames, m.poolX, m.poolY);
    }

    /** Percikan dan riak (di dalam transform zoom, setelah semua entitas supaya ada di depan pemain). */
    public void drawOverlay(Graphics2D g2) {
        if (pts.isEmpty() && rings.isEmpty()) return;
        int camX = gp.player.x, camY = gp.player.y, psx = gp.player.screenX, psy = gp.player.screenY;
        g2.setStroke(new BasicStroke(3));
        for (Ring r : rings) {
            if (r.delay > 0) continue;
            double p = r.age / (double) r.max;
            int rx = (int) Math.round(10 + 78 * p), ry = (int) Math.round(rx * 0.42);
            int sx = (int) Math.round(r.x) - camX + psx, sy = (int) Math.round(r.y) - camY + psy;
            g2.setColor(new Color(0xEE, 0xF9, 0xFF, (int) Math.round((1 - p) * 210)));
            g2.drawOval(sx - rx, sy - ry, rx * 2, ry * 2);
        }
        g2.setStroke(new BasicStroke(1));
        for (Pt q : pts) {
            int sx = (int) Math.round(q.x) - camX + psx, sy = (int) Math.round(q.y) - camY + psy;
            int alpha = (int) Math.round(255 * Math.min(1.0, q.life / (q.max * 0.5)));
            g2.setColor(new Color((q.col >> 16) & 255, (q.col >> 8) & 255, q.col & 255, Math.max(0, Math.min(255, alpha))));
            g2.fillRect(sx, sy, q.size, q.size);
        }
    }

    /** Petunjuk di bawah layar. */
    public void drawHint(Graphics2D g2) {
        if (st != St.IDLE || cooldown > 0 || gp.dialogOn) return;
        String hint = null;
        if (gp.gameState == GamePanel.GameState.OUTSIDE) {
            if (boxDist(OUT_X, OUT_Y) < 170) hint = "...";
        } else if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager.isWaterDomain()) {
            WaterDomainMap m = gp.domainManager.getWaterMap();
            if (m != null && boxDist(m.poolX, m.poolY) < 72) hint = "[E]";
        }
        if (hint == null) return;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(255, 255, 150, 225));
        g2.drawString(hint, gp.screenWidth / 2 - g2.getFontMetrics().stringWidth(hint) / 2,
            gp.screenHeight - UI.Hotbar.RESERVED_HEIGHT - 40);
    }

    /** Layar gelap transisi (digambar paling akhir, menutupi semuanya). */
    public void drawFade(Graphics2D g2) {
        if (fade <= 0) return;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fade / 255f)));
        g2.setColor(new Color(2, 8, 16));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(old);
    }
}
