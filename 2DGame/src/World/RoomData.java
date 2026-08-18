package World;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub-ruangan di dalam sebuah domain (contoh: boss room di dalam Tomb).
 * Beda sama DomainData yang di-trigger dari OUTSIDE, RoomData di-trigger
 * dari DALAM domain (lewat pintu).
 */
public class RoomData {
    public final String id;
    public final Rectangle doorTriggerArea; // posisi pintu di dalam domain induk
    public final int spawnX, spawnY;         // posisi player saat masuk room
    public final int width, height;          // ukuran interior room (px)
    public final String backgroundPath;      // path gambar background room
    public final List<Rectangle> collisions = new ArrayList<>();

    // Titik player balik ke domain induk pas keluar room
    public int returnX = 0;
    public int returnY = 0;

    public RoomData(String id, Rectangle doorTriggerArea, int spawnX, int spawnY,
                     int width, int height, String backgroundPath) {
        this.id = id;
        this.doorTriggerArea = doorTriggerArea;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.width = width;
        this.height = height;
        this.backgroundPath = backgroundPath;
    }

    public RoomData setReturnPoint(int x, int y) {
        this.returnX = x;
        this.returnY = y;
        return this;
    }

    public RoomData addCollision(int x, int y, int w, int h) {
        collisions.add(new Rectangle(x, y, w, h));
        return this;
    }
}