package UI;

import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.ArrayList;
import Item.ItemStack;
import Item.ItemManager;
import Main.GamePanel;
import Object.Weapon;
import Object.PlayerArmor;

public class BackpackUI implements MouseWheelListener {
    public boolean visible = false;
    private int cols = 5;
    private int gridRows = 10; 
    private int slotSize = 52; 
    private int slotPadding = 8; 
    private int scrollY = 0;
    public List<ItemStack> items; 

    // CHANGED: item yang lagi di-KLIK (bukan hover lagi). Detailnya tampil di panel kanan.
    private ItemStack selectedItem = null;

    // panel dikecilin dari 860 -> 730 biar muat di screenWidth (768), sebelumnya
    // kepotong karena panelX jadi negatif
    private int panelW = 730;
    private int panelH = 460;
    
    public ItemStack[] equippedSlots = new ItemStack[5]; 
    private final String[] slotShortcuts = {"[W]", "[H]", "[A]", "[L]", "[B]"}; 
    private final String[] slotLabels = {"WEAPON", "HELM", "ARMOR", "LEGS", "BOOTS"};

    private int gridBoxW = 328;
    private int gapBetween = 12;

    private Color colorBgOuter  = new Color(45, 30, 20);   
    private Color colorBgInner  = new Color(85, 65, 50);   
    private Color colorSubPanel = new Color(35, 25, 20);   
    private Color colorSlot     = new Color(60, 45, 35);   
    private Color colorSlotDark = new Color(25, 20, 15);   
    private Color colorBorder   = new Color(220, 190, 150); 
    private Color colorAccent   = new Color(250, 180, 40);  

    // ── State & hitbox delete (sekarang letaknya di panel kanan, mode detail item) ──
    private Rectangle deleteBtnRect = null;
    private Rectangle minusBtnRect = null;
    private Rectangle plusBtnRect = null;
    private Rectangle confirmBtnRect = null;
    private Rectangle cancelBtnRect = null;
    private Rectangle closeDetailBtnRect = null; // NEW: tombol silang di kanan atas detail item
    private boolean showDeleteConfirm = false;
    private int deleteAmount = 1;

    // ── State panel MENU (kanan) & Settings ──
    private boolean settingsOpen = false; // false = tampilin list menu, true = tampilin isi Settings
    private int musicVolumePercent = 75;  // default match MusicManager.play() (-10dB)
    private Rectangle settingsBtnRect = null;
    private Rectangle settingsBackBtnRect = null;
    private Rectangle volMinusBtnRect = null;
    private Rectangle volPlusBtnRect = null;

    public BackpackUI(List<ItemStack> items) {
        this.items = items;
    }

    /**
     * Hitung geometry panel grid (kotak isi 5xN slot inventory).
     */
    private Rectangle getGridBoxRect(int panelX, int panelY) {
        int contentX = panelX + 230;
        int contentY = panelY + 75;
        int contentH = panelH - 100;
        return new Rectangle(contentX, contentY, gridBoxW, contentH);
    }

    /**
     * Hitung geometry panel kanan. Isinya ganti-ganti:
     * - default            -> list menu (Settings)
     * - ada item di-klik   -> detail item (deskripsi, stat, delete)
     */
    private Rectangle getMenuBoxRect(int panelX, int panelY) {
        int contentX = panelX + 230;
        int contentY = panelY + 75;
        int contentW = panelW - 246;
        int contentH = panelH - 100;
        int menuX = contentX + gridBoxW + gapBetween;
        int menuW = contentW - gridBoxW - gapBetween;
        return new Rectangle(menuX, contentY, menuW, contentH);
    }

