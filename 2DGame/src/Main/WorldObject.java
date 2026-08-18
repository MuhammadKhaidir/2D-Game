package Main;

import java.awt.Rectangle;
import java.util.ArrayList;

public class WorldObject {

    public ArrayList<Rectangle> objects = new ArrayList<>();

    public void add(int x, int y, int w, int h) {
        objects.add(new Rectangle(x, y, w, h));
    }

    // 🔥 TAMBAHIN INI
    public ArrayList<Rectangle> getList() {
        return objects;
    }
}
