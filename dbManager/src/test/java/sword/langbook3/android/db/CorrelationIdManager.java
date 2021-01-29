package sword.langbook3.android.db;

import sword.database.DbValue;

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
