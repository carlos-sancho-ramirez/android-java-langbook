package sword.langbook3.android.db;

public final class DbIdColumn extends DbColumn {

    public static final String idColumnName = "id";

    DbIdColumn() {
        super(idColumnName);
    }

    @Override
    public boolean isText() {
        return false;
    }

    @Override
    public boolean isPrimaryKey() {
        return true;
    }

    @Override
    public boolean isUnique() {
        return true;
    }
}
