package Item;

import Entitas.Entity;
import Entitas.Skeleton;
import Item.DeathTotem.CapturedType;
import Main.GamePanel;
import Tamed.GolemMinion;
import Tamed.GolemiteMinion;
import Tamed.UndeadMinion;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class DeathTotemManager {

    private final GamePanel gp;
    private final DeathTotem totem;
    private final List<Entity> minions = new ArrayList<>();

    private int cooldownTimer = 0;
    private static final int COOLDOWN_MAX = 100;
    private int activeRegenTimer = 0;

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
        }
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