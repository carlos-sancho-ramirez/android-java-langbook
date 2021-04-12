package sword.langbook3.android.sdb;

import sword.database.DbValue;
import sword.langbook3.android.db.IntSetter;

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
