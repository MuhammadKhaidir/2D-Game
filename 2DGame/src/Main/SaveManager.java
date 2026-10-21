package Main;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SaveManager {

    private static final String SAVE_DIR = "saves/";
    private static final int MAX_SLOTS = 3;

    public static void init() {
        new File(SAVE_DIR).mkdirs();
    }

    public static void save(GamePanel gp, int slot) {
        SaveData data = new SaveData();
        data.saveName     = "Slot " + slot;
        data.saveTime     = System.currentTimeMillis();

        // Player (Lama)
        data.playerX          = gp.player.x;
        data.playerY          = gp.player.y;
        // Max HP disimpan tanpa bonus Max HP Potion (buff-nya sementara, tidak ikut tersimpan)
        data.playerMaxLife    = gp.player.getBaseMaxLife();
        data.playerLife       = Math.min(gp.player.life, data.playerMaxLife);
        data.playerLevel      = gp.player.level;
        data.playerExp        = gp.player.exp;
        data.playerNextLevelExp = gp.player.nextLevelExp;
        data.playerStamina    = gp.player.stamina;
        data.attributePoints  = gp.player.attributePoints;
        data.strength         = gp.player.strength;
        data.dexterity        = gp.player.dexterity;

        // ===== BARU: Daftarkan Stat Tambahan dari UI Agar Ikut Tersimpan =====
        data.def              = gp.player.def;
        data.punchDamage      = gp.player.punchDamage;
        data.baseSpeed        = gp.player.baseSpeed;
        data.dodge            = gp.player.dodge;
        data.playerMaxStamina = gp.player.maxStamina;

        // State
        data.gameState = gp.gameState.name();
        data.activeDomainId = gp.domainManager.getActiveDomainId();
        // World objects
        data.removedObjectIndexes.clear();
        for (int i = 0; i < gp.obj.length; i++) {
            if (gp.obj[i] == null) {
                data.removedObjectIndexes.add(i);
            }
        }
        // Hotbar
        data.hotbarNames = new ArrayList<>();
        if (gp.hotbar != null) {
            for (int i = 0; i < UI.Hotbar.SLOT_COUNT; i++) {
                Item.Item hb = gp.hotbar.getSlot(i);
                data.hotbarNames.add(hb != null ? hb.name : "");
            }
        }
        // Isi peti penyimpanan
        data.chestNames = new ArrayList<>();
        data.chestAmounts = new ArrayList<>();
        if (gp.chest != null) {
            for (Item.ItemStack stack : gp.chest.getContents()) {
                if (stack != null && stack.item != null) {
                    data.chestNames.add(stack.item.name);
                    data.chestAmounts.add(stack.amount);
                }
            }
        }
        // Inventory
        data.inventoryNames.clear();
        data.inventoryAmounts.clear();
        for (Item.ItemStack stack : Item.ItemManager.inventory) {
            if (stack != null && stack.item != null) {
                data.inventoryNames.add(stack.item.name);
                data.inventoryAmounts.add(stack.amount);
            }
        }
        
     // ── DeathTotem multi-slot save ──
        Item.DeathTotem totem = gp.deathTotemManager.getTotem();
        data.totemCapacity = totem.getCapacity();
        data.totemSlotTypes.clear();
        data.totemSlotHP.clear();
        data.totemSlotMaxHP.clear();
        data.totemSlotLevel.clear();
        data.totemSlotExp.clear();
        data.totemSlotBaseHP.clear();
        data.totemSlotBaseAtk.clear();

        for (Item.CapturedMob mob : totem.getSlots()) {
            data.totemSlotTypes.add(mob.type.name());
            data.totemSlotHP.add(mob.storedHP);
            data.totemSlotMaxHP.add(mob.maxHP);
            data.totemSlotLevel.add(mob.level);
            data.totemSlotExp.add(mob.currentExp);
            data.totemSlotBaseHP.add(mob.baseMaxHP);
            data.totemSlotBaseAtk.add(mob.baseAttack);
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SAVE_DIR + "slot" + slot + ".dat"))) {
            oos.writeObject(data);
            System.out.println("[Save] Tersimpan di slot " + slot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SaveData load(int slot) {
        File f = new File(SAVE_DIR + "slot" + slot + ".dat");
        if (!f.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (SaveData) ois.readObject();
        } catch (java.io.InvalidClassException e) {
            // Save file dari versi lama — format berubah, hapus otomatis
            System.out.println("[SaveManager] Save slot " + slot
                + " format lama (incompatible), dihapus otomatis.");
            f.delete();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void applyToGame(GamePanel gp, SaveData data) {
        gp.player.buffs.clear(); // buff potion tidak ikut tersimpan; bersihin dulu sebelum stat di-load
        gp.player.x             = data.playerX;
        gp.player.y             = data.playerY;
        gp.player.life          = data.playerLife;
        gp.player.maxLife       = data.playerMaxLife;
        gp.player.level         = data.playerLevel;
        gp.player.exp           = data.playerExp;
        gp.player.nextLevelExp  = data.playerNextLevelExp;
        gp.player.stamina       = data.playerStamina;
        gp.player.attributePoints = data.attributePoints;
        gp.player.strength      = data.strength;
        gp.player.dexterity     = data.dexterity;

        // ===== BARU: Muat Stat Tambahan Kembali ke Player Saat Load Game =====
        gp.player.def           = data.def;
        gp.player.punchDamage   = data.punchDamage;
        gp.player.baseSpeed     = data.baseSpeed;
        gp.player.dodge         = data.dodge;
        gp.player.maxStamina    = data.playerMaxStamina;

        switch (data.gameState) {
            case "DOMAIN":
                gp.domainManager.forceEnterDomain(data.activeDomainId != null ? data.activeDomainId : "Tomb");
                break;
            case "INSIDE":
                gp.gameState = GamePanel.GameState.INSIDE;
                break;
            default:
                gp.gameState = GamePanel.GameState.OUTSIDE;
        }
        // Restore world — null-kan objek yang sudah dihapus saat save
        for (int idx : data.removedObjectIndexes) {
            if (idx >= 0 && idx < gp.obj.length) {
                gp.obj[idx] = null;
            }
        }
        // Restore inventory
        Item.ItemManager.inventory.clear();
        Item.ItemManager im = Item.ItemManager.get();
        for (int i = 0; i < data.inventoryNames.size(); i++) {
            String name   = data.inventoryNames.get(i);
            int amount    = data.inventoryAmounts.get(i);
            Item.Item item = getItemByName(im, name);
            if (item != null) {
                Item.ItemManager.inventory.add(new Item.ItemStack(item, amount));
            }
        }
        // Restore peti penyimpanan (save lama yang belum punya peti → chestNames null → peti dikosongin)
        if (gp.chest != null) {
            gp.chest.clearContents();
            if (data.chestNames != null && data.chestAmounts != null) {
                for (int i = 0; i < data.chestNames.size() && i < data.chestAmounts.size(); i++) {
                    Item.Item chestItem = getItemByName(im, data.chestNames.get(i));
                    if (chestItem != null) gp.chest.restore(chestItem, data.chestAmounts.get(i));
                }
            }
        }
        // Restore hotbar (save lama yang belum punya hotbar → hotbarNames null → hotbar dikosongin)
        if (gp.hotbar != null) {
            gp.hotbar.clear();
            if (data.hotbarNames != null) {
                for (int i = 0; i < data.hotbarNames.size() && i < UI.Hotbar.SLOT_COUNT; i++) {
                    String hbName = data.hotbarNames.get(i);
                    if (hbName == null || hbName.isEmpty()) continue;
                    gp.hotbar.setSlot(i, getItemByName(im, hbName));
                }
            }
        }
     // ── DeathTotem multi-slot restore ──
        java.util.List<Item.CapturedMob> savedSlots = new java.util.ArrayList<>();
        for (int i = 0; i < data.totemSlotTypes.size(); i++) {
            Item.DeathTotem.CapturedType type =
                Item.DeathTotem.CapturedType.valueOf(data.totemSlotTypes.get(i));
            Item.CapturedMob mob = new Item.CapturedMob(type);
            mob.storedHP   = data.totemSlotHP.get(i);
            mob.maxHP      = data.totemSlotMaxHP.get(i);
            mob.level      = data.totemSlotLevel.get(i);
            mob.currentExp = data.totemSlotExp.get(i);
            // baseMaxHP dan baseAttack di CapturedMob itu final, jadi ga bisa di-set langsung.
            // Nilai mereka udah bener dari constructor karena kita passing type yang sama.
            savedSlots.add(mob);
        }
        Item.DeathTotem totem = gp.deathTotemManager.getTotem();
        totem.restoreState(savedSlots, data.totemCapacity);
    }

    // Cek slot mana yang ada isinya
    public static SaveData[] getAllSlots() {
        SaveData[] slots = new SaveData[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots[i] = load(i + 1);
        }
        return slots;
    }

    private static Item.Item getItemByName(Item.ItemManager im, String name) {
        switch (name) {
            case "Log":       return im.LOG;
            case "Ore":       return im.ORE;
            case "Slime Gel": return im.SLIME_GEL;
            case "Sword":     return im.SWORD;
            case "Axe":       return im.AXE;
            case "Bone":         return im.BONE;          // DROP SKELETON
            case "Skull":        return im.SKULL;         // DROP SKELETON
            case "Death Totem":  return im.DEATH_TOTEM;  // DROP SKELETON LANGKA
            case "Skeleton Key": return im.SKELETON_KEY; // DROP SKELETON LANGKA
            case "Bone Sword":   return im.BONE_SWORD;   // DARK MERCHANT
            case "Coin":                                  // CURRENCY
            case "Gold":         return im.COIN;          // nama lama, biar save lama tetap kebaca
            case "Soul of Skeleton": return im.SOUL_OF_SKELETON;
            case "Helm":         return im.HELM;
            case "Health Potion": return im.getPotion(Item.PotionType.SMALL_HP); // nama lama, biar save lama tetap kebaca
            default:
                Item.Item potion = im.getPotionByName(name); // CONSUMABLE (semua potion di PotionType)
                if (potion != null) return potion;
                System.out.println("[SaveManager] Item tidak dikenal: " + name);
                return null;
        }
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }
}