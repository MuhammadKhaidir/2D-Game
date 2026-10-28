package Entitas;

import Main.GamePanel;
import Tamed.GolemMinion;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/**
 * SkeletonHand — tangan skeleton yang muncul dari tanah, bagian dari skill
 * Skeleton King yang trigger tiap 10 detik. Diem di tempat, mainin 9 frame
 * animasi, ngasih damage sekali di pertengahan animasi, terus ilang sendiri.
 *
 * Entity ini gak punya AI/gerak, dan default-nya gak bisa dipukul balik
 * (dianggap hazard/trap, bukan monster biasa). Kalau mau bisa dihancurkan
 * player, tinggal isi ulang method takeDamage().
 */
public class SkeletonHand extends Entity {

    // ══════════════════════════════════════════════════════
    //  STATIC IMAGE CACHE
    // ══════════════════════════════════════════════════════
    private static BufferedImage[] S_hand = new BufferedImage[9];
    private static boolean imagesLoaded = false;

    public static synchronized void preloadImages() {
        if (imagesLoaded) return;
        try {
            Class<?> cls = SkeletonHand.class;
            for (int i = 0; i < 9; i++) {
                int f = i + 1;
                S_hand[i] = ImageIO.read(cls.getResourceAsStream("/Aset/SkeletonHand (" + f + ").png"));
            }
            imagesLoaded = true;
            Main.Log.d("[SkeletonHand] Semua gambar berhasil di-preload.");
        } catch (Exception e) {
            System.out.println("[SkeletonHand] Gagal preload gambar: " + e.getMessage());
        }
    }

    private int frameIndex = 0;

    // ── Telegraph: tangan "nongol pelan-pelan dari tanah" selama WARMUP_TICKS,
    //    baru damage kena pas warmup abis. Ngasih player waktu buat kabur.
    //    NOTE: ini nentuin reaction window player, sengaja GAK ikut dipercepat
    //    (beda kategori sama animasi muncul/ngilang murni) — bilang aja kalau mau ini juga dikurangin. ──
    private final int WARMUP_TICKS = 60;   // 1 detik @ 60 FPS — jeda sebelum damage kena
    private final int HIT_FRAME = 4;       // 0..HIT_FRAME-1 = frame telegraph, HIT_FRAME = frame pas kena
    private int warmupTimer = 0;

    // Setelah damage kena, sisa frame (HIT_FRAME..8) main cepet sebagai animasi "nyekek"/retract -> ngilang
    private int postHitAnimTimer = 0;
    private final int POST_HIT_FRAME_DELAY = 3; // dipercepat dari 5 -> 3, animasi ngilang abis kena gak kelamaan

    private boolean hasHit = false;
    public boolean finished = false; // true kalau animasi udah kelar semua

    public int damage;
    private final int drawScale = 2; // ukuran gambar tangan di layar, tinggal sesuaikan

    public SkeletonHand(GamePanel gp, int x, int y, int damage) {
        super(gp);
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.direction = "down";

        // Hitbox damage-nya, disamain kira-kira sama ukuran satu tile
        int hbSize = (int) (gp.tileSize * 0.9);
        this.solidArea = new Rectangle(0, 0, hbSize, hbSize);
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
    }

    @Override
    public void update() {
        if (finished) return;

        if (!hasHit) {
            // ── Fase telegraph: 1 detik nongol dari tanah, belum ngasih damage ──
            warmupTimer++;
            int progressFrame = (int) ((double) warmupTimer / WARMUP_TICKS * HIT_FRAME);
            if (progressFrame > HIT_FRAME - 1) progressFrame = HIT_FRAME - 1;
            frameIndex = progressFrame;

            if (warmupTimer >= WARMUP_TICKS) {
                hasHit = true;
                dealDamage();
                frameIndex = HIT_FRAME;
                postHitAnimTimer = 0;
            }
        } else {
            // ── Fase setelah kena: sisa frame main cepet, terus ilang ──
            postHitAnimTimer++;
            if (postHitAnimTimer >= POST_HIT_FRAME_DELAY) {
                postHitAnimTimer = 0;
                frameIndex++;
                if (frameIndex >= 9) {
                    frameIndex = 8;
                    finished = true;
                }
            }
        }
    }

    /** Cek siapa aja yang lagi berdiri di titik tangan ini pas dia "nyekek". */
    private void dealDamage() {
        Rectangle atk = new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);

        // Kena Player
        Rectangle pBox = new Rectangle(
            gp.player.x + gp.player.solidArea.x,
            gp.player.y + gp.player.solidArea.y,
            gp.player.solidArea.width,
            gp.player.solidArea.height
        );
        if (atk.intersects(pBox)) {
            gp.player.takeDamageFrom(damage, this);
        }

        // Kena GolemMinion yang kebetulan lagi di titik itu juga
        if (gp.deathTotemManager != null && gp.deathTotemManager.getMinions() != null) {
            for (Entity e : gp.deathTotemManager.getMinions()) {
                if (e instanceof GolemMinion && e.alive) {
                    Rectangle mBox = new Rectangle(e.x + e.solidArea.x, e.y + e.solidArea.y, e.solidArea.width, e.solidArea.height);
                    if (atk.intersects(mBox)) {
                        ((GolemMinion) e).takeDamage(damage);
                    }
                }
            }
        }
    }

    @Override
    public void takeDamage(int dmg) {
        // Hazard, gak bisa dipukul balik. Ubah di sini kalau mau tangan ini destructible.
    }

    @Override
    public void draw(Graphics2D g2) {
        if (finished || frameIndex >= 9) return;
        BufferedImage img = S_hand[frameIndex];
        if (img == null) return;

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        int drawSize = gp.tileSize * drawScale;
        int sx = screenX - drawSize / 2 + gp.tileSize / 2;
        int sy = screenY - drawSize / 2 + gp.tileSize / 2;

        if (sx + drawSize < gp.viewMinX() || sx > gp.viewMaxX() || sy + drawSize < gp.viewMinY() || sy > gp.viewMaxY()) return;

        g2.drawImage(img, sx, sy, drawSize, drawSize, null);
    }
}