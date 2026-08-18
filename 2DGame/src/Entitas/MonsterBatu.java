package Entitas;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import Main.GamePanel;

public class MonsterBatu extends Entity {

    public enum MonsterState {
        IDLE,
        CHASE,
        ATTACK,
        HIT,
        DEAD
    }

    MonsterState state = MonsterState.IDLE;

    int maxLife = 200;
    int life = maxLife;

    int attackDamage = 15;

    int aggroRadius = 200;
    int attackRadius = 40;

    boolean isDead = false;

    BufferedImage idle;
    BufferedImage hit;

    public MonsterBatu(GamePanel gp) {
        super(gp);
        x = gp.tileSize * 10;  // ← set di sini
        y = gp.tileSize * 15;
        // wanderArea otomatis bener karena spawnX/spawnY assign setelah x y bener
    }
    
    public void loadImages() {
        try {

            idle = ImageIO.read(getClass().getResourceAsStream("/Aset/Sticklooter_carrot_idle3.png"));
            hit = ImageIO.read(getClass().getResourceAsStream("/Aset/Sticklooter_carrot_idle3.png"));

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    // =========================
    // DAMAGE SYSTEM MONSTER
    // =========================
    public void takeDamage(int dmg, Entity attacker){

        if(state == MonsterState.DEAD) return;

        life -= dmg;

        state = MonsterState.HIT;

        if(life <= 0){
            life = 0;
            die();
        }
    }

    public void die(){

        state = MonsterState.DEAD;
        isDead = true;

        System.out.println("Monster Batu Mati");
    }

    // =========================
    // AI UPDATE
    // =========================
    public void update(){

        if(state == MonsterState.DEAD) return;

        int dx = gp.player.x - x;
        int dy = gp.player.y - y;

        double playerDist = Math.sqrt(dx*dx + dy*dy);

        switch(state){

            case IDLE:

                if(playerDist < aggroRadius){
                    state = MonsterState.CHASE;
                }

                break;

            case CHASE:

                chasePlayer();

                if(playerDist < attackRadius){
                    state = MonsterState.ATTACK;
                }

                if(playerDist > aggroRadius){
                    state = MonsterState.IDLE;
                }

                break;

            case ATTACK:

                attackPlayer();
                state = MonsterState.CHASE;

                break;

            case HIT:

                // setelah kena pukul balik ngejar
                state = MonsterState.CHASE;
                break;

            case DEAD:
                break;
        }
    }

    public void chasePlayer(){

        if(gp.player.x < x) x -= speed;
        if(gp.player.x > x) x += speed;

        if(gp.player.y < y) y -= speed;
        if(gp.player.y > y) y += speed;
    }

    public void attackPlayer(){

        gp.player.takeDamage(attackDamage);
    }

    // =========================
    // DRAW
    // =========================
    public void draw(Graphics2D g2){

        if(state == MonsterState.DEAD) return;

        BufferedImage image = idle;

        if(state == MonsterState.HIT){
            image = hit;
        }

        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);

        drawHPBar(g2, screenX, screenY);
    }

    public void drawHPBar(Graphics2D g2, int x, int y){

        int width = 40;

        g2.setColor(Color.black);
        g2.fillRect(x, y - 10, width, 5);

        g2.setColor(Color.red);

        int hpWidth = (int)((double)life / maxLife * width);

        g2.fillRect(x, y - 10, hpWidth, 5);
    }
}