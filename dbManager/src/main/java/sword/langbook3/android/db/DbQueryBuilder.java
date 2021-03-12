package sword.langbook3.android.db;

import sword.database.DbQuery;
import sword.database.DbTable;
import sword.database.DbView;

final class DbQueryBuilder {

    private final DbView _table;
    private final DbQuery.Builder _builder;

    DbQueryBuilder(DbView table) {
        _table = table;
        _builder = new DbQuery.Builder(table);
    }

    public DbQueryBuilder join(DbTable table, int left, int newTableColumnIndex) {
        _builder.join(table, left, newTableColumnIndex);
        return this;
    }

    public DbQueryBuilder where(int columnIndex, DbQuery.Restriction restriction) {
        _builder.where(columnIndex, restriction);
        return this;
    }

    public DbQueryBuilder where(int columnIndex, int value) {
        _builder.where(columnIndex, value);
        return this;
    }

    public DbQueryBuilder where(int columnIndex, String value) {
        _builder.where(columnIndex, value);
        return this;
    }

    public DbQueryBuilder where(int columnIndex, IdWhereInterface id) {
        if (id != null) {
            id.where(columnIndex, _builder);
        }
        else if (_table.columns().get(columnIndex).isText()) {
            _builder.where(columnIndex, (String) null);
        }
        else {
            _builder.where(columnIndex, 0);
        }

        return this;
    }

    public DbQueryBuilder whereColumnValueMatch(int columnIndexA, int columnIndexB) {
        _builder.whereColumnValueMatch(columnIndexA, columnIndexB);
        return this;
    }

    public DbQueryBuilder whereColumnValueDiffer(int columnIndexA, int columnIndexB) {
        _builder.whereColumnValueDiffer(columnIndexA, columnIndexB);
        return this;
    }

    public DbQueryBuilder orderBy(int... columnIndexes) {
        _builder.orderBy(columnIndexes);
        return this;
    }

    public DbQuery select(int... selection) {
        return _builder.select(selection);
    }
}
