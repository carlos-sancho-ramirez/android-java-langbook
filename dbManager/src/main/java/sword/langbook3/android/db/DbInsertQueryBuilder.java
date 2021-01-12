package sword.langbook3.android.db;

import sword.database.DbInsertQuery;
import sword.database.DbTable;

final class DbInsertQueryBuilder {

    private final DbInsertQuery.Builder _builder;

    DbInsertQueryBuilder(DbTable table) {
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

    public DbInsertQueryBuilder put(int columnIndex, LanguageIdInterface language) {
        language.put(columnIndex, _builder);
        return this;
    }

    public DbInsertQueryBuilder put(int columnIndex, AlphabetIdInterface alphabet) {
        alphabet.put(columnIndex, _builder);
        return this;
    }

    public DbInsertQueryBuilder put(int columnIndex, SymbolArrayIdInterface alphabet) {
        alphabet.put(columnIndex, _builder);
        return this;
    }

    public DbInsertQuery build() {
        return _builder.build();
    }
}
