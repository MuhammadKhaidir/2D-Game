package World;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public class RainDrop {

    int x, y;
    int speed;
    int screenW, screenH;

    public RainDrop(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;

        Random r = new Random();
        x = r.nextInt(screenW);
        y = r.nextInt(screenH);
        speed = 6 + r.nextInt(4);
    }

    public void update() {
        y += speed;
        if (y > screenH) {
            y = 0;
            x = new Random().nextInt(screenW);
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(new Color(180, 180, 255, 80));
        g2.drawLine(x, y, x + 15, y + 100);
    }
}
