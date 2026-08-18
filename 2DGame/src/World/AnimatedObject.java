package World;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;

public class AnimatedObject {

    public ImageIcon[] frames;   // array 8 frame
    public int worldX, worldY;
    public int width, height;    // ukuran beda per objek

    private int currentFrame = 0;
    private int frameTimer   = 0;
    public int frameDelay    = 6; // makin kecil = makin cepet animasinya

    public AnimatedObject(String[] framePaths, int worldX, int worldY,
                          int width, int height, int frameDelay, Class<?> loader) {
        this.worldX     = worldX;
        this.worldY     = worldY;
        this.width      = width;
        this.height     = height;
        this.frameDelay = frameDelay;

        frames = new ImageIcon[framePaths.length];
        for (int i = 0; i < framePaths.length; i++) {
            var url = loader.getResource(framePaths[i]);
            if (url != null) {
                frames[i] = new ImageIcon(url);
            }
        }
    }

    public void update() {
        frameTimer++;
        if (frameTimer >= frameDelay) {
            frameTimer = 0;
            currentFrame = (currentFrame + 1) % frames.length;
        }
    }

    public void draw(Graphics2D g2, int screenX, int screenY) {
        if (frames[currentFrame] != null) {
            g2.drawImage(frames[currentFrame].getImage(),
                         screenX, screenY, width, height, null);
        }
    }
}