package sword.langbook3.android.db;

import sword.database.DbValue;

final class CorrelationArrayIdManager implements IntSetter<CorrelationArrayIdHolder> {

    @Override
    public CorrelationArrayIdHolder getKeyFromInt(int key) {
        return new CorrelationArrayIdHolder(key);
    }

    @Override
    public CorrelationArrayIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
