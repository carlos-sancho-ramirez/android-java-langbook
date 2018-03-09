package sword.langbook3.android.db;

public final class DbIntColumn extends DbColumn {

    public DbIntColumn(String name) {
        super(name);
    }

    @Override
    public String getSqlType() {
        return "INTEGER";
    }

    @Override
    public boolean isText() {
        return false;
    }
}