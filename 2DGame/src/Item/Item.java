package Item;

import java.awt.image.BufferedImage;

public class Item {
    public String name;
    public BufferedImage icon;
    public int maxStack = 99;

    public Item(String name, BufferedImage icon) {
        this.name = name;
        this.icon = icon;
    }
}
