package sword.langbook3.android.models;

public final class IdentifiableResult<Id> {
    public final Id id;
    public final String text;

    public IdentifiableResult(Id id, String text) {
        this.id = id;
        this.text = text;
    }
}
