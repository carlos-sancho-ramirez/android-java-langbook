package sword.langbook3.android.db;

import sword.database.DbDeleteQuery;
import sword.database.DbTable;

final class DbDeleteQueryBuilder {

    private final DbDeleteQuery.Builder _builder;

    DbDeleteQueryBuilder(DbTable table) {
        _builder = new DbDeleteQuery.Builder(table);
    }

    public DbDeleteQueryBuilder where(int columnIndex, int value) {
        _builder.where(columnIndex, value);
        return this;
    }

    public DbDeleteQueryBuilder where(int columnIndex, LanguageIdInterface language) {
        language.where(columnIndex, _builder);
        return this;
    }

    public DbDeleteQueryBuilder where(int columnIndex, AlphabetIdInterface alphabet) {
        alphabet.where(columnIndex, _builder);
        return this;
    }

    public DbDeleteQuery build() {
        return _builder.build();
    }
}