    /**
     * Panel kanan. Kalau selectedItem != null -> gambar detail item.
     * Kalau null -> gambar menu biasa (Settings). Struktur menu di bawah ini
     * gampang ditambahin entry lain (Diary, Map, dll):
     * tinggal copy blok tombol "Settings", geser btnY += (btnH + 8), kasih nama beda.
     */
    private void drawMenuPanel(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(colorSubPanel);
        g2.fillRect(x, y, w, h);
        g2.setColor(colorBgOuter);
        g2.drawRect(x, y, w, h);

        // ── Mode DETAIL ITEM ──
        if (selectedItem != null && selectedItem.item != null) {
            drawItemDetails(g2, x, y, w, h);
            return;
        }

        // ── Mode MENU / SETTINGS ──
        // reset hitbox mode detail biar klik gak nyangkut ke tombol yang gak keliatan
        deleteBtnRect = null;
        minusBtnRect = null;
        plusBtnRect = null;
        confirmBtnRect = null;
        cancelBtnRect = null;
        closeDetailBtnRect = null;

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(colorBorder);
        g2.drawString("- MENU -", x + 12, y + 22);

        if (!settingsOpen) {
            settingsBackBtnRect = null;
            volMinusBtnRect = null;
            volPlusBtnRect = null;

            int btnY = y + 40;
            int btnH = 28;
            int btnW = w - 24;
            int btnX = x + 12;

            settingsBtnRect = new Rectangle(btnX, btnY, btnW, btnH);

            g2.setColor(colorSlot);
            g2.fillRect(btnX, btnY, btnW, btnH);
            g2.setColor(colorBorder);
            g2.drawRect(btnX, btnY, btnW, btnH);
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            g2.drawString("Settings", btnX + 10, btnY + 18);

            // NOTE buat nanti: nambah menu baru tinggal duplikasi 6 baris draw
            // di atas dengan btnY += (btnH + 8), contoh:
            // int btnY2 = btnY + btnH + 8;
            // Rectangle diaryBtnRect = new Rectangle(btnX, btnY2, btnW, btnH);
            // ... gambar tombol "Diary" ...
        } else {
            settingsBtnRect = null;

            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(colorAccent);
            g2.drawString("Music Volume", x + 12, y + 48);

            int rowY = y + 60;
            int minusX = x + 12;
            volMinusBtnRect = new Rectangle(minusX, rowY, 22, 22);
            g2.setColor(colorSlot);
            g2.fillRect(minusX, rowY, 22, 22);
            g2.setColor(colorBorder);
            g2.drawRect(minusX, rowY, 22, 22);
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("-", minusX + 8, rowY + 16);

            String volText = musicVolumePercent + "%";
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(colorAccent);
            g2.drawString(volText, minusX + 30, rowY + 16);

            int plusX = minusX + 72;
            volPlusBtnRect = new Rectangle(plusX, rowY, 22, 22);
            g2.setColor(colorSlot);
            g2.fillRect(plusX, rowY, 22, 22);
            g2.setColor(colorBorder);
            g2.drawRect(plusX, rowY, 22, 22);
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("+", plusX + 7, rowY + 16);

            // bar indikator volume
            int barY = rowY + 34;
            int barW = w - 24;
            int barH = 8;
            g2.setColor(colorSlotDark);
            g2.fillRect(x + 12, barY, barW, barH);
            int fillW = (int) (barW * (musicVolumePercent / 100f));
            g2.setColor(colorAccent);
            g2.fillRect(x + 12, barY, fillW, barH);
            g2.setColor(colorBorder);
            g2.drawRect(x + 12, barY, barW, barH);

            int backY = y + h - 32;
            int backW = w - 24;
            settingsBackBtnRect = new Rectangle(x + 12, backY, backW, 22);
            g2.setColor(colorSlot);
            g2.fillRect(x + 12, backY, backW, 22);
            g2.setColor(colorBorder);
            g2.drawRect(x + 12, backY, backW, 22);
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString("< Back", x + 22, backY + 16);
        }
    }

