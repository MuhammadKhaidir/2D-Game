package World;

import Main.GamePanel;
import bahan.TileM;

/**
 * WitchHouse — Rumah Penyihir.
 *
 * - Interior pakai GIF animasi: JungleHouseInside_animated.gif
 * - Keluarnya BUKAN lewat ESC, tapi otomatis begitu player jalan
 *   ke exitZone (biasanya ditaruh persis di depan pintu keluar).
 *
 * Sprite eksterior (JungleHouse.png) ditaruh di map lewat sistem
 * object/tile yang udah kamu punya (TileM dkk) — HouseData di sini
 * cuma ngatur doorArea (buat trigger masuk) dan interior-nya aja.
 * Jadi worldX/worldY/width/height di bawah HARUS disamain sama
 * posisi & ukuran sprite JungleHouse.png yang kamu taruh di map.
 */
public class WitchHouse {

    public static HouseData create(GamePanel gp) {
        int ts = gp.tileSize;  // ni jumlahnnya 48

        HouseData witchHouse = new HouseData(
                "Rumah Penyihir",
                // TODO: samain sama posisi & ukuran sprite JungleHouse.png di map
                4000, 3850, 220, 180,     // worldX, worldY, width, height
                445, 600            // spawnX, spawnY (posisi player pas masuk, koordinat lokal interior)
        )
        		
            // TODO: sesuaikan cols/rows biar (cols*ts) x (rows*ts) match
            // sama ukuran gambar JungleHouseInside_animated.gif
            .setFloor(20, 17, 21)
            .setExit(4140, 4000)          // posisi world player muncul pas keluar rumah
            .setGifBackground("/Aset/JungleHouseInside_animated.gif")
            .disableAutoWalls()          // dinding auto dimatiin, gambar dinding udah ada di gif

            // TODO: sesuaikan collision manual biar pas sama tembok yang keliatan di gif
            //.addCollision(0, 0, ts * 11, ts)          // tembok atas
            //.addCollision(0, ts * 7, ts * 11, ts)     // tembok bawah
            //.addCollision(0, 0, ts, ts * 8)           // tembok kiri
            //.addCollision(ts * 10, 0, ts, ts * 8)     // tembok kanan

            // Exit zone: area kecil di depan pintu keluar (dekat tembok bawah).
            // Begitu player nginjek area ini, otomatis keluar — gak perlu ESC/E.
            // NOTE: pastikan ini gak nempel/overlap sama spawnX,spawnY di atas,
            // biar gak langsung ke-trigger begitu baru masuk rumah.
            .setExitZone(440, 680, ts, 20)
                      // ESC dimatiin khusus rumah ini
            .disableEscExit();
        
		return witchHouse; 

        
    }
}