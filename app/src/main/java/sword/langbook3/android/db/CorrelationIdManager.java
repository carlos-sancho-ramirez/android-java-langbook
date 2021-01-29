package sword.langbook3.android.db;

import sword.database.DbValue;

public final class CorrelationIdManager implements IntSetter<CorrelationId> {

    @Override
    public CorrelationId getKeyFromInt(int key) {
        return (key != 0)? new CorrelationId(key) : null;
    }

    @Override
    public CorrelationId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
