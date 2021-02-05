package sword.langbook3.android.db;

import sword.database.DbValue;

final class AcceptationIdManager implements IntSetter<AcceptationIdHolder> {

    @Override
    public AcceptationIdHolder getKeyFromInt(int key) {
        return new AcceptationIdHolder(key);
    }

    @Override
    public AcceptationIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
