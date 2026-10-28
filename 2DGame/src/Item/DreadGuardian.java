package Item;

import Entitas.Entity;
import Entitas.Golemite;
import Entitas.Skeleton;
import Entitas.SkeletonKing;
import Main.GamePanel;
import Object.PlayerArmor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DreadGuardian - set effect Dread Armor.
 *
 * Tiap player kena damage (damage beneran masuk, bukan dodge / kebal SkillV) muncul 1 bayangan, maks 3 sekaligus.
 *  - Bayangan langsung ngejar musuh yang mukul. Begitu nyentuh, dia MERASUKI musuh itu: nempel di badan musuh,
 *    ngasih damage 50% basic attack player, lalu ngilang. Sekali pakai, bukan summon permanen.
 *  - Kalau musuhnya mati sebelum kena -> cari musuh terdekat lain. Kalau gak ada musuh sama sekali -> ngikut player
 *    (sampai ada musuh dekat).
 *  - Bayangan BUKAN Entity: gak masuk list monster/NPC/minion manapun, jadi gak bisa diserang, gak ngehalangin
 *    (gak ikut collision), dan cuma manggil takeDamage(int) polos (tanpa suara, knockback, atau efek on-hit lain).
 *  - Semua bayangan hilang kalau armor dilepas / player mati.
 */
public class DreadGuardian {

    public static final int MAX_SHADOWS = 3;

    private static final double DAMAGE_RATIO     = 0.5;   // 50% basic attack
    private static final double CHASE_SPEED      = 5.5;   // px/tick, sedikit di atas sprint player
    private static final double FOLLOW_SPEED     = 4.5;
    private static final double CONTACT_GAP      = 8;     // jarak ke hitbox musuh yang dianggap "nyentuh"
    private static final int    SEARCH_RADIUS_T  = 10;    // radius cari musuh baru (dalam tile)
    private static final int    SNAP_DISTANCE_T  = 14;    // kejauhan dari player (pindah area, dll) -> langsung nyusul
    private static final int    POSSESS_TICKS    = 24;    // lama animasi merasuki musuh sebelum bayangan ngilang
    private static final int    POSSESS_HIT_TICK = 8;     // damage masuk di tick ini
    private static final int    FADE_IN_TICKS    = 12;
    private static final int    CENTER_OFFSET_X  = 24;    // pusat hitbox player = (x + 24, y + 32)
    private static final int    CENTER_OFFSET_Y  = 32;
    private static final int[][] FOLLOW_SLOTS    = { { -40, 24 }, { 40, 24 }, { 0, 56 } };

    private final GamePanel gp;
    private final List<Shadow> shadows = new ArrayList<>();
    private final Map<String, BufferedImage> silhouetteCache = new HashMap<>();
    private PlayerArmor cachedFor;

    private static class Shadow {
        double cx, cy;            // pusat badan di world
        Entity target;            // null = ngikut player
        String dir = "down";
        int possessTick = -1;     // -1 = belum merasuki; >= 0 = lagi nempel di musuh
        boolean dealt = false;    // damage sudah masuk
        int animTick = 0;
        int age = 0;
        boolean moving = false;
    }

    public DreadGuardian(GamePanel gp) {
        this.gp = gp;
    }

    // ================= HOOK DARI PLAYER =================

    /** Dipanggil player.takeDamageFrom() setelah damage beneran masuk. attacker boleh null (sumber gak dikenal). */
    public void onPlayerHit(Entity attacker) {
        if (!isActiveSet() || gp.player.isDead) return;

        Entity target = isEnemy(attacker) ? attacker : null;

        if (shadows.size() < MAX_SHADOWS) {
            Shadow s = new Shadow();
            s.cx = gp.player.x + CENTER_OFFSET_X;
            s.cy = gp.player.y + CENTER_OFFSET_Y;
            s.dir = gp.player.direction != null ? gp.player.direction : "down";
            s.target = target != null ? target : nearestEnemy(s.cx, s.cy);
            shadows.add(s);
        }
        // bayangan yang lagi nganggur (ngikut player, belum nemu musuh) ikut ngejar yang mukul
        if (target != null) {
            for (Shadow s : shadows) {
                if (s.target == null && s.possessTick < 0) s.target = target;
            }
        }
    }

    public void clear() {
        shadows.clear();
        silhouetteCache.clear();
        cachedFor = null;
    }

    // ================= UPDATE =================

