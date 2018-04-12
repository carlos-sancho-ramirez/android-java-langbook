package sword.langbook3.android.db;

public interface DbValue {
    boolean isText();

    int toInt() throws UnsupportedOperationException;
    String toText();
}
