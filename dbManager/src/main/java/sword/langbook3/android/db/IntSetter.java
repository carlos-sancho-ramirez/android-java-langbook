package sword.langbook3.android.db;

import sword.database.DbValue;

public interface IntSetter<T> {

    T getKeyFromInt(int key);
    T getKeyFromDbValue(DbValue value);
}
