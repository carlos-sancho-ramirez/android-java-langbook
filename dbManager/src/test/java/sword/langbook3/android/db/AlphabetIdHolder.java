package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbInsertQuery;
import sword.database.DbValue;

final class AlphabetIdHolder implements AlphabetIdInterface {

    final int key;

    AlphabetIdHolder(int key) {
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

        if (!(other instanceof AlphabetIdHolder)) {
            return false;
        }

        return ((AlphabetIdHolder) other).key == key;
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
}
