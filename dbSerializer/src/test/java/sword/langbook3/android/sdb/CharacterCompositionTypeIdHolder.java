package sword.langbook3.android.sdb;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;
import sword.langbook3.android.db.CharacterCompositionTypeIdInterface;

final class CharacterCompositionTypeIdHolder implements CharacterCompositionTypeIdInterface<ConceptIdHolder> {

    final int key;

    CharacterCompositionTypeIdHolder(int key) {
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

        if (!(other instanceof CharacterCompositionTypeIdHolder)) {
            return false;
        }

        return ((CharacterCompositionTypeIdHolder) other).key == key;
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
