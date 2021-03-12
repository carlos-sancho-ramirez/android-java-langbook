package sword.langbook3.android.db;

import sword.database.DbValue;

public final class RuleIdManager implements IntSetter<RuleId> {

    @Override
    public RuleId getKeyFromInt(int key) {
        return (key == 0)? null : new RuleId(key);
    }

    @Override
    public RuleId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static RuleId conceptAsRuleId(int concept) {
        return (concept == 0)? null : new RuleId(concept);
    }
}
