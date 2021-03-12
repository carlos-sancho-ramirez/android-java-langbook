package sword.langbook3.android.db;

import sword.database.DbTable;
import sword.database.DbUpdateQuery;

final class DbUpdateQueryBuilder {

    private final DbTable _table;
    private final DbUpdateQuery.Builder _builder;

    DbUpdateQueryBuilder(DbTable table) {
        _table = table;
        _builder = new DbUpdateQuery.Builder(table);
    }

    public DbUpdateQueryBuilder where(int columnIndex, int value) {
        _builder.where(columnIndex, value);
        return this;
    }

    public DbUpdateQueryBuilder where(int columnIndex, IdWhereInterface id) {
        id.where(columnIndex, _builder);
        return this;
    }

    public DbUpdateQueryBuilder put(int columnIndex, int value) {
        _builder.put(columnIndex, value);
        return this;
    }

    public DbUpdateQueryBuilder put(int columnIndex, String value) {
        _builder.put(columnIndex, value);
        return this;
    }

    public DbUpdateQueryBuilder put(int columnIndex, IdPutInterface id) {
        if (id != null) {
            id.put(columnIndex, _builder);
        }
        else if (_table.columns().get(columnIndex).isText()) {
            _builder.put(columnIndex, null);
        }
        else {
            _builder.put(columnIndex, 0);
        }

        return this;
    }

    public DbUpdateQuery build() {
        return _builder.build();
    }
}
