package sword.langbook3.android.models;

public final class DynamizableResult {
    public final int id;
    public final boolean dynamic;
    public final String text;

    public DynamizableResult(int id, boolean dynamic, String text) {
        this.id = id;
        this.dynamic = dynamic;
        this.text = text;
    }
}
