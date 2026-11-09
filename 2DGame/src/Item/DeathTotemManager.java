package Item;

import Entitas.Entity;
import Entitas.Skeleton;
import Item.DeathTotem.CapturedType;
import Main.GamePanel;
import Tamed.GolemMinion;
import Tamed.GolemiteMinion;
import Tamed.UndeadMinion;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class DeathTotemManager {

    private final GamePanel gp;
    private final DeathTotem totem;
    private final List<Entity> minions = new ArrayList<>();

    private int cooldownTimer = 0;
    private static final int COOLDOWN_MAX = 100;
    private int activeRegenTimer = 0;

    // Pion yang ketinggalan lebih jauh dari ini (dalam tile, dari player) langsung teleport ke player.
    // 12 tile = udah di luar layar (layar cuma 16x12 tile), jadi gak kejadian pas pion cuma lagi ngejar musuh deket.
    private static final int TELEPORT_DISTANCE_TILES = 12;
    private static final int TELEPORT_RETRY_TICKS    = 30; // belum ada spot kosong -> coba lagi tiap 0.5 detik
    private static final int TELEPORT_MAX_RING       = 4;  // cari spot kosong sampai 4 tile dari player
    private static final int PATH_CHECK_STEP         = 8;  // jarak antar titik cek jalur lurus (px)
    private static final int[][] RING_DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
    };
    private int teleportRetryTimer = 0;

    public DeathTotemManager(GamePanel gp) {
        this.gp    = gp;
        this.totem = new DeathTotem();
    }

    public DeathTotem   getTotem()   { return totem; }
    public List<Entity> getMinions() { return minions; }

    public int getCooldownSeconds() {
        return (int) Math.ceil((double) cooldownTimer / 60);
    }

    // ── CAPTURE ──────────────────────────────────────────────────────────────

    public void tryCapture(Entity killed) {
        if (!isPlayerHasTotem()) return;
        if (!totem.canCapture())  return;

        CapturedType type = null;
        if (killed instanceof Skeleton)      type = CapturedType.SKELETON;
        if (killed instanceof Entitas.Golem) type = CapturedType.GOLEM;
        if (killed instanceof Entitas.Golemite) type = CapturedType.GOLEMITE;

        if (type != null) {
            boolean ok = totem.capture(type);
            if (ok && gp.speechBubble != null) {
                gp.speechBubble.show(capitalize(type.name()) + " soul captured! Press T to spawn.");
            }
        }
    }

    // ── SPAWN dari slot tertentu (dipanggil dari TotemUI) ────────────────────

    public void trySpawnFromSlot(int slotIndex) {
        if (cooldownTimer > 0) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("Skill on cooldown! " + getCooldownSeconds() + "s left.");
            return;
        }
        if (totem.hasPawnAlive()) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("Recall your active pawn first! (T)");
            return;
        }

        int spawnX = gp.player.x + gp.tileSize;
        int spawnY = gp.player.y;

        // Spot spawn kehalang collision (tembok / object / air / musuh / NPC) -> batal total:
        // pion belum dibikin, slot gak berubah, cooldown gak kepakai.
        Rectangle pawnBox = totem.getPawnSolidArea(slotIndex);
        if (pawnBox != null && !isSpotFree(pawnBox, spawnX, spawnY, null)) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("No space to summon here! Move to an open spot.");
            return;
        }

        Object pawn = totem.spawnFromSlot(slotIndex, gp, spawnX, spawnY);
        if (pawn instanceof GolemiteMinion) {   // harus sebelum GolemMinion (turunannya)
            minions.add((GolemiteMinion) pawn);
            if (gp.speechBubble != null) gp.speechBubble.show("Golemite revived!");
            cooldownTimer = COOLDOWN_MAX;
        } else if (pawn instanceof GolemMinion) {
            minions.add((GolemMinion) pawn);
            if (gp.speechBubble != null) gp.speechBubble.show("Golem revived!");
            cooldownTimer = COOLDOWN_MAX;
        } else if (pawn instanceof UndeadMinion) {
            minions.add((UndeadMinion) pawn);
            if (gp.speechBubble != null) gp.speechBubble.show("Skeleton revived!");
            cooldownTimer = COOLDOWN_MAX;
        }
    }

    // ── RECALL / T key ────────────────────────────────────────────────────────

    public void trySpawn() {
        // FIX: totem gak ada di backpack → tombol T gak ngapa-ngapain sama sekali
        // (gak ada pesan, gak ada cooldown, gak ada recall).
        if (!isPlayerHasTotem()) return;

        if (cooldownTimer > 0) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("Skill on cooldown! " + getCooldownSeconds() + "s left.");
            return;
        }
        if (totem.hasPawnAlive()) {
            recallActivePawn();
            return;
        }
        // Totem ADA di backpack tapi belum ada soul → baru munculin pesan ini
        if (totem.isEmpty()) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("Totem is empty! Defeat an enemy first.");
            return;
        }
        trySpawnFromSlot(0);
    }

    private void recallActivePawn() {
        int currentHP = 0;
        for (Entity m : minions) {
            if (m.alive) { currentHP = m.life; break; }
            if (m instanceof UndeadMinion) ((UndeadMinion)m).alive = false;
            if (m instanceof GolemMinion)  ((GolemMinion)m).alive  = false;
            if (m instanceof GolemiteMinion) ((GolemiteMinion)m).alive = false;
        }
        minions.removeIf(m -> {
            if (m instanceof UndeadMinion) { ((UndeadMinion)m).alive = false; return true; }
            if (m instanceof GolemMinion)  { ((GolemMinion)m).alive  = false; return true; }
            if (m instanceof GolemiteMinion) { ((GolemiteMinion)m).alive = false; return true; }
            return false;
        });
        totem.recallPawn(currentHP);
        if (gp.speechBubble != null)
            gp.speechBubble.show("Pawn recalled. Soul still stored.");
        cooldownTimer = COOLDOWN_MAX;
    }

    // ── DELETE SLOT (dipanggil dari TotemUI) ──────────────────────────────────

    /**
     * Hapus soul di slot tertentu secara permanen.
     * Pion yang lagi aktif (keluar) gak boleh dihapus — harus di-recall dulu.
     *
     * @return true kalau berhasil dihapus
     */
    public boolean tryDeleteSlot(int slotIndex) {
        List<CapturedMob> slots = totem.getSlots();
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;

        if (totem.getActivePawnSlot() == slotIndex) {
            if (gp.speechBubble != null)
                gp.speechBubble.show("Recall this pawn first before deleting it! (T)");
            return false;
        }

        String name = slots.get(slotIndex).getTypeName();
        totem.removeSlot(slotIndex);

        if (gp.speechBubble != null)
            gp.speechBubble.show(name + " soul deleted.");
        return true;
    }

    // ── EXP DISTRIBUTION (dipanggil saat pion kill musuh) ────────────────────

    /**
     * Panggil ini tiap kali pion aktif berhasil membunuh musuh.
     * exp = nilai exp musuh yang mati.
     */
    public void onPawnKill(int exp) {
        List<String> leveledMobs = totem.distributeExp(exp);
        if (gp.speechBubble != null) {
            for (String name : leveledMobs) {
                gp.speechBubble.show(name + " leveled up!");
            }
        }
    }

    // ── UPDATE & DRAW ─────────────────────────────────────────────────────────

    public void update() {
        if (cooldownTimer > 0) cooldownTimer--;
        if (teleportRetryTimer > 0) teleportRetryTimer--;

        minions.removeIf(m -> {
            if (m instanceof UndeadMinion) {
                UndeadMinion um = (UndeadMinion) m;
                if (!um.isAlive() && um.isDeathAnimDone()) {
                    totem.releasePawn();
                    return true;
                }
            }
            if (m instanceof GolemMinion) {
                GolemMinion gm = (GolemMinion) m;
                if (!gm.isAlive() && gm.isDeathAnimDone()) {
                    totem.releasePawn();
                    return true;
                }
            }
            if (m instanceof GolemiteMinion) {
                GolemiteMinion gt = (GolemiteMinion) m;
                if (!gt.isAlive() && gt.isDeathAnimDone()) {
                    totem.releasePawn();
                    return true;
                }
            }
            return false;
        });

        activeRegenTimer++;
        if (activeRegenTimer >= 60) activeRegenTimer = 0;

        for (Entity m : minions) {
            m.update();
            if (m.alive && m.life < m.maxLife) {
                if (activeRegenTimer == 0) {
                    m.life += 15;
                    if (m.life > m.maxLife) m.life = m.maxLife;
                }
            }
            if (m.alive) teleportIfLeftBehind(m);
        }
    }

    // ── TELEPORT KE PLAYER (pion ketinggalan jauh) ────────────────────────────

    private void teleportIfLeftBehind(Entity m) {
        int plCX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width  / 2;
        int plCY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;
        int myCX = m.x + m.solidArea.x + m.solidArea.width  / 2;
        int myCY = m.y + m.solidArea.y + m.solidArea.height / 2;
        double dist = Math.hypot(plCX - myCX, plCY - myCY);
        if (dist <= TELEPORT_DISTANCE_TILES * gp.tileSize) return;
        if (teleportRetryTimer > 0) return;

        int[] spot = findFreeSpotNearPlayer(m);
        if (spot == null) {                 // semua spot sekitar player penuh -> jangan maksa, coba lagi bentar lagi
            teleportRetryTimer = TELEPORT_RETRY_TICKS;
            return;
        }
        m.x = spot[0];
        m.y = spot[1];
    }

    /**
     * Cari spot kosong di sekitar player (cincin 1..4 tile, 8 arah). Pertama yang jalurnya lurus dari player gak
     * ketutup tembok (biar pion gak muncul di balik tembok tipis dan nyangkut), kalau gak ada baru spot kosong biasa.
     * Return {x, y} atau null kalau gak ada spot yang bebas collision.
     */
    private int[] findFreeSpotNearPlayer(Entity m) {
        for (int pass = 0; pass < 2; pass++) {
            for (int ring = 1; ring <= TELEPORT_MAX_RING; ring++) {
                for (int[] d : RING_DIRS) {
                    int tx = gp.player.x + d[0] * ring * gp.tileSize;
                    int ty = gp.player.y + d[1] * ring * gp.tileSize;
                    if (!isSpotFree(m.solidArea, tx, ty, m)) continue;
                    if (pass == 0 && !isPathClearFromPlayer(m.solidArea, tx, ty)) continue;
                    return new int[] { tx, ty };
                }
            }
        }
        return null;
    }

    /** True kalau jalur lurus dari posisi player ke (tx, ty) gak nabrak collision dunia sama sekali. */
    private boolean isPathClearFromPlayer(Rectangle solid, int tx, int ty) {
        int dx = tx - gp.player.x, dy = ty - gp.player.y;
        int steps = Math.max(Math.abs(dx), Math.abs(dy)) / PATH_CHECK_STEP;
        for (int i = 1; i < steps; i++) {
            int px = gp.player.x + dx * i / steps;
            int py = gp.player.y + dy * i / steps;
            if (gp.colision.isBlockedByWorld(solid, px, py)) return false;
        }
        return true;
    }

    // ── CEK SPOT KOSONG (dipakai spawn & teleport) ────────────────────────────

    /**
     * True kalau hitbox `solid` (relatif ke x, y) yang berdiri di (worldX, worldY) gak nabrak collision dunia
     * (tembok / object / air, sesuai gameState) dan gak numpuk sama player, NPC, atau musuh yang lagi hidup.
     * `self` = pion yang lagi dicek supaya gak dianggap nabrak dirinya sendiri; null kalau pionnya belum dibikin.
     */
    public boolean isSpotFree(Rectangle solid, int worldX, int worldY, Entity self) {
        if (gp.colision.isBlockedByWorld(solid, worldX, worldY)) return false;

        Rectangle box = new Rectangle(worldX + solid.x, worldY + solid.y, solid.width, solid.height);
        if (overlapsLive(box, gp.player, self)) return false;
        if (overlapsAnyLive(box, java.util.Arrays.asList(gp.outsideNpcs()), self))     return false;
        if (overlapsAnyLive(box, java.util.Arrays.asList(gp.outsideMonsters()), self)) return false;

        if (gp.gameState == GamePanel.GameState.DOMAIN && gp.domainManager != null) {
            if (overlapsAnyLive(box, gp.domainManager.getSkeletons(), self))      return false;
            if (overlapsAnyLive(box, gp.domainManager.getRoomSkeletons(), self))  return false;
            if (overlapsAnyLive(box, gp.domainManager.getGolemites(), self))      return false;
            if (overlapsAnyLive(box, gp.domainManager.getGolems(), self))         return false;
            if (overlapsAnyLive(box, gp.domainManager.getDrownedSoldiers(), self)) return false;
            if (overlapsLive(box, gp.domainManager.getBossSkeleton(), self))      return false;
        }
        return true;
    }

    private boolean overlapsAnyLive(Rectangle box, Iterable<? extends Entity> list, Entity self) {
        if (list == null) return false;
        for (Entity e : list) if (overlapsLive(box, e, self)) return true;
        return false;
    }

    private boolean overlapsLive(Rectangle box, Entity e, Entity self) {
        if (e == null || e == self || !isLive(e)) return false;
        return box.intersects(new Rectangle(e.x + e.solidArea.x, e.y + e.solidArea.y,
                                            e.solidArea.width, e.solidArea.height));
    }

    /** Entity mati / belum punya hitbox gak dianggap collision. Pola sama kayak GolemMinion.isTargetableEnemy. */
    private static boolean isLive(Entity e) {
        if (e.isDead || e.solidArea == null) return false;
        if (e instanceof Skeleton)               return ((Skeleton) e).alive;
        if (e instanceof Entitas.SkeletonKing)   return ((Entitas.SkeletonKing) e).alive;
        if (e instanceof Entitas.Golem)          return ((Entitas.Golem) e).isAlive();
        if (e instanceof Entitas.Golemite)       return ((Entitas.Golemite) e).isAlive();
        if (e instanceof Entitas.Wolf)           return ((Entitas.Wolf) e).isAlive();
        if (e instanceof Entitas.Werewolf)       return ((Entitas.Werewolf) e).isAlive();
        if (e instanceof Entitas.DrownedSoldier) return ((Entitas.DrownedSoldier) e).isAlive();
        return true;
    }

    public void draw(Graphics2D g2) {
        for (Entity m : minions) m.draw(g2);
    }

    // ── UPGRADE ───────────────────────────────────────────────────────────────

    public boolean tryUpgradeTotem() {
        boolean ok = totem.tryUpgrade(gp);
        if (gp.speechBubble != null) {
            if (ok) gp.speechBubble.show("Totem upgraded! Capacity: " + totem.getCapacity());
            else    gp.speechBubble.show("Not enough resources to upgrade!");
        }
        return ok;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private boolean isPlayerHasTotem() {
        for (ItemStack stack : ItemManager.inventory) {
            if (stack != null && stack.item == ItemManager.get().DEATH_TOTEM) return true;
        }
        return false;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}