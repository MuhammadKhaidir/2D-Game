package Object;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.image.BufferedImage;
import Item.Item;
import Item.ItemManager;
import Main.GamePanel;

public class breakable {
    public int worldX, worldY;
    public int width, height;
    public int hp;
    public BufferedImage image;
    public Item dropItem;
    public Rectangle solidArea;
    public boolean collisionOn = true;
    public boolean destroyed = false;
    public int dropAmount = 1;
    public boolean hasShadow = true; // true = ada bayangan (default), false = gak ada bayangan
    
    // 🔥 KECEPATAN ANIMASI (1 FPS = 60 Ticks pada Game Loop 60 FPS)
    private int idleCounter = 0;
    private int idleSpriteNum = 0;
    private final int IDLE_ANIMATION_SPEED = 60; // 1 FPS untuk Idle
    private final int HIT_ANIMATION_SPEED = 3;  // 2 FPS untuk Hit (bisa diubah ke 60 jika ingin benar-benar 1 FPS)

    // ===== RESPAWN =====
    private int maxHp;                          // simpan HP awal
    private int respawnTimerMax = 60 * 30;      // 30 detik (60 fps × 30)
    private int respawnTimer = 0;
    private boolean waitingRespawn = false;

    // Simpan posisi & ukuran awal untuk reset collision
    private int initWorldX, initWorldY, initWidth, initHeight;
    
    private GamePanel gp;
    private Rectangle registeredCollision;

    // Hit flash
    public boolean isHit = false;
    public int hitFlashTimer = 0;

    // ===== ANIMASI SEBELUM TUMBANG =====
    private BufferedImage[] idleFrames;
    private BufferedImage[] hitFrames; 
    public boolean isPlayingHitAnim = false;
    private int hitAnimTimer = 0;
    private int currentHitFrameIndex = 0;

    // ===== ANIMASI POHON HANCUR/TUMBANG =====
    public boolean isFalling = false; 
    private BufferedImage[] downFrames;
    private int downAnimTimer = 0;
    private int currentDownFrameIndex = 0;
    private float fallAlpha = 1.0f;
    private boolean fadingOut = false;

    public breakable(int worldX, int worldY, int width, int height,
                     BufferedImage image, int hp, Item dropItem, GamePanel gp) {
        this.worldX     = worldX;
        this.worldY     = worldY;
        this.width      = width;
        this.height     = height;
        this.image      = image;
        this.hp         = hp;
        this.dropItem   = dropItem;
        this.gp         = gp;
        this.maxHp      = hp;
        this.initWorldX = worldX;
        this.initWorldY = worldY;
        this.initWidth  = width;
        this.initHeight = height;

        solidArea = new Rectangle(worldX, worldY, width, height);

        double cx = worldX + (double) width  / 3.0;
        double cy = worldY + (double) height / 1.6;
        double cw = (double) width  / 3.5;
        double ch = (double) height / 6.5;

        registeredCollision = new Rectangle((int) cx, (int) cy, (int) cw, (int) ch);
        gp.objCol.objects.add(registeredCollision);
    }

    public void setIdleFrames(BufferedImage[] frames) { this.idleFrames = frames; }
    public void setHitFrames(BufferedImage[] frames) { this.hitFrames = frames; }
    public void setDownFrames(BufferedImage[] frames) { this.downFrames = frames; }
    public void setHasShadow(boolean hasShadow) { this.hasShadow = hasShadow; }

    private void dropToInventory() {
        ItemManager.get().add(dropItem, dropAmount);
        if (gp != null && gp.speechBubble != null) {
            String itemName = (dropItem != null && dropItem.name != null) ? dropItem.name : "item";
            gp.speechBubble.show("Obtained " + dropAmount + "x " + itemName);
        }
    }

