package sword.langbook3.android.db;

import sword.database.DbValue;

final class SentenceIdManager implements IntSetter<SentenceIdHolder> {

    @Override
    public SentenceIdHolder getKeyFromInt(int key) {
        return (key == 0)? null : new SentenceIdHolder(key);
    }

    @Override
    public SentenceIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
