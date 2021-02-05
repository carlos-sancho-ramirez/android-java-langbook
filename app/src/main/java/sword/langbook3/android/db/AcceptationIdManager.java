package sword.langbook3.android.db;

import sword.database.DbValue;

public final class AcceptationIdManager implements IntSetter<AcceptationId> {

    @Override
    public AcceptationId getKeyFromInt(int key) {
        return (key != 0)? new AcceptationId(key) : null;
    }

    @Override
    public AcceptationId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