    public void update() {
        // Logika timer flash bawaan
        if (isHit) {
            hitFlashTimer--;
            if (hitFlashTimer <= 0) isHit = false;
        }

        // ── 1. LOGIKA ANIMASI HIT (Hanya jalan jika dipukul & tidak sedang tumbang) ──
        if (isPlayingHitAnim && hitFrames != null && !isFalling) {
            hitAnimTimer++;
            if (hitAnimTimer >= HIT_ANIMATION_SPEED) { 
                hitAnimTimer = 0;
                currentHitFrameIndex++;
                
                if (currentHitFrameIndex >= hitFrames.length) {
                    isPlayingHitAnim = false; // Selesai dipukul, bersiap kembali ke idle
                    currentHitFrameIndex = 0;
                }
            }
        }

        // ── 2. LOGIKA ANIMASI DOWN/TUMBANG ──
        // FIX: sebelumnya blok ini cuma jalan kalau downFrames != null, jadi objek
        // yang gak dikasih downFrames (mis. batu) nyangkut isFalling=true selamanya
        // dan gak pernah jadi destroyed=true -> gambar gak ilang, gak respawn.
        if (isFalling) {
            if (downFrames != null && downFrames.length > 0) {
                if (!fadingOut) {
                    downAnimTimer++;
                    if (downAnimTimer >= 10) { // Kecepatan ambruknya pohon
                        downAnimTimer = 0;
                        currentDownFrameIndex++;

                        if (currentDownFrameIndex >= downFrames.length) {
                            currentDownFrameIndex = downFrames.length - 1;
                            fadingOut = true;
                        }
                    }
                } else {
                    fallAlpha -= 0.025f;
                    if (fallAlpha <= 0f) {
                        fallAlpha = 0f;
                        isFalling = false;
                        destroyed = true; 
                    }
                }
            } else {
                // Objek ini gak punya animasi tumbang (mis. batu) -> langsung
                // anggap proses "jatuh" selesai supaya konsisten sama sistem
                // destroyed/respawn di bawah.
                isFalling = false;
                destroyed = true;
            }
        }

        // ── 3. LOGIKA RESPOND TIMER ──
        if (destroyed && !isFalling) {
            if (!waitingRespawn) {
                waitingRespawn = true;
                respawnTimer   = respawnTimerMax;
            }
            respawnTimer--;
            if (respawnTimer <= 0) {
                respawn();
            }
        }

        // ── 4. LOGIKA ANIMASI IDLE (🔥 KUNCI: Mati total saat dipukul/tumbang) ──
        if (!destroyed && !isFalling && !isPlayingHitAnim && idleFrames != null) { 
            idleCounter++;
            if (idleCounter >= IDLE_ANIMATION_SPEED) {
                idleSpriteNum = (idleSpriteNum + 1) % idleFrames.length;
                idleCounter = 0;
            }
        }
    }

    public void takeDamage(int dmg, GamePanel gp) {
        if (destroyed || isFalling) return;

        hp -= dmg;
        isHit = true;
        hitFlashTimer = 8;

        // 🔥 Reset hit animation ke frame awal setiap kali menerima hit baru dari player
        if (hitFrames != null) {
            isPlayingHitAnim = true;
            hitAnimTimer = 0;
            currentHitFrameIndex = 0;
        }

        if (hp <= 0) {
            hp          = 0;
            isFalling   = true;
            isPlayingHitAnim = false; 
            collisionOn = false;
            gp.objCol.objects.remove(registeredCollision);
            solidArea = new Rectangle(0, 0, 0, 0);
            dropToInventory();
        }
    }

    public void draw(Graphics2D g2, int screenX, int screenY) {
        if (destroyed && !isFalling) return;

        // ── KONDISI 1: ANIMASI DOWN (TUMBANG) ──
        if (isFalling && downFrames != null && currentDownFrameIndex < downFrames.length && downFrames[currentDownFrameIndex] != null) {
            Composite oldComposite = g2.getComposite();
            if (fadingOut) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fallAlpha));
            }
            g2.drawImage(downFrames[currentDownFrameIndex], screenX, screenY, width, height, null);
            g2.setComposite(oldComposite);
            return; // Keluar agar tidak menggambar layer lain
        }

        // ── KONDISI 2: ANIMASI HIT (SEDANG DIPUKUL) ──
        if (isPlayingHitAnim && hitFrames != null && currentHitFrameIndex < hitFrames.length && hitFrames[currentHitFrameIndex] != null) {
            g2.drawImage(hitFrames[currentHitFrameIndex], screenX, screenY, width, height, null);
            return; // Keluar agar frame Idle tidak menimpa state Hit
        }

        // ── KONDISI 3: ANIMASI IDLE ATAU GAMBAR DEFAULT (NORMAL) ──
        if (!destroyed) {
            if (idleFrames != null && idleSpriteNum < idleFrames.length && idleFrames[idleSpriteNum] != null) {
                g2.drawImage(idleFrames[idleSpriteNum], screenX, screenY, width, height, null);
            } else {
                g2.drawImage(image, screenX, screenY, width, height, null); 
            }
        }
    }

    private void respawn() {
        hp                   = maxHp;
        destroyed            = false;
        waitingRespawn       = false;
        isFalling            = false;
        fadingOut            = false;
        fallAlpha            = 1.0f;
        isHit                = false;
        hitFlashTimer        = 0;
        isPlayingHitAnim     = false;
        currentHitFrameIndex = 0;
        hitAnimTimer         = 0;
        currentDownFrameIndex= 0;
        downAnimTimer        = 0;
        collisionOn          = true;
        idleCounter          = 0;
        idleSpriteNum        = 0;

        solidArea = new Rectangle(initWorldX, initWorldY, initWidth, initHeight);

        double cx = initWorldX + (double) initWidth  / 3.0;
        double cy = initWorldY + (double) initHeight / 1.6;
        double cw = (double) initWidth  / 3.5;
        double ch = (double) initHeight / 6.5;

        registeredCollision = new Rectangle((int) cx, (int) cy, (int) cw, (int) ch);
        gp.objCol.objects.add(registeredCollision);
    }
}