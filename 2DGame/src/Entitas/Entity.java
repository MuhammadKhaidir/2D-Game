package Entitas;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import Entitas.DialogNode;

import Main.GamePanel;

public class Entity {

	// RPG STATS
	public int level = 1;
	public int exp = 0;
	public int nextLevelExp = 50;
	public int attributePoints = 0;
	public int getExpValue = 0;
	public int coinDrop = 1; // jumlah koin yang pasti didapat player tiap monster ini mati

	public int strength = 1;
	public int dexterity = 1;
	
	// HP
	public int maxLife = 1000;
	public int life = maxLife;

	// Marah / Aggro
	public boolean isAngry = false;
	public int angryTimer = 15;
	public final int ANGRY_LIMIT = 600;

	// Regen HP
	public int regenTimer = 0;
	public final int REGEN_DELAY = 180;
	public final int REGEN_RATE = 10;

	// Knockback
	public boolean isKnockback = false;
	public int knockbackCounter = 0;
	public int knockbackDirectionX = 0;
	public int knockbackDirectionY = 0;
	public int knockbackSpeed = 3;

	// Mati & respawn
	public int respawnTimer = 10;
	public final int RESPAWN_TIME = 600;
	public int spawnX, spawnY;

	// Damage feedback
	public boolean isHit = false;
	public int hitFlashTimer = 0;
	
	public int attackRange = 40;
	public int attackCooldown = 130;
	
	public int x;
	public int y;
	public static int xW;
	public static int yW;
	public double speed;
	public boolean isTalking = false;
	public int talkCooldown = 0;
	
	// FOLLOW DETECTION
	public int lastTargetX;
	public int lastTargetY;

	public boolean isDead = false;

	
	// Stuck Chat NPC
	public int stuckCounter = 0;
	public String chatText = "";
	public int chatTimer = 0;
	public boolean blockedByPlayer = false;
	
	// follow patience
	public int followIdleTimer = 0;
	public boolean alreadyComplained = false;

	public String name = "";
	public boolean showName = true;

	public DialogNode dialogRoot;
	public DialogNode currentDialog;
	public String dialogId;

	// NPC Command State
	public enum NPCState {
	    IDLE,
	    FOLLOW,
	    ATTACK,
	    STAY
	}

	public NPCState npcState = NPCState.IDLE;
	public Entity followTarget = null;

	public class DialogOption {
	    public String text;
	    public DialogNode next;

	    public DialogOption(String text, DialogNode next) {
	        this.text = text;
	        this.next = next;
	    }
	}

	public int animIndex = 0;
	public boolean wasMoving = false;
	public BufferedImage stand, front1, front2, back1, back2, standback,
	    right1, right2, standright, left1, left2, standleft, hi,
	    jumpUpPrep, jumpUpAir, jumpDownPrep, jumpDownAir,
	    jumpLeftPrep, jumpLeftAir, jumpRightPrep, jumpRightAir,
	    KaelFront1, KaelFront2, stand2, standback2, standright2, standleft2,
	    punchUp, punchUp2, punchDown, punchDown2, punchLeft, punchLeft2,
	    punchRight, punchRight2;

    public BufferedImage
        mfront1, mfront2,
        mback1, mback2,
        mleft1, mleft2,
        mright1, mright2,
        mstand, mstandback, mstandleft, mstandright;

	public String direction;
	public String emote;
	public int actionLockCounter = 0;

	public int imgCount = 0;
	public int imgNum = 1;
	
	public Rectangle solidArea = new Rectangle(0, 0, 45, 45);
	public int solidAreaDefaultX, solidAreaDefaultY;
	public boolean collisionOn = false;
	public GamePanel gp;

	// ===== A* PATHFINDING =====
	private AStar aStar;

	// Path yang sedang diikuti
	private List<int[]> currentPath = null;
	private int pathIndex = 0;

