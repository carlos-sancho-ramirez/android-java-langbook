package sword.langbook3.android.models;

public final class DisplayableItem<Id> {

    public final Id id;
    public final String text;

    public DisplayableItem(Id id, String text) {
        this.id = id;
        this.text = text;
    }
}
