package sword.langbook3.android.sdb;

import sword.database.DbIdentifiableQueryBuilder;
import sword.database.DbSettableQueryBuilder;
import sword.database.DbValue;
import sword.langbook3.android.db.CharacterIdInterface;

final class CharacterIdHolder implements CharacterIdInterface {

    final int key;

    CharacterIdHolder(int key) {
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

        if (!(other instanceof CharacterIdHolder)) {
            return false;
        }

        return ((CharacterIdHolder) other).key == key;
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
    public void put(int columnIndex, DbSettableQueryBuilder builder) {
        builder.put(columnIndex, key);
    }
}
