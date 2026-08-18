package Entitas;

import java.awt.Rectangle;
import java.util.*;
import Main.GamePanel;
import Object.breakable; // ← import langsung, bukan pakai prefix "Object."

public class AStar {

    private final GamePanel gp;

    private static final int NODE_SIZE = 48;
    private static final int MAX_NODES = 800;

    public AStar(GamePanel gp) {
        this.gp = gp;
    }

    // ===== NODE =====
    private static class Node {
        int gridX, gridY;
        int worldX, worldY;
        int g, h;
        Node parent;

        Node(int gridX, int gridY, int worldX, int worldY) {
            this.gridX  = gridX;
            this.gridY  = gridY;
            this.worldX = worldX;
            this.worldY = worldY;
        }

        int f() { return g + h; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node)) return false;
            Node n = (Node) o;
            return gridX == n.gridX && gridY == n.gridY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(gridX, gridY);
        }
    }

    public List<int[]> findPath(int startWorldX, int startWorldY,
                                int targetWorldX, int targetWorldY) {

        int startGX = startWorldX / NODE_SIZE;
        int startGY = startWorldY / NODE_SIZE;
        int goalGX  = targetWorldX / NODE_SIZE;
        int goalGY  = targetWorldY / NODE_SIZE;

        if (startGX == goalGX && startGY == goalGY) return Collections.emptyList();

        Node startNode = new Node(startGX, startGY, startGX * NODE_SIZE, startGY * NODE_SIZE);
        Node goalNode  = new Node(goalGX,  goalGY,  goalGX  * NODE_SIZE, goalGY  * NODE_SIZE);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::f));
        Set<Node> closed         = new HashSet<>();
        Map<Node, Node> openMap  = new HashMap<>();

        startNode.g = 0;
        startNode.h = heuristic(startNode, goalNode);
        open.add(startNode);
        openMap.put(startNode, startNode);

        int iterations = 0;

        while (!open.isEmpty() && iterations < MAX_NODES) {
            iterations++;

            Node current = open.poll();
            openMap.remove(current);
            closed.add(current);

            if (current.equals(goalNode)) {
                return reconstructPath(current);
            }

            for (int[] dir : getDirections()) {
                int nx  = current.gridX + dir[0];
                int ny  = current.gridY + dir[1];
                int nwx = nx * NODE_SIZE;
                int nwy = ny * NODE_SIZE;

                int worldWidth  = gp.maxWorldCol * gp.tileSize;
                int worldHeight = gp.maxWorldRow * gp.tileSize;
                if (nwx < 0 || nwy < 0 || nwx >= worldWidth || nwy >= worldHeight) continue;

                Node neighbor = new Node(nx, ny, nwx, nwy);

                if (closed.contains(neighbor)) continue;
                if (isBlocked(nwx, nwy))       continue;

                int moveCost = current.g + (dir[0] != 0 && dir[1] != 0 ? 14 : 10);

                Node existing = openMap.get(neighbor);
                if (existing == null) {
                    neighbor.g      = moveCost;
                    neighbor.h      = heuristic(neighbor, goalNode);
                    neighbor.parent = current;
                    open.add(neighbor);
                    openMap.put(neighbor, neighbor);
                } else if (moveCost < existing.g) {
                    existing.g      = moveCost;
                    existing.parent = current;
                    open.remove(existing);
                    open.add(existing);
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean isBlocked(int worldX, int worldY) {

        Rectangle nodeRect = new Rectangle(worldX + 4, worldY + 4, NODE_SIZE - 8, NODE_SIZE - 8);

        // 1. Cek tile collision (4 sudut node)
        int[][] corners = {
            {worldX,             worldY},
            {worldX + NODE_SIZE - 1, worldY},
            {worldX,             worldY + NODE_SIZE - 1},
            {worldX + NODE_SIZE - 1, worldY + NODE_SIZE - 1}
        };

        for (int[] corner : corners) {
            int col = corner[0] / gp.tileSize;
            int row = corner[1] / gp.tileSize;
            if (col < 0 || row < 0 || col >= gp.maxWorldCol || row >= gp.maxWorldRow) continue;
            int tileNum = gp.tileM.mapTileNum[col][row];
            if (gp.tileM.tile[tileNum] != null && gp.tileM.tile[tileNum].collision) return true;
        }

        // 2. Cek object collision (worldObjects / gp.objCol)
        for (Rectangle r : gp.objCol.objects) {
            if (r.intersects(nodeRect)) return true;
        }

        // 3. Cek breakable yang masih utuh
        if (gp.tileM.breakables != null) {
            for (breakable b : gp.tileM.breakables) { // ← pakai nama class langsung, sudah di-import atas
                if (b == null || b.destroyed) continue;
                if (b.solidArea.intersects(nodeRect)) return true;
            }
        }

        return false;
    }

    private int heuristic(Node a, Node b) {
        int dx = Math.abs(a.gridX - b.gridX);
        int dy = Math.abs(a.gridY - b.gridY);
        return 10 * (dx + dy) - 6 * Math.min(dx, dy); // octile distance
    }

    private List<int[]> reconstructPath(Node goal) {
        LinkedList<int[]> path = new LinkedList<>();
        Node current = goal;
        while (current != null) {
            path.addFirst(new int[]{current.worldX, current.worldY});
            current = current.parent;
        }
        if (!path.isEmpty()) path.removeFirst();
        return path;
    }

    private int[][] getDirections() {
        return new int[][] {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0},
            {-1,-1}, {1,-1}, {-1, 1}, {1, 1}
        };
    }
}