package sword.langbook3.android.sdb;

import sword.database.DbValue;
import sword.langbook3.android.db.IntSetter;

final class CorrelationIdManager implements IntSetter<CorrelationIdHolder> {

    @Override
    public CorrelationIdHolder getKeyFromInt(int key) {
        return new CorrelationIdHolder(key);
    }

    @Override
    public CorrelationIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