	// Seberapa sering NPC minta path baru (frame)
	// Jangan terlalu sering → mahal di CPU
	private static final int PATH_REFRESH_RATE = 45; // ~0.75 detik di 60 FPS
	private int pathRefreshTimer = 0; public boolean alive;

	// Jarak (piksel) untuk dianggap "sampai" di satu node path
	private static final int NODE_REACH_DIST = 12;

	// Jarak langsung ke target: kalau sudah cukup dekat, pakai direct chase
	// bukan A* (lebih responsif, lebih murah)
	private static final int DIRECT_CHASE_DIST = 80;
	
	public Entity(GamePanel gp) {
		this.gp = gp;
		this.aStar = new AStar(gp);
	}

	// Legacy compat
	public void takeDamage(int dmg) {
	    takeDamage(dmg, gp.player);
	}
	
	public void applyKnockback(String attackerDirection) {
	    isKnockback = true;
	    knockbackCounter = 0;

	    switch (attackerDirection) {
	        case "up":    knockbackDirectionY = -1; knockbackDirectionX =  0; break;
	        case "down":  knockbackDirectionY =  1; knockbackDirectionX =  0; break;
	        case "left":  knockbackDirectionX = -1; knockbackDirectionY =  0; break;
	        case "right": knockbackDirectionX =  1; knockbackDirectionY =  0; break;
	    }
	}

	public void takeDamage(int dmg, player attacker) {
	    if (isDead) return;

	    life -= dmg;

	    if (attacker != null) {
	        isAngry = true;
	        angryTimer = 0;
	        regenTimer = 0;

	        if (!(this instanceof monster)) {
	            npcState = NPCState.IDLE;
	            followTarget = attacker;
	            npcState = NPCState.FOLLOW;
	            facePlayer(attacker);
	        }
	    }

	    isHit = true;
	    hitFlashTimer = 10;

	    // Reset path supaya langsung minta path baru menuju attacker
	    currentPath = null;
	    pathRefreshTimer = PATH_REFRESH_RATE; // paksa refresh segera

	    if (life <= 0) {
	        life = 0;
	        isDead = true;
	        onDeath();
	    }
	}

	public void onDeath() {
	    // default: diam
	}

	public void setAction() {
	    // override di subclass
	}

	public void facePlayer(player p) {
	    int dx = p.x - this.x;
	    int dy = p.y - this.y;

	    if (Math.abs(dx) > Math.abs(dy)) {
	        direction = (dx > 0) ? "right" : "left";
	    } else {
	        direction = (dy > 0) ? "down" : "up";
	    }
	}

	public void respawn() {
	    isDead = false;
	    life = maxLife;
	    x = 700;
	    y = 1000;
	    collisionOn = true;
	    currentPath = null;
	    pathRefreshTimer = 0;
	}
	
	public void onAttackHit() {
	    if (followTarget instanceof player) {
	        followTarget.takeDamage(10);
	    }
	}

	// ===== PATHFINDING HELPER =====

	/**
	 * Minta path baru dari posisi NPC sekarang ke target.
	 * Dipanggil periodik, bukan tiap frame.
	 */
	private void requestNewPath(int targetX, int targetY) {
	    currentPath  = aStar.findPath(x, y, targetX, targetY);
	    pathIndex     = 0;
	    pathRefreshTimer = 0;
	}

	/**
	 * Ambil arah gerak NPC menuju node path berikutnya.
	 * Return false kalau path habis / kosong.
	 */
	private boolean followPath() {
	    if (currentPath == null || pathIndex >= currentPath.size()) return false;

	    int[] node = currentPath.get(pathIndex);
	    int nx = node[0];
	    int ny = node[1];

	    int dx = nx - x;
	    int dy = ny - y;
	    double dist = Math.sqrt(dx * dx + dy * dy);

	    // Sudah dekat ke node ini → maju ke node berikutnya
	    if (dist <= NODE_REACH_DIST) {
	        pathIndex++;
	        if (pathIndex >= currentPath.size()) return false;
	        node = currentPath.get(pathIndex);
	        nx   = node[0];
	        ny   = node[1];
	        dx   = nx - x;
	        dy   = ny - y;
	    }

	    // Tentukan arah berdasarkan dx/dy terbesar
	    if (Math.abs(dx) > Math.abs(dy)) {
	        direction = (dx > 0) ? "right" : "left";
	    } else {
	        direction = (dy > 0) ? "down" : "up";
	    }

	    return true;
	}

