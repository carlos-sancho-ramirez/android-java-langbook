package sword.langbook3.android.db;

import sword.collections.ImmutableList;

public interface DbSchema {

    ImmutableList<DbTable> tables();
    ImmutableList<DbIndex> indexes();

    final class DbIndex {
        public final DbTable table;
        public final int column;

        public DbIndex(DbTable table, int column) {
            if (table == null || column < 0 || column >= table.getColumnCount()) {
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
}
