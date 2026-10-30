package Main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean emoteHi, speedUp;
    public boolean interactPressed = false;
    public boolean escPressed = false;
    public boolean onePressed, twoPressed, threePressed;
    public boolean shiftPressed = false;
    public boolean chatPressed = false;
    public boolean tPressed = false; // ◄ BARU: Ditambahkan untuk mendeteksi tombol T
    public boolean fPressed = false; // tombol F = mancing (lempar / tarik pancingan). Dikonsumsi FishingManager.handleKey
    private boolean fKeyDown = false; // nahan F cuma dihitung SEKALI (auto-repeat keyboard diabaikan)
    public boolean enterPressed = false;
    public boolean backspacePressed = false;
    public char typedChar;
    public boolean inspectPressed;
    public boolean spacePressed = false;
    public boolean yPressed = false;
    public boolean zPressed = false;
    public boolean xPressed = false;
    public boolean cPressed = false;
    public boolean vPressed = false;
    public boolean rPressed = false; // skill senjata Leviathan Fang (Maw of the Forgotten)
    public boolean tabPressed;
    GamePanel gp;
    public boolean f5Pressed;
    // buat nahan auto-repeat: nahan angka 1-5 cuma dihitung SEKALI tekan (biar potion gak keabisan)
    private final boolean[] hotbarKeyDown = new boolean[5];
	public boolean mPressed;
    
    /** VK_1..VK_5 (atau numpad 1..5) → 0..4, selain itu -1. */
    private int hotbarIndexFor(int code) {
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_5) return code - KeyEvent.VK_1;
        if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD5) return code - KeyEvent.VK_NUMPAD1;
        return -1;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        typedChar = e.getKeyChar();
    }

    public void mouseMoved(MouseEvent e) {
        if (gp.backpack != null) {
            gp.backpack.handleMouseMove(e, gp);
        }
    }
    
    @Override
    public void keyPressed(KeyEvent e) {

        int code = e.getKeyCode();
        
        if (gp != null && gp.mainMenu != null && gp.mainMenu.active) {
            gp.mainMenu.handleInput(e.getKeyCode());
            return;
        }
        if (code == KeyEvent.VK_F3 && gp != null) { // toggle koordinat player
            gp.toggleDebug();
        }
        if (code == KeyEvent.VK_F4 && gp != null) { // toggle kotak collision + log konsol
            gp.toggleCollisionDebug();
        }
        // ZOOM: tombol + / = (zoom in), - (zoom out), 0 (reset 100%). Scroll mouse juga bisa.
        if (gp != null && gp.canZoom()) {
            if (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD) {
                gp.zoomBy(GamePanel.ZOOM_STEP);
            } else if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) {
                gp.zoomBy(-GamePanel.ZOOM_STEP);
            } else if (code == KeyEvent.VK_0 || code == KeyEvent.VK_NUMPAD0) {
                gp.resetZoom();
            }
        }
        if (code == KeyEvent.VK_TAB) { // buat liat stat player nih
            tabPressed = true;
        }
        // skill player, human btw
        if (code == KeyEvent.VK_F) {
            if (!fKeyDown) fPressed = true;
            fKeyDown = true;
        }
        if (code == KeyEvent.VK_M) mPressed = true;
        if (code == KeyEvent.VK_Z) zPressed = true;
        if (code == KeyEvent.VK_X) xPressed = true;
        if (code == KeyEvent.VK_C) cPressed = true;
        if (code == KeyEvent.VK_V) vPressed = true;
        if (code == KeyEvent.VK_R) rPressed = true;
        
        if (code == KeyEvent.VK_I) {
            inspectPressed = true;
        }
        if (code == KeyEvent.VK_F5) f5Pressed = true;
        if(code == KeyEvent.VK_W) upPressed = true;
        if(code == KeyEvent.VK_S) downPressed = true;
        if(code == KeyEvent.VK_A) leftPressed = true;
        if(code == KeyEvent.VK_D) rightPressed = true;
        if(code == KeyEvent.VK_H) emoteHi = true;
        if(code == KeyEvent.VK_E) interactPressed = true;
        if(code == KeyEvent.VK_ESCAPE) {
            escPressed = true;
            // ◄ BARU: Kirim ESC ke BackpackUI biar bisa nutup panel/description/delete-confirm
            if (gp != null && gp.backpack != null) {
                gp.backpack.handleKeyPress(code);
            }
        }
        // Angka 1-5: HOTBAR (pakai item di slot). Tapi kalau lagi dialog / panel command NPC,
        // angka 1-3 tetap jadi pilihan dialog kayak sebelumnya.
        int hb = hotbarIndexFor(code);
        if (hb != -1) {
            boolean npcMenuOpen = gp != null && (gp.dialogOn || gp.inspectOn);
            if (npcMenuOpen || gp == null || gp.hotbar == null) {
                if (hb == 0) onePressed = true;
                else if (hb == 1) twoPressed = true;
                else if (hb == 2) threePressed = true;
            } else if (!hotbarKeyDown[hb]) {
                hotbarKeyDown[hb] = true;
                gp.hotbar.onNumberKey(hb);
            }
        }
        
        // ◄ PERBAIKAN: Tombol T sekarang mengaktifkan tPressed untuk DeathTotem
        if (code == KeyEvent.VK_T) tPressed = true; 
        
        if (code == KeyEvent.VK_ENTER) enterPressed = true;
        if (code == KeyEvent.VK_BACK_SPACE) backspacePressed = true;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_Y) {
            yPressed = true;
        }
        if (code == KeyEvent.VK_SPACE) spacePressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int code = e.getKeyCode();
        int hbRel = hotbarIndexFor(code);
        if (hbRel != -1) hotbarKeyDown[hbRel] = false;
        if (code == KeyEvent.VK_F5) f5Pressed = false;
        if (code == KeyEvent.VK_F) fKeyDown = false;
        if (code == KeyEvent.VK_M) mPressed = false;
        if (code == KeyEvent.VK_Z) zPressed = false;
        if (code == KeyEvent.VK_X) xPressed = false;
        if (code == KeyEvent.VK_C) cPressed = false;
        if (code == KeyEvent.VK_V) vPressed = false;
        if (code == KeyEvent.VK_R) rPressed = false;
        
        if(code == KeyEvent.VK_W) upPressed = false;
        if(code == KeyEvent.VK_S) downPressed = false;
        if(code == KeyEvent.VK_A) leftPressed = false;
        if(code == KeyEvent.VK_D) rightPressed = false;
        if(code == KeyEvent.VK_E) interactPressed = false;
        if(code == KeyEvent.VK_ESCAPE) escPressed = false;
        
        // ◄ BARU: Matikan status tPressed saat tombol dilepas
        if(code == KeyEvent.VK_T) tPressed = false; 
        if(code == KeyEvent.VK_T) escPressed = false; 
        
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        }
        if (code == KeyEvent.VK_SPACE) spacePressed = false;
    }
}