	/**
	 * Cek apakah garis lurus ke target bebas dari collision.
	 * Kalau iya, NPC bisa langsung chase tanpa A*.
	 */
	private boolean hasDirectLine(int targetX, int targetY) {
	    int steps = 8;
	    for (int i = 1; i <= steps; i++) {
	        int cx = x + (targetX - x) * i / steps;
	        int cy = y + (targetY - y) * i / steps;

	        // Cek objek collision
	        Rectangle probe = new Rectangle(cx + solidArea.x, cy + solidArea.y,
	                                        solidArea.width, solidArea.height);
	        for (Rectangle r : gp.objCol.objects) {
	            if (r.intersects(probe)) return false;
	        }

	        // Cek tile collision
	        int col = cx / gp.tileSize;
	        int row = cy / gp.tileSize;
	        if (col >= 0 && row >= 0 && col < gp.maxWorldCol && row < gp.maxWorldRow) {
	            int tileNum = gp.tileM.mapTileNum[col][row];
	            if (gp.tileM.tile[tileNum] != null && gp.tileM.tile[tileNum].collision) {
	                return false;
	            }
	        }
	    }
	    return true;
	}

	public void update() {

		if (isKnockback) {
		    int dx = knockbackDirectionX * 6;
		    int dy = knockbackDirectionY * 6;
		    collisionOn = false;
		    gp.colision.checkTile(this);
		    gp.colision.checkObject(this, dx, dy);
		    gp.colision.checkEntity(followTarget, followTarget, dx, dy);

		    if (!collisionOn) { x += dx; y += dy; }
		    else isKnockback = false;

		    knockbackCounter++;
		    if (knockbackCounter >= 10) { isKnockback = false; knockbackCounter = 0; }
		    return;
		}

		if (npcState == NPCState.ATTACK) {
		    attackCooldown++;
		    if (attackCooldown == 10) onAttackHit();
		    if (attackCooldown >= 20) { attackCooldown = 0; npcState = NPCState.FOLLOW; }
		    return;
		}

		if (isDead) {
		    respawnTimer++;
		    if (respawnTimer >= RESPAWN_TIME) respawn();
		    return;
		}

		if (isAngry && followTarget instanceof player) {
		    player p = (player) followTarget;
		    int distX = Math.abs(p.x - x);
		    int distY = Math.abs(p.y - y);
		    if (distX <= attackRange && distY <= attackRange) {
		        npcState = NPCState.ATTACK;
		    } else {
		        npcState = NPCState.FOLLOW;
		    }
		}

		if (!isAngry && life < maxLife) {
		    regenTimer++;
		    if (regenTimer >= REGEN_DELAY) {
		        life += REGEN_RATE;
		        if (life > maxLife) life = maxLife;
		    }
		}

		if (isHit) {
		    hitFlashTimer--;
		    if (hitFlashTimer <= 0) isHit = false;
		}

        if (isAngry) {
            angryTimer++;
            if (angryTimer >= ANGRY_LIMIT) {
                isAngry = false;
                angryTimer = 0;
                followTarget = null;
                npcState = NPCState.IDLE;
                currentPath = null; // buang path saat berhenti aggro
            }
        }

		if (npcState == NPCState.STAY) {
		    wasMoving = false;
		    imgCount++;
		    if (imgCount >= 20) { imgNum = (imgNum == 4) ? 3 : 4; imgCount = 0; }
		    return;
		}

		if (chatTimer > 0) {
		    chatTimer--;
		    if (chatTimer == 0) chatText = "";
		}

		if (isTalking) {
		    imgNum = 3;
		    imgCount = 0;
		    animIndex = 0;
		    return;
		}

		if (talkCooldown > 0) {
		    talkCooldown--;
		    imgNum = 3;
		    return;
		}

		// ===== FOLLOW DENGAN A* =====
		if (!isKnockback && npcState == NPCState.FOLLOW && followTarget != null) {

		    int targetX = followTarget.x;
		    int targetY = followTarget.y;

		    int distX = Math.abs(targetX - x);
		    int distY = Math.abs(targetY - y);
		    double dist = Math.sqrt(distX * distX + distY * distY);

		    if (distX > 10 || distY > 10) {

		        // ── MODE 1: Direct Chase ──────────────────────────────────────
		        // Kalau dekat atau ada garis lurus bebas → langsung kejar
		        if (dist <= DIRECT_CHASE_DIST || hasDirectLine(targetX, targetY)) {

		            // Buang path A* yang ada supaya tidak konflik
		            currentPath = null;

		            if (Math.abs(distX) > Math.abs(distY)) {
		                direction = (targetX > x) ? "right" : "left";
		            } else {
		                direction = (targetY > y) ? "down" : "up";
		            }

		        } else {
		            // ── MODE 2: A* Pathfinding ────────────────────────────────
		            // Refresh path secara periodik
		            pathRefreshTimer++;
		            if (pathRefreshTimer >= PATH_REFRESH_RATE || currentPath == null) {
		                requestNewPath(targetX, targetY);
		            }

		            // Ikuti path
		            boolean pathOk = followPath();

		            // Path habis / gagal → fallback ke direct
		            if (!pathOk) {
		                currentPath = null;
		                if (Math.abs(distX) > Math.abs(distY)) {
		                    direction = (targetX > x) ? "right" : "left";
		                } else {
		                    direction = (targetY > y) ? "down" : "up";
		                }
		            }
		        }

		    } else {
		        // Sudah sangat dekat → diam, hadap target
		        facePlayer((player) followTarget);
		        imgNum = 3;

		        // Follow patience check (sama seperti aslinya)
		        player p = (player) followTarget;
		        boolean playerMoved = (p.x != lastTargetX) || (p.y != lastTargetY);
		        if (!playerMoved || blockedByPlayer) {
		            followIdleTimer++;
		            if (followIdleTimer >= 600 && !alreadyComplained) {
		                chatText = "You just Trollin me!?";
		                chatTimer = 120;
		                alreadyComplained = true;
		                npcState = NPCState.IDLE;
		                followTarget = null;
		                currentPath = null;
		            }
		        } else {
		            followIdleTimer = 0;
		            alreadyComplained = false;
		            lastTargetX = p.x;
		            lastTargetY = p.y;
		        }
		        return;
		    }
		}

		// ===== GERAK NORMAL (IDLE / NPC biasa) =====
		if (npcState == NPCState.IDLE) {
		    setAction();
		}

		int dx = 0, dy = 0;
		boolean moving = false;

		switch (direction) {
		    case "up":    dy -= (int) speed; moving = true; break;
		    case "down":  dy += (int) speed; moving = true; break;
		    case "left":  dx -= (int) speed; moving = true; break;
		    case "right": dx += (int) speed; moving = true; break;
		}

		if (collisionOn) moving = false;

		collisionOn = false;
		blockedByPlayer = false;

		gp.colision.checkTile(this);
		gp.colision.checkObject(this, dx, dy);

		if (gp.colision.checkEntity(this, gp.player, dx, dy)) {
		    blockedByPlayer = true;
		}

		for(int i = 0; i < gp.npc.length; i++) {
		    // TAMBAHKAN pengaman `&& gp.npc[i] != this` di bawah ini
		    if(gp.npc[i] != null && gp.npc[i] != this) { 
		        gp.colision.checkEntity(this, gp.npc[i], dx, dy);
		    }
		}

		if (!collisionOn) {
		    x += dx;
		    y += dy;
		    stuckCounter = 0;
		} else {
		    // NPC stuck → kalau pakai A*, minta path baru segera
		    if (npcState == NPCState.FOLLOW && followTarget != null) {
		        pathRefreshTimer = PATH_REFRESH_RATE; // paksa refresh path di frame berikutnya
		    }

		    if ((dx != 0 || dy != 0) && blockedByPlayer) {
		        stuckCounter++;
		        if (stuckCounter > 40 && chatText.isEmpty()) {
		            String[] complain = {"need something??", "what?", "move away", "????"};
		            chatText = complain[new Random().nextInt(complain.length)];
		            chatTimer = 80;
		        }
		        if (stuckCounter > 80) { randomTurn(); stuckCounter = 0; }
		    }
		}

		if (collisionOn) {
		    imgCount++;
		    if (imgCount > 40) {
		        imgNum = (imgNum == 4) ? 3 : 4;
		        imgCount = 0;
		    }
		    wasMoving = false;
		}

		// ===== ANIMASI =====
		int[] animOrder = {1, 3, 2, 3};

		if (moving) {
		    if (!wasMoving) { imgNum = 1; imgCount = 0; animIndex = 0; }
		    imgCount++;
		    if (imgCount >= 10) {
		        animIndex = (animIndex + 1) % animOrder.length;
		        imgNum = animOrder[animIndex];
		        imgCount = 0;
		    }
		} else {
		    imgCount++;
		    if (imgCount > 40) { imgNum = (imgNum == 4) ? 3 : 4; imgCount = 0; }
		}

		wasMoving = moving;
	}

