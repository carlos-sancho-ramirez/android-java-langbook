package sword.langbook3.android.db;

public abstract class DbColumn {

    private final String _name;

    DbColumn(String name) {
        _name = name;
    }

    public final String name() {
        return _name;
    }

    /**
     * Whether the field content may be understood as a char sequence.
     * Right now there is only int and text fields, then so far it is secure
     * to assume that all non-text field are actually int fields.
     */
    public abstract boolean isText();

    public boolean isPrimaryKey() {
        return false;
    }

    public boolean isUnique() {
        return false;
    }
}
