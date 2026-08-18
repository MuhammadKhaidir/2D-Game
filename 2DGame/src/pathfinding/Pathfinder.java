package pathfinding;

import java.util.ArrayList;
import Main.GamePanel;

public class Pathfinder {

    GamePanel gp;
    public Node[][] nodes;
    public ArrayList<Node> openList = new ArrayList<>();
    public ArrayList<Node> pathList = new ArrayList<>();

    Node startNode, goalNode, currentNode;

    public Pathfinder(GamePanel gp) {
        this.gp = gp;
        initNodes();
    }

    public void initNodes() {

        nodes = new Node[gp.maxWorldCol][gp.maxWorldRow];

        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {

                nodes[col][row] = new Node(col, row);

                // ❗ HANYA TILE & OBJECT
                nodes[col][row].solid =
                    gp.tileM.tile[gp.tileM.mapTileNum[col][row]].collision;
            }
        }
    }

    public boolean search(int startCol, int startRow, int goalCol, int goalRow) {
    	for (int col = 0; col < gp.maxWorldCol; col++) {
    	    for (int row = 0; row < gp.maxWorldRow; row++) {
    	        nodes[col][row].parent = null;
    	        nodes[col][row].gCost = 0;
    	        nodes[col][row].hCost = 0;
    	        nodes[col][row].fCost = 0;
    	    }
    	}

        openList.clear();
        pathList.clear();

        startNode = nodes[startCol][startRow];
        goalNode  = nodes[goalCol][goalRow];

        openList.add(startNode);

        while (!openList.isEmpty()) {

            currentNode = openList.get(0);
            for (Node n : openList) {
                if (n.fCost < currentNode.fCost) currentNode = n;
            }

            openList.remove(currentNode);

            if (currentNode == goalNode) {
                buildPath();
                return true;
            }

            openNeighbors(currentNode);
        }
        return false;
    }

    void openNeighbors(Node node) {

        int[] dx = {0, -1, 1, 0};
        int[] dy = {-1, 0, 0, 1};

        for (int i = 0; i < 4; i++) {

            int col = node.col + dx[i];
            int row = node.row + dy[i];

            if (col < 0 || row < 0 ||
                col >= gp.maxWorldCol || row >= gp.maxWorldRow)
                continue;

            Node neighbor = nodes[col][row];

            if (neighbor.solid || openList.contains(neighbor))
                continue;

            neighbor.parent = node;
            neighbor.gCost = node.gCost + 10;
            neighbor.hCost = Math.abs(col - goalNode.col)
                           + Math.abs(row - goalNode.row);
            neighbor.calculateFCost();

            openList.add(neighbor);
        }
    }

    void buildPath() {
        Node current = goalNode;
        while (current != startNode) {
            pathList.add(0, current);
            current = current.parent;
        }
    }

public ArrayList<Node> searchPath(
        int startCol, int startRow,
        int goalCol, int goalRow
) {

    // sementara: PATH LURUS (SAFE, TIDAK NGERUSAK)
    ArrayList<Node> path = new ArrayList<>();

    int col = startCol;
    int row = startRow;

    while (col != goalCol || row != goalRow) {

        if (col < goalCol) col++;
        else if (col > goalCol) col--;
        else if (row < goalRow) row++;
        else if (row > goalRow) row--;

        path.add(new Node(col, row));
    }

    return path;
}}