    /**
     * NEW: Detail item di panel kanan (gantiin tooltip melayang).
     * Isinya: header (ikon + qty + nama), deskripsi, stat, tombol delete.
     * Tombol X di kanan atas buat balik ke tampilan menu/settings.
     * Panel kanan sempit (~144px), jadi layout dibikin vertikal.
     */
    private void drawItemDetails(Graphics2D g2, int x, int y, int w, int h) {
        // reset hitbox mode menu/settings
        settingsBtnRect = null;
        settingsBackBtnRect = null;
        volMinusBtnRect = null;
        volPlusBtnRect = null;

        int pad = 12;

        // Judul + tombol silang
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(colorBorder);
        g2.drawString("- ITEM -", x + pad, y + 22);

        closeDetailBtnRect = new Rectangle(x + w - 28, y + 8, 18, 18);
        drawButton(g2, closeDetailBtnRect, "X", colorSlot);

        // Header: ikon, qty, nama
        int iconSize = 36;
        int iconX = x + pad;
        int iconY = y + 36;

        g2.drawImage(selectedItem.item.icon, iconX, iconY, iconSize, iconSize, null);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Qty: " + selectedItem.amount, iconX + iconSize + 8, iconY + 22);

        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(colorAccent);
        g2.drawString(truncateText(selectedItem.item.name, 18), x + pad, y + 90);

        // Divider
        g2.setColor(new Color(120, 100, 80));
        g2.drawLine(x + pad, y + 100, x + w - pad, y + 100);

        // Deskripsi
        g2.setColor(colorAccent);
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.drawString("DESCRIPTION", x + pad, y + 117);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        int nextY = drawWrappedString(
                g2,
                getItemDescriptionText(selectedItem.item.name),
                x + pad, y + 131, w - pad * 2, 12);

        // Stat
        ItemStats stats = getItemStats(selectedItem.item.name);
        int statsX = x + pad;
        int statsW = w - pad * 2;
        int statsY = nextY + 2;

        if (stats != null) {
            if (stats.damage != null) {
                statsY = drawStatRow(g2, "DAMAGE", String.valueOf(stats.damage), statsX, statsY, statsW);
            }
            if (stats.defense != null) {
                statsY = drawStatRow(g2, "DEFENSE", String.valueOf(stats.defense), statsX, statsY, statsW);
            }
            if (stats.critChance != null) {
                statsY = drawStatRow(g2, "CRIT CHANCE", stats.critChance + "%", statsX, statsY, statsW);
            }
            if (stats.critDamage != null) {
                statsY = drawStatRow(g2, "CRIT DAMAGE", stats.critDamage + "%", statsX, statsY, statsW);
            }
            if (stats.damageReduction != null) {
                statsY = drawStatRow(g2, "DMG REDUCTION", stats.damageReduction + "%", statsX, statsY, statsW);
            }
        } else {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g2.setColor(new Color(170, 170, 170));
            g2.drawString("No equipment stats.", statsX, statsY + 10);
        }

        // Tombol delete (nempel di bawah panel)
        int bx = x + pad;
        int bw = w - pad * 2;

        if (!showDeleteConfirm) {
            deleteBtnRect = new Rectangle(bx, y + h - 32, bw, 20);
            minusBtnRect = null;
            plusBtnRect = null;
            confirmBtnRect = null;
            cancelBtnRect = null;

            drawButton(g2, deleteBtnRect, "[ Delete ]", new Color(120, 30, 25));
        } else {
            deleteBtnRect = null;

            int row1Y = y + h - 58;
            int row2Y = y + h - 32;

            // baris 1: [-]  qty  [+]
            minusBtnRect = new Rectangle(bx, row1Y, 20, 20);
            plusBtnRect = new Rectangle(bx + bw - 20, row1Y, 20, 20);
            drawButton(g2, minusBtnRect, "-", colorSlot);
            drawButton(g2, plusBtnRect, "+", colorSlot);

            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(colorAccent);
            drawCenteredText(g2, deleteAmount + "/" + selectedItem.amount,
                    new Rectangle(bx + 20, row1Y, bw - 40, 20));

            // baris 2: OK  Cancel
            int halfW = (bw - 4) / 2;
            confirmBtnRect = new Rectangle(bx, row2Y, halfW, 20);
            cancelBtnRect = new Rectangle(bx + halfW + 4, row2Y, halfW, 20);
            drawButton(g2, confirmBtnRect, "OK", new Color(120, 30, 25));
            drawButton(g2, cancelBtnRect, "Cancel", colorSlot);
        }
    }

    // NEW: helper gambar tombol kotak + teks di tengah
    private void drawButton(Graphics2D g2, Rectangle r, String label, Color fill) {
        g2.setColor(fill);
        g2.fillRect(r.x, r.y, r.width, r.height);

        g2.setColor(colorBorder);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(r.x, r.y, r.width, r.height);

        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.setColor(Color.WHITE);
        drawCenteredText(g2, label, r);
    }

    // NEW: helper teks di tengah rectangle (pakai font & warna g2 yang lagi aktif)
    private void drawCenteredText(Graphics2D g2, String text, Rectangle r) {
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(text)) / 2;
        int ty = r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    /**
     * CHANGED: hover tooltip dihapus (detail item sekarang muncul pas klik,
     * di panel kanan). Method ini sengaja dikosongin tapi dipertahankan biar
     * pemanggilnya (GamePanel / MouseHandler) gak error.
     */
    public void handleMouseMove(MouseEvent e, GamePanel gp) {
        // kosong
    }

