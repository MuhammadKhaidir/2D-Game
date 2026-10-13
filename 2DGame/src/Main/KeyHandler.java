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
    public boolean tabPressed;
    GamePanel gp;
    public boolean f5Pressed;
	public boolean mPressed;
    
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
        if (code == KeyEvent.VK_TAB) { // buat liat stat player nih
            tabPressed = true;
        }
        // skill player, human btw
        if (code == KeyEvent.VK_M) mPressed = true;
        if (code == KeyEvent.VK_Z) zPressed = true;
        if (code == KeyEvent.VK_X) xPressed = true;
        if (code == KeyEvent.VK_C) cPressed = true;
        if (code == KeyEvent.VK_V) vPressed = true;
        
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
        if (code == KeyEvent.VK_1) onePressed = true;
        if (code == KeyEvent.VK_2) twoPressed = true;
        if (code == KeyEvent.VK_3) threePressed = true;
        
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
        if (code == KeyEvent.VK_F5) f5Pressed = false;
        if (code == KeyEvent.VK_M) mPressed = false;
        if (code == KeyEvent.VK_Z) zPressed = false;
        if (code == KeyEvent.VK_X) xPressed = false;
        if (code == KeyEvent.VK_C) cPressed = false;
        if (code == KeyEvent.VK_V) vPressed = false;
        
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