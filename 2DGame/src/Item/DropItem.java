package Item;

import java.awt.Rectangle;

public class DropItem {
    public int x, y;
    public Item item;
    public int amount;

    public Rectangle solidArea;

    public DropItem(int x, int y, Item item, int amount){
        this.x = x;
        this.y = y;
        this.item = item;
        this.amount = amount;

        solidArea = new Rectangle(0, 0, 32, 32);
    }

    public Rectangle getHitbox(){
        return new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }
}