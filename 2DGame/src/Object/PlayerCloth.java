package Object;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * PlayerCloth - Mengelola animasi baju/kostum karakter secara terpisah.
 *
 * Mapping frame sesuai state serangan:
 *   State 1 (Attack)  → BaseClothFront2/Back2/Left2/Right2 frame (1)-(4)
 *   State 2 (Swing)   → BaseClothFront2/Back2/Left2/Right2 frame (5)-(8)
 *   State 3           → BaseClothFront/Back/Left/Right      frame (1)-(4)
 *   State 4           → BaseClothFront/Back/Left/Right      frame (5)-(8)
 *
 * Bila senjata TIDAK diequip, hanya state 1 & 2 yang dimuat (weaponEquipped = false).
 */
public class PlayerCloth {

    public final String clothName;

    // ── State 1: Attack (4 frame per arah) ──────────────────────────────────
    public BufferedImage[] state1Up    = new BufferedImage[4];
    public BufferedImage[] state1Down  = new BufferedImage[4];
    public BufferedImage[] state1Left  = new BufferedImage[4];
    public BufferedImage[] state1Right = new BufferedImage[4];

    // ── State 2: Swing (4 frame per arah, diambil dari indeks 5-8) ──────────
    public BufferedImage[] state2Up    = new BufferedImage[4];
    public BufferedImage[] state2Down  = new BufferedImage[4];
    public BufferedImage[] state2Left  = new BufferedImage[4];
    public BufferedImage[] state2Right = new BufferedImage[4];

    // ── State 3: (4 frame per arah, hanya ada jika senjata diequip) ─────────
    public BufferedImage[] state3Up    = new BufferedImage[4];
    public BufferedImage[] state3Down  = new BufferedImage[4];
    public BufferedImage[] state3Left  = new BufferedImage[4];
    public BufferedImage[] state3Right = new BufferedImage[4];

    // ── State 4: (4 frame per arah, hanya ada jika senjata diequip) ─────────
    public BufferedImage[] state4Up    = new BufferedImage[4];
    public BufferedImage[] state4Down  = new BufferedImage[4];
    public BufferedImage[] state4Left  = new BufferedImage[4];
    public BufferedImage[] state4Right = new BufferedImage[4];
    
    BufferedImage[] clothDeadFront = new BufferedImage[3];
    BufferedImage[] clothDeadBack  = new BufferedImage[3];
    BufferedImage[] clothDeadLeft  = new BufferedImage[3];
    BufferedImage[] clothDeadRight = new BufferedImage[3];
    
 // ── Array baru di field PlayerCloth ──
    BufferedImage[] clothWalkFront = new BufferedImage[6];
    BufferedImage[] clothWalkBack  = new BufferedImage[6];
    BufferedImage[] clothWalkLeft  = new BufferedImage[6];
    BufferedImage[] clothWalkRight = new BufferedImage[6];

    BufferedImage[] clothRunFront  = new BufferedImage[3];
    BufferedImage[] clothRunBack   = new BufferedImage[3];
    BufferedImage[] clothRunLeft   = new BufferedImage[3];
    BufferedImage[] clothRunRight  = new BufferedImage[3];

    // Jump: cuma index 0 dan 1, tapi urutan animasinya {0,1,0} ditangani di player
    BufferedImage[] clothJumpFront = new BufferedImage[2];
    BufferedImage[] clothJumpBack  = new BufferedImage[2];
    BufferedImage[] clothJumpLeft  = new BufferedImage[2];
    BufferedImage[] clothJumpRight = new BufferedImage[2];

    BufferedImage clothIdleFront;
    BufferedImage clothIdleBack;
    BufferedImage clothIdleLeft;
    BufferedImage clothIdleRight;

    // ── Flag apakah state 3 & 4 sudah dimuat ────────────────────────────────
    private boolean weaponStatesLoaded = false;

    /**
     * @param clothName     Nama baju ini (untuk logging)
     * @param weaponEquipped true  → muat semua 4 state
     *                       false → muat state 1 & 2 saja
     */
    public PlayerCloth(String clothName, boolean weaponEquipped) {
        this.clothName = clothName;
        loadClothImages(weaponEquipped);
    }

