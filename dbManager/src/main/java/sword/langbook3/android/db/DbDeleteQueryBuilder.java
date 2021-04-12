package sword.langbook3.android.db;

import sword.database.DbDeleteQuery;
import sword.database.DbTable;
import sword.database.DbView;

final class DbDeleteQueryBuilder {

    private final DbView _table;
    private final DbDeleteQuery.Builder _builder;

    DbDeleteQueryBuilder(DbTable table) {
        _table = table;
        _builder = new DbDeleteQuery.Builder(table);
    }

    public DbDeleteQueryBuilder where(int columnIndex, int value) {
        _builder.where(columnIndex, value);
        return this;
    }

    public DbDeleteQueryBuilder where(int columnIndex, IdWhereInterface id) {
        if (id != null) {
            id.where(columnIndex, _builder);
        }
        else if (_table.columns().get(columnIndex).isText()) {
            throw new UnsupportedOperationException();
        }
        else {
            _builder.where(columnIndex, 0);
        }

        return this;
    }

    public DbDeleteQuery build() {
        return _builder.build();
    }
}
