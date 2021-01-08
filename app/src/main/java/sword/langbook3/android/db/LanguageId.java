package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbInsertQuery;
import sword.database.DbValue;

public final class LanguageId extends ConceptId implements LanguageIdInterface {

    LanguageId(int key) {
        super(key);
    }

    @Override
    public boolean sameValue(DbValue value) {
        return value.toInt() == key;
    }

    @Override
    public void where(int columnIndex, DbIdentifiableQueryBuilder builder) {
        builder.where(columnIndex, key);
    }

    @Override
    public void put(int columnIndex, DbInsertQuery.Builder builder) {
        builder.put(columnIndex, key);
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LanguageId && ((LanguageId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
