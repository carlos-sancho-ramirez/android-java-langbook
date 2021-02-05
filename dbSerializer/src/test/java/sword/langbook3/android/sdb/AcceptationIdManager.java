package sword.langbook3.android.sdb;

import sword.database.DbValue;

final class AcceptationIdManager implements sword.langbook3.android.db.IntSetter<AcceptationIdHolder> {

    @Override
    public AcceptationIdHolder getKeyFromInt(int key) {
        return new AcceptationIdHolder(key);
    }

    @Override
    public AcceptationIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
