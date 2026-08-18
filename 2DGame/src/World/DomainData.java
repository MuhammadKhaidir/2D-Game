package World;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class DomainData {
    public final String id;
    public final Rectangle triggerArea; // area di world buat trigger masuk (deket ruin)
    public final int spawnX, spawnY;     // posisi player saat masuk domain
    public final int width, height;      // ukuran interior domain (px)
    public final String backgroundPath;  // path gambar background domain
    public int exitWorldX = 0;
    public int exitWorldY = 0;
    public final List<Rectangle> collisions = new ArrayList<>();
    public final List<RoomData> rooms = new ArrayList<>(); // sub-ruangan (misal boss room)

    public DomainData(String id, Rectangle triggerArea, int spawnX, int spawnY,
                       int width, int height, String backgroundPath) {
        this.id = id;
        this.triggerArea = triggerArea;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.width = width;
        this.height = height;
        this.backgroundPath = backgroundPath;
    }
    public DomainData setExit(int x, int y) {
        this.exitWorldX = x;
        this.exitWorldY = y;
        return this;
    }
    public DomainData addCollision(int x, int y, int w, int h) {
        collisions.add(new Rectangle(x, y, w, h));
        return this;
    }
    public DomainData addRoom(RoomData r) {
        rooms.add(r);
        return this;
    }
}