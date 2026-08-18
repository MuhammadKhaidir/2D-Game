package Item;

/**
 * SuperItem — Kelas dasar untuk item-item spesial/unik
 * (DeathTotem, dll yang akan dibuat nanti).
 *
 * Berbeda dari Item biasa: punya state, bisa aktif/nonaktif,
 * dan punya deskripsi status dinamis.
 */
public abstract class SuperItem {

    public final String name;
    public final String description;
    public final String iconPath;

    protected SuperItem(String name, String description, String iconPath) {
        this.name        = name;
        this.description = description;
        this.iconPath    = iconPath;
    }

    /**
     * Deskripsi status real-time item ini.
     * Tiap subclass override sesuai state internalnya.
     */
    public abstract String getStatusDescription();
}