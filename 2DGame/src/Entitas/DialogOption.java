package Entitas;

public class DialogOption {

    public String text;
    public DialogNode next;

    public DialogOption(String text, DialogNode next) {
        this.text = text;
        this.next = next;
    }
}
