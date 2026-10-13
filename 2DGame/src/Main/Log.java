package Main;

/**
 * Logger sederhana. Pesan debug cuma tampil kalau Log.enabled = true
 * (tekan F3 di dalam game untuk menyalakan/mematikan).
 * Pesan error/peringatan tetap pakai System.out/System.err biasa.
 */
public final class Log {
    public static volatile boolean enabled = false;

    private Log() {}

    public static void d(Object msg) {
        if (enabled) System.out.println(msg);
    }
}
