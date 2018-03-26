package sword.langbook3.android.db;

public final class DbTextColumn extends DbColumn {

    public DbTextColumn(String name) {
        super(name);
    }

    @Override
    public String sqlType() {
        return "TEXT";
    }

    @Override
    public boolean isText() {
        return true;
    }
}
