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
}
