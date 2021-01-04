package sword.langbook3.android.sdb;

import sword.database.DbValue;

final class AlphabetIdManager implements sword.langbook3.android.db.IntSetter<AlphabetIdHolder> {

    @Override
    public AlphabetIdHolder getKeyFromInt(int key) {
        return new AlphabetIdHolder(key);
    }

    @Override
    public AlphabetIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
