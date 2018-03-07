package sword.langbook3.android.db;

public final class DbIdColumn extends DbColumn {

    public static final String idColumnName = "id";

    DbIdColumn() {
        super(idColumnName);
    }

    @Override
    public String getSqlType() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public boolean isText() {
        return false;
    }
}
