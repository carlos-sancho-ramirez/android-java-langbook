package sword.langbook3.android.db;

public final class DbIndex {
    public final DbTable table;
    public final int column;

    public DbIndex(DbTable table, int column) {
        if (table == null || column < 0 || column >= table.columns().size()) {
            throw new IllegalArgumentException();
        }

        this.table = table;
        this.column = column;
    }

    @Override
    public int hashCode() {
        return table.hashCode() * 13 + column;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DbIndex)) {
            return false;
        }

        final DbIndex that = (DbIndex) other;
        return column == that.column && table.equals(that.table);
    }
}
