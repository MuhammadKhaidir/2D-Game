package Item;

public class ItemStack {
    public Item item;
    public int amount;

    public ItemStack(Item item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public boolean isSame(Item other) {
        if (item == null || other == null) return false;
        return item.name.equals(other.name);
    }

}
