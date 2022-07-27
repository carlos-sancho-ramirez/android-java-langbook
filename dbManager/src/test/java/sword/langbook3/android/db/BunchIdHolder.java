package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;

final class BunchIdHolder implements BunchIdInterface<ConceptIdHolder> {

    final int key;

    BunchIdHolder(int key) {
        if (key == 0) {
            throw new IllegalArgumentException();
        }

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

        if (!(other instanceof BunchIdHolder)) {
            return false;
        }

        return ((BunchIdHolder) other).key == key;
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
    public ConceptIdHolder getConceptId() {
        return new ConceptIdHolder(key);
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
