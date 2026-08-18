package World;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * TransitionOverlay menangani efek fade-to-black saat player
 * masuk atau keluar dari sebuah bangunan.
 *
 * Cara pakai:
 *   overlay.startFade(callback)   ← panggil saat mau pindah state
 *   overlay.update()              ← panggil tiap frame di game loop
 *   overlay.draw(g2, w, h)        ← panggil paling TERAKHIR di draw()
 */
public class TransitionOverlay {

    public enum Phase { IDLE, FADE_IN, HOLD, FADE_OUT }

    // ── Konfigurasi ───────────────────────────────────────────────────────────
    private static final float FADE_SPEED = 0.07f;   // kecepatan fade (0–1 per frame)
    private static final int   HOLD_FRAMES = 8;      // frame nunggu saat layar penuh hitam

    // ── State internal ────────────────────────────────────────────────────────
    private Phase   phase      = Phase.IDLE;
    private float   alpha      = 0f;
    private int     holdTimer  = 0;
    private Runnable onMidpoint = null;   // callback dipanggil saat layar hitam penuh

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Mulai transisi.
     * @param midpointCallback dipanggil saat layar sudah hitam penuh
     *                         (saat inilah state game & posisi player diganti)
     */
    public void startFade(Runnable midpointCallback) {
        if (phase != Phase.IDLE) return;   // jangan dobel-trigger
        this.onMidpoint = midpointCallback;
        this.alpha      = 0f;
        this.phase      = Phase.FADE_IN;
    }

    public boolean isRunning() {
        return phase != Phase.IDLE;
    }

    // ── Update (panggil tiap frame) ───────────────────────────────────────────
    public void update() {
        switch (phase) {

            case FADE_IN:
                alpha += FADE_SPEED;
                if (alpha >= 1f) {
                    alpha     = 1f;
                    holdTimer = HOLD_FRAMES;
                    phase     = Phase.HOLD;
                }
                break;

            case HOLD:
                if (holdTimer-- <= 0) {
                    // Titik tengah: jalankan callback (ganti state/posisi)
                    if (onMidpoint != null) {
                        onMidpoint.run();
                        onMidpoint = null;
                    }
                    phase = Phase.FADE_OUT;
                }
                break;

            case FADE_OUT:
                alpha -= FADE_SPEED;
                if (alpha <= 0f) {
                    alpha = 0f;
                    phase = Phase.IDLE;
                }
                break;

            default:
                break;
        }
    }

    // ── Draw (panggil paling terakhir di pipeline render) ─────────────────────
    public void draw(Graphics2D g2, int screenWidth, int screenHeight) {
        if (phase == Phase.IDLE) return;

        // Simpan composite lama biar gak merusak render lain
        var oldComp = g2.getComposite();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setComposite(oldComp);
    }
}