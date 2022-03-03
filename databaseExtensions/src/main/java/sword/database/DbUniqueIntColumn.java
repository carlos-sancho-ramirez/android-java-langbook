package sword.database;

public final class DbUniqueIntColumn extends DbColumn {

    public DbUniqueIntColumn(String name) {
        super(name);
    }

    @Override
    public boolean isText() {
        return false;
    }

    @Override
    public boolean isUnique() {
        return true;
    }
}
