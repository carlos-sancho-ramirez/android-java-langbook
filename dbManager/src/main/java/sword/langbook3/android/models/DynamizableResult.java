package sword.langbook3.android.models;

public final class DynamizableResult<Id> {
    public final Id id;
    public final boolean dynamic;
    public final String text;

    public DynamizableResult(Id id, boolean dynamic, String text) {
        this.id = id;
        this.dynamic = dynamic;
        this.text = text;
    }
}
