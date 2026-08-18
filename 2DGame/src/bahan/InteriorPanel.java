package bahan;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import Entitas.player;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import Main.GamePanel;
import Main.GamePanel.GameState;

import java.awt.Graphics;
import java.awt.Color;

public class InteriorPanel extends JPanel {

    GamePanel gp;

    public InteriorPanel(GamePanel gp) {
        this.gp = gp;
        setFocusable(true);
        setBackground(Color.BLACK);
        setOpaque(true);
        // === KEY BINDING ESC ===
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "exitHouse");

        getActionMap().put("exitHouse", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitHouse();
            }
        });
    }




    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.white);
        g.drawString("RUMAH KOSONG", 100, 100);
        g.drawString("Tekan ESC untuk keluar", 100, 120);
        checkExitHouse();
    }
    public void exitHouse() {

        // posisi keluar rumah (depan pintu)
    	gp.player.x = 100;
    	gp.player.y = 800;
    }


    public void checkExitHouse() {

        if (gp.KeyH.escPressed) {
            exitHouse();
            gp.KeyH.escPressed = false; // anti spam
        }
    }

}