	public Rectangle getInteractArea() {
	    return new Rectangle(x - 1, y - 1, gp.tileSize + 1, gp.tileSize + 1);
	}
	public int getSortY() {
	    return this.y + solidArea.y + solidArea.height;
	}

	public void randomTurn() {
	    int r = new Random().nextInt(4);
	    switch (r) {
	        case 0: direction = "up";    break;
	        case 1: direction = "down";  break;
	        case 2: direction = "left";  break;
	        case 3: direction = "right"; break;
	    }
	}

	public void drawShadow(Graphics2D g2) {
	    int screenX = x - gp.player.x + gp.player.screenX;
	    int screenY = y - gp.player.y + gp.player.screenY;

	    int shadowWidth  = gp.tileSize - 20;
	    int shadowHeight = gp.tileSize / 4;
	    int shadowX = screenX + (gp.tileSize - shadowWidth) / 2;
	    int shadowY = screenY + gp.tileSize - shadowHeight / 2;

	    g2.setColor(new Color(0, 0, 0, 100));
	    g2.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
	}

	public void drawOverhead(Graphics2D g2) {
	    int screenX = x - gp.player.x + gp.player.screenX;
	    int screenY = y - gp.player.y + gp.player.screenY;

	    if (!chatText.isEmpty()) {
	        int bubbleWidth  = chatText.length() * 7 + 20;
	        int bubbleHeight = 22;
	        int bx = screenX + gp.tileSize / 2 - bubbleWidth / 2;
	        int by = screenY - 48;

	        g2.setColor(new Color(0, 0, 0, 180));
	        g2.fillRoundRect(bx, by, bubbleWidth, bubbleHeight, 10, 10);
	        g2.setColor(Color.WHITE);
	        g2.drawRoundRect(bx, by, bubbleWidth, bubbleHeight, 10, 10);
	        g2.drawString(chatText, bx + 10, by + 15);
	    }

	    if (name != null && !name.isEmpty()) {
	        g2.setFont(new Font("Dialog", Font.BOLD, 12));
	        FontMetrics fm = g2.getFontMetrics();
	        int textWidth = fm.stringWidth(name);
	        int nameX = screenX + gp.tileSize / 2 - textWidth / 2;
	        int nameY = screenY - 10;
	        g2.setColor(Color.WHITE);
	        g2.drawString(name, nameX, nameY);
	    }
	}

