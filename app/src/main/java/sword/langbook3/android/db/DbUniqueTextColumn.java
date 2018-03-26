package sword.langbook3.android.db;

public final class DbUniqueTextColumn extends DbColumn {

    public DbUniqueTextColumn(String name) {
        super(name);
    }

    @Override
    public String sqlType() {
        return "TEXT UNIQUE ON CONFLICT IGNORE";
    }

    @Override
    public boolean isText() {
        return true;
    }
}