    private String getItemDescriptionText(String itemName) {

        switch (itemName.toLowerCase()) {

            case "log":
                return "A solid chunk of raw wood. Useful for crafting basic "
                     + "survival items or burning as campfire fuel. "
                     + "Can be crafted.";

            case "ore":
                return "Unrefined iron ore rich in metal. Requires smelting "
                     + "in a hot furnace. Can be crafted.";

            case "slime gel":
                return "A sticky, glowing gelatinous glob dropped by slimes. "
                     + "Gross, but chemically reactive.";

            case "sword":
                return "A standard iron sword forged by blacksmiths. "
                     + "Reliable weapon for combat. ";

            case "axe":
                return "A sharp iron tool engineered for felling thick trees. "
                     + "Can also be used as a weapon. ";
            case "bone":
                return "A sturdy skeletal bone covered in crypt dust. "
                     + "Seems to hum with weak, dark residual magic. "
                     + "Can be crafted.";

            case "skull":
                return "A hollow skull of a long-dead wanderer. "
                     + "Radiates an unsettling chill to whoever holds it. "
                     + "Can be crafted.";

            case "death totem":
                return "An ancient, forbidden relic that traps the souls "
                     + "of slain foes. Press T to spawn them as minions.";

            case "skeleton key":
                return "An old bony key carved from calcified remains. "
                     + "Looks like it could awaken something terrifying.";

            case "bone sword":
                return "A lethal, dark-attributed sword crafted entirely "
                     + "from hardened bones. ";

            case "helm":
                return "A protective iron helmet that shields the wearer's "
                     + "head from blunt strikes and falling debris. ";
                
            case "soul of skeleton":
                return "A faintly glowing soul torn from a fallen Skeleton King. "
                     + "Contains traces of dark energy. Can be crafted.";

            case "coin":
                return "A shiny coin dropped by every monster you defeat. "
                     + "Used as currency.";

            default:
                return "A mysterious artifact discovered on your journey. "
                     + "Its exact properties and uses are unknown.";

        }

    }