	public void draw(Graphics2D g2) {
	    BufferedImage image = null;
	    int screenX = x - gp.player.x + gp.player.screenX;
	    int screenY = y - gp.player.y + gp.player.screenY;

	    if (npcState == NPCState.ATTACK) {
	        switch (direction) {
	            case "up":     image = punchUp;    break;
	            case "down":   image = punchDown;  break;
	            case "left":   image = punchLeft;  break;
	            case "right":  image = punchRight; break;
	            case "up2":    image = punchUp2;   break;
	            case "down2":  image = punchDown2; break;
	            case "left2":  image = punchLeft2; break;
	            case "right2": image = punchRight2;break;
	        }
	        drawShadow(g2);
	        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
	        return;
	    }
	 // Golem punya draw() sendiri — langsung delegasikan
	    if (this instanceof Golem) {
	        ((Golem) this).draw(g2);
	        return;
	    }

	    if (this instanceof monster) {
	        switch (direction) {
	            case "down":  image = (imgNum==1)?mfront1:(imgNum==2)?mfront2:mstand; break;
	            case "up":    image = (imgNum==1)?mback1 :(imgNum==2)?mback2 :mstand; break;
	            case "left":  image = (imgNum==1)?mleft1 :(imgNum==2)?mleft2 :mstand; break;
	            case "right": image = (imgNum==1)?mright1:(imgNum==2)?mright2:mstand; break;
	        }
	        drawShadow(g2);
	        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
	        return;
	    }

	    if (isDead) return;

	    if (isHit) {
	        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
	        g2.setColor(Color.RED);
	        g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
	        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
	    }

	    if (!isDead && maxLife > 0) {
	        int barWidth  = gp.tileSize;
	        int barHeight = 4;
	        int hpX = screenX;
	        int hpY = screenY - 6;
	        g2.setColor(Color.RED);
	        g2.fillRect(hpX, hpY, barWidth, barHeight);
	        g2.setColor(Color.GREEN);
	        int hpFill = (int)((double) life / maxLife * barWidth);
	        g2.fillRect(hpX, hpY, hpFill, barHeight);
	    }

	    if (this instanceof NPCLow) {
	        image = (imgNum == 1) ? KaelFront1 : KaelFront2;
	        drawShadow(g2);
	        int scale = 2;
	        int drawX = screenX;
	        int drawY = screenY - gp.tileSize * (scale - 1);
	        g2.drawImage(image, drawX, drawY, gp.tileSize * scale, gp.tileSize * scale, null);
	        return;
	    }

	    if (x + gp.tileSize > gp.player.x - gp.player.screenX &&
	        x - gp.tileSize < gp.player.x + gp.player.screenX &&
	        y + gp.tileSize > gp.player.y - gp.player.screenY &&
	        y - gp.tileSize < gp.player.y + gp.player.screenY) {

	        switch (direction) {
	            case "down":
	                if (imgNum==1) image=front1; else if (imgNum==3) image=stand;
	                else if (imgNum==4) image=stand2; else image=front2; break;
	            case "up":
	                if (imgNum==1) image=back1;  else if (imgNum==3) image=standback;
	                else if (imgNum==4) image=standback2; else image=back2; break;
	            case "left":
	                if (imgNum==1) image=left1;  else if (imgNum==3) image=standleft;
	                else if (imgNum==4) image=standleft2; else image=left2; break;
	            case "right":
	                if (imgNum==1) image=right1; else if (imgNum==3) image=standright;
	                else if (imgNum==4) image=standright2; else image=right2; break;
	            case "emote":   image=hi;    break;
	            case "speedUp": speed=20;    break;
	        }

	        drawShadow(g2);
	        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
	    }
	}
}