package sword.langbook3.android.db;

import sword.database.DbValue;

public final class BunchIdManager implements IntSetter<BunchId> {

    @Override
    public BunchId getKeyFromInt(int key) {
        return new BunchId(key);
    }

    @Override
    public BunchId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static BunchId conceptAsBunchId(int concept) {
        return new BunchId(concept);
    }
}
