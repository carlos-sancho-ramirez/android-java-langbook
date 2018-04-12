package sword.langbook3.android.db;

public final class DbUniqueTextColumn extends DbColumn {

    public DbUniqueTextColumn(String name) {
        super(name);
    }

    @Override
    public boolean isText() {
        return true;
    }

    @Override
    public boolean isUnique() {
        return true;
    }
}
