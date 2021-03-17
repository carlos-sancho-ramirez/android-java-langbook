package sword.langbook3.android.db;

import sword.database.DbValue;

public final class RuleIdManager implements ConceptualizableSetter<ConceptId, RuleId> {

    @Override
    public RuleId getKeyFromInt(int key) {
        return (key == 0)? null : new RuleId(key);
    }

    @Override
    public RuleId getKeyFromDbValue(DbValue value) {
        return getKeyFromInt(value.toInt());
    }

    public static RuleId conceptAsRuleId(ConceptId concept) {
        return (concept == null)? null : new RuleId(concept.key);
    }

    @Override
    public RuleId getKeyFromConceptId(ConceptId concept) {
        return (concept == null)? null : new RuleId(concept.key);
    }
}
