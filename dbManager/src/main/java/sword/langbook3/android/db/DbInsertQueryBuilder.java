package sword.langbook3.android.db;

import sword.database.DbInsertQuery;
import sword.database.DbTable;

final class DbInsertQueryBuilder {

    private final DbTable _table;
    private final DbInsertQuery.Builder _builder;

    DbInsertQueryBuilder(DbTable table) {
        _table = table;
        _builder = new DbInsertQuery.Builder(table);
    }

    public DbInsertQueryBuilder put(int column, int value) {
        _builder.put(column, value);
        return this;
    }

    public DbInsertQueryBuilder put(int column, String value) {
        _builder.put(column, value);
        return this;
    }

    public DbInsertQueryBuilder put(int columnIndex, IdPutInterface value) {
        if (value != null) {
            value.put(columnIndex, _builder);
        }
        else if (_table.columns().get(columnIndex).isText()) {
            _builder.put(columnIndex, null);
        }
        else {
            _builder.put(columnIndex, 0);
        }

        return this;
    }

    public DbInsertQuery build() {
        return _builder.build();
    }
}
