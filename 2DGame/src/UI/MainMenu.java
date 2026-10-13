package UI;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import Main.GamePanel;
import Main.SaveManager;

public class MainMenu {

    private GamePanel gp;

    // ===== STATE (FUNGSI TETAP) =====
    public boolean active = true;
    public enum MenuState { MAIN, SETTINGS, CONFIRM_QUIT, LOAD_GAME }
    public MenuState menuState = MenuState.MAIN;

    private final String[] mainItems = {"New Game", "Load Game", "Settings", "Quit"};
    private int selectedIndex = 0;

    private final String[] settingsItems = {"Music: ON", "SFX: ON", "Back"};
    private int settingsIndex = 0;
    public boolean musicOn = true;
    public boolean sfxOn   = true;
    
    private Main.SaveData[] saveSlots;
    private int loadSelectedIndex = 0;

    // ===== ANIMASI PIXEL =====
    private int selectorTimer = 0;
    private final Random rand = new Random();

    // Partikel Kotak (Pixel)
    private static class Pixel {
        int x, y, size;
        Color color;
        Pixel(int sw, int sh, Random r) {
            x = r.nextInt(sw);
            y = r.nextInt(sh);
            size = 2 + r.nextInt(4);
            color = new Color(255, 255, 255, 40 + r.nextInt(60));
        }
        void update(int sh) {
            y -= 1;
            if (y < 0) y = sh;
        }
    }
    private List<Pixel> pixels = new ArrayList<>();

