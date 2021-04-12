package sword.langbook3.android.db;

import sword.database.DbValue;

public final class SentenceIdManager implements IntSetter<SentenceId> {

    @Override
    public SentenceId getKeyFromInt(int key) {
        return (key != 0)? new SentenceId(key) : null;
    }

    @Override
    public SentenceId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
