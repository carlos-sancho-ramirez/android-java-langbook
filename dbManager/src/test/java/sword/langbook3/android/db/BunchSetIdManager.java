package sword.langbook3.android.db;

import sword.database.DbValue;

final class BunchSetIdManager implements BunchSetIntSetter<BunchSetIdHolder> {

    @Override
    public BunchSetIdHolder getKeyFromInt(int key) {
        return new BunchSetIdHolder(key);
    }

    @Override
    public BunchSetIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    @Override
    public BunchSetIdHolder getDeclaredEmpty() {
        return new BunchSetIdHolder(LangbookDbSchema.EMPTY_BUNCH_SET_ID);
    }
}
