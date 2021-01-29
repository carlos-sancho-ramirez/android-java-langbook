package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;

import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID;

final class CorrelationArrayIdHolder implements CorrelationArrayIdInterface {

    final int key;

    CorrelationArrayIdHolder(int key) {
        this.key = key;
    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof CorrelationArrayIdHolder)) {
            return false;
        }

        return ((CorrelationArrayIdHolder) other).key == key;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + key + ")";
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
}
