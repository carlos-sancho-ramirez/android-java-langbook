package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;

import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID;

public final class CorrelationArrayId implements CorrelationArrayIdInterface {

    final int key;

    CorrelationArrayId(int key) {
        this.key = key;
    }

    @Override
    public boolean isEmptyReference() {
        return key == EMPTY_CORRELATION_ARRAY_ID;
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
    public void put(int columnIndex, DbSettableQueryBuilder builder) {
        builder.put(columnIndex, key);
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CorrelationArrayId && ((CorrelationArrayId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
