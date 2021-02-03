package sword.langbook3.android.models;

public final class IdTextPairResult<Id> {
    public final Id id;
    public final String text;

    public IdTextPairResult(Id id, String text) {
        this.id = id;
        this.text = text;
    }
}