    // ── Utility: baca gambar tanpa throw ─────────────────────────────────────
    private BufferedImage readSafely(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            System.err.println("[PlayerCloth] Asset tidak ditemukan: " + path);
            return null;
        }
        try {
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.println("[PlayerCloth] Gagal membaca: " + path);
            return null;
        }
    }

    // ── Load semua frame ──────────────────────────────────────────────────────
    private void loadClothImages(boolean weaponEquipped) {
        System.out.println("[PlayerCloth] Memuat baju: " + clothName
                + " | mode: " + (weaponEquipped ? "dengan senjata" : "tanpa senjata"));

        // ── State 1 → BaseClothXxx2 (1)-(4) ─────────────────────────────────
        for (int i = 0; i < 4; i++) {
            int f = i + 1;
            state1Up[i]    = readSafely("/Aset/BaseClothBack2 ("  + f + ").png");
            state1Down[i]  = readSafely("/Aset/BaseClothFront2 (" + f + ").png");
            state1Left[i]  = readSafely("/Aset/BaseClothLeft2 ("  + f + ").png");
            state1Right[i] = readSafely("/Aset/BaseClothRight2 (" + f + ").png");
        }

        // ── State 2 → BaseClothXxx2 (5)-(8) ─────────────────────────────────
        for (int i = 0; i < 4; i++) {
            int f = i + 5;
            state2Up[i]    = readSafely("/Aset/BaseClothBack2 ("  + f + ").png");
            state2Down[i]  = readSafely("/Aset/BaseClothFront2 (" + f + ").png");
            state2Left[i]  = readSafely("/Aset/BaseClothLeft2 ("  + f + ").png");
            state2Right[i] = readSafely("/Aset/BaseClothRight2 (" + f + ").png");
        }
        
        

        // ── State 3 & 4 hanya dimuat jika senjata diequip ────────────────────
        if (weaponEquipped) {
            loadWeaponStates();
        }
        loadMovementImages();
    }

    /**
     * Muat state 3 & 4 secara terpisah.
     * Dipanggil otomatis dari konstruktor jika weaponEquipped = true,
     * atau bisa dipanggil manual ketika player baru equip senjata.
     */
    public void loadWeaponStates() {
        if (weaponStatesLoaded) return;

        // ── State 3 → BaseClothXxx (1)-(4) ───────────────────────────────────
        for (int i = 0; i < 4; i++) {
            int f = i + 1;
            state3Up[i]    = readSafely("/Aset/BaseClothBack ("  + f + ").png");
            state3Down[i]  = readSafely("/Aset/BaseClothFront (" + f + ").png");
            state3Left[i]  = readSafely("/Aset/BaseClothLeft ("  + f + ").png");
            state3Right[i] = readSafely("/Aset/BaseClothRight (" + f + ").png");
        }

        // ── State 4 → BaseClothXxx (5)-(8) ───────────────────────────────────
        for (int i = 0; i < 4; i++) {
            int f = i + 5;
            state4Up[i]    = readSafely("/Aset/BaseClothBack ("  + f + ").png");
            state4Down[i]  = readSafely("/Aset/BaseClothFront (" + f + ").png");
            state4Left[i]  = readSafely("/Aset/BaseClothLeft ("  + f + ").png");
            state4Right[i] = readSafely("/Aset/BaseClothRight (" + f + ").png");
        }

        weaponStatesLoaded = true;
        System.out.println("[PlayerCloth] State 3 & 4 berhasil dimuat untuk: " + clothName);
    }
    
    private void loadMovementImages() {
        // Walk (6 frame per arah)
        for (int i = 0; i < 6; i++) {
            int f = i + 1;
            clothWalkFront[i] = readSafely("/Aset/ClothFrontWalk (" + f + ").png");
            clothWalkBack[i]  = readSafely("/Aset/ClothBackWalk ("  + f + ").png");
            clothWalkLeft[i]  = readSafely("/Aset/ClothLeftWalk ("  + f + ").png");
            clothWalkRight[i] = readSafely("/Aset/ClothRightWalk (" + f + ").png");
        }

        // Run (3 frame per arah)
        for (int i = 0; i < 3; i++) {
            int f = i + 1;
            clothRunFront[i] = readSafely("/Aset/ClothFrontRun (" + f + ").png");
            clothRunBack[i]  = readSafely("/Aset/ClothBackRun (" + f + ").png");
            clothRunLeft[i]  = readSafely("/Aset/ClothLeftRun (" + f + ").png");
            clothRunRight[i] = readSafely("/Aset/ClothRightRun (" + f + ").png");
        }

        // Jump (2 frame per arah, urutan {0,1,0} dihandle di player)
        for (int i = 0; i < 2; i++) {
            int f = i + 1;
            clothJumpFront[i] = readSafely("/Aset/ClothFrontJump (" + f + ").png");
            clothJumpBack[i]  = readSafely("/Aset/ClothBackJump ("  + f + ").png");
            clothJumpLeft[i]  = readSafely("/Aset/ClothLeftJump ("  + f + ").png");
            clothJumpRight[i] = readSafely("/Aset/ClothRightJump (" + f + ").png");
        }

        // Idle (1 frame per arah, tanpa nomor)
        clothIdleFront = readSafely("/Aset/ClothFrontIdle.png");
        clothIdleBack  = readSafely("/Aset/ClothBackIdle.png");
        clothIdleLeft  = readSafely("/Aset/ClothLeftIdle.png");
        clothIdleRight = readSafely("/Aset/ClothRightIdle.png");
        
        for (int i = 0; i < 3; i++) {
            int f = i + 1;
            clothDeadFront[i] = readSafely("/Aset/CharDeadFrontCloth (" + f + ").png");
            clothDeadBack[i]  = readSafely("/Aset/CharDeadBackCloth ("  + f + ").png");
            clothDeadLeft[i]  = readSafely("/Aset/CharDeadLeftCloth ("  + f + ").png");
            clothDeadRight[i] = readSafely("/Aset/CharDeadRightCloth (" + f + ").png");
        }
    }

    /**
     * Ambil frame baju yang sesuai dengan state & arah saat ini.
     *
     * @param attackType     1 / 2 / 3 / 4
     * @param frameIndex     0-3
     * @param direction      "up" / "down" / "left" / "right"
     * @return               BufferedImage frame baju, atau null jika belum dimuat
     */
    public BufferedImage getFrame(int attackType, int frameIndex, String direction) {
        BufferedImage[] arr = getArray(attackType, direction);
        if (arr == null || frameIndex < 0 || frameIndex >= arr.length) return null;
        return arr[frameIndex];
    }

    // Helper internal: pilih array yang tepat
    private BufferedImage[] getArray(int attackType, String direction) {
        switch (attackType) {
            case 1:
                switch (direction) {
                    case "up":    return state1Up;
                    case "down":  return state1Down;
                    case "left":  return state1Left;
                    case "right": return state1Right;
                }
                break;
            case 2:
                switch (direction) {
                    case "up":    return state2Up;
                    case "down":  return state2Down;
                    case "left":  return state2Left;
                    case "right": return state2Right;
                }
                break;
            case 3:
                if (!weaponStatesLoaded) return null;
                switch (direction) {
                    case "up":    return state3Up;
                    case "down":  return state3Down;
                    case "left":  return state3Left;
                    case "right": return state3Right;
                }
                break;
            case 4:
                if (!weaponStatesLoaded) return null;
                switch (direction) {
                    case "up":    return state4Up;
                    case "down":  return state4Down;
                    case "left":  return state4Left;
                    case "right": return state4Right;
                }
                break;
        }
        return null;
    }

    /** Apakah state 3 & 4 sudah tersedia? */
    public boolean hasWeaponStates() {
        return weaponStatesLoaded;
    }
    
    public BufferedImage getWalkFrame(int frameIndex, String direction) {
        BufferedImage[] arr = null;
        switch (direction) {
            case "up":    arr = clothWalkBack; break;
            case "down":  arr = clothWalkFront; break;
            case "left":  arr = clothWalkLeft; break;
            case "right": arr = clothWalkRight; break;
        }
        if (arr == null || arr.length == 0 || frameIndex < 0) return null;
        // Gunakan % arr.length agar aman jika jumlah frame baju & player berbeda
        return arr[frameIndex % arr.length];
    }

    public BufferedImage getRunFrame(int frameIndex, String direction) {
        BufferedImage[] arr = null;
        switch (direction) {
            case "up":    arr = clothRunBack; break;
            case "down":  arr = clothRunFront; break;
            case "left":  arr = clothRunLeft; break;
            case "right": arr = clothRunRight; break;
        }
        if (arr == null || arr.length == 0 || frameIndex < 0) return null;
        // Gunakan % arr.length untuk looping otomatis yang aman
        return arr[frameIndex % arr.length];
    }

    public BufferedImage getJumpFrame(int frameIndex, String direction) {
        BufferedImage[] arr = null;
        switch (direction) {
            case "up":    arr = clothJumpBack; break;
            case "down":  arr = clothJumpFront; break;
            case "left":  arr = clothJumpLeft; break;
            case "right": arr = clothJumpRight; break;
        }
        if (arr == null || arr.length == 0 || frameIndex < 0) return null;
        return arr[frameIndex % arr.length];
    }
    
    public BufferedImage getDeadFrame(int frameIndex, String direction) {
        BufferedImage[] arr = null;
        switch (direction) {
            case "up":    arr = clothDeadBack;  break;
            case "down":  arr = clothDeadFront; break;
            case "left":  arr = clothDeadLeft;  break;
            case "right": arr = clothDeadRight; break;
        }
        if (arr == null || arr.length == 0 || frameIndex < 0) return null;
        return arr[frameIndex % arr.length];
    }

    public BufferedImage getIdleFrame(String direction) {
        switch (direction) {
            case "down":  return clothIdleFront;
            case "up":    return clothIdleBack;
            case "left":  return clothIdleLeft;
            case "right": return clothIdleRight;
        }
        return clothIdleFront;
    }
}