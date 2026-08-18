package Object;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * PlayerArmor - kelas GENERIC buat satu armor piece (helm, chest, dll).
 * Tinggal instantiate lagi pake prefix nama beda kalau armor lain formatnya sama.
 * Contoh: new PlayerArmor("IronHelm"), nanti kalau udah ada chest: new PlayerArmor("IronChest")
 *
 * Format asset yang dipake (X = prefix, misal "IronHelm"):
 *   Walk   : X + Front/Back/Left/RightWalk (1)-(4)
 *   Run    : X + Front/Back/Left/RightRun (1)-(3)
 *   Thrust : X + Front/Back/Left/RightThrust (1)-(4)  -> attackType 1
 *   Bash   : X + Front/Back/Left/RightBash (1)-(4)    -> attackType 2
 *   Swing1 : X + Front/Back/Left/RightSwing (1)-(4)   -> attackType 3
 *   Swing2 : X + Front/Back/Left/RightSwing (5)-(6)   -> attackType 4
 *   Idle   : X + Front/Back/Left/RightIdle (1)          -> baru 1 frame, gampang diganti kalau nambah
 *
 * Kalau ada asset yang belum digambar, readSafely() bakal return null dan cuma
 * nge-print warning di console (nggak bikin crash). Waktu digambar, semua getter
 * di sini juga null-safe, jadi kalau frame-nya null berarti helm nggak digambar
 * di state itu (silent skip) - dicek lagi di Player.draw().
 */
public class PlayerArmor {

    public final String itemName;

    // ===== WALK (4 frame/arah) =====
    private final BufferedImage[] walkFront = new BufferedImage[4];
    private final BufferedImage[] walkBack  = new BufferedImage[4];
    private final BufferedImage[] walkLeft  = new BufferedImage[4];
    private final BufferedImage[] walkRight = new BufferedImage[4];

    // ===== RUN (3 frame/arah) =====
    private final BufferedImage[] runFront = new BufferedImage[3];
    private final BufferedImage[] runBack  = new BufferedImage[3];
    private final BufferedImage[] runLeft  = new BufferedImage[3];
    private final BufferedImage[] runRight = new BufferedImage[3];

    // ===== THRUST / attackType 1 (4 frame/arah) =====
    private final BufferedImage[] thrustFront = new BufferedImage[4];
    private final BufferedImage[] thrustBack  = new BufferedImage[4];
    private final BufferedImage[] thrustLeft  = new BufferedImage[4];
    private final BufferedImage[] thrustRight = new BufferedImage[4];

    // ===== BASH / attackType 2 (4 frame/arah) =====
    private final BufferedImage[] bashFront = new BufferedImage[4];
    private final BufferedImage[] bashBack  = new BufferedImage[4];
    private final BufferedImage[] bashLeft  = new BufferedImage[4];
    private final BufferedImage[] bashRight = new BufferedImage[4];

    // ===== SWING state 3 (frame 1-4, 4 frame) & state 4 (frame 5-6, 2 frame), 4 arah =====
    private final BufferedImage[] swing1Front = new BufferedImage[4];
    private final BufferedImage[] swing1Back  = new BufferedImage[4];
    private final BufferedImage[] swing1Left  = new BufferedImage[4];
    private final BufferedImage[] swing1Right = new BufferedImage[4];
    private final BufferedImage[] swing2Front = new BufferedImage[2];
    private final BufferedImage[] swing2Back  = new BufferedImage[2];
    private final BufferedImage[] swing2Left  = new BufferedImage[2];
    private final BufferedImage[] swing2Right = new BufferedImage[2];

    // ===== IDLE (1 frame/arah, baru ada 1 gambar) =====
    private final BufferedImage[] idleFront = new BufferedImage[1];
    private final BufferedImage[] idleBack  = new BufferedImage[1];
    private final BufferedImage[] idleLeft  = new BufferedImage[1];
    private final BufferedImage[] idleRight = new BufferedImage[1];

    public PlayerArmor(String itemName) {
        this.itemName = itemName;
        loadAll();
    }

