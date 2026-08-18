package UI;

import java.awt.*;
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

    // NEW: item yang lagi di-hover mouse (buat tooltip info), bukan lagi "selected"
    private ItemStack hoveredItem = null;
    // NEW: posisi slot yang lagi di-hover, dipake buat naro tooltip di sebelahnya
    private Rectangle hoveredSlotRect = null;
    // NEW: bounding box tooltip yang lagi digambar, dipake buat cek klik tombol di dalamnya
    private Rectangle tooltipRect = null;

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

    // ── State & hitbox delete (sekarang letaknya di tooltip, bukan panel kanan) ──
    private Rectangle deleteBtnRect = null;
    private Rectangle minusBtnRect = null;
    private Rectangle plusBtnRect = null;
    private Rectangle confirmBtnRect = null;
    private Rectangle cancelBtnRect = null;
    private boolean showDeleteConfirm = false;
    private int deleteAmount = 1;

    // ── NEW: State panel MENU (kanan) & Settings ──
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
     * Hitung geometry panel MENU (kanan) — dulu ini "detail item", sekarang
     * jadi list menu (Settings, dan nanti bisa ditambah Diary/Map/dll).
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
     * Panel MENU di sebelah kanan grid inventory. Sekarang cuma ada "Settings",
     * tapi struktur di bawah ini gampang ditambahin entry lain (Diary, Map, dll):
     * tinggal copy blok tombol "Settings", geser btnY += (btnH + 8), kasih nama beda.
     */
    private void drawMenuPanel(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(colorSubPanel);
        g2.fillRect(x, y, w, h);
        g2.setColor(colorBgOuter);
        g2.drawRect(x, y, w, h);

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
     * Hitung posisi & ukuran tooltip berdasarkan slot yang di-hover.
     * DIPAKAI BARENG oleh draw() dan handleMouseMove(), biar hasilnya SELALU
     * sinkron — gak ada lagi versi "basi" dari frame render sebelumnya yang
     * bikin celah ke-detect sebagai keluar area.
     */
    private Rectangle calcTooltipRect(Rectangle slot, int screenW, int screenH) {
        int tw = 230;
        int th = 180;

        // DEMPET: nempel langsung di tepi slot, malah overlap 2px biar
        // beneran nol celah (gak ada garis batas yang bisa "keloncat").
        int tx = slot.x + slot.width - 2;
        boolean placedLeft = false;
        if (tx + tw > screenW - 5) {
            tx = slot.x - tw + 2; // gak muat di kanan, taro nempel di kiri slot
            placedLeft = true;
        }
        if (tx < 5) tx = 5;

        int ty = slot.y;
        if (ty + th > screenH - 5) {
            ty = screenH - th - 5;
        }
        if (ty < 5) ty = 5;

        return new Rectangle(tx, ty, tw, th);
    }

    /**
     * Tooltip info item — muncul nempel di sebelah slot yang lagi di-hover.
     * Posisi dihitung via calcTooltipRect() biar konsisten sama hit-test di
     * handleMouseMove().
     */
    private void drawItemTooltip(Graphics2D g2, int screenW, int screenH) {
        if (hoveredItem == null || hoveredItem.item == null || hoveredSlotRect == null) {
            tooltipRect = null;
            deleteBtnRect = null;
            minusBtnRect = null;
            plusBtnRect = null;
            confirmBtnRect = null;
            cancelBtnRect = null;
            return;
        }

        tooltipRect = calcTooltipRect(hoveredSlotRect, screenW, screenH);
        int tx = tooltipRect.x;
        int ty = tooltipRect.y;
        int tw = tooltipRect.width;
        int th = tooltipRect.height;

        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(tx + 4, ty + 4, tw, th);
        g2.setColor(colorSubPanel);
        g2.fillRect(tx, ty, tw, th);
        g2.setColor(colorBorder);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(tx, ty, tw, th);

        int iconSize = 36;
        int iconX = tx + 12;
        int iconY = ty + 12;
        g2.drawImage(hoveredItem.item.icon, iconX, iconY, iconSize, iconSize, null);

        int textX = iconX + iconSize + 10;
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(colorAccent);
        g2.drawString(truncateText(hoveredItem.item.name, 15), textX, iconY + 14);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.setColor(new Color(170, 170, 170));
        g2.drawString("Quantity: " + hoveredItem.amount, textX, iconY + 30);

        int divY = iconY + iconSize + 10;
        g2.setColor(new Color(120, 100, 80));
        g2.drawLine(tx + 10, divY, tx + tw - 10, divY);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(Color.WHITE);
        String descText = getItemDescriptionText(hoveredItem.item.name);
        drawWrappedString(g2, descText, tx + 12, divY + 18, tw - 24, 14);

        int btnY = ty + th - 32;
        if (!showDeleteConfirm) {
            int btnW = 84, btnH = 20;
            int btnX = tx + 12;
            deleteBtnRect = new Rectangle(btnX, btnY, btnW, btnH);
            minusBtnRect = null;
            plusBtnRect = null;
            confirmBtnRect = null;
            cancelBtnRect = null;

            g2.setColor(new Color(120, 30, 25));
            g2.fillRect(btnX, btnY, btnW, btnH);
            g2.setColor(colorBorder);
            g2.drawRect(btnX, btnY, btnW, btnH);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("[ Delete ]", btnX + 6, btnY + 14);
        } else {
            deleteBtnRect = null;

            int btnX0 = tx + 12;

            minusBtnRect = new Rectangle(btnX0, btnY, 20, 20);
            g2.setColor(colorSlot);
            g2.fillRect(btnX0, btnY, 20, 20);
            g2.setColor(colorBorder);
            g2.drawRect(btnX0, btnY, 20, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("-", btnX0 + 7, btnY + 15);

            String qtyText = deleteAmount + "/" + hoveredItem.amount;
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(colorAccent);
            g2.drawString(qtyText, btnX0 + 26, btnY + 14);

            int plusX = btnX0 + 58;
            plusBtnRect = new Rectangle(plusX, btnY, 20, 20);
            g2.setColor(colorSlot);
            g2.fillRect(plusX, btnY, 20, 20);
            g2.setColor(colorBorder);
            g2.drawRect(plusX, btnY, 20, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("+", plusX + 6, btnY + 15);

            int confirmX = plusX + 26;
            confirmBtnRect = new Rectangle(confirmX, btnY, 50, 20);
            g2.setColor(new Color(120, 30, 25));
            g2.fillRect(confirmX, btnY, 50, 20);
            g2.setColor(colorBorder);
            g2.drawRect(confirmX, btnY, 50, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("OK", confirmX + 17, btnY + 14);

            int cancelX = confirmX + 56;
            cancelBtnRect = new Rectangle(cancelX, btnY, 50, 20);
            g2.setColor(colorSlot);
            g2.fillRect(cancelX, btnY, 50, 20);
            g2.setColor(colorBorder);
            g2.drawRect(cancelX, btnY, 50, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("X", cancelX + 20, btnY + 14);
        }
    }

    /**
     * Dipanggil tiap mouse gerak selagi backpack kebuka. Update hoveredItem
     * sesuai item yang lagi ditunjuk mouse. FIX: safe zone dihitung LANGSUNG
     * di sini pakai calcTooltipRect() (bukan baca field tooltipRect yang
     * di-set dari draw() frame sebelumnya), jadi gak ada lagi celah gara-gara
     * timing render vs event mouse.
     */
    public void handleMouseMove(MouseEvent e, GamePanel gp) {
        if (!visible) return;

        int mx = e.getPoint().x;
        int my = e.getPoint().y;

        int screenW = gp.screenWidth;
        int screenH = gp.screenHeight;
        int panelX = screenW / 2 - panelW / 2;
        int panelY = 60;

        ItemStack found = findItemAtPosition(mx, my, panelX, panelY);

        if (found != null) {
            if (found != hoveredItem) {
                hoveredItem = found;
                showDeleteConfirm = false;
                deleteAmount = 1;
            }
            return;
        }

        // Belum ketemu slot langsung — cek apakah mouse masih di union
        // (slot + tooltip), dihitung fresh saat ini juga, bukan dari cache.
        if (hoveredSlotRect != null) {
            Rectangle liveTooltipRect = calcTooltipRect(hoveredSlotRect, screenW, screenH);
            Rectangle safeZone = hoveredSlotRect.union(liveTooltipRect);
            safeZone.grow(4, 4); // buffer kecil, jaga-jaga jitter mouse

            if (safeZone.contains(mx, my)) {
                return; // masih dianggap "di area hover", tooltip tetap tampil
            }
        }

        // Beneran gak nyentuh item, slot, atau tooltip -> baru ilang
        if (hoveredItem != null) {
            hoveredItem = null;
            hoveredSlotRect = null;
            tooltipRect = null;
            showDeleteConfirm = false;
            deleteAmount = 1;
        }
    }

    private String getItemDescriptionText(String itemName) {
        switch (itemName.toLowerCase()) {
            case "log": return "A solid chunk of raw wood. Useful for crafting basic survival items or burning as campfire fuel.";
            case "ore": return "Unrefined mineral rock rich in metal iron ore. Requires smelting in a hot furnace.";
            case "slime gel": return "A sticky, glowing gelatinous glob dropped by slimes. Gross, but chemically reactive.";
            case "sword": return "A standard iron sword forged by blacksmiths. Reliable blade for fighting off dangerous wildlife.";
            case "axe": return "A sharp iron tool engineered for felling thick trees, though it splits skeleton skulls just fine.";
            case "bone": return "A sturdy skeletal bone covered in crypt dust. Seems to hum with a weak, dark residual magic.";
            case "skull": return "A hollow skull of a long-dead wanderer. Radiates an unsettling chill to whoever holds it.";
            case "death totem": return "An ancient, forbidden relic that traps souls of slain foes. Press T to spawn them as minions.";
            case "skeleton key": return "An old bony key carved from calcified remains. Looks like it unlocks a sealed dungeon gate.";
            case "bone sword": return "A lethal, dark-attributed sword crafted entirely out of hardened bones. High damage output.";
            case "helm": return "A protective iron helmet that shields the wearer's head from blunt strikes and falling debris.";
            case "soul of skeleton": return "A faintly glowing soul torn from a fallen Skeleton King. Whispers of dark power linger within.";
            default: return "A mysterious artifact discovered on your journey. Its exact properties and uses are unknown.";
        }
    }

    private void drawWrappedString(Graphics2D g2, String text, int x, int y, int wrapWidth, int lineHeight) {
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
        }
    }

    public void draw(Graphics2D g2, int screenW, int screenH, GamePanel gp) {
        if (!visible) return;

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

        // ── Panel GRID INVENTORY (tengah) & Panel MENU (kanan) ──
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

        // Panel kanan = MENU (Settings, dst) — bukan detail item lagi
        drawMenuPanel(g2, menuBox.x, menuBox.y, menuBox.width, menuBox.height);

        // Tooltip item — digambar paling akhir biar selalu di atas panel lain
        drawItemTooltip(g2, screenW, screenH);
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
        if (hoveredItem == null) return;

        hoveredItem.amount -= amount;
        if (hoveredItem.amount <= 0) {
            for (int i = 0; i < equippedSlots.length; i++) {
                if (equippedSlots[i] == hoveredItem) {
                    if (i == 0) {
                        gp.player.isWeaponEquipped = false;
                        gp.player.currentWeapon = null;
                    } else if (i == 1) {
                        gp.player.currentHelm = null;
                    }
                    equippedSlots[i] = null;
                }
            }
            items.remove(hoveredItem);
            hoveredItem = null;
            hoveredSlotRect = null;
            tooltipRect = null;
        }
        showDeleteConfirm = false;
        deleteAmount = 1;
    }

    // NEW: terapkan volume musik ke GamePanel.music (SFX.MusicManager)
    private void applyMusicVolume(GamePanel gp) {
        if (gp != null && gp.music != null) {
            gp.music.setVolume(percentToDb(musicVolumePercent));
        }
    }

    // NEW: 0% = -40dB (nyaris hening), 100% = 0dB (paling keras)
    private float percentToDb(int percent) {
        return (percent / 100f) * 40f - 40f;
    }

    public void handleKeyPress(int keyCode) {
        if (keyCode != KeyEvent.VK_ESCAPE) return;

        if (showDeleteConfirm) {
            showDeleteConfirm = false;
            deleteAmount = 1;
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

        // ── 0. KLIK DI TOOLTIP ITEM (kalau lagi kebuka) — Delete & quantity ──
        if (tooltipRect != null && tooltipRect.contains(mx, my)) {
            if (hoveredItem != null) {
                if (showDeleteConfirm) {
                    if (minusBtnRect != null && minusBtnRect.contains(mx, my)) {
                        deleteAmount = Math.max(1, deleteAmount - 1);
                    } else if (plusBtnRect != null && plusBtnRect.contains(mx, my)) {
                        deleteAmount = Math.min(hoveredItem.amount, deleteAmount + 1);
                    } else if (confirmBtnRect != null && confirmBtnRect.contains(mx, my)) {
                        performDelete(gp, deleteAmount);
                    } else if (cancelBtnRect != null && cancelBtnRect.contains(mx, my)) {
                        showDeleteConfirm = false;
                        deleteAmount = 1;
                    }
                } else {
                    if (deleteBtnRect != null && deleteBtnRect.contains(mx, my)) {
                        if (hoveredItem.amount <= 1) {
                            performDelete(gp, hoveredItem.amount);
                        } else {
                            showDeleteConfirm = true;
                            deleteAmount = 1;
                        }
                    }
                }
            }
            return;
        }

        // ── 0b. KLIK DI PANEL MENU (kanan) — Settings / volume ──
        Rectangle menuBox = getMenuBoxRect(panelX, panelY);
        if (menuBox.contains(mx, my)) {
            if (!settingsOpen) {
                if (settingsBtnRect != null && settingsBtnRect.contains(mx, my)) {
                    settingsOpen = true;
                }
            } else {
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
                            if (hoveredItem == equippedSlots[i]) {
                                hoveredItem = null;
                                tooltipRect = null;
                            }
                            equippedSlots[i] = null;
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
                                if (stack.item == ItemManager.get().DEATH_TOTEM
                                        && gp.deathTotemManager != null
                                        && gp.totemUI != null) {
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

    /**
     * Dipanggil tiap mouse gerak selagi backpack kebuka. Update hoveredItem
     * sesuai item yang lagi ditunjuk mouse, biar tooltip otomatis nongol/ganti
     * tanpa perlu diklik, dan otomatis ilang kalau mouse gak nyentuh item
     * ataupun tooltip-nya sendiri.
     */
 

    /**
     * Cari ItemStack di posisi (mx, my) — baik di panel EQUIPPED (kiri)
     * maupun grid INVENTORY (tengah). Set hoveredSlotRect sebagai efek
     * samping tiap ketemu item, biar tooltip tau posisi buat nempel.
     */
    private ItemStack findItemAtPosition(int mx, int my, int panelX, int panelY) {
        int eqX = panelX + 24;
        int eqY = panelY + 75;
        int startSlotY = eqY + 35;
        int slotXPosition = eqX + 15;

        if (mx >= slotXPosition && mx <= slotXPosition + slotSize) {
            for (int i = 0; i < equippedSlots.length; i++) {
                int slotYPosition = startSlotY + i * (slotSize + 11);
                if (my >= slotYPosition && my <= slotYPosition + slotSize) {
                    if (equippedSlots[i] != null) {
                        hoveredSlotRect = new Rectangle(slotXPosition, slotYPosition, slotSize, slotSize);
                        return equippedSlots[i];
                    }
                    return null; // slot kosong
                }
            }
        }

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
                    if (i < items.size() && items.get(i) != null) {
                        hoveredSlotRect = new Rectangle(sx, sy, slotSize, slotSize);
                        return items.get(i);
                    }
                    return null; // slot kosong
                }
            }
        }

        return null;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (!visible) return;

        // Kalau lagi buka konfirmasi delete, scroll dipakai buat atur quantity
        if (showDeleteConfirm && hoveredItem != null) {
            deleteAmount -= e.getWheelRotation();
            deleteAmount = Math.max(1, Math.min(hoveredItem.amount, deleteAmount));
            return;
        }

        int contentH = panelH - 100;
        int totalContentH = gridRows * (slotSize + slotPadding) + 16;
        int maxScroll = Math.max(0, totalContentH - contentH);

        scrollY += e.getWheelRotation() * (slotSize + slotPadding);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));
    }
}