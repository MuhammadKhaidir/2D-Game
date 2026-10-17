package Entitas;

import java.util.ArrayList;
import java.util.List;

public class DialogNode {

    public String text;
    public List<DialogOption> options = new ArrayList<>();

    public DialogNode(String text) {
        this.text = text;
    }

    public void addOption(String optionText, DialogNode next) {
        options.add(new DialogOption(optionText, next));
    }

    /** Sama seperti di atas, tapi {@code action} dijalankan pas opsi ini dipilih (mis. buka toko). */
    public void addOption(String optionText, DialogNode next, Runnable action) {
        options.add(new DialogOption(optionText, next, action));
    }
}