    private BufferedImage readSafely(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[PlayerArmor:" + itemName + "] Asset GA KETEMU: " + path);
            return null;
        }
        try {
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("[PlayerArmor:" + itemName + "] Gagal baca: " + path);
            return null;
        }
    }

    private void loadAll() {
        // Walk
        for (int i = 0; i < 4; i++) {
            int f = i + 1;
            walkFront[i] = readSafely("/Aset/" + itemName + "FrontWalk (" + f + ").png");
            walkBack[i]  = readSafely("/Aset/" + itemName + "BackWalk ("  + f + ").png");
            walkLeft[i]  = readSafely("/Aset/" + itemName + "LeftWalk ("  + f + ").png");
            walkRight[i] = readSafely("/Aset/" + itemName + "RightWalk (" + f + ").png");
        }

        // Run (4 arah)
        for (int i = 0; i < 3; i++) {
            int f = i + 1;
            runFront[i] = readSafely("/Aset/" + itemName + "FrontRun (" + f + ").png");
            runBack[i]  = readSafely("/Aset/" + itemName + "BackRun ("  + f + ").png");
            runLeft[i]  = readSafely("/Aset/" + itemName + "LeftRun ("  + f + ").png");
            runRight[i] = readSafely("/Aset/" + itemName + "RightRun (" + f + ").png");
        }

        // Thrust (attackType 1)
        for (int i = 0; i < 4; i++) {
            int f = i + 1;
            thrustFront[i] = readSafely("/Aset/" + itemName + "FrontThrust (" + f + ").png");
            thrustBack[i]  = readSafely("/Aset/" + itemName + "BackThrust ("  + f + ").png");
            thrustLeft[i]  = readSafely("/Aset/" + itemName + "LeftThrust ("  + f + ").png");
            thrustRight[i] = readSafely("/Aset/" + itemName + "RightThrust (" + f + ").png");
        }

        // Bash (attackType 2)
        for (int i = 0; i < 4; i++) {
            int f = i + 1;
            bashFront[i] = readSafely("/Aset/" + itemName + "FrontBash (" + f + ").png");
            bashBack[i]  = readSafely("/Aset/" + itemName + "BackBash ("  + f + ").png");
            bashLeft[i]  = readSafely("/Aset/" + itemName + "LeftBash ("  + f + ").png");
            bashRight[i] = readSafely("/Aset/" + itemName + "RightBash (" + f + ").png");
        }

        // Swing state 3 -> frame (1)-(4), 4 arah
        for (int i = 0; i < 4; i++) {
            int f3 = i + 1;
            swing1Front[i] = readSafely("/Aset/" + itemName + "FrontSwing (" + f3 + ").png");
            swing1Back[i]  = readSafely("/Aset/" + itemName + "BackSwing ("  + f3 + ").png");
            swing1Left[i]  = readSafely("/Aset/" + itemName + "LeftSwing ("  + f3 + ").png");
            swing1Right[i] = readSafely("/Aset/" + itemName + "RightSwing (" + f3 + ").png");
        }

        // Swing state 4 -> frame (5)-(6), 4 arah
        for (int i = 0; i < 2; i++) {
            int f4 = i + 5;
            swing2Front[i] = readSafely("/Aset/" + itemName + "FrontSwing (" + f4 + ").png");
            swing2Back[i]  = readSafely("/Aset/" + itemName + "BackSwing ("  + f4 + ").png");
            swing2Left[i]  = readSafely("/Aset/" + itemName + "LeftSwing ("  + f4 + ").png");
            swing2Right[i] = readSafely("/Aset/" + itemName + "RightSwing (" + f4 + ").png");
        }

        // Idle (1 frame/arah, baru ada 1 gambar - tinggal ganti angka di sini kalau nambah frame lagi)
        for (int i = 0; i < 1; i++) {
            int f = i + 1;
            idleFront[i] = readSafely("/Aset/" + itemName + "FrontIdle (" + f + ").png");
            idleBack[i]  = readSafely("/Aset/" + itemName + "BackIdle ("  + f + ").png");
            idleLeft[i]  = readSafely("/Aset/" + itemName + "LeftIdle ("  + f + ").png");
            idleRight[i] = readSafely("/Aset/" + itemName + "RightIdle (" + f + ").png");
        }
    }

    // ================= GETTERS (semua null-safe) =================

    public BufferedImage getWalkFrame(int frameIndex, String direction) {
        return safeGet(pick(walkFront, walkBack, walkLeft, walkRight, direction), frameIndex);
    }

    public BufferedImage getRunFrame(int frameIndex, String direction) {
        return safeGet(pick(runFront, runBack, runLeft, runRight, direction), frameIndex);
    }

    public BufferedImage getIdleFrame(int frameIndex, String direction) {
        return safeGet(pick(idleFront, idleBack, idleLeft, idleRight, direction), frameIndex);
    }

    /** attackType: 1=Thrust, 2=Bash, 3=Swing awal, 4=Swing lanjutan (sesuai attackType di player) */
    public BufferedImage getAttackFrame(int attackType, int frameIndex, String direction) {
        BufferedImage[] arr;
        switch (attackType) {
            case 1:
                arr = pick(thrustFront, thrustBack, thrustLeft, thrustRight, direction);
                break;
            case 2:
                arr = pick(bashFront, bashBack, bashLeft, bashRight, direction);
                break;
            case 3:
                arr = pick(swing1Front, swing1Back, swing1Left, swing1Right, direction);
                break;
            case 4:
                arr = pick(swing2Front, swing2Back, swing2Left, swing2Right, direction);
                break;
            default:
                return null;
        }
        return safeGet(arr, frameIndex);
    }

    private BufferedImage[] pick(BufferedImage[] front, BufferedImage[] back, BufferedImage[] left, BufferedImage[] right, String direction) {
        switch (direction) {
            case "down":  return front;
            case "up":    return back;
            case "left":  return left;
            case "right": return right;
            default:      return front;
        }
    }

    private BufferedImage safeGet(BufferedImage[] arr, int frameIndex) {
        if (arr == null || arr.length == 0 || frameIndex < 0) return null;
        return arr[frameIndex % arr.length]; // modulo biar aman kalo jumlah frame beda sama player
    }
}