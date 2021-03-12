package sword.langbook3.android.sdb;

import sword.database.DbValue;
import sword.langbook3.android.db.IntSetter;

final class BunchIdManager implements IntSetter<BunchIdHolder> {

    @Override
    public BunchIdHolder getKeyFromInt(int key) {
        return new BunchIdHolder(key);
    }

    @Override
    public BunchIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
