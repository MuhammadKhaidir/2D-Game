package bahan;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandlerObj implements KeyListener {

    public boolean interactPressed = false;

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_E) {
            interactPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_E) {
            interactPressed = false;
        }
    }
}
