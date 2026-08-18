package World;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedObjectManager {

    private List<AnimatedObject> objects = new ArrayList<>();
    private Class<?> loader;

    public AnimatedObjectManager(Class<?> loader) {
        this.loader = loader;
        registerAll();
    }

    private void registerAll() {

        add(bushFrames(), 800, 1975, 48, 48, 6);
        add(bushFrames(), 790, 1950, 40, 40, 5);
        add(bushFrames(), 810, 1130, 60, 60, 5);add(bushFrames(), 820, 1120, 60, 60, 5);add(bushFrames(), 800, 1140, 60, 60, 5);
        add(bushFrames(), 880, 1300, 60, 60, 5);add(bushFrames(), 880, 1320, 60, 60, 5);add(bushFrames(), 600, 1260, 60, 60, 5);
        add(bushFrames(), 590, 1320, 60, 60, 5);add(bushFrames(), 880, 1300, 60, 60, 5);add(bushFrames(), 550, 1320, 60, 60, 5);
        add(bushFrames(), 570, 1320, 60, 60, 5);add(bushFrames(), 610, 1250, 60, 60, 5);add(bushFrames(), 630, 1150, 60, 60, 5);
        add(bushFrames(), 550, 1280, 60, 60, 5);add(bushFrames(), 580, 1300, 60, 60, 5);add(bushFrames(), 550, 1300, 60, 60, 5);
        add(bushFrames(), 580, 1280, 60, 60, 5);add(bushFrames(), 600, 1280, 60, 60, 5);add(bushFrames(), 600, 1300, 60, 60, 5);
        add(bushFrames(), 620, 1280, 60, 60, 5);add(bushFrames(), 620, 1300, 60, 60, 5);add(bushFrames(), 620, 1320, 60, 60, 5);
        add(bushFrames(), 620, 1340, 60, 60, 5);

        // deket monoastery
        add(bushFrames(), 1500,700, 60, 60, 7);add(bushFrames(), 1510, 690, 60, 60, 6);add(bushFrames(), 1490, 710, 60, 60, 5);
        add(bushFrames(), 1510,680, 60, 60, 7);add(bushFrames(), 1510, 740, 60, 60, 6);add(bushFrames(), 1470, 710, 60, 60, 5);
        add(bushFrames(), 1530,700, 60, 60, 7);add(bushFrames(), 1530, 680, 60, 60, 6);add(bushFrames(), 1500, 720, 60, 60, 5);
        add(bushFrames(), 1480, 720, 60, 60, 5);add(bushFrames(), 1530, 735, 60, 60, 5);add(bushFrames(), 750, 2357, 60, 60, 5);
        add(bushFrames(), 850, 2320, 60, 60, 5);add(bushFrames(), 875, 2320, 60, 60, 5);
        add(bushFrames(), 640, 2450, 60, 60, 5);add(bushFrames(), 640, 2430, 60, 60, 5);add(bushFrames(), 650, 2470, 60, 60, 5);add(bushFrames(), 670, 2485, 60, 60, 5);
        
        
        // dibwah
        add(bushFrames(), 530, 2265, 60, 60, 5);

        clump(1500, 1500, 10, 70, 70, 0, 10);
        clump(1040, 2350, 20, 70, 70, 0, 10);
        
    }

    private String[] bushFrames() {
        return new String[] {
            "/Aset/Bush1000.png",
            "/Aset/Bush1001.png",
            "/Aset/Bush1002.png",
            "/Aset/Bush1003.png",
            "/Aset/Bush1004.png",
            "/Aset/Bush1005.png",
            "/Aset/Bush1006.png",
            "/Aset/Bush1007.png",
        };
    }

    private void clump(double cx, double cy, int count,
            double minSize, double maxSize,
            int minDel, int maxDel) {
        if (minSize > maxSize) { double t = minSize; minSize = maxSize; maxSize = t; }
        if (minDel  > maxDel)  { int t = minDel;  minDel  = maxDel;  maxDel  = t; }

        Random rng = new Random((long) cx * 31 + (long) cy);
        int delRange = maxDel - minDel;

        int cols = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil((double) count / cols);

        double size = minSize;

        // step pakai double → posisi tiap bush presisi, bisa koma
        double step = size * 1.5 / 4.0;

        double startX = cx - (cols * step) / 2.0;
        double startY = cy - (rows * step) / 2.0;

        int placed = 0;
        for (int r = 0; r < rows && placed < count; r++) {
            for (int c = 0; c < cols && placed < count; c++) {
                double ox = startX + c * step;
                double oy = startY + r * step;
                int del = minDel + (delRange > 0 ? rng.nextInt(delRange + 1) : 0);
                add(bushFrames(), (int) Math.round(ox), (int) Math.round(oy),
                    (int) Math.round(size), (int) Math.round(size), del);
                placed++;
            }
        }
    }

    private void add(String[] paths, int x, int y, int w, int h, int delay) {
        objects.add(new AnimatedObject(paths, x, y, w, h, delay, loader));
    }

    public void update() {
        for (AnimatedObject obj : objects) obj.update();
    }

    public void draw(Graphics2D g2, int camX, int camY,
                     int playerScreenX, int playerScreenY,
                     int screenW, int screenH) {
        for (AnimatedObject obj : objects) {
            int sx = obj.worldX - camX + playerScreenX;
            int sy = obj.worldY - camY + playerScreenY;

            if (sx + obj.width < 0 || sx > screenW) continue;
            if (sy + obj.height < 0 || sy > screenH) continue;

            obj.draw(g2, sx, sy);
        }
    }
}