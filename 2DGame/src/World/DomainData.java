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

    // ── Titik keluar DI DALAM domain (koordinat interior). Deketin -> otomatis keluar ──
    public boolean hasExitPoint = false;
    public int exitPointX = 0;
    public int exitPointY = 0;
    public int exitPointRadius = 60;

    public final List<Rectangle> collisions = new ArrayList<>();
    public final List<RoomData> rooms = new ArrayList<>(); // sub-ruangan (misal boss room)

    // Peta tembok berbasis gambar (hitam-putih) buat domain yang bentuknya gak kotak. Dipakai barengan sama `collisions`.
    public String walkMaskPath = null;

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
    /** Titik di dalam interior yang bikin player keluar otomatis kalau didekati (dalam radius px). */
    public DomainData setExitPoint(int x, int y, int radius) {
        this.hasExitPoint = true;
        this.exitPointX = x;
        this.exitPointY = y;
        this.exitPointRadius = radius;
        return this;
    }
    /** Pakai gambar hitam-putih sebagai peta tembok (putih = lantai, hitam = tembok). Lihat WalkMask. */
    public DomainData setWalkMask(String path) {
        this.walkMaskPath = path;
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