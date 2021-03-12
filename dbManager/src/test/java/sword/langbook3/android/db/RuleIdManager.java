package sword.langbook3.android.db;

import sword.database.DbValue;

final class RuleIdManager implements IntSetter<RuleIdHolder> {

    @Override
    public RuleIdHolder getKeyFromInt(int key) {
        return (key == 0)? null : new RuleIdHolder(key);
    }

    @Override
    public RuleIdHolder getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }
}