    // CHANGED: sekarang return y baris berikutnya (setelah baris terakhir),
    // biar stat bisa nyambung di bawah deskripsi berapa pun jumlah barisnya.
    private int drawWrappedString(Graphics2D g2, String text, int x, int y, int wrapWidth, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (fm.stringWidth(line + word) < wrapWidth) {
                line.append(word).append(" ");
            } else {
                g2.drawString(line.toString(), x, y);
                line = new StringBuilder(word).append(" ");
                y += lineHeight;
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, y);
            y += lineHeight;
        }
        return y;
    }

    public void draw(Graphics2D g2, int screenW, int screenH, GamePanel gp) {
        if (!visible) return;

        // kalau item yang lagi dipilih udah gak ada di inventory (kehapus/kepake), lepas pilihannya
        if (selectedItem != null && !items.contains(selectedItem)) {
            selectedItem = null;
            showDeleteConfirm = false;
            deleteAmount = 1;
        }

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int panelX = screenW / 2 - panelW / 2;
        int panelY = 60;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(panelX + 8, panelY + 8, panelW, panelH);

        g2.setColor(colorBgOuter);
        g2.fillRect(panelX, panelY, panelW, panelH);
        
        g2.setColor(colorBgInner);
        g2.fillRect(panelX + 6, panelY + 6, panelW - 12, panelH - 12);
        
        g2.setColor(colorBorder);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(panelX + 8, panelY + 8, panelW - 16, panelH - 16);

        String title = "INVENTORY & GEARS";
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;
        
        g2.setColor(colorBgOuter);
        g2.fillRect(titleX - 20, panelY + 18, titleW + 40, 30);
        g2.setColor(colorBorder);
        g2.drawRect(titleX - 20, panelY + 18, titleW + 40, 30);
        
        g2.setColor(Color.BLACK);
        g2.drawString(title, titleX + 2, panelY + 40); 
        g2.setColor(colorAccent);
        g2.drawString(title, titleX, panelY + 38);   

        // ── Panel EQUIPPED (kiri) ──
        int eqX = panelX + 24;
        int eqY = panelY + 75;
        int eqW = 190;
        int eqH = panelH - 100;
        
        g2.setColor(colorSubPanel);
        g2.fillRect(eqX, eqY, eqW, eqH);
        g2.setColor(colorBgOuter);
        g2.drawRect(eqX, eqY, eqW, eqH);
        
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(colorBorder);
        g2.drawString("- EQUIPPED -", eqX + 15, eqY + 22);

        int startSlotY = eqY + 35;
        for (int i = 0; i < equippedSlots.length; i++) {
            int slotYPosition = startSlotY + i * (slotSize + 11);
            int slotXPosition = eqX + 15;

            drawPixelSlot(g2, slotXPosition, slotYPosition, false);

            if (equippedSlots[i] != null && equippedSlots[i].item != null) {
                int baseIconSize = slotSize - 12;
                int iconSize = getIconDrawSize(equippedSlots[i].item, baseIconSize);
                int iconX = slotXPosition + (slotSize - iconSize) / 2;
                int iconY = slotYPosition + (slotSize - iconSize) / 2;
                g2.drawImage(equippedSlots[i].item.icon, iconX, iconY, iconSize, iconSize, null);
                
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                g2.setColor(Color.BLACK);
                g2.drawString("E", slotXPosition + 6, slotYPosition + 16);
                g2.setColor(colorAccent);
                g2.drawString("E", slotXPosition + 5, slotYPosition + 15);
            } else {
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawString(slotShortcuts[i], slotXPosition + 14, slotYPosition + 32);
            }

            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            if (equippedSlots[i] != null) {
                g2.setColor(Color.WHITE);
                g2.drawString(truncateText(equippedSlots[i].item.name, 12), slotXPosition + slotSize + 8, slotYPosition + 30);
            } else {
                g2.setColor(new Color(130, 110, 95));
                g2.drawString(slotLabels[i], slotXPosition + slotSize + 8, slotYPosition + 30);
            }
        }

        // ── Panel GRID INVENTORY (tengah) & Panel kanan (menu / detail item) ──
        Rectangle gridBox = getGridBoxRect(panelX, panelY);
        Rectangle menuBox = getMenuBoxRect(panelX, panelY);

        g2.setColor(colorSubPanel);
        g2.fillRect(gridBox.x, gridBox.y, gridBox.width, gridBox.height);
        g2.setColor(colorBgOuter);
        g2.drawRect(gridBox.x, gridBox.y, gridBox.width, gridBox.height);

        Shape oldClip = g2.getClip();
        g2.setClip(gridBox.x + 6, gridBox.y + 6, gridBox.width - 20, gridBox.height - 12);

        int gridStartX = gridBox.x + 12;
        int gridStartY = gridBox.y + 12 - scrollY;
        int totalSlots = cols * gridRows;

        for (int i = 0; i < totalSlots; i++) {
            int col = i % cols;
            int row = i / cols;

            int sx = gridStartX + col * (slotSize + slotPadding);
            int sy = gridStartY + row * (slotSize + slotPadding);

            if (sy + slotSize < gridBox.y || sy > gridBox.y + gridBox.height) continue;

            boolean isEquipped = false;
            if (i < items.size() && items.get(i) != null) {
                isEquipped = isCurrentlyEquipped(items.get(i), gp);
            }

            drawPixelSlot(g2, sx, sy, isEquipped);

            if (i < items.size()) {
                ItemStack stack = items.get(i);
                if (stack != null && stack.item != null) {
                    int baseIconSize = slotSize - 12;
                    int iconSize = getIconDrawSize(stack.item, baseIconSize);
                    int iconX = sx + (slotSize - iconSize) / 2;
                    int iconY = sy + (slotSize - iconSize) / 2;
                    g2.drawImage(stack.item.icon, iconX, iconY, iconSize, iconSize, null);

                    if (stack.amount > 1) {
                        String amt = String.valueOf(stack.amount);
                        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
                        g2.setColor(Color.BLACK);
                        g2.drawString(amt, sx + slotSize - 12, sy + slotSize - 4);
                        g2.setColor(Color.WHITE);
                        g2.drawString(amt, sx + slotSize - 13, sy + slotSize - 5);
                    }
                }
            }
        }
        g2.setClip(oldClip);

        drawScrollbar(g2, gridBox.x, gridBox.y, gridBox.width, gridBox.height);

        // Panel kanan = detail item (kalau ada yang diklik) / menu Settings (default)
        drawMenuPanel(g2, menuBox.x, menuBox.y, menuBox.width, menuBox.height);
    }

    private int getIconDrawSize(Item.Item item, int baseSize) {
        if (item != null && item.name != null && item.name.equalsIgnoreCase("Soul of Skeleton")) {
            return (int) (baseSize * 1.4);
        }
        return baseSize;
    }

    private void drawPixelSlot(Graphics2D g2, int x, int y, boolean highlight) {
        g2.setColor(colorSlot);
        g2.fillRect(x, y, slotSize, slotSize);

        g2.setColor(colorSlotDark);
        g2.fillRect(x, y + slotSize - 3, slotSize, 3);
        g2.fillRect(x + slotSize - 3, y, 3, slotSize);

        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRect(x, y, slotSize, 3);
        g2.fillRect(x, y, 3, slotSize);

        if (highlight) {
            g2.setColor(colorAccent); 
            g2.setStroke(new BasicStroke(2));
        } else {
            g2.setColor(new Color(20, 15, 10));
            g2.setStroke(new BasicStroke(1));
        }
        g2.drawRect(x, y, slotSize, slotSize);
    }

    
    private static class ItemStats {
        String equipType;
        Integer damage;
        Integer defense;
        Integer critChance;
        Integer critDamage;
        Integer damageReduction;

        ItemStats(String equipType) {
            this.equipType = equipType;
        }
    }
    
    // CHANGED: ikon [+] dibuang & value rata kanan, soalnya panel kanan cuma ~144px
    private int drawStatRow(
            Graphics2D g2,
            String label,
            String value,
            int x,
            int y,
            int width) {

        int rowH = 22;

        // Background baris stat
        g2.setColor(new Color(45, 35, 28));
        g2.fillRect(x, y, width, rowH);

        g2.setColor(new Color(100, 80, 60));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(x, y, width, rowH);

        // Nama stat
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.drawString(label, x + 6, y + 15);

        // Nilai stat (rata kanan)
        g2.setColor(colorAccent);
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        int valueW = g2.getFontMetrics().stringWidth(value);
        g2.drawString(value, x + width - 6 - valueW, y + 15);

        return y + rowH + 4;
    }
    
    private ItemStats getItemStats(String itemName) {

        String name = itemName.toLowerCase();
        ItemStats stats = null;

        switch (name) {

            case "sword":
                stats = new ItemStats("WEAPON");
                stats.damage = 20;       // TODO: Sesuaikan damage asli
                stats.critChance = 5;    // TODO: Sesuaikan crit asli
                stats.critDamage = 150;  // TODO: Sesuaikan crit damage asli
                break;

            case "bone sword":
                stats = new ItemStats("WEAPON");
                stats.damage = 35;       // TODO: Sesuaikan damage asli
                stats.critChance = 8;
                stats.critDamage = 175;
                break;

            case "axe":
                stats = new ItemStats("WEAPON");
                stats.damage = 15;       // TODO: Sesuaikan damage asli
                stats.critChance = 3;
                stats.critDamage = 150;
                break;

            case "helm":
                stats = new ItemStats("HELMET");
                stats.defense = 10;     // TODO: Sesuaikan defense asli
                stats.damageReduction = 5;
                break;

            default:
                break;
        }

        return stats;
    }
    
    private void drawScrollbar(Graphics2D g2, int contentX, int contentY, int contentW, int contentH) {
        int barW = 6;
        int barX = contentX + contentW - 12;
        int totalContentH = gridRows * (slotSize + slotPadding) + 16;
        
        if (totalContentH <= contentH) return;

        g2.setColor(colorSlotDark);
        g2.fillRect(barX, contentY + 6, barW, contentH - 12);

        float ratio = (float) contentH / totalContentH;
        int thumbH = (int)((contentH - 12) * ratio);
        if(thumbH < 15) thumbH = 15; 
        
        int maxScroll = totalContentH - contentH;
        int thumbY = contentY + 6 + (int)((scrollY / (float) maxScroll) * ((contentH - 12) - thumbH));

        g2.setColor(colorBorder);
        g2.fillRect(barX, thumbY, barW, thumbH);
        g2.setColor(colorBgOuter);
        g2.drawRect(barX, thumbY, barW, thumbH);
    }
    
    private boolean isCurrentlyEquipped(ItemStack stack, GamePanel gp) {
        for (ItemStack eq : equippedSlots) {
            if (eq != null && eq.item == stack.item) {
                return true;
            }
        }
        return false;
    }

    private String truncateText(String text, int maxChars) {
        if (text.length() > maxChars) {
            return text.substring(0, maxChars - 2) + "..";
        }
        return text;
    }

    private void performDelete(GamePanel gp, int amount) {
        if (selectedItem == null) return;

        selectedItem.amount -= amount;
        if (selectedItem.amount <= 0) {
            for (int i = 0; i < equippedSlots.length; i++) {
                if (equippedSlots[i] == selectedItem) {
                    if (i == 0) {
                        gp.player.isWeaponEquipped = false;
                        gp.player.currentWeapon = null;
                    } else if (i == 1) {
                        gp.player.currentHelm = null;
                    }
                    equippedSlots[i] = null;
                }
            }
            items.remove(selectedItem);
            selectedItem = null; // item abis -> panel kanan balik ke menu
        }
        showDeleteConfirm = false;
        deleteAmount = 1;
    }

    // terapkan volume musik ke GamePanel.music (SFX.MusicManager)
    private void applyMusicVolume(GamePanel gp) {
        if (gp != null && gp.music != null) {
            gp.music.setVolume(percentToDb(musicVolumePercent));
        }
    }

    // 0% = -40dB (nyaris hening), 100% = 0dB (paling keras)
    private float percentToDb(int percent) {
        return (percent / 100f) * 40f - 40f;
    }

    public void handleKeyPress(int keyCode) {
        if (keyCode != KeyEvent.VK_ESCAPE) return;

        if (showDeleteConfirm) {
            showDeleteConfirm = false;
            deleteAmount = 1;
        } else if (selectedItem != null) {
            selectedItem = null; // ESC tutup detail item dulu
        } else if (settingsOpen) {
            settingsOpen = false; // ESC balik ke list menu dulu
        } else {
            visible = false;
        }
    }

    public void handleMouseClick(MouseEvent e, GamePanel gp) {
        if (!visible) return;

        int mx = e.getPoint().x;
        int my = e.getPoint().y;

        int screenW = gp.screenWidth;
        int panelX = screenW / 2 - panelW / 2;
        int panelY = 60;

        // ── 0. KLIK DI PANEL KANAN — detail item (X / Delete) atau menu (Settings / volume) ──
        Rectangle menuBox = getMenuBoxRect(panelX, panelY);
        if (menuBox.contains(mx, my)) {
            if (selectedItem != null) {
                // mode DETAIL ITEM
                if (closeDetailBtnRect != null && closeDetailBtnRect.contains(mx, my)) {
                    selectedItem = null; // balik ke tampilan menu/settings
                    showDeleteConfirm = false;
                    deleteAmount = 1;
                } else if (showDeleteConfirm) {
                    if (minusBtnRect != null && minusBtnRect.contains(mx, my)) {
                        deleteAmount = Math.max(1, deleteAmount - 1);
                    } else if (plusBtnRect != null && plusBtnRect.contains(mx, my)) {
                        deleteAmount = Math.min(selectedItem.amount, deleteAmount + 1);
                    } else if (confirmBtnRect != null && confirmBtnRect.contains(mx, my)) {
                        performDelete(gp, deleteAmount);
                    } else if (cancelBtnRect != null && cancelBtnRect.contains(mx, my)) {
                        showDeleteConfirm = false;
                        deleteAmount = 1;
                    }
                } else {
                    if (deleteBtnRect != null && deleteBtnRect.contains(mx, my)) {
                        if (selectedItem.amount <= 1) {
                            performDelete(gp, selectedItem.amount);
                        } else {
                            showDeleteConfirm = true;
                            deleteAmount = 1;
                        }
                    }
                }
            } else if (!settingsOpen) {
                // mode MENU
                if (settingsBtnRect != null && settingsBtnRect.contains(mx, my)) {
                    settingsOpen = true;
                }
            } else {
                // mode SETTINGS
                if (volMinusBtnRect != null && volMinusBtnRect.contains(mx, my)) {
                    musicVolumePercent = Math.max(0, musicVolumePercent - 10);
                    applyMusicVolume(gp);
                } else if (volPlusBtnRect != null && volPlusBtnRect.contains(mx, my)) {
                    musicVolumePercent = Math.min(100, musicVolumePercent + 10);
                    applyMusicVolume(gp);
                } else if (settingsBackBtnRect != null && settingsBackBtnRect.contains(mx, my)) {
                    settingsOpen = false;
                }
            }
            return;
        }

        // ── 1. KLIK PANEL EQUIPPED (KIRI) ──
        int eqX = panelX + 24;
        int eqY = panelY + 75;
        int startSlotY = eqY + 35;
        int slotXPosition = eqX + 15;

        if (mx >= slotXPosition && mx <= slotXPosition + slotSize) {
            for (int i = 0; i < equippedSlots.length; i++) {
                int slotYPosition = startSlotY + i * (slotSize + 11);
                if (my >= slotYPosition && my <= slotYPosition + slotSize) {
                    
                    if (e.getButton() == MouseEvent.BUTTON3) { // KLIK KANAN: Lepas Equip
                        if (equippedSlots[i] != null) {
                            if (i == 0) {
                                gp.player.isWeaponEquipped = false;
                                gp.player.currentWeapon = null; 
                            } else if (i == 1) {
                                gp.player.currentHelm = null;
                            }
                            equippedSlots[i] = null;
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON1) { // KLIK KIRI: lihat detail
                        if (equippedSlots[i] != null && equippedSlots[i].item != null
                                && equippedSlots[i] != selectedItem) {
                            selectedItem = equippedSlots[i];
                            showDeleteConfirm = false;
                            deleteAmount = 1;
                        }
                    }
                    return;
                }
            }
        }

        // ── 2. KLIK GRID INVENTORY (TENGAH) ──
        Rectangle gridBox = getGridBoxRect(panelX, panelY);
        int gridStartX = gridBox.x + 12;
        int gridStartY = gridBox.y + 12 - scrollY;

        if (gridBox.contains(mx, my)) {
            int totalSlots = cols * gridRows;

            for (int i = 0; i < totalSlots; i++) {
                int col = i % cols;
                int row = i / cols;
                int sx = gridStartX + col * (slotSize + slotPadding);
                int sy = gridStartY + row * (slotSize + slotPadding);

                if (mx >= sx && mx <= sx + slotSize && my >= sy && my <= sy + slotSize) {
                    if (i < items.size()) {
                        ItemStack stack = items.get(i);
                        if (stack != null && stack.item != null) {
                            
                            if (e.getButton() == MouseEvent.BUTTON3) { // KLIK KANAN: Pasang/Lepas Equip
                                String itemName = stack.item.name.toLowerCase();
                                int targetSlot = -1;

                                if (itemName.contains("sword") || itemName.contains("weapon") || itemName.contains("axe") || itemName.contains("kapak") || itemName.contains("bone sword")) targetSlot = 0;
                                else if (itemName.contains("helm") || itemName.contains("hat")) targetSlot = 1;
                                else if (itemName.contains("baju") || itemName.contains("armor") || itemName.contains("cloth")) targetSlot = 2;
                                else if (itemName.contains("legging") || itemName.contains("pants") || itemName.contains("celana")) targetSlot = 3;
                                else if (itemName.contains("sepatu") || itemName.contains("boots")) targetSlot = 4;

                                if (targetSlot != -1) {
                                    if (equippedSlots[targetSlot] != null && equippedSlots[targetSlot].item == stack.item) {
                                        if (targetSlot == 0) {
                                            gp.player.isWeaponEquipped = false;
                                            gp.player.currentWeapon = null;
                                        } else if (targetSlot == 1) {
                                            gp.player.currentHelm = null;
                                        }
                                        equippedSlots[targetSlot] = null;
                                    } else {
                                        equippedSlots[targetSlot] = stack;
                                        if (targetSlot == 0) {
                                            gp.player.isWeaponEquipped = true;
                                            if (itemName.contains("axe") || itemName.contains("kapak")) {
                                                gp.player.currentWeapon = ItemManager.get().weaponAxeInstance;
                                            } else if (itemName.contains("bone sword")) {
                                                gp.player.currentWeapon = ItemManager.get().weaponBoneSwordInstance;
                                            } else {
                                                gp.player.currentWeapon = ItemManager.get().weaponSwordInstance;
                                            }
                                        } else if (targetSlot == 1) {
                                            gp.player.currentHelm = new PlayerArmor("IronHelm");
                                        }
                                    }
                                }
                            } else if (e.getButton() == MouseEvent.BUTTON1) {
                                // KLIK KIRI: pilih item -> detailnya tampil di panel kanan
                                if (stack != selectedItem) {
                                    selectedItem = stack;
                                    showDeleteConfirm = false;
                                    deleteAmount = 1;
                                }

                                // DOUBLE-KLIK Death Totem: buka Totem UI (dulu cukup 1x klik)
                                if (e.getClickCount() >= 2
                                        && stack.item == ItemManager.get().DEATH_TOTEM
                                        && gp.deathTotemManager != null
                                        && gp.totemUI != null) {
                                    selectedItem = null;
                                    visible = false;
                                    gp.totemUI.show();
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!visible) return;

        // Kalau lagi buka konfirmasi delete, scroll dipakai buat atur quantity
        if (showDeleteConfirm && selectedItem != null) {
            deleteAmount -= e.getWheelRotation();
            deleteAmount = Math.max(1, Math.min(selectedItem.amount, deleteAmount));
            return;
        }

        int contentH = panelH - 100;
        int totalContentH = gridRows * (slotSize + slotPadding) + 16;
        int maxScroll = Math.max(0, totalContentH - contentH);

        scrollY += e.getWheelRotation() * (slotSize + slotPadding);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
    }
}