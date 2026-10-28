package bahan;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Collision air yang dihitung OTOMATIS dari tile peta (map01.txt).
 *
 * Sebelumnya air dijaga rectangle manual (gp.objCol.add(...)) yang ukurannya ditebak, jadi ada yang
 * lebarnya 5000px dan malah jadi tembok gaib di daratan. Sekarang bentuk air tinggal diubah di
 * map01.txt dan collision-nya ikut sendiri.
 *
 * Cara kerja:
 *  1. Tiap tile dibagi 6x6 sel kecil (48px -> 8px).
 *  2. Tile air penuh memblokir semua sel. Tile tepi pantai cuma memblokir bagian yang BERAIR,
 *     bagian daratannya (bibir sungai) tetap bisa diinjak. Bentuknya dibaca dari gambar tile-nya.
 *  3. Area "walkable" (dek jembatan) dibuang dari hasil.
 *  4. Sel yang masih terblokir digabung jadi sedikit rectangle besar (greedy), supaya
 *     daftar gp.objCol.objects tidak membengkak.
 */
public final class WaterCollision {

    private WaterCollision() {}

    /** tiap tile dibagi SUB x SUB sel kecil */
    public static final int SUB = 6;

    /** Tile tepi pantai versi salju (140-151) berbentuk sama dengan tile aslinya; kembalikan tile asli itu. */
    public static int baseOf(int t) {
        int i = t - WorldGen.SNOW_SHORE_FIRST;
        return (i >= 0 && i < WorldGen.SHORE_BASE.length) ? WorldGen.SHORE_BASE[i] : t;
    }

    /** true kalau tile index ini bagian dari air (termasuk tile tepi pantai) */
    public static boolean isWaterTile(int t) {
        switch (baseOf(t)) {
            case 1: case 2:                                   // air penuh
            case 14: case 15: case 17: case 18: case 19:      // tepi pantai
            case 21: case 57: case 59:
            case 16: case 20: case 22: case 23:               // sudut (air hampir penuh)
            case 24: case 28: case 29:
                return true;
            default:
                return false;
        }
    }

    /**
     * true kalau titik (fx, fy) di dalam tile t memang berair. fx/fy = posisi di dalam tile, 0.0 - 1.0.
     * Beda sama {@link #isWaterTile}: tile tepi pantai cuma sebagian yang berair, jadi titik di bagian
     * daratannya dianggap BUKAN air. Dipakai buat mancing (Item.FishingManager).
     */
    public static boolean isWaterAt(int t, float fx, float fy) {
        if (!isWaterTile(t)) return false;
        int[] p = waterPart(t);
        int sx = Math.min(SUB - 1, Math.max(0, (int) (fx * SUB)));
        int sy = Math.min(SUB - 1, Math.max(0, (int) (fy * SUB)));
        return sx >= p[0] && sx < p[1] && sy >= p[2] && sy < p[3];
    }

    /**
     * Bagian tile yang berair: {xMulai, xAkhir, yMulai, yAkhir} dalam satuan 1/6 tile (akhir eksklusif).
     * Angka diambil dari gambar tile (bagian biru vs bagian daratan).
     */
    static int[] waterPart(int t) {
        switch (baseOf(t)) {
            case 14: return new int[] {0, 4, 0, 6};   // daratan di timur
            case 21: return new int[] {2, 6, 0, 6};   // daratan di barat
            case 18: return new int[] {0, 6, 3, 6};   // daratan di utara
            case 17: return new int[] {0, 6, 0, 4};   // daratan di selatan
            case 15: return new int[] {0, 4, 3, 6};   // daratan di utara + timur
            case 19: return new int[] {2, 6, 3, 6};   // daratan di utara + barat
            case 57: return new int[] {0, 4, 0, 4};   // daratan di selatan + timur
            case 59: return new int[] {2, 6, 0, 4};   // daratan di selatan + barat
            default: return new int[] {0, 6, 0, 6};   // air penuh dan tile sudut
        }
    }

    /**
     * @param map      mapTileNum, diindeks [kolom][baris]
     * @param cols     jumlah kolom peta yang terisi
     * @param rows     jumlah baris peta yang terisi
     * @param tileSize ukuran tile dalam pixel dunia (48)
     * @param walkable area yang boleh dilewati walau ada tile air di bawahnya (dek jembatan), boleh null
     * @return rectangle collision dalam koordinat dunia
     */
    public static List<Rectangle> build(int[][] map, int cols, int rows, int tileSize, List<Rectangle> walkable) {
        int sub = Math.max(1, tileSize / SUB);
        int gw = cols * SUB;
        int gh = rows * SUB;
        boolean[][] blocked = new boolean[gh][gw];   // [y][x]

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                int t = map[c][r];
                if (!isWaterTile(t)) continue;
                int[] p = waterPart(t);
                for (int sy = p[2]; sy < p[3]; sy++) {
                    for (int sx = p[0]; sx < p[1]; sx++) {
                        blocked[r * SUB + sy][c * SUB + sx] = true;
                    }
                }
            }
        }

        if (walkable != null) {
            for (Rectangle w : walkable) {
                int x0 = Math.max(0, Math.floorDiv(w.x, sub));
                int x1 = Math.min(gw, -Math.floorDiv(-(w.x + w.width), sub));
                int y0 = Math.max(0, Math.floorDiv(w.y, sub));
                int y1 = Math.min(gh, -Math.floorDiv(-(w.y + w.height), sub));
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) blocked[y][x] = false;
                }
            }
        }

        // gabung jadi rectangle besar: ambil run horizontal, lalu lebarkan ke bawah selama masih penuh
        List<Rectangle> out = new ArrayList<>();
        for (int y = 0; y < gh; y++) {
            int x = 0;
            while (x < gw) {
                if (!blocked[y][x]) { x++; continue; }
                int x1 = x;
                while (x1 < gw && blocked[y][x1]) x1++;
                int y1 = y + 1;
                grow:
                while (y1 < gh) {
                    for (int xx = x; xx < x1; xx++) {
                        if (!blocked[y1][xx]) break grow;
                    }
                    y1++;
                }
                for (int yy = y; yy < y1; yy++) {
                    for (int xx = x; xx < x1; xx++) blocked[yy][xx] = false;
                }
                out.add(new Rectangle(x * sub, y * sub, (x1 - x) * sub, (y1 - y) * sub));
                x = x1;
            }
        }
        return out;
    }
}
