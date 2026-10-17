package Entitas;

public class DialogOption {

    public String text;
    public DialogNode next;

    /** Opsional: dijalankan sekali pas opsi ini dipilih (mis. buka toko). null = tidak ada aksi. */
    public Runnable action;

    public DialogOption(String text, DialogNode next) {
        this(text, next, null);
    }

    public DialogOption(String text, DialogNode next, Runnable action) {
        this.text = text;
        this.next = next;
        this.action = action;
    }
}
