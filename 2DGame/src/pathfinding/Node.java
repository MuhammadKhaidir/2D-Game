package pathfinding;

public class Node {
    public int col, row;
    public int gCost, hCost, fCost;
    public Node parent;
    public boolean solid;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public void calculateFCost() {
        fCost = gCost + hCost;
    }
}
