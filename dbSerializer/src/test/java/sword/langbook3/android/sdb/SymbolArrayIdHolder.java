package sword.langbook3.android.sdb;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbInsertQuery;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.langbook3.android.db.SymbolArrayIdInterface;

final class SymbolArrayIdHolder implements SymbolArrayIdInterface {

    final int key;

    SymbolArrayIdHolder(int key) {
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

        if (!(other instanceof SymbolArrayIdHolder)) {
            return false;
        }

        return ((SymbolArrayIdHolder) other).key == key;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + key + ")";
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
    public void put(int columnIndex, DbUpdateQuery.Builder builder) {
        builder.put(columnIndex, key);
    }
}
