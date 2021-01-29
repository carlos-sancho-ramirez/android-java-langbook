package sword.langbook3.android.db;

import sword.database.DbValue;

public final class CorrelationArrayIdManager implements IntSetter<CorrelationArrayId> {

    @Override
    public CorrelationArrayId getKeyFromInt(int key) {
        return new CorrelationArrayId(key);
    }

    @Override
    public CorrelationArrayId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
