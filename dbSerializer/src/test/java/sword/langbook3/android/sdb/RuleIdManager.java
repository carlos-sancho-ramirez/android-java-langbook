package sword.langbook3.android.sdb;

import sword.database.DbValue;
import sword.langbook3.android.db.IntSetter;

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