    public void update() {
        if (!isActiveSet() || gp.player.isDead) {
            if (!shadows.isEmpty()) clear();
            return;
        }
        if (!inCombatArea()) return;   // di dalam rumah dll: bayangan nunggu

        double pcx = gp.player.x + CENTER_OFFSET_X;
        double pcy = gp.player.y + CENTER_OFFSET_Y;

        Iterator<Shadow> it = shadows.iterator();
        while (it.hasNext()) {
            Shadow s = it.next();
            s.age++;
            s.animTick++;

            if (s.possessTick >= 0) {                 // lagi merasuki musuh
                if (updatePossess(s)) it.remove();    // selesai -> bayangan ngilang
                continue;
            }

            // kejauhan dari player (pindah area / teleport) -> langsung nyusul, lepas target
            if (dist(s.cx, s.cy, pcx, pcy) > gp.tileSize * SNAP_DISTANCE_T) {
                s.cx = pcx;
                s.cy = pcy;
                s.target = null;
            }

            // target mati / hilang -> musuh terdekat lain -> kalau gak ada, ngikut player
            if (!isEnemy(s.target)) {
                s.target = nearestEnemy(s.cx, s.cy);
            }

            if (s.target != null) updateChase(s);
            else updateFollow(s, shadows.indexOf(s), pcx, pcy);
        }
    }

    private void updateChase(Shadow s) {
        Rectangle box = hitboxOf(s.target);
        if (distToBox(s.cx, s.cy, box) <= CONTACT_GAP) {
            s.moving = false;
            s.possessTick = 0;                        // nyentuh -> rasuki musuh
            s.dealt = false;
            return;
        }
        moveToward(s, box.getCenterX(), box.getCenterY(), CHASE_SPEED, 0);
    }

    /** @return true kalau animasi rasuk selesai dan bayangan harus dihapus. */
    private boolean updatePossess(Shadow s) {
        s.moving = false;
        boolean alive = isEnemy(s.target);

        if (!s.dealt && !alive) {                     // musuh keburu mati sebelum kena: batal, cari musuh lain
            s.possessTick = -1;
            s.target = null;
            return false;
        }
        if (alive) {                                  // nempel di badan musuh (ikut kalau musuhnya gerak)
            Rectangle box = hitboxOf(s.target);
            s.cx = box.getCenterX();
            s.cy = box.getCenterY();
        }
        s.possessTick++;
        if (!s.dealt && s.possessTick >= POSSESS_HIT_TICK) {
            s.target.takeDamage(shadowDamage());      // polos: gak ada knockback / suara / efek on-hit
            s.dealt = true;
        }
        return s.possessTick >= POSSESS_TICKS;
    }

    private void updateFollow(Shadow s, int index, double pcx, double pcy) {
        int[] slot = FOLLOW_SLOTS[Math.max(0, Math.min(index, FOLLOW_SLOTS.length - 1))];
        double tx = pcx + slot[0];
        double ty = pcy + slot[1];
        if (dist(s.cx, s.cy, tx, ty) > 6) {
            moveToward(s, tx, ty, FOLLOW_SPEED, 6);
        } else {
            s.moving = false;
            s.dir = gp.player.direction != null ? gp.player.direction : s.dir;
        }
    }

