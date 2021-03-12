package sword.langbook3.android.db;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;

public final class BunchId extends ConceptId implements BunchIdInterface {

    BunchId(int key) {
        super(key);
    }

    @Override
    public boolean sameValue(DbValue value) {
        return value.toInt() == key;
    }

    @Override
    public int getConceptId() {
        return key;
    }

    @Override
    public boolean isNoBunchForQuiz() {
        return key == 0;
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
        return other instanceof BunchId && ((BunchId) other).key == key;
    }

    @Override
    public String toString() {
        return Integer.toString(key);
    }
}