    public MainMenu(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i < 40; i++) pixels.add(new Pixel(gp.screenWidth, gp.screenHeight, rand));
    }

    public void update() {
        if (!active) return;
        for (Pixel p : pixels) p.update(gp.screenHeight);
        selectorTimer++;
        if (selectorTimer > 30) selectorTimer = 0;
    }
    
    private void drawLoadItems(Graphics2D g2, int x, int y, int w) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("LOAD GAME", getCenterX(g2, "LOAD GAME"), y + 35);

        if (saveSlots == null) saveSlots = SaveManager.getAllSlots();

        for (int i = 0; i < 3; i++) {
            int ty = y + 70 + (i * 55);
            Main.SaveData sd = saveSlots[i];

            if (i == loadSelectedIndex) g2.setColor(Color.YELLOW);
            else g2.setColor(Color.GRAY);

            if (sd != null) {
                g2.drawString("> Slot " + (i+1) + " | Lv." + sd.playerLevel, x + 20, ty);
                g2.setColor(Color.LIGHT_GRAY);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                g2.drawString("  " + SaveManager.formatTime(sd.saveTime), x + 20, ty + 16);
                g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            } else {
                g2.drawString("> Slot " + (i+1) + " | -- KOSONG --", x + 20, ty);
            }
        }

        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString("[ESC] Kembali", x + 20, y + 230);
    }

    // ===== DRAW (Hanya Ubah Visual Menjadi Pixel) =====
    public void draw(Graphics2D g2) {
        if (!active) return;

        // MATIKAN SMOOTHING AGAR PIXEL TAJAM
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // 1. BG SOLID RETRO
        g2.setColor(new Color(15, 15, 25));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // 2. PIXEL PARTICLES
        for (Pixel p : pixels) {
            g2.setColor(p.color);
            g2.fillRect(p.x, p.y, p.size, p.size);
        }

        // 3. TITLE (PIXEL STYLE)
        g2.setFont(new Font("Monospaced", Font.BOLD, 52));
        String title = "LORD K";
        int tx = getCenterX(g2, title);
        
        // Shadow kaku (Pixel offset)
        g2.setColor(new Color(80, 50, 0));
        g2.drawString(title, tx + 4, 94);
        g2.setColor(new Color(255, 200, 0));
        g2.drawString(title, tx, 90);

        // 4. MENU WINDOW
        int winW = 300;
        int winH = 250;
        int winX = gp.screenWidth/2 - winW/2;
        int winY = 150;
        drawPixelWindow(g2, winX, winY, winW, winH);

        // 5. ISI MENU SESUAI STATE
        switch (menuState) {
            case MAIN:         drawMainItems(g2, winX, winY, winW); break;
            case SETTINGS:     drawSettingsItems(g2, winX, winY, winW); break;
            case CONFIRM_QUIT: drawQuitItems(g2, winX, winY, winW); break;
            case LOAD_GAME: drawLoadItems(g2, winX, winY, winW); break;
        }
    }

    private void drawMainItems(Graphics2D g2, int x, int y, int w) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        for (int i = 0; i < mainItems.length; i++) {
            int ty = y + 60 + (i * 45);
            if (i == selectedIndex) {
                g2.setColor(Color.WHITE);
                if (selectorTimer < 15) g2.drawString("> " + mainItems[i] + " <", getCenterX(g2, mainItems[i]) - 15, ty);
                else g2.drawString("  " + mainItems[i] + "  ", getCenterX(g2, mainItems[i]) - 15, ty);
            } else {
                g2.setColor(Color.GRAY);
                g2.drawString(mainItems[i], getCenterX(g2, mainItems[i]), ty);
            }
        }
    }

    private void drawSettingsItems(Graphics2D g2, int x, int y, int w) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        for (int i = 0; i < settingsItems.length; i++) {
            int ty = y + 60 + (i * 45);
            if (i == settingsIndex) {
                g2.setColor(Color.YELLOW);
                g2.drawString("> " + settingsItems[i], x + 40, ty);
            } else {
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("  " + settingsItems[i], x + 40, ty);
            }
        }
    }

    private void drawQuitItems(Graphics2D g2, int x, int y, int w) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.setColor(Color.WHITE);
        String msg = "QUIT GAME?";
        g2.drawString(msg, getCenterX(g2, msg), y + 80);
        
        g2.setColor(Color.RED);
        g2.drawString("[ENTER] YES", x + 40, y + 150);
        g2.setColor(Color.GREEN);
        g2.drawString("[ESC] NO", x + 160, y + 150);
    }

    private void drawPixelWindow(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(x + 4, y + 4, w - 8, h - 8); // Border dalam
    }

    private int getCenterX(Graphics2D g2, String text) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }

    // ===== INPUT (SEMUA FUNGSI LAMA DIKEMBALIKAN) =====
    public void handleInput(int keyCode) {
        switch (menuState) {
            case MAIN:         handleMainInput(keyCode);     break;
            case SETTINGS:     handleSettingsInput(keyCode); break;
            case CONFIRM_QUIT: handleQuitInput(keyCode);     break;
            case LOAD_GAME: handleLoadInput(keyCode); break;
        }
    }
    private void handleLoadInput(int keyCode) {
        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP)
            loadSelectedIndex = (loadSelectedIndex - 1 + 3) % 3;
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN)
            loadSelectedIndex = (loadSelectedIndex + 1) % 3;
        if (keyCode == KeyEvent.VK_ENTER) {
            if (saveSlots[loadSelectedIndex] != null) {
                gp.setupGame();
                SaveManager.applyToGame(gp, saveSlots[loadSelectedIndex]);
                active = false;
            }
        }
        if (keyCode == KeyEvent.VK_ESCAPE) menuState = MenuState.MAIN;
    }
    
    private void handleMainInput(int keyCode) {
        Main.Log.d("KEY: " + keyCode + " | selectedIndex: " + selectedIndex); // DEBUG
        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) selectedIndex = (selectedIndex - 1 + 4) % 4;
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) selectedIndex = (selectedIndex + 1) % 4;
        if (keyCode == KeyEvent.VK_ENTER) {
            Main.Log.d("ENTER pressed, selectedIndex: " + selectedIndex); // DEBUG
            if (selectedIndex == 0) { gp.setupGame(); active = false; }
            if (selectedIndex == 1) { 
                saveSlots = SaveManager.getAllSlots();
                menuState = MenuState.LOAD_GAME; 
            }
            if (selectedIndex == 2) menuState = MenuState.SETTINGS;           
            if (selectedIndex == 3) menuState = MenuState.CONFIRM_QUIT;
        }
    }

    private void handleSettingsInput(int keyCode) {
        if (keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_UP) settingsIndex = (settingsIndex - 1 + 3) % 3;
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_DOWN) settingsIndex = (settingsIndex + 1) % 3;
        if (keyCode == KeyEvent.VK_ENTER) {
            if (settingsIndex == 0) { musicOn = !musicOn; settingsItems[0] = "Music: " + (musicOn ? "ON" : "OFF"); }
            if (settingsIndex == 1) { sfxOn = !sfxOn; settingsItems[1] = "SFX: " + (sfxOn ? "ON" : "OFF"); }
            if (settingsIndex == 2) menuState = MenuState.MAIN;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) menuState = MenuState.MAIN;
    }

    private void handleQuitInput(int keyCode) {
        if (keyCode == KeyEvent.VK_ENTER) System.exit(0);
        if (keyCode == KeyEvent.VK_ESCAPE) menuState = MenuState.MAIN;
    }
}