    private void moveToward(Shadow s, double tx, double ty, double maxSpeed, double stopGap) {
        double dx = tx - s.cx, dy = ty - s.cy;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d <= stopGap || d == 0) { s.moving = false; return; }
        double step = (maxSpeed == CHASE_SPEED) ? maxSpeed                       // ngejar musuh: kecepatan penuh
                : Math.min(maxSpeed, Math.max(1.0, d * 0.2 + 1.0));              // ngikut player: melambat pas dekat
        step = Math.min(step, d - stopGap);
        s.cx += dx / d * step;                                                   // tembus tile: bayangan gak kena collision
        s.cy += dy / d * step;
        s.moving = true;
        s.dir = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "right" : "left") : (dy > 0 ? "down" : "up");
    }

    /** 50% basic attack (pukulan biasa tipe 1, tanpa pengali combo), minimal 1. */
    private int shadowDamage() {
        return Math.max(1, (int) Math.round(gp.player.getTotalDamage() * DAMAGE_RATIO));
    }

    // ================= CARI MUSUH =================

    private List<Entity> enemies() {
        List<Entity> list = new ArrayList<>();
        if (gp.gameState == GamePanel.GameState.OUTSIDE) {
            if (gp.monsters != null) {
                for (Entity m : gp.monsters) if (m != null) list.add(m);
            }
        } else if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null) {
            if (gp.domainManager.getSkeletons() != null) list.addAll(gp.domainManager.getSkeletons());
            if (gp.domainManager.getRoomSkeletons() != null) list.addAll(gp.domainManager.getRoomSkeletons());
            if (gp.domainManager.getGolemites() != null) list.addAll(gp.domainManager.getGolemites());
            if (gp.domainManager.getBossSkeleton() != null) list.add(gp.domainManager.getBossSkeleton());
        }
        return list;
    }

    private boolean isEnemy(Entity e) {
        if (e == null || e == gp.player) return false;
        return isAlive(e) && enemies().contains(e);
    }

    private static boolean isAlive(Entity e) {
        if (e == null || e.isDead) return false;
        if (e instanceof Skeleton)     return ((Skeleton) e).alive;
        if (e instanceof SkeletonKing) return ((SkeletonKing) e).alive;
        if (e instanceof Golemite)     return ((Golemite) e).isAlive() && e.life > 0;
        return true;
    }

    private Entity nearestEnemy(double cx, double cy) {
        Entity best = null;
        double bestD = gp.tileSize * SEARCH_RADIUS_T;
        for (Entity e : enemies()) {
            if (!isAlive(e)) continue;
            double d = distToBox(cx, cy, hitboxOf(e));
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    private static Rectangle hitboxOf(Entity e) {
        return new Rectangle(e.x + e.solidArea.x, e.y + e.solidArea.y, e.solidArea.width, e.solidArea.height);
    }

    private static double distToBox(double cx, double cy, Rectangle r) {
        double dx = Math.max(Math.max(r.x - cx, 0), cx - (r.x + r.width));
        double dy = Math.max(Math.max(r.y - cy, 0), cy - (r.y + r.height));
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double dist(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ================= STATE =================

    private boolean isActiveSet() {
        return gp.player != null && gp.player.getArmorSet() == ArmorSet.DREAD;
    }

    private boolean inCombatArea() {
        return gp.gameState == GamePanel.GameState.OUTSIDE || gp.gameState == GamePanel.GameState.DOMAIN;
    }

    // ================= DRAW =================

    /** Dipanggil GamePanel di dalam transform zoom dunia, setelah entity lain digambar. */
    public void draw(Graphics2D g2) {
        if (shadows.isEmpty() || !isActiveSet() || !inCombatArea()) return;
        PlayerArmor armor = gp.player.currentArmor;
        if (armor != cachedFor) {
            silhouetteCache.clear();
            cachedFor = armor;
        }

        int size = gp.tileSize * 2;
        Composite old = g2.getComposite();
        for (Shadow s : shadows) {
            boolean possessing = s.possessTick >= 0;
            String state = s.moving ? "run" : "idle";
            int frame = s.moving ? (s.animTick / 5) % 3 : 0;
            BufferedImage img = silhouette(armor, state, frame, s.dir);
            if (img == null) continue;

            double wx = s.cx - CENTER_OFFSET_X;
            double wy = s.cy - CENTER_OFFSET_Y;
            int sx = (int) Math.round(wx - gp.player.x + gp.player.screenX);
            int sy = (int) Math.round(wy - gp.player.y + gp.player.screenY);
            int drawX = sx - gp.tileSize / 2;
            int drawY = sy - gp.tileSize / 3;

            float alpha = 0.62f * Math.min(1f, s.age / (float) FADE_IN_TICKS);
            int w = size, h = size;
            if (possessing) {                                   // menyusut & memudar masuk ke badan musuh
                float p = Math.min(1f, s.possessTick / (float) POSSESS_TICKS);
                alpha *= (1f - p);
                w = h = Math.max(4, Math.round(size * (1f - 0.65f * p)));
                drawX += (size - w) / 2;
                drawY += (size - h) / 2;

                int glow = Math.round(gp.tileSize * (0.9f - 0.6f * p));   // cincin ungu di badan musuh
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, 0.35f * (1f - p))));
                g2.setColor(new Color(150, 60, 220));
                g2.fillOval(sx + gp.tileSize / 2 - glow / 2, sy + gp.tileSize / 2 - glow / 2, glow, glow);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            g2.drawImage(img, drawX, drawY, w, h, null);
        }
        g2.setComposite(old);
    }

    /** Siluet gelap-keunguan dari sprite armor (badan + helm). Di-cache per state/frame/arah. */
    private BufferedImage silhouette(PlayerArmor armor, String state, int frame, String dir) {
        if (armor == null) return null;
        String key = state + ":" + frame + ":" + dir;
        BufferedImage cached = silhouetteCache.get(key);
        if (cached != null) return cached;

        BufferedImage body = armor.getBody(state, frame, dir);
        BufferedImage head = armor.getHead(state, frame, dir);
        BufferedImage ref = body != null ? body : head;
        if (ref == null) return null;

        BufferedImage buf = new BufferedImage(ref.getWidth(), ref.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gb = buf.createGraphics();
        if (body != null) gb.drawImage(body, 0, 0, null);
        if (head != null) gb.drawImage(head, 0, 0, null);
        gb.setComposite(AlphaComposite.SrcAtop);   // warnai cuma bagian yang ada gambarnya
        gb.setColor(new Color(30, 10, 60, 220));
        gb.fillRect(0, 0, buf.getWidth(), buf.getHeight());
        gb.dispose();

        silhouetteCache.put(key, buf);
        return buf;
    }
}
